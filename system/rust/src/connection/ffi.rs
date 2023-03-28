//! FFI interfaces for the Connection module.

use std::fmt::Debug;

use cxx::UniquePtr;
pub use inner::*;
use log::warn;

use crate::{core::address::AddressWithType, do_in_rust_thread};

use super::{
    hci_operations::{ErrorCode, HciConnectProxy, HciEvent},
    AddressManager, Connection, ConnectionManagerClient, Role,
};

unsafe impl Send for LeConnectHciManagerShim {}
unsafe impl Send for LeAddressManagerShim {}

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    impl UniquePtr<LeConnectHciManagerShim> {}
    impl UniquePtr<LeAddressManagerShim> {}

    #[namespace = "bluetooth::core"]
    extern "C++" {
        type AddressWithTypeForFFI = crate::core::AddressWithTypeForFFI;
    }

    /// The role of a device in an LE connection
    #[derive(Debug)]
    #[namespace = "bluetooth::connection"]
    enum RoleForFFI {
        /// We initiated the connection
        #[cxx_name = "CENTRAL"]
        Central = 0u32,
        /// The other device initiated the connection
        #[cxx_name = "PERIPHERAL"]
        Peripheral = 1u32,
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        /// This lets us send HCI commands, either directly,
        /// or via the address manager
        type LeConnectHciManagerShim;

        /// Send the HCI command LE (Extended) Create Connection
        #[cxx_name = "LeCreateConnection"]
        fn le_create_connection(self: &LeConnectHciManagerShim, use_fast_parameters: bool);

        /// Send the HCI command LE Cancel Connection
        #[cxx_name = "LeCancelConnection"]
        fn le_cancel_connection(self: &LeConnectHciManagerShim);

        /// Add the specified address to the filter accept list
        /// using the address manager
        #[cxx_name = "AddToFilterAcceptList"]
        fn add_to_filter_accept_list(
            self: &LeConnectHciManagerShim,
            address: AddressWithTypeForFFI,
        );

        /// Remove the specified address from the filter accept list
        /// using the address manager
        #[cxx_name = "RemoveFromFilterAcceptList"]
        fn remove_from_filter_accept_list(
            self: &LeConnectHciManagerShim,
            address: AddressWithTypeForFFI,
        );

        /// Register event listeners required by Rust
        fn StoreHciCallbacksFromRust(
            // Events
            on_create_connection_status: fn(status: u8),
            on_connection_complete: fn(
                address: AddressWithTypeForFFI,
                role: RoleForFFI,
                status: u8,
            ),
            on_disconnect: fn(address: AddressWithTypeForFFI),
        );

        type LeAddressManagerShim;

        #[cxx_name = "AckPause"]
        fn ack_pause(self: &LeAddressManagerShim);

        #[cxx_name = "AckResume"]
        fn ack_resume(self: &LeAddressManagerShim);

        /// Register event listeners required by Rust
        fn RegisterWithAddressManager(pause: fn(), resume: fn());
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        include!("stack/arbiter/acl_arbiter.h");
        type InterceptAction;

        /// Register APIs exposed by Rust
        fn RegisterRustApis(
            // APIs
            start_direct_connection: fn(client_id: u8, address: AddressWithTypeForFFI),
            stop_direct_connection: fn(client_id: u8, address: AddressWithTypeForFFI),
            add_background_connection: fn(client_id: u8, address: AddressWithTypeForFFI),
            remove_background_connection: fn(client_id: u8, address: AddressWithTypeForFFI),
            stop_all_connections_from_client: fn(client_id: u8),
            stop_all_connections_to_device: fn(address: AddressWithTypeForFFI),
        );
    }
}

/// Implementation of HciConnectProxy wrapping the corresponding C++ methods
pub struct HciConnectProxyImpl(pub UniquePtr<LeConnectHciManagerShim>);

impl HciConnectProxy for HciConnectProxyImpl {
    fn create_connect(&self, is_direct: bool) {
        self.0.le_create_connection(is_direct);
    }

    fn cancel_connect(&self) {
        self.0.le_cancel_connection();
    }

    fn add_to_accept_list(&self, address: AddressWithType) {
        self.0.add_to_filter_accept_list(address.into())
    }

    fn remove_from_accept_list(&self, address: AddressWithType) {
        self.0.remove_from_filter_accept_list(address.into());
    }

    fn disconnect(&self, conn: Connection) {
        todo!()
    }
}

impl Debug for HciConnectProxyImpl {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_tuple("HciConnectProxyImpl").finish()
    }
}

/// Implementation of LeAddressManagerShim wrapping the corresponding C++ methods
pub struct AddressManagerImpl(pub UniquePtr<LeAddressManagerShim>);

impl AddressManager for AddressManagerImpl {
    fn ack_pause(&self) {
        self.0.ack_pause()
    }

    fn ack_resume(&self) {
        self.0.ack_resume()
    }
}

impl Debug for AddressManagerImpl {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_tuple("AddressManagerImpl").finish()
    }
}

pub fn register_callbacks() {
    StoreHciCallbacksFromRust(
        |status| {
            do_in_rust_thread(move |modules| {
                modules
                    .connection_manager
                    .on_hci_event(HciEvent::CreateConnectionStatus(ErrorCode(status)))
            })
        },
        |address, role, status| {
            do_in_rust_thread(move |modules| {
                modules.connection_manager.on_hci_event(HciEvent::CreateConnectionComplete(
                    address.try_into().expect("received invalid AddressWithType"),
                    match role {
                        RoleForFFI::Central => Role::Central,
                        RoleForFFI::Peripheral => Role::Peripheral,
                        _ => unreachable!("received invalid role {role:?}"),
                    },
                    ErrorCode(status),
                ))
            })
        },
        |address| todo!(),
    );
    RegisterWithAddressManager(
        || do_in_rust_thread(|modules| modules.connection_manager.pause()),
        || do_in_rust_thread(|modules| modules.connection_manager.resume()),
    );
    RegisterRustApis(
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            let address = address.try_into().expect("invalid address");
            do_in_rust_thread(move |modules| {
                let result = modules.connection_manager.start_direct_connection(
                    client,
                    address,
                );
                if let Err(err) = result {
                    warn!("Failed to start direct connection from {client:?} to {address:?} ({err:?})")
                }
            });
        },
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            let address = address.try_into().expect("invalid address");
            do_in_rust_thread(move |modules| {
                let result = modules.connection_manager.cancel_direct_connection(
                    client,
                    address,
                );
                if let Err(err) = result {
                    warn!("Failed to cancel direct connection from {client:?} to {address:?} ({err:?})")
                }
            })
        },
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            let address = address.try_into().expect("invalid address");
            do_in_rust_thread(move |modules| {
                let result = modules.connection_manager.add_background_connection(
                    client,
                    address,
                );
                if let Err(err) = result {
                    warn!("Failed to add background connection from {client:?} to {address:?} ({err:?})")
                }
            })
        },
        |client, address| {
            let client = ConnectionManagerClient::GattClient(client);
            let address = address.try_into().expect("invalid address");
            do_in_rust_thread(move |modules| {
                let result = modules.connection_manager.remove_background_connection(
                    client,
                    address,
                );
                if let Err(err) = result {
                    warn!("Failed to remove background connection from {client:?} to {address:?} ({err:?})")
                }
            })
        },
        |client| {
            todo!()
        },
        |address| {
            todo!()
        },
    )
}
