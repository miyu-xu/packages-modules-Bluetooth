use btstack::suspend::{ISuspend, ISuspendCallback, SuspendType};
use btstack::RPCProxy;
use dbus::channel::MatchingReceiver;
use dbus::message::MatchRule;
use dbus::nonblock::SyncConnection;
use dbus_crossroads::Crossroads;
use dbus_projection::DisconnectWatcher;
use manager_service::suspend::{
    RegisterSuspendDelayReply, RegisterSuspendDelayRequest, SuspendDone, SuspendImminent,
    SuspendImminent_Reason, SuspendReadinessInfo,
};
use protobuf::{CodedInputStream, CodedOutputStream, Message};
use std::sync::{Arc, Mutex};

use crate::dbus_iface::{export_suspend_callback_dbus_obj, SuspendDBus};
use crate::service_watcher::ServiceWatcher;

const POWERD_SERVICE: &str = "org.chromium.PowerManager";
const POWERD_INTERFACE: &str = "org.chromium.PowerManager";
const POWERD_PATH: &str = "/org/chromium/PowerManager";
const ADAPTER_SERVICE: &str = "org.chromium.bluetooth";
const ADAPTER_SUSPEND_INTERFACE: &str = "org.chromium.bluetooth.Suspend";
const SUSPEND_IMMINENT_SIGNAL: &str = "SuspendImminent";
const SUSPEND_DONE_SIGNAL: &str = "SuspendDone";

#[derive(Debug)]
enum SuspendManagerMessage {
    PowerdStarted,
    PowerdStopped,
    SuspendImminentReceived(SuspendImminent),
    SuspendDoneReceived(SuspendDone),
    AdapterFound(dbus::Path<'static>),
    AdapterRemoved,
}

struct PowerdSession {
    delay_id: i32,
    powerd_proxy: dbus::nonblock::Proxy<'static, Arc<SyncConnection>>,
}

/// Callback container for suspend interface callbacks.
pub(crate) struct SuspendCallback {
    objpath: String,

    dbus_connection: Arc<SyncConnection>,
    dbus_crossroads: Arc<Mutex<Crossroads>>,

    context: Arc<Mutex<SuspendManagerContext>>,
}

impl SuspendCallback {
    pub(crate) fn new(
        objpath: String,
        dbus_connection: Arc<SyncConnection>,
        dbus_crossroads: Arc<Mutex<Crossroads>>,
        context: Arc<Mutex<SuspendManagerContext>>,
    ) -> Self {
        Self { objpath, dbus_connection, dbus_crossroads, context }
    }
}

fn send_handle_suspend_readiness(
    powerd_proxy: dbus::nonblock::Proxy<'static, Arc<SyncConnection>>,
    delay_id: i32,
    suspend_id: i32,
) {
    let mut suspend_readiness_info = SuspendReadinessInfo::new();
    suspend_readiness_info.set_delay_id(delay_id);
    suspend_readiness_info.set_suspend_id(suspend_id);
    let mut suspend_readiness_info_proto: Vec<u8> = vec![];
    let mut output_stream = CodedOutputStream::vec(&mut suspend_readiness_info_proto);
    let write_result = suspend_readiness_info.write_to_with_cached_sizes(&mut output_stream);
    if write_result.is_err() {
        log::error!("Error writing SuspendReadinessInfo: {}", write_result.err().unwrap());
        return;
    }

    tokio::spawn(async move {
        log::debug!(
            "Sending HandleSuspendReadiness, delay id = {}, suspend id = {}",
            suspend_readiness_info.get_delay_id(),
            suspend_readiness_info.get_suspend_id()
        );
        let ret: Result<(), dbus::Error> = powerd_proxy
            .method_call(
                POWERD_INTERFACE,
                "HandleSuspendReadiness",
                (suspend_readiness_info_proto,),
            )
            .await;

        log::debug!("HandleSuspendReadiness returns {:?}", ret);
        if ret.is_err() {
            log::error!("Error calling HandleSuspendReadiness: {}", ret.err().unwrap())
        }
    });
}

impl ISuspendCallback for SuspendCallback {
    fn on_callback_registered(&self, callback_id: u32) {
        log::debug!("Suspend callback registered, callback_id = {}", callback_id);
    }

    fn on_suspend_ready(&self, suspend_id: u32) {
        log::debug!("Suspend ready, adapter suspend_id = {}", suspend_id);
        let context = self.context.lock().unwrap();
        if context.pending_suspend_imminent.is_none() {
            log::warn!("Suspend ready but no SuspendImminent signal");
            return;
        }

        send_handle_suspend_readiness(
            context.powerd_session.as_ref().unwrap().powerd_proxy.clone(),
            context.powerd_session.as_ref().unwrap().delay_id,
            context.pending_suspend_imminent.as_ref().unwrap().get_suspend_id(),
        );
    }

    fn on_resumed(&self, suspend_id: u32) {
        log::debug!("Suspend resumed, adapter suspend_id = {}", suspend_id);
    }
}

impl RPCProxy for SuspendCallback {
    fn register_disconnect(&mut self, _f: Box<dyn Fn(u32) + Send>) -> u32 {
        0
    }

    fn get_object_id(&self) -> String {
        self.objpath.clone()
    }

    fn unregister(&mut self, _id: u32) -> bool {
        false
    }

    fn export_for_rpc(self: Box<Self>) {
        let cr = self.dbus_crossroads.clone();
        export_suspend_callback_dbus_obj(
            self.get_object_id(),
            self.dbus_connection.clone(),
            &mut cr.lock().unwrap(),
            Arc::new(Mutex::new(self)),
            Arc::new(Mutex::new(DisconnectWatcher::new())),
        );
    }
}

pub struct SuspendManagerContext {
    dbus_crossroads: Arc<Mutex<Crossroads>>,
    powerd_session: Option<PowerdSession>,
    adapter_suspend_dbus: Option<SuspendDBus>,
    pending_suspend_imminent: Option<SuspendImminent>,
}

pub struct PowerdSuspendManager {
    context: Arc<Mutex<SuspendManagerContext>>,
    conn: Arc<SyncConnection>,
    tx: tokio::sync::mpsc::Sender<SuspendManagerMessage>,
    rx: tokio::sync::mpsc::Receiver<SuspendManagerMessage>,
}

impl PowerdSuspendManager {
    pub fn new(conn: Arc<SyncConnection>, dbus_crossroads: Arc<Mutex<Crossroads>>) -> Self {
        let (tx, rx) = tokio::sync::mpsc::channel::<SuspendManagerMessage>(10);
        Self {
            context: Arc::new(Mutex::new(SuspendManagerContext {
                dbus_crossroads,
                powerd_session: None,
                adapter_suspend_dbus: None,
                pending_suspend_imminent: None,
            })),
            conn,
            tx,
            rx,
        }
    }

    pub async fn init(&mut self) {
        let powerd_watcher = ServiceWatcher::new(self.conn.clone(), String::from(POWERD_SERVICE));
        let tx1 = self.tx.clone();
        let tx2 = self.tx.clone();
        powerd_watcher
            .start_watch(
                Box::new(move || {
                    let tx_clone = tx1.clone();
                    tokio::spawn(async move {
                        let _ = tx_clone.send(SuspendManagerMessage::PowerdStarted).await;
                    });
                }),
                Box::new(move || {
                    let tx_clone = tx2.clone();
                    tokio::spawn(async move {
                        let _ = tx_clone.send(SuspendManagerMessage::PowerdStopped).await;
                    });
                }),
            )
            .await;

        let adapter_watcher = ServiceWatcher::new(self.conn.clone(), String::from(ADAPTER_SERVICE));
        let tx1 = self.tx.clone();
        let tx2 = self.tx.clone();
        adapter_watcher
            .start_watch_interface(
                String::from(ADAPTER_SUSPEND_INTERFACE),
                Box::new(move |path| {
                    let tx_clone = tx1.clone();
                    tokio::spawn(async move {
                        let _ = tx_clone.send(SuspendManagerMessage::AdapterFound(path)).await;
                    });
                }),
                Box::new(move || {
                    let tx_clone = tx2.clone();
                    tokio::spawn(async move {
                        let _ = tx_clone.send(SuspendManagerMessage::AdapterRemoved).await;
                    });
                }),
            )
            .await;

        // SuspendImminent listener.
        let mr = MatchRule::new_signal(POWERD_INTERFACE, SUSPEND_IMMINENT_SIGNAL)
            .with_sender(POWERD_SERVICE)
            .with_path(POWERD_PATH);
        self.conn.add_match_no_cb(&mr.match_str()).await.unwrap();

        let tx = self.tx.clone();
        self.conn.start_receive(
            mr,
            Box::new(move |msg, _conn| {
                let arg = msg.get1::<Vec<u8>>();

                if arg.is_none() {
                    log::warn!("received empty SuspendImminent signal");
                }

                let bytes = arg.unwrap();
                let mut suspend_imminent = SuspendImminent::new();
                let mut input_stream = CodedInputStream::from_bytes(&bytes[..]);
                let decode_result = suspend_imminent.merge_from(&mut input_stream);
                if decode_result.is_err() {
                    log::error!("Error decoding SuspendImminent signal: {:?}", decode_result.err());
                }

                let tx_clone = tx.clone();
                tokio::spawn(async move {
                    let _ = tx_clone
                        .send(SuspendManagerMessage::SuspendImminentReceived(suspend_imminent))
                        .await;
                });

                true
            }),
        );

        // SuspendDone listener.
        let mr = MatchRule::new_signal(POWERD_INTERFACE, SUSPEND_DONE_SIGNAL)
            .with_sender(POWERD_SERVICE)
            .with_path(POWERD_PATH);
        self.conn.add_match_no_cb(&mr.match_str()).await.unwrap();
        let tx = self.tx.clone();
        self.conn.start_receive(
            mr,
            Box::new(move |msg, _conn| {
                let arg = msg.get1::<Vec<u8>>();

                if arg.is_none() {
                    log::warn!("received empty SuspendDone signal");
                }

                let bytes = arg.unwrap();
                let mut suspend_done = SuspendDone::new();
                let mut input_stream = CodedInputStream::from_bytes(&bytes[..]);
                let decode_result = suspend_done.merge_from(&mut input_stream);
                if decode_result.is_err() {
                    log::error!("Error decoding SuspendDone signal: {:?}", decode_result.err());
                }

                let tx_clone = tx.clone();
                tokio::spawn(async move {
                    let _ = tx_clone
                        .send(SuspendManagerMessage::SuspendDoneReceived(suspend_done))
                        .await;
                });

                true
            }),
        );
    }

    pub async fn mainloop(&mut self) {
        loop {
            let m = self.rx.recv().await;

            if m.is_none() {
                log::info!("Exiting suspend manager mainloop");
                break;
            }

            match m.unwrap() {
                SuspendManagerMessage::PowerdStarted => {
                    self.on_powerd_started();
                }
                SuspendManagerMessage::PowerdStopped => {
                    self.on_powerd_stopped();
                }
                SuspendManagerMessage::SuspendImminentReceived(suspend_imminent) => {
                    self.on_suspend_imminent(suspend_imminent);
                }
                SuspendManagerMessage::SuspendDoneReceived(suspend_done) => {
                    self.on_suspend_done(suspend_done);
                }
                SuspendManagerMessage::AdapterFound(object_path) => {
                    self.on_adapter_found(object_path);
                }
                SuspendManagerMessage::AdapterRemoved => {
                    self.on_adapter_removed();
                }
            }
        }
    }

    fn on_powerd_started(&mut self) {
        log::debug!("powerd started, initializing suspend manager");
        if self.context.lock().unwrap().powerd_session.is_some() {
            log::warn!("powerd session already exists, cleaning up first");
            self.on_powerd_stopped();
        }

        let conn = self.conn.clone();
        let powerd_proxy = dbus::nonblock::Proxy::new(
            POWERD_SERVICE,
            POWERD_PATH,
            std::time::Duration::from_secs(2),
            conn,
        );

        let mut request = RegisterSuspendDelayRequest::new();
        request.set_description(String::from("Bluetooth Manager"));
        let mut register_suspend_delay_proto: Vec<u8> = vec![];
        let mut output_stream = CodedOutputStream::vec(&mut register_suspend_delay_proto);
        let write_result = request.write_to_with_cached_sizes(&mut output_stream);
        if write_result.is_err() {
            log::error!(
                "Error writing RegisterSuspendDelayRequest: {}",
                write_result.err().unwrap()
            );
            return;
        }

        let return_proto: Result<(Vec<u8>,), dbus::Error> = futures::executor::block_on(async {
            powerd_proxy
                .method_call(
                    POWERD_INTERFACE,
                    "RegisterSuspendDelay",
                    (register_suspend_delay_proto,),
                )
                .await
        });

        if !return_proto.is_ok() {
            log::error!("D-Bus error: {:?}", return_proto.err().unwrap());
            return;
        }

        let (return_proto,) = return_proto.unwrap();
        let mut reply = RegisterSuspendDelayReply::new();
        let mut input_stream = CodedInputStream::from_bytes(&return_proto[..]);
        let decode_result = reply.merge_from(&mut input_stream);
        if decode_result.is_err() {
            log::error!("Error decoding RegisterSuspendDelayReply {:?}", decode_result.err());
        }

        log::debug!("Suspend delay id = {}", reply.get_delay_id());

        self.context.lock().unwrap().powerd_session =
            Some(PowerdSession { delay_id: reply.get_delay_id(), powerd_proxy });
    }

    fn on_powerd_stopped(&mut self) {
        // TODO: Consider an edge case where powerd unexpectedly is stopped (maybe crashes)  but we
        // still have pending SuspendImminent.
        log::debug!("powerd stopped, cleaning up");
        let mut context = self.context.lock().unwrap();

        if context.powerd_session.is_none() {
            log::warn!("powerd session does not exist, ignoring");
            return;
        }

        context.powerd_session = None;
    }

    fn on_suspend_imminent(&mut self, suspend_imminent: SuspendImminent) {
        log::debug!(
            "received suspend imminent: suspend_id = {:?}, reason = {:?}",
            suspend_imminent.get_suspend_id(),
            suspend_imminent.get_reason()
        );

        let mut context = self.context.lock().unwrap();
        if context.pending_suspend_imminent.is_some() {
            log::warn!("SuspendImminent signal received while there is a pending suspend imminent");
        }

        context.pending_suspend_imminent = Some(suspend_imminent.clone());

        if context.adapter_suspend_dbus.is_none() {
            log::debug!("Adapter not available, suspend is ready.");
            if context.powerd_session.is_none() {
                log::warn!("SuspendImminent is received when there is no powerd session");
                return;
            }

            send_handle_suspend_readiness(
                context.powerd_session.as_ref().unwrap().powerd_proxy.clone(),
                context.powerd_session.as_ref().unwrap().delay_id,
                suspend_imminent.get_suspend_id(),
            );

            return;
        }

        let adapter_suspend_id = context.adapter_suspend_dbus.as_mut().unwrap().suspend(
            match suspend_imminent.get_reason() {
                SuspendImminent_Reason::IDLE => SuspendType::AllowWakeFromHid,
                SuspendImminent_Reason::LID_CLOSED => SuspendType::NoWakesAllowed,
                SuspendImminent_Reason::OTHER => SuspendType::Other,
            },
        );

        log::debug!("Adapter suspend id = {}", adapter_suspend_id);
    }

    fn on_suspend_done(&mut self, suspend_done: SuspendDone) {
        log::debug!("SuspendDone received: {:?}", suspend_done);
        let mut context = self.context.lock().unwrap();

        if context.pending_suspend_imminent.is_none() {
            log::warn!("Receveid SuspendDone signal when there is no pending SuspendImminent");
        }

        context.pending_suspend_imminent = None;

        if context.adapter_suspend_dbus.is_none() {
            log::debug!("Adapter is not available, nothing to resume.");
            return;
        }

        let success = context.adapter_suspend_dbus.as_mut().unwrap().resume();
        log::debug!("Adapter resume successful = {}", success);
    }

    fn on_adapter_found(&mut self, path: dbus::Path<'static>) {
        log::debug!("Found adapter {:?}", path);
        let mut context = self.context.lock().unwrap();

        let conn = self.conn.clone();
        context.adapter_suspend_dbus = Some(SuspendDBus::new(conn.clone(), path));

        let suspend_cb_objpath: String =
            format!("/org/chromium/bluetooth/Manager/suspend_callback");
        let crossroads = context.dbus_crossroads.clone();
        context.adapter_suspend_dbus.as_mut().unwrap().register_callback(Box::new(
            SuspendCallback::new(suspend_cb_objpath, conn, crossroads, self.context.clone()),
        ));
    }

    fn on_adapter_removed(&mut self) {
        log::debug!("Adapter removed");
        self.context.lock().unwrap().adapter_suspend_dbus = None;
    }
}
