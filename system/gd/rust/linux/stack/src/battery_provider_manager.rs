use log::debug;

pub trait IBatteryProviderManager {
    fn register_battery_provider(&mut self, device_id: i32);
}

pub struct BatteryProviderManager {}

impl BatteryProviderManager {
    pub fn new() -> BatteryProviderManager {
        BatteryProviderManager{}
    }
}

impl IBatteryProviderManager for BatteryProviderManager {
    fn register_battery_provider(&mut self, device_id: i32) {  
      debug!("Registered /org/chromium/bluetooth/hci{}", device_id);
    }
}
