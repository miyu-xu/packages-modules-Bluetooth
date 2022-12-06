use crate::ast;
use crate::backends::rust::{mask_bits, types};
use quote::{format_ident, quote};

pub struct FieldParser {
    endianness: ast::EndiannessValue,
    packet_name: String,
    span: proc_macro2::Ident,
    chunk: Vec<(usize, usize, ast::Field)>,
    code: Vec<proc_macro2::TokenStream>,
    shift: usize,
    offset: usize,
}

impl FieldParser {
    pub fn new(
        endianness: ast::EndiannessValue,
        packet_name: &str,
        span: &proc_macro2::Ident,
    ) -> FieldParser {
        FieldParser {
            endianness,
            packet_name: packet_name.to_string(),
            span: span.clone(),
            chunk: Vec::new(),
            code: Vec::new(),
            shift: 0,
            offset: 0,
        }
    }

    fn endianness_suffix(&self, width: usize) -> &'static str {
        if width > 8 && self.endianness == ast::EndiannessValue::LittleEndian {
            "_le"
        } else {
            ""
        }
    }

    /// Parse an unsigned integer with the given `width`.
    ///
    /// The generated code requires that `self.span` is a mutable
    /// `bytes::Buf` value.
    fn get_uint(&self, width: usize) -> proc_macro2::TokenStream {
        let span = &self.span;
        let suffix = self.endianness_suffix(width);
        let value_type = types::Integer::new(width);
        if value_type.width == width {
            let get_u = format_ident!("get_u{}{}", value_type.width, suffix);
            quote! {
                #span.#get_u()
            }
        } else {
            let get_uint = format_ident!("get_uint{}", suffix);
            let value_nbytes = proc_macro2::Literal::usize_unsuffixed(width / 8);
            let cast = (value_type.width < 64).then(|| quote!(as #value_type));
            quote! {
                #span.#get_uint(#value_nbytes) #cast
            }
        }
    }

    pub fn add(&mut self, field: &ast::Field) {
        if field.is_bitfield() {
            self.add_bit_field(field);
            return;
        }

        todo!("not yet supported: {field:?}")
    }

    fn add_bit_field(&mut self, field: &ast::Field) {
        let width = field.width().unwrap();
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

        let get = self.get_uint(self.shift);
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
                _ => todo!(),
            });
        }

        self.offset = end_offset;
        self.shift = 0;
        self.chunk.clear();
    }

    pub fn done(&mut self) {}
}

impl quote::ToTokens for FieldParser {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let code = &self.code;
        tokens.extend(quote! {
            #(#code)*
        });
    }
}
