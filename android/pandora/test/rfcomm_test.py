# Copyright 2025 Google LLC
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

from avatar import PandoraDevices, enableFlag
from avatar.aio import asynchronous
from avatar.pandora_client import BumblePandoraClient, PandoraClient
from bumble_experimental.rfcomm import RFCOMMService
from mobly import base_test, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import fail  # type: ignore
from pandora.host_pb2 import Connection
from pandora.security_pb2 import LEVEL2
from pandora_experimental.rfcomm_grpc_aio import RFCOMM
from pandora_experimental.rfcomm_pb2 import (
    AcceptConnectionRequest,
    ConnectionRequest,
    RxRequest,
    StartServerRequest,
    StopServerRequest,
    TxRequest,
)
from typing import Optional, Tuple

RFCOMM_FIX_MUX_COLLISION_HANDLING = 'com.android.bluetooth.flags.rfcomm_fix_mux_collision_handling'

SERIAL_PORT_UUID = "00001101-0000-1000-8000-00805F9B34FB"
TEST_SERVER_NAME = "RFCOMM-Server"


class RfcommTest(base_test.BaseTestClass):
    devices: Optional[PandoraDevices] = None
    dut: PandoraClient
    ref: BumblePandoraClient

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, ref, *_ = self.devices
        assert isinstance(ref, BumblePandoraClient)
        self.ref = ref
        # Enable BR/EDR mode and SSP for Bumble devices.
        self.ref.config.setdefault('classic_enabled', True)
        self.ref.config.setdefault('classic_ssp_enabled', True)
        self.ref.config.setdefault(
            'server',
            {
                'io_capability': 'no_output_no_input',
            },
        )

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @avatar.asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

        self.dut.rfcomm = RFCOMM(channel=self.dut.aio.channel)
        self.ref.rfcomm = RFCOMMService(self.ref.device)

    # TODO(b/286338264): Should use shared util script for connecting and bonding
    async def make_classic_connection(self) -> Tuple[Connection, Connection]:
        dut_ref, ref_dut = await asyncio.gather(
            self.dut.aio.host.WaitConnection(address=self.ref.address),
            self.ref.aio.host.Connect(address=self.dut.address),
        )

        assert_equal(dut_ref.result_variant(), 'connection')
        assert_equal(ref_dut.result_variant(), 'connection')
        assert dut_ref.connection is not None and ref_dut.connection is not None

        return dut_ref.connection, ref_dut.connection

    async def make_classic_bond(self, dut_ref: Connection, ref_dut: Connection) -> None:
        dut_ref_sec, ref_dut_sec = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref, classic=LEVEL2),
            self.ref.aio.security.WaitSecurity(connection=ref_dut, classic=LEVEL2),
        )
        assert_equal(dut_ref_sec.result_variant(), 'success')
        assert_equal(ref_dut_sec.result_variant(), 'success')

    @avatar.asynchronous
    @enableFlag(RFCOMM_FIX_MUX_COLLISION_HANDLING)
    async def test_server_mux_collision(self) -> None:
        # dut is server, ref is client
        context = grpc.ServicerContext
        # by connecting and bonding, HFP tries to connect automatically to ref
        # ref attempting to connect then causes collision scenario
        dut_ref, ref_dut = await self.make_classic_connection()
        await self.make_classic_bond(dut_ref, ref_dut)
        server = await self.dut.rfcomm.StartServer(name=TEST_SERVER_NAME, uuid=SERIAL_PORT_UUID)
        server = server.server
        rfc_dut_ref, rfc_ref_dut = await asyncio.gather(
            self.ref.rfcomm.ConnectToServer(request=ConnectionRequest(address=self.dut.address, uuid=SERIAL_PORT_UUID),
                                            context=grpc.ServicerContext),
            self.dut.rfcomm.AcceptConnection(server=server))

    @avatar.asynchronous
    async def test_client_connect_and_exchange_data(self) -> None:
        # dut is client, ref is server
        context = grpc.ServicerContext
        server = await self.ref.rfcomm.StartServer(StartServerRequest(name=TEST_SERVER_NAME, uuid=SERIAL_PORT_UUID),
                                                   context=context)
        # Convert StartServerResponse to its server
        server = server.server
        rfc_dut_ref, rfc_ref_dut = await asyncio.gather(
            self.dut.rfcomm.ConnectToServer(address=self.ref.address, uuid=SERIAL_PORT_UUID),
            self.ref.rfcomm.AcceptConnection(request=AcceptConnectionRequest(server=server), context=context))
        # Convert Responses to their corresponding RfcommConnection
        rfc_dut_ref = rfc_dut_ref.connection
        rfc_ref_dut = rfc_ref_dut.connection

        # Transmit data
        tx_data = b'Data from dut to ref'
        await self.dut.rfcomm.Send(data=tx_data, connection=rfc_dut_ref)
        ref_receive = await self.ref.rfcomm.Receive(request=RxRequest(connection=rfc_ref_dut), context=context)
        assert_equal(ref_receive.data, tx_data)

        # Receive data
        rx_data = b'Data from ref to dut'
        await self.ref.rfcomm.Send(request=TxRequest(connection=rfc_ref_dut, data=rx_data), context=context)
        dut_receive = await self.dut.rfcomm.Receive(connection=rfc_dut_ref)
        assert_equal(dut_receive.data.rstrip(b'\x00'), rx_data)

        # Disconnect (from dut)
        await self.dut.rfcomm.Disconnect(connection=rfc_dut_ref)
        await self.ref.rfcomm.StopServer(request=StopServerRequest(server=server), context=context)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
