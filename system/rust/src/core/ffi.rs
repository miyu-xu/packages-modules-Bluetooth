use crate::core::init;

use cxx::{type_id, ExternType};
pub use inner::*;

unsafe impl Send for GattServerCallbacks {}

unsafe impl ExternType for Uuid {
    type Id = type_id!("bluetooth::Uuid");
    type Kind = cxx::kind::Trivial;
}

unsafe impl ExternType for AddressWithType {
    type Id = type_id!("bluetooth::core::AddressWithType");
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
    extern "C++" {
        include!("src/core/ffi/types.h");
        type AddressWithType = crate::core::address::AddressWithType;
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
        include!("src/connection/ffi/connection_shim.h");
        type LeAclManagerShim = crate::connection::LeAclManagerShim;
    }

    #[namespace = "bluetooth::rust_shim"]
    extern "Rust" {
        fn init(
            gatt_server_callbacks: UniquePtr<GattServerCallbacks>,
            le_acl_manager: UniquePtr<LeAclManagerShim>,
        );
    }
}
