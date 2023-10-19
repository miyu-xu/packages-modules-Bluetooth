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

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from pandora.host_pb2 import DISCOVERABLE_GENERAL
from bumble.hci import HCI_Write_Extended_Inquiry_Response_Command
from mobly import base_test, test_runner, signals
from mobly.asserts import assert_equal, assert_not_equal, assert_fail  # type: ignore
from typing import Optional


class GapTest(base_test.BaseTestClass):  # type: ignore[misc]

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

        await asyncio.gather(self.dut.reset(), self.ref.reset())

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()


    @avatar.parameterized(
        # TODO: try to cover all android branches with malformed eir
        (b'123',),
        (b'\x03\x01\x02\x03',),
    )
    def test_malformed_eir(self, eir: bytes) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip('this test require a Bumble reference device')
        
        self.ref.device.host.send_command_sync(
            HCI_Write_Extended_Inquiry_Response_Command(  # type: ignore
                fec_required=0,
                extended_inquiry_response=b'\23'
            )
        )

        self.ref.host.SetDiscoverabilityMode(mode=DISCOVERABLE_GENERAL)

        for report in self.dut.host.Inquiry(timeout=15.0):
            if report.address == self.ref.address:
                assert_not_equal(eir, report.data)
                return

        assert_fail("No inquiry response from Bumble")

    @avatar.parameterized(
        # TODO: try to cover all android branches with good eir
        (b'\x03\x01\x02\x03',),
    )
    def test_eir(self, eir: bytes) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip('this test require a Bumble reference device')
        
        self.ref.device.host.send_command_sync(
            HCI_Write_Extended_Inquiry_Response_Command(  # type: ignore
                fec_required=0,
                extended_inquiry_response=b'\23'
            )
        )

        self.ref.host.SetDiscoverabilityMode(mode=DISCOVERABLE_GENERAL)

        for report in self.dut.host.Inquiry(timeout=15.0):
            if report.address == self.ref.address:
                assert_equal(eir, report.data)
                return

        assert_fail("No inquiry response from Bumble")


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
