//! FFI interfaces for the GATT module. Some structs are exported so that
//! core::init can instantiate and pass them into the main loop.

pub use inner::*;
use log::{error, info, warn};

use crate::{
    do_in_rust_thread,
    packets::{AttBuilder, Serializable, SerializeError},
};

use super::{
    channel::AttTransport,
    ids::{AttHandle, ServerId, TransportIndex},
    server::gatt_database::{AttPermissions, GattCharacteristicWithHandle, GattServiceWithHandle},
};

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    #[namespace = "bluetooth"]
    extern "C++" {
        include!("bluetooth/uuid.h");
        /// A C++ UUid.
        type Uuid = crate::core::Uuid;
    }

    /// The type of GATT record supplied over FFI
    #[derive(Debug)]
    #[namespace = "bluetooth::gatt"]
    enum GattRecordType {
        PrimaryService,
        SecondaryService,
        IncludedService,
        Characteristic,
        Descriptor,
    }

    /// An entry in a service definition received from JNI. See GattRecordType for possible types.
    #[namespace = "bluetooth::gatt"]
    struct GattRecord<'a> {
        uuid: &'a Uuid,
        record_type: GattRecordType,
        attribute_handle: u16,

        properties: u8,
        extended_properties: u16,

        permissions: u16,
    }

    #[namespace = "bluetooth::shim::arbiter"]
    unsafe extern "C++" {
        include!("stack/arbiter/acl_arbiter.h");
        /// Send an outgoing packet on the specified tcb_idx
        fn SendPacketToPeer(tcb_idx: u8, packet: Vec<u8>);
    }

    #[namespace = "bluetooth::gatt"]
    extern "Rust" {
        // service management
        fn open_server(server_id: u8);
        fn close_server(server_id: u8);
        unsafe fn add_service(server_id: u8, service_records: Vec<GattRecord>);
        fn remove_service(server_id: u8, service_handle: u16);
    }
}

/// Implementation of AttTransport wrapping the corresponding C++ method
pub struct AttTransportImpl();

impl AttTransport for AttTransportImpl {
    fn send_packet(
        &self,
        tcb_idx: TransportIndex,
        packet: AttBuilder,
    ) -> Result<(), SerializeError> {
        SendPacketToPeer(tcb_idx.0, packet.to_vec()?);
        Ok(())
    }
}

fn open_server(server_id: u8) {
    let server_id = ServerId(server_id);

    do_in_rust_thread(move |modules| {
        modules.gatt_module.open_gatt_server(server_id);
    })
}

fn close_server(server_id: u8) {
    let server_id = ServerId(server_id);

    do_in_rust_thread(move |modules| {
        modules.gatt_module.close_gatt_server(server_id);
    })
}

fn add_service(server_id: u8, service_records: Vec<GattRecord<'_>>) {
    // marshal into the form expected by GattModule
    let server_id = ServerId(server_id);
    let mut characteristics = vec![];
    let mut service_handle_uuid = None;

    for record in service_records {
        match record.record_type {
            GattRecordType::PrimaryService => {
                service_handle_uuid = Some((record.attribute_handle, record.uuid));
            }
            GattRecordType::Characteristic => characteristics.push(GattCharacteristicWithHandle {
                handle: AttHandle(record.attribute_handle),
                uuid: record.uuid.into(),
                permissions: AttPermissions {
                    readable: record.properties & 0x02 != 0,
                    writable: record.properties & 0x08 != 0,
                },
            }),
            _ => {
                warn!("ignoring unsupported database entry of type {:?}", record.record_type)
            }
        }
    }

    if let Some((handle, uuid)) = service_handle_uuid {
        let service =
            GattServiceWithHandle { handle: AttHandle(handle), uuid: uuid.into(), characteristics };
        do_in_rust_thread(move |modules| {
            let ok = modules.gatt_module.register_gatt_service(server_id, service.clone());
            match ok {
                Ok(_) => info!(
                    "successfully registered service for server {server_id:?} with handle {handle} (service={service:?})"
                ),
                Err(err) => error!(
                    "failed to register GATT service for server {server_id:?} with error: {err},  (service={service:?})"
                ),
            }
        });
    } else {
        error!("got service registration but with no primary service! {characteristics:?}");
    }
}

fn remove_service(server_id: u8, service_handle: u16) {
    let server_id = ServerId(server_id);
    let service_handle = AttHandle(service_handle);
    do_in_rust_thread(move |modules| {
        let ok = modules.gatt_module.unregister_gatt_service(server_id, service_handle);
        match ok {
            Ok(_) => info!(
                "successfully removed service {service_handle:?} for server {server_id:?}"
            ),
            Err(err) => error!(
                "failed to remove GATT service {service_handle:?} for server {server_id:?} with error: {err}"
            ),
        }
    })
}
