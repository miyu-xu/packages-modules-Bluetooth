use quote::{format_ident, quote};

use crate::backends::rust::mask_bits;
use crate::backends::rust::types;
use crate::{ast, lint};

/// Like [`ast::Field::Scalar`].
#[derive(Debug, Clone)]
pub struct ScalarField {
    pub id: String,
    pub width: usize,
}

impl ScalarField {
    fn new(id: &str, width: usize) -> ScalarField {
        ScalarField { id: String::from(id), width }
    }

    fn get_ident(&self) -> proc_macro2::Ident {
        format_ident!("{}", self.id)
    }

    fn get_type(&self) -> types::Integer {
        types::Integer::new(self.width)
    }

    fn generate_decl(&self, visibility: syn::Visibility) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let field_type = self.get_type();
        quote! {
            #visibility #field_name: #field_type
        }
    }

    fn generate_getter(&self, packet_name: &syn::Ident) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let getter_name = format_ident!("get_{}", self.id);
        let field_type = self.get_type();
        quote! {
            pub fn #getter_name(&self) -> #field_type {
                self.#packet_name.as_ref().#field_name
            }
        }
    }

    fn generate_read_adjustment(
        &self,
        offset: usize,
        chunk_type: types::Integer,
    ) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let field_type = self.get_type();
        let mut field = quote! {
            chunk
        };
        if offset > 0 {
            let offset = syn::Index::from(offset);
            field = quote! {
                (#field >> #offset)
            };
        }

        if self.width < field_type.width {
            let bit_mask = mask_bits(self.width);
            field = quote! {
                (#field & #bit_mask)
            };
        }

        if field_type.width < chunk_type.width {
            field = quote! {
                #field as #field_type;
            };
        }

        quote! {
            let #field_name = #field;
        }
    }

    fn generate_write_adjustment(
        &self,
        offset: usize,
        chunk_type: types::Integer,
    ) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let field_type = self.get_type();

        let mut field = quote! {
            self.#field_name
        };

        if field_type.width < chunk_type.width {
            field = quote! {
                (#field as #chunk_type)
            };
        }

        if self.width < field_type.width {
            let bit_mask = mask_bits(self.width);
            field = quote! {
                (#field & #bit_mask)
            };
        }

        if offset > 0 {
            let field_offset = syn::Index::from(offset);
            field = quote! {
                (#field << #field_offset)
            };
        }

        quote! {
            let chunk = chunk | #field;
        }
    }
}

/// Like [`ast::Field::Typedef`].
#[derive(Debug, Clone)]
pub struct EnumField {
    pub id: String,
    pub enum_id: String,
    pub width: usize,
}

impl EnumField {
    fn new(id: &str, enum_id: &str, width: usize) -> EnumField {
        EnumField { id: String::from(id), enum_id: String::from(enum_id), width }
    }

    fn get_ident(&self) -> proc_macro2::Ident {
        format_ident!("{}", self.id)
    }

    fn get_type(&self) -> types::Enum<'_> {
        types::Enum::new(&self.enum_id, self.width)
    }

    fn generate_decl(&self, visibility: syn::Visibility) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let field_type = self.get_type();
        quote! {
            #visibility #field_name: #field_type
        }
    }

    fn generate_getter(&self, packet_name: &syn::Ident) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let getter_name = format_ident!("get_{}", self.id);
        let field_type = self.get_type();
        quote! {
            pub fn #getter_name(&self) -> #field_type {
                self.#packet_name.as_ref().#field_name
            }
        }
    }

    fn generate_read_adjustment(
        &self,
        offset: usize,
        chunk_type: types::Integer,
    ) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let field_type = self.get_type();
        let mut field = quote! {
            chunk
        };
        if offset > 0 {
            let offset = syn::Index::from(offset);
            field = quote! {
                (#field >> #offset)
            };
        }

        if self.width < field_type.width {
            let bit_mask = mask_bits(self.width);
            field = quote! {
                (#field & #bit_mask)
            };
        }

        let from = format_ident!("from_u{}", chunk_type.width);
        quote! {
            let #field_name = #field_type::#from(#field).unwrap();
        }
    }

    fn generate_write_adjustment(
        &self,
        offset: usize,
        chunk_type: types::Integer,
    ) -> proc_macro2::TokenStream {
        let field_name = self.get_ident();
        let field_type = self.get_type();

        let to = format_ident!("to_u{}", chunk_type.width);
        let mut field = quote! {
            self.#field_name.#to().unwrap()
        };

        if self.width < field_type.width {
            let bit_mask = mask_bits(self.width);
            field = quote! {
                (#field & #bit_mask)
            };
        }

        if offset > 0 {
            let field_offset = syn::Index::from(offset);
            field = quote! {
                (#field << #field_offset)
            };
        }

        quote! {
            let chunk = chunk | #field;
        }
    }
}

/// Projection of [`ast::Field`] with the bits needed for the Rust
/// backend.
#[derive(Debug, Clone)]
pub enum Field {
    Scalar(ScalarField),
    Enum(EnumField),
}

impl Field {
    pub fn from_ast(scope: &lint::Scope<'_>, field: &ast::Field) -> Field {
        match field {
            ast::Field::Scalar { id, width, .. } => Field::Scalar(ScalarField::new(id, *width)),
            ast::Field::Typedef { id, type_id, .. } => {
                let enum_field = scope
                    .typedef
                    .get(type_id.as_str())
                    .map(|f| {
                        if let ast::Decl::Enum { id: enum_id, width, .. } = f {
                            EnumField::new(id.as_str(), enum_id.as_str(), *width)
                        } else {
                            panic!("Expected ast::Decl::Enum, found {f:?}");
                        }
                    })
                    .unwrap_or_else(|| panic!("Missing enum declaration: {type_id}"));
                Field::Enum(enum_field)
            }
            _ => todo!("Unsupported field: {:?}", field),
        }
    }

    pub fn get_width(&self) -> usize {
        match self {
            Field::Scalar(field) => field.width,
            Field::Enum(field) => field.width,
        }
    }

    pub fn get_ident(&self) -> proc_macro2::Ident {
        match self {
            Field::Scalar(field) => field.get_ident(),
            Field::Enum(field) => field.get_ident(),
        }
    }

    pub fn generate_decl(&self, visibility: syn::Visibility) -> proc_macro2::TokenStream {
        match self {
            Field::Scalar(field) => field.generate_decl(visibility),
            Field::Enum(field) => field.generate_decl(visibility),
        }
    }

    pub fn generate_getter(&self, packet_name: &syn::Ident) -> proc_macro2::TokenStream {
        match self {
            Field::Scalar(field) => field.generate_getter(packet_name),
            Field::Enum(field) => field.generate_getter(packet_name),
        }
    }

    pub fn generate_read_adjustment(
        &self,
        offset: usize,
        chunk_type: types::Integer,
    ) -> proc_macro2::TokenStream {
        match self {
            Field::Scalar(field) => field.generate_read_adjustment(offset, chunk_type),
            Field::Enum(field) => field.generate_read_adjustment(offset, chunk_type),
        }
    }

    pub fn generate_write_adjustment(
        &self,
        offset: usize,
        chunk_type: types::Integer,
    ) -> proc_macro2::TokenStream {
        match self {
            Field::Scalar(field) => field.generate_write_adjustment(offset, chunk_type),
            Field::Enum(field) => field.generate_write_adjustment(offset, chunk_type),
        }
    }
}
