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


from google.protobuf import empty_pb2
from pandora_experimental import _android_pb2

from pandora_experimental._utils import unwrap, Sender, Stream, StreamStream



class AccessType(enum.IntEnum):
    ACCESS_MESSAGE = 0
    ACCESS_PHONEBOOK = 1
    ACCESS_SIM = 2


@dataclass
class LogRequest(Message):
    text: str = ''

setattr(LogRequest, '__new__', lambda _, *args, **kwargs: _android_pb2.LogRequest(*args, **kwargs))  # type: ignore

@dataclass
class LogResponse(Message):
    pass

setattr(LogResponse, '__new__', lambda _, *args, **kwargs: _android_pb2.LogResponse(*args, **kwargs))  # type: ignore

@dataclass
class SetAccessPermissionRequest(Message):
    address: bytes = b''
    access_type: AccessType = AccessType.ACCESS_MESSAGE

setattr(SetAccessPermissionRequest, '__new__', lambda _, *args, **kwargs: _android_pb2.SetAccessPermissionRequest(*args, **kwargs))  # type: ignore

@dataclass
class InternalConnectionRef(Message):
    address: bytes = b''
    transport: int = 0

setattr(InternalConnectionRef, '__new__', lambda _, *args, **kwargs: _android_pb2.InternalConnectionRef(*args, **kwargs))  # type: ignore


class Android:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel):
        self.channel = channel

    def Log(self, text: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> LogResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Android/Log',
            request_serializer=_android_pb2.LogRequest.SerializeToString,  # type: ignore
            response_deserializer=_android_pb2.LogResponse.FromString  # type: ignore
        )(_android_pb2.LogRequest(text=text), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetAccessPermission(self, address: bytes = b'', access_type: AccessType = AccessType.ACCESS_MESSAGE, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Android/SetAccessPermission',
            request_serializer=_android_pb2.SetAccessPermissionRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(_android_pb2.SetAccessPermissionRequest(address=address, access_type=access_type), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SendSMS(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Android/SendSMS',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AcceptIncomingFile(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Android/AcceptIncomingFile',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SendFile(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.Android/SendFile',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class AndroidServicer:
    def Log(self, request: LogRequest, context: grpc.ServicerContext) -> LogResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetAccessPermission(self, request: SetAccessPermissionRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SendSMS(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AcceptIncomingFile(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SendFile(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_AndroidServicer_to_server(servicer: AndroidServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'Log': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Log,
            request_deserializer=_android_pb2.LogRequest.FromString,  # type: ignore
            response_serializer=_android_pb2.LogResponse.SerializeToString,  # type: ignore
        ),
        'SetAccessPermission': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetAccessPermission,
            request_deserializer=_android_pb2.SetAccessPermissionRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SendSMS': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendSMS,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'AcceptIncomingFile': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.AcceptIncomingFile,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SendFile': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendFile,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.Android', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore


