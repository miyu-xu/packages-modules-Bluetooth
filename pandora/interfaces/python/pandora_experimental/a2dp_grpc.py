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
from pandora_experimental import a2dp_pb2

from pandora_experimental._utils import unwrap, Sender, Stream, StreamStream



class AudioEncoding(enum.IntEnum):
    PCM_S16_LE_44K1_STEREO = 0
    PCM_S16_LE_48K_STEREO = 1


@dataclass
class Source(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(Source, '__new__', lambda _, *args, **kwargs: a2dp_pb2.Source(*args, **kwargs))  # type: ignore

@dataclass
class Sink(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(Sink, '__new__', lambda _, *args, **kwargs: a2dp_pb2.Sink(*args, **kwargs))  # type: ignore

@dataclass
class OpenSourceRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(OpenSourceRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.OpenSourceRequest(*args, **kwargs))  # type: ignore

@dataclass
class OpenSourceResponse(Message):
    # Oneof `result` variants.
    source: Optional['Source'] = None

    @property
    def result(self) -> Optional['Source']: ...

    if sys.version_info >= (3, 8):
        class result_dict(TypedDict, total=False):
            source: 'Source'

        def result_asdict(self) -> 'OpenSourceResponse.result_dict': ...
    else:
        def result_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def result_variant(self) -> Union[Literal['source'], None]: ...
    else:
        def result_variant(self) -> Union[str, None]: ...

setattr(OpenSourceResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.OpenSourceResponse(*args, **kwargs))  # type: ignore

@dataclass
class OpenSinkRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(OpenSinkRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.OpenSinkRequest(*args, **kwargs))  # type: ignore

@dataclass
class OpenSinkResponse(Message):
    # Oneof `result` variants.
    sink: Optional['Sink'] = None

    @property
    def result(self) -> Optional['Sink']: ...

    if sys.version_info >= (3, 8):
        class result_dict(TypedDict, total=False):
            sink: 'Sink'

        def result_asdict(self) -> 'OpenSinkResponse.result_dict': ...
    else:
        def result_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def result_variant(self) -> Union[Literal['sink'], None]: ...
    else:
        def result_variant(self) -> Union[str, None]: ...

setattr(OpenSinkResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.OpenSinkResponse(*args, **kwargs))  # type: ignore

@dataclass
class WaitSourceRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(WaitSourceRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.WaitSourceRequest(*args, **kwargs))  # type: ignore

@dataclass
class WaitSourceResponse(Message):
    # Oneof `result` variants.
    source: Optional['Source'] = None

    @property
    def result(self) -> Optional['Source']: ...

    if sys.version_info >= (3, 8):
        class result_dict(TypedDict, total=False):
            source: 'Source'

        def result_asdict(self) -> 'WaitSourceResponse.result_dict': ...
    else:
        def result_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def result_variant(self) -> Union[Literal['source'], None]: ...
    else:
        def result_variant(self) -> Union[str, None]: ...

setattr(WaitSourceResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.WaitSourceResponse(*args, **kwargs))  # type: ignore

@dataclass
class WaitSinkRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(WaitSinkRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.WaitSinkRequest(*args, **kwargs))  # type: ignore

@dataclass
class WaitSinkResponse(Message):
    # Oneof `result` variants.
    sink: Optional['Sink'] = None

    @property
    def result(self) -> Optional['Sink']: ...

    if sys.version_info >= (3, 8):
        class result_dict(TypedDict, total=False):
            sink: 'Sink'

        def result_asdict(self) -> 'WaitSinkResponse.result_dict': ...
    else:
        def result_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def result_variant(self) -> Union[Literal['sink'], None]: ...
    else:
        def result_variant(self) -> Union[str, None]: ...

setattr(WaitSinkResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.WaitSinkResponse(*args, **kwargs))  # type: ignore

@dataclass
class IsSuspendedRequest(Message):
    # Oneof `target` variants.
    sink: Optional['Sink'] = None
    source: Optional['Source'] = None

    @property
    def target(self) -> Union['Source', None, 'Sink']: ...

    if sys.version_info >= (3, 8):
        class target_dict(TypedDict, total=False):
            sink: 'Sink'
            source: 'Source'

        def target_asdict(self) -> 'IsSuspendedRequest.target_dict': ...
    else:
        def target_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
    else:
        def target_variant(self) -> Union[str, None]: ...

setattr(IsSuspendedRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.IsSuspendedRequest(*args, **kwargs))  # type: ignore

@dataclass
class IsSuspendedResponse(Message):
    is_suspended: bool = False

setattr(IsSuspendedResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.IsSuspendedResponse(*args, **kwargs))  # type: ignore

@dataclass
class StartRequest(Message):
    # Oneof `target` variants.
    sink: Optional['Sink'] = None
    source: Optional['Source'] = None

    @property
    def target(self) -> Union['Source', None, 'Sink']: ...

    if sys.version_info >= (3, 8):
        class target_dict(TypedDict, total=False):
            sink: 'Sink'
            source: 'Source'

        def target_asdict(self) -> 'StartRequest.target_dict': ...
    else:
        def target_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
    else:
        def target_variant(self) -> Union[str, None]: ...

setattr(StartRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.StartRequest(*args, **kwargs))  # type: ignore

@dataclass
class StartResponse(Message):
    pass

setattr(StartResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.StartResponse(*args, **kwargs))  # type: ignore

@dataclass
class SuspendRequest(Message):
    # Oneof `target` variants.
    sink: Optional['Sink'] = None
    source: Optional['Source'] = None

    @property
    def target(self) -> Union['Source', None, 'Sink']: ...

    if sys.version_info >= (3, 8):
        class target_dict(TypedDict, total=False):
            sink: 'Sink'
            source: 'Source'

        def target_asdict(self) -> 'SuspendRequest.target_dict': ...
    else:
        def target_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
    else:
        def target_variant(self) -> Union[str, None]: ...

setattr(SuspendRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.SuspendRequest(*args, **kwargs))  # type: ignore

@dataclass
class SuspendResponse(Message):
    pass

setattr(SuspendResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.SuspendResponse(*args, **kwargs))  # type: ignore

@dataclass
class CloseRequest(Message):
    # Oneof `target` variants.
    sink: Optional['Sink'] = None
    source: Optional['Source'] = None

    @property
    def target(self) -> Union['Source', None, 'Sink']: ...

    if sys.version_info >= (3, 8):
        class target_dict(TypedDict, total=False):
            sink: 'Sink'
            source: 'Source'

        def target_asdict(self) -> 'CloseRequest.target_dict': ...
    else:
        def target_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
    else:
        def target_variant(self) -> Union[str, None]: ...

setattr(CloseRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.CloseRequest(*args, **kwargs))  # type: ignore

@dataclass
class CloseResponse(Message):
    pass

setattr(CloseResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.CloseResponse(*args, **kwargs))  # type: ignore

@dataclass
class GetAudioEncodingRequest(Message):
    # Oneof `target` variants.
    sink: Optional['Sink'] = None
    source: Optional['Source'] = None

    @property
    def target(self) -> Union['Source', None, 'Sink']: ...

    if sys.version_info >= (3, 8):
        class target_dict(TypedDict, total=False):
            sink: 'Sink'
            source: 'Source'

        def target_asdict(self) -> 'GetAudioEncodingRequest.target_dict': ...
    else:
        def target_asdict(self) -> Dict: ...

    if sys.version_info >= (3, 8):
        def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
    else:
        def target_variant(self) -> Union[str, None]: ...

setattr(GetAudioEncodingRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.GetAudioEncodingRequest(*args, **kwargs))  # type: ignore

@dataclass
class GetAudioEncodingResponse(Message):
    encoding: AudioEncoding = AudioEncoding.PCM_S16_LE_44K1_STEREO

setattr(GetAudioEncodingResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.GetAudioEncodingResponse(*args, **kwargs))  # type: ignore

@dataclass
class PlaybackAudioRequest(Message):
    source: 'Source' = Source()
    data: bytes = b''

setattr(PlaybackAudioRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.PlaybackAudioRequest(*args, **kwargs))  # type: ignore

@dataclass
class PlaybackAudioResponse(Message):
    pass

setattr(PlaybackAudioResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.PlaybackAudioResponse(*args, **kwargs))  # type: ignore

@dataclass
class CaptureAudioRequest(Message):
    sink: 'Sink' = Sink()

setattr(CaptureAudioRequest, '__new__', lambda _, *args, **kwargs: a2dp_pb2.CaptureAudioRequest(*args, **kwargs))  # type: ignore

@dataclass
class CaptureAudioResponse(Message):
    data: bytes = b''

setattr(CaptureAudioResponse, '__new__', lambda _, *args, **kwargs: a2dp_pb2.CaptureAudioResponse(*args, **kwargs))  # type: ignore


class A2DP:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel):
        self.channel = channel

    def OpenSource(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> OpenSourceResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/OpenSource',
            request_serializer=a2dp_pb2.OpenSourceRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.OpenSourceResponse.FromString  # type: ignore
        )(a2dp_pb2.OpenSourceRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def OpenSink(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> OpenSinkResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/OpenSink',
            request_serializer=a2dp_pb2.OpenSinkRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.OpenSinkResponse.FromString  # type: ignore
        )(a2dp_pb2.OpenSinkRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitSource(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> WaitSourceResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/WaitSource',
            request_serializer=a2dp_pb2.WaitSourceRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.WaitSourceResponse.FromString  # type: ignore
        )(a2dp_pb2.WaitSourceRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitSink(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> WaitSinkResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/WaitSink',
            request_serializer=a2dp_pb2.WaitSinkRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.WaitSinkResponse.FromString  # type: ignore
        )(a2dp_pb2.WaitSinkRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def IsSuspended(self, sink: Optional['Sink'] = None, source: Optional['Source'] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> IsSuspendedResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/IsSuspended',
            request_serializer=a2dp_pb2.IsSuspendedRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.IsSuspendedResponse.FromString  # type: ignore
        )(a2dp_pb2.IsSuspendedRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Start(self, sink: Optional['Sink'] = None, source: Optional['Source'] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> StartResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/Start',
            request_serializer=a2dp_pb2.StartRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.StartResponse.FromString  # type: ignore
        )(a2dp_pb2.StartRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Suspend(self, sink: Optional['Sink'] = None, source: Optional['Source'] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SuspendResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/Suspend',
            request_serializer=a2dp_pb2.SuspendRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.SuspendResponse.FromString  # type: ignore
        )(a2dp_pb2.SuspendRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Close(self, sink: Optional['Sink'] = None, source: Optional['Source'] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> CloseResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/Close',
            request_serializer=a2dp_pb2.CloseRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.CloseResponse.FromString  # type: ignore
        )(a2dp_pb2.CloseRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def GetAudioEncoding(self, sink: Optional['Sink'] = None, source: Optional['Source'] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> GetAudioEncodingResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/GetAudioEncoding',
            request_serializer=a2dp_pb2.GetAudioEncodingRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.GetAudioEncodingResponse.FromString  # type: ignore
        )(a2dp_pb2.GetAudioEncodingRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def PlaybackAudio(self, iterator: Iterator[PlaybackAudioRequest], timeout: Optional[float] = None) -> PlaybackAudioResponse:
        return self.channel.stream_unary(  # type: ignore
            '/pandora.A2DP/PlaybackAudio',
            request_serializer=a2dp_pb2.PlaybackAudioRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.PlaybackAudioResponse.FromString  # type: ignore
        )(iterator)

    def CaptureAudio(self, sink: 'Sink' = Sink(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Stream[CaptureAudioResponse]:
        return self.channel.unary_stream(  # type: ignore
            '/pandora.A2DP/CaptureAudio',
            request_serializer=a2dp_pb2.CaptureAudioRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.CaptureAudioResponse.FromString  # type: ignore
        )(a2dp_pb2.CaptureAudioRequest(sink=sink), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class A2DPServicer:
    def OpenSource(self, request: OpenSourceRequest, context: grpc.ServicerContext) -> OpenSourceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def OpenSink(self, request: OpenSinkRequest, context: grpc.ServicerContext) -> OpenSinkResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitSource(self, request: WaitSourceRequest, context: grpc.ServicerContext) -> WaitSourceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitSink(self, request: WaitSinkRequest, context: grpc.ServicerContext) -> WaitSinkResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def IsSuspended(self, request: IsSuspendedRequest, context: grpc.ServicerContext) -> IsSuspendedResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Start(self, request: StartRequest, context: grpc.ServicerContext) -> StartResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Suspend(self, request: SuspendRequest, context: grpc.ServicerContext) -> SuspendResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Close(self, request: CloseRequest, context: grpc.ServicerContext) -> CloseResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def GetAudioEncoding(self, request: GetAudioEncodingRequest, context: grpc.ServicerContext) -> GetAudioEncodingResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def PlaybackAudio(self, request: Iterator[PlaybackAudioRequest], context: grpc.ServicerContext) -> PlaybackAudioResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def CaptureAudio(self, request: CaptureAudioRequest, context: grpc.ServicerContext) -> Generator[CaptureAudioResponse, None, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield CaptureAudioResponse()  # no-op: to make the linter happy


def add_A2DPServicer_to_server(servicer: A2DPServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'OpenSource': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.OpenSource,
            request_deserializer=a2dp_pb2.OpenSourceRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.OpenSourceResponse.SerializeToString,  # type: ignore
        ),
        'OpenSink': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.OpenSink,
            request_deserializer=a2dp_pb2.OpenSinkRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.OpenSinkResponse.SerializeToString,  # type: ignore
        ),
        'WaitSource': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitSource,
            request_deserializer=a2dp_pb2.WaitSourceRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.WaitSourceResponse.SerializeToString,  # type: ignore
        ),
        'WaitSink': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitSink,
            request_deserializer=a2dp_pb2.WaitSinkRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.WaitSinkResponse.SerializeToString,  # type: ignore
        ),
        'IsSuspended': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.IsSuspended,
            request_deserializer=a2dp_pb2.IsSuspendedRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.IsSuspendedResponse.SerializeToString,  # type: ignore
        ),
        'Start': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Start,
            request_deserializer=a2dp_pb2.StartRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.StartResponse.SerializeToString,  # type: ignore
        ),
        'Suspend': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Suspend,
            request_deserializer=a2dp_pb2.SuspendRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.SuspendResponse.SerializeToString,  # type: ignore
        ),
        'Close': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Close,
            request_deserializer=a2dp_pb2.CloseRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.CloseResponse.SerializeToString,  # type: ignore
        ),
        'GetAudioEncoding': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.GetAudioEncoding,
            request_deserializer=a2dp_pb2.GetAudioEncodingRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.GetAudioEncodingResponse.SerializeToString,  # type: ignore
        ),
        'PlaybackAudio': grpc.stream_unary_rpc_method_handler(  # type: ignore
            servicer.PlaybackAudio,
            request_deserializer=a2dp_pb2.PlaybackAudioRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.PlaybackAudioResponse.SerializeToString,  # type: ignore
        ),
        'CaptureAudio': grpc.unary_stream_rpc_method_handler(  # type: ignore
            servicer.CaptureAudio,
            request_deserializer=a2dp_pb2.CaptureAudioRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.CaptureAudioResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.A2DP', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore


def _OpenSourceResponse_result_variant(self: OpenSourceResponse) -> Optional[str]:
    return self.WhichOneof('result')  # type: ignore

setattr(a2dp_pb2.OpenSourceResponse, 'result_variant', _OpenSourceResponse_result_variant)

def _OpenSourceResponse_result(self: OpenSourceResponse) -> Optional['Source']:
    variant: Optional[str] = self.result_variant()
    if variant is None: return None
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.OpenSourceResponse, 'result', property(_OpenSourceResponse_result))

def _OpenSourceResponse_result_asdict(self: OpenSourceResponse) -> 'OpenSourceResponse.result_dict':
    variant: Optional[str] = self.result_variant()
    if variant is None: return {}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.OpenSourceResponse, 'result_asdict', _OpenSourceResponse_result_asdict)

def _OpenSinkResponse_result_variant(self: OpenSinkResponse) -> Optional[str]:
    return self.WhichOneof('result')  # type: ignore

setattr(a2dp_pb2.OpenSinkResponse, 'result_variant', _OpenSinkResponse_result_variant)

def _OpenSinkResponse_result(self: OpenSinkResponse) -> Optional['Sink']:
    variant: Optional[str] = self.result_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.OpenSinkResponse, 'result', property(_OpenSinkResponse_result))

def _OpenSinkResponse_result_asdict(self: OpenSinkResponse) -> 'OpenSinkResponse.result_dict':
    variant: Optional[str] = self.result_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.OpenSinkResponse, 'result_asdict', _OpenSinkResponse_result_asdict)

def _WaitSourceResponse_result_variant(self: WaitSourceResponse) -> Optional[str]:
    return self.WhichOneof('result')  # type: ignore

setattr(a2dp_pb2.WaitSourceResponse, 'result_variant', _WaitSourceResponse_result_variant)

def _WaitSourceResponse_result(self: WaitSourceResponse) -> Optional['Source']:
    variant: Optional[str] = self.result_variant()
    if variant is None: return None
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.WaitSourceResponse, 'result', property(_WaitSourceResponse_result))

def _WaitSourceResponse_result_asdict(self: WaitSourceResponse) -> 'WaitSourceResponse.result_dict':
    variant: Optional[str] = self.result_variant()
    if variant is None: return {}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.WaitSourceResponse, 'result_asdict', _WaitSourceResponse_result_asdict)

def _WaitSinkResponse_result_variant(self: WaitSinkResponse) -> Optional[str]:
    return self.WhichOneof('result')  # type: ignore

setattr(a2dp_pb2.WaitSinkResponse, 'result_variant', _WaitSinkResponse_result_variant)

def _WaitSinkResponse_result(self: WaitSinkResponse) -> Optional['Sink']:
    variant: Optional[str] = self.result_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.WaitSinkResponse, 'result', property(_WaitSinkResponse_result))

def _WaitSinkResponse_result_asdict(self: WaitSinkResponse) -> 'WaitSinkResponse.result_dict':
    variant: Optional[str] = self.result_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    raise Exception('Field `result` not found.')

setattr(a2dp_pb2.WaitSinkResponse, 'result_asdict', _WaitSinkResponse_result_asdict)

def _IsSuspendedRequest_target_variant(self: IsSuspendedRequest) -> Optional[str]:
    return self.WhichOneof('target')  # type: ignore

setattr(a2dp_pb2.IsSuspendedRequest, 'target_variant', _IsSuspendedRequest_target_variant)

def _IsSuspendedRequest_target(self: IsSuspendedRequest) -> Union['Source', None, 'Sink']:
    variant: Optional[str] = self.target_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.IsSuspendedRequest, 'target', property(_IsSuspendedRequest_target))

def _IsSuspendedRequest_target_asdict(self: IsSuspendedRequest) -> 'IsSuspendedRequest.target_dict':
    variant: Optional[str] = self.target_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.IsSuspendedRequest, 'target_asdict', _IsSuspendedRequest_target_asdict)

def _StartRequest_target_variant(self: StartRequest) -> Optional[str]:
    return self.WhichOneof('target')  # type: ignore

setattr(a2dp_pb2.StartRequest, 'target_variant', _StartRequest_target_variant)

def _StartRequest_target(self: StartRequest) -> Union['Source', None, 'Sink']:
    variant: Optional[str] = self.target_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.StartRequest, 'target', property(_StartRequest_target))

def _StartRequest_target_asdict(self: StartRequest) -> 'StartRequest.target_dict':
    variant: Optional[str] = self.target_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.StartRequest, 'target_asdict', _StartRequest_target_asdict)

def _SuspendRequest_target_variant(self: SuspendRequest) -> Optional[str]:
    return self.WhichOneof('target')  # type: ignore

setattr(a2dp_pb2.SuspendRequest, 'target_variant', _SuspendRequest_target_variant)

def _SuspendRequest_target(self: SuspendRequest) -> Union['Source', None, 'Sink']:
    variant: Optional[str] = self.target_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.SuspendRequest, 'target', property(_SuspendRequest_target))

def _SuspendRequest_target_asdict(self: SuspendRequest) -> 'SuspendRequest.target_dict':
    variant: Optional[str] = self.target_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.SuspendRequest, 'target_asdict', _SuspendRequest_target_asdict)

def _CloseRequest_target_variant(self: CloseRequest) -> Optional[str]:
    return self.WhichOneof('target')  # type: ignore

setattr(a2dp_pb2.CloseRequest, 'target_variant', _CloseRequest_target_variant)

def _CloseRequest_target(self: CloseRequest) -> Union['Source', None, 'Sink']:
    variant: Optional[str] = self.target_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.CloseRequest, 'target', property(_CloseRequest_target))

def _CloseRequest_target_asdict(self: CloseRequest) -> 'CloseRequest.target_dict':
    variant: Optional[str] = self.target_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.CloseRequest, 'target_asdict', _CloseRequest_target_asdict)

def _GetAudioEncodingRequest_target_variant(self: GetAudioEncodingRequest) -> Optional[str]:
    return self.WhichOneof('target')  # type: ignore

setattr(a2dp_pb2.GetAudioEncodingRequest, 'target_variant', _GetAudioEncodingRequest_target_variant)

def _GetAudioEncodingRequest_target(self: GetAudioEncodingRequest) -> Union['Source', None, 'Sink']:
    variant: Optional[str] = self.target_variant()
    if variant is None: return None
    if variant == 'sink': return unwrap(self.sink)
    if variant == 'source': return unwrap(self.source)
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.GetAudioEncodingRequest, 'target', property(_GetAudioEncodingRequest_target))

def _GetAudioEncodingRequest_target_asdict(self: GetAudioEncodingRequest) -> 'GetAudioEncodingRequest.target_dict':
    variant: Optional[str] = self.target_variant()
    if variant is None: return {}
    if variant == 'sink': return {'sink': unwrap(self.sink)}
    if variant == 'source': return {'source': unwrap(self.source)}
    raise Exception('Field `target` not found.')

setattr(a2dp_pb2.GetAudioEncodingRequest, 'target_asdict', _GetAudioEncodingRequest_target_asdict)
