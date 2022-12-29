use std::collections::HashMap;

use serde::Deserialize;
use std::fmt::Write;

use crate::{ast, parser::parse_inline};

use super::codegen::snake_to_camel;

#[derive(Deserialize)]
struct PacketTest {
    packet: String,
    tests: Box<[PacketTestCase]>,
}

#[derive(Deserialize)]
struct PacketTestCase {
    packed: String,
    unpacked: UnpackedTestFields,
    packet: Option<String>,
}

#[derive(Deserialize)]
struct UnpackedTestFields(HashMap<String, Field>);

// fields can be scalars, lists, or structs
#[derive(Deserialize)]
#[serde(untagged)]
enum Field {
    Number(usize),
    Struct(UnpackedTestFields),
    List(Box<[ListEntry]>),
}

// lists can either contain scalars or structs
#[derive(Deserialize)]
#[serde(untagged)]
enum ListEntry {
    Number(usize),
    Struct(UnpackedTestFields),
}

fn generate_matchers(
    out: &mut String,
    base: &str,
    value: &UnpackedTestFields,
    filter_fields: &dyn Fn(&str) -> Result<bool, String>,
) -> Result<(), String> {
    for (field_name, field_value) in value.0.iter() {
        if !filter_fields(&field_name)? {
            continue;
        }
        let camel_field = snake_to_camel(field_name);
        match field_value {
            Field::Number(num) => {
                writeln!(out, "EXPECT_EQ({base}.Get{camel_field}(), {num});").unwrap();
            }
            Field::List(lst) => {
                let len = lst.len();
                for (i, val) in lst.iter().enumerate() {
                    let list_elem = if field_name == "payload" {
                        writeln!(out, "ASSERT_EQ({base}.GetPayloadSize(), {len});").unwrap();
                        format!("{base}.GetPayload()[{i}]")
                    } else {
                        writeln!(out, "ASSERT_EQ({base}.Get{camel_field}Count(), {len});").unwrap();
                        format!("{base}.GetNth{camel_field}({i})")
                    };
                    match val {
                        ListEntry::Number(num) => {
                            writeln!(out, "EXPECT_EQ({list_elem}, {num});").unwrap();
                        }
                        ListEntry::Struct(fields) => {
                            generate_matchers(out, &list_elem, fields, &|_| Ok(true))?;
                        }
                    }
                }
            }
            Field::Struct(fields) => {
                generate_matchers(out, &format!("{base}.Get{camel_field}()"), fields, &|_| {
                    Ok(true)
                })?;
            }
        }
    }
    Ok(())
}

pub fn generate_test_file() -> Result<String, String> {
    let mut out = String::new();

    let file = include_str!("../../../tests/canonical/le_test_vectors.json");
    let test_vectors: Box<[_]> =
        serde_json::from_str(file).map_err(|_| "could not parse test vectors")?;

    let pdl = include_str!("../../../tests/canonical/le_test_file_cpp_target.pdl");
    let ast = parse_inline(&mut ast::SourceDatabase::new(), "test.pdl".to_owned(), pdl.to_owned())
        .map_err(|_| "could not parse reference PDL")
        .unwrap();
    let packet_lookup = ast
        .declarations
        .iter()
        .filter_map(|decl| match decl {
            ast::Decl::Packet { id, fields, .. } | ast::Decl::Struct { id, fields, .. } => {
                Some((id.as_str(), fields))
            }
            _ => None,
        })
        .collect::<HashMap<_, _>>();

    out.push_str(&include_str!("cpp_test_preamble.h"));

    writeln!(out, "namespace packet {{").unwrap();

    for PacketTest { packet, tests } in test_vectors.iter() {
        if !pdl.contains(packet) {
            // huge brain hack to skip unsupported test vectors
            continue;
        }

        for (i, PacketTestCase { packed, unpacked, packet: sub_packet }) in tests.iter().enumerate()
        {
            if let Some(sub_packet) = sub_packet {
                if !pdl.contains(sub_packet) {
                    // huge brain hack to skip unsupported test vectors
                    continue;
                }
            }

            writeln!(out, "TEST({packet}Test, LittleEndianPackets{i}) {{").unwrap();
            let size = packed.len() / 2;
            writeln!(out, "auto buf = hexToByteString(\"{packed}\", {size});").unwrap();
            writeln!(out, "auto base = BasePacket::parse(buf.data(), {size});").unwrap();

            let mut packet_name = packet;
            writeln!(out, "auto {packet_name}_instance = {packet}Packet::tryParse(base);").unwrap();
            writeln!(out, "ASSERT_TRUE({packet_name}_instance.has_value());").unwrap();
            if let Some(sub_packet) = sub_packet {
                writeln!(
                    out,
                    "auto {sub_packet}_instance = {sub_packet}Packet::tryParse({packet_name}_instance.value());"
                )
                .unwrap();
                packet_name = sub_packet;
                writeln!(out, "ASSERT_TRUE({packet_name}_instance.has_value());").unwrap();
            }

            generate_matchers(
                &mut out,
                &format!("(*{packet_name}_instance)"),
                unpacked,
                &|field| {
                    Ok(packet_lookup
                        .get(packet_name.as_str())
                        .ok_or(format!("could not find packet {packet_name}"))?
                        .iter()
                        .find(|x| x.id().map(|x| x == field).unwrap_or(false))
                        .is_some()
                        || field == "payload")
                },
            )?;
            writeln!(out, "}}").unwrap();
        }
    }

    writeln!(out, "}}").unwrap();

    Ok(out)
}
