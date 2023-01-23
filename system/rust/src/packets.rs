// We inherit casing from the PDL file
#![allow(non_snake_case)]
#![allow(non_camel_case_types)]
#![allow(warnings, missing_docs)]
#![allow(clippy::all)]
// this is now stable
#![feature(mixed_integer_ops)]

include!(concat!(env!("OUT_DIR"), "/_packets.rs"));
