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

pub const CHARACTERISTIC_BATTERY_LEVEL: &str = "00002A19-0000-1000-8000-00805F9B34FB";

pub struct BatteryService {
    gatt: Arc<Mutex<Box<BluetoothGatt>>>,
    tx: Sender<Message>,
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
    client_id: Arc<Mutex<Option<i32>>>,
    battery_levels: Arc<Mutex<HashMap<String, u32>>>,
    notifications_enabled: Arc<Mutex<HashSet<u32>>>,
    handles: Arc<Mutex<HashMap<String, i32>>>,
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

    pub fn init(&self) {
        self.gatt.lock().unwrap().register_client(
            String::from("e4d2acffcfaa42198f494606b7412117"),
            Box::new(GattCallback::new(
                self.client_id.clone(),
                self.callbacks.clone(),
                self.gatt.clone(),
                self.battery_levels.clone(),
                self.notifications_enabled.clone(),
                self.handles.clone(),
            )),
            false,
        );
    }
}

pub trait IBatteryService {
    fn register_callback(
        &mut self,
        callback: Box<dyn IBatteryServiceCallback + Send>,
    ) -> Option<u32>;
    fn unregister_callback(&mut self, callback_id: u32);
    fn enable_notifications(&mut self, callback_id: u32, enable: bool);
    fn get_battery_level(&self, remote_address: String) -> Option<u32>;
    fn refresh_battery_level(&self, remote_address: String);
}

pub enum BatteryServiceStatus {
    BatteryServiceNotReady,
    BatteryServiceReady,
    BatteryServiceNotSupported,
}

pub trait IBatteryServiceCallback: RPCProxy {
    fn on_battery_service_status_updated(
        &self,
        remote_address: String,
        status: BatteryServiceStatus,
    );
    fn on_battery_level_updated(&self, remote_address: String, battery_level: u32);
    fn on_battery_level_read(&self, remote_address: String, battery_level: u32);
}

impl IBatteryService for BatteryService {
    // Refactor to not use remote_address here
    fn register_callback(
        &mut self,
        callback: Box<dyn IBatteryServiceCallback + Send>,
    ) -> Option<u32> {
        let callback_id = self.callbacks.lock().unwrap().add_callback(callback);
        Some(callback_id)
    }

    fn unregister_callback(&mut self, callback_id: u32) {
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
            None => return,
        };
        self.gatt.lock().unwrap().read_characteristic(client_id, remote_address, handle, 0);
    }
}

struct GattCallback {
    client_id: Arc<Mutex<Option<i32>>>,
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
    gatt: Arc<Mutex<Box<BluetoothGatt>>>,
    // TODO: support multiple batteries per device
    battery_levels: Arc<Mutex<HashMap<String, u32>>>,
    notifications_enabled: Arc<Mutex<HashSet<u32>>>,
    handles: Arc<Mutex<HashMap<String, i32>>>,
}

impl GattCallback {
    fn new(
        client_id: Arc<Mutex<Option<i32>>>,
        callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
        gatt: Arc<Mutex<Box<BluetoothGatt>>>,
        battery_levels: Arc<Mutex<HashMap<String, u32>>>,
        notifications_enabled: Arc<Mutex<HashSet<u32>>>,
        handles: Arc<Mutex<HashMap<String, i32>>>,
    ) -> Self {
        Self {
            client_id: client_id,
            callbacks: callbacks,
            gatt: gatt,
            battery_levels: battery_levels,
            notifications_enabled: notifications_enabled,
            handles: handles,
        }
    }

    fn set_battery_level(&self, remote_address: String, value: Vec<u8>) {
        let mut level: [u8; 4] = [0, 0, 0, 0];
        let copy_limit = if value.len() < 4 { value.len() } else { 4 };
        for i in 0..copy_limit {
            level[i] = value[i];
        }
        if let Some(battery_level) = self.battery_levels.lock().unwrap().get_mut(&remote_address) {
            *battery_level = u32::from_le_bytes(level);
        }
    }
}

impl IBluetoothGattCallback for GattCallback {
    fn on_client_registered(&self, status: GattStatus, client_id: i32) {
        *self.client_id.lock().unwrap() = Some(client_id);
    }

    fn on_search_complete(
        &self,
        addr: String,
        services: Vec<BluetoothGattService>,
        status: GattStatus,
    ) {
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
        if let Some(id) = *self.client_id.lock().unwrap() {
            let handle = battery_level.instance_id;
            self.handles.lock().unwrap().insert(addr.clone(), handle.clone());
            self.gatt.lock().unwrap().register_for_notification(id, addr.clone(), handle, true);
            if let None = self.battery_levels.lock().unwrap().get(&addr) {
                self.gatt.lock().unwrap().read_characteristic(
                    id,
                    addr,
                    battery_level.instance_id,
                    0,
                );
            }
        }
    }

    fn on_characteristic_read(
        &self,
        addr: String,
        status: GattStatus,
        handle: i32,
        value: Vec<u8>,
    ) {
        if status != GattStatus::Success {
            return;
        }
        self.set_battery_level(addr.clone(), value);
        self.callbacks.lock().unwrap().for_all_callbacks(|callback| {
            callback.on_battery_level_read(
                addr.clone(),
                *self.battery_levels.lock().unwrap().get(&addr).unwrap(),
            )
        });
    }

    fn on_notify(&self, addr: String, handle: i32, value: Vec<u8>) {
        self.set_battery_level(addr.clone(), value);
        let level = match self.battery_levels.lock().unwrap().get(&addr) {
            Some(level) => *level,
            None => return,
        };
        // TODO: expand Callbacks to allow direct filtering/exposing the underlying iter
        self.notifications_enabled.lock().unwrap().iter().for_each(|id| {
            match self.callbacks.lock().unwrap().get_by_id(*id) {
                Some(callback) => callback.on_battery_level_updated(addr.clone(), level),
                None => (),
            }
        });
    }
}

impl RPCProxy for GattCallback {
    fn get_object_id(&self) -> String {
        todo!()
    }
}
