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

#[derive(Eq, PartialEq, Hash)]
pub struct AddressWithType(u8);

pub struct ErrorCode(u16);

pub trait LeAclManager {
    fn add_to_direct_list(&self, address: AddressWithType); // CreateLeConnection(is_direct=true)
    fn add_to_background_list(&self, address: AddressWithType); // CreateLeConnection(is_direct=false)
    fn remove_from_all_lists(&self, address: AddressWithType); // CancelLeConnect
}

pub trait LeAclManagerConnectionCallbacks {
    fn on_le_connect_success(address: AddressWithType);
    fn on_le_connect_fail(address: AddressWithType, status: ErrorCode);
}
