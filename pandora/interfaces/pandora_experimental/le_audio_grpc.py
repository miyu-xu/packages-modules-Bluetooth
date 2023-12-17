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
from pandora import host_pb2
from pandora import le_audio_pb2
from typing import Optional
import grpc

class LeAudio:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def Open(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.LeAudio/Open',
            request_serializer=le_audio_pb2.OpenRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(le_audio_pb2.OpenRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class LeAudioServicer:
    def Open(self, request: le_audio_pb2.OpenRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_LeAudioServicer_to_server(servicer: LeAudioServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'Open': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Open,
            request_deserializer=le_audio_pb2.OpenRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.LeAudio', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
