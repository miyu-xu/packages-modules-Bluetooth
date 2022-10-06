//! Generate Rust unit tests for canonical test vectors.

use quote::{format_ident, quote};
use serde::Deserialize;
use serde_json::Value;

#[derive(Debug, Deserialize)]
struct Packet {
    #[serde(rename = "packet")]
    name: String,
    tests: Vec<TestVector>,
}

#[derive(Debug, Deserialize)]
struct TestVector {
    packed: String,
    unpacked: Value,
}

fn hexadecimal_to_vec(mut hex: &str) -> proc_macro2::TokenStream {
    assert!(hex.len() % 2 == 0, "Expects an even number of hex digits");
    let mut bytes = Vec::with_capacity(hex.len() / 2);
    while !hex.is_empty() {
        let (head, tail) = hex.split_at(2);
        bytes.push(
            syn::parse_str::<syn::LitInt>(&format!("0x{head}"))
                .unwrap_or_else(|err| panic!("Could not parse {head:?}: {err}")),
        );
        hex = tail;
    }

    quote! {
        vec![#(#bytes),*]
    }
}

fn generate_unit_tests(input: &str, packet_names: &[&str], generated: &str) {
    eprintln!("Reading test vectors from {input}, will use {} packets", packet_names.len());

    let data = std::fs::read_to_string(input)
        .unwrap_or_else(|err| panic!("Could not read {input}: {err}"));
    let packets: Vec<Packet> = serde_json::from_str(&data).expect("Could not parse JSON");

    let mut tests = Vec::new();
    for packet in &packets {
        if !packet_names.contains(&packet.name.as_str()) {
            eprintln!("Skipping packet {}", packet.name);
            continue;
        }
        for (i, test_vector) in packet.tests.iter().enumerate() {
            let test_name = format_ident!("{}_test_vector_{}", packet.name, i + 1);
            let packed = hexadecimal_to_vec(&test_vector.packed);
            let packet_name = format_ident!("{}Packet", packet.name);

            let object = test_vector.unpacked.as_object().unwrap_or_else(|| {
                panic!("Expected test vector object, found: {}", test_vector.unpacked)
            });
            let assertions = object.iter().map(|(key, value)| {
                let getter = format_ident!("get_{key}");
                let value_u64 = value
                    .as_u64()
                    .unwrap_or_else(|| panic!("Expected u64 for {key:?} key, got {value}"));
                let value = proc_macro2::Literal::u64_unsuffixed(value_u64);
                quote! {
                    assert_eq!(actual.#getter(), #value);
                }
            });

            let module = format_ident!("{}", generated);
            tests.push(quote! {
                #[test]
                fn #test_name() {
                    let packed = #packed;
                    let actual = #module::#packet_name::parse(&packed).unwrap();
                    #(#assertions)*
                }
            });
        }
    }

    let code = quote! {
        #(#tests)*
    };
    println!("{code}");
}

fn main() {
    let input_path = std::env::args().nth(1).expect("Need path to input test vectors");
    let output_path = std::env::args().nth(2).expect("Need path to the generated PDL output");
    generate_unit_tests(&input_path, &["Packet_Scalar_Field"], &output_path);
}
