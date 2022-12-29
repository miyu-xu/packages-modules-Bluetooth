use crate::{
    ast,
    backends::{cpp_no_allocation::codegen::generate_backing_int, intermediate::Schema},
    push,
};
use std::fmt::Write;

use super::codegen::State;

pub fn generate_builders(file: &ast::File, schema: &Schema) -> Result<String, String> {
    let mut state = State::new();
    for decl in &file.declarations {
        match decl {
            ast::Decl::Packet { id, loc, constraints, fields, parent_id } => {
                generate_packet_builder(
                    &mut state,
                    &id,
                    &fields,
                    parent_id.as_ref().map(|x| &**x),
                    schema,
                )?;
            }
            ast::Decl::Struct { id, loc, constraints, fields, parent_id } => {
                generate_packet_builder(
                    &mut state,
                    &id,
                    &fields,
                    parent_id.as_ref().map(|x| &**x),
                    schema,
                )?;
            }
            ast::Decl::Group { .. } => unreachable!(),
            _ => {}
        }
    }
    Ok(state.code)
}

struct BuilderField(String, String);

fn generate_packet_builder(
    state: &mut State,
    id: &str,
    fields: &[ast::Field],
    parent_id: Option<&str>,
    schema: &Schema,
) -> Result<(), String> {
    push!(state, "struct {id}Builder");
    state.within_block(|block| {
        let builder_fields: Vec<_> = fields
            .iter()
            .flat_map(|field| match field {
                ast::Field::Array { id, width, type_id, size, .. } => {
                    if let Some(width) = width {
                        // too painful to propagate out of the iterator, and this should never happen anyway if the linter does its job
                        let backing_type = generate_backing_int(*width).unwrap();
                        vec![
                            BuilderField(format!("{backing_type}*"), format!("{id}_data")),
                            BuilderField("uint64_t".to_owned(), format!("{id}_count")),
                        ]
                    } else if let Some(type_id) = type_id {
                        if schema.enums.contains_key(type_id.as_str()) {
                            vec![BuilderField(type_id.to_owned(), id.to_owned())]
                        } else {
                            vec![BuilderField(format!("{type_id}Builder"), id.to_owned())]
                        }
                    } else {
                        unreachable!()
                    }
                }
                ast::Field::Scalar { id, width, .. } => {
                    vec![BuilderField(
                        id.to_owned(),
                        generate_backing_int(*width).unwrap().to_owned(),
                    )]
                }
                ast::Field::Typedef { id, type_id, .. } => {
                    if schema.enums.contains_key(type_id.as_str()) {
                        vec![BuilderField(type_id.to_owned(), id.to_owned())]
                    } else {
                        vec![BuilderField(format!("{type_id}Builder"), id.to_owned())]
                    }
                }
                _ => vec![],
            })
            .collect();

        for BuilderField(type_name, field_name) in &builder_fields {
            push!(block, "{type_name} {field_name};")
        }

        push!(block, "static {id}Builder Create(");
        if let Some(parent_id) = parent_id {
            push!(block, "{parent_id}Builder parent_builder");
        }
        for BuilderField(type_name, field_name) in &builder_fields {
            push!(block, "{type_name} {field_name},")
        }
        push!(block, ") : ");
        for BuilderField(_, field_name) in &builder_fields {
            push!(block, "{field_name}({field_name})")
        }
        push!(block, "{{}};");

        push!(block, "uint64_t Size() const");
        block.within_block(|block| todo!());

        Ok::<_, String>(())
    })?;
    Ok(())
}
