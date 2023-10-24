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
"""Client class to access the Floss battery manager interface."""

import logging
import math
import random

from floss.pandora.floss import observer_base
from floss.pandora.floss import utils


class BluetoothBatteryManagerCallbacks:
    """Callbacks used by BluetoothBatteryManager.

    Implement this to receive callbacks from BluetoothBatteryManager.
    """

    def on_battery_info_updated(self, remote_address, battery_set):
        """Called when battery information is updated.

        Args:
            remote_address: The bluetooth address of the device to update.
            battery_set: Updated battery info.
        """
        pass


class FlossBatteryManagerClient(BluetoothBatteryManagerCallbacks):
    """Handles method calls and callbacks from the battery manager interface."""

    BLUETOOTH_DBUS = 'org.chromium.bluetooth'
    BATTERY_MANAGER_INTERFACE = 'org.chromium.bluetooth.BatteryManager'
    BATTERY_MANAGER_OBJ_PATH_PATTERN = '/org/chromium/bluetooth/hci{}/battery_manager'
    BATTERY_MANAGER_CB_INTF = 'org.chromium.bluetooth.BatteryManagerCallback'
    BATTERY_CB_OBJ_PATTERN = '/org/chromium/bluetooth/hci{}/test_battery_client{}'

    @staticmethod
    def parse_dbus_battery_info(battery_info):
        """Extracts battery info tuple.

        Args:
            battery_info: Battery information obtained from D-Bus.

        Returns:
            Battery info tuple on success; None otherwise.
        """
        if not battery_info:
            return None

        optional_info = battery_info['optional_value']
        keys = ['address', 'source_uuid', 'source_info', 'batteries']

        if set(keys) <= optional_info.keys():
            return tuple(optional_info[k] for k in keys)
        return None

    class ExportedBatteryManagerCallbacks(observer_base.ObserverBase):
        """
        <node>
            <interface name="org.chromium.bluetooth.BatteryManagerCallback">
                <method name="OnBatteryInfoUpdated">
                    <arg type="s" name="remote_address" direction="in" />
                    <arg type="a{sv}" name="battery_set" direction="in" />
                </method>
            </interface>
        </node>
        """

        def __init__(self):
            """Constructs exported callbacks object."""
            observer_base.ObserverBase.__init__(self)

        def OnBatteryInfoUpdated(self, remote_address, battery_set):
            """Handles battery info updated callback.

            Args:
                remote_address: The bluetooth address of the device to update.
                battery_set: Updated battery info.
            """
            for observer in self.observers.values():
                observer.on_battery_info_updated(remote_address, battery_set)

    def __init__(self, bus, hci):
        """Constructs the client.

        Args:
            bus: D-Bus bus over which we'll establish connections.
            hci: HCI adapter index. Get this value from `get_default_adapter` on FlossManagerClient.
        """
        self.bus = bus
        self.hci = hci
        self.objpath = self.BATTERY_MANAGER_OBJ_PATH_PATTERN.format(hci)

        # We don't register callbacks by default.
        self.callbacks = None
        self.callback_id = None

    def __del__(self):
        """Destructor"""
        del self.callbacks

    @utils.glib_callback()
    def on_battery_info_updated(self, remote_address, battery_set):
        """Handles battery information updated callback.

        Args:
            remote_address: The bluetooth address of the device to update.
            battery_set: Updated battery info.
        """
        logging.debug('on_battery_info_updated: address: %s, battery_set: %s, ', remote_address, battery_set)

    @utils.glib_call(False)
    def has_proxy(self):
        """Checks whether Battery proxy can be acquired."""
        return bool(self.proxy())

    def proxy(self):
        """Gets proxy object to Battery interface for method calls."""
        return self.bus.get(self.BLUETOOTH_DBUS, self.objpath)[self.BATTERY_MANAGER_INTERFACE]

    @utils.glib_call(False)
    def register_battery_callback(self):
        """Registers battery callbacks if it doesn't exist."""

        if self.callbacks:
            return True

        # Generate a random number between 1-1000
        rnumber = math.floor(random.random() * 1000 + 1)

        # Create and publish callbacks
        self.callbacks = self.ExportedBatteryManagerCallbacks()

        self.callbacks.add_observer('battery_manager', self)
        objpath = self.BATTERY_CB_OBJ_PATTERN.format(self.hci, rnumber)
        self.bus.register_object(objpath, self.callbacks, None)

        # Register published callbacks with admin daemon
        self.callback_id = self.proxy().RegisterBatteryCallback(objpath)
        return True

    @utils.glib_call(False)
    def unregister_battery_callback(self):
        """Unregisters battery manager callbacks for this client.

        Returns:
            True on success, False otherwise.
        """
        return self.proxy().UnregisterBatteryCallback(self.callback_id)

    @utils.glib_call(None)
    def get_battery_information(self, remote_address):
        """Gets battery information.

        Args:
            remote_address: The bluetooth address of the target remote device.

        Returns:
            BatterySet tuple as (String, String, String, List[Battery]) on success, None otherwise.
        """
        battery_info_dbus = self.proxy().GetBatteryInformation(remote_address)
        battery_info = FlossBatteryManagerClient.parse_dbus_battery_info(battery_info_dbus)

        if battery_info is None:
            logging.error('Failed to parse battery info for device address %s, '
                          'battery info = %s', remote_address, battery_info_dbus)
        else:
            logging.debug('Got the battery info for device (addr: %s): %s', remote_address, battery_info)
        return battery_info

    def get_battery_property(self, remote_address, prop_name):
        """Gets the battery property.

        Args:
            remote_address: The bluetooth address of the target remote device.
            prop_name: Property to be required.

        Returns:
            Battery property on success, None otherwise.
        """
        battery_info = self.get_battery_information(remote_address)
        batteries = battery_info[3]

        # Currently we don't have any sources that provide more than one Battery.
        # So, it should be safe to use the first Battery.
        if batteries:
            return batteries[0][prop_name.lower()]
        return None
