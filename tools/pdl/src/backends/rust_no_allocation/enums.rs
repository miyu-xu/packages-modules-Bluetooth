use quote::format_ident;

use crate::{ast, quote_block};

pub fn generate_enum(
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
