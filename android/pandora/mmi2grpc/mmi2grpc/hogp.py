from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.hid_grpc import HID
from pandora.host_grpc import Host
from pandora.sm_grpc import SM


class HOGPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.hid = HID(channel)
        self.host = Host(channel)
        self.sm = SM(channel)
        self.connection = None
        self.pairing_stream = None

    @assert_description
    def IUT_INITIATE_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.
    
        Description: Verify that
        the Implementation Under Test (IUT) can initiate a GATT connect request
        to the PTS.
        """

        self.connection = self.host.ConnectLE(address=pts_addr).connection
        self.pairing_stream = self.sm.OnPairing()
        self.sm.Pair(connection=self.connection)

        return "OK"

    def _mmi_2004(self, pts_addr: bytes, description: str, **kwargs):
        """
        Please confirm that 6 digit number is matched with XXXXXX.
        """
        passkey = int(description[-7:-1])
        for event in self.pairing_stream:
            if event.address == pts_addr and event.numeric_comparison == passkey:
                self.pairing_stream.send(
                    event=event,
                    confirm=True,
                )
                self.pairing_stream.close()
                break
        return "OK"