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
import sys
import time

from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy
from mmi2grpc._streaming import StreamWrapper

from pandora_experimental.security_grpc import Security
from pandora_experimental.host_grpc import Host

# The tests needs the MMI to accept pairing confirmation request.
NEEDS_PAIRING_CONFIRMATION = {
    "SM/CEN/EKS/BV-01-C",
    "SM/CEN/JW/BI-04-C",
    "SM/CEN/JW/BI-01-C",
    "SM/CEN/KDU/BV-04-C",
    "SM/CEN/KDU/BV-05-C",
    "SM/CEN/KDU/BV-06-C",
    "SM/CEN/KDU/BV-10-C",
    "SM/CEN/KDU/BV-11-C",
}

ACCEPTS_REMOTE_PAIRING_CONFIRMATION = {
    "SM/CEN/KDU/BI-01-C",
    "SM/CEN/KDU/BI-02-C",
    "SM/CEN/KDU/BI-03-C",
}

NUM_OF_CONFIRMATIONS_REQUIRED = {
    "SM/PER/PROT/BV-02-C": 1,
    "SM/PER/JW/BV-02-C": 2,
    "SM/PER/JW/BI-02-C": 2,
    "SM/PER/JW/BI-03-C": 2,
    "SM/PER/PKE/BV-02-C": 1,
    "SM/PER/PKE/BV-05-C": 2,
    "SM/PER/PKE/BI-03-C": 1
}


def debug(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


def get_event(pairing_stream: StreamWrapper, addr: str):
    for event in pairing_stream:
        if event.address == addr:
            return event
    return None


class SMProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.security = Security(channel)
        self.host = Host(channel)
        self.connection = None
        self.pairing_stream = None

    @assert_description
    def MMI_IUT_ENABLE_CONNECTION_SM(self, test, pts_addr: bytes, **kwargs):
        """
        Initiate an connection from the IUT to the PTS.
        """
        self.connection = self.host.ConnectLE(address=pts_addr).connection
        self.pairing_stream = self.security.OnPairing()

        if self.connection and test in ACCEPTS_REMOTE_PAIRING_CONFIRMATION:
            event = get_event(pairing_stream=self.pairing_stream, addr=pts_addr)
            self.pairing_stream.send(event=event, confirm=True)
            self.pairing_stream.close()
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_PAIRING_PROCESS(self, test, pts_addr: bytes, **kwargs):
        """
        Please start pairing process.
        """
        if self.connection:
            self.security.Pair(connection=self.connection)
            if test in NEEDS_PAIRING_CONFIRMATION:
                event = get_event(pairing_stream=self.pairing_stream, addr=pts_addr)
                self.pairing_stream.send(event=event, confirm=True)
                self.pairing_stream.close()
            return "OK"

    @assert_description
    def MMI_IUT_SEND_DISCONNECTION_REQUEST(self, **kwargs):
        """
        Please initiate a disconnection to the PTS.

        Description: Verify that
        the Implementation Under Test(IUT) can initiate a disconnect request to
        PTS.
        """
        self.host.DisconnectLE(connection=self.connection)
        self.connection = None
        return "OK"

    def MMI_LESC_NUMERIC_COMPARISON(self, **kwargs):
        """
        Please confirm the following number matches IUT: 385874.
        """
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_RESET(self, **kwargs):
        """
        Please reset your device.
        """
        self.host.SoftReset()
        return "OK"

    @assert_description
    def MMI_TESTER_ENABLE_CONNECTION_SM(self, test, pts_addr, **kwargs):
        """
        Action: Place the IUT in connectable mode
        """
        self.pairing_stream = self.security.OnPairing()
        self.host.EnableConnectableMode(address=pts_addr)

        for _ in range(NUM_OF_CONFIRMATIONS_REQUIRED.get(test, 0)):
            event = get_event(pairing_stream=self.pairing_stream, addr=pts_addr)
            self.pairing_stream.send(event=event, confirm=True)

        self.pairing_stream.close()
        return "OK"

    @assert_description
    def MMI_IUT_SMP_TIMEOUT_30_SECONDS(self, **kwargs):
        """
        Wait for the 30 seconds. Lower tester will not send corresponding or
        next SMP message.
        """
        time.sleep(30)
        return "OK"

    @assert_description
    def MMI_IUT_SMP_TIMEOUT_ADDITIONAL_10_SECONDS(self, **kwargs):
        """
        Wait for an additional 10 seconds. Lower test will send corresponding or
        next SMP message.
        """
        time.sleep(10)
        return "OK"

    @match_description
    def MMI_DISPLAY_PASSKEY_CODE(self, pts_addr: bytes, passkey: str, **kwargs):
        """
        Please enter (?P<passkey>[0-9]*) in the IUT.
        """
        self.host.EnterPasskey(address=pts_addr, passkey=passkey)
        return "OK"

    @assert_description
    def MMI_ENTER_PASSKEY_CODE(self, **kwargs):
        """
        Please enter 6 digit passkey code.
        """

        return "OK"
