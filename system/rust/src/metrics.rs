//! This module manages stateful (as opposed to event-based) metrics throughout the stack.
//! It can be accessed either from native directly, or from Java via the MetricsNativeInterface.

use std::rc::Rc;

use self::{atoms::AtomLogger, rfcomm::RfcommTracker, device::Devices};

mod ffi;
mod rfcomm;
mod device;
pub mod atoms;

pub struct Metrics {
  pub devices: Rc<Devices>,
  pub rfcomm: RfcommTracker,
}

impl Metrics {
  /// Constructor
  pub fn new(atoms_logger: Box<dyn AtomLogger>) -> Self {
    let devices = Rc::new(Devices::new());
    Self {
      rfcomm: RfcommTracker::new(atoms_logger.into(), devices.clone()),
      devices,
    }
  }
}
