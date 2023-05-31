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

import asyncio
import avatar
import logging

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from avatar.pandora_server import AndroidPandoraServer
from bumble.core import (
    BT_GENERIC_AUDIO_SERVICE,
    BT_HANDSFREE_AUDIO_GATEWAY_SERVICE,
    BT_L2CAP_PROTOCOL_ID,
    BT_RFCOMM_PROTOCOL_ID,
)
from bumble.rfcomm import Server as RfcommServer
from bumble.sdp import (
    DataElement,
    ServiceAttribute,
    SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
    SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_RECORD_HANDLE_ATTRIBUTE_ID,
)
from bumble.hfp import HfpProtocol
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_not_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from mobly.asserts import assert_not_in  # type: ignore
from pandora.security_pb2 import LEVEL2
from typing import Optional

HFP_AG_FEATURE_HF_INDICATORS = (1 << 10)

HFP_HF_FEATURE_HF_INDICATORS = (1 << 8)
HFP_HF_FEATURE_DEFAULT = hex(0x01b5)

PROPERTY_HF_ENABLED = 'bluetooth.profile.hfp.hf.enabled'
PROPERTY_HF_FEATURES = 'bluetooth.hfp.hf_client_features.config'
PROPERTY_HF_INDICATOR_ENHANCED_DRIVER_SAFETY = 'bluetooth.headset_client.indicator.enhanced_driver_safety.enabled'


class HfpClientTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    def setup_class(self) -> None:
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
        for server in self.devices._servers:
            if isinstance(server, AndroidPandoraServer):
                self.dut_adb = server.device.adb
                # Enable HFP Client
                self.dut_adb.shell(['setprop', PROPERTY_HF_ENABLED, 'true'])
                # Set HF features if not set yet
                hf_feature_text = self.dut_adb.getprop(PROPERTY_HF_FEATURES)
                if len(hf_feature_text) == 0:
                    self.dut_adb.shell(['setprop', PROPERTY_HF_FEATURES, HFP_HF_FEATURE_DEFAULT])
                break

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @avatar.asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def make_classic_connection(self):
        dut_ref, ref_dut = await asyncio.gather(
            self.dut.aio.host.WaitConnection(address=self.ref.address),
            self.ref.aio.host.Connect(address=self.dut.address),
        )

        assert_equal(dut_ref.result_variant(), 'connection')
        assert_equal(ref_dut.result_variant(), 'connection')
        assert dut_ref.connection is not None and ref_dut.connection is not None

        return dut_ref.connection, ref_dut.connection

    async def make_classic_bond(self, dut_ref, ref_dut) -> None:
        dut_ref_sec, ref_dut_sec = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref, classic=LEVEL2),
            self.ref.aio.security.WaitSecurity(connection=ref_dut, classic=LEVEL2),
        )
        assert_equal(dut_ref_sec.result_variant(), 'success')
        assert_equal(ref_dut_sec.result_variant(), 'success')

    async def make_hfp_connection(self):
        # Listen RFCOMM
        dlc_connected = asyncio.get_running_loop().create_future()

        def on_dlc(dlc):
            dlc_connected.set_result(HfpProtocol(dlc))

        rfcomm_server = RfcommServer(self.ref.device)
        channel_number = rfcomm_server.listen(on_dlc)

        # Setup SDP records
        self.ref.device.sdp_service_records = make_sdp_records(channel_number)

        # Connect and pair
        dut_ref, ref_dut = await self.make_classic_connection()
        await self.make_classic_bond(dut_ref, ref_dut)

        # By default, Android HF should auto connect
        return await dlc_connected

    @avatar.parameterized((True,), (False,))
    @avatar.asynchronous
    async def test_hf_indicator_setup(self, enhanced_driver_safety_enabled: bool) -> None:
        if isinstance(self.dut, BumblePandoraDevice):
            raise signals.TestSkip('TODO: Fix test for Bumble DUT')
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip('Test require Bumble as reference device(s)')

        if enhanced_driver_safety_enabled:
            self.dut_adb.shell(['setprop', PROPERTY_HF_INDICATOR_ENHANCED_DRIVER_SAFETY, 'true'])
        else:
            self.dut_adb.shell(['setprop', PROPERTY_HF_INDICATOR_ENHANCED_DRIVER_SAFETY, 'false'])
        await self.dut.reset()

        ref_dut_hfp_protocol = await self.make_hfp_connection()

        while True:
            line = await ref_dut_hfp_protocol.next_line()

            if line.startswith('AT+BRSF='):
                # HF indicators should be enabled
                hf_features = int(line[len('AT+BRSF='):])
                assert_not_equal(hf_features & HFP_HF_FEATURE_HF_INDICATORS, 0)
                ref_dut_hfp_protocol.send_response_line(f'+BRSF: {(HFP_AG_FEATURE_HF_INDICATORS)}')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+BIND='):
                indicators = line[len('AT+BIND='):].split(',')
                if enhanced_driver_safety_enabled:
                    assert_in('2', indicators)
                else:
                    assert_not_in('2', indicators)
                ref_dut_hfp_protocol.send_response_line('OK')
                break
            elif line.startswith('AT+CIND=?'):
                ref_dut_hfp_protocol.send_response_line('+CIND: ("call",(0,1)),("callsetup",(0-3)),("service",(0-1)),'
                                                        '("signal",(0-5)),("roam",(0,1)),("battchg",(0-5)),'
                                                        '("callheld",(0-2))')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+CIND?'):
                ref_dut_hfp_protocol.send_response_line('+CIND: 0,0,1,4,1,5,0')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+CHLD=?'):
                ref_dut_hfp_protocol.send_response_line('+CHLD: 0')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+BTRH?'):
                ref_dut_hfp_protocol.send_response_line('+BTRH: 0')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith((
                    'AT+CLIP=',
                    'AT+VGS=',
                    'AT+BIA=',
                    'AT+CMER=',
                    'AT+XEVENT=',
                    'AT+XAPL=',
            )):
                ref_dut_hfp_protocol.send_response_line('OK')
            else:
                ref_dut_hfp_protocol.send_response_line('ERROR')


def make_sdp_records(rfcomm_channel):
    return {
        0x00010001: [
            ServiceAttribute(
                SDP_SERVICE_RECORD_HANDLE_ATTRIBUTE_ID,
                DataElement.unsigned_integer_32(0x00010001),
            ),
            ServiceAttribute(
                SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.uuid(BT_HANDSFREE_AUDIO_GATEWAY_SERVICE),
                    DataElement.uuid(BT_GENERIC_AUDIO_SERVICE),
                ]),
            ),
            ServiceAttribute(
                SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([DataElement.uuid(BT_L2CAP_PROTOCOL_ID)]),
                    DataElement.sequence([
                        DataElement.uuid(BT_RFCOMM_PROTOCOL_ID),
                        DataElement.unsigned_integer_8(rfcomm_channel),
                    ]),
                ]),
            ),
            ServiceAttribute(
                SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                DataElement.sequence([
                    DataElement.sequence([
                        DataElement.uuid(BT_HANDSFREE_AUDIO_GATEWAY_SERVICE),
                        DataElement.unsigned_integer_16(0x0107),
                    ])
                ]),
            ),
            ServiceAttribute(
                0x0311,
                DataElement.unsigned_integer_16(0),
            ),
        ]
    }


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
