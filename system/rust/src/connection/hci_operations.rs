use std::fmt::Debug;

use crate::core::address::AddressWithType;

use super::{Connection, Role};

#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub struct ErrorCode(pub u8);

impl ErrorCode {
    pub const SUCCESS: Self = ErrorCode(0);
}

pub trait HciConnectProxy: Debug {
    fn create_connect(&self, is_direct: bool);
    fn cancel_connect(&self);
    fn add_to_accept_list(&self, address: AddressWithType);
    fn remove_from_accept_list(&self, address: AddressWithType);
    fn disconnect(&self, conn: Connection);
}

pub enum HciEvent {
    CreateConnectionStatus(ErrorCode),
    CreateConnectionComplete(AddressWithType, Role, ErrorCode),
}
