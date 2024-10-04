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

from bumble.device import Device, Connection as BumbleConnection
from bumble.pairing import PairingConfig, PairingDelegate as BasePairingDelegate
from bumble.pandora import utils, Config
from bumble.pandora.security import PairingDelegate
from pandora_experimental.config_grpc_aio import ConfigServicer
from pandora_experimental.config_pb2 import SetConfigRequest
from google.protobuf.empty_pb2 import Empty


class ConfigService(ConfigServicer):
    device: Device

    def __init__(self, device: Device, server_config: Config) -> None:
        self.log = utils.BumbleServerLoggerAdapter(logging.getLogger(), {"service_name": "Config", "device": device})
        self.device = device
        self.server_config = server_config
        self.log = utils.BumbleServerLoggerAdapter(logging.getLogger(), {"service_name": "Config", "device": device})

    @utils.rpc
    async def SetConfig(self, request: SetConfigRequest, context: grpc.ServicerContext) -> Empty:

        # oob config ?
        if request.HasField("pairing_config"):

            def pairing_config_factory(connection: BumbleConnection) -> PairingConfig:
                pairing_delegate = PairingDelegate(
                    connection=connection,
                    io_capability=BasePairingDelegate.IoCapability(request.io_capability),
                    local_initiator_key_distribution=BasePairingDelegate.KeyDistribution(
                        request.initiator_key_distribution),
                    local_responder_key_distribution=BasePairingDelegate.KeyDistribution(
                        request.responder_key_distribution),
                )

                pc_req = request.pairing_config
                return PairingConfig(
                    sc=pc_req.sc,
                    mitm=pc_req.mitm,
                    bonding=pc_req.bonding,
                    delegate=pairing_delegate,
                )

            self.device.pairing_config_factory = pairing_config_factory

        return Empty()
