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



from ._utils import AioSender as Sender
from ._utils import AioStream as Stream
from ._utils import AioStreamStream as StreamStream
from google.protobuf import empty_pb2
from google.protobuf import wrappers_pb2
from pandora import host_pb2
from pandora import security_pb2
from typing import AsyncGenerator
from typing import AsyncIterator
from typing import Awaitable
from typing import Optional
import grpc
import grpc.aio

class Security:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel) -> None:
        self.channel = channel

    def OnPairing(self, timeout: Optional[float] = None) -> StreamStream[security_pb2.PairingEventAnswer, security_pb2.PairingEvent]:
        tx: Sender[security_pb2.PairingEventAnswer] = Sender()
        rx: Stream[security_pb2.PairingEvent] = self.channel.stream_stream(  # type: ignore
            '/pandora.Security/OnPairing',
            request_serializer=security_pb2.PairingEventAnswer.SerializeToString,  # type: ignore
            response_deserializer=security_pb2.PairingEvent.FromString  # type: ignore
        )(tx)
        return StreamStream(tx, rx)

    def Secure(self, connection: host_pb2.Connection = host_pb2.Connection(), classic: Optional[security_pb2.SecurityLevel] = None, le: Optional[security_pb2.LESecurityLevel] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[security_pb2.SecureResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Security/Secure',
            request_serializer=security_pb2.SecureRequest.SerializeToString,  # type: ignore
            response_deserializer=security_pb2.SecureResponse.FromString  # type: ignore
        )(security_pb2.SecureRequest(connection=connection, classic=classic, le=le), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitSecurity(self, connection: host_pb2.Connection = host_pb2.Connection(), classic: Optional[security_pb2.SecurityLevel] = None, le: Optional[security_pb2.LESecurityLevel] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[security_pb2.WaitSecurityResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Security/WaitSecurity',
            request_serializer=security_pb2.WaitSecurityRequest.SerializeToString,  # type: ignore
            response_deserializer=security_pb2.WaitSecurityResponse.FromString  # type: ignore
        )(security_pb2.WaitSecurityRequest(connection=connection, classic=classic, le=le), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

class SecurityStorage:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel) -> None:
        self.channel = channel

    def IsBonded(self, public: Optional[bytes] = None, random: Optional[bytes] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[wrappers_pb2.BoolValue]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.SecurityStorage/IsBonded',
            request_serializer=security_pb2.IsBondedRequest.SerializeToString,  # type: ignore
            response_deserializer=wrappers_pb2.BoolValue.FromString  # type: ignore
        )(security_pb2.IsBondedRequest(public=public, random=random), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DeleteBond(self, public: Optional[bytes] = None, random: Optional[bytes] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.SecurityStorage/DeleteBond',
            request_serializer=security_pb2.DeleteBondRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(security_pb2.DeleteBondRequest(public=public, random=random), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class SecurityServicer:
    async def OnPairing(self, request: AsyncIterator[security_pb2.PairingEventAnswer], context: grpc.ServicerContext) -> AsyncGenerator[security_pb2.PairingEvent, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield security_pb2.PairingEvent()  # no-op: to make the linter happy

    async def Secure(self, request: security_pb2.SecureRequest, context: grpc.ServicerContext) -> security_pb2.SecureResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def WaitSecurity(self, request: security_pb2.WaitSecurityRequest, context: grpc.ServicerContext) -> security_pb2.WaitSecurityResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

class SecurityStorageServicer:
    async def IsBonded(self, request: security_pb2.IsBondedRequest, context: grpc.ServicerContext) -> wrappers_pb2.BoolValue:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def DeleteBond(self, request: security_pb2.DeleteBondRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_SecurityServicer_to_server(servicer: SecurityServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'OnPairing': grpc.stream_stream_rpc_method_handler(  # type: ignore
            servicer.OnPairing,
            request_deserializer=security_pb2.PairingEventAnswer.FromString,  # type: ignore
            response_serializer=security_pb2.PairingEvent.SerializeToString,  # type: ignore
        ),
        'Secure': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Secure,
            request_deserializer=security_pb2.SecureRequest.FromString,  # type: ignore
            response_serializer=security_pb2.SecureResponse.SerializeToString,  # type: ignore
        ),
        'WaitSecurity': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitSecurity,
            request_deserializer=security_pb2.WaitSecurityRequest.FromString,  # type: ignore
            response_serializer=security_pb2.WaitSecurityResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.Security', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore

def add_SecurityStorageServicer_to_server(servicer: SecurityStorageServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'IsBonded': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.IsBonded,
            request_deserializer=security_pb2.IsBondedRequest.FromString,  # type: ignore
            response_serializer=wrappers_pb2.BoolValue.SerializeToString,  # type: ignore
        ),
        'DeleteBond': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DeleteBond,
            request_deserializer=security_pb2.DeleteBondRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.SecurityStorage', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
