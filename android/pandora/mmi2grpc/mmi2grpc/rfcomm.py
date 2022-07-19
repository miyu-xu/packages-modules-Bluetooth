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

    def __init__(self, channel):
        super().__init__()
        self.rfcomm = RFCOMM(channel)
        self.host = Host(channel)
        self.serverCookie = None
        self.connectionCookie = None

    @assert_description
    def TSC_RFCOMM_mmi_iut_initiate_slc(self, pts_addr: bytes, test: str, **kwargs):
        """
        Take action to initiate an RFCOMM service level connection (l2cap).
        """
        self.host.Connect(address=pts_addr)

        try:
            self.rfcomm.Connect(address=pts_addr).cookie.id
        except:
            if test == "RFCOMM/DEVA/RFC/BV-01-C":
                print(f'{test}: PTS disconnected as expected', file=sys.stderr)
                return "OK"
            else:
                raise Exception("Unexpected disconnection")
        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_accept_slc(self, pts_addr: bytes, **kwargs):
        """
        Take action to accept the RFCOMM service level connection from the
        tester.
        """

        self.serverCookie = self.rfcomm.StartServer().cookie

        self.host.WaitConnection(address=pts_addr)

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_accept_sabm(self, **kwargs):
        """
        Take action to accept the SABM operation initiated by the tester.

        Note:
        Make sure that the RFCOMM server channel is set correctly in
        TSPX_server_channel_iut
        """

        self.connectionCookie = self.rfcomm.AcceptConnection(cookie=self.serverCookie)
        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_respond_PN(self, **kwargs):
        """
        Take action to respond PN.
        """

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

    @assert_description
    def TSC_RFCOMM_mmi_iut_accept_disc(self, **kwargs):
        """
        Take action to accept the DISC operation initiated by the tester.
        """

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_accept_data_link_connection(self, **kwargs):
        """
        Take action to accept a new DLC initiated by the tester.
        """

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_initiate_close_session(self, **kwargs):
        """
        Take action to close the RFCOMM session.
        """

        self.rfcomm.Disconnect(cookie=self.connectionCookie.cookie)

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_respond_RLS(self, **kwargs):
        """
        Take action to respond RLS command.
        """

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_respond_RPN(self, **kwargs):
        """
        Take action to respond RPN.
        """

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_respond_NSC(self, **kwargs):
        """
        Take action to respond NSC.
        """

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_initiate_close_dlc(self, **kwargs):
        """
        Take action to close the DLC.
        """

        self.rfcomm.Disconnect(cookie=self.connectionCookie.cookie)

        return "OK"

    @assert_description
    def TSC_RFCOMM_mmi_iut_respond_Test(self, **kwargs):
        """
        Take action to respond Test.
        """

        return "OK"
