# Copyright (C) 2025 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import threading
import time

from mmi2grpc._audio import AudioSignal
from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy
from mmi2grpc._rootcanal import Dongle
from pandora.host_grpc import Host
from pandora.host_pb2 import PUBLIC, RANDOM
from pandora.security_grpc import Security
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer
from pandora_experimental.gatt_grpc import GATT
from pandora_experimental.gmap_grpc import GMAP
from pandora_experimental.gmap_pb2 import GmaPlaybackAudioRequest
from pandora_experimental.le_audio_grpc import LeAudio

AUDIO_SIGNAL_AMPLITUDE = 0.8
AUDIO_SIGNAL_SAMPLING_RATE = 44100

class GMAPProxy(ProfileProxy):

    def __init__(self, channel, rootcanal):
        super().__init__(channel)
        self.gmap = GMAP(channel)
        self.rootcanal = rootcanal
        self.gatt = GATT(channel)
        self.host = Host(channel)
        self.le_audio = LeAudio(channel)
        self.security = Security(channel)
        self.pairing_events = self.security.OnPairing()
        self.discovered_services = None
        self.connection = None

        def convert_frame(data):
            return GmaPlaybackAudioRequest(data=data)

        self.audio = AudioSignal(lambda frames: self.gmap.GmaPlaybackAudio(map(convert_frame, frames)),
                                 AUDIO_SIGNAL_AMPLITUDE, AUDIO_SIGNAL_SAMPLING_RATE)
        
    def test_started(self, test: str, **kwargs):
        self.rootcanal.select_pts_dongle(Dongle.LAIRD_BL654)
        return "OK"
    
    @assert_description
    def _mmi_20100(self, test, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.

        Description: Verify that
        the Implementation Under Test (IUT) can initiate a GATT connect request
        to the PTS.
        """

        self.connection = self.host.ConnectLE(own_address_type=RANDOM, public=pts_addr).connection

        def secure():
            self.security.Secure(connection=self.connection, le=LE_LEVEL3)

        threading.Thread(target=secure).start()

        return "OK"

    @match_description
    def _mmi_2004(self, pts_addr: bytes, passkey: str, **kwargs):
        """
        Please confirm that 6 digit number is matched with (?P<passkey>[0-9]+).
        """

        for event in self.pairing_events:
            if event.address == pts_addr and event.numeric_comparison == int(passkey):
                self.pairing_events.send(PairingEventAnswer(
                    event=event,
                    confirm=True,
                ))
                return "OK"

        assert False

    @match_description
    def _mmi_20106(self, test: str, characteristic_name: str, type: str, **kwargs):
        """
        Please write to Client Characteristic Configuration Descriptor
        of (?P<characteristic_name>(ASE Control Point|Sink Audio Stream Endpoint|Active Preset Index))
        characteristic to enable (?P<type>(notification|indication)).
        """

        return "OK"
    
    @assert_description
    def _mmi_311(self, **kwargs):
        """
        Please configure 1 SINK ASE with Config Setting: .
        After that, configure
        to streaming state.
        """

        self.le_audio.Open(connection=self.connection)
        self.gmap.GmaStart(connection=self.connection)
        self.audio.start()
        return "OK"

    def _mmi_20001(self, **kwargs):
        """
        Please prepare IUT into a connectable mode.
    
        Description: Verify that
        the Implementation Under Test (IUT) can accept GATT connect request from
        PTS.
        """
        self.advertise = self.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=PUBLIC,
        )
        self.pairing_events = self.security.OnPairing()
        time.sleep(1)
        return "OK"

    @assert_description
    def _mmi_20206(self, **kwargs):
        """
        Please verify that for each supported characteristic, attribute
        handle/UUID pair(s) is returned to the upper tester.GMAP Role: Attribute
        Handle = 0x0281
        Characteristic Properties = 0x02
        Handle = 0x0282
        UUID =
        0x2C00

        UGG Features: Attribute Handle = 0x0283
        Characteristic
        Properties = 0x02
        Handle = 0x0284
        UUID = 0x2C01

        UGT Features: Attribute
        Handle = 0x0285
        Characteristic Properties = 0x02
        Handle = 0x0286
        UUID =
        0x2C02

        BGS Features: Attribute Handle = 0x0287
        Characteristic
        Properties = 0x02
        Handle = 0x0288
        UUID = 0x2C03

        BGR Features: Attribute
        Handle = 0x0289
        Characteristic Properties = 0x02
        Handle = 0x028A
        UUID =
        0x2C04
        """

        return "OK"
