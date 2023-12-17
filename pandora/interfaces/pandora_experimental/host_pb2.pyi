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



from google.protobuf import any_pb2
from google.protobuf import empty_pb2
from google.protobuf.internal.enum_type_wrapper import EnumTypeWrapper
from google.protobuf.message import Message
from typing import Dict
from typing import List
from typing import Optional
from typing import Union
from typing_extensions import Literal
from typing_extensions import TypedDict

class OwnAddressType(int, EnumTypeWrapper):
  pass

PUBLIC: OwnAddressType
RANDOM: OwnAddressType
RESOLVABLE_OR_PUBLIC: OwnAddressType
RESOLVABLE_OR_RANDOM: OwnAddressType

class PrimaryPhy(int, EnumTypeWrapper):
  pass

PRIMARY_1M: PrimaryPhy
PRIMARY_CODED: PrimaryPhy

class SecondaryPhy(int, EnumTypeWrapper):
  pass

SECONDARY_NONE: SecondaryPhy
SECONDARY_1M: SecondaryPhy
SECONDARY_2M: SecondaryPhy
SECONDARY_CODED: SecondaryPhy

class DiscoverabilityMode(int, EnumTypeWrapper):
  pass

NOT_DISCOVERABLE: DiscoverabilityMode
DISCOVERABLE_LIMITED: DiscoverabilityMode
DISCOVERABLE_GENERAL: DiscoverabilityMode

class ConnectabilityMode(int, EnumTypeWrapper):
  pass

NOT_CONNECTABLE: ConnectabilityMode
CONNECTABLE: ConnectabilityMode


class Connection(Message):
  cookie: any_pb2.Any

  def __init__(self, cookie: any_pb2.Any = any_pb2.Any()) -> None: ...

class DataTypes(Message):
  incomplete_service_class_uuids16: List[str]
  complete_service_class_uuids16: List[str]
  incomplete_service_class_uuids32: List[str]
  complete_service_class_uuids32: List[str]
  incomplete_service_class_uuids128: List[str]
  complete_service_class_uuids128: List[str]
  peripheral_connection_interval_min: int
  peripheral_connection_interval_max: int
  service_solicitation_uuids16: List[str]
  service_solicitation_uuids128: List[str]
  service_data_uuid16: Dict[str, bytes]
  public_target_addresses: List[bytes]
  random_target_addresses: List[bytes]
  appearance: int
  service_solicitation_uuids32: List[str]
  service_data_uuid32: Dict[str, bytes]
  service_data_uuid128: Dict[str, bytes]
  uri: str
  le_supported_features: bytes
  manufacturer_specific_data: bytes
  le_discoverability_mode: DiscoverabilityMode
  encrypted_data: DataTypes
  shortened_local_name: Optional[str]
  include_shortened_local_name: Optional[bool]
  complete_local_name: Optional[str]
  include_complete_local_name: Optional[bool]
  tx_power_level: Optional[int]
  include_tx_power_level: Optional[bool]
  class_of_device: Optional[int]
  include_class_of_device: Optional[bool]
  advertising_interval: Optional[int]
  include_advertising_interval: Optional[bool]

  def __init__(self, incomplete_service_class_uuids16: List[str] = [], complete_service_class_uuids16: List[str] = [], incomplete_service_class_uuids32: List[str] = [], complete_service_class_uuids32: List[str] = [], incomplete_service_class_uuids128: List[str] = [], complete_service_class_uuids128: List[str] = [], peripheral_connection_interval_min: int = 0, peripheral_connection_interval_max: int = 0, service_solicitation_uuids16: List[str] = [], service_solicitation_uuids128: List[str] = [], service_data_uuid16: Dict[str, bytes] = {}, public_target_addresses: List[bytes] = [], random_target_addresses: List[bytes] = [], appearance: int = 0, service_solicitation_uuids32: List[str] = [], service_data_uuid32: Dict[str, bytes] = {}, service_data_uuid128: Dict[str, bytes] = {}, uri: str = '', le_supported_features: bytes = b'', manufacturer_specific_data: bytes = b'', le_discoverability_mode: DiscoverabilityMode = NOT_DISCOVERABLE, encrypted_data: DataTypes = DataTypes(), shortened_local_name: Optional[str] = None, include_shortened_local_name: Optional[bool] = None, complete_local_name: Optional[str] = None, include_complete_local_name: Optional[bool] = None, tx_power_level: Optional[int] = None, include_tx_power_level: Optional[bool] = None, class_of_device: Optional[int] = None, include_class_of_device: Optional[bool] = None, advertising_interval: Optional[int] = None, include_advertising_interval: Optional[bool] = None) -> None: ...

  @property
  def shortened_local_name_oneof(self) -> Union[bool, str, None]: ...
  def shortened_local_name_oneof_variant(self) -> Union[Literal['shortened_local_name'], Literal['include_shortened_local_name'], None]: ...
  def shortened_local_name_oneof_asdict(self) -> DataTypes_shortened_local_name_oneof_dict: ...

  @property
  def complete_local_name_oneof(self) -> Union[bool, str, None]: ...
  def complete_local_name_oneof_variant(self) -> Union[Literal['complete_local_name'], Literal['include_complete_local_name'], None]: ...
  def complete_local_name_oneof_asdict(self) -> DataTypes_complete_local_name_oneof_dict: ...

  @property
  def tx_power_level_oneof(self) -> Union[int, bool, None]: ...
  def tx_power_level_oneof_variant(self) -> Union[Literal['tx_power_level'], Literal['include_tx_power_level'], None]: ...
  def tx_power_level_oneof_asdict(self) -> DataTypes_tx_power_level_oneof_dict: ...

  @property
  def class_of_device_oneof(self) -> Union[int, bool, None]: ...
  def class_of_device_oneof_variant(self) -> Union[Literal['class_of_device'], Literal['include_class_of_device'], None]: ...
  def class_of_device_oneof_asdict(self) -> DataTypes_class_of_device_oneof_dict: ...

  @property
  def advertising_interval_oneof(self) -> Union[int, bool, None]: ...
  def advertising_interval_oneof_variant(self) -> Union[Literal['advertising_interval'], Literal['include_advertising_interval'], None]: ...
  def advertising_interval_oneof_asdict(self) -> DataTypes_advertising_interval_oneof_dict: ...

class DataTypes_shortened_local_name_oneof_dict(TypedDict, total=False):
  shortened_local_name: str
  include_shortened_local_name: bool

class DataTypes_complete_local_name_oneof_dict(TypedDict, total=False):
  complete_local_name: str
  include_complete_local_name: bool

class DataTypes_tx_power_level_oneof_dict(TypedDict, total=False):
  tx_power_level: int
  include_tx_power_level: bool

class DataTypes_class_of_device_oneof_dict(TypedDict, total=False):
  class_of_device: int
  include_class_of_device: bool

class DataTypes_advertising_interval_oneof_dict(TypedDict, total=False):
  advertising_interval: int
  include_advertising_interval: bool

class ReadLocalAddressResponse(Message):
  address: bytes

  def __init__(self, address: bytes = b'') -> None: ...

class ConnectRequest(Message):
  address: bytes

  def __init__(self, address: bytes = b'') -> None: ...

class ConnectResponse(Message):
  connection: Optional[Connection]
  peer_not_found: Optional[empty_pb2.Empty]
  connection_already_exists: Optional[empty_pb2.Empty]
  pairing_failure: Optional[empty_pb2.Empty]
  authentication_failure: Optional[empty_pb2.Empty]
  encryption_failure: Optional[empty_pb2.Empty]

  def __init__(self, connection: Optional[Connection] = None, peer_not_found: Optional[empty_pb2.Empty] = None, connection_already_exists: Optional[empty_pb2.Empty] = None, pairing_failure: Optional[empty_pb2.Empty] = None, authentication_failure: Optional[empty_pb2.Empty] = None, encryption_failure: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Union[None, Connection, empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['connection'], Literal['peer_not_found'], Literal['connection_already_exists'], Literal['pairing_failure'], Literal['authentication_failure'], Literal['encryption_failure'], None]: ...
  def result_asdict(self) -> ConnectResponse_result_dict: ...

class ConnectResponse_result_dict(TypedDict, total=False):
  connection: Connection
  peer_not_found: empty_pb2.Empty
  connection_already_exists: empty_pb2.Empty
  pairing_failure: empty_pb2.Empty
  authentication_failure: empty_pb2.Empty
  encryption_failure: empty_pb2.Empty

class WaitConnectionRequest(Message):
  address: bytes

  def __init__(self, address: bytes = b'') -> None: ...

class WaitConnectionResponse(Message):
  connection: Optional[Connection]

  def __init__(self, connection: Optional[Connection] = None) -> None: ...

  @property
  def result(self) -> Optional[Connection]: ...
  def result_variant(self) -> Union[Literal['connection'], None]: ...
  def result_asdict(self) -> WaitConnectionResponse_result_dict: ...

class WaitConnectionResponse_result_dict(TypedDict, total=False):
  connection: Connection

class ConnectLERequest(Message):
  own_address_type: OwnAddressType
  public: Optional[bytes]
  random: Optional[bytes]
  public_identity: Optional[bytes]
  random_static_identity: Optional[bytes]

  def __init__(self, own_address_type: OwnAddressType = PUBLIC, public: Optional[bytes] = None, random: Optional[bytes] = None, public_identity: Optional[bytes] = None, random_static_identity: Optional[bytes] = None) -> None: ...

  @property
  def address(self) -> Optional[bytes]: ...
  def address_variant(self) -> Union[Literal['public'], Literal['random'], Literal['public_identity'], Literal['random_static_identity'], None]: ...
  def address_asdict(self) -> ConnectLERequest_address_dict: ...

class ConnectLERequest_address_dict(TypedDict, total=False):
  public: bytes
  random: bytes
  public_identity: bytes
  random_static_identity: bytes

class ConnectLEResponse(Message):
  connection: Optional[Connection]
  peer_not_found: Optional[empty_pb2.Empty]
  connection_already_exists: Optional[empty_pb2.Empty]

  def __init__(self, connection: Optional[Connection] = None, peer_not_found: Optional[empty_pb2.Empty] = None, connection_already_exists: Optional[empty_pb2.Empty] = None) -> None: ...

  @property
  def result(self) -> Union[None, Connection, empty_pb2.Empty]: ...
  def result_variant(self) -> Union[Literal['connection'], Literal['peer_not_found'], Literal['connection_already_exists'], None]: ...
  def result_asdict(self) -> ConnectLEResponse_result_dict: ...

class ConnectLEResponse_result_dict(TypedDict, total=False):
  connection: Connection
  peer_not_found: empty_pb2.Empty
  connection_already_exists: empty_pb2.Empty

class DisconnectRequest(Message):
  connection: Connection

  def __init__(self, connection: Connection = Connection()) -> None: ...

class WaitDisconnectionRequest(Message):
  connection: Connection

  def __init__(self, connection: Connection = Connection()) -> None: ...

class AdvertiseRequest(Message):
  legacy: bool
  data: DataTypes
  scan_response_data: DataTypes
  own_address_type: OwnAddressType
  connectable: bool
  interval: float
  interval_range: float
  primary_phy: PrimaryPhy
  secondary_phy: SecondaryPhy
  public: Optional[bytes]
  random: Optional[bytes]

  def __init__(self, legacy: bool = False, data: DataTypes = DataTypes(), scan_response_data: DataTypes = DataTypes(), own_address_type: OwnAddressType = PUBLIC, connectable: bool = False, interval: float = 0.0, interval_range: float = 0.0, primary_phy: PrimaryPhy = PRIMARY_1M, secondary_phy: SecondaryPhy = SECONDARY_NONE, public: Optional[bytes] = None, random: Optional[bytes] = None) -> None: ...

  @property
  def target(self) -> Optional[bytes]: ...
  def target_variant(self) -> Union[Literal['public'], Literal['random'], None]: ...
  def target_asdict(self) -> AdvertiseRequest_target_dict: ...

class AdvertiseRequest_target_dict(TypedDict, total=False):
  public: bytes
  random: bytes

class AdvertiseResponse(Message):
  connection: Connection

  def __init__(self, connection: Connection = Connection()) -> None: ...

class ScanRequest(Message):
  legacy: bool
  passive: bool
  own_address_type: OwnAddressType
  interval: float
  window: float
  phys: List[PrimaryPhy]

  def __init__(self, legacy: bool = False, passive: bool = False, own_address_type: OwnAddressType = PUBLIC, interval: float = 0.0, window: float = 0.0, phys: List[PrimaryPhy] = []) -> None: ...

class ScanningResponse(Message):
  legacy: bool
  connectable: bool
  scannable: bool
  truncated: bool
  sid: int
  primary_phy: PrimaryPhy
  secondary_phy: SecondaryPhy
  tx_power: int
  rssi: int
  periodic_advertising_interval: float
  data: DataTypes
  public: Optional[bytes]
  random: Optional[bytes]
  public_identity: Optional[bytes]
  random_static_identity: Optional[bytes]
  direct_public: Optional[bytes]
  direct_non_resolvable_random: Optional[bytes]
  direct_resolved_public: Optional[bytes]
  direct_resolved_random: Optional[bytes]
  direct_unresolved_random: Optional[bytes]

  def __init__(self, legacy: bool = False, connectable: bool = False, scannable: bool = False, truncated: bool = False, sid: int = 0, primary_phy: PrimaryPhy = PRIMARY_1M, secondary_phy: SecondaryPhy = SECONDARY_NONE, tx_power: int = 0, rssi: int = 0, periodic_advertising_interval: float = 0.0, data: DataTypes = DataTypes(), public: Optional[bytes] = None, random: Optional[bytes] = None, public_identity: Optional[bytes] = None, random_static_identity: Optional[bytes] = None, direct_public: Optional[bytes] = None, direct_non_resolvable_random: Optional[bytes] = None, direct_resolved_public: Optional[bytes] = None, direct_resolved_random: Optional[bytes] = None, direct_unresolved_random: Optional[bytes] = None) -> None: ...

  @property
  def address(self) -> Optional[bytes]: ...
  def address_variant(self) -> Union[Literal['public'], Literal['random'], Literal['public_identity'], Literal['random_static_identity'], None]: ...
  def address_asdict(self) -> ScanningResponse_address_dict: ...

  @property
  def direct_address(self) -> Optional[bytes]: ...
  def direct_address_variant(self) -> Union[Literal['direct_public'], Literal['direct_non_resolvable_random'], Literal['direct_resolved_public'], Literal['direct_resolved_random'], Literal['direct_unresolved_random'], None]: ...
  def direct_address_asdict(self) -> ScanningResponse_direct_address_dict: ...

class ScanningResponse_address_dict(TypedDict, total=False):
  public: bytes
  random: bytes
  public_identity: bytes
  random_static_identity: bytes

class ScanningResponse_direct_address_dict(TypedDict, total=False):
  direct_public: bytes
  direct_non_resolvable_random: bytes
  direct_resolved_public: bytes
  direct_resolved_random: bytes
  direct_unresolved_random: bytes

class InquiryResponse(Message):
  address: bytes
  page_scan_repetition_mode: int
  class_of_device: int
  clock_offset: int
  rssi: int
  data: DataTypes

  def __init__(self, address: bytes = b'', page_scan_repetition_mode: int = 0, class_of_device: int = 0, clock_offset: int = 0, rssi: int = 0, data: DataTypes = DataTypes()) -> None: ...

class SetDiscoverabilityModeRequest(Message):
  mode: DiscoverabilityMode

  def __init__(self, mode: DiscoverabilityMode = NOT_DISCOVERABLE) -> None: ...

class SetConnectabilityModeRequest(Message):
  mode: ConnectabilityMode

  def __init__(self, mode: ConnectabilityMode = NOT_CONNECTABLE) -> None: ...

