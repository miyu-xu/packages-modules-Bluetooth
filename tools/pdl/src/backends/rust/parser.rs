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
        if field.is_bitfield() {
            self.add_bit_field(field);
            return;
        }

        match field {
            ast::Field::Array { id, width, type_id, size, .. } => self.add_array_field(
                id,
                *width,
                type_id.as_deref(),
                *size,
                field.declaration(self.scope),
            ),
            _ => todo!("{field:?}"),
        }
    }

    fn add_bit_field(&mut self, field: &ast::Field) {
        let width = field.width(self.scope, false).unwrap();
        self.chunk.push((self.shift, width, field.clone()));
        self.shift += width;
        if self.shift % 8 != 0 {
            return;
        }

        let size = self.shift / 8;
        let end_offset = self.offset + size;

        let wanted = proc_macro2::Literal::usize_unsuffixed(size);
        self.check_size(&quote!(#wanted));

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
                ast::Field::Size { field_id, .. } => {
                    let id = format_ident!("{field_id}_size");
                    quote! {
                        let #id = #v;
                    }
                }
                ast::Field::Count { field_id, .. } => {
                    let id = format_ident!("{field_id}_count");
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

    fn packet_scope(&self) -> Option<&lint::PacketScope> {
        self.scope.scopes.get(self.scope.typedef.get(self.packet_name)?)
    }

    fn find_count_field(&self, id: &str) -> Option<proc_macro2::Ident> {
        let field_path = self.packet_scope()?.sizes.get(id)?;
        match field_path.0.last()? {
            ast::Field::Count { .. } => Some(format_ident!("{id}_count")),
            _ => None,
        }
    }

    fn find_size_field(&self, id: &str) -> Option<proc_macro2::Ident> {
        let field_path = self.packet_scope()?.sizes.get(id)?;
        match field_path.0.last()? {
            ast::Field::Size { .. } => Some(format_ident!("{id}_size")),
            _ => None,
        }
    }

    fn check_size(&mut self, wanted: &proc_macro2::TokenStream) {
        let packet_name = &self.packet_name;
        let span = self.span;
        self.code.push(quote! {
            if #span.remaining() < #wanted {
                return Err(Error::InvalidLengthError {
                    obj: #packet_name.to_string(),
                    wanted: #wanted,
                    got: #span.remaining(),
                });
            }
        });
    }

    fn add_array_field(
        &mut self,
        id: &str,
        // `width`: the width in bits of the array elements (if Some).
        width: Option<usize>,
        // `type_id`: the enum type of the array elements (if Some).
        // Mutually exclusive with `width`.
        type_id: Option<&str>,
        // `size`: the size of the array in number of elements (if
        // known). If None, the array is a Vec with a dynamic size.
        size: Option<usize>,
        decl: Option<&ast::Decl>,
    ) {
        // Array element width in bytes.
        let element_width = width.or_else(|| decl?.width(self.scope, false)).map(|w| {
            assert_eq!(w % 8, 0, "Array element size ({w}) is not a multiple of 8");
            syn::Index::from(w / 8)
        });

        // The number of array elements, either as a static size or a dynamic field.
        let count_static = size;
        let count_field = self.find_count_field(id);
        // The bit width of the array. Should probably be called `array_width`
        let size_field = self.find_size_field(id);

        // TODO consume_span

        // TODO size modifier

        // TODO padded_size

        let id = format_ident!("{id}");
        let span = self.span;

        //        enum Count {
        //            Static(proc_macro2::TokenStream),
        //            Dynamic(proc_macro2::Ident),
        //        }

        let parse_element = self.parse_array_element(self.span, width, type_id, decl);

        struct Shape {
            element_width: Option<syn::Index>,
            count_static: Option<usize>,
            count_field: Option<proc_macro2::Ident>,
            size_field: Option<proc_macro2::Ident>,
        }
        let shape = Shape { element_width, count_static, count_field, size_field };
        match shape {
            Shape {
                element_width: None,
                count_static: None,
                count_field: None,
                size_field: Some(size_field),
            } => {
                // The element width is not known, but the array full
                // octet size is known by size field. Parse elements
                // item by item as a vector.
                self.check_size(&quote!(#size_field as usize));
                self.code.push(quote! {
                    let __case_1__ = "111";
                    let array_span = #span.split_to(#size_field);
                    let #id = Vec::new();
                    while !array_span.is_empty() {
                        parse_array_element_dynamic()
                    }
                });
            }
            Shape {
                element_width: None,
                count_static: Some(count),
                count_field: None,
                size_field: None,
            } => {
                // The element width is not known, but the array
                // element count is known statically. Parse elements
                // item by item as an array.

                let count = proc_macro2::Literal::usize_unsuffixed(count);
                self.code.push(quote! {
                    let __case_2__ = "222";
                    let #id = (0..#count).map(|_| #parse_element).collect();
                });
                todo!("2");
            }
            Shape {
                element_width: None,
                count_static: None,
                count_field: Some(count_field),
                size_field: None,
            } => {
                // The element width is not known, but the array
                // element count is known by the count field. Parse
                // elements item by item as a vector.
                self.code.push(quote! {
                    let __case_3__ = "333";
                    let #id = (0..#count_field).map(|_| #parse_element + 1).collect();
                });
            }
            Shape {
                element_width: None,
                count_static: None,
                count_field: None,
                size_field: None,
            } => {
                // Neither the count not size is known, parse elements
                // until the end of the span.
                self.code.push(quote! {
                    let __case_4__ = "444";
                    let #id = Vec::new();
                    while !#span.is_empty() {
                        parse_array_element_dynamic()
                    }
                });
                todo!("4");
            }
            Shape {
                element_width: Some(element_width),
                count_static: Some(count),
                count_field: None,
                size_field: None,
            } => {
                // The element width is known, and the array element
                // count is known statically.
                let count = syn::Index::from(count);

                // This creates a nicely formatted size.
                let array_size = if element_width.index == 1 {
                    quote!(#count)
                } else {
                    quote!(#count * #element_width)
                };
                self.check_size(&array_size);
                self.code.push(quote! {
                    let __case_5__ = "555";
                    let #id = std::array::from_fn(|_| #parse_element);
                });
            }
            Shape {
                element_width: Some(_),
                count_static: None,
                count_field: Some(count_field),
                size_field: None,
            } => {
                // The element width is known, and the array element
                // count is known dynamically by the count field.
                self.check_size(&quote!(#count_field as usize));
                self.code.push(quote! {
                    let __case_6__ = "666";
                    let #id = (0..#count_field).map(|_| #parse_element).collect();
                });
            }
            Shape {
                element_width: Some(element_width),
                count_static: None,
                count_field: None,
                size_field,
            } => {
                // The element width is known, and the array full size
                // is known by size field, or unknown (in which case
                // it is the remaining span length).
                if let Some(size_field) = &size_field {
                    self.check_size(&quote!(#size_field as usize));
                }
                let array_size =
                    size_field.map(|size| quote!(#size)).unwrap_or(quote!(#span.remaining()));
                let count_field = format_ident!("{id}_count");
                let array_count = if element_width.index != 1 {
                    self.code.push(quote! {
                        if #array_size % #element_width != 0 {
                            return Err(InvalidArraySizeError {
                                array: #array_size,
                                element: #element_width,
                            });
                        }
                        let #count_field = #array_size / #element_width;
                    });
                    quote!(#count_field)
                } else {
                    array_size
                };

                self.code.push(quote! {
                    let __case_7__ = "777";
                    let #id = (0..#array_count).map(|_| #parse_element).collect();
                });
            }
            _ => todo!(),
        }
    }

    /// Parse a single array field element from `span`.
    fn parse_array_element(
        &self,
        span: &proc_macro2::Ident,
        width: Option<usize>,
        type_id: Option<&str>,
        decl: Option<&ast::Decl>,
    ) -> proc_macro2::TokenStream {
        if let Some(width) = width {
            return types::get_uint(self.endianness, width, span);
        }

        if let Some(ast::Decl::Enum { id, width, .. }) = decl {
            let element_type = types::Integer::new(*width);
            let get_uint = types::get_uint(self.endianness, *width, span);
            let type_id = format_ident!("{id}");
            let from_u = format_ident!("from_u{}", element_type.width);
            return quote! {
                #type_id::#from_u(#get_uint).unwrap()
            };
        }

        let type_id = format_ident!("{}", type_id.unwrap());
        quote! {
            #type_id.parse(#span)
        }
    }

    pub fn done(&mut self) {}
}

impl quote::ToTokens for FieldParser<'_> {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let code = &self.code;
        tokens.extend(quote! {
            #(#code)*
        });
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ast;
    use crate::parser::parse_inline;

    /// Parse a string fragment as a PDL file.
    ///
    /// # Panics
    ///
    /// Panics on parse errors.
    pub fn parse_str(text: &str) -> ast::File {
        let mut db = ast::SourceDatabase::new();
        parse_inline(&mut db, String::from("stdin"), String::from(text)).expect("parse error")
    }

    #[test]
    fn test_find_fields_static() {
        let code = "
              little_endian_packets
              packet P {
                a: 24[3],
              }
            ";
        let file = parse_str(code);
        let scope = lint::Scope::new(&file).unwrap();
        let span = format_ident!("bytes");
        let parser = FieldParser::new(&scope, file.endianness.value, "P", &span);
        assert_eq!(parser.find_size_field("a"), None);
        assert_eq!(parser.find_count_field("a"), None);
    }

    #[test]
    fn test_find_fields_dynamic_count() {
        let code = "
              little_endian_packets
              packet P {
                _count_(b): 24,
                b: 16[],
              }
            ";
        let file = parse_str(code);
        let scope = lint::Scope::new(&file).unwrap();
        let span = format_ident!("bytes");
        let parser = FieldParser::new(&scope, file.endianness.value, "P", &span);
        assert_eq!(parser.find_size_field("b"), None);
        assert_eq!(parser.find_count_field("b"), Some(format_ident!("b_count")));
    }

    #[test]
    fn test_find_fields_dynamic_size() {
        let code = "
              little_endian_packets
              packet P {
                _size_(c): 8,
                c: 24[],
              }
            ";
        let file = parse_str(code);
        let scope = lint::Scope::new(&file).unwrap();
        let span = format_ident!("bytes");
        let parser = FieldParser::new(&scope, file.endianness.value, "P", &span);
        assert_eq!(parser.find_size_field("c"), Some(format_ident!("c_size")));
        assert_eq!(parser.find_count_field("c"), None);
    }
}
