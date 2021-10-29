//! Media service facade

use bt_blueberry_protobuf::a2dp::{
    AbortRequest, AbortResponse, CloseRequest, CloseResponse, OpenSinkRequest, OpenSinkResponse,
    OpenSourceRequest, OpenSourceResponse, Sink, Source, StartRequest, StartResponse,
    SuspendRequest, SuspendResponse,
};
use bt_blueberry_protobuf::a2dp_grpc::{create_a2_dp, A2Dp};
use bt_topshim::btif::{BluetoothInterface, RawAddress};
use bt_topshim::profiles::a2dp::{
    A2dp, A2dpCallbacks, A2dpCallbacksDispatcher, A2dpSink, A2dpSinkCallbacks,
    A2dpSinkCallbacksDispatcher, BtavConnectionState,
};
use bt_topshim::profiles::avrcp::{Avrcp, AvrcpCallbacksDispatcher};
use bt_topshim_facade_protobuf::facade::{
    A2dpSourceConnectRequest, A2dpSourceConnectResponse, StartA2dpRequest, StartA2dpResponse,
};
use bt_topshim_facade_protobuf::facade_grpc::{create_media_service, MediaService};

use grpcio::*;

use std::sync::{Arc, Mutex};
use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::sync::Mutex as TokioMutex;

fn get_a2dp_dispatcher(tx: mpsc::Sender<A2dpCallbacks>) -> A2dpCallbacksDispatcher {
    A2dpCallbacksDispatcher {
        dispatch: Box::new(move |cb| {
            if let Err(cb) = tx.try_send(cb) {
                println!("Cannot send event {:?}", cb);
            }
        }),
    }
}

fn get_a2dp_sink_dispatcher(tx: mpsc::Sender<A2dpSinkCallbacks>) -> A2dpSinkCallbacksDispatcher {
    A2dpSinkCallbacksDispatcher {
        dispatch: Box::new(move |cb| {
            if let Err(cb) = tx.try_send(cb) {
                println!("Cannot send event {:?}", cb);
            }
        }),
    }
}

fn get_avrcp_dispatcher() -> AvrcpCallbacksDispatcher {
    AvrcpCallbacksDispatcher { dispatch: Box::new(move |_cb| {}) }
}

/// Main object for Media facade service
#[derive(Clone)]
pub struct MediaServiceImpl {
    rt: Arc<Runtime>,
    pub btif_a2dp: Arc<Mutex<A2dp>>,
    btif_a2dp_sink: Arc<Mutex<A2dpSink>>,
    pub btif_avrcp: Arc<Mutex<Avrcp>>,
    a2dp_rx: Arc<TokioMutex<mpsc::Receiver<A2dpCallbacks>>>,
    a2dp_sink_rx: Arc<TokioMutex<mpsc::Receiver<A2dpSinkCallbacks>>>,
}

impl MediaServiceImpl {
    /// Create a new instance of the root facade service
    pub fn create(
        rt: Arc<Runtime>,
        btif_intf: Arc<Mutex<BluetoothInterface>>,
        blueberry: bool,
    ) -> grpcio::Service {
        let mut btif_a2dp = A2dp::new(&btif_intf.lock().unwrap());
        let mut btif_a2dp_sink = A2dpSink::new(&btif_intf.lock().unwrap());
        let mut btif_avrcp = Avrcp::new(&btif_intf.lock().unwrap());

        let (a2dp_tx, a2dp_rx) = mpsc::channel(10);
        let (a2dp_sink_tx, a2dp_sink_rx) = mpsc::channel(10);
        btif_a2dp.initialize(get_a2dp_dispatcher(a2dp_tx));
        if blueberry {
            btif_a2dp_sink.initialize(get_a2dp_sink_dispatcher(a2dp_sink_tx));
        }
        btif_avrcp.initialize(get_avrcp_dispatcher());

        let service = Self {
            rt,
            btif_a2dp: Arc::new(Mutex::new(btif_a2dp)),
            btif_a2dp_sink: Arc::new(Mutex::new(btif_a2dp_sink)),
            btif_avrcp: Arc::new(Mutex::new(btif_avrcp)),
            a2dp_rx: Arc::new(TokioMutex::new(a2dp_rx)),
            a2dp_sink_rx: Arc::new(TokioMutex::new(a2dp_sink_rx)),
        };

        if blueberry {
            create_a2_dp(service)
        } else {
            create_media_service(service)
        }
    }
}

impl MediaService for MediaServiceImpl {
    fn start_a2dp(
        &mut self,
        ctx: RpcContext<'_>,
        req: StartA2dpRequest,
        sink: UnarySink<StartA2dpResponse>,
    ) {
        if req.start_a2dp_source {
            ctx.spawn(async move {
                sink.success(StartA2dpResponse::default()).await.unwrap();
            })
        } else if req.start_a2dp_sink {
            let (a2dp_sink_tx, a2dp_sink_rx) = mpsc::channel(10);
            self.btif_a2dp_sink.lock().unwrap().initialize(get_a2dp_sink_dispatcher(a2dp_sink_tx));
            self.a2dp_sink_rx = Arc::new(TokioMutex::new(a2dp_sink_rx));
            ctx.spawn(async move {
                sink.success(StartA2dpResponse::default()).await.unwrap();
            })
        }
    }

    fn a2dp_source_connect(
        &mut self,
        ctx: RpcContext<'_>,
        req: A2dpSourceConnectRequest,
        sink: UnarySink<A2dpSourceConnectResponse>,
    ) {
        let a2dp = self.btif_a2dp.clone();

        let addr = RawAddress::from_string(req.address).unwrap();
        ctx.spawn(async move {
            a2dp.lock().unwrap().connect(addr);
            a2dp.lock().unwrap().set_active_device(addr);
            sink.success(A2dpSourceConnectResponse::default()).await.unwrap();
        })
    }
}

#[allow(unused)]
impl A2Dp for MediaServiceImpl {
    fn open_source(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: OpenSourceRequest,
        sink: UnarySink<OpenSourceResponse>,
    ) {
        let a2dp = self.btif_a2dp.clone();
        let rx = self.a2dp_rx.clone();

        let cookie = req.mut_connection().take_cookie();

        let addr = RawAddress::from_bytes(&cookie).unwrap();

        let mut response = OpenSourceResponse::new();
        let mut source = Source::new();
        source.set_cookie(cookie);
        response.set_source(source);

        ctx.spawn(async move {
            a2dp.lock().unwrap().connect(addr);

            // Wait for connected event
            while let Some(event) = rx.lock().await.recv().await {
                if let A2dpCallbacks::ConnectionState(_, BtavConnectionState::Connected) = event {
                    break;
                }
            }
            a2dp.lock().unwrap().set_active_device(addr);
            sink.success(response).await.unwrap();
        })
    }

    fn open_sink(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: OpenSinkRequest,
        sink: UnarySink<OpenSinkResponse>,
    ) {
        let a2dp_sink = self.btif_a2dp_sink.clone();
        let rx = self.a2dp_sink_rx.clone();

        let cookie = req.mut_connection().take_cookie();

        let addr = RawAddress::from_bytes(&cookie).unwrap();

        let mut response = OpenSinkResponse::new();
        {
            let mut sink = Sink::new();
            sink.set_cookie(cookie);
            response.set_sink(sink);
        }

        ctx.spawn(async move {
            a2dp_sink.lock().unwrap().connect(addr);

            // Wait for connected event
            while let Some(event) = rx.lock().await.recv().await {
                if let A2dpSinkCallbacks::ConnectionState(_, BtavConnectionState::Connected) = event
                {
                    break;
                }
            }
            a2dp_sink.lock().unwrap().set_active_device(addr);
            sink.success(response).await.unwrap();
        })
    }

    fn start(&mut self, ctx: RpcContext<'_>, req: StartRequest, sink: UnarySink<StartResponse>) {
        if req.has_source() {
            let a2dp = self.btif_a2dp.clone();

            ctx.spawn(async move {
                a2dp.lock().unwrap().start_audio_request();
                sink.success(StartResponse::new()).await.unwrap();
            })
        } else {
            unimplemented_call!(ctx, sink);
        }
    }

    fn suspend(
        &mut self,
        ctx: RpcContext<'_>,
        req: SuspendRequest,
        sink: UnarySink<SuspendResponse>,
    ) {
        unimplemented_call!(ctx, sink);
    }

    fn close(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: CloseRequest,
        sink: UnarySink<CloseResponse>,
    ) {
        if req.has_sink() {
            let a2dp_sink = self.btif_a2dp_sink.clone();

            let cookie = req.mut_sink().take_cookie();

            let addr = RawAddress::from_bytes(&cookie).unwrap();

            ctx.spawn(async move {
                a2dp_sink.lock().unwrap().disconnect(addr);
                sink.success(CloseResponse::default()).await.unwrap();
            })
        } else {
            unimplemented_call!(ctx, sink);
        }
    }

    fn abort(&mut self, ctx: RpcContext<'_>, req: AbortRequest, sink: UnarySink<AbortResponse>) {
        unimplemented_call!(ctx, sink);
    }
}
