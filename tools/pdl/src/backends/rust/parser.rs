use crate::backends::rust::{mask_bits, types};
use crate::{ast, lint};
use quote::{format_ident, quote};

pub struct FieldParser<'a> {
    scope: &'a lint::Scope<'a>,
    endianness: ast::EndiannessValue,
    packet_name: &'a str,
    span: &'a proc_macro2::Ident,
    chunk: Vec<(usize, usize, ast::Field)>,
    code: Vec<proc_macro2::TokenStream>,
    shift: usize,
    offset: usize,
}

impl<'a> FieldParser<'a> {
    pub fn new(
        scope: &'a lint::Scope<'a>,
        endianness: ast::EndiannessValue,
        packet_name: &'a str,
        span: &'a proc_macro2::Ident,
    ) -> FieldParser<'a> {
        FieldParser {
            scope,
            endianness,
            packet_name,
            span,
            chunk: Vec::new(),
            code: Vec::new(),
            shift: 0,
            offset: 0,
        }
    }

    pub fn add(&mut self, field: &ast::Field) {
        if field.is_bitfield(self.scope) {
            self.add_bit_field(field);
            return;
        }

        match field {
            ast::Field::Array { id, width, type_id, size, .. } => {
                self.add_array_field(id, *width, type_id.as_deref(), *size)
            }
            _ => todo!("{field:?}"),
        }
    }

    fn add_bit_field(&mut self, field: &ast::Field) {
        let width = field.width(self.scope).unwrap();
        self.chunk.push((self.shift, width, field.clone()));
        self.shift += width;
        if self.shift % 8 != 0 {
            return;
        }

        let size = self.shift / 8;
        let end_offset = self.offset + size;

        let wanted = proc_macro2::Literal::usize_unsuffixed(size);
        let packet_name = &self.packet_name;
        self.code.push(quote! {
            if bytes.remaining() < #wanted {
                return Err(Error::InvalidLengthError {
                    obj: #packet_name.to_string(),
                    wanted: #wanted,
                    got: bytes.remaining(),
                });
            }
        });

        let chunk_type = types::Integer::new(self.shift);
        let chunk_name = format_ident!("chunk");

        let get = types::get_uint(self.endianness, self.shift, self.span);
        if self.chunk.len() > 1 {
            // Multiple values: we to read into a local variable.
            self.code.push(quote! {
                let #chunk_name = #get;
            });
        }
        for (shift, width, field) in &self.chunk {
            let value_type = types::Integer::new(*width);

            let mut v = if self.chunk.len() == 1 && *shift == 0 {
                // Single value: read directly.
                quote! { #get }
            } else {
                // Multiple values: read from `chunk_name`.
                quote! { #chunk_name }
            };

            if *shift > 0 {
                let shift = proc_macro2::Literal::usize_unsuffixed(*shift);
                v = quote! { (#v >> #shift) }
            }
            if *width % 8 != 0 {
                let mask = mask_bits(*width);
                v = quote! { (#v & #mask) };
            }
            if value_type.width < chunk_type.width {
                v = quote! { #v as #value_type };
            }

            self.code.push(match field {
                ast::Field::Scalar { id, .. } => {
                    let id = format_ident!("{id}");
                    quote! {
                        let #id = #v;
                    }
                }
                ast::Field::Typedef { id, type_id, .. } => {
                    let id = format_ident!("{id}");
                    let type_id = format_ident!("{type_id}");
                    let from_u = format_ident!("from_u{}", value_type.width);
                    quote! {
                        let #id = #type_id::#from_u(#v).unwrap();
                    }
                }
                _ => todo!(),
            });
        }

        self.offset = end_offset;
        self.shift = 0;
        self.chunk.clear();
    }

    fn add_array_field(
        &mut self,
        id: &str,
        width: Option<usize>,
        type_id: Option<&str>,
        size: Option<usize>,
    ) {
        let array_size = self.get_array_size(id, size);
        let element_type = types::array_element_type(id, width, type_id);
        let element_width = element_type.width(self.scope);

        if let Some(width) = element_width {
            assert_eq!(width % 8, 0);
        }

        let count = match array_size {
            ArraySize::Static(size) => {
                let size = proc_macro2::Literal::usize_unsuffixed(size);
                Some(quote!(#size))
            }
            ArraySize::CountField => {
                let count_field = format_ident!("{id}_count");
                Some(quote!(self.#count_field))
            }
            ArraySize::SizeField | ArraySize::Unknown => None,
        };

        let size = match array_size {
            ArraySize::SizeField => {
                let size_field = format_ident!("{id}_size");
                Some(quote!(self.#size_field))
            }
            ArraySize::Static(_) | ArraySize::CountField | ArraySize::Unknown => None,
        };

        // TODO consume_span

        // TODO size modifier

        // TODO padded_size

        let id = format_ident!("{id}");
        let span = self.span;
        match (element_width, count, size) {
            (None, _, Some(size)) => {
                self.code.push(quote! {
                    let array_span = #span.split_to(#size);
                    let self.#id = Vec::new();
                    while !array_span.is_empty() {
                        parse_array_element_dynamic()
                    }
                });
            }
            (None, Some(count), _) => {
                self.code.push(quote! {
                    let self.#id = Vec::new();
                    for _ in 0..#count {
                        parse_array_element_dynamic()
                    }
                });
            }
            (None, _, _) => {
                self.code.push(quote! {
                    let self.#id = Vec::new();
                    while !#span.is_empty() {
                        parse_array_element_dynamic()
                    }
                });
            }
            (Some(_), Some(_), _) => {
                // The element width is known, and the array element
                // count is known statically, or by count field.

                // TODO: This only handles arrays with a fixed size.
                // If the count file is used, we actually need to
                // return a Vec.

                // TODO: check size
                let parse_array_element = self.parse_array_element_static(self.span, &element_type);
                self.code.push(quote! {
                    let #id = std::array::from_fn(|_| #parse_array_element);
                });
            }
            (Some(_element_width), None, size) => {
                if let Some(_size) = &size {
                    // check_size(size)
                }

                let _array_size = size.unwrap_or(quote!(len(#span)));
            }
        }
    }

    fn parse_array_element_static(
        &'a self,
        span: &'a proc_macro2::Ident,
        element_field: &'a ast::Field,
    ) -> proc_macro2::TokenStream {
        match element_field {
            ast::Field::Scalar { width, .. } => types::get_uint(self.endianness, *width, span),
            ast::Field::Typedef { type_id, .. } => {
                let width = element_field.width(self.scope).unwrap();
                let element_type = types::Integer::new(width);
                let get_uint = types::get_uint(self.endianness, width, span);
                let type_id = format_ident!("{type_id}");
                let from_u = format_ident!("from_u{}", element_type.width);
                quote! {
                    #type_id::#from_u(#get_uint).unwrap()
                }
            }
            _ => todo!(),
        }
    }

    fn get_array_size(&self, _id: &str, size: Option<usize>) -> ArraySize {
        match size {
            Some(size) => ArraySize::Static(size),
            None => todo!(),
        }
    }

    pub fn done(&mut self) {}
}

// TODO: remove dead code.
enum ArraySize {
    Static(usize),
    #[allow(dead_code)]
    SizeField,
    #[allow(dead_code)]
    CountField,
    #[allow(dead_code)]
    Unknown,
}

impl quote::ToTokens for FieldParser<'_> {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let code = &self.code;
        tokens.extend(quote! {
            #(#code)*
        });
    }
}
