import btsnoop
from packets import ssp
from colorama import Fore, Style


# HCI event codes for SSP-related events
HCI_LINK_KEY_REQUEST = 0x17
HCI_LINK_KEY_NOTIFICATION = 0x18
HCI_IO_CAPABILITY_REQUEST = 0x31
HCI_IO_CAPABILITY_RESPONSE = 0x32
HCI_USER_CONFIRMATION_REQUEST = 0x33
HCI_USER_PASSKEY_REQUEST = 0x34
HCI_REMOTE_OOB_DATA_REQUEST = 0x35
HCI_SIMPLE_PAIRING_COMPLETE = 0x36
HCI_USER_PASSKEY_NOTIFICATION = 0x3B
HCI_KEYPRESS_NOTIFICATION = 0x3C


def log(ts, msg):
    print(Fore.CYAN + f"  {ts}" + Style.RESET_ALL + f" | {msg}")


def log_detail(msg):
    print(Fore.WHITE + f"           | " + Style.DIM + f"  {msg}" + Style.RESET_ALL)


def format_bd_addr(bd_addr: bytes) -> str:
    # HCI BD_ADDR is little-endian on the wire; print in canonical big-endian form
    return ":".join(f"{b:02x}" for b in reversed(bytes(bd_addr)))


def log_event(ts, name: str, bd_addr: bytes, extra: str = ""):
    suffix = f" {extra}" if extra else ""
    log(ts, f"{name} [{format_bd_addr(bd_addr)}]{suffix}")


def plot_btsnoop(snoop: btsnoop.Btsnoop, **kwargs):
    if snoop is None:
        return

    events_seen = 0
    pairings_started = 0
    pairings_completed = 0
    pairings_failed = 0

    header_printed = False

    def print_header():
        nonlocal header_printed
        if header_printed:
            return
        print(f"\n" + Fore.MAGENTA + "=" * 80 + Style.RESET_ALL)
        print(Fore.MAGENTA + "SSP (Secure Simple Pairing) Analysis" + Style.RESET_ALL)
        print(Fore.MAGENTA + "=" * 80 + Style.RESET_ALL + "\n")
        header_printed = True

    for packet in snoop.packets:
        if packet.idc != btsnoop.Idc.EVENT or not packet.payload:
            continue

        event_code = packet.payload[0]
        # HCI event header: [event_code:1][param_length:1][parameters...]
        params = packet.payload[2:]

        ts = packet.timestamp
        try:
            if event_code == HCI_IO_CAPABILITY_REQUEST:
                print_header()
                events_seen += 1
                e = ssp.IoCapabilityRequestEvent.parse_all(params)
                log_event(ts, "IO Capability Request", e.bd_addr)

            elif event_code == HCI_IO_CAPABILITY_RESPONSE:
                print_header()
                events_seen += 1
                pairings_started += 1
                e = ssp.IoCapabilityResponseEvent.parse_all(params)
                log_event(ts, "IO Capability Response", e.bd_addr)
                log_detail(f"IO Capability: {e.io_capability.name}")
                log_detail(f"OOB Data: {e.oob_data_present.name}")
                log_detail(f"Auth Requirements: {e.authentication_requirements.name}")

            elif event_code == HCI_USER_CONFIRMATION_REQUEST:
                print_header()
                events_seen += 1
                e = ssp.UserConfirmationRequestEvent.parse_all(params)
                log_event(ts, "User Confirmation Request", e.bd_addr,
                          f"value={e.numeric_value:06d}")

            elif event_code == HCI_USER_PASSKEY_REQUEST:
                print_header()
                events_seen += 1
                e = ssp.UserPasskeyRequestEvent.parse_all(params)
                log_event(ts, "User Passkey Request", e.bd_addr)

            elif event_code == HCI_REMOTE_OOB_DATA_REQUEST:
                print_header()
                events_seen += 1
                e = ssp.RemoteOobDataRequestEvent.parse_all(params)
                log_event(ts, "Remote OOB Data Request", e.bd_addr)

            elif event_code == HCI_USER_PASSKEY_NOTIFICATION:
                print_header()
                events_seen += 1
                e = ssp.UserPasskeyNotificationEvent.parse_all(params)
                log_event(ts, "User Passkey Notification", e.bd_addr,
                          f"passkey={e.passkey:06d}")

            elif event_code == HCI_KEYPRESS_NOTIFICATION:
                print_header()
                events_seen += 1
                e = ssp.KeypressNotificationEvent.parse_all(params)
                log_event(ts, "Keypress Notification", e.bd_addr,
                          f"type={e.notification_type.name}")

            elif event_code == HCI_SIMPLE_PAIRING_COMPLETE:
                print_header()
                events_seen += 1
                e = ssp.SimplePairingCompleteEvent.parse_all(params)
                ok = e.status == ssp.SspStatus.SUCCESS
                if ok:
                    pairings_completed += 1
                    color = Fore.GREEN
                else:
                    pairings_failed += 1
                    color = Fore.RED
                log_event(ts,
                          color + "Simple Pairing Complete" + Style.RESET_ALL,
                          e.bd_addr,
                          f"status={e.status.name}")

            elif event_code == HCI_LINK_KEY_REQUEST:
                print_header()
                events_seen += 1
                e = ssp.LinkKeyRequestEvent.parse_all(params)
                log_event(ts, "Link Key Request", e.bd_addr)

            elif event_code == HCI_LINK_KEY_NOTIFICATION:
                print_header()
                events_seen += 1
                e = ssp.LinkKeyNotificationEvent.parse_all(params)
                log_event(ts, "Link Key Notification", e.bd_addr,
                          f"key_type={e.key_type.name}")
                log_detail(f"Link Key: {bytes(e.link_key).hex()}")
        except Exception as exn:
            log(ts, Fore.YELLOW + f"SSP event 0x{event_code:02x} parse error: {exn}"
                + Style.RESET_ALL)

    if not header_printed:
        return

    print(f"\n" + Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
    print(Fore.MAGENTA + Style.BRIGHT + "Summary:" + Style.RESET_ALL)
    print(f"  SSP events:          {events_seen}")
    print(f"  Pairings started:    {pairings_started}")
    print(f"  Pairings completed:  {pairings_completed}")
    if pairings_failed:
        print(f"  Pairings failed:     {Fore.RED}{pairings_failed}{Style.RESET_ALL}")
    print(Fore.MAGENTA + "-" * 40 + Style.RESET_ALL)
