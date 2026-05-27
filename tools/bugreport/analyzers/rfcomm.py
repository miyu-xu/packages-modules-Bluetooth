import btsnoop
import dataclasses
import struct
from packets import l2cap
from packets import rfcomm
from typing import Dict, Optional, Set
from btsnoop import Direction
from colorama import Fore, Style


RFCOMM_PSM = 0x0003

# RFCOMM frame types — the wire byte has the P/F bit (0x10) set when the
# frame carries a poll or final indication. Mask it off before comparing
# against the FrameType enum.
PF_BIT = 0x10


def log(ts, msg):
    print(Fore.CYAN + f"  {ts}" + Style.RESET_ALL + f" | {msg}")


def log_detail(msg):
    print(Fore.WHITE + f"           | " + Style.DIM + f"  {msg}" + Style.RESET_ALL)


def format_direction(direction: Direction) -> str:
    return "-->" if direction == Direction.SENT else "<--"


def frame_type_name(byte: int) -> str:
    masked = byte & ~PF_BIT
    try:
        return rfcomm.FrameType(masked).name
    except ValueError:
        return f"0x{byte:02X}"


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


def parse_rfcomm(data: bytes):
    """Parse a single RFCOMM frame, picking short vs long form based on length EA bit."""
    if len(data) < 3:
        return None
    if data[2] & 0x01:
        return rfcomm.RfcommShortFrame.parse_all(data[:-1])  # strip FCS
    return rfcomm.RfcommLongFrame.parse_all(data[:-1])


def discover_rfcomm_channels(acl_packets, override_cids) -> Dict[int, str]:
    """Return CID -> role ('local' or 'remote') for RFCOMM channels."""
    channels: Dict[int, str] = {}
    non_rfcomm_cids: set = set()

    for cid in override_cids or ():
        channels[cid] = "override"

    for acl_packet in acl_packets:
        if acl_packet.channel_id != 0x0001:
            continue
        try:
            signal = l2cap.SignalingPacket.parse_all(acl_packet.payload)
        except Exception:
            continue
        if isinstance(signal, l2cap.ConnectionRequest):
            if signal.psm == RFCOMM_PSM:
                channels[signal.source_cid] = "remote"
        elif isinstance(signal, l2cap.ConnectionResponse):
            if signal.source_cid in channels:
                channels[signal.destination_cid] = "local"
            elif signal.destination_cid in channels:
                channels[signal.source_cid] = "local"

    # Heuristic fallback: look for plausible RFCOMM frames (SABM/UA on DLCI 0)
    # on any unclassified CID. Useful when L2CAP setup is missing from the snoop.
    if not channels:
        for acl_packet in acl_packets:
            if (acl_packet.channel_id in (0x0001, 0x0002)
                    or acl_packet.channel_id in channels
                    or acl_packet.channel_id in non_rfcomm_cids):
                continue
            data = acl_packet.payload
            if len(data) < 4:
                continue
            ft = data[1] & ~PF_BIT
            dlci = (data[0] >> 2) & 0x3F
            if dlci == 0 and ft in (int(rfcomm.FrameType.SABM),
                                    int(rfcomm.FrameType.UA)):
                channels[acl_packet.channel_id] = "discovered"

    return channels


def plot_acl_connection(acl_connection: btsnoop.AclConnection,
                        signal_lcid: Optional[int] = None,
                        signal_rcid: Optional[int] = None,
                        **_kwargs):
    acl_packets = [AclPacket.parse(p) for p in acl_connection.packets]
    if not acl_packets:
        return

    override_cids = [c for c in (signal_lcid, signal_rcid) if c]
    channels = discover_rfcomm_channels(acl_packets, override_cids)
    if not channels:
        return

    started_ts = (acl_connection.connected.timestamp
                  if acl_connection.connected else acl_packets[0].packet.timestamp)
    print(f"\n" + Fore.MAGENTA + "=" * 80 + Style.RESET_ALL)
    print(Fore.MAGENTA
          + f"RFCOMM Session Analysis - Connection Handle "
            f"0x{acl_connection.connection_handle:04x}"
          + Style.RESET_ALL)
    print(Fore.MAGENTA + f"Started: {started_ts}" + Style.RESET_ALL)
    print(Fore.MAGENTA + f"L2CAP CIDs: "
          + ", ".join(f"0x{c:04x}({r})" for c, r in channels.items())
          + Style.RESET_ALL)
    print(Fore.MAGENTA + "=" * 80 + Style.RESET_ALL + "\n")

    frame_count = 0
    by_type: Dict[str, int] = {}
    dlcis_seen: Set[int] = set()

    for acl_packet in acl_packets:
        if acl_packet.channel_id not in channels:
            continue

        try:
            frame = parse_rfcomm(acl_packet.payload)
        except Exception as e:
            log(acl_packet.packet.timestamp,
                f"{format_direction(acl_packet.direction)} "
                f"RFCOMM [parse error: {e}]")
            continue
        if frame is None:
            continue

        frame_count += 1
        dlcis_seen.add(frame.dlci)
        ft_name = frame_type_name(frame.frame_type)
        by_type[ft_name] = by_type.get(ft_name, 0) + 1

        if isinstance(frame, rfcomm.RfcommLongFrame):
            length = (frame.length_high << 7) | frame.length_low
        else:
            length = frame.length

        cr_str = "C" if frame.cr else "R"
        pf_str = " P/F" if (frame.frame_type & PF_BIT) else ""
        direction_str = format_direction(acl_packet.direction)

        log(acl_packet.packet.timestamp,
            f"{direction_str} RFCOMM {ft_name}{pf_str} "
            f"DLCI={frame.dlci} [{cr_str}] len={length}")

        if length and frame.payload:
            log_detail(f"Data: {frame.payload[:length].hex()}")

    print(f"\n" + Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
    print(Fore.MAGENTA + Style.BRIGHT + "Summary:" + Style.RESET_ALL)
    print(f"  Total RFCOMM frames: {frame_count}")
    if dlcis_seen:
        print(f"  DLCIs seen:          {sorted(dlcis_seen)}")
    for ft, n in sorted(by_type.items(), key=lambda kv: -kv[1]):
        print(f"  {ft:20s} {n}")
    print(Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
