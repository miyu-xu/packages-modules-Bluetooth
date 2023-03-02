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


from pandora import host_pb2
from pandora_experimental import gatt_pb2
from pandora_experimental.gatt_grpc import AttStatusCode, AttProperties, AttPermissions
from pandora_experimental.gatt_grpc import GattService, GattCharacteristic, GattCharacteristicDescriptor, AttValue, ExchangeMTURequest, ExchangeMTUResponse, WriteRequest, WriteResponse, DiscoverServiceByUuidRequest, DiscoverServicesRequest, DiscoverServicesResponse, DiscoverServicesSdpRequest, DiscoverServicesSdpResponse, ClearCacheRequest, ClearCacheResponse, ReadCharacteristicRequest, ReadCharacteristicsFromUuidRequest, ReadCharacteristicResponse, ReadCharacteristicsFromUuidResponse, ReadCharacteristicDescriptorRequest, ReadCharacteristicDescriptorResponse, GattServiceParams, GattCharacteristicParams, RegisterServiceRequest, RegisterServiceResponse

from pandora_experimental._utils import unwrap, AioSender as Sender, AioStream as Stream, AioStreamStream as StreamStream

class GATT:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel):
        self.channel = channel

    def ExchangeMTU(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), mtu: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[ExchangeMTUResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ExchangeMTU',
            request_serializer=gatt_pb2.ExchangeMTURequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ExchangeMTUResponse.FromString  # type: ignore
        )(gatt_pb2.ExchangeMTURequest(connection=connection, mtu=mtu), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WriteAttFromHandle(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), handle: int = 0, value: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[WriteResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/WriteAttFromHandle',
            request_serializer=gatt_pb2.WriteRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.WriteResponse.FromString  # type: ignore
        )(gatt_pb2.WriteRequest(connection=connection, handle=handle, value=value), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServiceByUuid(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[DiscoverServicesResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServiceByUuid',
            request_serializer=gatt_pb2.DiscoverServiceByUuidRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServiceByUuidRequest(connection=connection, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServices(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[DiscoverServicesResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServices',
            request_serializer=gatt_pb2.DiscoverServicesRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServicesRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServicesSdp(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[DiscoverServicesSdpResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServicesSdp',
            request_serializer=gatt_pb2.DiscoverServicesSdpRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesSdpResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServicesSdpRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ClearCache(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[ClearCacheResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ClearCache',
            request_serializer=gatt_pb2.ClearCacheRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ClearCacheResponse.FromString  # type: ignore
        )(gatt_pb2.ClearCacheRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicFromHandle(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[ReadCharacteristicResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicFromHandle',
            request_serializer=gatt_pb2.ReadCharacteristicRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicsFromUuid(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), uuid: str = '', start_handle: int = 0, end_handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[ReadCharacteristicsFromUuidResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicsFromUuid',
            request_serializer=gatt_pb2.ReadCharacteristicsFromUuidRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicsFromUuidResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicsFromUuidRequest(connection=connection, uuid=uuid, start_handle=start_handle, end_handle=end_handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicDescriptorFromHandle(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[ReadCharacteristicDescriptorResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicDescriptorFromHandle',
            request_serializer=gatt_pb2.ReadCharacteristicDescriptorRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicDescriptorResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicDescriptorRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def RegisterService(self, service: 'GattServiceParams' = GattServiceParams(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[RegisterServiceResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/RegisterService',
            request_serializer=gatt_pb2.RegisterServiceRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.RegisterServiceResponse.FromString  # type: ignore
        )(gatt_pb2.RegisterServiceRequest(service=service), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class GATTServicer:
    async def ExchangeMTU(self, request: ExchangeMTURequest, context: grpc.ServicerContext) -> ExchangeMTUResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def WriteAttFromHandle(self, request: WriteRequest, context: grpc.ServicerContext) -> WriteResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def DiscoverServiceByUuid(self, request: DiscoverServiceByUuidRequest, context: grpc.ServicerContext) -> DiscoverServicesResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def DiscoverServices(self, request: DiscoverServicesRequest, context: grpc.ServicerContext) -> DiscoverServicesResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def DiscoverServicesSdp(self, request: DiscoverServicesSdpRequest, context: grpc.ServicerContext) -> DiscoverServicesSdpResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def ClearCache(self, request: ClearCacheRequest, context: grpc.ServicerContext) -> ClearCacheResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def ReadCharacteristicFromHandle(self, request: ReadCharacteristicRequest, context: grpc.ServicerContext) -> ReadCharacteristicResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def ReadCharacteristicsFromUuid(self, request: ReadCharacteristicsFromUuidRequest, context: grpc.ServicerContext) -> ReadCharacteristicsFromUuidResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def ReadCharacteristicDescriptorFromHandle(self, request: ReadCharacteristicDescriptorRequest, context: grpc.ServicerContext) -> ReadCharacteristicDescriptorResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def RegisterService(self, request: RegisterServiceRequest, context: grpc.ServicerContext) -> RegisterServiceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_GATTServicer_to_server(servicer: GATTServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'ExchangeMTU': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ExchangeMTU,
            request_deserializer=gatt_pb2.ExchangeMTURequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.ExchangeMTUResponse.SerializeToString,  # type: ignore
        ),
        'WriteAttFromHandle': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WriteAttFromHandle,
            request_deserializer=gatt_pb2.WriteRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.WriteResponse.SerializeToString,  # type: ignore
        ),
        'DiscoverServiceByUuid': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DiscoverServiceByUuid,
            request_deserializer=gatt_pb2.DiscoverServiceByUuidRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.DiscoverServicesResponse.SerializeToString,  # type: ignore
        ),
        'DiscoverServices': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DiscoverServices,
            request_deserializer=gatt_pb2.DiscoverServicesRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.DiscoverServicesResponse.SerializeToString,  # type: ignore
        ),
        'DiscoverServicesSdp': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DiscoverServicesSdp,
            request_deserializer=gatt_pb2.DiscoverServicesSdpRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.DiscoverServicesSdpResponse.SerializeToString,  # type: ignore
        ),
        'ClearCache': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ClearCache,
            request_deserializer=gatt_pb2.ClearCacheRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.ClearCacheResponse.SerializeToString,  # type: ignore
        ),
        'ReadCharacteristicFromHandle': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ReadCharacteristicFromHandle,
            request_deserializer=gatt_pb2.ReadCharacteristicRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.ReadCharacteristicResponse.SerializeToString,  # type: ignore
        ),
        'ReadCharacteristicsFromUuid': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ReadCharacteristicsFromUuid,
            request_deserializer=gatt_pb2.ReadCharacteristicsFromUuidRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.ReadCharacteristicsFromUuidResponse.SerializeToString,  # type: ignore
        ),
        'ReadCharacteristicDescriptorFromHandle': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ReadCharacteristicDescriptorFromHandle,
            request_deserializer=gatt_pb2.ReadCharacteristicDescriptorRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.ReadCharacteristicDescriptorResponse.SerializeToString,  # type: ignore
        ),
        'RegisterService': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.RegisterService,
            request_deserializer=gatt_pb2.RegisterServiceRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.RegisterServiceResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.GATT', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
