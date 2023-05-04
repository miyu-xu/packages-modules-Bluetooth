//! This trait represents the API of the LeScanningManager. It lets us
//! register for scan results and enable/disable APCF scan filters.
//! The upper layer guarantees that a scan will always be active, so we
//! do not need to manage scan parameters here.

use std::fmt::Debug;

use crate::core::address::AddressWithType;

/// The callbacks invoked by the LeScanningManager in response to scan results
pub trait LeScanningManagerCallbacks {
    /// Invoked when a device is seen advertising using targeted announcements
    fn on_targeted_announcement_scan_result(&self, address: AddressWithType);
}

/// The operations provided by GD LeScanningManager to the connection manager
pub trait LeScanner: LeScannerFilterControls {
    /// Register callbacks for scan results, or return an error if not possible
    fn register_callbacks(
        &mut self,
        callbacks: impl LeScanningManagerCallbacks + 'static,
    ) -> Result<(), ()>;
}

/// Scan filter interface for LE scanner. In a separate trait so it can be made object-safe.
pub trait LeScannerFilterControls: Debug {
  /// Enable / disable the APCF filter for targeted announcements. Idempotent.
  fn set_targeted_announcement_filter_enabled(&mut self, enable: bool);
}
