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



from pandora import host_pb2
from pandora import l2cap_pb2
from typing import Awaitable
from typing import Optional
import grpc
import grpc.aio

class L2CAP:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel) -> None:
        self.channel = channel

    def CreateLECreditBasedChannel(self, connection: host_pb2.Connection = host_pb2.Connection(), psm: int = 0, secure: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[l2cap_pb2.CreateLECreditBasedChannelResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.L2CAP/CreateLECreditBasedChannel',
            request_serializer=l2cap_pb2.CreateLECreditBasedChannelRequest.SerializeToString,  # type: ignore
            response_deserializer=l2cap_pb2.CreateLECreditBasedChannelResponse.FromString  # type: ignore
        )(l2cap_pb2.CreateLECreditBasedChannelRequest(connection=connection, psm=psm, secure=secure), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SendData(self, connection: host_pb2.Connection = host_pb2.Connection(), data: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[l2cap_pb2.SendDataResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.L2CAP/SendData',
            request_serializer=l2cap_pb2.SendDataRequest.SerializeToString,  # type: ignore
            response_deserializer=l2cap_pb2.SendDataResponse.FromString  # type: ignore
        )(l2cap_pb2.SendDataRequest(connection=connection, data=data), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReceiveData(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[l2cap_pb2.ReceiveDataResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.L2CAP/ReceiveData',
            request_serializer=l2cap_pb2.ReceiveDataRequest.SerializeToString,  # type: ignore
            response_deserializer=l2cap_pb2.ReceiveDataResponse.FromString  # type: ignore
        )(l2cap_pb2.ReceiveDataRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ListenL2CAPChannel(self, connection: host_pb2.Connection = host_pb2.Connection(), secure: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[l2cap_pb2.ListenL2CAPChannelResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.L2CAP/ListenL2CAPChannel',
            request_serializer=l2cap_pb2.ListenL2CAPChannelRequest.SerializeToString,  # type: ignore
            response_deserializer=l2cap_pb2.ListenL2CAPChannelResponse.FromString  # type: ignore
        )(l2cap_pb2.ListenL2CAPChannelRequest(connection=connection, secure=secure), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AcceptL2CAPChannel(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[l2cap_pb2.AcceptL2CAPChannelResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.L2CAP/AcceptL2CAPChannel',
            request_serializer=l2cap_pb2.AcceptL2CAPChannelRequest.SerializeToString,  # type: ignore
            response_deserializer=l2cap_pb2.AcceptL2CAPChannelResponse.FromString  # type: ignore
        )(l2cap_pb2.AcceptL2CAPChannelRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class L2CAPServicer:
    async def CreateLECreditBasedChannel(self, request: l2cap_pb2.CreateLECreditBasedChannelRequest, context: grpc.ServicerContext) -> l2cap_pb2.CreateLECreditBasedChannelResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def SendData(self, request: l2cap_pb2.SendDataRequest, context: grpc.ServicerContext) -> l2cap_pb2.SendDataResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def ReceiveData(self, request: l2cap_pb2.ReceiveDataRequest, context: grpc.ServicerContext) -> l2cap_pb2.ReceiveDataResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def ListenL2CAPChannel(self, request: l2cap_pb2.ListenL2CAPChannelRequest, context: grpc.ServicerContext) -> l2cap_pb2.ListenL2CAPChannelResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def AcceptL2CAPChannel(self, request: l2cap_pb2.AcceptL2CAPChannelRequest, context: grpc.ServicerContext) -> l2cap_pb2.AcceptL2CAPChannelResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_L2CAPServicer_to_server(servicer: L2CAPServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'CreateLECreditBasedChannel': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.CreateLECreditBasedChannel,
            request_deserializer=l2cap_pb2.CreateLECreditBasedChannelRequest.FromString,  # type: ignore
            response_serializer=l2cap_pb2.CreateLECreditBasedChannelResponse.SerializeToString,  # type: ignore
        ),
        'SendData': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendData,
            request_deserializer=l2cap_pb2.SendDataRequest.FromString,  # type: ignore
            response_serializer=l2cap_pb2.SendDataResponse.SerializeToString,  # type: ignore
        ),
        'ReceiveData': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ReceiveData,
            request_deserializer=l2cap_pb2.ReceiveDataRequest.FromString,  # type: ignore
            response_serializer=l2cap_pb2.ReceiveDataResponse.SerializeToString,  # type: ignore
        ),
        'ListenL2CAPChannel': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ListenL2CAPChannel,
            request_deserializer=l2cap_pb2.ListenL2CAPChannelRequest.FromString,  # type: ignore
            response_serializer=l2cap_pb2.ListenL2CAPChannelResponse.SerializeToString,  # type: ignore
        ),
        'AcceptL2CAPChannel': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.AcceptL2CAPChannel,
            request_deserializer=l2cap_pb2.AcceptL2CAPChannelRequest.FromString,  # type: ignore
            response_serializer=l2cap_pb2.AcceptL2CAPChannelResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.L2CAP', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
