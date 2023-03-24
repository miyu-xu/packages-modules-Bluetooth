//! This trait represents the lower-level operations
//! made available to the connection manager. In particular,
//! we can add devices to either the "direct" or "background"
//! connect list, which are in turn mapped to an appropriate choice
//! of scan parameters / the filter accept list.
//!
//! Note that the connection manager is unaware of address resolution,
//! so this must be handled by the upper layer. Conversely, the upper layer
//! does not need to consider the HCI state machine, and can send requests
//! at any time.
//! 
//! In addition to the supplied API, when a connection completes to a peer device,
//! it is removed from the "direct" connect list (based on exact address match).

use std::fmt::Debug;

#[derive(Copy, Clone, Debug, Eq, PartialEq, Hash)]
pub struct AddressWithType(u8);

#[derive(Debug)]
pub struct ErrorCode(u16);

pub trait InactiveLeAclManager {
    type ActiveManager: LeAclManager + 'static;

    fn register_callbacks(self, callbacks: impl LeAclManagerConnectionCallbacks) -> Self::ActiveManager;
}

pub trait LeAclManager: Debug {
    // Precondition: Must NOT be currently connected to this adddress (if connected due to race, is a no-op)
    fn add_to_direct_list(&self, address: AddressWithType); // CreateLeConnection(is_direct=true)
    // Precondition: Must NOT be currently connected to this adddress (if connected, will add to list, but do nothing)
    fn add_to_background_list(&self, address: AddressWithType); // CreateLeConnection(is_direct=false)
    // Precondition: Must be CONNECTED to this address (else, UNDEFINED behavior, since we don't pull it out of the accept list)
    // Since it is possible to get a disconnection while enqueuing this, this method is UNSAFE!
    fn remove_device_from_background_list(&self, address: AddressWithType); // RemoveFromBackgroundList
    // Precondition: Must NOT be currently connected to this adddress (if connected, removes from background list only)
    // Due to races, it is possible to call this, and THEN get a connection complete with us as central
    fn remove_from_all_lists(&self, address: AddressWithType); // CancelLeConnect
}

pub trait LeAclManagerConnectionCallbacks {
    fn on_le_connect_success(&self, address: AddressWithType);
    fn on_le_connect_fail(&self, address: AddressWithType, status: ErrorCode);
    fn on_disconnect(&self, address: AddressWithType);
}
