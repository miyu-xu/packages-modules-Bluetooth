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

use crate::codec::{CodecCConfig, Encode, PcmFrame};
use core::ffi::{c_int, c_longlong, c_uint, c_void};
use core::ptr;
use std::alloc::{alloc, dealloc, Layout};

pub struct Lc3Encoder {
    instances: Lc3Instances,
    frame_samples: usize,
    block_bytes: usize,
}

#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct Lc3CConfig {
    hr_mode: bool,
    frame_duration_us: c_int,
    sample_rate: c_int,
}

impl CodecCConfig for Lc3CConfig {
    fn validate(&self, _: usize) -> Result<(), String> {
        match (self.hr_mode, self.frame_duration_us) {
            (false, 2500 | 5000 | 7500 | 10000) => Ok(()),
            (true, 2500 | 5000 | 10000) => Ok(()),
            (_, v) => Err(format!("Invalid frame duration: {} us", v)),
        }
        .and(match (self.hr_mode, self.sample_rate) {
            (false, 8000 | 16000 | 24000 | 32000 | 48000) => Ok(()),
            (true, 48000 | 96000) => Ok(()),
            (_, v) => Err(format!("Invalid sample rate: {} Hz", v)),
        })
    }

    fn sample_rate(&self) -> u32 {
        self.sample_rate as u32
    }

    fn new_encoder(&self, channels: usize, max_sdu_size: usize) -> Box<dyn Encode> {
        Box::new(Lc3Encoder::new(channels, max_sdu_size, self))
    }
}

impl Lc3Encoder {
    fn new(channels: usize, max_sdu_size: usize, config: &Lc3CConfig) -> Self {
        assert!(config.validate(channels).is_ok());

        let hr_mode = config.hr_mode;
        let dt_us = config.frame_duration_us;
        let sr_hz = config.sample_rate;
        Self {
            instances: Lc3Instances::new(
                channels,
                // SAFETY: All the parameters are memory-safe; the size of the encoder
                //         returned is guaranteed to be less than `usize`; the zero value,
                //         in case of error, value is safetely proceeded.
                || unsafe { lc3_hr_encoder_size(hr_mode, dt_us, sr_hz) },
                // SAFETY: The memory is allocated using the largest possible C alignment,
                //         and is valid for the indicated size, using the same parameters.
                |mem| unsafe { lc3_hr_setup_encoder(hr_mode, dt_us, sr_hz, 0, mem) },
            ),
            // SAFETY: All the parameters are memory-safe; the number of samples by
            //         frame is stored to validate the length of PCM stream.
            frame_samples: unsafe { lc3_hr_frame_samples(hr_mode, dt_us, sr_hz) } as usize,
            block_bytes: max_sdu_size,
        }
    }
}

impl Encode for Lc3Encoder {
    fn encode(&self, pcm: &PcmFrame) -> Vec<u8> {
        let handles = &self.instances.handles;
        let channels = handles.len();

        let pcm = pcm.to_vec_f32();
        assert!(pcm.len() == channels * self.frame_samples);

        let mut data = vec![0u8; self.block_bytes];
        let mut offset = 0;

        for ch in 0..channels {
            let frame_size = data.len() / channels + (ch < data.len() % channels) as usize;
            // SAFETY: The handle points to a memory area valid for `lc3_hr_encoder_size()`
            //         bytes, and set up by `lc3_hr_setup_encoder()`.
            //         The PCM input is valid for the number of samples by frame for
            //         as many frames as the number of channels. The output buffer is
            //         valid from `offet` to `frame_size`, and is initialized to 0.
            unsafe {
                lc3_encode(
                    handles[ch],
                    PcmFormat::Float,
                    pcm[ch..].as_ptr().cast(),
                    channels as c_int,
                    frame_size as c_int,
                    data[offset..].as_mut_ptr().cast(),
                );
            }
            offset += frame_size;
        }

        data
    }
}

struct Lc3Instances {
    layout: Layout,
    handles: Vec<*mut c_void>,
}

impl Lc3Instances {
    fn new<F, G>(channels: usize, get_size: F, setup: G) -> Self
    where
        F: FnOnce() -> c_uint,
        G: Fn(*mut c_void) -> *mut c_void,
    {
        let size = get_size() as usize;
        let align = (c_longlong::BITS / 8) as usize;
        let layout = Layout::from_size_align(size, align).unwrap();
        assert_ne!(size, 0);

        let mut handles = Vec::with_capacity(channels);
        for _ in 0..channels {
            // SAFETY: The C code returned valid allocable memory size;
            //         the alignment is suitable for any C standard types.
            let instance = setup(unsafe { alloc(layout).cast() });
            assert_ne!(instance, ptr::null_mut());
            handles.push(instance);
        }

        Self { layout, handles }
    }
}

impl Drop for Lc3Instances {
    fn drop(&mut self) {
        for h in &self.handles {
            // SAFETY: The handles points to memory allocated by `alloc()`
            //         using the same layout.
            unsafe {
                dealloc(h.cast(), self.layout);
            }
        }
    }
}

#[repr(C)]
#[allow(dead_code)]
enum PcmFormat {
    S16,
    S24,
    S24_3Le,
    Float,
}

#[rustfmt::skip]
extern "C" {
    fn lc3_hr_frame_samples(
        hrmode: bool, dt_us: c_int, sr_hz: c_int
    ) -> c_int;

    fn lc3_hr_encoder_size(
        hrmode: bool, dt_us: c_int, sr_hz: c_int
    ) -> c_uint;

    fn lc3_hr_setup_encoder(
        hrmode: bool, dt_us: c_int, sr_hz: c_int, sr_pcm_hz: c_int,
        mem: *mut c_void
    ) -> *mut c_void;

    fn lc3_encode(enc: *mut c_void,
        fmt: PcmFormat, pcm: *const c_void, stride: c_int,
        nbytes: c_int, data: *mut c_void,
    ) -> c_int;
}
