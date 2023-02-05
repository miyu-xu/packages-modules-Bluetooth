use codespan_reporting::diagnostic::Diagnostic;
use codespan_reporting::files;
use codespan_reporting::term;
use codespan_reporting::term::termcolor;
use std::collections::HashMap;

use crate::ast::*;
use crate::parser::ast as parser_ast;

pub mod ast {
    use serde::Serialize;

    /// Field and declaration size information.
    #[derive(Default, Debug, Clone, Copy)]
    #[allow(unused)]
    pub enum Size {
        /// Constant size in bits.
        Static(usize),
        /// Size indicated at packet parsing by a size or count field.
        /// The parameter is the static part of the size.
        Dynamic,
        /// The size cannot be determined statically or at runtime.
        /// The packet assumes the largest possible size.
        #[default]
        Unknown,
    }

    #[derive(Debug, Serialize)]
    pub struct Annotation();

    #[derive(Default, Debug, Clone)]
    pub struct FieldAnnotation {
        // Size of field.
        pub size: Size,
    }

    #[derive(Default, Debug, Clone)]
    pub struct DeclAnnotation {
        // Size computed excluding the payload.
        pub size: Size,
        // Payload size, or Static(0) if the declaration does not
        // have a payload.
        pub payload_size: Size,
    }

    impl std::ops::Add for Size {
        type Output = Size;
        fn add(self, rhs: Size) -> Self::Output {
            match (self, rhs) {
                (Size::Unknown, _) | (_, Size::Unknown) => Size::Unknown,
                (Size::Dynamic, _) | (_, Size::Dynamic) => Size::Dynamic,
                (Size::Static(lhs), Size::Static(rhs)) => Size::Static(lhs + rhs),
            }
        }
    }

    impl std::ops::Mul for Size {
        type Output = Size;
        fn mul(self, rhs: Size) -> Self::Output {
            match (self, rhs) {
                (Size::Unknown, _) | (_, Size::Unknown) => Size::Unknown,
                (Size::Dynamic, _) | (_, Size::Dynamic) => Size::Dynamic,
                (Size::Static(lhs), Size::Static(rhs)) => Size::Static(lhs * rhs),
            }
        }
    }

    impl std::ops::Mul<usize> for Size {
        type Output = Size;
        fn mul(self, rhs: usize) -> Self::Output {
            match self {
                Size::Unknown => Size::Unknown,
                Size::Dynamic => Size::Dynamic,
                Size::Static(lhs) => Size::Static(lhs * rhs),
            }
        }
    }

    impl crate::ast::Annotation for Annotation {
        type FieldAnnotation = FieldAnnotation;
        type DeclAnnotation = DeclAnnotation;
    }

    #[allow(unused)]
    pub type Field = crate::ast::Field<Annotation>;
    #[allow(unused)]
    pub type Decl = crate::ast::Decl<Annotation>;
    #[allow(unused)]
    pub type File = crate::ast::File<Annotation>;
}

/// Aggregate analyzer diagnostics.
#[derive(Debug, Default)]
pub struct Diagnostics {
    pub diagnostics: Vec<Diagnostic<FileId>>,
}

/// Gather information about the full AST.
#[derive(Debug, Default)]
pub struct Scope<'d, A: Annotation> {
    /// Collection of Group, Packet, Enum, Struct, Checksum, and CustomField
    /// declarations.
    pub typedef: HashMap<String, &'d crate::ast::Decl<A>>,
}

impl Diagnostics {
    fn is_empty(&self) -> bool {
        self.diagnostics.is_empty()
    }

    fn push(&mut self, diagnostic: Diagnostic<FileId>) {
        self.diagnostics.push(diagnostic)
    }

    fn err_or<T>(self, value: T) -> Result<T, Diagnostics> {
        if self.is_empty() {
            Ok(value)
        } else {
            Err(self)
        }
    }

    pub fn print(
        &self,
        sources: &SourceDatabase,
        color: termcolor::ColorChoice,
    ) -> Result<(), files::Error> {
        let writer = termcolor::StandardStream::stderr(color);
        let config = term::Config::default();
        for d in self.diagnostics.iter() {
            term::emit(&mut writer.lock(), &config, sources, d)?;
        }
        Ok(())
    }
}

pub struct FieldIter<'s, 'd, A: Annotation + Default> {
    scope: &'s Scope<'d, A>,
    decl: &'d crate::ast::Decl<A>,
    slice: std::slice::Iter<'d, crate::ast::Field<A>>,
}

impl<'s, 'd, A: Annotation + Default> Iterator for FieldIter<'s, 'd, A> {
    type Item = &'d crate::ast::Field<A>;

    fn next(&mut self) -> std::option::Option<<Self as Iterator>::Item> {
        if let Some(item) = self.slice.next() {
            Some(item)
        } else {
            match self.scope.get_parent(self.decl) {
                Some(decl) => {
                    self.decl = decl;
                    self.slice = decl.fields();
                    self.slice.next()
                }
                None => None,
            }
        }
    }
}

impl<'d, A: Annotation + Default> Scope<'d, A> {
    pub fn new(file: &'d crate::ast::File<A>) -> Result<Scope<'d, A>, Diagnostics> {
        // Gather top-level declarations.
        let mut scope: Scope<A> = Default::default();
        let mut diagnostics: Diagnostics = Default::default();
        for decl in &file.declarations {
            if let Some(id) = decl.id() {
                if let Some(prev) = scope.typedef.insert(id.to_string(), decl) {
                    diagnostics.push(
                        Diagnostic::error()
                            .with_code("E1")
                            .with_message(format!(
                                "redeclaration of {} identifier `{}`",
                                decl.kind(),
                                id
                            ))
                            .with_labels(vec![
                                decl.loc.primary(),
                                prev.loc
                                    .secondary()
                                    .with_message(format!("`{}` is first declared here", id)),
                            ]),
                    )
                }
            }
        }

        // Return failure if any diagnostic is raised.
        if diagnostics.is_empty() {
            Ok(scope)
        } else {
            Err(diagnostics)
        }
    }

    /// Return the parent declaration of the selected declaration,
    /// if it has one.
    pub fn get_parent(&self, decl: &crate::ast::Decl<A>) -> Option<&'d crate::ast::Decl<A>> {
        decl.parent_id().and_then(|parent_id| self.typedef.get(parent_id).cloned())
    }

    /// Iterate over the declaration and its parent's fields.
    pub fn iter_fields<'s>(&'s self, decl: &'d crate::ast::Decl<A>) -> FieldIter<'s, 'd, A> {
        FieldIter { scope: self, decl, slice: decl.fields() }
    }
}

/// Return the bit-width of a scalar value.
fn bit_width(value: usize) -> usize {
    usize::BITS as usize - value.leading_zeros() as usize
}

/// Check declaration identifiers.
/// Raises error diagnostics for the following cases:
///      - undeclared parent identifier
///      - invalid parent identifier
///      - undeclared group identifier
///      - invalid group identifier
///      - undeclared typedef identifier
///      - invalid typedef identifier
///      - undeclared test identifier
///      - invalid test identifier
///      - recursive declaration
fn check_decl_identifiers(
    file: &parser_ast::File,
    scope: &Scope<parser_ast::Annotation>,
) -> Result<(), Diagnostics> {
    enum Mark {
        Temporary,
        Permanent,
    }
    #[derive(Default)]
    struct Context<'d> {
        visited: HashMap<&'d str, Mark>,
    }

    fn bfs<'d>(
        decl: &'d parser_ast::Decl,
        context: &mut Context<'d>,
        scope: &Scope<'d, parser_ast::Annotation>,
        diagnostics: &mut Diagnostics,
    ) {
        let decl_id = decl.id().unwrap();
        match context.visited.get(decl_id) {
            Some(Mark::Permanent) => return,
            Some(Mark::Temporary) => {
                diagnostics.push(
                    Diagnostic::error()
                        .with_code("E2")
                        .with_message(format!(
                            "recursive declaration of {} `{}`",
                            decl.kind(),
                            decl_id
                        ))
                        .with_labels(vec![decl.loc.primary()]),
                );
                return;
            }
            _ => (),
        }

        // Start visiting current declaration.
        context.visited.insert(decl_id, Mark::Temporary);

        // Iterate over Struct and Group fields.
        for field in decl.fields() {
            match &field.desc {
                // Validate that the group field has a valid identifier.
                // If the type is a group recurse the group definition.
                FieldDesc::Group { group_id, .. } => match scope.typedef.get(group_id) {
                    None => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E3")
                            .with_message(format!("undeclared group identifier `{}`", group_id))
                            .with_labels(vec![field.loc.primary()])
                            .with_notes(vec!["hint: expected group identifier".to_owned()]),
                    ),
                    Some(group_decl @ Decl { desc: DeclDesc::Group { .. }, .. }) => {
                        bfs(group_decl, context, scope, diagnostics)
                    }
                    Some(_) => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E4")
                            .with_message(format!("invalid group identifier `{}`", group_id))
                            .with_labels(vec![field.loc.primary()])
                            .with_notes(vec!["hint: expected group identifier".to_owned()]),
                    ),
                },
                // Validate that the typedef field has a valid identifier.
                // If the type is a struct recurse the struct definition.
                // Append the field to the packet re-definition.
                FieldDesc::Typedef { type_id, .. }
                | FieldDesc::Array { type_id: Some(type_id), .. } => {
                    match scope.typedef.get(type_id) {
                        None => diagnostics.push(
                            Diagnostic::error().with_code("E5")
                                .with_message(format!(
                                    "undeclared {} identifier `{}`",
                                    field.kind(),
                                    type_id
                                ))
                                .with_labels(vec![field.loc.primary()])
                                .with_notes(vec!["hint: expected enum, struct, custom_field, checksum identifier".to_owned()]),
                        ),
                        Some(Decl { desc: DeclDesc::Packet { .. }, .. }) => diagnostics.push(
                            Diagnostic::error().with_code("E6")
                                .with_message(format!(
                                    "invalid {} identifier `{}`",
                                    field.kind(),
                                    type_id
                                ))
                                .with_labels(vec![field.loc.primary()])
                                .with_notes(vec!["hint: expected enum, struct, custom_field, checksum identifier".to_owned()]),
                        ),
                        Some(typedef_decl) =>
                            // Not recursing on array type since it is allowed to
                            // have recursive structures, e.g. nested TLV types.
                            if matches!(&field.desc, FieldDesc::Typedef { .. }) ||
                               matches!(&field.desc, FieldDesc::Array { size: Some(_), .. }) {
                                bfs(typedef_decl, context, scope, diagnostics)
                            }
                    }
                }
                // Ignore other fields.
                _ => (),
            }
        }

        // Iterate over parent declaration.
        if let Some(parent_id) = decl.parent_id() {
            let parent_decl = scope.typedef.get(parent_id);
            match (&decl.desc, parent_decl) {
                (DeclDesc::Packet { .. }, None) | (DeclDesc::Struct { .. }, None) => diagnostics
                    .push(
                        Diagnostic::error()
                            .with_code("E7")
                            .with_message(format!("undeclared parent identifier `{}`", parent_id))
                            .with_labels(vec![decl.loc.primary()])
                            .with_notes(vec![format!("hint: expected {} identifier", decl.kind())]),
                    ),
                (
                    DeclDesc::Packet { .. },
                    Some(parent_decl @ Decl { desc: DeclDesc::Packet { .. }, .. }),
                )
                | (
                    DeclDesc::Struct { .. },
                    Some(parent_decl @ Decl { desc: DeclDesc::Struct { .. }, .. }),
                ) => bfs(parent_decl, context, scope, diagnostics),
                (_, Some(_)) => diagnostics.push(
                    Diagnostic::error()
                        .with_code("E8")
                        .with_message(format!("invalid parent identifier `{}`", parent_id))
                        .with_labels(vec![decl.loc.primary()])
                        .with_notes(vec![format!("hint: expected {} identifier", decl.kind())]),
                ),
                _ => unreachable!(),
            }
        }

        // Done visiting current declaration.
        context.visited.insert(decl_id, Mark::Permanent);
    }

    // Start bfs.
    let mut diagnostics = Default::default();
    let mut context = Default::default();
    for decl in &file.declarations {
        match &decl.desc {
            DeclDesc::Checksum { .. } | DeclDesc::CustomField { .. } | DeclDesc::Enum { .. } => (),
            DeclDesc::Packet { .. } | DeclDesc::Struct { .. } | DeclDesc::Group { .. } => {
                bfs(&decl, &mut context, scope, &mut diagnostics)
            }
            DeclDesc::Test { type_id, .. } => match scope.typedef.get(type_id) {
                None => diagnostics.push(
                    Diagnostic::error()
                        .with_code("E9")
                        .with_message(format!("undeclared test identifier `{}`", type_id))
                        .with_labels(vec![decl.loc.primary()])
                        .with_notes(vec!["hint: expected packet identifier".to_owned()]),
                ),
                Some(Decl { desc: DeclDesc::Packet { .. }, .. }) => (),
                Some(_) => diagnostics.push(
                    Diagnostic::error()
                        .with_code("E10")
                        .with_message(format!("invalid test identifier `{}`", type_id))
                        .with_labels(vec![decl.loc.primary()])
                        .with_notes(vec!["hint: expected packet identifier".to_owned()]),
                ),
            },
        }
    }

    diagnostics.err_or(())
}

/// Check field identifiers.
/// Raises error diagnostics for the following cases:
///      - duplicate field identifier
fn check_field_identifiers(file: &parser_ast::File) -> Result<(), Diagnostics> {
    let mut diagnostics: Diagnostics = Default::default();
    for decl in &file.declarations {
        let mut local_scope = HashMap::new();
        for field in decl.fields() {
            if let Some(id) = field.id() {
                if let Some(prev) = local_scope.insert(id.to_string(), field) {
                    diagnostics.push(
                        Diagnostic::error()
                            .with_code("E11")
                            .with_message(format!(
                                "redeclaration of {} field identifier `{}`",
                                field.kind(),
                                id
                            ))
                            .with_labels(vec![
                                field.loc.primary(),
                                prev.loc
                                    .secondary()
                                    .with_message(format!("`{}` is first declared here", id)),
                            ]),
                    )
                }
            }
        }
    }

    diagnostics.err_or(())
}

/// Check enum declarations.
/// Raises error diagnostics for the following cases:
///      - duplicate tag identifier
///      - duplicate tag value
fn check_enum_declarations(file: &parser_ast::File) -> Result<(), Diagnostics> {
    let mut diagnostics: Diagnostics = Default::default();
    for decl in &file.declarations {
        match &decl.desc {
            DeclDesc::Enum { tags, width, .. } => {
                let mut tags_by_id = HashMap::new();
                let mut tags_by_value = HashMap::new();

                for tag in tags {
                    if let Some(prev) = tags_by_id.insert(&tag.id, tag) {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E12")
                                .with_message(format!("duplicate tag identifier `{}`", tag.id))
                                .with_labels(vec![
                                    tag.loc.primary(),
                                    prev.loc.secondary().with_message(format!(
                                        "`{}` is first declared here",
                                        tag.id
                                    )),
                                ]),
                        )
                    }
                    if let Some(prev) = tags_by_value.insert(&tag.value, tag) {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E13")
                                .with_message(format!("duplicate tag value `{}`", tag.value))
                                .with_labels(vec![
                                    tag.loc.primary(),
                                    prev.loc.secondary().with_message(format!(
                                        "`{}` is first declared here",
                                        tag.value
                                    )),
                                ]),
                        )
                    }

                    if bit_width(tag.value) > *width {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E14")
                                .with_message(format!(
                                    "tag value `{}` is larger than the maximum value",
                                    tag.value
                                ))
                                .with_labels(vec![tag.loc.primary()]),
                        )
                    }
                }
            }
            _ => (),
        }
    }

    diagnostics.err_or(())
}

/// Check constraints.
/// Raises error diagnostics for the following cases:
///      - undeclared constraint identifier
///      - invalid constraint identifier
///      - invalid constraint scalar value (bad type)
///      - invalid constraint scalar value (overflow)
///      - invalid constraint enum value (bad type)
///      - invalid constraint enum value (undeclared tag)
///      - TODO duplicate constraint
fn check_constraints(
    file: &parser_ast::File,
    scope: &Scope<parser_ast::Annotation>,
) -> Result<(), Diagnostics> {
    fn check_constraint(
        constraint: &Constraint,
        decl: &parser_ast::Decl,
        scope: &Scope<parser_ast::Annotation>,
        diagnostics: &mut Diagnostics,
    ) {
        match scope.iter_fields(decl).filter(|field| field.id() == Some(&constraint.id)).next() {
            None => diagnostics.push(
                Diagnostic::error()
                    .with_code("E15")
                    .with_message(format!("undeclared constraint identifier `{}`", constraint.id))
                    .with_labels(vec![constraint.loc.primary()])
                    .with_notes(vec!["hint: expected scalar, typedef identifier".to_owned()]),
            ),
            Some(field @ Field { desc: FieldDesc::Array { .. }, .. }) => diagnostics.push(
                Diagnostic::error()
                    .with_code("E16")
                    .with_message(format!("invalid constraint identifier `{}`", constraint.id))
                    .with_labels(vec![
                        constraint.loc.primary(),
                        field.loc.secondary().with_message(format!(
                            "`{}` is declared here as array field",
                            constraint.id
                        )),
                    ])
                    .with_notes(vec!["hint: expected scalar, typedef identifier".to_owned()]),
            ),
            Some(field @ Field { desc: FieldDesc::Scalar { width, .. }, .. }) => {
                match constraint.value {
                    None => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E17")
                            .with_message(format!(
                                "invalid constraint value `{}`",
                                constraint.tag_id.as_ref().unwrap()
                            ))
                            .with_labels(vec![
                                constraint.loc.primary(),
                                field.loc.secondary().with_message(format!(
                                    "`{}` is declared here as scalar field",
                                    constraint.id
                                )),
                            ])
                            .with_notes(vec!["hint: expected scalar value".to_owned()]),
                    ),
                    Some(value) if bit_width(value) > *width => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E18")
                            .with_message(format!(
                                "constraint value `{}` is larger than maximum value",
                                value
                            ))
                            .with_labels(vec![constraint.loc.primary(), field.loc.secondary()]),
                    ),
                    _ => (),
                }
            }
            Some(field @ Field { desc: FieldDesc::Typedef { type_id, .. }, .. }) => {
                match scope.typedef.get(type_id) {
                    None => (),
                    Some(Decl { desc: DeclDesc::Enum { tags, .. }, .. }) => {
                        match &constraint.tag_id {
                            None => diagnostics.push(
                                Diagnostic::error()
                                    .with_code("E19")
                                    .with_message(format!(
                                        "invalid constraint value `{}`",
                                        constraint.value.unwrap()
                                    ))
                                    .with_labels(vec![
                                        constraint.loc.primary(),
                                        field.loc.secondary().with_message(format!(
                                            "`{}` is declared here as typedef field",
                                            constraint.id
                                        )),
                                    ])
                                    .with_notes(vec!["hint: expected enum value".to_owned()]),
                            ),
                            Some(tag_id) if !tags.iter().any(|tag| &tag.id == tag_id) => {
                                diagnostics.push(
                                    Diagnostic::error()
                                        .with_code("E20")
                                        .with_message(format!("undeclared enum tag `{}`", tag_id))
                                        .with_labels(vec![
                                            constraint.loc.primary(),
                                            field.loc.secondary().with_message(format!(
                                                "`{}` is declared here",
                                                constraint.id
                                            )),
                                        ]),
                                )
                            }
                            _ => (),
                        }
                    }
                    Some(decl) => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E21")
                            .with_message(format!(
                                "invalid constraint identifier `{}`",
                                constraint.value.unwrap()
                            ))
                            .with_labels(vec![
                                constraint.loc.primary(),
                                field.loc.secondary().with_message(format!(
                                    "`{}` is declared here as {} typedef field",
                                    constraint.id,
                                    decl.kind()
                                )),
                            ])
                            .with_notes(vec!["hint: expected enum value".to_owned()]),
                    ),
                }
            }
            Some(_) => unreachable!(),
        }
    }

    let mut diagnostics: Diagnostics = Default::default();
    for decl in &file.declarations {
        // Check constraints for packet inheritance.
        match &decl.desc {
            DeclDesc::Packet { constraints, parent_id: Some(parent_id), .. }
            | DeclDesc::Struct { constraints, parent_id: Some(parent_id), .. } => {
                let parent_decl = scope.typedef.get(parent_id).unwrap();
                for constraint in constraints {
                    check_constraint(constraint, parent_decl, scope, &mut diagnostics)
                }
            }
            _ => (),
        }

        // Check constraints for group inlining.
        for field in decl.fields() {
            match &field.desc {
                FieldDesc::Group { group_id, constraints } => {
                    let group_decl = scope.typedef.get(group_id).unwrap();
                    for constraint in constraints {
                        check_constraint(constraint, group_decl, scope, &mut diagnostics)
                    }
                }
                _ => (),
            }
        }
    }

    diagnostics.err_or(())
}

/// Check size fields.
/// Raises error diagnostics for the following cases:
///      - undeclared size identifier
///      - invalid size identifier
///      - duplicate size field
///      - undeclared count identifier
///      - invalid count identifier
///      - duplicate count field
///      - undeclared elementsize identifier
///      - invalid elementsize identifier
///      - duplicate elementsize field
fn check_size_fields(file: &parser_ast::File) -> Result<(), Diagnostics> {
    let mut diagnostics: Diagnostics = Default::default();
    let mut size_for_id = HashMap::new();
    for decl in &file.declarations {
        for field in decl.fields() {
            match &field.desc {
                FieldDesc::Size { field_id, .. } => {
                    if let Some(prev) = size_for_id.insert(field_id, field) {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E22")
                                .with_message("duplicate size field".to_owned())
                                .with_labels(vec![
                                    field.loc.primary(),
                                    prev.loc.secondary().with_message(format!(
                                        "{} is first declared here",
                                        prev.kind()
                                    )),
                                ]),
                        )
                    }

                    match decl.fields().find(|field| match &field.desc {
                        FieldDesc::Payload { .. } => field_id == "_payload_",
                        FieldDesc::Body { .. } => field_id == "_body_",
                        _ => field.id() == Some(field_id),
                    }) {
                        None => diagnostics.push(
                            Diagnostic::error()
                                .with_code("E23")
                                .with_message(format!(
                                    "undeclared {} identifier `{}`",
                                    field.kind(),
                                    field_id
                                ))
                                .with_labels(vec![field.loc.primary()])
                                .with_notes(vec![
                                    "hint: expected payload, body, array identifier".to_owned()
                                ]),
                        ),
                        Some(Field { desc: FieldDesc::Body { .. }, .. })
                        | Some(Field { desc: FieldDesc::Payload { .. }, .. })
                        | Some(Field { desc: FieldDesc::Array { .. }, .. }) => (),
                        Some(Field { loc, .. }) => diagnostics.push(
                            Diagnostic::error()
                                .with_code("E24")
                                .with_message(format!(
                                    "invalid {} identifier `{}`",
                                    field.kind(),
                                    field_id
                                ))
                                .with_labels(vec![field.loc.primary(), loc.secondary()])
                                .with_notes(vec![
                                    "hint: expected payload, body, array identifier".to_owned()
                                ]),
                        ),
                    }
                }

                FieldDesc::Count { field_id, .. } | FieldDesc::ElementSize { field_id, .. } => {
                    if let Some(prev) = size_for_id.insert(field_id, field) {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E25")
                                .with_message("duplicate count field".to_owned())
                                .with_labels(vec![
                                    field.loc.primary(),
                                    prev.loc.secondary().with_message(format!(
                                        "{} is first declared here",
                                        prev.kind()
                                    )),
                                ]),
                        )
                    }
                    match decl.fields().find(|field| field.id() == Some(field_id)) {
                        None => diagnostics.push(
                            Diagnostic::error()
                                .with_code("E26")
                                .with_message(format!(
                                    "undeclared {} identifier `{}`",
                                    field.kind(),
                                    field_id
                                ))
                                .with_labels(vec![field.loc.primary()])
                                .with_notes(vec!["hint: expected array identifier".to_owned()]),
                        ),
                        Some(Field { desc: FieldDesc::Array { .. }, .. }) => (),
                        Some(Field { loc, .. }) => diagnostics.push(
                            Diagnostic::error()
                                .with_code("E27")
                                .with_message(format!(
                                    "invalid {} identifier `{}`",
                                    field.kind(),
                                    field_id
                                ))
                                .with_labels(vec![field.loc.primary(), loc.secondary()])
                                .with_notes(vec!["hint: expected array identifier".to_owned()]),
                        ),
                    }
                }
                _ => (),
            }
        }
    }

    diagnostics.err_or(())
}

/// Check fixed fields.
/// Raises error diagnostics for the following cases:
///      - invalid scalar value
///      - undeclared enum identifier
///      - invalid enum identifier
///      - undeclared tag identifier
fn check_fixed_fields(
    file: &parser_ast::File,
    scope: &Scope<parser_ast::Annotation>,
) -> Result<(), Diagnostics> {
    let mut diagnostics: Diagnostics = Default::default();
    for decl in &file.declarations {
        for field in decl.fields() {
            match &field.desc {
                FieldDesc::FixedScalar { value, width } if bit_width(*value) > *width => {
                    diagnostics.push(
                        Diagnostic::error()
                            .with_code("E28")
                            .with_message(format!(
                                "fixed value `{}` is larger than maximum value",
                                value
                            ))
                            .with_labels(vec![field.loc.primary()]),
                    )
                }
                FieldDesc::FixedEnum { tag_id, enum_id } => match scope.typedef.get(enum_id) {
                    None => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E29")
                            .with_message(format!("undeclared type identifier `{}`", enum_id))
                            .with_labels(vec![field.loc.primary()])
                            .with_notes(vec!["hint: expected enum identifier".to_owned()]),
                    ),
                    Some(enum_decl @ Decl { desc: DeclDesc::Enum { tags, .. }, .. }) => {
                        if !tags.iter().any(|tag| &tag.id == tag_id) {
                            diagnostics.push(
                                Diagnostic::error()
                                    .with_code("E30")
                                    .with_message(format!("undeclared tag identifier `{}`", tag_id))
                                    .with_labels(vec![
                                        field.loc.primary(),
                                        enum_decl.loc.secondary(),
                                    ]),
                            )
                        }
                    }
                    Some(decl) => diagnostics.push(
                        Diagnostic::error()
                            .with_code("E31")
                            .with_message(format!("invalid type identifier `{}`", enum_id))
                            .with_labels(vec![
                                field.loc.primary(),
                                decl.loc
                                    .secondary()
                                    .with_message(format!("`{}` is declared here", enum_id)),
                            ])
                            .with_notes(vec!["hint: expected enum identifier".to_owned()]),
                    ),
                },
                _ => (),
            }
        }
    }

    diagnostics.err_or(())
}

/// Check payload fields.
/// Raises error diagnostics for the following cases:
///      - duplicate payload field
///      - duplicate payload field size
///      - duplicate body field
///      - duplicate body field size
///      - missing payload field
fn check_payload_fields(file: &parser_ast::File) -> Result<(), Diagnostics> {
    // Check whether the declaration requires a payload field.
    // The payload is required if any child packets declares fields.
    fn requires_payload(file: &parser_ast::File, decl: &parser_ast::Decl) -> bool {
        file.declarations
            .iter()
            .filter(|other_decl| other_decl.parent_id() == decl.id())
            .any(|other_decl| other_decl.fields().next().is_some())
    }

    let mut diagnostics: Diagnostics = Default::default();
    for decl in &file.declarations {
        let mut payload: Option<&parser_ast::Field> = None;
        for field in decl.fields() {
            match &field.desc {
                FieldDesc::Payload { .. } | FieldDesc::Body { .. } => {
                    if let Some(prev) = payload {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E32")
                                .with_message(format!("duplicate {} field", field.kind()))
                                .with_labels(vec![
                                    field.loc.primary(),
                                    prev.loc.secondary().with_message(format!(
                                        "{} is first declared here",
                                        prev.kind()
                                    )),
                                ]),
                        )
                    } else {
                        payload = Some(field);
                    }
                }
                _ => (),
            }
        }

        if payload.is_none() && requires_payload(file, decl) {
            diagnostics.push(
                Diagnostic::error()
                    .with_code("E33")
                    .with_message("missing payload field".to_owned())
                    .with_labels(vec![decl.loc.primary()])
                    .with_notes(vec![format!(
                        "hint: one child packet is extending `{}`",
                        decl.id().unwrap()
                    )]),
            )
        }
    }

    diagnostics.err_or(())
}

/// Check array fields.
/// Raises error diagnostics for the following cases:
///      - redundant array field size
fn check_array_fields(file: &parser_ast::File) -> Result<(), Diagnostics> {
    let mut diagnostics: Diagnostics = Default::default();
    for decl in &file.declarations {
        for field in decl.fields() {
            match &field.desc {
                FieldDesc::Array { id, size: Some(size), .. } => {
                    if let Some(size_field) = decl
                        .fields()
                        .filter(|field| match &field.desc {
                            FieldDesc::Size { field_id, .. }
                            | FieldDesc::Count { field_id, .. } => field_id == id,
                            _ => false,
                        })
                        .next()
                    {
                        diagnostics.push(
                            Diagnostic::error()
                                .with_code("E34")
                                .with_message(format!(
                                    "redundant array {} field",
                                    size_field.kind()
                                ))
                                .with_labels(vec![
                                    size_field.loc.primary(),
                                    field.loc.secondary().with_message(format!(
                                        "`{}` has constant size {}",
                                        id, size
                                    )),
                                ]),
                        )
                    }
                }
                _ => (),
            }
        }
    }

    diagnostics.err_or(())
}

/// Check correct definition of packet sizes.
/// Annotate fields and declarations with the size in bits.
fn compute_field_sizes(file: &parser_ast::File) -> ast::File {
    fn annotate_decl(
        decl: &parser_ast::Decl,
        scope: &HashMap<String, ast::DeclAnnotation>,
    ) -> ast::Decl {
        // Annotate the declaration fields.
        let mut decl = decl.annotate(Default::default(), |fields| {
            fields.iter().map(|field| annotate_field(decl, field, scope)).collect()
        });

        // Compute the declaration annotation.
        decl.annot = match &decl.desc {
            DeclDesc::Packet { fields, .. }
            | DeclDesc::Struct { fields, .. }
            | DeclDesc::Group { fields, .. } => {
                let mut size = ast::Size::Static(0);
                let mut payload_size = ast::Size::Static(0);
                for field in fields {
                    match &field.desc {
                        FieldDesc::Payload { .. } | FieldDesc::Body { .. } => {
                            payload_size = field.annot.size
                        }
                        _ => size = size + field.annot.size,
                    }
                }
                ast::DeclAnnotation { size, payload_size, ..decl.annot }
            }
            DeclDesc::Enum { width, .. }
            | DeclDesc::Checksum { width, .. }
            | DeclDesc::CustomField { width: Some(width), .. } => {
                ast::DeclAnnotation { size: ast::Size::Static(*width), ..decl.annot }
            }
            DeclDesc::CustomField { width: None, .. } => {
                ast::DeclAnnotation { size: ast::Size::Dynamic, ..decl.annot }
            }
            DeclDesc::Test { .. } => {
                ast::DeclAnnotation { size: ast::Size::Static(0), ..decl.annot }
            }
        };
        decl
    }

    fn annotate_field(
        decl: &parser_ast::Decl,
        field: &parser_ast::Field,
        scope: &HashMap<String, ast::DeclAnnotation>,
    ) -> ast::Field {
        field.annotate(match &field.desc {
            FieldDesc::Checksum { .. } | FieldDesc::Padding { .. } => {
                ast::FieldAnnotation { size: ast::Size::Static(0) }
            }
            FieldDesc::Size { width, .. }
            | FieldDesc::Count { width, .. }
            | FieldDesc::ElementSize { width, .. }
            | FieldDesc::FixedScalar { width, .. }
            | FieldDesc::Reserved { width }
            | FieldDesc::Scalar { width, .. } => {
                ast::FieldAnnotation { size: ast::Size::Static(*width) }
            }
            FieldDesc::Body | FieldDesc::Payload { .. } => {
                let has_payload_size = decl.fields().any(|field| match &field.desc {
                    FieldDesc::Size { field_id, .. } => {
                        field_id == "_body_" || field_id == "_payload_"
                    }
                    _ => false,
                });
                ast::FieldAnnotation {
                    size: if has_payload_size { ast::Size::Dynamic } else { ast::Size::Unknown },
                }
            }
            FieldDesc::Typedef { type_id, .. }
            | FieldDesc::FixedEnum { enum_id: type_id, .. }
            | FieldDesc::Group { group_id: type_id, .. } => {
                let type_annot = scope.get(type_id).unwrap();
                ast::FieldAnnotation { size: type_annot.size + type_annot.payload_size }
            }
            FieldDesc::Array { width: Some(width), size: Some(size), .. } => {
                ast::FieldAnnotation { size: ast::Size::Static(*size * *width) }
            }
            FieldDesc::Array { width: None, size: Some(size), type_id: Some(type_id), .. } => {
                let type_annot = scope.get(type_id).unwrap();
                ast::FieldAnnotation { size: (type_annot.size + type_annot.payload_size) * *size }
            }
            FieldDesc::Array { id, size: None, .. } => {
                // The element does not matter when the size of the array is
                // not static. The array size depends on there being a count
                // or size field or not.
                let has_array_size = decl.fields().any(|field| match &field.desc {
                    FieldDesc::Size { field_id, .. } | FieldDesc::Count { field_id, .. } => {
                        field_id == id
                    }
                    _ => false,
                });
                ast::FieldAnnotation {
                    size: if has_array_size { ast::Size::Dynamic } else { ast::Size::Unknown },
                }
            }
            FieldDesc::Array { .. } => unreachable!(),
        })
    }

    // Construct a scope mapping typedef identifiers to decl annotations.
    let mut scope = HashMap::new();

    // Annotate declarations.
    let mut declarations = Vec::new();
    for decl in file.declarations.iter() {
        let decl = annotate_decl(decl, &scope);
        if let Some(id) = decl.id() {
            scope.insert(id.to_string(), decl.annot.clone());
        }
        declarations.push(decl);
    }

    File {
        version: file.version.clone(),
        file: file.file,
        comments: file.comments.clone(),
        endianness: file.endianness,
        declarations,
    }
}

/// Inline group fields and remove group declarations.
fn inline_groups(_file: &mut ast::File) -> Result<ast::File, Diagnostics> {
    todo!()
}

/// Analyzer entry point, produces a new AST with annotations resulting
/// from the analysis.
pub fn analyze(file: &parser_ast::File) -> Result<ast::File, Diagnostics> {
    let scope = Scope::new(file)?;
    check_decl_identifiers(file, &scope)?;
    check_field_identifiers(file)?;
    check_enum_declarations(file)?;
    check_constraints(file, &scope)?;
    check_size_fields(file)?;
    check_fixed_fields(file, &scope)?;
    check_payload_fields(file)?;
    check_array_fields(file)?;
    // TODO check_checksum_fields(file, &scope)?;
    // TODO check_padding_fields(file, &scope)?;
    let mut file = compute_field_sizes(file);
    inline_groups(&mut file)
}

#[cfg(test)]
mod test {
    use crate::analyzer;
    use crate::ast::*;
    use crate::parser::parse_inline;
    use codespan_reporting::term::termcolor;

    macro_rules! raises {
        ($code:literal, $text:literal) => {{
            let mut db = SourceDatabase::new();
            let file = parse_inline(&mut db, "stdin".to_owned(), $text.to_owned())
                .expect("parsing failure");
            let result = analyzer::analyze(&file);
            assert!(matches!(result, Err(_)));
            let diagnostics = result.err().unwrap();
            let _ = diagnostics.print(&db, termcolor::ColorChoice::Always);
            assert_eq!(diagnostics.diagnostics.len(), 1);
            assert_eq!(diagnostics.diagnostics[0].code, Some($code.to_owned()));
        }};
    }

    #[test]
    fn test_e1() {
        raises!(
            "E1",
            r#"
            little_endian_packets
            struct A { }
            packet A { }
            "#
        );

        raises!(
            "E1",
            r#"
            little_endian_packets
            struct A { }
            enum A : 8 { X = 0, Y = 1 }
            "#
        );
    }

    #[test]
    fn test_e2() {
        raises!(
            "E2",
            r#"
            little_endian_packets
            packet A : A { }
            "#
        );

        raises!(
            "E2",
            r#"
            little_endian_packets
            packet A : B { }
            packet B : A { }
            "#
        );

        raises!(
            "E2",
            r#"
            little_endian_packets
            struct B { x : B }
            "#
        );

        raises!(
            "E2",
            r#"
            little_endian_packets
            struct B { x : B[8] }
            "#
        );

        raises!(
            "E2",
            r#"
            little_endian_packets
            group C { C { x = 1 } }
            "#
        );
    }

    #[test]
    fn test_e3() {
        raises!(
            "E3",
            r#"
        little_endian_packets
        packet A { C { x = 1 } }
        "#
        );
    }

    #[test]
    fn test_e4() {
        raises!(
            "E4",
            r#"
        little_endian_packets
        struct C { x : 8 }
        packet A { C { x = 1 } }
        "#
        );
    }

    #[test]
    fn test_e5() {
        raises!(
            "E5",
            r#"
        little_endian_packets
        packet A { x : B }
        "#
        );

        raises!(
            "E5",
            r#"
        little_endian_packets
        packet A { x : B[] }
        "#
        );
    }

    #[test]
    fn test_e6() {
        raises!(
            "E6",
            r#"
        little_endian_packets
        packet A { x : 8 }
        packet B { x : A }
        "#
        );

        raises!(
            "E6",
            r#"
        little_endian_packets
        packet A { x : 8 }
        packet B { x : A[] }
        "#
        );
    }

    #[test]
    fn test_e7() {
        raises!(
            "E7",
            r#"
        little_endian_packets
        packet A : B { }
        "#
        );

        raises!(
            "E7",
            r#"
        little_endian_packets
        struct A : B { }
        "#
        );
    }

    #[test]
    fn test_e8() {
        raises!(
            "E8",
            r#"
        little_endian_packets
        struct A { }
        packet B : A { }
        "#
        );

        raises!(
            "E8",
            r#"
        little_endian_packets
        packet A { }
        struct B : A { }
        "#
        );

        raises!(
            "E8",
            r#"
        little_endian_packets
        group A { x : 1 }
        struct B : A { }
        "#
        );
    }

    #[ignore]
    #[test]
    fn test_e9() {
        raises!(
            "E9",
            r#"
        little_endian_packets
        test A { "aaa" }
        "#
        );
    }

    #[ignore]
    #[test]
    fn test_e10() {
        raises!(
            "E10",
            r#"
        little_endian_packets
        struct A { }
        test A { "aaa" }
        "#
        );

        raises!(
            "E10",
            r#"
        little_endian_packets
        group A { x : 8 }
        test A { "aaa" }
        "#
        );
    }

    #[test]
    fn test_e11() {
        raises!(
            "E11",
            r#"
        little_endian_packets
        enum A : 8 { X = 0 }
        struct B {
            x : 8,
            x : A
        }
        "#
        );

        raises!(
            "E11",
            r#"
        little_endian_packets
        enum A : 8 { X = 0 }
        packet B {
            x : 8,
            x : A[]
        }
        "#
        );
    }

    #[test]
    fn test_e12() {
        raises!(
            "E12",
            r#"
        little_endian_packets
        enum A : 8 {
            X = 0,
            X = 1,
        }
        "#
        );
    }

    #[test]
    fn test_e13() {
        raises!(
            "E13",
            r#"
        little_endian_packets
        enum A : 8 {
            X = 0,
            Y = 0,
        }
        "#
        );
    }

    #[test]
    fn test_e14() {
        raises!(
            "E14",
            r#"
        little_endian_packets
        enum A : 8 {
            X = 256,
        }
        "#
        );
    }

    #[test]
    fn test_e15() {
        raises!(
            "E15",
            r#"
        little_endian_packets
        packet A { }
        packet B : A (x = 1) { }
        "#
        );

        raises!(
            "E15",
            r#"
        little_endian_packets
        group A { x : 8 }
        packet B {
            A { y = 1 }
        }
        "#
        );
    }

    #[test]
    fn test_e16() {
        raises!(
            "E16",
            r#"
        little_endian_packets
        packet A { x : 8[] }
        packet B : A (x = 1) { }
        "#
        );

        raises!(
            "E16",
            r#"
        little_endian_packets
        group A { x : 8[] }
        packet B {
            A { x = 1 }
        }
        "#
        );
    }

    #[test]
    fn test_e17() {
        raises!(
            "E17",
            r#"
        little_endian_packets
        packet A { x : 8 }
        packet B : A (x = X) { }
        "#
        );

        raises!(
            "E17",
            r#"
        little_endian_packets
        group A { x : 8 }
        packet B {
            A { x = X }
        }
        "#
        );
    }

    #[test]
    fn test_e18() {
        raises!(
            "E18",
            r#"
        little_endian_packets
        packet A { x : 8 }
        packet B : A (x = 256) { }
        "#
        );

        raises!(
            "E18",
            r#"
        little_endian_packets
        group A { x : 8 }
        packet B {
            A { x = 256 }
        }
        "#
        );
    }

    #[test]
    fn test_e19() {
        raises!(
            "E19",
            r#"
        little_endian_packets
        enum C : 8 { X = 0 }
        packet A { x : C }
        packet B : A (x = 0) { }
        "#
        );

        raises!(
            "E19",
            r#"
        little_endian_packets
        enum C : 8 { X = 0 }
        group A { x : C }
        packet B {
            A { x = 0 }
        }
        "#
        );
    }

    #[test]
    fn test_e20() {
        raises!(
            "E20",
            r#"
        little_endian_packets
        enum C : 8 { X = 0 }
        packet A { x : C }
        packet B : A (x = Y) { }
        "#
        );

        raises!(
            "E20",
            r#"
        little_endian_packets
        enum C : 8 { X = 0 }
        group A { x : C }
        packet B {
            A { x = Y }
        }
        "#
        );
    }

    #[test]
    fn test_e21() {
        raises!(
            "E21",
            r#"
        little_endian_packets
        struct C { }
        packet A { x : C }
        packet B : A (x = 0) { }
        "#
        );

        raises!(
            "E21",
            r#"
        little_endian_packets
        struct C { }
        group A { x : C }
        packet B {
            A { x = 0 }
        }
        "#
        );
    }

    #[test]
    fn test_e22() {
        raises!(
            "E22",
            r#"
        little_endian_packets
        struct A {
            _size_ (_payload_) : 8,
            _size_ (_payload_) : 8,
            _payload_,
        }
        "#
        );

        raises!(
            "E22",
            r#"
        little_endian_packets
        struct A {
            _count_ (x) : 8,
            _size_ (x) : 8,
            x: 8[],
        }
        "#
        );
    }

    #[test]
    fn test_e23() {
        raises!(
            "E23",
            r#"
        little_endian_packets
        struct A {
            _size_ (x) : 8,
        }
        "#
        );

        raises!(
            "E23",
            r#"
        little_endian_packets
        struct A {
            _size_ (_payload_) : 8,
        }
        "#
        );
    }

    #[test]
    fn test_e24() {
        raises!(
            "E24",
            r#"
        little_endian_packets
        enum B : 8 { X = 0 }
        struct A {
            _size_ (x) : 8,
            x : B,
        }
        "#
        );
    }

    #[test]
    fn test_e25() {
        raises!(
            "E25",
            r#"
        little_endian_packets
        struct A {
            _size_ (x) : 8,
            _count_ (x) : 8,
            x: 8[],
        }
        "#
        );
    }

    #[test]
    fn test_e26() {
        raises!(
            "E26",
            r#"
        little_endian_packets
        struct A {
            _count_ (x) : 8,
        }
        "#
        );
    }

    #[test]
    fn test_e27() {
        raises!(
            "E27",
            r#"
        little_endian_packets
        enum B : 8 { X = 0 }
        struct A {
            _count_ (x) : 8,
            x : B,
        }
        "#
        );
    }

    #[test]
    fn test_e28() {
        raises!(
            "E28",
            r#"
        little_endian_packets
        struct A {
            _fixed_ = 256 : 8,
        }
        "#
        );
    }

    #[test]
    fn test_e29() {
        raises!(
            "E29",
            r#"
        little_endian_packets
        struct A {
            _fixed_ = X : B,
        }
        "#
        );
    }

    #[test]
    fn test_e30() {
        raises!(
            "E30",
            r#"
        little_endian_packets
        enum B : 8 { X = 0 }
        struct A {
            _fixed_ = Y : B,
        }
        "#
        );
    }

    #[test]
    fn test_e31() {
        raises!(
            "E31",
            r#"
        little_endian_packets
        struct B { }
        struct A {
            _fixed_ = X : B,
        }
        "#
        );
    }

    #[test]
    fn test_e32() {
        raises!(
            "E32",
            r#"
        little_endian_packets
        packet A {
            _payload_,
            _body_,
        }
        "#
        );

        raises!(
            "E32",
            r#"
        little_endian_packets
        packet A {
            _body_,
            _payload_,
        }
        "#
        );
    }

    #[test]
    fn test_e33() {
        raises!(
            "E33",
            r#"
        little_endian_packets
        packet A { x : 8 }
        packet B : A { y : 8 }
        "#
        );

        raises!(
            "E33",
            r#"
        little_endian_packets
        packet A { x : 8 }
        packet B : A (x = 0) { }
        packet C : B { y : 8 }
        "#
        );
    }

    #[test]
    fn test_e34() {
        raises!(
            "E34",
            r#"
        little_endian_packets
        packet A {
            _size_ (x) : 8,
            x : 8[8]
        }
        "#
        );

        raises!(
            "E34",
            r#"
        little_endian_packets
        packet A {
            _count_ (x) : 8,
            x : 8[8]
        }
        "#
        );
    }
}
