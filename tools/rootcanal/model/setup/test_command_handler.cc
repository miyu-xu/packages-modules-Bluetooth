/*
 * Copyright 2018 The Android Open Source Project
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

#include "test_command_handler.h"

#include <stdlib.h>

#include <fstream>
#include <memory>
#include <regex>

#include "device_boutique.h"
#include "os/log.h"
#include "osi/include/osi.h"
#include "phy.h"

using std::vector;

namespace rootcanal {

TestCommandHandler::TestCommandHandler(TestModel& test_model)
    : model_(test_model) {
#define SET_HANDLER(command_name, method)                                     \
  active_commands_[command_name] = [this](const vector<std::string>& param) { \
    method(param);                                                            \
  };
  SET_HANDLER("add", Add);
  SET_HANDLER("add_remote", AddRemote);
  SET_HANDLER("del", Del);
  SET_HANDLER("list", List);
  SET_HANDLER("set_device_address", SetDeviceAddress);
  SET_HANDLER("set_timer_period", SetTimerPeriod);
  SET_HANDLER("start_timer", StartTimer);
  SET_HANDLER("stop_timer", StopTimer);
  SET_HANDLER("reset", Reset);
#undef SET_HANDLER
}

void TestCommandHandler::AddDefaults() {
  // Add default test devices and add the devices to the phys
  // Add({"beacon", "be:ac:10:00:00:01", "1000"});

  // Add({"keyboard", "cc:1c:eb:0a:12:d1", "500"});

  // Add({"classic", "c1:a5:51:c0:00:01", "22"});

  // Add({"car_kit", "ca:12:1c:17:00:01", "238"});

  // Add({"sniffer", "ca:12:1c:17:00:01"});

  // Add({"sniffer", "3c:5a:b4:04:05:06"});
  // Add({"remote_loopback_device", "10:0d:00:ba:c1:06"});
  List({});

  SetTimerPeriod({"10"});
  StartTimer({});
}

void TestCommandHandler::HandleCommand(const std::string& name,
                                       const vector<std::string>& args) {
  if (active_commands_.count(name) == 0) {
    response_string_ = "Unhandled command: " + name;
    send_response_(response_string_);
    return;
  }
  active_commands_[name](args);
}

void TestCommandHandler::FromFile(const std::string& file_name) {
  if (file_name.size() == 0) {
    return;
  }

  std::ifstream file(file_name.c_str());

  const std::regex re("\\s+");

  std::string line;
  while (std::getline(file, line)) {
    auto begin = std::sregex_token_iterator(line.begin(), line.end(), re, -1);
    auto end = std::sregex_token_iterator();
    auto params = std::vector<std::string>(std::next(begin), end);

    HandleCommand(*begin, params);
  }

  if (file.fail()) {
    LOG_ERROR("Error reading commands from file.");
    return;
  }
}

void TestCommandHandler::RegisterSendResponse(
    const std::function<void(const std::string&)> callback) {
  send_response_ = callback;
  send_response_("RegisterSendResponse called");
}

void TestCommandHandler::Add(const vector<std::string>& args) {
  if (args.size() < 1) {
    response_string_ = "TestCommandHandler 'add' takes an argument";
    send_response_(response_string_);
    return;
  }
  std::shared_ptr<Device> new_dev = DeviceBoutique::Create(args);

  if (new_dev == NULL) {
    response_string_ = "TestCommandHandler 'add' " + args[0] + " failed!";
    send_response_(response_string_);
    LOG_WARN("%s", response_string_.c_str());
    return;
  }

  LOG_INFO("Add %s", new_dev->ToString().c_str());
  size_t dev_index = model_.Add(new_dev);
  response_string_ =
      std::to_string(dev_index) + std::string(":") + new_dev->ToString();
  send_response_(response_string_);
}

void TestCommandHandler::AddRemote(const vector<std::string>& args) {
  if (args.size() < 3) {
    response_string_ =
        "TestCommandHandler usage: add_remote host port phy_type";
    send_response_(response_string_);
    return;
  }

  size_t port = std::stoi(args[1]);
  Phy::Type phy_type = Phy::Type::BR_EDR;
  if ("LOW_ENERGY" == args[2]) {
    phy_type = Phy::Type::LOW_ENERGY;
  }
  if (port == 0 || port > 0xffff || args[0].size() < 2) {
    response_string_ = "TestCommandHandler bad arguments to 'add_remote': ";
    response_string_ += args[0];
    response_string_ += "@";
    response_string_ += args[1];
    send_response_(response_string_);
    return;
  }

  model_.AddRemote(args[0], port, phy_type);

  response_string_ = args[0] + std::string("@") + std::to_string(port);
  send_response_(response_string_);
}

void TestCommandHandler::Del(const vector<std::string>& args) {
  size_t dev_index = std::stoi(args[0]);

  model_.Del(dev_index);
  response_string_ = "TestCommandHandler 'del' called with device at index " +
                     std::to_string(dev_index);
  send_response_(response_string_);
}

void TestCommandHandler::List(const vector<std::string>& args) {
  if (args.size() > 0) {
    LOG_INFO("Unused args: arg[0] = %s", args[0].c_str());
    return;
  }
  send_response_(model_.List());
}

void TestCommandHandler::SetDeviceAddress(const vector<std::string>& args) {
  if (args.size() != 2) {
    response_string_ =
        "TestCommandHandler 'set_device_address' takes two arguments";
    send_response_(response_string_);
    return;
  }
  size_t device_id = std::stoi(args[0]);
  Address device_address{};
  Address::FromString(args[1], device_address);
  model_.SetDeviceAddress(device_id, device_address);
  response_string_ = "set_device_address " + args[0];
  response_string_ += " ";
  response_string_ += args[1];
  send_response_(response_string_);
}

void TestCommandHandler::SetTimerPeriod(const vector<std::string>& args) {
  if (args.size() != 1) {
    LOG_INFO("SetTimerPeriod takes 1 argument");
  }
  size_t period = std::stoi(args[0]);
  if (period != 0) {
    response_string_ = "set timer period to ";
    response_string_ += args[0];
    model_.SetTimerPeriod(std::chrono::milliseconds(period));
  } else {
    response_string_ = "invalid timer period ";
    response_string_ += args[0];
  }
  send_response_(response_string_);
}

void TestCommandHandler::StartTimer(const vector<std::string>& args) {
  if (args.size() > 0) {
    LOG_INFO("Unused args: arg[0] = %s", args[0].c_str());
  }
  model_.StartTimer();
  response_string_ = "timer started";
  send_response_(response_string_);
}

void TestCommandHandler::StopTimer(const vector<std::string>& args) {
  if (args.size() > 0) {
    LOG_INFO("Unused args: arg[0] = %s", args[0].c_str());
  }
  model_.StopTimer();
  response_string_ = "timer stopped";
  send_response_(response_string_);
}

void TestCommandHandler::Reset(const std::vector<std::string>& args) {
  if (args.size() > 0) {
    LOG_INFO("Unused args: arg[0] = %s", args[0].c_str());
  }
  model_.Reset();
  response_string_ = "model reset";
  send_response_(response_string_);
}

}  // namespace rootcanal
