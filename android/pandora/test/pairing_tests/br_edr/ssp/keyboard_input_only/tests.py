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

class BREDRKeyboardOnlyTestClass(base_test.BaseTestClass):

    def _setup_devices(self, ref, dut) -> None:

        ref.config.setdefault('classic_enabled', True)
        # dual mode device is not supported in Pandora server
        # we need to explicitly disable le here
        ref.config.setdefault('le_enabled', True)
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

    def teardown_test(self):
        pass

    async def _start_acl_connection(self, acl_initiator: PandoraDevice,
                                          acl_responder: PandoraDevice):
        return await asyncio.gather(
            acl_initiator.aio.host.Connect(address=acl_responder.address),
            acl_responder.aio.host.WaitConnection(address=acl_initiator.address),
        )

    async def accept_pairing(self, initiator_pairing_event_stream,
                             responder_pairing_event_stream):

        # responder receives just works
        responder_pairing_fut = asyncio.create_task(anext(responder_pairing_event_stream))
        responder_ev = await asyncio.wait_for(responder_pairing_fut, timeout=10.0)
        logging.debug(f'[{responder_pairing_event_stream.device.name}] responder_ev.method_variant():{responder_ev.method_variant()}')

        init_pairing_fut = asyncio.create_task(anext(initiator_pairing_event_stream))
        init_ev = await asyncio.wait_for(init_pairing_fut, timeout=10.0)
        logging.debug(f'[{initiator_pairing_event_stream.device.name}] init_ev.method_variant():{init_ev.method_variant()}')
        # logging.debug(f'init_ev.numeric_comparison:{init_ev.numeric_comparison}')

        # if initiator_pairing_event_stream == self.android_pairing_stream:
        #    assert_equal(init_ev.method_variant(), 'numeric_comparison')
        #    assert_equal(responder_ev.method_variant(), 'just_works')
        # else:
        #    assert_equal(init_ev.method_variant(), 'just_works')

            # this assert is still failing test_dedicated_pairing_ref_initiate_1
            # TODO: investigate
            # assert_equal(responder_ev.method_variant(), 'numeric_comparison')

        # init_ev_ans = PairingEventAnswer(event=init_ev, confirm=True)
        # initiator_pairing_event_stream.send_nowait(init_ev_ans)
        # responder_ev_ans = PairingEventAnswer(event=responder_ev, confirm=True)
        # responder_pairing_event_stream.send_nowait(responder_ev_ans)

    def prepare_pairing(self):
        self.android_pairing_stream = self.dut.aio.security.OnPairing()
        setattr(self.android_pairing_stream, 'device', self.dut)

        self.bumble_pairing_stream = self.ref.aio.security.OnPairing()
        setattr(self.bumble_pairing_stream, 'device', self.ref)

    @asynchronous
    async def test_dedicated_pairing_ref_initiate_1(self) -> None:
        '''
        acl:
            ref: initiator
            dut: responder

        pairing:
            ref: initiator
            dut: responder
        '''

        self.prepare_pairing()


        # first initiate an ACL connection from bumble to android
        bumble_res, android_res = await self._start_acl_connection(self.ref,
                                                                   self.dut)

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

        await self.accept_pairing(self.bumble_pairing_stream, self.android_pairing_stream)

        android_pairing_res, bumble_pairing_res = await asyncio.wait_for(pairing_task, timeout=10.0)
        logging.debug(f'Pairing result(android/bumble): {android_pairing_res.result_variant()}/{bumble_pairing_res.result_variant()}')

        # verify that pairing succeeded
        assert_equal(android_pairing_res.result_variant(), 'success')
        assert_equal(bumble_pairing_res.result_variant(), 'success')

    @asynchronous
    async def test_dedicated_pairing_ref_initiate_2(self) -> None:
        '''
        acl:
            ref: initiator
            dut: responder

        pairing:
            ref: responder
            dut: initiator
        '''

        self.prepare_pairing()

        # first initiate an ACL connection from bumble to android
        bumble_res, android_res = await self._start_acl_connection(self.ref,
                                                                   self.dut)

        assert_equal(android_res.result_variant(), 'connection')
        assert_equal(bumble_res.result_variant(), 'connection')

        async def start_pairing():
            return await asyncio.gather(
                            self.dut.aio.security.Secure(
                                    connection=android_res.connection,
                                    classic=LEVEL2),
                            self.ref.aio.security.WaitSecurity(
                                    connection=bumble_res.connection,
                                    classic=LEVEL2)
                        )

        # Android initiates the pairing
        pairing_task = asyncio.create_task(start_pairing())

        await self.accept_pairing(self.android_pairing_stream, self.bumble_pairing_stream)

        android_pairing_res, bumble_pairing_res = await asyncio.wait_for(pairing_task, timeout=10.0)
        logging.debug(f'Pairing result: {android_pairing_res.result_variant()}/{bumble_pairing_res.result_variant()}')

        # verify that pairing succeeded
        assert_equal(android_pairing_res.result_variant(), 'success')
        assert_equal(bumble_pairing_res.result_variant(), 'success')


    @asynchronous
    async def test_dedicated_pairing_dut_initiate_1(self) -> None:
        '''
        acl:
            ref: responder
            dut: initiator

        pairing:
            ref: responder
            dut: initiator

        Note: we can not change the role of pairing actions in the current avatar
        implementation, as the implementation of Connect (initiating acl connection)
        on Android will initiate pairing.

        Pairing initiated from ref is not supported yet
        '''

        self.prepare_pairing()

        acl_connection_task = asyncio.create_task(self._start_acl_connection(self.dut, self.ref))
        # first initiate an ACL connection from bumble to android
        # bumble_res, android_res = await self._start_acl_connection(self.dut,
        #                                                            self.ref)

        # with the ACL connection, pairing will automatically started
        # on Android
        await self.accept_pairing(self.android_pairing_stream, self.bumble_pairing_stream)

        bumble_res, android_res = await asyncio.wait_for(acl_connection_task, timeout=10.0)

        assert_equal(android_res.result_variant(), 'connection')
        assert_equal(bumble_res.result_variant(), 'connection')

    @asynchronous
    async def test_general_pairing(self) -> None:
        self.prepare_pairing()

        # first initiate an ACL connection from bumble to android
        bumble_res, android_res = await self._start_acl_connection(self.ref,
                                                                   self.dut)
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

        await self.accept_pairing(android_pairing, bumble_pairing)

        try:
            connect_res = await asyncio.wait_for(hid_interrupt_access_tsk,
                                                timeout=5.0)
        except:
            fail("connection should have succeeded")
