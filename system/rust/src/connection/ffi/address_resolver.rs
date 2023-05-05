//! FFI interfaces for address resolution.

use std::fmt::Debug;

use async_trait::async_trait;

use cxx::SharedPtr;
pub use inner::*;

use tokio::sync::oneshot;

use crate::core::{address::AddressWithType, invoke_callback, Callback};

use super::super::le_manager::{AddressResolver, CanonicalAddress};

unsafe impl Send for AddressResolverShim {}
unsafe impl Sync for AddressResolverShim {}

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    impl SharedPtr<AddressResolverShim> {}

    #[namespace = "bluetooth::core"]
    extern "C++" {
        type AddressWithType = crate::core::address::AddressWithType;
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        include!("src/connection/ffi/connection_shim.h");

        /// This lets us resolve RPAs to an identity address, using the security database
        type AddressResolverShim;

        /// Resolve an address into "canonical form", that can be passed to the create/cancel
        /// callbacks. The exact means of resolution is implementation-defined (i.e. it could
        /// be the identity address, or the pseudo-address, or anything else)
        ///
        /// # Safety
        /// `on_resolved` must be Send, since we use it from a C++ thread.
        #[cxx_name = "ResolveAddress"]
        unsafe fn unchecked_resolve_address(
            &self,
            address: AddressWithType,
            on_resolved: Box<ResolveAddressCallback>,
        );
    }

    #[namespace = "bluetooth::connection"]
    extern "Rust" {
        type ResolveAddressCallback;

        #[cxx_name = "Invoke"]
        fn invoke_callback(callback: Box<ResolveAddressCallback>, address: AddressWithType);
    }
}

type ResolveAddressCallback = Callback<AddressWithType>;

/// Implementation of AddressResolver wrapping the corresponding C++ methods
#[derive(Clone)]
pub struct AddressResolverImpl(pub SharedPtr<AddressResolverShim>);

#[async_trait(?Send)]
impl AddressResolver for AddressResolverImpl {
    async fn resolve_address(&self, address: AddressWithType) -> CanonicalAddress {
        let (tx, rx) = oneshot::channel();
        // SAFETY: Since Callback<T> is always Send, this is safe.
        let callback = Box::new(tx.into());
        {
            fn check(_: &impl Send) {}
            check(&callback);
        }
        unsafe {
            self.0.unchecked_resolve_address(address, callback);
        }
        CanonicalAddress::new(rx.await.unwrap())
    }
}

impl Debug for AddressResolverImpl {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_tuple("AddressResolverImpl").finish()
    }
}
