use crate::bluetooth_battery_service::{
    BatteryService, BatteryServiceStatus, IBatteryService, IBatteryServiceCallback,
};
use crate::callbacks::Callbacks;
use crate::Message;
use crate::RPCProxy;
use std::collections::HashSet;
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::Sender;

#[derive(Debug, Clone)]
pub struct Battery {
    pub percentage: u32,
    pub source_info: String,
    pub variant: String,
}

pub struct BatteryManager {
    bas: Arc<Mutex<Box<BatteryService>>>,
    tx: Sender<Message>,
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryManagerCallback + Send>>>,
    notifications_enabled: Arc<Mutex<HashSet<u32>>>,
}

impl BatteryManager {
    pub fn new(bas: Arc<Mutex<Box<BatteryService>>>, tx: Sender<Message>) -> BatteryManager {
        let callbacks = Arc::new(Mutex::new(Callbacks::new(
            tx.clone(),
            Message::BatteryManagerCallbackDisconnected,
        )));
        Self {
            bas: bas,
            tx: tx.clone(),
            callbacks: callbacks,
            notifications_enabled: Arc::new(Mutex::new(HashSet::new())),
        }
    }

    pub fn init(&self) {
        self.bas
            .lock()
            .unwrap()
            .register_callback(Box::new(BasCallback::new(self.callbacks.clone())));
    }
}

struct BasCallback {
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryManagerCallback + Send>>>,
}

impl BasCallback {
    pub fn new(
        callbacks: Arc<Mutex<Callbacks<dyn IBatteryManagerCallback + Send>>>,
    ) -> BasCallback {
        Self { callbacks: callbacks }
    }
}

impl IBatteryServiceCallback for BasCallback {
    fn on_battery_service_status_updated(
        &self,
        remote_address: String,
        status: BatteryServiceStatus,
    ) {
        return;
    }

    fn on_battery_level_updated(&self, remote_address: String, battery_level: u32) {
        return;
    }

    fn on_battery_level_read(&self, remote_address: String, battery_level: u32) {
        return;
    }
}

impl RPCProxy for BasCallback {
    fn get_object_id(&self) -> String {
        todo!()
    }
}

/// Callback for interacting with the BatteryManager.
pub trait IBatteryManagerCallback: RPCProxy {
    /// Invoked whenever battery information associated with the given remote changes.
    fn on_battery_info_updated(&self, remote_address: String, battery: Battery);

    fn on_battery_info_read(&self, remote_address: String, battery: Battery);
}

/// Central point for getting battery information that might be sourced from numerous systems.
pub trait IBatteryManager {
    /// Registers a callback for interfacing with the BatteryManager and returns a unique
    /// callback_id for future calls.
    fn register_battery_callback(
        &mut self,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> Option<u32>;

    /// Unregister a callback.
    fn unregister_battery_callback(&mut self, callback_id: u32);

    fn enable_notifications(&mut self, callback_id: u32, enable: bool);

    /// Returns battery information for the remote, sourced from the highest priority origin.
    fn get_battery_information(&self, remote_address: String) -> Option<Battery>;

    fn refresh_battery_information(&self, remote_address: String);
}

impl IBatteryManager for BatteryManager {
    fn register_battery_callback(
        &mut self,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> Option<u32> {
        Some(self.callbacks.lock().unwrap().add_callback(battery_manager_callback))
    }

    fn unregister_battery_callback(&mut self, callback_id: u32) {
        self.callbacks.lock().unwrap().remove_callback(callback_id);
    }

    fn enable_notifications(&mut self, callback_id: u32, enable: bool) {
        if let None = self.callbacks.lock().unwrap().get_by_id(callback_id) {
            return;
        }
        self.notifications_enabled.lock().unwrap().remove(&callback_id);
        if enable {
            self.notifications_enabled.lock().unwrap().insert(callback_id);
        }
    }

    fn get_battery_information(&self, remote_address: String) -> Option<Battery> {
        log::info!("Attempting to return battery information for {}", remote_address.clone());
        let battery_level = self.bas.lock().unwrap().get_battery_level(remote_address)?;
        Some(Battery {
            percentage: battery_level,
            source_info: "BAS".to_string(),
            variant: "".to_string(),
        })
    }

    fn refresh_battery_information(&self, remote_address: String) {
        self.bas.lock().unwrap().refresh_battery_level(remote_address);
    }
}
