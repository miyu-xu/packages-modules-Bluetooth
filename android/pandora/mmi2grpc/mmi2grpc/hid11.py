from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.hid11_grpc import HID11
from pandora.host_grpc import Host


class HID11Proxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.hid = HID11(channel)
        self.host = Host(channel)

    def TSC_MMI_verify_num_lock_LED_disabled(self, pts_addr: bytes, **kwargs):
        """
        Initiate an inquiry using the GIAC on the Implementation Under Test
        (IUT) to discover the tester/PTS. If PTS is discoverable, click OK.
        """

        devices = self.host.RunInquiry(timeout_seconds=15, address=pts_addr)

        # check that the PTS was found
        assert any(device.address == pts_addr for device in devices.device), devices.device

        return "OK"
