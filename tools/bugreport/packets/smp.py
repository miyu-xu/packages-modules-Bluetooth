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


class Code(enum.IntEnum):
    PAIRING_REQUEST = 0x01
    PAIRING_RESPONSE = 0x02
    PAIRING_CONFIRM = 0x03
    PAIRING_RANDOM = 0x04
    PAIRING_FAILED = 0x05
    ENCRYPTION_INFORMATION = 0x06
    CENTRAL_IDENTIFICATION = 0x07
    IDENTITY_INFORMATION = 0x08
    IDENTITY_ADDRESS_INFORMATION = 0x09
    SIGNING_INFORMATION = 0x0A
    SECURITY_REQUEST = 0x0B
    PAIRING_PUBLIC_KEY = 0x0C
    PAIRING_DHKEY_CHECK = 0x0D
    PAIRING_KEYPRESS_NOTIFICATION = 0x0E

    @staticmethod
    def from_int(v: int) -> Union[int, "Code"]:
        try:
            return Code(v)
        except ValueError as exn:
            raise exn


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


class OobDataFlag(enum.IntEnum):
    NOT_PRESENT = 0x00
    PRESENT = 0x01

    @staticmethod
    def from_int(v: int) -> Union[int, "OobDataFlag"]:
        try:
            return OobDataFlag(v)
        except ValueError as exn:
            raise exn


class PairingFailedReason(enum.IntEnum):
    PASSKEY_ENTRY_FAILED = 0x01
    OOB_NOT_AVAILABLE = 0x02
    AUTHENTICATION_REQUIREMENTS = 0x03
    CONFIRM_VALUE_FAILED = 0x04
    PAIRING_NOT_SUPPORTED = 0x05
    ENCRYPTION_KEY_SIZE = 0x06
    COMMAND_NOT_SUPPORTED = 0x07
    UNSPECIFIED_REASON = 0x08
    REPEATED_ATTEMPTS = 0x09
    INVALID_PARAMETERS = 0x0A
    DHKEY_CHECK_FAILED = 0x0B
    NUMERIC_COMPARISON_FAILED = 0x0C
    BREDR_PAIRING_IN_PROGRESS = 0x0D
    CROSS_TRANSPORT_KEY_NOT_ALLOWED = 0x0E
    KEY_REJECTED = 0x0F

    @staticmethod
    def from_int(v: int) -> Union[int, "PairingFailedReason"]:
        try:
            return PairingFailedReason(v)
        except ValueError as exn:
            raise exn


class AddrType(enum.IntEnum):
    PUBLIC = 0x00
    RANDOM = 0x01

    @staticmethod
    def from_int(v: int) -> Union[int, "AddrType"]:
        try:
            return AddrType(v)
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


@dataclass
class Smp(Packet):
    code: Code = Code.PAIRING_REQUEST

    @staticmethod
    def parse(span: bytes) -> Tuple["Smp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        payload = bytes(span[1:])

        return Smp(
            code=code,
            payload=payload,
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)]) + self.payload

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class PairingRequest(Smp):
    io_capability: IoCapability = IoCapability.DISPLAY_ONLY
    oob_data_flag: OobDataFlag = OobDataFlag.NOT_PRESENT
    auth_req: int = 0
    maximum_encryption_key_size: int = 0
    initiator_key_distribution: int = 0
    responder_key_distribution: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingRequest", bytes]:
        if len(span) < 7:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        io_capability = IoCapability.from_int(span[1])
        oob_data_flag = OobDataFlag.from_int(span[2])
        auth_req = span[3]
        maximum_encryption_key_size = span[4]
        initiator_key_distribution = span[5]
        responder_key_distribution = span[6]

        return PairingRequest(
            code=code,
            io_capability=io_capability,
            oob_data_flag=oob_data_flag,
            auth_req=auth_req,
            maximum_encryption_key_size=maximum_encryption_key_size,
            initiator_key_distribution=initiator_key_distribution,
            responder_key_distribution=responder_key_distribution,
            payload=bytes(span[7:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([
            int(self.code),
            int(self.io_capability),
            int(self.oob_data_flag),
            self.auth_req & 0xFF,
            self.maximum_encryption_key_size & 0xFF,
            self.initiator_key_distribution & 0xFF,
            self.responder_key_distribution & 0xFF,
        ]) + self.payload

    @property
    def size(self) -> int:
        return 7 + len(self.payload)


@dataclass
class PairingResponse(Smp):
    io_capability: IoCapability = IoCapability.DISPLAY_ONLY
    oob_data_flag: OobDataFlag = OobDataFlag.NOT_PRESENT
    auth_req: int = 0
    maximum_encryption_key_size: int = 0
    initiator_key_distribution: int = 0
    responder_key_distribution: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingResponse", bytes]:
        if len(span) < 7:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        io_capability = IoCapability.from_int(span[1])
        oob_data_flag = OobDataFlag.from_int(span[2])
        auth_req = span[3]
        maximum_encryption_key_size = span[4]
        initiator_key_distribution = span[5]
        responder_key_distribution = span[6]

        return PairingResponse(
            code=code,
            io_capability=io_capability,
            oob_data_flag=oob_data_flag,
            auth_req=auth_req,
            maximum_encryption_key_size=maximum_encryption_key_size,
            initiator_key_distribution=initiator_key_distribution,
            responder_key_distribution=responder_key_distribution,
            payload=bytes(span[7:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([
            int(self.code),
            int(self.io_capability),
            int(self.oob_data_flag),
            self.auth_req & 0xFF,
            self.maximum_encryption_key_size & 0xFF,
            self.initiator_key_distribution & 0xFF,
            self.responder_key_distribution & 0xFF,
        ]) + self.payload

    @property
    def size(self) -> int:
        return 7 + len(self.payload)


@dataclass
class PairingConfirm(Smp):
    confirm_value: List[int] = field(default_factory=lambda: [0] * 16)

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingConfirm", bytes]:
        if len(span) < 17:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        confirm_value = list(span[1:17])

        return PairingConfirm(
            code=code,
            confirm_value=confirm_value,
            payload=bytes(span[17:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.confirm_value[:16]) + self.payload

    @property
    def size(self) -> int:
        return 17 + len(self.payload)


@dataclass
class PairingRandom(Smp):
    random_value: List[int] = field(default_factory=lambda: [0] * 16)

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingRandom", bytes]:
        if len(span) < 17:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        random_value = list(span[1:17])

        return PairingRandom(
            code=code,
            random_value=random_value,
            payload=bytes(span[17:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.random_value[:16]) + self.payload

    @property
    def size(self) -> int:
        return 17 + len(self.payload)


@dataclass
class PairingFailed(Smp):
    reason: PairingFailedReason = PairingFailedReason.UNSPECIFIED_REASON

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingFailed", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        reason = PairingFailedReason.from_int(span[1])

        return PairingFailed(
            code=code,
            reason=reason,
            payload=bytes(span[2:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code), int(self.reason)]) + self.payload

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


@dataclass
class EncryptionInformation(Smp):
    long_term_key: List[int] = field(default_factory=lambda: [0] * 16)

    @staticmethod
    def parse(span: bytes) -> Tuple["EncryptionInformation", bytes]:
        if len(span) < 17:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        long_term_key = list(span[1:17])

        return EncryptionInformation(
            code=code,
            long_term_key=long_term_key,
            payload=bytes(span[17:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.long_term_key[:16]) + self.payload

    @property
    def size(self) -> int:
        return 17 + len(self.payload)


@dataclass
class CentralIdentification(Smp):
    ediv: int = 0
    rand: List[int] = field(default_factory=lambda: [0] * 8)

    @staticmethod
    def parse(span: bytes) -> Tuple["CentralIdentification", bytes]:
        if len(span) < 11:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        ediv = span[1] | (span[2] << 8)
        rand = list(span[3:11])

        return CentralIdentification(
            code=code,
            ediv=ediv,
            rand=rand,
            payload=bytes(span[11:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([
            int(self.code),
            self.ediv & 0xFF,
            (self.ediv >> 8) & 0xFF,
        ] + self.rand[:8]) + self.payload

    @property
    def size(self) -> int:
        return 11 + len(self.payload)


@dataclass
class IdentityInformation(Smp):
    identity_resolving_key: List[int] = field(default_factory=lambda: [0] * 16)

    @staticmethod
    def parse(span: bytes) -> Tuple["IdentityInformation", bytes]:
        if len(span) < 17:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        identity_resolving_key = list(span[1:17])

        return IdentityInformation(
            code=code,
            identity_resolving_key=identity_resolving_key,
            payload=bytes(span[17:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.identity_resolving_key[:16]) + self.payload

    @property
    def size(self) -> int:
        return 17 + len(self.payload)


@dataclass
class IdentityAddressInformation(Smp):
    addr_type: AddrType = AddrType.PUBLIC
    bd_addr: List[int] = field(default_factory=lambda: [0] * 6)

    @staticmethod
    def parse(span: bytes) -> Tuple["IdentityAddressInformation", bytes]:
        if len(span) < 8:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        addr_type = AddrType.from_int(span[1])
        bd_addr = list(span[2:8])

        return IdentityAddressInformation(
            code=code,
            addr_type=addr_type,
            bd_addr=bd_addr,
            payload=bytes(span[8:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code), int(self.addr_type)] + self.bd_addr[:6]) + self.payload

    @property
    def size(self) -> int:
        return 8 + len(self.payload)


@dataclass
class SigningInformation(Smp):
    signature_key: List[int] = field(default_factory=lambda: [0] * 16)

    @staticmethod
    def parse(span: bytes) -> Tuple["SigningInformation", bytes]:
        if len(span) < 17:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        signature_key = list(span[1:17])

        return SigningInformation(
            code=code,
            signature_key=signature_key,
            payload=bytes(span[17:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.signature_key[:16]) + self.payload

    @property
    def size(self) -> int:
        return 17 + len(self.payload)


@dataclass
class SecurityRequest(Smp):
    auth_req: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["SecurityRequest", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        auth_req = span[1]

        return SecurityRequest(
            code=code,
            auth_req=auth_req,
            payload=bytes(span[2:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code), self.auth_req & 0xFF]) + self.payload

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


@dataclass
class PairingPublicKey(Smp):
    public_key_x: List[int] = field(default_factory=lambda: [0] * 32)
    public_key_y: List[int] = field(default_factory=lambda: [0] * 32)

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingPublicKey", bytes]:
        if len(span) < 65:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        public_key_x = list(span[1:33])
        public_key_y = list(span[33:65])

        return PairingPublicKey(
            code=code,
            public_key_x=public_key_x,
            public_key_y=public_key_y,
            payload=bytes(span[65:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.public_key_x[:32] + self.public_key_y[:32]) + self.payload

    @property
    def size(self) -> int:
        return 65 + len(self.payload)


@dataclass
class PairingDhkeyCheck(Smp):
    dhkey_check: List[int] = field(default_factory=lambda: [0] * 16)

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingDhkeyCheck", bytes]:
        if len(span) < 17:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        dhkey_check = list(span[1:17])

        return PairingDhkeyCheck(
            code=code,
            dhkey_check=dhkey_check,
            payload=bytes(span[17:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code)] + self.dhkey_check[:16]) + self.payload

    @property
    def size(self) -> int:
        return 17 + len(self.payload)


@dataclass
class PairingKeypressNotification(Smp):
    notification_type: KeypressNotificationType = KeypressNotificationType.PASSKEY_ENTRY_STARTED

    @staticmethod
    def parse(span: bytes) -> Tuple["PairingKeypressNotification", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        code = Code.from_int(span[0])
        notification_type = KeypressNotificationType.from_int(span[1])

        return PairingKeypressNotification(
            code=code,
            notification_type=notification_type,
            payload=bytes(span[2:]),
        ), bytes()

    def serialize(self) -> bytes:
        return bytes([int(self.code), int(self.notification_type)]) + self.payload

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


def parse_smp(span: bytes) -> Tuple[Packet, bytes]:
    """Parse an SMP packet based on its opcode."""
    if len(span) < 1:
        raise Exception("Packet too short")

    code = span[0]

    if code == Code.PAIRING_REQUEST:
        return PairingRequest.parse(span)
    elif code == Code.PAIRING_RESPONSE:
        return PairingResponse.parse(span)
    elif code == Code.PAIRING_CONFIRM:
        return PairingConfirm.parse(span)
    elif code == Code.PAIRING_RANDOM:
        return PairingRandom.parse(span)
    elif code == Code.PAIRING_FAILED:
        return PairingFailed.parse(span)
    elif code == Code.ENCRYPTION_INFORMATION:
        return EncryptionInformation.parse(span)
    elif code == Code.CENTRAL_IDENTIFICATION:
        return CentralIdentification.parse(span)
    elif code == Code.IDENTITY_INFORMATION:
        return IdentityInformation.parse(span)
    elif code == Code.IDENTITY_ADDRESS_INFORMATION:
        return IdentityAddressInformation.parse(span)
    elif code == Code.SIGNING_INFORMATION:
        return SigningInformation.parse(span)
    elif code == Code.SECURITY_REQUEST:
        return SecurityRequest.parse(span)
    elif code == Code.PAIRING_PUBLIC_KEY:
        return PairingPublicKey.parse(span)
    elif code == Code.PAIRING_DHKEY_CHECK:
        return PairingDhkeyCheck.parse(span)
    elif code == Code.PAIRING_KEYPRESS_NOTIFICATION:
        return PairingKeypressNotification.parse(span)
    else:
        return Smp.parse(span)
