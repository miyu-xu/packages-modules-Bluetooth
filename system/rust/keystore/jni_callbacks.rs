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

/// These callbacks are expected to be made available to the KeystoreModule from JNI.
pub trait KeystoreJniCallbacks {
    /// Associate the decrypted cleartext with a lookup key `prefix`. If `decrypted` is an empty string, clear the storage.
    fn set_encrypt_key_or_remove_key_callback(&self, prefix: &str, decrypted: &str);

    /// Fetch decrypted data indexed by `prefix`. If no data found, return an empty String.
    fn get_key(&self, prefix: &str) -> String;
}
