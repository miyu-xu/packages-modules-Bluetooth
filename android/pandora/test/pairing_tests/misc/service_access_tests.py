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
from bumble.core import ConnectionError, ProtocolError
from bumble.hci import Address
from bumble.l2cap import ClassicChannelSpec
from bumble.rfcomm import Client
from mobly import base_test
from mobly.asserts import assert_equal, fail, assert_raises

from pandora.security_pb2 import LEVEL2, PairingEventAnswer
from typing import Optional

class ServiceAccessTempBondingTest(base_test.BaseTestClass):  # type: ignore[misc]
    """
    This test verifies that access to secure services from a peer device via a
    temporary bonding is blocked, while insecure services are allowed.
    """

    devices: Optional[PandoraDevices] = None

    dut: PandoraDevice
    ref: PandoraDevice

    def _setup_bumble_device(self, device):
        if isinstance(device, BumblePandoraDevice):
            # enable BR/EDR
            device.config.setdefault('classic_enabled', True)
            device.config.setdefault('classic_ssp_enabled', True)
            device.config.setdefault('classic_sc_enabled', True)
            device.config.setdefault(
                'server',
                {
                    'io_capability': 'no_output_no_input',
                    # create a temp bonding
                    'pairing_bonding_enable': False,
                    'pairing_mitm_enable': False,
                    'pairing_sc_enable': True,
                },
            )
        else:
            fail(f"{device} is not a bumble device")

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        self._setup_bumble_device(self.ref)

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

        android_pairing = self.dut.aio.security.OnPairing()
        bumble_pairing = self.ref.aio.security.OnPairing()

        # first initiate an ACL connection from bumble to android
        android_res, bumble_res = await asyncio.gather(
            self.dut.aio.host.WaitConnection(address=self.ref.address),
            self.ref.aio.host.Connect(address=self.dut.address),
        )

        assert_equal(android_res.result_variant(), 'connection')
        assert_equal(bumble_res.result_variant(), 'connection')

        async def start_pairing():
            await asyncio.gather(
                self.dut.aio.security.WaitSecurity(connection=android_res.connection, classic=LEVEL2),
                self.ref.aio.security.Secure(connection=bumble_res.connection, classic=LEVEL2),
            )

        pairing_task = asyncio.create_task(start_pairing())

        try:
            # android_pairing_fut = asyncio.create_task(anext(android_pairing))
            bumble_pairing_fut = asyncio.create_task(anext(bumble_pairing))

            bumble_ev = await asyncio.wait_for(bumble_pairing_fut, timeout=5.0)
            assert_equal(bumble_ev.method_variant(), 'just_works')

            bumble_ev_answer = PairingEventAnswer(event=bumble_ev, confirm=True)

            # accept the pairing from bumble
            bumble_pairing.send_nowait(bumble_ev_answer)

            # pairing on android side is auto-accepted
            # so no pairing event will be triggered here
            # ignore it now
        except:
            fail('no exception should have happened during pairing')

        android_addr = Address.from_string_for_transport(str(self.dut.address), Address.PUBLIC_DEVICE_ADDRESS)
        self.acl_connection = self.ref.device.find_connection_by_bd_addr(android_addr)

    @asynchronous
    async def test_access_sdp_service(self):
        sdp_psm = 0x0001
        sdp_channel = self.acl_connection.create_l2cap_channel(spec=ClassicChannelSpec(psm=sdp_psm))
        try:
            chan_sdp = await sdp_channel
        except:
            fail("access to SDP service should be allowed")

    @asynchronous
    async def test_access_rfcomm_service(self):
        rfc_psm = 0x0003
        rfcomm_channel = self.acl_connection.create_l2cap_channel(spec=ClassicChannelSpec(psm=rfc_psm))
        try:
            chann_rfcomm = await rfcomm_channel
        except:
            fail("access to RFCOMM service should be allowed")

    @asynchronous
    async def test_access_rfcomm_mx_secure_service(self):
        rfcomm_client = Client(self.ref.device, self.acl_connection)
        rfcomm_mux = await rfcomm_client.start()

        # hfp rfcomm mx service
        # it is a secure service exposed in the layer of rfcomm
        # access should be blocked
        hfp_rfcomm_chan = 0x0002
        with assert_raises(ConnectionError):
            session = await rfcomm_mux.open_dlc(hfp_rfcomm_chan)

    @asynchronous
    async def test_access_rfcomm_mx_insecure_service(self):
        '''
        # TODO: this chan is not stable yet
        # need some API support from avatar
        '''

        '''
        rfcomm_client = Client(self.ref.device, self.acl_connection)
        rfcomm_mux = await rfcomm_client.start()

        insec_rfchan = 0x0007
        try:
            session = await rfcomm_mux.open_dlc(insec_rfchan)
        except ConnectionError:
            fail(f"access to rfcomm mx chann {insec_rfchan} should not be blocked")
            pass
        '''
        pass

    @asynchronous
    async def test_access_hid_control_service(self):
        # hid control service (secure)
        # should be blocked
        with assert_raises(ProtocolError):
            hid_control_psm = 0x0011
            connector_hid_control = self.acl_connection.create_l2cap_channel(spec=ClassicChannelSpec(psm=hid_control_psm))
            chan_hid_control = await connector_hid_control

    @asynchronous
    async def test_access_hid_interrupt_service(self):
        # HID interrupt service (secure)
        # should be blocked
        with assert_raises(ProtocolError):
            hid_interrupt_psm = 0x0013
            connector_hid_interrupt = self.acl_connection.create_l2cap_channel(spec=ClassicChannelSpec(psm=hid_interrupt_psm))
            chan_hid_interrupt = await connector_hid_interrupt
