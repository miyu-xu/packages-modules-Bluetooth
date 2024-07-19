# Copyright 2023 Google LLC
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

import argparse
import asyncio
import logging
import json

from bumble import pandora as bumble_server
from bumble.a2dp import (A2DP_SBC_CODEC_TYPE, SBC_DUAL_CHANNEL_MODE, SBC_JOINT_STEREO_CHANNEL_MODE,
                         SBC_LOUDNESS_ALLOCATION_METHOD, SBC_MONO_CHANNEL_MODE, SBC_SNR_ALLOCATION_METHOD,
                         SBC_STEREO_CHANNEL_MODE, SbcMediaCodecInformation, make_audio_sink_service_sdp_records,
                         make_audio_source_service_sdp_records)
from bumble.avdtp import (
    Listener as AvdtpListener,
    AVDTP_AUDIO_MEDIA_TYPE,
    MediaCodecCapabilities,
)
from bumble.avrcp import make_target_service_sdp_records, make_controller_service_sdp_records
from bumble.device import Device
from bumble.pandora import PandoraDevice, Config, serve
from bumble.hfp import (
    make_hf_sdp_records,
    HfConfiguration,
    HfFeature,
    HfIndicator,
    AudioCodec as HfAudioCodec,
    ProfileVersion as HfProfileVersion,
)
from bumble.sdp import ServiceAttribute

from bumble_experimental.asha import AshaService
from bumble_experimental.dck import DckService
from bumble_experimental.gatt import GATTService
from bumble_experimental.rfcomm import RFCOMMService

from pandora_experimental.asha_grpc_aio import add_AshaServicer_to_server
from pandora_experimental.dck_grpc_aio import add_DckServicer_to_server
from pandora_experimental.gatt_grpc_aio import add_GATTServicer_to_server
from pandora_experimental.rfcomm_grpc_aio import add_RFCOMMServicer_to_server

from typing import Dict, Any

from typing import Any, Dict, List

BUMBLE_SERVER_GRPC_PORT = 7999
ROOTCANAL_PORT_CUTTLEFISH = 7300


def main(grpc_port: int, rootcanal_port: int, transport: str, config: str) -> None:
    register_experimental_services()
    if '<rootcanal-port>' in transport:
        transport = transport.replace('<rootcanal-port>', str(rootcanal_port))

    bumble_config = retrieve_config(config)
    bumble_config.setdefault('transport', transport)
    sdp_service_records = _sdp_service_records()
    device = PandoraDevice(bumble_config, sdp_service_records=sdp_service_records, enable_profiles=enable_profiles)

    server_config = Config()
    server_config.load_from_dict(bumble_config.get('server', {}))

    logging.basicConfig(level=logging.DEBUG,
                        format='%(asctime)s.%(msecs).03d %(levelname)-8s %(message)s',
                        datefmt='%m-%d %H:%M:%S')
    asyncio.run(serve(device, config=server_config, port=grpc_port))


def args_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Bumble command-line tool")

    parser.add_argument('--grpc-port', type=int, default=BUMBLE_SERVER_GRPC_PORT, help='gRPC port to serve')
    parser.add_argument('--rootcanal-port', type=int, default=ROOTCANAL_PORT_CUTTLEFISH, help='Rootcanal TCP port')
    parser.add_argument('--transport',
                        type=str,
                        default='tcp-client:127.0.0.1:<rootcanal-port>',
                        help='HCI transport (default: tcp-client:127.0.0.1:<rootcanal-port>)')
    parser.add_argument('--config', type=str, help='Bumble json configuration file')

    return parser


def enable_profiles(bumble_device: Device) -> None:
    enable_a2dp(bumble_device)


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


def enable_a2dp(bumble_device: Device) -> None:
    # TODO CHECK IF WE NEED TO STORE THE VARIABLE
    a2dp = AvdtpListener.for_device(bumble_device)

    def on_avdtp_connection(server) -> None:  # type: ignore
        a2dp_sink = server.add_sink(codec_capabilities())  # type: ignore

    a2dp.on('connection', on_avdtp_connection)  # type: ignore


def register_experimental_services() -> None:
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_AshaServicer_to_server(AshaService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_DckServicer_to_server(DckService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_GATTServicer_to_server(GATTService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_RFCOMMServicer_to_server(RFCOMMService(bumble.device), server))


def _sdp_service_records() -> Dict[int, List[ServiceAttribute]]:
    rfcomm_channel = 1
    hf_configuration = HfConfiguration(
        supported_hf_features=[
            HfFeature.THREE_WAY_CALLING,
            HfFeature.EC_NR,
            HfFeature.VOICE_RECOGNITION_ACTIVATION,
            HfFeature.ENHANCED_CALL_STATUS,
            HfFeature.CODEC_NEGOTIATION,
            HfFeature.ESCO_S4_SETTINGS_SUPPORTED,
        ],
        supported_hf_indicators=[
            HfIndicator.BATTERY_LEVEL,
        ],
        supported_audio_codecs=[
            HfAudioCodec.CVSD,
            HfAudioCodec.MSBC,
        ],
    )
    sdp_records = {
        0x00010001:
            make_hf_sdp_records(  # HandsFree
                0x00010001,
                rfcomm_channel,
                hf_configuration,
                version=HfProfileVersion.V1_5,
            ),
        0x00010002:
            make_audio_source_service_sdp_records(0x00010002),  # A2DP Source
        0x00010003:
            make_audio_sink_service_sdp_records(0x00010003),  # A2DP Sink
        0x00010004:
            make_controller_service_sdp_records(0x00010004),  # AVRCP Controller
        0x00010005:
            make_target_service_sdp_records(0x00010005),  # AVRCP Target
    }

    return sdp_records


def retrieve_config(config: str) -> Dict[str, Any]:
    if not config:
        return {}

    with open(config, 'r') as f:
        return json.load(f)


if __name__ == '__main__':
    args = args_parser().parse_args()
    main(**vars(args))
