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

    @assert_description
    def MMI_IUT_INITIATE_BR_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection over BR/EDR to the PTS.
    
        Description:
        Verify that the Implementation Under Test (IUT) can initiate GATT
        connect request over BR/EDR to PTS.
        """

        self.gatt.ConnectBrEdr(address=pts_addr)

        return "OK"

    @assert_description
    def MMI_IUT_INITIATE_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.
    
        Description: Verify that
        the Implementation Under Test (IUT) can initiate GATT connect request to
        PTS.
        """

        self.gatt.ConnectLe(address=pts_addr)
    
        return "OK"

    @assert_description
    def MMI_IUT_MTU_EXCHANGE(self, **kwargs):
        """
        Please send exchange MTU command to the PTS.
    
        Description: Verify that
        the Implementation Under Test (IUT) can send Exchange MTU command to the
        tester.
        """
        self.gatt.ExchangeMTU(mtu=512)
        return "OK"

    def MMI_IUT_SEND_PREPARE_WRITE_REQUEST_VALID_SIZE(self, description: str, **kwargs):
        """
        Please send prepare write request with handle = 'FFFF'O and size = 'XXX'
        to the PTS.
    
        Description: Verify that the Implementation Under Test
        (IUT) can send data according to negotiate MTU size.
        """
        self.gatt.DiscoverServices()

        matches = re.findall("'([a0-Z9]*)'O and size = '([a0-Z9]*)'", description)
        handle = [matches[0][0]][0]
        size = int(matches[0][1])
        self.gatt.WriteCharacteristic(handle=handle, size=size)
    
        return "OK"

    @assert_description
    def MMI_IUT_INITIATE_DISCONNECTION(self, **kwargs):
        """
        Please initiate a GATT disconnection to the PTS.
    
        Description: Verify
        that the Implementation Under Test (IUT) can initiate GATT disconnect
        request to PTS.
        """

        self.gatt.Disconnect()
    
        return "OK"

    def MMI_CONFIRM_PASSKEY(self, pts_addr: bytes, **kwargs):

        self.gatt.ConfirmPasskey(address=pts_addr)

        return "OK"

    @assert_description
    def MMI_IUT_DISCOVER_PRIMARY_SERVICES(self, **kwargs):
        """
        Please send discover all primary services command to the PTS.
        Description: Verify that the Implementation Under Test (IUT) can send
        Discover All Primary Services.
        """

        self.gatt.DiscoverServices()
    
        return "OK"

    def MMI_SEND_PRIMARY_SERVICE_UUID(self, description: str, pts_addr: bytes, **kwargs):
        """
        Please send discover primary services with UUID value set to 'XXXX'O to
        the PTS.
    
        Description: Verify that the Implementation Under Test (IUT)
        can send Discover Primary Services UUID = 'XXXX'O.
        """

        uuid = re.findall("'([a0-Z9]*)'O", description)
        self.gatt.DiscoverServicesByUUID(uuid=uuid, address=pts_addr)
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
    
        return "OK"

    def MMI_SEND_PRIMARY_SERVICE_UUID_128(self, description: str, pts_addr: bytes, **kwargs):
        """
        Please send discover primary services with UUID value set to
        'XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX'O to the PTS.
    
        Description:
        Verify that the Implementation Under Test (IUT) can send Discover
        Primary Services UUID = 'XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX'O.
        """

        uuid = re.findall("'([a0-Z9-]*)'O", description)
        uuid[0] = uuid[0][:4]+uuid[0][5:]
        uuid[0] = uuid[0][:28]+uuid[0][29:]
        uuid[0] = uuid[0][:32]+uuid[0][33:]
        self.gatt.DiscoverServicesByUUID128(uuid=uuid, address=pts_addr)
    
        return "OK"

    @assert_description
    def MMI_CONFIRM_NO_PRIMARY_SERVICE_SMALL(self, **kwargs):
        """
        Please confirm that IUT received NO service uuid found in the small
        database file. Click Yes if NO service found, otherwise click No.
        Description: Verify that the Implementation Under Test (IUT) can send
        Discover primary service by UUID in small database.
        """
    
        return "OK"

    def MMI_CONFIRM_PRIMARY_SERVICE_UUID_128(self, **kwargs):
        """
        Please confirm IUT received primary services uuid=
        'XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX'O, Service start handle =
        'XXXX'O, end handle = 'XXXX'O in database. Click Yes if IUT received it,
        otherwise click No.
    
        Description: Verify that the Implementation Under
        Test (IUT) can send Discover primary service by UUID in database.
        """
    
        return "OK"

    @assert_description
    def MMI_IUT_FIND_INCLUDED_SERVICES(self, **kwargs):
        """
        Please send discover all include services to the PTS to discover all
        Include Service supported in the PTS. Discover primary service if
        needed.
    
        Description: Verify that the Implementation Under Test (IUT)
        can send Discover all include services command.
        """
    
        self.gatt.DiscoverServices()

        return "OK"

    @assert_description
    def MMI_CONFIRM_NO_INCLUDE_SERVICE(self, **kwargs):
        """
        There is no include service in the database file.
    
        Description: Verify
        that the Implementation Under Test (IUT) can send Discover all include
        services in database.
        """
    
        return "OK"


    def MMI_CONFIRM_INCLUDE_SERVICE(self, **kwargs):
        """
        Please confirm IUT received include services: 
        Attribute Handle =
        'XXXX'O Included Service Attribute handle = 'XXXX'O,End Group Handle =
        'XXXX'O,Service UUID = 'XXXX'O 
    
        Attribute Handle = 'XXXX'O Included
        Service Attribute handle = 'XXXX'O,End Group Handle = 'XXXX'O,Service
        UUID = 'XXXX'O 
    
        Attribute Handle = 'XXXX'O Included Service Attribute
        handle = 'XXXX'O,End Group Handle = 'XXXX'O,Service UUID = 'XXXX'O
        Click Yes if IUT received it, otherwise click No.
    
        Description: Verify
        that the Implementation Under Test (IUT) can send Discover all include
        services in database.
        """
    
        return "OK"

    def MMI_IUT_DISCOVER_SERVICE_UUID(self, description: str, **kwargs):
        """
        Discover all characteristics of service UUID= 'XXXX'O,  Service start
        handle = 'XXXX'O, end handle = 'XXXX'O.
    
        Description: Verify that the
        Implementation Under Test (IUT) can send Discover all charactieristics
        of a service.
        """

        service16BitUuid = re.findall("UUID= '([a0-Z9]*)'O", description)
        self.gatt.DiscoverCharacteristics(service_uuids=service16BitUuid)
    
        return "OK"

    def MMI_CONFIRM_ALL_CHARACTERISTICS_SERVICE(self, **kwargs):
        """
        Please confirm IUT received all characteristics of service
        handle='XXXX'O handle='XXXX'O handle='XXXX'O handle='XXXX'O
        handle='XXXX'O handle='XXXX'O handle='XXXX'O handle='XXXX'O
        handle='XXXX'O handle='XXXX'O handle='XXXX'O  in database. Click Yes if
        IUT received it, otherwise click No.
    
        Description: Verify that the
        Implementation Under Test (IUT) can send Discover all characteristics of
        a service in database.
        """
    
        return "OK"

    def MMI_IUT_DISCOVER_SERVICE_UUID_RANGE(self, **kwargs):
        """
        Please send discover characteristics by UUID. Range start from handle =
        'XXXX'O end handle = 'XXXX'O characteristics UUID = 0xXXXX'O.
        Description: Verify that the Implementation Under Test (IUT) can send
        Discover characteristics by UUID.
        """

        return "OK"

    def MMI_CONFIRM_CHARACTERISTICS(self, **kwargs):
        """
        Please confirm IUT received characteristic handle='XXXX'O UUID='XXXX'O
        in database. Click Yes if IUT received it, otherwise click No.
        Description: Verify that the Implementation Under Test (IUT) can send
        Discover primary service by UUID in database.
        """

        return "OK"

    @assert_description
    def MMI_CONFIRM_NO_CHARACTERISTICSUUID_SMALL(self, **kwargs):
        """
        Please confirm that IUT received NO 128 bit uuid in the small database
        file. Click Yes if NO handle found, otherwise click No.
    
        Description:
        Verify that the Implementation Under Test (IUT) can discover
        characteristics by UUID in small database.
        """
    
        return "OK"

    def MMI_IUT_DISCOVER_DESCRIPTOR_RANGE(self, **kwargs):
        """
        Please send discover characteristics descriptor range start from handle
        = 'XXXX'O end handle = 'XXXX'O to the PTS.
    
        Description: Verify that the
        Implementation Under Test (IUT) can send Discover characteristics
        descriptor.
        """

        self.gatt.DiscoverDescriptors()
    
        return "OK"

    def MMI_IUT_DISCOVER_ALL_SERVICE_RECORD(self, **kwargs):
        """
        Please send Service Discovery to discover all primary Services. Click
        YES if GATT='XXXX'O services are discovered, otherwise click No.
        Description: Verify that the Implementation Under Test (IUT) can
        discover basic rate all primary services.
        """

        #TODO Real check
        self.gatt.DiscoverServices()
    
        return "OK"

    def MMI_IUT_SEND_INDICATION(self, description: str, **kwargs):
        """
        Please write to client characteristic configuration handle = 'XXXX'O to
        enable indication to the PTS. Discover all characteristics if needed.
        Description: Verify that the Implementation Under Test (IUT) can receive
        indication sent from PTS.
        """

        self.gatt.DiscoverServices()

        matches = re.findall("'([a0-Z9]*)'O and size = '([a0-Z9]*)'", description)
        handle = [matches[0][0]][0]
        size = int(matches[0][1])
        self.gatt.WriteCharacteristic(handle=handle, size=2)
    
        return "OK"

    def MMI_IUT_SEND_READ_CHARACTERISTIC_HANDLE(self, description: str, **kwargs):
        """
        Please send read characteristic handle = 'XXXX'O to the PTS.
        Description: Verify that the Implementation Under Test (IUT) can send
        Read characteristic.
        """

        handle = re.findall("'([a0-Z9]*)'O", description)[0]
        self.gatt.ReadCharacteristic(handle=handle)
        return "OK"