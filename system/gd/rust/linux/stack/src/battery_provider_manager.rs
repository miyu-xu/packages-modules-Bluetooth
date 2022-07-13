use log::debug;

#[derive(Debug, Default, Clone)]
pub struct BatteryProvider {
    pub source_info: String,
    pub remote_address: String,
}

#[derive(Debug, Default, Clone)]
pub struct Battery {
    pub percentage: i32,
    pub source_info: String,
    pub variant: String,
}

pub trait IBatteryProviderCallback {
    fn refresh_battery_info(&self);
}

pub trait IBatteryProviderManager {
    fn register_battery_provider(&mut self, battery_provider: BatteryProvider, battery_provider_callback: Box<dyn IBatteryProviderCallback + Send>) -> i32;
    fn unregister_battery_provider(&mut self, battery_id: i32);
    fn set_battery_percentage(&mut self, battery_id: i32, battery: Battery);
}

pub struct BatteryProviderManager {}

impl BatteryProviderManager {
    pub fn new() -> BatteryProviderManager {
        BatteryProviderManager{}
    }
}

impl IBatteryProviderManager for BatteryProviderManager {
    fn register_battery_provider(&mut self, battery_provider: BatteryProvider, battery_provider_callback: Box<dyn IBatteryProviderCallback + Send>) -> i32 {  
      debug!("Registered /org/chromium/bluetooth/hci");
      213423
    }

    fn unregister_battery_provider(&mut self, battery_id: i32) {
      todo!()
    }

    fn set_battery_percentage(&mut self, battery_id: i32, battery: Battery) {
      todo!()
    }
}
