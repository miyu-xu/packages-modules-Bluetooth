"""
Copied from tools/rootcanal/scripts/test_channel.py
"""

import socket
from time import sleep
from netsim.frontend_pb2_grpc import FrontendServiceStub
from netsim.frontend_pb2 import PatchDeviceRequest
from netsim.model_pb2 import Device, Position
from google.protobuf.empty_pb2 import Empty
from typing import List
import sys

class RootCanal:
    """Binds to netsim over gRPC to manipulate Bluetooth devices
    in ways external to HCI"""

    def __init__(self, channel):
        self.frontend = FrontendServiceStub(channel)

    def move_out_of_range(self):
        """Space out the connected devices to generate a supervision
        timeout for all existing connections."""
        for device in self._list_devices():
            print(device, file=sys.stderr)
            self._set_position(device.id, 1000. * device.id, 0., 0.)

    def move_in_range(self):
        """Move the connected devices to the same point to ensure
        the reconnection of previous links."""
        for device in self._list_devices():
            print(device, file=sys.stderr)
            self._set_position(device.id, 0., 0., 0.)

    def _list_devices(self) -> List[Device]:
        """List existing devices in RootCanal"""
        return self.frontend.GetDevices(request=Empty()).devices

    def _set_position(self, id: int, x: float, y: float, z: float):
        """Update the position of a device identified by its id"""
        print("set_position {}", id, file=sys.stderr)
        self.frontend.PatchDevice(request=PatchDeviceRequest(
            device=Device(id=id, position=Position(x=x, y=y, z=z))))

