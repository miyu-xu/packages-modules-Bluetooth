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

from pandora_experimental._utils import unwrap, Sender, Stream, StreamStream



class AttStatusCode(enum.IntEnum):
    SUCCESS = 0
    UNKNOWN_ERROR = 257
    INVALID_HANDLE = 1
    READ_NOT_PERMITTED = 2
    WRITE_NOT_PERMITTED = 3
    INSUFFICIENT_AUTHENTICATION = 5
    INVALID_OFFSET = 7
    ATTRIBUTE_NOT_FOUND = 10
    INVALID_ATTRIBUTE_LENGTH = 13
    APPLICATION_ERROR = 128

class AttProperties(enum.IntEnum):
    PROPERTY_NONE = 0
    PROPERTY_READ = 2
    PROPERTY_WRITE = 8

class AttPermissions(enum.IntEnum):
    PERMISSION_NONE = 0
    PERMISSION_READ = 1
    PERMISSION_WRITE = 16
    PERMISSION_READ_ENCRYPTED = 2


@dataclass
class GattService(Message):
    handle: int = 0
    type: int = 0
    uuid: str = ''
    included_services: List['GattService'] = field(default_factory=list)
    characteristics: List['GattCharacteristic'] = field(default_factory=list)

setattr(GattService, '__new__', lambda _, *args, **kwargs: gatt_pb2.GattService(*args, **kwargs))  # type: ignore

@dataclass
class GattCharacteristic(Message):
    properties: int = 0
    permissions: int = 0
    uuid: str = ''
    handle: int = 0
    descriptors: List['GattCharacteristicDescriptor'] = field(default_factory=list)

setattr(GattCharacteristic, '__new__', lambda _, *args, **kwargs: gatt_pb2.GattCharacteristic(*args, **kwargs))  # type: ignore

@dataclass
class GattCharacteristicDescriptor(Message):
    handle: int = 0
    permissions: int = 0
    uuid: str = ''

setattr(GattCharacteristicDescriptor, '__new__', lambda _, *args, **kwargs: gatt_pb2.GattCharacteristicDescriptor(*args, **kwargs))  # type: ignore

@dataclass
class AttValue(Message):
    handle: int = 0
    value: bytes = b''

setattr(AttValue, '__new__', lambda _, *args, **kwargs: gatt_pb2.AttValue(*args, **kwargs))  # type: ignore

@dataclass
class ExchangeMTURequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    mtu: int = 0

setattr(ExchangeMTURequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.ExchangeMTURequest(*args, **kwargs))  # type: ignore

@dataclass
class ExchangeMTUResponse(Message):
    pass

setattr(ExchangeMTUResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.ExchangeMTUResponse(*args, **kwargs))  # type: ignore

@dataclass
class WriteRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    handle: int = 0
    value: bytes = b''

setattr(WriteRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.WriteRequest(*args, **kwargs))  # type: ignore

@dataclass
class WriteResponse(Message):
    handle: int = 0
    status: AttStatusCode = AttStatusCode.SUCCESS

setattr(WriteResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.WriteResponse(*args, **kwargs))  # type: ignore

@dataclass
class DiscoverServiceByUuidRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    uuid: str = ''

setattr(DiscoverServiceByUuidRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.DiscoverServiceByUuidRequest(*args, **kwargs))  # type: ignore

@dataclass
class DiscoverServicesRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(DiscoverServicesRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.DiscoverServicesRequest(*args, **kwargs))  # type: ignore

@dataclass
class DiscoverServicesResponse(Message):
    services: List['GattService'] = field(default_factory=list)

setattr(DiscoverServicesResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.DiscoverServicesResponse(*args, **kwargs))  # type: ignore

@dataclass
class DiscoverServicesSdpRequest(Message):
    address: bytes = b''

setattr(DiscoverServicesSdpRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.DiscoverServicesSdpRequest(*args, **kwargs))  # type: ignore

@dataclass
class DiscoverServicesSdpResponse(Message):
    service_uuids: List[str] = field(default_factory=list)

setattr(DiscoverServicesSdpResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.DiscoverServicesSdpResponse(*args, **kwargs))  # type: ignore

@dataclass
class ClearCacheRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(ClearCacheRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.ClearCacheRequest(*args, **kwargs))  # type: ignore

@dataclass
class ClearCacheResponse(Message):
    pass

setattr(ClearCacheResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.ClearCacheResponse(*args, **kwargs))  # type: ignore

@dataclass
class ReadCharacteristicRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    handle: int = 0

setattr(ReadCharacteristicRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.ReadCharacteristicRequest(*args, **kwargs))  # type: ignore

@dataclass
class ReadCharacteristicsFromUuidRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    uuid: str = ''
    start_handle: int = 0
    end_handle: int = 0

setattr(ReadCharacteristicsFromUuidRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.ReadCharacteristicsFromUuidRequest(*args, **kwargs))  # type: ignore

@dataclass
class ReadCharacteristicResponse(Message):
    value: 'AttValue' = AttValue()
    status: AttStatusCode = AttStatusCode.SUCCESS

setattr(ReadCharacteristicResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.ReadCharacteristicResponse(*args, **kwargs))  # type: ignore

@dataclass
class ReadCharacteristicsFromUuidResponse(Message):
    characteristics_read: List['ReadCharacteristicResponse'] = field(default_factory=list)

setattr(ReadCharacteristicsFromUuidResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.ReadCharacteristicsFromUuidResponse(*args, **kwargs))  # type: ignore

@dataclass
class ReadCharacteristicDescriptorRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    handle: int = 0

setattr(ReadCharacteristicDescriptorRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.ReadCharacteristicDescriptorRequest(*args, **kwargs))  # type: ignore

@dataclass
class ReadCharacteristicDescriptorResponse(Message):
    value: 'AttValue' = AttValue()
    status: AttStatusCode = AttStatusCode.SUCCESS

setattr(ReadCharacteristicDescriptorResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.ReadCharacteristicDescriptorResponse(*args, **kwargs))  # type: ignore

@dataclass
class GattServiceParams(Message):
    uuid: str = ''
    characteristics: List['GattCharacteristicParams'] = field(default_factory=list)

setattr(GattServiceParams, '__new__', lambda _, *args, **kwargs: gatt_pb2.GattServiceParams(*args, **kwargs))  # type: ignore

@dataclass
class GattCharacteristicParams(Message):
    properties: int = 0
    permissions: int = 0
    uuid: str = ''

setattr(GattCharacteristicParams, '__new__', lambda _, *args, **kwargs: gatt_pb2.GattCharacteristicParams(*args, **kwargs))  # type: ignore

@dataclass
class RegisterServiceRequest(Message):
    service: 'GattServiceParams' = GattServiceParams()

setattr(RegisterServiceRequest, '__new__', lambda _, *args, **kwargs: gatt_pb2.RegisterServiceRequest(*args, **kwargs))  # type: ignore

@dataclass
class RegisterServiceResponse(Message):
    service: 'GattService' = GattService()

setattr(RegisterServiceResponse, '__new__', lambda _, *args, **kwargs: gatt_pb2.RegisterServiceResponse(*args, **kwargs))  # type: ignore


class GATT:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel):
        self.channel = channel

    def ExchangeMTU(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), mtu: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ExchangeMTUResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ExchangeMTU',
            request_serializer=gatt_pb2.ExchangeMTURequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ExchangeMTUResponse.FromString  # type: ignore
        )(gatt_pb2.ExchangeMTURequest(connection=connection, mtu=mtu), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WriteAttFromHandle(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), handle: int = 0, value: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> WriteResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/WriteAttFromHandle',
            request_serializer=gatt_pb2.WriteRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.WriteResponse.FromString  # type: ignore
        )(gatt_pb2.WriteRequest(connection=connection, handle=handle, value=value), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServiceByUuid(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> DiscoverServicesResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServiceByUuid',
            request_serializer=gatt_pb2.DiscoverServiceByUuidRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServiceByUuidRequest(connection=connection, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServices(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> DiscoverServicesResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServices',
            request_serializer=gatt_pb2.DiscoverServicesRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServicesRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServicesSdp(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> DiscoverServicesSdpResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServicesSdp',
            request_serializer=gatt_pb2.DiscoverServicesSdpRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesSdpResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServicesSdpRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ClearCache(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ClearCacheResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ClearCache',
            request_serializer=gatt_pb2.ClearCacheRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ClearCacheResponse.FromString  # type: ignore
        )(gatt_pb2.ClearCacheRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicFromHandle(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ReadCharacteristicResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicFromHandle',
            request_serializer=gatt_pb2.ReadCharacteristicRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicsFromUuid(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), uuid: str = '', start_handle: int = 0, end_handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ReadCharacteristicsFromUuidResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicsFromUuid',
            request_serializer=gatt_pb2.ReadCharacteristicsFromUuidRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicsFromUuidResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicsFromUuidRequest(connection=connection, uuid=uuid, start_handle=start_handle, end_handle=end_handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicDescriptorFromHandle(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ReadCharacteristicDescriptorResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicDescriptorFromHandle',
            request_serializer=gatt_pb2.ReadCharacteristicDescriptorRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicDescriptorResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicDescriptorRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def RegisterService(self, service: 'GattServiceParams' = GattServiceParams(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> RegisterServiceResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/RegisterService',
            request_serializer=gatt_pb2.RegisterServiceRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.RegisterServiceResponse.FromString  # type: ignore
        )(gatt_pb2.RegisterServiceRequest(service=service), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class GATTServicer:
    def ExchangeMTU(self, request: ExchangeMTURequest, context: grpc.ServicerContext) -> ExchangeMTUResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WriteAttFromHandle(self, request: WriteRequest, context: grpc.ServicerContext) -> WriteResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DiscoverServiceByUuid(self, request: DiscoverServiceByUuidRequest, context: grpc.ServicerContext) -> DiscoverServicesResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DiscoverServices(self, request: DiscoverServicesRequest, context: grpc.ServicerContext) -> DiscoverServicesResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DiscoverServicesSdp(self, request: DiscoverServicesSdpRequest, context: grpc.ServicerContext) -> DiscoverServicesSdpResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ClearCache(self, request: ClearCacheRequest, context: grpc.ServicerContext) -> ClearCacheResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadCharacteristicFromHandle(self, request: ReadCharacteristicRequest, context: grpc.ServicerContext) -> ReadCharacteristicResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadCharacteristicsFromUuid(self, request: ReadCharacteristicsFromUuidRequest, context: grpc.ServicerContext) -> ReadCharacteristicsFromUuidResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadCharacteristicDescriptorFromHandle(self, request: ReadCharacteristicDescriptorRequest, context: grpc.ServicerContext) -> ReadCharacteristicDescriptorResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def RegisterService(self, request: RegisterServiceRequest, context: grpc.ServicerContext) -> RegisterServiceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_GATTServicer_to_server(servicer: GATTServicer, server: grpc.Server) -> None:
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


