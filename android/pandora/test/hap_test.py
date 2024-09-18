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

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices, asynchronous
from bumble import att
from bumble.gatt import GATT_HEARING_ACCESS_SERVICE, GATT_AUDIO_STREAM_CONTROL_SERVICE, GATT_PUBLISHED_AUDIO_CAPABILITIES_SERVICE
from bumble.profiles import hap
from bumble.profiles.hap import DynamicPresets, HearingAccessService, HearingAidFeatures, HearingAidType, IndependentPresets, PresetRecord, PresetSynchronizationSupport, WritablePresetsSupport

from pandora_experimental.gatt_grpc_aio import GATT
from pandora_experimental.hap_grpc_aio import HAP  # type: ignore
from pandora_experimental.hap_pb2 import PresetRecord as grpcPresetRecord  # type: ignore
from pandora._utils import AioStream
from pandora.security_pb2 import LE_LEVEL3
from pandora.host_pb2 import RANDOM, AdvertiseResponse, Connection, DataTypes, ScanningResponse
from mobly import base_test, signals
from truth.truth import AssertThat  # type: ignore
from typing import List, Tuple

COMPLETE_LOCAL_NAME: str = "Bumble"
HAP_UUID = GATT_HEARING_ACCESS_SERVICE.to_hex_str('-')
ASCS_UUID = GATT_AUDIO_STREAM_CONTROL_SERVICE.to_hex_str('-')
PACS_UUID = GATT_PUBLISHED_AUDIO_CAPABILITIES_SERVICE.to_hex_str('-')

long_name = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
foo_preset = PresetRecord(1, "foo preset")
bar_preset = PresetRecord(50, "bar preset")
longname_preset = PresetRecord(5, f'[{long_name[:38]}]')
unavailable_preset = PresetRecord(
    7, "unavailable preset",
    PresetRecord.Property(PresetRecord.Property.Writable.CANNOT_BE_WRITTEN,
                          PresetRecord.Property.IsAvailable.IS_UNAVAILABLE))

att.ATT_DEFAULT_MTU = 49  # 2.5. GATT sub-procedure requirements


def toBumblePreset(grpc_preset: grpcPresetRecord) -> PresetRecord:  # type: ignore
    return PresetRecord(
        grpc_preset.index,
        grpc_preset.name,  # type: ignore
        PresetRecord.Property(
            PresetRecord.Property.Writable(grpc_preset.isWritable),  # type: ignore
            PresetRecord.Property.IsAvailable(grpc_preset.isAvailable)))  # type: ignore


def toBumblePresetList(grpc_preset_list: List[grpcPresetRecord]) -> List[PresetRecord]:  # type: ignore
    return [toBumblePreset(grpc_preset) for grpc_preset in grpc_preset_list]  # type: ignore


def get_server_preset_sorted(has: HearingAccessService) -> List[PresetRecord]:
    return [has.preset_records[key] for key in sorted(has.preset_records.keys())]


class HapTest(base_test.BaseTestClass):
    devices: PandoraDevices
    dut: PandoraDevice
    ref_left: BumblePandoraDevice
    ref_right: BumblePandoraDevice
    hap_grpc: HAP
    has_left: HearingAccessService
    has_right: HearingAccessService

    def setup_class(self):
        self.devices = PandoraDevices(self)
        # dut, ref_left, *_ = self.devices
        dut, ref_left, ref_right, *_ = self.devices

        if isinstance(dut, BumblePandoraDevice):
            raise signals.TestAbortClass('DUT Bumble does not support HAP')
        self.dut = dut
        if not isinstance(ref_left, BumblePandoraDevice):
            raise signals.TestAbortClass('Test require Bumble as reference device(s)')
        self.ref_left = ref_left
        if not isinstance(ref_right, BumblePandoraDevice):
            raise signals.TestAbortClass('Test require Bumble as reference device(s)')
        self.ref_right = ref_right

    def teardown_class(self):
        self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref_left.reset(), self.ref_right.reset())
        self.hap_grpc = HAP(channel=self.dut.aio.channel)
        self.gatt = GATT(channel=self.dut.aio.channel)

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
        dut_scan = self.dut.aio.host.Scan(RANDOM)  # type: ignore
        scan_response = await anext((x async for x in dut_scan if HAP_UUID in x.data.incomplete_service_class_uuids16))
        dut_scan.cancel()
        return scan_response

    async def dut_connect_to_ref(self, advertisement: AioStream[AdvertiseResponse],
                                 ref: ScanningResponse) -> Tuple[Connection, Connection]:
        """
        Helper method for Dut connects to Ref
        :return: a Tuple (DUT to REF connection, REF to DUT connection)
        """
        (dut_ref_res, ref_dut_res) = await asyncio.gather(
            self.dut.aio.host.ConnectLE(own_address_type=RANDOM, **ref.address_asdict()),
            anext(aiter(advertisement)),
        )
        AssertThat(dut_ref_res.result_variant()).IsEqualTo('connection')  # type: ignore
        dut_ref, ref_dut = dut_ref_res.connection, ref_dut_res.connection
        AssertThat(dut_ref).IsNotNone()  # type: ignore
        assert dut_ref
        advertisement.cancel()
        return dut_ref, ref_dut

    async def setupHapConnection(self, ref: BumblePandoraDevice):
        advertisement = await self.ref_advertise_hap(ref)
        scan_response = await self.dut_scan_for_hap()
        dut_connection_to_ref, ref_connection_to_dut = await self.dut_connect_to_ref(advertisement, scan_response)

        await self.gatt.ExchangeMTU(mtu=512, connection=dut_connection_to_ref)

        (secure, wait_security) = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_connection_to_ref, le=LE_LEVEL3),
            ref.aio.security.WaitSecurity(connection=ref_connection_to_dut, le=LE_LEVEL3),
        )

        AssertThat(secure.result_variant()).IsEqualTo('success')  # type: ignore
        AssertThat(wait_security.result_variant()).IsEqualTo('success')  # type: ignore

        await self.hap_grpc.WaitPeripheral(connection=dut_connection_to_ref)  # type: ignore
        advertisement.cancel()

        return dut_connection_to_ref, ref_connection_to_dut

    async def assertIdentiqPresetInDutAndRef(self, dut_connection_to_ref: Connection):
        remote_preset = toBumblePresetList(
            (await
             self.hap_grpc.GetAllPresetsInfo(connection=dut_connection_to_ref)).preset_record_list)  # type: ignore
        AssertThat(remote_preset).ContainsExactlyElementsIn(  # type: ignore
            get_server_preset_sorted(self.has_left)).InOrder()  # type: ignore

    async def setup_monaural(self) -> tuple[Connection, Connection]:
        device_features = HearingAidFeatures(HearingAidType.MONAURAL_HEARING_AID,
                                             PresetSynchronizationSupport.PRESET_SYNCHRONIZATION_IS_NOT_SUPPORTED,
                                             IndependentPresets.IDENTICAL_PRESET_RECORD,
                                             DynamicPresets.PRESET_RECORDS_MAY_CHANGE,
                                             WritablePresetsSupport.WRITABLE_PRESET_RECORDS_SUPPORTED)
        self.has_left = HearingAccessService(self.ref_left.device, device_features,
                                             [foo_preset, bar_preset, longname_preset, unavailable_preset])
        self.ref_left.device.add_service(self.has_left)  # type: ignore

        return await self.setupHapConnection(self.ref_left)

    async def setup_binaural(self) -> tuple[tuple[Connection, Connection], tuple[Connection, Connection]]:
        device_features = HearingAidFeatures(HearingAidType.BINAURAL_HEARING_AID,
                                             PresetSynchronizationSupport.PRESET_SYNCHRONIZATION_IS_NOT_SUPPORTED,
                                             IndependentPresets.IDENTICAL_PRESET_RECORD,
                                             DynamicPresets.PRESET_RECORDS_MAY_CHANGE,
                                             WritablePresetsSupport.WRITABLE_PRESET_RECORDS_SUPPORTED)
        self.has_left = HearingAccessService(self.ref_left.device, device_features,
                                             [foo_preset, bar_preset, longname_preset, unavailable_preset])
        self.ref_left.device.add_service(self.has_left)  # type: ignore

        self.has_right = HearingAccessService(self.ref_right.device, device_features,
                                              [foo_preset, bar_preset, longname_preset, unavailable_preset])
        self.ref_right.device.add_service(self.has_right)  # type: ignore

        return (await self.setupHapConnection(self.ref_left), await self.setupHapConnection(self.ref_right))

    @asynchronous
    async def test_get_features(self) -> None:
        (dut_connection_to_ref, _) = await self.setup_monaural()

        features = hap.HearingAidFeatures_from_bytes(
            (await self.hap_grpc.GetFeatures(connection=dut_connection_to_ref)).features)  # type: ignore
        AssertThat(features).IsEqualTo(self.has_left.server_features)  # type: ignore

    @asynchronous
    async def test_get_preset(self) -> None:
        (dut_connection_to_ref, _) = await self.setup_monaural()

        await self.assertIdentiqPresetInDutAndRef(dut_connection_to_ref)

    @asynchronous
    async def test_preset__remove_preset__verify_dut_is_updated(self) -> None:
        (dut_connection_to_ref, _) = await self.setup_monaural()

        await self.assertIdentiqPresetInDutAndRef(dut_connection_to_ref)

        await self.has_left.delete_preset(unavailable_preset.index)
        await asyncio.sleep(3)  # TODO wait event

        await self.assertIdentiqPresetInDutAndRef(dut_connection_to_ref)

    @asynchronous
    async def test_set_active_preset(self) -> None:
        (dut_connection_to_ref, _) = await self.setup_monaural()

        await self.hap_grpc.SetActivePreset(connection=dut_connection_to_ref, index=bar_preset.index)
        await asyncio.sleep(1)  # TODO wait event
        assert (await
                self.hap_grpc.GetActivePreset(connection=dut_connection_to_ref)).preset_record.index == bar_preset.index
        await self.hap_grpc.SetActivePreset(connection=dut_connection_to_ref, index=foo_preset.index)
        await asyncio.sleep(1)  # TODO wait event
        assert (await
                self.hap_grpc.GetActivePreset(connection=dut_connection_to_ref)).preset_record.index == foo_preset.index

    # @asynchronous
    # async def test_set_active_when_disconnecting(self) -> None:
    #     # (dut_connection_to_ref, ref_connection_to_dut) = await self.setup_monaural()
    #     (dut_connection_to_ref_left, dut_connection_to_ref_right) = await self.setup_binaural()
    #     await self.assertIdentiqPresetInDutAndRef(dut_connection_to_ref_left)

    #     # preliminary check to be sure we are setting a new & different preset
    #     active_preset = (await self.hap_grpc.GetActivePreset(connection=dut_connection_to_ref_left)).preset_record
    #     assert active_preset.index == foo_preset.index

    #     await self.hap_grpc.SetActivePreset(connection=dut_connection_to_ref_left, index=bar_preset.index)
    #     await self.dut.aio.host.Disconnect(connection=dut_connection_to_ref_left)
    #     # await self.ref_left.aio.host.Disconnect(connection=ref_dut)
    #     await asyncio.gather(self.ref_left.reset())

    @asynchronous
    async def test_set_active_when_disconnecting(self) -> None:
        (dut_connection_to_ref, ref_connection_to_dut) = await self.setup_monaural()
        # (dut_connection_to_ref_left, dut_connection_to_ref_right) = await self.setup_binaural()
        await self.assertIdentiqPresetInDutAndRef(dut_connection_to_ref)

        # preliminary check to be sure we are setting a new & different preset
        active_preset: grpcPresetRecord = (await self.hap_grpc.GetActivePreset(connection=dut_connection_to_ref
                                                                              )).preset_record
        assert active_preset.index == foo_preset.index

        await asyncio.gather(self.hap_grpc.SetActivePreset(connection=dut_connection_to_ref, index=bar_preset.index),
                             self.ref_left.aio.host.Disconnect(connection=ref_connection_to_dut))
        await asyncio.sleep(3)  # TODO wait event
