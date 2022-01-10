"""Tests for Subsequent pairing feature of Triangle."""

import time

from mobly import test_runner
from mobly import signals
from blueberry.utils import triangle_base_test as base_test
from blueberry.utils import triangle_constants

# Internal import
# Internal import

_DEFAULT_HEADSET_NAME = '.*Pixel Buds A-Series.*'
_SUBSEQUENT_PAIRING_RETRIES = 2
_FOOTPRINTS_SYNC_WAITING_TIME_SEC = 90


class SubsequentPairingTest(base_test.TriangleBaseTest):
  """Subsequent Pairing Test."""

  def setup_class(self):
    """Executes Subsequent pairing setups."""
    super().setup_class()
    self.headset_name = self.user_params.get('subsequent_pairing_headset_name',
                                             _DEFAULT_HEADSET_NAME)

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
      if wear_pairing_util.search_and_click(
          device=self.watch,
          matching_text=self.headset_name,
          button_text=self.headset_name,
          ui_timeout=constants.UI_HALF_MIN_WAIT_TIMEOUT_MS):
        self.watch.log.info('Pair and connect to Headset.')
        return
    raise signals.TestFailure('Headset did not appear in Bluetooth device list '
                              'of Watch.')

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
    # TODO(user): Trigger syncing or detect completion to avoid sleeping.
    time.sleep(_FOOTPRINTS_SYNC_WAITING_TIME_SEC)
    self._execute_subsequent_pairing_logic()
    self.assert_headset_a2dp_connection(connected=True, device=self.watch)
    self.assert_headset_hsp_connection(connected=True, device=self.watch)


if __name__ == '__main__':
  test_runner.main()
