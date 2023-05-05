//! FFI interfaces for LE scanning

use std::fmt::Debug;

use bt_common::init_flags;
use cxx::UniquePtr;
pub use inner::*;

use tokio::{
    sync::mpsc::{unbounded_channel, UnboundedSender},
    task::spawn_local,
};

use crate::core::address::AddressWithType;

use super::super::le_scanner::{LeScanner, LeScannerCallbacks, LeScannerFilterControls};

unsafe impl Send for LeScannerShim {}

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    impl UniquePtr<LeScannerShim> {}

    #[namespace = "bluetooth::core"]
    extern "C++" {
        type AddressWithType = crate::core::address::AddressWithType;
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        include!("src/connection/ffi/connection_shim.h");

        /// This lets us register for scan results, and configure scan filters
        type LeScannerShim;

        /// Register Rust callbacks for scan results
        ///
        /// # Safety
        /// `callbacks` must be Send + Sync, since C++ moves it to a different thread and
        /// invokes it from several others (GD + legacy threads).
        #[cxx_name = "RegisterRustCallbacks"]
        unsafe fn unchecked_register_rust_callbacks(
            self: Pin<&mut Self>,
            callbacks: Box<LeScannerCallbacksShim>,
        );

        /// Enable / disable the APCF filter for targeted announcements.
        /// Sends HCI commands whenever invoked, so do so only if needed.
        #[cxx_name = "SetTargetedAnnouncementFilterEnabled"]
        fn set_targeted_announcement_filter_enabled(&self, enable: bool);
    }

    #[namespace = "bluetooth::connection"]
    extern "Rust" {
        type LeScannerCallbacksShim;
        #[cxx_name = "OnTargetedAnnouncementScanResult"]
        fn on_targeted_announcement_scan_result(&self, address: AddressWithType);
    }
}

/// Implementation of LeScanner wrapping the corresponding C++ methods
pub struct LeScannerImpl {
    shim: UniquePtr<LeScannerShim>,
    registered: bool,
    targeted_announcements_filter_enabled: bool,
}

impl LeScannerImpl {
    pub fn new(shim: UniquePtr<LeScannerShim>) -> Self {
        Self { shim, registered: false, targeted_announcements_filter_enabled: false }
    }
}

impl LeScanner for LeScannerImpl {
    fn register_callbacks(&mut self, callbacks: impl LeScannerCallbacks + 'static) -> Result<(), ()>
    where
        Box<LeScannerCallbacksShim>: Send + Sync,
    {
        if self.registered {
            return Err(());
        }

        let (tx, mut rx) = unbounded_channel();

        // only register callbacks if the feature is enabled
        if init_flags::use_unified_connection_manager_is_enabled() {
            unsafe {
                self.shim
                    .pin_mut()
                    .unchecked_register_rust_callbacks(Box::new(LeScannerCallbacksShim(tx)));
            }
        }

        spawn_local(async move {
            while let Some(f) = rx.recv().await {
                f(&callbacks)
            }
        });

        Ok(())
    }
}

impl LeScannerFilterControls for LeScannerImpl {
    fn set_targeted_announcement_filter_enabled(&mut self, enable: bool) {
        if enable != self.targeted_announcements_filter_enabled {
            self.shim.set_targeted_announcement_filter_enabled(enable);
        }
        self.targeted_announcements_filter_enabled = enable;
    }
}

impl Debug for LeScannerImpl {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("LeScannerImpl").field("registered", &self.registered).finish()
    }
}

pub struct LeScannerCallbacksShim(UnboundedSender<Box<dyn FnOnce(&dyn LeScannerCallbacks) + Send>>);

impl LeScannerCallbacksShim {
    fn on_targeted_announcement_scan_result(&self, address: AddressWithType) {
        let _ = self.0.send(Box::new(move |callback| {
            callback.on_targeted_announcement_scan_result(address);
        }));
    }
}
