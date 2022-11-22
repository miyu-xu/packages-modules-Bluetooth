/*
 * Copyright 2020 The Android Open Source Project
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

#include <cstddef>
#include <list>
#include <map>
#include <string>

#include "types/bluetooth/uuid.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace test {
namespace headless {

class ModOpt {
 public:
  ModOpt() = default;
  ModOpt(const char* optarg);
  virtual ~ModOpt() {}

  std::list<std::string> GetStringList() const { return string_list_; }
  std::map<std::string, std::string> GetDefaultShortArgMap() const {
    return arg_map_;
  }

  std::string GetArg(std::string arg_key, std::string arg_default) const;

 protected:
  std::list<std::string> string_list_;
  std::map<std::string, std::string> arg_map_;
};

class GetOpt {
 public:
  GetOpt(int argc, char** arv);
  virtual ~GetOpt();

  virtual void Usage() const;
  virtual bool IsValid() const { return valid_; };

  std::string GetNextSubTest() const {
    std::string test = non_options_.front();
    non_options_.pop_front();
    return test;
  }

  const char** StackInitFlags() const;

  template <typename T>
  const T* get_module_options() const {
    return static_cast<const T*>(&mod_opt_);
  }

  std::list<RawAddress> device_;
  std::list<std::string> init_flags_;
  std::list<bluetooth::Uuid> uuid_;
  unsigned long loop_{1};
  unsigned long msec_{0};
  std::string pass_;

  bool close_stderr_{true};
  bool clear_logcat_{false};

  mutable std::list<std::string> non_options_;

  static std::vector<std::string> Split(std::string);

  static void ParseValue(char* optarg, std::list<std::string>& my_list);

 private:
  void ProcessOption(int option_index, char* optarg);
  void ParseStackInitFlags();
  const char* name_{nullptr};
  const char** stack_init_flags_{nullptr};
  bool valid_{true};
  ModOpt mod_opt_;
};

}  // namespace headless
}  // namespace test
}  // namespace bluetooth
