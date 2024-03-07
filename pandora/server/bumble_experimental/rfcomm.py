# Copyright (C) 2024 The Android Open Source Project
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import asyncio
import grpc
import logging

from bumble.device import Device
from bumble.rfcomm import Client, Server
from pandora_experimental.rfcomm_pb2 import (
    AcceptConnectionRequest,
    AcceptConnectionResponse,
    ConnectionRequest,
    ConnectionResponse,
    DisconnectionRequest,
    DisconnectionResponse,
    RfcommConnection,
    ServerId,
    ServerOptions,
    StartServerResponse,
    StopServerRequest,
    StopServerResponse,
)
from pandora_experimental.rfcomm_grpc_aio import RFCOMMServicer

class RFCOMMService(RFCOMMServicer):
    device: Device

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device

    async def ConnectToServer(self, request: ConnectionRequest, context: grpc.ServicerContext) -> ConnectionResponse:
        # TODO
        # ConnectionRequest has an address and a uuid
        logging.info(f"ConnectToServer")

        connection = # type RfcommConnection, new connection
        return ConnectionResponse(connection=connection)

    async def StartServer(self, request: ServerOptions, context: grpc.ServicerContext) -> StartServerResponse:
        # TODO
        # ServerOptions has a name and a uuid
        logging.info(f"StartServer")

        server =  # type ServerId, server that was started
        return StartServerResponse(server=server)

    async def AcceptConnection(self, request: AcceptConnectionRequest, context: grpc.ServicerContext) -> AcceptConnectionResponse:
        # TODO
        # AcceptConnectionRequest has ServerId of the server accepting the request
        logging.info(f"AcceptConnection")

        connection = # type RfcommConnection, connection that was accepted
        return AcceptConnectionResponse(connection=connection)

    async def Disconnect(self, request: DisconnectionRequest, context: grpc.ServicerContext) -> DisconnectionResponse():
        # TODO
        # DisconnectionRequest has the RfcommConnection that we're trying to disconnect
        logging.info(f"Disconnect")
        
        return DisconnectionResponse()

    async def StopServer(self, request:StopServerRequest, context: grpc.ServicerContext) -> StopServerResponse:
        # TODO
        # StopServerRequest has the serverId of the server we're trying to stop
        logging.info(f"StopServer")

        return StopServerResponse()

