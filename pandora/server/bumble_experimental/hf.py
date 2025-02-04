from __future__ import annotations
import asyncio
import grpc
import grpc.aio
import logging
import struct
import functools
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

peer_address = None
hf_protocol: Optional[HfProtocol] = None
active_connection: Optional[Connection] = None



def on_dlc(dlc: rfcomm.DLC, configuration: hfp.HfConfiguration):
    logging.error(f'*** DLC connected {dlc}')
    global hf_protocol, active_connection
    global peer_address

    active_connection = dlc.multiplexer.l2cap_channel.connection

    hf_protocol = HfProtocol(dlc, configuration)
    asyncio.create_task(hf_protocol.run())
    peer_address = dlc.multiplexer.l2cap_channel.connection.peer_address


def on_connect(connection: Connection):
    logging.info(f'ACL connection with peer_1 {connection.peer_address}')
    global active_connection, peer_address
    active_connection = connection
    logging.info(f'ACL Active Connection {active_connection}')
    peer_address = connection.peer_address
    logging.info(f'ACL peer_address {peer_address}')


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
    global peer_address

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
    async def Connect(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.error(f'Connect RPC Function')
        global active_connection, peer_address
        # Connect ACL if not connected
        # await perform_acl_connection(self.device, peer_address)

        channel_number = rfcomm_server.listen(lambda dlc: on_dlc(dlc, hf_config))
        logging.info(f'Channel_number= {channel_number}')

        client_mux = await rfcomm.Client(active_connection).start()

        logging.error(f'Open_dlc')
        await client_mux.open_dlc(channel_number)

        return empty_pb2.Empty()

    @utils.rpc
    async def Aclconnect(self, request: AclConnectRequest , context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.error(f'Aclconnect RPC Function')
        # Connect ACL if not connected
        logging.error(f'ACL connect to addr : {request.address}')
        await perform_acl_connection(self.device, request.address)

        return empty_pb2.Empty()

    @utils.rpc
    async def Disconnect(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        logging.error(f'Disconnect RPC Function')
        global active_connection

        if active_connection:
            await active_connection.disconnect()
            active_connection = None

        return empty_pb2.Empty()
