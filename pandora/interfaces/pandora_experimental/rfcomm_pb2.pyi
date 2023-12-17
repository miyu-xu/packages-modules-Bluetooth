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



class ConnectionRequest(Message):
  address: bytes
  uuid: str

  def __init__(self, address: bytes = b'', uuid: str = '') -> None: ...

class RfcommConnection(Message):
  id: int

  def __init__(self, id: int = 0) -> None: ...

class ConnectionResponse(Message):
  connection: RfcommConnection

  def __init__(self, connection: RfcommConnection = RfcommConnection()) -> None: ...

class ServerOptions(Message):
  name: str
  uuid: str

  def __init__(self, name: str = '', uuid: str = '') -> None: ...

class ServerId(Message):
  id: int

  def __init__(self, id: int = 0) -> None: ...

class StartServerResponse(Message):
  server: ServerId

  def __init__(self, server: ServerId = ServerId()) -> None: ...

class StopServerRequest(Message):
  server: ServerId

  def __init__(self, server: ServerId = ServerId()) -> None: ...

class StopServerResponse(Message):

  def __init__(self) -> None: ...

class AcceptConnectionRequest(Message):
  server: ServerId

  def __init__(self, server: ServerId = ServerId()) -> None: ...

class AcceptConnectionResponse(Message):
  connection: RfcommConnection

  def __init__(self, connection: RfcommConnection = RfcommConnection()) -> None: ...

class DisconnectionRequest(Message):
  connection: RfcommConnection

  def __init__(self, connection: RfcommConnection = RfcommConnection()) -> None: ...

class DisconnectionResponse(Message):

  def __init__(self) -> None: ...

class TxRequest(Message):
  connection: RfcommConnection
  data: bytes

  def __init__(self, connection: RfcommConnection = RfcommConnection(), data: bytes = b'') -> None: ...

class TxResponse(Message):

  def __init__(self) -> None: ...

class RxRequest(Message):
  connection: RfcommConnection

  def __init__(self, connection: RfcommConnection = RfcommConnection()) -> None: ...

class RxResponse(Message):
  data: bytes

  def __init__(self, data: bytes = b'') -> None: ...

