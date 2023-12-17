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



class CreateLECreditBasedChannelRequest(Message):
  connection: host_pb2.Connection
  psm: int
  secure: bool

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), psm: int = 0, secure: bool = False) -> None: ...

class CreateLECreditBasedChannelResponse(Message):

  def __init__(self) -> None: ...

class SendDataRequest(Message):
  connection: host_pb2.Connection
  data: bytes

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), data: bytes = b'') -> None: ...

class SendDataResponse(Message):

  def __init__(self) -> None: ...

class ReceiveDataRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class ReceiveDataResponse(Message):
  data: bytes

  def __init__(self, data: bytes = b'') -> None: ...

class ListenL2CAPChannelRequest(Message):
  connection: host_pb2.Connection
  secure: bool

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), secure: bool = False) -> None: ...

class ListenL2CAPChannelResponse(Message):

  def __init__(self) -> None: ...

class AcceptL2CAPChannelRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class AcceptL2CAPChannelResponse(Message):

  def __init__(self) -> None: ...

