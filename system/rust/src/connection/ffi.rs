//! FFI interfaces for the Connection module.

pub mod address_resolver;
pub mod le_manager;
pub mod le_scanner;

pub use inner::*;
use log::warn;
use tokio::task::spawn_local;

use crate::do_in_rust_thread;

use super::{attempt_manager::ConnectionMode, ConnectionManagerClient};

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    #[namespace = "bluetooth::core"]
    extern "C++" {
        type AddressWithType = crate::core::address::AddressWithType;
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
