# File generated from <stdin>, with the command:
#  external/rust/android-crates-io/crates/pdl-compiler/scripts/generate_python_backend.py
# /!\ Do not edit by hand.
from dataclasses import dataclass, field, fields
from typing import Optional, List, Tuple, Union
import enum
import inspect
import math


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


class PacketType(enum.IntEnum):
    SINGLE_PACKET = 0x0
    START_PACKET = 0x1
    CONTINUE_PACKET = 0x2
    END_PACKET = 0x3

    @staticmethod
    def from_int(v: int) -> Union[int, "PacketType"]:
        try:
            return PacketType(v)
        except ValueError as exn:
            raise exn


class MessageType(enum.IntEnum):
    COMMAND = 0x0
    RESPONSE = 0x1

    @staticmethod
    def from_int(v: int) -> Union[int, "MessageType"]:
        try:
            return MessageType(v)
        except ValueError as exn:
            raise exn


@dataclass
class AvctpSinglePacket(Packet):
    transaction_label: int = 0
    packet_type: PacketType = PacketType.SINGLE_PACKET
    cr: MessageType = MessageType.COMMAND
    ipid: int = 0
    pid: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["AvctpSinglePacket", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        byte0 = span[0]
        transaction_label = (byte0 >> 4) & 0x0F
        packet_type_val = (byte0 >> 2) & 0x03
        cr_val = (byte0 >> 1) & 0x01
        ipid = byte0 & 0x01

        pid = (span[1] << 8) | span[2]

        packet_type = PacketType.from_int(packet_type_val)
        cr = MessageType.from_int(cr_val)

        return AvctpSinglePacket(
            transaction_label=transaction_label,
            packet_type=packet_type,
            cr=cr,
            ipid=ipid,
            pid=pid,
            payload=bytes(span[3:]),
        ), bytes()

    def serialize(self) -> bytes:
        byte0 = (
            ((self.transaction_label & 0x0F) << 4) |
            ((int(self.packet_type) & 0x03) << 2) |
            ((int(self.cr) & 0x01) << 1) |
            (self.ipid & 0x01)
        )
        return bytes([byte0, (self.pid >> 8) & 0xFF, self.pid & 0xFF]) + self.payload

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class AvctpStartPacket(Packet):
    transaction_label: int = 0
    packet_type: PacketType = PacketType.START_PACKET
    cr: MessageType = MessageType.COMMAND
    ipid: int = 0
    number_of_packets: int = 0
    pid: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["AvctpStartPacket", bytes]:
        if len(span) < 4:
            raise Exception("Packet too short")

        byte0 = span[0]
        transaction_label = (byte0 >> 4) & 0x0F
        packet_type_val = (byte0 >> 2) & 0x03
        cr_val = (byte0 >> 1) & 0x01
        ipid = byte0 & 0x01

        number_of_packets = span[1]
        pid = (span[2] << 8) | span[3]

        packet_type = PacketType.from_int(packet_type_val)
        cr = MessageType.from_int(cr_val)

        return AvctpStartPacket(
            transaction_label=transaction_label,
            packet_type=packet_type,
            cr=cr,
            ipid=ipid,
            number_of_packets=number_of_packets,
            pid=pid,
            payload=bytes(span[4:]),
        ), bytes()

    def serialize(self) -> bytes:
        byte0 = (
            ((self.transaction_label & 0x0F) << 4) |
            ((int(self.packet_type) & 0x03) << 2) |
            ((int(self.cr) & 0x01) << 1) |
            (self.ipid & 0x01)
        )
        return bytes([
            byte0,
            self.number_of_packets & 0xFF,
            (self.pid >> 8) & 0xFF,
            self.pid & 0xFF
        ]) + self.payload

    @property
    def size(self) -> int:
        return 4 + len(self.payload)


@dataclass
class AvctpContinuePacket(Packet):
    transaction_label: int = 0
    packet_type: PacketType = PacketType.CONTINUE_PACKET
    cr: MessageType = MessageType.COMMAND
    ipid: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["AvctpContinuePacket", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        byte0 = span[0]
        transaction_label = (byte0 >> 4) & 0x0F
        packet_type_val = (byte0 >> 2) & 0x03
        cr_val = (byte0 >> 1) & 0x01
        ipid = byte0 & 0x01

        packet_type = PacketType.from_int(packet_type_val)
        cr = MessageType.from_int(cr_val)

        return AvctpContinuePacket(
            transaction_label=transaction_label,
            packet_type=packet_type,
            cr=cr,
            ipid=ipid,
            payload=bytes(span[1:]),
        ), bytes()

    def serialize(self) -> bytes:
        byte0 = (
            ((self.transaction_label & 0x0F) << 4) |
            ((int(self.packet_type) & 0x03) << 2) |
            ((int(self.cr) & 0x01) << 1) |
            (self.ipid & 0x01)
        )
        return bytes([byte0]) + self.payload

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


def parse_avctp(span: bytes) -> Tuple[Packet, bytes]:
    """Parse an AVCTP packet based on its packet type."""
    if len(span) < 1:
        raise Exception("Packet too short")

    byte0 = span[0]
    packet_type_val = (byte0 >> 2) & 0x03

    if packet_type_val == PacketType.SINGLE_PACKET:
        return AvctpSinglePacket.parse(span)
    elif packet_type_val == PacketType.START_PACKET:
        return AvctpStartPacket.parse(span)
    else:
        return AvctpContinuePacket.parse(span)
