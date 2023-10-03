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

# from bumble.hci import HCI_PIN_TYPE_FIXED

from mobly import base_test
from mobly.asserts import assert_equal, fail

from avatar import (
    asynchronous,
    PandoraDevice,
    PandoraDevices,
    BumblePandoraDevice,
)

from pandora.security_pb2 import (
    LEVEL2,
    PairingEventAnswer
)

import bumble
from bumble.hci import Address

class LegacyTestClass(base_test.BaseTestClass):

    def _setup_devices(self, ref, dut) -> None:

        ref.config.setdefault('classic_enabled', True)
        # dual mode device is not supported in Pandora server
        ref.config.setdefault('le_enabled', False)
        ref.config.setdefault('classic_ssp_enabled', False)

        ref.config.setdefault(
                    'server',
                    {
                        # this is needed for bumble to pair with BR/EDR
                        # legacy pairing
                        'io_capability': 'keyboard_input_only',
                        # these are useless for legacy pairing
                        # 'pairing_bonding_enable': False,
                        # 'pairing_mitm_enable': False,
                    },
                )
        # ref.config.setdefault('classic_pin_type', HCI_PIN_TYPE_FIXED)

    @asynchronous
    async def setup_class(self) -> None:
        # what should be done here?
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

    async def _start_acl_connection(self, initiator: PandoraDevice,
                                          responder: PandoraDevice):
        return await asyncio.gather(
            initiator.aio.host.WaitConnection(address=responder.address),
            responder.aio.host.Connect(address=initiator.address),
        )

    @asynchronous
    async def test_dedicated_pairing(self) -> None:
        '''
            Test User initiated pairing

            see:
            BLUETOOTH CORE SPECIFICATION Version 5.3 | Vol 2, Part F page 731
            Step 7a
        '''

        android_pairing = self.dut.aio.security.OnPairing()
        bumble_pairing = self.ref.aio.security.OnPairing()

        # first initiate an ACL connection from bumble to android
        android_res, bumble_res = await self._start_acl_connection(self.dut,
                                                                   self.ref)

        assert_equal(android_res.result_variant(), 'connection')
        assert_equal(bumble_res.result_variant(), 'connection')

        async def start_pairing():
            return await asyncio.gather(
                            self.dut.aio.security.WaitSecurity(
                                    connection=android_res.connection,
                                    classic=LEVEL2),
                            self.ref.aio.security.Secure(
                                    connection=bumble_res.connection,
                                    classic=LEVEL2)
                        )

        # bumble initiates the pairing
        pairing_task = asyncio.create_task(start_pairing())

        # try:

        expected_pairing_method = 'pin_code_request'
        pairing_pin_code = b'123456'
        # Step 1: Bumble (initiator) receives pin code request
        bumble_pairing_fut = asyncio.create_task(anext(bumble_pairing))
        bumble_ev = await asyncio.wait_for(bumble_pairing_fut, timeout=10.0)
        logging.debug(f'bumble_ev.method_variant():{bumble_ev.method_variant()}')
        assert_equal(bumble_ev.method_variant(), expected_pairing_method)
        bumble_ev_ans = PairingEventAnswer(event=bumble_ev,
                                           pin=pairing_pin_code)

        # accept pairing on bumble with pairing pin code
        bumble_pairing.send_nowait(bumble_ev_ans)

        # step 2: Android (responder) receives pin code request
        android_pairing_fut = asyncio.create_task(anext(android_pairing))
        android_ev = await asyncio.wait_for(android_pairing_fut, timeout=5.0)

        logging.debug(f'android_ev.method_variant():{android_ev.method_variant()}')
        assert_equal(android_ev.method_variant(), expected_pairing_method)
        android_ev_ans = PairingEventAnswer(event=android_ev,
                                            pin=pairing_pin_code)
        # accept pairing on bumble with pairing pin code
        android_pairing.send_nowait(android_ev_ans)

        android_pairing_res, bumble_pairing_res = await asyncio.wait_for(pairing_task, timeout=5.0)
        logging.debug(f'Pairing result: {android_pairing_res.result_variant()}/{bumble_pairing_res.result_variant()}')

        # verify that pairing succeeded
        assert_equal(android_pairing_res.result_variant(), 'success')
        assert_equal(bumble_pairing_res.result_variant(), 'success')

        # except:
        #    pass


    @asynchronous
    async def test_general_pairing(self) -> None:

        expected_pairing_method = 'pin_code_request'
        pairing_pin_code = b'123456'

        android_pairing = self.dut.aio.security.OnPairing()
        bumble_pairing = self.ref.aio.security.OnPairing()

        # first initiate an ACL connection from bumble to android
        # TODO: clarify which level android is running on
        android_res, bumble_res = await self._start_acl_connection(self.dut,
                                                                   self.ref)

        # Try accessing Android secure services from bumble
        # use bumble API to get the underlying API to get the ACL connection
        # as l2cap APIs in bumble are based on it
        android_addr = Address.from_string_for_transport(str(self.dut.address), Address.PUBLIC_DEVICE_ADDRESS)
        bumble_acl_connection = self.ref.device.find_connection_by_bd_addr(android_addr)

        async def access_l2cap_service(psm):
            connector = bumble_acl_connection.create_l2cap_connector(psm)
            return await connector()

        # start accessing hid interrupt service
        hid_interrupt_psm = 0x13
        hid_interrupt_access_tsk = asyncio.create_task(access_l2cap_service(hid_interrupt_psm))

        # android will initiate pairing
        android_pairing_fut = asyncio.create_task(anext(android_pairing))
        android_ev = await asyncio.wait_for(android_pairing_fut, timeout=5.0)
        logging.debug(f'android_ev.method_variant():{android_ev.method_variant()}')
        assert_equal(android_ev.method_variant(), expected_pairing_method)

        android_ev_ans = PairingEventAnswer(event=android_ev,
                                            pin=pairing_pin_code)
        android_pairing.send_nowait(android_ev_ans)

        # bumble is the responder
        bumble_pairing_fut = asyncio.create_task(anext(bumble_pairing))
        bumble_ev = await asyncio.wait_for(bumble_pairing_fut, timeout=5.0)
        logging.debug(f'bumble_ev.method_variant():{bumble_ev.method_variant()}')
        assert_equal(bumble_ev.method_variant(), expected_pairing_method)

        bumble_ev_ans = PairingEventAnswer(event=bumble_ev,
                                           pin=pairing_pin_code)

        bumble_pairing.send_nowait(bumble_ev_ans)

        try:
            connect_res = await asyncio.wait_for(hid_interrupt_access_tsk,
                                                timeout=5.0)
        except:
            fail("connection should have succeeded")
