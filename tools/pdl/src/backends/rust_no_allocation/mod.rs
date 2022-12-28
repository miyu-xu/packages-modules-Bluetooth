//! Rust no-allocation backend
//!
//! The motivation for this backend is to be a more "idiomatic" backend than
//! the existing backend. Specifically, it should
//! 1. Use lifetimes, not reference counting
//! 2. Avoid expensive memory copies unless needed
//! 3. Use the intermediate Schema rather than doing all the logic from scratch
//!
//! One notable consequence is that we avoid .specialize(), as it has "magic" behavior
//! not defined in the spec. Instead we mimic the C++ approach of calling tryParse() and
//! getting a Result<> back.

mod preamble;
pub mod test;

use std::iter::empty;

use proc_macro2::{Ident, TokenStream};
use quote::{format_ident, quote};

use crate::{ast, quote_block};

use super::intermediate::{
    ComputedOffset, ComputedOffsetId, ComputedValue, ComputedValueId, PacketOrStruct,
    PacketOrStructLength, Schema,
};

pub fn generate(file: &ast::File, schema: &Schema) -> Result<String, String> {
    match file.endianness.value {
        ast::EndiannessValue::LittleEndian => {}
        _ => unimplemented!("Only little_endian endianness supported"),
    };

    let mut out = String::new();

    out.push_str(include_str!("preamble.rs"));

    for decl in &file.declarations {
        generate_decl(&mut out, decl, schema)?;
    }

    Ok(out)
}

fn generate_decl(out: &mut String, decl: &ast::Decl, schema: &Schema) -> Result<(), String> {
    match decl {
        ast::Decl::Enum { id, tags, width, .. } => generate_enum(out, id, tags, *width),
        ast::Decl::Packet { id, fields, parent_id, .. } => generate_packet(
            out,
            id,
            fields,
            parent_id.as_ref().map(|x| &**x),
            schema,
            &schema.packets[id.as_str()].0,
        ),
        ast::Decl::Struct { id, fields, parent_id, .. } => generate_packet(
            out,
            id,
            fields,
            parent_id.as_ref().map(|x| &**x),
            schema,
            &schema.structs[id.as_str()].0,
        ),
        _ => unimplemented!("Unsupported decl type"),
    }
}

fn generate_enum(
    out: &mut String,
    id: &str,
    tags: &[ast::Tag],
    width: usize,
) -> Result<(), String> {
    let id_ident = format_ident!("{id}");
    let tag_ids = tags.iter().map(|tag| format_ident!("{}", tag.id)).collect::<Vec<_>>();
    let tag_values = tags.iter().map(|tag| tag.value as u64).collect::<Vec<_>>();

    out.push_str(&quote_block! {
      #[derive(Copy, Clone, PartialEq, Eq, Debug)]
      pub enum #id_ident {
          #(#tag_ids),*
      }

      impl #id_ident {
          fn try_parse(buf: BitSlice) -> Result<Self, ParseError> {
              let value = buf.slice(#width)?.try_parse()?;
              match value {
                  #(#tag_values => Ok(Self::#tag_ids)),*,
                  _ => Err(ParseError::InvalidEnumValue),
              }
          }

          fn value(&self) -> u64 {
            match self {
                #(Self::#tag_ids => #tag_values),*,
            }
          }
      }

      impl From<#id_ident> for u64 {
        fn from(x: #id_ident) -> u64 {
            x.value()
        }
      }
    });

    Ok(())
}

/// This trait is implemented on computed quantities (offsets and values) that can be retrieved via a function call
trait Declarable {
    fn get_name(&self) -> String;

    fn get_ident(&self) -> Ident {
        format_ident!("try_get_{}", self.get_name())
    }

    fn call_fn(&self) -> TokenStream {
        let fn_name = self.get_ident();
        quote! { self.#fn_name()? }
    }

    fn declare_fn(&self, body: TokenStream) -> TokenStream {
        let fn_name = self.get_ident();
        quote! {
            #[inline]
            fn #fn_name(&self) -> Result<usize, ParseError> {
                #body
            }
        }
    }
}

impl Declarable for ComputedValueId<'_> {
    fn get_name(&self) -> String {
        match self {
            ComputedValueId::FieldSize(field) => format!("{field}_size"),
            ComputedValueId::FieldElementSize(field) => format!("{field}_element_size"),
            ComputedValueId::FieldCount(field) => format!("{field}_count"),
            ComputedValueId::Custom(i) => format!("custom_value_{i}"),
        }
    }
}

impl Declarable for ComputedOffsetId<'_> {
    fn get_name(&self) -> String {
        match self {
            ComputedOffsetId::HeaderStart => "header_start_offset".to_string(),
            ComputedOffsetId::PacketEnd => "packet_end_offset".to_string(),
            ComputedOffsetId::FieldOffset(field) => format!("{field}_offset"),
            ComputedOffsetId::FieldEndOffset(field) => format!("{field}_end_offset"),
            ComputedOffsetId::Custom(i) => format!("custom_offset_{i}"),
            ComputedOffsetId::TrailerStart => "trailer_start_offset".to_string(),
        }
    }
}

/// This trait is implemented on computed expressions that are computed on-demand (i.e. not via a function call)
trait Computable {
    fn compute(&self) -> TokenStream;
}

impl Computable for ComputedValue<'_> {
    fn compute(&self) -> TokenStream {
        match self {
            ComputedValue::Constant(k) => quote! { Ok(#k) },
            ComputedValue::CountStructsUpToSize { base_id, size, struct_type } => {
                let base_offset = base_id.call_fn();
                let size = size.call_fn();
                let struct_type = format_ident!("{struct_type}View");
                quote! {
                    let mut cnt = 0;
                    let mut view = self.buf.offset(#base_offset)?;
                    let mut remaining_size = #size;
                    while remaining_size > 0 {
                        let next_struct_size = #struct_type::try_parse(view)?.try_get_size()?;
                        if next_struct_size > remaining_size {
                            return Err(ParseError::OutOfBoundsAccess);
                        }
                        remaining_size -= next_struct_size;
                        view = view.offset(next_struct_size * 8)?;
                        cnt += 1;
                    }
                    Ok(cnt)
                }
            }
            ComputedValue::SizeOfNStructs { base_id, n, struct_type } => {
                let base_offset = base_id.call_fn();
                let n = n.call_fn();
                let struct_type = format_ident!("{struct_type}View");
                quote! {
                    let mut view = self.buf.offset(#base_offset)?;
                    let mut size = 0;
                    for _ in 0..#n {
                        let next_struct_size = #struct_type::try_parse(view)?.try_get_size()?;
                        size += next_struct_size;
                        view = view.offset(next_struct_size * 8)?;
                    }
                    Ok(size)
                }
            }
            ComputedValue::Product(x, y) => {
                let x = x.call_fn();
                let y = y.call_fn();
                quote! { #x.checked_mul(#y).ok_or(ParseError::ArithmeticOverflow) }
            }
            ComputedValue::Divide(x, y) => {
                let x = x.call_fn();
                let y = y.call_fn();
                quote! {
                    if #y == 0 || #x % #y != 0 {
                        return Err(ParseError::DivisionFailure)
                    }
                    Ok(#x / #y)
                }
            }
            ComputedValue::Difference(x, y) => {
                let x = x.call_fn();
                let y = y.call_fn();
                quote! {
                   let bit_difference = #x.checked_sub(#y).ok_or(ParseError::ArithmeticOverflow)?;
                   if bit_difference % 8 != 0 {
                       return Err(ParseError::DivisionFailure);
                   }
                   Ok(bit_difference / 8)
                }
            }
            ComputedValue::ValueAt { offset, width } => {
                let offset = offset.call_fn();
                quote! { Ok(self.buf.offset(#offset)?.slice(#width)?.try_parse()? as usize) }
            }
        }
    }
}

impl Computable for ComputedOffset<'_> {
    fn compute(&self) -> TokenStream {
        match self {
            ComputedOffset::ConstantPlusOffsetInBits(base_id, offset) => {
                let base_id = base_id.call_fn();
                quote! { #base_id.checked_add_signed(#offset as isize).ok_or(ParseError::ArithmeticOverflow) }
            }
            ComputedOffset::SumWithOctets(x, y) => {
                let x = x.call_fn();
                let y = y.call_fn();
                quote! {
                    #x.checked_add(#y.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)
                      .ok_or(ParseError::ArithmeticOverflow)
                }
            }
            ComputedOffset::Alias(alias) => {
                let alias = alias.call_fn();
                quote! { Ok(#alias) }
            }
        }
    }
}

fn generate_packet(
    out: &mut String,
    id: &str,
    fields: &[ast::Field],
    parent_id: Option<&str>,
    schema: &Schema,
    curr_schema: &PacketOrStruct,
) -> Result<(), String> {
    let id_ident = format_ident!("{id}View");

    let needs_external = matches!(curr_schema.length, PacketOrStructLength::NeedsExternal);

    let length_getter = if needs_external {
        ComputedOffsetId::PacketEnd.declare_fn(quote! { Ok(self.buf.get_size_in_bits()) })
    } else {
        quote! {}
    };

    let computed_getters = empty()
        .chain(
            curr_schema.computed_offsets.iter().map(|(decl, defn)| decl.declare_fn(defn.compute())),
        )
        .chain(
            curr_schema.computed_values.iter().map(|(decl, defn)| decl.declare_fn(defn.compute())),
        );

    let field_getters = fields.iter().map(|field| {
        match field {
            ast::Field::Padding { .. }
            | ast::Field::Reserved { .. }
            | ast::Field::Fixed { .. }
            | ast::Field::ElementSize { .. }
            | ast::Field::Count { .. }
            | ast::Field::Size { .. } => {
                // no-op, no getter generated for this type
                quote! {}
            }
            ast::Field::Group { .. } => unreachable!(),
            ast::Field::Checksum { .. } => {
                unimplemented!("checksums not yet supported with this backend")
            }
            ast::Field::Payload { .. } => {
                let payload_start_offset = ComputedOffsetId::FieldOffset("_payload_").call_fn();
                let payload_end_offset = ComputedOffsetId::FieldEndOffset("_payload_").call_fn();
                quote! {
                    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
                        let payload_start_offset = #payload_start_offset;
                        let payload_end_offset = #payload_end_offset;
                        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
                    }
                }
            }
            ast::Field::Body { .. } => {
                let payload_start_offset = ComputedOffsetId::FieldOffset("_body_").call_fn();
                let payload_end_offset = ComputedOffsetId::FieldEndOffset("_body_").call_fn();
                quote! {
                    // note: this is called payload, not body, intentionally!
                    // (since this backend does not distinguish them)
                    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
                        let payload_start_offset = #payload_start_offset;
                        let payload_end_offset = #payload_end_offset;
                        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
                    }
                }
            }
            ast::Field::Array { id, width, type_id, .. } => {
                let (elem_type, return_type) = if let Some(_) = width {
                    (format_ident!("u64"), quote!{ u64 })
                } else if let Some(type_id) = type_id {
                    if schema.enums.contains_key(type_id.as_str()) {
                        let ident = format_ident!("{}", type_id);
                        (ident.clone(), quote! { #ident })
                    } else {
                        let ident = format_ident!("{}View", type_id);
                        (ident.clone(), quote! { #ident<'a> })
                    }
                } else {
                    unreachable!()
                };

                let try_getter_name = format_ident!("try_get_{id}_iter");
                let getter_name = format_ident!("get_{id}_iter");

                let start_offset = ComputedOffsetId::FieldOffset(id).call_fn();
                let count = ComputedValueId::FieldCount(id).call_fn();

                let element_size_known = curr_schema
                    .computed_values
                    .contains_key(&ComputedValueId::FieldElementSize(id));

                let body = if element_size_known {
                    let element_size = ComputedValueId::FieldElementSize(id).call_fn();
                    let parsed_curr_view = if let Some(_) = width {
                        quote! { curr_view.try_parse() }
                    } else {
                        quote! { #elem_type::try_parse(curr_view.into()) }
                    };
                    quote! {
                        let view = self.buf.offset(#start_offset)?;
                        let count = #count;
                        let element_size = #element_size;
                        Ok((0..count).map(move |i| {
                            let curr_view = view.offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                                    .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
                            #parsed_curr_view
                        }))
                    }
                } else {
                    quote! {
                        let mut view = self.buf.offset(#start_offset)?;
                        let count = #count;
                        Ok((0..count).map(move |i| {
                            let parsed = #elem_type::try_parse(view.into())?;
                            view = view.offset(parsed.try_get_size()? * 8)?;
                            Ok(parsed)
                        }))
                    }
                };

                quote! {
                    fn #try_getter_name(&self) -> Result<impl Iterator<Item = Result<#return_type, ParseError>> + 'a, ParseError> {
                        #body
                    }

                    #[inline]
                    pub fn #getter_name(&self) -> impl Iterator<Item = #return_type> + 'a {
                        self.#try_getter_name().unwrap().map(|x| x.unwrap())
                    }
                }
            }
            ast::Field::Scalar { id, width, .. } => {
                let try_getter_name = format_ident!("try_get_{id}");
                let getter_name = format_ident!("get_{id}");
                let offset = ComputedOffsetId::FieldOffset(id).call_fn();
                quote! {
                    fn #try_getter_name(&self) -> Result<u64, ParseError> {
                        self.buf.offset(#offset)?.slice(#width)?.try_parse()
                    }

                    #[inline]
                    pub fn #getter_name(&self) -> u64 {
                        self.#try_getter_name().unwrap()
                    }
                }
            }
            ast::Field::Typedef { id, type_id, .. } => {
                let try_getter_name = format_ident!("try_get_{id}");
                let getter_name = format_ident!("get_{id}");

                let (type_ident, return_type) = if schema.enums.contains_key(type_id.as_str()) {
                    let ident = format_ident!("{type_id}");
                    (ident.clone(), quote! { #ident })
                } else {
                    let ident = format_ident!("{}View", type_id);
                    (ident.clone(), quote! { #ident<'a> })
                };
                let offset = ComputedOffsetId::FieldOffset(id).call_fn();
                let end_offset_known = curr_schema
                    .computed_offsets
                    .contains_key(&ComputedOffsetId::FieldEndOffset(id));
                let sliced_view = if end_offset_known {
                    let element_size = ComputedOffsetId::FieldEndOffset(id).call_fn();
                    quote! { self.buf.offset(#offset)?.slice(#element_size)? }
                } else {
                    quote! { self.buf.offset(#offset)? }
                };

                quote! {
                    fn #try_getter_name(&self) -> Result<#return_type, ParseError> {
                        #type_ident::try_parse(#sliced_view.into())
                    }

                    #[inline]
                    pub fn #getter_name(&self) -> #return_type {
                        self.#try_getter_name().unwrap()
                    }
                }
            }
        }
    });

    let backing_buffer = if needs_external {
        quote! { SizedBitSlice<'a> }
    } else {
        quote! { BitSlice<'a> }
    };

    let parent_ident = match parent_id {
        Some(parent) => format_ident!("{parent}View"),
        None => match curr_schema.length {
            PacketOrStructLength::Static(_) => format_ident!("BitSlice"),
            PacketOrStructLength::Dynamic => format_ident!("BitSlice"),
            PacketOrStructLength::NeedsExternal => format_ident!("SizedBitSlice"),
        },
    };

    let buffer_extractor = if parent_id.is_some() {
        quote! { parent.try_get_payload().unwrap().into() }
    } else {
        quote! { parent }
    };

    let field_validators = fields.iter().map(|field| match field {
        ast::Field::Checksum { .. } => unimplemented!(),
        ast::Field::Group { .. } => unreachable!(),
        ast::Field::Padding { .. }
        | ast::Field::Size { .. }
        | ast::Field::Count { .. }
        | ast::Field::ElementSize { .. }
        | ast::Field::Body { .. }
        | ast::Field::Fixed { .. }
        | ast::Field::Reserved { .. } => {
            quote! {}
        }
        ast::Field::Payload { .. } => {
            quote! { self.try_get_payload()?; }
        }
        ast::Field::Array { id, .. } => {
            let iter_ident = format_ident!("try_get_{id}_iter");
            quote! {
                for elem in self.#iter_ident()? {
                    elem?;
                }
            }
        }
        ast::Field::Scalar { id, .. } | ast::Field::Typedef { id, .. } => {
            let getter_ident = format_ident!("try_get_{id}");
            quote! { self.#getter_ident()?; }
        }
    });

    let packet_end_offset = ComputedOffsetId::PacketEnd.call_fn();

    out.push_str(&quote_block! {
        #[derive(Clone, Copy, Debug)]
        pub struct #id_ident<'a> {
            buf: #backing_buffer,
        }

        impl<'a> #id_ident<'a> {
            #length_getter

            #(#computed_getters)*

            #(#field_getters)*

            #[inline]
            fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
                Ok(0)
            }

            #[inline]
            fn try_get_size(&self) -> Result<usize, ParseError> {
                let size = #packet_end_offset;
                if size % 8 != 0 {
                    return Err(ParseError::MisalignedPayload);
                }
                Ok(size / 8)
            }

            fn validate(&self) -> Result<(), ParseError> {
                #(#field_validators)*
                Ok(())
            }

            pub fn try_parse<'b>(parent: #parent_ident<'a>) -> Result<Self, ParseError> {
                let out = Self { buf: #buffer_extractor };
                out.validate()?;
                Ok(out)
            }
        }
    });

    Ok(())
}
