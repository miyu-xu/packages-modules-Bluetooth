use crate::bluetooth_gatt::{
    BluetoothGatt, BluetoothGattService, IBluetoothGatt, IBluetoothGattCallback,
};
use crate::callbacks::Callbacks;
use crate::uuid::parse_uuid_string;
use crate::uuid::BAS;
use crate::Message;
use crate::RPCProxy;
use bt_topshim::profiles::gatt::GattStatus;
use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::Sender;

pub const CHARACTERISTIC_BATTERY_LEVEL: &str = "00002A1900001000800000805F9B34FB";

pub struct BatteryService {
    gatt: Arc<Mutex<Box<BluetoothGatt>>>,
    tx: Sender<Message>,
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
    client_id: Arc<Mutex<Option<i32>>>,
    battery_levels: Arc<Mutex<HashMap<String, u32>>>,
    notifications_enabled: Arc<Mutex<HashSet<u32>>>,
    handles: Arc<Mutex<HashMap<String, i32>>>,
}

pub enum GattBatteryCallbacks {
    OnClientRegistered(GattStatus, i32),
    OnClientConnectionState(GattStatus, i32, bool, String),
    OnSearchComplete(String, Vec<BluetoothGattService>, GattStatus),
    OnCharacteristicRead(String, GattStatus, i32, Vec<u8>),
    OnNotify(String, i32, Vec<u8>),
}

impl BatteryService {
    pub fn new(gatt: Arc<Mutex<Box<BluetoothGatt>>>, tx: Sender<Message>) -> BatteryService {
        let client_id = Arc::new(Mutex::new(None));
        let battery_levels = Arc::new(Mutex::new(HashMap::new()));
        let callbacks = Arc::new(Mutex::new(Callbacks::new(
            tx.clone(),
            Message::BatteryServiceCallbackDisconnected,
        )));
        let notifications_enabled = Arc::new(Mutex::new(HashSet::new()));
        let handles = Arc::new(Mutex::new(HashMap::new()));
        Self {
            gatt: gatt,
            tx: tx.clone(),
            callbacks: callbacks,
            client_id: client_id,
            battery_levels: battery_levels,
            notifications_enabled: notifications_enabled,
            handles: handles,
        }
    }

    /// Must be called after BluetoothGatt's init_profiles method has completed.
    pub fn init(&self) {
        self.gatt.lock().unwrap().register_client(
            // TODO: make dynamic
            String::from("e4d2acffcfaa42198f494606b7412117"),
            Box::new(GattCallback::new(self.tx.clone())),
            false,
        );
    }

    pub fn handle_callback(&self, callback: GattBatteryCallbacks) {
        match callback {
            GattBatteryCallbacks::OnClientRegistered(status, client_id) => {
                *self.client_id.lock().unwrap() = Some(client_id);
            }

            GattBatteryCallbacks::OnClientConnectionState(status, client_id, connected, addr) => {
                if !connected {
                    return;
                }
                let client_id = match *self.client_id.lock().unwrap() {
                    Some(id) => id,
                    None => {
                        return;
                    }
                };
                self.gatt.lock().unwrap().discover_services(client_id, addr);
            }

            GattBatteryCallbacks::OnSearchComplete(addr, services, status) => {
                let bas_uuid = match parse_uuid_string(BAS) {
                    Some(uuid) => uuid,
                    None => return,
                };
                let battery_level_uuid = match parse_uuid_string(CHARACTERISTIC_BATTERY_LEVEL) {
                    Some(uuid) => uuid,
                    None => return,
                };
                // TODO: handle multiple instances of BAS
                let bas = match services.iter().find(|service| service.uuid == bas_uuid.uu) {
                    Some(bas) => bas,
                    None => {
                        self.callbacks.lock().unwrap().for_all_callbacks(|callback| {
                            callback.on_battery_service_status_updated(
                                addr.clone(),
                                BatteryServiceStatus::BatteryServiceNotSupported,
                            )
                        });
                        return;
                    }
                };
                let battery_level = match bas
                    .characteristics
                    .iter()
                    .find(|characteristic| characteristic.uuid == battery_level_uuid.uu)
                {
                    Some(battery_level) => battery_level,
                    None => return,
                };
                if status != GattStatus::Success {
                    return;
                }
                let client_id = match *self.client_id.lock().unwrap() {
                    Some(id) => id,
                    None => return,
                };
                let handle = battery_level.instance_id;
                self.handles.lock().unwrap().insert(addr.clone(), handle.clone());
                self.gatt.lock().unwrap().register_for_notification(
                    client_id,
                    addr.clone(),
                    handle,
                    true,
                );
                if let None = self.battery_levels.lock().unwrap().get(&addr) {
                    self.gatt.lock().unwrap().read_characteristic(
                        client_id,
                        addr,
                        battery_level.instance_id,
                        0,
                    );
                }
            }

            GattBatteryCallbacks::OnCharacteristicRead(addr, status, handle, value) => {
                if status != GattStatus::Success {
                    return;
                }
                let level = self.set_battery_level(addr.clone(), value.clone());
                self.callbacks.lock().unwrap().for_all_callbacks(|callback| {
                    callback.on_battery_level_read(addr.clone(), level);
                });
            }

            GattBatteryCallbacks::OnNotify(addr, handle, value) => {
                let level = self.set_battery_level(addr.clone(), value);
                // TODO: expand Callbacks to allow direct filtering/exposing the underlying iter
                self.notifications_enabled.lock().unwrap().iter().for_each(|id| {
                    match self.callbacks.lock().unwrap().get_by_id(*id) {
                        Some(callback) => callback.on_battery_level_updated(addr.clone(), level),
                        None => (),
                    }
                });
            }
        }
    }

    fn set_battery_level(&self, remote_address: String, value: Vec<u8>) -> u32 {
        let mut level: [u8; 4] = [0, 0, 0, 0];
        let copy_limit = if value.len() < 4 { value.len() } else { 4 };
        for i in 0..copy_limit {
            level[i] = value[i];
        }
        let level = u32::from_le_bytes(level);
        self.battery_levels.lock().unwrap().insert(remote_address, level);
        level
    }

    fn init_device(&self, remote_address: String) {
        let client_id = match *self.client_id.lock().unwrap() {
            Some(id) => id,
            None => return,
        };
        let bas_uuid = match parse_uuid_string(BAS) {
            Some(uuid) => uuid,
            None => return,
        };
        self.gatt.lock().unwrap().client_connect(client_id, remote_address, false, 2, false, 1);
    }

    pub fn remove_callback(&self, callback_id: u32) {
        self.callbacks.lock().unwrap().remove_callback(callback_id);
    }
}

pub trait IBatteryService {
    /// Registers a callback for interacting with BatteryService.
    fn register_callback(
        &mut self,
        callback: Box<dyn IBatteryServiceCallback + Send>,
    ) -> Option<u32>;

    /// Unregisters a callback.
    fn unregister_callback(&mut self, callback_id: u32);

    /// Enables notifications for a given callback.
    fn enable_notifications(&mut self, callback_id: u32, enable: bool);

    /// Returns the battery level of the remove device if available in BatteryService's cache. Call refresh_battery_level at least once to ensure that BatteryService is tracking the device's battery information.
    fn get_battery_level(&self, remote_address: String) -> Option<u32>;

    /// Forces an explicit read of the device's battery level, including initiating battery level tracking if not yet performed.
    fn refresh_battery_level(&self, remote_address: String);
}

pub enum BatteryServiceStatus {
    BatteryServiceNotReady,
    BatteryServiceReady,
    BatteryServiceNotSupported,
}

pub trait IBatteryServiceCallback: RPCProxy {
    /// Called when the status of BatteryService has changed. Trying to read from devices that do not support BAS will result in this method being called with BatteryServiceNotSupported.
    fn on_battery_service_status_updated(
        &self,
        remote_address: String,
        status: BatteryServiceStatus,
    );

    /// Invoked when battery level for a device has been changed due to notification.
    fn on_battery_level_updated(&self, remote_address: String, battery_level: u32);

    /// Invoked whenever an explicit read of a devices battery level completes.
    fn on_battery_level_read(&self, remote_address: String, battery_level: u32);
}

impl IBatteryService for BatteryService {
    fn register_callback(
        &mut self,
        callback: Box<dyn IBatteryServiceCallback + Send>,
    ) -> Option<u32> {
        let callback_id = self.callbacks.lock().unwrap().add_callback(callback);
        Some(callback_id)
    }

    fn unregister_callback(&mut self, callback_id: u32) {
        self.remove_callback(callback_id);
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

    fn get_battery_level(&self, remote_address: String) -> Option<u32> {
        self.battery_levels.lock().unwrap().get(&remote_address).cloned()
    }

    fn refresh_battery_level(&self, remote_address: String) {
        let client_id = match *self.client_id.lock().unwrap() {
            Some(id) => id,
            None => return,
        };
        let handle = match self.handles.lock().unwrap().get(&remote_address) {
            Some(id) => *id,
            None => {
                self.init_device(remote_address);
                return;
            }
        };
        self.gatt.lock().unwrap().read_characteristic(client_id, remote_address.clone(), handle, 0);
        self.gatt.lock().unwrap().register_for_notification(
            client_id,
            remote_address,
            handle,
            true,
        );
    }
}

struct GattCallback {
    tx: Sender<Message>,
}

impl GattCallback {
    fn new(tx: Sender<Message>) -> Self {
        Self { tx: tx }
    }
}

impl IBluetoothGattCallback for GattCallback {
    fn on_client_registered(&self, status: GattStatus, client_id: i32) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            tx.send(Message::BatteryServiceCallbacks(GattBatteryCallbacks::OnClientRegistered(
                status, client_id,
            )))
            .await;
        });
    }

    fn on_client_connection_state(
        &self,
        status: GattStatus,
        client_id: i32,
        connected: bool,
        addr: String,
    ) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            tx.send(Message::BatteryServiceCallbacks(
                GattBatteryCallbacks::OnClientConnectionState(status, client_id, connected, addr),
            ))
            .await;
        });
    }

    fn on_search_complete(
        &self,
        addr: String,
        services: Vec<BluetoothGattService>,
        status: GattStatus,
    ) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            tx.send(Message::BatteryServiceCallbacks(GattBatteryCallbacks::OnSearchComplete(
                addr, services, status,
            )))
            .await;
        });
    }

    fn on_characteristic_read(
        &self,
        addr: String,
        status: GattStatus,
        handle: i32,
        value: Vec<u8>,
    ) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            tx.send(Message::BatteryServiceCallbacks(GattBatteryCallbacks::OnCharacteristicRead(
                addr, status, handle, value,
            )))
            .await;
        });
    }

    fn on_notify(&self, addr: String, handle: i32, value: Vec<u8>) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            tx.send(Message::BatteryServiceCallbacks(GattBatteryCallbacks::OnNotify(
                addr, handle, value,
            )))
            .await;
        });
    }
}

impl RPCProxy for GattCallback {
    fn get_object_id(&self) -> String {
        "BAS Gatt Callback".to_string()
    }
}
