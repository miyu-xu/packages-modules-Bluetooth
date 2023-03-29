use std::{fmt::Debug, rc::Rc};

use crate::core::address::AddressWithType;

use super::{
    hci_operations::{ErrorCode, HciConnectProxy, HciEvent},
    AddressManager, Connection, Role,
};

#[derive(Copy, Clone, Eq, PartialEq, Debug)]
enum InitiatorState {
    WaitingToEnter(InitiatorStableState), // We have sent the HCI command to reach this state, but have not yet reached it
    StableAt(InitiatorStableState), // We are currently in this state and have no commands pending
}

#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub enum InitiatorStableState {
    Stopped,
    BackgroundInitiation,
    DirectInitiation,
}

#[derive(Debug)]
pub struct HciConnectionStateMachine {
    // dependencies
    hci: Rc<dyn HciConnectProxy>,
    address_manager: Box<dyn AddressManager>,

    // inputs
    requested_state: InitiatorStableState, // input from connection manager
    is_paused: bool,                       // input from address manager

    // current state
    state: InitiatorState,
    need_to_ack: bool, // whether we need to send an ack to the address manager when we reach its target state
}

impl HciConnectionStateMachine {
    /// Constructor
    pub fn new(
        hci: Rc<dyn HciConnectProxy>,
        address_manager: impl AddressManager + 'static,
    ) -> Self {
        Self {
            hci,
            address_manager: Box::new(address_manager),
            requested_state: InitiatorStableState::Stopped, // after reset, we are idle initially
            is_paused: false, // when initially registered with the address manager, we expect to be unpaused
            state: InitiatorState::StableAt(InitiatorStableState::Stopped), // nothing is pending after reset
            need_to_ack: false,
        }
    }

    /// This is used to indicate what state the connection manager wants us to achieve.
    /// Depending on the pause/resume request from address manager, we may try to achieve a different state instead
    pub fn request_state(&mut self, state: InitiatorStableState) {
        self.requested_state = state;
        self.drive_to_target_state();
    }

    /// Invoked when we get an event from the controller. If a new connection is available,
    /// return it and move to the idle state (so request_state() must be called to resume operation).
    pub fn on_hci_event(&mut self, event: HciEvent) -> Option<Connection> {
        match event {
            HciEvent::CreateConnectionStatus(status) => {
                self.on_connection_status(status);
                None
            }
            HciEvent::CreateConnectionComplete(address, role, status) => {
                self.on_connection_complete(address, role, status)
            }
        }
    }

    fn on_connection_status(&mut self, status: ErrorCode) {
        let InitiatorState::WaitingToEnter(
              state @ (InitiatorStableState::BackgroundInitiation
              | InitiatorStableState::DirectInitiation),
          ) = self.state else {
            unreachable!(
                "got unexpected CreateConnectionStatus when not awaiting a CreateConnection"
            )
          };
        if status == ErrorCode::SUCCESS {
            // we have reached our pending state
            self.state = InitiatorState::StableAt(state);
        } else {
            // we failed to start a connection
            self.state = InitiatorState::StableAt(InitiatorStableState::Stopped)
        }

        // TODO(aryarahul): can we avoid getting in a loop of continuously failing to start?
        // how can this be surfaced to clients?

        self.drive_to_target_state();
    }

    fn on_connection_complete(
        &mut self,
        address: AddressWithType,
        role: Role,
        status: ErrorCode,
    ) -> Option<Connection> {
        if role == Role::Central {
            // if we got this event with Role=Central, we are no longer initiating
            // (either because of a new connection, or because we cancelled)
            self.state = InitiatorState::StableAt(InitiatorStableState::Stopped);
            if status == ErrorCode::SUCCESS {
                // If we got a successful connection as central, remain in the
                // stopped state until a further command is received.
                self.requested_state = InitiatorStableState::Stopped;
            }
            self.drive_to_target_state();
        };

        if status == ErrorCode::SUCCESS {
            Some(Connection { remote_address: address, role })
        } else {
            None
        }
    }

    pub fn pause(&mut self) {
        assert!(!self.need_to_ack, "already handling address manager request");
        self.is_paused = true;
        self.need_to_ack = true;

        self.drive_to_target_state();
    }

    pub fn resume(&mut self) {
        assert!(!self.need_to_ack, "already handling address manager request");
        self.is_paused = false;
        self.need_to_ack = true;

        self.drive_to_target_state();
    }

    /// This combines the requests from the connection + address managers to determine
    /// what state we should try to achieve next
    fn target_state(&self) -> InitiatorStableState {
        if self.is_paused {
            // if we are paused, we should always try to stop initiating
            InitiatorStableState::Stopped
        } else {
            // otherwise, try to do whatever the connection manager wants
            self.requested_state
        }
    }

    fn drive_to_target_state(&mut self) {
        let InitiatorState::StableAt(state) = self.state else {
          // this is a transient state, so don't do anything until the HCI command completes
          // and we know what state we are in
          return;
        };

        let target_state = self.target_state();

        if state == target_state {
            // handle address manager requests
            if self.need_to_ack {
                if self.is_paused {
                    self.address_manager.ack_pause()
                } else {
                    // Note: it is possible that we are resuming even if we are still stopped
                    // this happens in the case when our requested state is stopped
                    //
                    // Further note: if the requested state changes *while* we are resuming,
                    // we may end up going from stopped -> direct -> stopped -> background
                    // We will only ack_resume() once we stabilize.
                    //
                    // TODO(aryarahul): write a test for this!
                    self.address_manager.ack_resume()
                }
                self.need_to_ack = false;
            }

            // no need to do anything further here
            return;
        }

        if state == InitiatorStableState::Stopped {
            // if we are stopped, and this is not the target state, we need to restart initiation
            self.hci.create_connect(target_state == InitiatorStableState::DirectInitiation);
            self.state = InitiatorState::WaitingToEnter(target_state);
        } else {
            // otherwise, before changing states, we need to stop
            self.hci.cancel_connect();
            self.state = InitiatorState::WaitingToEnter(target_state);
        }
    }
}
