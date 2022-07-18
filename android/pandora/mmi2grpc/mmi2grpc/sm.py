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
from pandora.host_grpc import Host


class SMProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.sm = SM(channel)
        self.host = Host(channel)
        self.connection = None

    @assert_description
    def MMI_IUT_ENABLE_CONNECTION_SM(self, pts_addr: bytes, **kwargs):
        """
        Initiate an connection from the IUT to the PTS.
        """
        self.connection = self.host.ConnectLE(address=pts_addr).connection
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_PAIRING_PROCESS(self, **kwargs):
        """
        Please start pairing process.
        """
        if self.connection:
            self.sm.Pair(connection=self.connection)
            self.sm.AcceptPairingConfirmationDialog(connection=self.connection)
            return "OK"

    @assert_description
    def MMI_IUT_SEND_DISCONNECTION_REQUEST(self, **kwargs):
        """
        Please initiate a disconnection to the PTS.

        Description: Verify that
        the Implementation Under Test(IUT) can initiate a disconnect request to
        PTS.
        """
        if self.connection:
            self.host.DisconnectLE(connection=self.connection)
            self.connection = None
            return "OK"
