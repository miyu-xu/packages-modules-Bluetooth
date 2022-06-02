#!/usr/bin/env python3
#
#   Copyright 2022 - The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import queue
import logging

from google.protobuf import empty_pb2 as empty_proto

from blueberry.tests.gd_sl4a.lib import gd_sl4a_base_test
from blueberry.tests.gd.cert.truth import assertThat

from blueberry.facade import common_pb2 as common
from blueberry.facade.hci import le_initiator_address_facade_pb2 as le_initiator_address_facade

from mobly import test_runner

class AndroidTvTest(gd_sl4a_base_test.GdSl4aBaseTestClass):

    def _set_cert_privacy_policy_with_public_address(self):
        public_address_bytes = self.cert.hci_controller.GetMacAddress(empty_proto.Empty()).address
        private_policy = le_initiator_address_facade.PrivacyPolicy(
            address_policy=le_initiator_address_facade.AddressPolicy.USE_PUBLIC_ADDRESS,
            address_with_type=common.BluetoothAddressWithType(
                address=common.BluetoothAddress(address=public_address_bytes), type=common.PUBLIC_DEVICE_ADDRESS))
        self.cert.hci_le_initiator_address.SetPrivacyPolicyForInitiatorAddress(private_policy)
        # Bluetooth MAC address must be upper case
        return public_address_bytes.decode('utf-8').upper()

    def setup_class(self):
        super().setup_class(cert_module='SECURITY')
        self.default_timeout = 5  # seconds

    def setup_test(self):
        super().setup_test()
        self.cert_security = PyLeSecurity(self.cert)

    def teardown_test(self):
        self.cert_security.close()
        super().teardown_test()

    def test_android_tv_remote(self):
        public_address = self._set_cert_privacy_policy_with_public_address()
        #self.cert is GD
        #self.dut.sl4a is Android Framework/SL4A.apk


if __name__ == '__main__':
    test_runner.main()
