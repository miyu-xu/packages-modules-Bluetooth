use crate::ast;
use quote::{format_ident, quote};
use std::collections::HashMap;
use std::path::Path;
use thiserror::Error;

#[derive(Error, Debug)]
pub enum Error {
    // TODO(mgeisler): create enum variants for unsupported fields,
    // decls, etc.
    #[error("unsupported construct: {0}")]
    Unsupported(String),

    #[error(transparent)]
    IO(#[from] std::io::Error),

    #[error(transparent)]
    Syn(#[from] syn::Error),
}

/// Generate a block of code.
///
/// Like `quote!`, but the code block will be followed by an empty
/// line of code. This makes the generated code more readable.
macro_rules! quote_block {
    ($($tt:tt)*) => {
        format!("{}\n\n", quote!($($tt)*))
    }
}

/// Generate the file preamble.
fn generate_preamble(path: &Path) -> Result<String, Error> {
    let mut code = String::new();
    let filename = path
        .file_name()
        .and_then(|path| path.to_str())
        .ok_or_else(|| Error::Unsupported(format!("could not find filename in {:?}", path)))?;
    code.push_str(&format!("// @generated rust packets from {filename}\n\n"));

    code.push_str(&quote_block! {
        use bytes::{BufMut, Bytes, BytesMut};
        use num_derive::{FromPrimitive, ToPrimitive};
        use num_traits::{FromPrimitive, ToPrimitive};
        use std::convert::{TryFrom, TryInto};
        use std::fmt;
        use std::sync::Arc;
        use thiserror::Error;
    });

    code.push_str(&quote_block! {
        type Result<T> = std::result::Result<T, Error>;
    });

    code.push_str(&quote_block! {
        #[derive(Debug, Error)]
        pub enum Error {
            #[error("Packet parsing failed")]
            InvalidPacketError,
            #[error("{field} was {value:x}, which is not known")]
            ConstraintOutOfBounds { field: String, value: u64 },
            #[error("when parsing {obj}.{field} needed length of {wanted} but got {got}")]
            InvalidLengthError {
                obj:     String,
                field:   String,
                wanted:  usize,
                got:     usize,
            },
            #[error("Due to size restrictions a struct could not be parsed.")]
            ImpossibleStructError,
            #[error("when parsing field {obj}.{field}, {value} is not a valid {type_} value")]
            InvalidEnumValueError {
                obj: String,
                field: String,
                value: u64,
                type_: String,
            },
        }
    });

    code.push_str(&quote_block! {
        #[derive(Debug, Error)]
        #[error("{0}")]
        pub struct TryFromError(&'static str);
    });

    code.push_str(&quote_block! {
        pub trait Packet {
            fn to_bytes(self) -> Bytes;
            fn to_vec(self) -> Vec<u8>;
        }
    });

    Ok(code)
}

fn generate_field(
    field: &ast::Field,
    visibility: syn::Visibility,
) -> Result<proc_macro2::TokenStream, Error> {
    match field {
        ast::Field::Scalar { id, width, .. } => {
            let field_name = format_ident!("{id}");
            let field_type: syn::Type = syn::parse_str(&format!("u{width}"))?;
            Ok(quote! {
                #visibility #field_name: #field_type
            })
        }
        _ => Err(Error::Unsupported(format!("Field: {:?}", field))),
    }
}

fn generate_field_getter(
    packet_name: &syn::Ident,
    field: &ast::Field,
) -> Result<proc_macro2::TokenStream, Error> {
    match field {
        ast::Field::Scalar { id, width, .. } => {
            // TODO(mgeisler): refactor with generate_field above.
            let getter_name = format_ident!("get_{id}");
            let field_name = format_ident!("{id}");
            let field_type: syn::Type = syn::parse_str(&format!("u{width}"))?;
            Ok(quote! {
                pub fn #getter_name(&self) -> #field_type {
                    self.#packet_name.as_ref().#field_name
                }
            })
        }
        _ => Err(Error::Unsupported(format!("Field: {:?}", field))),
    }
}

fn round_bit_width(width: usize) -> Result<usize, Error> {
    match width {
        8 => Ok(8),
        16 => Ok(16),
        24 | 32 => Ok(32),
        40 | 48 | 56 | 64 => Ok(64),
        _ => Err(Error::Unsupported(format!("Unsupported width: {width}"))),
    }
}

fn generate_field_parser(
    grammar: &ast::Grammar,
    packet_name: &str,
    field: &ast::Field,
    offset: usize,
) -> Result<proc_macro2::TokenStream, Error> {
    match field {
        ast::Field::Scalar { id, width, .. } => {
            let field_name = format_ident!("{id}");
            let type_width = round_bit_width(*width)?;
            let field_type: syn::Type = syn::parse_str(&format!("u{type_width}"))?;

            let getter = match grammar.endianness.value {
                ast::EndiannessValue::BigEndian => format_ident!("from_be_bytes"),
                ast::EndiannessValue::LittleEndian => format_ident!("from_le_bytes"),
            };

            let wanted_len = syn::Index::from(offset + width / 8);
            let indices = (offset..offset + width / 8).map(syn::Index::from);
            let padding = vec![syn::Index::from(0); (type_width - width) / 8];
            let mask = if *width != type_width {
                quote! {
                    let #field_name = #field_name & 0xfff;
                }
            } else {
                quote! {}
            };

            Ok(quote! {
                // TODO(mgeisler): call a function instead to avoid
                // generating so much code for this.
                if bytes.len() < #wanted_len {
                    return Err(Error::InvalidLengthError {
                        obj: #packet_name.to_string(),
                        field: #id.to_string(),
                        wanted: #wanted_len,
                        got: bytes.len(),
                    });
                }
                let #field_name = #field_type::#getter([#(bytes[#indices]),* #(, #padding)*]);
                #mask
            })
        }
        _ => todo!("unsupported field: {:?}", field),
    }
}

fn generate_field_writer(
    grammar: &ast::Grammar,
    field: &ast::Field,
    offset: usize,
) -> Result<proc_macro2::TokenStream, Error> {
    match field {
        ast::Field::Scalar { id, width, .. } => {
            let field_name = format_ident!("{id}");
            let bit_width = round_bit_width(*width)?;
            let start = syn::Index::from(offset);
            let end = syn::Index::from(offset + bit_width / 8);
            let byte_width = syn::Index::from(bit_width / 8);
            let writer = match grammar.endianness.value {
                ast::EndiannessValue::BigEndian => format_ident!("to_be_bytes"),
                ast::EndiannessValue::LittleEndian => format_ident!("to_le_bytes"),
            };
            Ok(quote! {
                let #field_name = self.#field_name;
                buffer[#start..#end].copy_from_slice(&#field_name.#writer()[0..#byte_width]);
            })
        }
        _ => Err(Error::Unsupported(format!("Field: {:?}", field))),
    }
}

fn get_field_size(field: &ast::Field) -> Result<usize, Error> {
    match field {
        ast::Field::Scalar { width, .. } => Ok(width / 8),
        _ => Err(Error::Unsupported(format!("Field: {:?}", field))),
    }
}

fn generate_decl(
    grammar: &ast::Grammar,
    packets: &HashMap<&str, &ast::Decl>,
    child_decls: &HashMap<&str, Vec<&ast::Decl>>,
    decl: &ast::Decl,
) -> Result<String, Error> {
    match decl {
        ast::Decl::Packet { id, fields, parent_id, .. } => {
            let mut code = String::new();

            // TODO(mgeisler): use convert_case crate instead.
            let ident = format_ident!("{}", id.to_lowercase());
            let data_name = format_ident!("{id}Data");
            let packet_name = format_ident!("{id}Packet");
            let child_name = format_ident!("{id}Child");
            let data_child_name = format_ident!("{id}DataChild");
            let builder_name = format_ident!("{id}Builder");
            let plain_fields = fields
                .iter()
                .map(|field| generate_field(field, syn::Visibility::Inherited))
                .collect::<Result<Vec<_>, _>>()?;
            let pub_token = syn::parse_str("pub")?;
            let pub_fields = fields
                .iter()
                .map(|field| generate_field(field, syn::VisPublic { pub_token }.into()))
                .collect::<Result<Vec<_>, _>>()?;

            let child_decl_name = match child_decls.get(id.as_str()) {
                Some(children) => children
                    .iter()
                    .map(|child_decl| match child_decl {
                        ast::Decl::Packet { id, .. } => Ok(format_ident!("{id}")),
                        _ => Err(Error::Unsupported(format!(
                            "Expected Decl::Packet, found {:?}",
                            child_decl
                        ))),
                    })
                    .collect::<Result<Vec<_>, _>>()?,
                None => vec![],
            };

            let child_decl_data_name = child_decl_name
                .iter()
                .map(|child_decl_name| format_ident!("{child_decl_name}Data"))
                .collect::<Vec<_>>();
            let child_decl_packet_name = child_decl_name
                .iter()
                .map(|child_decl_name| format_ident!("{child_decl_name}Packet"))
                .collect::<Vec<_>>();
            let child_field = if child_decl_name.is_empty() {
                quote! {}
            } else {
                quote! {
                    child: #data_child_name,
                }
            };

            if !child_decl_name.is_empty() {
                code.push_str(&quote_block! {
                    #[derive(Debug)]
                    enum #data_child_name {
                        #(#child_decl_name(Arc<#child_decl_data_name>),)*
                        None,
                    }

                    impl #data_child_name {
                        fn get_total_size(&self) -> usize {
                            // TODO(mgeisler): use Self instad of #data_child_name.
                            match self {
                                #(#data_child_name::#child_decl_name(value) => value.get_total_size(),)*
                                #data_child_name::None => 0,
                            }
                        }
                    }

                    #[derive(Debug)]
                    pub enum #child_name {
                        #(#child_decl_name(#child_decl_packet_name),)*
                        None,
                    }
                });
            }

            code.push_str(&quote_block! {
                #[derive(Debug)]
                struct #data_name {
                    #(#plain_fields,)*
                    #child_field
                }
            });

            let parent =
                parent_id.as_ref().map(|parent_id| match packets.get(parent_id.as_str()) {
                    Some(ast::Decl::Packet { id, .. }) => {
                        let parent_ident = format_ident!("{}", id.to_lowercase());
                        let parent_data = format_ident!("{id}Data");
                        quote! {
                            #parent_ident: Arc<#parent_data>,
                        }
                    }
                    _ => todo!("Could not find {parent_id}"),
                });

            code.push_str(&quote_block! {
                #[derive(Debug, Clone)]
                pub struct #packet_name {
                    #parent
                    #ident: Arc<#data_name>,
                }
            });

            code.push_str(&quote_block! {
                #[derive(Debug)]
                pub struct #builder_name {
                    #(#pub_fields,)*
                }
            });

            // TODO(mgeisler): use the `Buf` trait instead of tracking
            // the offset manually.
            let mut offset = 0;
            let field_parsers = fields
                .iter()
                .map(|field| {
                    let parser = generate_field_parser(grammar, id, field, offset);
                    offset += get_field_size(field)?;
                    parser
                })
                .collect::<Result<Vec<_>, _>>()?;
            let field_names = fields
                .iter()
                .map(|field| match field {
                    ast::Field::Scalar { id, .. } => format_ident!("{id}"),
                    _ => todo!("Field: {:?}", field),
                })
                .collect::<Vec<_>>();
            let mut offset = 0;
            let field_writers = fields
                .iter()
                .map(|field| {
                    let writer = generate_field_writer(grammar, field, offset);
                    offset += get_field_size(field)?;
                    writer
                })
                .collect::<Result<Vec<_>, _>>()?;
            let total_field_size = syn::Index::from(
                fields.iter().try_fold::<_, _, Result<usize, Error>>(0, |acc, field| {
                    Ok(acc + get_field_size(field)?)
                })?,
            );

            code.push_str(&quote_block! {
                impl #data_name {
                    fn conforms(bytes: &[u8]) -> bool {
                        true
                    }

                    fn parse(bytes: &[u8]) -> Result<Self> {
                        #(#field_parsers)*
                        Ok(Self { #(#field_names),* })
                    }

                    fn write_to(&self, buffer: &mut BytesMut) {
                        #(#field_writers)*
                    }

                    fn get_total_size(&self) -> usize {
                        self.get_size()
                    }

                    fn get_size(&self) -> usize {
                        let ret = 0;
                        let ret = ret + #total_field_size;
                        ret
                    }
                }
            });

            code.push_str(&quote_block! {
                impl Packet for #packet_name {
                    fn to_bytes(self) -> Bytes {
                        let mut buffer = BytesMut::new();
                        buffer.resize(self.#ident.get_total_size(), 0);
                        self.#ident.write_to(&mut buffer);
                        buffer.freeze()
                    }
                    fn to_vec(self) -> Vec<u8> {
                        self.to_bytes().to_vec()
                    }
                }
                impl From<#packet_name> for Bytes {
                    fn from(packet: #packet_name) -> Self {
                        packet.to_bytes()
                    }
                }
                impl From<#packet_name> for Vec<u8> {
                    fn from(packet: #packet_name) -> Self {
                        packet.to_vec()
                    }
                }
            });

            let field_getters = fields
                .iter()
                .map(|field| generate_field_getter(&ident, field))
                .collect::<Result<Vec<_>, _>>()?;

            let specialize = if !child_decl_name.is_empty() {
                quote! {
                    pub fn specialize(&self) -> #child_name {
                        match &self.#ident.child {
                            #(#data_child_name::#child_decl_name(_) =>
                              #child_name::#child_decl_name(
                                  #child_decl_packet_name::new(self.#ident.clone()).unwrap()),)*
                            #data_child_name::None => #child_name::None,
                        }
                    }
                }
            } else {
                quote! {}
            };

            code.push_str(&quote_block! {
                impl #packet_name {
                    pub fn parse(bytes: &[u8]) -> Result<Self> {
                        Ok(Self::new(Arc::new(#data_name::parse(bytes)?)).unwrap())
                    }

                    #specialize

                    fn new(root: Arc<#data_name>) -> std::result::Result<Self, &'static str> {
                        let #ident = root;
                        Ok(Self { #ident })
                    }

                    #(#field_getters)*
                }
            });

            let child = if child_decl_name.is_empty() {
                quote! {}
            } else {
                quote! {
                    child: #data_child_name::None,
                }
            };

            code.push_str(&quote_block! {
                impl #builder_name {
                    pub fn build(self) -> #packet_name {
                        let #ident = Arc::new(#data_name {
                            #(#field_names: self.#field_names,)*
                            #child
                        });
                        #packet_name::new(#ident).unwrap()
                    }
                }
            });

            Ok(code)
        }
        _ => Err(Error::Unsupported(format!("Decl::{:?}", decl))),
    }
}

/// Generate Rust code from `grammar`.
///
/// The code is not formatted, pipe it through `rustfmt` to get
/// readable source code.
pub fn generate_rust(
    sources: &ast::SourceDatabase,
    grammar: &ast::Grammar,
) -> Result<String, Error> {
    let source = sources.get(grammar.file).map_err(|err| {
        Error::Unsupported(format!("could not read {} from sources: {}", grammar.file, err))
    })?;

    let mut child_decls = HashMap::new();
    let mut packets = HashMap::new();
    for decl in &grammar.declarations {
        if let ast::Decl::Packet { id, parent_id, .. } = decl {
            packets.insert(id.as_str(), decl);
            if let Some(parent_id) = parent_id {
                let children = child_decls.entry(parent_id.as_str()).or_insert_with(Vec::new);
                children.push(decl);
            }
        }
    }

    let mut code = String::new();

    code.push_str(&generate_preamble(Path::new(source.name()))?);

    for decl in &grammar.declarations {
        let decl_code = generate_decl(grammar, &packets, &child_decls, decl).map_err(|err| {
            Error::Unsupported(format!("error generating code for decl: {}", err))
        })?;
        code.push_str(&decl_code);
        code.push_str("\n\n");
    }

    Ok(code)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::parser::parse_inline;
    use std::io::Write;
    use std::process::Command;
    use std::process::Stdio;

    fn parse(text: &str) -> ast::Grammar {
        let mut db = ast::SourceDatabase::new();
        parse_inline(&mut db, String::from("stdin"), String::from(text)).expect("parsing failure")
    }

    fn format_with_rustfmt(unformatted: &str) -> Result<String, String> {
        let mut rustfmt = Command::new("rustfmt")
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .spawn()
            .map_err(|err| format!("failed to start rustfmt: {}", err))?;

        let mut stdin =
            rustfmt.stdin.take().ok_or_else(|| String::from("rustfmt stdin was None"))?;
        let unformatted = String::from(unformatted);
        std::thread::spawn(move || {
            stdin.write_all(unformatted.as_bytes()).unwrap();
        });

        let output = rustfmt
            .wait_with_output()
            .map_err(|err| format!("error executing rustfmt: {}", err))?;
        if !output.status.success() {
            return Err(format!("rustfmt failed: {}", output.status));
        }
        String::from_utf8(output.stdout)
            .map_err(|err| format!("rustfmt output was not UTF-8: {}", err))
    }

    #[test]
    fn test_generate_preamble() -> Result<(), String> {
        let actual_code =
            generate_preamble(Path::new("some/path/foo.pdl")).map_err(|err| format!("{}", err))?;
        let expected_code = include_str!("../test/generated/preamble.rs");
        assert_eq!(format_with_rustfmt(&actual_code)?, expected_code);
        Ok(())
    }

    #[test]
    fn test_generate_packet_decl() -> Result<(), String> {
        let grammar = parse(
            r#"
              big_endian_packets

              packet Foo {
                x: 8,
                y: 16,
              }
            "#,
        );
        let packets = HashMap::new();
        let child_decls = HashMap::new();
        let decl = &grammar.declarations[0];
        let actual_code = generate_decl(&grammar, &packets, &child_decls, decl)
            .map_err(|err| format!("{}", err))?;
        let expected_code = include_str!("../test/generated/packet_decl.rs");
        assert_eq!(format_with_rustfmt(&actual_code)?, expected_code);
        Ok(())
    }
}
