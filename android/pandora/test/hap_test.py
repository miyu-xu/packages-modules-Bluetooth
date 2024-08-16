# Copyright (C) 2024 The Android Open Source Project
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import asyncio
import pprint

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices, asynchronous
from bumble.gatt import GATT_HEARING_ACCESS_SERVICE
from bumble.profiles import hap
from mobly.asserts import assert_is_not_none  # type: ignore
from bumble.profiles.hap import DynamicPresets, HearingAccessService, HearingAidFeatures,HearingAidType, IndependentPresets, PresetRecord, PresetSynchronizationSupport, WritablePresetsSupport
from pandora_experimental.hap_grpc_aio import HAP as HapGrpcAio
from pandora._utils import AioStream
from pandora.security_pb2 import LE_LEVEL3
from pandora.host_pb2 import RANDOM, AdvertiseResponse, Connection, DataTypes, ScanningResponse 
from mobly import base_test, signals
from mobly.asserts import assert_equal  # type: ignore
from typing import Tuple
import logging

COMPLETE_LOCAL_NAME: str = "Bumble"
HAP_UUID = GATT_HEARING_ACCESS_SERVICE.to_hex_str('-')

device_features = HearingAidFeatures(HearingAidType.MONAURAL_HEARING_AID,PresetSynchronizationSupport.PRESET_SYNCHRONIZATION_IS_NOT_SUPPORTED,IndependentPresets.IDENTICAL_PRESET_RECORD, DynamicPresets.PRESET_RECORDS_DOES_NOT_CHANGE,WritablePresetsSupport.WRITABLE_PRESET_RECORDS_SUPPORTED)

foo_preset = PresetRecord(1, "foo preset")
bar_preset = PresetRecord(50, "bar preset")
foobar_preset = PresetRecord(5, "foobar preset")

class HapTest(base_test.BaseTestClass):
    devices: PandoraDevices
    dut: PandoraDevice
    ref_left: BumblePandoraDevice
    hap_grpc: HapGrpcAio

    def setup_class(self):
        self.devices = PandoraDevices(self)
        dut, ref_left, *_ = self.devices

        if isinstance(dut, BumblePandoraDevice):
            raise signals.TestAbortClass('DUT Bumble does not support HAP')
        self.dut = dut
        if not isinstance(ref_left, BumblePandoraDevice):
            raise signals.TestAbortClass('Test require Bumble as reference device(s)')
        self.ref_left = ref_left

        self.ref_left.device.add_service(HearingAccessService(self.ref_left.device, device_features, [foo_preset, bar_preset, foobar_preset]))  # type:ignore

    def teardown_class(self):
        self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref_left.reset())
        self.hap_grpc = HapGrpcAio(channel=self.dut.aio.channel)

    async def ref_advertise_hap(self, device: PandoraDevice) -> AioStream[AdvertiseResponse]:
        return device.aio.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=RANDOM,
            data=DataTypes(
                complete_local_name=COMPLETE_LOCAL_NAME,
                incomplete_service_class_uuids16=[HAP_UUID],
            ),
            )

    async def dut_scan_for_hap(self) -> ScanningResponse:
        """
        DUT starts to scan for the Ref device.
        :return: ScanningResponse for ASHA
        """
        dut_scan = self.dut.aio.host.Scan(RANDOM)
        scan_response = await anext(
            (
                x
                async for x in dut_scan
                if HAP_UUID in x.data.incomplete_service_class_uuids16
            )
        )
        dut_scan.cancel()
        return scan_response

    async def dut_connect_to_ref(
        self, advertisement: AioStream[AdvertiseResponse], ref: ScanningResponse) -> Tuple[Connection, Connection]:
        """
        Helper method for Dut connects to Ref
        :return: a Tuple (DUT to REF connection, REF to DUT connection)
        """
        (dut_ref_res, ref_dut_res) = await asyncio.gather(
            self.dut.aio.host.ConnectLE(own_address_type=RANDOM, **ref.address_asdict()),
            anext(aiter(advertisement)),
        )
        assert_equal(dut_ref_res.result_variant(), 'connection')
        dut_ref, ref_dut = dut_ref_res.connection, ref_dut_res.connection
        assert_is_not_none(dut_ref)
        assert dut_ref
        advertisement.cancel()
        return dut_ref, ref_dut


    @asynchronous
    async def test_get_features(self) -> None:
        advertisement = await self.ref_advertise_hap(self.ref_left)
        scan_response = await self.dut_scan_for_hap()
        dut_connection_to_ref, ref_connection_to_dut = await self.dut_connect_to_ref(advertisement, scan_response)

        (secure, wait_security) = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_connection_to_ref, le=LE_LEVEL3),
            self.ref_left.aio.security.WaitSecurity(connection=ref_connection_to_dut, le=LE_LEVEL3),
        )

        advertisement.cancel()

        assert_equal(secure.result_variant(), 'success')
        assert_equal(wait_security.result_variant(), 'success')

        featuresResponse = await self.hap_grpc.GetFeatures(connection=dut_connection_to_ref)
        hearingaid_features = hap.HearingAidFeatures_from_bytes(featuresResponse.features)
        logging.info(f"getFeatures returned {pprint.pformat(hearingaid_features)}")
        assert_equal(hearingaid_features, device_features)  # type: ignore
