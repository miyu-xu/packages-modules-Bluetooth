"""Tests for blueberry.pbap.bluetooth_pbap."""

from mobly import asserts
from mobly import test_runner
from mobly import signals
from mobly import utils

from blueberry.tests.pbap import bluetooth_pbap_base
from blueberry.utils import bt_constants


class BluetoothPbapTest(bluetooth_pbap_base.BluetoothPbapBase):
  """Test Class for Bluetooth PBAP Test."""

  def test_download_empty_contacts(self):
    """Tests that PCE can download contacts from PSE."""
    default_contact_count = 0
    current_count = self._download_contacts(default_contact_count)
    asserts.assert_true(
        current_count == default_contact_count,
        'PCE failed to download %d contact(s) within %ds, '
        'actually downloaded %d contact(s).' %
        (default_contact_count, bluetooth_pbap_base.WAITING_TIMEOUT_SEC,
         current_count))

  def test_download_contacts(self):
    """Test for the feature of downloading contacts.

    Tests that PCE can download contacts from PSE.
    """
    test_data_count = 200
    current_count = self._download_contacts(test_data_count)

    asserts.assert_true(
        current_count == test_data_count,
        'PCE failed to download %d contact(s) within %ds, '
        'actually downloaded %d contact(s).' %
        (test_data_count, bluetooth_pbap_base.WAITING_TIMEOUT_SEC,
         current_count))

    # Compare contacts
    result = self._compare_contacts()
    asserts.assert_true(result,
                        'The contacts between PSE and PCE are different')

  def test_download_empty_call_logs(self):
    """Tests for the feature of downloading empty call logs.

    Tests that PCE can download empty incoming/outgoing/missed call logs from
    PSE.
    """
    default_call_log_count = 0

    # Make sure no any call logs exist on the devices.
    for device in [self.pri_phone, self.derived_bt_device]:
      device.sl4a.callLogsEraseAll()

    for call_log_type in bluetooth_pbap_base.CALL_LOG_TYPES:
      current_count = self._download_call_logs(default_call_log_count,
                                               call_log_type)
      # Compare call log count
      asserts.assert_true(
          current_count == default_call_log_count,
          'PCE failed to download %d call log(s) which type are "%s" within %ds'
          ', actually downloaded %d call log(s).' %
          (default_call_log_count, call_log_type,
           bluetooth_pbap_base.WAITING_TIMEOUT_SEC,
           current_count))

      # Compare call log content
      compare_result = self._compare_call_logs(call_log_type)
      asserts.assert_true(
          compare_result,
          f'PCE download call log: {call_log_type} are different then PSE')

  def test_download_call_logs(self):
    """Test for the feature of downloading call logs.

    Tests that PCE can download incoming/outgoing/missed call logs from PSE.
    """
    test_data_count = 200
    # Make sure no any call logs exist on the devices.
    for device in [self.pri_phone, self.derived_bt_device]:
      device.sl4a.callLogsEraseAll()

    for call_log_type in bluetooth_pbap_base.CALL_LOG_TYPES:
      # Add call logs to PSE.
      self._generate_call_logs_on_pse(call_log_type, test_data_count)

    # When PCE is connected to PSE, it will download PSE's contacts.
    self.derived_bt_device.pbap_connect()
    self.derived_bt_device.log.info('Downloading call logs...')

    for call_log_type in bluetooth_pbap_base.CALL_LOG_TYPES:
      current_count = self._wait_and_get_call_log_count(
          self.derived_bt_device,
          call_log_type,
          test_data_count,
          bluetooth_pbap_base.WAITING_TIMEOUT_SEC)
      self.derived_bt_device.log.info(
          'Successfully downloaded %d call log(s) which type are "%s".' %
          (current_count, call_log_type))

      # Compare call log count
      asserts.assert_true(
          current_count == test_data_count,
          'PCE failed to download %d call log(s) which type are "%s" within %ds'
          ', actually downloaded %d call log(s).' %
          (test_data_count, call_log_type,
           bluetooth_pbap_base.WAITING_TIMEOUT_SEC,
           current_count))

      # Compare call log content
      compare_result = self._compare_call_logs(call_log_type)
      asserts.assert_true(
          compare_result,
          f'PCE download call log: {call_log_type} are different then PSE')

  def test_show_caller_name(self):
    """Test for caller name of the incoming phone call is correct on PCE.

    Tests that caller name matches contact name which is downloaded via PBAP.
    """
    # Checks if two android devices exist.
    if len(self.android_devices) < 2:
      raise signals.TestError('This test requires two Android devices.')
    primary_phone = self.pri_phone
    secondary_phone = self.android_devices[1]
    secondary_phone.init_setup()
    for phone in [primary_phone, secondary_phone]:
      # Checks if SIM state is loaded for every devices.
      if not phone.is_sim_state_loaded():
        raise signals.TestError(f'Please insert a SIM Card to the phone '
                                f'"{phone.serial}".')
      # Checks if phone_number is provided in the support dimensions.
      phone.phone_number = phone.dimensions.get('phone_number')
      if not phone.phone_number:
        raise signals.TestError(f'Please add "phone_number" to support '
                                f'dimensions of the phone "{phone.serial}".')
    # Make sure no any contacts exist on the devices.
    for device in [primary_phone, self.derived_bt_device]:
      device.sl4a.contactsEraseAll()
    # Generate a contact name randomly.
    first_name = utils.rand_ascii_str(4)
    last_name = utils.rand_ascii_str(4)
    full_name = f'{first_name} {last_name}'
    primary_phone.log.info('Creating a contact "%s"...', full_name)
    self._generate_contacts_on_pse(
        num_of_contacts=1,
        first_name=first_name,
        last_name=last_name,
        phone_number=secondary_phone.phone_number)
    self.derived_bt_device.log.info('Connecting to PSE...')
    self.derived_bt_device.pbap_connect()
    self.derived_bt_device.log.info('Downloading contacts from PSE...')
    current_count = self._wait_and_get_contact_count(
        device=self.derived_bt_device,
        expected_contact_count=1,
        timeout_sec=bluetooth_pbap_base.WAITING_TIMEOUT_SEC)
    self.derived_bt_device.log.info('Successfully downloaded %d contact(s).',
                                    current_count)
    asserts.assert_equal(
        first=current_count,
        second=1,
        msg=f'Failed to download the contact "{full_name}".')
    secondary_phone.sl4a.telecomCallNumber(primary_phone.phone_number)
    secondary_phone.log.info('Made a phone call to device "%s".',
                             primary_phone.serial)
    primary_phone.log.info('Waiting for the incoming call from device "%s"...',
                           secondary_phone.serial)
    is_ringing = primary_phone.wait_for_call_state(
        bt_constants.CALL_STATE_RINGING,
        bt_constants.CALL_STATE_TIMEOUT_SEC)
    if not is_ringing:
      raise signals.TestError(
          f'Timed out after {bt_constants.CALL_STATE_TIMEOUT_SEC}s waiting for '
          f'the incoming call from device "{secondary_phone.serial}".')
    try:
      self.derived_bt_device.aud.open_notification()
      hfp_address = primary_phone.get_bluetooth_mac_address()
      if not self.derived_bt_device.aud(
          text=f'Incoming call via HFP {hfp_address}').exists():
        raise signals.TestError('The incoming call was not received from '
                                'the Handsfree device side.')
      # Asserts that caller name of the incoming phone call is correct in the
      # notification bar.
      asserts.assert_true(
          self.derived_bt_device.aud(text=full_name).exists(),
          f'Caller name is incorrect. Expectation: "{full_name}"')
    finally:
      # Takes a screenshot for debugging.
      self.derived_bt_device.take_screenshot(self.derived_bt_device.log_path)
      # Recovery actions.
      self.derived_bt_device.aud.close_notification()
      secondary_phone.sl4a.telecomEndCall()


if __name__ == '__main__':
  test_runner.main()
