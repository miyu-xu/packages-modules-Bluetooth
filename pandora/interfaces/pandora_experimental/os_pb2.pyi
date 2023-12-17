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

class AccessType(int, EnumTypeWrapper):
  pass

ACCESS_MESSAGE: AccessType
ACCESS_PHONEBOOK: AccessType
ACCESS_SIM: AccessType


class LogRequest(Message):
  text: str

  def __init__(self, text: str = '') -> None: ...

class LogResponse(Message):

  def __init__(self) -> None: ...

class SetAccessPermissionRequest(Message):
  address: bytes
  access_type: AccessType

  def __init__(self, address: bytes = b'', access_type: AccessType = ACCESS_MESSAGE) -> None: ...

class InternalConnectionRef(Message):
  address: bytes
  transport: int

  def __init__(self, address: bytes = b'', transport: int = 0) -> None: ...

class SendPingRequest(Message):
  ip_address: str

  def __init__(self, ip_address: str = '') -> None: ...

