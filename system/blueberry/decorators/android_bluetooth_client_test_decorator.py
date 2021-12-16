"""An example Bluetooth Client Decorator.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from typing import Any

from mobly.controllers import android_device


class AndroidBluetoothClientTestDecorator(android_device.AndroidDevice):
  """A class used to test Blueberry's BT Client Profile decoration."""

  def __init__(self, ad: android_device.AndroidDevice) -> None:
    self._ad = ad
    if not isinstance(self._ad, android_device.AndroidDevice):
      raise TypeError('Must apply AndroidBluetoothClientTestDecorator to an '
                      'AndroidDevice')

  def __getattr__(self, name: str) -> Any:
    return getattr(self._ad, name)

  def test_decoration(self) -> str:
    return 'I make this device fancy!'
