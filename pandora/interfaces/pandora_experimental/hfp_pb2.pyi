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



from google.protobuf.internal.enum_type_wrapper import EnumTypeWrapper
from google.protobuf.message import Message
from pandora import host_pb2

class AudioPath(int, EnumTypeWrapper):
  pass

AUDIO_PATH_UNKNOWN: AudioPath
AUDIO_PATH_SPEAKERS: AudioPath
AUDIO_PATH_HANDSFREE: AudioPath


class EnableSlcRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class DisableSlcRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class SetBatteryLevelRequest(Message):
  connection: host_pb2.Connection
  battery_percentage: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), battery_percentage: int = 0) -> None: ...

class AnswerCallRequest(Message):

  def __init__(self) -> None: ...

class AnswerCallResponse(Message):

  def __init__(self) -> None: ...

class DeclineCallRequest(Message):

  def __init__(self) -> None: ...

class DeclineCallResponse(Message):

  def __init__(self) -> None: ...

class SetAudioPathRequest(Message):
  audio_path: AudioPath

  def __init__(self, audio_path: AudioPath = AUDIO_PATH_UNKNOWN) -> None: ...

class SetAudioPathResponse(Message):

  def __init__(self) -> None: ...

class SwapActiveCallRequest(Message):

  def __init__(self) -> None: ...

class SwapActiveCallResponse(Message):

  def __init__(self) -> None: ...

class SetInBandRingtoneRequest(Message):
  enabled: bool

  def __init__(self, enabled: bool = False) -> None: ...

class SetInBandRingtoneResponse(Message):

  def __init__(self) -> None: ...

class MakeCallRequest(Message):
  number: str

  def __init__(self, number: str = '') -> None: ...

class MakeCallResponse(Message):

  def __init__(self) -> None: ...

class SetVoiceRecognitionRequest(Message):
  connection: host_pb2.Connection
  enabled: bool

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), enabled: bool = False) -> None: ...

class SetVoiceRecognitionResponse(Message):

  def __init__(self) -> None: ...

class ClearCallHistoryRequest(Message):

  def __init__(self) -> None: ...

class ClearCallHistoryResponse(Message):

  def __init__(self) -> None: ...

class AnswerCallAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class AnswerCallAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class EndCallAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class EndCallAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class DeclineCallAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class DeclineCallAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class ConnectToAudioAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class ConnectToAudioAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class DisconnectFromAudioAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class DisconnectFromAudioAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class MakeCallAsHandsfreeRequest(Message):
  connection: host_pb2.Connection
  number: str

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), number: str = '') -> None: ...

class MakeCallAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class CallTransferAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class CallTransferAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class EnableSlcAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class DisableSlcAsHandsfreeRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class SetVoiceRecognitionAsHandsfreeRequest(Message):
  connection: host_pb2.Connection
  enabled: bool

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), enabled: bool = False) -> None: ...

class SetVoiceRecognitionAsHandsfreeResponse(Message):

  def __init__(self) -> None: ...

class SendDtmfFromHandsfreeRequest(Message):
  connection: host_pb2.Connection
  code: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), code: int = 0) -> None: ...

class SendDtmfFromHandsfreeResponse(Message):

  def __init__(self) -> None: ...

