#!/usr/bin/env python3

import argparse
from dataclasses import dataclass, field
import json
from pathlib import Path
import sys
from textwrap import dedent
from typing import List, Tuple, Union, Optional

from pdl import ast, core


def indent(lines: List[str], depth: int) -> str:
    """Indent a code block to the selected depth.
    The first line is intentionally not indented so that
    the caller may use it as:

    '''
    def generated():
        {codeblock}
    '''
    """
    sep = '\n' + (' ' * (depth * 4))
    return sep.join(lines)


def indent_block(text: str, depth: int) -> str:
    return indent(text.split('\n'), depth)


def mask(width: int) -> str:
    return hex((1 << width) - 1)


def get_cxx_scalar_type(width: int) -> str:
    """Return the cxx scalar type to be used to back a PDL type."""
    for n in [8, 16, 32, 64]:
        if width <= n:
            return f'uint{n}_t'
    # PDL type does not fit on non-extended scalar types.
    assert False


def generate_enum_declaration(decl: ast.EnumDeclaration) -> str:
    """Generate the implementation of an enum type."""

    enum_name = decl.id
    enum_type = get_cxx_scalar_type(decl.width)
    tag_decls = []
    for t in decl.tags:
        tag_decls.append(f"{t.id} = {hex(t.value)},")

    return dedent("""\

        enum {enum_name} : {enum_type} {{
            {tag_decls}
        }};
        """).format(enum_name=enum_name, enum_type=enum_type,
                    tag_decls=indent(tag_decls, 1))


def generate_enum_to_text(decl: ast.EnumDeclaration) -> str:
    """Generate the helper function that will convert an enum tag to string."""

    enum_name = decl.id
    tag_cases = []
    for t in decl.tags:
        tag_cases.append(f"case {t.id}: return \"{t.id}\";")

    return dedent("""\

        std::string {enum_name}Text({enum_name} tag) {{
            match (tag) {{
                {tag_cases}
                default:
                    return std::string("Unknown {enum_name}: " +
                           std::to_string(static_cast<uint64_t>(tag));
            }}
        }}
        """).format(enum_name=enum_name, tag_cases=indent(tag_cases, 2))


def get_field_view(field: ast.Field) -> List[str]:
    """Compute the iterator for the beginning of the selected field,
    as offset from begin()."""


    pass

def generate_packet_view_field_accessors(packet: ast.PacketDeclaration) -> str:
    accessors = []

    for field in packet.fields:
        if isinstance(field, (ast.PayloadField, ast.BodyField)):
            accessors.append(dedent("""\
                PacketView<kLittleEndian> GetPayload() const {{
                    ASSERT(was_validated_);
                }}
                """).format())

            if

    return []

    return "\n".join(accessors)


def generate_packet_view_field_members(packet: ast.PacketDeclaration) -> List[str]:
    """Return the declaration of fields that are backed in the view
    class declaration."""

    for field in packet.fields:
        if isinstance(field, (ast.PayloadField, ast.BodyField)):
            return [
                "// Fast-access to the payload field to simplify iterating",
                "// over child packets.",
                f"PacketView<kLittleEndian> payload_;"
            ]

    return []


def generate_packet_view_stringifier(packet: ast.PacketDeclaration) -> str:
    return ""


def generate_packet_view_validator(packet: ast.PacketDeclaration) -> str:
    body = ""
    virtual = "" if packet.parent else "virtual "
    override = " override" if packet.parent else ""

    if packet.parent:
        parent_size = packet.parent.get_declaration_size(skip_payload=True)
        body += dedent("""\
            if (!{parent_name}View::Validate()) {{
                return false;
            }}
            """).format(parent_name=packet.parent.id)

    return dedent("""\
        {virtual}bool Validate() const{override} {{
            {body}
        }}
        """).format(body=indent(body, 1),
                    virtual=virtual,
                    override=override)


def generate_packet_view(packet: ast.PacketDeclaration) -> str:
    """Generate the implementation of the View class for a
    packet declaration."""

    parent_class = f"{packet.parent.id}View" if packet.parent else "PacketView<kLittleEndian>"
    field_accessors = generate_packet_view_field_accessors(packet)
    field_members = generate_packet_view_field_members(packet)
    stringifier = generate_packet_view_stringifier(packet)
    validate = generate_packet_view_validator(packet)
    is_valid = ""

    if not packet.parent:
        is_valid = dedent("""\

            bool IsValid() {
                if (was_validated_) {
                  return true;
                } else {
                  return (was_validated_ = Validate());
                }
            }
            """)

    return dedent("""\

        class {packet_name}View : protected {parent_class} {{
        public:
            static {packet_name}View Create({parent_class} parent) {{
                return {packet_name}View(std::move(parent));
            }}

            {field_accessors}
            {stringifier}
            {is_valid}

        protected:
            explicit {packet_name}View({parent_class} parent)
                : {parent_class}(std::move(parent)) {{
                was_validated_ = false;
            }}

            {field_members}

            {validate}
        }};
        """).format(packet_name=packet.id,
                    parent_class=parent_class,
                    field_accessors=indent_block(field_accessors, 1),
                    field_members=indent(field_members, 1),
                    stringifier=indent(stringifier, 1),
                    is_valid=indent_block(is_valid, 1),
                    validate=indent_block(validate, 1))


def generate_packet_builder(packet: ast.PacketDeclaration) -> str:
    """Generate the implementation of the Builder class for a
    packet declaration."""

    return ""


def generate_struct_declaration(struct: ast.StructDeclaration) -> str:
    return ""

def run(input: argparse.FileType, output: argparse.FileType, custom_type_location: Optional[str]):
    file = ast.File.from_json(json.load(input))
    core.desugar(file)

    custom_types = []
    custom_type_checks = ""
    for d in file.declarations:
        if isinstance(d, ast.CustomFieldDeclaration):
            custom_types.append(d.id)
        elif isinstance(d, ast.ChecksumDeclaration):
            custom_types.append(d.id)

    output.write(f"# File generated from {input.name}, with the command:\n")
    output.write(f"#  {' '.join(sys.argv)}\n")
    output.write("# /!\\ Do not edit by hand.\n")
    if custom_types and custom_type_location:
        output.write(f"\nfrom {custom_type_location} import {', '.join(custom_types)}\n")
    #output.write(generate_prelude())

    for d in file.declarations:
        if isinstance(d, ast.EnumDeclaration):
            output.write(generate_enum_declaration(d))
            output.write(generate_enum_to_text(d))
        elif isinstance(d, ast.PacketDeclaration):
            output.write(generate_packet_view(d))
            output.write(generate_packet_builder(d))
        elif isinstance(d, ast.StructDeclaration):
            output.write(generate_struct_declaration(d))


def main() -> int:
    """Generate cxx PDL backend."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', type=argparse.FileType('r'), default=sys.stdin, help='Input PDL-JSON source')
    parser.add_argument('--output', type=argparse.FileType('w'), default=sys.stdout, help='Output Python file')
    parser.add_argument('--custom-type-location',
                        type=str,
                        required=False,
                        help='Module of declaration of custom types')
    return run(**vars(parser.parse_args()))


if __name__ == '__main__':
    sys.exit(main())
