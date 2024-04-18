/*
 * Copyright 2019 The Android Open Source Project
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
#define LOG_TAG "bt_gd_shim"

#include "dumpsys/dumpsys.h"

#include <android_bluetooth_flags.h>
#include <bluetooth/log.h>
#include <unistd.h>

#include <future>
#include <sstream>
#include <string>

#include "dumpsys/filter.h"
#include "dumpsys_data_generated.h"
#include "main/shim/stack.h"
#include "module.h"
#include "module_dumper.h"
#include "os/system_properties.h"
#include "shim/dumpsys.h"
#include "shim/dumpsys_args.h"

namespace bluetooth {
namespace shim {

static const std::string kReadOnlyDebuggableProperty = "ro.debuggable";

namespace {
constexpr char kModuleName[] = "shim::Dumpsys";
constexpr char kDumpsysTitle[] = "----- Gd Dumpsys ------";
}  // namespace

struct Dumpsys::impl {
 public:
  void DumpWithArgsSync(int fd, const char** args, std::promise<void> promise);

  impl(const Dumpsys& dumpsys_module);
  ~impl() = default;

 protected:
  std::string PrintAsJson(std::string* dumpsys_data) const;

  bool IsDebuggable() const;

 private:
  void DumpWithArgsAsync(int fd, const char** args) const;

  const Dumpsys& dumpsys_module_;
};

const ModuleFactory Dumpsys::Factory = ModuleFactory([]() { return new Dumpsys(); });

Dumpsys::impl::impl(const Dumpsys& dumpsys_module) : dumpsys_module_(dumpsys_module) {}

bool Dumpsys::impl::IsDebuggable() const {
  return (os::GetSystemProperty(kReadOnlyDebuggableProperty) == "1");
}

std::string Dumpsys::impl::PrintAsJson(std::string* dumpsys_data) const {
  log::assert_that(dumpsys_data != nullptr, "assert failed: dumpsys_data != nullptr");

  flatbuffers::IDLOptions options{};
  options.output_default_scalars_in_json = true;
  flatbuffers::Parser parser{options};

  std::string jsongen;
  // GenerateText was renamed to GenText in 23.5.26 because the return behavior was changed.
  // https://github.com/google/flatbuffers/commit/950a71ab893e96147c30dd91735af6db73f72ae0
#if FLATBUFFERS_VERSION_MAJOR < 23 ||   \
    (FLATBUFFERS_VERSION_MAJOR == 23 && \
     (FLATBUFFERS_VERSION_MINOR < 5 ||  \
      (FLATBUFFERS_VERSION_MINOR == 5 && FLATBUFFERS_VERSION_REVISION < 26)))
  flatbuffers::GenerateText(parser, dumpsys_data->data(), &jsongen);
#else
  const char* error = flatbuffers::GenText(parser, dumpsys_data->data(), &jsongen);
  if (error != nullptr) {
    log::warn("{}", error);
  }
#endif
  return jsongen;
}

void Dumpsys::impl::DumpWithArgsAsync(int fd, const char** args) const {
  ParsedDumpsysArgs parsed_dumpsys_args(args);
  const auto registry = dumpsys_module_.GetModuleRegistry();

  int dumper_fd = STDOUT_FILENO;
  if (IS_FLAG_ENABLED(dumpsys_use_passed_in_fd)) {
    dumper_fd = fd;
  }
  ModuleDumper dumper(dumper_fd, *registry, kDumpsysTitle);
  std::string dumpsys_data;
  std::ostringstream oss;
  dumper.DumpState(&dumpsys_data, oss);
  dprintf(fd, "%s", PrintAsJson(&dumpsys_data).c_str());
}

void Dumpsys::impl::DumpWithArgsSync(int fd, const char** args, std::promise<void> promise) {
  if (IS_FLAG_ENABLED(dumpsys_acquire_stack_when_executing)) {
    if (bluetooth::shim::Stack::GetInstance()->LockForDumpsys(
            [=, *this]() { this->DumpWithArgsAsync(fd, args); })) {
      log::info("Successful dumpsys procedure");
    } else {
      log::info("Failed dumpsys procedure as stack was not longer active");
    }
  } else {
    DumpWithArgsAsync(fd, args);
  }
  promise.set_value();
}

Dumpsys::Dumpsys() {}

// DEPRECATED Flag: dumpsys_acquire_stack_when_executing
void Dumpsys::Dump(int fd, const char** args) {
  if (fd <= 0) {
    return;
  }
  std::promise<void> promise;
  auto future = promise.get_future();
  CallOn(pimpl_.get(), &Dumpsys::impl::DumpWithArgsSync, fd, args, std::move(promise));
  future.get();
}
// !DEPRECATED Flag: dumpsys_acquire_stack_when_executing

void Dumpsys::Dump(int fd, const char** args, std::promise<void> promise) {
  if (fd <= 0) {
    promise.set_value();
    return;
  }
  CallOn(pimpl_.get(), &Dumpsys::impl::DumpWithArgsSync, fd, args, std::move(promise));
}

os::Handler* Dumpsys::GetGdShimHandler() {
  return GetHandler();
}

/**
 * Module methods
 */
void Dumpsys::ListDependencies(ModuleList* /* list */) const {}

void Dumpsys::Start() {
  pimpl_ = std::make_unique<impl>(*this);
}

void Dumpsys::Stop() {
  pimpl_.reset();
}

DumpsysDataFinisher Dumpsys::GetDumpsysData(flatbuffers::FlatBufferBuilder* fb_builder) const {
  auto name = fb_builder->CreateString("----- Shim Dumpsys -----");

  DumpsysModuleDataBuilder builder(*fb_builder);
  builder.add_title(name);
  builder.add_number_of_bundled_schemas(123);
  auto dumpsys_data = builder.Finish();

  return [dumpsys_data](DumpsysDataBuilder* builder) { builder->add_shim_dumpsys_data(dumpsys_data); };
}

std::string Dumpsys::ToString() const {
  return kModuleName;
}

}  // namespace shim
}  // namespace bluetooth
