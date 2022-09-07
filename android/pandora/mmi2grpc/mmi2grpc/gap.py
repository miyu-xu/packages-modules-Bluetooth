"""TODO(yuyangh): DO NOT SUBMIT without one-line documentation for gap.

TODO(yuyangh): DO NOT SUBMIT without a detailed description of gap.
"""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy
from pandora.gap_grpc import GAP


class GAPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.gap = GAP(channel)

    @assert_description
    def TSC_MMI_make_iut_general_discoverable(self, **kwargs):
        """
        Please make IUT general discoverable. Press OK to continue.
        """
        
        self.gap.MakeDiscoverable()
        
        return "OK"

