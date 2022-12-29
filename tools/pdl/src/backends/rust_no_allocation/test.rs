use std::collections::HashMap;

use proc_macro2::TokenStream;
use quote::{format_ident, quote};
use serde::Deserialize;

use crate::{ast, parser::parse_inline, quote_block};

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
    base: TokenStream,
    value: &UnpackedTestFields,
    filter_fields: &dyn Fn(&str) -> Result<bool, String>,
) -> Result<TokenStream, String> {
    let mut out = vec![];

    for (field_name, field_value) in value.0.iter() {
        if !filter_fields(field_name)? {
            continue;
        }
        let getter_ident = format_ident!("get_{field_name}");
        match field_value {
            Field::Number(num) => {
                let num = *num as u64;
                out.push(quote! { assert_eq!(u64::from(#base.#getter_ident()), #num); });
            }
            Field::List(lst) => {
                let get_iter_ident = format_ident!("get_{field_name}_iter");
                let vec_ident = format_ident!("{field_name}_vec");
                out.push(quote! { let #vec_ident = #base.#get_iter_ident().collect::<Vec<_>>(); });

                for (i, val) in lst.iter().enumerate() {
                    let list_elem = if field_name == "payload" {
                        todo!()
                    } else {
                        quote! { #vec_ident[#i] }
                    };
                    out.push(match val {
                        ListEntry::Number(num) => {
                            let num = *num as u64;
                            quote! { assert_eq!(u64::from(#list_elem), #num); }
                        }
                        ListEntry::Struct(fields) => {
                            generate_matchers(list_elem, fields, &|_| Ok(true))?
                        }
                    })
                }
            }
            Field::Struct(fields) => {
                out.push(generate_matchers(quote! { #base.#getter_ident() }, fields, &|_| {
                    Ok(true)
                })?);
            }
        }
    }
    Ok(quote! { { #(#out)* } })
}

pub fn generate_test_file() -> Result<String, String> {
    let mut out = String::new();

    out.push_str(include_str!("test_preamble.rs"));

    let file = include_str!("../../../tests/canonical/le_test_vectors.json");
    let test_vectors: Box<[_]> =
        serde_json::from_str(file).map_err(|_| "could not parse test vectors")?;

    let pdl = include_str!("../../../tests/canonical/le_rust_noalloc_test_file.pdl");
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

            let test_name_ident = format_ident!("test_{packet}_{i}");
            let packet_ident = format_ident!("{packet}_instance");
            let packet_view = format_ident!("{packet}View");

            let mut leaf_packet = packet;

            let specialization = if let Some(sub_packet) = sub_packet {
                let sub_packet_ident = format_ident!("{}_instance", sub_packet);
                let sub_packet_view_ident = format_ident!("{}View", sub_packet);

                leaf_packet = sub_packet;
                quote! { let #sub_packet_ident = #sub_packet_view_ident::try_parse(#packet_ident).unwrap(); }
            } else {
                quote! {}
            };

            let leaf_packet_ident = format_ident!("{leaf_packet}_instance");

            let packet_matchers =
                generate_matchers(quote! { #leaf_packet_ident }, unpacked, &|field| {
                    Ok(
                        packet_lookup
                            .get(leaf_packet.as_str())
                            .ok_or(format!("could not find packet {packet}"))?
                            .iter()
                            .any(|x| x.id().map(|x| x == field).unwrap_or(false)), // || field == "payload"
                    )
                })?;

            out.push_str(&quote_block! {
              #[test]
              fn #test_name_ident() {
                let base = hex_to_byte_string(#packed);
                let #packet_ident = #packet_view::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();

                #specialization

                #packet_matchers
              }
            });
        }
    }

    Ok(out)
}
