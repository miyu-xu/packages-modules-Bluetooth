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
import logging

from avatar import PandoraDevice, PandoraDevices, BumbleDevice, asynchronous
from bumble.smp import PairingConfig

from mobly import base_test, test_runner
from pandora.host_pb2 import RANDOM, DataTypes
from pandora_experimental.gatt_grpc import GATT
from pandora.security_pb2 import PairingEventAnswer, LESecurityLevel

from bumble.gatt import Characteristic, Service

from typing import Optional


class GattTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: BumbleDevice

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        dut, ref = self.devices
        assert isinstance(ref, BumbleDevice)
        self.dut, self.ref = dut, ref

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def test_print_dut_gatt_services(self) -> None:
        advertise = self.ref.aio.host.Advertise(legacy=True, connectable=True)
        dut_ref = self.dut.host.ConnectLE(public=self.ref.address, own_address_type=RANDOM).connection
        advertise.cancel()

        assert dut_ref
        gatt = GATT(self.dut.aio.channel)
        services = gatt.DiscoverServices(dut_ref)
        self.dut.log.info(f'DUT services: {services}')

    async def test_print_ref_gatt_services(self) -> None:
        advertise = self.dut.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=RANDOM,
            data=DataTypes(manufacturer_specific_data=b'pause cafe'),
        )

        scan = self.ref.host.Scan()
        dut = next((x for x in scan if b'pause cafe' in x.data.manufacturer_specific_data))
        scan.cancel()

        ref_dut = (await self.ref.aio.host.ConnectLE(own_address_type=RANDOM, **dut.address_asdict())).connection
        advertise.cancel()

        assert ref_dut
        gatt = GATT(self.ref.aio.channel)
        services = gatt.DiscoverServices(ref_dut)
        self.ref.log.info(f'REF services: {services}')

    @asynchronous
    async def test_write_characteristic_while_pairing(self) -> None:
        SERVICE_UUID = "00005a00-0000-1000-8000-00805f9b34fb"
        CHARACTERISTIC_UUID = "00006a00-0000-1000-8000-00805f9b34fb"

        self.ref.device.pairing_config_factory = lambda _: PairingConfig(sc=True, mitm=False, bonding=True) # type: ignore

        service = Service(
            SERVICE_UUID,
            [
                Characteristic(
                    CHARACTERISTIC_UUID,
                    Characteristic.READ,
                    Characteristic.READ_REQUIRES_ENCRYPTION,
                    b"Hello, world!",
                ),
            ]
        )
        self.ref.device.add_service(service) # type:ignore

        dut_pairing_events = self.dut.aio.security.OnPairing()

        ref_advertisement = self.ref.aio.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=RANDOM,
            data=DataTypes(manufacturer_specific_data=b'target'),
        )

        scan = self.dut.aio.host.Scan()
        ref = await anext((x async for x in scan if b'target' in x.data.manufacturer_specific_data))
        scan.cancel()

        self.ref.log.info("connecting...")

        dut_connection_to_ref = (await self.dut.aio.host.ConnectLE(own_address_type=RANDOM, **ref.address_asdict())).connection
        assert dut_connection_to_ref

        self.ref.log.info("connected!")

        ref_connection_to_dut = (await anext(aiter(ref_advertisement))).connection

        ref_advertisement.cancel()

        gatt = GATT(self.dut.aio.channel)
        services = await gatt.DiscoverServices(dut_connection_to_ref)

        self.ref.log.info(f'REF services: {services}')

        assert ref_connection_to_dut
        async def ref_secure():
            return await self.ref.aio.security.Secure(connection=ref_connection_to_dut, le=LESecurityLevel.LE_LEVEL3)
        _ref_secure = asyncio.create_task(ref_secure())

        event = await anext(dut_pairing_events)

        async def dut_read():
            return await gatt.ReadCharacteristicsFromUuid(dut_connection_to_ref, CHARACTERISTIC_UUID, 1, 0xFFFF)
        dut_read_task = asyncio.create_task(dut_read())

        dut_pairing_events.send_nowait(PairingEventAnswer(event=event, confirm=True))

        # android pops up a second notif for some reason
        event = await anext(dut_pairing_events)
        dut_pairing_events.send_nowait(PairingEventAnswer(event=event, confirm=True))

        read_response = await dut_read_task

        self.ref.log.info(f'REF device name: {read_response}')


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
