use std::{env, fs::File, io::Write, path::Path};

extern crate pdl;

fn main() {
    let out_dir = env::var_os("OUT_DIR").unwrap();
    let dest_path = Path::new(&out_dir).join("_packets.rs");
    let mut dest_file = File::create(dest_path).unwrap();

    let file =
        pdl::parser::parse_file(&mut pdl::ast::SourceDatabase::new(), "src/packets.pdl".into())
            .unwrap();
    let schema = pdl::backends::intermediate::generate(&file).unwrap();
    let output = pdl::backends::rust_no_allocation::generate(&file, &schema).unwrap();
    dest_file.write_all(output.as_bytes()).unwrap();

    println!("cargo:rerun-if-changed=build.rs");
    println!("cargo:rerun-if-changed=src/packets.pdl");
}
