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
import avatar
import logging

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from bumble import pandora as bumble_server
from bumble.pairing import PairingConfig
from bumble.core import BT_HANDSFREE_SERVICE, BT_RFCOMM_PROTOCOL_ID, BT_BR_EDR_TRANSPORT
from bumble.rfcomm import Client as RfcommClient
from bumble.sdp import (
    Client as SdpClient,
    DataElement,
    ServiceAttribute,
    SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
    SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
    SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
)
from bumble.hfp import HfpProtocol
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_not_in  # type: ignore
from pandora.host_pb2 import RANDOM, Connection, DataTypes
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer, SecureResponse
from typing import Optional, Tuple


class HfpClientTest(base_test.BaseTestClass):  # type: ignore[misc]
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

    @avatar.asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def bredr_connect_and_pair(self) -> None:
        dut_ref, ref_dut = await asyncio.gather(
            self.dut.aio.host.WaitConnection(address=self.ref.address),
            self.ref.aio.host.Connect(address=self.dut.address),
        )

        assert_equal(dut_ref.result_variant(), 'connection')
        assert_equal(ref_dut.result_variant(), 'connection')
        assert dut_ref.connection is not None and ref_dut.connection is not None

        dut_ref_sec, ref_dut_sec = await asyncio.gather(
            self.dut.aio.security.Secure(connection=dut_ref.connection, le=LE_LEVEL3),
            self.ref.aio.security.WaitSecurity(connection=ref_dut.connection, le=LE_LEVEL3),
        )
        assert_equal(dut_ref_sec.result_variant(), 'success')
        assert_equal(ref_dut_sec.result_variant(), 'success')

        return dut_ref.connection, ref_dut.connection

    @avatar.asynchronous
    async def test_hf_indicator_setup(self) -> None:
        if isinstance(self.dut, BumblePandoraDevice):
            raise signals.TestSkip('TODO: Fix test for Bumble DUT')
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip('Test require Bumble as reference device(s)')

        dut_ref, ref_dut = await self.bredr_connect_and_pair()

        ref_dut_hfp_protocol = connect_hfp_protocol(self.ref, ref_dut)

        while True:
            line = await ref_dut_hfp_protocol.next_line()

            if line.startswith('AT+BRSF='):
                ref_dut_hfp_protocol.send_response_line('+BRSF: 30')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+CIND=?'):
                ref_dut_hfp_protocol.send_response_line('+CIND: ("call",(0,1)),("callsetup",(0-3)),("service",(0-1)),'
                                                        '("signal",(0-5)),("roam",(0,1)),("battchg",(0-5)),'
                                                        '("callheld",(0-2))')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+CIND?'):
                ref_dut_hfp_protocol.send_response_line('+CIND: 0,0,1,4,1,5,0')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+CHLD=?'):
                ref_dut_hfp_protocol.send_response_line('+CHLD: 0')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith('AT+BTRH?'):
                ref_dut_hfp_protocol.send_response_line('+BTRH: 0')
                ref_dut_hfp_protocol.send_response_line('OK')
            elif line.startswith((
                    'AT+CLIP=',
                    'AT+VGS=',
                    'AT+BIA=',
                    'AT+CMER=',
                    'AT+XEVENT=',
                    'AT+XAPL=',
            )):
                ref_dut_hfp_protocol.send_response_line('OK')
            else:
                ref_dut_hfp_protocol.send_response_line('ERROR')


async def connect_hfp_protocol(device, connection):
    raw_conection = device.lookup_connection(int.from_bytes(connection.cookie.value, 'big'))
    # Get a list of all the Handsfree services (should only be 1)
    rfc_channels = await list_rfcomm_channels(device, raw_conection)
    # Pick the first one
    rfc_channel = rfc_channels[0]
    rfcomm_client = RfcommClient(device, raw_conection)
    rfcomm_mux = await rfcomm_client.start()

    session = await rfcomm_mux.open_dlc(rfc_channel)
    return HfpProtocol(session)


async def list_rfcomm_channels(device, connection):
    # Connect to the SDP Server
    sdp_client = SdpClient(device)
    await sdp_client.connect(connection)

    # Search for services that support the Handsfree Profile
    search_result = await sdp_client.search_attributes(
        [BT_HANDSFREE_SERVICE],
        [
            SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID,
            SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
            SDP_SERVICE_CLASS_ID_LIST_ATTRIBUTE_ID,
        ],
    )
    rfcomm_channels = []
    for attribute_list in search_result:
        # Look for the RFCOMM Channel number
        protocol_descriptor_list = ServiceAttribute.find_attribute_in_list(attribute_list,
                                                                           SDP_PROTOCOL_DESCRIPTOR_LIST_ATTRIBUTE_ID)
        if protocol_descriptor_list:
            for protocol_descriptor in protocol_descriptor_list.value:
                if len(protocol_descriptor.value) >= 2:
                    if protocol_descriptor.value[0].value == BT_RFCOMM_PROTOCOL_ID:
                        rfcomm_channels.append(protocol_descriptor.value[1].value)

                        # List profiles
                        bluetooth_profile_descriptor_list = (ServiceAttribute.find_attribute_in_list(
                            attribute_list,
                            SDP_BLUETOOTH_PROFILE_DESCRIPTOR_LIST_ATTRIBUTE_ID,
                        ))
                        if bluetooth_profile_descriptor_list:
                            if bluetooth_profile_descriptor_list.value:
                                if (bluetooth_profile_descriptor_list.value[0].type == DataElement.SEQUENCE):
                                    bluetooth_profile_descriptors = (bluetooth_profile_descriptor_list.value)
                                else:
                                    # Sometimes, instead of a list of lists, we just
                                    # find a list. Fix that
                                    bluetooth_profile_descriptors = [bluetooth_profile_descriptor_list]

    await sdp_client.disconnect()
    return rfcomm_channels


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
