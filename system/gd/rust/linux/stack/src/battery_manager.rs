use crate::battery_provider_manager::{BatteryProviderManager, IBatteryConsumerCallback};
use crate::callbacks::Callbacks;
use crate::Message;
use crate::RPCProxy;
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::Sender;

/// The primary representation of battery information for internal passing and
/// external calls.
#[derive(Debug, Clone)]
pub struct Battery {
    pub address: String,
    pub percentage: u32,
    pub source_info: String,
    pub variant: String,
}

/// Helper representation of a collection of batteries to simplify passing
/// around data internally.
pub struct Batteries {
    pub batteries: Vec<Battery>,
}

/// Callback for interacting with the BatteryManager.
pub trait IBatteryManagerCallback: RPCProxy {
    /// Invoked whenever battery information associated with the given remote
    /// changes.
    fn on_battery_info_updated(&self, remote_address: String, batteries: Vec<Battery>);
}

/// Central point for getting battery information that might be sourced from
/// numerous systems.
pub trait IBatteryManager {
    /// Registers a callback for interfacing with the BatteryManager and returns
    /// a unique callback_id for future calls.
    fn register_battery_callback(
        &mut self,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> u32;

    /// Unregister a callback.
    fn unregister_battery_callback(&mut self, callback_id: u32);

    /// Returns battery information for the remote, sourced from the highest
    /// priority origin.
    fn get_battery_information(&self, remote_address: String) -> Option<Vec<Battery>>;
}

/// Repesentation of the BatteryManager.
pub struct BatteryManager {
    battery_provider_manager: Arc<Mutex<Box<BatteryProviderManager>>>,
    callbacks: Callbacks<dyn IBatteryManagerCallback + Send>,
}

/// Enum for BatteryConsumerCallback to relay messages back to the main thread.
pub enum BatteryConsumerCallbacks {
    /// Params: remote_address, batteries
    OnBatteryInfoUpdated(String, Option<Batteries>),
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
            BatteryConsumerCallbacks::OnBatteryInfoUpdated(remote_address, batteries) => {
                let batteries = batteries.map_or(vec![], |battery| battery.batteries);
                self.callbacks.for_all_callbacks(|callback| {
                    callback.on_battery_info_updated(remote_address.clone(), batteries.clone())
                });
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

    fn get_battery_information(&self, remote_address: String) -> Option<Vec<Battery>> {
        Some(
            self.battery_provider_manager
                .lock()
                .unwrap()
                .get_battery_info(remote_address)?
                .batteries,
        )
    }
}

impl Batteries {
    pub fn new() -> Self {
        Self { batteries: vec![] }
    }

    pub fn from_vec(batteries: Vec<Battery>) -> Self {
        Self { batteries: batteries }
    }

    /// Updates a battery matching all non-battery-level fields if found,
    /// otherwise adds new_battery verbatim.
    pub fn add_or_update_battery(&mut self, new_battery: Battery) {
        match self.batteries.iter_mut().find(|battery| {
            battery.address == new_battery.address
                && battery.source_info == new_battery.source_info
                && battery.variant == new_battery.variant
        }) {
            Some(battery) => *battery = new_battery,
            None => self.batteries.push(new_battery),
        }
    }

    /// Returns a Batteries object containing the highest quality pick from all
    /// available data.
    pub fn pick_best(&self) -> Batteries {
        let batteries = self
            .batteries
            .iter()
            .filter(|battery| battery.source_info == "BAS")
            .cloned()
            .collect::<Vec<Battery>>();
        if batteries.len() > 0 {
            return Batteries::from_vec(batteries);
        }
        let batteries = self
            .batteries
            .iter()
            .filter(|battery| battery.source_info == "powerd")
            .cloned()
            .collect::<Vec<Battery>>();
        if batteries.len() > 0 {
            return Batteries::from_vec(batteries);
        }
        Batteries::from_vec(self.batteries.clone())
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

    fn on_battery_info_updated(&self, remote_address: String, batteries: Option<Batteries>) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx
                .send(Message::BatteryManagerCallbacks(
                    BatteryConsumerCallbacks::OnBatteryInfoUpdated(remote_address, batteries),
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
