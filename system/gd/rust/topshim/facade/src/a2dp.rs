use bt_blueberry_protobuf::a2dp::{
    CloseRequest, CloseResponse, OpenSinkRequest, OpenSinkResponse, OpenSourceRequest,
    OpenSourceResponse, PlaybackAudioRequest, PlaybackAudioResponse, Sink, Source, StartRequest,
    StartResponse, SuspendRequest, SuspendResponse, WaitSinkRequest, WaitSinkResponse,
    WaitSourceRequest, WaitSourceResponse,
};
use bt_blueberry_protobuf::a2dp_grpc;
use bt_topshim::btif::{BluetoothInterface, RawAddress};
use bt_topshim::profiles::a2dp::{
    A2dp, A2dpCallbacks, A2dpCallbacksDispatcher, A2dpSink, A2dpSinkCallbacks,
    A2dpSinkCallbacksDispatcher, BtavConnectionState,
};
use bt_topshim::profiles::avrcp::{Avrcp, AvrcpCallbacksDispatcher};

use grpcio::{
    unimplemented_call, ClientStreamingSink, RequestStream, RpcContext, RpcStatus, RpcStatusCode,
    Service, UnarySink,
};

use std::future::{self, Future};
use std::os::unix::io::FromRawFd;
use std::os::unix::net::UnixStream as StdUnixStream;
use std::sync::{Arc, Mutex};

use tokio::io::AsyncWriteExt;
use tokio::net::UnixStream;
use tokio::pin;
use tokio::runtime::Handle;
use tokio::sync::broadcast;

use futures::stream::{unfold, Stream, StreamExt};

use nix::sys::socket::{connect, socket, AddressFamily, SockAddr, SockFlag, SockType, UnixAddr};

fn broadcast_stream<T: Clone>(rx: broadcast::Receiver<T>) -> impl Stream<Item = T> {
    unfold(rx, |mut rx| async move {
        if let Ok(value) = rx.recv().await {
            Some((value, rx))
        } else {
            None
        }
    })
}

fn get_a2dp_dispatcher(tx: broadcast::Sender<A2dpCallbacks>) -> A2dpCallbacksDispatcher {
    A2dpCallbacksDispatcher {
        dispatch: Box::new(move |cb| {
            println!("A2DP Event {:?}", cb);
            if let Err(cb) = tx.send(cb) {
                println!("Cannot send event {:?}", cb);
            }
        }),
    }
}

fn get_a2dp_sink_dispatcher(
    tx: broadcast::Sender<A2dpSinkCallbacks>,
) -> A2dpSinkCallbacksDispatcher {
    A2dpSinkCallbacksDispatcher {
        dispatch: Box::new(move |cb| {
            println!("\n\nA2DP Sink Event {:?}\n\n", cb);
            if let Err(cb) = tx.send(cb) {
                println!("Cannot send event {:?}", cb);
            }
        }),
    }
}

fn get_avrcp_dispatcher() -> AvrcpCallbacksDispatcher {
    AvrcpCallbacksDispatcher { dispatch: Box::new(move |_cb| {}) }
}

fn send_result<T: Send + 'static>(
    ctx: RpcContext<'_>,
    sink: UnarySink<T>,
    result: impl Future<Output = Result<T, &'static str>> + Send + 'static,
) {
    ctx.spawn(async move {
        match result.await {
            Ok(value) => sink.success(value),
            Err(details) => {
                sink.fail(RpcStatus::with_message(RpcStatusCode::UNKNOWN, details.to_owned()))
            }
        }
        .await
        .unwrap();
    })
}

pub enum State {
    Init(Option<(A2dp, A2dpSink)>),
    Source(A2dp, broadcast::Sender<A2dpCallbacks>),
    Sink(A2dpSink, broadcast::Sender<A2dpSinkCallbacks>),
}

impl State {
    fn source(&mut self) -> Result<(&mut A2dp, &broadcast::Sender<A2dpCallbacks>), &'static str> {
        match self {
            Self::Init(value) => {
                let (events, _) = broadcast::channel(10);
                let (mut source, _) = value.take().unwrap();
                source.initialize(get_a2dp_dispatcher(events.clone()));
                *self = State::Source(source, events);
                self.source()
            }
            Self::Source(source, events) => Ok((source, events)),
            Self::Sink(..) => Err("already initialized as a source"),
        }
    }

    fn sink(
        &mut self,
    ) -> Result<(&mut A2dpSink, &broadcast::Sender<A2dpSinkCallbacks>), &'static str> {
        match self {
            Self::Init(value) => {
                let (events, _) = broadcast::channel(10);
                let (_, mut sink) = value.take().unwrap();
                println!("\n\nInitialize sink\n\n");
                sink.initialize(get_a2dp_sink_dispatcher(events.clone()));
                *self = State::Sink(sink, events);
                self.sink()
            }
            Self::Sink(sink, events) => Ok((sink, events)),
            Self::Source(..) => Err("already initialized as a sink"),
        }
    }
}

#[derive(Clone)]
pub struct A2DP {
    handle: Handle,
    state: Arc<Mutex<State>>,
}

impl From<A2DP> for Service {
    fn from(a2dp: A2DP) -> Self {
        a2dp_grpc::create_a2_dp(a2dp)
    }
}

impl A2DP {
    pub fn initialize(handle: Handle, btif: &BluetoothInterface) -> Self {
        let a2dp = A2dp::new(btif);
        let a2dp_sink = A2dpSink::new(btif);

        let mut avrcp = Avrcp::new(btif);
        avrcp.initialize(get_avrcp_dispatcher());

        Self { handle, state: Arc::new(Mutex::new(State::Init(Some((a2dp, a2dp_sink))))) }
    }

    async fn wait_for_source(
        rx: broadcast::Receiver<A2dpCallbacks>,
        wanted_address: RawAddress,
    ) -> bool {
        let stream = broadcast_stream(rx);
        pin!(stream);

        let status = stream
            .filter_map(|event| {
                future::ready(match event {
                    A2dpCallbacks::ConnectionState(
                        address,
                        state @ BtavConnectionState::Connected
                        | state @ BtavConnectionState::Disconnected,
                    ) if address == wanted_address => Some(state),
                    _ => None,
                })
            })
            .next()
            .await;

        matches!(status, Some(BtavConnectionState::Connected))
    }

    async fn wait_for_sink(
        rx: broadcast::Receiver<A2dpSinkCallbacks>,
        wanted_address: RawAddress,
    ) -> bool {
        let stream = broadcast_stream(rx);
        pin!(stream);

        let status = stream
            .filter_map(|event| {
                future::ready(match event {
                    A2dpSinkCallbacks::ConnectionState(
                        address,
                        state @ BtavConnectionState::Connected
                        | state @ BtavConnectionState::Disconnected,
                    ) if address == wanted_address => Some(state),
                    _ => None,
                })
            })
            .next()
            .await;

        matches!(status, Some(BtavConnectionState::Connected))
    }
}

impl a2dp_grpc::A2Dp for A2DP {
    fn open_source(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: OpenSourceRequest,
        sink: UnarySink<OpenSourceResponse>,
    ) {
        let cookie = req.mut_connection().take_cookie();
        let addr = RawAddress::from_bytes(&cookie).unwrap();

        let state = self.state.clone();

        let connected = state.lock().unwrap().source().map(|(a2dp, events)| {
            a2dp.connect(addr);
            Self::wait_for_source(events.subscribe(), addr)
        });

        send_result(ctx, sink, async move {
            if connected?.await {
                let mut state = state.lock().unwrap();
                state.source().unwrap().0.set_active_device(addr);

                let mut response = OpenSourceResponse::new();
                let mut source = Source::new();
                source.set_cookie(cookie);
                response.set_source(source);

                Ok(response)
            } else {
                Err("Device disconnected")
            }
        })
    }

    fn wait_source(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: WaitSourceRequest,
        sink: UnarySink<WaitSourceResponse>,
    ) {
        let cookie = req.mut_connection().take_cookie();
        let addr = RawAddress::from_bytes(&cookie).unwrap();

        let state = self.state.clone();

        let connected = state
            .lock()
            .unwrap()
            .source()
            .map(|(_, events)| Self::wait_for_source(events.subscribe(), addr));

        send_result(ctx, sink, async move {
            if connected?.await {
                let mut state = state.lock().unwrap();
                state.source().unwrap().0.set_active_device(addr);

                let mut response = WaitSourceResponse::new();
                let mut source = Source::new();
                source.set_cookie(cookie);
                response.set_source(source);

                Ok(response)
            } else {
                Err("Device disconnected")
            }
        })
    }

    fn open_sink(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: OpenSinkRequest,
        sink: UnarySink<OpenSinkResponse>,
    ) {
        let cookie = req.mut_connection().take_cookie();
        let addr = RawAddress::from_bytes(&cookie).unwrap();

        let connected = self.state.lock().unwrap().sink().map(|(a2dp, events)| {
            a2dp.connect(addr);
            Self::wait_for_sink(events.subscribe(), addr)
        });

        send_result(ctx, sink, async move {
            if connected?.await {
                let mut response = OpenSinkResponse::new();
                let mut sink = Sink::new();
                sink.set_cookie(cookie);
                response.set_sink(sink);

                Ok(response)
            } else {
                Err("Device disconnected")
            }
        })
    }

    fn wait_sink(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: WaitSinkRequest,
        sink: UnarySink<WaitSinkResponse>,
    ) {
        let cookie = req.mut_connection().take_cookie();
        let addr = RawAddress::from_bytes(&cookie).unwrap();

        let connected = self
            .state
            .lock()
            .unwrap()
            .sink()
            .map(|(_, events)| Self::wait_for_sink(events.subscribe(), addr));

        send_result(ctx, sink, async move {
            if connected?.await {
                let mut response = WaitSinkResponse::new();
                let mut sink = Sink::new();
                sink.set_cookie(cookie);
                response.set_sink(sink);

                Ok(response)
            } else {
                Err("Device disconnected")
            }
        })
    }

    fn start(&mut self, ctx: RpcContext<'_>, req: StartRequest, sink: UnarySink<StartResponse>) {
        let mut state = self.state.lock().unwrap();

        if req.has_source() {
            let result = state.source().map(|(a2dp, _)| {
                a2dp.start_audio_request();

                StartResponse::new()
            });
            send_result(ctx, sink, future::ready(result));
        } else {
            unimplemented_call!(ctx, sink);
        };
    }

    fn suspend(
        &mut self,
        ctx: RpcContext<'_>,
        req: SuspendRequest,
        sink: UnarySink<SuspendResponse>,
    ) {
        let mut state = self.state.lock().unwrap();

        if req.has_source() {
            let result = state.source().map(|(a2dp, _)| {
                a2dp.stop_audio_request();

                SuspendResponse::new()
            });
            send_result(ctx, sink, future::ready(result));
        } else {
            unimplemented_call!(ctx, sink);
        };
    }

    fn close(
        &mut self,
        ctx: RpcContext<'_>,
        mut req: CloseRequest,
        sink: UnarySink<CloseResponse>,
    ) {
        let mut state = self.state.lock().unwrap();

        let result = if req.has_source() {
            state.source().map(|(a2dp, _)| {
                let cookie = req.mut_source().take_cookie();
                let addr = RawAddress::from_bytes(&cookie).unwrap();

                a2dp.disconnect(addr);

                CloseResponse::new()
            })
        } else {
            state.sink().map(|(a2dp, _)| {
                let cookie = req.mut_sink().take_cookie();
                let addr = RawAddress::from_bytes(&cookie).unwrap();

                a2dp.disconnect(addr);

                CloseResponse::new()
            })
        };

        send_result(ctx, sink, future::ready(result));
    }

    fn playback_audio(
        &mut self,
        _ctx: RpcContext<'_>,
        mut stream: RequestStream<PlaybackAudioRequest>,
        sink: ClientStreamingSink<PlaybackAudioResponse>,
    ) {
        self.handle.spawn(async move {
            let fd = socket(AddressFamily::Unix, SockType::Stream, SockFlag::SOCK_CLOEXEC, None)
                .unwrap();
            let addr = SockAddr::Unix(
                UnixAddr::new_abstract(b"/var/run/bluetooth/audio/.a2dp_data").unwrap(),
            );
            connect(fd, &addr).unwrap();
            let mut a2dp_data =
                UnixStream::from_std(unsafe { StdUnixStream::from_raw_fd(fd) }).unwrap();

            while let Some(req) = stream.next().await {
                if let Err(e) = a2dp_data.write_all(&*req.unwrap().data).await {
                    eprintln!("PlaybackAudio err {:?}", e);
                    break;
                }
            }
            sink.success(PlaybackAudioResponse::new()).await.unwrap();
        });
    }
}
