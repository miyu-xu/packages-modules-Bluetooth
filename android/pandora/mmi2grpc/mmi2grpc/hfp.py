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
"""HFP proxy module."""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.hfp_grpc import HFP
from pandora.host_grpc import Host

import os
import socket
import sys
import time
import threading

# Standard time to wait before asking for waitConnection
WAIT_DELAY_BEFORE_CONNECTION = 2


class HFPProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        os.system('adb forward tcp:4242 vsock:2:9200')
        self.hfp = HFP(channel)
        self.host = Host(channel)

        self.connection = None

    def asyncWaitConnection(self, pts_addr, delay=WAIT_DELAY_BEFORE_CONNECTION):
        """
        Send a WaitConnection in a grpc callback
        """

        def waitConnectionCallback(self, pts_addr):
            self.connection = self.host.WaitConnection(address=pts_addr)

        print(f'HFP placeholder mmi: asyncWaitConnection', file=sys.stderr)
        th = threading.Timer(interval=delay, function=waitConnectionCallback, args=(self, pts_addr))
        th.start()

    def test_started(self, test: str, pts_addr: bytes, **kwargs):
        if test in [
                'HFP/AG/ACC/BI-12-I',
                'HFP/AG/ACC/BI-13-I',
                'HFP/AG/ACC/BI-14-I',
                'HFP/AG/ACC/BV-08-I',
                'HFP/AG/ACC/BV-09-I',
                'HFP/AG/ACC/BV-10-I',
                'HFP/AG/ACC/BV-11-I',
                'HFP/AG/ACC/BV-15-I',
                'HFP/AG/ATH/BV-04-I',
                'HFP/AG/IIA/BV-01-I',
                'HFP/AG/SLC/BV-07-I',
                'HFP/AG/WBS/BV-01-I',
        ]:
            self.asyncWaitConnection(pts_addr)

        return "OK"

    @assert_description
    def TSC_delete_pairing_iut(self, pts_addr: bytes, **kwargs):
        """
        Delete the pairing with the PTS using the Implementation Under Test
        (IUT), then click Ok.
        """

        self.host.DeletePairing(address=pts_addr)
        return "OK"

    @assert_description
    def TSC_iut_connectable(self, test: str, pts_addr: bytes, **kwargs):
        """
        Make the Implementation Under Test (IUT) connectable, then click Ok.
        """

        if test in ["HFP/AG/SDP/BV-01-I"]:
            self.asyncWaitConnection(pts_addr)

        return "OK"

    @assert_description
    def TSC_INFO_slc_with_30_seconds_wait(self, **kwargs):
        """
        After clicking the OK button, PTS will connect to the IUT and then be
        idle for 30 seconds as part of the test procedure.

        Click OK to proceed.
        """

        return "OK"

    def tryCall(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(("127.0.0.1", 4242))
        s.sendall(b'REM0\r\nAT+REMOTECALL=4,0,0,"42",129\r\n')
        self.socket = s

    @assert_description
    def TSC_ag_iut_enable_call(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then place a call from an external line to the Implementation
        Under Test (IUT). Do not answer the call unless prompted to do so.
        """

        # TODO b/238708237 Not Implemented
        raise NotImplementedError(self.__class__.__name__ + '.TSC_ag_iut_enable_call See b/238708237       (╥﹏╥)')

        th = threading.Timer(interval=3, function=self.tryCall)
        th.start()
        return "OK"

    @assert_description
    def TSC_iut_enable_slc(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then initiate a service level connection from the
        Implementation Under Test (IUT) to the PTS.
        """

        if not self.connection:
            self.connection = self.host.Connect(address=pts_addr).connection
        self.hfp.EnableSlc(connection=self.connection)
        return "OK"

    @assert_description
    def TSC_ag_iut_call_no_slc(self, test: str, pts_addr: bytes, **kwargs):
        """
        Place a call from an external line to the Implementation Under Test
        (IUT).  When the call is active, click Ok.
        """

        # TODO b/238708237 Not Implemented
        raise NotImplementedError(self.__class__.__name__ +
                                  '.TSC_ag_iut_call_no_slc See b/238708237     .·´¯`(>▂<)´¯`·. ')
        self.tryCall()
        time.sleep(2)
        if test in ["HFP/AG/ATH/BV-03-I"]:
            self.asyncWaitConnection(pts_addr)

        return "OK"

    @assert_description
    def TSC_iut_search(self, **kwargs):
        """
        Using the Implementation Under Test (IUT), perform a search for the PTS.
        If found, click OK.
        """

        return "OK"

    @assert_description
    def TSC_iut_connect(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then make a connection request to the PTS from the
        Implementation Under Test (IUT).
        """

        self.connection = self.host.Connect(address=pts_addr).connection
        return "OK"

    @assert_description
    def TSC_iut_disable_slc(self, pts_addr: bytes, **kwargs):
        """
        Click Ok, then disable the service level connection using the
        Implementation Under Test (IUT).
        """

        self.hfp.DisableSlc(connection=self.connection)
        return "OK"

    @assert_description
    def TSC_make_battery_charged(self, **kwargs):
        """
        Click Ok, then manipulate the Implementation Under Test (IUT) so that
        the battery is fully charged.
        """

        self.hfp.SetBatteryLevel(connection=self.connection, battery_percentage=100)

        return "OK"

    @assert_description
    def TSC_make_battery_discharged(self, **kwargs):
        """
        Manipulate the Implementation Under Test (IUT) so that the battery level
        is not fully charged, then click Ok.
        """

        self.hfp.SetBatteryLevel(connection=self.connection, battery_percentage=42)

        return "OK"

    @assert_description
    def TSC_disable_inband_ring(self, **kwargs):
        """
        Click Ok, then disable the in-band ringtone using the Implemenation
        Under Test (IUT).
        """
        self.hfp.DisableInbandRing()

        return "OK"

    @assert_description
    def TSC_disable_ag_cellular_network_expect_notification(self, **kwargs):
        """
        Click OK. Then, disable the control channel, such that the AG is de-
        registered.
        """

        return "OK"

    def TSC_signal_strength_verify(self, **kwargs):
        """
        Verify that the signal reported on the Implementaion Under Test (IUT) is
        proportional to the value (out of 5), then click Ok.x
        """

        print(
            f'Try to print from TSC_signal_strength_verify' + ", ".join(
                f"{key}={value}" for key, value in kwargs.items()),
            file=sys.stderr)
        return "OK"

    @assert_description
    def TSC_signal_strength_impair(self, **kwargs):
        """
        Impair the cellular signal by placing the Implementation Under Test
        (IUT) under partial RF shielding, then click Ok.
        """

        return "OK"

    @assert_description
    def TSC_prepare_iut_for_vra(self, **kwargs):
        """
        Place the Implementation Under Test (IUT) in a state which will allow a
        request from the PTS to activate voice recognition, then click Ok.
        """

        return "OK"

    @assert_description
    def TSC_iut_disable_network(self, **kwargs):
        """
        Click Ok, then disable the cellular network using the Implementation
        Under Test (IUT) by performing one of the below actions:

        1. If the IUT
        is an Audio Gateway (AG), turn the cellular network using the UI.
        2.
        Place the PTS and IUT in an RF shield box. Once the network is disabled
        the PTS will send an alert to your machine confirming the network
        connection was lost.  Please note that speakers are needed to hear the
        said alert.
        """

        raise NotImplementedError(self.__class__.__name__ + '.TSC_iut_disable_network       (ง •̀_•́)ง  ')
        return "OK"

    @assert_description
    def TSC_ag_prepare_at_bldn(self, **kwargs):
        """
        Place the Implemenation Under Test (IUT) in a state which will accept an
        outgoing call set-up request from the PTS, then click OK.  

        Note:  The
        PTS will send a request to establish an outgoing call from the IUT to
        the last dialed number.  Answer the incoming call when alerted.
        """

        # self.tryCall()
        # time.sleep(10)
        return "OK"

    @assert_description
    def TSC_ag_iut_prepare_for_atd(self, **kwargs):
        """
        Place the Implementation Under Test (IUT) in a mode that will allow an
        outgoing call initiated by the PTS, and click Ok.
        """

        return "OK"

    @assert_description
    def TSC_terminal_answer_call(self, **kwargs):
        """
        Click Ok, then answer the incoming call on the external terminal.
        """

        return "OK"

    @assert_description
    def TSC_ag_iut_clear_call_history(self, **kwargs):
        """
        Clear the call history on  the Implementation Under Test (IUT) such that
        there are zero records of any numbers dialed, then click Ok.
        """

        return "OK"
