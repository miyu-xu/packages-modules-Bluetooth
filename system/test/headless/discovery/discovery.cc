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

#define LOG_TAG "bt_headless_discovery"

#include "test/headless/discovery/discovery.h"

#include <future>

#include "base/logging.h"  // LOG() stdout and android log
#include "btif/include/btif_api.h"
#include "osi/include/log.h"  // android log only
#include "stack/include/sdp_api.h"
// #include "test/headless/bt_property.h"
#include "test/headless/get_options.h"
#include "test/headless/handler.h"
#include "test/headless/headless.h"
#include "test/headless/interface.h"
#include "test/headless/log.h"
#include "test/headless/messenger.h"
#include "test/headless/sdp/sdp.h"
#include "test/headless/stopwatch.h"
#include "test/headless/tamper.h"
#include "test/headless/timeout.h"
#include "types/bluetooth/uuid.h"
#include "types/raw_address.h"

using namespace bluetooth::test::headless;
using namespace std::chrono_literals;

namespace {

class DiscoveryOpt : public ModOpt {
 public:
  unsigned get_num_passes() const {
    return static_cast<unsigned>(std::stoi(GetArg("-p", "0")));
  }

  int get_disconnect_time_sec() const { return std::stoi(GetArg("-d", "0")); }
};

int start_discovery(bluetooth::test::headless::Handler* handler,
                    const DiscoveryOpt* opts, const RawAddress& raw_address) {
  RawAddress bd_addr{raw_address};

  unsigned num_passes = opts->get_num_passes();
  int disconnect_time_sec = opts->get_disconnect_time_sec();

  Stopwatch acl_stopwatch("ACL_connection");

  if (disconnect_time_sec != 0) {
    // Add a random disconnect 4 seconds afterwards
    handler->Post(bluetooth::common::BindOnce(disconnector, handler, bd_addr,
                                              BT_TRANSPORT_BR_EDR));
  }

  LOG_CONSOLE("Started service discovery");
  double total_ms = 0;

  for (unsigned i = 0; i < num_passes; i++) {
    LOG_CONSOLE("  Pass:%d", i);
    Stopwatch sdp_stopwatch("SDP_discovery");
    auto check_point = messenger::sdp::get_check_point();
    ASSERT(bluetoothInterface.get_remote_services(&bd_addr, 0) ==
           BT_STATUS_SUCCESS);

    if (!messenger::acl::await_connected(8s)) {
      LOG_CONSOLE("TIMEOUT waiting for connection to %s",
                  raw_address.ToString().c_str());
      continue;
    }
    LOG_CONSOLE("ACL connected to %s :%s", STR(raw_address),
                STR(acl_stopwatch));

    if (!messenger::sdp::await_service_discovery(8s, check_point, 1UL)) {
      LOG_CONSOLE("TIMEOUT waiting for service discovery to %s",
                  raw_address.ToString().c_str());
      continue;
    }

    auto callback_queue = messenger::sdp::collect_from(check_point);
    const size_t queue_size = callback_queue.size();
    LOG_CONSOLE("queue size:%zu", queue_size);
    ASSERT_LOG(queue_size == 1, "Received unexpected number of SDP queries");

    auto params = callback_queue.front();
    callback_queue.pop_front();

    // Throw out the first SDP connection
    if (i != 0) total_ms += sdp_stopwatch.LapMs();
    LOG_CONSOLE("got remote services :%s %s", params.ToString().c_str(),
                STR(sdp_stopwatch));

    ASSERT_LOG(params.properties.size() == 1,
               "This callback only returns a single property");

    property::uuid_t* uuid_property =
        get_property_type<property::uuid_t>(params.properties[0]);
    auto uuids = uuid_property->get_uuids();

    for (const auto& uuid : uuids) {
      LOG_CONSOLE(" Uuid:%s", uuid.ToString().c_str());
    }
  }

  LOG_CONSOLE("Awaiting disconnect");
  if (!messenger::acl::await_disconnected(6s)) {
    LOG_CONSOLE("TIMEOUT waiting for disconnection to %s",
                raw_address.ToString().c_str());
    return -1;
  }

  LOG_CONSOLE("SDP time cnt:%u avg:%2.3fms", num_passes - 1,
              total_ms / (num_passes - 1));

  LOG_CONSOLE("Dumpsys system");
  bluetoothInterface.dump(2, nullptr);
  LOG_CONSOLE("Done dumpsys system");

  return 0;
}

}  // namespace

// extern uint8_t btu_trace_level;

int bluetooth::test::headless::Discovery::Run() {
  if (options_.loop_ < 1) {
    LOG_CONSOLE("This test requires at least a single loop");
    options_.Usage();
    return -1;
  }
  if (options_.device_.size() != 1) {
    LOG_CONSOLE("This test requires a single device specified");
    options_.Usage();
    return -1;
  }
  return RunOnHeadlessStack<int>([this]() {
    const DiscoveryOpt* opts = options_.get_module_options<DiscoveryOpt>();
    return start_discovery(headless_handler_, opts, options_.device_.front());
  });
}
