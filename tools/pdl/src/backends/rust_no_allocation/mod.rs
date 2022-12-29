//! Rust no-allocation backend
//!
//! The motivation for this backend is to be a more "idiomatic" backend than
//! the existing backend. Specifically, it should
//! 1. Use lifetimes, not reference counting
//! 2. Avoid expensive memory copies unless needed
//! 3. Use the intermediate Schema rather than doing all the logic from scratch
//!
//! One notable consequence is that we avoid .specialize(), as it has "magic" behavior
//! not defined in the spec. Instead we mimic the C++ approach of calling tryParse() and
//! getting a Result<> back.

mod computed_values;
mod enums;
mod packet_parser;
pub mod test;

use crate::ast;

use self::{enums::generate_enum, packet_parser::generate_packet};

use super::intermediate::Schema;

pub fn generate(file: &ast::File, schema: &Schema) -> Result<String, String> {
    match file.endianness.value {
        ast::EndiannessValue::LittleEndian => {}
        _ => unimplemented!("Only little_endian endianness supported"),
    };

    let mut out = String::new();

    out.push_str(include_str!("preamble.rs"));

    for decl in &file.declarations {
        generate_decl(&mut out, decl, schema)?;
    }

    Ok(out)
}

fn generate_decl(out: &mut String, decl: &ast::Decl, schema: &Schema) -> Result<(), String> {
    match decl {
        ast::Decl::Enum { id, tags, width, .. } => generate_enum(out, id, tags, *width),
        ast::Decl::Packet { id, fields, parent_id, .. } => generate_packet(
            out,
            id,
            fields,
            parent_id.as_ref().map(|x| &**x),
            schema,
            &schema.packets[id.as_str()].0,
        ),
        ast::Decl::Struct { id, fields, parent_id, .. } => generate_packet(
            out,
            id,
            fields,
            parent_id.as_ref().map(|x| &**x),
            schema,
            &schema.structs[id.as_str()].0,
        ),
        _ => unimplemented!("Unsupported decl type"),
    }
}
