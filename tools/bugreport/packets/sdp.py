"""
SDP (Service Discovery Protocol) packet parser.

Parses SDP PDUs as defined in Bluetooth Core Specification Vol 3, Part B.
SDP uses a request/response model over L2CAP PSM 0x0001.
"""

import enum
import struct
import sys
from dataclasses import dataclass, field
from typing import List, Tuple, Union


# Maximum nesting depth for recursive Data Element parsing.
# 32 levels is far beyond any legitimate SDP record.
MAX_DATA_ELEMENT_DEPTH = 32


class PduId(enum.IntEnum):
    """SDP PDU IDs (Bluetooth Core Spec Vol 3, Part B, Section 4.2)."""
    ERROR_RESPONSE = 0x01
    SERVICE_SEARCH_REQUEST = 0x02
    SERVICE_SEARCH_RESPONSE = 0x03
    SERVICE_ATTRIBUTE_REQUEST = 0x04
    SERVICE_ATTRIBUTE_RESPONSE = 0x05
    SERVICE_SEARCH_ATTRIBUTE_REQUEST = 0x06
    SERVICE_SEARCH_ATTRIBUTE_RESPONSE = 0x07

    @staticmethod
    def from_int(v: int) -> Union[int, "PduId"]:
        try:
            return PduId(v)
        except ValueError:
            return v


class ErrorCode(enum.IntEnum):
    """SDP Error Codes (Bluetooth Core Spec Vol 3, Part B, Section 4.4.1)."""
    INVALID_SDP_VERSION = 0x0001
    INVALID_SERVICE_RECORD_HANDLE = 0x0002
    INVALID_REQUEST_SYNTAX = 0x0003
    INVALID_PDU_SIZE = 0x0004
    INVALID_CONTINUATION_STATE = 0x0005
    INSUFFICIENT_RESOURCES = 0x0006

    @staticmethod
    def from_int(v: int) -> Union[int, "ErrorCode"]:
        try:
            return ErrorCode(v)
        except ValueError:
            return v


class DataElementType(enum.IntEnum):
    """SDP Data Element Type Descriptor (Bluetooth Core Spec Vol 3, Part B, Section 3.2)."""
    NIL = 0
    UNSIGNED_INT = 1
    SIGNED_INT = 2
    UUID = 3
    TEXT_STRING = 4
    BOOLEAN = 5
    DATA_ELEMENT_SEQUENCE = 6
    DATA_ELEMENT_ALTERNATIVE = 7
    URL = 8


# Valid size indices per type descriptor (Bluetooth Core Spec Vol 3, Part B, Section 3.2).
# size_index 0-4 → 1,2,4,8,16 byte fixed sizes; 5-7 → variable-length with 1,2,4 byte length.
_VALID_SIZE_INDICES = {
    DataElementType.NIL:                    {0},
    DataElementType.UNSIGNED_INT:           {0, 1, 2, 3, 4},       # 1,2,4,8,16 bytes
    DataElementType.SIGNED_INT:             {0, 1, 2, 3, 4},       # 1,2,4,8,16 bytes
    DataElementType.UUID:                   {1, 2, 4},              # 2,4,16 bytes
    DataElementType.TEXT_STRING:            {5, 6, 7},              # variable-length
    DataElementType.BOOLEAN:               {0, 1},                  # 1 byte (spec), 2 bytes (common)
    DataElementType.DATA_ELEMENT_SEQUENCE:  {5, 6, 7},              # variable-length
    DataElementType.DATA_ELEMENT_ALTERNATIVE: {5, 6, 7},            # variable-length
    DataElementType.URL:                   {5, 6, 7},              # variable-length
}


# Well-known Bluetooth UUID16 values for service classes and protocols.
UUID16_NAMES = {
    # Protocols
    0x0001: "SDP",
    0x0002: "UDP",
    0x0003: "RFCOMM",
    0x0004: "TCP",
    0x0005: "TCS-BIN",
    0x0006: "TCS-AT",
    0x0007: "ATT",
    0x0008: "OBEX",
    0x0009: "IP",
    0x000A: "FTP",
    0x000C: "HTTP",
    0x000E: "WSP",
    0x000F: "BNEP",
    0x0010: "UPNP",
    0x0011: "HIDP",
    0x0012: "HCRP-Ctrl",
    0x0014: "HCRP-Data",
    0x0016: "HCRP-Notif",
    0x0017: "AVCTP",
    0x0019: "AVDTP",
    0x001B: "CMTP",
    0x001E: "MCAP-Ctrl",
    0x001F: "MCAP-Data",
    0x0100: "L2CAP",
    # Service Classes
    0x1000: "ServiceDiscoveryServerServiceClassID",
    0x1001: "BrowseGroupDescriptorServiceClassID",
    0x1101: "SerialPort",
    0x1102: "LANAccessUsingPPP",
    0x1103: "DialupNetworking",
    0x1104: "IrMCSync",
    0x1105: "OBEXObjectPush",
    0x1106: "OBEXFileTransfer",
    0x1107: "IrMCSyncCommand",
    0x1108: "Headset",
    0x1109: "CordlessTelephony",
    0x110A: "AudioSource",
    0x110B: "AudioSink",
    0x110C: "A/V_RemoteControlTarget",
    0x110D: "AdvancedAudioDistribution",
    0x110E: "A/V_RemoteControl",
    0x110F: "A/V_RemoteControlController",
    0x1110: "Intercom",
    0x1111: "Fax",
    0x1112: "Headset-AudioGateway",
    0x1115: "PANU",
    0x1116: "NAP",
    0x1117: "GN",
    0x111E: "Handsfree",
    0x111F: "HandsfreeAudioGateway",
    0x1124: "HumanInterfaceDeviceService",
    0x1125: "HardcopyCableReplacement",
    0x1126: "HCR_Print",
    0x1127: "HCR_Scan",
    0x112D: "SIM_Access",
    0x112E: "PB-PCE",
    0x112F: "PB-PSE",
    0x1130: "Phonebook Access",
    0x1131: "Headset-HS",
    0x1132: "MessageAccessServer",
    0x1133: "MessageNotificationServer",
    0x1134: "MessageAccessProfile",
    0x1135: "GNSS",
    0x1136: "GNSS_Server",
    0x1200: "PnPInformation",
    0x1203: "GenericAudio",
    0x1303: "VideoSource",
    0x1304: "VideoSink",
    0x1305: "VideoDistribution",
    0x1400: "HDP",
    0x1401: "HDP_Source",
    0x1402: "HDP_Sink",
}

# Well-known SDP Attribute IDs.
ATTRIBUTE_NAMES = {
    0x0000: "ServiceRecordHandle",
    0x0001: "ServiceClassIDList",
    0x0002: "ServiceRecordState",
    0x0003: "ServiceID",
    0x0004: "ProtocolDescriptorList",
    0x0005: "BrowseGroupList",
    0x0006: "LanguageBaseAttributeIDList",
    0x0007: "ServiceInfoTimeToLive",
    0x0008: "ServiceAvailability",
    0x0009: "BluetoothProfileDescriptorList",
    0x000A: "DocumentationURL",
    0x000B: "ClientExecutableURL",
    0x000C: "IconURL",
    0x000D: "AdditionalProtocolDescriptorLists",
    0x0100: "ServiceName",
    0x0101: "ServiceDescription",
    0x0102: "ProviderName",
    0x0200: "GoepL2capPsm",
    0x0301: "SupportedFormatsList",
    0x0311: "SupportedFeatures",
}


@dataclass
class DataElement:
    """Parsed SDP Data Element."""
    type: DataElementType
    value: object  # int, bytes, str, bool, list, or None
    size: int = 0  # original byte size of the value

    def __repr__(self):
        if self.type == DataElementType.UUID:
            if isinstance(self.value, int):
                name = UUID16_NAMES.get(self.value, None)
                if name:
                    return f"UUID16(0x{self.value:04X} = {name})"
                return f"UUID16(0x{self.value:04X})"
            elif isinstance(self.value, bytes):
                if len(self.value) == 4:
                    val = int.from_bytes(self.value, "big")
                    return f"UUID32(0x{val:08X})"
                return f"UUID128({self.value.hex()})"
        elif self.type == DataElementType.UNSIGNED_INT:
            return f"UINT({self.value})"
        elif self.type == DataElementType.SIGNED_INT:
            return f"INT({self.value})"
        elif self.type == DataElementType.BOOLEAN:
            return f"BOOL({self.value})"
        elif self.type == DataElementType.TEXT_STRING:
            try:
                return f"TEXT({self.value.decode('utf-8', errors='replace')!r})"
            except Exception:
                return f"TEXT({self.value.hex()})"
        elif self.type == DataElementType.URL:
            try:
                return f"URL({self.value.decode('utf-8', errors='replace')!r})"
            except Exception:
                return f"URL({self.value.hex()})"
        elif self.type in (DataElementType.DATA_ELEMENT_SEQUENCE,
                           DataElementType.DATA_ELEMENT_ALTERNATIVE):
            kind = "SEQ" if self.type == DataElementType.DATA_ELEMENT_SEQUENCE else "ALT"
            items = ", ".join(repr(e) for e in self.value)
            return f"{kind}[{items}]"
        elif self.type == DataElementType.NIL:
            return "NIL"
        return f"DataElement({self.type}, {self.value})"


def parse_data_element(span: bytes, _depth: int = 0) -> Tuple[DataElement, bytes]:
    """Parse a single SDP Data Element from the byte stream.

    Returns the parsed element and remaining bytes.
    Raises ValueError on malformed input or excessive nesting.
    """
    if _depth > MAX_DATA_ELEMENT_DEPTH:
        raise ValueError(
            f"Data element nesting exceeds maximum depth ({MAX_DATA_ELEMENT_DEPTH})"
        )

    if len(span) < 1:
        raise ValueError("Empty data element")

    header = span[0]
    type_id = (header >> 3) & 0x1F
    size_index = header & 0x07
    span = span[1:]

    try:
        elem_type = DataElementType(type_id)
    except ValueError:
        raise ValueError(f"Unknown SDP data element type descriptor: {type_id}")

    # Validate type/size_index combination per spec (Vol 3, Part B, Section 3.2)
    valid_indices = _VALID_SIZE_INDICES.get(elem_type)
    if valid_indices is not None and size_index not in valid_indices:
        print(
            f"Warning: SDP data element type {elem_type.name} with "
            f"invalid size index {size_index} (expected one of {valid_indices})",
            file=sys.stderr,
        )

    # NIL type
    if elem_type == DataElementType.NIL:
        return DataElement(elem_type, None, 0), span

    # Determine size based on size index
    if size_index == 0:
        size = 1
    elif size_index == 1:
        size = 2
    elif size_index == 2:
        size = 4
    elif size_index == 3:
        size = 8
    elif size_index == 4:
        size = 16
    elif size_index == 5:
        if len(span) < 1:
            raise ValueError("Truncated data element size")
        size = span[0]
        span = span[1:]
    elif size_index == 6:
        if len(span) < 2:
            raise ValueError("Truncated data element size")
        size = struct.unpack(">H", span[:2])[0]
        span = span[2:]
    elif size_index == 7:
        if len(span) < 4:
            raise ValueError("Truncated data element size")
        size = struct.unpack(">I", span[:4])[0]
        span = span[4:]
    else:
        raise ValueError(f"Invalid size index {size_index}")

    if len(span) < size:
        raise ValueError(f"Truncated data element body: need {size}, have {len(span)}")

    body = span[:size]
    span = span[size:]

    # Parse value based on type
    if elem_type == DataElementType.UNSIGNED_INT:
        value = int.from_bytes(body, "big")
    elif elem_type == DataElementType.SIGNED_INT:
        value = int.from_bytes(body, "big", signed=True)
    elif elem_type == DataElementType.UUID:
        if size in (2, 4):
            value = int.from_bytes(body, "big")
        else:
            value = bytes(body)
    elif elem_type == DataElementType.BOOLEAN:
        value = bool(body[0])
    elif elem_type in (DataElementType.TEXT_STRING, DataElementType.URL):
        value = bytes(body)
    elif elem_type in (DataElementType.DATA_ELEMENT_SEQUENCE,
                       DataElementType.DATA_ELEMENT_ALTERNATIVE):
        elements = []
        remaining = body
        while remaining:
            element, remaining = parse_data_element(remaining, _depth + 1)
            elements.append(element)
        value = elements
    else:
        value = bytes(body)

    return DataElement(elem_type, value, size), span


def parse_data_element_list(span: bytes) -> List[DataElement]:
    """Parse a sequence of data elements from a byte stream."""
    elements = []
    while span:
        element, span = parse_data_element(span)
        elements.append(element)
    return elements


def _parse_continuation_state(data: bytes) -> bytes:
    """Parse SDP continuation state (Vol 3, Part B, Section 4.3).

    The continuation state is a length-prefixed field:
      [0]    ContinuationStateLength (0 = no continuation, 1-16 = info length)
      [1:N]  ContinuationStateInfo bytes
    Returns the raw continuation info bytes (empty if length is 0).
    """
    if not data or len(data) < 1:
        return b""
    cont_len = data[0]
    if cont_len == 0:
        return b""
    if len(data) < 1 + cont_len:
        return data[1:]  # return what we have
    return data[1:1 + cont_len]


@dataclass
class SdpPdu:
    """Base SDP PDU."""
    pdu_id: PduId
    transaction_id: int
    parameter_length: int
    payload: bytes = field(default=b"", repr=False)
    parse_error: str = ""

    @staticmethod
    def parse(span: bytes) -> "SdpPdu":
        """Parse an SDP PDU from bytes."""
        if len(span) < 5:
            raise ValueError(f"SDP PDU too short ({len(span)} bytes, need 5)")

        pdu_id = PduId.from_int(span[0])
        transaction_id = struct.unpack(">H", span[1:3])[0]
        parameter_length = struct.unpack(">H", span[3:5])[0]
        available = len(span) - 5
        payload = span[5:5 + parameter_length]

        if available < parameter_length:
            raise ValueError(
                f"SDP PDU truncated: parameter_length={parameter_length}, "
                f"but only {available} bytes available"
            )

        if not isinstance(pdu_id, PduId):
            return SdpPdu(pdu_id, transaction_id, parameter_length, payload)

        if pdu_id == PduId.ERROR_RESPONSE:
            return SdpErrorResponse.parse_params(pdu_id, transaction_id, parameter_length, payload)
        elif pdu_id == PduId.SERVICE_SEARCH_REQUEST:
            return SdpServiceSearchRequest.parse_params(pdu_id, transaction_id, parameter_length, payload)
        elif pdu_id == PduId.SERVICE_SEARCH_RESPONSE:
            return SdpServiceSearchResponse.parse_params(pdu_id, transaction_id, parameter_length, payload)
        elif pdu_id == PduId.SERVICE_ATTRIBUTE_REQUEST:
            return SdpServiceAttributeRequest.parse_params(pdu_id, transaction_id, parameter_length, payload)
        elif pdu_id == PduId.SERVICE_ATTRIBUTE_RESPONSE:
            return SdpServiceAttributeResponse.parse_params(pdu_id, transaction_id, parameter_length, payload)
        elif pdu_id == PduId.SERVICE_SEARCH_ATTRIBUTE_REQUEST:
            return SdpServiceSearchAttributeRequest.parse_params(pdu_id, transaction_id, parameter_length, payload)
        elif pdu_id == PduId.SERVICE_SEARCH_ATTRIBUTE_RESPONSE:
            return SdpServiceSearchAttributeResponse.parse_params(pdu_id, transaction_id, parameter_length, payload)

        return SdpPdu(pdu_id, transaction_id, parameter_length, payload)


@dataclass
class SdpErrorResponse(SdpPdu):
    error_code: ErrorCode = ErrorCode.INVALID_REQUEST_SYNTAX

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpErrorResponse":
        error_code = ErrorCode.from_int(struct.unpack(">H", payload[:2])[0]) if len(payload) >= 2 else 0
        return SdpErrorResponse(pdu_id, transaction_id, parameter_length, payload, "", error_code)


@dataclass
class SdpServiceSearchRequest(SdpPdu):
    service_search_pattern: List[DataElement] = field(default_factory=list)
    maximum_service_record_count: int = 0
    continuation_state: bytes = b""

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpServiceSearchRequest":
        parse_error = ""
        try:
            pattern_elem, rest = parse_data_element(payload)
            pattern = pattern_elem.value if isinstance(pattern_elem.value, list) else [pattern_elem]
            max_count = struct.unpack(">H", rest[:2])[0] if len(rest) >= 2 else 0
            cont = _parse_continuation_state(rest[2:]) if len(rest) > 2 else b""
        except Exception as e:
            parse_error = str(e)
            pattern = []
            max_count = 0
            cont = b""
        return SdpServiceSearchRequest(pdu_id, transaction_id, parameter_length, payload,
                                       parse_error, pattern, max_count, cont)


@dataclass
class SdpServiceSearchResponse(SdpPdu):
    total_service_record_count: int = 0
    current_service_record_count: int = 0
    service_record_handle_list: List[int] = field(default_factory=list)
    continuation_state: bytes = b""

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpServiceSearchResponse":
        parse_error = ""
        try:
            total = struct.unpack(">H", payload[:2])[0]
            current = struct.unpack(">H", payload[2:4])[0]
            handles = []
            offset = 4
            for _ in range(current):
                handles.append(struct.unpack(">I", payload[offset:offset + 4])[0])
                offset += 4
            cont = _parse_continuation_state(payload[offset:]) if offset < len(payload) else b""
        except Exception as e:
            parse_error = str(e)
            total = 0
            current = 0
            handles = []
            cont = b""
        return SdpServiceSearchResponse(pdu_id, transaction_id, parameter_length, payload,
                                        parse_error, total, current, handles, cont)


@dataclass
class SdpServiceAttributeRequest(SdpPdu):
    service_record_handle: int = 0
    maximum_attribute_byte_count: int = 0
    attribute_id_list: List[DataElement] = field(default_factory=list)
    continuation_state: bytes = b""

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpServiceAttributeRequest":
        parse_error = ""
        try:
            handle = struct.unpack(">I", payload[:4])[0]
            max_bytes = struct.unpack(">H", payload[4:6])[0]
            attr_elem, rest = parse_data_element(payload[6:])
            attrs = attr_elem.value if isinstance(attr_elem.value, list) else [attr_elem]
            cont = _parse_continuation_state(rest)
        except Exception as e:
            parse_error = str(e)
            handle = 0
            max_bytes = 0
            attrs = []
            cont = b""
        return SdpServiceAttributeRequest(pdu_id, transaction_id, parameter_length, payload,
                                          parse_error, handle, max_bytes, attrs, cont)


@dataclass
class SdpServiceAttributeResponse(SdpPdu):
    attribute_list_byte_count: int = 0
    attribute_list: List[DataElement] = field(default_factory=list)
    continuation_state: bytes = b""

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpServiceAttributeResponse":
        parse_error = ""
        try:
            byte_count = struct.unpack(">H", payload[:2])[0]
            attr_bytes = payload[2:2 + byte_count]
            attrs = parse_data_element_list(attr_bytes)
            cont = _parse_continuation_state(payload[2 + byte_count:])
        except Exception as e:
            parse_error = str(e)
            byte_count = 0
            attrs = []
            cont = b""
        return SdpServiceAttributeResponse(pdu_id, transaction_id, parameter_length, payload,
                                           parse_error, byte_count, attrs, cont)


@dataclass
class SdpServiceSearchAttributeRequest(SdpPdu):
    service_search_pattern: List[DataElement] = field(default_factory=list)
    maximum_attribute_byte_count: int = 0
    attribute_id_list: List[DataElement] = field(default_factory=list)
    continuation_state: bytes = b""

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpServiceSearchAttributeRequest":
        parse_error = ""
        try:
            pattern_elem, rest = parse_data_element(payload)
            pattern = pattern_elem.value if isinstance(pattern_elem.value, list) else [pattern_elem]
            max_bytes = struct.unpack(">H", rest[:2])[0]
            attr_elem, rest2 = parse_data_element(rest[2:])
            attrs = attr_elem.value if isinstance(attr_elem.value, list) else [attr_elem]
            cont = _parse_continuation_state(rest2)
        except Exception as e:
            parse_error = str(e)
            pattern = []
            max_bytes = 0
            attrs = []
            cont = b""
        return SdpServiceSearchAttributeRequest(pdu_id, transaction_id, parameter_length, payload,
                                                parse_error, pattern, max_bytes, attrs, cont)


@dataclass
class SdpServiceSearchAttributeResponse(SdpPdu):
    attribute_lists_byte_count: int = 0
    attribute_lists: List[DataElement] = field(default_factory=list)
    continuation_state: bytes = b""

    @staticmethod
    def parse_params(pdu_id, transaction_id, parameter_length, payload) -> "SdpServiceSearchAttributeResponse":
        parse_error = ""
        try:
            byte_count = struct.unpack(">H", payload[:2])[0]
            attr_bytes = payload[2:2 + byte_count]
            attrs = parse_data_element_list(attr_bytes)
            cont = _parse_continuation_state(payload[2 + byte_count:])
        except Exception as e:
            parse_error = str(e)
            byte_count = 0
            attrs = []
            cont = b""
        return SdpServiceSearchAttributeResponse(pdu_id, transaction_id, parameter_length, payload,
                                                 parse_error, byte_count, attrs, cont)
