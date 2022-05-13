use crate::bluetooth_manager::BluetoothManager;
use crate::config_util;
use bt_common::time::Alarm;
use bt_socket::{BtSocket, HciChannels, MgmtCommand, MgmtCommandResponse, MgmtEvent, HCI_DEV_NONE};
use log::{debug, error, info, warn};
use nix::sys::signal::{self, Signal};
use nix::unistd::Pid;
use regex::Regex;
use std::convert::TryFrom;
use std::process::{Child, Command, Stdio};
use std::sync::Arc;
use tokio::io::unix::AsyncFd;
use tokio::sync::mpsc;
use tokio::time::Duration;

// Directory for Bluetooth pid file
pub const PID_DIR: &str = "/var/run/bluetooth";

#[derive(Debug, PartialEq, Copy, Clone)]
#[repr(u32)]
pub enum State {
    Off = 0,        // Bluetooth is not running
    TurningOn = 1,  // We are not notified that the Bluetooth is running
    On = 2,         // Bluetooth is running
    TurningOff = 3, // We are not notified that the Bluetooth is stopped
}

/// Check whether adapter is enabled by checking internal state.
pub fn state_to_enabled(state: State) -> bool {
    match state {
        State::On => true,
        _ => false,
    }
}

/// Adapter state actions
#[derive(Debug)]
pub enum AdapterStateActions {
    StartBluetooth(i32),
    StopBluetooth(i32),
    BluetoothStarted(i32, i32), // PID and HCI
    BluetoothStopped(i32),
}

/// Enum of all the messages that state machine handles.
#[derive(Debug)]
pub enum Message {
    AdapterStateChange(AdapterStateActions),
    PidChange(inotify::EventMask, Option<String>),
    HciDeviceAdded(u16),
    HciDeviceRemoved(u16),
    CallbackDisconnected(u32),
    CommandTimeout(),
}

pub struct StateMachineContext {
    tx: mpsc::Sender<Message>,
    rx: mpsc::Receiver<Message>,
    state_machine: ManagerStateMachine,
}

impl StateMachineContext {
    fn new(state_machine: ManagerStateMachine) -> StateMachineContext {
        let (tx, rx) = mpsc::channel::<Message>(10);
        StateMachineContext { tx: tx, rx: rx, state_machine: state_machine }
    }

    pub fn get_proxy(&self) -> StateMachineProxy {
        StateMachineProxy { tx: self.tx.clone(), state: self.state_machine.state.clone() }
    }
}

pub fn start_new_state_machine_context(invoker: Invoker) -> StateMachineContext {
    match invoker {
        Invoker::NativeInvoker => StateMachineContext::new(ManagerStateMachine::new_native()),
        Invoker::SystemdInvoker => StateMachineContext::new(ManagerStateMachine::new_systemd()),
        Invoker::UpstartInvoker => StateMachineContext::new(ManagerStateMachine::new_upstart()),
    }
}

#[derive(Clone)]
pub struct StateMachineProxy {
    tx: mpsc::Sender<Message>,
    state: Arc<std::sync::Mutex<State>>,
}

const TX_SEND_TIMEOUT_DURATION: Duration = Duration::from_secs(3);
const COMMAND_TIMEOUT_DURATION: Duration = Duration::from_secs(3);

impl StateMachineProxy {
    pub fn start_bluetooth(&self, hci_interface: i32) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx
                .send(Message::AdapterStateChange(AdapterStateActions::StartBluetooth(
                    hci_interface,
                )))
                .await;
        });
    }

    pub fn stop_bluetooth(&self, hci_interface: i32) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx
                .send(Message::AdapterStateChange(AdapterStateActions::StopBluetooth(
                    hci_interface,
                )))
                .await;
        });
    }

    pub fn get_state(&self) -> State {
        // This assumes that self.state is never locked for a long period, i.e. never lock() and
        // await for something else without unlocking. Otherwise this function will block.
        return *self.state.lock().unwrap();
    }

    pub fn get_tx(&self) -> mpsc::Sender<Message> {
        self.tx.clone()
    }
}

fn pid_inotify_async_fd() -> AsyncFd<inotify::Inotify> {
    let mut pid_detector = inotify::Inotify::init().expect("cannot use inotify");
    pid_detector
        .add_watch(PID_DIR, inotify::WatchMask::CREATE | inotify::WatchMask::DELETE)
        .expect("failed to add watch on pid directory");
    AsyncFd::new(pid_detector).expect("failed to add async fd for pid detector")
}

/// Given an pid path, returns the adapter index for that pid path.
fn get_hci_index_from_pid_path(path: &str) -> Option<i32> {
    let re = Regex::new(r"bluetooth([0-9]+).pid").unwrap();
    re.captures(path)?.get(1)?.as_str().parse().ok()
}

fn event_name_to_string(name: Option<&std::ffi::OsStr>) -> Option<String> {
    if let Some(val) = &name {
        if let Some(strval) = val.to_str() {
            return Some(strval.to_string());
        }
    }

    return None;
}

// List existing pids and then configure inotify on pid dir.
fn configure_pid(pid_tx: mpsc::Sender<Message>) {
    // Configure PID listener.
    tokio::spawn(async move {
        debug!("Spawned pid notify task");

        // Get a list of active pid files to determine initial adapter status
        let files = config_util::list_pid_files(PID_DIR);
        for file in files {
            let _ = pid_tx
                .send_timeout(
                    Message::PidChange(inotify::EventMask::CREATE, Some(file)),
                    TX_SEND_TIMEOUT_DURATION,
                )
                .await
                .unwrap();
        }

        // Set up a PID file listener to emit PID inotify messages
        let mut pid_async_fd = pid_inotify_async_fd();

        loop {
            let r = pid_async_fd.readable_mut();
            let mut fd_ready = r.await.unwrap();
            let mut buffer: [u8; 1024] = [0; 1024];
            debug!("Found new pid inotify entries. Reading them");
            match fd_ready.try_io(|inner| inner.get_mut().read_events(&mut buffer)) {
                Ok(Ok(events)) => {
                    for event in events {
                        debug!("got some events from pid {:?}", event.mask);
                        let _ = pid_tx
                            .send_timeout(
                                Message::PidChange(event.mask, event_name_to_string(event.name)),
                                TX_SEND_TIMEOUT_DURATION,
                            )
                            .await
                            .unwrap();
                    }
                }
                Err(_) | Ok(Err(_)) => panic!("Inotify watcher on {} failed.", PID_DIR),
            }
            fd_ready.clear_ready();
            drop(fd_ready);
        }
    });
}

async fn start_hci_if_floss_enabled(hci: u16, floss_enabled: bool, tx: mpsc::Sender<Message>) {
    // Initialize adapter states based on saved config only if floss is enabled.
    if floss_enabled {
        let is_enabled = config_util::is_hci_n_enabled(hci.into());
        debug!("Start hci {}: floss={}, enabled={}", hci, floss_enabled, is_enabled);

        if is_enabled {
            let _ = tx
                .send_timeout(
                    Message::AdapterStateChange(AdapterStateActions::StartBluetooth(hci.into())),
                    TX_SEND_TIMEOUT_DURATION,
                )
                .await
                .unwrap();
        }
    }
}

// Configure the HCI socket listener and prepare the system to receive mgmt events for index added
// and index removed.
fn configure_hci(
    hci_tx: mpsc::Sender<Message>,
    bluetooth_manager: Arc<std::sync::Mutex<Box<BluetoothManager>>>,
) {
    let mut btsock = BtSocket::new();

    // If the bluetooth socket isn't available, the kernel module is not loaded and we can't
    // actually listen to it for index added/removed events.
    match btsock.open() {
        -1 => {
            panic!(
                "Bluetooth socket unavailable (errno {}). Try loading the kernel module first.",
                std::io::Error::last_os_error().raw_os_error().unwrap_or(0)
            );
        }
        x => debug!("Socket open at fd: {}", x),
    }

    // Bind to control channel (which is used for mgmt commands). We provide
    // HCI_DEV_NONE because we don't actually need a valid HCI dev for some MGMT commands.
    match btsock.bind_channel(HciChannels::Control, HCI_DEV_NONE) {
        -1 => {
            panic!(
                "Failed to bind control channel with errno={}",
                std::io::Error::last_os_error().raw_os_error().unwrap_or(0)
            );
        }
        _ => (),
    };

    tokio::spawn(async move {
        debug!("Spawned hci notify task");

        // Make this into an AsyncFD and start using it for IO
        let mut hci_afd = AsyncFd::new(btsock).expect("Failed to add async fd for BT socket.");

        // Start by first reading the index list
        match hci_afd.writable_mut().await {
            Ok(mut guard) => {
                let _ = guard.try_io(|sock| {
                    let command = MgmtCommand::ReadIndexList;
                    sock.get_mut().write_mgmt_packet(command.into());
                    Ok(())
                });
            }
            Err(e) => debug!("Failed to write to hci socket: {:?}", e),
        };

        let floss_enabled = bluetooth_manager.lock().unwrap().get_floss_enabled_internal();

        // Now listen only for devices that are newly added or removed.
        loop {
            if let Ok(mut guard) = hci_afd.readable_mut().await {
                let result = guard.try_io(|sock| Ok(sock.get_mut().read_mgmt_packet()));
                let packet = match result {
                    Ok(v) => v.unwrap_or(None),
                    Err(_) => None,
                };

                if let Some(p) = packet {
                    debug!("Got a valid packet from btsocket: {:?}", p);

                    if let Ok(ev) = MgmtEvent::try_from(p) {
                        debug!("Got a valid mgmt event: {:?}", ev);

                        match ev {
                            MgmtEvent::CommandComplete { opcode: _, status: _, response } => {
                                if let MgmtCommandResponse::ReadIndexList {
                                    num_intf: _,
                                    interfaces,
                                } = response
                                {
                                    for hci in interfaces {
                                        debug!("IndexList response: {}", hci);

                                        let _ = hci_tx
                                            .send_timeout(
                                                Message::HciDeviceAdded(hci),
                                                TX_SEND_TIMEOUT_DURATION,
                                            )
                                            .await
                                            .unwrap();

                                        // With a list of initial hci devices, make sure to
                                        // enable them if they were previously enabled and we
                                        // are using floss.
                                        start_hci_if_floss_enabled(
                                            hci,
                                            floss_enabled,
                                            hci_tx.clone(),
                                        )
                                        .await;
                                    }
                                }
                            }
                            MgmtEvent::IndexAdded(hci) => {
                                let _ = hci_tx
                                    .send_timeout(
                                        Message::HciDeviceAdded(hci),
                                        TX_SEND_TIMEOUT_DURATION,
                                    )
                                    .await
                                    .unwrap();
                            }
                            MgmtEvent::IndexRemoved(hci) => {
                                let _ = hci_tx
                                    .send_timeout(
                                        Message::HciDeviceRemoved(hci),
                                        TX_SEND_TIMEOUT_DURATION,
                                    )
                                    .await
                                    .unwrap();
                            }
                        }
                    }
                } else {
                    // Got nothing from the previous read so clear the ready bit.
                    guard.clear_ready();
                }
            }
        }
    });
}

pub async fn mainloop(
    mut context: StateMachineContext,
    bluetooth_manager: Arc<std::sync::Mutex<Box<BluetoothManager>>>,
) {
    // Set up a command timeout listener to emit timeout messages
    let command_timeout = Arc::new(Alarm::new());
    let timeout_clone = command_timeout.clone();
    let timeout_tx = context.tx.clone();
    tokio::spawn(async move {
        loop {
            let _expired = timeout_clone.expired().await;
            let _ = timeout_tx
                .send_timeout(Message::CommandTimeout(), TX_SEND_TIMEOUT_DURATION)
                .await
                .unwrap();
        }
    });

    // Set up an HCI device listener to emit HCI device inotify messages.
    // This is also responsible for configuring the initial list of HCI devices available on the
    // system.
    configure_hci(context.tx.clone(), bluetooth_manager.clone());
    configure_pid(context.tx.clone());

    // Listen for all messages and act on them
    loop {
        let m = context.rx.recv().await;

        if m.is_none() {
            info!("Exiting manager mainloop");
            break;
        }

        debug!("Message handler: {:?}", m);

        match m.unwrap() {
            // Adapter action has changed
            Message::AdapterStateChange(action) => {
                // Grab previous state from lock and release
                let next_state;
                let prev_state;
                {
                    prev_state = *context.state_machine.state.lock().unwrap();
                }
                let hci;

                match action {
                    AdapterStateActions::StartBluetooth(i) => {
                        next_state = State::TurningOn;
                        hci = i;

                        match context.state_machine.action_start_bluetooth(i) {
                            true => {
                                command_timeout.reset(COMMAND_TIMEOUT_DURATION);
                            }
                            false => command_timeout.cancel(),
                        }
                    }
                    AdapterStateActions::StopBluetooth(i) => {
                        next_state = State::TurningOff;
                        hci = i;

                        match context.state_machine.action_stop_bluetooth(i) {
                            true => {
                                command_timeout.reset(COMMAND_TIMEOUT_DURATION);
                            }
                            false => command_timeout.cancel(),
                        }
                    }
                    AdapterStateActions::BluetoothStarted(pid, i) => {
                        next_state = State::On;
                        hci = i;

                        match context.state_machine.action_on_bluetooth_started(pid, hci) {
                            true => {
                                command_timeout.cancel();
                            }
                            false => warn!("unexpected BluetoothStarted pid{} hci{}", pid, hci),
                        }
                    }
                    AdapterStateActions::BluetoothStopped(i) => {
                        next_state = State::Off;
                        hci = i;

                        match context.state_machine.action_on_bluetooth_stopped() {
                            true => {
                                command_timeout.cancel();
                            }
                            false => {
                                command_timeout.reset(COMMAND_TIMEOUT_DURATION);
                            }
                        }
                    }
                };

                // Only emit enabled event for certain transitions
                if next_state != prev_state && (next_state == State::On || prev_state == State::On)
                {
                    bluetooth_manager
                        .lock()
                        .unwrap()
                        .callback_hci_enabled_change(hci, next_state == State::On);
                }
            }

            // Monitored pid directory has a change
            Message::PidChange(mask, filename) => match (mask, &filename) {
                (inotify::EventMask::CREATE, Some(fname)) => {
                    let path = std::path::Path::new(PID_DIR).join(&fname);
                    match (get_hci_index_from_pid_path(&fname), tokio::fs::read(path).await.ok()) {
                        (Some(hci), Some(s)) => {
                            let pid = String::from_utf8(s)
                                .expect("invalid pid file")
                                .parse::<i32>()
                                .unwrap_or(0);
                            debug!("Sending bluetooth started action for pid={}, hci={}", pid, hci);
                            let _ = context
                                .tx
                                .send_timeout(
                                    Message::AdapterStateChange(
                                        AdapterStateActions::BluetoothStarted(pid, hci),
                                    ),
                                    TX_SEND_TIMEOUT_DURATION,
                                )
                                .await
                                .unwrap();
                        }
                        _ => debug!("Invalid pid path: {}", fname),
                    }
                }
                (inotify::EventMask::DELETE, Some(fname)) => {
                    if let Some(hci) = get_hci_index_from_pid_path(&fname) {
                        debug!("Sending bluetooth stopped action for hci={}", hci);
                        context
                            .tx
                            .send_timeout(
                                Message::AdapterStateChange(AdapterStateActions::BluetoothStopped(
                                    hci,
                                )),
                                TX_SEND_TIMEOUT_DURATION,
                            )
                            .await
                            .unwrap();
                    }
                }
                _ => debug!("Ignored event {:?} - {:?}", mask, &filename),
            },

            Message::HciDeviceAdded(hci) => {
                bluetooth_manager.lock().unwrap().callback_hci_device_change(hci.into(), true)
            }
            Message::HciDeviceRemoved(hci) => {
                bluetooth_manager.lock().unwrap().callback_hci_device_change(hci.into(), false)
            }

            // Callback client has disconnected
            Message::CallbackDisconnected(id) => {
                bluetooth_manager.lock().unwrap().callback_disconnected(id);
            }

            // Handle command timeouts
            Message::CommandTimeout() => {
                // Hold state lock for short duration
                {
                    debug!("expired {:?}", *context.state_machine.state.lock().unwrap());
                }
                let timeout_action = context.state_machine.action_on_command_timeout();
                match timeout_action {
                    StateMachineTimeoutActions::Noop => (),
                    _ => command_timeout.reset(COMMAND_TIMEOUT_DURATION),
                }
            }
        }
    }
}

pub trait ProcessManager {
    fn start(&mut self, hci_interface: String);
    fn stop(&mut self, hci_interface: String);
}

pub enum Invoker {
    #[allow(dead_code)]
    NativeInvoker,
    SystemdInvoker,
    UpstartInvoker,
}

pub struct NativeInvoker {
    process_container: Option<Child>,
    bluetooth_pid: u32,
}

impl NativeInvoker {
    pub fn new() -> NativeInvoker {
        NativeInvoker { process_container: None, bluetooth_pid: 0 }
    }
}

impl ProcessManager for NativeInvoker {
    fn start(&mut self, hci_interface: String) {
        let new_process = Command::new("/usr/bin/btadapterd")
            .arg(format!("HCI={}", hci_interface))
            .stdout(Stdio::piped())
            .spawn()
            .expect("cannot open");
        self.bluetooth_pid = new_process.id();
        self.process_container = Some(new_process);
    }
    fn stop(&mut self, _hci_interface: String) {
        match self.process_container {
            Some(ref mut _p) => {
                signal::kill(Pid::from_raw(self.bluetooth_pid as i32), Signal::SIGTERM).unwrap();
                self.process_container = None;
            }
            None => {
                warn!("Process doesn't exist");
            }
        }
    }
}

pub struct UpstartInvoker {}

impl UpstartInvoker {
    pub fn new() -> UpstartInvoker {
        UpstartInvoker {}
    }
}

impl ProcessManager for UpstartInvoker {
    fn start(&mut self, hci_interface: String) {
        if let Err(e) = Command::new("initctl")
            .args(&["start", "btadapterd", format!("HCI={}", hci_interface).as_str()])
            .output()
        {
            error!("Failed to start btadapterd: {}", e);
        }
    }

    fn stop(&mut self, hci_interface: String) {
        if let Err(e) = Command::new("initctl")
            .args(&["stop", "btadapterd", format!("HCI={}", hci_interface).as_str()])
            .output()
        {
            error!("Failed to stop btadapterd: {}", e);
        }
    }
}

pub struct SystemdInvoker {}

impl SystemdInvoker {
    pub fn new() -> SystemdInvoker {
        SystemdInvoker {}
    }
}

impl ProcessManager for SystemdInvoker {
    fn start(&mut self, hci_interface: String) {
        Command::new("systemctl")
            .args(&["restart", format!("btadapterd@{}.service", hci_interface).as_str()])
            .output()
            .expect("failed to start bluetooth");
    }

    fn stop(&mut self, hci_interface: String) {
        Command::new("systemctl")
            .args(&["stop", format!("btadapterd@{}.service", hci_interface).as_str()])
            .output()
            .expect("failed to stop bluetooth");
    }
}

struct ManagerStateMachine {
    state: Arc<std::sync::Mutex<State>>,
    process_manager: Box<dyn ProcessManager + Send>,
    hci_interface: i32,
    bluetooth_pid: i32,
}

impl ManagerStateMachine {
    pub fn new_upstart() -> ManagerStateMachine {
        ManagerStateMachine::new(Box::new(UpstartInvoker::new()))
    }

    pub fn new_systemd() -> ManagerStateMachine {
        ManagerStateMachine::new(Box::new(SystemdInvoker::new()))
    }

    pub fn new_native() -> ManagerStateMachine {
        ManagerStateMachine::new(Box::new(NativeInvoker::new()))
    }
}

#[derive(Debug, PartialEq)]
enum StateMachineTimeoutActions {
    RetryStart,
    RetryStop,
    Noop,
}

impl ManagerStateMachine {
    pub fn new(process_manager: Box<dyn ProcessManager + Send>) -> ManagerStateMachine {
        ManagerStateMachine {
            state: Arc::new(std::sync::Mutex::new(State::Off)),
            process_manager: process_manager,
            hci_interface: 0,
            bluetooth_pid: 0,
        }
    }

    /// Returns true if we are starting bluetooth process.
    pub fn action_start_bluetooth(&mut self, hci_interface: i32) -> bool {
        let mut state = self.state.lock().unwrap();
        match *state {
            State::Off => {
                *state = State::TurningOn;
                self.hci_interface = hci_interface;
                self.process_manager.start(format!("{}", hci_interface));
                true
            }
            // Otherwise no op
            _ => false,
        }
    }

    /// Returns true if we are stopping bluetooth process.
    pub fn action_stop_bluetooth(&mut self, hci_interface: i32) -> bool {
        if self.hci_interface != hci_interface {
            warn!(
                "We are running hci{} but attempting to stop hci{}",
                self.hci_interface, hci_interface
            );
            return false;
        }

        let mut state = self.state.lock().unwrap();
        match *state {
            State::On => {
                *state = State::TurningOff;
                self.process_manager.stop(self.hci_interface.to_string());
                true
            }
            State::TurningOn => {
                *state = State::Off;
                self.process_manager.stop(self.hci_interface.to_string());
                false
            }
            // Otherwise no op
            _ => false,
        }
    }

    /// Returns true if the event is expected.
    pub fn action_on_bluetooth_started(&mut self, pid: i32, hci_interface: i32) -> bool {
        let mut state = self.state.lock().unwrap();
        if self.hci_interface != hci_interface {
            warn!(
                "We should start hci{} but hci{} is started; capturing that process",
                self.hci_interface, hci_interface
            );
            self.hci_interface = hci_interface;
        }
        if *state != State::TurningOn {
            warn!("Unexpected Bluetooth started");
        }
        *state = State::On;
        self.bluetooth_pid = pid;
        true
    }

    /// Returns true if the event is expected.
    /// If unexpected, Bluetooth probably crashed;
    /// start the timer for restart timeout
    pub fn action_on_bluetooth_stopped(&mut self) -> bool {
        let mut state = self.state.lock().unwrap();

        match *state {
            State::TurningOff => {
                *state = State::Off;
                true
            }
            State::On => {
                warn!("Bluetooth stopped unexpectedly, try restarting");
                *state = State::TurningOn;
                self.process_manager.start(format!("{}", self.hci_interface));
                false
            }
            State::TurningOn | State::Off => {
                // Unexpected
                panic!("unexpected bluetooth shutdown");
            }
        }
    }

    /// Triggered on Bluetooth start/stop timeout.  Return the actions that the
    /// state machine has taken, for the external context to reset the timer.
    pub fn action_on_command_timeout(&mut self) -> StateMachineTimeoutActions {
        let mut state = self.state.lock().unwrap();
        match *state {
            State::TurningOn => {
                info!("Restarting bluetooth {}", self.hci_interface);
                *state = State::TurningOn;
                self.process_manager.stop(format! {"{}", self.hci_interface});
                self.process_manager.start(format! {"{}", self.hci_interface});
                StateMachineTimeoutActions::RetryStart
            }
            State::TurningOff => {
                info!("Killing bluetooth {}", self.hci_interface);
                self.process_manager.stop(format! {"{}", self.hci_interface});
                StateMachineTimeoutActions::RetryStop
            }
            _ => StateMachineTimeoutActions::Noop,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::VecDeque;

    #[derive(Debug, PartialEq)]
    enum ExecutedCommand {
        Start,
        Stop,
    }

    struct MockProcessManager {
        last_command: VecDeque<ExecutedCommand>,
    }

    impl MockProcessManager {
        fn new() -> MockProcessManager {
            MockProcessManager { last_command: VecDeque::new() }
        }

        fn expect_start(&mut self) {
            self.last_command.push_back(ExecutedCommand::Start);
        }

        fn expect_stop(&mut self) {
            self.last_command.push_back(ExecutedCommand::Stop);
        }
    }

    impl ProcessManager for MockProcessManager {
        fn start(&mut self, _: String) {
            let start = self.last_command.pop_front().expect("Should expect start event");
            assert_eq!(start, ExecutedCommand::Start);
        }

        fn stop(&mut self, _: String) {
            let stop = self.last_command.pop_front().expect("Should expect stop event");
            assert_eq!(stop, ExecutedCommand::Stop);
        }
    }

    impl Drop for MockProcessManager {
        fn drop(&mut self) {
            assert_eq!(self.last_command.len(), 0);
        }
    }

    #[test]
    fn initial_state_is_off() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let process_manager = MockProcessManager::new();
            let state_machine = ManagerStateMachine::new(Box::new(process_manager));
            assert_eq!(*state_machine.state.lock().unwrap(), State::Off);
        })
    }

    #[test]
    fn off_turnoff_should_noop() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let process_manager = MockProcessManager::new();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_stop_bluetooth(0);
            assert_eq!(*state_machine.state.lock().unwrap(), State::Off);
        })
    }

    #[test]
    fn off_turnon_should_turningon() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            // Expect to send start command
            process_manager.expect_start();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            assert_eq!(*state_machine.state.lock().unwrap(), State::TurningOn);
        })
    }

    #[test]
    fn turningon_turnon_again_noop() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            // Expect to send start command just once
            process_manager.expect_start();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            assert_eq!(state_machine.action_start_bluetooth(0), false);
        })
    }

    #[test]
    fn turningon_bluetooth_started() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            state_machine.action_on_bluetooth_started(0, 0);
            assert_eq!(*state_machine.state.lock().unwrap(), State::On);
        })
    }

    #[test]
    fn turningon_bluetooth_different_hci_started() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(1);
            state_machine.action_on_bluetooth_started(1, 1);
            assert_eq!(*state_machine.state.lock().unwrap(), State::On);
        })
    }

    #[test]
    fn turningon_timeout() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            process_manager.expect_stop();
            process_manager.expect_start(); // start bluetooth again
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            assert_eq!(
                state_machine.action_on_command_timeout(),
                StateMachineTimeoutActions::RetryStart
            );
            assert_eq!(*state_machine.state.lock().unwrap(), State::TurningOn);
        })
    }

    #[test]
    fn turningon_turnoff_should_turningoff_and_send_command() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            // Expect to send stop command
            process_manager.expect_stop();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            state_machine.action_stop_bluetooth(0);
            assert_eq!(*state_machine.state.lock().unwrap(), State::Off);
        })
    }

    #[test]
    fn on_turnoff_should_turningoff_and_send_command() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            // Expect to send stop command
            process_manager.expect_stop();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            state_machine.action_on_bluetooth_started(0, 0);
            state_machine.action_stop_bluetooth(0);
            assert_eq!(*state_machine.state.lock().unwrap(), State::TurningOff);
        })
    }

    #[test]
    fn on_bluetooth_stopped() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            // Expect to start again
            process_manager.expect_start();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            state_machine.action_on_bluetooth_started(0, 0);
            assert_eq!(state_machine.action_on_bluetooth_stopped(), false);
            assert_eq!(*state_machine.state.lock().unwrap(), State::TurningOn);
        })
    }

    #[test]
    fn turningoff_bluetooth_down_should_off() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            process_manager.expect_stop();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            state_machine.action_on_bluetooth_started(0, 0);
            state_machine.action_stop_bluetooth(0);
            state_machine.action_on_bluetooth_stopped();
            assert_eq!(*state_machine.state.lock().unwrap(), State::Off);
        })
    }

    #[test]
    fn restart_bluetooth() {
        tokio::runtime::Runtime::new().unwrap().block_on(async {
            let mut process_manager = MockProcessManager::new();
            process_manager.expect_start();
            process_manager.expect_stop();
            process_manager.expect_start();
            let mut state_machine = ManagerStateMachine::new(Box::new(process_manager));
            state_machine.action_start_bluetooth(0);
            state_machine.action_on_bluetooth_started(0, 0);
            state_machine.action_stop_bluetooth(0);
            state_machine.action_on_bluetooth_stopped();
            state_machine.action_start_bluetooth(0);
            state_machine.action_on_bluetooth_started(0, 0);
            assert_eq!(*state_machine.state.lock().unwrap(), State::On);
        })
    }

    #[test]
    fn path_to_pid() {
        assert_eq!(get_hci_index_from_pid_path("/var/run/bluetooth/bluetooth0.pid"), Some(0));
        assert_eq!(get_hci_index_from_pid_path("/var/run/bluetooth/bluetooth1.pid"), Some(1));
        assert_eq!(get_hci_index_from_pid_path("/var/run/bluetooth/bluetooth10.pid"), Some(10));
        assert_eq!(get_hci_index_from_pid_path("/var/run/bluetooth/garbage"), None);
    }
}
