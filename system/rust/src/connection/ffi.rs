//! FFI interfaces for the Connection module.

use std::{fmt::Debug, pin::Pin};

use async_trait::async_trait;
use cxx::{SharedPtr, UniquePtr};
pub use inner::*;
use log::warn;
use tokio::{
    sync::{
        mpsc::{unbounded_channel, UnboundedSender},
        oneshot,
    },
    task::spawn_local,
};

use crate::{
    core::{address::AddressWithType, invoke_callback, Callback},
    do_in_rust_thread,
};

use super::{
    attempt_manager::ConnectionMode,
    le_manager::{
        AddressResolver, CanonicalAddress, ErrorCode, InactiveLeAclManager, LeAclManager,
        LeAclManagerConnectionCallbacks,
    },
    ConnectionManagerClient, LeConnection,
};

unsafe impl Send for LeAclManagerShim {}
unsafe impl Send for AddressResolverShim {}
unsafe impl Sync for AddressResolverShim {}

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    impl UniquePtr<LeAclManagerShim> {}
    impl SharedPtr<AddressResolverShim> {}

    #[namespace = "bluetooth::core"]
    extern "C++" {
        type AddressWithType = crate::core::address::AddressWithType;
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        include!("src/connection/ffi/connection_shim.h");

        /// This lets us send HCI commands, either directly,
        /// or via the address manager
        type LeAclManagerShim;

        /// Add address to direct/background connect list, if not already connected
        /// If connected, then adding to direct list is a no-op, but adding to the
        /// background list will still take place.
        #[cxx_name = "CreateLeConnection"]
        fn create_le_connection(&self, address: AddressWithType, is_direct: bool);

        /// Remove address from both direct + background connect lists
        #[cxx_name = "CancelLeConnect"]
        fn cancel_le_connect(&self, address: AddressWithType);

        /// Register Rust callbacks for connection events
        ///
        /// # Safety
        /// `callbacks` must be Send + Sync, since C++ moves it to a different thread and
        /// invokes it from several others (GD + legacy threads).
        #[cxx_name = "RegisterRustCallbacks"]
        unsafe fn unchecked_register_rust_callbacks(
            self: Pin<&mut Self>,
            callbacks: Box<LeAclManagerCallbackShim>,
        );
    }

    #[namespace = "bluetooth::connection"]
    extern "Rust" {
        type ResolveAddressCallback;

        #[cxx_name = "Invoke"]
        fn invoke_callback(callback: Box<ResolveAddressCallback>, address: AddressWithType);
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
        type LeAclManagerCallbackShim;
        #[cxx_name = "OnLeConnectSuccess"]
        fn on_le_connect_success(&self, address: AddressWithType);
        #[cxx_name = "OnLeConnectFail"]
        fn on_le_connect_fail(&self, address: AddressWithType, status: u8);
        #[cxx_name = "OnLeDisconnection"]
        fn on_disconnect(&self, address: AddressWithType);
        #[cxx_name = "OnResolvingListChange"]
        fn on_resolving_list_change(&self);
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        include!("stack/arbiter/acl_arbiter.h");

        /// Register APIs exposed by Rust
        fn RegisterRustApis(
            start_direct_connection: fn(client_id: u8, address: AddressWithType),
            stop_direct_connection: fn(client_id: u8, address: AddressWithType),
            add_background_connection: fn(client_id: u8, address: AddressWithType),
            remove_background_connection: fn(client_id: u8, address: AddressWithType),
            remove_client: fn(client_id: u8),
            stop_all_connections_to_device: fn(address: AddressWithType),
        );
    }
}

impl LeAclManagerShim {
    fn register_rust_callbacks(
        self: Pin<&mut LeAclManagerShim>,
        callbacks: Box<LeAclManagerCallbackShim>,
    ) where
        Box<LeAclManagerCallbackShim>: Send + Sync,
    {
        // SAFETY: The requirements of this method are enforced
        // by our own trait bounds.
        unsafe {
            self.unchecked_register_rust_callbacks(callbacks);
        }
    }
}

/// Implementation of HciConnectProxy wrapping the corresponding C++ methods
pub struct LeAclManagerImpl(pub UniquePtr<LeAclManagerShim>);

pub struct LeAclManagerCallbackShim(
    UnboundedSender<Box<dyn FnOnce(&dyn LeAclManagerConnectionCallbacks) + Send>>,
);

impl LeAclManagerCallbackShim {
    fn on_le_connect_success(&self, address: AddressWithType) {
        let _ = self.0.send(Box::new(move |callback| {
            callback.on_le_connect(address, Ok(LeConnection { remote_address: address }))
        }));
    }

    fn on_le_connect_fail(&self, address: AddressWithType, status: u8) {
        let _ = self.0.send(Box::new(move |callback| {
            callback.on_le_connect(address, Err(ErrorCode(status)))
        }));
    }

    fn on_disconnect(&self, address: AddressWithType) {
        let _ = self.0.send(Box::new(move |callback| {
            callback.on_disconnect(address);
        }));
    }

    fn on_resolving_list_change(&self) {
        let _ = self.0.send(Box::new(move |callback| {
            callback.on_resolving_list_change();
        }));
    }
}

impl InactiveLeAclManager for LeAclManagerImpl {
    type ActiveManager = Self;

    fn register_callbacks(
        mut self,
        callbacks: impl LeAclManagerConnectionCallbacks + 'static,
    ) -> Self::ActiveManager {
        let (tx, mut rx) = unbounded_channel();

        self.0.pin_mut().register_rust_callbacks(Box::new(LeAclManagerCallbackShim(tx)));

        spawn_local(async move {
            while let Some(f) = rx.recv().await {
                f(&callbacks)
            }
        });
        self
    }
}

type ResolveAddressCallback = Callback<AddressWithType>;

impl LeAclManager for LeAclManagerImpl {
    fn add_to_direct_list(&self, address: AddressWithType) {
        self.0.create_le_connection(address, /* is_direct= */ true)
    }

    fn add_to_background_list(&self, address: AddressWithType) {
        self.0.create_le_connection(address, /* is_direct= */ false)
    }

    fn remove_from_all_lists(&self, address: AddressWithType) {
        self.0.cancel_le_connect(address)
    }
}

impl Debug for LeAclManagerImpl {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_tuple("LeAclManagerImpl").finish()
    }
}

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

/// Registers all connection-manager callbacks into C++ dependencies
pub fn register_callbacks() {
    RegisterRustApis(
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            do_in_rust_thread(move |modules| {
                let connection_manager = modules.connection_manager.clone();
                spawn_local(async move {
                    let result = connection_manager.start_direct_connection(client, address).await;
                    if let Err(err) = result {
                        warn!("Failed to start direct connection from {client:?} to {address:?} ({err:?})")
                    }
                });
            });
        },
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            do_in_rust_thread(move |modules| {
                let connection_manager = modules.connection_manager.clone();
                spawn_local(async move {
                    let result = connection_manager
                        .cancel_connection(client, address, ConnectionMode::Direct)
                        .await;
                    if let Err(err) = result {
                        warn!("Failed to cancel direct connection from {client:?} to {address:?} ({err:?})")
                    }
                });
            })
        },
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            do_in_rust_thread(move |modules| {
                let connection_manager = modules.connection_manager.clone();
                spawn_local(async move {
                    let result =
                        connection_manager.add_background_connection(client, address).await;
                    if let Err(err) = result {
                        warn!("Failed to add background connection from {client:?} to {address:?} ({err:?})")
                    }
                });
            })
        },
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            do_in_rust_thread(move |modules| {
                let connection_manager = modules.connection_manager.clone();
                spawn_local(async move {
                    let result = connection_manager
                        .cancel_connection(client, address, ConnectionMode::Background)
                        .await;
                    if let Err(err) = result {
                        warn!("Failed to remove background connection from {client:?} to {address:?} ({err:?})")
                    }
                });
            })
        },
        |client| {
            let client = ConnectionManagerClient::GattClient(client);
            do_in_rust_thread(move |modules| {
                let connection_manager = modules.connection_manager.clone();
                spawn_local(async move {
                    connection_manager.remove_client(client).await;
                });
            })
        },
        |address| {
            do_in_rust_thread(move |modules| {
                let connection_manager = modules.connection_manager.clone();
                spawn_local(async move {
                    connection_manager.cancel_unconditionally(address).await;
                });
            })
        },
    )
}
