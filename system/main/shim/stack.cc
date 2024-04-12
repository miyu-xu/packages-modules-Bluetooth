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

#include "main/shim/stack.h"

#include <bluetooth/log.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

#include <string>

#include "common/init_flags.h"
#include "common/strings.h"
#include "hal/hci_hal.h"
#include "hci/acl_manager.h"
#include "hci/acl_manager/acl_scheduler.h"
#include "hci/controller.h"
#include "hci/controller_interface.h"
#include "hci/distance_measurement_manager.h"
#include "hci/hci_layer.h"
#include "hci/le_advertising_manager.h"
#include "hci/le_scanning_manager.h"
#if TARGET_FLOSS
#include "hci/msft.h"
#endif
#include "hci/remote_name_request.h"
#include "hci/vendor_specific_event_manager.h"
#include "main/shim/acl.h"
#include "main/shim/acl_legacy_interface.h"
#include "main/shim/distance_measurement_manager.h"
#include "main/shim/entry.h"
#include "main/shim/hci_layer.h"
#include "main/shim/le_advertising_manager.h"
#include "main/shim/le_scanning_manager.h"
#include "metrics/counter_metrics.h"
#include "os/log.h"
#include "shim/dumpsys.h"
#include "storage/storage_module.h"
#include "sysprops/sysprops_module.h"

namespace bluetooth {
namespace shim {

using ::bluetooth::common::InitFlags;
using ::bluetooth::common::StringFormat;

struct Stack::impl {
  impl();
  ~impl()

  void Start();

  os::Thread* thread_;
  os::Handler* handler_;

  // Keep the modules here sorted by reverse dependency order
  // to improve init readability.
  Dumpsys* dumpsys_{nullptr};
  sysprops::SyspropsModule* sysprops_{nullptr};
  metrics::CounterMetrics* counter_metrics_{nullptr};
  storage::StorageModule* storage_{nullptr};
  hci::acl_manager::AclScheduler* acl_scheduler_{nullptr};

  hal::HciHal* hci_hal_{nullptr};
  hci::HciLayer* hci_layer_{nullptr};
  hci::Controller* controller_{nullptr};
  hci::VendorSpecificEventManager* vendor_specific_event_manager_{nullptr};
  hci::RemoteNameRequestModule* remote_name_request_{nullptr};
  hci::AclManager* acl_manager_{nullptr};
  hci::LeAdvertisingManager* le_advertising_manager_{nullptr};
  hci::LeScanningManager* le_scanning_manager_{nullptr};
  hci::DistanceMeasurementManager* distance_measurement_manager_{nullptr};
#if TARGET_FLOSS
  hci::MsftExtensionManager* msft_extension_manager_{nullptr};
#endif

  legacy::Acl* acl_{nullptr};
};

Stack::impl::impl() {
  thread_ = new os::Thread("gd_stack_thread", os::Thread::Priority::REAL_TIME);
  handler_ = new os::Handler(thread_);

  dumpsys_ = new Dumpsys();
  sysprops_ = new sysprops::SyspropsModule();
  counter_metrics_ = new metrics::CounterMetrics();
  storage_ = new storage::StorageModule(counter_metrics_);
  acl_scheduler_ = new hci::acl_manager::AclScheduler();

  hci_hal_ = new hal::HciHal();
  hci_layer_ = new hal::HciLayer(hci_hal_, storage_module_);
  controller_ = new hci::Controller(hci_layer_, sysprops_);
  vendor_specific_event_manager_ = new hci::VendorSpecificEventManager(hci_layer_, controller_);
  remote_name_request_ = new hci::RemoteNameRequestModule(hci_layer_, acl_scheduler_);
  acl_manager_ = new hci::AclManager(
      hci_layer_, controller_, storage_, acl_scheduler_, remote_name_request_);
  le_advertising_manager_ = new hci::LeAdvertisingManager(
      hci_layer_, controller_, acl_manager_, vendor_specific_event_manager_);
  le_scanning_manager_ = new hci::LeScanningManager(
      hci_layer_, controller_, acl_manager_, vendor_specific_event_manager_,
      storage_module_);
  distance_measurement_manager_ = new hci::DistanceMeasurementManager(
      hci_layer_, acl_manager_);
#if TARGET_FLOSS
  msft_extension_manager_ = new hci::MsftExtensionManager(
      hci_hal_, hci_layer_, vendor_specific_event_manager_);
#endif
}

void Stack::impl::Start(std::promise<()> promise) {
  dumpsys_->Start();
  sysprops_->Start();
  counter_metrics_->Start();
  storage_->Start();
  acl_scheduler_->Start();
  hci_hal_->Start();
  hci_layer_->Start();
  controller_->Start();
  vendor_specific_event_manager_->Start();
  remote_name_request_->Start();
  acl_manager_->Start();
  le_advertising_manager_->Start();
  le_scanning_manager_->Start();
  distance_measurement_manager_->Start();
#if TARGET_FLOSS
  msft_extension_manager_->Start();
#endif

  acl_ = new legacy::Acl(handler_, legacy::GetAclInterface(),
                                 controller_->GetLeFilterAcceptListSize(),
                                 controller_->GetLeResolvingListSize());

  promise.set_value();
}

Stack::Stack() {}

Stack* Stack::GetInstance() {
  static Stack instance;
  return &instance;
}

void Stack::Start() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(pimpl_ != nullptr, "Gd stack is already running");
  log::info("Starting Gd stack");

  pimpl_ = new Stack::impl();

  std::promise<void> promise;
  std::future<void> future = promise.get_future();

  pimpl_->handler_->Post(
      common::BindOnce(&impl::Start, common::Unretained(pimpl_), std::move(promise)));

  auto status = future.wait_for(std::chrono::milliseconds(3000));

  log::info("Gd stack start completed with status {}", int(status));
  log::assert_that(
      status == std::future_status::ready,
      "Gd stack failed to start within a 3000ms delay");

  bluetooth::shim::hci_on_reset_complete();
  bluetooth::shim::init_advertising_manager();
  bluetooth::shim::init_scanning_manager();
  bluetooth::shim::init_distance_measurement_manager();
}

void Stack::Stop() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  bluetooth::shim::hci_on_shutting_down();

  // Make sure gd acl flag is enabled and we started it up
  if (pimpl_->acl_ != nullptr) {
    pimpl_->acl_->FinalShutdown();
    delete pimpl_->acl_;
    pimpl_->acl_ = nullptr;
  }

  log::assert_that(is_running_, "Gd stack not running");
  is_running_ = false;

  stack_handler_->Clear();

  stack_manager_.ShutDown();

  delete stack_handler_;
  stack_handler_ = nullptr;

  stack_thread_->Stop();
  delete stack_thread_;
  stack_thread_ = nullptr;

  log::info("Successfully shut down Gd stack");
}

bool Stack::IsRunning() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  return is_running_;
}

legacy::Acl* Stack::GetAcl() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(is_running_, "assert failed: is_running_");
  log::assert_that(pimpl_->acl_ != nullptr,
                   "Acl shim layer has not been created");
  return pimpl_->acl_;
}

os::Handler* Stack::GetHandler() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(is_running_, "assert failed: is_running_");
  return stack_handler_;
}

bool Stack::IsDumpsysModuleStarted() const {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  return GetStackManager()->IsStarted<Dumpsys>();
}

bool Stack::LockForDumpsys(std::function<void()> dumpsys_callback) {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  if (is_running_) {
    dumpsys_callback();
  }
  return is_running_;
}

}  // namespace shim
}  // namespace bluetooth
