from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from time import sleep

from pandora.hid11_grpc import HID11
from pandora.host_grpc import Host


class HID11Proxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.hid = HID11(channel)
        self.host = Host(channel)
        self.connection = None

    def TSC_MMI_verify_num_lock_LED_disabled(self, pts_addr: bytes, **kwargs):
        """
        Initiate an inquiry using the GIAC on the Implementation Under Test
        (IUT) to discover the tester/PTS. If PTS is discoverable, click OK.
        """

        devices = self.host.RunInquiry(timeout_seconds=1, address=pts_addr)

        # TODO: make this assert pass
        # assert any(device.address == pts_addr for device in devices.device), devices.device

        return "OK"

    @assert_description
    def TSC_HID_MMI_iut_establish_control_channel(self, pts_addr: bytes, **kwargs):
        """
        Establish the control channel connection from the Implementation Under
        Test (IUT).
        """

        self.connection = self.host.Connect(address=pts_addr).connection

        return "OK"

    @assert_description
    def TSC_HID_MMI_iut_move_out_of_range(self, pts_addr: bytes, **kwargs):
        """
        Please move the IUT out of range of PTS (or shield the IUT) to generate
        disconnection.
        """

        if self.connection is None:
            self.connection = self.host.GetConnection(address=pts_addr).connection
        self.host.Disconnect(connection=self.connection)
        self.connection = None
        return "OK"

    @assert_description
    def TSC_HID_MMI_iut_accept_connection_ready_confirm(self, **kwargs):
        """
        Please prepare the IUT to accept connection from PTS and then click OK.
        """

        return "OK"

    @assert_description
    def TSC_HID_MMI_iut_accept_control_channel(self, pts_addr: bytes, **kwargs):
        """
        Accept the control channel connection from the Implementation Under Test
        (IUT).
        """
        sleep(100000)
        # self.connection = self.host.Connect(address=pts_addr).connection

        return "OK"
