import btsnoop
import dataclasses
import struct
from packets import smp
from typing import List, Optional
from btsnoop import Direction
from colorama import Fore, Style


# SMP uses L2CAP fixed channel CID 0x0006 for LE, 0x0007 for BR/EDR
SMP_LE_CID = 0x0006
SMP_BREDR_CID = 0x0007


# AuthReq / KeyDist bitmasks (not modelled in PDL since they are bit fields)
class AuthReqFlag:
    BONDING = 0x01
    MITM = 0x04
    SC = 0x08
    KEYPRESS = 0x10
    CT2 = 0x20


class KeyDistFlag:
    ENC_KEY = 0x01   # LTK + EDIV + Rand
    ID_KEY = 0x02    # IRK + Identity Address
    SIGN = 0x04      # CSRK
    LINK_KEY = 0x08  # BR/EDR Link Key (cross-transport)


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


def format_auth_req(auth_req: int) -> str:
    parts = []
    bonding = auth_req & 0x03
    if bonding == 0x01:
        parts.append("Bonding")
    elif bonding == 0x00:
        parts.append("No Bonding")
    if auth_req & AuthReqFlag.MITM:
        parts.append("MITM")
    if auth_req & AuthReqFlag.SC:
        parts.append("SC")
    if auth_req & AuthReqFlag.KEYPRESS:
        parts.append("Keypress")
    if auth_req & AuthReqFlag.CT2:
        parts.append("CT2")
    return ", ".join(parts) if parts else "None"


def format_key_dist(key_dist: int) -> str:
    parts = []
    if key_dist & KeyDistFlag.ENC_KEY:
        parts.append("EncKey(LTK)")
    if key_dist & KeyDistFlag.ID_KEY:
        parts.append("IdKey(IRK)")
    if key_dist & KeyDistFlag.SIGN:
        parts.append("Sign(CSRK)")
    if key_dist & KeyDistFlag.LINK_KEY:
        parts.append("LinkKey")
    return ", ".join(parts) if parts else "None"


def format_bd_addr(addr) -> str:
    return ":".join(f"{b:02X}" for b in reversed(bytes(addr)))


def format_io_capability(io_cap) -> str:
    try:
        return smp.IoCapability(io_cap).name
    except ValueError:
        return f"0x{int(io_cap):02X}"


def format_oob_flag(oob) -> str:
    return "Present" if int(oob) else "Not Present"


def format_pairing_failed_reason(reason) -> str:
    try:
        return smp.PairingFailedReason(reason).name.replace("_", " ").title()
    except ValueError:
        return f"Unknown (0x{int(reason):02X})"


def format_keypress_type(notification_type) -> str:
    try:
        return smp.KeypressNotificationType(notification_type).name.replace("_", " ").title()
    except ValueError:
        return f"Unknown (0x{int(notification_type):02X})"


# Association model tables from Core Spec Vol 3, Part H, Table 2.8
_LE_SC_MODEL_TABLE = {
    (0, 0): "Just Works",
    (0, 1): "Just Works",
    (0, 2): "Passkey Entry (responder displays, initiator enters)",
    (0, 3): "Just Works",
    (0, 4): "Passkey Entry (responder displays, initiator enters)",
    (1, 0): "Just Works",
    (1, 1): "Numeric Comparison",
    (1, 2): "Passkey Entry (responder displays, initiator enters)",
    (1, 3): "Just Works",
    (1, 4): "Numeric Comparison",
    (2, 0): "Passkey Entry (initiator displays, responder enters)",
    (2, 1): "Passkey Entry (initiator displays, responder enters)",
    (2, 2): "Passkey Entry (both enter)",
    (2, 3): "Just Works",
    (2, 4): "Passkey Entry (initiator displays, responder enters)",
    (3, 0): "Just Works",
    (3, 1): "Just Works",
    (3, 2): "Just Works",
    (3, 3): "Just Works",
    (3, 4): "Just Works",
    (4, 0): "Passkey Entry (initiator displays, responder enters)",
    (4, 1): "Numeric Comparison",
    (4, 2): "Passkey Entry (initiator displays, responder enters)",
    (4, 3): "Just Works",
    (4, 4): "Numeric Comparison",
}

_LE_LEGACY_MODEL_TABLE = {
    (0, 0): "Just Works",
    (0, 1): "Just Works",
    (0, 2): "Passkey Entry (responder displays, initiator enters)",
    (0, 3): "Just Works",
    (0, 4): "Passkey Entry (responder displays, initiator enters)",
    (1, 0): "Just Works",
    (1, 1): "Just Works",
    (1, 2): "Passkey Entry (responder displays, initiator enters)",
    (1, 3): "Just Works",
    (1, 4): "Passkey Entry (responder displays, initiator enters)",
    (2, 0): "Passkey Entry (initiator displays, responder enters)",
    (2, 1): "Passkey Entry (initiator displays, responder enters)",
    (2, 2): "Passkey Entry (both enter)",
    (2, 3): "Just Works",
    (2, 4): "Passkey Entry (initiator displays, responder enters)",
    (3, 0): "Just Works",
    (3, 1): "Just Works",
    (3, 2): "Just Works",
    (3, 3): "Just Works",
    (3, 4): "Just Works",
    (4, 0): "Passkey Entry (initiator displays, responder enters)",
    (4, 1): "Passkey Entry (initiator displays, responder enters)",
    (4, 2): "Passkey Entry (both enter)",
    (4, 3): "Just Works",
    (4, 4): "Passkey Entry (initiator displays, responder enters)",
}


def determine_association_model(initiator_io, responder_io,
                                initiator_oob, responder_oob,
                                initiator_mitm, responder_mitm, sc) -> str:
    if initiator_oob or responder_oob:
        return "OOB"
    if not initiator_mitm and not responder_mitm:
        return "Just Works"
    table = _LE_SC_MODEL_TABLE if sc else _LE_LEGACY_MODEL_TABLE
    return table.get((initiator_io, responder_io), "Unknown")


def log_pairing_req_rsp(pkt) -> None:
    log_detail(f"IO Capability: {format_io_capability(pkt.io_capability)}")
    log_detail(f"OOB Data: {format_oob_flag(pkt.oob_data_flag)}")
    log_detail(f"AuthReq: {format_auth_req(pkt.auth_req)} (0x{pkt.auth_req:02X})")
    log_detail(f"Max Encryption Key Size: {pkt.maximum_encryption_key_size}")
    log_detail(f"Initiator Key Distribution: {format_key_dist(pkt.initiator_key_distribution)}")
    log_detail(f"Responder Key Distribution: {format_key_dist(pkt.responder_key_distribution)}")


def plot_acl_connection(acl_connection: btsnoop.AclConnection, **_kwargs):
    # Cheap pre-check: L2CAP CID is at bytes [6:8] of the raw payload.
    smp_cids = {SMP_LE_CID, SMP_BREDR_CID}
    has_smp = any(
        len(p.payload) >= 8
        and struct.unpack_from("<H", p.payload, 6)[0] in smp_cids
        for p in acl_connection.packets
    )
    if not has_smp:
        return

    acl_packets = [AclPacket.parse(p) for p in acl_connection.packets]

    started_ts = (acl_connection.connected.timestamp
                  if acl_connection.connected else acl_packets[0].packet.timestamp)
    print(f"\n" + Fore.MAGENTA + "=" * 80 + Style.RESET_ALL)
    print(Fore.MAGENTA
          + f"SMP Session Analysis - Connection Handle "
            f"0x{acl_connection.connection_handle:04x}"
          + Style.RESET_ALL)
    print(Fore.MAGENTA + f"Started: {started_ts}" + Style.RESET_ALL)
    print(Fore.MAGENTA + "=" * 80 + Style.RESET_ALL + "\n")

    initiator_io: Optional[int] = None
    initiator_oob: Optional[int] = None
    initiator_auth_req: Optional[int] = None
    responder_io: Optional[int] = None
    responder_oob: Optional[int] = None
    responder_auth_req: Optional[int] = None

    packet_count = 0
    pairing_started = False
    pairing_succeeded = False
    pairing_failed = False
    is_secure_connections = False
    keys_distributed: List[str] = []
    pairing_start_us: Optional[int] = None

    for acl_packet in acl_packets:
        if acl_packet.channel_id not in (SMP_LE_CID, SMP_BREDR_CID):
            continue
        if len(acl_packet.payload) < 1:
            continue

        try:
            smp_pkt, _ = smp.parse_smp(acl_packet.payload)
        except Exception:
            continue

        packet_count += 1
        direction_str = format_direction(acl_packet.direction)
        channel_str = "LE" if acl_packet.channel_id == SMP_LE_CID else "BR/EDR"
        ts = acl_packet.packet.timestamp

        if isinstance(smp_pkt, smp.PairingRequest):
            pairing_started = True
            pairing_start_us = acl_packet.packet.timestamp_us
            initiator_io = int(smp_pkt.io_capability)
            initiator_oob = int(smp_pkt.oob_data_flag)
            initiator_auth_req = smp_pkt.auth_req
            log(ts,
                f"{direction_str} {Fore.CYAN}Pairing Request{Style.RESET_ALL} "
                f"[{channel_str}] IO={format_io_capability(smp_pkt.io_capability)} "
                f"AuthReq=[{format_auth_req(smp_pkt.auth_req)}]")
            log_pairing_req_rsp(smp_pkt)

        elif isinstance(smp_pkt, smp.PairingResponse):
            latency_str = ""
            if pairing_start_us is not None:
                latency_us = acl_packet.packet.timestamp_us - pairing_start_us
                latency_str = f" [latency: {format_latency(latency_us)}]"

            responder_io = int(smp_pkt.io_capability)
            responder_oob = int(smp_pkt.oob_data_flag)
            responder_auth_req = smp_pkt.auth_req
            is_secure_connections = (
                bool((initiator_auth_req or 0) & AuthReqFlag.SC)
                and bool(responder_auth_req & AuthReqFlag.SC)
            )

            log(ts,
                f"{direction_str} {Fore.CYAN}Pairing Response{Style.RESET_ALL} "
                f"[{channel_str}] IO={format_io_capability(smp_pkt.io_capability)} "
                f"AuthReq=[{format_auth_req(smp_pkt.auth_req)}]{latency_str}")

            if initiator_io is not None and responder_io is not None:
                model = determine_association_model(
                    initiator_io,
                    responder_io,
                    initiator_oob or 0,
                    responder_oob or 0,
                    bool((initiator_auth_req or 0) & AuthReqFlag.MITM),
                    bool(responder_auth_req & AuthReqFlag.MITM),
                    is_secure_connections,
                )
                pairing_type = "LE Secure Connections" if is_secure_connections else "LE Legacy"
                log(ts, Fore.GREEN
                    + f"  Pairing: {pairing_type}, Association Model: {model}"
                    + Style.RESET_ALL)
            log_pairing_req_rsp(smp_pkt)

        elif isinstance(smp_pkt, smp.PairingConfirm):
            log(ts, f"{direction_str} SMP Pairing Confirm")
            log_detail(f"Confirm Value: {bytes(smp_pkt.confirm_value).hex()}")

        elif isinstance(smp_pkt, smp.PairingRandom):
            log(ts, f"{direction_str} SMP Pairing Random")
            log_detail(f"Random Value: {bytes(smp_pkt.random_value).hex()}")

        elif isinstance(smp_pkt, smp.PairingFailed):
            pairing_failed = True
            reason_str = format_pairing_failed_reason(smp_pkt.reason)
            latency_str = ""
            if pairing_start_us is not None:
                latency_us = acl_packet.packet.timestamp_us - pairing_start_us
                latency_str = f" [total: {format_latency(latency_us)}]"
            log(ts, Fore.RED + f"PAIRING FAILED: {reason_str}"
                + Style.RESET_ALL + latency_str)

        elif isinstance(smp_pkt, smp.EncryptionInformation):
            keys_distributed.append("LTK")
            log(ts, f"{direction_str} SMP Encryption Information (LTK)")
            log_detail(f"LTK: {bytes(smp_pkt.long_term_key).hex()}")

        elif isinstance(smp_pkt, smp.CentralIdentification):
            keys_distributed.append("EDIV+Rand")
            log(ts, f"{direction_str} SMP Central Identification (EDIV + Rand)")
            log_detail(f"EDIV: 0x{smp_pkt.ediv:04X}")
            log_detail(f"Rand: {bytes(smp_pkt.rand).hex()}")

        elif isinstance(smp_pkt, smp.IdentityInformation):
            keys_distributed.append("IRK")
            log(ts, f"{direction_str} SMP Identity Information (IRK)")
            log_detail(f"IRK: {bytes(smp_pkt.identity_resolving_key).hex()}")

        elif isinstance(smp_pkt, smp.IdentityAddressInformation):
            keys_distributed.append("Identity Address")
            log(ts, f"{direction_str} SMP Identity Address Information")
            log_detail(f"Address Type: {smp_pkt.addr_type.name}")
            log_detail(f"Address: {format_bd_addr(smp_pkt.bd_addr)}")

        elif isinstance(smp_pkt, smp.SigningInformation):
            keys_distributed.append("CSRK")
            log(ts, f"{direction_str} SMP Signing Information (CSRK)")
            log_detail(f"CSRK: {bytes(smp_pkt.signature_key).hex()}")

        elif isinstance(smp_pkt, smp.SecurityRequest):
            log(ts, f"{direction_str} {Fore.YELLOW}Security Request{Style.RESET_ALL} "
                    f"[{channel_str}]")
            log_detail(f"AuthReq: {format_auth_req(smp_pkt.auth_req)} "
                       f"(0x{smp_pkt.auth_req:02X})")

        elif isinstance(smp_pkt, smp.PairingPublicKey):
            log(ts, f"{direction_str} SMP Pairing Public Key")
            log_detail(f"Public Key X: {bytes(smp_pkt.public_key_x).hex()}")
            log_detail(f"Public Key Y: {bytes(smp_pkt.public_key_y).hex()}")

        elif isinstance(smp_pkt, smp.PairingDhkeyCheck):
            log(ts, f"{direction_str} SMP Pairing DHKey Check")
            log_detail(f"DHKey Check: {bytes(smp_pkt.dhkey_check).hex()}")

        elif isinstance(smp_pkt, smp.PairingKeypressNotification):
            log(ts, f"{direction_str} SMP Pairing Keypress Notification")
            log_detail(f"Notification: {format_keypress_type(smp_pkt.notification_type)}")

        else:
            code_name = smp_pkt.code.name.replace("_", " ").title()
            log(ts, f"{direction_str} SMP {code_name}")
            if smp_pkt.payload:
                log_detail(f"Data: {smp_pkt.payload.hex()}")

    if pairing_started and keys_distributed and not pairing_failed:
        pairing_succeeded = True

    print(f"\n" + Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
    print(Fore.MAGENTA + Style.BRIGHT + "Summary:" + Style.RESET_ALL)
    print(f"  Total SMP packets:   {packet_count}")
    if pairing_started:
        pairing_type = "LE Secure Connections" if is_secure_connections else "LE Legacy"
        print(f"  Pairing type:        {pairing_type}")
        if pairing_succeeded:
            print(f"  Result:              {Fore.GREEN}Success{Style.RESET_ALL}")
        elif pairing_failed:
            print(f"  Result:              {Fore.RED}Failed{Style.RESET_ALL}")
        else:
            print(f"  Result:              In Progress / Incomplete")
        if keys_distributed:
            print(f"  Keys distributed:    {', '.join(keys_distributed)}")
    else:
        print(f"  Pairing:             No pairing observed")
    print(Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
