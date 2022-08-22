from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.hid_grpc import HID
from pandora.host_grpc import Host


class HOGPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.hid = HID(channel)
        self.host = Host(channel)
        self.connection = None

    @assert_description
    def IUT_INITIATE_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.
    
        Description: Verify that
        the Implementation Under Test (IUT) can initiate a GATT connect request
        to the PTS.
        """

        self.connection = self.host.ConnectLE(address=pts_addr).connection

        return "OK"