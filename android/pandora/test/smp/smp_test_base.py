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

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from avatar.aio import asynchronous
from concurrent import futures
from contextlib import suppress
from mobly import base_test
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from pandora.host_pb2 import DataTypes, OwnAddressType, ScanningResponse
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer
from typing import NoReturn, Optional


class SmpTestBase(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    dut: PandoraDevice
    ref: PandoraDevice

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        # Enable BR/EDR mode for Bumble devices.
        for device in self.devices:
            if isinstance(device, BumblePandoraDevice):
                device.config.setdefault('classic_enabled', True)

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def handle_pairing_events(self) -> NoReturn:
        dut_pairing_stream = self.dut.aio.security.OnPairing()
        ref_pairing_stream = self.ref.aio.security.OnPairing()
        try:
            while True:
                dut_pairing_event = await (anext(dut_pairing_stream))

                if dut_pairing_event.method_variant() == 'passkey_entry_notification':
                    ref_pairing_event = await (anext(ref_pairing_stream))

                    assert_equal(ref_pairing_event.method_variant(), 'passkey_entry_request')
                    assert_is_not_none(dut_pairing_event.passkey_entry_notification)
                    assert dut_pairing_event.passkey_entry_notification is not None

                    ref_ev_answer = PairingEventAnswer(
                        event=ref_pairing_event,
                        passkey=dut_pairing_event.passkey_entry_notification,
                    )
                    ref_pairing_stream.send_nowait(ref_ev_answer)
                else:
                    dut_pairing_stream.send_nowait(PairingEventAnswer(
                        event=dut_pairing_event,
                        confirm=True,
                    ))
                    ref_pairing_event = await (anext(ref_pairing_stream))

                    ref_pairing_stream.send_nowait(PairingEventAnswer(
                        event=ref_pairing_event,
                        confirm=True,
                    ))

        finally:
            dut_pairing_stream.cancel()

    async def dut_pair(self, dut_address_type: OwnAddressType, ref_address_type: OwnAddressType) -> ScanningResponse:
        advertisement = self.ref.aio.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=ref_address_type,
            data=DataTypes(manufacturer_specific_data=b'pause cafe'),
        )

        scan = self.dut.aio.host.Scan(own_address_type=dut_address_type)
        ref = await anext((x async for x in scan if b'pause cafe' in x.data.manufacturer_specific_data))
        scan.cancel()

        pairing = asyncio.create_task(self.handle_pairing_events())
        (dut_ref_res, ref_dut_res) = await asyncio.gather(
            self.dut.aio.host.ConnectLE(own_address_type=dut_address_type, **ref.address_asdict()),
            anext(aiter(advertisement)),
        )

        advertisement.cancel()
        ref_dut, dut_ref = ref_dut_res.connection, dut_ref_res.connection
        assert_is_not_none(dut_ref)
        assert dut_ref

        (secure, wait_security) = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref, le=LE_LEVEL3),
            self.ref.aio.security.WaitSecurity(connection=ref_dut, le=LE_LEVEL3),
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
