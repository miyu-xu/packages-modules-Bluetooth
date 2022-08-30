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
import queue
import sys
import time

from mobly import test_runner, base_test, asserts
from grpc import RpcError

from avatar.controllers import pandora_device
from pandora.security_pb2 import PairingEvent, PairingEventAnswer


class ExampleTest(base_test.BaseTestClass):

    def setup_class(self):
        self.pandora_devices = self.register_controller(pandora_device)
        self.dut = self.pandora_devices[0]
        self.ref = self.pandora_devices[1]

        self.dut.host.HardReset()
        # TODO: wait for server
        time.sleep(3)

    def setup_test(self):
        self.dut.host.SoftReset()
        self.ref.host.SoftReset()

    def test_classic_connect_from_ref(self):
        dut_address = self.dut.address
        self.dut.log.info(f'Address: {dut_address}')
        response = self.ref.host.Connect(address=dut_address)
        assert response.WhichOneof("result") == "connection"

    # def test_classic_connect_from_dut(self):
    #     ref_address = self.ref.address
    #     self.ref.log.info(f'Address: {ref_address}')
    #     response = self.dut.host.Connect(address=ref_address)
    #     assert response.WhichOneof("result") == "connection"

    def test_classic_pair_from_ref(self):
        io_cap = 3
        sc = True
        mitm = True
        dut_address = self.dut.address
        self.dut.log.info(f'Address: {dut_address}')
        self.ref.security.SetPairingConfig(
            io_capability=io_cap, bonding=True, mitm_required=mitm, secure_connection_supported=sc, oob_data=0)
        response = self.ref.host.Connect(address=dut_address)
        assert response.WhichOneof("result") == "connection"

        pairing_event_stream = self.dut.security.OnPairing()

        self.ref.security.Pair(connection=response.connection)
        pairing_event = pairing_event_stream.recv()
        pairing_method = pairing_event.WhichOneof("method")
        self.dut.log.info(f'method: {pairing_method}')
        pairing_event_stream.send(event=pairing_event, confirm=True)

        time.sleep(3)


if __name__ == '__main__':
    # MoblyBinaryHostTest pass test_runner arguments after a "--"
    # to make it work with rewrite argv to skip the "--"
    index = sys.argv.index('--')
    sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()
