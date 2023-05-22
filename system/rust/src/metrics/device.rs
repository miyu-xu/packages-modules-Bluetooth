//! This module tracks and retrieves metadata about devices, used for the purposes of metrics.
//! Data from here should NOT be used for any functional decisions made by the stack, the
//! security database should be used instead.

use crate::core::address::RawAddress;

use super::atoms::{PeerDeviceMetadata, LocalDeviceMetadata};

#[derive(Default)]
pub struct Devices {

}

impl Devices {
  pub fn new() -> Self {
    todo!()
  }

  pub fn get_local_device(&self) -> LocalDeviceMetadata {
    todo!()
  }

  pub fn get_peer_device(&self, _addr: RawAddress) -> PeerDeviceMetadata {
    todo!()
  }
}
