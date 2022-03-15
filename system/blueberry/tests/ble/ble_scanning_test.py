"""Module of Blueberry BLE scanning test cases."""

import logging

from mobly import asserts as mobly_asserts
from mobly import test_runner
from mobly import signals
from mobly.controllers.android_device_lib import errors
from blueberry.utils import asserts
from blueberry.utils import blueberry_base_test


class BleScanningTest(blueberry_base_test.BlueberryBaseTest):
  """Test Class for BLE Scanning Test.

  Test will reset the bluetooth settings of the phone and attempt to examine
  the advertisement from scanning of phone to look for the `derived_bt_device`
  specified in the configuration file.
  """

  def setup_class(self):
    """Conducts testing setup in class level."""
    super().setup_class()

    self.primary_device = self.android_devices[0]
    self.primary_device.init_setup()
    self.primary_device.sl4a_setup()

    self.derived_bt_device = self.derived_bt_devices[0]
    self.derived_bt_device.factory_reset_bluetooth()
    self.bt_device_name = self.derived_bt_device.get_device_name()
    self.bt_device_mac_address = (
        self.derived_bt_device.get_bluetooth_mac_address())

  def setup_test(self):
    """Conducts testing setup before running each test cases."""
    super().setup_test()
    self.primary_device.factory_reset_bluetooth()

  def _check_derived_bt_advertising(self):
    """Checks if the derived BT device is advertising or not.

    Raises:
      errors.Error: The derived BT device is not making advertisement.
    """
    self.derived_bt_device._device.cli.go_tsh()
    self.derived_bt_device._device.cli.exec_cmd('bt le adv')
    cmd_outputs = self.derived_bt_device._device.cli.exec_cmd('status')
    logging.info('Advertisement status=%s', cmd_outputs)
    if not cmd_outputs or cmd_outputs[0].results['advertising'] != '1':
      raise errors.Error('BT device is not sending advertisement!')

  def assert_scanning(self,
                      bt_device_name: str,
                      expected_mac_address: str,
                      timeout_sec: int = 90) -> None:
    """Asserts that the primary device's scanning can find given BT device name.

    Args:
      bt_device_name: The name of BT device.
      expected_mac_address: The expected MAC address of given BT device name.
      timeout_sec: Number of seconds to wait for the advertisement with desired
        device name.
    """
    with asserts.assert_not_raises(signals.ControllerError):
      ble_mac_address = self.primary_device.scan_and_get_ble_device_address(
          bt_device_name, timeout_sec=timeout_sec)

      mobly_asserts.assert_equal(ble_mac_address, expected_mac_address)

  def test_scanning_after_power_reset_bt_target(self):
    """Tests BLE scanning after rebooting target BT device."""
    logging.info('Rebooting target bt device...')
    self.derived_bt_device.reboot()
    self._check_derived_bt_advertising()
    self.assert_scanning(self.bt_device_name, self.bt_device_mac_address)

  def test_scanning_after_power_reset_phone(self):
    """Tests BLE scanning after rebooting primary device."""
    logging.info('Rebooting primary device...')
    self.primary_device.reboot()
    self.primary_device.wait_for_bluetooth_toggle_state(True)
    self._check_derived_bt_advertising()
    self.assert_scanning(self.bt_device_name, self.bt_device_mac_address)

  def test_scanning_after_reset_airplane_mode_of_phone(self):
    """Tests BLE scanning after flipping on/off airplane mode."""
    logging.info('Flipping on/off airplane mode...')
    self.primary_device.enable_airplane_mode()
    self.primary_device.disable_airplane_mode()
    self.primary_device.wait_for_bluetooth_toggle_state(True)
    self._check_derived_bt_advertising()
    self.assert_scanning(self.bt_device_name, self.bt_device_mac_address)


if __name__ == '__main__':
  test_runner.main()
