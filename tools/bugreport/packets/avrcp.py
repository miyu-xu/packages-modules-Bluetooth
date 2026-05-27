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


# AV/C Command Type (ctype)
class Ctype(enum.IntEnum):
    CONTROL = 0x0
    STATUS = 0x1
    SPECIFIC_INQUIRY = 0x2
    NOTIFY = 0x3
    GENERAL_INQUIRY = 0x4

    @staticmethod
    def from_int(v: int) -> Union[int, "Ctype"]:
        try:
            return Ctype(v)
        except ValueError:
            return v


# AV/C Response Code
class ResponseCode(enum.IntEnum):
    NOT_IMPLEMENTED = 0x8
    ACCEPTED = 0x9
    REJECTED = 0xA
    IN_TRANSITION = 0xB
    IMPLEMENTED_STABLE = 0xC
    CHANGED = 0xD
    INTERIM = 0xF

    @staticmethod
    def from_int(v: int) -> Union[int, "ResponseCode"]:
        try:
            return ResponseCode(v)
        except ValueError:
            return v


# AV/C Subunit Type
class SubunitType(enum.IntEnum):
    MONITOR = 0x00
    AUDIO = 0x01
    PRINTER = 0x02
    DISC = 0x03
    TAPE_RECORDER_PLAYER = 0x04
    TUNER = 0x05
    CA = 0x06
    CAMERA = 0x07
    PANEL = 0x09
    BULLETIN_BOARD = 0x0A
    CAMERA_STORAGE = 0x0B
    VENDOR_UNIQUE = 0x1C
    UNIT = 0x1F

    @staticmethod
    def from_int(v: int) -> Union[int, "SubunitType"]:
        try:
            return SubunitType(v)
        except ValueError:
            return v


# AV/C Opcode
class Opcode(enum.IntEnum):
    VENDOR_DEPENDENT = 0x00
    UNIT_INFO = 0x30
    SUBUNIT_INFO = 0x31
    PASS_THROUGH = 0x7C

    @staticmethod
    def from_int(v: int) -> Union[int, "Opcode"]:
        try:
            return Opcode(v)
        except ValueError:
            return v


# AVRCP Passthrough Operation ID
class PassthroughOpId(enum.IntEnum):
    SELECT = 0x00
    UP = 0x01
    DOWN = 0x02
    LEFT = 0x03
    RIGHT = 0x04
    RIGHT_UP = 0x05
    RIGHT_DOWN = 0x06
    LEFT_UP = 0x07
    LEFT_DOWN = 0x08
    ROOT_MENU = 0x09
    SETUP_MENU = 0x0A
    CONTENTS_MENU = 0x0B
    FAVORITE_MENU = 0x0C
    EXIT = 0x0D
    CHANNEL_UP = 0x30
    CHANNEL_DOWN = 0x31
    PREVIOUS_CHANNEL = 0x32
    SOUND_SELECT = 0x33
    INPUT_SELECT = 0x34
    DISPLAY_INFORMATION = 0x35
    HELP = 0x36
    PAGE_UP = 0x37
    PAGE_DOWN = 0x38
    POWER = 0x40
    VOLUME_UP = 0x41
    VOLUME_DOWN = 0x42
    MUTE = 0x43
    PLAY = 0x44
    STOP = 0x45
    PAUSE = 0x46
    RECORD = 0x47
    REWIND = 0x48
    FAST_FORWARD = 0x49
    EJECT = 0x4A
    FORWARD = 0x4B
    BACKWARD = 0x4C
    ANGLE = 0x50
    SUBPICTURE = 0x51
    F1 = 0x71
    F2 = 0x72
    F3 = 0x73
    F4 = 0x74
    F5 = 0x75
    VENDOR_UNIQUE = 0x7E

    @staticmethod
    def from_int(v: int) -> Union[int, "PassthroughOpId"]:
        try:
            return PassthroughOpId(v)
        except ValueError:
            return v


# AVRCP PDU ID
class PduId(enum.IntEnum):
    GET_CAPABILITIES = 0x10
    LIST_PLAYER_APPLICATION_SETTING_ATTRIBUTES = 0x11
    LIST_PLAYER_APPLICATION_SETTING_VALUES = 0x12
    GET_CURRENT_PLAYER_APPLICATION_SETTING_VALUE = 0x13
    SET_PLAYER_APPLICATION_SETTING_VALUE = 0x14
    GET_PLAYER_APPLICATION_SETTING_ATTRIBUTE_TEXT = 0x15
    GET_PLAYER_APPLICATION_SETTING_VALUE_TEXT = 0x16
    INFORM_DISPLAYABLE_CHARACTER_SET = 0x17
    INFORM_BATTERY_STATUS_OF_CT = 0x18
    GET_ELEMENT_ATTRIBUTES = 0x20
    GET_PLAY_STATUS = 0x30
    REGISTER_NOTIFICATION = 0x31
    REQUEST_CONTINUING_RESPONSE = 0x40
    ABORT_CONTINUING_RESPONSE = 0x41
    SET_ABSOLUTE_VOLUME = 0x50
    SET_ADDRESSED_PLAYER = 0x60
    SET_BROWSED_PLAYER = 0x70
    GET_FOLDER_ITEMS = 0x71
    CHANGE_PATH = 0x72
    GET_ITEM_ATTRIBUTES = 0x73
    PLAY_ITEM = 0x74
    GET_TOTAL_NUMBER_OF_ITEMS = 0x75
    SEARCH = 0x80
    ADD_TO_NOW_PLAYING = 0x90
    GENERAL_REJECT = 0xA0

    @staticmethod
    def from_int(v: int) -> Union[int, "PduId"]:
        try:
            return PduId(v)
        except ValueError:
            return v


# AVRCP Event ID
class EventId(enum.IntEnum):
    PLAYBACK_STATUS_CHANGED = 0x01
    TRACK_CHANGED = 0x02
    TRACK_REACHED_END = 0x03
    TRACK_REACHED_START = 0x04
    PLAYBACK_POS_CHANGED = 0x05
    BATT_STATUS_CHANGED = 0x06
    SYSTEM_STATUS_CHANGED = 0x07
    PLAYER_APPLICATION_SETTING_CHANGED = 0x08
    NOW_PLAYING_CONTENT_CHANGED = 0x09
    AVAILABLE_PLAYERS_CHANGED = 0x0A
    ADDRESSED_PLAYER_CHANGED = 0x0B
    UIDS_CHANGED = 0x0C
    VOLUME_CHANGED = 0x0D

    @staticmethod
    def from_int(v: int) -> Union[int, "EventId"]:
        try:
            return EventId(v)
        except ValueError:
            return v


# AVRCP Play Status
class PlayStatus(enum.IntEnum):
    STOPPED = 0x00
    PLAYING = 0x01
    PAUSED = 0x02
    FWD_SEEK = 0x03
    REV_SEEK = 0x04
    ERROR = 0xFF

    @staticmethod
    def from_int(v: int) -> Union[int, "PlayStatus"]:
        try:
            return PlayStatus(v)
        except ValueError:
            return v


# AVRCP Status Code
class StatusCode(enum.IntEnum):
    INVALID_COMMAND = 0x00
    INVALID_PARAMETER = 0x01
    PARAMETER_CONTENT_ERROR = 0x02
    INTERNAL_ERROR = 0x03
    OPERATION_COMPLETED = 0x04
    UID_CHANGED = 0x05
    INVALID_DIRECTION = 0x07
    NOT_A_DIRECTORY = 0x08
    DOES_NOT_EXIST = 0x09
    INVALID_SCOPE = 0x0A
    RANGE_OUT_OF_BOUNDS = 0x0B
    FOLDER_ITEM_NOT_PLAYABLE = 0x0C
    MEDIA_IN_USE = 0x0D
    NOW_PLAYING_LIST_FULL = 0x0E
    SEARCH_NOT_SUPPORTED = 0x0F
    SEARCH_IN_PROGRESS = 0x10
    INVALID_PLAYER_ID = 0x11
    PLAYER_NOT_BROWSABLE = 0x12
    PLAYER_NOT_ADDRESSED = 0x13
    NO_VALID_SEARCH_RESULTS = 0x14
    NO_AVAILABLE_PLAYERS = 0x15
    ADDRESSED_PLAYER_CHANGED = 0x16

    @staticmethod
    def from_int(v: int) -> Union[int, "StatusCode"]:
        try:
            return StatusCode(v)
        except ValueError:
            return v


# Capability ID
class CapabilityId(enum.IntEnum):
    COMPANY_ID = 0x02
    EVENTS_SUPPORTED = 0x03

    @staticmethod
    def from_int(v: int) -> Union[int, "CapabilityId"]:
        try:
            return CapabilityId(v)
        except ValueError:
            return v


# Media Attribute ID
class MediaAttributeId(enum.IntEnum):
    ILLEGAL = 0x00
    TITLE = 0x01
    ARTIST_NAME = 0x02
    ALBUM_NAME = 0x03
    TRACK_NUMBER = 0x04
    TOTAL_NUMBER_OF_TRACKS = 0x05
    GENRE = 0x06
    PLAYING_TIME = 0x07
    DEFAULT_COVER_ART = 0x08

    @staticmethod
    def from_int(v: int) -> Union[int, "MediaAttributeId"]:
        try:
            return MediaAttributeId(v)
        except ValueError:
            return v


# Scope
class Scope(enum.IntEnum):
    MEDIA_PLAYER_LIST = 0x00
    MEDIA_PLAYER_VIRTUAL_FILESYSTEM = 0x01
    SEARCH = 0x02
    NOW_PLAYING = 0x03

    @staticmethod
    def from_int(v: int) -> Union[int, "Scope"]:
        try:
            return Scope(v)
        except ValueError:
            return v


# Direction
class Direction(enum.IntEnum):
    FOLDER_UP = 0x00
    FOLDER_DOWN = 0x01

    @staticmethod
    def from_int(v: int) -> Union[int, "Direction"]:
        try:
            return Direction(v)
        except ValueError:
            return v


# BT SIG Company ID for AVRCP
BT_SIG_COMPANY_ID = 0x001958


@dataclass
class AvcCommandFrame(Packet):
    """Base AV/C Command Frame."""
    ctype: Ctype = Ctype.CONTROL
    subunit_type: SubunitType = SubunitType.PANEL
    subunit_id: int = 0
    opcode: Opcode = Opcode.VENDOR_DEPENDENT

    @staticmethod
    def parse(span: bytes) -> Tuple["AvcCommandFrame", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        byte0 = span[0]
        ctype_val = byte0 & 0x0F
        subunit_type_val = (byte0 >> 3) & 0x1F

        byte1 = span[1]
        subunit_id = byte1 & 0x07

        opcode_val = span[2]

        ctype = Ctype.from_int(ctype_val)
        subunit_type = SubunitType.from_int(subunit_type_val)
        opcode = Opcode.from_int(opcode_val)

        return AvcCommandFrame(
            ctype=ctype,
            subunit_type=subunit_type,
            subunit_id=subunit_id,
            opcode=opcode,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class AvcResponseFrame(Packet):
    """Base AV/C Response Frame."""
    response: ResponseCode = ResponseCode.ACCEPTED
    subunit_type: SubunitType = SubunitType.PANEL
    subunit_id: int = 0
    opcode: Opcode = Opcode.VENDOR_DEPENDENT

    @staticmethod
    def parse(span: bytes) -> Tuple["AvcResponseFrame", bytes]:
        if len(span) < 3:
            raise Exception("Packet too short")

        byte0 = span[0]
        response_val = byte0 & 0x0F
        subunit_type_val = (byte0 >> 3) & 0x1F

        byte1 = span[1]
        subunit_id = byte1 & 0x07

        opcode_val = span[2]

        response = ResponseCode.from_int(response_val)
        subunit_type = SubunitType.from_int(subunit_type_val)
        opcode = Opcode.from_int(opcode_val)

        return AvcResponseFrame(
            response=response,
            subunit_type=subunit_type,
            subunit_id=subunit_id,
            opcode=opcode,
            payload=bytes(span[3:]),
        ), bytes()

    @property
    def size(self) -> int:
        return 3 + len(self.payload)


@dataclass
class PassThroughCommand(Packet):
    """PASS_THROUGH Command."""
    ctype: Ctype = Ctype.CONTROL
    state_flag: int = 0  # 0=pressed, 1=released
    operation_id: PassthroughOpId = PassthroughOpId.PLAY
    operation_data: bytes = field(default_factory=bytes)

    @staticmethod
    def parse(span: bytes) -> Tuple["PassThroughCommand", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        ctype_val = span[0] & 0x0F
        ctype = Ctype.from_int(ctype_val)

        state_flag = (span[3] >> 7) & 0x01
        op_id_val = span[3] & 0x7F
        operation_id = PassthroughOpId.from_int(op_id_val)

        op_data_length = span[4]
        operation_data = bytes(span[5:5 + op_data_length])

        return PassThroughCommand(
            ctype=ctype,
            state_flag=state_flag,
            operation_id=operation_id,
            operation_data=operation_data,
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.operation_data)


@dataclass
class PassThroughResponse(Packet):
    """PASS_THROUGH Response."""
    response: ResponseCode = ResponseCode.ACCEPTED
    state_flag: int = 0
    operation_id: PassthroughOpId = PassthroughOpId.PLAY
    operation_data: bytes = field(default_factory=bytes)

    @staticmethod
    def parse(span: bytes) -> Tuple["PassThroughResponse", bytes]:
        if len(span) < 5:
            raise Exception("Packet too short")

        response_val = span[0] & 0x0F
        response = ResponseCode.from_int(response_val)

        state_flag = (span[3] >> 7) & 0x01
        op_id_val = span[3] & 0x7F
        operation_id = PassthroughOpId.from_int(op_id_val)

        op_data_length = span[4]
        operation_data = bytes(span[5:5 + op_data_length])

        return PassThroughResponse(
            response=response,
            state_flag=state_flag,
            operation_id=operation_id,
            operation_data=operation_data,
        ), bytes()

    @property
    def size(self) -> int:
        return 5 + len(self.operation_data)


@dataclass
class VendorDependentCommand(Packet):
    """VENDOR_DEPENDENT Command."""
    ctype: Ctype = Ctype.CONTROL
    company_id: int = BT_SIG_COMPANY_ID
    pdu_id: PduId = PduId.GET_CAPABILITIES
    packet_type: int = 0
    parameter_length: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["VendorDependentCommand", bytes]:
        if len(span) < 10:
            raise Exception("Packet too short")

        ctype_val = span[0] & 0x0F
        ctype = Ctype.from_int(ctype_val)

        company_id = (span[3] << 16) | (span[4] << 8) | span[5]
        pdu_id_val = span[6]
        pdu_id = PduId.from_int(pdu_id_val)

        packet_type = (span[7] >> 6) & 0x03
        parameter_length = (span[8] << 8) | span[9]

        return VendorDependentCommand(
            ctype=ctype,
            company_id=company_id,
            pdu_id=pdu_id,
            packet_type=packet_type,
            parameter_length=parameter_length,
            payload=bytes(span[10:10 + parameter_length]),
        ), bytes()

    @property
    def size(self) -> int:
        return 10 + len(self.payload)


@dataclass
class VendorDependentResponse(Packet):
    """VENDOR_DEPENDENT Response."""
    response: ResponseCode = ResponseCode.ACCEPTED
    company_id: int = BT_SIG_COMPANY_ID
    pdu_id: PduId = PduId.GET_CAPABILITIES
    packet_type: int = 0
    parameter_length: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["VendorDependentResponse", bytes]:
        if len(span) < 10:
            raise Exception("Packet too short")

        response_val = span[0] & 0x0F
        response = ResponseCode.from_int(response_val)

        company_id = (span[3] << 16) | (span[4] << 8) | span[5]
        pdu_id_val = span[6]
        pdu_id = PduId.from_int(pdu_id_val)

        packet_type = (span[7] >> 6) & 0x03
        parameter_length = (span[8] << 8) | span[9]

        return VendorDependentResponse(
            response=response,
            company_id=company_id,
            pdu_id=pdu_id,
            packet_type=packet_type,
            parameter_length=parameter_length,
            payload=bytes(span[10:10 + parameter_length]),
        ), bytes()

    @property
    def size(self) -> int:
        return 10 + len(self.payload)


@dataclass
class GetPlayStatusResponse(Packet):
    """GET_PLAY_STATUS Response."""
    response: ResponseCode = ResponseCode.IMPLEMENTED_STABLE
    song_length: int = 0xFFFFFFFF
    song_position: int = 0xFFFFFFFF
    play_status: PlayStatus = PlayStatus.STOPPED

    @staticmethod
    def parse(span: bytes) -> Tuple["GetPlayStatusResponse", bytes]:
        if len(span) < 19:
            raise Exception("Packet too short")

        response_val = span[0] & 0x0F
        response = ResponseCode.from_int(response_val)

        # Skip to parameter data (after company_id, pdu_id, packet_type, param_length)
        song_length = (span[10] << 24) | (span[11] << 16) | (span[12] << 8) | span[13]
        song_position = (span[14] << 24) | (span[15] << 16) | (span[16] << 8) | span[17]
        play_status = PlayStatus.from_int(span[18])

        return GetPlayStatusResponse(
            response=response,
            song_length=song_length,
            song_position=song_position,
            play_status=play_status,
        ), bytes()


@dataclass
class SetAbsoluteVolumeCommand(Packet):
    """SET_ABSOLUTE_VOLUME Command."""
    ctype: Ctype = Ctype.CONTROL
    absolute_volume: int = 0  # 0x00-0x7F (0%-100%)

    @staticmethod
    def parse(span: bytes) -> Tuple["SetAbsoluteVolumeCommand", bytes]:
        if len(span) < 11:
            raise Exception("Packet too short")

        ctype_val = span[0] & 0x0F
        ctype = Ctype.from_int(ctype_val)

        absolute_volume = span[10] & 0x7F

        return SetAbsoluteVolumeCommand(
            ctype=ctype,
            absolute_volume=absolute_volume,
        ), bytes()


@dataclass
class SetAbsoluteVolumeResponse(Packet):
    """SET_ABSOLUTE_VOLUME Response."""
    response: ResponseCode = ResponseCode.ACCEPTED
    absolute_volume: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["SetAbsoluteVolumeResponse", bytes]:
        if len(span) < 11:
            raise Exception("Packet too short")

        response_val = span[0] & 0x0F
        response = ResponseCode.from_int(response_val)

        absolute_volume = span[10] & 0x7F

        return SetAbsoluteVolumeResponse(
            response=response,
            absolute_volume=absolute_volume,
        ), bytes()


@dataclass
class RegisterNotificationCommand(Packet):
    """REGISTER_NOTIFICATION Command."""
    ctype: Ctype = Ctype.NOTIFY
    event_id: EventId = EventId.VOLUME_CHANGED
    playback_interval: int = 0

    @staticmethod
    def parse(span: bytes) -> Tuple["RegisterNotificationCommand", bytes]:
        if len(span) < 15:
            raise Exception("Packet too short")

        ctype_val = span[0] & 0x0F
        ctype = Ctype.from_int(ctype_val)

        event_id = EventId.from_int(span[10])
        playback_interval = (span[11] << 24) | (span[12] << 16) | (span[13] << 8) | span[14]

        return RegisterNotificationCommand(
            ctype=ctype,
            event_id=event_id,
            playback_interval=playback_interval,
        ), bytes()


def parse_avc_frame(span: bytes, is_response: bool) -> Tuple[Packet, bytes]:
    """Parse an AV/C frame based on context."""
    if is_response:
        return AvcResponseFrame.parse(span)
    else:
        return AvcCommandFrame.parse(span)
