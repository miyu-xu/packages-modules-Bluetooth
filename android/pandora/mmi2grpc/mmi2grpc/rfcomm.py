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
"""Rfcomm proxy module."""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.rfcomm_grpc import RFCOMM
from pandora.host_grpc import Host

import sys
import threading
import os
import socket


class RFCOMMProxy(ProfileProxy):
    # The UUID for Serial-Port Profile
    SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb"
    TSPX_SERVICE_NAME_TESTER = "COM5"

    def __init__(self, channel):
        super().__init__()
        self.rfcomm = RFCOMM(channel)
        self.host = Host(channel)

    @assert_description
    def TSC_RFCOMM_mmi_iut_initiate_slc(self, pts_addr: bytes, test: str, **kwargs):
        """
        Take action to initiate an RFCOMM service level connection (l2cap).
        """

        cookie = self.rfcomm.createInsecureRfcommSocket(address=pts_addr, uuid=RFCOMMProxy.SPP_UUID)

        # Return early if we don't need to complete the connection.
        if test == "RFCOMM/DEVA/RFC/BV-01-C":
            self.rfcomm.startConnectRfcommDevice(id=cookie.id)
            return "OK"

        cookie = self.rfcomm.connectRfcommDevice(id=cookie.id)
        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_initiate_sabm_control_channel(self, **kwargs):
        """
        Take action to initiate an SABM operation for the RFCOMM control
        channel.
        """

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_initiate_PN(self, **kwargs):
        """
        Take action to initiate PN.
        """

        return "OK"

    def TSC_RFCOMM_mmi_iut_initiate_sabm_data_channel(self, **kwargs):
        """
        Take action to initiate an SABM operation for an RFCOMM data channel.
        Note: RFCOMM server channel can be found on PTS's SDP record
        """

        return "OK"
