from dataclasses import dataclass, field
from typing import Optional, List, Dict


@dataclass
class SourceLocation:
    offset: int
    line: int
    column: int


@dataclass
class SourceRange:
    file: int
    start: SourceLocation
    end: SourceLocation


@dataclass
class Node:
    kind: str
    loc: SourceLocation


@dataclass
class Tag(Node):
    id: str
    value: int


@dataclass
class Constraint(Node):
    id: str
    value: Optional[int]
    tag_id: Optional[str]


@dataclass
class Field(Node):
    parent: Node = field(init=False)


@dataclass
class ChecksumField(Field):
    field_id: str


@dataclass
class PaddingField(Field):
    width: int


@dataclass
class SizeField(Field):
    field_id: str
    width: int


@dataclass
class CountField(Field):
    field_id: str
    width: int


@dataclass
class BodyField(Field):
    id: str = field(init=False, default='_body_')


@dataclass
class PayloadField(Field):
    size_modifier: Optional[str]
    id: str = field(init=False, default='_payload_')


@dataclass
class FixedField(Field):
    width: Optional[int] = None
    value: Optional[int] = None
    enum_id: Optional[str] = None
    tag_id: Optional[str] = None

    @property
    def type(self) -> Optional['Declaration']:
        return self.parent.grammar.typedef_scope[self.enum_id] if self.enum_id else None


@dataclass
class ReservedField(Field):
    width: int


@dataclass
class ArrayField(Field):
    id: str
    width: Optional[int]
    type_id: Optional[str]
    size_modifier: Optional[str]
    size: Optional[int]

    @property
    def type(self) -> Optional['Declaration']:
        return self.parent.grammar.typedef_scope[self.type_id] if self.type_id else None


@dataclass
class ScalarField(Field):
    id: str
    width: int


@dataclass
class TypedefField(Field):
    id: str
    type_id: str

    @property
    def type(self) -> 'Declaration':
        return self.parent.grammar.typedef_scope[self.type_id]


@dataclass
class GroupField(Field):
    group_id: str
    constraints: List[Constraint]


@dataclass
class Declaration(Node):
    grammar: 'Grammar' = field(init=False)

    def __post_init__(self):
        if hasattr(self, 'fields'):
            for f in self.fields:
                f.parent = self


@dataclass
class EndiannessDeclaration(Node):
    value: str


@dataclass
class ChecksumDeclaration(Declaration):
    id: str
    function: str
    width: int


@dataclass
class CustomFieldDeclaration(Declaration):
    id: str
    function: str
    width: Optional[int]


@dataclass
class EnumDeclaration(Declaration):
    id: str
    tags: List[Tag]
    width: int


@dataclass
class PacketDeclaration(Declaration):
    id: str
    parent_id: Optional[str]
    constraints: List[Constraint]
    fields: List[Field]

    @property
    def parent(self) -> Optional['PacketDeclaration']:
        return self.grammar.packet_scope[self.parent_id] if self.parent_id else None


@dataclass
class StructDeclaration(Declaration):
    id: str
    parent_id: Optional[str]
    constraints: List[Constraint]
    fields: List[Field]

    @property
    def parent(self) -> Optional['StructDeclaration']:
        return self.grammar.typedef_scope[self.parent_id] if self.parent_id else None


@dataclass
class GroupDeclaration(Declaration):
    id: str
    fields: List[Field]


@dataclass
class Grammar:
    endianness: EndiannessDeclaration
    declarations: List[Declaration]
    packet_scope: Dict[str, Declaration] = field(init=False)
    typedef_scope: Dict[str, Declaration] = field(init=False)
    group_scope: Dict[str, Declaration] = field(init=False)

    def __post_init__(self):
        self.packet_scope = dict()
        self.typedef_scope = dict()
        self.group_scope = dict()

        # Construct the toplevel declaration scopes.
        for d in self.declarations:
            d.grammar = self
            if isinstance(d, PacketDeclaration):
                self.packet_scope[d.id] = d
            elif isinstance(d, GroupDeclaration):
                self.group_scope[d.id] = d
            else:
                self.typedef_scope[d.id] = d

    @staticmethod
    def from_json(obj: object) -> 'Grammar':
        """Import a Grammar exported as JSON object by the PDL parser."""
        endianness = convert_(obj['endianness'])
        declarations = convert_(obj['declarations'])
        return Grammar(endianness, declarations)

    @property
    def byteorder(self) -> str:
        return 'little' if self.endianness.value == 'little_endian' else 'big'


def convert_(obj: object) -> object:
    if obj is None:
        return None
    if isinstance(obj, int) or isinstance(obj, str):
        return obj
    if isinstance(obj, list):
        return [convert_(elt) for elt in obj]
    if isinstance(obj, object):
        kind = obj['kind']
        loc = obj['loc']
        loc = SourceRange(loc['file'], SourceLocation(**loc['start']), SourceLocation(**loc['end']))
        constructor = globals().get(''.join([w.title() for w in kind.split('_')]))
        members = {'loc': loc, 'kind': kind}
        for name, value in obj.items():
            if name != 'kind' and name != 'loc':
                members[name] = convert_(value)
        return constructor(**members)
    raise Exception('Unhandled json object type')
