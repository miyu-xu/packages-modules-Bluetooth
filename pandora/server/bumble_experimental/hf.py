from __future__ import annotations
import asyncio
import grpc
import grpc.aio
import logging
import struct

from bumble.device import Device
from google.protobuf import empty_pb2  # pytype: disable=pyi-error

from pandora_experimental.hf_grpc_aio import HfServicer

from bumble.pandora import utils


# This class implements the Hf Pandora interface.
class HfService(HfServicer):

    hf_device = None

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device

    @utils.rpc
    async def Connect(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:

        logging.error(f'AAA HFP Profile connect - Rajesh rpc')
        # try:
        #     hid_device.virtual_cable_unplug()
        #     try:
        #         hid_host_bd_addr = str(hid_device.remote_device_bd_address)
        #         await hid_device.device.keystore.delete(hid_host_bd_addr)
        #     except KeyError:
        #         logging.error(f'Device not found or Device already unpaired.')
        #         raise
        # except AttributeError as e:
        #     logging.exception(f'Device does not exist')
        #     raise e

        logging.info(f'AAA ConnectHost')
        return empty_pb2.Empty()
