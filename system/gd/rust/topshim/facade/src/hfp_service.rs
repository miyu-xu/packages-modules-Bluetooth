//! HFP service facade

use bt_topshim::btif::{BluetoothInterface, RawAddress};
use bt_topshim::profiles::hfp::{Hfp, HfpCallbacks, HfpCallbacksDispatcher};
use bt_topshim_facade_protobuf::empty::Empty;
use bt_topshim_facade_protobuf::facade::{
    ConnectAudioRequest, DisconnectAudioRequest, EventType, FetchEventsRequest,
    FetchEventsResponse, SetVolumeRequest, StartSlcRequest, StopSlcRequest,
};
use bt_topshim_facade_protobuf::facade_grpc::{create_hfp_service, HfpService};

use grpcio::*;

use std::str::from_utf8;
use std::sync::{Arc, Mutex};
use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::sync::Mutex as TokioMutex;

fn get_hfp_dispatcher(
    hfp: Arc<Mutex<Hfp>>,
    tx: mpsc::Sender<HfpCallbacks>,
) -> HfpCallbacksDispatcher {
    HfpCallbacksDispatcher {
        dispatch: Box::new(move |cb: HfpCallbacks| {
            if tx.clone().try_send(cb.clone()).is_err() {
                println!("Cannot send event {:?}", cb);
            }
            match cb {
                HfpCallbacks::BthfConnectionState(state, address) => {
                    println!(
                        "Hfp Connection state changed to {:?} for address {:?}",
                        state, address
                    );
                }
                _ => (),
            }
        }),
    }
}

/// Main object for Hfp facade service
#[derive(Clone)]
pub struct HfpServiceImpl {
    #[allow(dead_code)]
    rt: Arc<Runtime>,
    pub btif_hfp: Arc<Mutex<Hfp>>,
    event_rx: Arc<TokioMutex<mpsc::Receiver<HfpCallbacks>>>,
    #[allow(dead_code)]
    event_tx: mpsc::Sender<HfpCallbacks>,
}

impl HfpServiceImpl {
    /// Create a new instance of the root facade service
    pub fn create(rt: Arc<Runtime>, btif_intf: Arc<Mutex<BluetoothInterface>>) -> grpcio::Service {
        let (event_tx, rx) = mpsc::channel(10);
        let mut btif_hfp = Hfp::new(&btif_intf.lock().unwrap());
        btif_hfp.initialize(get_hfp_dispatcher(btif_hfp.clone(), event_tx.clone()));

        create_hfp_service(Self {
            rt,
            btif_hfp: Arc::new(Mutex::new(btif_hfp)),
            event_rx: Arc::new(TokioMutex::new(rx)),
            event_tx,
        })
    }
}

impl HfpService for HfpServiceImpl {
    fn start_slc(&mut self, ctx: RpcContext<'_>, req: StartSlcRequest, sink: UnarySink<Empty>) {
        let hfp = self.btif_hfp.clone();
        ctx.spawn(async move {
            let bt_addr = &req.connection.unwrap().cookie;
            if let Some(addr) = RawAddress::from_bytes(bt_addr) {
                hfp.lock().unwrap().connect(addr);
                sink.success(Empty::default()).await.unwrap();
            } else {
                sink.fail(RpcStatus::with_message(
                    RpcStatusCode::INVALID_ARGUMENT,
                    format!("Invalid Request Address: {}", from_utf8(bt_addr).unwrap()),
                ))
                .await
                .unwrap();
            }
        })
    }

    fn stop_slc(&mut self, ctx: RpcContext<'_>, req: StopSlcRequest, sink: UnarySink<Empty>) {
        let hfp = self.btif_hfp.clone();
        ctx.spawn(async move {
            let bt_addr = &req.connection.unwrap().cookie;
            if let Some(addr) = RawAddress::from_bytes(bt_addr) {
                hfp.lock().unwrap().disconnect(addr);
                sink.success(Empty::default()).await.unwrap();
            } else {
                sink.fail(RpcStatus::with_message(
                    RpcStatusCode::INVALID_ARGUMENT,
                    format!("Invalid Request Address: {}", from_utf8(bt_addr).unwrap()),
                ))
                .await
                .unwrap();
            }
        })
    }

    fn connect_audio(
        &mut self,
        ctx: RpcContext<'_>,
        req: ConnectAudioRequest,
        sink: UnarySink<Empty>,
    ) {
        let hfp = self.btif_hfp.clone();
        ctx.spawn(async move {
            let bt_addr = &req.connection.unwrap().cookie;
            if let Some(addr) = RawAddress::from_bytes(bt_addr) {
                hfp.lock().unwrap().connect_audio(addr, req.is_sco_offload_enabled, req.force_cvsd);
                hfp.lock().unwrap().set_active_device(addr);
                sink.success(Empty::default()).await.unwrap();
            } else {
                sink.fail(RpcStatus::with_message(
                    RpcStatusCode::INVALID_ARGUMENT,
                    format!("Invalid Request Address: {}", from_utf8(bt_addr).unwrap()),
                ))
                .await
                .unwrap();
            }
        })
    }

    fn disconnect_audio(
        &mut self,
        ctx: RpcContext<'_>,
        req: DisconnectAudioRequest,
        sink: UnarySink<Empty>,
    ) {
        let hfp = self.btif_hfp.clone();
        ctx.spawn(async move {
            let bt_addr = &req.connection.unwrap().cookie;
            if let Some(addr) = RawAddress::from_bytes(bt_addr) {
                hfp.lock().unwrap().disconnect_audio(addr);
                sink.success(Empty::default()).await.unwrap();
            } else {
                sink.fail(RpcStatus::with_message(
                    RpcStatusCode::INVALID_ARGUMENT,
                    format!("Invalid Request Address: {}", from_utf8(bt_addr).unwrap()),
                ))
                .await
                .unwrap();
            }
        })
    }

    fn set_volume(&mut self, ctx: RpcContext<'_>, req: SetVolumeRequest, sink: UnarySink<Empty>) {
        let hfp = self.btif_hfp.clone();
        ctx.spawn(async move {
            let bt_addr = &req.connection.unwrap().cookie;
            if let Some(addr) = RawAddress::from_bytes(bt_addr) {
                // TODO(aritrasen): Consider using TryFrom and cap the maximum volume here
                // since `as` silently deals with data overflow, which might not be preferred.
                hfp.lock().unwrap().set_volume(req.volume as i8, addr);
                sink.success(Empty::default()).await.unwrap();
            } else {
                sink.fail(RpcStatus::with_message(
                    RpcStatusCode::INVALID_ARGUMENT,
                    format!("Invalid Request Address: {}", from_utf8(bt_addr).unwrap()),
                ))
                .await
                .unwrap();
            }
        })
    }

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
                    HfpCallbacks::BthfConnectionState(state, address) => {
                        let mut rsp = FetchEventsResponse::new();
                        rsp.event_type = EventType::HFP_CONNECTION_STATE;
                        rsp.data = format!("{}, {}", state, address);
                        sink.send((rsp, WriteFlags::default())).await.unwrap();
                    }
                    _ => (),
                }
            }
        })
    }
}
