// Copyright 2022, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma once

#include <memory>

#include "core/hci_shim.h"
#include "gd/storage/classic_device.h"
#include "gd/storage/device.h"
#include "gd/storage/le_device.h"
#include "gd/storage/mutation_entry.h"
#include "gd/storage/storage_module.h"
#include "rust/cxx.h"

namespace bluetooth {
namespace storage {
namespace rust_shim {

using PropertyType = MutationEntry::PropertyType;

std::unique_ptr<Mutation> ModifyOnHeap(StorageModule& module) {
  return std::make_unique<Mutation>(module.Modify());
}

std::unique_ptr<MutationEntry> Set(PropertyType property_type,
                                   rust::Str section_param,
                                   rust::Str property_param,
                                   rust::Str value_param) {
  return std::make_unique<MutationEntry>(
      MutationEntry::Set(property_type, std::string(std::move(section_param)),
                         std::string(std::move(property_param)),
                         std::string(std::move(value_param))));
}

void Add(Mutation& mutation, std::unique_ptr<MutationEntry> entry) {
  mutation.Add(std::move(*entry.get()));
}

std::unique_ptr<std::vector<Device>> GetBondedDevices(
    const StorageModule& module) {
  return std::make_unique<std::vector<Device>>(
      std::move(module.GetBondedDevices()));
}

bluetooth::hci::rust_shim::RawAddress GetAddress(const Device& device) {
  return {device.GetAddress().address};
}

hci::DeviceType GetDeviceType(const Device& device) {
  if (device.GetDeviceType().has_value()) {
    return device.GetDeviceType().value();
  } else {
    return hci::DeviceType::UNKNOWN;
  }
}

std::unique_ptr<ClassicDevice> Classic(Device& device) {
  return std::make_unique<ClassicDevice>(std::move(device.Classic()));
}

std::unique_ptr<LeDevice> Le(Device& device) {
  return std::make_unique<LeDevice>(std::move(device.Le()));
}

#define GENERATE_PROPERTY(BASE_TYPE, NAME)                              \
  std::unique_ptr<std::string> Get##NAME(const BASE_TYPE& device) {     \
    if (device.Get##NAME().has_value()) {                               \
      return std::make_unique<std::string>(device.Get##NAME().value()); \
    } else {                                                            \
      return {};                                                        \
    }                                                                   \
  }                                                                     \
                                                                        \
  std::unique_ptr<MutationEntry> Set##NAME(BASE_TYPE& device,           \
                                           rust::Str value) {           \
    return std::make_unique<MutationEntry>(                             \
        device.Set##NAME(std::string(std::move(value))));               \
  }

GENERATE_PROPERTY(ClassicDevice, RawLinkKey);

GENERATE_PROPERTY(LeDevice, LocalId);
GENERATE_PROPERTY(LeDevice, PeerId);
GENERATE_PROPERTY(LeDevice, LocalEncryptionKeys);
GENERATE_PROPERTY(LeDevice, PeerEncryptionKeys);
GENERATE_PROPERTY(LeDevice, LocalSignatureResolvingKeys);
GENERATE_PROPERTY(LeDevice, PeerSignatureResolvingKeys);
}  // namespace rust_shim
}  // namespace storage
}  // namespace bluetooth
