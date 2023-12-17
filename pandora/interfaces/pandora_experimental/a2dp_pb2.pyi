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
from google.protobuf.internal.enum_type_wrapper import EnumTypeWrapper
from google.protobuf.message import Message
from pandora import host_pb2
from typing import Optional
from typing import Union
from typing_extensions import Literal
from typing_extensions import TypedDict

class AudioEncoding(int, EnumTypeWrapper):
  pass

PCM_S16_LE_44K1_STEREO: AudioEncoding
PCM_S16_LE_48K_STEREO: AudioEncoding


class Source(Message):
  cookie: bytes

  def __init__(self, cookie: bytes = b'') -> None: ...

class Sink(Message):
  cookie: bytes

  def __init__(self, cookie: bytes = b'') -> None: ...

class OpenSourceRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class OpenSourceResponse(Message):
  source: Optional[Source]
  disconnected: Optional[empty_pb2.Empty]

  def __init__(self, source: Optional[Source] = None, disconnected: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Union[None, empty_pb2.Empty, Source]: ...
  def result_variant(self) -> Union[Literal['source'], Literal['disconnected'], None]: ...
  def result_asdict(self) -> OpenSourceResponse_result_dict: ...

class OpenSourceResponse_result_dict(TypedDict, total=False):
  source: Source
  disconnected: empty_pb2.Empty

class OpenSinkRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class OpenSinkResponse(Message):
  sink: Optional[Sink]
  disconnected: Optional[empty_pb2.Empty]

  def __init__(self, sink: Optional[Sink] = None, disconnected: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Union[None, Sink, empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['sink'], Literal['disconnected'], None]: ...
  def result_asdict(self) -> OpenSinkResponse_result_dict: ...

class OpenSinkResponse_result_dict(TypedDict, total=False):
  sink: Sink
  disconnected: empty_pb2.Empty

class WaitSourceRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class WaitSourceResponse(Message):
  source: Optional[Source]
  disconnected: Optional[empty_pb2.Empty]

  def __init__(self, source: Optional[Source] = None, disconnected: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Union[None, empty_pb2.Empty, Source]: ...
  def result_variant(self) -> Union[Literal['source'], Literal['disconnected'], None]: ...
  def result_asdict(self) -> WaitSourceResponse_result_dict: ...

class WaitSourceResponse_result_dict(TypedDict, total=False):
  source: Source
  disconnected: empty_pb2.Empty

class WaitSinkRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class WaitSinkResponse(Message):
  sink: Optional[Sink]
  disconnected: Optional[empty_pb2.Empty]

  def __init__(self, sink: Optional[Sink] = None, disconnected: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Union[None, Sink, empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['sink'], Literal['disconnected'], None]: ...
  def result_asdict(self) -> WaitSinkResponse_result_dict: ...

class WaitSinkResponse_result_dict(TypedDict, total=False):
  sink: Sink
  disconnected: empty_pb2.Empty

class IsSuspendedRequest(Message):
  sink: Optional[Sink]
  source: Optional[Source]

  def __init__(self, sink: Optional[Sink] = None, source: Optional[Source] = None) -> None: ...

  @property
  def target(self) -> Union[None, Sink, Source]: ...
  def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
  def target_asdict(self) -> IsSuspendedRequest_target_dict: ...

class IsSuspendedRequest_target_dict(TypedDict, total=False):
  sink: Sink
  source: Source

class StartRequest(Message):
  sink: Optional[Sink]
  source: Optional[Source]

  def __init__(self, sink: Optional[Sink] = None, source: Optional[Source] = None) -> None: ...

  @property
  def target(self) -> Union[None, Sink, Source]: ...
  def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
  def target_asdict(self) -> StartRequest_target_dict: ...

class StartRequest_target_dict(TypedDict, total=False):
  sink: Sink
  source: Source

class StartResponse(Message):
  started: Optional[empty_pb2.Empty]
  already_started: Optional[empty_pb2.Empty]
  disconnected: Optional[empty_pb2.Empty]

  def __init__(self, started: Optional[empty_pb2.Empty] = None, already_started: Optional[empty_pb2.Empty] = None, disconnected: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Optional[empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['started'], Literal['already_started'], Literal['disconnected'], None]: ...
  def result_asdict(self) -> StartResponse_result_dict: ...

class StartResponse_result_dict(TypedDict, total=False):
  started: empty_pb2.Empty
  already_started: empty_pb2.Empty
  disconnected: empty_pb2.Empty

class SuspendRequest(Message):
  sink: Optional[Sink]
  source: Optional[Source]

  def __init__(self, sink: Optional[Sink] = None, source: Optional[Source] = None) -> None: ...

  @property
  def target(self) -> Union[None, Sink, Source]: ...
  def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
  def target_asdict(self) -> SuspendRequest_target_dict: ...

class SuspendRequest_target_dict(TypedDict, total=False):
  sink: Sink
  source: Source

class SuspendResponse(Message):
  suspended: Optional[empty_pb2.Empty]
  already_suspended: Optional[empty_pb2.Empty]
  disconnected: Optional[empty_pb2.Empty]

  def __init__(self, suspended: Optional[empty_pb2.Empty] = None, already_suspended: Optional[empty_pb2.Empty] = None, disconnected: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Optional[empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['suspended'], Literal['already_suspended'], Literal['disconnected'], None]: ...
  def result_asdict(self) -> SuspendResponse_result_dict: ...

class SuspendResponse_result_dict(TypedDict, total=False):
  suspended: empty_pb2.Empty
  already_suspended: empty_pb2.Empty
  disconnected: empty_pb2.Empty

class CloseRequest(Message):
  sink: Optional[Sink]
  source: Optional[Source]

  def __init__(self, sink: Optional[Sink] = None, source: Optional[Source] = None) -> None: ...

  @property
  def target(self) -> Union[None, Sink, Source]: ...
  def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
  def target_asdict(self) -> CloseRequest_target_dict: ...

class CloseRequest_target_dict(TypedDict, total=False):
  sink: Sink
  source: Source

class CloseResponse(Message):

  def __init__(self) -> None: ...

class GetAudioEncodingRequest(Message):
  sink: Optional[Sink]
  source: Optional[Source]

  def __init__(self, sink: Optional[Sink] = None, source: Optional[Source] = None) -> None: ...

  @property
  def target(self) -> Union[None, Sink, Source]: ...
  def target_variant(self) -> Union[Literal['sink'], Literal['source'], None]: ...
  def target_asdict(self) -> GetAudioEncodingRequest_target_dict: ...

class GetAudioEncodingRequest_target_dict(TypedDict, total=False):
  sink: Sink
  source: Source

class GetAudioEncodingResponse(Message):
  encoding: AudioEncoding

  def __init__(self, encoding: AudioEncoding = PCM_S16_LE_44K1_STEREO) -> None: ...

class PlaybackAudioRequest(Message):
  source: Source
  data: bytes

  def __init__(self, source: Source = Source(), data: bytes = b'') -> None: ...

class PlaybackAudioResponse(Message):

  def __init__(self) -> None: ...

class CaptureAudioRequest(Message):
  sink: Sink

  def __init__(self, sink: Sink = Sink()) -> None: ...

class CaptureAudioResponse(Message):
  data: bytes

  def __init__(self, data: bytes = b'') -> None: ...

