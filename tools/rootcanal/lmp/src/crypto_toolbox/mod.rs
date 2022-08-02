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
use std::convert::TryInto;

pub mod ec;

// f1(U, V, X, Z) = HMAC-SHA-256<X>(U || V || Z) [0..16]
pub fn _f1(u: &[u8], v: &[u8], x: &[u8], z: &[u8]) -> [u8; 16] {
    let key = hmac::Key::new(hmac::HMAC_SHA256, x);
    let mut ctx = hmac::Context::with_key(&key);
    ctx.update(u);
    ctx.update(v);
    ctx.update(z);
    let hmac = ctx.sign();
    hmac.as_ref()[0..16].try_into().unwrap()
}

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
    hmac.as_ref()[0..16].try_into().unwrap()
}

// f3(W, N1, N2, R, IOcap, A1, A2) = HMAC-SHA-256W (N1 || N2 || R || IOcap || A1 || A2) [0..16]
pub fn _f3(
    w: &[u8],
    n1: &[u8],
    n2: &[u8],
    r: &[u8],
    io_cap: &[u8],
    a1: &[u8],
    a2: &[u8],
) -> [u8; 16] {
    let key = hmac::Key::new(hmac::HMAC_SHA256, w);
    let mut ctx = hmac::Context::with_key(&key);
    ctx.update(n1);
    ctx.update(n2);
    ctx.update(r);
    ctx.update(io_cap);
    ctx.update(a1);
    ctx.update(a2);
    let hmac = ctx.sign();
    hmac.as_ref()[0..16].try_into().unwrap()
}

// g(U, V, X, Y) = SHA-256(U || V || X || Y) % 2**32
// pub fn g(u: &[u8], v: &[u8], x: &[u8], y: &[u8]) -> [u8; 16] {
// }

#[cfg(test)]
mod tests {
    use crate::crypto_toolbox::{_f1, f2};

    const F1_CASE: [(&[u8], &[u8], &[u8], &[u8], &[u8]); 1] = [(
        b"\x15 p\t\x98D!\xa6Xo\x9f\xc3\xfe~C)\xd2\x80\x9e\xa5\x11%\xf8\xed",
        b"5k1\x93\x84!\xfb\xbf/\xb31\xc8\x9f\xd5\x88\xa6\x93g\xe9\xa83\xf5h\x12",
        b"\xd5\xcb\x84T\xd1ws>\xff\xff\xb2\xecq+\xae\xab",
        b"\x00",
        b"\x1b\xdc\x95Z\x9dT/\xfc\x9f\x9eg\x0c\xdffP\x10",
    )];

    const F2_CASE: [(&[u8], &[u8], &[u8], &[u8], &[u8], &[u8], &[u8]); 1] = [(
        b"\xfb;\xa2\x01,~bFnHn\"\x92\x90\x17[J\xfe\xbc\x13\xfd\xcc\xeeF",
        b"\xd5\xcb\x84T\xd1ws>\xff\xff\xb2\xecq+\xae\xab",
        b"\xa6\xe8\xe7\xcc%\xa7_n!e\x83\xf7\xff=\xc4\xcf",
        b"btlk",
        b"V\x1277\xbf\xce",
        b"\xa7\x13p-\xcf\xc1",
        b"\xc24\xc1\x19\x8f;R\x01\x86\xab\x92\xa2\xf8t\x93N",
    )];

    #[test]
    fn test_f1() {
        for test_case in F1_CASE {
            assert_eq!(_f1(test_case.0, test_case.1, test_case.2, test_case.3), test_case.4);
        }
    }

    #[test]
    fn test_f2() {
        for test_case in F2_CASE {
            assert_eq!(
                f2(test_case.0, test_case.1, test_case.2, test_case.3, test_case.4, test_case.5),
                test_case.6
            );
        }
    }
}
