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

import asyncio
import logging

from avatar import PandoraDevices, parameterized
from avatar.aio import asynchronous
from avatar.bumble_server.security import PairingDelegate
from avatar.pandora_client import BumblePandoraClient, PandoraClient
from bumble.gatt import GATT_ASHA_SERVICE
from enum import Enum
from mobly import base_test, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from pandora._utils import Stream
from pandora.host_pb2 import PUBLIC, RANDOM, AdvertiseResponse, Connection, DataTypes, OwnAddressType, ScanningResponse
from typing import List, Optional, Tuple

ASHA_UUID = GATT_ASHA_SERVICE.to_hex_str()
HISYCNID: List[int] = [0x01, 0x02, 0x03, 0x04, 0x5, 0x6, 0x7, 0x8]
CAPABILITY: int = 0x0
COMPLETE_LOCAL_NAME: str = "Bumble"


class Device(Enum):
    """Reference devices type"""

    Left = 0
    Right = 1


class ASHADualDeviceTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None
    dut: PandoraClient
    ref_left: BumblePandoraClient
    ref_right: BumblePandoraClient

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        dut, ref_left, ref_right, *_ = self.devices
        assert isinstance(ref_left, BumblePandoraClient)
        assert isinstance(ref_right, BumblePandoraClient)
        self.dut, self.ref_left, self.ref_right = dut, ref_left, ref_right

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref_left.reset(), self.ref_right.reset())
        # ASHA hearing aid's IO capability is NO_OUTPUT_NO_INPUT
        setattr(self.ref_left, "io_capability", PairingDelegate.NO_OUTPUT_NO_INPUT)
        setattr(self.ref_right, "io_capability", PairingDelegate.NO_OUTPUT_NO_INPUT)

    def ref_advertise_asha(self, ref_device: BumblePandoraClient,
                           ref_address_type: OwnAddressType) -> Stream[AdvertiseResponse]:
        """
        Ref device starts to advertise
        :return: Ref device's advertise response
        """
        # Ref starts advertising with ASHA service data
        ref_device.asha.Register(capability=CAPABILITY, hisyncid=HISYCNID)
        return ref_device.host.Advertise(
            legacy=True,
            connectable=True,
            data=DataTypes(
                complete_local_name=COMPLETE_LOCAL_NAME,
                incomplete_service_class_uuids16=[ASHA_UUID],
            ),
            own_address_type=ref_address_type,
        )

    def dut_scan_for_asha(self, dut_address_type: OwnAddressType) -> ScanningResponse:
        """
        DUT starts to scan for the Ref device.
        :return: ScanningResponse for ASHA
        """
        scan_result = self.dut.host.Scan(own_address_type=dut_address_type)
        ref = next((x for x in scan_result if ASHA_UUID in x.data.incomplete_service_class_uuids16))
        scan_result.cancel()

        assert ref
        return ref

    def dut_connect_to_ref(self, advertisement: Stream[AdvertiseResponse], ref: ScanningResponse,
                           dut_address_type: OwnAddressType) -> Tuple[Connection, Connection]:
        """
        Helper method for Dut connects to Ref
        :return: a Tuple (DUT to REF connection, REF to DUT connection)
        """
        dut_ref = self.dut.host.ConnectLE(own_address_type=dut_address_type, **ref.address_asdict()).connection
        ref_dut = (next(advertisement)).connection
        assert dut_ref
        assert ref_dut

        advertisement.cancel()
        return dut_ref, ref_dut

    def advertise_and_connect(self, ref_device: BumblePandoraClient, dut_address_type: OwnAddressType,
                              ref_address_type: OwnAddressType):
        """
        Helper method to combine the following methods:
        1. Ref device starts to advertise
        2. DUT starts to scan for the Ref device.
        3. Dut connects to Ref
        :return: a Tuple (DUT to REF connection, REF to DUT connection)
        """
        advertisement = self.ref_advertise_asha(ref_device=ref_device, ref_address_type=ref_address_type)
        ref = self.dut_scan_for_asha(dut_address_type=dut_address_type)
        return self.dut_connect_to_ref(advertisement, ref, dut_address_type)

    @parameterized(
        (RANDOM, RANDOM, RANDOM, Device.Left),
        (RANDOM, PUBLIC, PUBLIC, Device.Right),
    )  # type: ignore[misc]
    def test_disconnect_acceptor(
        self,
        dut_address_type: OwnAddressType,
        ref_left_address_type: OwnAddressType,
        ref_right_address_type: OwnAddressType,
        disconnect_device: Device,
    ) -> None:
        """
        Prerequisites: DUT and Ref are connected and bonded.
        Description:
           1. One peripheral of Ref initiates disconnection to DUT.
           2. Verify that it is disconnected and that the other peripheral is still connected.
        """
        dut_ref_left, ref_left_dut = self.advertise_and_connect(ref_device=self.ref_left,
                                                                ref_address_type=ref_left_address_type,
                                                                dut_address_type=dut_address_type)
        assert dut_ref_left
        assert ref_left_dut

        dut_ref_right, ref_right_dut = self.advertise_and_connect(ref_device=self.ref_right,
                                                                  ref_address_type=ref_right_address_type,
                                                                  dut_address_type=dut_address_type)
        assert dut_ref_right
        assert ref_right_dut

        if disconnect_device == Device.Left:
            self.ref_left.host.Disconnect(connection=ref_left_dut)
            assert self.ref_right.host.IsConnected(ref_right_dut).value == True
            assert self.ref_left.host.IsConnected(ref_left_dut).value == False
        else:
            self.ref_right.host.Disconnect(connection=ref_right_dut)
            assert self.ref_right.host.IsConnected(ref_right_dut).value == False
            assert self.ref_left.host.IsConnected(ref_left_dut).value == True


if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
