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
import logging

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from avatar.aio import asynchronous
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_false  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_true  # type: ignore
from pandora.security_pb2 import LEVEL2
from typing import Optional


class A2dpTest(base_test.BaseTestClass):  # type: ignore[misc]
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

    @asynchronous
    async def test_yolo(self) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip("")
        ref_dut = await self.ref.aio.host.Connect(address=self.dut.address)
        assert ref_dut and ref_dut.connection
        await self.ref.aio.security.Secure(connection=ref_dut.connection, classic=LEVEL2)

        # Retrieve Bumble connection object from Pandora connection token
        connection_handle = int.from_bytes(ref_dut.connection.cookie.value, 'big')
        connection = self.ref.device.lookup_connection(connection_handle)  # type: ignore

        # 1. Open AVRCP L2CAP channel
        avrcp = await self.ref.device.l2cap_channel_manager.connect(connection, psm=0x0017)  # type: ignore
        self.ref.log.info(f"AVRCP: {avrcp}")

        # 2. Wait for AVDTP L2CAP channel
        avdtp_future = asyncio.get_running_loop().create_future()
        self.ref.device.l2cap_channel_manager.register_server(0x0019, avdtp_future.set_result)
        avdtp = await asyncio.wait_for(avdtp_future, timeout=5.0)
        self.ref.log.info(f"AVDTP: {avdtp}")


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
