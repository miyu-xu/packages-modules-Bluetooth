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

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy
from mmi2grpc._rootcanal import Dongle
from pandora.host_grpc import Host
from pandora_experimental.le_audio_grpc import LeAudio


class BAPProxy(ProfileProxy):
    def __init__(self, channel, rootcanal):
        super().__init__(channel)
        self.host = Host(channel)
        self.le_audio = LeAudio(channel)
        self.rootcanal = rootcanal

    def test_started(self, test: str, **kwargs):
        self.rootcanal.select_pts_dongle(Dongle.LAIRD_BL654)
        return "OK"

    @assert_description
    def MMI_IUT_SEND_BROADCAST_AUDIO_ANNOUNCEMENT(self, pts_addr: bytes, **kwargs):
        """
        Please advertise with Broadcast Audio Announcement (0x1852) service data
        """
        self.le_audio.StartBroadcast()
        return "OK"

