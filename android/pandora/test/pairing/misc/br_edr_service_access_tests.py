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

import asyncio
import logging

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from avatar.aio import asynchronous
from bumble.colors import color
from bumble.core import BT_L2CAP_PROTOCOL_ID, BT_RFCOMM_PROTOCOL_ID, UUID, ConnectionError, ProtocolError
from bumble.hci import Address
from bumble.l2cap import ClassicChannelSpec
from bumble.rfcomm import Client
from bumble.sdp import (
    SDP_ADDITIONAL_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_ALL_ATTRIBUTES_RANGE,
    SDP_BROWSE_GROUP_LIST_ATTRIBUTE_ID,
    SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_RECORD_HANDLE_ATTRIBUTE_ID,
    Client as SDPClient,
)
from mobly import base_test
from mobly.asserts import assert_equal, assert_is_not_none, assert_raises, fail
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

            bumble_ev = await asyncio.wait_for(bumble_pairing_fut, timeout=120.0)
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

    def _parse_rfcomm_channel_from_sdp_service_attributes(self, attributes):
        '''
        The SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID attribute of an insecure
        rfcomm service record should look like this
        id=SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
        value=SEQUENCE([
                SEQUENCE([UUID(UUID-16:0100 (L2CAP))]),
                SEQUENCE([
                    UUID(UUID-16:0003 (RFCOMM)),
                    UNSIGNED_INTEGER(7#1)])
                ])
        '''

        for attribute in attributes:
            print(f"attribute: {attribute.to_string(with_colors=True)}")
            if attribute.id == SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID and len(attribute.value.value) >= 2:
                proto0 = attribute.value.value[0]
                proto1 = attribute.value.value[1]

                if proto0.value[0].value == BT_L2CAP_PROTOCOL_ID and proto1.value[0].value == BT_RFCOMM_PROTOCOL_ID:
                    return proto1.value[1].value

        return None

    async def _lookup_rfcomm_channel_with_sdp(self, uuid):
        sdp_client = SDPClient(self.ref.device)
        await sdp_client.connect(self.acl_connection)

        service_record_handles = await sdp_client.search_services([UUID(uuid)])

        if len(service_record_handles) < 1:
            await sdp_client.disconnect()
            raise Exception(color(f'service not found on peer device!!!!', 'red'))

        ret = None
        for service_record_handle in service_record_handles:
            attributes = await sdp_client.get_attributes(service_record_handle, [SDP_ALL_ATTRIBUTES_RANGE])

            print(color(f'SERVICE {service_record_handle:04X} attributes:', 'yellow'))
            ret = self._parse_rfcomm_channel_from_sdp_service_attributes(attributes)
            if ret is None:
                continue
            else:
                break

        assert_is_not_none(ret)
        await sdp_client.disconnect()
        return ret

    @asynchronous
    async def test_access_rfcomm_mx_insecure_service(self):
        uuid = "F6FB4732-A802-487D-A9FA-9664D5C91F13"
        name = "test_rfcomm_server"

        # StartServer implementation on Android uses
        # listenUsingInsecureRfcommWithServiceRecord
        server_resp = await self.dut.aio.rfcomm.StartServer(name=name, uuid=uuid)
        # logging.debug(f'server_connection:{server_resp.connection}')

        rfc_channel = await self._lookup_rfcomm_channel_with_sdp(uuid)
        rfcomm_client = Client(self.ref.device, self.acl_connection)
        rfcomm_mux = await rfcomm_client.start()

        try:
            session = await rfcomm_mux.open_dlc(rfc_channel)
        except:
            fail("access to insecure rfcomm service should be allowed")

    @asynchronous
    async def test_access_hid_control_service(self):
        # hid control service (secure)
        # should be blocked
        with assert_raises(ProtocolError):
            hid_control_psm = 0x0011
            connector_hid_control = self.acl_connection.create_l2cap_channel(
                spec=ClassicChannelSpec(psm=hid_control_psm)
            )
            chan_hid_control = await connector_hid_control

    @asynchronous
    async def test_access_hid_interrupt_service(self):
        # HID interrupt service (secure)
        # should be blocked
        with assert_raises(ProtocolError):
            hid_interrupt_psm = 0x0013
            connector_hid_interrupt = self.acl_connection.create_l2cap_channel(
                spec=ClassicChannelSpec(psm=hid_interrupt_psm)
            )
            chan_hid_interrupt = await connector_hid_interrupt
