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
from typing import Optional
import grpc

class Map:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def SendSMS(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Map/SendSMS',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class MapServicer:
    def SendSMS(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_MapServicer_to_server(servicer: MapServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'SendSMS': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendSMS,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.Map', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
