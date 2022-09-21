//! Security service facade

//use bt_topshim::btif;
//, RawAddress
use bt_topshim::btif::BluetoothInterface;

use bt_topshim_facade_protobuf::empty::Empty;
//use bt_topshim_facade_protobuf::facade::{EventType, FetchEventsRequest, FetchEventsResponse}
use bt_topshim_facade_protobuf::facade::RemoveBondRequest;
use bt_topshim_facade_protobuf::facade_grpc::{create_security_service, SecurityService};
//use futures::sink::SinkExt;
use grpcio::*;

use std::sync::{Arc, Mutex};
use tokio::runtime::Runtime;
//use tokio::sync::mpsc;
//use tokio::sync::Mutex as TokioMutex;

/// Main object for Adapter facade service
#[derive(Clone)]
pub struct SecurityServiceImpl {
    #[allow(dead_code)]
    rt: Arc<Runtime>,
    #[allow(dead_code)]
    btif_intf: Arc<Mutex<BluetoothInterface>>,
    //    #[allow(dead_code)]
    //    event_rx: Arc<TokioMutex<mpsc::Receiver<BaseCallbacks>>>,
    //    #[allow(dead_code)]
    //    event_tx: mpsc::Sender<BaseCallbacks>,
}

#[allow(dead_code)]
impl SecurityServiceImpl {
    /// Create a new instance of the root facade service
    pub fn create(rt: Arc<Runtime>, btif_intf: Arc<Mutex<BluetoothInterface>>) -> grpcio::Service {
        //        let (event_tx, rx) = mpsc::channel(10);
        //        let btif_clone = btif_intf.clone();
        create_security_service(Self {
            rt,
            btif_intf,
            //            event_rx: Arc::new(TokioMutex::new(rx)),
            //            event_tx,
        })
    }
}

impl SecurityService for SecurityServiceImpl {
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
    //                    BaseCallbacks::AdapterState(_state) => {
    //                        let mut rsp = FetchEventsResponse::new();
    //                        rsp.event_type = EventType::ADAPTER_STATE;
    //                        rsp.data = "ON".to_string();
    //                        sink.send((rsp, WriteFlags::default())).await.unwrap();
    //                    }
    //                    BaseCallbacks::SspRequest(_, _, _, _, _) => {}
    //                    BaseCallbacks::LeRandCallback(random) => {
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

    fn remove_bond(
        &mut self,
        ctx: RpcContext<'_>,
        _req: RemoveBondRequest,
        sink: UnarySink<Empty>,
    ) {
        //        println!("Test");
        //        println!("Test: {:?} ", req.address);
        //        let raw_address = RawAddress::from_string(req.address).unwrap();
        //        self.btif_intf.lock().unwrap().remove_bond(&raw_address);
        ctx.spawn(async move {
            sink.success(Empty::default()).await.unwrap();
        })
    }
}
