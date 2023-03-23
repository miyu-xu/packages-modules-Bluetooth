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
"""BAP proxy module."""
from queue import Empty, Queue
from threading import Thread
import asyncio
import sys
import threading

from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy

from pandora.security_grpc import Security
from pandora.security_pb2 import LE_LEVEL3, LESecurityLevel, PairingEventAnswer
from pandora.host_grpc import Host
from pandora.host_pb2 import ConnectabilityMode, OwnAddressType, RANDOM
from pandora_experimental.le_audio_grpc import LeAudio
from pandora_experimental.gatt_grpc import GATT


def debug(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


class BAPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__(channel)
        self.host = Host(channel)
        self.security = Security(channel)
        self.gatt = GATT(channel)
        self.le_audio = LeAudio(channel)
        self.le_audio_opened = False
        self.connection = None
        self.passkey_queue = Queue()
        self._auto_confirm_requests()

    @assert_description
    def MMI_IUT_CONFIRM_BASE(self, **kwargs):
        """
        Please confirm received BASE entry Basic Audio Announcements:
        Length: [37 (0x25)]
            AD Type: [22 (0x16)]
            Basic Audio
        Announcement Service UUID: [6225 (0x1851)] Service UUID
            Presentation
        Delay: [40000 (0x009C40)]
            Num Subgroups: [1 (0x01)]
            Codec And
        Metadata Subgroups: {
            Num BIS: [1 (0x01)]
            Codec And Metadata
        Lv2:
                Codec Configuration:
                    Codec ID: [6 (0x06)]
        Codec ID Company ID: [0 (0x0000)]
                    Codec ID Vendor ID: [0
        (0x0000)]
                    Codec Specific Configuration Length: [10 (0x0A)]
        Codec Specific Configuration LTV:
                        LTV Wrapper: {
        Length: [2 (0x02)]
            Type and Value:
                Type: [1 (0x01)]
        Value: [0x03],
            Length: [2 (0x02)]
            Type and Value:
        Type: [2 (0x02)]
                Value: [0x01],
            Length: [3 (0x03)]
            Type
        and Value:
                Type: [4 (0x04)]
                Value: [0x2800]}
        Metadata Length: [4 (0x04)]
                Metadata:
                    LTV Wrapper:
        {
            Length: [3 (0x03)]
            Type and Value:
                Type: [2 (0x02)]
        Value: [0x0400]}
            BIS Codec Subgroup Lv3: {
            BIS index: [1 (0x01)]
        Codec Specific Configuration Length: [6 (0x06)]
            Codec Specific
        Configuration LTV:
                Length: [5 (0x05)]
                Type and Value:
        Type: [3 (0x03)]
                    Value: [0x01000000]}}
        """

        return "OK"

    @assert_description
    def MMI_IUT_CONFIRM_ADV(self, **kwargs):
        """
        Please scan for Advertising Packets and Press OK to confirm receiving
        the ASCS UUID and Available Audio Contexts.
        """

        assert False
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
        return "OK"

    @match_description
    def _mmi_2004(self, **kwargs):
        """
        Please confirm that 6 digit number is matched with (?P<passkey>[0-9]+).
        """

        return "OK"

    @match_description
    def _mmi_20103(self, **kwargs):
        """
        Please take action to discover the (?P<characteristic>.*?) characteristic
        from the (?P<service>.*?). Discover the primary service if needed.

        Description: Verify that the Implementation Under Test \(IUT\)
        can send Discover All Characteristics command.
        """

        self._open_le_audio()
        return "OK"

    @assert_description
    def _mmi_20106(self, **kwargs):
        """
        Please write to Client Characteristic Configuration Descriptor of
        Broadcast Receive State characteristic to enable notification.
        """

        self._open_le_audio()
        return "OK"

    @match_description
    def _mmi_20107(self, **kwargs):
        """
        Please send Read Request to read (?P<characteristic>.*?) characteristic
        with handle = (?P<handle>0x[A-F0-9]{4}).
        """

        self._open_le_audio()
        return "OK"

    @assert_description
    def _mmi_20115(self, **kwargs):
        """
        Please initiate a GATT disconnection to the PTS.

        Description: Verify
        that the Implementation Under Test (IUT) can initiate GATT disconnect
        request to PTS.
        """

        self.host.Disconnect(connection=self.connection)
        self.connection = None
        return "OK"

    @assert_description
    def _mmi_20206(self, **kwargs):
        """
        Please verify that for each supported characteristic, attribute
        handle/UUID pair(s) is returned to the upper tester.Sink PAC: Attribute
        Handle = 0x00A1
        Characteristic Properties = 0x12
        Handle = 0x00A2
        UUID =
        0x2BC9

        Sink Audio Locations: Attribute Handle = 0x00A4
        Characteristic
        Properties = 0x1A
        Handle = 0x00A5
        UUID = 0x2BCA

        Source PAC: Attribute
        Handle = 0x00A7
        Characteristic Properties = 0x12
        Handle = 0x00A8
        UUID =
        0x2BCB

        Source Audio Locations: Attribute Handle = 0x00AA
        Characteristic
        Properties = 0x1A
        Handle = 0x00AB
        UUID = 0x2BCC

        Available Audio
        Contexts: Attribute Handle = 0x00AD
        Characteristic Properties = 0x12
        Handle = 0x00AE
        UUID = 0x2BCD

        Supported Audio Contexts: Attribute
        Handle = 0x00B0
        Characteristic Properties = 0x12
        Handle = 0x00B1
        UUID =
        0x2BCE
        """

        return "OK"

    @assert_description
    def _mmi_20145(self, **kwargs):
        """
        Please click Yes if IUT support Write Request, otherwise click No.
        """

        return "Yes"

    @match_description
    def _mmi_20110(self, test: str, handle: str, **kwargs):
        """
        Please send write request to handle (?P<handle>0x[A-F0-9]{4}) with following value.
        Any
        attribute value
        """

        # These characteristics are never written in the LE Audio
        # implementation:
        #   - Sink Audio Locations
        #   - Source Audio Locations
        if test in ['BAP/CL/CGGIT/CHA/BV-02-C', 'BAP/CL/CGGIT/CHA/BV-04-C']:
            self.gatt.WriteAttFromHandle(connection=self.connection,
                                         handle=int(handle, 16), value=bytes([42]))
        return "OK"


    def _auto_confirm_requests(self):

        def task():
            pairing_events = self.security.OnPairing()
            for event in pairing_events:
                if event.just_works or event.numeric_comparison:
                    pairing_events.send(PairingEventAnswer(event=event, confirm=True))
                if event.passkey_entry_request:
                    try:
                        passkey = self.passkey_queue.get(timeout=15)
                        pairing_events.send(PairingEventAnswer(event=event, passkey=int(passkey)))
                    except Empty:
                        debug("No passkey provided within 15 seconds")

        threading.Thread(target=task).start()

    def _open_le_audio(self):
        if self.le_audio_opened:
            return
        self.security.WaitSecurity(connection=self.connection, le=LE_LEVEL3)
        self.le_audio.Open(connection=self.connection)
        self.le_audio_opened = True
