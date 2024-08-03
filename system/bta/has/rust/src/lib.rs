// Note carefully the AIDL crates structure:
// * the AIDL module name: "com_example_android_remoteservice"
// * next "::aidl"
// * next the AIDL package name "::com::example::android"
// * the interface: "::IRemoteService"
// * finally, the 'BnRemoteService' and 'IRemoteService' submodules

//! This module implements the IBluetoothHapClient AIDL interface
// use binder::{BinderFeatures, Interface, Result as BinderResult, Strong};
use async_trait::async_trait;
use binder::{Interface, Result, Strong};
use HapClient_remoteservice::aidl::android::bluetooth::{
    IBluetoothHapClient::IBluetoothHapClientAsyncServer,
    IBluetoothHapClientCallback::IBluetoothHapClientCallback,
};

use android_bluetooth::BluetoothDevice;
use android_bluetooth::BluetoothHapPresetInfo;
use android_content::AttributionSource;

/// This struct is defined to implement IBluetoothHapClient  AIDL interface.
pub struct BluetoothHapClient;

impl Interface for BluetoothHapClient {}

// impl IBluetoothHapClient for BluetoothHapClient {
//     fn getConnectedDevices(
//         &self,
//         _: &AttributionSource,
//     ) -> std::result::Result<std::vec::Vec<BluetoothDevice>, binder::Status> {
//         todo!()
//     }
//     fn getDevicesMatchingConnectionStates(
//         &self,
//         _: &[i32],
//         _: &AttributionSource,
//     ) -> std::result::Result<std::vec::Vec<BluetoothDevice>, binder::Status> {
//         todo!()
//     }
//     fn getConnectionState(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<i32, binder::Status> {
//         todo!()
//     }
//     fn setConnectionPolicy(
//         &self,
//         _: &BluetoothDevice,
//         _: i32,
//         _: &AttributionSource,
//     ) -> std::result::Result<bool, binder::Status> {
//         todo!()
//     }
//     fn getConnectionPolicy(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<i32, binder::Status> {
//         todo!()
//     }
//     fn getHapGroup(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<i32, binder::Status> {
//         todo!()
//     }
//     fn getActivePresetIndex(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<i32, binder::Status> {
//         todo!()
//     }
//     fn getActivePresetInfo(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<BluetoothHapPresetInfo, binder::Status> {
//         todo!()
//     }
//     fn selectPreset(
//         &self,
//         _: &BluetoothDevice,
//         _: i32,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn selectPresetForGroup(
//         &self,
//         _: i32,
//         _: i32,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn switchToNextPreset(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn switchToNextPresetForGroup(
//         &self,
//         _: i32,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn switchToPreviousPreset(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn switchToPreviousPresetForGroup(
//         &self,
//         _: i32,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn getPresetInfo(
//         &self,
//         _: &BluetoothDevice,
//         _: i32,
//         _: &AttributionSource,
//     ) -> std::result::Result<BluetoothHapPresetInfo, binder::Status> {
//         todo!()
//     }
//     fn getAllPresetInfo(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<std::vec::Vec<BluetoothHapPresetInfo>, binder::Status> {
//         todo!()
//     }
//     fn getFeatures(
//         &self,
//         _: &BluetoothDevice,
//         _: &AttributionSource,
//     ) -> std::result::Result<i32, binder::Status> {
//         todo!()
//     }
//     fn setPresetName(
//         &self,
//         _: &BluetoothDevice,
//         _: i32,
//         _: &str,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn setPresetNameForGroup(
//         &self,
//         _: i32,
//         _: i32,
//         _: &str,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn registerCallback(
//         &self,
//         _: &Strong<(dyn IBluetoothHapClientCallback + 'static)>,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
//     fn unregisterCallback(
//         &self,
//         _: &Strong<(dyn IBluetoothHapClientCallback + 'static)>,
//         _: &AttributionSource,
//     ) -> std::result::Result<(), binder::Status> {
//         todo!()
//     }
// }

#[async_trait]
impl IBluetoothHapClientAsyncServer for BluetoothHapClient {
    async fn getConnectedDevices(&self, _: &AttributionSource) -> Result<Vec<BluetoothDevice>> {
        todo!()
    }
    async fn getDevicesMatchingConnectionStates(
        &self,
        _: &[i32],
        _: &AttributionSource,
    ) -> Result<Vec<BluetoothDevice>> {
        todo!()
    }
    async fn getConnectionState(&self, _: &BluetoothDevice, _: &AttributionSource) -> Result<i32> {
        todo!()
    }
    async fn setConnectionPolicy(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &AttributionSource,
    ) -> Result<bool> {
        todo!()
    }
    async fn getConnectionPolicy(&self, _: &BluetoothDevice, _: &AttributionSource) -> Result<i32> {
        todo!()
    }
    async fn getHapGroup(&self, _: &BluetoothDevice, _: &AttributionSource) -> Result<i32> {
        todo!()
    }
    async fn getActivePresetIndex(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> Result<i32> {
        todo!()
    }
    async fn getActivePresetInfo(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> Result<BluetoothHapPresetInfo> {
        todo!()
    }
    async fn selectPreset(&self, _: &BluetoothDevice, _: i32, _: &AttributionSource) -> Result<()> {
        todo!()
    }
    async fn selectPresetForGroup(&self, _: i32, _: i32, _: &AttributionSource) -> Result<()> {
        todo!()
    }
    async fn switchToNextPreset(&self, _: &BluetoothDevice, _: &AttributionSource) -> Result<()> {
        todo!()
    }
    async fn switchToNextPresetForGroup(&self, _: i32, _: &AttributionSource) -> Result<()> {
        todo!()
    }
    async fn switchToPreviousPreset(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> Result<()> {
        todo!()
    }
    async fn switchToPreviousPresetForGroup(&self, _: i32, _: &AttributionSource) -> Result<()> {
        todo!()
    }
    async fn getPresetInfo(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &AttributionSource,
    ) -> Result<BluetoothHapPresetInfo> {
        todo!()
    }
    async fn getAllPresetInfo(
        &self,
        _: &BluetoothDevice,
        _: &AttributionSource,
    ) -> Result<Vec<BluetoothHapPresetInfo>> {
        todo!()
    }
    async fn getFeatures(&self, _: &BluetoothDevice, _: &AttributionSource) -> Result<i32> {
        todo!()
    }
    async fn setPresetName(
        &self,
        _: &BluetoothDevice,
        _: i32,
        _: &str,
        _: &AttributionSource,
    ) -> Result<()> {
        todo!()
    }
    async fn setPresetNameForGroup(
        &self,
        _: i32,
        _: i32,
        _: &str,
        _: &AttributionSource,
    ) -> Result<()> {
        todo!()
    }
    async fn registerCallback(
        &self,
        _: &Strong<(dyn IBluetoothHapClientCallback)>,
        _: &AttributionSource,
    ) -> Result<()> {
        todo!()
    }
    async fn unregisterCallback(
        &self,
        _: &Strong<dyn IBluetoothHapClientCallback>,
        _: &AttributionSource,
    ) -> Result<()> {
        todo!()
    }
}
