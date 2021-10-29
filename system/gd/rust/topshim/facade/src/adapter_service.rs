//! Adapter service facade

use bt_topshim::btif;
use bt_topshim::btif::{
    BaseCallbacks, BaseCallbacksDispatcher, BluetoothInterface, BtBondState, BtState, BtTransport,
    RawAddress,
};
use bt_topshim::controller::Controller;

use bt_blueberry_protobuf::empty::Empty;
use bt_blueberry_protobuf::host::{
    ConnectRequest, ConnectResponse, Connection, DisconnectRequest, DisconnectResponse,
    GetConnectionRequest, GetConnectionResponse, ReadLocalAddressResponse, WaitConnectionRequest,
    WaitConnectionResponse,
};
use bt_blueberry_protobuf::host_grpc::{create_host, Host};
use bt_topshim_facade_protobuf::facade::{
    EventType, FetchEventsRequest, FetchEventsResponse, SetDiscoveryModeRequest,
    SetDiscoveryModeResponse, ToggleStackRequest, ToggleStackResponse,
};
use bt_topshim_facade_protobuf::facade_grpc::{create_adapter_service, AdapterService};

use futures::sink::SinkExt;
use grpcio::*;

use std::future::Future;
use std::sync::{Arc, Mutex};
use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::sync::oneshot;
use tokio::sync::Mutex as TokioMutex;
fn get_bt_dispatcher(
    btif: Arc<Mutex<BluetoothInterface>>,
    tx: mpsc::Sender<BaseCallbacks>,
) -> BaseCallbacksDispatcher {
    BaseCallbacksDispatcher {
        dispatch: Box::new(move |cb| {
            println!("BT Event {:?}", cb);
            if let Err(cb) = tx.try_send(cb.clone()) {
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
        }),
    }
}

/// Main object for Adapter facade service
#[derive(Clone)]
pub struct AdapterServiceImpl {
    #[allow(dead_code)]
    rt: Arc<Runtime>,
    btif_intf: Arc<Mutex<BluetoothInterface>>,
    controller: Arc<Mutex<Controller>>,
    event_rx: Arc<TokioMutex<mpsc::Receiver<BaseCallbacks>>>,
    #[allow(dead_code)]
    event_tx: mpsc::Sender<BaseCallbacks>,
    reset: Arc<Mutex<Option<oneshot::Sender<()>>>>,
}

impl AdapterServiceImpl {
    /// Create a new instance of the root facade service
    pub fn create(
        rt: Arc<Runtime>,
        btif_intf: Arc<Mutex<BluetoothInterface>>,
        reset: oneshot::Sender<()>,
        blueberry: bool,
    ) -> (grpcio::Service, impl Future<Output = ()>) {
        let (event_tx, rx) = mpsc::channel(10);
        btif_intf.lock().unwrap().initialize(
            get_bt_dispatcher(btif_intf.clone(), event_tx.clone()),
            vec!["INIT_gd_hci=true".to_string()],
        );
        let controller = Controller::new();

        let service = Self {
            rt,
            btif_intf,
            controller: Arc::new(Mutex::new(controller)),
            event_rx: Arc::new(TokioMutex::new(rx)),
            event_tx,
            reset: Arc::new(Mutex::new(Some(reset))),
        };

        let future = service.wait_for_adapter();

        (if blueberry { create_host(service) } else { create_adapter_service(service) }, future)
    }

    pub fn wait_for_adapter(&self) -> impl Future<Output = ()> {
        let rx = self.event_rx.clone();
        async move {
            while let Some(event) = rx.lock().await.recv().await {
                if let BaseCallbacks::AdapterState(BtState::On) = event {
                    break;
                }
            }
        }
    }
}

impl AdapterService for AdapterServiceImpl {
    fn fetch_events(
        &mut self,
        ctx: RpcContext<'_>,
        _req: FetchEventsRequest,
        mut sink: ServerStreamingSink<FetchEventsResponse>,
    ) {
        let rx = self.event_rx.clone();
        ctx.spawn(async move {
            while let Some(event) = rx.lock().await.recv().await {
                match event {
                    BaseCallbacks::AdapterState(_state) => {
                        let mut rsp = FetchEventsResponse::new();
                        rsp.event_type = EventType::ADAPTER_STATE;
                        rsp.data = "ON".to_string();
                        sink.send((rsp, WriteFlags::default())).await.unwrap();
                    }
                    BaseCallbacks::SspRequest(_, _, _, _, _) => {}
                    _ => (),
                }
            }
        })
    }

    fn toggle_stack(
        &mut self,
        ctx: RpcContext<'_>,
        req: ToggleStackRequest,
        sink: UnarySink<ToggleStackResponse>,
    ) {
        match req.start_stack {
            true => self.btif_intf.lock().unwrap().enable(),
            false => self.btif_intf.lock().unwrap().disable(),
        };
        ctx.spawn(async move {
            sink.success(ToggleStackResponse::default()).await.unwrap();
        })
    }

    fn set_discovery_mode(
        &mut self,
        ctx: RpcContext<'_>,
        _req: SetDiscoveryModeRequest,
        sink: UnarySink<SetDiscoveryModeResponse>,
    ) {
        self.btif_intf.lock().unwrap().set_adapter_property(
            btif::BluetoothProperty::AdapterScanMode(btif::BtScanMode::Connectable),
        );

        ctx.spawn(async move {
            sink.success(SetDiscoveryModeResponse::default()).await.unwrap();
        })
    }
}

impl Host for AdapterServiceImpl {
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
        self.btif_intf
            .lock()
            .unwrap()
            .create_bond(&RawAddress::from_bytes(&*req.address).unwrap(), BtTransport::Bredr);

        let mut response = ConnectResponse::new();
        let mut connection = Connection::new();
        connection.set_cookie(req.address);
        response.set_connection(connection);

        let rx = self.event_rx.clone();

        ctx.spawn(async move {
            // Wait for connected event
            while let Some(event) = rx.lock().await.recv().await {
                if let BaseCallbacks::BondState(_, _, BtBondState::Bonded, _) = event {
                    break;
                }
            }
            sink.success(response).await.unwrap();
        })
    }

    fn wait_connection(
        &mut self,
        ctx: RpcContext<'_>,
        req: WaitConnectionRequest,
        sink: UnarySink<WaitConnectionResponse>,
    ) {
        let mut response = WaitConnectionResponse::new();
        let mut connection = Connection::new();
        connection.set_cookie(req.address);
        response.set_connection(connection);

        let rx = self.event_rx.clone();

        ctx.spawn(async move {
            // Wait for connected event
            while let Some(event) = rx.lock().await.recv().await {
                if let BaseCallbacks::BondState(_, _, BtBondState::Bonded, _) = event {
                    break;
                }
            }
            sink.success(response).await.unwrap();
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
        let addr = req.mut_connection().take_cookie();

        self.btif_intf.lock().unwrap().remove_bond(&RawAddress::from_bytes(&*addr).unwrap());

        let rx = self.event_rx.clone();

        ctx.spawn(async move {
            // Wait for disconnected event
            while let Some(event) = rx.lock().await.recv().await {
                if let BaseCallbacks::BondState(_, _, BtBondState::NotBonded, _) = event {
                    break;
                }
            }
            sink.success(DisconnectResponse::new()).await.unwrap();
        })
    }

    fn read_local_address(
        &mut self,
        ctx: RpcContext<'_>,
        _req: Empty,
        sink: UnarySink<ReadLocalAddressResponse>,
    ) {
        let addr = self.controller.lock().unwrap().read_local_addr();

        let mut response = ReadLocalAddressResponse::new();
        response.set_address(addr.to_vec());

        ctx.spawn(async move {
            sink.success(response).await.unwrap();
        })
    }
}
