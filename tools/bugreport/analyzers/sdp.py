import btsnoop
import dataclasses
import struct
from packets import l2cap
from packets import sdp
from typing import Dict, List, Optional
from btsnoop import Direction
from colorama import Fore, Style


SDP_PSM = 0x0001


L2CAP_RESULT_CODES = {
    0x0000: "Success",
    0x0001: "Pending",
    0x0002: "Refused - PSM not supported",
    0x0003: "Refused - security block",
    0x0004: "Refused - no resources",
    0x0006: "Refused - invalid source CID",
    0x0007: "Refused - source CID already allocated",
}

L2CAP_PSM_NAMES = {
    0x0001: "SDP",
    0x0003: "RFCOMM",
    0x0005: "TCS-BIN",
    0x0007: "TCS-BIN-CORDLESS",
    0x000F: "BNEP",
    0x0011: "HID Control",
    0x0013: "HID Interrupt",
    0x0015: "UPnP",
    0x0017: "AVCTP",
    0x0019: "AVDTP",
    0x001B: "AVCTP Browsing",
    0x001D: "UDI",
    0x001F: "ATT",
    0x0021: "3DSP",
    0x0023: "IPSP",
    0x0025: "OTS",
    0x1001: "Apple Notification Center",
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


def format_pdu_name(pdu_id) -> str:
    if isinstance(pdu_id, sdp.PduId):
        return pdu_id.name
    return f"Unknown(0x{pdu_id:02X})"


def format_uuid(elem: sdp.DataElement) -> str:
    if elem.type != sdp.DataElementType.UUID:
        return repr(elem)
    if isinstance(elem.value, int):
        name = sdp.UUID16_NAMES.get(elem.value)
        if name:
            return f"0x{elem.value:04X} ({name})"
        return f"0x{elem.value:04X}"
    elif isinstance(elem.value, bytes):
        if len(elem.value) == 4:
            return f"0x{int.from_bytes(elem.value, 'big'):08X}"
        return elem.value.hex()
    return repr(elem)


def format_attribute_name(attr_id: int) -> str:
    name = sdp.ATTRIBUTE_NAMES.get(attr_id)
    if name:
        return f"0x{attr_id:04X} ({name})"
    return f"0x{attr_id:04X}"


def format_data_element(elem: sdp.DataElement, indent: int = 0, _depth: int = 0) -> List[str]:
    if _depth > sdp.MAX_DATA_ELEMENT_DEPTH:
        return [f"{'  ' * indent}(max depth exceeded)"]

    prefix = "  " * indent
    lines = []

    if elem.type == sdp.DataElementType.UUID:
        lines.append(f"{prefix}{format_uuid(elem)}")
    elif elem.type in (sdp.DataElementType.DATA_ELEMENT_SEQUENCE,
                       sdp.DataElementType.DATA_ELEMENT_ALTERNATIVE):
        kind = ("Sequence" if elem.type == sdp.DataElementType.DATA_ELEMENT_SEQUENCE
                else "Alternative")
        lines.append(f"{prefix}{kind}:")
        for child in elem.value:
            lines.extend(format_data_element(child, indent + 1, _depth + 1))
    elif elem.type == sdp.DataElementType.TEXT_STRING:
        try:
            text = elem.value.decode("utf-8", errors="replace")
            lines.append(f'{prefix}"{text}"')
        except Exception:
            lines.append(f"{prefix}0x{elem.value.hex()}")
    elif elem.type == sdp.DataElementType.UNSIGNED_INT:
        lines.append(f"{prefix}{elem.value} (0x{elem.value:X})")
    elif elem.type == sdp.DataElementType.SIGNED_INT:
        lines.append(f"{prefix}{elem.value}")
    elif elem.type == sdp.DataElementType.BOOLEAN:
        lines.append(f"{prefix}{elem.value}")
    elif elem.type == sdp.DataElementType.URL:
        try:
            lines.append(f"{prefix}{elem.value.decode('utf-8', errors='replace')}")
        except Exception:
            lines.append(f"{prefix}0x{elem.value.hex()}")
    elif elem.type == sdp.DataElementType.NIL:
        lines.append(f"{prefix}NIL")
    else:
        lines.append(f"{prefix}{repr(elem)}")

    return lines


def log_service_search_pattern(pattern: List[sdp.DataElement]) -> None:
    uuids = [format_uuid(e) for e in pattern if e.type == sdp.DataElementType.UUID]
    if uuids:
        log_detail(f"Search UUIDs: {', '.join(uuids)}")


def format_attribute_id_list(attrs: List[sdp.DataElement]) -> str:
    parts = []
    for elem in attrs:
        if elem.type == sdp.DataElementType.UNSIGNED_INT:
            if elem.size == 4:
                start = (elem.value >> 16) & 0xFFFF
                end = elem.value & 0xFFFF
                start_name = sdp.ATTRIBUTE_NAMES.get(start, f"0x{start:04X}")
                end_name = sdp.ATTRIBUTE_NAMES.get(end, f"0x{end:04X}")
                if start == 0x0000 and end == 0xFFFF:
                    parts.append("0x0000..0xFFFF (all)")
                else:
                    parts.append(f"{start_name}..{end_name}")
            else:
                name = sdp.ATTRIBUTE_NAMES.get(elem.value)
                parts.append(
                    f"0x{elem.value:04X} ({name})" if name else f"0x{elem.value:04X}"
                )
    return ", ".join(parts) if parts else "(all)"


def log_attribute_list(attrs: List[sdp.DataElement], prefix: str = "") -> None:
    i = 0
    while i + 1 < len(attrs):
        attr_id_elem = attrs[i]
        attr_val_elem = attrs[i + 1]
        i += 2

        if attr_id_elem.type == sdp.DataElementType.UNSIGNED_INT:
            attr_name = format_attribute_name(attr_id_elem.value)
        else:
            attr_name = repr(attr_id_elem)

        val_lines = format_data_element(attr_val_elem)
        if len(val_lines) == 1:
            log_detail(f"{prefix}{attr_name} = {val_lines[0].strip()}")
        else:
            log_detail(f"{prefix}{attr_name}:")
            for line in val_lines:
                log_detail(f"{prefix}  {line}")


def _extract_service_names(attrs: List[sdp.DataElement]) -> List[str]:
    names = []
    i = 0
    while i + 1 < len(attrs):
        attr_id_elem = attrs[i]
        attr_val_elem = attrs[i + 1]
        i += 2
        if (attr_id_elem.type == sdp.DataElementType.UNSIGNED_INT
                and attr_id_elem.value == 0x0001
                and attr_val_elem.type == sdp.DataElementType.DATA_ELEMENT_SEQUENCE
                and isinstance(attr_val_elem.value, list)):
            for uuid_elem in attr_val_elem.value:
                if (uuid_elem.type == sdp.DataElementType.UUID
                        and isinstance(uuid_elem.value, int)):
                    names.append(
                        sdp.UUID16_NAMES.get(uuid_elem.value, f"0x{uuid_elem.value:04X}")
                    )
    return names


def log_service_records(elems: List[sdp.DataElement]) -> None:
    records = elems
    if (len(elems) == 1
            and elems[0].type == sdp.DataElementType.DATA_ELEMENT_SEQUENCE
            and isinstance(elems[0].value, list)):
        records = elems[0].value

    for idx, record in enumerate(records):
        if (record.type == sdp.DataElementType.DATA_ELEMENT_SEQUENCE
                and isinstance(record.value, list)):
            svc_names = _extract_service_names(record.value)
            header = f"Service Record #{idx + 1}"
            if svc_names:
                header += f": {', '.join(svc_names)}"
            log_detail(Fore.WHITE + Style.BRIGHT + header + Style.RESET_ALL)
            log_attribute_list(record.value, prefix="  ")
        else:
            for line in format_data_element(record):
                log_detail(f"  {line}")


def log_sdp_pdu(pdu: sdp.SdpPdu, direction_str: str, ts, latency_str: str = "") -> None:
    pdu_name = format_pdu_name(pdu.pdu_id)

    if isinstance(pdu, sdp.SdpErrorResponse):
        error_name = (pdu.error_code.name
                      if isinstance(pdu.error_code, sdp.ErrorCode)
                      else f"0x{pdu.error_code:04X}")
        log(ts, Fore.RED + f"✗ SDP Error: {error_name}{latency_str}" + Style.RESET_ALL)

    elif isinstance(pdu, sdp.SdpServiceSearchRequest):
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]")
        log_service_search_pattern(pdu.service_search_pattern)
        log_detail(f"Max records: {pdu.maximum_service_record_count}")
        if pdu.continuation_state and len(pdu.continuation_state) > 1:
            log_detail(f"Continuation: {pdu.continuation_state.hex()}")

    elif isinstance(pdu, sdp.SdpServiceSearchResponse):
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]{latency_str}")
        log_detail(f"Total records: {pdu.total_service_record_count}, "
                   f"Current: {pdu.current_service_record_count}")
        for handle in pdu.service_record_handle_list:
            log_detail(f"  Handle: 0x{handle:08X}")
        if pdu.continuation_state and len(pdu.continuation_state) > 1:
            log_detail(f"Continuation: {pdu.continuation_state.hex()}")

    elif isinstance(pdu, sdp.SdpServiceAttributeRequest):
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]")
        log_detail(f"Handle: 0x{pdu.service_record_handle:08X}")
        log_detail(f"Max bytes: {pdu.maximum_attribute_byte_count}")
        if pdu.attribute_id_list:
            log_detail(f"Attributes: {format_attribute_id_list(pdu.attribute_id_list)}")
        if pdu.continuation_state and len(pdu.continuation_state) > 1:
            log_detail(f"Continuation: {pdu.continuation_state.hex()}")

    elif isinstance(pdu, sdp.SdpServiceAttributeResponse):
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]{latency_str}")
        log_detail(f"Attribute bytes: {pdu.attribute_list_byte_count}")
        log_attribute_list(pdu.attribute_list)
        if pdu.continuation_state and len(pdu.continuation_state) > 1:
            log_detail(f"Continuation: {pdu.continuation_state.hex()}")

    elif isinstance(pdu, sdp.SdpServiceSearchAttributeRequest):
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]")
        log_service_search_pattern(pdu.service_search_pattern)
        log_detail(f"Max bytes: {pdu.maximum_attribute_byte_count}")
        if pdu.attribute_id_list:
            log_detail(f"Attributes: {format_attribute_id_list(pdu.attribute_id_list)}")
        if pdu.continuation_state and len(pdu.continuation_state) > 1:
            log_detail(f"Continuation: {pdu.continuation_state.hex()}")

    elif isinstance(pdu, sdp.SdpServiceSearchAttributeResponse):
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]{latency_str}")
        log_detail(f"Attribute list bytes: {pdu.attribute_lists_byte_count}")
        log_service_records(pdu.attribute_lists)
        if pdu.continuation_state and len(pdu.continuation_state) > 1:
            log_detail(f"Continuation: {pdu.continuation_state.hex()}")

    else:
        log(ts, f"{direction_str} SDP {pdu_name} [tid={pdu.transaction_id}]{latency_str}")
        if pdu.payload:
            log_detail(f"Payload: {pdu.payload.hex()}")


def extract_discovered_services(pdu: sdp.SdpPdu) -> List[str]:
    services = []
    if isinstance(pdu, sdp.SdpServiceSearchAttributeResponse):
        for elem in pdu.attribute_lists:
            if (elem.type == sdp.DataElementType.DATA_ELEMENT_SEQUENCE
                    and isinstance(elem.value, list)):
                services.extend(_extract_service_names(elem.value))
    elif isinstance(pdu, sdp.SdpServiceAttributeResponse):
        services.extend(_extract_service_names(pdu.attribute_list))
    return services


def plot_acl_connection(acl_connection: btsnoop.AclConnection,
                        signal_lcid: Optional[int] = None,
                        signal_rcid: Optional[int] = None,
                        **kwargs):
    acl_packets = [AclPacket.parse(p) for p in acl_connection.packets]
    if not acl_packets:
        return

    sdp_local_cid: Optional[int] = signal_lcid
    sdp_remote_cid: Optional[int] = signal_rcid
    pending_connection = None
    pending_l2cap_requests: Dict[int, AclPacket] = {}
    pending_sdp_requests: Dict[int, AclPacket] = {}

    sdp_request_count = 0
    sdp_response_count = 0
    sdp_error_count = 0
    discovered_services: List[str] = []

    started_ts = (acl_connection.connected.timestamp
                  if acl_connection.connected else acl_packets[0].packet.timestamp)

    header_printed = False

    def print_header():
        nonlocal header_printed
        if header_printed:
            return
        print(f"\n" + Fore.MAGENTA + "=" * 80 + Style.RESET_ALL)
        print(Fore.MAGENTA
              + f"SDP Session Analysis - Connection Handle "
                f"0x{acl_connection.connection_handle:04x}"
              + Style.RESET_ALL)
        print(Fore.MAGENTA + f"Started: {started_ts}" + Style.RESET_ALL)
        print(Fore.MAGENTA + "=" * 80 + Style.RESET_ALL + "\n")
        header_printed = True

    if sdp_local_cid and sdp_remote_cid:
        print_header()
        log(started_ts, Fore.GREEN
            + f"Using provided CIDs (Local: 0x{sdp_local_cid:04x}, "
              f"Remote: 0x{sdp_remote_cid:04x})"
            + Style.RESET_ALL)

    for acl_packet in acl_packets:
        if acl_packet.channel_id == 0x0001:
            try:
                signal = l2cap.SignalingPacket.parse_all(acl_packet.payload)
            except Exception:
                continue

            identifier = signal.identifier
            direction_str = format_direction(acl_packet.direction)

            if isinstance(signal, l2cap.ConnectionRequest):
                if signal.psm != SDP_PSM:
                    continue
                print_header()
                psm_name = L2CAP_PSM_NAMES.get(signal.psm, f"Unknown(0x{signal.psm:04x})")
                log(acl_packet.packet.timestamp,
                    f"{direction_str} L2CAP Connection Request [id={identifier}]")
                log_detail(f"PSM: 0x{signal.psm:04x} ({psm_name})")
                log_detail(f"Source CID: 0x{signal.source_cid:04x}")
                pending_connection = (acl_packet, signal)
                pending_l2cap_requests[identifier] = acl_packet

            elif isinstance(signal, l2cap.ConnectionResponse):
                if not pending_connection or identifier != pending_connection[1].identifier:
                    continue
                result_str = L2CAP_RESULT_CODES.get(
                    signal.result, f"Unknown(0x{signal.result:04x})"
                )
                latency_str = ""
                if identifier in pending_l2cap_requests:
                    req_packet = pending_l2cap_requests[identifier]
                    latency_us = (acl_packet.packet.timestamp_us
                                  - req_packet.packet.timestamp_us)
                    latency_str = f" [latency: {format_latency(latency_us)}]"
                    del pending_l2cap_requests[identifier]

                log(acl_packet.packet.timestamp,
                    f"{direction_str} L2CAP Connection Response "
                    f"[id={identifier}]{latency_str}")
                log_detail(f"Result: {result_str}")

                destination_cid = signal.destination_cid
                source_cid = signal.source_cid
                if acl_packet.direction == Direction.SENT:
                    destination_cid, source_cid = source_cid, destination_cid

                if signal.result == 0:
                    log(acl_packet.packet.timestamp,
                        Fore.GREEN + f"SDP channel connected "
                        f"(Local: 0x{source_cid:04x}, Remote: 0x{destination_cid:04x})"
                        + Style.RESET_ALL)
                    sdp_local_cid = source_cid
                    sdp_remote_cid = destination_cid
                    pending_connection = None
                elif signal.result == 1:
                    log_detail("Connection pending...")
                else:
                    log(acl_packet.packet.timestamp,
                        Fore.RED + f"✗ Connection failed: {result_str}" + Style.RESET_ALL)
                    pending_connection = None

            elif isinstance(signal, l2cap.DisconnectionResponse):
                destination_cid = signal.destination_cid
                if acl_packet.direction == Direction.SENT:
                    destination_cid = signal.source_cid
                if sdp_local_cid and destination_cid in (sdp_local_cid, sdp_remote_cid):
                    log(acl_packet.packet.timestamp,
                        Fore.RED + "✗ SDP channel disconnected" + Style.RESET_ALL)
                    sdp_local_cid = None
                    sdp_remote_cid = None

        elif sdp_local_cid and acl_packet.channel_id in (sdp_local_cid, sdp_remote_cid):
            direction_str = format_direction(acl_packet.direction)

            try:
                pdu = sdp.SdpPdu.parse(acl_packet.payload)
            except Exception as e:
                log(acl_packet.packet.timestamp,
                    f"{direction_str} SDP [parse error: {e}]")
                continue

            is_request = isinstance(pdu.pdu_id, sdp.PduId) and pdu.pdu_id in (
                sdp.PduId.SERVICE_SEARCH_REQUEST,
                sdp.PduId.SERVICE_ATTRIBUTE_REQUEST,
                sdp.PduId.SERVICE_SEARCH_ATTRIBUTE_REQUEST,
            )
            is_response = isinstance(pdu.pdu_id, sdp.PduId) and pdu.pdu_id in (
                sdp.PduId.SERVICE_SEARCH_RESPONSE,
                sdp.PduId.SERVICE_ATTRIBUTE_RESPONSE,
                sdp.PduId.SERVICE_SEARCH_ATTRIBUTE_RESPONSE,
                sdp.PduId.ERROR_RESPONSE,
            )

            latency_str = ""
            if is_response and pdu.transaction_id in pending_sdp_requests:
                req_packet = pending_sdp_requests[pdu.transaction_id]
                latency_us = (acl_packet.packet.timestamp_us
                              - req_packet.packet.timestamp_us)
                latency_str = f" [latency: {format_latency(latency_us)}]"
                del pending_sdp_requests[pdu.transaction_id]

            if is_request:
                pending_sdp_requests[pdu.transaction_id] = acl_packet
                sdp_request_count += 1

            if is_response:
                sdp_response_count += 1
                svc = extract_discovered_services(pdu)
                if svc:
                    for s in svc:
                        if s not in discovered_services:
                            discovered_services.append(s)

            if isinstance(pdu, sdp.SdpErrorResponse):
                sdp_error_count += 1

            log_sdp_pdu(pdu, direction_str, acl_packet.packet.timestamp, latency_str)

    if not header_printed:
        return

    print(f"\n" + Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
    print(Fore.MAGENTA + Style.BRIGHT + "Summary:" + Style.RESET_ALL)
    print(f"  SDP requests:        {sdp_request_count}")
    print(f"  SDP responses:       {sdp_response_count}")
    if sdp_error_count:
        print(f"  SDP errors:          {sdp_error_count}")
    if discovered_services:
        print(f"  Discovered services: {', '.join(discovered_services)}")
    else:
        print(f"  Discovered services: (none)")
    print(Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
