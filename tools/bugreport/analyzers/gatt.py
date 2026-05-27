import btsnoop
import dataclasses
import struct
from packets import att
from packets import l2cap
from typing import Dict, Optional, Set
from btsnoop import Direction
from colorama import Fore, Style


# ATT uses L2CAP fixed channel CID 0x0004
ATT_CID = 0x0004

# L2CAP LE signaling channel
L2CAP_SIGNALING_CID_LE = 0x0005

# EATT uses LE Credit Based Connection (PSM 0x0027)
EATT_PSM = 0x0027


GATT_UUID16_NAMES = {
    # GATT Attribute Types
    0x2800: "Primary Service",
    0x2801: "Secondary Service",
    0x2802: "Include",
    0x2803: "Characteristic",
    # GATT Descriptors
    0x2900: "Char Extended Properties",
    0x2901: "Char User Description",
    0x2902: "Client Char Configuration",
    0x2903: "Server Char Configuration",
    0x2904: "Char Presentation Format",
    0x2905: "Char Aggregate Format",
    # Common GATT Services
    0x1800: "Generic Access",
    0x1801: "Generic Attribute",
    0x180A: "Device Information",
    0x180F: "Battery Service",
    0x1812: "HID Service",
    0x181C: "User Data",
    0x1844: "Volume Control",
    0x1846: "Coordinated Set Identification",
    0x184D: "Microphone Control",
    0x184E: "Audio Stream Control",
    0x184F: "Broadcast Audio Scan",
    0x1850: "Published Audio Capabilities",
    0x1853: "Common Audio",
    0x1854: "Hearing Access",
    0x1855: "TMAS",
    0x1856: "Public Broadcast Announcement",
    # Common Characteristics
    0x2A00: "Device Name",
    0x2A01: "Appearance",
    0x2A04: "Peripheral Preferred Conn Params",
    0x2A05: "Service Changed",
    0x2A19: "Battery Level",
    0x2A29: "Manufacturer Name",
    0x2A24: "Model Number",
    0x2A25: "Serial Number",
    0x2A26: "Firmware Revision",
    0x2A27: "Hardware Revision",
    0x2A28: "Software Revision",
    0x2B3A: "Server Supported Features",
    0x2B29: "Client Supported Features",
    0x2BDA: "Database Hash",
}


ATT_ERROR_CODE_NAMES = {
    0x01: "Invalid Handle",
    0x02: "Read Not Permitted",
    0x03: "Write Not Permitted",
    0x04: "Invalid PDU",
    0x05: "Insufficient Authentication",
    0x06: "Request Not Supported",
    0x07: "Invalid Offset",
    0x08: "Insufficient Authorization",
    0x09: "Prepare Queue Full",
    0x0A: "Attribute Not Found",
    0x0B: "Attribute Not Long",
    0x0C: "Insufficient Encryption Key Size",
    0x0D: "Invalid Attribute Value Length",
    0x0E: "Unlikely Error",
    0x0F: "Insufficient Encryption",
    0x10: "Unsupported Group Type",
    0x11: "Insufficient Resources",
    0x13: "Value Not Allowed",
}


def log(ts, msg):
    print(Fore.CYAN + f"  {ts}" + Style.RESET_ALL + f" | {msg}")


def log_detail(msg):
    print(Fore.WHITE + f"           | " + Style.DIM + f"  {msg}" + Style.RESET_ALL)


def format_direction(direction: Direction) -> str:
    return "-->" if direction == Direction.SENT else "<--"


def format_latency(latency_us: int) -> str:
    if latency_us < 1000:
        return f"{latency_us}us"
    if latency_us < 1_000_000:
        return f"{latency_us / 1000:.1f}ms"
    return f"{latency_us / 1_000_000:.2f}s"


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


def format_uuid(data: bytes) -> str:
    if len(data) == 2:
        uuid16 = struct.unpack_from("<H", data, 0)[0]
        name = GATT_UUID16_NAMES.get(uuid16)
        return f"0x{uuid16:04X} ({name})" if name else f"0x{uuid16:04X}"
    elif len(data) == 16:
        b = bytes(reversed(data))
        return (
            f"{b[0:4].hex()}-{b[4:6].hex()}-{b[6:8].hex()}-"
            f"{b[8:10].hex()}-{b[10:16].hex()}"
        )
    return data.hex()


def format_handle(handle: int) -> str:
    return f"0x{handle:04X}"


def format_opcode(opcode) -> str:
    if isinstance(opcode, att.Opcode):
        return opcode.name.replace("_", " ").title()
    return f"0x{int(opcode):02X}"


def format_error_code(code) -> str:
    int_code = int(code)
    name = ATT_ERROR_CODE_NAMES.get(int_code)
    if name:
        return f"0x{int_code:02X} ({name})"
    if 0x80 <= int_code <= 0x9F:
        return f"0x{int_code:02X} (Application Error)"
    if 0xE0 <= int_code <= 0xFF:
        return f"0x{int_code:02X} (Common Profile/Service Error)"
    return f"0x{int_code:02X}"


def format_value_preview(data: bytes, max_len: int = 32) -> str:
    if not data:
        return "(empty)"
    hex_str = bytes(data[:max_len]).hex()
    if len(data) > max_len:
        hex_str += "..."
    return hex_str


def format_channel(cid: int, eatt_cids: Set[int]) -> str:
    if cid == ATT_CID:
        return "ATT"
    if cid in eatt_cids:
        return f"EATT(0x{cid:04X})"
    return f"CID(0x{cid:04X})"


def plot_acl_connection(acl_connection: btsnoop.AclConnection, **_kwargs):
    # Cheap pre-check: look for ATT CID at bytes [6:8] of raw payload
    has_att = any(
        len(p.payload) >= 8
        and struct.unpack_from("<H", p.payload, 6)[0] == ATT_CID
        for p in acl_connection.packets
    )
    if not has_att:
        return

    acl_packets = [AclPacket.parse(p) for p in acl_connection.packets]

    started_ts = (acl_connection.connected.timestamp
                  if acl_connection.connected else acl_packets[0].packet.timestamp)
    print(f"\n" + Fore.MAGENTA + "=" * 80 + Style.RESET_ALL)
    print(Fore.MAGENTA
          + f"GATT Analysis - Connection Handle "
            f"0x{acl_connection.connection_handle:04x}"
          + Style.RESET_ALL)
    print(Fore.MAGENTA + f"Started: {started_ts}" + Style.RESET_ALL)
    print(Fore.MAGENTA + "=" * 80 + Style.RESET_ALL + "\n")

    # ---- EATT bearer discovery ----
    eatt_cids: Set[int] = set()
    pending_eatt: Dict[int, AclPacket] = {}

    for acl_pkt in acl_packets:
        if acl_pkt.channel_id != L2CAP_SIGNALING_CID_LE:
            continue
        try:
            signal = l2cap.SignalingPacket.parse_all(acl_pkt.payload)
        except (ValueError, struct.error):
            continue

        if isinstance(signal, l2cap.LeCreditBasedConnectionRequest):
            if signal.psm == EATT_PSM:
                pending_eatt[signal.identifier] = acl_pkt
                log(acl_pkt.packet.timestamp,
                    f"{format_direction(acl_pkt.direction)} EATT L2CAP CoC Request "
                    f"scid=0x{signal.source_cid:04X} mtu={signal.mtu} mps={signal.mps}")

        elif isinstance(signal, l2cap.LeCreditBasedConnectionResponse):
            if signal.identifier in pending_eatt:
                req_pkt = pending_eatt.pop(signal.identifier)
                if signal.result == 0:
                    eatt_cids.add(signal.destination_cid)
                    latency_us = (acl_pkt.packet.timestamp_us
                                  - req_pkt.packet.timestamp_us)
                    log(acl_pkt.packet.timestamp,
                        Fore.GREEN
                        + f"EATT bearer opened dcid=0x{signal.destination_cid:04X} "
                          f"mtu={signal.mtu}" + Style.RESET_ALL
                        + f" [latency: {format_latency(latency_us)}]")
                else:
                    log(acl_pkt.packet.timestamp,
                        Fore.RED + f"EATT bearer FAILED result=0x{signal.result:04X}"
                        + Style.RESET_ALL)

    # ---- ATT PDU processing ----
    att_cids = {ATT_CID} | eatt_cids

    stats = {
        "total_pdus": 0,
        "requests": 0,
        "responses": 0,
        "commands": 0,
        "notifications": 0,
        "indications": 0,
        "confirmations": 0,
        "errors": 0,
        "mtu_exchanges": 0,
        "service_discoveries": 0,
        "reads": 0,
        "writes": 0,
        "eatt_bearers": len(eatt_cids),
    }

    pending_requests: Dict[int, AclPacket] = {}
    pending_indications: Dict[int, AclPacket] = {}
    mtu_per_cid: Dict[int, int] = {}
    effective_mtu: Optional[int] = None

    def take_request(cid: int, ts_us: int) -> str:
        if cid in pending_requests:
            req_acl = pending_requests.pop(cid)
            return f" [latency: {format_latency(ts_us - req_acl.packet.timestamp_us)}]"
        return ""

    for acl_pkt in acl_packets:
        if acl_pkt.channel_id not in att_cids:
            continue
        if len(acl_pkt.payload) < 1:
            continue

        try:
            att_pdu, _ = att.parse_att(acl_pkt.payload)
        except Exception:
            continue

        stats["total_pdus"] += 1
        direction_str = format_direction(acl_pkt.direction)
        cid = acl_pkt.channel_id
        chan = format_channel(cid, eatt_cids)
        ts = acl_pkt.packet.timestamp
        ts_us = acl_pkt.packet.timestamp_us

        if isinstance(att_pdu, att.ErrorRsp):
            stats["errors"] += 1
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            log(ts,
                Fore.RED + f"[{chan}] Error Response: "
                f"{format_error_code(att_pdu.error_code)}" + Style.RESET_ALL
                + f" (for {format_opcode(att_pdu.request_opcode_in_error)} "
                f"handle={format_handle(att_pdu.attribute_handle_in_error)})"
                + latency_str)

        elif isinstance(att_pdu, att.ExchangeMtuReq):
            stats["requests"] += 1
            stats["mtu_exchanges"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Exchange MTU Request: "
                f"client_rx_mtu={att_pdu.client_rx_mtu}")

        elif isinstance(att_pdu, att.ExchangeMtuRsp):
            stats["responses"] += 1
            client_mtu = None
            latency_str = ""
            if cid in pending_requests:
                req_acl = pending_requests.pop(cid)
                latency_us = ts_us - req_acl.packet.timestamp_us
                latency_str = f" [latency: {format_latency(latency_us)}]"
                try:
                    req_pdu, _ = att.parse_att(req_acl.payload)
                    if isinstance(req_pdu, att.ExchangeMtuReq):
                        client_mtu = req_pdu.client_rx_mtu
                except Exception:
                    pass

            server_mtu = att_pdu.server_rx_mtu
            if client_mtu is not None:
                negotiated = min(client_mtu, server_mtu)
                mtu_per_cid[cid] = negotiated
                if cid == ATT_CID:
                    effective_mtu = negotiated
                log(ts,
                    f"{direction_str} [{chan}] Exchange MTU Response: "
                    f"server_rx_mtu={server_mtu}, "
                    + Fore.GREEN + f"negotiated={negotiated}" + Style.RESET_ALL
                    + latency_str)
            else:
                log(ts,
                    f"{direction_str} [{chan}] Exchange MTU Response: "
                    f"server_rx_mtu={server_mtu}" + latency_str)

        elif isinstance(att_pdu, att.ReadByGroupTypeReq):
            stats["requests"] += 1
            stats["service_discoveries"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Read By Group Type Request: "
                f"handles={format_handle(att_pdu.starting_handle)}-"
                f"{format_handle(att_pdu.ending_handle)} "
                f"uuid={format_uuid(att_pdu.payload)}")

        elif isinstance(att_pdu, att.ReadByGroupTypeRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            entry_len = att_pdu.length
            data = att_pdu.payload
            n_entries = len(data) // entry_len if entry_len > 0 else 0
            log(ts,
                f"{direction_str} [{chan}] Read By Group Type Response: "
                f"{n_entries} entries (each {entry_len}B)" + latency_str)
            if entry_len >= 6:
                offset = 0
                while offset + entry_len <= len(data):
                    s_handle = struct.unpack_from("<H", data, offset)[0]
                    e_handle = struct.unpack_from("<H", data, offset + 2)[0]
                    svc_uuid = data[offset + 4:offset + entry_len]
                    log_detail(f"Service {format_handle(s_handle)}-"
                               f"{format_handle(e_handle)}: "
                               f"{format_uuid(bytes(svc_uuid))}")
                    offset += entry_len

        elif isinstance(att_pdu, att.ReadByTypeReq):
            stats["requests"] += 1
            stats["service_discoveries"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Read By Type Request: "
                f"handles={format_handle(att_pdu.starting_handle)}-"
                f"{format_handle(att_pdu.ending_handle)} "
                f"uuid={format_uuid(att_pdu.payload)}")

        elif isinstance(att_pdu, att.ReadByTypeRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            entry_len = att_pdu.length
            data = att_pdu.payload
            n_entries = len(data) // entry_len if entry_len > 0 else 0
            log(ts,
                f"{direction_str} [{chan}] Read By Type Response: "
                f"{n_entries} entries (each {entry_len}B)" + latency_str)
            if entry_len >= 2:
                offset = 0
                while offset + entry_len <= len(data):
                    handle = struct.unpack_from("<H", data, offset)[0]
                    value = data[offset + 2:offset + entry_len]
                    log_detail(f"Handle {format_handle(handle)}: "
                               f"{format_value_preview(bytes(value))}")
                    offset += entry_len

        elif isinstance(att_pdu, att.FindInformationReq):
            stats["requests"] += 1
            stats["service_discoveries"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Find Information Request: "
                f"handles={format_handle(att_pdu.starting_handle)}-"
                f"{format_handle(att_pdu.ending_handle)}")

        elif isinstance(att_pdu, att.FindInformationRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            fmt = att_pdu.format
            uuid_size = 2 if fmt == att.FindInfoFormat.UUID_16BIT else 16
            entry_size = 2 + uuid_size
            data = att_pdu.payload
            n_entries = len(data) // entry_size if entry_size > 0 else 0
            log(ts,
                f"{direction_str} [{chan}] Find Information Response: "
                f"{n_entries} entries ({uuid_size * 8}-bit UUIDs)" + latency_str)
            offset = 0
            while offset + entry_size <= len(data):
                handle = struct.unpack_from("<H", data, offset)[0]
                uuid_bytes = data[offset + 2:offset + 2 + uuid_size]
                log_detail(f"Handle {format_handle(handle)}: "
                           f"{format_uuid(bytes(uuid_bytes))}")
                offset += entry_size

        elif isinstance(att_pdu, att.FindByTypeValueReq):
            stats["requests"] += 1
            stats["service_discoveries"] += 1
            pending_requests[cid] = acl_pkt
            uuid_str = format_uuid(struct.pack("<H", att_pdu.attribute_type))
            log(ts,
                f"{direction_str} [{chan}] Find By Type Value Request: "
                f"handles={format_handle(att_pdu.starting_handle)}-"
                f"{format_handle(att_pdu.ending_handle)} "
                f"type={uuid_str} value={format_value_preview(att_pdu.payload)}")

        elif isinstance(att_pdu, att.FindByTypeValueRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            data = att_pdu.payload
            n_entries = len(data) // 4
            log(ts,
                f"{direction_str} [{chan}] Find By Type Value Response: "
                f"{n_entries} handle ranges" + latency_str)

        elif isinstance(att_pdu, att.ReadReq):
            stats["requests"] += 1
            stats["reads"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Read Request: "
                f"handle={format_handle(att_pdu.attribute_handle)}")

        elif isinstance(att_pdu, att.ReadRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            log(ts,
                f"{direction_str} [{chan}] Read Response: "
                f"{len(att_pdu.payload)}B" + latency_str)
            log_detail(f"Value: {format_value_preview(att_pdu.payload)}")

        elif isinstance(att_pdu, att.ReadBlobReq):
            stats["requests"] += 1
            stats["reads"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Read Blob Request: "
                f"handle={format_handle(att_pdu.attribute_handle)} "
                f"offset={att_pdu.value_offset}")

        elif isinstance(att_pdu, att.ReadBlobRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            log(ts,
                f"{direction_str} [{chan}] Read Blob Response: "
                f"{len(att_pdu.payload)}B" + latency_str)

        elif isinstance(att_pdu, (att.ReadMultipleReq, att.ReadMultipleVariableReq)):
            stats["requests"] += 1
            stats["reads"] += 1
            pending_requests[cid] = acl_pkt
            handles = []
            data = att_pdu.payload
            off = 0
            while off + 2 <= len(data):
                handles.append(format_handle(struct.unpack_from("<H", data, off)[0]))
                off += 2
            multi_type = ("Variable "
                          if isinstance(att_pdu, att.ReadMultipleVariableReq) else "")
            log(ts,
                f"{direction_str} [{chan}] Read {multi_type}Multiple Request: "
                f"handles=[{', '.join(handles)}]")

        elif isinstance(att_pdu, (att.ReadMultipleRsp, att.ReadMultipleVariableRsp)):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            multi_type = ("Variable "
                          if isinstance(att_pdu, att.ReadMultipleVariableRsp) else "")
            log(ts,
                f"{direction_str} [{chan}] Read {multi_type}Multiple Response: "
                f"{len(att_pdu.payload)}B" + latency_str)

        elif isinstance(att_pdu, att.WriteReq):
            stats["requests"] += 1
            stats["writes"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Write Request: "
                f"handle={format_handle(att_pdu.attribute_handle)} "
                f"{len(att_pdu.payload)}B")
            log_detail(f"Value: {format_value_preview(att_pdu.payload)}")

        elif isinstance(att_pdu, att.WriteRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            log(ts, f"{direction_str} [{chan}] Write Response" + latency_str)

        elif isinstance(att_pdu, att.WriteCmd):
            stats["commands"] += 1
            stats["writes"] += 1
            log(ts,
                f"{direction_str} [{chan}] Write Command: "
                f"handle={format_handle(att_pdu.attribute_handle)} "
                f"{len(att_pdu.payload)}B")

        elif isinstance(att_pdu, att.SignedWriteCmd):
            stats["commands"] += 1
            stats["writes"] += 1
            log(ts,
                f"{direction_str} [{chan}] Signed Write Command: "
                f"handle={format_handle(att_pdu.attribute_handle)} "
                f"{len(att_pdu.payload)}B")

        elif isinstance(att_pdu, att.PrepareWriteReq):
            stats["requests"] += 1
            stats["writes"] += 1
            pending_requests[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] Prepare Write Request: "
                f"handle={format_handle(att_pdu.attribute_handle)} "
                f"offset={att_pdu.value_offset} {len(att_pdu.payload)}B")

        elif isinstance(att_pdu, att.PrepareWriteRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            log(ts,
                f"{direction_str} [{chan}] Prepare Write Response: "
                f"handle={format_handle(att_pdu.attribute_handle)} "
                f"offset={att_pdu.value_offset}" + latency_str)

        elif isinstance(att_pdu, att.ExecuteWriteReq):
            stats["requests"] += 1
            pending_requests[cid] = acl_pkt
            flags_str = "Write" if int(att_pdu.flags) == 1 else "Cancel"
            log(ts,
                f"{direction_str} [{chan}] Execute Write Request: flags={flags_str}")

        elif isinstance(att_pdu, att.ExecuteWriteRsp):
            stats["responses"] += 1
            latency_str = take_request(cid, ts_us)
            log(ts, f"{direction_str} [{chan}] Execute Write Response" + latency_str)

        elif isinstance(att_pdu, att.HandleValueNtf):
            stats["notifications"] += 1
            log(ts,
                f"{direction_str} [{chan}] "
                + Fore.CYAN + "Notification" + Style.RESET_ALL
                + f" handle={format_handle(att_pdu.attribute_handle)} "
                f"{len(att_pdu.payload)}B")
            log_detail(f"Value: {format_value_preview(att_pdu.payload)}")

        elif isinstance(att_pdu, att.HandleValueInd):
            stats["indications"] += 1
            pending_indications[cid] = acl_pkt
            log(ts,
                f"{direction_str} [{chan}] "
                + Fore.YELLOW + "Indication" + Style.RESET_ALL
                + f" handle={format_handle(att_pdu.attribute_handle)} "
                f"{len(att_pdu.payload)}B")
            log_detail(f"Value: {format_value_preview(att_pdu.payload)}")

        elif isinstance(att_pdu, att.HandleValueCfm):
            stats["confirmations"] += 1
            latency_str = ""
            if cid in pending_indications:
                ind_acl = pending_indications.pop(cid)
                latency_us = ts_us - ind_acl.packet.timestamp_us
                latency_str = f" [latency: {format_latency(latency_us)}]"
            log(ts,
                f"{direction_str} [{chan}] "
                + Fore.GREEN + "Confirmation" + Style.RESET_ALL + latency_str)

        elif isinstance(att_pdu, att.MultipleHandleValueNtf):
            stats["notifications"] += 1
            data = att_pdu.payload
            n_handles = 0
            off = 0
            while off + 4 <= len(data):
                vlen = struct.unpack_from("<H", data, off + 2)[0]
                n_handles += 1
                off += 4 + vlen
            log(ts,
                f"{direction_str} [{chan}] "
                + Fore.CYAN + "Multiple Handle Value Notification" + Style.RESET_ALL
                + f" ({n_handles} values, {len(data)}B)")

        else:
            log(ts,
                f"{direction_str} [{chan}] ATT {format_opcode(att_pdu.opcode)} "
                f"({len(acl_pkt.payload)}B)")

    print(f"\n" + Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
    print(Fore.MAGENTA + Style.BRIGHT + "Summary:" + Style.RESET_ALL)
    print(f"  Total ATT PDUs:      {stats['total_pdus']}")
    print(f"  Requests:            {stats['requests']}")
    print(f"  Responses:           {stats['responses']}")
    print(f"  Commands:            {stats['commands']}")
    print(f"  Notifications:       {stats['notifications']}")
    print(f"  Indications:         {stats['indications']}")
    print(f"  Confirmations:       {stats['confirmations']}")
    if stats["errors"]:
        print(f"  Errors:              {Fore.RED}{stats['errors']}{Style.RESET_ALL}")
    if effective_mtu is not None:
        print(f"  Negotiated MTU:      {Fore.GREEN}{effective_mtu}{Style.RESET_ALL}")
    if stats["eatt_bearers"]:
        print(f"  EATT bearers:        {Fore.GREEN}{stats['eatt_bearers']}{Style.RESET_ALL}")
    if stats["service_discoveries"]:
        print(f"  Discovery requests:  {stats['service_discoveries']}")
    if stats["reads"]:
        print(f"  Read operations:     {stats['reads']}")
    if stats["writes"]:
        print(f"  Write operations:    {stats['writes']}")
    print(Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
