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

from blueberry.tests.gd_sl4a.lib.gd_sl4a_base_test import GdSl4aBaseTestClass
from blueberry.tests.gd.cert.py_le_security import PyLeSecurity
from blueberry.tests.gd.cert.truth import assertThat

from blueberry.facade.security.facade_pb2 import LeIoCapabilityMessage
from blueberry.facade.security.facade_pb2 import LeOobDataPresentMessage

LeIoCapabilities = LeIoCapabilityMessage.LeIoCapabilities
LeOobDataFlag = LeOobDataPresentMessage.LeOobDataFlag

DISPLAY_ONLY = LeIoCapabilityMessage(capabilities=LeIoCapabilities.DISPLAY_ONLY)

OOB_NOT_PRESENT = LeOobDataPresentMessage(data_present=LeOobDataFlag.NOT_PRESENT)


class OobData:

    address = None
    confirmation = None
    randomizer = None

    def __init__(self, address, confirmation, randomizer):
        self.address = address
        self.confirmation = confirmation
        self.randomizer = randomizer


class OobPairingSl4aTest(GdSl4aBaseTestClass):
    # Events sent from SL4A
    SL4A_EVENT_GENERATED = "GeneratedOobData"
    SL4A_EVENT_ERROR = "ErrorOobData"
    SL4A_EVENT_BONDED = "Bonded"

    # Matches tBT_TRANSPORT
    # Used Strings because ints were causing gRPC problems
    TRANSPORT_AUTO = "0"
    TRANSPORT_BREDR = "1"
    TRANSPORT_LE = "2"

    def setup_class(self):
        super().setup_class(cert_module='SECURITY')
        self.default_timeout = 5  # seconds

    def setup_test(self):
        super().setup_test()
        self.cert_security = PyLeSecurity(self.cert)

    def teardown_test(self):
        super().teardown_test()

    def _generate_sl4a_oob_data(self, transport):
        logging.info("Fetching OOB data...")
        self.dut.sl4a.bluetoothGenerateLocalOobData(transport)
        try:
            event_info = self.dut.ed.pop_event(self.SL4A_EVENT_GENERATED, self.default_timeout)
        except queue.Empty as error:
            logging.error("Failed to generate OOB data!")
            return None
        logging.info("Data received!")
        return OobData(event_info["data"]["address"], event_info["data"]["confirmation"],
                       event_info["data"]["randomizer"])

    def _generate_cert_oob_data(self, transport):
        if transport == self.TRANSPORT_LE:
            oob_data = self.cert.security.GetLeOutOfBandData(empty_proto.Empty())
            # GetLeOutOfBandData adds null terminator to string in C code
            # (length 17) before passing back to python via gRPC where it is
            # converted back to bytes. Remove the null terminator for handling
            # in python test, since length is known to be 16 for
            # confirmation_value and random_value
            oob_data.confirmation_value = oob_data.confirmation_value[:-1]
            oob_data.random_value = oob_data.random_value[:-1]
            return oob_data
        return None

    def test_sl4a_classic_generate_oob_data(self):
        oob_data = self._generate_sl4a_oob_data(self.TRANSPORT_BREDR)
        assertThat(oob_data).isNotNone()

    def test_sl4a_classic_generate_oob_data_twice(self):
        self.test_sl4a_classic_generate_oob_data()
        self.test_sl4a_classic_generate_oob_data()

    def test_sl4a_ble_generate_oob_data(self):
        oob_data = self._generate_sl4a_oob_data(self.TRANSPORT_LE)
        assertThat(oob_data).isNotNone()

    def test_cert_ble_generate_oob_data(self):
        oob_data = self._generate_cert_oob_data(self.TRANSPORT_LE)
        assertThat(oob_data).isNotNone()

    def test_sl4a_create_bond_out_of_band(self):
        oob_data = self._generate_cert_oob_data(self.TRANSPORT_LE)
        assertThat(oob_data).isNotNone()
        self.cert.security.SetLeIoCapability(DISPLAY_ONLY)
        self.cert.security.SetLeOobDataPresent(OOB_NOT_PRESENT)
        self.cert_security.SetLeAuthRequirements()

        byte_address = self.cert.hci_controller.GetMacAddress(empty_proto.Empty()).address
        self.dut.sl4a.bluetoothCreateBondOutOfBand(
            byte_address.decode("utf-8").upper(), self.TRANSPORT_LE, oob_data.confirmation_value.hex(),
            oob_data.random_value.hex())
        try:
            bond_state = self.dut.ed.pop_event(self.SL4A_EVENT_BONDED, self.default_timeout)
        except queue.Empty as error:
            logging.error("Failed to generate OOB data!")

        assertThat(bond_state).isNotNone()
        assertThat(bond_state["data"]["bonded_state"]).isEqualTo("1")
