//! GATT service facade

use bt_topshim::btif::BluetoothInterface;
use bt_topshim::profiles::gatt::Gatt;

use bt_topshim_facade_protobuf::empty::Empty;
//use bt_topshim_facade_protobuf::facade::{
//    EventType, FetchEventsRequest, FetchEventsResponse, SetDiscoveryModeRequest,
//    SetDiscoveryModeResponse, ToggleStackRequest, ToggleStackResponse,
//};
use bt_topshim_facade_protobuf::facade_grpc::{create_gatt_service, GattService};
//use futures::sink::SinkExt;
use grpcio::*;

use std::sync::{Arc, Mutex};
use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::sync::Mutex as TokioMutex;

struct GattCallbacks {}

/// Main object for GATT facade service
#[derive(Clone)]
pub struct GattServiceImpl {
    #[allow(dead_code)]
    rt: Arc<Runtime>,
    #[allow(dead_code)]
    btif_intf: Arc<Mutex<BluetoothInterface>>,
    #[allow(dead_code)]
    gatt: Arc<Mutex<Option<Gatt>>>,
    #[allow(dead_code)]
    event_rx: Arc<TokioMutex<mpsc::Receiver<GattCallbacks>>>,
    #[allow(dead_code)]
    event_tx: mpsc::Sender<GattCallbacks>,
}

impl GattServiceImpl {
    /// Create a new instance of the root facade service
    pub fn create(rt: Arc<Runtime>, btif_intf: Arc<Mutex<BluetoothInterface>>) -> grpcio::Service {
        let (event_tx, rx) = mpsc::channel(10);
        let btif_clone = btif_intf.clone();
        let x = create_gatt_service(Self {
            rt,
            btif_intf,
            gatt: Arc::new(Mutex::new(Gatt::new(&btif_clone.lock().unwrap()))),
            event_rx: Arc::new(TokioMutex::new(rx)),
            event_tx,
        });
        x
    }
}

impl GattService for GattServiceImpl {
    //    fn fetch_events(
    //        &mut self,
    //        ctx: RpcContext<'_>,
    //        _req: FetchEventsRequest,
    //        mut sink: ServerStreamingSink<FetchEventsResponse>,
    //    ) {
    //        let rx = self.event_rx.clone();
    //        ctx.spawn(async move {
    //            while let Some(event) = rx.lock().await.recv().await {
    //                match event {
    //                    GattCallbacks::AdapterState(_state) => {
    //                        let mut rsp = FetchEventsResponse::new();
    //                        rsp.event_type = EventType::ADAPTER_STATE;
    //                        rsp.data = "ON".to_string();
    //                        sink.send((rsp, WriteFlags::default())).await.unwrap();
    //                    }
    //                    GattCallbacks::SspRequest(_, _, _, _, _) => {}
    //                    GattCallbacks::LeRandCallback(random) => {
    //                        let mut rsp = FetchEventsResponse::new();
    //                        rsp.event_type = EventType::LE_RAND;
    //                        rsp.data = random.to_string();
    //                        sink.send((rsp, WriteFlags::default())).await.unwrap();
    //                    }
    //                    _ => (),
    //                }
    //            }
    //        })
    //    }

    fn stop_advertising_set(&mut self, ctx: RpcContext<'_>, _req: Empty, sink: UnarySink<Empty>) {
        ctx.spawn(async move {
            sink.success(Empty::default()).await.unwrap();
        })
    }
}
