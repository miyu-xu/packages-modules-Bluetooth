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

#include "le_connection_arbiter.h"

namespace bluetooth {
namespace arbiter {
struct LeConnectionArbiterModule::impl {
  impl(const LeConnectionArbiterModule& module) : module_(module) {}

  void Start() {
    acl_manager_ = module_.GetDependency<hci::AclManager>();
    handler_ = module_.GetHandler();
  }

  void Stop() {
    acl_manager_ = nullptr;
    handler_ = nullptr;
  }

  void RegisterLeCallbacks(hci::acl_manager::LeConnectionCallbacks* callbacks, os::Handler* handler) {
    acl_manager_->RegisterLeCallbacks(callbacks, handler);
  }

  void UnregisterLeCallbacks(hci::acl_manager::LeConnectionCallbacks* callbacks, std::promise<void> promise) {
    acl_manager_->UnregisterLeCallbacks(callbacks, std::move(promise));
  }

  const LeConnectionArbiterModule& module_;
  hci::AclManager* acl_manager_;
  os::Handler* handler_;
};

void LeConnectionArbiterModule::RegisterLeCallbacks(
    hci::acl_manager::LeConnectionCallbacks* callbacks, os::Handler* handler) {
  CallOn(pimpl_.get(), &impl::RegisterLeCallbacks, callbacks, handler);
}
void LeConnectionArbiterModule::UnregisterLeCallbacks(
    hci::acl_manager::LeConnectionCallbacks* callbacks, std::promise<void> promise) {
  CallOn(pimpl_.get(), &impl::UnregisterLeCallbacks, callbacks, std::move(promise));
}

const ModuleFactory LeConnectionArbiterModule::Factory =
    ModuleFactory([]() { return new LeConnectionArbiterModule(); });

LeConnectionArbiterModule::LeConnectionArbiterModule() : pimpl_(std::make_unique<impl>(*this)){};
LeConnectionArbiterModule::~LeConnectionArbiterModule() = default;

void LeConnectionArbiterModule::ListDependencies(ModuleList* list) const {
  list->add<hci::AclManager>();
}

void LeConnectionArbiterModule::Start() {
  pimpl_->Start();
}

void LeConnectionArbiterModule::Stop() {
  pimpl_->Stop();
}

}  // namespace arbiter
}  // namespace bluetooth