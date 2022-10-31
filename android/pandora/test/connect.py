# Copyright 2022 Google LLC
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
import sys
import time

from mobly import test_runner, base_test, asserts
from grpc import RpcError

from avatar.controllers import pandora_device
from avatar.utils import Address

import google.protobuf.descriptor_pool

# Reset protobuf descriptor_pool as we are reimporting
# a module with the same package
google.protobuf.descriptor_pool.Default().__init__()

from pandora_experimental.host_grpc import Host
from pandora_experimental.host_pb2 import Connection, ConnectabilityMode, AddressType
from pandora_experimental.l2cap_grpc import L2CAP
from pandora_experimental.security_grpc import Security
from pandora_experimental.gatt_grpc import GATT
from pandora_experimental.gatt_pb2 import AttStatusCode, AttProperties, AttPermissions
from pandora_experimental.gatt_pb2 import GattServiceParams
from pandora_experimental.gatt_pb2 import GattCharacteristicParams
from pandora_experimental.gatt_pb2 import ReadCharacteristicResponse
from pandora_experimental.gatt_pb2 import ReadCharacteristicsFromUuidResponse


class ExampleTest(base_test.BaseTestClass):

    def setup_class(self):
        self.pandora_devices = self.register_controller(pandora_device)
        self.dut = self.pandora_devices[0]
        self.ref = self.pandora_devices[1]

    def setup_test(self):
        Host(self.dut.channel).HardReset()
        # TODO: wait for server
        time.sleep(3)

    def test_classic_connect(self):
        print('test_classic_connect', file=sys.stderr)
        dut_address = self.dut.address
        self.dut.log.info(f'Address: {dut_address}')
        response = self.ref.host.Connect(address=dut_address)
        assert response.WhichOneof("result") == "connection"

    def test_le_connect_ref_initiate(self):
        print('test_le_connect_ref_initiate', file=sys.stderr)
        dut_address = self.dut.address
        ref_address = self.ref.address

        self.dut.host.StartAdvertising(
            connectability_mode=ConnectabilityMode.CONNECTABILITY_CONNECTABLE,
            own_address_type=AddressType.PUBLIC,
        )

        self.dut.log.debug(f'DUT Address: {dut_address}')
        self.ref.log.debug(f'REF Address: {ref_address}')

        response = self.ref.host.ConnectLE(address=dut_address)
        assert response.WhichOneof("result") == "connection"

    def test_le_connect_dut_initiate(self):
        """
        REF device advertises, then DUT runs discovery, and connects.
        """
        print('test_le_connect_dut_initiate', file=sys.stderr)

        dut_address = self.dut.address
        ref_address = self.ref.address
        self.dut.log.debug(f'DUT Address: {dut_address}')
        self.ref.log.debug(f'REF Address: {ref_address}')

        self.ref.log.debug(f'REF StartAdvertising with random address: {self.ref.random_address}')
        self.ref.host.StartAdvertising(
            connectability_mode=ConnectabilityMode.CONNECTABILITY_CONNECTABLE,
            own_address_type=AddressType.RANDOM,
        )

        self.dut.log.debug(f'DUT RunDiscovery')
        discovery_scans = self.dut.host.RunDiscovery()
        for discovery_scan in discovery_scans:
            self.dut.log.debug(f'DUT found discovery_scan with address: {Address(discovery_scan.device.address)}')
            if discovery_scan.device.address == Address(self.ref.random_address):
                self.dut.log.debug(f'DUT found REF device with random address: {self.ref.random_address}')
                discovery_scans.cancel()
                break

        self.dut.log.debug(f'DUT ConnectLE with random address: {self.ref.random_address}')
        connectLE_response = self.dut.host.ConnectLE(address=Address(self.ref.random_address))
        assert connectLE_response.WhichOneof("result") == "connection"

    def test_dut_discover_services(self):
        print('test_dut_discover_services', file=sys.stderr)

        dut_address = self.dut.address
        ref_address = self.ref.address
        self.dut.log.debug(f'DUT Address: {dut_address}')
        self.ref.log.debug(f'REF Address: {ref_address}')

        self.ref.log.debug(f'REF StartAdvertising with random address: {self.ref.random_address}')
        self.ref.host.StartAdvertising(
            connectability_mode=ConnectabilityMode.CONNECTABILITY_CONNECTABLE,
            own_address_type=AddressType.RANDOM,
        )

        self.dut.log.debug(f'DUT RunDiscovery')
        discovery_scans = self.dut.host.RunDiscovery()
        for discovery_scan in discovery_scans:
            self.dut.log.debug(f'DUT found discovery_scan with address: {Address(discovery_scan.device.address)}')
            if discovery_scan.device.address == Address(self.ref.random_address):
                self.dut.log.debug(f'DUT found REF device with random address: {self.ref.random_address}')
                discovery_scans.cancel()
                break

        self.dut.log.debug(f'DUT ConnectLE with random address: {self.ref.random_address}')
        connectLE_response = self.dut.host.ConnectLE(address=Address(self.ref.random_address))
        connection = connectLE_response.connection
        assert connection is not None

        self.dut.log.debug(f'DUT DiscoverServices')
        dut_services = self.dut.gatt.DiscoverServices(connection=connection).services
        for service in dut_services:
            self.dut.log(
                f'service handle: {service.handle}, service type: {service.type}, service uuid: {service.uuid}')
        assert dut_services is not None


if __name__ == '__main__':
    # MoblyBinaryHostTest pass test_runner arguments after a "--"
    # to make it work with rewrite argv to skip the "--"
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()
