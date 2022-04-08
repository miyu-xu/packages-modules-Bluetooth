#!/usr/bin/env python3
#
#   Copyright 2021 - The Android Open Source Project
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

import io
import os
import queue
import logging

from google.protobuf import empty_pb2 as empty_proto

from bluetooth_packets_python3 import hci_packets
from blueberry.tests.gd.cert.context import get_current_context
from blueberry.tests.gd_sl4a.lib.bt_constants import ble_scan_settings_modes, ble_address_types, scan_result, ble_scan_settings_phys
from blueberry.tests.gd_sl4a.lib.ble_lib import generate_ble_scan_objects, generate_ble_advertise_objects
from blueberry.tests.sl4a_sl4a.lib.sl4a_sl4a_base_test import Sl4aSl4aBaseTestClass
from blueberry.facade.hci import le_advertising_manager_facade_pb2 as le_advertising_facade
from blueberry.facade.hci import le_initiator_address_facade_pb2 as le_initiator_address_facade
from blueberry.facade import common_pb2 as common

from mobly.controllers.android_device_lib.adb import AdbError


class ConnectWithIrkTest(Sl4aSl4aBaseTestClass):

    def setup_class(self):
        super().setup_class()
        self.default_timeout = 10  # seconds

    def setup_test(self):
        super().setup_test()

    def teardown_test(self):
        super().teardown_test()

    def test_scan_connect_unbonded_device_with_irk(self):
        logging.info("Running the test")

        # Set up SL4A cert side to advertise
        logging.info("Starting advertising")
        advertise_callback, advertise_data, advertise_settings = generate_ble_advertise_objects(self.cert.sl4a)
        self.cert.sl4a.bleStartBleAdvertising(advertise_callback, advertise_data, advertise_settings)

        # TODO: Wait for advertising to start

        # TODO: Pull IRK from SL4A cert side to pass in from SL4A DUT side when scanning
        bt_config_file_path = os.path.join(get_current_context().get_full_output_path(),
                                           "DUT_%s_bt_config.conf" % self.cert.serial)
        try:
            self.cert.adb.pull(["/data/misc/bluedroid/bt_config.conf", bt_config_file_path])
            # TODO: Add method to parse and serialize BT config here first but then likely move to util file
        except AdbError as error:
            logging.error("Failed to pull SL4A cert BT config")
            return False
        logging.info("Reading SL4A cert BT config")
        with io.open(bt_config_file_path) as f:
            for line in f.readlines():
                stripped_line = line.strip()
                if (stripped_line.startswith("Address")):
                    address_fields = stripped_line.split(' ')  # TODO: could generalize this to parse key/value
                    # API currently requires public address to be capitalized
                    address = address_fields[2].upper()
                    #logging.debug("Found cert address: %s" % address)
                    logging.info("Found cert address: %s" % address)
                    continue
                if (stripped_line.startswith("LE_LOCAL_KEY_IRK")):
                    irk_fields = stripped_line.split(' ')
                    irk = irk_fields[2]
                    #logging.debug("Found cert IRK: %s" % irk)
                    logging.info("Found cert IRK: %s" % irk)
                    continue

        # TODO: Set up SL4A DUT side to scan
        #addr_type = ble_address_types["public"] # TODO: This shouldn't be public; should be random and with IRK (see line 621)
        #logging.info("Start scanning for PUBLIC_ADDRESS %s with address type %d" % (address, addr_type))
        addr_type = ble_address_types["random"]
        # TODO: This isn't the random address; this is the mac address....
        logging.info("Start scanning for RANDOM_ADDRESS %s with address type %d and IRK %s" % (address, addr_type, irk))
        self.dut.sl4a.bleSetScanSettingsScanMode(ble_scan_settings_modes['low_latency'])
        self.dut.sl4a.bleSetScanSettingsLegacy(False)
        filter_list, scan_settings, scan_callback = generate_ble_scan_objects(self.dut.sl4a)
        expected_event_name = scan_result.format(scan_callback)

        # Set up SL4A DUT filter
        self.dut.sl4a.bleSetScanFilterDeviceAddressTypeAndIrkHexString(address, int(addr_type), irk)
        self.dut.sl4a.bleBuildScanFilter(filter_list)

        # TODO: Start scanning on SL4A DUT
        self.dut.sl4a.bleStartBleScan(filter_list, scan_settings, scan_callback)
        logging.info("Started scanning")

        # TODO: Verify if there is scan result
        try:
            event_info = self.dut.ed.pop_event(expected_event_name, self.default_timeout)
        except queue.Empty as error:
            logging.error("Could not find initial advertisement.")  # TODO: Need to fail if hit this
            return False
        mac_address = event_info['data']['Result']['deviceInfo']['address']
        logging.info("Filter advertisement with address {}".format(mac_address))

        # Stop scanning
        self.dut.sl4a.bleStopBleScan(scan_callback)

        # Connect
        gatt_callback = self.dut.sl4a.gattCreateGattCallback()
        # 0 = Auto, 1 = bredr, 2=le - DCK uses LE
        bluetooth_gatt = self.dut.sl4a.gattClientConnectGatt(gatt_callback, mac_address, False, 2, False, None)

        # TODO: Get connection event
        expected_event_name = "GattConnect{}onConnectionStateChange".format(gatt_callback)
        try:
            event_info = self.dut.ed.pop_event(expected_event_name, self.default_timeout)
        except queue.Empty as error:
            logging.error("Could not find connection event")  # TODO: Need to fail if hit this
            return False

        logging.info(event_info)

        # Stop advertising
        logging.info("Stopping advertising")
        self.cert.sl4a.bleStopBleAdvertising(advertise_callback)

        return True
