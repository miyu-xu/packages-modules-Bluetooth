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



from pandora import pan_pb2
from typing import Optional
import grpc

class PAN:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def EnableTethering(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> pan_pb2.EnableTetheringResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.PAN/EnableTethering',
            request_serializer=pan_pb2.EnableTetheringRequest.SerializeToString,  # type: ignore
            response_deserializer=pan_pb2.EnableTetheringResponse.FromString  # type: ignore
        )(pan_pb2.EnableTetheringRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ConnectPan(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> pan_pb2.ConnectPanResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.PAN/ConnectPan',
            request_serializer=pan_pb2.ConnectPanRequest.SerializeToString,  # type: ignore
            response_deserializer=pan_pb2.ConnectPanResponse.FromString  # type: ignore
        )(pan_pb2.ConnectPanRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class PANServicer:
    def EnableTethering(self, request: pan_pb2.EnableTetheringRequest, context: grpc.ServicerContext) -> pan_pb2.EnableTetheringResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ConnectPan(self, request: pan_pb2.ConnectPanRequest, context: grpc.ServicerContext) -> pan_pb2.ConnectPanResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_PANServicer_to_server(servicer: PANServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'EnableTethering': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.EnableTethering,
            request_deserializer=pan_pb2.EnableTetheringRequest.FromString,  # type: ignore
            response_serializer=pan_pb2.EnableTetheringResponse.SerializeToString,  # type: ignore
        ),
        'ConnectPan': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ConnectPan,
            request_deserializer=pan_pb2.ConnectPanRequest.FromString,  # type: ignore
            response_serializer=pan_pb2.ConnectPanResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.PAN', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
