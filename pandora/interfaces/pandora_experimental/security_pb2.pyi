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

class SecurityLevel(int, EnumTypeWrapper):
  pass

LEVEL0: SecurityLevel
LEVEL1: SecurityLevel
LEVEL2: SecurityLevel
LEVEL3: SecurityLevel
LEVEL4: SecurityLevel

class LESecurityLevel(int, EnumTypeWrapper):
  pass

LE_LEVEL1: LESecurityLevel
LE_LEVEL2: LESecurityLevel
LE_LEVEL3: LESecurityLevel
LE_LEVEL4: LESecurityLevel


class PairingEvent(Message):
  address: Optional[bytes]
  connection: Optional[host_pb2.Connection]
  just_works: Optional[empty_pb2.Empty]
  numeric_comparison: Optional[int]
  passkey_entry_request: Optional[empty_pb2.Empty]
  passkey_entry_notification: Optional[int]
  pin_code_request: Optional[empty_pb2.Empty]
  pin_code_notification: Optional[bytes]

  def __init__(self, address: Optional[bytes] = None, connection: Optional[host_pb2.Connection] = None, just_works: Optional[empty_pb2.Empty] = None, numeric_comparison: Optional[int] = None, passkey_entry_request: Optional[empty_pb2.Empty] = None, passkey_entry_notification: Optional[int] = None, pin_code_request: Optional[empty_pb2.Empty] = None, pin_code_notification: Optional[bytes] = None) -> None: ...

  @property
  def remote(self) -> Union[None, bytes, host_pb2.Connection]: ...
  def remote_variant(self) -> Union[Literal['address'], Literal['connection'], None]: ...
  def remote_asdict(self) -> PairingEvent_remote_dict: ...

  @property
  def method(self) -> Union[int, None, bytes, empty_pb2.Empty]: ...
  def method_variant(self) -> Union[Literal['just_works'], Literal['numeric_comparison'], Literal['passkey_entry_request'], Literal['passkey_entry_notification'], Literal['pin_code_request'], Literal['pin_code_notification'], None]: ...
  def method_asdict(self) -> PairingEvent_method_dict: ...

class PairingEvent_remote_dict(TypedDict, total=False):
  address: bytes
  connection: host_pb2.Connection

class PairingEvent_method_dict(TypedDict, total=False):
  just_works: empty_pb2.Empty
  numeric_comparison: int
  passkey_entry_request: empty_pb2.Empty
  passkey_entry_notification: int
  pin_code_request: empty_pb2.Empty
  pin_code_notification: bytes

class PairingEventAnswer(Message):
  event: PairingEvent
  confirm: Optional[bool]
  passkey: Optional[int]
  pin: Optional[bytes]

  def __init__(self, event: PairingEvent = PairingEvent(), confirm: Optional[bool] = None, passkey: Optional[int] = None, pin: Optional[bytes] = None) -> None: ...

  @property
  def answer(self) -> Union[int, bool, bytes, None]: ...
  def answer_variant(self) -> Union[Literal['confirm'], Literal['passkey'], Literal['pin'], None]: ...
  def answer_asdict(self) -> PairingEventAnswer_answer_dict: ...

class PairingEventAnswer_answer_dict(TypedDict, total=False):
  confirm: bool
  passkey: int
  pin: bytes

class SecureRequest(Message):
  connection: host_pb2.Connection
  classic: Optional[SecurityLevel]
  le: Optional[LESecurityLevel]

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), classic: Optional[SecurityLevel] = None, le: Optional[LESecurityLevel] = None) -> None: ...

  @property
  def level(self) -> Union[None, SecurityLevel, LESecurityLevel]: ...
  def level_variant(self) -> Union[Literal['classic'], Literal['le'], None]: ...
  def level_asdict(self) -> SecureRequest_level_dict: ...

class SecureRequest_level_dict(TypedDict, total=False):
  classic: SecurityLevel
  le: LESecurityLevel

class SecureResponse(Message):
  success: Optional[empty_pb2.Empty]
  not_reached: Optional[empty_pb2.Empty]
  connection_died: Optional[empty_pb2.Empty]
  pairing_failure: Optional[empty_pb2.Empty]
  authentication_failure: Optional[empty_pb2.Empty]
  encryption_failure: Optional[empty_pb2.Empty]

  def __init__(self, success: Optional[empty_pb2.Empty] = None, not_reached: Optional[empty_pb2.Empty] = None, connection_died: Optional[empty_pb2.Empty] = None, pairing_failure: Optional[empty_pb2.Empty] = None, authentication_failure: Optional[empty_pb2.Empty] = None, encryption_failure: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Optional[empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['success'], Literal['not_reached'], Literal['connection_died'], Literal['pairing_failure'], Literal['authentication_failure'], Literal['encryption_failure'], None]: ...
  def result_asdict(self) -> SecureResponse_result_dict: ...

class SecureResponse_result_dict(TypedDict, total=False):
  success: empty_pb2.Empty
  not_reached: empty_pb2.Empty
  connection_died: empty_pb2.Empty
  pairing_failure: empty_pb2.Empty
  authentication_failure: empty_pb2.Empty
  encryption_failure: empty_pb2.Empty

class WaitSecurityRequest(Message):
  connection: host_pb2.Connection
  classic: Optional[SecurityLevel]
  le: Optional[LESecurityLevel]

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), classic: Optional[SecurityLevel] = None, le: Optional[LESecurityLevel] = None) -> None: ...

  @property
  def level(self) -> Union[None, SecurityLevel, LESecurityLevel]: ...
  def level_variant(self) -> Union[Literal['classic'], Literal['le'], None]: ...
  def level_asdict(self) -> WaitSecurityRequest_level_dict: ...

class WaitSecurityRequest_level_dict(TypedDict, total=False):
  classic: SecurityLevel
  le: LESecurityLevel

class WaitSecurityResponse(Message):
  success: Optional[empty_pb2.Empty]
  connection_died: Optional[empty_pb2.Empty]
  pairing_failure: Optional[empty_pb2.Empty]
  authentication_failure: Optional[empty_pb2.Empty]
  encryption_failure: Optional[empty_pb2.Empty]

  def __init__(self, success: Optional[empty_pb2.Empty] = None, connection_died: Optional[empty_pb2.Empty] = None, pairing_failure: Optional[empty_pb2.Empty] = None, authentication_failure: Optional[empty_pb2.Empty] = None, encryption_failure: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Optional[empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['success'], Literal['connection_died'], Literal['pairing_failure'], Literal['authentication_failure'], Literal['encryption_failure'], None]: ...
  def result_asdict(self) -> WaitSecurityResponse_result_dict: ...

class WaitSecurityResponse_result_dict(TypedDict, total=False):
  success: empty_pb2.Empty
  connection_died: empty_pb2.Empty
  pairing_failure: empty_pb2.Empty
  authentication_failure: empty_pb2.Empty
  encryption_failure: empty_pb2.Empty

class IsBondedRequest(Message):
  public: Optional[bytes]
  random: Optional[bytes]

  def __init__(self, public: Optional[bytes] = None, random: Optional[bytes] = None) -> None: ...

  @property
  def address(self) -> Optional[bytes]: ...
  def address_variant(self) -> Union[Literal['public'], Literal['random'], None]: ...
  def address_asdict(self) -> IsBondedRequest_address_dict: ...

class IsBondedRequest_address_dict(TypedDict, total=False):
  public: bytes
  random: bytes

class DeleteBondRequest(Message):
  public: Optional[bytes]
  random: Optional[bytes]

  def __init__(self, public: Optional[bytes] = None, random: Optional[bytes] = None) -> None: ...

  @property
  def address(self) -> Optional[bytes]: ...
  def address_variant(self) -> Union[Literal['public'], Literal['random'], None]: ...
  def address_asdict(self) -> DeleteBondRequest_address_dict: ...

class DeleteBondRequest_address_dict(TypedDict, total=False):
  public: bytes
  random: bytes

