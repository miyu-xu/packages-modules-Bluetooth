/*
 * Copyright 2023 The Android Open Source Project
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

#ifndef MMC_MMC_INTERFACE_CODEC_CLIENT_FACTORY_H_
#define MMC_MMC_INTERFACE_CODEC_CLIENT_FACTORY_H_

#include <dbus/bus.h>

#include <memory>
#include <optional>

#include "mmc/codec_client/codec_client.h"
#include "mmc/mmc_interface/mmc_interface.h"
#include "mmc/socket_wrapper/socket_wrapper_impl.h"

namespace mmc {

// Generates a codec client with associated DBus and socket wrapper.
class CodecClientFactory {
 public:
  // Returns:
  //   codec client instance on succeed.
  //   "no value" on setup failure.
  std::optional<std::unique_ptr<MmcInterface>> CreateCodecClient() {
    // Set up DBus connection.
    dbus::Bus* bus;
    dbus::Bus::Options options;
    options.bus_type = dbus::Bus::SYSTEM;
    bus = new dbus::Bus(options);

    if (!bus->Connect()) {
      return std::nullopt;
    }

    // Set up SocketWrapperImpl
    return std::make_unique<CodecClient>(bus,
                                         std::make_unique<SocketWrapperImpl>());
  }
};

}  // namespace mmc

#endif  // MMC_MMC_INTERFACE_CODEC_CLIENT_FACTORY_H_
