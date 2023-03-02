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
from pandora_experimental import pan_pb2

from pandora_experimental._utils import unwrap, Sender, Stream, StreamStream





@dataclass
class EnableTetheringRequest(Message):
    pass

setattr(EnableTetheringRequest, '__new__', lambda _, *args, **kwargs: pan_pb2.EnableTetheringRequest(*args, **kwargs))  # type: ignore

@dataclass
class EnableTetheringResponse(Message):
    pass

setattr(EnableTetheringResponse, '__new__', lambda _, *args, **kwargs: pan_pb2.EnableTetheringResponse(*args, **kwargs))  # type: ignore

@dataclass
class ConnectPanRequest(Message):
    address: bytes = b''

setattr(ConnectPanRequest, '__new__', lambda _, *args, **kwargs: pan_pb2.ConnectPanRequest(*args, **kwargs))  # type: ignore

@dataclass
class ConnectPanResponse(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(ConnectPanResponse, '__new__', lambda _, *args, **kwargs: pan_pb2.ConnectPanResponse(*args, **kwargs))  # type: ignore


class PAN:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel):
        self.channel = channel

    def EnableTethering(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> EnableTetheringResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.PAN/EnableTethering',
            request_serializer=pan_pb2.EnableTetheringRequest.SerializeToString,  # type: ignore
            response_deserializer=pan_pb2.EnableTetheringResponse.FromString  # type: ignore
        )(pan_pb2.EnableTetheringRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ConnectPan(self, address: bytes = b'', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ConnectPanResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.PAN/ConnectPan',
            request_serializer=pan_pb2.ConnectPanRequest.SerializeToString,  # type: ignore
            response_deserializer=pan_pb2.ConnectPanResponse.FromString  # type: ignore
        )(pan_pb2.ConnectPanRequest(address=address), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class PANServicer:
    def EnableTethering(self, request: EnableTetheringRequest, context: grpc.ServicerContext) -> EnableTetheringResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ConnectPan(self, request: ConnectPanRequest, context: grpc.ServicerContext) -> ConnectPanResponse:
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


