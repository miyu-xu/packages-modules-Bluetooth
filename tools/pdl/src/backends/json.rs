use crate::ast;

/// Turn the AST into a JSON representation.
pub fn generate(grammar: &ast::Grammar) -> Result<String, String> {
    serde_json::to_string_pretty(&grammar)
        .map_err(|err| format!("could not JSON serialize grammar: {err}"))
}
