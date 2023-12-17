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
from pandora import opp_pb2
from typing import Optional
import grpc

class Opp:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def OpenRfcommChannel(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Opp/OpenRfcommChannel',
            request_serializer=opp_pb2.OpenRfcommChannelRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(opp_pb2.OpenRfcommChannelRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def OpenL2capChannel(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Opp/OpenL2capChannel',
            request_serializer=opp_pb2.OpenL2capChannelRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(opp_pb2.OpenL2capChannelRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AcceptPutOperation(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> opp_pb2.AcceptPutOperationResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Opp/AcceptPutOperation',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=opp_pb2.AcceptPutOperationResponse.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class OppServicer:
    def OpenRfcommChannel(self, request: opp_pb2.OpenRfcommChannelRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def OpenL2capChannel(self, request: opp_pb2.OpenL2capChannelRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AcceptPutOperation(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> opp_pb2.AcceptPutOperationResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_OppServicer_to_server(servicer: OppServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'OpenRfcommChannel': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.OpenRfcommChannel,
            request_deserializer=opp_pb2.OpenRfcommChannelRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'OpenL2capChannel': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.OpenL2capChannel,
            request_deserializer=opp_pb2.OpenL2capChannelRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'AcceptPutOperation': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.AcceptPutOperation,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=opp_pb2.AcceptPutOperationResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.Opp', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
