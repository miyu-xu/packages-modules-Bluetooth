# Copyright 2023 Google LLC
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
"""All bluetooth utils."""

import enum
from pandora import host_pb2


class BleAddressType(enum.IntEnum):
    BLE_ADDR_PUBLIC = 0x00
    BLE_ADDR_RANDOM = 0x01
    BLE_ADDR_PUBLIC_ID = 0x02
    BLE_ADDR_RANDOM_ID = 0x03
    BLE_ADDR_ANONYMOUS = 0xFF


class OwnAddressType(enum.IntEnum):
    DEFAULT = -1
    PUBLIC = 0
    RANDOM = 1


def address_from(request_address: bytes):
    """Converts address from grpc server format to floss format."""
    address = request_address.hex()
    address = (address[:2] + ':' + address[2:4] + ':' + address[4:6] + ':' + address[6:8] + ':' + address[8:10] + ':' +
               address[10:12])
    return address.upper()


def address_to(address: str):
    """Converts address from floss format to grpc server format."""
    request_address = bytes.fromhex(address.replace(':', ''))
    return request_address


def advertise_data_from(request_data: host_pb2.DataTypes):
    """Mapping DataTypes to a dict.

    The dict content follows the format of floss AdvertiseData.

    Args:
        request_data : advertising data.

    Raises:
        NotImplementedError: if request data is not implemented.

    Returns:
        dict: advertising data.
    """
    advertise_data = {
        'service_uuids': [],
        'solicit_uuids': [],
        'transport_discovery_data': [],
        'manufacturer_data': {},
        'service_data': {},
        'include_tx_power_level': False,
        'include_device_name': False,
    }

    # incomplete_service_class_uuids
    if (request_data.incomplete_service_class_uuids16 or request_data.incomplete_service_class_uuids32 or
            request_data.incomplete_service_class_uuids128):
        raise NotImplementedError('Incomplete service class uuid not supported')

    # service_uuids
    for uuid16 in request_data.complete_service_class_uuids16:
        uuid16 = f'0000{uuid16}-0000-1000-8000-00805f9b34fb'
        advertise_data['service_uuids'].append(uuid16)

    for uuid32 in request_data.complete_service_class_uuids32:
        uuid32 = f'{uuid32}-0000-1000-8000-00805f9b34fb'
        advertise_data['service_uuids'].append(uuid32)

    for uuid128 in request_data.complete_service_class_uuids128:
        advertise_data['service_uuids'].append(uuid128)

    # solicit_uuids
    for uuid16 in request_data.service_solicitation_uuids16:
        uuid16 = f'0000{uuid16}-0000-1000-8000-00805f9b34fb'
        advertise_data['solicit_uuids'].append(uuid16)

    for uuid32 in request_data.service_solicitation_uuids32:
        uuid32 = f'{uuid32}-0000-1000-8000-00805f9b34fb'
        advertise_data['solicit_uuids'].append(uuid32)

    for uuid128 in request_data.service_solicitation_uuids128:
        advertise_data['solicit_uuids'].append(uuid128)

    # service_data
    for (uuid16, data) in request_data.service_data_uuid16:
        uuid16 = f'0000{uuid16}-0000-1000-8000-00805f9b34fb'
        advertise_data['service_data'][uuid16] = data

    for (uuid32, data) in request_data.service_data_uuid32:
        uuid32 = f'{uuid32}-0000-1000-8000-00805f9b34fb'
        advertise_data['service_data'][uuid32] = data

    for (uuid128, data) in request_data.service_data_uuid128:
        advertise_data['service_data'][uuid128] = data

    advertise_data['manufacturer_data']['0x00e0'] = request_data.manufacturer_specific_data

    # The name is derived from adapter directly in floss.
    if request_data.WhichOneof('shortened_local_name_oneof') in ('include_shortened_local_name',
                                                                 'include_complete_local_name'):
        advertise_data['include_device_name'] = True

    # The tx power level is decided by the lower layers.
    if request_data.WhichOneof('tx_power_level_oneof') == 'include_tx_power_level':
        advertise_data['include_tx_power_level'] = request_data.include_tx_power_level
    return advertise_data


def create_observer_name(observer):
    """Generates an unique name for an observer.

    Args:
        observer: an observer class to observer the bluetooth callbacks.

    Returns:
        str: an unique name.
    """
    return observer.__class__.__name__ + str(id(observer))


# anext build-in is new in python3.10. Deprecate this function
# when we are able to use it.
async def anext(ait):
    return await ait.__anext__()
