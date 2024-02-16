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

from avatar import PandoraDevice

from pairing.test_base import PairTestBase

from pandora.security_pb2 import LEVEL2

from bumble.hci import Address
from bumble.l2cap import ClassicChannelSpec

class BREDRPairTestBase(PairTestBase):

    async def start_acl_connection(self):
        return await asyncio.gather(
            self.acl_initiator.aio.host.Connect(address=self.acl_responder.address),
            self.acl_responder.aio.host.WaitConnection(address=self.acl_initiator.address),
        )

    async def start_pairing(
        self,
        initiator_acl_connection,
        responder_acl_connection,
    ):
        return await asyncio.gather(
            self.pairing_initiator.aio.security.Secure(connection=initiator_acl_connection, classic=LEVEL2),
            self.pairing_responder.aio.security.WaitSecurity(connection=responder_acl_connection, classic=LEVEL2),
        )

    async def start_service_access(
        self,
        initiator_acl_connection,
        responder_acl_connection,
    ):
        # Try accessing Android secure services from bumble
        # use bumble API to get the underlying API to get the ACL connection
        # as l2cap APIs in bumble are based on it
        android_addr = Address.from_string_for_transport(str(self.dut.address), Address.PUBLIC_DEVICE_ADDRESS)
        bumble_raw_acl_connection = self.ref.device.find_connection_by_bd_addr(android_addr)

        # start accessing hid interrupt service
        hid_interrupt_psm = 0x13
        channel = bumble_raw_acl_connection.create_l2cap_channel(
            spec=ClassicChannelSpec(psm=hid_interrupt_psm)
        )

        return await channel
