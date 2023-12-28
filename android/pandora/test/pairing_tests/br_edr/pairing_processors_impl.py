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

from mobly.asserts import assert_equal, fail, assert_true
from pairing_tests.interfaces import IPairingProcessor
from pandora.security_pb2 import LEVEL2, PairingEventAnswer


class BREDRLegacyPairingProcessor(IPairingProcessor):
    async def process_pairing(self, pin_code1, pin_code2):
        expected_pairing_method = 'pin_code_request'

        # initiator receives pin code request
        init_pairing_fut = asyncio.create_task(anext(self._init_pairing_event_stream))
        init_ev = await asyncio.wait_for(init_pairing_fut, timeout=30.0)
        logging.debug(f'init_ev.method_variant():{init_ev.method_variant()}')
        assert_equal(init_ev.method_variant(), expected_pairing_method)
        init_ev_ans = PairingEventAnswer(event=init_ev, pin=pin_code1)

        # accept pairing on initator with pairing pin code
        self._init_pairing_event_stream.send_nowait(init_ev_ans)

        # responder receives pin code request
        responder_pairing_fut = asyncio.create_task(anext(self._resp_pairing_event_stream))
        responder_ev = await asyncio.wait_for(responder_pairing_fut, timeout=30.0)

        logging.debug(f'responder_ev.method_variant():{responder_ev.method_variant()}')
        assert_equal(responder_ev.method_variant(), expected_pairing_method)
        responder_ev_ans = PairingEventAnswer(event=responder_ev, pin=pin_code2)
        # accept pairing on bumble with pairing pin code
        self._resp_pairing_event_stream.send_nowait(responder_ev_ans)

    async def accept_pairing(self):
        await self.process_pairing(b'123456', b'123456')

    async def reject_pairing(self):
        await self.process_pairing(b'123456', b'654321')

class BREDRJustworksPairingProcessor(IPairingProcessor):

    async def process_pairing(self, accept):
        expected_pairing_method = 'just_works'

        responder_pairing_fut = asyncio.create_task(anext(self._resp_pairing_event_stream))
        responder_ev = await asyncio.wait_for(responder_pairing_fut, timeout=10.0)
        logging.debug(f'responder_ev.method_variant():{responder_ev.method_variant()}')
        assert_equal(responder_ev.method_variant(), expected_pairing_method)

        init_pairing_fut = asyncio.create_task(anext(self._init_pairing_event_stream))
        init_ev = await asyncio.wait_for(init_pairing_fut, timeout=10.0)
        logging.debug(f'init_ev.method_variant():{init_ev.method_variant()}')
        assert_equal(init_ev.method_variant(), expected_pairing_method)

        init_ev_ans = PairingEventAnswer(event=init_ev, confirm=True)
        self._init_pairing_event_stream.send_nowait(init_ev_ans)
        responder_ev_ans = PairingEventAnswer(event=responder_ev, confirm=True)
        self._resp_pairing_event_stream.send_nowait(responder_ev_ans)

    async def accept_pairing(self):
        await self.process_pairing(True)

    async def reject_pairing(self):
        await self.process_pairing(False)

class BREDRNumericComparisonPairingProcessor(IPairingProcessor):

    async def process_pairing(self, confirm):
        expected_pairing_method = 'numeric_comparison'

        # initiator receives numeric_comparison
        init_pairing_fut = asyncio.create_task(anext(self._init_pairing_event_stream))
        init_ev = await asyncio.wait_for(init_pairing_fut, timeout=10.0)
        logging.debug(f'init_ev.method_variant():{init_ev.method_variant()}')
        assert_equal(init_ev.method_variant(), expected_pairing_method)
        logging.debug(f'init_ev.numeric_comparison:{init_ev.numeric_comparison}')

        # responder receives numeric_comparison
        responder_pairing_fut = asyncio.create_task(anext(self._resp_pairing_event_stream))
        responder_ev = await asyncio.wait_for(responder_pairing_fut, timeout=10.0)
        logging.debug(f'responder_ev.method_variant():{responder_ev.method_variant()}')
        assert_equal(responder_ev.method_variant(), expected_pairing_method)
        logging.debug(f'responder_ev.numeric_comparison:{responder_ev.numeric_comparison}')

        _confirm = (responder_ev.numeric_comparison == init_ev.numeric_comparison)

        assert_true(_confirm, "numeric value received on both devices are not the same")

        logging.debug(f'confirm:{confirm}')

        # FIXME: we do not need respond on both side
        # TODO: remove one of them
        init_ev_ans = PairingEventAnswer(event=init_ev, confirm=confirm)
        # respond pairing based on numeric comparison on initiator
        self._init_pairing_event_stream.send_nowait(init_ev_ans)

        responder_ev_ans = PairingEventAnswer(event=responder_ev, confirm=confirm)
        # respond pairing based on numeric comparison on responder
        self._resp_pairing_event_stream.send_nowait(responder_ev_ans)

    async def accept_pairing(self):
        await self.process_pairing(True)

    async def reject_pairing(self):
        await self.process_pairing(False)
