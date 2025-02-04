from __future__ import annotations
import asyncio
import grpc
import grpc.aio
import logging
from typing import Optional

from bumble import core
from bumble import rfcomm
from bumble import hci

from bumble.device import Device, Connection
from google.protobuf import empty_pb2  # pytype: disable=pyi-error

from bumble import hfp
from bumble.hfp import HfProtocol

hf_protocol: Optional[HfProtocol] = None
from pandora_experimental.hf_grpc_aio import HFServicer

from bumble.pandora import utils

from bumble.core import (
    BT_BR_EDR_TRANSPORT,
    ProtocolError,
)

hf_protocol: Optional[HfProtocol] = None
active_connection: Optional[Connection] = None



def on_dlc(dlc: rfcomm.DLC, configuration: hfp.HfConfiguration):
    logging.error(f'*** DLC connected {dlc}')
    global hf_protocol

    hf_protocol = HfProtocol(dlc, configuration)
    asyncio.create_task(hf_protocol.run())


def on_connect(connection: Connection):
    logging.info(f'ACL connection with peer {connection.peer_address}')
    global active_connection
    active_connection = connection
    logging.info(f'ACL Active Connection {active_connection}')


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


async def perform_acl_connection(t_device, p_address):
    global active_connection

    try:
        active_connection = await t_device.connect(p_address, transport=BT_BR_EDR_TRANSPORT)
        logging.info(f'ACL connection establised {active_connection}')
        await active_connection.authenticate()

    except ProtocolError as e:
        if e.error_code == hci.HCI_CONNECTION_ALREADY_EXISTS_ERROR:
            logging.warning(f'Connection with {active_connection.peer_address} already exsist')
            logging.warning(f'Connection Details {active_connection}')
        else:
            logging.error(f'Failed to establish connection')

    if not active_connection.is_encrypted:
        await active_connection.encrypt()


# This class implements the Hid Pandora interface.
class HFService(HFServicer):

    hf_config = None

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device

        global hf_config
        hf_config = _default_hf_configuration()

        # Create and register a server
        global rfcomm_server
        rfcomm_server = rfcomm.Server(self.device)

        # Listen for incoming DLC connections
        global channel_number
        channel_number = rfcomm_server.listen(lambda dlc: on_dlc(dlc, hf_config))
        logging.error(f'### Listening for connection on channel {channel_number}')

        # Advertise the HFP RFComm channel in the SDP
        self.device.sdp_service_records.update({0x00010001: hfp.make_hf_sdp_records(0x00010001, channel_number, hf_config)})

        self.device.on('connection', on_connect)

    @utils.rpc
    async def Connect(self, request: HfConnectRequest , context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.error(f'Connect RPC Function')
        logging.error(f'connect to addr : {request.address}')
        await perform_acl_connection(self.device, request.address)


        return empty_pb2.Empty()

    @utils.rpc
    async def Disconnect(self, request: HfDisconnectRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.error(f'Disconnect RPC Function')

        logging.error(f'disconnect to addr : {request.address}')

        acl_connection = self.device.find_connection_by_bd_addr(
            hci.Address.from_string_for_transport(
                request.address, BT_BR_EDR_TRANSPORT
        ), transport=0)  # BR/EDR

        if acl_connection:
            await acl_connection.disconnect()

        return empty_pb2.Empty()
