// Note carefully the AIDL crates structure:
// * the AIDL module name: "com_example_android_remoteservice"
// * next "::aidl"
// * next the AIDL package name "::com::example::android"
// * the interface: "::IRemoteService"
// * finally, the 'BnRemoteService' and 'IRemoteService' submodules

//! This module implements the IBluetoothHapClient AIDL interface
// use binder::{BinderFeatures, Interface, Result as BinderResult, Strong};
use binder::Interface;
use HapClient_remoteservice::aidl::android::bluetooth::IBluetoothHapClient::IBluetoothHapClient;

use android_bluetooth_aidl::BluetoothDevice;
use android_bluetooth_aidl::BluetoothHapPresetInfo;
use android_content_aidl::AttributionSource;

/// This struct is defined to implement IBluetoothHapClient  AIDL interface.
pub struct MyService;

impl Interface for MyService {}

impl IBluetoothHapClient for MyService {
    fn getConnectedDevices(
        &self,
        _: &AttributionSource,
    ) -> std::result::Result<std::vec::Vec<BluetoothDevice>, binder::Status> {
        todo!()
    }
    fn getDevicesMatchingConnectionStates(
        &self,
        _: &[i32],
        _: &AttributionSource,
    ) -> std::result::Result<std::vec::Vec<BluetoothDevice>, binder::Status> {
        todo!()
    }
    fn getConnectionState(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<i32, binder::Status> {
        todo!()
    }
    fn setConnectionPolicy(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &AttributionSource,
    ) -> std::result::Result<bool, binder::Status> {
        todo!()
    }
    fn getConnectionPolicy(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<i32, binder::Status> {
        todo!()
    }
    fn getHapGroup(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<i32, binder::Status> {
        todo!()
    }
    fn getActivePresetIndex(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<i32, binder::Status> {
        todo!()
    }
    fn getActivePresetInfo(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<BluetoothHapPresetInfo, binder::Status> {
        todo!()
    }
    fn selectPreset(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn selectPresetForGroup(
        &self,
        _: i32,
        _: i32,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn switchToNextPreset(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn switchToNextPresetForGroup(
        &self,
        _: i32,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn switchToPreviousPreset(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn switchToPreviousPresetForGroup(
        &self,
        _: i32,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn getPresetInfo(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &AttributionSource,
    ) -> std::result::Result<BluetoothHapPresetInfo, binder::Status> {
        todo!()
    }
    fn getAllPresetInfo(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<std::vec::Vec<BluetoothHapPresetInfo>, binder::Status> {
        todo!()
    }
    fn getFeatures(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> std::result::Result<i32, binder::Status> {
        todo!()
    }
    fn setPresetName(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &str,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn setPresetNameForGroup(
        &self,
        _: i32,
        _: i32,
        _: &str,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn registerCallback(
        &self,
        _: &binder::Strong<(dyn HapClient_remoteservice::mangled::_7_android_9_bluetooth_27_IBluetoothHapClientCallback + 'static)>,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
    fn unregisterCallback(
        &self,
        _: &binder::Strong<(dyn HapClient_remoteservice::mangled::_7_android_9_bluetooth_27_IBluetoothHapClientCallback + 'static)>,
        _: &AttributionSource,
    ) -> std::result::Result<(), binder::Status> {
        todo!()
    }
}
