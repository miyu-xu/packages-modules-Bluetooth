# File generated from <stdin>, with the command:
#  external/rust/android-crates-io/crates/pdl-compiler/scripts/generate_python_backend.py
# /!\ Do not edit by hand.
from dataclasses import dataclass, field, fields
from typing import Optional, List, Tuple, Union
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
                    print_val(n_p, n_pp, f"[{idx}]", align, typ.__args__[0], elt)

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


class IoCapability(enum.IntEnum):
    DISPLAY_ONLY = 0x00
    DISPLAY_YES_NO = 0x01
    KEYBOARD_ONLY = 0x02
    NO_INPUT_NO_OUTPUT = 0x03
    KEYBOARD_DISPLAY = 0x04

    @staticmethod
    def from_int(v: int) -> Union[int, "IoCapability"]:
        try:
            return IoCapability(v)
        except ValueError as exn:
            raise exn


class OobDataPresent(enum.IntEnum):
    NOT_PRESENT = 0x00
    P192_PRESENT = 0x01
    P256_PRESENT = 0x02
    P192_AND_P256_PRESENT = 0x03

    @staticmethod
    def from_int(v: int) -> Union[int, "OobDataPresent"]:
        try:
            return OobDataPresent(v)
        except ValueError as exn:
            raise exn


class AuthenticationRequirements(enum.IntEnum):
    NO_BONDING_NO_MITM = 0x00
    NO_BONDING_MITM = 0x01
    DEDICATED_BONDING_NO_MITM = 0x02
    DEDICATED_BONDING_MITM = 0x03
    GENERAL_BONDING_NO_MITM = 0x04
    GENERAL_BONDING_MITM = 0x05

    @staticmethod
    def from_int(v: int) -> Union[int, "AuthenticationRequirements"]:
        try:
            return AuthenticationRequirements(v)
        except ValueError as exn:
            raise exn


class LinkKeyType(enum.IntEnum):
    COMBINATION = 0x00
    LOCAL_UNIT = 0x01
    REMOTE_UNIT = 0x02
    DEBUG_COMBINATION = 0x03
    UNAUTHENTICATED_P192 = 0x04
    AUTHENTICATED_P192 = 0x05
    CHANGED_COMBINATION = 0x06
    UNAUTHENTICATED_P256 = 0x07
    AUTHENTICATED_P256 = 0x08

    @staticmethod
    def from_int(v: int) -> Union[int, "LinkKeyType"]:
        try:
            return LinkKeyType(v)
        except ValueError as exn:
            raise exn


class SspStatus(enum.IntEnum):
    SUCCESS = 0x00
    UNKNOWN_CONNECTION_ID = 0x02
    HARDWARE_FAILURE = 0x03
    AUTHENTICATION_FAILURE = 0x05
    PIN_OR_KEY_MISSING = 0x06
    MEMORY_CAPACITY_EXCEEDED = 0x07
    CONNECTION_TIMEOUT = 0x08
    CONNECTION_LIMIT_EXCEEDED = 0x09
    COMMAND_DISALLOWED = 0x0C
    CONNECTION_REJECTED_LIMITED_RESOURCES = 0x0D
    CONNECTION_REJECTED_SECURITY = 0x0E
    CONNECTION_ACCEPT_TIMEOUT = 0x10
    PAIRING_NOT_ALLOWED = 0x18
    REPEATED_ATTEMPTS = 0x17
    UNSPECIFIED_ERROR = 0x1F
    SIMPLE_PAIRING_NOT_SUPPORTED = 0x37

    @staticmethod
    def from_int(v: int) -> Union[int, "SspStatus"]:
        try:
            return SspStatus(v)
        except ValueError as exn:
            raise exn


class KeypressNotificationType(enum.IntEnum):
    PASSKEY_ENTRY_STARTED = 0x00
    PASSKEY_DIGIT_ENTERED = 0x01
    PASSKEY_DIGIT_ERASED = 0x02
    PASSKEY_CLEARED = 0x03
    PASSKEY_ENTRY_COMPLETED = 0x04

    @staticmethod
    def from_int(v: int) -> Union[int, "KeypressNotificationType"]:
        try:
            return KeypressNotificationType(v)
        except ValueError as exn:
            raise exn


# -------------------------------------------------------------------------
# HCI Event parameter structures
# -------------------------------------------------------------------------

@dataclass
class IoCapabilityRequestEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)

    @staticmethod
    def parse(span: bytes) -> Tuple["IoCapabilityRequestEvent", bytes]:
        if len(span) < 6:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])

        return IoCapabilityRequestEvent(
            bd_addr=bd_addr,
            payload=bytes(span[6:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6]) + self.payload

    @property
    def size(self) -> int:
        return 6 + len(self.payload)


@dataclass
class IoCapabilityResponseEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    io_capability: IoCapability = IoCapability.DISPLAY_ONLY
    oob_data_present: OobDataPresent = OobDataPresent.NOT_PRESENT
    authentication_requirements: AuthenticationRequirements = AuthenticationRequirements.NO_BONDING_NO_MITM

    @staticmethod
    def parse(span: bytes) -> Tuple["IoCapabilityResponseEvent", bytes]:
        if len(span) < 9:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        io_capability = IoCapability.from_int(span[6])
        oob_data_present = OobDataPresent.from_int(span[7])
        authentication_requirements = AuthenticationRequirements.from_int(span[8])

        return IoCapabilityResponseEvent(
            bd_addr=bd_addr,
            io_capability=io_capability,
            oob_data_present=oob_data_present,
            authentication_requirements=authentication_requirements,
            payload=bytes(span[9:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + [
            int(self.io_capability),
            int(self.oob_data_present),
            int(self.authentication_requirements),
        ]) + self.payload

    @property
    def size(self) -> int:
        return 9 + len(self.payload)


@dataclass
class UserConfirmationRequestEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    numeric_value: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["UserConfirmationRequestEvent", bytes]:
        if len(span) < 10:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        numeric_value = span[6] | (span[7] << 8) | (span[8] << 16) | (span[9] << 24)

        return UserConfirmationRequestEvent(
            bd_addr=bd_addr,
            numeric_value=numeric_value,
            payload=bytes(span[10:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + [
            self.numeric_value & 0xFF,
            (self.numeric_value >> 8) & 0xFF,
            (self.numeric_value >> 16) & 0xFF,
            (self.numeric_value >> 24) & 0xFF,
        ]) + self.payload

    @property
    def size(self) -> int:
        return 10 + len(self.payload)


@dataclass
class UserPasskeyRequestEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)

    @staticmethod
    def parse(span: bytes) -> Tuple["UserPasskeyRequestEvent", bytes]:
        if len(span) < 6:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])

        return UserPasskeyRequestEvent(
            bd_addr=bd_addr,
            payload=bytes(span[6:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6]) + self.payload

    @property
    def size(self) -> int:
        return 6 + len(self.payload)


@dataclass
class RemoteOobDataRequestEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)

    @staticmethod
    def parse(span: bytes) -> Tuple["RemoteOobDataRequestEvent", bytes]:
        if len(span) < 6:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])

        return RemoteOobDataRequestEvent(
            bd_addr=bd_addr,
            payload=bytes(span[6:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6]) + self.payload

    @property
    def size(self) -> int:
        return 6 + len(self.payload)


@dataclass
class SimplePairingCompleteEvent(Packet):
    status: SspStatus = SspStatus.SUCCESS
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)

    @staticmethod
    def parse(span: bytes) -> Tuple["SimplePairingCompleteEvent", bytes]:
        if len(span) < 7:
            raise Exception("Packet too short")

        status = SspStatus.from_int(span[0])
        bd_addr = list(span[1:7])

        return SimplePairingCompleteEvent(
            status=status,
            bd_addr=bd_addr,
            payload=bytes(span[7:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.status)] + self.bd_addr[:6]) + self.payload

    @property
    def size(self) -> int:
        return 7 + len(self.payload)


@dataclass
class UserPasskeyNotificationEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    passkey: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["UserPasskeyNotificationEvent", bytes]:
        if len(span) < 10:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        passkey = span[6] | (span[7] << 8) | (span[8] << 16) | (span[9] << 24)

        return UserPasskeyNotificationEvent(
            bd_addr=bd_addr,
            passkey=passkey,
            payload=bytes(span[10:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + [
            self.passkey & 0xFF,
            (self.passkey >> 8) & 0xFF,
            (self.passkey >> 16) & 0xFF,
            (self.passkey >> 24) & 0xFF,
        ]) + self.payload

    @property
    def size(self) -> int:
        return 10 + len(self.payload)


@dataclass
class KeypressNotificationEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    notification_type: KeypressNotificationType = KeypressNotificationType.PASSKEY_ENTRY_STARTED

    @staticmethod
    def parse(span: bytes) -> Tuple["KeypressNotificationEvent", bytes]:
        if len(span) < 7:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        notification_type = KeypressNotificationType.from_int(span[6])

        return KeypressNotificationEvent(
            bd_addr=bd_addr,
            notification_type=notification_type,
            payload=bytes(span[7:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + [int(self.notification_type)]) + self.payload

    @property
    def size(self) -> int:
        return 7 + len(self.payload)


@dataclass
class LinkKeyRequestEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)

    @staticmethod
    def parse(span: bytes) -> Tuple["LinkKeyRequestEvent", bytes]:
        if len(span) < 6:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])

        return LinkKeyRequestEvent(
            bd_addr=bd_addr,
            payload=bytes(span[6:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6]) + self.payload

    @property
    def size(self) -> int:
        return 6 + len(self.payload)


@dataclass
class LinkKeyNotificationEvent(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    link_key: List[int] = field(default_factory=lambda: [0] * 16)
    key_type: LinkKeyType = LinkKeyType.COMBINATION

    @staticmethod
    def parse(span: bytes) -> Tuple["LinkKeyNotificationEvent", bytes]:
        if len(span) < 23:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        link_key = list(span[6:22])
        key_type = LinkKeyType.from_int(span[22])

        return LinkKeyNotificationEvent(
            bd_addr=bd_addr,
            link_key=link_key,
            key_type=key_type,
            payload=bytes(span[23:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + self.link_key[:16] + [int(self.key_type)]) + self.payload

    @property
    def size(self) -> int:
        return 23 + len(self.payload)


@dataclass
class IoCapabilityRequestReply(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    io_capability: IoCapability = IoCapability.DISPLAY_ONLY
    oob_data_present: OobDataPresent = OobDataPresent.NOT_PRESENT
    authentication_requirements: AuthenticationRequirements = AuthenticationRequirements.NO_BONDING_NO_MITM

    @staticmethod
    def parse(span: bytes) -> Tuple["IoCapabilityRequestReply", bytes]:
        if len(span) < 9:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        io_capability = IoCapability.from_int(span[6])
        oob_data_present = OobDataPresent.from_int(span[7])
        authentication_requirements = AuthenticationRequirements.from_int(span[8])

        return IoCapabilityRequestReply(
            bd_addr=bd_addr,
            io_capability=io_capability,
            oob_data_present=oob_data_present,
            authentication_requirements=authentication_requirements,
            payload=bytes(span[9:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + [
            int(self.io_capability),
            int(self.oob_data_present),
            int(self.authentication_requirements),
        ]) + self.payload

    @property
    def size(self) -> int:
        return 9 + len(self.payload)


@dataclass
class IoCapabilityRequestNegativeReply(Packet):
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)
    reason: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["IoCapabilityRequestNegativeReply", bytes]:
        if len(span) < 7:
            raise Exception("Packet too short")

        bd_addr = list(span[:6])
        reason = span[6]

        return IoCapabilityRequestNegativeReply(
            bd_addr=bd_addr,
            reason=reason,
            payload=bytes(span[7:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes(self.bd_addr[:6] + [self.reason]) + self.payload

    @property
    def size(self) -> int:
        return 7 + len(self.payload)
