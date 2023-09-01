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

from avatar import BumblePandoraDevice
from avatar.aio import asynchronous
from bumble import smp
from bumble.pairing import PairingDelegate
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_true  # type: ignore
from pandora.host_pb2 import RANDOM, DataTypes
from pandora.security_pb2 import LE_LEVEL3
from .smp_test_base import SmpTestBase


# SMP Security Request with MITM on encrypted link
#
# Test steps
# 1. Connect over LE
# 2. Perform authenticated LE pairing
# 3. Disconnect
# 4. Reconnect as central
# 5. Wait for the link to be encrypted
# 6. Send SMP Security Request with MITM protection requirement from the peripheral
#
# Expectation: The link is encrypted again. No repairing.
class SmpTestMitmSecReqOnEnc(SmpTestBase):  # type: ignore[misc]

    @asynchronous
    async def test_mitm_sec_req_on_enc(self) -> None:
        if isinstance(self.ref, BumblePandoraDevice):
            io_capability = PairingDelegate.IoCapability.DISPLAY_OUTPUT_AND_KEYBOARD_INPUT
            self.ref.server_config.io_capability = io_capability

        advertisement = self.ref.aio.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=RANDOM,
            data=DataTypes(manufacturer_specific_data=b'pause cafe'),
        )

        scan = self.dut.aio.host.Scan(own_address_type=RANDOM)
        ref = await anext((x async for x in scan if b'pause cafe' in x.data.manufacturer_specific_data))
        scan.cancel()

        asyncio.create_task(self.handle_pairing_events())
        (dut_ref_res, ref_dut_res) = await asyncio.gather(
            self.dut.aio.host.ConnectLE(own_address_type=RANDOM, **ref.address_asdict()),
            anext(aiter(advertisement)),
        )

        advertisement.cancel()
        ref_dut, dut_ref = ref_dut_res.connection, dut_ref_res.connection
        assert_is_not_none(dut_ref)
        assert dut_ref

        # Pair with MITM requirements
        (secure, wait_security) = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref, le=LE_LEVEL3),
            self.ref.aio.security.WaitSecurity(connection=ref_dut, le=LE_LEVEL3),
        )

        assert_equal(secure.result_variant(), 'success')
        assert_equal(wait_security.result_variant(), 'success')

        # Disconnect
        await asyncio.gather(
            self.ref.aio.host.Disconnect(connection=ref_dut),
            self.dut.aio.host.WaitDisconnection(connection=dut_ref),
        )

        advertisement = self.ref.aio.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=RANDOM,
            data=DataTypes(manufacturer_specific_data=b'pause cafe'),
        )

        scan = self.dut.aio.host.Scan(own_address_type=RANDOM)
        ref = await anext((x async for x in scan if b'pause cafe' in x.data.manufacturer_specific_data))
        scan.cancel()

        (dut_ref_res, ref_dut_res) = await asyncio.gather(
            self.dut.aio.host.ConnectLE(own_address_type=RANDOM, **ref.address_asdict()),
            anext(aiter(advertisement)),
        )
        ref_dut, dut_ref = ref_dut_res.connection, dut_ref_res.connection

        # Wait for the link to get encrypted
        connection = self.ref.device.lookup_connection(int.from_bytes(ref_dut.cookie.value, 'big'))

        def on_connection_encryption_change():
            self.ref.device.smp_manager.request_pairing(connection)

        connection.on('connection_encryption_change', on_connection_encryption_change)

        # Fail if repairing is initiated
        fut = asyncio.get_running_loop().create_future()

        class Session(smp.Session):

            def on_smp_pairing_request_command(self, command: smp.SMP_Pairing_Request_Command) -> None:
                nonlocal fut
                fut.set_result(False)

        self.ref.device.smp_session_proxy = Session

        # Pass if the link is encrypted again
        def on_connection_encryption_key_refresh():
            nonlocal fut
            fut.set_result(True)

        connection.on('connection_encryption_key_refresh', on_connection_encryption_key_refresh)

        assert_true(await fut, "Repairing initiated")
