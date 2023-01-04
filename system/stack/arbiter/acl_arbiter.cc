/*
 * Copyright 2022 The Android Open Source Project
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

#include "stack/arbiter/acl_arbiter.h"

#include <base/bind.h>

#include <iterator>
#include <unordered_map>

#include "common/init_flags.h"
#include "os/log.h"
#include "osi/include/allocator.h"
#include "stack/include/btu.h"  // do_in_main_thread
#include "stack/include/l2c_api.h"

namespace bluetooth {
namespace shim {
namespace arbiter {

class PassthroughAclArbiter : public AclArbiter {
 public:
  virtual void OnLeConnect(const RawAddress& address,
                           uint16_t handle) override {
    // no-op
  }

  virtual void OnLeDisconnect(const RawAddress& address) override {
    // no-op
  }

  virtual InterceptAction InterceptAttPacket(const RawAddress& address,
                                             const BT_HDR* packet) override {
    return InterceptAction::FORWARD;
  }

  static PassthroughAclArbiter& Get() {
    static auto singleton = PassthroughAclArbiter();
    return singleton;
  }
};

namespace {
struct RustArbiterCallbacks {
  ::rust::Fn<void(uint16_t handle)> on_le_connect;
  ::rust::Fn<void(uint16_t handle)> on_le_disconnect;
  ::rust::Fn<InterceptAction(uint16_t handle, ::rust::Vec<uint8_t> buffer)>
      intercept_packet;
};

RustArbiterCallbacks callbacks_{};
}  // namespace

class RustGattAclArbiter : public AclArbiter {
 public:
  virtual void OnLeConnect(const RawAddress& address,
                           uint16_t handle) override {
    LOG_INFO("Notifying Rust of LE connection");
    address_to_handle_[address] = handle;
    handle_to_address_[handle] = address;
    callbacks_.on_le_connect(handle);
  }

  virtual void OnLeDisconnect(const RawAddress& address) override {
    LOG_INFO("Notifying Rust of LE disconnection");
    if (address_to_handle_.find(address) != address_to_handle_.end()) {
      auto handle = address_to_handle_[address];
      handle_to_address_.erase(handle);
      callbacks_.on_le_disconnect(handle);
      address_to_handle_.erase(address);
    }
  }

  virtual InterceptAction InterceptAttPacket(const RawAddress& address,
                                             const BT_HDR* packet) override {
    LOG_INFO("Intercepting ATT packet and forwarding to Rust");
    // ignore weird packets
    if (packet->len <= 1) {
      return InterceptAction::FORWARD;
    }

    if (address_to_handle_.find(address) != address_to_handle_.end()) {
      auto handle = address_to_handle_[address];

      uint8_t* packet_start = (uint8_t*)(packet + 1) + packet->offset;
      uint8_t* packet_end = packet_start + packet->len;

      auto vec = ::rust::Vec<uint8_t>();
      std::copy(packet_start, packet_end, std::back_inserter(vec));
      return callbacks_.intercept_packet(handle, std::move(vec));
    } else {
      return InterceptAction::FORWARD;
    }
  }

  void SendPacketToPeer(uint16_t handle, ::rust::Vec<uint8_t> buffer) {
    if (handle_to_address_.find(handle) != handle_to_address_.end()) {
      auto address = handle_to_address_[handle];
      BT_HDR* p_buf = (BT_HDR*)osi_malloc(sizeof(BT_HDR) + buffer.size() +
                                          L2CAP_MIN_OFFSET);
      if (p_buf == nullptr) {
        LOG_ALWAYS_FATAL("OOM when sending packet");
      }
      auto p = (uint8_t*)(p_buf + 1) + L2CAP_MIN_OFFSET;
      std::copy(buffer.begin(), buffer.end(), p);
      p_buf->offset = L2CAP_MIN_OFFSET;
      p_buf->len = buffer.size();
      L2CA_SendFixedChnlData(4, address, p_buf);
    } else {
      LOG_ERROR("Dropping packet since connection no longer exists");
    }
  }

  static RustGattAclArbiter& Get() {
    static auto singleton = RustGattAclArbiter();
    return singleton;
  }

 private:
  std::unordered_map<RawAddress, uint16_t> address_to_handle_{};
  std::unordered_map<uint16_t, RawAddress> handle_to_address_{};
};

void StoreCallbacksFromRust(
    ::rust::Fn<void(uint16_t handle)> on_le_connect,
    ::rust::Fn<void(uint16_t handle)> on_le_disconnect,
    ::rust::Fn<InterceptAction(uint16_t handle, ::rust::Vec<uint8_t> buffer)>
        intercept_packet) {
  LOG_INFO("Received callbacks from Rust, registering in Arbiter");
  callbacks_ = {on_le_connect, on_le_disconnect, intercept_packet};
}

void SendPacketToPeer(uint16_t handle, ::rust::Vec<uint8_t> buffer) {
  do_in_main_thread(FROM_HERE,
                    base::Bind(&RustGattAclArbiter::SendPacketToPeer,
                               base::Unretained(&RustGattAclArbiter::Get()),
                               handle, std::move(buffer)));
}

AclArbiter& GetArbiter() {
  return common::init_flags::private_gatt_is_enabled()
             ? static_cast<AclArbiter&>(RustGattAclArbiter::Get())
             : static_cast<AclArbiter&>(PassthroughAclArbiter::Get());
}

}  // namespace arbiter
}  // namespace shim
}  // namespace bluetooth
