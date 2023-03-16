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


def to_pascal_case(text: str) -> str:
    return text.replace('_', ' ').title().replace(' ', '')


def mask(width: int) -> str:
    return hex((1 << width) - 1)


def get_cxx_scalar_type(width: int) -> str:
    """Return the cxx scalar type to be used to back a PDL type."""
    for n in [8, 16, 32, 64]:
        if width <= n:
            return f'uint{n}_t'
    # PDL type does not fit on non-extended scalar types.
    assert False


@dataclass
class FieldParser:
    offset: int = 0
    shift: int = 0
    extract_arrays: bool = field(default=False)
    chunk: List[Tuple[int, int, ast.Field]] = field(default_factory=lambda: [])
    chunk_nr: int = field(default=0)
    unchecked_code: List[str] = field(default_factory=lambda: [])
    code: List[str] = field(default_factory=lambda: [])

    def unchecked_append_(self, line: str):
        """Append unchecked field parsing code.
        The function check_size_ must be called to generate a size guard
        after parsing is completed."""
        self.unchecked_code.append(line)

    def append_(self, line: str):
        """Append field parsing code.
        There must be no unchecked code left before this function is called."""
        assert len(self.unchecked_code) == 0
        self.code.append(line)

    def check_size_(self, size: str):
        """Generate a check of the current span size."""
        self.append_(f"if (span.size() < {size}) {{")
        self.append_("    return false;")
        self.append_("}")

    def check_code_(self):
        """Generate a size check for pending field parsing."""
        if len(self.unchecked_code) > 0:
            assert len(self.chunk) == 0
            unchecked_code = self.unchecked_code
            self.unchecked_code = []
            self.check_size_(str(self.offset))
            self.code.extend(unchecked_code)
            self.offset = 0

    def parse_bit_field_(self, field: ast.Field):
        """Parse the selected field as a bit field.
        The field is added to the current chunk. When a byte boundary
        is reached all saved fields are extracted together."""

        # Add to current chunk.
        width = core.get_field_size(field)
        self.chunk.append((self.shift, width, field))
        self.shift += width

        # Wait for more fields if not on a byte boundary.
        if (self.shift % 8) != 0:
            return

        # Parse the backing integer using the configured endianness,
        # extract field values.
        size = int(self.shift / 8)
        backing_type = get_cxx_scalar_type(self.shift)

        # Special case when no field is actually used from
        # the chunk.
        should_drop_value = all(isinstance(field, ast.ReservedField) for (_, _, field) in self.chunk)
        if should_drop_value:
            self.unchecked_append_(f"span.skip({size}); // skip reserved fields")
            self.offset += size
            self.shift = 0
            self.chunk = []
            return

        if len(self.chunk) > 1:
            value = f"chunk{self.chunk_nr}"
            self.unchecked_append_(f"{backing_type} {value} = span.read<{backing_type}, {size}>();")
            self.chunk_nr += 1
        else:
            value = f"span.read<{backing_type}, {size}>()"

        for shift, width, field in self.chunk:
            v = (value if len(self.chunk) == 1 and shift == 0 else f"({value} >> {shift}) & {mask(width)}")

            if isinstance(field, ast.ScalarField):
                self.unchecked_append_(f"{field.id}_ = {v};")
            elif isinstance(field, ast.FixedField) and field.enum_id:
                self.unchecked_append_(f"if ({field.enum_id}({v}) != {field.enum_id}::{field.tag_id}) {{")
                self.unchecked_append_("    return false;")
                self.unchecked_append_("}")
            elif isinstance(field, ast.FixedField):
                self.unchecked_append_(f"if (({v}) != {hex(field.value)}) {{")
                self.unchecked_append_("    return false;")
                self.unchecked_append_("}")
            elif isinstance(field, ast.TypedefField):
                self.unchecked_append_(f"{field.id}_ = {field.type_id}({v});")
            elif isinstance(field, ast.SizeField):
                self.unchecked_append_(f"{field.field_id}_size = {v};")
            elif isinstance(field, ast.CountField):
                self.unchecked_append_(f"{field.field_id}_count = {v};")
            elif isinstance(field, ast.ReservedField):
                pass
            else:
                raise Exception(f'Unsupported bit field type {field.kind}')

        # Reset state.
        self.offset += size
        self.shift = 0
        self.chunk = []

    def parse_typedef_field_(self, field: ast.TypedefField):
        """Parse a typedef field, to the exclusion of Enum fields."""
        if self.shift != 0:
            raise Exception('Typedef field does not start on an octet boundary')

        self.check_code_()
        self.append_(
            dedent("""\
            if (!{field_type}::Parse(span, &{field_id}_)) {{
                return false;
            }}""".format(field_type=field.type.id, field_id=field.id)))

    def parse_array_field_lite_(self, field: ast.ArrayField):
        """Parse the selected array field.
        This function does not attempt to parse all elements but just to
        identify the span of the array."""
        array_size = core.get_array_field_size(field)
        element_width = core.get_array_element_size(field)
        padded_size = field.padded_size

        if element_width:
            element_width = int(element_width / 8)

        if isinstance(array_size, int):
            size = None
            count = array_size
        elif isinstance(array_size, ast.SizeField):
            size = f'{field.id}_size'
            count = None
        elif isinstance(array_size, ast.CountField):
            size = None
            count = f'{field.id}_count'
        else:
            size = None
            count = None

        # Shift the span to reset the offset to 0.
        self.check_code_()

        # Apply the size modifier.
        if field.size_modifier and size:
            self.append_(f"{size} = {size} - {field.size_modifier};")

        # Compute the array size if the count and element width are known.
        if count is not None and element_width is not None:
            size = f"{count} * {element_width}"

        # Parse from the padded array if padding is present.
        if padded_size:
            self.check_size_(padded_size)
            self.append_("{")
            self.append_(
                f"pdl::packet::slice remaining_span = span.subrange({padded_size}, span.size() - {padded_size});")
            self.append_(f"span = span.subrange(0, {padded_size});")

        # The array size is known in bytes.
        if size is not None:
            self.check_size_(size)
            self.append_(f"{field.id}_ = span.subrange(0, {size});")
            self.append_(f"span.skip({size});")

        # The array count is known. The element width is dynamic.
        # Parse each element iteratively and derive the array span.
        elif count is not None:
            self.append_("{")
            self.append_("pdl::packet::slice temp_span = span;")
            self.append_(f"for (size_t n = 0; n < {count}; n++) {{")
            self.append_(f"    {field.type_id} element;")
            self.append_(f"    if (!{field.type_id}::Parse(temp_span, &element)) {{")
            self.append_("        return false;")
            self.append_("    }")
            self.append_("}")
            self.append_(f"{field.id}_ = span.subrange(0, span.size() - temp_span.size());")
            self.append_(f"span.skip({field.id}_.size());")
            self.append_("}")

        # The array size is not known, assume the array takes the
        # full remaining space. TODO support having fixed sized fields
        # following the array.
        else:
            self.append_(f"{field.id}_ = span;")
            self.append_("span.clear();")

        if padded_size:
            self.append_(f"span = remaining_span;")
            self.append_("}")

    def parse_array_field_full_(self, field: ast.ArrayField):
        """Parse the selected array field.
        This function does not attempt to parse all elements but just to
        identify the span of the array."""
        array_size = core.get_array_field_size(field)
        element_width = core.get_array_element_size(field)
        element_type = field.type_id or get_cxx_scalar_type(field.width)
        padded_size = field.padded_size

        if element_width:
            element_width = int(element_width / 8)

        if isinstance(array_size, int):
            size = None
            count = array_size
        elif isinstance(array_size, ast.SizeField):
            size = f'{field.id}_size'
            count = None
        elif isinstance(array_size, ast.CountField):
            size = None
            count = f'{field.id}_count'
        else:
            size = None
            count = None

        # Shift the span to reset the offset to 0.
        self.check_code_()

        # Apply the size modifier.
        if field.size_modifier and size:
            self.append_(f"{size} = {size} - {field.size_modifier};")

        # Compute the array size if the count and element width are known.
        if count is not None and element_width is not None:
            size = f"{count} * {element_width}"

        # Parse from the padded array if padding is present.
        if padded_size:
            self.check_size_(padded_size)
            self.append_("{")
            self.append_(
                f"pdl::packet::slice remaining_span = span.subrange({padded_size}, span.size() - {padded_size});")
            self.append_(f"span = span.subrange(0, {padded_size});")

        # The array size is known in bytes.
        if size is not None:
            self.check_size_(size)
            self.append_("{")
            self.append_(f"pdl::packet::slice temp_span = span.subrange(0, {size});")
            self.append_(f"span.skip({size});")
            self.append_(f"while (temp_span.size() > 0) {{")
            if field.width:
                element_size = int(field.width / 8)
                self.append_(f"    if (temp_span.size() < {element_size}) {{")
                self.append_(f"        return false;")
                self.append_("    }")
                self.append_(f"    {field.id}_.push_back(temp_span.read<{element_type}, {element_size}>());")
            elif isinstance(field.type, ast.EnumDeclaration):
                backing_type = get_cxx_scalar_type(field.type.width)
                element_size = int(field.type.width / 8)
                self.append_(f"    if (temp_span.size() < {element_size}) {{")
                self.append_(f"        return false;")
                self.append_("    }")
                self.append_(
                    f"    {field.id}_.push_back({element_type}(temp_span.read<{backing_type}, {element_size}>()));")
            else:
                self.append_(f"    {element_type} element;")
                self.append_(f"    if (!{element_type}::Parse(temp_span, &element)) {{")
                self.append_(f"        return false;")
                self.append_("    }")
                self.append_(f"    {field.id}_.emplace_back(std::move(element));")
            self.append_("}")
            self.append_("}")

        # The array count is known. The element width is dynamic.
        # Parse each element iteratively and derive the array span.
        elif count is not None:
            self.append_(f"for (size_t n = 0; n < {count}; n++) {{")
            self.append_(f"    {element_type} element;")
            self.append_(f"    if (!{field.type_id}::Parse(span, &element)) {{")
            self.append_("        return false;")
            self.append_("    }")
            self.append_(f"    {field.id}_.emplace_back(std::move(element));")
            self.append_("}")

        # The array size is not known, assume the array takes the
        # full remaining space. TODO support having fixed sized fields
        # following the array.
        elif field.width:
            element_size = int(field.width / 8)
            self.append_(f"while (span.size() > 0) {{")
            self.append_(f"    if (span.size() < {element_size}) {{")
            self.append_(f"        return false;")
            self.append_("    }")
            self.append_(f"    {field.id}_.push_back(span.read<{element_type}, {element_size}>());")
            self.append_("}")
        elif isinstance(field.type, ast.EnumDeclaration):
            element_size = int(field.type.width / 8)
            backing_type = get_cxx_scalar_type(field.type.width)
            self.append_(f"while (span.size() > 0) {{")
            self.append_(f"    if (span.size() < {element_size}) {{")
            self.append_(f"        return false;")
            self.append_("    }")
            self.append_(f"    {field.id}_.push_back({element_type}(span.read<{backing_type}, {element_size}>()));")
            self.append_("}")
        else:
            self.append_(f"while (span.size() > 0) {{")
            self.append_(f"    {element_type} element;")
            self.append_(f"    if (!{element_type}::Parse(span, &element)) {{")
            self.append_(f"        return false;")
            self.append_("    }")
            self.append_(f"    {field.id}_.emplace_back(std::move(element));")
            self.append_("}")

        if padded_size:
            self.append_(f"span = remaining_span;")
            self.append_("}")

    def parse_payload_field_lite_(self, field: Union[ast.BodyField, ast.PayloadField]):
        """Parse body and payload fields."""
        if self.shift != 0:
            raise Exception('Payload field does not start on an octet boundary')

        payload_size = core.get_payload_field_size(field)
        offset_from_end = core.get_field_offset_from_end(field)
        self.check_code_()

        if payload_size and getattr(field, 'size_modifier', None):
            self.append_(f"{field.id}_size -= {field.size_modifier};")

        # The payload or body has a known size.
        # Consume the payload and update the span in case
        # fields are placed after the payload.
        if payload_size:
            self.check_size_(f"{field.id}_size")
            self.append_(f"payload_ = span.subrange(0, {field.id}_size);")
            self.append_(f"span.skip({field.id}_size);")
        # The payload or body is the last field of a packet,
        # consume the remaining span.
        elif offset_from_end == 0:
            self.append_(f"payload_ = span;")
            self.append_(f"span.clear();")
        # The payload or body is followed by fields of static size.
        # Consume the span that is not reserved for the following fields.
        elif offset_from_end:
            if (offset_from_end % 8) != 0:
                raise Exception('Payload field offset from end of packet is not a multiple of 8')
            offset_from_end = int(offset_from_end / 8)
            self.check_size_(f'{offset_from_end}')
            self.append_(f"payload_ = span.subrange(0, span.size() - {offset_from_end});")
            self.append_(f"span.skip(payload_.size());")

    def parse_payload_field_full_(self, field: Union[ast.BodyField, ast.PayloadField]):
        """Parse body and payload fields."""
        if self.shift != 0:
            raise Exception('Payload field does not start on an octet boundary')

        payload_size = core.get_payload_field_size(field)
        offset_from_end = core.get_field_offset_from_end(field)
        self.check_code_()

        if payload_size and getattr(field, 'size_modifier', None):
            self.append_(f"{field.id}_size -= {field.size_modifier};")

        # The payload or body has a known size.
        # Consume the payload and update the span in case
        # fields are placed after the payload.
        if payload_size:
            self.check_size_(f"{field.id}_size")
            self.append_(f"for (size_t n = 0; n < {field.id}_size; n++) {{")
            self.append_("    payload_.push_back(span.read<uint8_t>();")
            self.append_("}")
        # The payload or body is the last field of a packet,
        # consume the remaining span.
        elif offset_from_end == 0:
            self.append_("while (span.size() > 0) {")
            self.append_("    payload_.push_back(span.read<uint8_t>();")
            self.append_("}")
        # The payload or body is followed by fields of static size.
        # Consume the span that is not reserved for the following fields.
        elif offset_from_end is not None:
            if (offset_from_end % 8) != 0:
                raise Exception('Payload field offset from end of packet is not a multiple of 8')
            offset_from_end = int(offset_from_end / 8)
            self.check_size_(f'{offset_from_end}')
            self.append_(f"while (span.size() > {offset_from_end}) {{")
            self.append_("    payload_.push_back(span.read<uint8_t>();")
            self.append_("}")

    def parse(self, field: ast.Field):
        # Field has bit granularity.
        # Append the field to the current chunk,
        # check if a byte boundary was reached.
        if core.is_bit_field(field):
            self.parse_bit_field_(field)

        # Padding fields.
        elif isinstance(field, ast.PaddingField):
            pass

        # Array fields.
        elif isinstance(field, ast.ArrayField) and self.extract_arrays:
            self.parse_array_field_full_(field)

        elif isinstance(field, ast.ArrayField) and not self.extract_arrays:
            self.parse_array_field_lite_(field)

        # Other typedef fields.
        elif isinstance(field, ast.TypedefField):
            self.parse_typedef_field_(field)

        # Payload and body fields.
        elif isinstance(field, (ast.PayloadField, ast.BodyField)) and self.extract_arrays:
            self.parse_payload_field_full_(field)

        elif isinstance(field, (ast.PayloadField, ast.BodyField)) and not self.extract_arrays:
            self.parse_payload_field_lite_(field)

        else:
            raise Exception(f'Unsupported field type {field.kind}')

    def done(self):
        self.check_code_()


def generate_enum_declaration(decl: ast.EnumDeclaration) -> str:
    """Generate the implementation of an enum type."""

    enum_name = decl.id
    enum_type = get_cxx_scalar_type(decl.width)
    tag_decls = []
    for t in decl.tags:
        tag_decls.append(f"{t.id} = {hex(t.value)},")

    return dedent("""\

        enum class {enum_name} : {enum_type} {{
            {tag_decls}
        }};
        """).format(enum_name=enum_name, enum_type=enum_type, tag_decls=indent(tag_decls, 1))


def generate_enum_to_text(decl: ast.EnumDeclaration) -> str:
    """Generate the helper function that will convert an enum tag to string."""

    enum_name = decl.id
    tag_cases = []
    for t in decl.tags:
        tag_cases.append(f"case {enum_name}::{t.id}: return \"{t.id}\";")

    return dedent("""\

        std::string {enum_name}Text({enum_name} tag) {{
            switch (tag) {{
                {tag_cases}
                default:
                    return std::string("Unknown {enum_name}: " +
                           std::to_string(static_cast<uint64_t>(tag)));
            }}
        }}
        """).format(enum_name=enum_name, tag_cases=indent(tag_cases, 2))


def generate_packet_field_members(packet: ast.Declaration) -> List[str]:
    """Return the declaration of fields that are backed in the view
    class declaration."""

    fields = core.get_unconstrained_parent_fields(packet) + packet.fields
    members = []
    for field in fields:
        if isinstance(field, (ast.PayloadField, ast.BodyField)):
            members.append("pdl::packet::slice payload_;")
        elif (isinstance(field, ast.ArrayField) and isinstance(packet, ast.PacketDeclaration)):
            members.append(f"pdl::packet::slice {field.id}_;")
        elif (isinstance(field, ast.ArrayField) and isinstance(packet, ast.StructDeclaration)):
            element_type = field.type_id or get_cxx_scalar_type(field.width)
            members.append(f"std::vector<{element_type}> {field.id}_;")
        elif isinstance(field, ast.ScalarField):
            members.append(f"{get_cxx_scalar_type(field.width)} {field.id}_;")
        elif isinstance(field, ast.TypedefField):
            members.append(f"{field.type_id} {field.id}_;")

    return members


def generate_scalar_array_field_accessor(field: ast.ArrayField) -> str:
    """Parse the selected scalar array field."""
    element_size = int(field.width / 8)
    backing_type = get_cxx_scalar_type(field.width)
    return dedent("""\
        pdl::packet::slice span = {field_id}_;
        std::vector<{backing_type}> elements;
        while (span.size() >= {element_size}) {{
            elements.push_back(span.read<{backing_type}, {element_size}>());
        }}
        return std::move(elements);""").format(field_id=field.id, backing_type=backing_type, element_size=element_size)


def generate_enum_array_field_accessor(field: ast.ArrayField) -> str:
    """Parse the selected enum array field."""
    element_size = int(field.type.width / 8)
    backing_type = get_cxx_scalar_type(field.type.width)
    return dedent("""\
        pdl::packet::slice span = {field_id}_;
        std::vector<{enum_type}> elements;
        while (span.size() >= {element_size}) {{
            elements.push_back({enum_type}(span.read<{backing_type}, {element_size}>()));
        }}
        return std::move(elements);""").format(field_id=field.id,
                                               enum_type=field.type_id,
                                               backing_type=backing_type,
                                               element_size=element_size)


def generate_typedef_array_field_accessor(field: ast.ArrayField) -> str:
    """Parse the selected typedef array field."""
    return dedent("""\
        pdl::packet::slice span = {field_id}_;
        std::vector<{struct_type}> elements;
        for (;;) {{
            {struct_type} element;
            if (!{struct_type}::Parse(span, &element)) {{
                break;
            }}
            elements.emplace_back(std::move(element));
        }}
        return std::move(elements);""").format(field_id=field.id, struct_type=field.type_id)


def generate_array_field_accessor(field: ast.ArrayField):
    """Parse the selected array field."""

    if field.width is not None:
        return generate_scalar_array_field_accessor(field)
    elif isinstance(field.type, ast.EnumDeclaration):
        return generate_enum_array_field_accessor(field)
    else:
        return generate_typedef_array_field_accessor(field)


def generate_packet_view_field_accessors(packet: ast.PacketDeclaration) -> List[str]:
    """Return the declaration of accessors for fields that are backed
    in the view class declaration."""

    fields = core.get_unconstrained_parent_fields(packet) + packet.fields
    accessors = []
    for field in fields:
        if isinstance(field, (ast.PayloadField, ast.BodyField)):
            accessors.append(
                dedent("""\
                pdl::packet::slice GetPayload() const {
                    ASSERT(valid_);
                    return payload_;
                }

                """))
        elif isinstance(field, ast.ArrayField):
            element_type = field.type_id or get_cxx_scalar_type(field.width)
            accessor_name = to_pascal_case(field.id)
            accessors.append(
                dedent("""\
                std::vector<{element_type}> Get{accessor_name}() const {{
                    ASSERT(valid_);
                    {accessor}
                }}

                """).format(element_type=element_type,
                            accessor_name=accessor_name,
                            accessor=indent_block(generate_array_field_accessor(field), 1)))
        elif isinstance(field, ast.ScalarField):
            field_type = get_cxx_scalar_type(field.width)
            accessor_name = to_pascal_case(field.id)
            accessors.append(
                dedent("""\
                {field_type} Get{accessor_name}() const {{
                    ASSERT(valid_);
                    return {member_name}_;
                }}

                """).format(field_type=field_type, accessor_name=accessor_name, member_name=field.id))
        elif isinstance(field, ast.TypedefField):
            field_qualifier = "" if isinstance(field.type, ast.EnumDeclaration) else " const&"
            accessor_name = to_pascal_case(field.id)
            accessors.append(
                dedent("""\
                {field_type}{field_qualifier} Get{accessor_name}() const {{
                    ASSERT(valid_);
                    return {member_name}_;
                }}

                """).format(field_type=field.type_id,
                            field_qualifier=field_qualifier,
                            accessor_name=accessor_name,
                            member_name=field.id))

    # Add accessors for constrained parent fields.
    # The accessors return a constant value in this case.
    for c in core.get_parent_constraints(packet):
        field = core.get_packet_field(packet, c.id)
        if isinstance(field, ast.ScalarField):
            field_type = get_cxx_scalar_type(field.width)
            accessor_name = to_pascal_case(field.id)
            accessors.append(
                dedent("""\
                {field_type} Get{accessor_name}() const {{
                    return {value};
                }}

                """).format(field_type=field_type, accessor_name=accessor_name, value=c.value))
        else:
            accessor_name = to_pascal_case(field.id)
            accessors.append(
                dedent("""\
                {field_type} Get{accessor_name}() const {{
                    return {field_type}::{tag_id};
                }}

                """).format(field_type=field.type_id, accessor_name=accessor_name, tag_id=c.tag_id))

    return "".join(accessors)


def generate_packet_stringifier(packet: ast.PacketDeclaration) -> str:
    return dedent("""\
        std::string ToString() const {
            return "";
        }
        """)


def generate_packet_view_field_parsers(packet: ast.PacketDeclaration) -> str:
    """Generate the packet parser. The validator will extract
    the fields it can in a pre-parsing phase. """

    code = []

    # Generate code to check the validity of the parent,
    # and import parent fields that do not have a fixed value in the
    # current packet.
    if packet.parent:
        code.append(
            dedent("""\
            // Check validity of parent packet.
            if (!parent.IsValid()) {
                return false;
            }
            """))
        parent_fields = core.get_unconstrained_parent_fields(packet)
        if parent_fields:
            code.append("// Copy parent field values.")
            for f in parent_fields:
                code.append(f"{f.id}_ = parent.{f.id}_;")
            code.append("")
        span = "parent.payload_"
    else:
        span = "parent"

    # Validate parent constraints.
    for c in packet.constraints:
        if c.tag_id:
            enum_type = core.get_packet_field(packet.parent, c.id).type_id
            code.append(
                dedent("""\
                if (parent.{field_id}_ != {enum_type}::{tag_id}) {{
                    return false;
                }}
                """).format(field_id=c.id, enum_type=enum_type, tag_id=c.tag_id))
        else:
            code.append(
                dedent("""\
                if (parent.{field_id}_ != {value}) {{
                    return false;
                }}
                """).format(field_id=c.id, value=c.value))

    # Parse fields linearly.
    if packet.fields:
        code.append("// Parse packet field values.")
        code.append(f"pdl::packet::slice span = {span};")
        for f in packet.fields:
            if isinstance(f, ast.SizeField):
                code.append(f"{get_cxx_scalar_type(f.width)} {f.field_id}_size;")
            elif isinstance(f, (ast.SizeField, ast.CountField)):
                code.append(f"{get_cxx_scalar_type(f.width)} {f.field_id}_count;")
        parser = FieldParser(extract_arrays=False)
        for f in packet.fields:
            parser.parse(f)
        parser.done()
        code.extend(parser.code)

    code.append("return true;")
    return '\n'.join(code)


def generate_packet_view_friend_classes(packet: ast.PacketDeclaration) -> str:
    """Generate the list of friend declarations for a packet.
    These are the direct children of the class."""

    return [f"friend class {decl.id}View;" for (_, decl) in core.get_derived_packets(packet, traverse=False)]


def generate_packet_view(packet: ast.PacketDeclaration) -> str:
    """Generate the implementation of the View class for a
    packet declaration."""

    parent_class = f"{packet.parent.id}View" if packet.parent else "pdl::packet::slice"
    field_accessors = generate_packet_view_field_accessors(packet)
    field_members = generate_packet_field_members(packet)
    field_parsers = generate_packet_view_field_parsers(packet)
    friend_classes = generate_packet_view_friend_classes(packet)
    stringifier = generate_packet_stringifier(packet)

    return dedent("""\

        class {packet_name}View {{
        public:
            static {packet_name}View Create({parent_class} const& parent) {{
                return {packet_name}View(parent);
            }}

            {field_accessors}
            {stringifier}

            bool IsValid() const {{
                return valid_;
            }}

        protected:
            explicit {packet_name}View({parent_class} const& parent) {{
                valid_ = Parse(parent);
            }}

            bool Parse({parent_class} const& parent) {{
                {field_parsers}
            }}

            bool valid_{{false}};
            {field_members}

            {friend_classes}
        }};
        """).format(packet_name=packet.id,
                    parent_class=parent_class,
                    field_accessors=indent_block(field_accessors, 1),
                    field_members=indent(field_members, 1),
                    field_parsers=indent_block(field_parsers, 2),
                    friend_classes=indent(friend_classes, 1),
                    stringifier=indent_block(stringifier, 1))


def generate_packet_builder(packet: ast.PacketDeclaration) -> str:
    """Generate the implementation of the Builder class for a
    packet declaration."""

    return ""


def generate_struct_constructor(struct: ast.StructDeclaration) -> str:
    """Generate the implementation of the constructor for a
    struct declaration."""

    constructor_params = []
    constructor_initializers = []

    for field in struct.fields:
        if isinstance(field, (ast.PayloadField, ast.BodyField)):
            constructor_params.append("std::vector<uint8_t> payload")
            constructor_initializers.append("payload_(std::move(payload))")
        elif isinstance(field, ast.ArrayField):
            element_type = field.type_id or get_cxx_scalar_type(field.width)
            constructor_params.append(f"std::vector<{element_type}> {field.id}")
            constructor_initializers.append(f"{field.id}_(std::move({field.id}))")
        elif isinstance(field, ast.ScalarField):
            backing_type = get_cxx_scalar_type(field.width)
            constructor_params.append(f"{backing_type} {field.id}")
            constructor_initializers.append(f"{field.id}_({field.id})")
        elif (isinstance(field, ast.TypedefField) and isinstance(field.type, ast.EnumDeclaration)):
            constructor_params.append(f"{field.type_id} {field.id}")
            constructor_initializers.append(f"{field.id}_({field.id})")
        elif isinstance(field, ast.TypedefField):
            constructor_params.append(f"{field.type_id} {field.id}")
            constructor_initializers.append(f"{field.id}_(std::move({field.id}))")

    if not constructor_params:
        return ""

    constructor_params = ', '.join(constructor_params)
    constructor_initializers = ', '.join(constructor_initializers)

    return dedent("""\
        {struct_id}({constructor_params})
            : {constructor_initializers} {{}}""").format(struct_id=struct.id,
                                                         constructor_params=constructor_params,
                                                         constructor_initializers=constructor_initializers)


def generate_struct_field_parsers(struct: ast.StructDeclaration) -> str:
    """Generate the struct parser. The validator will extract
    the fields it can in a pre-parsing phase. """

    code = []
    parsed_fields = []
    post_processing = []

    for field in struct.fields:
        if isinstance(field, (ast.PayloadField, ast.BodyField)):
            code.append("std::vector<uint8_t> payload_;")
            parsed_fields.append("std::move(payload_)")
        elif isinstance(field, ast.ArrayField):
            element_type = field.type_id or get_cxx_scalar_type(field.width)
            code.append(f"std::vector<{element_type}> {field.id}_;")
            parsed_fields.append(f"std::move({field.id}_)")
        elif isinstance(field, ast.ScalarField):
            backing_type = get_cxx_scalar_type(field.width)
            code.append(f"{backing_type} {field.id}_;")
            parsed_fields.append(f"{field.id}_")
        elif (isinstance(field, ast.TypedefField) and isinstance(field.type, ast.EnumDeclaration)):
            code.append(f"{field.type_id} {field.id}_;")
            parsed_fields.append(f"{field.id}_")
        elif isinstance(field, ast.TypedefField):
            code.append(f"{field.type_id} {field.id}_;")
            parsed_fields.append(f"std::move({field.id}_)")
        elif isinstance(field, ast.SizeField):
            code.append(f"{get_cxx_scalar_type(field.width)} {field.field_id}_size;")
        elif isinstance(field, ast.CountField):
            code.append(f"{get_cxx_scalar_type(field.width)} {field.field_id}_count;")

    parser = FieldParser(extract_arrays=True)
    for f in struct.fields:
        parser.parse(f)
    parser.done()
    code.extend(parser.code)

    parsed_fields = ', '.join(parsed_fields)
    code.append(f"*output = {struct.id}({parsed_fields});")
    code.append("return true;")
    return '\n'.join(code)


def generate_struct_declaration(struct: ast.StructDeclaration) -> str:
    """Generate the implementation of the class for a
    struct declaration."""

    if struct.parent:
        raise Exception("Struct declaration with parents are not supported")

    struct_constructor = generate_struct_constructor(struct)
    field_members = generate_packet_field_members(struct)
    field_parsers = generate_struct_field_parsers(struct)
    stringifier = generate_packet_stringifier(struct)

    return dedent("""\

        class {struct_name} {{
        public:
            {struct_name}() = default;
            {struct_name}({struct_name} const&) = default;
            {struct_name}({struct_name}&&) = default;
            {struct_name}& operator=({struct_name} const&) = default;
            {struct_constructor}

            static bool Parse(pdl::packet::slice& span, {struct_name}* output) {{
                {field_parsers}
            }}

            {stringifier}

            {field_members}
        }};
        """).format(struct_name=struct.id,
                    struct_constructor=struct_constructor,
                    field_members=indent(field_members, 1),
                    field_parsers=indent_block(field_parsers, 2),
                    stringifier=indent_block(stringifier, 1))


def run(input: argparse.FileType, output: argparse.FileType, custom_type_headers: List[str],
        custom_type_namespace: Optional[str], namespace: Optional[str]):

    file = ast.File.from_json(json.load(input))
    core.desugar(file)

    # Big endian not supported.
    assert file.endianness.value == 'little_endian'

    additional_includes = '\n'.join([f"#include <{header}>" for header in custom_type_headers])
    using_namespace = f"using {custom_type_namespace};" if custom_type_namespace else ""
    open_namespace = f"namespace {namespace} {{" if namespace else ""
    close_namespace = f"}}  // {namespace}" if namespace else ""

    # Disable unsupported features in the canonical test suite.
    skipped_decls = [
        'Packet_Custom_Field_ConstantSize',
        'Packet_Custom_Field_VariableSize',
        'Packet_Checksum_Field_FromStart',
        'Packet_Checksum_Field_FromEnd',
        'Struct_Custom_Field_ConstantSize',
        'Struct_Custom_Field_VariableSize',
        'Struct_Checksum_Field_FromStart',
        'Struct_Checksum_Field_FromEnd',
        'Struct_Custom_Field_ConstantSize_',
        'Struct_Custom_Field_VariableSize_',
        'Struct_Checksum_Field_FromStart_',
        'Struct_Checksum_Field_FromEnd_',
        'PartialParent5',
        'PartialChild5_A',
        'PartialChild5_B',
        'PartialParent12',
        'PartialChild12_A',
        'PartialChild12_B',
    ]

    output.write(
        dedent("""\
        // File generated from {input_name}, with the command:
        //  {input_command}
        // /!\\ Do not edit by hand

        #pragma once

        #include <cstdint>
        #include <string>
        #include <packet_runtime.h>
        {additional_includes}

        #ifndef ASSERT
        #include <cassert>
        #define ASSERT assert
        #endif  // !ASSERT

        {using_namespace}

        {open_namespace}
        """).format(input_name=input.name,
                    input_command=' '.join(sys.argv),
                    additional_includes=additional_includes,
                    using_namespace=using_namespace,
                    open_namespace=open_namespace))

    for d in file.declarations:
        if d.id in skipped_decls:
            continue

        if isinstance(d, ast.EnumDeclaration):
            output.write(generate_enum_declaration(d))
            output.write(generate_enum_to_text(d))
        elif isinstance(d, ast.PacketDeclaration):
            output.write(generate_packet_view(d))
            output.write(generate_packet_builder(d))
        elif isinstance(d, ast.StructDeclaration):
            output.write(generate_struct_declaration(d))

    output.write(f"{close_namespace}\n")


def main() -> int:
    """Generate cxx PDL backend."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', type=argparse.FileType('r'), default=sys.stdin, help='Input PDL-JSON source')
    parser.add_argument('--output', type=argparse.FileType('w'), default=sys.stdout, help='Output C++ file')
    parser.add_argument('--namespace', type=str, help='Generated module namespace')
    parser.add_argument('--custom-type-headers',
                        type=str,
                        default=[],
                        action='append',
                        help='Declaration headers of custom types')
    parser.add_argument('--custom-type-namespace', type=str, help='Declaration namespace of custom types')
    return run(**vars(parser.parse_args()))


if __name__ == '__main__':
    sys.exit(main())
