from mmi2grpc._helpers import assert_description
from mmi2grpc._rootcanal import Dongle
from mmi2grpc._proxy import ProfileProxy
# from pandora.hap_grpc import HAP


class HAPProxy(ProfileProxy):

    def __init__(self, channel, rootcanal):
        super().__init__()
        self.hap = HAP(channel)
        self.rootcanal = rootcanal

    def test_started(self, test: str, **kwargs):
        self.rootcanal.select_pts_dongle(Dongle.LAIRD_BL654)
