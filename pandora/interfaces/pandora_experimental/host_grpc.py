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



from ._utils import Stream
from google.protobuf import empty_pb2
from pandora import host_pb2
from pandora.host_pb2 import NOT_CONNECTABLE
from pandora.host_pb2 import NOT_DISCOVERABLE
from pandora.host_pb2 import PRIMARY_1M
from pandora.host_pb2 import PUBLIC
from pandora.host_pb2 import SECONDARY_NONE
from typing import Generator
from typing import List
from typing import Optional
import grpc

class Host:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def FactoryReset(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/FactoryReset',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Reset(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/Reset',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ReadLocalAddress(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> host_pb2.ReadLocalAddressResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/ReadLocalAddress',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.ReadLocalAddressResponse.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Connect(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> host_pb2.ConnectResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/Connect',
            request_serializer=host_pb2.ConnectRequest.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.ConnectResponse.FromString  # type: ignore
        )(host_pb2.ConnectRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitConnection(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> host_pb2.WaitConnectionResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/WaitConnection',
            request_serializer=host_pb2.WaitConnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.WaitConnectionResponse.FromString  # type: ignore
        )(host_pb2.WaitConnectionRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ConnectLE(self, own_address_type: host_pb2.OwnAddressType = PUBLIC, public: Optional[bytes] = None, random: Optional[bytes] = None, public_identity: Optional[bytes] = None, random_static_identity: Optional[bytes] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> host_pb2.ConnectLEResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/ConnectLE',
            request_serializer=host_pb2.ConnectLERequest.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.ConnectLEResponse.FromString  # type: ignore
        )(host_pb2.ConnectLERequest(own_address_type=own_address_type, public=public, random=random, public_identity=public_identity, random_static_identity=random_static_identity), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Disconnect(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/Disconnect',
            request_serializer=host_pb2.DisconnectRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(host_pb2.DisconnectRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitDisconnection(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/WaitDisconnection',
            request_serializer=host_pb2.WaitDisconnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(host_pb2.WaitDisconnectionRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Advertise(self, legacy: bool = False, data: host_pb2.DataTypes = host_pb2.DataTypes(), scan_response_data: host_pb2.DataTypes = host_pb2.DataTypes(), own_address_type: host_pb2.OwnAddressType = PUBLIC, connectable: bool = False, interval: float = 0.0, interval_range: float = 0.0, primary_phy: host_pb2.PrimaryPhy = PRIMARY_1M, secondary_phy: host_pb2.SecondaryPhy = SECONDARY_NONE, public: Optional[bytes] = None, random: Optional[bytes] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Stream[host_pb2.AdvertiseResponse]:
        return self.channel.unary_stream(  # type: ignore
            '/pandora.Host/Advertise',
            request_serializer=host_pb2.AdvertiseRequest.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.AdvertiseResponse.FromString  # type: ignore
        )(host_pb2.AdvertiseRequest(legacy=legacy, data=data, scan_response_data=scan_response_data, own_address_type=own_address_type, connectable=connectable, interval=interval, interval_range=interval_range, primary_phy=primary_phy, secondary_phy=secondary_phy, public=public, random=random), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Scan(self, legacy: bool = False, passive: bool = False, own_address_type: host_pb2.OwnAddressType = PUBLIC, interval: float = 0.0, window: float = 0.0, phys: List[host_pb2.PrimaryPhy] = [], wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Stream[host_pb2.ScanningResponse]:
        return self.channel.unary_stream(  # type: ignore
            '/pandora.Host/Scan',
            request_serializer=host_pb2.ScanRequest.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.ScanningResponse.FromString  # type: ignore
        )(host_pb2.ScanRequest(legacy=legacy, passive=passive, own_address_type=own_address_type, interval=interval, window=window, phys=phys), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Inquiry(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Stream[host_pb2.InquiryResponse]:
        return self.channel.unary_stream(  # type: ignore
            '/pandora.Host/Inquiry',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=host_pb2.InquiryResponse.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetDiscoverabilityMode(self, mode: host_pb2.DiscoverabilityMode = NOT_DISCOVERABLE, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/SetDiscoverabilityMode',
            request_serializer=host_pb2.SetDiscoverabilityModeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(host_pb2.SetDiscoverabilityModeRequest(mode=mode), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetConnectabilityMode(self, mode: host_pb2.ConnectabilityMode = NOT_CONNECTABLE, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Host/SetConnectabilityMode',
            request_serializer=host_pb2.SetConnectabilityModeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(host_pb2.SetConnectabilityModeRequest(mode=mode), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class HostServicer:
    def FactoryReset(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Reset(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ReadLocalAddress(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> host_pb2.ReadLocalAddressResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Connect(self, request: host_pb2.ConnectRequest, context: grpc.ServicerContext) -> host_pb2.ConnectResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitConnection(self, request: host_pb2.WaitConnectionRequest, context: grpc.ServicerContext) -> host_pb2.WaitConnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ConnectLE(self, request: host_pb2.ConnectLERequest, context: grpc.ServicerContext) -> host_pb2.ConnectLEResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Disconnect(self, request: host_pb2.DisconnectRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitDisconnection(self, request: host_pb2.WaitDisconnectionRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Advertise(self, request: host_pb2.AdvertiseRequest, context: grpc.ServicerContext) -> Generator[host_pb2.AdvertiseResponse, None, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield host_pb2.AdvertiseResponse()  # no-op: to make the linter happy

    def Scan(self, request: host_pb2.ScanRequest, context: grpc.ServicerContext) -> Generator[host_pb2.ScanningResponse, None, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield host_pb2.ScanningResponse()  # no-op: to make the linter happy

    def Inquiry(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> Generator[host_pb2.InquiryResponse, None, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield host_pb2.InquiryResponse()  # no-op: to make the linter happy

    def SetDiscoverabilityMode(self, request: host_pb2.SetDiscoverabilityModeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetConnectabilityMode(self, request: host_pb2.SetConnectabilityModeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_HostServicer_to_server(servicer: HostServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'FactoryReset': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.FactoryReset,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Reset': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Reset,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'ReadLocalAddress': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ReadLocalAddress,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=host_pb2.ReadLocalAddressResponse.SerializeToString,  # type: ignore
        ),
        'Connect': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Connect,
            request_deserializer=host_pb2.ConnectRequest.FromString,  # type: ignore
            response_serializer=host_pb2.ConnectResponse.SerializeToString,  # type: ignore
        ),
        'WaitConnection': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitConnection,
            request_deserializer=host_pb2.WaitConnectionRequest.FromString,  # type: ignore
            response_serializer=host_pb2.WaitConnectionResponse.SerializeToString,  # type: ignore
        ),
        'ConnectLE': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ConnectLE,
            request_deserializer=host_pb2.ConnectLERequest.FromString,  # type: ignore
            response_serializer=host_pb2.ConnectLEResponse.SerializeToString,  # type: ignore
        ),
        'Disconnect': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Disconnect,
            request_deserializer=host_pb2.DisconnectRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'WaitDisconnection': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitDisconnection,
            request_deserializer=host_pb2.WaitDisconnectionRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Advertise': grpc.unary_stream_rpc_method_handler(  # type: ignore
            servicer.Advertise,
            request_deserializer=host_pb2.AdvertiseRequest.FromString,  # type: ignore
            response_serializer=host_pb2.AdvertiseResponse.SerializeToString,  # type: ignore
        ),
        'Scan': grpc.unary_stream_rpc_method_handler(  # type: ignore
            servicer.Scan,
            request_deserializer=host_pb2.ScanRequest.FromString,  # type: ignore
            response_serializer=host_pb2.ScanningResponse.SerializeToString,  # type: ignore
        ),
        'Inquiry': grpc.unary_stream_rpc_method_handler(  # type: ignore
            servicer.Inquiry,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=host_pb2.InquiryResponse.SerializeToString,  # type: ignore
        ),
        'SetDiscoverabilityMode': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetDiscoverabilityMode,
            request_deserializer=host_pb2.SetDiscoverabilityModeRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SetConnectabilityMode': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetConnectabilityMode,
            request_deserializer=host_pb2.SetConnectabilityModeRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.Host', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
