"""A Bluetooth Client Decorator util for an Android Device.

This utility allows the user to decorate an device with a custom decorator from
the blueberry/decorators directory.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import importlib
import re
from mobly.controllers import android_device


def decorate(ad: android_device.AndroidDevice,
             decorator: str) -> android_device.AndroidDevice:
  """Utility to decorate an AndroidDevice.

  Args:
    ad: Device, must be of type AndroidDevice.
    decorator: String, class name of the decorator to use.
  Returns:
    AndroidDevice object.
  """

  if not isinstance(ad, android_device.AndroidDevice):
    raise TypeError('Must apply AndroidBluetoothClientDecorator to an '
                    'AndroidDevice')
  decorator_module = camel_to_snake(decorator)
  module = importlib.import_module(
      'blueberry.decorators.%s' % decorator_module)
  cls = getattr(module, decorator)

  return cls(ad)


def camel_to_snake(cls_name: str) -> str:
  """Utility to convert a class name from camel case to snake case.

  Args:
    cls_name: string
  Returns:
    string
  """
  s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', cls_name)
  return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()
