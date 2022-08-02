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

import re
from threading import Thread

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.gatt_grpc import GATT
from pandora.host_grpc import Host
from pandora.host_pb2 import Connection


class GATTProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.gatt = GATT(channel)
        self.host = Host(channel)
        self.connection = None

    # Connection related MMIs. Used to set the connection token used in other MMIs.

    @assert_description
    def MMI_IUT_INITIATE_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.

        Description: Verify that
        the Implementation Under Test (IUT) can initiate GATT connect request to
        PTS.
        """

        self.connection = self.host.ConnectLE(address=pts_addr).connection
        return "OK"

    @assert_description
    def MMI_IUT_INITIATE_BR_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection over BR/EDR to the PTS.

        Description:
        Verify that the Implementation Under Test (IUT) can initiate GATT
        connect request over BR/EDR to PTS.
        """

        Thread(target=asyncBrConnect, args=(pts_addr, self)).start()
        return "OK"

    @assert_description
    def MMI_IUT_INITIATE_DISCONNECTION(self, **kwargs):
        """
        Please initiate a GATT disconnection to the PTS.

        Description: Verify
        that the Implementation Under Test (IUT) can initiate GATT disconnect
        request to PTS.
        """

        assert self.connection is not None
        self.host.DisconnectLE(connection=self.connection)
        self.connection = None
        return "OK"

    @assert_description
    def MMI_CONFIRM_PASSKEY(self, **kwargs):
        """
        Please verify the passKey is correct: 000000
        """

        return "OK"

    # GATT specific MMIs.

    @assert_description
    def MMI_IUT_MTU_EXCHANGE(self, **kwargs):
        """
        Please send exchange MTU command to the PTS.

        Description: Verify that
        the Implementation Under Test (IUT) can send Exchange MTU command to the
        tester.
        """

        assert self.connection is not None
        self.gatt.ExchangeMTU(mtu=512, connection=self.connection)
        return "OK"

    def MMI_IUT_SEND_PREPARE_WRITE_REQUEST_VALID_SIZE(self, description: str, **kwargs):
        """
        Please send prepare write request with handle = 'XXXX'O and size = 'XXX'
        to the PTS.

        Description: Verify that the Implementation Under Test
        (IUT) can send data according to negotiate MTU size.
        """

        assert self.connection is not None
        matches = re.findall("'([a0-Z9]*)'O and size = '([a0-Z9]*)'", description)
        handle = int(matches[0][0], 16)
        data = bytes([1]) * int(matches[0][1])
        self.gatt.WriteCharacteristicFromHandle(connection=self.connection, handle=handle, value=data)
        return "OK"

    @assert_description
    def MMI_IUT_DISCOVER_PRIMARY_SERVICES(self, **kwargs):
        """
        Please send discover all primary services command to the PTS.
        Description: Verify that the Implementation Under Test (IUT) can send
        Discover All Primary Services.
        """

        assert self.connection is not None
        self.gatt.DiscoverServiceByUuid(connection=self.connection, uuid="2800")
        return "OK"

    def MMI_SEND_PRIMARY_SERVICE_UUID(self, description: str, **kwargs):
        """
        Please send discover primary services with UUID value set to 'XXXX'O to
        the PTS.

        Description: Verify that the Implementation Under Test (IUT)
        can send Discover Primary Services UUID = 'XXXX'O.
        """

        assert self.connection is not None
        uuid = re.findall("'([a0-Z9]*)'O", description)[0]
        self.gatt.DiscoverServiceByUuid(connection=self.connection, uuid=uuid)
        return "OK"

    def MMI_SEND_PRIMARY_SERVICE_UUID_128(self, description: str, **kwargs):
        """
        Please send discover primary services with UUID value set to
        'XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX'O to the PTS.

        Description:
        Verify that the Implementation Under Test (IUT) can send Discover
        Primary Services UUID = 'XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX'O.
        """

        assert self.connection is not None
        uuid = re.findall("'([a0-Z9-]*)'O", description)[0]
        self.gatt.DiscoverServiceByUuid(connection=self.connection, uuid=uuid)
        return "OK"

    def MMI_CONFIRM_PRIMARY_SERVICE_UUID(self, **kwargs):
        """
        Please confirm IUT received primary services uuid = 'XXXX'O , Service
        start handle = 'XXXX'O, end handle = 'XXXX'O in database. Click Yes if
        IUT received it, otherwise click No.

        Description: Verify that the
        Implementation Under Test (IUT) can send Discover primary service by
        UUID in database.
        """

        # Android doesn't store services discovered by UUID
        return "Yes"

    @assert_description
    def MMI_CONFIRM_NO_PRIMARY_SERVICE_SMALL(self, **kwargs):
        """
        Please confirm that IUT received NO service uuid found in the small
        database file. Click Yes if NO service found, otherwise click No.
        Description: Verify that the Implementation Under Test (IUT) can send
        Discover primary service by UUID in small database.
        """

        # Android doesn't store services discovered by UUID
        return "Yes"

    def MMI_CONFIRM_PRIMARY_SERVICE_UUID_128(self, **kwargs):
        """
        Please confirm IUT received primary services uuid=
        'XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX'O, Service start handle =
        'XXXX'O, end handle = 'XXXX'O in database. Click Yes if IUT received it,
        otherwise click No.

        Description: Verify that the Implementation Under
        Test (IUT) can send Discover primary service by UUID in database.
        """

        # Android doesn't store services discovered by UUID
        return "Yes"


# Asynchronous utils


def asyncBrConnect(pts_addr: bytes, proxy: GATTProxy):
    proxy.connection = proxy.host.Connect(address=pts_addr).connection
    proxy.gatt.ConnectGattBrEdr(connection=proxy.connection)