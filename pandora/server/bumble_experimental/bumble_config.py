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

import logging

import grpc
from bumble.device import Connection as BumbleConnection
from bumble.device import Device
from bumble.pairing import PairingConfig
from bumble.pairing import PairingDelegate as BasePairingDelegate
from bumble.pandora import Config, utils
from bumble.pandora.security import PairingDelegate
from google.protobuf.empty_pb2 import Empty
from pandora.host_pb2 import PUBLIC
from pandora_experimental.bumble_config_grpc_aio import BumbleConfigServicer
from pandora_experimental.bumble_config_pb2 import OverrideRequest
from pandora.security_pb2 import (
    PairingEvent,
    PairingEventAnswer,
)


class BumbleConfigService(BumbleConfigServicer):
    device: Device

    def __init__(self, device: Device, server_config: Config) -> None:
        self.log = utils.BumbleServerLoggerAdapter(logging.getLogger(), {
            "service_name": "BumbleConfig",
            "device": device
        })
        self.event_queue: Optional[asyncio.Queue[PairingEvent]] = None
        self.event_answer: Optional[AsyncIterator[PairingEventAnswer]] = None
        self.device = device
        self.server_config = server_config

    @utils.rpc
    async def Override(self, request: OverrideRequest, context: grpc.ServicerContext) -> Empty:

        pc_req = request.pairing_config
        self.log.debug(f"Override: {pc_req}")

        def pairing_config_factory(connection: BumbleConnection) -> PairingConfig:
            return PairingConfig(
                sc=pc_req.sc,
                mitm=pc_req.mitm,
                bonding=pc_req.bonding,
                identity_address_type=(PairingConfig.AddressType.PUBLIC if pc_req.identity_address_type == PUBLIC else
                                       PairingConfig.AddressType.RANDOM,),
                delegate=PairingDelegate(
                    connection,
                    self,
                    io_capability=BasePairingDelegate.IoCapability(request.io_capability),
                    local_initiator_key_distribution=request.initiator_key_distribution,
                    local_responder_key_distribution=request.responder_key_distribution,
                ),
            )

        self.device.pairing_config_factory = pairing_config_factory

        return Empty()
