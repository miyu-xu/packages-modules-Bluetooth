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



from pandora import rfcomm_pb2
from typing import Optional
import grpc

class RFCOMM:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def ConnectToServer(self, address: bytes = b'', uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.ConnectionResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/ConnectToServer',
            request_serializer=rfcomm_pb2.ConnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.ConnectionResponse.FromString  # type: ignore
        )(rfcomm_pb2.ConnectionRequest(address=address, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def StartServer(self, name: str = '', uuid: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.StartServerResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/StartServer',
            request_serializer=rfcomm_pb2.ServerOptions.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.StartServerResponse.FromString  # type: ignore
        )(rfcomm_pb2.ServerOptions(name=name, uuid=uuid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AcceptConnection(self, server: rfcomm_pb2.ServerId = rfcomm_pb2.ServerId(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.AcceptConnectionResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/AcceptConnection',
            request_serializer=rfcomm_pb2.AcceptConnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.AcceptConnectionResponse.FromString  # type: ignore
        )(rfcomm_pb2.AcceptConnectionRequest(server=server), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Disconnect(self, connection: rfcomm_pb2.RfcommConnection = rfcomm_pb2.RfcommConnection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.DisconnectionResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/Disconnect',
            request_serializer=rfcomm_pb2.DisconnectionRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.DisconnectionResponse.FromString  # type: ignore
        )(rfcomm_pb2.DisconnectionRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def StopServer(self, server: rfcomm_pb2.ServerId = rfcomm_pb2.ServerId(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.StopServerResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/StopServer',
            request_serializer=rfcomm_pb2.StopServerRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.StopServerResponse.FromString  # type: ignore
        )(rfcomm_pb2.StopServerRequest(server=server), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Send(self, connection: rfcomm_pb2.RfcommConnection = rfcomm_pb2.RfcommConnection(), data: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.TxResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/Send',
            request_serializer=rfcomm_pb2.TxRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.TxResponse.FromString  # type: ignore
        )(rfcomm_pb2.TxRequest(connection=connection, data=data), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Receive(self, connection: rfcomm_pb2.RfcommConnection = rfcomm_pb2.RfcommConnection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> rfcomm_pb2.RxResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.RFCOMM/Receive',
            request_serializer=rfcomm_pb2.RxRequest.SerializeToString,  # type: ignore
            response_deserializer=rfcomm_pb2.RxResponse.FromString  # type: ignore
        )(rfcomm_pb2.RxRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class RFCOMMServicer:
    def ConnectToServer(self, request: rfcomm_pb2.ConnectionRequest, context: grpc.ServicerContext) -> rfcomm_pb2.ConnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def StartServer(self, request: rfcomm_pb2.ServerOptions, context: grpc.ServicerContext) -> rfcomm_pb2.StartServerResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AcceptConnection(self, request: rfcomm_pb2.AcceptConnectionRequest, context: grpc.ServicerContext) -> rfcomm_pb2.AcceptConnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Disconnect(self, request: rfcomm_pb2.DisconnectionRequest, context: grpc.ServicerContext) -> rfcomm_pb2.DisconnectionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def StopServer(self, request: rfcomm_pb2.StopServerRequest, context: grpc.ServicerContext) -> rfcomm_pb2.StopServerResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Send(self, request: rfcomm_pb2.TxRequest, context: grpc.ServicerContext) -> rfcomm_pb2.TxResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Receive(self, request: rfcomm_pb2.RxRequest, context: grpc.ServicerContext) -> rfcomm_pb2.RxResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_RFCOMMServicer_to_server(servicer: RFCOMMServicer, server: grpc.Server) -> None:
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
