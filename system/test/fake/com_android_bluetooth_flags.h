#pragma once

#ifdef __cplusplus

#include <memory>

namespace com::android::bluetooth::flags {

class flag_provider_interface {
public:
  virtual ~flag_provider_interface() = default;
  virtual bool headtracker_codec_capability() = 0;
  virtual bool headtracker_sdu_size() = 0;
  virtual bool asymmetric_phy_for_unidirectional_cis() = 0;
  virtual bool le_ase_read_multiple_variable() = 0;
  virtual bool le_audio_base_ecosystem_interval() = 0;
  virtual bool le_audio_support_unidirectional_voice_assistant() = 0;
  virtual bool le_periodic_scanning_reassembler() = 0;
  virtual bool le_scan_fix_remote_exception() = 0;
  virtual bool le_scan_use_address_type() = 0;
  virtual bool le_scan_use_uid_for_importance() = 0;
  virtual bool leaudio_add_sampling_frequencies() = 0;
  virtual bool leaudio_allow_leaudio_only_devices() = 0;
  virtual bool leaudio_allowed_context_mask() = 0;
  virtual bool leaudio_big_depends_on_audio_state() = 0;
  virtual bool leaudio_broadcast_assistant_handle_command_statuses() = 0;
  virtual bool leaudio_broadcast_assistant_peripheral_entrustment() = 0;
  virtual bool leaudio_broadcast_audio_handover_policies() = 0;
  virtual bool leaudio_broadcast_destroy_after_timeout() = 0;
  virtual bool leaudio_broadcast_extract_periodic_scanner_from_state_machine() = 0;
  virtual bool leaudio_broadcast_feature_support() = 0;
  virtual bool leaudio_broadcast_monitor_source_sync_status() = 0;
  virtual bool leaudio_broadcast_update_metadata_callback() = 0;
  virtual bool leaudio_broadcast_volume_control_for_connected_devices() = 0;
  virtual bool leaudio_broadcast_volume_control_primary_group_only() = 0;
  virtual bool leaudio_broadcast_volume_control_with_set_volume() = 0;
  virtual bool leaudio_call_start_scan_directly() = 0;
  virtual bool leaudio_callback_on_group_stream_status() = 0;
  virtual bool leaudio_codec_config_callback_order_fix() = 0;
  virtual bool leaudio_dynamic_spatial_audio() = 0;
  virtual bool leaudio_getting_active_state_support() = 0;
  virtual bool leaudio_hal_client_asrc() = 0;
  virtual bool leaudio_mono_location_errata() = 0;
  virtual bool leaudio_multicodec_aidl_support() = 0;
  virtual bool leaudio_multiple_vocs_instances_api() = 0;
  virtual bool leaudio_no_context_validate_streaming_request() = 0;
  virtual bool leaudio_quick_leaudio_toggle_switch_fix() = 0;
  virtual bool leaudio_resume_active_after_hfp_handover() = 0;
  virtual bool leaudio_speed_up_reconfiguration_between_call() = 0;
  virtual bool leaudio_start_request_state_mutex_check() = 0;
  virtual bool leaudio_start_stream_race_fix() = 0;
  virtual bool leaudio_synchronize_start() = 0;
  virtual bool leaudio_use_audio_mode_listener() = 0;
  virtual bool run_ble_audio_ticks_in_worker_thread() = 0;
};

extern std::unique_ptr<flag_provider_interface> provider_;

inline bool headtracker_codec_capability() { return provider_->headtracker_codec_capability(); }
inline bool headtracker_sdu_size() { return provider_->headtracker_sdu_size(); }

inline bool asymmetric_phy_for_unidirectional_cis() {
  return provider_->asymmetric_phy_for_unidirectional_cis();
}

inline bool le_ase_read_multiple_variable() { return provider_->le_ase_read_multiple_variable(); }
inline bool le_audio_base_ecosystem_interval() {
  return provider_->le_audio_base_ecosystem_interval();
}
inline bool le_audio_support_unidirectional_voice_assistant() {
  return provider_->le_audio_support_unidirectional_voice_assistant();
}
inline bool le_periodic_scanning_reassembler() {
  return provider_->le_periodic_scanning_reassembler();
}
inline bool le_scan_fix_remote_exception() { return provider_->le_scan_fix_remote_exception(); }
inline bool le_scan_use_address_type() { return provider_->le_scan_use_address_type(); }
inline bool le_scan_use_uid_for_importance() { return provider_->le_scan_use_uid_for_importance(); }
inline bool leaudio_add_sampling_frequencies() {
  return provider_->leaudio_add_sampling_frequencies();
}
inline bool leaudio_allow_leaudio_only_devices() {
  return provider_->leaudio_allow_leaudio_only_devices();
}
inline bool leaudio_allowed_context_mask() { return provider_->leaudio_allowed_context_mask(); }
inline bool leaudio_big_depends_on_audio_state() {
  return provider_->leaudio_big_depends_on_audio_state();
}
inline bool leaudio_broadcast_assistant_handle_command_statuses() {
  return provider_->leaudio_broadcast_assistant_handle_command_statuses();
}
inline bool leaudio_broadcast_assistant_peripheral_entrustment() {
  return provider_->leaudio_broadcast_assistant_peripheral_entrustment();
}
inline bool leaudio_broadcast_audio_handover_policies() {
  return provider_->leaudio_broadcast_audio_handover_policies();
}
inline bool leaudio_broadcast_destroy_after_timeout() {
  return provider_->leaudio_broadcast_destroy_after_timeout();
}
inline bool leaudio_broadcast_extract_periodic_scanner_from_state_machine() {
  return provider_->leaudio_broadcast_extract_periodic_scanner_from_state_machine();
}
inline bool leaudio_broadcast_feature_support() {
  return provider_->leaudio_broadcast_feature_support();
}
inline bool leaudio_broadcast_monitor_source_sync_status() {
  return provider_->leaudio_broadcast_monitor_source_sync_status();
}
inline bool leaudio_broadcast_update_metadata_callback() {
  return provider_->leaudio_broadcast_update_metadata_callback();
}
inline bool leaudio_broadcast_volume_control_for_connected_devices() {
  return provider_->leaudio_broadcast_volume_control_for_connected_devices();
}
inline bool leaudio_broadcast_volume_control_with_set_volume() {
  return provider_->leaudio_broadcast_volume_control_with_set_volume();
}
inline bool leaudio_broadcast_volume_control_primary_group_only() {
  return provider_->leaudio_broadcast_volume_control_primary_group_only();
}
inline bool leaudio_call_start_scan_directly() {
  return provider_->leaudio_call_start_scan_directly();
}
inline bool leaudio_callback_on_group_stream_status() {
  return provider_->leaudio_callback_on_group_stream_status();
}
inline bool leaudio_codec_config_callback_order_fix() {
  return provider_->leaudio_codec_config_callback_order_fix();
}
inline bool leaudio_dynamic_spatial_audio() { return provider_->leaudio_dynamic_spatial_audio(); }
inline bool leaudio_getting_active_state_support() {
  return provider_->leaudio_getting_active_state_support();
}
inline bool leaudio_hal_client_asrc() { return provider_->leaudio_hal_client_asrc(); }
inline bool leaudio_mono_location_errata() { return provider_->leaudio_mono_location_errata(); }
inline bool leaudio_multicodec_aidl_support() {
  return provider_->leaudio_multicodec_aidl_support();
}
inline bool leaudio_multiple_vocs_instances_api() {
  return provider_->leaudio_multiple_vocs_instances_api();
}
inline bool leaudio_no_context_validate_streaming_request() {
  return provider_->leaudio_no_context_validate_streaming_request();
}
inline bool leaudio_quick_leaudio_toggle_switch_fix() {
  return provider_->leaudio_quick_leaudio_toggle_switch_fix();
}
inline bool leaudio_resume_active_after_hfp_handover() {
  return provider_->leaudio_resume_active_after_hfp_handover();
}
inline bool leaudio_speed_up_reconfiguration_between_call() {
  return provider_->leaudio_speed_up_reconfiguration_between_call();
}
inline bool leaudio_start_request_state_mutex_check() {
  return provider_->leaudio_start_request_state_mutex_check();
}
inline bool leaudio_start_stream_race_fix() { return provider_->leaudio_start_stream_race_fix(); }
inline bool leaudio_synchronize_start() { return provider_->leaudio_synchronize_start(); }
inline bool leaudio_use_audio_mode_listener() {
  return provider_->leaudio_use_audio_mode_listener();
}
inline bool run_ble_audio_ticks_in_worker_thread() {
  return provider_->run_ble_audio_ticks_in_worker_thread();
}

}  // namespace com::android::bluetooth::flags

extern "C" {
#endif  // __cplusplus

bool com_android_bluetooth_flags_headtracker_codec_capability();
bool com_android_bluetooth_flags_headtracker_sdu_size();
bool com_android_bluetooth_flags_asymmetric_phy_for_unidirectional_cis();
bool com_android_bluetooth_flags_le_ase_read_multiple_variable();
bool com_android_bluetooth_flags_le_audio_base_ecosystem_interval();
bool com_android_bluetooth_flags_le_audio_support_unidirectional_voice_assistant();
bool com_android_bluetooth_flags_le_periodic_scanning_reassembler();
bool com_android_bluetooth_flags_le_scan_fix_remote_exception();
bool com_android_bluetooth_flags_le_scan_use_address_type();
bool com_android_bluetooth_flags_le_scan_use_uid_for_importance();
bool com_android_bluetooth_flags_leaudio_add_sampling_frequencies();
bool com_android_bluetooth_flags_leaudio_allow_leaudio_only_devices();
bool com_android_bluetooth_flags_leaudio_allowed_context_mask();
bool com_android_bluetooth_flags_leaudio_big_depends_on_audio_state();
bool com_android_bluetooth_flags_leaudio_broadcast_assistant_handle_command_statuses();
bool com_android_bluetooth_flags_leaudio_broadcast_assistant_peripheral_entrustment();
bool com_android_bluetooth_flags_leaudio_broadcast_audio_handover_policies();
bool com_android_bluetooth_flags_leaudio_broadcast_destroy_after_timeout();
bool com_android_bluetooth_flags_leaudio_broadcast_extract_periodic_scanner_from_state_machine();
bool com_android_bluetooth_flags_leaudio_broadcast_feature_support();
bool com_android_bluetooth_flags_leaudio_broadcast_monitor_source_sync_status();
bool com_android_bluetooth_flags_leaudio_broadcast_update_metadata_callback();
bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_for_connected_devices();
bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_with_set_volume();
bool com_android_bluetooth_flags_leaudio_call_start_scan_directly();
bool com_android_bluetooth_flags_leaudio_callback_on_group_stream_status();
bool com_android_bluetooth_flags_leaudio_codec_config_callback_order_fix();
bool com_android_bluetooth_flags_leaudio_dynamic_spatial_audio();
bool com_android_bluetooth_flags_leaudio_getting_active_state_support();
bool com_android_bluetooth_flags_leaudio_hal_client_asrc();
bool com_android_bluetooth_flags_leaudio_mono_location_errata();
bool com_android_bluetooth_flags_leaudio_multicodec_aidl_support();
bool com_android_bluetooth_flags_leaudio_multiple_vocs_instances_api();
bool com_android_bluetooth_flags_leaudio_no_context_validate_streaming_request();
bool com_android_bluetooth_flags_leaudio_quick_leaudio_toggle_switch_fix();
bool com_android_bluetooth_flags_leaudio_resume_active_after_hfp_handover();
bool com_android_bluetooth_flags_leaudio_speed_up_reconfiguration_between_call();
bool com_android_bluetooth_flags_leaudio_start_request_state_mutex_check();
bool com_android_bluetooth_flags_leaudio_start_stream_race_fix();
bool com_android_bluetooth_flags_leaudio_synchronize_start();
bool com_android_bluetooth_flags_leaudio_use_audio_mode_listener();
bool com_android_bluetooth_flags_run_ble_audio_ticks_in_worker_thread();

#ifdef __cplusplus
}  // extern "C"
#endif
