import btsnoop
import dataclasses
import struct
from packets import avctp
from packets import avrcp
from packets import l2cap
from typing import Dict, Optional
from btsnoop import Direction
from colorama import Fore, Style


AVRCP_PSM = 0x0017          # AVCTP Control
AVRCP_BROWSE_PSM = 0x001B   # AVCTP Browsing
AVRCP_PROFILE_ID = 0x110E   # AV Remote Control
AVRCP_TG_PROFILE_ID = 0x110C  # AV Remote Control Target


def log(ts, msg):
    print(Fore.CYAN + f"  {ts}" + Style.RESET_ALL + f" | {msg}")


def log_detail(msg):
    print(Fore.WHITE + f"           | " + Style.DIM + f"  {msg}" + Style.RESET_ALL)


def format_direction(direction: Direction) -> str:
    return "-->" if direction == Direction.SENT else "<--"


def enum_name(enum_cls, value: int) -> str:
    try:
        return enum_cls(value).name
    except ValueError:
        return f"0x{value:X}"


@dataclasses.dataclass
class AclPacket:
    packet: btsnoop.Packet
    channel_id: int
    direction: btsnoop.Direction
    payload: bytes

    @staticmethod
    def parse(packet: btsnoop.Packet) -> "AclPacket":
        payload = bytearray(packet.payload[4:])
        for fragment in packet.continuing_fragments:
            payload.extend(fragment.payload[4:])

        pdu_length, channel_id = struct.unpack("<HH", payload[:4])
        assert pdu_length == len(payload[4:])

        return AclPacket(packet, channel_id, packet.direction, bytes(payload[4:]))


@dataclasses.dataclass
class AvrcpPacket:
    acl_packet: AclPacket
    avctp_header: avctp.Packet
    avc_frame: Optional[avrcp.Packet] = None

    @property
    def is_response(self) -> bool:
        cr = getattr(self.avctp_header, "cr", None)
        return cr == avctp.MessageType.RESPONSE

    @property
    def transaction_label(self) -> int:
        return getattr(self.avctp_header, "transaction_label", 0)


def parse_avrcp_packet(acl_packet: AclPacket) -> Optional[AvrcpPacket]:
    data = acl_packet.payload
    if len(data) < 3:
        return None

    try:
        avctp_pkt, _ = avctp.parse_avctp(data)
    except Exception:
        return None

    pid = getattr(avctp_pkt, "pid", 0)
    if pid not in (AVRCP_PROFILE_ID, AVRCP_TG_PROFILE_ID):
        return None

    avc_payload = avctp_pkt.payload
    avc_frame = None
    if len(avc_payload) >= 3:
        try:
            if getattr(avctp_pkt, "cr", None) == avctp.MessageType.RESPONSE:
                avc_frame, _ = avrcp.AvcResponseFrame.parse(avc_payload)
            else:
                avc_frame, _ = avrcp.AvcCommandFrame.parse(avc_payload)
        except Exception:
            pass

    return AvrcpPacket(acl_packet=acl_packet, avctp_header=avctp_pkt, avc_frame=avc_frame)


def log_passthrough(operands: bytes) -> None:
    if len(operands) < 2:
        log_detail("(incomplete passthrough)")
        return
    state_flag = (operands[0] >> 7) & 0x01
    op_id = operands[0] & 0x7F
    log_detail(f"Operation: {enum_name(avrcp.PassthroughOpId, op_id)}")
    log_detail(f"State: {'RELEASED' if state_flag else 'PRESSED'}")
    if len(operands) > 2:
        log_detail(f"Data: {operands[2:].hex()}")


def log_vendor_dependent(operands: bytes, is_response: bool) -> None:
    if len(operands) < 7:
        log_detail("(incomplete vendor dependent)")
        return

    company_id = (operands[0] << 16) | (operands[1] << 8) | operands[2]
    pdu_id = operands[3]
    packet_type = (operands[4] >> 6) & 0x03
    param_length = (operands[5] << 8) | operands[6]
    params = operands[7:7 + param_length]

    packet_type_names = ["Single", "Start", "Continue", "End"]
    log_detail(
        f"Company ID: 0x{company_id:06X}"
        + (" (BT SIG)" if company_id == 0x001958 else "")
    )
    log_detail(f"Packet Type: {packet_type_names[packet_type]}")

    if pdu_id == avrcp.PduId.GET_PLAY_STATUS and is_response and len(params) >= 9:
        song_length = int.from_bytes(params[0:4], "big")
        song_position = int.from_bytes(params[4:8], "big")
        play_status = params[8]
        log_detail(f"Song Length: {song_length} ms"
                   + (" (N/A)" if song_length == 0xFFFFFFFF else ""))
        log_detail(f"Song Position: {song_position} ms"
                   + (" (N/A)" if song_position == 0xFFFFFFFF else ""))
        log_detail(f"Play Status: {enum_name(avrcp.PlayStatus, play_status)}")

    elif pdu_id == avrcp.PduId.REGISTER_NOTIFICATION and len(params) >= 1:
        event_id = params[0]
        log_detail(f"Event: {enum_name(avrcp.EventId, event_id)}")
        if len(params) >= 5 and event_id == avrcp.EventId.PLAYBACK_POS_CHANGED:
            interval = int.from_bytes(params[1:5], "big")
            log_detail(f"Interval: {interval} sec")
        elif is_response and len(params) > 1:
            if event_id == avrcp.EventId.PLAYBACK_STATUS_CHANGED and len(params) >= 2:
                log_detail(f"Status: {enum_name(avrcp.PlayStatus, params[1])}")
            elif event_id == avrcp.EventId.VOLUME_CHANGED and len(params) >= 2:
                volume = params[1] & 0x7F
                log_detail(f"Volume: {int(volume * 100 / 127)}% (0x{volume:02X})")
            elif event_id == avrcp.EventId.TRACK_CHANGED and len(params) >= 9:
                track_id = int.from_bytes(params[1:9], "big")
                log_detail(f"Track ID: 0x{track_id:016X}")

    elif pdu_id == avrcp.PduId.SET_ABSOLUTE_VOLUME and len(params) >= 1:
        volume = params[0] & 0x7F
        log_detail(f"Volume: {int(volume * 100 / 127)}% (0x{volume:02X})")

    elif pdu_id == avrcp.PduId.GET_CAPABILITIES and len(params) >= 1:
        cap_id = params[0]
        log_detail(f"Capability: {enum_name(avrcp.CapabilityId, cap_id)}")
        if is_response and len(params) >= 2:
            count = params[1]
            log_detail(f"Count: {count}")
            if cap_id == avrcp.CapabilityId.EVENTS_SUPPORTED and len(params) >= 2 + count:
                events = [enum_name(avrcp.EventId, params[2 + i]) for i in range(count)]
                log_detail(f"Events: {', '.join(events)}")

    elif pdu_id == avrcp.PduId.GET_ELEMENT_ATTRIBUTES and not is_response and len(params) >= 9:
        identifier = int.from_bytes(params[0:8], "big")
        num_attrs = params[8]
        log_detail(f"Identifier: 0x{identifier:016X}"
                   + (" (now playing)" if identifier == 0 else ""))
        log_detail(f"Num Attributes: {num_attrs}" + (" (all)" if num_attrs == 0 else ""))

    elif params:
        log_detail(f"Params: {params.hex()}")


def log_avrcp_packet(pkt: AvrcpPacket) -> None:
    acl = pkt.acl_packet
    avc = pkt.avc_frame
    is_response = pkt.is_response
    direction_str = format_direction(acl.direction)
    msg_type = "RSP" if is_response else "CMD"

    if avc and is_response and isinstance(avc, avrcp.AvcResponseFrame):
        type_str = enum_name(avrcp.ResponseCode, int(avc.response))
    elif avc and isinstance(avc, avrcp.AvcCommandFrame):
        type_str = enum_name(avrcp.Ctype, int(avc.ctype))
    else:
        type_str = "?"

    opcode = int(avc.opcode) if avc and hasattr(avc, "opcode") else 0
    operands = avc.payload if avc else b""

    if opcode == avrcp.Opcode.PASS_THROUGH:
        if len(operands) >= 2:
            op_id = operands[0] & 0x7F
            state_str = "released" if (operands[0] >> 7) & 0x01 else "pressed"
            op_name = enum_name(avrcp.PassthroughOpId, op_id)
            log(acl.packet.timestamp,
                f"{direction_str} PASS_THROUGH {op_name} ({state_str}) [{type_str}]")
            log_passthrough(operands)
        return

    if opcode == avrcp.Opcode.VENDOR_DEPENDENT:
        if len(operands) >= 7:
            pdu_name = enum_name(avrcp.PduId, operands[3])
            log(acl.packet.timestamp,
                f"{direction_str} AVRCP {msg_type} TL={pkt.transaction_label} "
                f"[{type_str}] {pdu_name}")
            log_vendor_dependent(operands, is_response)
        return

    opcode_name = enum_name(avrcp.Opcode, opcode)
    log(acl.packet.timestamp,
        f"{direction_str} AVRCP {msg_type} TL={pkt.transaction_label} "
        f"[{type_str}] {opcode_name}")

    if opcode == avrcp.Opcode.UNIT_INFO and is_response and len(operands) >= 5:
        unit_type = (operands[1] >> 3) & 0x1F
        company_id = (operands[2] << 16) | (operands[3] << 8) | operands[4]
        log_detail(f"Unit Type: 0x{unit_type:02X}")
        log_detail(f"Company ID: 0x{company_id:06X}")
    elif opcode == avrcp.Opcode.SUBUNIT_INFO and len(operands) >= 1:
        log_detail(f"Page: {(operands[0] >> 4) & 0x07}")


def discover_avrcp_channels(acl_packets, override_cids) -> Dict[int, int]:
    """Map CID -> PSM for AVRCP channels seen on this ACL connection."""
    channels: Dict[int, int] = {}
    non_avrcp_cids: set = set()

    if override_cids:
        for cid in override_cids:
            channels[cid] = AVRCP_PSM

    for acl_packet in acl_packets:
        if acl_packet.channel_id == 0x0001:
            try:
                signal = l2cap.SignalingPacket.parse_all(acl_packet.payload)
            except Exception:
                continue
            if isinstance(signal, l2cap.ConnectionRequest):
                if signal.psm in (AVRCP_PSM, AVRCP_BROWSE_PSM):
                    channels[signal.source_cid] = signal.psm
            elif isinstance(signal, l2cap.ConnectionResponse):
                if signal.source_cid in channels:
                    channels[signal.destination_cid] = channels[signal.source_cid]
                elif signal.destination_cid in channels:
                    channels[signal.source_cid] = channels[signal.destination_cid]
        elif (acl_packet.channel_id not in (0x0001, 0x0002)
              and acl_packet.channel_id not in channels
              and acl_packet.channel_id not in non_avrcp_cids):
            if len(acl_packet.payload) >= 3:
                try:
                    avctp_pkt, _ = avctp.parse_avctp(acl_packet.payload)
                    pid = getattr(avctp_pkt, "pid", 0)
                    if pid in (AVRCP_PROFILE_ID, AVRCP_TG_PROFILE_ID):
                        channels[acl_packet.channel_id] = AVRCP_PSM
                    else:
                        non_avrcp_cids.add(acl_packet.channel_id)
                except Exception:
                    non_avrcp_cids.add(acl_packet.channel_id)

    return channels


def plot_acl_connection(acl_connection: btsnoop.AclConnection,
                        control_cid: Optional[int] = None,
                        browse_cid: Optional[int] = None,
                        **kwargs):
    acl_packets = [AclPacket.parse(p) for p in acl_connection.packets]

    override_cids = [c for c in (control_cid, browse_cid) if c]
    channels = discover_avrcp_channels(acl_packets, override_cids)
    if not channels:
        return

    started_ts = (acl_connection.connected.timestamp
                  if acl_connection.connected else acl_packets[0].packet.timestamp)
    print(f"\n" + Fore.MAGENTA + "=" * 80 + Style.RESET_ALL)
    print(Fore.MAGENTA
          + f"AVRCP Session Analysis - Connection Handle "
            f"0x{acl_connection.connection_handle:04x}"
          + Style.RESET_ALL)
    print(Fore.MAGENTA + f"Started: {started_ts}" + Style.RESET_ALL)
    print(Fore.MAGENTA + f"Channels: "
          + ", ".join(f"0x{c:04x}({'browse' if p == AVRCP_BROWSE_PSM else 'control'})"
                      for c, p in channels.items())
          + Style.RESET_ALL)
    print(Fore.MAGENTA + "=" * 80 + Style.RESET_ALL + "\n")

    packet_count = 0
    cmd_count = 0
    rsp_count = 0
    passthrough_count = 0
    vendor_count = 0

    for acl_packet in acl_packets:
        if acl_packet.channel_id not in channels:
            continue

        avrcp_pkt = parse_avrcp_packet(acl_packet)
        if not avrcp_pkt:
            continue

        packet_count += 1
        if avrcp_pkt.is_response:
            rsp_count += 1
        else:
            cmd_count += 1

        if avrcp_pkt.avc_frame and hasattr(avrcp_pkt.avc_frame, "opcode"):
            opcode = int(avrcp_pkt.avc_frame.opcode)
            if opcode == avrcp.Opcode.PASS_THROUGH:
                passthrough_count += 1
            elif opcode == avrcp.Opcode.VENDOR_DEPENDENT:
                vendor_count += 1

        log_avrcp_packet(avrcp_pkt)

    print(f"\n" + Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
    print(Fore.MAGENTA + Style.BRIGHT + "Summary:" + Style.RESET_ALL)
    print(f"  Total AVRCP packets: {packet_count}")
    print(f"  Commands:            {cmd_count}")
    print(f"  Responses:           {rsp_count}")
    print(f"  Pass-through:        {passthrough_count}")
    print(f"  Vendor-dependent:    {vendor_count}")
    print(Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
