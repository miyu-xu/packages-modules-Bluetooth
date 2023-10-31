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

from pairing_tests.common import (
    AclConnectionHarnessBase,
    DedicatedPairingHarnessBase,
    GeneralPairingHarnessBase,
)

class ClassicAclConnectionHarness(IAclConnectionHarness):

    async def _start_connection(self):
        return await asyncio.gather(
            self.initiator.aio.host.Connect(address=self.responder.address),
            self.responder.aio.host.WaitConnection(address=self.initiator.address),
        )

    async def start_connection():
        self._connection_task = asyncio.create_task(
            self._start_connection(
                self.initiator,
                self.responder,
            )
        )

    async def verify_connection_success(self) -> None:
        init_res, resp_res = asyncio.wait_for(self._connection_task,
                                              timeout=10.0)

        assert_equal(init_res.result_variant(), 'connection')
        setattr(self.initiator, 'connection', init_res.connection)

        assert_equal(resp_res.result_variant(), 'connection')
        setattr(self.responder, 'connection', resp_res.connection)

    async def verify_connection_failure(self) -> None:
        init_res, resp_res = asyncio.wait_for(self._connection_task,
                                              timeout=10.0)

        # TODO
        # assert_equal(init_res.result_variant(), 'connection')
        # assert_equal(resp_res.result_variant(), 'connection')


class ClassicDDPairingHarness_Ref_Init_ACL(DedicatedPairingHarnessBase):
    pass

class ClassicDDPairingHarness_Android_Init_ACL(DedicatedPairingHarnessBase):
    pass

class ClassicGeneralPairingHarness(GeneralPairingHarnessBase):
    pass
