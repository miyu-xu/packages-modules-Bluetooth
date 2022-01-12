"""Tests for Connection switching feature of Triangle."""

import logging
import time

from mobly import test_runner
from mobly import signals
from blueberry.utils import bt_audio_utils
from blueberry.utils import bt_test_utils
from blueberry.utils import triangle_base_test as base_test
from blueberry.utils import triangle_constants


class ConnectionSwitchingTest(base_test.TriangleBaseTest):
  """Connection Switching Test."""

  def setup_class(self):
    """Executes Connection switching setups.

    Pairs Phone to headset and Watch, then pairs and connect Watch to Headset,
    let Watch be last connected device of Headset.
    """
    super().setup_class()
    self.headset.factory_reset_bluetooth()
    self.pair_and_connect_phone_to_headset()
    self.pair_and_connect_phone_to_watch()
    self.pair_and_connect_watch_to_headset()
    sine_wave_file = bt_audio_utils.generate_sine_wave_to_device(self.watch)[0]
    self.audio_file_on_watch = f'file://{sine_wave_file}'

  def setup_test(self):
    """Makes sure that Headset is connected to Watch instead of Phone."""
    super().setup_test()
    self.phone.disconnect_bluetooth(self.headset.mac_address)
    # Play media to avoid Headset connection is switched.
    self.watch.sl4a.mediaPlayOpen(self.audio_file_on_watch)
    self.watch.sl4a.mediaPlayStart()
    self.watch.connect_bluetooth(self.headset.mac_address)
    self.assert_headset_a2dp_connection(connected=True, device=self.watch)
    self.assert_headset_hsp_connection(connected=True, device=self.watch)
    self.assert_headset_a2dp_connection(connected=False, device=self.phone)
    self.assert_headset_hsp_connection(connected=False, device=self.phone)
    self.watch.sl4a.mediaPlayStop()
    self.watch.sl4a.mediaPlayClose()

  def test_trigger_connection_switching_when_headset_powered_on(self):
    """Test for triggering connection switching when Headset is powered on.

    Steps:
      1. Power off Headset.
      2. Wait 1 minute.
      3. Power on Headset, and then it will be reconnect.

    Verifications:
      The Headset connection is switched from Watch to Phone.
    """
    logging.info('Power off Headset and wait 1 minute.')
    self.headset.power_off()
    self.watch.wait_for_disconnection_success(self.headset.mac_address)
    time.sleep(triangle_constants.WAITING_TIME_SEC)
    logging.info('Power on Headset.')
    self.headset.power_on()
    self.watch.wait_for_connection_success(self.headset.mac_address)
    self.assert_headset_a2dp_connection(connected=True, device=self.phone)
    self.assert_headset_hsp_connection(connected=True, device=self.phone)

  def test_trigger_connection_switching_when_phone_tethered_watch(self):
    """Test for triggering connection switching when Phone is tethered to Watch.

    Steps:
      1. Disable Bluetooth on Phone.
      2. Wait 1 minute.
      3. Enable Bluetooth on Phone, and then Phone will be tethered to Watch.

    Verifications:
      The Headset connection is switched from Watch to Phone.
    """
    self.phone.log.info('Disable Bluetooth and wait 1 minute.')
    self.phone.mbs.btDisable()
    self.wait_for_watch_connection(connected=False)
    time.sleep(triangle_constants.WAITING_TIME_SEC)
    self.phone.log.info('Enable Bluetooth.')
    self.phone.mbs.btEnable()
    self.wait_for_watch_connection(connected=True)
    self.assert_headset_a2dp_connection(connected=True, device=self.phone)
    self.assert_headset_hsp_connection(connected=True, device=self.phone)

  def test_trigger_connection_switching_when_media_paused_on_watch(self):
    """Test for triggering connection switching when Media is paused on Watch.

    Steps:
      1. Power off Headset.
      2. Wait 1 minute.
      3. Play and then pause media on Watch.
      4. Power on Headset, and then it will be reconnect.

    Verifications:
      The Headset connection is switched from Watch to Phone.
    """
    logging.info('Power off Headset and wait 1 minute.')
    self.headset.power_off()
    self.watch.wait_for_disconnection_success(self.headset.mac_address)
    time.sleep(triangle_constants.WAITING_TIME_SEC)
    self.watch.log.info('Play and then pause media.')
    self.watch.sl4a.mediaPlayOpen(self.audio_file_on_watch)
    self.watch.sl4a.mediaPlayStart()
    self.watch.sl4a.mediaPlayPause()
    logging.info('Power on Headset.')
    self.headset.power_on()
    self.watch.wait_for_connection_success(self.headset.mac_address)
    self.assert_headset_a2dp_connection(connected=True, device=self.phone)
    self.assert_headset_hsp_connection(connected=True, device=self.phone)

  def test_trigger_connection_switching_when_media_playing_on_phone(self):
    """Test for triggering connection switching when Media is playing on Phone.

    Steps:
      1. Power off Headset.
      2. Wait 1 minute.
      3. Play media on Phone.
      4. Power on Headset, and then it will be reconnect.

    Verifications:
      A2DP is playing when Headset connection is switched from Watch to Phone.
    """
    sine_wave_file = bt_audio_utils.generate_sine_wave_to_device(
        device=self.phone,
        duration_sec=90)[0]
    audio_file_on_phone = f'file://{sine_wave_file}'
    logging.info('Power off Headset and wait 1 minute.')
    self.headset.power_off()
    self.watch.wait_for_disconnection_success(self.headset.mac_address)
    time.sleep(triangle_constants.WAITING_TIME_SEC)
    self.phone.log.info('Play media.')
    self.phone.sl4a.mediaPlayOpen(audio_file_on_phone)
    self.phone.sl4a.mediaPlayStart()
    logging.info('Power on Headset.')
    self.headset.power_on()
    self.watch.wait_for_connection_success(self.headset.mac_address)
    self.assert_headset_a2dp_connection(connected=True, device=self.phone)
    self.assert_headset_hsp_connection(connected=True, device=self.phone)
    bt_test_utils.wait_until(
        timeout_sec=10,
        condition_func=self.phone.mbs.btIsA2dpPlaying,
        func_args=[self.headset.mac_address],
        expected_value=True,
        exception=signals.TestFailure(
            'A2DP is not playing when Phone is connected to Headset.'))
    self.phone.sl4a.mediaPlayStop()
    self.phone.sl4a.mediaPlayClose()


if __name__ == '__main__':
  test_runner.main()
