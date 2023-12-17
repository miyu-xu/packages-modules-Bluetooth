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



from google.protobuf import empty_pb2
from pandora import os_pb2
from pandora.os_pb2 import ACCESS_MESSAGE
from typing import Awaitable
from typing import Optional
import grpc
import grpc.aio

class Os:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel) -> None:
        self.channel = channel

    def Log(self, text: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[os_pb2.LogResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Os/Log',
            request_serializer=os_pb2.LogRequest.SerializeToString,  # type: ignore
            response_deserializer=os_pb2.LogResponse.FromString  # type: ignore
        )(os_pb2.LogRequest(text=text), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetAccessPermission(self, address: bytes = b'', access_type: os_pb2.AccessType = ACCESS_MESSAGE, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Os/SetAccessPermission',
            request_serializer=os_pb2.SetAccessPermissionRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(os_pb2.SetAccessPermissionRequest(address=address, access_type=access_type), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SendPing(self, ip_address: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Os/SendPing',
            request_serializer=os_pb2.SendPingRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(os_pb2.SendPingRequest(ip_address=ip_address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class OsServicer:
    async def Log(self, request: os_pb2.LogRequest, context: grpc.ServicerContext) -> os_pb2.LogResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def SetAccessPermission(self, request: os_pb2.SetAccessPermissionRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def SendPing(self, request: os_pb2.SendPingRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_OsServicer_to_server(servicer: OsServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'Log': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Log,
            request_deserializer=os_pb2.LogRequest.FromString,  # type: ignore
            response_serializer=os_pb2.LogResponse.SerializeToString,  # type: ignore
        ),
        'SetAccessPermission': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetAccessPermission,
            request_deserializer=os_pb2.SetAccessPermissionRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SendPing': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendPing,
            request_deserializer=os_pb2.SendPingRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.Os', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
