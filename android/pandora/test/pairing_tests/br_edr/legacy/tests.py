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
import bumble
import logging
import time

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices, asynchronous
from bumble.hci import Address
from bumble.hid import HID_CONTROL_PSM
from bumble.l2cap import ClassicChannelSpec
from mobly import base_test
from mobly.asserts import assert_equal
from pairing_tests.br_edr.acl_connection_builder_impl import BREDRAclConnectionBuilder
from pairing_tests.br_edr.pairing_processors_impl import BREDRLegacyPairingProcessor
from pairing_tests.br_edr.service_accessors_impl import BumbleHidServiceAccessor

from pairing_tests.decorators import add_common_tests

from pandora.security_pb2 import LEVEL2, PairingEventAnswer
from typing import Any, Literal, Optional, Tuple, Union


@add_common_tests(
    acl_connection_builder_classes=[BREDRAclConnectionBuilder],
    pairing_process_class=BREDRLegacyPairingProcessor,
    service_accessor_classes=[BumbleHidServiceAccessor],
)
class BREDRLegacyTestClass(base_test.BaseTestClass):
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    def _setup_devices(self, ref, dut) -> None:
        ref.config.setdefault('classic_enabled', True)
        # dual mode device is not supported in Pandora server
        # we need to explicitly disable le here
        ref.config.setdefault('le_enabled', False)
        ref.config.setdefault('classic_ssp_enabled', False)

        ref.config.setdefault(
            'server',
            {
                # Android io_capability: display_yesno
                'io_capability': 'keyboard_input_only',
            },
        )

    @asynchronous
    async def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        self._setup_devices(self.ref, self.dut)

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())
        await self.dut.aio.security_storage.DeleteBond(public=self.ref.address)
        await self.ref.aio.security_storage.DeleteBond(public=self.dut.address)

    def teardown_test(self):
        time.sleep(5)
