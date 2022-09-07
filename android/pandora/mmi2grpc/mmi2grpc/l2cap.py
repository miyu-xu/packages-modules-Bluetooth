from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy
from typing import Optional

from pandora.l2cap_grpc import L2CAP
from pandora.host_grpc import Host
from pandora.host_pb2 import Connection


class L2CAPProxy(ProfileProxy):

    connection: Optional[Connection] = None

    def __init__(self, channel):
        super().__init__()
        self.l2cap = L2CAP(channel)
        self.host = Host(channel)

    @assert_description
    def MMI_IUT_SEND_LE_CREDIT_BASED_CONNECTION_REQUEST(self, pts_addr: bytes, **kwargs):
        """
        Using the Implementation Under Test (IUT), send a LE Credit based
        connection request to PTS.
    
        Description: Verify that IUT can setup LE
        credit based channel.
        """
        if self.connection is None:
            self.connection = self.host.GetLEConnection(address=pts_addr).connection
        self.l2cap.MakeConnection(connection=self.connection)

    
        return "OK"

    @assert_description
    def MMI_TESTER_ENABLE_LE_CONNECTION(self, **kwargs):
        """
        Place the IUT into LE connectable mode.
        """
        self.l2cap.StartAdvertisement()
        return "OK"

    @assert_description
    def MMI_UPPER_TESTER_SEND_LE_DATA_PACKET_LARGE(self, **kwargs):
        """
        Upper Tester command IUT to send LE data packet(s) to the PTS.
        Description : The Implementation Under Test(IUT) should send multiple LE
        frames of LE data to PTS.
        """
        self.l2cap.SendLEDataPacket(data=b"this is a large data package: MMI_UPPER_TESTER_SEND_LE_DATA_PACKET_LARGE")
        return "OK"


    @assert_description
    def MMI_UPPER_TESTER_CONFIRM_LE_DATA(self, **kwargs):
        """
        Did the Upper Tester send the data 746869732069732061206C617267652064617
        461207061636B6167653A204D4D495F55505045525F5445535445525F53454E445F4C455
        F444154415F5041434B45545F4C41524745 to to the PTS? Click Yes if it
        matched, otherwise click No.

        Description: The Implementation Under Test
        (IUT) send data is receive correctly in the PTS.
        """

        return "OK"

    @assert_description
    def MMI_UPPER_TESTER_SEND_LE_DATA_PACKET4(self, **kwargs):
        """
        Upper Tester command IUT to send at least 4 frames of LE data packets to
        the PTS.
        """
        self.l2cap.SendLEDataPacket(data=b"this is a large data package with at least 4 frames: MMI_UPPER_TESTER_SEND_LE_DATA_PACKET_LARGE")
        return "OK"


    @assert_description
    def MMI_UPPER_TESTER_SEND_LE_DATA_PACKET_CONTINUE(self, **kwargs):
        """
        IUT continue to send LE data packet(s) to the PTS.
        """
        self.l2cap.SendLEDataPacket(data=b"this is a large data package with at least 4 frames: MMI_UPPER_TESTER_SEND_LE_DATA_PACKET_LARGE")
        return "OK"





    

