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
from avatar.pandora_client import PandoraClient

from mobly import base_test, test_runner

from typing import Optional


class HidTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraClient
    ref: PandoraClient

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref = self.devices

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    def test_report(self) -> None:
        from pandora_experimental.hid_grpc import HID, HidReportType

        HID(self.ref.channel).SendHostReport(
            address=self.dut.address,
            report_type=HidReportType.HID_REPORT_TYPE_INPUT,
            report="pause cafe"
        )


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
