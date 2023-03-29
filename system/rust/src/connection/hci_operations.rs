//! The basic HCI operations used in connection management. These are sent
//! to the HCI layer directly, except for acceptlist operations, which go
//! to the address manager.

use std::fmt::Debug;

use crate::core::address::AddressWithType;

use super::{Connection, Role};

/// An HCI Error Code from the controller
#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub struct ErrorCode(pub u8);

impl ErrorCode {
    /// Operation completed successfully
    pub const SUCCESS: Self = ErrorCode(0);
}

/// HCI operations exposed from the lower layer to perform
/// connection management.
pub trait HciConnectProxy: Debug {
    /// Send an HCI LE (Extended) Create Connection command
    fn create_connect(&self, is_direct: bool);
    /// Send an HCI LE Cancel Create Connection Command
    fn cancel_connect(&self);
    /// Add an address to the filter accept list via the address manager
    fn add_to_accept_list(&self, address: AddressWithType);
    /// Remove an address to the filter accept list via the address manager
    fn remove_from_accept_list(&self, address: AddressWithType);
    /// Send an HCI Disconnect command
    fn disconnect(&self, conn: Connection);
}

/// Events generated that we are interested in
pub enum HciEvent {
    /// The status event generated from an LE Create Connection command
    CreateConnectionStatus(ErrorCode),
    /// The Create Connection Complete event generated from a successful connection,
    /// or cancelling an outstanding connection attempt.
    CreateConnectionComplete(AddressWithType, Role, ErrorCode),
}
