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

#pragma once

#include <base/bind.h>

#include <vector>

#include "arbiter_client_types.h"
#include "common/callback.h"
#include "hci/acl_manager.h"

namespace bluetooth {
namespace arbiter {
namespace internal {

class LeConnectionCallbackDispatcher : public hci::acl_manager::LeConnectionManagementCallbacks {
 public:
  LeConnectionCallbackDispatcher(
      base::Callback<std::vector<ArbiterClientWithConnection*>()> get_active_clients,
      std::mutex& lock)
      : get_active_clients_{std::move(get_active_clients)}, lock_{lock} {}

 private:
  virtual void OnConnectionUpdate(
      hci::ErrorCode hci_status,
      uint16_t connection_interval,
      uint16_t connection_latency,
      uint16_t supervision_timeout) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnConnectionUpdate(
          hci_status, connection_interval, connection_latency, supervision_timeout);
    }
  }

  virtual void OnDataLengthChange(
      uint16_t tx_octets, uint16_t tx_time, uint16_t rx_octets, uint16_t rx_time) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnDataLengthChange(tx_octets, tx_time, rx_octets, rx_time);
    }
  }

  virtual void OnDisconnection(hci::ErrorCode reason) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnDisconnection(reason);
    }
  }

  virtual void OnReadRemoteVersionInformationComplete(
      hci::ErrorCode hci_status,
      uint8_t lmp_version,
      uint16_t manufacturer_name,
      uint16_t sub_version) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnReadRemoteVersionInformationComplete(
          hci_status, lmp_version, manufacturer_name, sub_version);
    }
  }

  virtual void OnLeReadRemoteFeaturesComplete(
      hci::ErrorCode hci_status, uint64_t features) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnLeReadRemoteFeaturesComplete(hci_status, features);
    }
  }

  virtual void OnPhyUpdate(hci::ErrorCode hci_status, uint8_t tx_phy, uint8_t rx_phy) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnPhyUpdate(hci_status, tx_phy, rx_phy);
    }
  }

  virtual void OnLeSubrateChange(
      hci::ErrorCode hci_status,
      uint16_t subrate_factor,
      uint16_t peripheral_latency,
      uint16_t continuation_number,
      uint16_t supervision_timeout) override {
    std::scoped_lock lock{lock_};
    for (auto client : get_active_clients_.Run()) {
      client->callbacks->OnLeSubrateChange(
          hci_status, subrate_factor, peripheral_latency, continuation_number, supervision_timeout);
    }
  }

  base::Callback<std::vector<ArbiterClientWithConnection*>()> get_active_clients_;
  std::mutex& lock_;
};

}  // namespace internal
}  // namespace arbiter
}  // namespace bluetooth
