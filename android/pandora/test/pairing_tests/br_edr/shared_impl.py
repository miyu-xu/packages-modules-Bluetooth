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

from pairing_tests.interfaces import IAclConnectionBuilder

from mobly.asserts import assert_equal, assert_not_equal

class BREDRAclConnectionBuilder(IAclConnectionBuilder):

    async def _start_acl_connection(self):
        return await asyncio.gather(
            self.initiator.aio.host.Connect(address=acl_responder.address),
            self.responder.aio.host.WaitConnection(address=acl_initiator.address),
        )

    async def start(self):
        self._connection_task = asyncio.create_task(self._start_acl_connection())

    async def verify_success(self):
        assert_equal(self.initiator_connection.result_variant(), 'connection')
        assert_equal(self.responder_connection.result_variant(), 'connection')

    async def verify_failure(self):
        assert_not_equal(self.initiator_connection.result_variant(), 'connection')
        assert_not_equal(self.responder_connection.result_variant(), 'connection')

    async def _wait_for_completion(self):
        if not hasattr(self, '_connection_task'):
            raise ValueError('acl connection has not been started yet')

        try:
            init_conn, resp_conn = await asyncio.wait_for(self._connection_task, timeout=10.0)
            self._init_connection = init_conn
            self._resp_connection = resp_conn
        except TimeoutError as e:
            raise e

    async def initiator_connection(self):
        if hasattr(self, '_init_connection'):
            return self._init_connection
        await self._wait_for_completion()
        return self._init_connection

    async def responder_connection(self):
        if hasattr(self, '_resp_connection'):
            return self._resp_connection
        await self._wait_for_completion()
        return self._resp_connection