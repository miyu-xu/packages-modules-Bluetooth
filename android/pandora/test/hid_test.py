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

import asyncio
import avatar
import logging
import grpc

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from bumble.device import Device
from pandora_experimental.hid_grpc import HID
from pandora_experimental.hid_pb2 import HID_HOST_CONNECTED, HID_HOST_DISCONNECTED
from pandora.host_pb2 import Connection as PandoraConnection
from pandora.security_pb2 import LEVEL2
from typing import Tuple

from bumble.transport import open_transport_or_link
from bumble.core import (
    BT_BR_EDR_TRANSPORT,
    BT_L2CAP_PROTOCOL_ID,
    BT_HUMAN_INTERFACE_DEVICE_SERVICE,
    BT_HIDP_PROTOCOL_ID,
    UUID,
)
from bumble.hci import Address
from bumble.hid import (
    Device as HID_Device,
    HID_CONTROL_PSM,
    HID_INTERRUPT_PSM,
    Message,
)

from bumble.sdp import (
    Client as SDP_Client,
    DataElement,
    ServiceAttribute,
    SDP_PUBLIC_BROWSE_ROOT,
    SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
    SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_ALL_ATTRIBUTES_RANGE,
    SDP_LANGUAGE_BASE_ATTRIBUTE_ID_LIST_ATTRIBUTE_ID,
    SDP_ADDITIONAL_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_RECORD_HANDLE_ATTRIBUTE_ID,
    SDP_BROWSE_GROUP_LIST_ATTRIBUTE_ID,
)
from bumble.utils import AsyncRunner

# -----------------------------------------------------------------------------
# SDP attributes for Bluetooth HID devices
SDP_HID_SERVICE_NAME_ATTRIBUTE_ID = 0x0100
SDP_HID_SERVICE_DESCRIPTION_ATTRIBUTE_ID = 0x0101
SDP_HID_PROVIDER_NAME_ATTRIBUTE_ID = 0x0102
SDP_HID_DEVICE_RELEASE_NUMBER_ATTRIBUTE_ID = 0x0200  # [DEPRECATED]
SDP_HID_PARSER_VERSION_ATTRIBUTE_ID = 0x0201
SDP_HID_DEVICE_SUBCLASS_ATTRIBUTE_ID = 0x0202
SDP_HID_COUNTRY_CODE_ATTRIBUTE_ID = 0x0203
SDP_HID_VIRTUAL_CABLE_ATTRIBUTE_ID = 0x0204
SDP_HID_RECONNECT_INITIATE_ATTRIBUTE_ID = 0x0205
SDP_HID_DESCRIPTOR_LIST_ATTRIBUTE_ID = 0x0206
SDP_HID_LANGID_BASE_LIST_ATTRIBUTE_ID = 0x0207
SDP_HID_SDP_DISABLE_ATTRIBUTE_ID = 0x0208  # [DEPRECATED]
SDP_HID_BATTERY_POWER_ATTRIBUTE_ID = 0x0209
SDP_HID_REMOTE_WAKE_ATTRIBUTE_ID = 0x020A
SDP_HID_PROFILE_VERSION_ATTRIBUTE_ID = 0x020B  # DEPRECATED]
SDP_HID_SUPERVISION_TIMEOUT_ATTRIBUTE_ID = 0x020C
SDP_HID_NORMALLY_CONNECTABLE_ATTRIBUTE_ID = 0x020D
SDP_HID_BOOT_DEVICE_ATTRIBUTE_ID = 0x020E
SDP_HID_SSR_HOST_MAX_LATENCY_ATTRIBUTE_ID = 0x020F
SDP_HID_SSR_HOST_MIN_TIMEOUT_ATTRIBUTE_ID = 0x0210

# Refer to HID profile specification v1.1.1, "5.3 Service Discovery Protocol (SDP)" for details
# HID SDP attribute values
LANGUAGE = 0x656E  # 0x656E uint16 “en” (English)
ENCODING = 0x6A  # 0x006A uint16 UTF-8 encoding
PRIMARY_LANGUAGE_BASE_ID = 0x100  # 0x0100 uint16 PrimaryLanguageBaseID
VERSION_NUMBER = 0x0101  # 0x0101 uint16 version number (v1.1)
SERVICE_NAME = b'Bumble HID'
SERVICE_DESCRIPTION = b'Bumble'
PROVIDER_NAME = b'Bumble'
HID_PARSER_VERSION = 0x0111  # uint16 0x0111 (v1.1.1)
HID_DEVICE_SUBCLASS = 0xC0  # Combo keyboard/pointing device
HID_COUNTRY_CODE = 0x21  # 0x21 Uint8, USA
HID_VIRTUAL_CABLE = True  # Virtual cable enabled
HID_RECONNECT_INITIATE = True  #  Reconnect initiate enabled
REPORT_DESCRIPTOR_TYPE = 0x22  # 0x22 Type = Report Descriptor
HID_LANGID_BASE_LANGUAGE = 0x0409  # 0x0409 Language = English (United States)
HID_LANGID_BASE_BLUETOOTH_STRING_OFFSET = 0x100  # 0x0100 Default
HID_BATTERY_POWER = True  #  Battery power enabled
HID_REMOTE_WAKE = True  #  Remote wake enabled
HID_SUPERVISION_TIMEOUT = 0xC80  # uint16 0xC80 (2s)
HID_NORMALLY_CONNECTABLE = True  #  Normally connectable enabled
HID_BOOT_DEVICE = True  #  Boot device support enabled
HID_SSR_HOST_MAX_LATENCY = 0x640  # uint16 0x640 (1s)
HID_SSR_HOST_MIN_TIMEOUT = 0xC80  # uint16 0xC80 (2s)
HID_REPORT_MAP = bytes(  # Text String, 50 Octet Report Descriptor
    # pylint: disable=line-too-long
    [
        0x05,
        0x01,  # Usage Page (Generic Desktop Ctrls)
        0x09,
        0x06,  # Usage (Keyboard)
        0xA1,
        0x01,  # Collection (Application)
        0x85,
        0x01,  # . Report ID (1)
        0x05,
        0x07,  # . Usage Page (Kbrd/Keypad)
        0x19,
        0xE0,  # . Usage Minimum (0xE0)
        0x29,
        0xE7,  # . Usage Maximum (0xE7)
        0x15,
        0x00,  # . Logical Minimum (0)
        0x25,
        0x01,  # . Logical Maximum (1)
        0x75,
        0x01,  # . Report Size (1)
        0x95,
        0x08,  # . Report Count (8)
        0x81,
        0x02,  # . Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95,
        0x01,  # . Report Count (1)
        0x75,
        0x08,  # . Report Size (8)
        0x81,
        0x03,  # . Input (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95,
        0x05,  # . Report Count (5)
        0x75,
        0x01,  # . Report Size (1)
        0x05,
        0x08,  # . Usage Page (LEDs)
        0x19,
        0x01,  # . Usage Minimum (Num Lock)
        0x29,
        0x05,  # . Usage Maximum (Kana)
        0x91,
        0x02,  # . Output (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        0x95,
        0x01,  # . Report Count (1)
        0x75,
        0x03,  # . Report Size (3)
        0x91,
        0x03,  # . Output (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        0x95,
        0x06,  # . Report Count (6)
        0x75,
        0x08,  # . Report Size (8)
        0x15,
        0x00,  # . Logical Minimum (0)
        0x25,
        0x65,  # . Logical Maximum (101)
        0x05,
        0x07,  # . Usage Page (Kbrd/Keypad)
        0x19,
        0x00,  # . Usage Minimum (0x00)
        0x29,
        0x65,  # . Usage Maximum (0x65)
        0x81,
        0x00,  # . Input (Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0xC0,  # End Collection
        0x05,
        0x01,  # Usage Page (Generic Desktop Ctrls)
        0x09,
        0x02,  # Usage (Mouse)
        0xA1,
        0x01,  # Collection (Application)
        0x85,
        0x02,  # . Report ID (2)
        0x09,
        0x01,  # . Usage (Pointer)
        0xA1,
        0x00,  # . Collection (Physical)
        0x05,
        0x09,  # .   Usage Page (Button)
        0x19,
        0x01,  # .   Usage Minimum (0x01)
        0x29,
        0x03,  # .   Usage Maximum (0x03)
        0x15,
        0x00,  # .   Logical Minimum (0)
        0x25,
        0x01,  # .   Logical Maximum (1)
        0x95,
        0x03,  # .   Report Count (3)
        0x75,
        0x01,  # .   Report Size (1)
        0x81,
        0x02,  # .   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95,
        0x01,  # .   Report Count (1)
        0x75,
        0x05,  # .   Report Size (5)
        0x81,
        0x03,  # .   Input (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05,
        0x01,  # .   Usage Page (Generic Desktop Ctrls)
        0x09,
        0x30,  # .   Usage (X)
        0x09,
        0x31,  # .   Usage (Y)
        0x15,
        0x81,  # .   Logical Minimum (-127)
        0x25,
        0x7F,  # .   Logical Maximum (127)
        0x75,
        0x08,  # .   Report Size (8)
        0x95,
        0x02,  # .   Report Count (2)
        0x81,
        0x06,  # .   Input (Data,Var,Rel,No Wrap,Linear,Preferred State,No Null Position)
        0xC0,  # . End Collection
        0xC0,  # End Collection
    ])

# Default protocol mode set to report protocol
protocol_mode = Message.ProtocolMode.REPORT_PROTOCOL


# -----------------------------------------------------------------------------
def sdp_records():
    service_record_handle = 0x00010002
    return {
        service_record_handle: [
            ServiceAttribute(
                SDP_SERVICE_RECORD_HANDLE_ATTRIBUTE_ID,
                DataElement.unsigned_integer_32(service_record_handle),
            ),
            ServiceAttribute(
                SDP_BROWSE_GROUP_LIST_ATTRIBUTE_ID,
                DataElement.sequence([DataElement.uuid(SDP_PUBLIC_BROWSE_ROOT)]),
            ),
            ServiceAttribute(
                SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
                DataElement.sequence([DataElement.uuid(BT_HUMAN_INTERFACE_DEVICE_SERVICE)]),
            ),
            ServiceAttribute(
                SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([
                        DataElement.uuid(BT_L2CAP_PROTOCOL_ID),
                        DataElement.unsigned_integer_16(HID_CONTROL_PSM),
                    ]),
                    DataElement.sequence([
                        DataElement.uuid(BT_HIDP_PROTOCOL_ID),
                    ]),
                ]),
            ),
            ServiceAttribute(
                SDP_LANGUAGE_BASE_ATTRIBUTE_ID_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.unsigned_integer_16(LANGUAGE),
                    DataElement.unsigned_integer_16(ENCODING),
                    DataElement.unsigned_integer_16(PRIMARY_LANGUAGE_BASE_ID),
                ]),
            ),
            ServiceAttribute(
                SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([
                        DataElement.uuid(BT_HUMAN_INTERFACE_DEVICE_SERVICE),
                        DataElement.unsigned_integer_16(VERSION_NUMBER),
                    ]),
                ]),
            ),
            ServiceAttribute(
                SDP_ADDITIONAL_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([
                        DataElement.sequence([
                            DataElement.uuid(BT_L2CAP_PROTOCOL_ID),
                            DataElement.unsigned_integer_16(HID_INTERRUPT_PSM),
                        ]),
                        DataElement.sequence([
                            DataElement.uuid(BT_HIDP_PROTOCOL_ID),
                        ]),
                    ]),
                ]),
            ),
            ServiceAttribute(
                SDP_HID_SERVICE_NAME_ATTRIBUTE_ID,
                DataElement(DataElement.TEXT_STRING, SERVICE_NAME),
            ),
            ServiceAttribute(
                SDP_HID_SERVICE_DESCRIPTION_ATTRIBUTE_ID,
                DataElement(DataElement.TEXT_STRING, SERVICE_DESCRIPTION),
            ),
            ServiceAttribute(
                SDP_HID_PROVIDER_NAME_ATTRIBUTE_ID,
                DataElement(DataElement.TEXT_STRING, PROVIDER_NAME),
            ),
            ServiceAttribute(
                SDP_HID_PARSER_VERSION_ATTRIBUTE_ID,
                DataElement.unsigned_integer_32(HID_PARSER_VERSION),
            ),
            ServiceAttribute(
                SDP_HID_DEVICE_SUBCLASS_ATTRIBUTE_ID,
                DataElement.unsigned_integer_32(HID_DEVICE_SUBCLASS),
            ),
            ServiceAttribute(
                SDP_HID_COUNTRY_CODE_ATTRIBUTE_ID,
                DataElement.unsigned_integer_32(HID_COUNTRY_CODE),
            ),
            ServiceAttribute(
                SDP_HID_VIRTUAL_CABLE_ATTRIBUTE_ID,
                DataElement.boolean(HID_VIRTUAL_CABLE),
            ),
            ServiceAttribute(
                SDP_HID_RECONNECT_INITIATE_ATTRIBUTE_ID,
                DataElement.boolean(HID_RECONNECT_INITIATE),
            ),
            ServiceAttribute(
                SDP_HID_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([
                        DataElement.unsigned_integer_16(REPORT_DESCRIPTOR_TYPE),
                        DataElement(DataElement.TEXT_STRING, HID_REPORT_MAP),
                    ]),
                ]),
            ),
            ServiceAttribute(
                SDP_HID_LANGID_BASE_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([
                        DataElement.unsigned_integer_16(HID_LANGID_BASE_LANGUAGE),
                        DataElement.unsigned_integer_16(HID_LANGID_BASE_BLUETOOTH_STRING_OFFSET),
                    ]),
                ]),
            ),
            ServiceAttribute(
                SDP_HID_BATTERY_POWER_ATTRIBUTE_ID,
                DataElement.boolean(HID_BATTERY_POWER),
            ),
            ServiceAttribute(
                SDP_HID_REMOTE_WAKE_ATTRIBUTE_ID,
                DataElement.boolean(HID_REMOTE_WAKE),
            ),
            ServiceAttribute(
                SDP_HID_SUPERVISION_TIMEOUT_ATTRIBUTE_ID,
                DataElement.unsigned_integer_16(HID_SUPERVISION_TIMEOUT),
            ),
            ServiceAttribute(
                SDP_HID_NORMALLY_CONNECTABLE_ATTRIBUTE_ID,
                DataElement.boolean(HID_NORMALLY_CONNECTABLE),
            ),
            ServiceAttribute(
                SDP_HID_BOOT_DEVICE_ATTRIBUTE_ID,
                DataElement.boolean(HID_BOOT_DEVICE),
            ),
            ServiceAttribute(
                SDP_HID_SSR_HOST_MAX_LATENCY_ATTRIBUTE_ID,
                DataElement.unsigned_integer_16(HID_SSR_HOST_MAX_LATENCY),
            ),
            ServiceAttribute(
                SDP_HID_SSR_HOST_MIN_TIMEOUT_ATTRIBUTE_ID,
                DataElement.unsigned_integer_16(HID_SSR_HOST_MIN_TIMEOUT),
            ),
        ]
    }


from mobly import base_test, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from mobly.asserts import assert_is_none  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import fail  # type: ignore
from typing import Optional


class HidTest(base_test.BaseTestClass):  # type: ignore[misc]
    '''
    This class aim to test SDP on Classic Bluetooth devices.
    '''

    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    @avatar.asynchronous
    async def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        # Enable BR/EDR mode and SSP for Bumble devices.
        for device in self.devices:
            if isinstance(device, BumblePandoraDevice):
                device.config.setdefault('classic_enabled', True)
                device.config.setdefault('classic_ssp_enabled', True)
                device.config.setdefault(
                    'server',
                    {
                        'io_capability': 'no_output_no_input',
                    },
                )
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    async def make_classic_connection(self) -> Tuple[PandoraConnection, PandoraConnection]:
        dut_ref, ref_dut = await asyncio.gather(
            self.dut.aio.host.WaitConnection(address=self.ref.address),
            self.ref.aio.host.Connect(address=self.dut.address),
        )

        assert_equal(dut_ref.result_variant(), 'connection')
        assert_equal(ref_dut.result_variant(), 'connection')
        assert dut_ref.connection is not None and ref_dut.connection is not None

        return dut_ref.connection, ref_dut.connection

    async def make_classic_bond(self, dut_ref: PandoraConnection, ref_dut: PandoraConnection) -> None:
        dut_ref_sec, ref_dut_sec = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref, classic=LEVEL2),
            self.ref.aio.security.WaitSecurity(connection=ref_dut, classic=LEVEL2),
        )
        assert_equal(dut_ref_sec.result_variant(), 'success')
        assert_equal(ref_dut_sec.result_variant(), 'success')

    @avatar.asynchronous
    async def test_hid_connect_and_disconnect(self) -> None:
        # Pandora connection tokens
        ref_dut, dut_ref = None, None
        self.ref.device.sdp_service_records = sdp_records()
        # Create and register HID device
        hid_device = HID_Device(self.ref.device)
        # Connect and pair
        dut_ref, ref_dut = await self.make_classic_connection()
        await self.make_classic_bond(dut_ref, ref_dut)

        await asyncio.sleep(3)

        hid = HID(self.dut.aio.channel)

        conn_state = await hid.HostGetConnectionState(address=self.ref.address)
        assert_equal(conn_state.state, HID_HOST_CONNECTED)

        #Disconnect HID profile from HID host
        conn_state = await hid.HostDisconnect(address=self.ref.address)
        assert_equal(conn_state.state, HID_HOST_DISCONNECTED)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
