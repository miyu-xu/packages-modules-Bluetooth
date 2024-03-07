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
import queue

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
        self.open_channel = None
        self.wait_dlc = None
        self.dlc = None
        self.data_queue = queue.Queue()

    @utils.rpc
    async def ConnectionRequest(self, request: ConnectionRequest, context: grpc.ServicerContext) -> ConnectionResponse:
        logging.info(f"ConnectionRequest")
        # ConnectionRequest contains an address and a uuid
        # ConnectionResponse contains an RfcommConnection connection

    @utils.rpc
    async def StartServer(self, request: StartServerRequest, context: grpc.ServicerContext) -> StartServerResponse:
        logging.info(f"StartServer")
        if self.server_id:
            logging.warning(f"Server already started, returning existing server")
            return StartServerResponse(server=self.server_id)
        else:
            self.server_id = ServerId(id=self.next_server_id)
            self.next_server_id += 1
            self.server = Server(self.device)
            self.server_name = request.name
            self.server_uuid = core.UUID(request.uuid)
        handle = 0
        self.wait_dlc = asyncio.get_running_loop().create_future()
        self.open_channel = self.server.listen(acceptor=self.wait_dlc.set_result)
        records = make_service_sdp_records(handle, self.open_channel, self.server_uuid)
        self.device.sdp_service_records[handle] = records

        return StartServerResponse(server=self.server_id)

    @utils.rpc
    async def AcceptConnection(self, request: AcceptConnectionRequest,
                               context: grpc.ServicerContext) -> AcceptConnectionResponse:
        logging.info(f"AcceptConnection")
        assert self.server_id.id == request.server.id
        dlc = await asyncio.wait_for(self.wait_dlc, timeout=10)
        dlc.sink = self.incoming_data
        new_conn = RfcommConnection(id=self.next_conn_id)
        self.next_conn_id += 1
        self.connections[new_conn.id] = dlc
        return AcceptConnectionResponse(connection=new_conn)

    @utils.rpc
    async def StopServer(self, request: StopServerRequest, context: grpc.ServicerContext) -> StopServerResponse:
        logging.info(f"StopServer")
        assert self.server_id.id == request.server.id
        self.server = None
        self.server_id = None
        self.server_name = None
        self.server_uuid = None

        return StopServerResponse()

    @utils.rpc
    async def Send(self, request:TxRequest, context: grpc.ServicerContext) -> TxResponse:
        logging.info(f"Send")
        dlc = self.connections[request.connection.id]
        if dlc is not None:
            dlc.write(request.data)

    @utils.rpc
    async def Receive(self, request:RxRequest, context: grpc.ServicerContext) -> RxResponse:
        logging.info(f"Receive")
        received_data = self.data_queue.get(block=True, timeout=10)
        return RxResponse(data=received_data)

    async def incoming_data(self, data: bytes) -> None:
        logging.debug(f"Data recieved: {data.decode('utf-8')}")
        self.data_queue.put(data)
