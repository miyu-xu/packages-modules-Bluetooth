use crate::bluetooth_battery_service::BatteryService;
use crate::callbacks::Callbacks;
use crate::Message;
use crate::RPCProxy;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::Sender;

#[derive(Debug, Clone)]
pub struct Battery {
    pub percentage: i32,
    pub source_info: String,
    pub variant: String,
}

pub struct BatteryManager {
    bas: Arc<Mutex<Box<BatteryService>>>,
    tx: Sender<Message>,
    callbacks: Arc<Mutex<HashMap<String, Callbacks<dyn IBatteryManagerCallback + Send>>>>,
}

impl BatteryManager {
    pub fn new(bas: Arc<Mutex<Box<BatteryService>>>, tx: Sender<Message>) -> BatteryManager {
        let callbacks = Arc::new(Mutex::new(HashMap::new()));
        Self { bas: bas, tx: tx.clone(), callbacks: callbacks }
    }

    pub fn init(&self) {}
}

/// Callback for interacting with the BatteryManager.
pub trait IBatteryManagerCallback: RPCProxy {
    /// Invoked whenever battery information associated with the given remote changes.
    fn on_battery_info_updated(&self, remote_address: String, battery: Battery);
}

/// Central point for getting battery information that might be sourced from numerous systems.
pub trait IBatteryManager {
    /// Registers a callback for interfacing with the BatteryManager and returns a unique
    /// callback_id for future calls.
    fn register_battery_callback(
        &mut self,
        remote_address: String,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> u32;

    /// Unregister a callback.
    fn unregister_battery_callback(&mut self, callback_id: i32);

    /// Returns battery information for the remote, sourced from the highest priority origin.
    fn get_battery_information(&self, remote_address: String) -> Battery;
}

impl IBatteryManager for BatteryManager {
    fn register_battery_callback(
        &mut self,
        remote_address: String,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> u32 {
        self.callbacks.lock().unwrap().entry(remote_address.clone()).or_insert(Callbacks::new(
            self.tx.clone(),
            Message::BatteryManagerCallbackDisconnected,
        ));
        let mut callback_id = 0;
        match self.callbacks.lock().unwrap().get_mut(&remote_address) {
            Some(callbacks) => callback_id = callbacks.add_callback(battery_manager_callback),
            None => return 0,
        }
        callback_id
    }

    fn unregister_battery_callback(&mut self, _callback_id: i32) {
        todo!()
    }

    fn get_battery_information(&self, _remote_address: String) -> Battery {
        todo!()
    }
}
