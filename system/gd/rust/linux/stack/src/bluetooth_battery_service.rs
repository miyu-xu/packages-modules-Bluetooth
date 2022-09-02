use crate::bluetooth_gatt::{
    BluetoothGatt, BluetoothGattService, IBluetoothGatt, IBluetoothGattCallback,
};
use crate::callbacks::Callbacks;
use crate::uuid::parse_uuid_string;
use crate::uuid::BAS;
use crate::Message;
use crate::RPCProxy;
use bt_topshim::profiles::gatt::GattStatus;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::Sender;

pub const CHARACTERISTIC_BATTERY_LEVEL: &str = "00002A19-0000-1000-8000-00805F9B34FB";

pub struct BatteryService {
    gatt: Arc<Mutex<Box<BluetoothGatt>>>,
    tx: Sender<Message>,
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
    callback_ids: Arc<Mutex<HashMap<String, Vec<u32>>>>,
    client_id: Arc<Mutex<Option<i32>>>,
    battery_levels: Arc<Mutex<HashMap<String, u32>>>,
    notifications_enabled: Arc<Mutex<HashMap<u32, bool>>>,
}

impl BatteryService {
    pub fn new(gatt: Arc<Mutex<Box<BluetoothGatt>>>, tx: Sender<Message>) -> BatteryService {
        let client_id = Arc::new(Mutex::new(None));
        let battery_levels = Arc::new(Mutex::new(HashMap::new()));
        let callback_ids = Arc::new(Mutex::new(HashMap::new()));
        let callbacks = Arc::new(Mutex::new(Callbacks::new(
            tx.clone(),
            Message::BatteryServiceCallbackDisconnected,
        )));
        let notifications_enabled = Arc::new(Mutex::new(HashMap::new()));
        Self {
            gatt: gatt,
            tx: tx.clone(),
            callbacks: callbacks,
            callback_ids: callback_ids,
            client_id: client_id,
            battery_levels: battery_levels,
            notifications_enabled: notifications_enabled,
        }
    }

    pub fn init(&self) {
        self.gatt.lock().unwrap().register_client(
            String::from("e4d2acffcfaa42198f494606b7412117"),
            Box::new(GattCallback::new(
                self.client_id.clone(),
                self.callback_ids.clone(),
                self.callbacks.clone(),
                self.gatt.clone(),
                self.battery_levels.clone(),
                self.notifications_enabled.clone(),
            )),
            false,
        );
    }
}

pub trait IBatteryService {
    fn register_callback(
        &mut self,
        remote_address: String,
        callback: Box<dyn IBatteryServiceCallback + Send>,
    ) -> u32;
    fn unregister_callback(&mut self, callback_id: u32);
    fn enable_notifications(&mut self, callback_id: u32, enable: bool);
    fn get_battery_level(&self, callback_id: u32, remote_address: String);
}

pub enum BatteryServiceStatus {
    BatteryServiceNotReady,
    BatteryServiceReady,
    BatteryServiceNotSupported,
}

pub trait IBatteryServiceCallback: RPCProxy {
    fn on_battery_service_status_updated(&self, status: BatteryServiceStatus);
    fn on_battery_level_updated(&self, remote_address: String, battery_level: u32);
}

impl IBatteryService for BatteryService {
    fn register_callback(
        &mut self,
        remote_address: String,
        callback: Box<dyn IBatteryServiceCallback + Send>,
    ) -> u32 {
        if self.client_id.lock().unwrap().is_none() {
            callback
                .on_battery_service_status_updated(BatteryServiceStatus::BatteryServiceNotReady);
            return 0;
        }
        let callback_id = self.callbacks.lock().unwrap().add_callback(callback);
        self.callback_ids.lock().unwrap().entry(remote_address.clone()).or_insert(vec![]);
        if let Some(callbacks) = self.callback_ids.lock().unwrap().get_mut(&remote_address) {
            callbacks.push(callback_id);
        }
        let should_discover = match self.callback_ids.lock().unwrap().get(&remote_address) {
            Some(ids) => ids.len() == 1,
            None => false,
        };
        if should_discover {
            if let Some(id) = *self.client_id.lock().unwrap() {
                self.gatt.lock().unwrap().discover_service_by_uuid(
                    id,
                    remote_address,
                    BAS.to_string(),
                );
            }
        }
        callback_id
    }

    fn unregister_callback(&mut self, callback_id: u32) {
        self.callbacks.lock().unwrap().remove_callback(callback_id);
    }

    fn enable_notifications(&mut self, callback_id: u32, enable: bool) {
        if let None = self.callbacks.lock().unwrap().get_by_id(callback_id) {
            return;
        }
        self.notifications_enabled.lock().unwrap().remove(&callback_id);
        self.notifications_enabled.lock().unwrap().insert(callback_id, enable);
    }

    fn get_battery_level(&self, callback_id: u32, remote_address: String) {
        let mut battery_level = 0;
        match self.battery_levels.lock().unwrap().get(&remote_address) {
            Some(level) => battery_level = *level,
            None => return,
        }

        match self.callbacks.lock().unwrap().get_by_id(callback_id) {
            Some(callback) => callback.on_battery_level_updated(remote_address, battery_level),
            None => return,
        }
    }
}

struct GattCallback {
    client_id: Arc<Mutex<Option<i32>>>,
    callback_ids: Arc<Mutex<HashMap<String, Vec<u32>>>>,
    callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
    gatt: Arc<Mutex<Box<BluetoothGatt>>>,
    // TODO: support multiple batteries per device
    battery_levels: Arc<Mutex<HashMap<String, u32>>>,
    notifications_enabled: Arc<Mutex<HashMap<u32, bool>>>,
}

impl GattCallback {
    fn new(
        client_id: Arc<Mutex<Option<i32>>>,
        callback_ids: Arc<Mutex<HashMap<String, Vec<u32>>>>,
        callbacks: Arc<Mutex<Callbacks<dyn IBatteryServiceCallback + Send>>>,
        gatt: Arc<Mutex<Box<BluetoothGatt>>>,
        battery_levels: Arc<Mutex<HashMap<String, u32>>>,
        notifications_enabled: Arc<Mutex<HashMap<u32, bool>>>,
    ) -> Self {
        Self {
            client_id: client_id,
            callback_ids: callback_ids,
            callbacks: callbacks,
            gatt: gatt,
            battery_levels: battery_levels,
            notifications_enabled: notifications_enabled,
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
        log::info!("melhuishj: client registered, assigning");
        let mut client_id_lock = self.client_id.lock().unwrap();
        *client_id_lock = Some(client_id);
    }

    fn on_search_complete(
        &self,
        addr: String,
        services: Vec<BluetoothGattService>,
        status: GattStatus,
    ) {
        let bas_uuid = parse_uuid_string(BAS);
        if bas_uuid.is_none() {
            return;
        }
        let battery_level_uuid = parse_uuid_string(CHARACTERISTIC_BATTERY_LEVEL);
        if battery_level_uuid.is_none() {
            return;
        }
        // TODO: handle multiple instances of BAS
        let bas = services.iter().find(|service| service.uuid == bas_uuid.unwrap().uu);
        let battery_level = bas
            .unwrap()
            .characteristics
            .iter()
            .find(|characteristic| characteristic.uuid == battery_level_uuid.unwrap().uu);
        if status != GattStatus::Success || bas.is_none() || battery_level.is_none() {
            self.callback_ids
                .lock()
                .unwrap()
                .get(&addr)
                .unwrap_or(&Vec::<u32>::new())
                .iter()
                .for_each(|id| match self.callbacks.lock().unwrap().get_by_id(*id) {
                    Some(callback) => callback.on_battery_service_status_updated(
                        BatteryServiceStatus::BatteryServiceNotSupported,
                    ),
                    None => (),
                });
        }
        if let Some(id) = *self.client_id.lock().unwrap() {
            self.gatt.lock().unwrap().register_for_notification(
                id,
                addr.clone(),
                battery_level.unwrap().instance_id,
                true,
            );
            if let None = self.battery_levels.lock().unwrap().get(&addr) {
                self.gatt.lock().unwrap().read_characteristic(
                    id,
                    addr,
                    battery_level.unwrap().instance_id,
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
        self.set_battery_level(addr, value);
    }

    fn on_notify(&self, addr: String, handle: i32, value: Vec<u8>) {
        self.set_battery_level(addr.clone(), value);
        let mut level = 0;
        match self.battery_levels.lock().unwrap().get(&addr) {
            Some(battery_level) => level = *battery_level,
            None => return,
        }
        self.callback_ids
            .lock()
            .unwrap()
            .get(&addr)
            .unwrap_or(&Vec::<u32>::new())
            .iter()
            .filter(|id| match self.notifications_enabled.lock().unwrap().get(id) {
                Some(enabled) => *enabled,
                None => false,
            })
            .for_each(|id| match self.callbacks.lock().unwrap().get_by_id(*id) {
                Some(callback) => callback.on_battery_level_updated(addr.clone(), level),
                None => (),
            });
    }
}

impl RPCProxy for GattCallback {
    fn get_object_id(&self) -> String {
        todo!()
    }
}
