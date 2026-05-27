# File generated from <stdin>, with the command:
#  external/rust/android-crates-io/crates/pdl-compiler/scripts/generate_python_backend.py
# /!\ Do not edit by hand.
from dataclasses import dataclass, field, fields
from typing import Optional, Tuple, Union
import enum
import inspect


@dataclass
class Packet:
    payload: Optional[bytes] = field(repr=False, default_factory=bytes, compare=False)

    @classmethod
    def parse_all(cls, span: bytes) -> "Packet":
        packet, remain = getattr(cls, "parse")(span)
        if len(remain) > 0:
            raise Exception("Unexpected parsing remainder")
        return packet

    @property
    def size(self) -> int:
        pass

    def show(self, prefix: str = ""):
        print(f"{self.__class__.__name__}")

        def print_val(p: str, pp: str, name: str, align: int, typ, val):
            if name == "payload":
                pass

            elif typ is int:
                print(f"{p}{name:{align}} = {val} (0x{val:x})")

            elif typ is bytes:
                print(f"{p}{name:{align}} = [", end="")
                line = ""
                n_pp = ""
                for idx, b in enumerate(val):
                    if idx > 0 and idx % 8 == 0:
                        print(f"{n_pp}{line}")
                        line = ""
                        n_pp = pp + (" " * (align + 4))
                    line += f" {b:02x}"
                print(f"{n_pp}{line} ]")

            elif inspect.isclass(typ) and issubclass(typ, enum.IntEnum):
                print(f"{p}{name:{align}} = {typ.__name__}::{val.name} (0x{val:x})")

            elif inspect.isclass(typ) and issubclass(typ, globals().get("Packet")):
                print(f"{p}{name:{align}} = ", end="")
                val.show(prefix=pp)

            elif getattr(typ, "__origin__", None) == list:
                print(f"{p}{name:{align}}")
                last = len(val) - 1
                align = 5
                for idx, elt in enumerate(val):
                    n_p = pp + ("├── " if idx != last else "└── ")
                    n_pp = pp + ("│   " if idx != last else "    ")
                    print_val(n_p, n_pp, f"[{idx}]", align, typ.__args__[0], val[idx])

            elif inspect.isclass(typ):
                print(f"{p}{name:{align}} = {repr(val)}")

            else:
                print(f"{p}{name:{align}} = ##{typ}##")

        last = len(fields(self)) - 1
        align = max(len(f.name) for f in fields(self) if f.name != "payload")

        for idx, f in enumerate(fields(self)):
            p = prefix + ("├── " if idx != last else "└── ")
            pp = prefix + ("│   " if idx != last else "    ")
            val = getattr(self, f.name)

            print_val(p, pp, f.name, align, f.type, val)


class FrameType(enum.IntEnum):
    SABM = 0x2F
    UA = 0x63
    DM = 0x0F
    DISC = 0x43
    UIH = 0xEF
    UI = 0x03

    @staticmethod
    def from_int(v: int) -> "FrameType":
        return FrameType(v)


@dataclass
class RfcommShortFrame(Packet):
    ea: int = 0
    cr: int = 0
    dlci: int = 0
    frame_type: int = 0
    length_ea: int = 0
    length: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["RfcommShortFrame", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        # Byte 0: address field (little-endian bit order)
        #   bit 0: ea
        #   bit 1: cr
        #   bits 2-7: dlci
        byte0 = span[0]
        ea = byte0 & 0x01
        cr = (byte0 >> 1) & 0x01
        dlci = (byte0 >> 2) & 0x3F

        # Byte 1: frame_type (8 bits)
        frame_type = span[1]

        # Byte 2: length field (little-endian bit order)
        #   bit 0: length_ea
        #   bits 1-7: length
        byte2 = span[2]
        length_ea = byte2 & 0x01
        length = (byte2 >> 1) & 0x7F

        return RfcommShortFrame(
            ea=ea,
            cr=cr,
            dlci=dlci,
            frame_type=frame_type,
            length_ea=length_ea,
            length=length,
            payload=bytes(span[3:]),
        ), bytes()

    def serialize(self) -> bytes:
        byte0 = (
            (self.ea & 0x01) |
            ((self.cr & 0x01) << 1) |
            ((self.dlci & 0x3F) << 2)
        )
        byte2 = (
            (self.length_ea & 0x01) |
            ((self.length & 0x7F) << 1)
        )
        return bytes([byte0, self.frame_type & 0xFF, byte2]) + self.payload

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class RfcommLongFrame(Packet):
    ea: int = 0
    cr: int = 0
    dlci: int = 0
    frame_type: int = 0
    length_ea: int = 0
    length_low: int = 0
    length_high: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["RfcommLongFrame", bytes]:
        if len(span) < 4:
            raise Exception("Packet too short")

        # Byte 0: address field (little-endian bit order)
        byte0 = span[0]
        ea = byte0 & 0x01
        cr = (byte0 >> 1) & 0x01
        dlci = (byte0 >> 2) & 0x3F

        # Byte 1: frame_type (8 bits)
        frame_type = span[1]

        # Byte 2: first length byte (little-endian bit order)
        #   bit 0: length_ea
        #   bits 1-7: length_low
        byte2 = span[2]
        length_ea = byte2 & 0x01
        length_low = (byte2 >> 1) & 0x7F

        # Byte 3: length_high (8 bits)
        length_high = span[3]

        return RfcommLongFrame(
            ea=ea,
            cr=cr,
            dlci=dlci,
            frame_type=frame_type,
            length_ea=length_ea,
            length_low=length_low,
            length_high=length_high,
            payload=bytes(span[4:]),
        ), bytes()

    def serialize(self) -> bytes:
        byte0 = (
            (self.ea & 0x01) |
            ((self.cr & 0x01) << 1) |
            ((self.dlci & 0x3F) << 2)
        )
        byte2 = (
            (self.length_ea & 0x01) |
            ((self.length_low & 0x7F) << 1)
        )
        return bytes([byte0, self.frame_type & 0xFF, byte2, self.length_high & 0xFF]) + self.payload

    @property
    def size(self) -> int:
        return 4 + len(self.payload)
