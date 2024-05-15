// Copyright 2024, Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::env;
use std::fs::File;
use std::io::Write;
use std::path::PathBuf;

fn main() {
    generate_module("att.pdl", "att_packets.rs", &["super::AttributeHandle".to_string()]);
}

fn generate_module(in_path: &str, out_path: &str, custom_fields: &[String]) {
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
    let mut out_file = File::create(out_dir.join(out_path)).unwrap();

    println!("cargo:rerun-if-changed={}", in_path);

    let mut sources = pdl_compiler::ast::SourceDatabase::new();
    let parsed_file =
        pdl_compiler::parser::parse_file(&mut sources, in_path).expect("PDL parse failed");
    let analyzed_file = pdl_compiler::analyzer::analyze(&parsed_file).expect("PDL analysis failed");
    let rust_source =
        pdl_compiler::backends::rust::generate(&sources, &analyzed_file, custom_fields);
    out_file.write_all(rust_source.as_bytes()).expect("Could not write to output file");
}
