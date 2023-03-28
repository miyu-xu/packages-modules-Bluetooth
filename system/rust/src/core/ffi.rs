use crate::core::init;

use cxx::{type_id, ExternType};
pub use inner::*;

unsafe impl Send for GattServerCallbacks {}

unsafe impl ExternType for Uuid {
    type Id = type_id!("bluetooth::Uuid");
    type Kind = cxx::kind::Trivial;
}

#[allow(dead_code, missing_docs)]
#[cxx::bridge]
mod inner {
    #[derive(Debug)]
    pub enum AddressTypeForFFI {
        Public,
        Random,
    }

    #[namespace = "bluetooth::core"]
    #[derive(Debug)]
    pub struct AddressWithTypeForFFI {
        /// Stored in little-endian format
        pub address: [u8; 16],
        pub address_type: AddressTypeForFFI,
    }

    #[namespace = "bluetooth"]
    extern "C++" {
        include!("bluetooth/uuid.h");
        type Uuid = crate::core::uuid::Uuid;
    }

    #[namespace = "bluetooth::gatt"]
    unsafe extern "C++" {
        include!("src/gatt/ffi/gatt_shim.h");
        type GattServerCallbacks = crate::gatt::GattServerCallbacks;
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        type LeConnectHciManagerShim = crate::connection::LeConnectHciManagerShim;
        type LeAddressManagerShim = crate::connection::LeAddressManagerShim;
    }

    #[namespace = "bluetooth::rust_shim"]
    extern "Rust" {
        fn init(
            hci_connect_proxy: UniquePtr<LeConnectHciManagerShim>,
            address_manager: UniquePtr<LeAddressManagerShim>,
            gatt_server_callbacks: UniquePtr<GattServerCallbacks>,
        );
    }
}
