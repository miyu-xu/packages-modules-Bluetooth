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
"""SMP proxy module."""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.sm_grpc import SM


class SMProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.sm = SM(channel)

    @assert_description
    def MMI_IUT_ENABLE_CONNECTION_SM(self, pts_addr: bytes, **kwargs):
        """
        Initiate an connection from the IUT to the PTS.
        """
        self.sm.ConnectLE(address=pts_addr)
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_PAIRING_PROCESS(self, pts_addr: bytes, **kwargs):
        """
        Please start pairing process.
        """
        self.sm.Pair(address=pts_addr)
        self.sm.AcceptPairingRequestAction(address=pts_addr)
        return "OK"

    @assert_description
    def MMI_IUT_SEND_DISCONNECTION_REQUEST(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a disconnection to the PTS.

        Description: Verify that
        the Implementation Under Test(IUT) can initiate a disconnect request to
        PTS.
        """
        self.sm.DisconnectLE(address=pts_addr)
        return "OK"
