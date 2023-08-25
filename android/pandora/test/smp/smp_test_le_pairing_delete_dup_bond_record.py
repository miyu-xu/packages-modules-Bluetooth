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

from avatar import BumblePandoraDevice
from avatar.aio import asynchronous
from bumble import smp
from bumble.hci import Address
from mobly import signals
from mobly.asserts import assert_false  # type: ignore
from mobly.asserts import assert_true  # type: ignore
from pandora.host_pb2 import RANDOM
from .smp_test_base import SmpTestBase


class SmpTestLePairingDeleteDupBond(SmpTestBase):  # type: ignore[misc]

    @asynchronous
    async def test_le_pairing_delete_dup_bond_record(self) -> None:
        if isinstance(self.dut, BumblePandoraDevice):
            raise signals.TestSkip('TODO: Fix test for Bumble DUT')
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip('Test require Bumble as reference device(s)')

        class Session(smp.Session):

            # Hack to send same identity address from ref during both pairing
            def send_command(self: smp.Session, command: smp.SMP_Command) -> None:
                if isinstance(command, smp.SMP_Identity_Address_Information_Command):
                    command = smp.SMP_Identity_Address_Information_Command(
                        addr_type=Address.RANDOM_IDENTITY_ADDRESS,
                        bd_addr=Address(
                            'F6:F7:F8:F9:FA:FB',
                            Address.RANDOM_IDENTITY_ADDRESS,
                        ),
                    )
                self.manager.send_command(self.connection, command)

        self.ref.device.smp_session_proxy = Session

        # Pair with same device 2 times.
        # Ref device advertises with different random address but uses same identity address
        ref1 = await self.dut_pair(dut_address_type=RANDOM, ref_address_type=RANDOM)
        is_bonded = await self.dut.aio.security_storage.IsBonded(random=ref1.random)
        assert_true(is_bonded.value, "")

        await self.ref.reset()
        self.ref.device.smp_session_proxy = Session

        ref2 = await self.dut_pair(dut_address_type=RANDOM, ref_address_type=RANDOM)
        is_bonded = await self.dut.aio.security_storage.IsBonded(random=ref2.random)
        assert_true(is_bonded.value, "")

        is_bonded = await self.dut.aio.security_storage.IsBonded(random=ref1.random)
        assert_false(is_bonded.value, "")
