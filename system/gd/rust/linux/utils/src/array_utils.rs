//! This library provides array utils.

pub fn to_sized_array<const S: usize>(v: &Vec<u8>) -> [u8; S] {
    v.iter().chain(std::iter::repeat(&0)).take(S).cloned().collect::<Vec<u8>>().try_into().unwrap()
}
