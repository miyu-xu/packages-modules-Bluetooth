from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.hid_grpc import HID
from pandora.host_grpc import Host


class HIDProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.hid = HID(channel)
        self.host = Host(channel)
        self.connection = None

    @assert_description
    def TSC_MMI_iut_enable_connection(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then using the Implementation Under Test (IUT) connect to the
        PTS.
        """

        self.connection = self.host.Connect(address=pts_addr).connection

        return "OK"

    @assert_description
    def TSC_MMI_iut_release_connection(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then release the HID connection from the Implementation Under
        Test (IUT) by closing the Interrupt Channel followed by the Control
        Channel.
    
        Description:  This can be done using the anticipated L2CAP
        Disconnection Requests.  If the host is unable to perform the connection
        request, the IUT may break the ACL or Baseband Link by going out of
        range.
        """

        if self.connection is None:
            self.connection = self.host.GetConnection(address=pts_addr).connection
        self.host.Disconnect(connection=self.connection)

        return "OK"

    @assert_description
    def TSC_MMI_iut_disable_connection(self, pts_addr: bytes, **kwargs):
        """
        Disable the connection using the Implementation UnderTest (IUT).
    
        Note:
        The IUT may either disconnect the Interupt Control Channels or send a
        host initiated virtual cable unplug and wait for the PTS to disconnect
        the channels.
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

        return "OK"

    @assert_description
    def TSC_MMI_tester_release_connection(self, **kwargs):
        """
        Place the Implementation Under Test (IUT) in a state which will allow
        the PTS to perform an HID connection release, then click Ok.
    
        Note:  The
        PTS will send an L2CAP disconnect request for the Interrupt channel,
        then the control channel.
        """

        return "OK"

    @assert_description
    def TSC_MMI_host_iut_prepare_to_receive_pointing_data(self, **kwargs):
        """
        Place the Implementation Under Test (IUT) in a state to receive and
        verify HID pointing data, then click Ok.
        """

        return "OK"

    @assert_description
    def TSC_MMI_host_iut_verify_pointing_data(self, **kwargs):
        """
        Verify that the pointer on the Implementation Under Test (IUT) moved to
        the left (X< 0), then click Ok.
        """

        # TODO: implement!

        return "OK"

    @assert_description
    def TSC_MMI_host_send_output_report(self, **kwargs):
        """
        Send an output report from the HOST.
        """

        return "OK"
