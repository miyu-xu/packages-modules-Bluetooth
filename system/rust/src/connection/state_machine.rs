#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub struct AddressWithType(u8);

#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub struct ErrorCode(u8);

impl ErrorCode {
    const SUCCESS: Self = ErrorCode(0);
    const UNKNOWN_CONNECTION: Self = ErrorCode(0x02);
}

#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub enum Role {
    Central,
    Peripheral,
}

trait HciConnectProxy {
    fn create_connect(&self, is_direct: bool);
    fn cancel_connect(&self);
    fn add_to_accept_list(&self, address: AddressWithType);
    fn remove_from_accept_list(&self, address: AddressWithType);
}

trait AddressManager {
    fn ack_pause(&self);
    fn ack_resume(&self);
}

trait ConnectionCallbacks {
    fn on_connection_complete(&self, address: AddressWithType, role: Role);
}

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

pub struct HciConnectionStateMachine {
    // dependencies
    hci: Box<dyn HciConnectProxy>,
    address_manager: Box<dyn AddressManager>,

    // inputs
    requested_state: InitiatorStableState, // input from connection manager
    is_paused: bool,                       // input from address manager

    // outputs
    callbacks: Box<dyn ConnectionCallbacks>,

    // current state
    state: InitiatorState,
    need_to_ack: bool, // whether we need to send an ack to the address manager when we reach its target state
}

impl HciConnectionStateMachine {
    /// This is used to indicate what state the connection manager wants us to achieve
    /// Depending on the pause/resume request from address manager, we may try to achieve a different state instead
    pub fn request_state(&mut self, state: InitiatorStableState) {
        self.requested_state = state;
        self.drive_to_target_state();
    }

    pub fn on_connection_status(&mut self, status: ErrorCode) {
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

        self.drive_to_target_state();
    }

    pub fn on_connection_complete(
        &mut self,
        address: AddressWithType,
        role: Role,
        status: ErrorCode,
    ) {
        if status == ErrorCode::SUCCESS {
            self.callbacks.on_connection_complete(address, role);
        }

        if role == Role::Central {
            // If we inform the upper layer, then they are responsible for informing us what state to take next.
            // This is safe since request_state is idempotent, so even if they don't know what state we are
            // currently in, they can always re-request it.
            // In the meantime, we will remain in the state we are currently in (stopped).
            self.requested_state = InitiatorStableState::Stopped;
            self.state = InitiatorState::StableAt(InitiatorStableState::Stopped);
        }

        self.drive_to_target_state();
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
