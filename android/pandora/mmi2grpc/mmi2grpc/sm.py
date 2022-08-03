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
"""SMP proxy module."""

from multiprocessing import connection
import time
import sys

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.security_grpc import Security
from pandora.host_grpc import Host

# The tests needs the MMI to accept pairing confirmation request.
NEEDS_PAIRING_CONFIRMATION = {
    "SM/CEN/EKS/BV-01-C",
    "SM/CEN/JW/BI-04-C",
    "SM/CEN/JW/BI-01-C",
    "SM/CEN/KDU/BV-04-C",
    "SM/CEN/KDU/BV-05-C",
    "SM/CEN/KDU/BV-06-C",
    "SM/CEN/KDU/BV-10-C",
    "SM/CEN/KDU/BV-11-C",
    "SM/CEN/PKE/BV-01-C",
    "SM/CEN/PKE/BV-04-C",
    "SM/CEN/SCJW/BV-04-C",
    "SM/CEN/SCPK/BV-04-C",
    "SM/CEN/SCCT/BV-03-C",
    "SM/CEN/SCCT/BV-09-C",
    "SM/CEN/SCCT/BV-07-C",
}

ACCEPTS_REMOTE_PAIRING_CONFIRMATION = {
    "SM/CEN/KDU/BI-01-C",
    "SM/CEN/KDU/BI-02-C",
    "SM/CEN/KDU/BI-03-C",
    "SM/CEN/SIP/BV-02-C",
    "SM/PER/JW/BV-02-C",
    "SM/PER/PROT/BV-02-C",
    "SM/PER/JW/BV-02-C",
}


class SMProxy(ProfileProxy):

    def __init__(self, channel):
        super().__init__()
        self.security = Security(channel)
        self.host = Host(channel)
        self.connection = None

    @assert_description
    def MMI_IUT_ENABLE_CONNECTION_SM(self, test, pts_addr: bytes, **kwargs):
        """
        Initiate an connection from the IUT to the PTS.
        """
        self.connection = self.host.ConnectLE(address=pts_addr).connection
        if self.connection and test in ACCEPTS_REMOTE_PAIRING_CONFIRMATION:
            self.security.ProvidePairingConfirmation(connection=self.connection, pairing_confirmation_value=True)
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_PAIRING_PROCESS(self, test, **kwargs):
        """
        Please start pairing process.
        """
        if self.connection:
            self.security.Pair(connection=self.connection)
            if test in NEEDS_PAIRING_CONFIRMATION:
                self.security.ProvidePairingConfirmation(connection=self.connection, pairing_confirmation_value=True)

        return "OK"

    @assert_description
    def MMI_IUT_SEND_DISCONNECTION_REQUEST(self, **kwargs):
        """
        Please initiate a disconnection to the PTS.

        Description: Verify that
        the Implementation Under Test(IUT) can initiate a disconnect request to
        PTS.
        """
        if self.connection:
            self.host.DisconnectLE(connection=self.connection)
            self.connection = None
        return "OK"

    def MMI_LESC_NUMERIC_COMPARISON(self, pts_addr, description, **kwargs):
        """
        Please confirm the following number matches IUT: 385874.
        """
        passkey = self.sm.GetPasskey(address=pts_addr).passkey
        print(f'The description:  {description[-8:-1]}', file=sys.stderr)
        print(f'The passkey:  {passkey}', file=sys.stderr)
        # return f'{description[-8:-1]}'
        # return f'{passkey}'
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_RESET(self, **kwargs):
        """
        Please reset your device.
        """
        self.host.SoftReset()
        return "OK"

    @assert_description
    def MMI_IUT_SMP_TIMEOUT_30_SECONDS(self, **kwargs):
        """
        Wait for the 30 seconds. Lower tester will not send corresponding or
        next SMP message.
        """
        return "OK"

    @assert_description
    def MMI_ENTER_PASSKEY_CODE(self, pts_addr, **kwargs):
        """
        Please enter 6 digit passkey code.
        """
        # SM/CEN/PKE/BI-01-C
        # SM/CEN/SCPK/BV-04-C
        # SM/CEN/SCPK/BI-01-C
        passkey = self.sm.GetPasskey(address=pts_addr).passkey
        print(f'Sending passkey:  {passkey}', file=sys.stderr)
        return f'{passkey}'

    @assert_description
    def MMI_ENTER_WRONG_DYNAMIC_PASSKEY_CODE(self, **kwargs):
        """
        Please enter invalid 6 digit pin code.
        """
        # SM/CEN/PKE/BI-02-C
        return "OK"

    @assert_description
    def MMI_IUT_INITIATE_CONNECTION_BR_EDR_PAIRING(self, pts_addr, **kwargs):
        """
        Please initiate a connection over BR/EDR to the PTS, and initiate
        pairing process.

        Description: Verify that the Implementation Under Test
        (IUT) can initiate a connect request over BR/EDR to PTS, and initiate
        pairing process.
        """
        print(
            f'******* Calling MMI IUT INIT BREDR 2001 {pts_addr}', file=sys.stderr)
        # self.host.Connect(address=pts_addr)
        # SM/CEN/SCCT/BV-03-C
        # SM/CEN/SCCT/BV-05-C
        # self.sm.Pair(address=pts_addr)
        self.sm.CreateClassicConnection(address=pts_addr)
        # self.sm.ProvidePairingConfirmation(
        #     address=pts_addr, pairing_confirmation_value=True)
        # self.host.Connect(address=pts_addr)
        return "OK"

    @assert_description
    def MMI_IUT_SMP_TIMEOUT_ADDITIONAL_10_SECONDS(self, **kwargs):
        """
        Wait for an additional 10 seconds. Lower test will send corresponding or
        next SMP message.
        """
        # SM/CEN/PROT/BV-01-C
        time.sleep(10)
        return "OK"

    @assert_description
    def MMI_IUT_ABORT_PAIRING_PROCESS_DISCONNECT(self, **kwargs):
        """
        Lower tester expects IUT aborts pairing process, and disconnect.
        """
        # SM/CEN/PKE/BI-02-C
        return "OK"

    @assert_description
    def MMI_TESTER_ENABLE_CONNECTION_SM(self, pts_addr, **kwargs):
        """
        Action: Place the IUT in connectable mode
        """
        self.sm.EnableConnectableMode(address=pts_addr)
        print(f'Enabled scan', file=sys.stderr)
        return "OK"

    @assert_description
    def MMI_IUT_ACCEPT_CONNECTION_BR_EDR(self, pts_addr, **kwargs):
        """
        Please prepare IUT into a connectable mode in BR/EDR.

        Description:
        Verify that the Implementation Under Test (IUT) can accept a connect
        request from PTS.
        """
        print(f'Calling accept br edr  {pts_addr}', file=sys.stderr)
        # self.sm.ProvidePairingConfirmation(
        #     address=pts_addr, pairing_confirmation_value=True)
        return "OK"

    @assert_description
    def _mmi_2001(self, pts_addr, **kwargs):
        """
        Please verify the passKey is correct: 000000
        """
        print(f'Calling mmi 2001 {pts_addr}', file=sys.stderr)
        return "OK"

    @assert_description
    def MMI_ASK_IUT_PERFORM_FEATURE_EXCHANGE_OVER_BR(self, pts_addr, **kwargs):
        """
        Please start pairing feature exchange over BR/EDR.
        """
        return "OK"
