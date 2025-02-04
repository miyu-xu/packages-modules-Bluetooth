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
use core::ffi::{c_int, c_void};

#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct OpusCConfig {
    frame_duration_us: c_int,
    sample_rate: c_int,
    complexity: c_int,
}

struct CustomEncoder {
    handle: *mut c_void,
    channels: usize,
    mode: CustomMode,
    frame_bytes: usize,
}

impl CodecCConfig for OpusCConfig {
    fn validate(&self, channels: usize) -> Result<(), String> {
        match channels {
            1..=2 => Ok(()),
            n => Err(format!("Invalid number of channels: {}", n)),
        }
        .and(match self.sample_rate {
            48_000 | 96_000 => Ok(()),
            n => Err(format!("Invalid sample rate: {} Hz", n)),
        })
        .and(match self.frame_duration_us {
            20_000 => Ok(()),
            n => Err(format!("Invalid frame duration: {} us", n)),
        })
        .and(match self.complexity {
            0..=10 => Ok(()),
            n => Err(format!("Invalid complexity: {}", n)),
        })
    }

    fn sample_rate(&self) -> u32 {
        self.sample_rate as u32
    }

    fn new_encoder(&self, channels: usize, max_sdu_size: usize) -> Box<dyn Encode> {
        let enc = CustomEncoder::new(channels, max_sdu_size, self);
        enc.set_complexity(self.complexity);
        Box::new(enc)
    }
}

impl CustomEncoder {
    fn new(channels: usize, max_sdu_size: usize, config: &OpusCConfig) -> Self {
        assert!(config.validate(channels).is_ok());

        let mode = CustomMode::new(config.sample_rate, config.frame_duration_us);

        let mut error = 0;
        let handle =
            // SAFETY: The `mode.handle()` points to a non-null mode living as long as
            //         the encoder. The error indicator lives the time of the procedure call.
            //         Unless error indication, an encoder state is returned.
            unsafe { opus_custom_encoder_create(mode.handle(), channels as i32, &mut error) };
        assert_eq!(error, 0, "Opus encoder creation failed with error: {}", error);

        Self { handle, channels, mode, frame_bytes: max_sdu_size }
    }

    fn set_complexity(&self, value: c_int) {
        // SAFETY: The handle points to an encoder state, with the self-lifetime.
        let result = unsafe { opus_custom_encoder_ctl(self.handle, SET_COMPLEXITY_REQUEST, value) };
        assert_eq!(result, 0, "Opus encoder control failed with error: {}", result);
    }
}

impl Encode for CustomEncoder {
    fn encode(&self, pcm: &PcmFrame) -> Vec<u8> {
        let pcm = pcm.to_vec_f32();
        let frame_samples = self.mode.frame_samples;
        assert!(pcm.len() == self.channels * frame_samples);

        let mut data = vec![0u8; self.frame_bytes];

        // SAFETY: The handle points to an encoder state, with the self-lifetime; The PCM input
        //         points to a frame of samples to encode. The output buffer is valid for
        //         the desired size of the encoded audio frame.
        let result = unsafe {
            opus_custom_encode_float(
                self.handle,
                pcm.as_ptr(),
                frame_samples as c_int,
                data.as_mut_ptr(),
                data.len() as c_int,
            )
        };
        assert!(result >= 0, "Opus encoding failed with error: {}", result);

        data.truncate(result as usize);
        data
    }
}

impl Drop for CustomEncoder {
    fn drop(&mut self) {
        // SAFETY: The handle points to an encoder state, created simultaneously of self.
        unsafe { opus_custom_encoder_destroy(self.handle) };
    }
}

struct CustomMode {
    handle: *mut c_void,
    frame_samples: usize,
}

impl CustomMode {
    fn new(sample_rate: c_int, frame_duration_us: c_int) -> Self {
        let frame_samples = ((frame_duration_us as u64 * sample_rate as u64) / 1_000_000) as c_int;

        let mut error = 0;
        // SAFETY: All input parameters are memory-safe; the error indicator lives the time
        //         of the procedure call. Unless error indication, a valid mode description,
        //         living until destroyed, is returned.
        let handle = unsafe { opus_custom_mode_create(sample_rate, frame_samples, &mut error) };
        assert_eq!(error, 0, "Opus custom mode creation failed with error: {}", error);

        Self { handle, frame_samples: frame_samples as usize }
    }

    fn handle(&self) -> *const c_void {
        self.handle
    }
}

impl Drop for CustomMode {
    fn drop(&mut self) {
        // SAFETY: The handle points to a mode definition, created simultaneously of self.
        unsafe {
            opus_custom_mode_destroy(self.handle);
        }
    }
}

const SET_COMPLEXITY_REQUEST: c_int = 4010;

#[rustfmt::skip]
extern "C" {
    fn opus_custom_mode_create(
        fs: i32, frame_size: c_int, error: *mut c_int
    ) -> *mut c_void;

    fn opus_custom_mode_destroy(mode: *mut c_void);

    fn opus_custom_encoder_create(
        mode: *const c_void, channels: c_int, error: *mut c_int
    ) -> *mut c_void;

    fn opus_custom_encoder_destroy(st: *mut c_void);

    fn opus_custom_encode_float(
        st: *mut c_void,
        pcm: *const f32,
        frame_size: c_int,
        compressed: *mut u8,
        max_compressed_bytes: c_int,
    ) -> c_int;

    fn opus_custom_encoder_ctl(st: *mut c_void, request: c_int, ...) -> c_int;
}
