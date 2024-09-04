from __future__ import annotations
import asyncio
import grpc
import grpc.aio
import logging
import struct
import functools
from typing import Optional

from bumble import rfcomm
from bumble import hci

from bumble.device import Device, Connection
from google.protobuf import empty_pb2  # pytype: disable=pyi-error

from bumble import hfp
from bumble.hfp import HfProtocol

hf_protocol: Optional[HfProtocol] = None
from pandora_experimental.hf_grpc_aio import HfServicer

from bumble.pandora import utils

from bumble.device import Address

from bumble.core import (
    BT_BR_EDR_TRANSPORT,
    BT_L2CAP_PROTOCOL_ID,
    BT_HUMAN_INTERFACE_DEVICE_SERVICE,
    BT_HIDP_PROTOCOL_ID,
    UUID,
    ProtocolError,
)

peer_address = None
hf_protocol: Optional[HfProtocol] = None
active_connection: Optional[Connection] = None


def on_dlc(dlc: rfcomm.DLC, configuration: hfp.HfConfiguration):
    logging.error(f'*** DLC connected {dlc}')
    global hf_protocol, active_connection
    global peer_address

    active_connection = dlc.multiplexer.l2cap_channel.connection

    hf_protocol = HfProtocol(dlc, configuration)
    logging.error(f'QAZ configuration {configuration}')
    asyncio.create_task(hf_protocol.run())
    peer_address = dlc.multiplexer.l2cap_channel.connection.peer_address
    # peer_address= temp_address[:-2]
    logging.error(f'QAZ peer_address {peer_address}')


def _default_hf_configuration():
    # Hands-Free profile configuration.
    # TODO: load configuration from file.
    configuration = hfp.HfConfiguration(
        supported_hf_features=[
            hfp.HfFeature.THREE_WAY_CALLING,
            hfp.HfFeature.REMOTE_VOLUME_CONTROL,
            hfp.HfFeature.ENHANCED_CALL_STATUS,
            hfp.HfFeature.ENHANCED_CALL_CONTROL,
            hfp.HfFeature.CODEC_NEGOTIATION,
            hfp.HfFeature.HF_INDICATORS,
            hfp.HfFeature.ESCO_S4_SETTINGS_SUPPORTED,
        ],
        supported_hf_indicators=[
            hfp.HfIndicator.BATTERY_LEVEL,
        ],
        supported_audio_codecs=[
            hfp.AudioCodec.CVSD,
            hfp.AudioCodec.MSBC,
        ],
    )
    return configuration


# This class implements the Hid Pandora interface.
class HfService(HfServicer):

    hf_config = None
    global peer_address

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device
        logging.error(f'QAZ device {device}')

        global hf_config
        hf_config = _default_hf_configuration()
        logging.error(f'QAZ hf_config {hf_config}')

        # Create and register a server
        global rfcomm_server
        rfcomm_server = rfcomm.Server(self.device)
        logging.error(f'QAZ rfcomm_server {rfcomm.Server}')

        # Listen for incoming DLC connections
        global channel_number
        channel_number = rfcomm_server.listen(lambda dlc: on_dlc(dlc, hf_config))
        logging.error(f'### Listening for connection on channel {channel_number}')

        # Advertise the HFP RFComm channel in the SDP
        self.device.sdp_service_records = {0x00010001: hfp.make_hf_sdp_records(0x00010001, channel_number, hf_config)}

        logging.error(f'QAZ hfp {hfp}')

        logging.error(f'QAZ self.device.sdp_service_records {self.device.sdp_service_records}')

    @utils.rpc
    async def Connect(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.error(f'QAZ HFP Profile Connect RPC')

        return empty_pb2.Empty()

    @utils.rpc
    async def Disconnect(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.info(f'QAZ HFP Profile Disconnect RPC')

        global active_connection

        if active_connection:
            await active_connection.disconnect()
            logging.info('Successfully Disconnected')

            active_connection = None

        return empty_pb2.Empty()
