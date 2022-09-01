from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy

from pandora.gap_grpc import GAP
from pandora.host_grpc import Host, AddressType
from pandora.security_grpc import Security


class GAPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.gap = GAP(channel)
        self.host = Host(channel)
        self.security = Security(channel)

        self.connection = None
        self.pairing_events = None

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

        self.pairing_events = self.security.OnPairing()
        # self.security.Pair(connection=self.connection)

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
