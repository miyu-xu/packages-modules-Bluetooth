use crate::battery_provider_manager::{BatteryProviderManager, IBatteryConsumerCallback};
use crate::battery_service::BAS_BATTERY_PROVIDER_UUID;
use crate::callbacks::Callbacks;
use crate::Message;
use crate::RPCProxy;
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::Sender;

/// The primary representation of battery information for internal passing and external calls.
#[derive(Debug, Clone)]
pub struct BatterySet {
    pub address: String,
    pub source_uuid: String,
    pub source_info: String,
    pub batteries: Vec<Battery>,
}

/// Describes an individual battery measurement, possibly one of many for a given device.
#[derive(Debug, Clone)]
pub struct Battery {
    pub percentage: u32,
    pub variant: String,
}

/// Helper representation of a collection of BatterySet to simplify passing around data internally.
pub struct Batteries {
    pub battery_sets: Vec<BatterySet>,
}

/// Callback for interacting with the BatteryManager.
pub trait IBatteryManagerCallback: RPCProxy {
    /// Invoked whenever battery information associated with the given remote changes.
    fn on_battery_info_updated(&self, remote_address: String, battery_set: BatterySet);
}

/// Central point for getting battery information that might be sourced from numerous systems.
pub trait IBatteryManager {
    /// Registers a callback for interfacing with the BatteryManager and returns a unique
    /// callback_id for future calls.
    fn register_battery_callback(
        &mut self,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> u32;

    /// Unregister a callback.
    fn unregister_battery_callback(&mut self, callback_id: u32);

    /// Returns battery information for the remote, sourced from the highest priority origin.
    fn get_battery_information(&self, remote_address: String) -> Option<BatterySet>;
}

/// Repesentation of the BatteryManager.
pub struct BatteryManager {
    battery_provider_manager: Arc<Mutex<Box<BatteryProviderManager>>>,
    callbacks: Callbacks<dyn IBatteryManagerCallback + Send>,
}

/// Enum for BatteryConsumerCallback to relay messages back to the main thread.
pub enum BatteryConsumerCallbacks {
    /// Params: remote_address, battery_set
    OnBatteryInfoUpdated(String, Option<BatterySet>),
}

impl BatteryManager {
    /// Construct a new BatteryManager with callbacks communicating on tx.
    pub fn new(
        battery_provider_manager: Arc<Mutex<Box<BatteryProviderManager>>>,
        tx: Sender<Message>,
    ) -> BatteryManager {
        let callbacks = Callbacks::new(tx.clone(), Message::BatteryManagerCallbackDisconnected);
        battery_provider_manager
            .lock()
            .unwrap()
            .register_battery_consumer_callback(Box::new(BatteryConsumerCallback::new(tx.clone())));
        Self { battery_provider_manager, callbacks }
    }

    /// Remove a callback due to disconnection or unregistration.
    pub fn remove_callback(&mut self, callback_id: u32) {
        self.callbacks.remove_callback(callback_id);
    }

    /// Handles all callback messages to avoid deadlocks.
    pub fn handle_callback(&mut self, callback: BatteryConsumerCallbacks) {
        match callback {
            BatteryConsumerCallbacks::OnBatteryInfoUpdated(remote_address, battery_set) => {
                match battery_set {
                    Some(battery_set) => self.callbacks.for_all_callbacks(|callback| {
                        callback
                            .on_battery_info_updated(remote_address.clone(), battery_set.clone())
                    }),
                    None => {}
                }
            }
        }
    }
}

impl IBatteryManager for BatteryManager {
    fn register_battery_callback(
        &mut self,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> u32 {
        self.callbacks.add_callback(battery_manager_callback)
    }

    fn unregister_battery_callback(&mut self, callback_id: u32) {
        self.remove_callback(callback_id);
    }

    fn get_battery_information(&self, remote_address: String) -> Option<BatterySet> {
        self.battery_provider_manager.lock().unwrap().get_battery_info(remote_address)
    }
}

impl BatterySet {
    pub fn new(address: String, source_uuid: String, source_info: String) -> Self {
        Self { address, source_uuid, source_info, batteries: vec![] }
    }

    pub fn add_or_update_battery(&mut self, new_battery: Battery) {
        match self.batteries.iter_mut().find(|battery| battery.variant == new_battery.variant) {
            Some(battery) => *battery = new_battery,
            None => self.batteries.push(new_battery),
        }
    }
}

impl Batteries {
    pub fn new() -> Self {
        Self { battery_sets: vec![] }
    }

    /// Updates a battery matching all non-battery-level fields if found, otherwise adds new_battery
    /// verbatim.
    pub fn add_or_update_battery_set(&mut self, new_battery_set: BatterySet) {
        match self
            .battery_sets
            .iter_mut()
            .find(|battery_set| battery_set.source_uuid == new_battery_set.source_uuid)
        {
            Some(battery_set) => *battery_set = new_battery_set,
            None => self.battery_sets.push(new_battery_set),
        }
    }

    /// Returns the best BatterySet from among reported battery data.
    pub fn pick_best(&self) -> Option<BatterySet> {
        self.battery_sets
            .iter()
            .find(|battery_set| battery_set.source_uuid == BAS_BATTERY_PROVIDER_UUID)
            .or_else(|| self.battery_sets.first())
            .cloned()
    }
}

/// The callback used to receive updates from BatteryProviderManager.
struct BatteryConsumerCallback {
    tx: Sender<Message>,
}

impl BatteryConsumerCallback {
    fn new(tx: Sender<Message>) -> Self {
        Self { tx }
    }
}

impl IBatteryConsumerCallback for BatteryConsumerCallback {
    // All callback messages get relayed to the main thread for processing.

    fn on_battery_info_updated(&self, remote_address: String, battery_set: Option<BatterySet>) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx
                .send(Message::BatteryManagerCallbacks(
                    BatteryConsumerCallbacks::OnBatteryInfoUpdated(remote_address, battery_set),
                ))
                .await;
        });
    }
}

impl RPCProxy for BatteryConsumerCallback {
    fn get_object_id(&self) -> String {
        "Battery Consumer Callback".to_string()
    }
}
