#include <server_configurable_flags/get_flags.h>

#include <vector>

#include "com_android_bluetooth_flags.h"

namespace com::android::bluetooth::flags {

class flag_provider : public flag_provider_interface {
public:
  virtual bool asymmetric_phy_for_unidirectional_cis() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.asymmetric_phy_for_unidirectional_cis",
                   "false") == "true";
  }

  virtual bool headtracker_codec_capability() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.headtracker_codec_capability", "false") == "true";
  }

  virtual bool headtracker_sdu_size() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.headtracker_sdu_size",
                   "false") == "true";
  }
  virtual bool le_ase_read_multiple_variable() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_ase_read_multiple_variable", "false") == "true";
  }

  virtual bool le_audio_base_ecosystem_interval() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_audio_base_ecosystem_interval",
                   "false") == "true";
  }

  virtual bool le_audio_support_unidirectional_voice_assistant() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_audio_support_unidirectional_voice_assistant",
                   "false") == "true";
  }

  virtual bool le_periodic_scanning_reassembler() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_periodic_scanning_reassembler",
                   "true") == "true";
  }

  virtual bool le_scan_fix_remote_exception() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_scan_fix_remote_exception", "true") == "true";
  }

  virtual bool le_scan_use_address_type() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_scan_use_address_type", "false") == "true";
  }

  virtual bool le_scan_use_uid_for_importance() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_scan_use_uid_for_importance", "false") == "true";
  }

  virtual bool leaudio_add_sampling_frequencies() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_add_sampling_frequencies",
                   "true") == "true";
  }

  virtual bool leaudio_allow_leaudio_only_devices() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_allow_leaudio_only_devices",
                   "false") == "true";
  }

  virtual bool leaudio_allowed_context_mask() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_allowed_context_mask", "false") == "true";
  }

  virtual bool leaudio_big_depends_on_audio_state() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_big_depends_on_audio_state",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_assistant_handle_command_statuses() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_assistant_handle_command_"
                   "statuses",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_assistant_peripheral_entrustment() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_assistant_peripheral_entrustment",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_audio_handover_policies() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_audio_handover_policies",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_destroy_after_timeout() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_destroy_after_timeout",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_extract_periodic_scanner_from_state_machine() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_extract_periodic_scanner_from_"
                   "state_machine",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_feature_support() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_feature_support",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_monitor_source_sync_status() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_monitor_source_sync_status",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_update_metadata_callback() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_update_metadata_callback",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_volume_control_for_connected_devices() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_volume_control_for_connected_"
                   "devices",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_volume_control_with_set_volume() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_volume_control_with_set_volume",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_volume_control_primary_group_only() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_volume_control_primary_group_"
                   "only",
                   "false") == "true";
  }

  virtual bool leaudio_call_start_scan_directly() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_call_start_scan_directly",
                   "false") == "true";
  }

  virtual bool leaudio_callback_on_group_stream_status() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_callback_on_group_stream_status",
                   "true") == "true";
  }

  virtual bool leaudio_codec_config_callback_order_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_codec_config_callback_order_fix",
                   "false") == "true";
  }

  virtual bool leaudio_dynamic_spatial_audio() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_dynamic_spatial_audio", "false") == "true";
  }

  virtual bool leaudio_getting_active_state_support() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_getting_active_state_support",
                   "false") == "true";
  }

  virtual bool leaudio_hal_client_asrc() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.leaudio_hal_client_asrc",
                   "false") == "true";
  }

  virtual bool leaudio_mono_location_errata() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_mono_location_errata", "false") == "true";
  }

  virtual bool leaudio_multicodec_aidl_support() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_multicodec_aidl_support",
                   "false") == "true";
  }

  virtual bool leaudio_multiple_vocs_instances_api() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_multiple_vocs_instances_api",
                   "true") == "true";
  }

  virtual bool leaudio_no_context_validate_streaming_request() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_no_context_validate_streaming_request",
                   "false") == "true";
  }

  virtual bool leaudio_quick_leaudio_toggle_switch_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_quick_leaudio_toggle_switch_fix",
                   "false") == "true";
  }

  virtual bool leaudio_resume_active_after_hfp_handover() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_resume_active_after_hfp_handover",
                   "false") == "true";
  }

  virtual bool leaudio_speed_up_reconfiguration_between_call() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_speed_up_reconfiguration_between_call",
                   "false") == "true";
  }

  virtual bool leaudio_start_request_state_mutex_check() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_start_request_state_mutex_check",
                   "false") == "true";
  }

  virtual bool leaudio_start_stream_race_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_start_stream_race_fix", "true") == "true";
  }

  virtual bool leaudio_synchronize_start() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_synchronize_start", "false") == "true";
  }

  virtual bool leaudio_use_audio_mode_listener() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_use_audio_mode_listener",
                   "false") == "true";
  }
  virtual bool run_ble_audio_ticks_in_worker_thread() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.run_ble_audio_ticks_in_worker_thread",
                   "false") == "true";
  }

private:
  std::vector<int8_t> cache_ = std::vector<int8_t>(189, -1);
};

std::unique_ptr<flag_provider_interface> provider_ = std::make_unique<flag_provider>();
}  // namespace com::android::bluetooth::flags

bool com_android_bluetooth_flags_asymmetric_phy_for_unidirectional_cis() {
  return com::android::bluetooth::flags::asymmetric_phy_for_unidirectional_cis();
}

bool com_android_bluetooth_flags_headtracker_codec_capability() {
  return com::android::bluetooth::flags::headtracker_codec_capability();
}

bool com_android_bluetooth_flags_headtracker_sdu_size() {
  return com::android::bluetooth::flags::headtracker_sdu_size();
}

bool com_android_bluetooth_flags_le_ase_read_multiple_variable() {
  return com::android::bluetooth::flags::le_ase_read_multiple_variable();
}

bool com_android_bluetooth_flags_le_audio_base_ecosystem_interval() {
  return com::android::bluetooth::flags::le_audio_base_ecosystem_interval();
}

bool com_android_bluetooth_flags_le_audio_support_unidirectional_voice_assistant() {
  return com::android::bluetooth::flags::le_audio_support_unidirectional_voice_assistant();
}

bool com_android_bluetooth_flags_le_periodic_scanning_reassembler() {
  return com::android::bluetooth::flags::le_periodic_scanning_reassembler();
}

bool com_android_bluetooth_flags_le_scan_fix_remote_exception() {
  return com::android::bluetooth::flags::le_scan_fix_remote_exception();
}

bool com_android_bluetooth_flags_le_scan_use_address_type() {
  return com::android::bluetooth::flags::le_scan_use_address_type();
}

bool com_android_bluetooth_flags_le_scan_use_uid_for_importance() {
  return com::android::bluetooth::flags::le_scan_use_uid_for_importance();
}

bool com_android_bluetooth_flags_leaudio_add_sampling_frequencies() {
  return com::android::bluetooth::flags::leaudio_add_sampling_frequencies();
}

bool com_android_bluetooth_flags_leaudio_allow_leaudio_only_devices() {
  return com::android::bluetooth::flags::leaudio_allow_leaudio_only_devices();
}

bool com_android_bluetooth_flags_leaudio_allowed_context_mask() {
  return com::android::bluetooth::flags::leaudio_allowed_context_mask();
}

bool com_android_bluetooth_flags_leaudio_big_depends_on_audio_state() {
  return com::android::bluetooth::flags::leaudio_big_depends_on_audio_state();
}

bool com_android_bluetooth_flags_leaudio_broadcast_assistant_handle_command_statuses() {
  return com::android::bluetooth::flags::leaudio_broadcast_assistant_handle_command_statuses();
}

bool com_android_bluetooth_flags_leaudio_broadcast_assistant_peripheral_entrustment() {
  return com::android::bluetooth::flags::leaudio_broadcast_assistant_peripheral_entrustment();
}

bool com_android_bluetooth_flags_leaudio_broadcast_audio_handover_policies() {
  return com::android::bluetooth::flags::leaudio_broadcast_audio_handover_policies();
}

bool com_android_bluetooth_flags_leaudio_broadcast_destroy_after_timeout() {
  return com::android::bluetooth::flags::leaudio_broadcast_destroy_after_timeout();
}

bool com_android_bluetooth_flags_leaudio_broadcast_extract_periodic_scanner_from_state_machine() {
  return com::android::bluetooth::flags::
          leaudio_broadcast_extract_periodic_scanner_from_state_machine();
}

bool com_android_bluetooth_flags_leaudio_broadcast_feature_support() {
  return com::android::bluetooth::flags::leaudio_broadcast_feature_support();
}

bool com_android_bluetooth_flags_leaudio_broadcast_monitor_source_sync_status() {
  return com::android::bluetooth::flags::leaudio_broadcast_monitor_source_sync_status();
}

bool com_android_bluetooth_flags_leaudio_broadcast_update_metadata_callback() {
  return com::android::bluetooth::flags::leaudio_broadcast_update_metadata_callback();
}

bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_for_connected_devices() {
  return com::android::bluetooth::flags::leaudio_broadcast_volume_control_for_connected_devices();
}

bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_with_set_volume() {
  return com::android::bluetooth::flags::leaudio_broadcast_volume_control_with_set_volume();
}

bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_primary_group_only() {
  return com::android::bluetooth::flags::leaudio_broadcast_volume_control_primary_group_only();
}

bool com_android_bluetooth_flags_leaudio_call_start_scan_directly() {
  return com::android::bluetooth::flags::leaudio_call_start_scan_directly();
}

bool com_android_bluetooth_flags_leaudio_callback_on_group_stream_status() {
  return com::android::bluetooth::flags::leaudio_callback_on_group_stream_status();
}

bool com_android_bluetooth_flags_leaudio_codec_config_callback_order_fix() {
  return com::android::bluetooth::flags::leaudio_codec_config_callback_order_fix();
}

bool com_android_bluetooth_flags_leaudio_dynamic_spatial_audio() {
  return com::android::bluetooth::flags::leaudio_dynamic_spatial_audio();
}

bool com_android_bluetooth_flags_leaudio_getting_active_state_support() {
  return com::android::bluetooth::flags::leaudio_getting_active_state_support();
}

bool com_android_bluetooth_flags_leaudio_hal_client_asrc() {
  return com::android::bluetooth::flags::leaudio_hal_client_asrc();
}

bool com_android_bluetooth_flags_leaudio_mono_location_errata() {
  return com::android::bluetooth::flags::leaudio_mono_location_errata();
}

bool com_android_bluetooth_flags_leaudio_multicodec_aidl_support() {
  return com::android::bluetooth::flags::leaudio_multicodec_aidl_support();
}

bool com_android_bluetooth_flags_leaudio_multiple_vocs_instances_api() {
  return com::android::bluetooth::flags::leaudio_multiple_vocs_instances_api();
}

bool com_android_bluetooth_flags_leaudio_no_context_validate_streaming_request() {
  return com::android::bluetooth::flags::leaudio_no_context_validate_streaming_request();
}

bool com_android_bluetooth_flags_leaudio_quick_leaudio_toggle_switch_fix() {
  return com::android::bluetooth::flags::leaudio_quick_leaudio_toggle_switch_fix();
}

bool com_android_bluetooth_flags_leaudio_resume_active_after_hfp_handover() {
  return com::android::bluetooth::flags::leaudio_resume_active_after_hfp_handover();
}

bool com_android_bluetooth_flags_leaudio_speed_up_reconfiguration_between_call() {
  return com::android::bluetooth::flags::leaudio_speed_up_reconfiguration_between_call();
}

bool com_android_bluetooth_flags_leaudio_start_request_state_mutex_check() {
  return com::android::bluetooth::flags::leaudio_start_request_state_mutex_check();
}

bool com_android_bluetooth_flags_leaudio_start_stream_race_fix() {
  return com::android::bluetooth::flags::leaudio_start_stream_race_fix();
}

bool com_android_bluetooth_flags_leaudio_synchronize_start() {
  return com::android::bluetooth::flags::leaudio_synchronize_start();
}

bool com_android_bluetooth_flags_leaudio_use_audio_mode_listener() {
  return com::android::bluetooth::flags::leaudio_use_audio_mode_listener();
}
bool com_android_bluetooth_flags_run_ble_audio_ticks_in_worker_thread() {
  return com::android::bluetooth::flags::run_ble_audio_ticks_in_worker_thread();
}
