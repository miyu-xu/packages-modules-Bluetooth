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

//use android_hardware_bluetooth_offload_leaudio::{aidl, binder};
//
use crate::LeAudioModule;
use bluetooth_offload_hci::{self as hci, EventToBytes, Module, ReturnParameters};
use mockall::{predicate::*, *};
use std::sync::Arc;

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
fn reset() {
    let mut sink = MockModuleSink::new();
    sink.expect_in_evt().return_const(());

    let m = LeAudioModule::new(Arc::new(sink));

    m.in_evt(
        &hci::CommandComplete {
            num_hci_command_packets: 0,
            return_parameters: ReturnParameters::Reset(hci::ResetComplete { status: 0 }),
        }
        .to_bytes(),
    );

    m.in_evt(
        &hci::CommandComplete {
            num_hci_command_packets: 0,
            return_parameters: ReturnParameters::LeReadBufferSizeV2(
                hci::LeReadBufferSizeV2Complete {
                    status: 0,
                    le_acl_data_packet_length: 0,
                    total_num_le_acl_data_packets: 0,
                    iso_data_packet_length: 128,
                    total_num_iso_data_packets: 2,
                },
            ),
        }
        .to_bytes(),
    );
}
