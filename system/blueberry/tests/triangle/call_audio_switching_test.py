"""Tests for Call Audio switching feature of Triangle."""

import datetime

from mobly import test_runner
from mobly import signals
from mobly.controllers import android_device as ad
from blueberry.utils import bt_audio_utils
from blueberry.utils import bt_constants
from blueberry.utils import bt_test_utils
from blueberry.utils import triangle_base_test as base_test

_CALL_STATE = bt_constants.CallState


class CallAudioSwitchingTest(base_test.TriangleBaseTest):
  """Call Audio Switching Test."""

  def setup_class(self):
    """Executes Call Audio Switching setups."""
    super().setup_class()
    self._setup_secondary_phone()

    # Pairs Phone to headset and Watch, then pairs and connect Watch to Headset,
    # let Watch be last connected device of Headset.
    self.headset.factory_reset_bluetooth()
    self.pair_and_connect_phone_to_headset()
    self.pair_and_connect_phone_to_watch()
    self.pair_and_connect_watch_to_headset()

    # Checks if SIM is loaded for each phones.
    for phone in (self.phone, self.secondary_phone):
      if not self.phone.is_sim_state_loaded():
        raise signals.TestError(
            f'SIM card on the phone "{phone.serial}" is not loaded.')
      phone.phone_number = phone.dimensions['phone_number']

    # Generates an audio file which is used to be played on Watch.
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
    self.assert_headset_hsp_connection(connected=True, device=self.watch)
    self.assert_headset_hsp_connection(connected=False, device=self.phone)
    self.watch.sl4a.mediaPlayStop()
    self.watch.sl4a.mediaPlayClose()

  def teardown_test(self):
    """Makes sure Phones are not in call."""
    super().teardown_test()
    for phone in (self.phone, self.secondary_phone):
      if phone.sl4a.telecomGetCallState() != bt_constants.CALL_STATE_IDLE:
        phone.log.info('End call.')
        phone.sl4a.telecomEndCall()
        if not self._wait_for_call_state(
            device=phone,
            call_state=_CALL_STATE.IDLE):
          raise signals.TestError(f'{phone.debug_tag} is still in call.')

  def _setup_secondary_phone(self) -> None:
    """Setups the secondary phone before executing tests.

    This phone is used to make/receive calls.
    """
    self.secondary_phone = self.android_devices[-1]
    self.secondary_phone.debug_tag = 'SecondaryPhone'
    self.secondary_phone.init_setup()

  def _assert_call_audio_routed_to_headset(self) -> None:
    """Asserts that Call audio is routed to Headset."""
    bt_test_utils.wait_until(
        timeout_sec=datetime.timedelta(seconds=20).seconds,
        condition_func=self.phone.is_bluetooth_sco_on,
        func_args=[],
        expected_value=True,
        exception=signals.TestFailure('Call audio is not routed to Headset.'))

  def _make_call(
      self,
      caller: ad.AndroidDevice,
      callee: ad.AndroidDevice) -> None:
    """Makes a phone call.

    Args:
      caller: Device to make the call.
      callee: Device to receive the call.
    """
    caller.log.info('Make a call to %s.', callee.debug_tag)
    caller.sl4a.telecomCallNumber(callee.phone_number)

  def _wait_for_call_state(
      self,
      device: ad.AndroidDevice,
      call_state: _CALL_STATE) -> bool:
    """Waits for call state of the device to be changed.

    Args:
      device: Mobly Android controller.
      call_state: The expected call state.

    Returns:
      True if the call state is changed else False.
    """
    return device.wait_for_call_state(
        call_state=call_state,
        timeout_sec=datetime.timedelta(seconds=30).seconds)

  def test_trigger_call_audio_switching_when_making_call(self):
    """Test for triggering call audio switching when making call.

    When making a call on Phone, Headset connection will be switched from Watch
    to Phone and call audio will be routed to Headset.

    Steps:
      1. Make a call on Phone.
      2. Check if Headset connection is switched from Watch to Phone.
      3. Check if Call audio is routed to Headset.
    """
    self._make_call(caller=self.phone, callee=self.secondary_phone)
    self.assert_headset_hsp_connection(connected=True, device=self.phone)
    self._assert_call_audio_routed_to_headset()

  def test_trigger_call_audio_switching_when_answering_call_on_watch(self):
    """Test for triggering call audio switching when answering call on Watch.

    When Phone is in the ringing state, Headset connection will be switched from
    Watch to Phone. When the call is accepted on Watch side, Call audio will be
    routed to Headset instead of Watch's speaker.

    Steps:
      1. Receive a call on Phone.
      2. Wait for ringing on Watch and Phone.
      3. Check if Headset connection is switched from Watch to Phone.
      4. Check if Call audio is routed to Headset.
      5. Answer the call on Watch and then wait for offhook state on Phone.
      6. Check if Call audio is still routed to Headset.
    """
    self._make_call(caller=self.secondary_phone, callee=self.phone)
    for device in (self.phone, self.watch):
      if not self._wait_for_call_state(
          device=device,
          call_state=_CALL_STATE.RINGING):
        raise signals.TestError(f'{device.debug_tag} is not ringing.')
    self.assert_headset_hsp_connection(connected=True, device=self.phone)
    self._assert_call_audio_routed_to_headset()
    self.watch.log.info('Answer the incoming call.')
    self.watch.sl4a.telecomAcceptRingingCall()
    if not self._wait_for_call_state(
        device=self.phone,
        call_state=_CALL_STATE.OFFHOOK):
      raise signals.TestError(f'{device.debug_tag} is not in the active call.')
    self._assert_call_audio_routed_to_headset()


if __name__ == '__main__':
  test_runner.main()
