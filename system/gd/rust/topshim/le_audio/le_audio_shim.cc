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

#include "gd/rust/topshim/le_audio/le_audio_shim.h"

#include "gd/os/log.h"
#include "src/profiles/le_audio.rs.h"
#include "types/raw_address.h"

namespace rusty = ::bluetooth::topshim::rust;

namespace bluetooth {
namespace topshim {
namespace rust {
namespace internal {
static LeAudioIntf* g_le_audio_if;

static void connection_state_cb(bluetooth::headset::bthf_connection_state_t state, RawAddress* addr) {
  rusty::hfp_connection_state_callback(state, *addr);
}

static void audio_state_cb(bluetooth::headset::bthf_audio_state_t state, RawAddress* addr) {
  rusty::hfp_audio_state_callback(state, *addr);
}
}  // namespace internal

class DBusHeadsetCallbacks : public headset::Callbacks {
 public:
  static Callbacks* GetInstance(headset::Interface* headset) {
    static Callbacks* instance = new DBusHeadsetCallbacks(headset);
    return instance;
  }

  DBusHeadsetCallbacks(headset::Interface* headset) : headset_(headset){};

  // headset::Callbacks
  void ConnectionStateCallback(headset::bthf_connection_state_t state, RawAddress* bd_addr) override {
    LOG_INFO("ConnectionStateCallback from %s", ADDRESS_TO_LOGGABLE_CSTR(*bd_addr));
    topshim::rust::internal::connection_state_cb(state, bd_addr);
  }

  void AudioStateCallback(headset::bthf_audio_state_t state, RawAddress* bd_addr) override {
    LOG_INFO("AudioStateCallback %u from %s", state, ADDRESS_TO_LOGGABLE_CSTR(*bd_addr));
    topshim::rust::internal::audio_state_cb(state, bd_addr);
  }

 private:
  headset::Interface* headset_;
};

int LeAudioIntf::init() {
  return intf_->Initialize(DBusHeadsetCallbacks::GetInstance(intf_), {});
}

void LeAudioIntf::cleanup() {

}

uint32_t LeAudioIntf::connect(RawAddress addr) {
  return intf_->Connect(&addr);
}

int LeAudioIntf::connect_audio(RawAddress addr, bool sco_offload, bool force_cvsd) {
  intf_->SetScoOffloadEnabled(sco_offload);
  return intf_->ConnectAudio(&addr, force_cvsd);
}

int LeAudioIntf::set_active_device(RawAddress addr) {
  return intf_->SetActiveDevice(&addr);
}

int LeAudioIntf::set_volume(int8_t volume, RawAddress addr) {
  return intf_->VolumeControl(headset::bthf_volume_type_t::BTHF_VOLUME_TYPE_SPK, volume, &addr);
}

uint32_t LeAudioIntf::disconnect(RawAddress addr) {
  return intf_->Disconnect(&addr);
}

int LeAudioIntf::disconnect_audio(RawAddress addr) {
  return intf_->DisconnectAudio(&addr);
}

uint32_t LeAudioIntf::device_status_notification(TelephonyDeviceStatus status, RawAddress addr) {
  return intf_->DeviceStatusNotification(
      status.network_available ? headset::BTHF_NETWORK_STATE_AVAILABLE
                               : headset::BTHF_NETWORK_STATE_NOT_AVAILABLE,
      status.roaming ? headset::BTHF_SERVICE_TYPE_ROAMING : headset::BTHF_SERVICE_TYPE_HOME,
      status.signal_strength,
      status.battery_level,
      &addr);
}

uint32_t LeAudioIntf::indicator_query_response(
    TelephonyDeviceStatus device_status, PhoneState phone_state, RawAddress addr) {
  return intf_->CindResponse(
      device_status.network_available ? 1 : 0,
      phone_state.num_active,
      phone_state.num_held,
      topshim::rust::internal::from_rust_call_state(phone_state.state),
      device_status.signal_strength,
      device_status.roaming ? 1 : 0,
      device_status.battery_level,
      &addr);
}

uint32_t LeAudioIntf::current_calls_query_response(
    const ::rust::Vec<CallInfo>& call_list, RawAddress addr) {
  for (const auto& c : call_list) {
    std::string number{c.number};
    intf_->ClccResponse(
        c.index,
        c.dir_incoming ? headset::BTHF_CALL_DIRECTION_INCOMING
                       : headset::BTHF_CALL_DIRECTION_OUTGOING,
        topshim::rust::internal::from_rust_call_state(c.state),
        /*mode=*/headset::BTHF_CALL_TYPE_VOICE,
        /*multi_party=*/headset::BTHF_CALL_MPTY_TYPE_SINGLE,
        number.c_str(),
        /*type=*/headset::BTHF_CALL_ADDRTYPE_UNKNOWN,
        &addr);
  }

  // NULL termination (Completes response)
  return intf_->ClccResponse(
      /*index=*/0,
      /*dir=*/(headset::bthf_call_direction_t)0,
      /*state=*/(headset::bthf_call_state_t)0,
      /*mode=*/(headset::bthf_call_mode_t)0,
      /*multi_party=*/(headset::bthf_call_mpty_type_t)0,
      /*number=*/"",
      /*type=*/(headset::bthf_call_addrtype_t)0,
      &addr);
}

uint32_t LeAudioIntf::phone_state_change(
    PhoneState phone_state, const ::rust::String& number_rs, RawAddress addr) {
  std::string number{number_rs};
  return intf_->PhoneStateChange(
      phone_state.num_active,
      phone_state.num_held,
      topshim::rust::internal::from_rust_call_state(phone_state.state),
      number.c_str(),
      /*type=*/(headset::bthf_call_addrtype_t)0,
      /*name=*/"",
      &addr);
}

uint32_t LeAudioIntf::simple_at_response(bool ok, RawAddress addr) {
  return intf_->AtResponse(
      (ok ? headset::BTHF_AT_RESPONSE_OK : headset::BTHF_AT_RESPONSE_ERROR), 0, &addr);
}

std::unique_ptr<LeAudioIntf> GetHfpProfile(const unsigned char* btif) {
  if (internal::g_hfpif) std::abort();

  const bt_interface_t* btif_ = reinterpret_cast<const bt_interface_t*>(btif);

  auto hfpif = std::make_unique<LeAudioIntf>(const_cast<headset::Interface*>(
      reinterpret_cast<const headset::Interface*>(btif_->get_profile_interface("handsfree"))));
  internal::g_hfpif = hfpif.get();

  return hfpif;
}

}  // namespace rust
}  // namespace topshim
}  // namespace bluetooth
