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

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices, asynchronous
# from bumble.device import Device as BumbleDevice
from bumble.profiles import hap
from bumble.profiles.hap import DynamicPresets, HearingAccessService, HearingAidFeatures,HearingAidType, IndependentPresets, PresetRecord, PresetSynchronizationSupport, WritablePresetsSupport
from pandora_experimental.hap_grpc_aio import HAP as HapGrpcAio
from mobly import base_test, signals
from mobly.asserts import assert_equal  # type: ignore
import logging

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

        self.hap_grpc = HapGrpcAio(self.dut.aio.channel)

    def teardown_class(self):
        self.devices.stop_all()

    # @asynchronous
    # async def setup_test(self) -> None:
        # _add_hearing_access_service_to_device(self.ref_left.device)


    @asynchronous
    async def test_get_features(self) -> None:
        logging.info(f"WILLIAM coucou from logging")
        logging.info(f"WILLIAM coucou public {self.ref_left.device.public_address} random {self.ref_left.device.random_address}")
        ft = await self.hap_grpc.GetFeatures(address=bytes(self.ref_left.device.public_address))
        assert_equal(hap.HearingAidFeatures_from_bytes(ft), device_features)  # type: ignore
