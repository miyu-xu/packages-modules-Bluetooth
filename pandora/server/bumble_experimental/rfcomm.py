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
import logging
from typing import Dict, Optional

from bumble.device import Device
from bumble.rfcomm import Server
import grpc
from pandora_experimental.rfcomm_grpc_aio import RFCOMMServicer
from pandora_experimental.rfcomm_pb2 import (
    AcceptConnectionRequest,
    AcceptConnectionResponse,
    RfcommConnection,
    ServerId,
    ServerOptions,
    StartServerResponse,
    StopServerRequest,
    StopServerResponse,
)

# RFCOMMService can currently only support one server at a time.
class RFCOMMService(RFCOMMServicer):
    device: Device
    server_id: Optional[ServerId]
    server: Optional[Server]
    connections: Dict[int, RfcommConnection]  # key: channel

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device
        self.server_id = None
        self.server = None
        self.server_name = None
        self.server_uuid = None
        self.connections = {}
        self.next_server_id = 1
        self.next_conn_id = 1

    def get_server_id(self) -> int:
        return self.server_id.id

    async def StartServer(self, request: ServerOptions, context: grpc.ServicerContext) -> StartServerResponse:
        logging.info(f"StartServer")
        if self.server_id:
            logging.warning(f"Server already started, returning existing server")
            return StartServerResponse(self.server_id)
        else:
            self.server_id = ServerId(self.next_server_id)
            self.next_server_id += 1
            self.server = Server(self.device)
            self.server_name = request.name
            self.server_uuid = request.uuid
        return StartServerResponse(self.server_id)

    async def AcceptConnection(self, request: AcceptConnectionRequest,
                               context: grpc.ServicerContext) -> AcceptConnectionResponse:
        logging.info(f"AcceptConnection")
        assert self.server_id.id == request.server.id

        wait_dlc = asyncio.get_running_loop().create_future()
        channel = self.server.listen(wait_dlc.set_result)

        new_conn = RfcommConnection(self.next_conn_id)
        self.next_conn_id += 1
        self.connections[channel] = new_conn
        return AcceptConnectionResponse(new_conn)

    async def StopServer(self, request: StopServerRequest, context: grpc.ServicerContext) -> StopServerResponse:
        logging.info(f"StopServer")
        assert self.server_id.id == request.server.id
        # close the L2CAP server
        self.server.__exit__()
        self.server = None
        self.server_id = None
        self.server_name = None
        self.server_uuid = None

        return StopServerResponse()
