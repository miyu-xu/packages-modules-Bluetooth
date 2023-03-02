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

"""Generated python gRPC interfaces."""

__version__ = "0.0.1"

import enum
import grpc
import sys

from dataclasses import dataclass, field
from typing import Dict, Generator, Optional, List, Union, Iterator, AsyncGenerator, AsyncIterator, Awaitable, TypeVar

if sys.version_info >= (3, 8):
    from typing import Literal, TypedDict

from google.protobuf.message import Message


from pandora_experimental import rfcomm_pb2
from pandora_experimental.rfcomm_grpc import ConnectionRequest, RfcommConnection, ConnectionResponse, ServerOptions, ServerId, StartServerResponse, StopServerRequest, StopServerResponse, AcceptConnectionRequest, AcceptConnectionResponse, DisconnectionRequest, DisconnectionResponse, TxRequest, TxResponse, RxRequest, RxResponse

from pandora_experimental._utils import unwrap, AioSender as Sender, AioStream as Stream, AioStreamStream as StreamStream

class RFCOMM:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel):
        self.channel = channel

    def ConnectToServer(self, address: bytes = b'', uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[ConnectionResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/ConnectToServer',
            request_serializer=rfcomm_pb2.ConnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.ConnectionResponse.FromString  # type: ignore
        )(rfcomm_pb2.ConnectionRequest(address=address, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def StartServer(self, name: str = '', uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[StartServerResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/StartServer',
            request_serializer=rfcomm_pb2.ServerOptions.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.StartServerResponse.FromString  # type: ignore
        )(rfcomm_pb2.ServerOptions(name=name, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AcceptConnection(self, server: 'ServerId' = ServerId(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[AcceptConnectionResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/AcceptConnection',
            request_serializer=rfcomm_pb2.AcceptConnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.AcceptConnectionResponse.FromString  # type: ignore
        )(rfcomm_pb2.AcceptConnectionRequest(server=server), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Disconnect(self, connection: 'RfcommConnection' = RfcommConnection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[DisconnectionResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/Disconnect',
            request_serializer=rfcomm_pb2.DisconnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.DisconnectionResponse.FromString  # type: ignore
        )(rfcomm_pb2.DisconnectionRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def StopServer(self, server: 'ServerId' = ServerId(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[StopServerResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/StopServer',
            request_serializer=rfcomm_pb2.StopServerRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.StopServerResponse.FromString  # type: ignore
        )(rfcomm_pb2.StopServerRequest(server=server), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Send(self, connection: 'RfcommConnection' = RfcommConnection(), data: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[TxResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/Send',
            request_serializer=rfcomm_pb2.TxRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.TxResponse.FromString  # type: ignore
        )(rfcomm_pb2.TxRequest(connection=connection, data=data), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Receive(self, connection: 'RfcommConnection' = RfcommConnection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[RxResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/Receive',
            request_serializer=rfcomm_pb2.RxRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.RxResponse.FromString  # type: ignore
        )(rfcomm_pb2.RxRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class RFCOMMServicer:
    async def ConnectToServer(self, request: ConnectionRequest, context: grpc.ServicerContext) -> ConnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def StartServer(self, request: ServerOptions, context: grpc.ServicerContext) -> StartServerResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def AcceptConnection(self, request: AcceptConnectionRequest, context: grpc.ServicerContext) -> AcceptConnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Disconnect(self, request: DisconnectionRequest, context: grpc.ServicerContext) -> DisconnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def StopServer(self, request: StopServerRequest, context: grpc.ServicerContext) -> StopServerResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Send(self, request: TxRequest, context: grpc.ServicerContext) -> TxResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Receive(self, request: RxRequest, context: grpc.ServicerContext) -> RxResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_RFCOMMServicer_to_server(servicer: RFCOMMServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'ConnectToServer': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ConnectToServer,
            request_deserializer=rfcomm_pb2.ConnectionRequest.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.ConnectionResponse.SerializeToString,  # type: ignore
        ),
        'StartServer': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.StartServer,
            request_deserializer=rfcomm_pb2.ServerOptions.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.StartServerResponse.SerializeToString,  # type: ignore
        ),
        'AcceptConnection': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.AcceptConnection,
            request_deserializer=rfcomm_pb2.AcceptConnectionRequest.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.AcceptConnectionResponse.SerializeToString,  # type: ignore
        ),
        'Disconnect': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Disconnect,
            request_deserializer=rfcomm_pb2.DisconnectionRequest.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.DisconnectionResponse.SerializeToString,  # type: ignore
        ),
        'StopServer': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.StopServer,
            request_deserializer=rfcomm_pb2.StopServerRequest.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.StopServerResponse.SerializeToString,  # type: ignore
        ),
        'Send': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Send,
            request_deserializer=rfcomm_pb2.TxRequest.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.TxResponse.SerializeToString,  # type: ignore
        ),
        'Receive': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Receive,
            request_deserializer=rfcomm_pb2.RxRequest.FromString,  # type: ignore
            response_serializer=rfcomm_pb2.RxResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.RFCOMM', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
