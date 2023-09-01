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

import logging

from mobly import test_runner

from .smp_test_le_pairing_delete_dup_bond_record import SmpTestLePairingDeleteDupBond
from .smp_test_mitm_sec_req_on_enc import SmpTestMitmSecReqOnEnc


class SmpTest(SmpTestLePairingDeleteDupBond, SmpTestMitmSecReqOnEnc):  # type: ignore[misc]
    pass


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
