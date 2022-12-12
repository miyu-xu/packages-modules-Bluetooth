use crate::backends::rust::{mask_bits, types};
use crate::{ast, lint};
use quote::{format_ident, quote};

pub struct FieldSerializer<'a> {
    scope: &'a lint::Scope<'a>,
    endianness: ast::EndiannessValue,
    packet_name: &'a str,
    span: &'a proc_macro2::Ident,
    chunk: Vec<(proc_macro2::TokenStream, Option<proc_macro2::Literal>)>,
    code: Vec<proc_macro2::TokenStream>,
    shift: usize,
}

impl<'a> FieldSerializer<'a> {
    pub fn new(
        scope: &'a lint::Scope<'a>,
        endianness: ast::EndiannessValue,
        packet_name: &'a str,
        span: &'a proc_macro2::Ident,
    ) -> FieldSerializer<'a> {
        FieldSerializer {
            scope,
            endianness,
            packet_name,
            span,
            chunk: Vec::new(),
            code: Vec::new(),
            shift: 0,
        }
    }

    pub fn add(&mut self, field: &ast::Field) {
        if field.is_bitfield(self.scope) {
            self.add_bit_field(field);
        } else {
            match field {
                ast::Field::Array { id, width, type_id, .. } => {
                    self.add_array_field(id, *width, type_id.as_deref())
                }
                _ => todo!(),
            }
        }
    }

    fn add_bit_field(&mut self, field: &ast::Field) {
        let width = field.width(self.scope).unwrap();
        let shift = if self.shift > 0 {
            Some(proc_macro2::Literal::usize_unsuffixed(self.shift))
        } else {
            None
        };

        match field {
            ast::Field::Scalar { id, width, .. } => {
                let field_name = format_ident!("{id}");
                let field_type = types::Integer::new(*width);
                if field_type.width > *width {
                    let packet_name = &self.packet_name;
                    let max_value = mask_bits(*width);
                    self.code.push(quote! {
                        if self.#field_name > #max_value {
                            panic!(
                                "Invalid value for {}::{}: {} > {}",
                                #packet_name, #id, self.#field_name, #max_value
                            );
                        }
                    });
                }
                self.chunk.push((quote!(self.#field_name), shift));
            }
            ast::Field::Typedef { id, .. } => {
                let field_name = format_ident!("{id}");
                let field_type = types::Integer::new(width);
                let to_u = format_ident!("to_u{}", field_type.width);
                self.chunk.push((quote!(self.#field_name.#to_u().unwrap()), shift));
            }
            _ => todo!(),
        }

        self.shift += width;
        if self.shift % 8 == 0 {
            self.pack_bit_fields()
        }
    }

    fn pack_bit_fields(&mut self) {
        assert_eq!(self.shift % 8, 0);
        let chunk_type = types::Integer::new(self.shift);
        let chunk_len = self.chunk.len();
        let values = self
            .chunk
            .drain(..)
            .map(|(mut value, shift)| {
                if chunk_len > 1 {
                    // We will be combining values with `|`, so we
                    // need to cast them first. If there is a single
                    // value in the chunk, `put_uint` will cast.
                    value = quote! { (#value as #chunk_type) };
                }
                if let Some(shift) = shift {
                    let op = quote!(<<);
                    value = quote! { (#value #op #shift) };
                }
                value
            })
            .collect::<Vec<_>>();

        match values.as_slice() {
            [] => todo!(),
            [value] => {
                let put = types::put_uint(self.endianness, value, self.shift, self.span);
                self.code.push(quote! {
                    #put;
                });
            }
            _ => {
                let put = types::put_uint(self.endianness, &quote!(value), self.shift, self.span);
                self.code.push(quote! {
                    let value = #(#values)|*;
                    #put;
                });
            }
        }

        self.shift = 0;
    }

    fn add_array_field(&mut self, id: &str, width: Option<usize>, type_id: Option<&str>) {
        // TODO: padding

        let element_field = types::array_element_type(id, width, type_id);
        let element_width = element_field.width(self.scope).unwrap();
        let to_u = format_ident!("to_u{}", element_width);
        let value = match element_field {
            ast::Field::Scalar { .. } => quote!(elem),
            ast::Field::Typedef { .. } => quote!(elem.#to_u().unwrap()),
            _ => todo!(),
        };

        let id = format_ident!("{id}");
        let put = types::put_uint(self.endianness, &value, element_width, self.span);
        self.code.push(quote! {
            for elem in self.#id {
                #put;
            }
        })
    }
}

impl quote::ToTokens for FieldSerializer<'_> {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let code = &self.code;
        tokens.extend(quote! {
            #(#code)*
        });
    }
}
