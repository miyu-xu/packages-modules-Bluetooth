//! FFI interfaces for the GATT module. Some structs are exported so that
//! core::init can instantiate and pass them into the main loop.

use cxx::UniquePtr;
pub use inner::*;
use log::{error, info, warn};

use crate::{
    do_in_rust_thread,
    packets::{
        AttAttributeDataChild, AttAttributeDataView, AttBuilder, AttErrorCode, Serializable,
        SerializeError,
    },
};

use super::{
    arbiter::{self, with_arbiter},
    channel::AttTransport,
    ids::{AdvertiserId, AttHandle, ConnectionId, ServerId, TransactionId, TransportIndex},
    server::gatt_database::{AttPermissions, GattCharacteristicWithHandle, GattServiceWithHandle},
    GattCallbacks,
};

#[cxx::bridge]
#[allow(clippy::needless_lifetimes)]
#[allow(clippy::too_many_arguments)]
#[allow(missing_docs)]
mod inner {
    impl UniquePtr<GattServerCallbacks> {}

    #[namespace = "bluetooth"]
    extern "C++" {
        /// A C++ UUid.
        type Uuid = crate::core::Uuid;
    }

    #[namespace = "bluetooth::gatt"]
    unsafe extern "C++" {
        include!("src/gatt/ffi/gatt_shim.h");

        /// This contains the callbacks from Rust into C++ JNI needed for GATT
        type GattServerCallbacks;

        /// This callback is invoked when reading a characteristic - the client must reply using SendReponse
        #[cxx_name = "OnServerReadCharacteristic"]
        fn on_server_read_characteristic(
            self: &GattServerCallbacks,
            conn_id: u16,
            trans_id: u32,
            attr_handle: u16,
            offset: u32,
            is_long: bool,
        );

        /// This callback is invoked when writing a characteristic - the client must reply using SendReponse
        #[cxx_name = "OnServerWriteCharacteristic"]
        fn on_server_write_characteristic(
            self: &GattServerCallbacks,
            conn_id: u16,
            trans_id: u32,
            attr_handle: u16,
            offset: u32,
            need_response: bool,
            is_prepare: bool,
            value: &[u8],
        );
    }

    /// What action the arbiter should take in response to an incoming packet
    #[namespace = "bluetooth::shim::arbiter"]
    enum InterceptAction {
        /// Forward the packet to the legacy stack
        #[cxx_name = "FORWARD"]
        Forward = 0u32,
        /// Discard the packet (typically because it has been intercepted)
        #[cxx_name = "DROP"]
        Drop = 1u32,
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
        type InterceptAction;

        /// Register callbacks from C++ into Rust within the Arbiter
        fn StoreCallbacksFromRust(
            on_le_connect: fn(tcb_idx: u8, advertiser: u8),
            on_le_disconnect: fn(tcb_idx: u8),
            intercept_packet: fn(tcb_idx: u8, packet: Vec<u8>) -> InterceptAction,
        );

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
        fn send_response(server_id: u8, conn_id: u16, trans_id: u32, status: u32, value: &[u8]);

        // connection
        fn is_connection_isolated(conn_id: u16) -> bool;

        // arbitration
        fn associate_server_with_advertiser(server_id: u8, advertiser_id: u8);
        fn clear_advertiser(advertiser_id: u8);
    }
}

/// Implementation of GattCallbacks wrapping the corresponding C++ methods
pub struct GattCallbacksImpl(pub UniquePtr<GattServerCallbacks>);

impl GattCallbacks for GattCallbacksImpl {
    fn on_server_read_characteristic(
        &self,
        conn_id: ConnectionId,
        trans_id: TransactionId,
        handle: AttHandle,
        offset: u32,
        is_long: bool,
    ) {
        self.0
            .as_ref()
            .unwrap()
            .on_server_read_characteristic(conn_id.0, trans_id.0, handle.0, offset, is_long);
    }

    fn on_server_write_characteristic(
        &self,
        conn_id: ConnectionId,
        trans_id: TransactionId,
        handle: AttHandle,
        offset: u32,
        need_response: bool,
        is_prepare: bool,
        value: AttAttributeDataView,
    ) {
        self.0.as_ref().unwrap().on_server_write_characteristic(
            conn_id.0,
            trans_id.0,
            handle.0,
            offset,
            need_response,
            is_prepare,
            &value.get_raw_payload().collect::<Vec<_>>(),
        );
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

    // DO NOT SUBMIT
    with_arbiter(|arbiter| arbiter.associate_server_with_advertiser(server_id, AdvertiserId(0)));

    do_in_rust_thread(move |modules| {
        modules.gatt_module.open_gatt_server(server_id);
    })
}

fn close_server(server_id: u8) {
    let server_id = ServerId(server_id);

    // DO NOT SUBMIT
    // arbiter::with_arbiter(move |arbiter| arbiter.clear_server(server_id));

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
    error!("ignoring service deregistration by {server_id:?} at handle {service_handle:?}")
}

fn is_connection_isolated(conn_id: u16) -> bool {
    with_arbiter(|arbiter| arbiter.is_connection_isolated(ConnectionId(conn_id)))
}

fn send_response(_server_id: u8, conn_id: u16, trans_id: u32, status: u32, value: &[u8]) {
    // TODO: fixup error codes to allow app-specific values (i.e. don't make it an enum in PDL)
    let value = if status == 0 {
        Ok(AttAttributeDataChild::RawData(value.to_vec().into_boxed_slice()))
    } else {
        Err(AttErrorCode::try_from(status as u64).unwrap_or(AttErrorCode::UNLIKELY_ERROR))
    };
    do_in_rust_thread(move |modules| {
        modules.gatt_callbacks.send_response(ConnectionId(conn_id), TransactionId(trans_id), value);
    })
}

fn associate_server_with_advertiser(server_id: u8, advertiser_id: u8) {
    arbiter::with_arbiter(move |arbiter| {
        arbiter.associate_server_with_advertiser(ServerId(server_id), AdvertiserId(advertiser_id))
    })
}

fn clear_advertiser(advertiser_id: u8) {
    arbiter::with_arbiter(move |arbiter| arbiter.clear_advertiser(AdvertiserId(advertiser_id)))
}
