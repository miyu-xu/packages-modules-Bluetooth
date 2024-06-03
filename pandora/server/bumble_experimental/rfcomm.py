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

from bumble import core
from bumble.device import Device
from bumble.rfcomm import (
    Server,
    make_service_sdp_records,
    DLC,
)
from bumble.pandora import utils
import grpc
from pandora_experimental.rfcomm_grpc_aio import RFCOMMServicer
from pandora_experimental.rfcomm_pb2 import (
    AcceptConnectionRequest,
    AcceptConnectionResponse,
    ConnectionRequest,
    ConnectionResponse,
    RfcommConnection,
    RxRequest,
    RxResponse,
    ServerId,
    StartServerRequest,
    StartServerResponse,
    StopServerRequest,
    StopServerResponse,
    TxRequest,
    TxResponse,
)


class RFCOMMService(RFCOMMServicer):
    device: Device

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device
        self.servers = {}  # key = id, value = ServerInstance
        self.next_server_id = 1
        self.connections = {}  # key = id, value = dlc
        self.next_conn_id = 1

    class Connection:

        def __init__(self, dlc):
            self.dlc = dlc
            self.data_queue = asyncio.Queue()

    class ServerInstance:

        def __init__(self, name, uuid, server):
            self.name = name
            self.uuid = uuid
            self.server = server
            self.wait_dlc = None
            self.open_channel = None

    @utils.rpc
    async def StartServer(self, request: StartServerRequest, context: grpc.ServicerContext) -> StartServerResponse:
        uuid = core.UUID(request.uuid)
        logging.info(f"StartServer {uuid}")

        for existing_id, server in self.servers:
            if server.uuid == uuid:
                logging.warning(f"Server already started for {uuid}, returning existing server")
                return StartServerResponse(server=ServerId(id=existing_id))

        id = self.next_server_id
        self.next_server_id += 1
        self.servers[id] = self.ServerInstance(name=request.name, uuid=uuid, server=Server(self.device))
        self.servers[id].wait_dlc = asyncio.get_running_loop().create_future()
        #TODO Add support for multiple clients
        self.servers[id].open_channel = self.servers[id].server.listen(acceptor=self.servers[id].wait_dlc.set_result,
                                                                       channel=2)
        handle = 1
        records = make_service_sdp_records(handle, self.servers[id].open_channel, uuid)
        self.device.sdp_service_records[handle] = records
        return StartServerResponse(server=ServerId(id=id))

    @utils.rpc
    async def AcceptConnection(self, request: AcceptConnectionRequest,
                               context: grpc.ServicerContext) -> AcceptConnectionResponse:
        logging.info(f"AcceptConnection")
        assert self.servers[request.server.id] is not None
        dlc = await self.servers[request.server.id].wait_dlc
        id = self.next_conn_id
        self.next_conn_id += 1
        self.connections[id] = self.Connection(dlc=dlc)
        self.connections[id].dlc.sink = self.connections[id].data_queue.put_nowait
        return AcceptConnectionResponse(connection=RfcommConnection(id=id))

    @utils.rpc
    async def StopServer(self, request: StopServerRequest, context: grpc.ServicerContext) -> StopServerResponse:
        logging.info(f"StopServer")
        assert self.servers[request.server.id] is not None
        self.servers[request.server.id] = None

        return StopServerResponse()

    @utils.rpc
    async def Send(self, request: TxRequest, context: grpc.ServicerContext) -> TxResponse:
        logging.info(f"Send")
        assert self.connections[request.connection.id] is not None
        self.connections[request.connection.id].dlc.write(request.data)
        return TxResponse()

    @utils.rpc
    async def Receive(self, request: RxRequest, context: grpc.ServicerContext) -> RxResponse:
        logging.info(f"Receive")
        assert self.connections[request.connection.id] is not None
        received_data = await self.connections[request.connection.id].data_queue.get()
        return RxResponse(data=received_data)
