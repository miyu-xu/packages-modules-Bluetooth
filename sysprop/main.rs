// Copyright 2023, The Android Open Source Project
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

//! Create C macro for Bluetooth sysprop to be used on all target

use anyhow::{bail, Context, Result};
use bluetooth_sysprop_proto::Properties;
use bluetooth_sysprop_proto::Property;
use bluetooth_sysprop_proto::Type;
use clap::Parser;
use num_traits::Num;
use protobuf::Message;
use std::fs::File;
use std::io::Write;
use std::path::PathBuf;

#[derive(Parser, Debug)]
#[command(author = "wescande@google.com", version = "1", about = "Transform encoded sysprop message in C macro", long_about = None)]
struct Args {
    /// Unused for now <- Input
    #[clap(long = "genDir")]
    gen_dir: PathBuf,
    /// File with sysprop to parse usually <xx>.sysprop <- Input
    #[clap(long = "in")]
    input: PathBuf,
    /// File to write the C Macro <- Output
    #[clap(long = "out")]
    output: PathBuf,
}

fn parse<T: Num>(input: &str) -> Result<T>
where
    <T as num_traits::Num>::FromStrRadixErr: std::fmt::Display,
{
    let input = input.trim();
    match if let Some(stripped) = input.strip_prefix("0x") {
        T::from_str_radix(stripped, 16)
    } else if let Some(stripped) = input.strip_prefix("0b") {
        T::from_str_radix(stripped, 2)
    } else if let Some(stripped) = input.strip_prefix("0o") {
        T::from_str_radix(stripped, 8)
    } else if let Some(stripped) = input.strip_prefix('0') {
        if stripped.is_empty() {
            Ok(T::zero())
        } else {
            T::from_str_radix(stripped, 8)
        }
    } else {
        T::from_str_radix(input, 10)
    } {
        Ok(x) => Ok(x),
        Err(err) => bail!("Failed to parse {input}: {err}"),
    }
}

fn get_namespace(module: &str) -> Result<&str> {
    Ok(&module[(module.rfind('.').context("Failed to parse namespace")? + 1)..])
}

fn get_default_and_type(prop: &Property) -> Result<(&'static str, String)> {
    if prop.default_value.is_empty() {
        bail!("Following property doesn't have a default value: {}\nPlease add the following line:\n    # default_value: \"<{:?}>\"\n", prop.api_name, prop.field_type);
    }
    match prop.field_type {
        Type::Boolean => Ok(("bool", format!("{}", prop.default_value.parse::<bool>()?))),
        Type::Integer => Ok(("int32_t", format!("{}", parse::<i32>(&prop.default_value)?))),
        any_type => bail!("Unsupported type: {any_type:?}"),
        // Type::Long => unimplemented!(),
        // Type::Double => unimplemented!(),
        // Type::String => unimplemented!(),
        // Type::Enum => unimplemented!(),
        // Type::UInt => unimplemented!(),
        // Type::ULong => unimplemented!(),
        // Type::BooleanList => unimplemented!(),
        // Type::IntegerList => unimplemented!(),
        // Type::LongList => unimplemented!(),
        // Type::DoubleList => unimplemented!(),
        // Type::StringList => unimplemented!(),
        // Type::EnumList => unimplemented!(),
        // Type::UIntList => unimplemented!(),
        // Type::ULongList => unimplemented!(),
    }
}

fn try_main() -> Result<()> {
    let args = Args::parse();
    let mut in_file = File::open(args.input)?;
    let mut out_file = File::create(args.output)?;

    let parsed_data = Properties::parse_from_reader(&mut in_file)?;
    let namespace = get_namespace(&parsed_data.module)?;
    writeln!(&mut out_file, "namespace sysprop::{namespace} {{")?;
    for prop in &parsed_data.prop {
        let api_name = &prop.api_name;
        let prop_name = &prop.prop_name;
        let (field_type, default_value) = get_default_and_type(prop)
            .context(format!("\nFailed to get default_value and type in {namespace}.sysprop"))?;
        writeln!(
            &mut out_file,
            "    GENERATE_PROPERTY_WRAPPER({namespace}, {api_name}, \"{prop_name}\", {default_value}, {field_type})",
        )?;
    }
    writeln!(&mut out_file, "}}")?;
    // write!(&mut out_file, "read: {:#?}", parsed_data)?;
    Ok(())
}

fn main() {
    try_main().unwrap()
}
