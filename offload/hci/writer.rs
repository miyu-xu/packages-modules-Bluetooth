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

pub trait Write {
    fn write(&self, w: &mut Writer)
    where
        Self: Sized;
}

pub struct Writer<'a> {
    vec: &'a mut Vec<u8>,
}

impl<'a> Writer<'a> {
    pub(crate) fn new(vec: &'a mut Vec<u8>) -> Self {
        Self { vec }
    }

    pub(crate) fn put(&mut self, vec: &[u8]) {
        self.vec.extend_from_slice(vec);
    }

    pub(crate) fn write<T: Write>(&mut self, v: &T) {
        v.write(self)
    }

    pub(crate) fn write_u8(&mut self, v: u8) {
        self.write_u32::<1>(v.into());
    }

    pub(crate) fn write_u16(&mut self, v: u16) {
        self.write_u32::<2>(v.into());
    }

    pub(crate) fn write_u32<const N: usize>(&mut self, mut v: u32) {
        for _ in 0..N {
            self.vec.push((v & 0xff) as u8);
            v >>= 8;
        }
    }

    pub(crate) fn write_vec<T: Write>(&mut self, vec: &Vec<T>) {
        self.write_u8(vec.len().try_into().unwrap());
        for v in vec {
            self.write(v);
        }
    }
}
