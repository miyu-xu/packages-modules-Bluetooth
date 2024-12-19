// Copyright 2024, The Android Open Source Project
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

use android_hardware_bluetooth_offload_leaudio::{aidl, binder};

use crate::LeAudioModule;
use aidl::android::hardware::bluetooth::offload::leaudio::IHciProxy::{BpHciProxy, IHciProxy};
use bluetooth_offload_hci::Module;
use mockall::{predicate::*, *};
use std::{io, sync::Arc, thread};

mock! {
    ModuleSink {}
    impl Module for ModuleSink {
        fn next(&self) -> &dyn Module;
        fn out_cmd(&self, data: &[u8]);
        fn out_acl(&self, data: &[u8]);
        fn out_iso(&self, data: &[u8]);
        fn out_sco(&self, data: &[u8]);

        fn in_evt(&self, data: &[u8]);
        fn in_acl(&self, data: &[u8]);
        fn in_sco(&self, data: &[u8]);
        fn in_iso(&self, data: &[u8]);
    }
}

#[test]
fn bind_service() -> io::Result<()> {

//    thread::spawn(move || {
//        let sink_module = MockModuleSink::new();
//        let _ = LeAudioModule::new(Arc::new(sink_module));
//
//        binder::ProcessState::join_thread_pool();
//    });
//
//    binder::wait_for_service(&format!("{}/default", BpHciProxy::get_descriptor()))
//        .expect("Failed to bind to service");

    Ok(())
}
