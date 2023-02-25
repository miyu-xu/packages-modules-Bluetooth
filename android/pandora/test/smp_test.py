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

from avatar import PandoraDevices
from avatar.aio import asynchronous
from avatar.pandora_client import BumblePandoraClient, PandoraClient
from bumble.smp import PairingDelegate, PairingConfig
from concurrent import futures
from contextlib import suppress
from mobly import base_test, test_runner
from mobly.asserts import assert_equal  # type: ignore
from pandora.host_grpc import DataTypes, OwnAddressType
from pandora.security_grpc import LESecurityLevel, PairingEventAnswer
from typing import NoReturn, Optional, Any


class SMPTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraClient
    ref: BumblePandoraClient

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        dut, ref = self.devices
        assert isinstance(ref, BumblePandoraClient)
        self.dut, self.ref = dut, ref

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def handle_pairing_events(self) -> NoReturn:
        dut_pairing_stream = self.dut.aio.security.OnPairing()
        try:
            while True:
                dut_pairing_event = await (anext(dut_pairing_stream))
                dut_pairing_stream.send_nowait(PairingEventAnswer(
                    event=dut_pairing_event,
                    confirm=True,
                ))
        finally:
            dut_pairing_stream.cancel()

    async def handle_le_pairing(self, dut_address_type, ref_address_type) -> Any:
        advertisement = self.ref.aio.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=ref_address_type,
            data=DataTypes(manufacturer_specific_data=b'pause cafe'),
        )

        scan = self.dut.aio.host.Scan(own_address_type=dut_address_type)
        ref = await anext((x async for x in scan if b'pause cafe' in x.data.manufacturer_specific_data))
        scan.cancel()
        assert ref

        pairing = asyncio.create_task(self.handle_pairing_events())
        (dut_ref_res, ref_dut_res) = await asyncio.gather(
            self.dut.aio.host.ConnectLE(own_address_type=dut_address_type, **ref.address_asdict()),
            anext(aiter(advertisement)),
        )

        advertisement.cancel()
        ref_dut, dut_ref = ref_dut_res.connection, dut_ref_res.connection
        assert ref_dut and dut_ref

        (secure, wait_security) = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref, le=LESecurityLevel.LE_LEVEL3),
            self.ref.aio.security.WaitSecurity(connection=ref_dut, le=LESecurityLevel.LE_LEVEL3),
        )

        pairing.cancel()
        with suppress(asyncio.CancelledError, futures.CancelledError):
            await pairing

        assert_equal(secure.result_variant(), 'success')
        assert_equal(wait_security.result_variant(), 'success')

        await asyncio.gather(
            self.ref.aio.host.Disconnect(connection=ref_dut),
            self.dut.aio.host.WaitDisconnection(connection=dut_ref),
        )
        return ref

    @asynchronous
    async def test_le_pairing__no_bonding(self) -> None:
        setattr(
            self.ref.device.smp_manager, 'pairing_config_factory', lambda _: PairingConfig(
                mitm=False,
                bonding=False,
                delegate=PairingDelegate(io_capability=PairingDelegate.NO_OUTPUT_NO_INPUT),
            ))

        le_pairing = asyncio.create_task(
            self.handle_le_pairing(
                dut_address_type=OwnAddressType.RANDOM,
                ref_address_type=OwnAddressType.RANDOM,
            ))
        ref = await le_pairing
        is_bonded = self.dut.security_storage.IsBonded(**ref.address_asdict())
        assert is_bonded

    @asynchronous
    async def test_le_pairing__twice_with_same_device(self) -> None:
        # Pair with same device 2 times.
        # Ref device advertises with different random address but uses same identity address
        le_pairing = asyncio.create_task(
            self.handle_le_pairing(
                dut_address_type=OwnAddressType.RANDOM,
                ref_address_type=OwnAddressType.RANDOM,
            ))
        ref1 = await le_pairing
        is_bonded = self.dut.security_storage.IsBonded(**ref1.address_asdict())
        assert is_bonded

        await self.ref.reset()

        le_pairing = asyncio.create_task(
            self.handle_le_pairing(
                dut_address_type=OwnAddressType.RANDOM,
                ref_address_type=OwnAddressType.RANDOM,
            ))
        ref2 = await le_pairing
        is_bonded = self.dut.security_storage.IsBonded(**ref2.address_asdict())
        assert is_bonded


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
