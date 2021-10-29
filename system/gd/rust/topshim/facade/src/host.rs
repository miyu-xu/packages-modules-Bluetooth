use bt_topshim::btif::{
    BaseCallbacks, BaseCallbacksDispatcher, BluetoothInterface, BluetoothProperty, BtBondState,
    BtScanMode, BtState, BtStatus, BtTransport, RawAddress,
};
use bt_topshim::controller::Controller;

use bt_blueberry_protobuf::empty::Empty;
use bt_blueberry_protobuf::host::{
    ConnectRequest, ConnectResponse, Connection, DisconnectRequest, DisconnectResponse,
    GetConnectionRequest, GetConnectionResponse, ReadLocalAddressResponse, WaitConnectionRequest,
    WaitConnectionResponse,
};
use bt_blueberry_protobuf::host_grpc;

use grpcio::*;

use std::future::{self, Future};
use std::sync::{Arc, Mutex};

use tokio::pin;
use tokio::sync::broadcast;
use tokio::sync::oneshot;

use futures::stream::{unfold, Stream, StreamExt};

fn broadcast_stream<T: Clone>(rx: broadcast::Receiver<T>) -> impl Stream<Item = T> {
    unfold(rx, |mut rx| async move {
        if let Ok(value) = rx.recv().await {
            Some((value, rx))
        } else {
            None
        }
    })
}

#[derive(Clone)]
pub struct Host {
    pub btif: Arc<Mutex<BluetoothInterface>>,
    controller: Arc<Mutex<Controller>>,
    events: broadcast::Sender<BaseCallbacks>,
    reset: Arc<Mutex<Option<oneshot::Sender<()>>>>,
}

impl From<Host> for grpcio::Service {
    fn from(host: Host) -> Self {
        host_grpc::create_host(host)
    }
}

impl Host {
    pub async fn initialize(
        btif: BluetoothInterface,
        reset: oneshot::Sender<()>,
        args: Vec<String>,
    ) -> Self {
        let btif = Arc::new(Mutex::new(btif));
        let controller = Arc::new(Mutex::new(Controller::new()));
        let (events, _) = broadcast::channel(10);
        let reset = Arc::new(Mutex::new(Some(reset)));

        btif.lock().unwrap().initialize(Self::get_dispatcher(btif.clone(), events.clone()), args);
        btif.lock().unwrap().enable();

        let stream = broadcast_stream(events.subscribe());
        pin!(stream);
        stream
            .filter(|event| {
                future::ready(matches!(event, BaseCallbacks::AdapterState(BtState::On)))
            })
            .next()
            .await
            .unwrap();

        btif.lock()
            .unwrap()
            .set_adapter_property(BluetoothProperty::AdapterScanMode(BtScanMode::Connectable));

        Self { btif, controller, events, reset }
    }

    fn get_dispatcher(
        btif: Arc<Mutex<BluetoothInterface>>,
        tx: broadcast::Sender<BaseCallbacks>,
    ) -> BaseCallbacksDispatcher {
        let dispatch = move |cb: BaseCallbacks| {
            println!("BT Event {:?}", cb);
            if let Err(cb) = tx.send(cb.clone()) {
                println!("Cannot send event {:?}", cb);
            }

            match cb {
                BaseCallbacks::AdapterState(state) => {
                    println!("State changed to {:?}", state);
                }
                BaseCallbacks::SspRequest(addr, _, _, variant, passkey) => {
                    btif.lock().unwrap().ssp_reply(&addr, variant, 1, passkey);
                }
                _ => (),
            }
        };

        BaseCallbacksDispatcher { dispatch: Box::new(dispatch) }
    }

    fn wait_for_bond_state(
        &self,
        wanted_address: RawAddress,
        wanted_state: BtBondState,
    ) -> impl Future<Output = bool> {
        let rx = self.events.subscribe();
        async move {
            let stream = broadcast_stream(rx);
            pin!(stream);
            let status = stream
                .filter_map(|event| {
                    future::ready(match event {
                        BaseCallbacks::BondState(status, address, state, _)
                            if address == wanted_address && state == wanted_state =>
                        {
                            Some(status)
                        }
                        _ => None,
                    })
                })
                .next()
                .await;

            matches!(status, Some(BtStatus::Success))
        }
    }
}

impl host_grpc::Host for Host {
    fn reset(&mut self, ctx: RpcContext<'_>, _req: Empty, sink: UnarySink<Empty>) {
        if let Some(reset) = self.reset.lock().unwrap().take() {
            ctx.spawn(async move {
                sink.success(Empty::new()).await.unwrap();
                reset.send(()).unwrap();
            })
        } else {
            ctx.spawn(async move {
                sink.fail(RpcStatus::new(RpcStatusCode::RESOURCE_EXHAUSTED)).await.unwrap();
            })
        }
    }

    fn connect(
        &mut self,
        ctx: RpcContext<'_>,
        req: ConnectRequest,
        sink: UnarySink<ConnectResponse>,
    ) {
        let address = RawAddress::from_bytes(&*req.address).unwrap();

        self.btif.lock().unwrap().create_bond(&address, BtTransport::Bredr);

        let bonded = self.wait_for_bond_state(address, BtBondState::Bonded);
        ctx.spawn(async move {
            if bonded.await {
                let mut response = ConnectResponse::new();
                let mut connection = Connection::new();
                connection.set_cookie(req.address);
                response.set_connection(connection);

                sink.success(response).await.unwrap();
            } else {
                sink.fail(RpcStatus::new(RpcStatusCode::UNKNOWN)).await.unwrap();
            }
        })
    }

    fn wait_connection(
        &mut self,
        ctx: RpcContext<'_>,
        req: WaitConnectionRequest,
        sink: UnarySink<WaitConnectionResponse>,
    ) {
        let address = RawAddress::from_bytes(&*req.address).unwrap();

        let bonded = self.wait_for_bond_state(address, BtBondState::Bonded);
        ctx.spawn(async move {
            if bonded.await {
                let mut response = WaitConnectionResponse::new();
                let mut connection = Connection::new();
                connection.set_cookie(req.address);
                response.set_connection(connection);

                sink.success(response).await.unwrap();
            } else {
                sink.fail(RpcStatus::new(RpcStatusCode::UNKNOWN)).await.unwrap();
            }
        })
    }

    fn get_connection(
        &mut self,
        ctx: RpcContext<'_>,
        req: GetConnectionRequest,
        sink: UnarySink<GetConnectionResponse>,
    ) {
        // FIXME: check if connection exist
        let mut response = GetConnectionResponse::new();
        let mut connection = Connection::new();
        connection.set_cookie(req.address);
        response.set_connection(connection);

        ctx.spawn(async move {
            sink.success(response).await.unwrap();
        })
    }

    fn disconnect(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: DisconnectRequest,
        sink: UnarySink<DisconnectResponse>,
    ) {
        let address = RawAddress::from_bytes(&*req.mut_connection().take_cookie()).unwrap();

        self.btif.lock().unwrap().remove_bond(&address);

        let unbonded = self.wait_for_bond_state(address, BtBondState::NotBonded);
        ctx.spawn(async move {
            if unbonded.await {
                sink.success(DisconnectResponse::new()).await.unwrap();
            } else {
                sink.fail(RpcStatus::new(RpcStatusCode::UNKNOWN)).await.unwrap();
            }
        })
    }

    fn read_local_address(
        &mut self,
        ctx: RpcContext<'_>,
        _req: Empty,
        sink: UnarySink<ReadLocalAddressResponse>,
    ) {
        let local_addr = self.controller.lock().unwrap().read_local_addr();

        ctx.spawn(async move {
            let mut response = ReadLocalAddressResponse::new();
            response.set_address(local_addr.to_vec());
            sink.success(response).await.unwrap();
        })
    }
}
