/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <vector>
#include <hardware/bluetooth.h>

#include "gd/rust/topshim/le_audio/le_audio_shim.h"

#include "gd/os/log.h"
#include "src/profiles/le_audio.rs.h"
#include "types/raw_address.h"

namespace rusty = ::bluetooth::topshim::rust;

namespace bluetooth {
namespace topshim {
namespace rust {
namespace internal {
static LeAudioClientIntf* g_lea_client_if;

static le_audio::btle_audio_codec_config_t from_rust_btle_audio_codec_config(
    BtLeAudioCodecConfig codec_config) {
  switch (codec_config.codec_type) {
    case static_cast<int>(BtLeAudioCodecIndex::SrcLc3):
      return le_audio::btle_audio_codec_config_t {
        .codec_type =
          le_audio::btle_audio_codec_index_t::LE_AUDIO_CODEC_INDEX_SOURCE_LC3
      };
    default:
      ASSERT_LOG(false, "%s: Unhandled enum value from Rust", __func__);
  }
}

static BtLeAudioCodecConfig to_rust_btle_audio_codec_config(
    le_audio::btle_audio_codec_config_t codec_config) {
  switch (codec_config.codec_type) {
    case le_audio::btle_audio_codec_index_t::LE_AUDIO_CODEC_INDEX_SOURCE_LC3:
      return BtLeAudioCodecConfig {
        .codec_type =
          static_cast<int>(BtLeAudioCodecIndex::SrcLc3)
      };
    default:
      ASSERT_LOG(false, "%s: Unhandled enum value from C++", __func__);
  }
}

static ::rust::vec<BtLeAudioCodecConfig> to_rust_btle_audio_codec_config_vec(
    std::vector<le_audio::btle_audio_codec_config_t> codec_configs) {
  ::rust::vec<BtLeAudioCodecConfig> rconfigs;
  for (auto c : codec_configs) {
    rconfigs.push_back(to_rust_btle_audio_codec_config(c));
  }
  return rconfigs;
}

static BtLeAudioConnectionState to_rust_btle_audio_connection_state(
    le_audio::ConnectionState state) {
  switch (state) {
    case le_audio::ConnectionState::DISCONNECTED:
      return BtLeAudioConnectionState::Disconnected;
    case le_audio::ConnectionState::CONNECTING:
      return BtLeAudioConnectionState::Connecting;
    case le_audio::ConnectionState::CONNECTED:
      return BtLeAudioConnectionState::Connected;
    case le_audio::ConnectionState::DISCONNECTING:
      return BtLeAudioConnectionState::Disconnecting;
    default:
      ASSERT_LOG(false, "%s: Unhandled enum value from C++", __func__);
  }
}

static BtLeAudioGroupStatus to_rust_btle_audio_group_status(
    le_audio::GroupStatus status) {
  switch (status) {
    case le_audio::GroupStatus::INACTIVE:
      return BtLeAudioGroupStatus::Inactive;
    case le_audio::GroupStatus::ACTIVE:
      return BtLeAudioGroupStatus::Active;
    case le_audio::GroupStatus::TURNED_IDLE_DURING_CALL:
      return BtLeAudioGroupStatus::TurnedIdleDuringCall;
    default:
      ASSERT_LOG(false, "%s: Unhandled enum value from C++", __func__);
  }
}

static BtLeAudioGroupNodeStatus to_rust_btle_audio_group_node_status(
    le_audio::GroupNodeStatus status) {
  switch (status) {
    case le_audio::GroupNodeStatus::ADDED:
      return BtLeAudioGroupNodeStatus::Added;
    case le_audio::GroupNodeStatus::REMOVED:
      return BtLeAudioGroupNodeStatus::Removed;
    default:
      ASSERT_LOG(false, "%s: Unhandled enum value from C++", __func__);
  }
}

static void initialized_cb() {
  le_audio_initialized_callback();
}

static void connection_state_cb(le_audio::ConnectionState state,
                                const RawAddress& address) {
  le_audio_connection_state_callback(
      to_rust_btle_audio_connection_state(state), address);
}

static void group_status_cb(int group_id, le_audio::GroupStatus group_status) {
  le_audio_group_status_callback(
      group_id, to_rust_btle_audio_group_status(group_status));
}

static void group_node_status_cb(const RawAddress& bd_addr, int group_id,
                                 le_audio::GroupNodeStatus node_status) {
  le_audio_group_node_status_callback(
      bd_addr, group_id, to_rust_btle_audio_group_node_status(node_status));
}

static void audio_conf_cb(uint8_t direction, int group_id,
                          uint32_t snk_audio_location,
                          uint32_t src_audio_location,
                          uint16_t avail_cont) {
  le_audio_audio_conf_callback(direction, group_id,
      snk_audio_location, src_audio_location, avail_cont);
}

static void sink_audio_location_available_cb(const RawAddress& address,
                                             uint32_t snk_audio_locations) {
  le_audio_sink_audio_location_available_callback(
      address, snk_audio_locations);
}

static void audio_local_codec_capabilities_cb(
    std::vector<le_audio::btle_audio_codec_config_t> local_input_capa_codec_conf,
    std::vector<le_audio::btle_audio_codec_config_t> local_output_capa_codec_conf) {
  le_audio_audio_local_codec_capabilities_callback(
      to_rust_btle_audio_codec_config_vec(local_input_capa_codec_conf),
      to_rust_btle_audio_codec_config_vec(local_output_capa_codec_conf));
}

static void audio_group_codec_conf_cb(
    int group_id, le_audio::btle_audio_codec_config_t input_codec_conf,
    le_audio::btle_audio_codec_config_t output_codec_conf,
    std::vector<le_audio::btle_audio_codec_config_t> input_selectable_codec_conf,
    std::vector<le_audio::btle_audio_codec_config_t> output_selectable_codec_conf) {
  le_audio_audio_group_codec_conf_callback(group_id,
      to_rust_btle_audio_codec_config(input_codec_conf),
      to_rust_btle_audio_codec_config(output_codec_conf),
      to_rust_btle_audio_codec_config_vec(input_selectable_codec_conf),
      to_rust_btle_audio_codec_config_vec(output_selectable_codec_conf));
}
}  // namespace internal

class DBusLeAudioClientCallbacks : public le_audio::LeAudioClientCallbacks {
 public:
  static le_audio::LeAudioClientCallbacks* GetInstance() {
    static auto instance = new DBusLeAudioClientCallbacks();
    return instance;
  }

  DBusLeAudioClientCallbacks() {};

  void OnInitialized() override {
    LOG_INFO("%s", __func__);
    topshim::rust::internal::initialized_cb();
  }

  void OnConnectionState(le_audio::ConnectionState state,
                         const RawAddress& address) override {
    LOG_INFO("%s from %s", __func__, ADDRESS_TO_LOGGABLE_CSTR(address));
    topshim::rust::internal::connection_state_cb(state, address);
  }

  void OnGroupStatus(int group_id, le_audio::GroupStatus group_status) override {
    LOG_INFO("%s gid=%d, group_status=%d", __func__, group_id, group_status);
    topshim::rust::internal::group_status_cb(group_id, group_status);
  }

  void OnGroupNodeStatus(const RawAddress& bd_addr, int group_id,
                         le_audio::GroupNodeStatus node_status) {
    LOG_INFO("%s from %s, gid=%d, node_status=%d", __func__,
        ADDRESS_TO_LOGGABLE_CSTR(bd_addr), group_id, node_status);
    topshim::rust::internal::group_node_status_cb(bd_addr, group_id,
        node_status);
  }

  void OnAudioConf(uint8_t direction, int group_id,
                   uint32_t snk_audio_location,
                   uint32_t src_audio_location,
                   uint16_t avail_cont) {
    LOG_INFO("%s dir=%u, gid=%d, snk_loc=%u, src_loc=%u, avail=%u", __func__,
        direction, group_id, snk_audio_location, src_audio_location,
        avail_cont);
    topshim::rust::internal::audio_conf_cb(direction, group_id,
        snk_audio_location, src_audio_location, avail_cont);
  }

  void OnSinkAudioLocationAvailable(const RawAddress& address,
                                    uint32_t snk_audio_locations) {
    LOG_INFO("%s from %s, snk_loc=%u", __func__,
        ADDRESS_TO_LOGGABLE_CSTR(address), snk_audio_locations);
    topshim::rust::internal::sink_audio_location_available_cb(
        address, snk_audio_locations);
  }

  void OnAudioLocalCodecCapabilities(
      std::vector<le_audio::btle_audio_codec_config_t> local_input_capa_codec_conf,
      std::vector<le_audio::btle_audio_codec_config_t> local_output_capa_codec_conf) {
    LOG_INFO("%s", __func__);
    topshim::rust::internal::audio_local_codec_capabilities_cb(
        local_input_capa_codec_conf, local_output_capa_codec_conf);
  }

  void OnAudioGroupCodecConf(
      int group_id, le_audio::btle_audio_codec_config_t input_codec_conf,
      le_audio::btle_audio_codec_config_t output_codec_conf,
      std::vector<le_audio::btle_audio_codec_config_t> input_selectable_codec_conf,
      std::vector<le_audio::btle_audio_codec_config_t> output_selectable_codec_conf) {
    LOG_INFO("%s gid=%d", __func__, group_id);
    topshim::rust::internal::audio_group_codec_conf_cb(
        group_id, input_codec_conf, output_codec_conf,
        input_selectable_codec_conf, output_selectable_codec_conf);
  }
};

void LeAudioClientIntf::init(/*
     LeAudioClientCallbacks* callbacks,
     const std::vector<le_audio::btle_audio_codec_config_t>& offloading_preference */) {
  return intf_->Initialize(DBusLeAudioClientCallbacks::GetInstance(), {});
}

void LeAudioClientIntf::connect(RawAddress addr) {
  return intf_->Connect(addr);
}

void LeAudioClientIntf::disconnect(RawAddress addr) {
  return intf_->Disconnect(addr);
}

void LeAudioClientIntf::set_enable_state(RawAddress addr, bool enabled) {
  return intf_->SetEnableState(addr, enabled);
}

void LeAudioClientIntf::cleanup() {
  return intf_->Cleanup();
}

void LeAudioClientIntf::remove_device(RawAddress addr) {
  return intf_->RemoveDevice(addr);
}

void LeAudioClientIntf::group_add_node(int group_id, RawAddress addr) {
  return intf_->GroupAddNode(group_id, addr);
}

void LeAudioClientIntf::group_remove_node(int group_id, RawAddress addr) {
  return intf_->GroupRemoveNode(group_id, addr);
}

void LeAudioClientIntf::group_set_active(int group_id) {
  return intf_->GroupSetActive(group_id);
}

void LeAudioClientIntf::set_codec_config_preference(int group_id,
    BtLeAudioCodecConfig input_codec_config,
    BtLeAudioCodecConfig output_codec_config) {
  return intf_->SetCodecConfigPreference(
      group_id,
      internal::from_rust_btle_audio_codec_config(input_codec_config),
      internal::from_rust_btle_audio_codec_config(output_codec_config));
}

void LeAudioClientIntf::set_ccid_information(int ccid, int context_type) {
  return intf_->SetCcidInformation(ccid, context_type);
}

void LeAudioClientIntf::set_in_call(bool in_call) {
  return intf_->SetInCall(in_call);
}

void LeAudioClientIntf::send_audio_profile_preferences(int group_id,
    bool is_output_preference_le_audio,
    bool is_duplex_preference_le_audio) {
  return intf_->SendAudioProfilePreferences(group_id,
      is_output_preference_le_audio, is_duplex_preference_le_audio);
}

std::unique_ptr<LeAudioClientIntf> GetLeAudioClientProfile(
    const unsigned char* btif) {
  if (internal::g_lea_client_if) std::abort();

  const bt_interface_t* btif_ = reinterpret_cast<const bt_interface_t*>(btif);

  auto lea_client_if = std::make_unique<LeAudioClientIntf>(
      const_cast<le_audio::LeAudioClientInterface*>(
        reinterpret_cast<const le_audio::LeAudioClientInterface*>(
          btif_->get_profile_interface("le_audio"))));

  internal::g_lea_client_if = lea_client_if.get();

  return lea_client_if;
}
}  // namespace rust
}  // namespace topshim
}  // namespace bluetooth
