use btstack::battery_provider_manager::IBatteryProviderManager;
use dbus_macros::{dbus_method, generate_dbus_exporter};
use dbus_projection::dbus_generated;

use crate::dbus_arg::DBusArg;

struct IBatteryProviderManagerDBus {}

#[generate_dbus_exporter(export_battery_provider_manager_dbus_intf, "org.chromium.bluetooth.BatteryProviderManager")]
impl IBatteryProviderManager for IBatteryProviderManagerDBus {
    #[dbus_method("RegisterBatteryProvider")]
    fn register_battery_provider(&mut self, device_id: i32) {
        dbus_generated!()
    }
}
