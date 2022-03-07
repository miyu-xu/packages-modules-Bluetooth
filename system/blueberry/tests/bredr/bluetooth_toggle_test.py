"""Tests for toggling Bluetooth status on the device."""

from mobly import test_runner
from mobly import signals
from mobly.controllers import android_device
from blueberry.utils import asserts
from blueberry.utils import blueberry_base_test


class BluetoothToggleTest(blueberry_base_test.BlueberryBaseTest):
  """Class for Bluetooth Toggle Test."""
  dut: android_device.AndroidDevice

  def setup_class(self):
    """Executes device setup in this phase."""
    super().setup_class()
    self.dut = self.android_devices[0]
    self.dut.init_setup()

  def setup_test(self):
    """Makes sure that Bluetooth is enabled in this phase."""
    super().setup_test()
    self.dut.wait_for_bluetooth_toggle_state(enabled=True)

  def _assert_bluetooth_status(self, enabled: bool) -> None:
    """Asserts that Bluetooth is in the expected status.

    Args:
      enabled: Enabled as expected if True.
    """
    with asserts.assert_not_raises(signals.ControllerError):
      self.dut.wait_for_bluetooth_toggle_state(enabled)

  def test_disable_and_enable_bluetooth_by_svc_command(self):
    """Test for disable and then enable Bluetooth by SVC command."""
    self.dut.adb.shell('svc bluetooth disable')
    self._assert_bluetooth_status(enabled=False)
    self.dut.adb.shell('svc bluetooth enable')
    self._assert_bluetooth_status(enabled=True)


if __name__ == '__main__':
  test_runner.main()
