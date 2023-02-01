use crate::backends::rust::types;
use crate::{ast, lint};
use quote::{format_ident, quote};

pub struct FieldDeclarations<'a> {
    scope: &'a lint::Scope<'a>,
    packet_name: &'a str,
    code: Vec<proc_macro2::TokenStream>,
}

impl<'a> FieldDeclarations<'a> {
    pub fn new(scope: &'a lint::Scope<'a>, packet_name: &'a str) -> FieldDeclarations<'a> {
        FieldDeclarations { scope, packet_name, code: Vec::new() }
    }

    pub fn add(&mut self, field: &ast::Field) {
        let id = match field.id() {
            Some(id) => format_ident!("{id}"),
            None => return, // No id => field not stored.
        };

        let field_type = types::rust_type(field);
        self.code.push(quote! {
            #id: #field_type,
        })
    }

    pub fn done(&mut self) {
        let packet_data_child = format_ident!("{}DataChild", self.packet_name);
        if self.scope.children.contains_key(self.packet_name) {
            self.code.push(quote! {
                child: #packet_data_child,
            });
        }
    }
}

impl quote::ToTokens for FieldDeclarations<'_> {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let code = &self.code;
        tokens.extend(quote! {
            #(#code)*
        });
    }
}
