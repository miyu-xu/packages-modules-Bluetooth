# Copyright 2024 Google LLC
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
"""BAP proxy module."""

import threading

from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy
from mmi2grpc._rootcanal import Dongle
from pandora.host_grpc import Host
from pandora.host_pb2 import RANDOM
from pandora.security_grpc import Security
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer
from pandora_experimental.gatt_grpc import GATT


class BAPProxy(ProfileProxy):
    def __init__(self, channel, rootcanal):
        super().__init__(channel)
        self.host = Host(channel)
        self.gatt = GATT(channel)
        self.security = Security(channel)
        self.rootcanal = rootcanal

        self.pairing_stream = self.security.OnPairing()
        self.connection = None

    def _secure_in_background(self):
        def secure_thread():
            self.security.Secure(connection=self.connection, le=LE_LEVEL3)

        threading.Thread(target=secure_thread).start()

    def test_started(self, test: str, **kwargs):
        self.rootcanal.select_pts_dongle(Dongle.LAIRD_BL654)
        return "OK"

    @assert_description
    def _mmi_20100(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.

        Description: Verify that
        the Implementation Under Test (IUT) can initiate a GATT connect request
        to the PTS.
        """
        self.connection = self.host.ConnectLE(own_address_type=RANDOM, public=pts_addr).connection
        self._secure_in_background()

        return "OK"

    @match_description
    def _mmi_2004(self, pts_addr: bytes, passkey: str, **kwargs):
        r"""
        Please confirm that 6 digit number is matched with (?P<passkey>[0-9]*).
        """
        received = []
        for event in self.pairing_stream:
            if event.address == pts_addr and event.numeric_comparison == int(passkey):
                self.pairing_stream.send(PairingEventAnswer(
                    event=event,
                    confirm=True,
                ))
                return "OK"
            received.append(event.numeric_comparison)

        assert False, f"mismatched passcode: expected {passkey}, received {received}"

    @match_description
    def _mmi_20107(self, characteristic_name: str, handle: str, **kwargs):
        r"""
        Please send Read Request to read (?P<characteristic_name>.*) characteristic with handle =
        (?P<handle>\S*).
        """
        handle = int(handle, base=16)

        self.gatt.ReadCharacteristicFromHandle(
            connection=self.connection,
            handle=handle,
        )

        return "OK"

    @assert_description
    def _mmi_20115(self, **kwargs):
        """
        Please initiate a GATT disconnection to the PTS.

        Description: Verify
        that the Implementation Under Test (IUT) can initiate GATT disconnect
        request to PTS.
        """

        assert self.connection is not None
        self.host.Disconnect(connection=self.connection)
        self.connection = None

        return "OK"
