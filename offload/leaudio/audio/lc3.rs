// Copyright 2025, The Android Open Source Project
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

use crate::codec::{CodecCConfig, Encode};

#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct Lc3CConfig {}

impl CodecCConfig for Lc3CConfig {
    fn validate(&self, _: usize) -> Result<(), String> {
        unimplemented!();
    }

    fn sample_rate(&self) -> u32 {
        unimplemented!();
    }

    fn to_encoder(&self, _channels: usize) -> Box<dyn Encode> {
        unimplemented!();
    }
}
