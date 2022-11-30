//! Utility functions for dealing with Rust integer types.

use quote::format_ident;

/// A Rust integer type such as `u8`.
#[derive(Copy, Clone)]
pub struct Integer {
    pub width: usize,
}

impl Integer {
    /// Get the Rust integer type for the given bit width.
    ///
    /// This will round up the size to the nearest Rust integer size.
    /// PDL supports integers up to 64 bit, so it is an error to call
    /// this with a width larger than 64.
    pub fn new(width: usize) -> Integer {
        for integer_width in [8, 16, 32, 64] {
            if width <= integer_width {
                return Integer { width: integer_width };
            }
        }
        panic!("Cannot construct Integer with width: {width}")
    }
}

impl quote::ToTokens for Integer {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let t: syn::Type = syn::parse_str(&format!("u{}", self.width))
            .expect("Could not parse integer, unsupported width?");
        t.to_tokens(tokens);
    }
}

/// A Rust enum type.
#[derive(Copy, Clone)]
pub struct Enum<'a> {
    pub name: &'a str,
    pub width: usize,
}

impl Enum<'_> {
    /// Get the Rust enum type for the given bit width.
    ///
    /// This will round up the size to the nearest Rust integer size.
    /// PDL supports integers up to 64 bit, so it is an error to call
    /// this with a width larger than 64.
    pub fn new(name: &str, width: usize) -> Enum<'_> {
        for integer_width in [8, 16, 32, 64] {
            if width <= integer_width {
                return Enum { name, width: integer_width };
            }
        }
        panic!("Cannot construct Enum {name} with width: {width}")
    }
}

impl quote::ToTokens for Enum<'_> {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        let t = format_ident!("{}", self.name);
        t.to_tokens(tokens);
    }
}

/// A Rust array type.
#[derive(Copy, Clone)]
pub struct Array<'a> {
    pub name: &'a str,
    pub width: usize,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_integer_new() {
        assert_eq!(Integer::new(0).width, 8);
        assert_eq!(Integer::new(8).width, 8);
        assert_eq!(Integer::new(9).width, 16);
        assert_eq!(Integer::new(64).width, 64);
    }

    #[test]
    #[should_panic]
    fn test_integer_new_panics_on_large_width() {
        Integer::new(65);
    }
}
