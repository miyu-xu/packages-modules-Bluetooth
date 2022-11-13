//! PDL parser and linter.

use clap::Parser;
use codespan_reporting::term::{self, termcolor};

mod ast;
mod backends;
mod lint;
mod parser;
#[cfg(test)]
mod test_utils;

use crate::lint::Lintable;

#[derive(Debug)]
enum OutputFormat {
    JSON,
    Rust,
}

impl std::str::FromStr for OutputFormat {
    type Err = String;

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        match input.to_lowercase().as_str() {
            "json" => Ok(Self::JSON),
            "rust" => Ok(Self::Rust),
            _ => Err(format!("could not parse {:?}, valid option are 'json' and 'rust'.", input)),
        }
    }
}

#[derive(Parser, Debug)]
#[clap(name = "pdl-parser", about = "Packet Description Language parser tool.")]
struct Opt {
    /// Print tool version and exit.
    #[clap(short, long = "--version")]
    version: bool,

    /// Generate output in this format ("json" or "rust"). The output
    /// will be printed on stdout in both cases.
    #[clap(short, long = "--output-format", name = "FORMAT", default_value = "JSON")]
    output_format: OutputFormat,

    /// Input file.
    #[clap(name = "FILE")]
    input_file: String,
}

fn expand_groups_in_fields(file: &ast::File, fields: &[ast::Field]) -> Vec<ast::Field> {
    fields.iter().flat_map(|field| match field {
        ast::Field::Group { loc, group_id, constraints } => {
            let group_fields = file.declarations.iter().find_map(|decl| {
                if let ast::Decl::Group { id, fields, .. } = decl {
                    if id == group_id {
                        return Some(fields)
                    }
                }
                None
            }).expect("Already linted");

            let middle = group_fields.iter().flat_map(|field| match field {
                ast::Field::Scalar { loc, id, width } => {
                    let constraint = constraints.iter().find(|c| c.id == *id);

                    vec![if let Some(constraint) = constraint {
                        ast::Field::Fixed {
                            loc: *loc,
                            width: Some(*width),
                            value: constraint.value,
                            enum_id: None,
                            tag_id: None,
                        }
                    } else {
                        field.clone()
                    }]
                },
                ast::Field::Typedef { loc, id, type_id } => {
                    let constraint = constraints.iter().find(|c| c.id == *id);

                    vec![if let Some(constraint) = constraint {
                        ast::Field::Fixed {
                            loc: *loc,
                            width: None,
                            value: None,
                            enum_id: Some(type_id.clone()),
                            tag_id: constraint.tag_id.clone(),
                        }
                    } else {
                        field.clone()
                    }]
                },
                f => expand_groups_in_fields(file, &[f.clone()])
            });

            let start = std::iter::once(ast::Field::GroupStart {
                group_id: group_id.clone(),
                loc: *loc,
                constraints: constraints.clone(),
            });

            let end = std::iter::once(ast::Field::GroupEnd {
                group_id: group_id.clone(),
                loc: *loc,
            });

            start.chain(middle).chain(end).collect()
        },
        v => vec![v.clone()],
    }).collect()
}

fn expand_fields(file: &ast::File, fields: &[ast::Field]) -> Vec<ast::Field> {
    let mut fields = expand_groups_in_fields(file, fields);

    let mut peekable = fields.iter_mut().peekable();

    while let Some(field) = peekable.next() {
        if let (ast::Field::Array { ref mut padded_size, .. }, Some(ast::Field::Padding { size, .. })) = (field, peekable.peek()) {
            *padded_size = Some(*size);
        }
    }

    fields
}

fn expand(file: ast::File) -> ast::File {
    let declarations = file.declarations.iter().map(|declaration| match declaration {
            ast::Decl::Packet { id, loc, constraints, fields, parent_id } => ast::Decl::Packet {
                id: id.clone(),
                loc: *loc,
                constraints: constraints.clone(),
                fields: expand_fields(&file, fields),
                parent_id: parent_id.clone(),
            },
            ast::Decl::Struct { id, loc, constraints, fields, parent_id } => ast::Decl::Struct {
                id: id.clone(),
                loc: *loc,
                constraints: constraints.clone(),
                fields: expand_fields(&file, fields),
                parent_id: parent_id.clone(),
            },
            v => v.clone(),
    }).collect();

    ast::File {
        declarations,
        ..file
    }
}

fn main() -> std::process::ExitCode {
    let opt = Opt::from_args();

    if opt.version {
        println!("Packet Description Language parser version 1.0");
        return std::process::ExitCode::SUCCESS;
    }

    let mut sources = ast::SourceDatabase::new();
    match parser::parse_file(&mut sources, opt.input_file) {
        Ok(file) => {
            let lint = file.lint();
            if !lint.diagnostics.is_empty() {
                lint.print(&sources, termcolor::ColorChoice::Always)
                    .expect("Could not print lint diagnostics");
                return std::process::ExitCode::FAILURE;
            }

            let file = expand(file);

            match opt.output_format {
                OutputFormat::JSON => {
                    println!("{}", backends::json::generate(&file).unwrap())
                }
                OutputFormat::Rust => {
                    println!("{}", backends::rust::generate(&sources, &file))
                }
            }
            std::process::ExitCode::SUCCESS
        }
        Err(err) => {
            let writer = termcolor::StandardStream::stderr(termcolor::ColorChoice::Always);
            let config = term::Config::default();
            term::emit(&mut writer.lock(), &config, &sources, &err).expect("Could not print error");
            std::process::ExitCode::FAILURE
        }
    }
}
