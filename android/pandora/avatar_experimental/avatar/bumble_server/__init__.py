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
"""Pandora Bumble Server."""

__version__ = "0.0.1"

import asyncio
import logging
import os
import grpc

from bumble.host import Host
from bumble.device import Device, DeviceConfiguration
from bumble.transport import open_transport

from bumble.a2dp import make_audio_sink_service_sdp_records
from bumble.smp import PairingConfig

from pandora_experimental.host_grpc import add_HostServicer_to_server
from pandora_experimental.security_grpc import add_SecurityServicer_to_server
from .host import HostService
from .security import SecurityService

BUMBLE_SERVER_PORT = 7999
ROOTCANAL_PORT_CUTTLEFISH = 7300

current_dir = os.path.dirname(os.path.realpath(__file__))


class BumblePandoraServer:

    def __init__(self, grpc_port, hci, config):
        self.hci = hci
        self.config = config
        self.pairing_config_factory = lambda connection: PairingConfig(bonding=False)
        self.host = Host(controller_source=hci.source, controller_sink=hci.sink)
        self.hard_reset()

        self.server = grpc.aio.server()
        self.host_service = HostService(self)
        self.security_service = SecurityService(self)
        add_HostServicer_to_server(self.host_service, self.server)
        add_SecurityServicer_to_server(self.security_service, self.server)
        self.grpc_port = self.server.add_insecure_port(f'localhost:{grpc_port}')

    @classmethod
    async def open(cls, grpc_port, transport_name, config):
        hci = await open_transport(transport_name)
        return cls(grpc_port=grpc_port, hci=hci, config=config)

    async def start(self):
        await self.device.power_on()
        await self.server.start()

    def hard_reset(self):
        device_config = DeviceConfiguration()
        device_config.load_from_dict(self.config)
        if hasattr(self, 'device'):
            self.device.host = None
            del self.device
        self.device = Device(config=device_config, host=self.host)
        self.device.classic_enabled = self.config.get('classic_enabled', False)
        self.device.pairing_config_factory = self.pairing_config_factory

    async def wait_for_termination(self):
        await self.server.wait_for_termination()

    async def close(self):
        await self.server.stop(None)
        await self.hci.close()


async def serve():
    transport = f'tcp-client:127.0.0.1:{ROOTCANAL_PORT_CUTTLEFISH}'
    server = await BumblePandoraServer.open(BUMBLE_SERVER_PORT, transport, {'classic_enabled': True})

    await server.start()
    await server.wait_for_termination()
    await server.close()


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    asyncio.run(serve())
