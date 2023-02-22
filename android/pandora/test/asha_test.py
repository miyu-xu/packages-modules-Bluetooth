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
from typing import List
from typing import Tuple

import time
from avatar import PandoraDevices
from avatar import parameterized
from avatar.aio import asynchronous
from avatar.bumble_server.security import PairingDelegate
from avatar.pandora_client import BumblePandoraClient, PandoraClient
from bumble.gatt import GATT_ASHA_SERVICE
from mobly import base_test, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from pandora.host_grpc import Connection
from pandora.host_grpc import DataTypes
from pandora.host_grpc import OwnAddressType
from pandora.security_grpc import LESecurityLevel


class ASHATest(base_test.BaseTestClass):  # type: ignore[misc]
    ASHA_UUID = GATT_ASHA_SERVICE.to_hex_str()
    HISYCNID: List[int] = [0x01, 0x02, 0x03, 0x04, 0x5, 0x6, 0x7, 0x8]

    devices: PandoraDevices
    dut: PandoraClient
    ref: BumblePandoraClient

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        dut, ref = self.devices
        assert isinstance(ref, BumblePandoraClient)
        self.dut, self.ref = dut, ref

    def teardown_class(self) -> None:
        self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        async def reset(device: PandoraClient) -> None:
            await device.aio.host.FactoryReset()
            device.address = (await device.aio.host.ReadLocalAddress(wait_for_ready=True)).address  # type: ignore[assignment]

        await asyncio.gather(reset(self.dut), reset(self.ref))

    def test_advertising1(self) -> None:
        complete_local_name = "Bumble"
        protocol_version = 0x01
        capability = 0x00
        truncated_hisyncid = ASHATest.HISYCNID[:4]

        self.ref.asha.Register(capability=capability, hisyncid=ASHATest.HISYCNID)

        advertisement = self.ref.host.Advertise(
            legacy=True,
            connectable=True,
            data=DataTypes(
                complete_local_name=complete_local_name,
                incomplete_service_class_uuids16=[ASHATest.ASHA_UUID],
            ),
        )
        scan = self.dut.host.Scan()

        scan_result = next(
            (
                x
                for x in scan
                if ASHATest.ASHA_UUID in x.data.incomplete_service_class_uuids16
            )
        )
        logging.debug(f"scan_response.data: {scan_result}")

        advertisement.cancel()
        scan.cancel()

        assert_in(ASHATest.ASHA_UUID, scan_result.data.service_data_uuid16)
        assert_equal(type(scan_result.data.complete_local_name), str)
        expected_advertisement_data = (
            "{:02x}".format(protocol_version)
            + "{:02x}".format(capability)
            + "".join([("{:02x}".format(x)) for x in truncated_hisyncid])
        )
        assert_equal(
            expected_advertisement_data,
            (scan_result.data.service_data_uuid16[ASHATest.ASHA_UUID]).hex(),
        )

    def test_advertising2(self) -> None:
        complete_local_name = "Bumble"
        protocol_version = 0x01
        capability = 0x00
        truncated_hisyncid = ASHATest.HISYCNID[:4]

        self.ref.asha.Register(capability=capability, hisyncid=ASHATest.HISYCNID)

        # advertise with ASHA service data in scan response
        advertisement = self.ref.host.Advertise(
            legacy=True,
            scan_response_data=DataTypes(
                complete_local_name=complete_local_name,
                complete_service_class_uuids16=[ASHATest.ASHA_UUID],
            ),
        )
        scan = self.dut.host.Scan()

        scan_response = next(
            (x for x in scan if ASHATest.ASHA_UUID in x.data.incomplete_service_class_uuids16)
        )
        logging.debug(f"scan_response.data: {scan_response}")

        advertisement.cancel()
        scan.cancel()

        assert_in(ASHATest.ASHA_UUID, scan_response.data.service_data_uuid16)
        expected_advertisement_data = (
            "{:02x}".format(protocol_version)
            + "{:02x}".format(capability)
            + "".join([("{:02x}".format(x)) for x in truncated_hisyncid])
        )
        assert_equal(
            expected_advertisement_data,
            (scan_response.data.service_data_uuid16[ASHATest.ASHA_UUID]).hex(),
        )

    @parameterized(
        (OwnAddressType.RANDOM, OwnAddressType.PUBLIC),
        (OwnAddressType.RANDOM, OwnAddressType.RANDOM),
    )  # type: ignore[misc]
    def test_pairing(
        self,
        dut_address_type: OwnAddressType,
        ref_address_type: OwnAddressType,
    ) -> None:
        # override reference device IO capability
        setattr(self.ref.device, "io_capability", PairingDelegate.NO_OUTPUT_NO_INPUT)

        dut_ref, ref_dut = self.connect(dut_address_type, ref_address_type)
        secure = self.dut.security.Secure(
            connection=dut_ref, le=LESecurityLevel.LE_LEVEL3
        )

        assert_equal(secure.WhichOneof("result"), "success")


    # @parameterized(
    #     (OwnAddressType.RANDOM, OwnAddressType.PUBLIC),
    #     (OwnAddressType.RANDOM, OwnAddressType.RANDOM),
    # )  # type: ignore[misc]
    def test_unbonding(
        self,
        dut_address_type: OwnAddressType = OwnAddressType.RANDOM,
        ref_address_type: OwnAddressType  = OwnAddressType.RANDOM,
    ) -> None:
        from mobly.signals import TestSkip
        raise TestSkip

        # override reference device IO capability
        setattr(self.ref.device, "io_capability", PairingDelegate.NO_OUTPUT_NO_INPUT)

        capability = 0x00
        complete_local_name = "Bumble"

        self.ref.asha.Register(capability=capability, hisyncid=ASHATest.HISYCNID)
        advertisement = self.ref.host.Advertise(
            legacy=True,
            connectable=True,
            scan_response_data=DataTypes(
                complete_local_name=complete_local_name,
                incomplete_service_class_uuids16=[ASHATest.ASHA_UUID],
            ),
            own_address_type=ref_address_type,
        )

        peers = self.dut.host.Scan(own_address_type=dut_address_type)

        ref = None
        for peer in peers:
            if complete_local_name == peer.data.complete_local_name:
                logging.debug(f"device:{complete_local_name} found")
                ref = peer
                break
        assert ref
        peers.cancel()

        # connect
        dut_ref = (
            self.dut.host.ConnectLE(
                own_address_type=dut_address_type, **ref.address_asdict()
            )
        ).connection
        ref_dut = (next(advertisement)).connection
        assert dut_ref
        assert ref_dut

        secure = self.dut.security.Secure(
            connection=dut_ref, le=LESecurityLevel.LE_LEVEL3
        )

        assert_equal(secure.WhichOneof("result"), "success")
        self.dut.host.Disconnect(dut_ref)
        self.ref.host.WaitDisconnection(ref_dut)

        # delete the bond
        if dut_address_type == OwnAddressType.PUBLIC:
            self.dut.security_storage.DeleteBond(public=self.ref.address)
        else:
            self.dut.security_storage.DeleteBond(random=self.ref.random_address)

        # DUT connect to REF again
        dut_ref = (
            self.dut.host.ConnectLE(
                own_address_type=dut_address_type, **ref.address_asdict()
            )
        ).connection
        # TODO very likely there is a bug in android here
        logging.debug('result should come out')

        advertisement.cancel()
        assert dut_ref

        secure = self.dut.security.Secure(
            connection=dut_ref, le=LESecurityLevel.LE_LEVEL3
        )

        assert_equal(secure.WhichOneof("result"), "success")

    def connect(
        self, dut_address_type: OwnAddressType, ref_address_type: OwnAddressType
    ) -> Tuple[Connection, Connection]:
        """
        Helper method for REF advertises and DUT connects to it
        :return: a Tuple (DUT to REF connection, REF to DUT connection)
        """
        capability = 0x00
        complete_local_name = "Bumble"

        self.ref.asha.Register(capability=capability, hisyncid=ASHATest.HISYCNID)
        advertisement = self.ref.host.Advertise(
            legacy=True,
            connectable=True,
            scan_response_data=DataTypes(
                complete_local_name=complete_local_name,
                incomplete_service_class_uuids16=[ASHATest.ASHA_UUID],
            ),
            own_address_type=ref_address_type,
        )

        peers = self.dut.host.Scan(own_address_type=dut_address_type)

        ref = None
        for peer in peers:
            if complete_local_name == peer.data.complete_local_name:
                logging.debug(f"device:{complete_local_name} found")
                ref = peer
                break
        peers.cancel()
        assert ref

        # connect
        dut_ref = (
            self.dut.host.ConnectLE(
                own_address_type=dut_address_type, **ref.address_asdict()
            )
        ).connection
        ref_dut = (next(advertisement)).connection
        assert dut_ref
        assert ref_dut

        advertisement.cancel()
        return dut_ref, ref_dut

    @parameterized(
        (OwnAddressType.RANDOM, OwnAddressType.RANDOM),
        (OwnAddressType.RANDOM, OwnAddressType.PUBLIC),
    )
    def test_connect(
        self, dut_address_type: OwnAddressType, ref_address_type: OwnAddressType
    ):
        dut_ref, ref_dut = self.connect(dut_address_type, ref_address_type)
        assert dut_ref
        assert ref_dut

    def test_disconnect_initiator(
        self,
        dut_address_type=OwnAddressType.RANDOM,
        ref_address_type=OwnAddressType.RANDOM,
    ) -> None:
        dut_ref, ref_dut = self.connect(dut_address_type, ref_address_type)
        assert dut_ref
        assert ref_dut
        self.dut.host.Disconnect(connection=dut_ref)

    def test_disconnect_acceptor(
        self,
        dut_address_type=OwnAddressType.RANDOM,
        ref_address_type=OwnAddressType.RANDOM,
    ) -> None:
        dut_ref, ref_dut = self.connect(dut_address_type, ref_address_type)
        assert dut_ref
        assert ref_dut
        self.ref.host.Disconnect(connection=ref_dut)

    @parameterized(
        (OwnAddressType.RANDOM, OwnAddressType.RANDOM, 0),
        (OwnAddressType.RANDOM, OwnAddressType.RANDOM, 0.5),
        (OwnAddressType.RANDOM, OwnAddressType.RANDOM, 1),
        (OwnAddressType.RANDOM, OwnAddressType.RANDOM, 5),
    )
    def test_reconnection(
        self,
        dut_address_type: OwnAddressType,
        ref_address_type: OwnAddressType,
        reconnection_gap: float,
    ) -> None:
        def connect_and_disconnect():
            dut_ref, ref_dut = self.connect(dut_address_type, ref_address_type)
            assert dut_ref
            assert ref_dut
            self.dut.host.Disconnect(connection=dut_ref)

        connect_and_disconnect()
        time.sleep(reconnection_gap)
        connect_and_disconnect()


if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
