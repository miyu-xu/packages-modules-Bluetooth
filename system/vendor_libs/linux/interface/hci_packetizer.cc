//
// Copyright 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include "hci_packetizer.h"

#define LOG_TAG "android.hardware.bluetooth.hci_packetizer"
#include <dlfcn.h>
#include <fcntl.h>
#include <log/log.h>

namespace {

const size_t header_size_for_type[] = {0,
                                       HCI_COMMAND_PREAMBLE_SIZE,
                                       HCI_ACL_PREAMBLE_SIZE,
                                       HCI_SCO_PREAMBLE_SIZE,
                                       HCI_EVENT_PREAMBLE_SIZE,
                                       HCI_ISO_PREAMBLE_SIZE};
const size_t packet_length_offset_for_type[] = {0,
                                                HCI_LENGTH_OFFSET_CMD,
                                                HCI_LENGTH_OFFSET_ACL,
                                                HCI_LENGTH_OFFSET_SCO,
                                                HCI_LENGTH_OFFSET_EVT,
                                                HCI_LENGTH_OFFSET_ISO};

size_t HciGetPacketLengthForType(HciPacketType type,
                                 const std::vector<uint8_t>& preamble) {
  size_t offset = packet_length_offset_for_type[type];
  if (type != HCI_PACKET_TYPE_ACL_DATA) return preamble[offset];
  return (((preamble[offset + 1]) << 8) | preamble[offset]);
}

}  // namespace

namespace android {
namespace hardware {
namespace bluetooth {
namespace hci {

const hidl_vec<uint8_t>& HciPacketizer::GetPacket() const { return packet_; }

bool HciPacketizer::OnDataReady(HciPacketType packet_type,
                                const std::vector<uint8_t>& buffer,
                                size_t offset) {
  bool packet_completed = false;
  size_t bytes_available = buffer.size() - offset;
  switch (state_) {
    case HCI_HEADER: {
      size_t header_size =
          header_size_for_type[static_cast<size_t>(packet_type)];
      if (bytes_remaining_ == 0) {
        bytes_remaining_ = header_size;
        packet_buffer_.clear();
      }
      size_t bytes_to_copy = std::min(bytes_remaining_, bytes_available);
      packet_buffer_.insert(packet_buffer_.end(), buffer.begin() + offset,
                            buffer.begin() + offset + bytes_to_copy);
      bytes_remaining_ -= bytes_to_copy;
      bytes_available -= bytes_to_copy;
      if (bytes_remaining_ == 0) {
        bytes_remaining_ =
            HciGetPacketLengthForType(packet_type, packet_buffer_);
        if (bytes_remaining_ > 0) {
          state_ = HCI_PAYLOAD;
          if (bytes_available > 0) {
            packet_completed =
                OnDataReady(packet_type, buffer, offset + bytes_to_copy);
          }
        } else {
          packet_completed = true;
        }
      }
      break;
    }

    case HCI_PAYLOAD: {
      size_t bytes_to_copy = std::min(bytes_remaining_, bytes_available);
      packet_buffer_.insert(packet_buffer_.end(), buffer.begin() + offset,
                            buffer.begin() + offset + bytes_to_copy);
      bytes_remaining_ -= bytes_to_copy;
      bytes_available -= bytes_to_copy;
      if (bytes_remaining_ == 0) {
        state_ = HCI_HEADER;
        packet_completed = true;
      }
      break;
    }
  }
  if (packet_completed) {
    packet_.setToExternal(packet_buffer_.data(), packet_buffer_.size());
  }
  return packet_completed;
}

}  // namespace hci
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
