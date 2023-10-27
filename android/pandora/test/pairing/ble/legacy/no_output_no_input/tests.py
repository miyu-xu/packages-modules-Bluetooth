# Copyright 2024 Google LLC
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

import logging

from mobly.asserts import assert_equal


from pairing.ble.test_base import BLEPairTestBaseWithGeneralAndDedicatedPairingTests

from pandora.security_pb2 import PairingEventAnswer


class BLELegNoInputNoOutputTestClass(BLEPairTestBaseWithGeneralAndDedicatedPairingTests):

    def _setup_devices(self) -> None:
        self.ref.config.setdefault('le_enabled', True)

        self.ref.config.setdefault('classic_enabled', False)
        self.ref.config.setdefault('classic_ssp_enabled', False)
        self.ref.config.setdefault('classic_sc_enabled', False)

        self.ref.config.setdefault(
            'server',
            {
                # legacy pairing
                'pairing_sc_enable': False,
                'pairing_mitm_enable': True,
                'pairing_bonding_enable': True,
                # Android IO CAP: Display_KBD
                # Ref IO CAP:
                'io_capability': 'no_output_no_input',
            },
        )

    async def accept_pairing(self):
        if self.initiator_pairing_event_stream == self.bumble_pairing_stream:
            confirm_stream = self.responder_pairing_event_stream
        else:
            confirm_stream = self.initiator_pairing_event_stream

        pairing_ev = await anext(confirm_stream)
        logging.debug(f'pairing_ev.method_variant():{pairing_ev.method_variant()}')
        assert_equal(pairing_ev.method_variant(), 'just_works')

        ans = PairingEventAnswer(event=pairing_ev, confirm=True)
        confirm_stream.send_nowait(ans)
