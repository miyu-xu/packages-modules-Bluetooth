use std::rc::Rc;

use bluetooth_core::{
    gatt::{
        self,
        ids::{AttHandle, ConnectionId, ServerId, TransportIndex},
        server::{
            gatt_database::{
                AttPermissions, AttUuid, GattCharacteristicWithHandle, GattServiceWithHandle,
            },
            GattModule,
        },
    },
    packets::{
        AttAttributeDataChild, AttBuilder, AttOpcode, AttReadRequestBuilder,
        AttReadResponseBuilder, AttServiceDeclarationValueBuilder, AttWriteRequestBuilder,
        AttWriteResponseBuilder, Serializable,
    },
    utils::packet::{build_att_data, build_att_view},
};

use mocks::{
    mock_datastore::{MockDatastore, MockDatastoreEvents},
    mock_transport::MockAttTransport,
};

use tokio::sync::mpsc::UnboundedReceiver;
use utils::start_test;

mod mocks;
mod utils;

const TCB_IDX: TransportIndex = TransportIndex(1);
const SERVER_ID: ServerId = ServerId(2);
const CONN_ID: ConnectionId = ConnectionId::new(TCB_IDX, SERVER_ID);
const HANDLE_1: AttHandle = AttHandle(3);
const HANDLE_2: AttHandle = AttHandle(5);
const UUID_1: AttUuid = AttUuid::new([1, 2, 0, 0]);
const UUID_2: AttUuid = AttUuid::new([1, 3, 0, 0]);

fn start_gatt_module() -> (
    gatt::server::GattModule,
    UnboundedReceiver<MockDatastoreEvents>,
    UnboundedReceiver<(TransportIndex, AttBuilder)>,
) {
    let (datastore, data_rx) = MockDatastore::new();
    let (transport, transport_rx) = MockAttTransport::new();
    let gatt = GattModule::new(Rc::new(datastore), Rc::new(transport));

    (gatt, data_rx, transport_rx)
}

fn create_server_and_open_connection(gatt: &mut GattModule) {
    gatt.open_gatt_server(SERVER_ID);
    gatt.register_gatt_service(
        SERVER_ID,
        GattServiceWithHandle {
            handle: HANDLE_1,
            uuid: UUID_1,
            characteristics: vec![GattCharacteristicWithHandle {
                handle: HANDLE_2,
                uuid: UUID_2,
                permissions: AttPermissions { readable: true, writable: false },
            }],
        },
    )
    .unwrap();
    gatt.on_le_connect(CONN_ID);
}

#[test]
fn test_connection_creation() {
    start_test(async move {
        // arrange
        let (mut gatt, mut data_rx, _) = start_gatt_module();

        gatt.open_gatt_server(SERVER_ID);
        gatt.register_gatt_service(
            SERVER_ID,
            GattServiceWithHandle { handle: HANDLE_1, uuid: UUID_1, characteristics: vec![] },
        )
        .unwrap();

        // act
        gatt.on_le_connect(CONN_ID);

        // assert
        assert!(matches!(
            data_rx.recv().await.unwrap(),
            MockDatastoreEvents::AddConnection(CONN_ID)
        ));
    })
}

#[test]
fn test_service_read() {
    start_test(async move {
        // arrange
        let (mut gatt, mut data_rx, mut transport_rx) = start_gatt_module();

        create_server_and_open_connection(&mut gatt);
        data_rx.recv().await.unwrap();

        // act
        gatt.handle_packet(
            CONN_ID,
            build_att_view(AttReadRequestBuilder { attribute_handle: HANDLE_1.into() }).view(),
        );
        let (tcb_idx, resp) = transport_rx.recv().await.unwrap();

        // assert
        assert_eq!(tcb_idx, TCB_IDX);
        assert_eq!(
            resp,
            AttBuilder {
                opcode: AttOpcode::READ_RESPONSE,
                _child_: AttReadResponseBuilder {
                    value: build_att_data(AttServiceDeclarationValueBuilder {
                        uuid: UUID_1.into()
                    })
                }
                .into()
            }
        );
    })
}

#[test]
fn test_characteristic_read() {
    start_test(async move {
        // arrange
        let (mut gatt, mut data_rx, mut transport_rx) = start_gatt_module();

        let data = AttAttributeDataChild::RawData(vec![5, 6, 7, 8].into_boxed_slice());

        create_server_and_open_connection(&mut gatt);
        data_rx.recv().await.unwrap();

        // act
        gatt.handle_packet(
            CONN_ID,
            build_att_view(AttReadRequestBuilder { attribute_handle: HANDLE_2.into() }).view(),
        );
        let tx = if let MockDatastoreEvents::ReadCharacteristic(CONN_ID, HANDLE_2, tx) =
            data_rx.recv().await.unwrap()
        {
            tx
        } else {
            unreachable!()
        };
        tx.send(Ok(data.clone())).unwrap();
        let (tcb_idx, resp) = transport_rx.recv().await.unwrap();

        // assert
        assert_eq!(tcb_idx, TCB_IDX);
        assert_eq!(
            resp,
            AttBuilder {
                opcode: AttOpcode::READ_RESPONSE,
                _child_: AttReadResponseBuilder { value: build_att_data(data) }.into()
            }
        );
    })
}

#[test]
fn test_characteristic_write() {
    start_test(async move {
        // arrange
        let (mut gatt, mut data_rx, mut transport_rx) = start_gatt_module();

        let data = AttAttributeDataChild::RawData(vec![5, 6, 7, 8].into_boxed_slice());

        create_server_and_open_connection(&mut gatt);
        data_rx.recv().await.unwrap();

        // act
        gatt.handle_packet(
            CONN_ID,
            build_att_view(AttWriteRequestBuilder {
                handle: HANDLE_2.into(),
                value: build_att_data(data.clone()),
            })
            .view(),
        );
        let (tx, written_data) =
            if let MockDatastoreEvents::WriteCharacteristic(CONN_ID, HANDLE_2, written_data, tx) =
                data_rx.recv().await.unwrap()
            {
                (tx, written_data)
            } else {
                unreachable!()
            };
        tx.send(Ok(())).unwrap();
        let (tcb_idx, resp) = transport_rx.recv().await.unwrap();

        // assert
        assert_eq!(tcb_idx, TCB_IDX);
        assert_eq!(
            resp,
            AttBuilder {
                opcode: AttOpcode::WRITE_RESPONSE,
                _child_: AttWriteResponseBuilder {}.into()
            }
        );
        assert_eq!(
            data.to_vec().unwrap(),
            written_data.view().get_raw_payload().collect::<Vec<_>>()
        )
    })
}
