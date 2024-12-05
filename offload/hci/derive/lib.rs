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

//! Derive of `hci::reader::Read` and `hci::writer::Write` traits on a `Struct`
//!
//! ```
//! #[derive(Read, Write)]
//! struct Example {
//!    #[N(1)] one_byte: u8,
//!    #[N(2)] two_bytes: u16,
//!    #[N(3)] three_bytes: u32,
//!    #[N(4)] foor_bytes: u32,
//!    #[N(1)] vec_u8: Vec<u8>,
//!    #[N(1)] vec_u16: Vec<u16>,
//!    other_type: OtherType,
//!    other_vec_type: Vec<OtherType>,
//! }
//! ```
//!
//! Produces:
//!
//! ```
//! impl Read for Example {
//!     fn read(r: &mut Reader) -> Option<Self> {
//!         Some(Self {
//!             one_byte: r.read_u8()?,
//!             two_bytes: r.read_u16()?,
//!             three_bytes: r.read_u32::<3>()?,
//!             foor_bytes: r.read_u32::<4>()?,
//!             vec_u8: r.read_vec_u8()?
//!             vec_u16: r.read_vec_u16()?
//!             other_type: r.read()?
//!             other_vec_type: r.read_vec()?
//!         })
//!     }
//! }
//!
//! impl Write for Example {
//!     fn write(&self, w: &mut Writer) {
//!         w.write_u8(self.one_byte);
//!         w.write_u16(self.two_bytes);
//!         w.write_u32::<3>(self.three_bytes);
//!         w.write_u32::<4>(self.foor_bytes);
//!         w.write_vec_u8(&self.vec_u8);
//!         w.write_vec_u16(&self.vec_u16);
//!         w.write(&self.other_type);
//!         w.write_vec(&self.other_vec_type);
//!     }
//! }
//! ```

extern crate proc_macro;

use proc_macro::TokenStream;
use quote::{quote, quote_spanned};
use syn::{spanned::Spanned, DeriveInput, Error};

enum Type {
    Primitive(syn::Ident),
    VecPrimitive(syn::Ident),
    Any(),
    VecAny(),
}

fn parse_type(ty: &syn::Type) -> Result<Type, Error> {
    Ok(match ty {
        syn::Type::Path(v) => {
            if v.path.segments.len() > 1 {
                return Err(Error::new(ty.span(), "Unimplemented"));
            }
            let segment = &v.path.segments[0];
            let ident = &segment.ident;
            match ident.to_string().as_str() {
                "u8" | "u16" | "u32" => Type::Primitive(ident.clone()),
                "Vec" => match &segment.arguments {
                    syn::PathArguments::AngleBracketed(params) => match &params.args[0] {
                        syn::GenericArgument::Type(ty) => match parse_type(ty)? {
                            Type::Primitive(ident) => Type::VecPrimitive(ident),
                            Type::Any() => Type::VecAny(),
                            _ => return Err(Error::new(ty.span(), "Unsupported vector type")),
                        },
                        _ => return Err(Error::new(ty.span(), "Unsupported vector type")),
                    },
                    _ => return Err(Error::new(ty.span(), "Unsupported vector type")),
                },
                _ => Type::Any(),
            }
        }
        _ => return Err(Error::new(ty.span(), "Unhandled identifier type")),
    })
}

struct Attributes {
    n: Option<usize>,
}

fn parse_attrs(syn_attrs: &[syn::Attribute]) -> Result<Attributes, Error> {
    let mut n = None;
    for syn_attr in syn_attrs.iter() {
        if syn_attr.path().is_ident("N") {
            let lit: syn::LitInt = syn_attr.parse_args()?;
            n = Some(lit.base10_parse()?);
        } else {
            return Err(Error::new(syn_attr.span(), "Unrecognized attribute"));
        }
    }

    Ok(Attributes { n })
}

fn read_expand(input: DeriveInput) -> Result<proc_macro2::TokenStream, Error> {
    let name = &input.ident;
    let mut fields = Vec::new();

    let syn::Data::Struct(data) = &input.data else {
        panic!("Only struct is supported");
    };

    for field in &data.fields {
        let ident = &field.ident.as_ref().unwrap();
        let attrs = parse_attrs(&field.attrs)?;
        let fn_token = match parse_type(&field.ty)? {
            Type::Primitive(ty) => match ty.to_string().as_str() {
                "u8" => {
                    if attrs.n.is_some() && attrs.n != Some(1) {
                        return Err(Error::new(ty.span(), "Expected N(1) for type `u8`"));
                    }
                    quote_spanned! { ty.span() => read_u8()? }
                }
                "u16" => {
                    if attrs.n.is_some() && attrs.n != Some(2) {
                        return Err(Error::new(ty.span(), "Expected N(2) for type `u16`"));
                    }
                    quote_spanned! { ty.span() => read_u16()? }
                }
                "u32" => {
                    if attrs.n.is_none() {
                        return Err(Error::new(ty.span(), "`N()` attribute required"));
                    }
                    let n = attrs.n.unwrap();
                    if n > 4 {
                        return Err(Error::new(ty.span(), "Expected N(n <= 4)"));
                    }
                    quote_spanned! { ty.span() => read_u32::<#n>()? }
                }
                _ => unimplemented!(),
            },
            Type::VecPrimitive(ty) => {
                if attrs.n.is_some() && attrs.n != Some(1) {
                    return Err(Error::new(ty.span(), "Expected N(1) for type `Vec`"));
                }
                match ty.to_string().as_str() {
                    "u8" => quote_spanned! { ty.span() => read_vec_u8()? },
                    "u16" => quote_spanned! { ty.span() => read_vec_u16()? },
                    _ => unimplemented!(),
                }
            }
            Type::Any() => quote_spanned! { ident.span() => read()? },
            Type::VecAny() => quote_spanned! { ident.span() => read_vec()? },
        };
        fields.push(quote! { #ident: r.#fn_token });
    }

    Ok(quote! {
        impl Read for #name {
            fn read(r: &mut Reader) -> Option<Self> {
                Some(Self {
                    #( #fields ),*
                })
            }
        }
    })
}

/// Derive of `hci::reader::Read` trait
#[proc_macro_derive(Read, attributes(N))]
pub fn derive_read_fn(input: TokenStream) -> TokenStream {
    let input = syn::parse_macro_input!(input as DeriveInput);
    let expanded = read_expand(input).unwrap_or_else(Error::into_compile_error);
    TokenStream::from(expanded)
}

fn write_expand(input: DeriveInput) -> Result<proc_macro2::TokenStream, Error> {
    let name = &input.ident;
    let mut fields = Vec::new();

    let syn::Data::Struct(data) = &input.data else {
        panic!("Only struct is supported");
    };

    for field in &data.fields {
        let ident = &field.ident.as_ref().unwrap();
        let attrs = parse_attrs(&field.attrs)?;
        let fn_token = match parse_type(&field.ty)? {
            Type::Primitive(ty) => match ty.to_string().as_str() {
                "u8" => {
                    if attrs.n.is_some() && attrs.n != Some(1) {
                        return Err(Error::new(ty.span(), "Expected N(1) for type `u8`"));
                    }
                    quote_spanned! { ty.span() => write_u8(self.#ident) }
                }
                "u16" => {
                    if attrs.n.is_some() && attrs.n != Some(2) {
                        return Err(Error::new(ty.span(), "Expected N(2) for type `u16`"));
                    }
                    quote_spanned! { ty.span() => write_u16(self.#ident) }
                }
                "u32" => {
                    if attrs.n.is_none() {
                        return Err(Error::new(ty.span(), "`N()` attribute required"));
                    }
                    let n = attrs.n.unwrap();
                    if n > 4 {
                        return Err(Error::new(ty.span(), "Expected N(n <= 4)"));
                    }
                    quote_spanned! { ty.span() => write_u32::<#n>(self.#ident) }
                }
                _ => unimplemented!(),
            },
            Type::VecPrimitive(ty) => {
                if attrs.n.is_some() && attrs.n != Some(1) {
                    return Err(Error::new(ty.span(), "Expected N(1) for type `Vec`"));
                }
                match ty.to_string().as_str() {
                    "u8" => quote_spanned! { ty.span() => write_vec_u8(&self.#ident) },
                    "u16" => quote_spanned! { ty.span() => write_vec_u16(&self.#ident) },
                    _ => unimplemented!(),
                }
            }
            Type::Any() => quote_spanned! { ident.span() => write(&self.#ident) },
            Type::VecAny() => quote_spanned! { ident.span() => write_vec(&self.#ident) },
        };
        fields.push(quote! { w.#fn_token; });
    }

    Ok(quote! {
        impl Write for #name {
            fn write(&self, w: &mut Writer) {
                #( #fields )*
            }
        }
    })
}

/// Derive of `hci::reader::Write` trait
#[proc_macro_derive(Write)]
pub fn derive_write_fn(input: TokenStream) -> TokenStream {
    let input = syn::parse_macro_input!(input as DeriveInput);
    let expanded = write_expand(input).unwrap_or_else(Error::into_compile_error);
    TokenStream::from(expanded)
}
