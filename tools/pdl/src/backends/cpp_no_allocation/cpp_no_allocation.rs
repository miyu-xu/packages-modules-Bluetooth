//! CHRE / C++ backend
//!
//! The motivation for this backend is to have a "minimal" backend for use on CHRE / embedded contexts.
//! C++ features are permitted, but should be used sparingly. std::vector and other data structures that
//! involve allocation are prohibited. Memory and code size are both concerns.

use crate::{
    ast,
    backends::cpp_no_allocation::codegen::{generate_backing_int, snake_to_camel},
    push,
};
use std::fmt::Write;

use crate::backends::intermediate::{
    ComputedOffset, ComputedOffsetId, ComputedValue, ComputedValueId, PacketOrStruct,
    PacketOrStructLength, Schema,
};

use super::codegen::State;

pub fn generate(file: &ast::File, schema: &Schema) -> Result<String, String> {
    let mut state = State::new();

    match file.endianness.value {
        ast::EndiannessValue::LittleEndian => {}
        _ => unimplemented!("Only little_endian endianness supported"),
    };

    push!(state, "#pragma once");

    let preamble = include_str!("cpp_preamble.h");
    state.code.push_str(preamble);

    push!(state, "namespace packet");

    state.within_block(|block| {
        for decl in &file.declarations {
            generate_decl(block, decl, schema)?;
        }
        Ok::<_, String>(())
    })?;

    Ok(state.code)
}

enum ContainerType {
    Packet,
    Struct,
}

fn generate_decl<'a>(
    state: &mut State,
    decl: &'a ast::Decl,
    schema: &Schema,
) -> Result<(), String> {
    match decl {
        ast::Decl::Enum { id, tags, width, .. } => generate_enum(state, id, tags, *width),
        ast::Decl::Packet { id, constraints, fields, parent_id, .. } => generate_packet(
            state,
            id,
            ContainerType::Packet,
            constraints,
            fields,
            parent_id.as_ref().map(|x| &**x),
            schema,
            &schema.packets[id.as_str()].0,
        ),
        ast::Decl::Struct { id, constraints, fields, parent_id, .. } => generate_packet(
            state,
            id,
            ContainerType::Struct,
            constraints,
            fields,
            parent_id.as_ref().map(|x| &**x),
            schema,
            &schema.structs[id.as_str()].0,
        ),
        _ => unimplemented!("Unsupported decl type"),
    }
}

fn generate_enum<'a, 'b>(
    state: &'b mut State,
    id: &'a str,
    tags: &'a [ast::Tag],
    width: usize,
) -> Result<(), String> {
    let backing_int = generate_backing_int(width)?;

    push!(state, "class {id}");

    state.within_block(|block| {
        push!(block, "public:");

        push!(block, "enum Value : {backing_int}");
        block.within_block(|block| {
            for ast::Tag { id: tag_id, value, .. } in tags {
                push!(block, "{tag_id} = {value},");
            }
        });

        push!(block, "{id}() = default;");
        push!(block, "constexpr {id}(Value v) : v(v) {{ }};");

        push!(block, "constexpr operator Value() const {{ return v; }}");
        push!(block, "explicit operator bool() const = delete;");

        push!(block, "static std::optional<{id}> tryParse(const uint8_t *buf, uint64_t offset, uint64_t offset_end)");
        block.within_block(|block| {
            push!(block, "bool corrupt = false;");
            push!(block, "auto out = GetTypeFromBuffer<Value>(buf, offset, offset_end, offset, {width}, false /* valid */, corrupt);");
            block.if_then("corrupt", |block| push!(block, "return std::nullopt;"));
            push!(block, "return out;");
        });

        push!(block, "private:");
        push!(block, "Value v;");
    });
    Ok(())
}

trait Gettable {
    fn get(&self) -> String;
}

trait Declarable {
    fn get_fn_name(&self) -> String;

    fn call_fn(&self) -> String {
        let fn_name = self.get_fn_name();
        format!("{fn_name}()")
    }

    fn declare_fn(&self) -> String {
        let fn_name = self.get_fn_name();
        format!("uint64_t {fn_name}() const")
    }
}

impl<T: Declarable> Gettable for T {
    fn get(&self) -> String {
        self.call_fn()
    }
}

impl Declarable for ComputedValueId<'_> {
    fn get_fn_name(&self) -> String {
        match self {
            ComputedValueId::FieldSize(field) => format!("get_{field}_size"),
            ComputedValueId::FieldCount(field) => format!("get_{field}_count"),
            ComputedValueId::FieldElementSize(field) => format!("get_elementsize_{field}"),
            ComputedValueId::Custom(id) => format!("get_custom_val_{id}"),
        }
    }
}

impl Declarable for ComputedOffsetId<'_> {
    fn get_fn_name(&self) -> String {
        match self {
            ComputedOffsetId::HeaderStart => "get_header_start_offset".to_string(),
            ComputedOffsetId::FieldOffset(field) => {
                if *field == "_body_" {
                    "get__payload__offset".to_owned()
                } else {
                    format!("get_{field}_offset")
                }
            }
            ComputedOffsetId::FieldEndOffset(field) => {
                if *field == "_body_" {
                    "get__payload__offset_end".to_owned()
                } else {
                    format!("get_{field}_offset_end")
                }
            }
            ComputedOffsetId::Custom(id) => format!("get_custom_offset_{id}"),
            ComputedOffsetId::TrailerStart => "get_trailer_start_offset".to_string(),
            ComputedOffsetId::PacketEnd => "get_end".to_string(),
        }
    }
}

impl ComputedValue<'_> {
    fn write(&self, state: &mut State, schema: &Schema) {
        match self {
            ComputedValue::Constant(value) => push!(state, "return {value};"),
            ComputedValue::Difference(arg1, arg2) => {
                let arg1 = arg1.get();
                let arg2 = arg2.get();
                state.check(&format!("({arg1} - {arg2}) % 8 == 0"));
                push!(state, "return ({arg1} - {arg2}) / 8;");
            }
            ComputedValue::Product(arg1, arg2) => {
                let arg1 = arg1.get();
                let arg2 = arg2.get();
                push!(state, "return {arg1} * {arg2};");
            }
            ComputedValue::Divide(arg1, arg2) => {
                let arg1 = arg1.get();
                let arg2 = arg2.get();
                state.check(&format!("{arg1} % {arg2} == 0"));
                push!(state, "return {arg1} / {arg2};");
            }
            ComputedValue::ValueAt { offset, width } => {
                state.get_type_from_buffer("uint64_t", offset.get(), *width)
            }
            ComputedValue::SizeOfNStructs { base_id, n, struct_type } => {
                let base_offset = base_id.get();
                let n = n.get();
                push!(state, "uint64_t byte_offset = 0;");
                push!(state, "for (int i = 0; i != {n}; ++i)");
                state.within_block(|block| {
                    parse_struct_knows_length(
                        block,
                        "parsedStruct",
                        struct_type,
                        &format!("{base_offset} + byte_offset * 8"),
                    );
                    push!(block, "byte_offset += parsedStruct.get_size();");
                });
                push!(state, "return byte_offset;");
            }
            ComputedValue::CountStructsUpToSize { base_id, struct_type, size } => {
                assert!(matches!(
                    schema.structs[struct_type].0.length,
                    PacketOrStructLength::Dynamic,
                ));

                let base_offset = base_id.get();
                let size = size.get();
                push!(state, "uint64_t i = 0;");
                push!(state, "uint64_t byte_offset = 0;");
                push!(state, "while (byte_offset < {size})");
                state.within_block(|block| {
                    parse_struct_knows_length(
                        block,
                        "parsedStruct",
                        struct_type,
                        &format!("{base_offset} + byte_offset * 8"),
                    );
                    block.check(&format!("byte_offset + parsedStruct.get_size() <= {size}"));
                    push!(block, "byte_offset += parsedStruct.get_size();");
                    push!(block, "++i;");
                    push!(block, "if (byte_offset == {size})");
                    block.within_block(|block| {
                        push!(block, "return i;");
                    });
                });
                push!(state, "return 0;");
            }
        }
    }
}

impl ComputedOffset<'_> {
    fn write(&self, state: &mut State) {
        match self {
            ComputedOffset::ConstantPlusOffsetInBits(base_id, offset) => {
                let base_id = base_id.get();
                push!(state, "return {base_id} + {offset};");
            }
            ComputedOffset::SumWithOctets(arg1, arg2) => {
                let arg1 = arg1.get();
                let arg2 = arg2.get();
                push!(state, "return {arg1} + {arg2} * 8;");
            }
            ComputedOffset::Alias(alias) => {
                let alias = alias.get();
                push!(state, "return {alias};");
            }
        }
    }
}

impl State {
    fn check(&mut self, condition: &str) {
        self.if_then(&format!("!valid && !({condition})"), |block| {
            push!(block, "corrupt = true;");
            push!(block, "return {{}};");
        });
    }

    fn if_then(&mut self, condition: &str, consequent: impl FnOnce(&mut State) -> ()) {
        push!(self, "if ({condition})");
        self.within_block(|block| consequent(block));
    }

    fn get_type_from_buffer(&mut self, t: &str, offset: impl ToString, width: usize) {
        let packet_start = ComputedOffsetId::HeaderStart.get();
        let offset = offset.to_string();
        let width = width.to_string();
        push!(self, "return GetTypeFromBuffer<{t}>(buf, {packet_start}, buf_end_offset, {offset}, {width}, valid, corrupt);")
    }
}

fn parse_struct(block: &mut State, schema: &Schema, var_name: &str, type_id: &str, field_id: &str) {
    let field_offset = ComputedOffsetId::FieldOffset(field_id).get();
    if matches!(schema.structs[type_id].0.length, PacketOrStructLength::NeedsExternal) {
        parse_struct_needs_length(
            block,
            var_name,
            type_id,
            &field_offset,
            &ComputedOffsetId::FieldEndOffset(field_id).get(),
        );
    } else {
        parse_struct_knows_length(block, var_name, type_id, &field_offset);
    }
}

fn parse_struct_needs_length(
    block: &mut State,
    var_name: &str,
    type_id: &str,
    offset: &str,
    end_offset: &str,
) {
    let tmp = "maybe".to_string() + &snake_to_camel(var_name);
    block.check(&format!("{offset} <= {end_offset}"));
    block.check(&format!("{end_offset} <= buf_end_offset"));
    push!(block, "auto {tmp} = {type_id}::tryParseWithKnownEndpoint(buf, {offset}, {end_offset});",);
    block.check(&format!("{tmp}.has_value()"));
    push!(block, "auto {var_name} = {tmp}.value();")
}

fn parse_struct_knows_length(block: &mut State, var_name: &str, type_id: &str, offset: &str) {
    let tmp = "maybe".to_string() + &snake_to_camel(var_name);
    push!(block, "auto {tmp} = {type_id}::tryParse(buf, {offset}, buf_end_offset);",);
    block.check(&format!("{tmp}.has_value()"));
    push!(block, "auto {var_name} = {tmp}.value();")
}

fn generate_packet(
    state: &mut State,
    id: &str,
    container_type: ContainerType,
    constraints: &[ast::Constraint],
    fields: &[ast::Field],
    parent_id: Option<&str>,
    schema: &Schema,
    curr_schema: &PacketOrStruct,
) -> Result<(), String> {
    let suffix = match container_type {
        ContainerType::Packet => "Packet",
        ContainerType::Struct => "",
    };
    // generate the wrapper struct, getters, and tryParse
    push!(state, "struct {id}{suffix}");
    state.within_block(|block| {
        // this is the backing storage for the entire BasePacket
        push!(block, "const uint8_t *buf;");

        // this is the offset (in BITS) that the current packet starts at within the BasePacket
        push!(block, "uint64_t offset;");
        push!(block, "uint64_t get_header_start_offset() const");
        block.within_block(|block| {
            push!(block, "return offset;");
        });

        // this is the size (in OCTETS) of the current struct
        // if we need it from an external source, we need to store it
        if matches!(container_type, ContainerType::Struct) {
            push!(block, "uint64_t get_size() const");
            block.within_block(|block| {
                push!(block, "auto size_bits = get_end() - get_header_start_offset();");
                block.check("size_bits % 8 != 0");
                push!(block, "return size_bits / 8;");
            });
        }

        // this is a pointer to just past the end of the valid region of the buffer
        // accessing past this is automatically a parse failure
        // if we need our length, we will use this
        push!(block, "uint64_t buf_end_offset;");
        if matches!(curr_schema.length, PacketOrStructLength::NeedsExternal) {
            push!(block, "uint64_t get_end() const");
            block.within_block(|block| {
                push!(block, "return buf_end_offset;");
            });
        }

        // this bit will be set after initial parsing, and disables bounds-checking
        // in the memory access primitive
        push!(block, "mutable bool valid = false;");

        // if bounds-checking is enabled, and we do an OOB read, this bit will be set
        // other checks may also choose to set this bit if something invalid is detected
        push!(block, "mutable bool corrupt = false;");

        // generate all offsets
        for (offset_id, offset) in &curr_schema.computed_offsets {
            push!(block, "{}", offset_id.declare_fn());
            block.within_block(|block| {
                offset.write(block);
            });
        }

        // generate all values
        for (value_id, value) in &curr_schema.computed_values {
            push!(block, "{}", value_id.declare_fn());
            block.within_block(|block| {
                value.write(block, schema);
            });
        }

        // generate getters
        for field in fields {
            match field {
                ast::Field::Scalar { id, width, .. } => {
                    let camel_field_name = snake_to_camel(id);
                    let field_type = generate_backing_int(*width)?;
                    push!(block, "{field_type} Get{camel_field_name}() const");
                    block.within_block(|block| {
                        block.get_type_from_buffer(field_type, ComputedOffsetId::FieldOffset(id).get(), *width);
                    });
                }
                ast::Field::Typedef { id, type_id, .. } => {
                    let camel_field_name = snake_to_camel(id);
                    push!(block, "{type_id} Get{camel_field_name}() const");
                    block.within_block(|block| {
                        parse_struct(block, schema, "parsedStruct", type_id, id);
                        push!(block, "return parsedStruct;");
                    });
                }
                ast::Field::Payload { .. }  => {
                    push!(block, "uint64_t GetPayloadSize() const");
                    block.within_block(|block| {
                        push!(block, "auto out = get__payload__offset_end() - get__payload__offset();");
                        block.check("out % 8 == 0");
                        push!(block, "return out / 8;");
                    });
                    push!(block, "const uint8_t *GetPayload() const");
                    block.within_block(|block| {
                        block.check("get__payload__offset() % 8 == 0");
                        push!(block, "return &buf[get__payload__offset() / 8];");
                    })
                }
                ast::Field::Body { .. }
                | ast::Field::Padding { .. }
                | ast::Field::Reserved { .. }
                | ast::Field::Fixed { .. }
                | ast::Field::ElementSize { .. }
                | ast::Field::Count { .. }
                | ast::Field::Size { .. }
                => {
                    // no-op, no getter generated for this type
                }
                ast::Field::Array { id, width, type_id,.. } => {
                    let elem_type = if let Some(width) = width {
                        generate_backing_int(*width)?.to_string()
                    } else if let Some(type_id) = type_id {
                        type_id.to_string()
                    } else {
                        unreachable!()
                    };
                    let camel_id = snake_to_camel(id);

                    push!(block, "uint64_t Get{camel_id}Count() const");
                    block.within_block(|block| {
                        push!(block, "return get_{id}_count();");
                    });

                    if let Some(type_id) = type_id {
                        push!(block, "{elem_type} GetNth{camel_id}(uint64_t n) const");
                        block.within_block(|block| {
                            let base_offset = ComputedOffsetId::FieldOffset(id).get();
                            if curr_schema.computed_values.contains_key(&ComputedValueId::FieldElementSize(id)) {
                                let size = ComputedValueId::FieldElementSize(id).get();
                                let needs_length = matches!(schema.structs[type_id.as_str()].0.length, PacketOrStructLength::NeedsExternal);
                                if needs_length {
                                    parse_struct_needs_length(block, "out", type_id, &format!("{base_offset} + {size} * n * 8"), &size);
                                } else {
                                    parse_struct_knows_length(block, "out", type_id, &format!("{base_offset} + {size} * n * 8"));
                                }
                                push!(block, "return out;")
                            } else {
                                // need to consume piece-by-piece, up to N
                                push!(block, "uint64_t i = 0;");
                                push!(block, "uint64_t byte_offset = 0;");
                                push!(block, "while (true)");
                                block.within_block(|block| {
                                    parse_struct_knows_length(block, "curr", type_id, &format!("{base_offset} + byte_offset * 8"));
                                    push!(block, "if (i == n) return curr;");
                                    push!(block, "byte_offset += curr.get_size();");
                                    push!(block, "++i;");
                                });
                            }
                        })
                    } else if let Some(width) = *width {
                        push!(block, "{elem_type} GetNth{camel_id}(uint64_t n) const");
                        block.within_block(|block| {
                            block.get_type_from_buffer(&elem_type, format!("{} + {width} * n", ComputedOffsetId::FieldOffset(id).get()), width);
                        });

                        // zero-copy getter available if we're a uint8_t
                        if width == 8 {
                            push!(block, "const {elem_type}* Get{camel_id}() const");
                            block.within_block(|block| {
                                push!(block, "return &buf[get_{id}_offset() / 8];");
                            });
                        }
                    } else {
                        unreachable!()
                    }
                },
                ast::Field::Group { .. } => unreachable!(),
                ast::Field::Checksum { .. } => unimplemented!(),
            }
        }

        match container_type {
            ContainerType::Packet => {
                let parent_source = parent_id.map(|parent| format!("{parent}{suffix}")).unwrap_or("BasePacket".to_string());
                push!(block, "static std::optional<{id}{suffix}> tryParse(const {parent_source}& parent)")
            },
            ContainerType::Struct => {
                let func_name = if matches!(curr_schema.length, PacketOrStructLength::NeedsExternal) {
                    "tryParseWithKnownEndpoint"
                } else {
                    "tryParse"
                };
                push!(block, "static std::optional<{id}> {func_name}(const uint8_t *buf, uint64_t start_offset, uint64_t end_offset)")
            },
        }

        block.within_block(|block| {
            // validate constraints
            for ast::Constraint { id, value, tag_id, .. } in constraints {
                let camel_id = snake_to_camel(id);
                if let Some(val) = value {
                    push!(block, "if (parent.Get{camel_id}() != {val})");
                    block.within_block(|block| push!(block, "return std::nullopt;"));
                } else if let Some(tag) = tag_id {
                    push!(block, "if (parent.Get{camel_id}() != decltype(parent.Get{camel_id}())::{tag})");
                    block.within_block(|block| push!(block, "return std::nullopt;"));
                } else {
                    unreachable!("constraints must have a value xor a tag")
                }
            }

            push!(block, "auto out = {id}{suffix}{{}};");

            match container_type {
                ContainerType::Packet => {
                    push!(block, "out.buf = parent.buf;");
                    push!(block, "out.offset = parent.get__payload__offset();");
                    push!(block, "out.buf_end_offset = parent.get__payload__offset_end();");
                },
                ContainerType::Struct => {
                    push!(block, "out.buf = buf;");
                    push!(block, "out.offset = start_offset;");
                    push!(block, "out.buf_end_offset = end_offset;");
                },
            }

            for field in fields {
                match field {
                    ast::Field::Checksum { .. } => unimplemented!(),
                    ast::Field::Group { .. } => unreachable!(),
                    ast::Field::Padding { .. }| ast::Field::Size { .. }| ast::Field::Count { .. } |   ast::Field::ElementSize { ..    } |     ast::Field::Body { .. } | ast::Field::Fixed { .. } |   ast::Field::Reserved { .. }  => {},
                    ast::Field::Payload { .. } => {
                        push!(block, "out.GetPayloadSize();");
                        push!(block, "out.GetPayload();");
                    },
                    ast::Field::Array { id, .. } => {
                        let camel_id = snake_to_camel(id);
                        push!(block, "uint64_t {id}_count = out.Get{camel_id}Count();");
                        push!(block, "for (uint64_t i = 0; i != {id}_count; ++i)");
                        block.within_block(|block| push!(block, "out.GetNth{camel_id}(i);"));
                    },
                    ast::Field::Scalar { id, .. } | ast::Field::Typedef { id, .. } => {
                        let id = snake_to_camel(id);
                        push!(block, "out.Get{id}();");
                    },
                }
            }

            push!(block, "out.valid = !out.corrupt;");
            push!(block, "if (!out.valid)");
            block.within_block(|block| push!(block, "return std::nullopt;"));
            push!(block, "return out;");

            Ok::<_, String>(())
        })?;

        Ok::<_, String>(())
    })?;

    Ok(())
}
