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
"""SDP proxy module."""

from mmi2grpc._helpers import assert_description
from mmi2grpc._proxy import ProfileProxy

from pandora.sdp_grpc import SDP
from pandora.host_grpc import Host

import sys
import threading
import os
import socket


class SDPProxy(ProfileProxy):

    def __init__(self, channel, test: str):
        super().__init__()
        self.sdp = SDP(channel)
        self.host = Host(channel)

    @assert_description
    def _mmi_6000(self, **kwargs):
        """
        If necessary take action to accept the SDP channel connection.
        """

        return "OK"

    @assert_description
    def _mmi_6001(self, **kwargs):
        """
        If necessary take action to respond to the Service Attribute operation
        appropriately.
        """

        return "OK"

    @assert_description
    def _mmi_6002(self, **kwargs):
        """
        If necessary take action to accept the Service Search operation.
        """

        return "OK"

    @assert_description
    def _mmi_6003(self, **kwargs):
        """
        If necessary take action to respond to the Service Search Attribute
        operation appropriately.
        """

        return "OK"

    @assert_description
    def TSC_SDP_mmi_verify_browsable_services(self, **kwargs):
        """
        Are all browsable service classes listed below?

        0x1800, 0x110A, 0x110C,
        0x110E, 0x1112, 0x1203, 0x111F, 0x1203, 0x1855, 0x1132, 0x1116, 0x1115,
        0x112F, 0x1105
        """

        return "OK"
