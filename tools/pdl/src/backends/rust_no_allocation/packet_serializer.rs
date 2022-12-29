use std::collections::HashMap;

use proc_macro2::TokenStream;
use quote::{format_ident, quote};

use crate::{
    ast,
    backends::intermediate::{ComputedValue, ComputedValueId, PacketOrStruct, Schema},
};

fn standardize_child(id: &str) -> &str {
    match id {
        "_body_" | "_payload_" => "_child_",
        _ => id,
    }
}

pub fn generate_packet_serializer(
    id: &str,
    fields: &[ast::Field],
    schema: &Schema,
    curr_schema: &PacketOrStruct,
    children: &HashMap<&str, Vec<&str>>,
) -> TokenStream {
    let id_ident = format_ident!("{id}Builder");

    let builder_fields = fields
        .iter()
        .map(|field| {
            match field {
                ast::Field::Padding { .. }
                | ast::Field::Reserved { .. }
                | ast::Field::Fixed { .. }
                | ast::Field::ElementSize { .. }
                | ast::Field::Count { .. }
                | ast::Field::Size { .. } => {
                    // no-op, no getter generated for this type
                    None
                }
                ast::Field::Group { .. } => unreachable!(),
                ast::Field::Checksum { .. } => {
                    unimplemented!("checksums not yet supported with this backend")
                }
                ast::Field::Body { .. } | ast::Field::Payload { .. } => {
                    let type_ident = format_ident!("{id}Child");
                    Some(("_child_", quote! { #type_ident }))
                }
                ast::Field::Array { id, width, type_id, .. } => {
                    let element_type = if width.is_some() {
                        format_ident!("u64")
                    } else if let Some(type_id) = type_id {
                        if schema.enums.contains_key(type_id.as_str()) {
                            format_ident!("{type_id}")
                        } else {
                            format_ident!("{type_id}Builder")
                        }
                    } else {
                        unreachable!();
                    };
                    Some((id.as_str(), quote! { Box<[#element_type]> }))
                }
                ast::Field::Scalar { id, .. } => Some((id.as_str(), quote! { u64 })),
                ast::Field::Typedef { id, type_id, .. } => {
                    let type_ident = if schema.enums.contains_key(type_id.as_str()) {
                        format_ident!("{type_id}")
                    } else {
                        format_ident!("{type_id}Builder")
                    };
                    Some((id.as_str(), quote! { #type_ident }))
                }
            }
        })
        .filter_map(|x| x)
        .map(|(id, typ)| {
            let id_ident = format_ident!("{id}");
            quote! { #id_ident: #typ }
        });

    let serializer = fields.iter().map(|field| {
        match field {
            ast::Field::Checksum { .. } | ast::Field::Group { .. } => unimplemented!(),
            ast::Field::Padding { size, .. } => {
                quote! {
                    if (most_recent_array_len > #size) {
                        return Err(SerializeError::NegativePadding);
                    }
                    for _ in 0..(#size - most_recent_array_len) {
                        writer.write_bits(8, || Ok(0u64))?;
                    }
                }
            },
            ast::Field::Size { field_id, width, .. } => {
                let field_id = standardize_child(field_id);
                let field_ident = format_ident!("{field_id}");

                // if the element-size is fixed, we can directly multiply
                if let Some(element_width) = curr_schema.computed_values.get(&ComputedValueId::FieldElementSize(field_id)) {
                    if let ComputedValue::Constant(element_width) = element_width {
                        return quote! {
                            writer.write_bits(#width, || (self.#field_ident.len() * #element_width).try_into().or(Err(SerializeError::IntegerConversionFailure)))?;
                        }
                    }
                }

                // if the field is "countable", loop over it to sum up the size
                if curr_schema.computed_values.contains_key(&ComputedValueId::FieldCount(field_id)) {
                    return quote! {
                        writer.write_bits(#width, || self.#field_ident.iter().map(|elem| elem.size_in_bits()).fold(Ok(0), |total, next| {
                            let total = total?;
                            let next = u64::try_from(next?).or(Err(SerializeError::IntegerConversionFailure))?;
                            Ok(total + next)
                        }))?;
                    }
                }

                // otherwise, try to get the size directly
                quote! {
                    writer.write_bits(#width, || self.#field_ident.size_in_bits()?.try_into().or(Err(SerializeError::IntegerConversionFailure)))?;
                }
            }
            ast::Field::Count { field_id, width, .. } => {
                let field_ident = format_ident!("{field_id}");
                quote! { writer.write_bits(#width, || self.#field_ident.len().try_into().or(Err(SerializeError::IntegerConversionFailure)))?; }
            }
            ast::Field::ElementSize { field_id, width, .. } => {
                // TODO(aryarahul) - add validation for elementsize against all the other elements
                let field_ident = format_ident!("{field_id}");
                quote! { writer.write_bits(#width, || self.#field_ident.get(0).map(|x| x.size_in_bits()).or(Ok(0)))?; }
            }
            ast::Field::Reserved { width, .. } => {
                quote!{ writer.write_bits(#width, || Ok(0u64))?; }
            }
            ast::Field::Scalar { width, id, .. } => {
                let field_ident = format_ident!("{id}");
                quote! { writer.write_bits(#width, || Ok(self.#field_ident))?; }
            }
            ast::Field::Fixed { width, enum_id, value, tag_id, .. } => {
                let width = if let Some(width) = width {
                    quote! { #width }
                } else if let Some(enum_id) = enum_id {
                    let width = schema.enums[enum_id.as_str()].width;
                    quote! { #width }
                } else {
                    unreachable!()
                };
                let value = if let Some(tag_id) = tag_id {
                    let enum_ident = format_ident!("{}", enum_id.as_ref().unwrap());
                    let tag_ident = format_ident!("{tag_id}");
                    quote! { #enum_ident::#tag_ident.value() }
                } else if let Some(value) = value {
                    let value = *value as u64;
                    quote! { #value }
                } else {
                    unreachable!()
                };
                quote!{ writer.write_bits(#width, || Ok(#value))?; }
            }
            ast::Field::Body { .. } | ast::Field::Payload { .. } => {
                quote! { self._child_.serialize(writer)?; }
            }
            ast::Field::Array { width, id, .. } => {
                let id_ident = format_ident!("{id}");
                if let Some(width) = width {
                    quote! {
                        for elem in self.#id_ident.iter() {
                            writer.write_bits(#width, || (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure)))?;
                        }
                        let most_recent_array_len = #width * self.#id_ident.len();
                    }
                } else {
                    quote! {
                        let mut most_recent_array_len = 0;
                        for elem in self.#id_ident.iter() {
                            most_recent_array_len += elem.size_in_bits()?;
                            elem.serialize(writer)?;
                        }
                     }
                }
            }
            ast::Field::Typedef { id, .. } => {
                let id_ident = format_ident!("{id}");
                quote! { self.#id_ident.serialize(writer)?; }
            }
        }
    });

    let variant_names = children.get(id).into_iter().flatten().collect::<Vec<_>>();

    let variants = variant_names.iter().map(|name| {
        let name_ident = format_ident!("{name}");
        let variant_ident = format_ident!("{name}Builder");
        quote! { #name_ident(#variant_ident) }
    });

    let variant_serializers = variant_names.iter().map(|name| {
        let name_ident = format_ident!("{name}");
        quote! {
            Self::#name_ident(x) => {
                x.serialize(writer)?;
            }
        }
    });

    let enum_ident = format_ident!("{id}Child");
    let children_enum = quote! {
        enum #enum_ident {
            RawData(Box<[u8]>),
            #(#variants),*
        }

        impl Serializable for #enum_ident {
          fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
            match self {
                Self::RawData(data) => {
                    for byte in data.iter() {
                        writer.write_bits(8, || Ok(*byte as u64))?;
                    }
                },
                #(#variant_serializers),*
            }
            Ok(())
          }
        }
    };

    quote! {
      pub struct #id_ident {
          #(#builder_fields),*
      }

      impl Serializable for #id_ident {
          fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
            #(#serializer)*
            Ok(())
          }
      }

      #children_enum
    }
}
