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
import avatar
import grpc
import logging

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from bumble import pandora as bumble_server
from bumble.gatt import Characteristic, Service
from bumble.l2cap import L2CAP_Control_Frame
from pandora.l2cap_pb2 import CreditBasedChannelRequest, WaitConnectionRequest, ConnectRequest, ConnectResponse, WaitConnectionResponse
from bumble.pairing import PairingConfig
from bumble_experimental.gatt import GATTService
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_not_in  # type: ignore
from mobly.asserts import assert_true  # type: ignore
from pandora.host_pb2 import RANDOM, Connection, DataTypes
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer, SecureResponse
from pandora_experimental.gatt_grpc import GATT
from pandora.l2cap_grpc_aio import L2CAP as AioL2CAP
from pandora_experimental.gatt_pb2 import SUCCESS, ReadCharacteristicsFromUuidResponse
from typing import Optional, Tuple

class L2capClientTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    def setup_class(self) -> None:
        # Register experimental bumble servicers hook.
        # bumble_server.register_servicer_hook(
        #    lambda bumble, _, server: add_GATTServicer_to_server(GATTService(bumble.device), server)
        # )

        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @avatar.asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def connect_dut_to_ref(self) -> Tuple[Connection, Connection]:
        ref_advertisement = self.ref.aio.host.Advertise(
            legacy=True,
            connectable=True,
        )

        dut_connection_to_ref = (
            await self.dut.aio.host.ConnectLE(public=self.ref.address, own_address_type=RANDOM)
        ).connection
        assert_is_not_none(dut_connection_to_ref)
        assert dut_connection_to_ref

        ref_connection_to_dut = (await anext(aiter(ref_advertisement))).connection
        ref_advertisement.cancel()

        return dut_connection_to_ref, ref_connection_to_dut

    @avatar.asynchronous
    async def test_l2cap_connect_disconnect(self) -> None:
        if isinstance(self.dut, BumblePandoraDevice):
            raise signals.TestSkip('TODO: b/273941061')
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip('Test require Bumble as reference device(s)')

        # disable MITM requirement on REF side (since it only does just works)
        self.ref.device.pairing_config_factory = lambda _: PairingConfig(  # type:ignore
            sc=True, mitm=False, bonding=True
        )
        # manually handle pairing on the DUT side
        dut_pairing_events = self.dut.aio.security.OnPairing()
        # set up connection
        dut_connection_to_ref, ref_connection_to_dut = await self.connect_dut_to_ref()

        # act: initiate pairing from REF side (send a security request)
        async def ref_secure() -> SecureResponse:
            return await self.ref.aio.security.Secure(connection=ref_connection_to_dut, le=LE_LEVEL3)

        ref_secure_task = asyncio.create_task(ref_secure())

        # wait for pairing to start
        event = await anext(dut_pairing_events)

         # now continue with pairing
        dut_pairing_events.send_nowait(PairingEventAnswer(event=event, confirm=True))

        # android pops up a second pairing notification for some reason, accept it
        event = await anext(dut_pairing_events)
        dut_pairing_events.send_nowait(PairingEventAnswer(event=event, confirm=True))

        # make sure pairing was successful
        ref_secure_res = await ref_secure_task
        assert_equal(ref_secure_res.result_variant(), 'success')

        dut_l2cap = AioL2CAP(self.dut.aio.channel);
        ref_l2cap = AioL2CAP(self.ref.aio.channel);
        # create client socket from dut
        #request = ConnectRequest(connection=dut_connection_to_ref,
        #                 le_credit_based=CreditBasedChannelRequest(spsm=0x80, mtu=2048, mps=2048, initial_credit=256))
        refResp = dut_l2cap.Connect(connection=dut_connection_to_ref,
                                            Lle_credit_based=CreditBasedChannelRequest(spsm=0x80, mtu=2048, mps=2048, initial_credit=256))

        # create server on bumble and wait for connection
        # waitConnReq = WaitConnectionRequest(connection=ref_connection_to_dut,
        #                                       le_credit_based=CreditBasedChannelRequest(spsm=0x80, mtu=2048, mps=2048, initial_credit=256))
        dutResp = ref_l2cap.WaitConnection(connection=ref_connection_to_dut,
                                               le_credit_based=CreditBasedChannelRequest(spsm=0x80, mtu=2048, mps=2048, initial_credit=256))



async def is_connected(device: PandoraDevice, connection: Connection) -> bool:
    try:
        await device.aio.host.WaitDisconnection(connection=connection, timeout=5)
        return False
    except grpc.RpcError as e:
        assert_equal(e.code(), grpc.StatusCode.DEADLINE_EXCEEDED)  # type: ignore
        return True


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
