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

import asyncio
import threading


class HFPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.hfp = HFP(channel)
        self.host = Host(channel)

    @assert_description
    def TSC_delete_pairing_iut(self, pts_addr: bytes, **kwargs):
        """
        Delete the pairing with the PTS using the Implementation Under Test
        (IUT), then click Ok.
        """

        self.hfp.DeletePairing(address=pts_addr)
        return "OK"

    def threadFunc(self, pts_addr: bytes):
        self.host.WaitConnection(address=pts_addr)

    @assert_description
    def TSC_iut_connectable(self, test: str, pts_addr: bytes, **kwargs):
        """
        Make the Implementation Under Test (IUT) connectable, then click Ok.
        """

        if "HFP/AG/SDP/BV-01-I" in test:
            # TODO@PTS: There is no MMI WaitConnection for this test
            th = threading.Thread(target=self.threadFunc, args=(pts_addr,))
            th.start()

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
    def TSC_ag_iut_enable_call(self, **kwargs):
        """
        Click Ok, then place a call from an external line to the Implementation
        Under Test (IUT). Do not answer the call unless prompted to do so.
        """

        return "OK"
