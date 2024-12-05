// Copyright 2024, The Android Open Source Project
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

//! Derive of `hci::reader::Read` and `hci::writer::Write` traits

extern crate proc_macro;
mod return_parameters;
mod r#struct;

use proc_macro::TokenStream;
use syn::{DeriveInput, Error};

/// Derive of `hci::reader::Read` trait
#[proc_macro_derive(Read, attributes(N))]
pub fn derive_read_fn(input: TokenStream) -> TokenStream {
    let input = syn::parse_macro_input!(input as DeriveInput);
    let expanded = match (input.ident.to_string().as_str(), &input.data) {
        ("ReturnParameters", syn::Data::Enum(..)) => return_parameters::read(input),
        (_, syn::Data::Struct(..)) => r#struct::read(input),
        (_, _) => panic!("Unsupported kind of input"),
    }
    .unwrap_or_else(Error::into_compile_error);
    TokenStream::from(expanded)
}

/// Derive of `hci::reader::Write` trait
#[proc_macro_derive(Write)]
pub fn derive_write_fn(input: TokenStream) -> TokenStream {
    let input = syn::parse_macro_input!(input as DeriveInput);
    let expanded = match (input.ident.to_string().as_str(), &input.data) {
        ("ReturnParameters", syn::Data::Enum(..)) => return_parameters::write(input),
        (_, syn::Data::Struct(..)) => r#struct::write(input),
        (_, _) => panic!("Unsupported kind of input"),
    }
    .unwrap_or_else(Error::into_compile_error);
    TokenStream::from(expanded)
}
