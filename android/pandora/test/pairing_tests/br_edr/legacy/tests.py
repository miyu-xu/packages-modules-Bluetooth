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
from pairing_tests.br_edr.pairing_processors_impl import BREDRLegacyPairingProcessor
from pairing_tests.br_edr.service_accessors_impl import BumbleHidServiceAccessor

from pairing_tests.decorators import add_common_tests, add_boilerplate_methods

@add_common_tests(
    acl_connection_builder_classes=[BREDRAclConnectionBuilder],
    pairing_process_class=BREDRLegacyPairingProcessor,
    service_accessor_classes=[BumbleHidServiceAccessor],
)
@add_boilerplate_methods
class BREDRLegacyTestClass(base_test.BaseTestClass):

    ref_config = {
        'classic_enabled': True,
        'le_enabled': False,
        'classic_ssp_enabled': False,
        'classic_false_enabled': False,
        'server': {  # pairing config
            # Android io_capability: display_yesno
            # BR/EDR legacy does not use IO capability
            # however, the implementation of ref (bumble) requires
            # this io capability to work
            'io_capability': 'keyboard_input_only',
        }
    }

    # additional tests can be added here
