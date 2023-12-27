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

from avatar import BumblePandoraDevice

from bumble.hci import Address
from bumble.hid import HID_CONTROL_PSM
from bumble.l2cap import ClassicChannelSpec
from mobly.asserts import assert_false, assert_true
from pairing_tests.interfaces import IServiceAccessor

class BumbleHidServiceAccessor(IServiceAccessor):
    async def verify_role_setup(self):
        assert_true(
            isinstance(self._acl_connection_builder.initiator, BumblePandoraDevice), "acl initiator should be Bumble"
        )
        assert_true(isinstance(self.initiator, BumblePandoraDevice), "initiator should be Bumble")

    async def start(self):
        # verify the ACL is established
        await self._acl_connection_builder.start()
        await self._acl_connection_builder.wait_for_completion()
        await self._acl_connection_builder.verify_success()

        # Try accessing Android secure services from bumble
        # use bumble API to get the underlying API to get the ACL connection
        # as l2cap APIs in bumble are based on it
        android_addr = Address.from_string_for_transport(str(self.dut.address), Address.PUBLIC_DEVICE_ADDRESS)
        bumble_acl_connection = self.ref.device.find_connection_by_bd_addr(android_addr)

        async def access_l2cap_service(psm):
            channel = bumble_acl_connection.create_l2cap_channel(spec=ClassicChannelSpec(psm=psm))
            return await channel

        self._hid_service_access_tsk = asyncio.create_task(access_l2cap_service(HID_CONTROL_PSM))

    async def wait_for_completion(self):
        try:
            self._channel = await asyncio.wait_for(self._hid_service_access_tsk, timeout=30.0)
            self._success = True
        except:
            self._success = False

    def success(self):
        if not hasattr(self, '_success'):
            raise ValueError("wait_for_completion not called yet")

        return self._success

    async def verify_success(self):
        assert_true(self.success(), "service access should have succeeded")

    async def verify_failure(self):
        assert_false(self.success(), "service access should have failed")

    async def cleanup(self):
        if self._success:
            await self._channel.disconnect()
