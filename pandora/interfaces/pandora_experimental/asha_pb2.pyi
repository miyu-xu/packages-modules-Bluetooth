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



from google.protobuf.message import Message
from pandora import host_pb2
from typing import List



class RegisterRequest(Message):
  capability: int
  hisyncid: List[int]

  def __init__(self, capability: int = 0, hisyncid: List[int] = []) -> None: ...

class CaptureAudioRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class CaptureAudioResponse(Message):
  data: bytes

  def __init__(self, data: bytes = b'') -> None: ...

class StartRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class StartResponse(Message):

  def __init__(self) -> None: ...

class PlaybackAudioRequest(Message):
  connection: host_pb2.Connection
  data: bytes

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), data: bytes = b'') -> None: ...

class PlaybackAudioResponse(Message):

  def __init__(self) -> None: ...

class StopRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class StopResponse(Message):

  def __init__(self) -> None: ...

class WaitPeripheralRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class WaitPeripheralResponse(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

