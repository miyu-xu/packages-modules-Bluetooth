# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""HFP proxy module."""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.hfp_grpc import HFP
from pandora.host_grpc import Host

import sys
import threading


class HFPProxy(ProfileProxy):

    def __init__(self, channel, test: str):
        super().__init__()
        self.hfp = HFP(channel)
        self.host = Host(channel)
        if test in [
                'HFP/AG/ACC/BI-12-I',
                'HFP/AG/ACC/BI-13-I',
                'HFP/AG/ACC/BI-14-I',
                'HFP/AG/ACC/BV-08-I',
                'HFP/AG/ACC/BV-09-I',
                'HFP/AG/ACC/BV-10-I',
                'HFP/AG/ACC/BV-11-I',
                'HFP/AG/ACC/BV-15-I',
                'HFP/AG/ATH/BV-04-I',
        ]:
            self.asyncWaitConnection(3)

    def asyncWaitConnection(self, delay, pts_addr=None):
        print(f'HFP NOT A MMI: WaitConnection', file=sys.stderr)
        args = ()
        if pts_addr != None:
            args = dict(address=pts_addr)

        th = threading.Timer(interval=delay, function=self.host.WaitConnection, args=args)
        th.start()

    @assert_description
    def TSC_delete_pairing_iut(self, pts_addr: bytes, **kwargs):
        """
        Delete the pairing with the PTS using the Implementation Under Test
        (IUT), then click Ok.
        """

        self.hfp.DeletePairing(address=pts_addr)
        return "OK"

    @assert_description
    def TSC_iut_connectable(self, test: str, pts_addr: bytes, **kwargs):
        """
        Make the Implementation Under Test (IUT) connectable, then click Ok.
        """

        if test in ["HFP/AG/SDP/BV-01-I"]:
            self.asyncWaitConnection(2, pts_addr)

        return "OK"

    @assert_description
    def TSC_INFO_slc_with_30_seconds_wait(self, **kwargs):
        """
        After clicking the OK button, PTS will connect to the IUT and then be
        idle for 30 seconds as part of the test procedure.

        Click OK to proceed.
        """

        return "OK"

    @assert_description
    def TSC_ag_iut_enable_call(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then place a call from an external line to the Implementation
        Under Test (IUT). Do not answer the call unless prompted to do so.
        """

        # TODO b/238708237 Not Implemented
        raise NotImplementedError(self.__class__.__name__ + '.TSC_ag_iut_enable_call See b/238708237       (╥﹏╥)')

        # # Delay thread to return the OK before doing the call
        th = threading.Timer(interval=2, function=self.hfp.startIncomingCall, kwargs=dict(address=pts_addr))
        th.start()

        return "OK"

    @assert_description
    def TSC_iut_enable_slc(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then initiate a service level connection from the
        Implementation Under Test (IUT) to the PTS.
        """

        self.hfp.EnableSlc(address=pts_addr)
        # th = threading.Timer(interval=2, function=self.host.Connect, kwargs=dict(address=pts_addr))
        # th.start()

        return "OK"

    @assert_description
    def TSC_ag_iut_call_no_slc(self, **kwargs):
        """
        Place a call from an external line to the Implementation Under Test
        (IUT).  When the call is active, click Ok.
        """

        # TODO b/238708237 Not Implemented
        raise NotImplementedError(self.__class__.__name__ +
                                  '.TSC_ag_iut_call_no_slc See b/238708237     .·´¯`(>▂<)´¯`·. ')

    @assert_description
    def TSC_iut_search(self, **kwargs):
        """
        Using the Implementation Under Test (IUT), perform a search for the PTS.
        If found, click OK.
        """

        return "OK"

    @assert_description
    def TSC_iut_connect(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then make a connection request to the PTS from the
        Implementation Under Test (IUT).
        """

        self.connection = self.host.Connect(address=pts_addr).connection

        return "OK"

    @assert_description
    def TSC_iut_disable_slc(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then disable the service level connection using the
        Implementation Under Test (IUT).
        """

        self.hfp.DisableSlc(address=pts_addr)

        return "OK"
