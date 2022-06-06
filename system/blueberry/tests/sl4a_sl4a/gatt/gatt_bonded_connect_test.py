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

import binascii
import io
import logging
import os
import queue

from blueberry.facade import common_pb2 as common
from blueberry.tests.gd.cert.context import get_current_context
from blueberry.tests.gd.cert.truth import assertThat
from blueberry.tests.gd_sl4a.lib.ble_lib import generate_ble_advertise_objects
from blueberry.tests.gd_sl4a.lib.ble_lib import generate_ble_scan_objects
from blueberry.tests.gd_sl4a.lib.bt_constants import adv_succ
from blueberry.tests.gd_sl4a.lib.bt_constants import ble_address_types
from blueberry.tests.gd_sl4a.lib.bt_constants import ble_advertise_settings_modes
from blueberry.tests.gd_sl4a.lib.bt_constants import ble_scan_settings_modes
from blueberry.tests.gd_sl4a.lib.bt_constants import scan_result
from blueberry.tests.sl4a_sl4a.lib import sl4a_sl4a_base_test
from blueberry.utils.bt_gatt_constants import GattCallbackString
from blueberry.utils.bt_gatt_constants import GattTransport
from mobly import test_runner
from mobly.controllers.android_device_lib.adb import AdbError


class OobData:

    address = None
    confirmation = None
    randomizer = None

    def __init__(self, address, confirmation, randomizer):
        self.address = address
        self.confirmation = confirmation
        self.randomizer = randomizer


class GattBondedConnectTest(sl4a_sl4a_base_test.Sl4aSl4aBaseTestClass):

    SL4A_EVENT_GENERATED = "GeneratedOobData"
    SL4A_EVENT_BONDED = "Bonded"

    TRANSPORT_LE = "2"

    def setup_class(self):
        super().setup_class()
        self.default_timeout = 10  # seconds

    def setup_test(self):
        super().setup_test()

    def teardown_test(self):
        super().teardown_test()

    def _wait_for_event(self, expected_event_name, device):
        try:
            event_info = device.ed.pop_event(expected_event_name, self.default_timeout)
            logging.info(event_info)
        except queue.Empty as error:
            logging.error("Failed to find event: %s", expected_event_name)
            return False
        return True

    def _wait_for_scan_result_event(self, expected_event_name, device):
        try:
            event_info = device.ed.pop_event(expected_event_name, self.default_timeout)
        except queue.Empty as error:
            logging.error("Could not find scan result event: %s", expected_event_name)
            return None
        return event_info['data']['Result']['deviceInfo']['address']

    def _get_cert_public_address_and_irk_from_bt_config(self):
        # Pull IRK from SL4A cert side to pass in from SL4A DUT side when scanning
        bt_config_file_path = os.path.join(get_current_context().get_full_output_path(),
                                           "DUT_%s_bt_config.conf" % self.cert.serial)
        try:
            self.cert.adb.pull(["/data/misc/bluedroid/bt_config.conf", bt_config_file_path])
        except AdbError as error:
            logging.error("Failed to pull SL4A cert BT config")
            return False
        logging.debug("Reading SL4A cert BT config")
        with io.open(bt_config_file_path) as f:
            for line in f.readlines():
                stripped_line = line.strip()
                if (stripped_line.startswith("Address")):
                    address_fields = stripped_line.split(' ')
                    # API currently requires public address to be capitalized
                    address = address_fields[2].upper()
                    logging.debug("Found cert address: %s" % address)
                    continue
                if (stripped_line.startswith("LE_LOCAL_KEY_IRK")):
                    irk_fields = stripped_line.split(' ')
                    irk = irk_fields[2]
                    logging.debug("Found cert IRK: %s" % irk)
                    continue

        return address, irk

    def _generate_oob_data(self, device, transport):
        logging.info("Fetching OOB data...")
        device.sl4a.bluetoothGenerateLocalOobData(transport)
        try:
            event_info = device.ed.pop_event(self.SL4A_EVENT_GENERATED, self.default_timeout)
        except queue.Empty as Error:
            logging.error("Failed to generate OOB data!")
            return None
        logging.info("Data received: %s", event_info["data"])
        return OobData(event_info["data"]["address_with_type"], event_info["data"]["confirmation"],
                       event_info["data"]["randomizer"])

    # TODO: Rename this test
    def test_scan_connect_unbonded_device_public_address_with_irk_bond_oob_scan_connect_gatt(self):
        self.dut.sl4a.bluetoothStartPairingHelper(True)
        self.cert.sl4a.bluetoothStartPairingHelper(True)

        cert_oob_data = self._generate_oob_data(self.cert, self.TRANSPORT_LE)
        assertThat(cert_oob_data).isNotNone()

        cert_oob_address = bytes.fromhex(cert_oob_data.address[:12])
        cert_oob_address = cert_oob_address[::-1]
        cert_oob_address = binascii.hexlify(cert_oob_address).decode("utf-8").upper()
        cert_oob_address_formatted = cert_oob_address[:
                                                      2] + ":" + cert_oob_address[2:
                                                                                  4] + ":" + cert_oob_address[4:
                                                                                                              6] + ":" + cert_oob_address[6:
                                                                                                                                          8] + ":" + cert_oob_address[8:
                                                                                                                                                                      10] + ":" + cert_oob_address[10:
                                                                                                                                                                                                   12]
        logging.info("address: %s", cert_oob_address)
        logging.info("formatted: %s", cert_oob_address_formatted)

        cert_public_address, irk = self._get_cert_public_address_and_irk_from_bt_config()

        # Set up SL4A DUT side to scan
        addr_type = ble_address_types["random"]
        logging.info("Start scanning for RANDOM_ADDRESS %s with address type %d and IRK %s" %
                     (cert_oob_address_formatted, addr_type, irk))
        self.dut.sl4a.bleSetScanSettingsScanMode(ble_scan_settings_modes['low_latency'])
        self.dut.sl4a.bleSetScanSettingsLegacy(False)
        filter_list, scan_settings, scan_callback = generate_ble_scan_objects(self.dut.sl4a)
        expected_event_name = scan_result.format(scan_callback)

        # Start scanning on SL4A DUT
        self.dut.sl4a.bleSetScanFilterDeviceAddressAndType(cert_oob_address_formatted, int(addr_type))
        self.dut.sl4a.bleBuildScanFilter(filter_list)
        self.dut.sl4a.bleStartBleScan(filter_list, scan_settings, scan_callback)
        logging.info("Started scanning")

        # Verify that scan result is received on SL4A DUT
        advertising_address = self._wait_for_scan_result_event(expected_event_name, self.dut)
        assertThat(advertising_address).isNotNone()
        logging.info("Filter advertisement with address {}".format(advertising_address))

        # Stop scanning and try to connect GATT
        self.dut.sl4a.bleStopBleScan(scan_callback)

        gatt_callback = self.dut.sl4a.gattCreateGattCallback()
        bluetooth_gatt = self.dut.sl4a.gattClientConnectGatt(gatt_callback, advertising_address, False,
                                                             GattTransport.TRANSPORT_LE, False, None)
        assertThat(bluetooth_gatt).isNotNone()

        # Verify that GATT connect event occurs on SL4A DUT
        expected_event_name = GattCallbackString.GATT_CONN_CHANGE.format(gatt_callback)
        assertThat(self._wait_for_event(expected_event_name, self.dut)).isTrue()

        # Bond
        self.dut.sl4a.bluetoothCreateBondOutOfBand(cert_oob_address_formatted, self.TRANSPORT_LE,
                                                   cert_oob_data.confirmation, cert_oob_data.randomizer)

        try:
            bond_state = self.dut.ed.pop_event(self.SL4A_EVENT_BONDED, self.default_timeout)
        except queue.Empty() as error:
            logging.error("Failed to get bond event!")

        assertThat(bond_state).isNotNone()
        logging.info("Bonded: %s", bond_state["data"]["bonded_state"])

        # Disconnect GATT
        self.dut.sl4a.gattClientDisconnect(bluetooth_gatt)
        # TODO: Wait for disconnect event?
        self.dut.sl4a.gattClientClose(bluetooth_gatt)

        # TODO: Start advertising again on cert side
        logging.info("Starting advertising")
        self.cert.sl4a.bleSetAdvertiseSettingsIsConnectable(True)
        self.cert.sl4a.bleSetAdvertiseDataIncludeDeviceName(True)
        self.cert.sl4a.bleSetAdvertiseSettingsAdvertiseMode(ble_advertise_settings_modes['low_latency'])
        self.cert.sl4a.bleSetAdvertiseSettingsOwnAddressType(common.RANDOM_DEVICE_ADDRESS)
        advertise_callback, advertise_data, advertise_settings = generate_ble_advertise_objects(self.cert.sl4a)
        self.cert.sl4a.bleStartBleAdvertising(advertise_callback, advertise_data, advertise_settings)

        # Wait for SL4A cert to start advertising
        assertThat(self._wait_for_event(adv_succ.format(advertise_callback), self.cert)).isTrue()
        logging.info("Advertising started again")

        # TODO: Scan and connect GATT again; consider advertising with different address on cert; consider scan/connect directly on bonded device object (from bond event)
        self.dut.sl4a.bleSetScanSettingsScanMode(ble_scan_settings_modes['low_latency'])
        self.dut.sl4a.bleSetScanSettingsLegacy(False)
        filter_list, scan_settings, scan_callback = generate_ble_scan_objects(self.dut.sl4a)
        expected_event_name = scan_result.format(scan_callback)

        # Start scanning on SL4A DUT
        self.dut.sl4a.bleSetScanFilterDeviceAddressAndType(cert_oob_address_formatted, int(addr_type))
        self.dut.sl4a.bleBuildScanFilter(filter_list)
        self.dut.sl4a.bleStartBleScan(filter_list, scan_settings, scan_callback)
        logging.info("Started scanning again")

        # Verify that scan result is received on SL4A DUT
        advertising_address = self._wait_for_scan_result_event(expected_event_name, self.dut)
        assertThat(advertising_address).isNotNone()
        logging.info("Filter advertisement with address {}".format(advertising_address))

        # Stop scanning and try to connect GATT
        self.dut.sl4a.bleStopBleScan(scan_callback)

        gatt_callback = self.dut.sl4a.gattCreateGattCallback()
        bluetooth_gatt = self.dut.sl4a.gattClientConnectGatt(gatt_callback, advertising_address, False,
                                                             GattTransport.TRANSPORT_LE, False, None)
        assertThat(bluetooth_gatt).isNotNone()

        # Verify that GATT connect event occurs on SL4A DUT
        expected_event_name = GattCallbackString.GATT_CONN_CHANGE.format(gatt_callback)
        assertThat(self._wait_for_event(expected_event_name, self.dut)).isTrue()

        # Stop cert advertising
        #self.cert.sl4a.bleStopBleAdvertising(advertise_callback)

        # Test over
        # TODO: unbond - confirm address is what we expect
        self.dut.sl4a.bluetoothUnbond(cert_oob_address_formatted)


if __name__ == '__main__':
    test_runner.main()
