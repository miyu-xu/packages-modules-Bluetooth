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



from pandora import gatt_pb2
from pandora import host_pb2
from pandora.gatt_pb2 import ENABLE_NOTIFICATION_VALUE
from typing import Optional
import grpc

class GATT:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def ExchangeMTU(self, connection: host_pb2.Connection = host_pb2.Connection(), mtu: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.ExchangeMTUResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ExchangeMTU',
            request_serializer=gatt_pb2.ExchangeMTURequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ExchangeMTUResponse.FromString  # type: ignore
        )(gatt_pb2.ExchangeMTURequest(connection=connection, mtu=mtu), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WriteAttFromHandle(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, value: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.WriteResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/WriteAttFromHandle',
            request_serializer=gatt_pb2.WriteRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.WriteResponse.FromString  # type: ignore
        )(gatt_pb2.WriteRequest(connection=connection, handle=handle, value=value), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServiceByUuid(self, connection: host_pb2.Connection = host_pb2.Connection(), uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.DiscoverServicesResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServiceByUuid',
            request_serializer=gatt_pb2.DiscoverServiceByUuidRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServiceByUuidRequest(connection=connection, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServices(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.DiscoverServicesResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServices',
            request_serializer=gatt_pb2.DiscoverServicesRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServicesRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DiscoverServicesSdp(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.DiscoverServicesSdpResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/DiscoverServicesSdp',
            request_serializer=gatt_pb2.DiscoverServicesSdpRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.DiscoverServicesSdpResponse.FromString  # type: ignore
        )(gatt_pb2.DiscoverServicesSdpRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ClearCache(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.ClearCacheResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ClearCache',
            request_serializer=gatt_pb2.ClearCacheRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ClearCacheResponse.FromString  # type: ignore
        )(gatt_pb2.ClearCacheRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicFromHandle(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.ReadCharacteristicResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicFromHandle',
            request_serializer=gatt_pb2.ReadCharacteristicRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicsFromUuid(self, connection: host_pb2.Connection = host_pb2.Connection(), uuid: str = '', start_handle: int = 0, end_handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.ReadCharacteristicsFromUuidResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicsFromUuid',
            request_serializer=gatt_pb2.ReadCharacteristicsFromUuidRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicsFromUuidResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicsFromUuidRequest(connection=connection, uuid=uuid, start_handle=start_handle, end_handle=end_handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadCharacteristicDescriptorFromHandle(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.ReadCharacteristicDescriptorResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/ReadCharacteristicDescriptorFromHandle',
            request_serializer=gatt_pb2.ReadCharacteristicDescriptorRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.ReadCharacteristicDescriptorResponse.FromString  # type: ignore
        )(gatt_pb2.ReadCharacteristicDescriptorRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def RegisterService(self, service: gatt_pb2.GattServiceParams = gatt_pb2.GattServiceParams(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.RegisterServiceResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/RegisterService',
            request_serializer=gatt_pb2.RegisterServiceRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.RegisterServiceResponse.FromString  # type: ignore
        )(gatt_pb2.RegisterServiceRequest(service=service), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetCharacteristicNotificationFromHandle(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, enable_value: gatt_pb2.EnableValue = ENABLE_NOTIFICATION_VALUE, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.SetCharacteristicNotificationFromHandleResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/SetCharacteristicNotificationFromHandle',
            request_serializer=gatt_pb2.SetCharacteristicNotificationFromHandleRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.SetCharacteristicNotificationFromHandleResponse.FromString  # type: ignore
        )(gatt_pb2.SetCharacteristicNotificationFromHandleRequest(connection=connection, handle=handle, enable_value=enable_value), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitCharacteristicNotification(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> gatt_pb2.WaitCharacteristicNotificationResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.GATT/WaitCharacteristicNotification',
            request_serializer=gatt_pb2.WaitCharacteristicNotificationRequest.SerializeToString,  # type: ignore
            response_deserializer=gatt_pb2.WaitCharacteristicNotificationResponse.FromString  # type: ignore
        )(gatt_pb2.WaitCharacteristicNotificationRequest(connection=connection, handle=handle), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class GATTServicer:
    def ExchangeMTU(self, request: gatt_pb2.ExchangeMTURequest, context: grpc.ServicerContext) -> gatt_pb2.ExchangeMTUResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WriteAttFromHandle(self, request: gatt_pb2.WriteRequest, context: grpc.ServicerContext) -> gatt_pb2.WriteResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DiscoverServiceByUuid(self, request: gatt_pb2.DiscoverServiceByUuidRequest, context: grpc.ServicerContext) -> gatt_pb2.DiscoverServicesResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DiscoverServices(self, request: gatt_pb2.DiscoverServicesRequest, context: grpc.ServicerContext) -> gatt_pb2.DiscoverServicesResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DiscoverServicesSdp(self, request: gatt_pb2.DiscoverServicesSdpRequest, context: grpc.ServicerContext) -> gatt_pb2.DiscoverServicesSdpResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ClearCache(self, request: gatt_pb2.ClearCacheRequest, context: grpc.ServicerContext) -> gatt_pb2.ClearCacheResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadCharacteristicFromHandle(self, request: gatt_pb2.ReadCharacteristicRequest, context: grpc.ServicerContext) -> gatt_pb2.ReadCharacteristicResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadCharacteristicsFromUuid(self, request: gatt_pb2.ReadCharacteristicsFromUuidRequest, context: grpc.ServicerContext) -> gatt_pb2.ReadCharacteristicsFromUuidResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadCharacteristicDescriptorFromHandle(self, request: gatt_pb2.ReadCharacteristicDescriptorRequest, context: grpc.ServicerContext) -> gatt_pb2.ReadCharacteristicDescriptorResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def RegisterService(self, request: gatt_pb2.RegisterServiceRequest, context: grpc.ServicerContext) -> gatt_pb2.RegisterServiceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetCharacteristicNotificationFromHandle(self, request: gatt_pb2.SetCharacteristicNotificationFromHandleRequest, context: grpc.ServicerContext) -> gatt_pb2.SetCharacteristicNotificationFromHandleResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitCharacteristicNotification(self, request: gatt_pb2.WaitCharacteristicNotificationRequest, context: grpc.ServicerContext) -> gatt_pb2.WaitCharacteristicNotificationResponse:
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
        'SetCharacteristicNotificationFromHandle': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetCharacteristicNotificationFromHandle,
            request_deserializer=gatt_pb2.SetCharacteristicNotificationFromHandleRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.SetCharacteristicNotificationFromHandleResponse.SerializeToString,  # type: ignore
        ),
        'WaitCharacteristicNotification': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitCharacteristicNotification,
            request_deserializer=gatt_pb2.WaitCharacteristicNotificationRequest.FromString,  # type: ignore
            response_serializer=gatt_pb2.WaitCharacteristicNotificationResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.GATT', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
