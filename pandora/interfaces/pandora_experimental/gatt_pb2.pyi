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
from typing import List

class AttStatusCode(int, EnumTypeWrapper):
  pass

SUCCESS: AttStatusCode
UNKNOWN_ERROR: AttStatusCode
INVALID_HANDLE: AttStatusCode
READ_NOT_PERMITTED: AttStatusCode
WRITE_NOT_PERMITTED: AttStatusCode
INSUFFICIENT_AUTHENTICATION: AttStatusCode
INVALID_OFFSET: AttStatusCode
ATTRIBUTE_NOT_FOUND: AttStatusCode
INVALID_ATTRIBUTE_LENGTH: AttStatusCode
APPLICATION_ERROR: AttStatusCode

class AttProperties(int, EnumTypeWrapper):
  pass

PROPERTY_NONE: AttProperties
PROPERTY_READ: AttProperties
PROPERTY_WRITE: AttProperties

class AttPermissions(int, EnumTypeWrapper):
  pass

PERMISSION_NONE: AttPermissions
PERMISSION_READ: AttPermissions
PERMISSION_READ_ENCRYPTED: AttPermissions
PERMISSION_READ_ENCRYPTED_MITM: AttPermissions
PERMISSION_WRITE: AttPermissions
PERMISSION_WRITE_ENCRYPTED: AttPermissions
PERMISSION_WRITE_ENCRYPTED_MITM: AttPermissions

class EnableValue(int, EnumTypeWrapper):
  pass

ENABLE_NOTIFICATION_VALUE: EnableValue
ENABLE_INDICATION_VALUE: EnableValue


class GattService(Message):
  handle: int
  type: int
  uuid: str
  included_services: List[GattService]
  characteristics: List[GattCharacteristic]

  def __init__(self, handle: int = 0, type: int = 0, uuid: str = '', included_services: List[GattService] = [], characteristics: List[GattCharacteristic] = []) -> None: ...

class GattCharacteristic(Message):
  properties: int
  permissions: int
  uuid: str
  handle: int
  descriptors: List[GattCharacteristicDescriptor]

  def __init__(self, properties: int = 0, permissions: int = 0, uuid: str = '', handle: int = 0, descriptors: List[GattCharacteristicDescriptor] = []) -> None: ...

class GattCharacteristicDescriptor(Message):
  handle: int
  permissions: int
  uuid: str

  def __init__(self, handle: int = 0, permissions: int = 0, uuid: str = '') -> None: ...

class AttValue(Message):
  handle: int
  value: bytes

  def __init__(self, handle: int = 0, value: bytes = b'') -> None: ...

class ExchangeMTURequest(Message):
  connection: host_pb2.Connection
  mtu: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), mtu: int = 0) -> None: ...

class ExchangeMTUResponse(Message):

  def __init__(self) -> None: ...

class WriteRequest(Message):
  connection: host_pb2.Connection
  handle: int
  value: bytes

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, value: bytes = b'') -> None: ...

class WriteResponse(Message):
  handle: int
  status: AttStatusCode

  def __init__(self, handle: int = 0, status: AttStatusCode = SUCCESS) -> None: ...

class SetCharacteristicNotificationFromHandleRequest(Message):
  connection: host_pb2.Connection
  handle: int
  enable_value: EnableValue

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0, enable_value: EnableValue = ENABLE_NOTIFICATION_VALUE) -> None: ...

class SetCharacteristicNotificationFromHandleResponse(Message):
  handle: int
  status: AttStatusCode

  def __init__(self, handle: int = 0, status: AttStatusCode = SUCCESS) -> None: ...

class WaitCharacteristicNotificationRequest(Message):
  connection: host_pb2.Connection
  handle: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0) -> None: ...

class WaitCharacteristicNotificationResponse(Message):
  characteristic_notification_received: bool

  def __init__(self, characteristic_notification_received: bool = False) -> None: ...

class DiscoverServiceByUuidRequest(Message):
  connection: host_pb2.Connection
  uuid: str

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), uuid: str = '') -> None: ...

class DiscoverServicesRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class DiscoverServicesResponse(Message):
  services: List[GattService]

  def __init__(self, services: List[GattService] = []) -> None: ...

class DiscoverServicesSdpRequest(Message):
  address: bytes

  def __init__(self, address: bytes = b'') -> None: ...

class DiscoverServicesSdpResponse(Message):
  service_uuids: List[str]

  def __init__(self, service_uuids: List[str] = []) -> None: ...

class ClearCacheRequest(Message):
  connection: host_pb2.Connection

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection()) -> None: ...

class ClearCacheResponse(Message):

  def __init__(self) -> None: ...

class ReadCharacteristicRequest(Message):
  connection: host_pb2.Connection
  handle: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0) -> None: ...

class ReadCharacteristicsFromUuidRequest(Message):
  connection: host_pb2.Connection
  uuid: str
  start_handle: int
  end_handle: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), uuid: str = '', start_handle: int = 0, end_handle: int = 0) -> None: ...

class ReadCharacteristicResponse(Message):
  value: AttValue
  status: AttStatusCode

  def __init__(self, value: AttValue = AttValue(), status: AttStatusCode = SUCCESS) -> None: ...

class ReadCharacteristicsFromUuidResponse(Message):
  characteristics_read: List[ReadCharacteristicResponse]

  def __init__(self, characteristics_read: List[ReadCharacteristicResponse] = []) -> None: ...

class ReadCharacteristicDescriptorRequest(Message):
  connection: host_pb2.Connection
  handle: int

  def __init__(self, connection: host_pb2.Connection = host_pb2.Connection(), handle: int = 0) -> None: ...

class ReadCharacteristicDescriptorResponse(Message):
  value: AttValue
  status: AttStatusCode

  def __init__(self, value: AttValue = AttValue(), status: AttStatusCode = SUCCESS) -> None: ...

class GattServiceParams(Message):
  uuid: str
  characteristics: List[GattCharacteristicParams]

  def __init__(self, uuid: str = '', characteristics: List[GattCharacteristicParams] = []) -> None: ...

class GattCharacteristicParams(Message):
  properties: int
  permissions: int
  uuid: str
  descriptors: List[GattDescriptorParams]

  def __init__(self, properties: int = 0, permissions: int = 0, uuid: str = '', descriptors: List[GattDescriptorParams] = []) -> None: ...

class GattDescriptorParams(Message):
  properties: int
  permissions: int
  uuid: str

  def __init__(self, properties: int = 0, permissions: int = 0, uuid: str = '') -> None: ...

class RegisterServiceRequest(Message):
  service: GattServiceParams

  def __init__(self, service: GattServiceParams = GattServiceParams()) -> None: ...

class RegisterServiceResponse(Message):
  service: GattService

  def __init__(self, service: GattService = GattService()) -> None: ...

