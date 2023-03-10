use btstack::bluetooth_qa::IBluetoothQA;

use dbus_macros::{dbus_method, generate_dbus_exporter};

use dbus_projection::dbus_generated;

use crate::dbus_arg::DBusArg;

struct IBluetoothQADBus {}

#[generate_dbus_exporter(export_bluetooth_qa_dbus_intf, "org.chromium.bluetooth.BluetoothQA")]
impl IBluetoothQA for IBluetoothQADBus {
    #[dbus_method("EnableA2dpSink")]
    fn enable_a2dp_sink(&self) {
        dbus_generated!()
    }
    #[dbus_method("SendAvrcpPassThrough")]
    fn send_avrcp_pass_through(&self, addr: String, key_code: u8, key_state: u8) {
        dbus_generated!()
    }
}
