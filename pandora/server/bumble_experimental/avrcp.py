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

import grpc
import logging

from bumble.avc import PassThroughFrame
from bumble.avdtp import Listener as AvdtpListener, MediaCodecCapabilities, AVDTP_AUDIO_MEDIA_TYPE
from bumble.avrcp import Protocol as AvrcpProtocol, make_target_service_sdp_records, make_controller_service_sdp_records
from bumble.a2dp import (A2DP_SBC_CODEC_TYPE, SBC_DUAL_CHANNEL_MODE, SBC_JOINT_STEREO_CHANNEL_MODE,
                         SBC_LOUDNESS_ALLOCATION_METHOD, SBC_MONO_CHANNEL_MODE, SBC_SNR_ALLOCATION_METHOD,
                         SBC_STEREO_CHANNEL_MODE, SbcMediaCodecInformation, make_audio_sink_service_sdp_records,
                         make_audio_source_service_sdp_records)
from bumble.device import Device, Connection
from bumble.pandora import utils
from google.protobuf.empty_pb2 import Empty
from pandora_experimental.avrcp_grpc_aio import AVRCPServicer


class AvrcpService(AVRCPServicer):
    device: Device
    connection: Connection
    avrcp_protocol: AvrcpProtocol
    avdtp_listener: AvdtpListener

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device
        self.logger = logging.getLogger(__name__)
        self.connection = None
        self.avrcp_connected = False

        sdp_records = {
            0x00010002: make_audio_source_service_sdp_records(0x00010002),  # A2DP Source
            0x00010003: make_audio_sink_service_sdp_records(0x00010003),  # A2DP Sink
            0x00010004: make_controller_service_sdp_records(0x00010004),  # AVRCP Controller
            0x00010005: make_target_service_sdp_records(0x00010005),  # AVRCP Target
        }
        self.device.sdp_service_records.update(sdp_records)

        # Register AVDTP L2cap
        self.avdtp_listener = AvdtpListener.for_device(device)

        self.device.on('connection', self.on_device_connected)  # type: ignore
        self.avdtp_listener.on('connection', self.on_avdtp_connection)  # type: ignore

        # Register AVRCP L2cap
        self.avrcp_protocol = AvrcpProtocol(delegate=None)
        self.avrcp_protocol.listen(device)

    def __del__(self) -> None:
        self.device.remove_listener('connection', self.on_device_connected)  # type: ignore
        self.avdtp_listener.remove_listener('connection', self.on_avdtp_connection)  # type: ignore

    def on_avdtp_connection(self, server) -> None:  # type: ignore
        server.add_sink(codec_capabilities())  # type: ignore

    def on_device_connected(self, connection) -> None:
        self.connection = connection

    @utils.rpc
    async def SendKeyEventNext(self, request: Empty, context: grpc.ServicerContext) -> Empty:
        if not self.avrcp_connected:
            await self.avrcp_protocol.connect(self.connection)
            self.avrcp_connected = True

        self.logger.info("SendKeyEventNext")
        await self.avrcp_protocol.send_key_event(PassThroughFrame.OperationId.FORWARD, True)
        await self.avrcp_protocol.send_key_event(PassThroughFrame.OperationId.FORWARD, False)
        return Empty()

    @utils.rpc
    async def MonitorNowPlayingContent(self, request: Empty, context: grpc.ServicerContext) -> Empty:
        if not self.avrcp_connected:
            await self.avrcp_protocol.connect(self.connection)
            self.avrcp_connected = True

        registered = False
        async for identifier in self.avrcp_protocol.monitor_now_playing_content():
            if registered is True:
                self.logger.info("MonitorNowPlayingContent CHANGED)")
                return Empty()
            self.logger.info("MonitorNowPlayingContent INTERIM")
            registered = True


def codec_capabilities() -> MediaCodecCapabilities:
    """Codec capabilities for the Bumble sink devices."""

    return MediaCodecCapabilities(
        media_type=AVDTP_AUDIO_MEDIA_TYPE,
        media_codec_type=A2DP_SBC_CODEC_TYPE,
        media_codec_information=SbcMediaCodecInformation.from_lists(
            sampling_frequencies=[48000, 44100, 32000, 16000],
            channel_modes=[
                SBC_MONO_CHANNEL_MODE,
                SBC_DUAL_CHANNEL_MODE,
                SBC_STEREO_CHANNEL_MODE,
                SBC_JOINT_STEREO_CHANNEL_MODE,
            ],
            block_lengths=[4, 8, 12, 16],
            subbands=[4, 8],
            allocation_methods=[
                SBC_LOUDNESS_ALLOCATION_METHOD,
                SBC_SNR_ALLOCATION_METHOD,
            ],
            minimum_bitpool_value=2,
            maximum_bitpool_value=53,
        ),
    )
