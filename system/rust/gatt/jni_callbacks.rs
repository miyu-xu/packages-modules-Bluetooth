// Copyright 2022, The Android Open Source Project
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

use crate::core::RawAddress;

use super::ids::{ConnectionId, TransactionId, AttHandle};

/// These callbacks are expected to be made available to the GattModule from JNI.
pub trait GattCallbacks {
    /// Invoked when a client tries to read a characteristic. Expects a response using bluetooth::gatt::send_response();
    fn on_server_read_characteristic(
        &self,
        address: RawAddress,
        conn_id: ConnectionId,
        trans_id: TransactionId,
        handle: AttHandle,
        offset: u32,
        is_long: bool,
    );
}
