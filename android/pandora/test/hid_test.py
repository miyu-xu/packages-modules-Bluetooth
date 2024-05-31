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
from bumble import pandora as bumble_server
from pandora_experimental.hid_grpc import HID
from pandora_experimental.hid_grpc_aio import HID as AioHID, add_HIDServicer_to_server
from pandora.host_pb2 import Connection as PandoraConnection
from pandora.security_pb2 import LEVEL2
from typing import Tuple
from bumble_experimental.hid import HIDService

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
        bumble_server.register_servicer_hook(
            lambda bumble, _, server: add_HIDServicer_to_server(HIDService(bumble.device), server)
        )
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
        HID(self.ref.channel).RegisterHidDevice()
        # Connect and pair
        dut_ref, ref_dut = await self.make_classic_connection()
        await self.make_classic_bond(dut_ref, ref_dut)

        await asyncio.sleep(3)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
