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

from mobly import base_test

from avatar import (
    asynchronous,
    PandoraDevice,
    PandoraDevices,
    BumblePandoraDevice,
)

from pairing_tests.br_edr.acl_connection_builder_impl import BREDRAclConnectionBuilder
from pairing_tests.br_edr.pairing_processors_impl import BREDRJustworksPairingProcessor
from pairing_tests.br_edr.service_accessors_impl import BumbleHidServiceAccessor

from pairing_tests.decorators import add_common_tests

@add_common_tests(
    acl_connection_builder_classes=[BREDRAclConnectionBuilder],
    pairing_process_class=BREDRJustworksPairingProcessor,
    service_accessor_classes=[BumbleHidServiceAccessor],
)
class BREDRNoOutputNoInputTestClass(base_test.BaseTestClass):

    def _setup_devices(self, ref, dut) -> None:

        ref.config.setdefault('classic_enabled', True)
        # dual mode device is not supported in Pandora server
        # we need to explicitly disable le here
        ref.config.setdefault('le_enabled', False)
        ref.config.setdefault('classic_ssp_enabled', True)
        ref.config.setdefault('classic_sc_enabled', False)

        ref.config.setdefault(
                    'server',
                    {
                        'pairing_sc_enable': False,
                        'pairing_mitm_enable': False,
                        'pairing_bonding_enable': True,
                        # Android IO CAP: Display_YESNO
                        # Ref IO CAP:
                        'io_capability': 'no_input_no_output',
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

    def teardown_test(self):
        pass