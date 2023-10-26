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

from mobly import base_test
from pairing_tests.br_edr.acl_connection_builder_impl import BREDRAclConnectionBuilder
from pairing_tests.br_edr.pairing_processors_impl import BREDRNumericComparisonJustworksPairingProcessor
from pairing_tests.br_edr.service_accessors_impl import BumbleHidServiceAccessor
from pairing_tests.decorators import add_boilerplate_methods, add_common_tests

@add_common_tests(
    acl_connection_builder_classes=[BREDRAclConnectionBuilder],
    pairing_process_class=BREDRNumericComparisonJustworksPairingProcessor,
    service_accessor_classes=[BumbleHidServiceAccessor],
)
@add_boilerplate_methods
class BREDRDisplayOnlyTestClass(base_test.BaseTestClass):
    ref_config = {
        'classic_enabled': True,
        'le_enabled': False,
        'classic_ssp_enabled': True,
        'classic_sc_enabled': False,
        'server': {
            'pairing_sc_enable': False,
            'pairing_mitm_enable': True,
            'pairing_bonding_enable': True,
            # Android IO CAP: Display_YESNO
            # Ref IO CAP:
            'io_capability': 'display_output_only',
        },
    }
