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
"""SAP proxy module."""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora_experimental.host_grpc import Host
from pandora_experimental.host_pb2 import Connection
from pandora_experimental.sap_grpc import SAP
from pandora_experimental.security_grpc import Security

import sys
import threading
import os
import socket


class SAPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__(channel)
        self.sap = SAP(channel)
        self.security = Security(channel)
        self.host = Host(channel)
        self.pairing_events = None

    @assert_description
    def TSC_MMI_delete_pairing(self, pts_addr: bytes, **kwargs):
        """
        Delete the pairing between the Implementation Under Test (IUT) and the
        PTS.
        """
        self.security.DeletePairing(address=pts_addr)

        return "OK"

    @assert_description
    def TSC_MMI_iut_connectable(self, pts_addr: bytes, **kwargs):
        """
        Place the Implementation Under Test (IUT) in connectable mode.
        """

        self.host.SetAccessPermission(address=pts_addr, access_type="SIM")
        self.host.SetAccessPermission(address=pts_addr, access_type="Phonebook")

        return "OK"

    @assert_description
    def TSC_MMI_iut_passKey_confirm_0(self, pts_addr: bytes, **kwargs):
        """
        Confirm that Implementation Under Test (IUT)Cuttlefish x86_64 phone
        display the pass key number 000000 if it is available? Click Yes if IUT
        display 000000 , otherwise click No.

        Description: Confirm that the
        Implementation Under Test (IUT) can correctly displays the secure simple
        pairing pass key if available.
        """
        self.pairing_events = self.security.OnPairing()
        self.connection = self.host.WaitConnection(address=pts_addr).connection
        '''
        passkey = '000000'
        for event in self.pairing_events:
            assert event.numeric_comparison == int(passkey), (event, passkey)
            self.pairing_events.send(event=event, confirm=True)
            return "OK"
        assert False, "did not receive expected pairing event"
        '''

        return "OK"
