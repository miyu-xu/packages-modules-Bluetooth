/*
 * Copyright 2025 The Android Open Source Project
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

#include "component_disabler.h"

#include <android/binder_manager.h>
#include <android/content/pm/IPackageManager.h>
#include <android/content/pm/PackageManager.h>
#include <binder/IServiceManager.h>
#include <binder/Status.h>
#include <bluetooth/log.h>
#include <private/android_filesystem_config.h>  // for AID_BLUETOOTH
#include <unistd.h>                             // for getuid()
#include <utils/String16.h>

namespace bluetooth {
namespace common {

const std::string ComponentDisabler::default_bt_package_name_ = "com.google.android.bluetooth";

bool ComponentDisabler::DisableComponents(const std::string& package_name) {
  log::info("Disabling enabled components for package: %s", package_name.c_str());

  // 1. Get the PackageManager binder
  ::ndk::SpAIBinder pmBinder(AServiceManager_checkService("package"));
  if (!pmBinder.get()) {
    log::warn("Failed to get PackageManager binder");
    return false;
  }

  // 2. Get the IPackageManager interface
  std::shared_ptr<aidl::android::content::pm::IPackageManager> packageManager =
          aidl::android::content::pm::IPackageManager::fromBinder(pmBinder);

  if (!packageManager) {
    log::warn("Failed to get IPackageManager interface");
    return false;
  }

  // 3. Get the current user ID
  int32_t userId = ::getuid() / 100000;

  // 4. Get the list of enabled components.
  std::vector<std::string> componentsToDisable;
  ::ndk::ScopedAStatus status;
  std::shared_ptr<aidl::android::content::pm::ParceledListSlice> components;

  String16 packageName16(package_name.c_str());
  status = packageManager->getComponentEnabledSettingList(packageName16, userId, &components);
  if (!status.isOk()) {
    log::warn("Failed to getComponentEnabledSettingList for package %s: %s", package_name.c_str(),
              status.getDescription().c_str());
    return false;
  }

  if (components == nullptr) {
    log::info("getComponentEnabledSettingList returned nullptr for %s", package_name.c_str());
    return false;
  }

  std::vector<aidl::android::content::pm::ComponentInfo> componentList;
  status = components->getList(&componentList);
  if (!status.isOk()) {
    log::warn("Failed to getList for package %s: %s", package_name.c_str(),
              status.getDescription().c_str());
    return false;
  }

  for (const auto& componentInfo : componentList) {
    std::optional<String16> name;
    status = componentInfo.getName(&name);
    if (!status.isOk() || !name.has_value()) {
      log::warn("Failed to component name: %s", status.getDescription().c_str());
      continue;
    }

    bool enabled = false;
    status = componentInfo.getEnabled(&enabled);
    if (!status.isOk()) {
      log::warn("Failed to get component enabled: %s", status.getDescription().c_str());
      continue;
    }

    if (enabled) {
      std::string compName = std::string(String8(name.value()).c_str());
      componentsToDisable.push_back(compName);
    }
  }

  // 5. Check if there are any components to disable.
  if (componentsToDisable.empty()) {
    log::info("No enabled components found to disable for package: %s", package_name.c_str());
    return true;
  }

  // 6. Iterate and disable components
  bool all_success = true;
  for (const auto& componentNameStr : componentsToDisable) {
    ::ndk::ScopedAStatus status;

    // Convert std::string to String16
    String16 componentName(componentNameStr.c_str());

    // Disable the component.
    status = packageManager->setComponentEnabledSetting(
            componentName,
            aidl::android::content::pm::PackageManager::COMPONENT_ENABLED_STATE_DISABLED,
            aidl::android::content::pm::PackageManager::DONT_KILL_APP |
                    aidl::android::content::pm::PackageManager::SYNCHRONOUS,
            userId);

    if (!status.isOk()) {
      log::warn("Failed to disable component %s: %s", componentNameStr.c_str(),
                status.getDescription().c_str());
      all_success = false;
    } else {
      log::info("Disabled component: %s", componentNameStr.c_str());
    }
  }

  return all_success;
}

bool ComponentDisabler::DisableComponents() { return DisableComponents(default_bt_package_name_); }
}  // namespace common
}  // namespace bluetooth