use bt_topshim::profiles::a2dp::{A2dpCodecConfig, PresentationPosition};
use bt_topshim::profiles::avrcp::PlayerMetadata;
use bt_topshim::profiles::hfp::HfpCodecCapability;
use btstack::bluetooth_media::{BluetoothAudioDevice, IBluetoothMedia, IBluetoothMediaCallback};
use btstack::RPCProxy;

use dbus::arg::RefArg;
use dbus::nonblock::SyncConnection;
use dbus::strings::Path;

use dbus_macros::{dbus_method, dbus_propmap, dbus_proxy_obj, generate_dbus_exporter};

use dbus_projection::DisconnectWatcher;
use dbus_projection::{dbus_generated, impl_dbus_arg_from_into};

use crate::dbus_arg::{DBusArg, DBusArgError, RefArgToRust};

use std::convert::{TryFrom, TryInto};
use std::sync::Arc;

#[allow(dead_code)]
struct BluetoothMediaCallbackDBus {}

#[dbus_propmap(A2dpCodecConfig)]
pub struct A2dpCodecConfigDBus {
    codec_type: i32,
    codec_priority: i32,
    sample_rate: i32,
    bits_per_sample: i32,
    channel_mode: i32,
    codec_specific_1: i64,
    codec_specific_2: i64,
    codec_specific_3: i64,
    codec_specific_4: i64,
}

#[dbus_propmap(BluetoothAudioDevice)]
pub struct BluetoothAudioDeviceDBus {
    address: String,
    name: String,
    a2dp_caps: Vec<A2dpCodecConfig>,
    hfp_cap: HfpCodecCapability,
    absolute_volume: bool,
}

impl_dbus_arg_from_into!(HfpCodecCapability, i32);

#[dbus_proxy_obj(BluetoothMediaCallback, "org.chromium.bluetooth.BluetoothMediaCallback")]
impl IBluetoothMediaCallback for BluetoothMediaCallbackDBus {
    #[dbus_method("OnBluetoothAudioDeviceAdded")]
    fn on_bluetooth_audio_device_added(&self, device: BluetoothAudioDevice) {
        dbus_generated!()
    }

    #[dbus_method("OnBluetoothAudioDeviceRemoved")]
    fn on_bluetooth_audio_device_removed(&self, addr: String) {
        dbus_generated!()
    }

    #[dbus_method("OnAbsoluteVolumeSupportedChanged")]
    fn on_absolute_volume_supported_changed(&self, supported: bool) {
        dbus_generated!()
    }

    #[dbus_method("OnAbsoluteVolumeChanged")]
    fn on_absolute_volume_changed(&self, volume: u8) {
        dbus_generated!()
    }

    #[dbus_method("OnHfpVolumeChanged")]
    fn on_hfp_volume_changed(&self, volume: u8, addr: String) {
        dbus_generated!()
    }

    #[dbus_method("OnHfpAudioDisconnected")]
    fn on_hfp_audio_disconnected(&self, addr: String) {
        dbus_generated!()
    }
}

#[allow(dead_code)]
struct IBluetoothMediaDBus {}

#[dbus_propmap(PresentationPosition)]
pub struct PresentationPositionDBus {
    remote_delay_report_ns: u64,
    total_bytes_read: u64,
    data_position_sec: i64,
    data_position_nsec: i32,
}

impl DBusArg for PlayerMetadata {
    type DBusType = dbus::arg::PropMap;
    fn from_dbus(
        data: dbus::arg::PropMap,
        _conn: Option<std::sync::Arc<dbus::nonblock::SyncConnection>>,
        _remote: Option<dbus::strings::BusName<'static>>,
        _disconnect_watcher: Option<
            std::sync::Arc<std::sync::Mutex<dbus_projection::DisconnectWatcher>>,
        >,
    ) -> Result<PlayerMetadata, Box<dyn std::error::Error>> {
        let mut metadata = PlayerMetadata {
            title: String::new(),
            artist: String::new(),
            album: String::new(),
            length: 0,
        };

        for (key, variant) in &data {
            match variant.arg_type() {
                dbus::arg::ArgType::Variant => {
                    match key.as_str() {
                        "title" => metadata.title =
                            match <<String as DBusArg>::DBusType as RefArgToRust>::ref_arg_to_rust(
                                variant.as_static_inner(0).unwrap(),
                                format!("PlayerMetadata::Title"),
                            ) {
                                Ok(title) => title,
                                Err(_) => String::new(),
                            },
                        "artist" => metadata.artist =
                            match <<String as DBusArg>::DBusType as RefArgToRust>::ref_arg_to_rust(
                                variant.as_static_inner(0).unwrap(),
                                format!("PlayerMetadata::Artist"),
                            ) {
                                Ok(artist) => artist,
                                Err(_) => String::new(),
                            },
                        "album" => metadata.album =
                            match <<String as DBusArg>::DBusType as RefArgToRust>::ref_arg_to_rust(
                                variant.as_static_inner(0).unwrap(),
                                format!("PlayerMetadata::Album"),
                            ) {
                                Ok(album) => album,
                                Err(_) => String::new(),
                            },
                        "length" => {
                            metadata.length =
                                match <<i64 as DBusArg>::DBusType as RefArgToRust>::ref_arg_to_rust(
                                    variant.as_static_inner(0).unwrap(),
                                    format!("PlayerMetadata::Length"),
                                ) {
                                    Ok(length) => length,
                                    Err(_) => 0,
                                }
                        }

                        _ => continue,
                    }
                }
                _ => continue,
            }
        }
        return Ok(metadata);
    }

    fn to_dbus(
        _metadata: PlayerMetadata,
    ) -> Result<dbus::arg::PropMap, Box<dyn std::error::Error>> {
        Ok(std::collections::HashMap::new())
    }
}

#[generate_dbus_exporter(export_bluetooth_media_dbus_intf, "org.chromium.bluetooth.BluetoothMedia")]
impl IBluetoothMedia for IBluetoothMediaDBus {
    #[dbus_method("RegisterCallback")]
    fn register_callback(&mut self, callback: Box<dyn IBluetoothMediaCallback + Send>) -> bool {
        dbus_generated!()
    }

    #[dbus_method("Initialize")]
    fn initialize(&mut self) -> bool {
        dbus_generated!()
    }

    #[dbus_method("Cleanup")]
    fn cleanup(&mut self) -> bool {
        dbus_generated!()
    }

    #[dbus_method("Connect")]
    fn connect(&mut self, address: String) {
        dbus_generated!()
    }

    #[dbus_method("Disconnect")]
    fn disconnect(&mut self, address: String) {
        dbus_generated!()
    }

    #[dbus_method("SetActiveDevice")]
    fn set_active_device(&mut self, address: String) {
        dbus_generated!()
    }

    #[dbus_method("SetHfpActiveDevice")]
    fn set_hfp_active_device(&mut self, address: String) {
        dbus_generated!()
    }

    #[dbus_method("SetAudioConfig")]
    fn set_audio_config(
        &mut self,
        sample_rate: i32,
        bits_per_sample: i32,
        channel_mode: i32,
    ) -> bool {
        dbus_generated!()
    }

    #[dbus_method("SetVolume")]
    fn set_volume(&mut self, volume: u8) {
        dbus_generated!()
    }

    #[dbus_method("SetHfpVolume")]
    fn set_hfp_volume(&mut self, volume: u8, address: String) {
        dbus_generated!()
    }

    #[dbus_method("StartAudioRequest")]
    fn start_audio_request(&mut self) {
        dbus_generated!()
    }

    #[dbus_method("GetA2dpAudioStarted")]
    fn get_a2dp_audio_started(&mut self, address: String) -> u8 {
        dbus_generated!()
    }

    #[dbus_method("StopAudioRequest")]
    fn stop_audio_request(&mut self) {
        dbus_generated!()
    }

    #[dbus_method("StartScoCall")]
    fn start_sco_call(&mut self, address: String, sco_offload: bool, force_cvsd: bool) {
        dbus_generated!()
    }

    #[dbus_method("GetHfpAudioStarted")]
    fn get_hfp_audio_started(&mut self, address: String) -> u8 {
        dbus_generated!()
    }

    #[dbus_method("StopScoCall")]
    fn stop_sco_call(&mut self, address: String) {
        dbus_generated!()
    }

    #[dbus_method("GetPresentationPosition")]
    fn get_presentation_position(&mut self) -> PresentationPosition {
        dbus_generated!()
    }

    #[dbus_method("SetPlayerPlaybackStatus")]
    fn set_player_playback_status(&mut self, status: String) {
        dbus_generated!()
    }

    #[dbus_method("SetPlayerPosition")]
    fn set_player_posistion(&mut self, position: i64) {
        dbus_generated!()
    }

    #[dbus_method("SetPlayerMetadata")]
    fn set_player_metadata(&mut self, metadata: PlayerMetadata) {
        dbus_generated!()
    }
}
