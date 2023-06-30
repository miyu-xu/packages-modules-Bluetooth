# Copyright 2023 Google LLC
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
"""Class to access the Floss Logger interface."""

from autotest_lib.client.cros.bluetooth.floss.utils import glib_call


class FlossLogger:
    """Handles method calls from the logger interface."""
    LOGGER_SERVICE = 'org.chromium.bluetooth'
    LOGGER_INTERFACE = 'org.chromium.bluetooth.Logging'
    LOGGER_OBJ_PATH_PATTERN = '/org/chromium/bluetooth/hci{}/logging'

    def __init__(self, bus, hci):
        """Constructs the logger client.

        @param bus: D-Bus bus over which we'll establish connections.
        @param hci: HCI adapter index. Get this value from `get_default_adapter`
                    on FlossManagerClient.
        """
        self.bus = bus
        self.hci = hci
        self.objpath = self.LOGGER_OBJ_PATH_PATTERN.format(hci)

    def proxy(self):
        """Gets proxy object to Logger interface for method calls."""
        return self.bus.get(self.LOGGER_SERVICE, self.objpath)[self.LOGGER_INTERFACE]

    @glib_call(None)
    def is_debug_enabled(self):
        """Checks if debug is enabled.

        @return: True on success, False on failure, None on DBus error.
        """
        return self.proxy().IsDebugEnabled()

    @glib_call(False)
    def set_debug_logging(self, enable):
        """Sets debug logging enabled or disabled.

        @param enable: Enable or disable debug logging.

        @return: True on success, False otherwise.
        """
        self.proxy().SetDebugLogging(enable)
        return True
