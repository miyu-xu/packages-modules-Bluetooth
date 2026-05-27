# File generated from <stdin>, with the command:
#  external/rust/android-crates-io/crates/pdl-compiler/scripts/generate_python_backend.py
# /!\ Do not edit by hand.
from dataclasses import dataclass, field
from typing import Optional, List, Tuple, Union
import enum
import struct


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


class Opcode(enum.IntEnum):
    ERROR_RSP = 0x01
    EXCHANGE_MTU_REQ = 0x02
    EXCHANGE_MTU_RSP = 0x03
    FIND_INFORMATION_REQ = 0x04
    FIND_INFORMATION_RSP = 0x05
    FIND_BY_TYPE_VALUE_REQ = 0x06
    FIND_BY_TYPE_VALUE_RSP = 0x07
    READ_BY_TYPE_REQ = 0x08
    READ_BY_TYPE_RSP = 0x09
    READ_REQ = 0x0A
    READ_RSP = 0x0B
    READ_BLOB_REQ = 0x0C
    READ_BLOB_RSP = 0x0D
    READ_MULTIPLE_REQ = 0x0E
    READ_MULTIPLE_RSP = 0x0F
    READ_BY_GROUP_TYPE_REQ = 0x10
    READ_BY_GROUP_TYPE_RSP = 0x11
    WRITE_REQ = 0x12
    WRITE_RSP = 0x13
    PREPARE_WRITE_REQ = 0x16
    PREPARE_WRITE_RSP = 0x17
    EXECUTE_WRITE_REQ = 0x18
    EXECUTE_WRITE_RSP = 0x19
    HANDLE_VALUE_NTF = 0x1B
    HANDLE_VALUE_IND = 0x1D
    HANDLE_VALUE_CFM = 0x1E
    READ_MULTIPLE_VARIABLE_REQ = 0x20
    READ_MULTIPLE_VARIABLE_RSP = 0x21
    MULTIPLE_HANDLE_VALUE_NTF = 0x23
    WRITE_CMD = 0x52
    SIGNED_WRITE_CMD = 0xD2

    @staticmethod
    def from_int(v: int) -> Union[int, "Opcode"]:
        try:
            return Opcode(v)
        except ValueError:
            return v


class ErrorCode(enum.IntEnum):
    INVALID_HANDLE = 0x01
    READ_NOT_PERMITTED = 0x02
    WRITE_NOT_PERMITTED = 0x03
    INVALID_PDU = 0x04
    INSUFFICIENT_AUTHENTICATION = 0x05
    REQUEST_NOT_SUPPORTED = 0x06
    INVALID_OFFSET = 0x07
    INSUFFICIENT_AUTHORIZATION = 0x08
    PREPARE_QUEUE_FULL = 0x09
    ATTRIBUTE_NOT_FOUND = 0x0A
    ATTRIBUTE_NOT_LONG = 0x0B
    INSUFFICIENT_ENCRYPTION_KEY_SIZE = 0x0C
    INVALID_ATTRIBUTE_VALUE_LENGTH = 0x0D
    UNLIKELY_ERROR = 0x0E
    INSUFFICIENT_ENCRYPTION = 0x0F
    UNSUPPORTED_GROUP_TYPE = 0x10
    INSUFFICIENT_RESOURCES = 0x11
    VALUE_NOT_ALLOWED = 0x13

    @staticmethod
    def from_int(v: int) -> Union[int, "ErrorCode"]:
        try:
            return ErrorCode(v)
        except ValueError:
            return v


class FindInfoFormat(enum.IntEnum):
    UUID_16BIT = 0x01
    UUID_128BIT = 0x02

    @staticmethod
    def from_int(v: int) -> Union[int, "FindInfoFormat"]:
        try:
            return FindInfoFormat(v)
        except ValueError:
            return v


class ExecuteWriteFlag(enum.IntEnum):
    CANCEL = 0x00
    WRITE = 0x01

    @staticmethod
    def from_int(v: int) -> Union[int, "ExecuteWriteFlag"]:
        try:
            return ExecuteWriteFlag(v)
        except ValueError:
            return v


# =============================================================================
# ATT PDU Structures
# =============================================================================


@dataclass
class Att(Packet):
    opcode: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["Att", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        payload = bytes(span[1:])

        return Att(
            opcode=opcode,
            payload=payload,
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ErrorRsp(Att):
    request_opcode_in_error: int = 0
    attribute_handle_in_error: int = 0
    error_code: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ErrorRsp", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        request_opcode_in_error = Opcode.from_int(span[1])
        attribute_handle_in_error = struct.unpack_from("<H", span, 2)[0]
        error_code = ErrorCode.from_int(span[4])

        return ErrorRsp(
            opcode=opcode,
            request_opcode_in_error=request_opcode_in_error,
            attribute_handle_in_error=attribute_handle_in_error,
            error_code=error_code,
            payload=bytes(span[5:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class ExchangeMtuReq(Att):
    client_rx_mtu: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ExchangeMtuReq", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        client_rx_mtu = struct.unpack_from("<H", span, 1)[0]

        return ExchangeMtuReq(
            opcode=opcode,
            client_rx_mtu=client_rx_mtu,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class ExchangeMtuRsp(Att):
    server_rx_mtu: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ExchangeMtuRsp", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        server_rx_mtu = struct.unpack_from("<H", span, 1)[0]

        return ExchangeMtuRsp(
            opcode=opcode,
            server_rx_mtu=server_rx_mtu,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class FindInformationReq(Att):
    starting_handle: int = 0
    ending_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["FindInformationReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        starting_handle = struct.unpack_from("<H", span, 1)[0]
        ending_handle = struct.unpack_from("<H", span, 3)[0]

        return FindInformationReq(
            opcode=opcode,
            starting_handle=starting_handle,
            ending_handle=ending_handle,
            payload=bytes(span[5:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class FindInformationRsp(Att):
    format: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["FindInformationRsp", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        format = FindInfoFormat.from_int(span[1])

        return FindInformationRsp(
            opcode=opcode,
            format=format,
            payload=bytes(span[2:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


@dataclass
class FindByTypeValueReq(Att):
    starting_handle: int = 0
    ending_handle: int = 0
    attribute_type: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["FindByTypeValueReq", bytes]:
        if len(span) < 7:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        starting_handle = struct.unpack_from("<H", span, 1)[0]
        ending_handle = struct.unpack_from("<H", span, 3)[0]
        attribute_type = struct.unpack_from("<H", span, 5)[0]

        return FindByTypeValueReq(
            opcode=opcode,
            starting_handle=starting_handle,
            ending_handle=ending_handle,
            attribute_type=attribute_type,
            payload=bytes(span[7:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 7 + len(self.payload)


@dataclass
class ReadByTypeReq(Att):
    starting_handle: int = 0
    ending_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ReadByTypeReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        starting_handle = struct.unpack_from("<H", span, 1)[0]
        ending_handle = struct.unpack_from("<H", span, 3)[0]

        return ReadByTypeReq(
            opcode=opcode,
            starting_handle=starting_handle,
            ending_handle=ending_handle,
            payload=bytes(span[5:]),  # UUID (2 or 16 bytes)
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class ReadByTypeRsp(Att):
    length: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ReadByTypeRsp", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        length = span[1]

        return ReadByTypeRsp(
            opcode=opcode,
            length=length,
            payload=bytes(span[2:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


@dataclass
class ReadReq(Att):
    attribute_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ReadReq", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]

        return ReadReq(
            opcode=opcode,
            attribute_handle=attribute_handle,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class ReadRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ReadRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ReadRsp(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ReadBlobReq(Att):
    attribute_handle: int = 0
    value_offset: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ReadBlobReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]
        value_offset = struct.unpack_from("<H", span, 3)[0]

        return ReadBlobReq(
            opcode=opcode,
            attribute_handle=attribute_handle,
            value_offset=value_offset,
            payload=bytes(span[5:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class ReadBlobRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ReadBlobRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ReadBlobRsp(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ReadByGroupTypeReq(Att):
    starting_handle: int = 0
    ending_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ReadByGroupTypeReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        starting_handle = struct.unpack_from("<H", span, 1)[0]
        ending_handle = struct.unpack_from("<H", span, 3)[0]

        return ReadByGroupTypeReq(
            opcode=opcode,
            starting_handle=starting_handle,
            ending_handle=ending_handle,
            payload=bytes(span[5:]),  # UUID (2 or 16 bytes)
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class ReadByGroupTypeRsp(Att):
    length: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ReadByGroupTypeRsp", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        length = span[1]

        return ReadByGroupTypeRsp(
            opcode=opcode,
            length=length,
            payload=bytes(span[2:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


@dataclass
class WriteReq(Att):
    attribute_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["WriteReq", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]

        return WriteReq(
            opcode=opcode,
            attribute_handle=attribute_handle,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class WriteRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["WriteRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return WriteRsp(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class WriteCmd(Att):
    attribute_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["WriteCmd", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]

        return WriteCmd(
            opcode=opcode,
            attribute_handle=attribute_handle,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class PrepareWriteReq(Att):
    attribute_handle: int = 0
    value_offset: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["PrepareWriteReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]
        value_offset = struct.unpack_from("<H", span, 3)[0]

        return PrepareWriteReq(
            opcode=opcode,
            attribute_handle=attribute_handle,
            value_offset=value_offset,
            payload=bytes(span[5:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class PrepareWriteRsp(Att):
    attribute_handle: int = 0
    value_offset: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["PrepareWriteRsp", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]
        value_offset = struct.unpack_from("<H", span, 3)[0]

        return PrepareWriteRsp(
            opcode=opcode,
            attribute_handle=attribute_handle,
            value_offset=value_offset,
            payload=bytes(span[5:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.payload)


@dataclass
class ExecuteWriteReq(Att):
    flags: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["ExecuteWriteReq", bytes]:
        if len(span) < 2:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        flags = ExecuteWriteFlag.from_int(span[1])

        return ExecuteWriteReq(
            opcode=opcode,
            flags=flags,
            payload=bytes(span[2:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 2 + len(self.payload)


@dataclass
class ExecuteWriteRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ExecuteWriteRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ExecuteWriteRsp(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class HandleValueNtf(Att):
    attribute_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["HandleValueNtf", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]

        return HandleValueNtf(
            opcode=opcode,
            attribute_handle=attribute_handle,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class HandleValueInd(Att):
    attribute_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["HandleValueInd", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]

        return HandleValueInd(
            opcode=opcode,
            attribute_handle=attribute_handle,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class HandleValueCfm(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["HandleValueCfm", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return HandleValueCfm(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ReadMultipleReq(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ReadMultipleReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ReadMultipleReq(
            opcode=opcode,
            payload=bytes(span[1:]),  # Set of 2+ attribute handles
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ReadMultipleRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ReadMultipleRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ReadMultipleRsp(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ReadMultipleVariableReq(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ReadMultipleVariableReq", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ReadMultipleVariableReq(
            opcode=opcode,
            payload=bytes(span[1:]),  # Set of 2+ attribute handles
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class ReadMultipleVariableRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["ReadMultipleVariableRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return ReadMultipleVariableRsp(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class MultipleHandleValueNtf(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["MultipleHandleValueNtf", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return MultipleHandleValueNtf(
            opcode=opcode,
            payload=bytes(span[1:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


@dataclass
class SignedWriteCmd(Att):
    attribute_handle: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["SignedWriteCmd", bytes]:
        if len(span) < 15:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])
        attribute_handle = struct.unpack_from("<H", span, 1)[0]
        # payload contains: attribute value + 12-byte authentication signature

        return SignedWriteCmd(
            opcode=opcode,
            attribute_handle=attribute_handle,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class FindByTypeValueRsp(Att):
    @staticmethod
    def parse(span: bytes) -> Tuple["FindByTypeValueRsp", bytes]:
        if len(span) < 1:
            raise Exception("Packet too short")

        opcode = Opcode.from_int(span[0])

        return FindByTypeValueRsp(
            opcode=opcode,
            payload=bytes(span[1:]),  # Handles Information List
        ), bytes()

    @property
    def size(self) -> int:
        return 1 + len(self.payload)


_PARSERS = {
    Opcode.ERROR_RSP: ErrorRsp,
    Opcode.EXCHANGE_MTU_REQ: ExchangeMtuReq,
    Opcode.EXCHANGE_MTU_RSP: ExchangeMtuRsp,
    Opcode.FIND_INFORMATION_REQ: FindInformationReq,
    Opcode.FIND_INFORMATION_RSP: FindInformationRsp,
    Opcode.FIND_BY_TYPE_VALUE_REQ: FindByTypeValueReq,
    Opcode.FIND_BY_TYPE_VALUE_RSP: FindByTypeValueRsp,
    Opcode.READ_BY_TYPE_REQ: ReadByTypeReq,
    Opcode.READ_BY_TYPE_RSP: ReadByTypeRsp,
    Opcode.READ_REQ: ReadReq,
    Opcode.READ_RSP: ReadRsp,
    Opcode.READ_BLOB_REQ: ReadBlobReq,
    Opcode.READ_BLOB_RSP: ReadBlobRsp,
    Opcode.READ_MULTIPLE_REQ: ReadMultipleReq,
    Opcode.READ_MULTIPLE_RSP: ReadMultipleRsp,
    Opcode.READ_BY_GROUP_TYPE_REQ: ReadByGroupTypeReq,
    Opcode.READ_BY_GROUP_TYPE_RSP: ReadByGroupTypeRsp,
    Opcode.WRITE_REQ: WriteReq,
    Opcode.WRITE_RSP: WriteRsp,
    Opcode.WRITE_CMD: WriteCmd,
    Opcode.PREPARE_WRITE_REQ: PrepareWriteReq,
    Opcode.PREPARE_WRITE_RSP: PrepareWriteRsp,
    Opcode.EXECUTE_WRITE_REQ: ExecuteWriteReq,
    Opcode.EXECUTE_WRITE_RSP: ExecuteWriteRsp,
    Opcode.HANDLE_VALUE_NTF: HandleValueNtf,
    Opcode.HANDLE_VALUE_IND: HandleValueInd,
    Opcode.HANDLE_VALUE_CFM: HandleValueCfm,
    Opcode.READ_MULTIPLE_VARIABLE_REQ: ReadMultipleVariableReq,
    Opcode.READ_MULTIPLE_VARIABLE_RSP: ReadMultipleVariableRsp,
    Opcode.MULTIPLE_HANDLE_VALUE_NTF: MultipleHandleValueNtf,
    Opcode.SIGNED_WRITE_CMD: SignedWriteCmd,
}


def parse_att(span: bytes) -> Tuple[Packet, bytes]:
    """Parse an ATT PDU based on its opcode."""
    if len(span) < 1:
        raise Exception("Packet too short")

    opcode = span[0]

    parser_cls = _PARSERS.get(opcode)
    if parser_cls is not None:
        return parser_cls.parse(span)
    else:
        return Att.parse(span)
