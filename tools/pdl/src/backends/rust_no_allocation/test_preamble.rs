#![allow(non_camel_case_types)]

use pdl_rust_no_allocation_tests::*;

fn hex_to_word(hex: u8) -> u8 {
    if b'0' <= hex && hex <= b'9' {
        hex - b'0'
    } else if b'A' <= hex && hex <= b'F' {
        hex - b'A' + 0xa
    } else {
        hex - b'a' + 0xa
    }
}

fn hex_to_byte_string(hex: &str) -> Vec<u8> {
    hex.as_bytes()
        .chunks_exact(2)
        .map(|chunk| hex_to_word(chunk[1]) + (hex_to_word(chunk[0]) << 4))
        .collect()
}
