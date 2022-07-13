#[derive(Debug, Default, Clone)]
pub struct Battery {
    pub percentage: i32,
    pub source_info: String,
    pub variant: String,
}

pub struct BatteryManager {}

impl BatteryManager {
    pub fn new() -> BatteryManager {
        BatteryManager {}
    }
}

pub trait IBatteryManagerCallback {
    fn battery_info_updated(&self, remote_address: String, battery: Battery);
}

pub trait IBatteryManager {
    fn get_battery_information(&self, emote_address: String) -> Battery;
    fn register_battery_callback(
        &mut self,
        remote_address: String,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> i32;
    fn unregister_battery_callback(&mut self, callback_id: i32);
}

impl IBatteryManager for BatteryManager {
    fn get_battery_information(&self, remote_address: String) -> Battery {
        todo!()
    }

    fn register_battery_callback(
        &mut self,
        remote_address: String,
        battery_manager_callback: Box<dyn IBatteryManagerCallback + Send>,
    ) -> i32 {
        todo!()
    }

    fn unregister_battery_callback(&mut self, callback_id: i32) {
        todo!()
    }
}
