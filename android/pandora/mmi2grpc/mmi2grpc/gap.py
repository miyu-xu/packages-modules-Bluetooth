from multiprocessing import connection
from threading import Thread
from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy

from pandora.gap_grpc import GAP
from pandora.gatt_grpc import GATT
from pandora.gatt_pb2 import GattService, GattCharacteristic
from pandora.host_grpc import Host
from pandora.host_pb2 import AddressType
from pandora.security_grpc import Security


class GAPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.gap = GAP(channel)
        self.gatt = GATT(channel)
        self.host = Host(channel)
        self.security = Security(channel)

        self.connection = None
        self.pairing_events = None

        self._auto_confirm_requests()

    @assert_description
    def TSC_MMI_iut_send_hci_connect_request(self, pts_addr: bytes, **kwargs):
        """
        Please send an HCI connect request to establish a basic rate connection
        after the IUT discovers the Lower Tester over BR and LE.
        """

        self.connection = self.host.Connect(address=pts_addr).connection

        return "OK"

    @assert_description
    def _mmi_222(self, **kwargs):
        """
        Please initiate a BR/EDR security authentication and pairing with
        interaction of HCI commands.

        Press OK to continue.
        """

        # pairing already initiated with Connect() on Android
        self.pairing_events = self.security.OnPairing()

        return "OK"

    @match_description
    def _mmi_2001(self, passkey: str, **kwargs):
        """
        Please verify the passKey is correct: (?P<passkey>[0-9]+)
        """

        for event in self.pairing_events:
            if event.numeric_comparison == int(passkey):
                self.pairing_events.send(event=event, confirm=True)
                return "OK"

        assert False, "did not receive expected pairing event"

    @assert_description
    def TSC_MMI_iut_send_advertising_report_event_connectable_undirected(self, **kwargs):
        """
        Please send a connectable undirected advertising report.
        """

        self.host.StartAdvertising(
            connectable=True,
            own_address_type=AddressType.PUBLIC,
        )

        return "OK"

    @assert_description
    def TSC_MMI_iut_enter_handle_for_insufficient_authentication(self, pts_addr: bytes, **kwargs):
        """
        Please enter the handle(2 octet) to the characteristic in the IUT
        database where Insufficient Authentication error will be returned :
        """

        # we need to be unbonded for this
        self.security.DeletePairing(address=pts_addr)

        response = self.gatt.StartService(
            service=GattService(
                uuid="955798ce-3022-455c-b759-ee8edcd73d1a",
                characteristics=[
                    GattCharacteristic(
                        uuid="cf99ed9b-3c43-4343-b8a7-8afa513752ce",
                        properties=0x02,  # PROPERTY_READ,
                        permissions=0x02,  # PERMISSION_READ_ENCRYPTED
                    ),
                ],
            ))

        self.pairing_events = self.security.OnPairing()

        return hex(response.service.characteristics[0].handle)[2:].zfill(4)

    @match_description
    def TSC_MMI_the_security_id_is(self, pts_addr: bytes, passkey: str, **kwargs):
        """
        The Secure ID is (?P<passkey>[0-9]*)
        """

        for event in self.pairing_events:
            if event.address == pts_addr and event.passkey_entry_request:
                self.pairing_events.send(event=event, passkey=int(passkey))
                return "OK"

        assert False

    @assert_description
    def TSC_MMI_iut_send_le_connect_request(self, pts_addr: bytes, **kwargs):
        """
        Please send an LE connect request to establish a connection.
        """

        self.connection = self.host.ConnectLE(address=pts_addr).connection

        return "OK"

    @assert_description
    def TSC_MMI_enter_security_id(self, pts_addr: bytes, **kwargs):
        """
        Please enter Secure Id.
        """

        for event in self.pairing_events:
            if event.address == pts_addr and event.passkey_entry_notification:
                return str(event.passkey_entry_notification)

        assert False

    @match_description
    def TSC_MMI_iut_send_att_service_request(self, pts_addr: bytes, handle: str, **kwargs):
        r"""
        Please send an ATT service request - read or write request with handle
        (?P<handle>[0-9a-e]+) \(octet\).Discover services if needed.
        """

        self.gatt.ReadCharacteristicFromHandle(
            connection=self.connection,
            handle=int(handle, base=16) - 1,
        )

        return "OK"

    def _auto_confirm_requests(self, times=None):

        def task():
            cnt = 0
            pairing_events = self.security.OnPairing()
            for event in pairing_events:
                if event.just_works:
                    if times is None or cnt < times:
                        cnt += 1
                        pairing_events.send(event=event, confirm=True)

        Thread(target=task).start()
