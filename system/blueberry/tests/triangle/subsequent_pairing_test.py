"""Tests for Subsequent pairing feature of Triangle."""

import datetime
import time

from mobly import test_runner
from mobly import signals

from mobly.controllers.android_device_lib.services import sl4a_service
from blueberry.utils import bt_test_utils
from blueberry.utils import triangle_base_test as base_test
from blueberry.utils import triangle_constants

# Internal import
# Internal import

_DEFAULT_HEADSET_NAME = '.*Pixel Buds A-Series.*'
_SL4A_ALIAS = 'sl4a'
_SUBSEQUENT_PAIRING_RETRIES = 5
_FOOTPRINTS_SYNC_TIMEOUT_SEC = datetime.timedelta(minutes=5).seconds


class SubsequentPairingTest(base_test.TriangleBaseTest):
  """Subsequent Pairing Test."""

  def setup_class(self):
    """Executes Subsequent pairing setups."""
    super().setup_class()
    self.headset_name = self.user_params.get('subsequent_pairing_headset_name',
                                             _DEFAULT_HEADSET_NAME)

    # Sets False since the device setups have been completed in this stage.
    self._need_reset = False

  def setup_test(self):
    """Executes factory reset Watch for the next run if needed."""
    super().setup_test()
    if self._need_reset:
      self.cleanup_app_and_reset_watch()

    # Sets True for next test run if the test iterations > 1.
    self._need_reset = self.test_iterations > 1

  def _execute_subsequent_pairing_logic(self):
    """Executes Subsequent pairing between Watch and Headset.

    `Subsequent Pairing` is that the headset which is paired and connected with
    Phone will be shown in Bluetooth device list of Watch after Footprints sync
    successful, then user can directly select the headset to pair with Watch
    and no need to enter pairing mode on the headset.

    Executes the following steps with 2 retries:
      1. Open Bluetooth Settings from Watch.
      2. Wait for Headset appearing in Bluetooth device list of Watch.
      3. Select Headset to pair.

    Raises:
      signals.TestFailure: Headset does not appear.
    """
    for _ in range(_SUBSEQUENT_PAIRING_RETRIES):
      self.watch.log.info('Check if the headset appears in Bluetooth device '
                          'list and then perform subsequent pairing.')
      self.watch.adb.shell(triangle_constants.START_BLUETOOTH_SETTINGS)
      # Searches subsequent pairing HUN with swiping up.
      self.phone.uia(textMatches=self.headset_name).swipe.up(steps=5)
      if wear_pairing_util.search_and_click(
          device=self.watch,
          matching_text=self.headset_name,
          button_text=self.headset_name,
          ui_timeout=constants.UI_HALF_MIN_WAIT_TIMEOUT_MS):
        self.watch.log.info('Pair and connect to Headset.')
        return
    raise signals.TestError('Headset did not appear in Bluetooth device list '
                            'of Watch.')

  def _wait_for_footprints_sync(self):
    """Waits for Footprints sync.

    Footprints sync means Footprints server sync happens on Watch side. After
    sync, the watch can obtain information of the headset which has paired with
    the connected phone, then perform Subsequent Pairing in further.

    Raises:
      signals.TestError: Footprints sync is not executed.
    """
    logcat_start_time = self.watch.get_device_time()
    self.watch.log.info('Waiting for Footprints sync...')
    start_time = time.time()
    end_time = start_time + _FOOTPRINTS_SYNC_TIMEOUT_SEC
    while time.time() < end_time:
      output = self.watch.logcat_filter(
          start_time=logcat_start_time,
          text_filter='executes the footprints force sync')
      if bool(output):
        sync_time = time.time()
        self.watch.log.info('Executed Footprints sync after approximately'
                            ' %s seconds.', (sync_time - start_time))
        return
      # Buffer between logcat commands.
      time.sleep(datetime.timedelta(seconds=1).seconds)
    raise signals.TestError(f'Failed to execute Footprints sync within '
                            f'{_FOOTPRINTS_SYNC_TIMEOUT_SEC} seconds.')

  def test_subsequent_pairing(self):
    """Test for Subsequent pairing.

    Steps:
      1. Pair Phone to Watch.
      2. Pair Phone to Headset.
      3. Wait for Footprints sync.
      4. Execute Subsequent pairing.

    Verifications:
      Watch can pair and connect to Headset successfully.
    """
    self.pair_and_connect_phone_to_watch()
    self.headset.factory_reset_bluetooth()
    self.pair_and_connect_phone_to_headset()
    # Unregisters sl4a service to avoid raising AdbError before reboot().
    self.watch.services.unregister(_SL4A_ALIAS)
    self.watch.log.info('Force enable Location.')
    output = int(self.watch.adb.shell(triangle_constants.ENABLE_LOCATION))
    if output != 3:
      raise signals.TestError('Location is disabled on Watch.')
    # Reboots Watch to force sync footprints.
    self.watch.reboot()
    self.watch.services.register(_SL4A_ALIAS, sl4a_service.Sl4aService)
    self.wait_for_watch_connection(connected=True)
    self._wait_for_footprints_sync()
    self._execute_subsequent_pairing_logic()
    bt_test_utils.wait_until(
        timeout_sec=datetime.timedelta(seconds=30).seconds,
        condition_func=self.watch.is_bt_paired,
        func_args=[self.headset.mac_address],
        expected_value=True,
        exception=signals.TestFailure(
            'Watch failed to bond with Headset by Subsequent Pairing.'))
    self.assert_headset_a2dp_connection(connected=True, device=self.watch)
    self.assert_headset_hsp_connection(connected=True, device=self.watch)


if __name__ == '__main__':
  test_runner.main()
