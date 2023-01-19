use async_trait::async_trait;
use bluetooth_core::{
    gatt::{
        callbacks::GattDatastore,
        ids::{AttHandle, ConnectionId},
    },
    packets::{
        AttAttributeDataChild, AttAttributeDataView, AttErrorCode, OwnedAttAttributeDataView,
        Packet,
    },
};
use tokio::sync::{
    mpsc::{self, unbounded_channel, UnboundedReceiver},
    oneshot,
};

pub struct MockDatastore(mpsc::UnboundedSender<MockDatastoreEvents>);

impl MockDatastore {
    pub fn new() -> (Self, UnboundedReceiver<MockDatastoreEvents>) {
        let (tx, rx) = unbounded_channel();
        (Self(tx), rx)
    }
}

#[derive(Debug)]
pub enum MockDatastoreEvents {
    AddConnection(ConnectionId),
    RemoveConnection(ConnectionId),
    ReadCharacteristic(
        ConnectionId,
        AttHandle,
        oneshot::Sender<Result<AttAttributeDataChild, AttErrorCode>>,
    ),
    WriteCharacteristic(
        ConnectionId,
        AttHandle,
        OwnedAttAttributeDataView,
        oneshot::Sender<Result<(), AttErrorCode>>,
    ),
}

#[async_trait(?Send)]
impl GattDatastore for MockDatastore {
    fn add_connection(&self, conn_id: ConnectionId) {
        self.0.send(MockDatastoreEvents::AddConnection(conn_id)).unwrap();
    }

    fn remove_connection(&self, conn_id: ConnectionId) {
        self.0.send(MockDatastoreEvents::RemoveConnection(conn_id)).unwrap();
    }

    async fn read_characteristic(
        &self,
        conn_id: ConnectionId,
        handle: AttHandle,
    ) -> Result<AttAttributeDataChild, AttErrorCode> {
        let (tx, rx) = oneshot::channel();
        self.0.send(MockDatastoreEvents::ReadCharacteristic(conn_id, handle, tx)).unwrap();
        rx.await.unwrap()
    }

    async fn write_characteristic(
        &self,
        conn_id: ConnectionId,
        handle: AttHandle,
        data: AttAttributeDataView<'_>,
    ) -> Result<(), AttErrorCode> {
        let (tx, rx) = oneshot::channel();
        self.0
            .send(MockDatastoreEvents::WriteCharacteristic(
                conn_id,
                handle,
                data.to_owned_packet(),
                tx,
            ))
            .unwrap();
        rx.await.unwrap()
    }
}
