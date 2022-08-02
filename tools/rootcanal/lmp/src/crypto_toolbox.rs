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

use ring::hmac;

// f2(W, N1, N2, KeyID, A1, A2) = HMAC-SHA-256<W>(N1 || N2 || KeyID || A1 || A2) [0..16]
pub fn f2(w: &[u8], n1: &[u8], n2: &[u8], key_id: &[u8], a1: &[u8], a2: &[u8]) -> [u8; 16] {
    let key = hmac::Key::new(hmac::HMAC_SHA256, w);
    let mut ctx = hmac::Context::with_key(&key);
    ctx.update(n1);
    ctx.update(n2);
    ctx.update(key_id);
    ctx.update(a1);
    ctx.update(a2);
    let hmac = ctx.sign();

    let mut buf = [0; 16];
    buf.copy_from_slice(&hmac.as_ref()[0..16]);
    buf
}

#[cfg(test)]
mod tests {
    use crate::crypto_toolbox::f2;

    struct F2TestCase<const N: usize> {
        pub w: [u8; N],
        pub n1: [u8; 16],
        pub n2: [u8; 16],
        pub key_id: [u8; 4],
        pub a1: [u8; 6],
        pub a2: [u8; 6],
        pub out: [u8; 16],
    }

    const F2_CASE_P192: F2TestCase<24> = F2TestCase::<24> {
        w: [
            251, 59, 162, 1, 44, 126, 98, 70, 110, 72, 110, 34, 146, 144, 23, 91, 74, 254, 188, 19,
            253, 204, 238, 70,
        ],
        n1: [213, 203, 132, 84, 209, 119, 115, 62, 255, 255, 178, 236, 113, 43, 174, 171],
        n2: [166, 232, 231, 204, 37, 167, 95, 110, 33, 101, 131, 247, 255, 61, 196, 207],
        key_id: [98, 116, 108, 107],
        a1: [86, 18, 55, 55, 191, 206],
        a2: [167, 19, 112, 45, 207, 193],
        out: [194, 52, 193, 25, 143, 59, 82, 1, 134, 171, 146, 162, 248, 116, 147, 78],
    };

    const F2_CASE_P256: F2TestCase<32> = F2TestCase::<32> {
        w: [
            236, 2, 52, 163, 87, 200, 173, 5, 52, 16, 16, 166, 10, 57, 125, 155, 153, 121, 107, 19,
            180, 248, 102, 241, 134, 141, 52, 243, 115, 191, 166, 152,
        ],
        n1: [213, 203, 132, 84, 209, 119, 115, 62, 255, 255, 178, 236, 113, 43, 174, 171],
        n2: [166, 232, 231, 204, 37, 167, 95, 110, 33, 101, 131, 247, 255, 61, 196, 207],
        key_id: [98, 116, 108, 107],
        a1: [86, 18, 55, 55, 191, 206],
        a2: [167, 19, 112, 45, 207, 193],
        out: [71, 48, 11, 185, 92, 116, 4, 18, 148, 80, 103, 75, 23, 65, 16, 77],
    };

    #[test]
    fn test_f2() {
        assert_eq!(
            f2(
                &F2_CASE_P192.w,
                &F2_CASE_P192.n1,
                &F2_CASE_P192.n2,
                &F2_CASE_P192.key_id,
                &F2_CASE_P192.a1,
                &F2_CASE_P192.a2
            ),
            F2_CASE_P192.out
        );
        assert_eq!(
            f2(
                &F2_CASE_P256.w,
                &F2_CASE_P256.n1,
                &F2_CASE_P256.n2,
                &F2_CASE_P256.key_id,
                &F2_CASE_P256.a1,
                &F2_CASE_P256.a2
            ),
            F2_CASE_P256.out
        );
    }
}
