from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.l2cap_grpc import L2CAP


class L2CAPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.l2cap = L2CAP(channel)

    @assert_description
    def MMI_IUT_INITIATE_ACL_CONNECTION(self, **kwargs):
        """
        Using the Implementation Under Test(IUT), initiate ACL Create Connection
        Request to the PTS.

        Description : The Implementation Under Test(IUT)
        should create ACL connection request to PTS.
        """

        self.l2cap.InitiateAclConnection()
        return "OK"
