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

import asyncio
import logging

from avatar import PandoraDevice, PandoraDevices, asynchronous
from mobly import base_test, test_runner
from pandora.host_pb2 import RANDOM, DataTypes, ScanFilter, ScanRequest
from pandora_experimental.gatt_grpc import GATT
from typing import Optional

COMPLETE_LOCAL_NAME: str = "Bumble"


class LeScanningTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    def test_scan_filter_device_name_legacy_pdu(self) -> None:
        advertise = self.ref.host.Advertise(
            legacy=True,
            connectable=True,
            data=DataTypes(complete_local_name=COMPLETE_LOCAL_NAME,),
        )

        scan = self.dut.host.Scan(scan_filter=ScanFilter(device_name=COMPLETE_LOCAL_NAME,),)
        scan_result = next((x for x in scan if COMPLETE_LOCAL_NAME == x.data.complete_local_name))
        assert scan_result

        scan.cancel()
        advertise.cancel()

    def test_scan_filter_device_random_address_legacy_pdu(self) -> None:
        advertise = self.ref.host.Advertise(
            legacy=True,
            connectable=True,
            own_address_type=RANDOM,
        )

        scan = self.dut.host.Scan(scan_filter=ScanFilter(device_address=self.ref.random_address,),)

        scan_result = next((x for x in scan if x.random == self.ref.random_address))
        assert scan_result

        scan.cancel()
        advertise.cancel()


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
