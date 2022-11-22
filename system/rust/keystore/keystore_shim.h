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

#include "gd/storage/keystore_interface.h"
#include "keystore.rs.h"

namespace bluetooth {
namespace keystore {
class BluetoothKeystoreInterfaceImpl final
    : public storage::BluetoothKeystoreInterface {
 public:
  BluetoothKeystoreInterfaceImpl(rust::Box<KeystoreInterfaceImpl> impl)
      : impl_(std::move(impl)){};

  virtual void StoreKey(std::string prefix, std::string value) {
    impl_->store_key(prefix, value);
  }

  virtual std::string GetKey(std::string prefix) {
    return std::string(impl_->get_key(prefix));
  }

 private:
  rust::Box<KeystoreInterfaceImpl> impl_;
};

std::unique_ptr<storage::BluetoothKeystoreInterface> GetInterface(
    rust::Box<KeystoreInterfaceImpl> impl) {
  return std::make_unique<BluetoothKeystoreInterfaceImpl>(std::move(impl));
}

}  // namespace keystore
}  // namespace bluetooth
