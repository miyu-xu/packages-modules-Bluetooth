"""Base Tests for blueberry.pbap.bluetooth_pbap."""

import os
import random
import re
import tempfile
import time
from typing import Mapping, Optional

from mobly import signals
from mobly.controllers import android_device

from blueberry.utils import blueberry_ui_base_test
from blueberry.utils import bt_constants
from blueberry.utils import bt_test_utils


# The path is used to place the created vcf files.
STORAGE_PATH = '/storage/emulated/0'

# URI for contacts database.
CONTACTS_URI = 'content://com.android.contacts/data/phones'

# Number of seconds to wait for contacts and call logs update.
WAITING_TIMEOUT_SEC = 60

# Permissions for Contacts app.
PERMISSION_LIST = [
    'android.permission.READ_CONTACTS',
    'android.permission.WRITE_CONTACTS',
]

# Types of call log type
CALL_LOG_TYPES = [
    bt_constants.INCOMING_CALL_LOG_TYPE,
    bt_constants.OUTGOING_CALL_LOG_TYPE,
    bt_constants.MISSED_CALL_LOG_TYPE,
]


class BluetoothPbapBase(blueberry_ui_base_test.BlueberryUiBaseTest):
  """Base Test Class for Bluetooth PBAP Test."""

  def __init__(self, configs: Mapping[str, str]):
    super().__init__(configs)
    self.derived_bt_device = None
    self.pri_phone = None
    self.pse_mac_address = None

  def setup_class(self):
    """Standard Mobly setup class."""
    super().setup_class()

    # Bluetooth carkit which role is Phone Book Client Equipment (PCE).
    self.derived_bt_device = self.derived_bt_devices[0]

    # Primary phone which role is Phone Book Server Equipment (PSE).
    self.pri_phone = self.android_devices[0]
    self.pri_phone.init_setup()
    self.pri_phone.sl4a_setup()
    self.derived_bt_device.add_sec_ad_device(self.pri_phone)

    # Grant the permissions to Contacts app.
    for device in [self.pri_phone, self.derived_bt_device]:
      required_permissions = PERMISSION_LIST.copy()
      # App requires READ_EXTERNAL_STORAGE to read contacts if SDK < 30.
      if int(device.build_info['build_version_sdk']) < 30:
        required_permissions.append('android.permission.READ_EXTERNAL_STORAGE')
      for permission in required_permissions:
        device.adb.shell('pm grant com.google.android.contacts %s' % permission)
    self.pse_mac_address = self.pri_phone.get_bluetooth_mac_address()
    mac_address = self.derived_bt_device.get_bluetooth_mac_address()
    self.derived_bt_device.activate_pairing_mode()
    self.pri_phone.pair_and_connect_bluetooth(mac_address)
    # Sleep until the connection stabilizes.
    time.sleep(5)

    # Allow permission access for PBAP profile.
    self.pri_phone.sl4a.bluetoothChangeProfileAccessPermission(
        mac_address,
        bt_constants.BluetoothProfile.PBAP.value,
        bt_constants.BluetoothAccessLevel.ACCESS_ALLOWED.value)

  def setup_test(self):
    super().setup_test()
    # Make sure PBAP is not connected before running tests.
    self._terminate_pbap_connection()

  def _import_vcf_to_pse(self, file_name: str,
                         expected_contact_count: int) -> None:
    """Imports the vcf file to PSE."""
    # Open ImportVcardActivity and click "OK" in the pop-up dialog, then
    # PickActivity will be launched and browses the existing vcf files.
    self.pri_phone.adb.shell(
        'am start com.google.android.contacts/'
        'com.google.android.apps.contacts.vcard.ImportVCardActivity')
    self.pri_phone.aud(text='OK').click()

    # Check if the vcf file appears in the PickActivity.
    if not self.pri_phone.aud(text=file_name).exists():
      raise android_device.DeviceError(
          self.pri_phone,
          'No file name matches "%s" in PickActivity.' % file_name)

    # TODO(user): Remove the check of code name for S build.
    if (self.pri_phone.build_info['build_version_codename'] != 'S' and
        int(self.pri_phone.build_info['build_version_sdk']) <= 30):
      # Since `adb shell input tap` cannot work in PickActivity before R build,
      # send TAB and ENETER Key events to select and import the vcf file.
      if self.pri_phone.aud(content_desc='Grid view').exists():
        # Switch Grid mode since ENTER Key event cannot work in List mode on
        # git_rvc-d2-release branch.
        self.pri_phone.aud(content_desc='Grid view').click()
      self.pri_phone.aud.send_key_code('KEYCODE_TAB')
      self.pri_phone.aud.send_key_code('KEYCODE_ENTER')
    else:
      self.pri_phone.aud(text=file_name).click()
    self.pri_phone.log.info('Importing "%s"...' % file_name)
    current_count = self._wait_and_get_contact_count(
        self.pri_phone, expected_contact_count, WAITING_TIMEOUT_SEC)
    if current_count != expected_contact_count:
      raise android_device.DeviceError(
          self.pri_phone,
          'Failed to import %d contact(s) within %ds. Actual count: %d' %
          (expected_contact_count, WAITING_TIMEOUT_SEC, current_count))
    self.pri_phone.log.info(
        'Successfully added %d contact(s).' % current_count)

  def _generate_contacts_on_pse(self,
                                num_of_contacts: int,
                                first_name: Optional[str] = None,
                                last_name: Optional[str] = None,
                                phone_number: Optional[int] = None) -> None:
    """Generates contacts to be tested on PSE."""
    vcf_file = bt_test_utils.create_vcf_from_vcard(
        output_path=self.pri_phone.log_path,
        num_of_contacts=num_of_contacts,
        first_name=first_name,
        last_name=last_name,
        phone_number=phone_number)
    self.pri_phone.adb.push([vcf_file, STORAGE_PATH])
    # For R build, since the pushed vcf file probably not found when importing
    # contacts, do a media scan to recognize the file.
    if int(self.pri_phone.build_info['build_version_sdk']) > 29:
      self.pri_phone.adb.shell('content call --uri content://media/ --method '
                               'scan_volume --arg external_primary')
    file_name = vcf_file.split('/')[-1]
    self._import_vcf_to_pse(file_name, num_of_contacts)
    self.pri_phone.adb.shell('rm -rf %s' %
                             os.path.join(STORAGE_PATH, file_name))

  def _generate_call_logs_on_pse(self, call_log_type: str,
                                 num_of_call_logs: int) -> None:
    """Generates call logs to be tested on PSE."""
    self.pri_phone.log.info('Putting %d call log(s) which type are "%s"...' %
                            (num_of_call_logs, call_log_type))
    for _ in range(num_of_call_logs):
      self.pri_phone.sl4a.callLogsPut(dict(
          type=call_log_type,
          number='8809%d' % random.randrange(int(10e8)),
          time=int(1000 * float(self.pri_phone.adb.shell('date +%s.%N')))))
    current_count = self._wait_and_get_call_log_count(
        self.pri_phone,
        call_log_type,
        num_of_call_logs,
        WAITING_TIMEOUT_SEC)
    if current_count != num_of_call_logs:
      raise android_device.DeviceError(
          self.pri_phone,
          'Failed to generate %d call log(s) within %ds. '
          'Actual count: %d, Call log type: %s' %
          (num_of_call_logs, WAITING_TIMEOUT_SEC, current_count, call_log_type))
    self.pri_phone.log.info(
        'Successfully added %d call log(s).' % current_count)

  def _wait_and_get_contact_count(self,
                                  device: android_device.AndroidDevice,
                                  expected_contact_count: int,
                                  timeout_sec: int) -> int:
    """Waits for contact update for a period time and returns contact count.

    This method should be used when a device imports some new contacts. It can
    wait some time for contact update until expectation or timeout and then
    return contact count.

    Args:
      device: AndroidDevice, Mobly Android controller class.
      expected_contact_count: Int, Number of contacts as expected.
      timeout_sec: Int, Number of seconds to wait for contact update.

    Returns:
      current_count: Int, number of the existing contacts on the device.
    """
    start_time = time.time()
    end_time = start_time + timeout_sec
    current_count = 0
    while time.time() < end_time:
      current_count = device.sl4a.contactsGetCount()
      if current_count == expected_contact_count:
        break
      # Interval between attempts to get contacts.
      time.sleep(1)
    if current_count != expected_contact_count:
      device.log.warning(
          'Failed to get expected contact count: %d. '
          'Actual contact count: %d.' %
          (expected_contact_count, current_count))
    return current_count

  def _wait_and_get_call_log_count(self,
                                   device: android_device.AndroidDevice,
                                   call_log_type: str,
                                   expected_call_log_count: int,
                                   timeout_sec: int) -> int:
    """Waits for call log update for a period time and returns call log count.

    This method should be used when a device adds some new call logs. It can
    wait some time for call log update until expectation or timeout and then
    return call log count.

    Args:
      device: AndroidDevice, Mobly Android controller class.
      call_log_type: String, Type of the call logs.
      expected_call_log_count: Int, Number of call logs as expected.
      timeout_sec: Int, Number of seconds to wait for call log update.

    Returns:
      current_count: Int, number of the existing call logs on the device.
    """
    start_time = time.time()
    end_time = start_time + timeout_sec
    current_count = 0
    while time.time() < end_time:
      current_count = len(device.sl4a.callLogsGet(call_log_type))
      if current_count == expected_call_log_count:
        break
      # Interval between attempts to get call logs.
      time.sleep(1)
    if current_count != expected_call_log_count:
      device.log.warning(
          'Failed to get expected call log count: %d. '
          'Actual call log count: %d.' %
          (expected_call_log_count, current_count))
    return current_count

  def _terminate_pbap_connection(self) -> None:
    status = self.derived_bt_device.sl4a.bluetoothPbapClientGetConnectionStatus(
        self.pse_mac_address)
    if status == bt_constants.BluetoothConnectionStatus.STATE_DISCONNECTED:
      return
    self.derived_bt_device.log.info('Disconnecting PBAP...')
    self.derived_bt_device.sl4a.bluetoothPbapClientDisconnect(
        self.pse_mac_address)
    # Buffer for the connection status check.
    time.sleep(3)
    status = self.derived_bt_device.sl4a.bluetoothPbapClientGetConnectionStatus(
        self.pse_mac_address)
    if status != bt_constants.BluetoothConnectionStatus.STATE_DISCONNECTED:
      raise signals.TestError('PBAP connection failed to be terminated.')
    self.derived_bt_device.log.info('Successfully disconnected PBAP.')

  def _download_contacts(self, contact_count: int) -> int:
    """PCE downloads PSE created contacts by PBAP profile.

    This method setups up PSE contacts based on contact_count parameter,
    then connect PSE and PCE by PBPA profile and finally will return corrent
    contact count in PCE.

    Args:
      contact_count: number of PSE contacts count.

    Returns:
      current_count: number of PCE contacts after connecting with PSE.
    """
    # Make sure no any contacts existed on the devices.
    for device in [self.pri_phone, self.derived_bt_device]:
      device.sl4a.contactsEraseAll()

    # Add contacts to PSE.
    if contact_count != 0:
      self._generate_contacts_on_pse(contact_count)

    # PCE connect PSE and then download default contacts.
    self.derived_bt_device.pbap_connect()
    self.derived_bt_device.log.info('Download %d contacts from PSE...' %
                                    contact_count)
    current_count = self._wait_and_get_contact_count(self.derived_bt_device,
                                                     contact_count,
                                                     WAITING_TIMEOUT_SEC)
    return current_count

  def _download_call_logs(self, call_log_count: int,
                          call_log_type: str) -> int:
    """PCE downloads PSE created call logs by PBAP profile.

    This method should be used when PSE creates call logs based on parameter,
    then connects PCE by PBAP profile and finally this method will return
    current call logs count in PCE.

    Args:
      call_log_count: number of PSE call logs count.
      call_log_type: type of call log

    Returns:
      current_count: number of PCE call logs after connecting with PSE.
    """

    # Add call logs to PSE.
    if call_log_count != 0:
      self._generate_call_logs_on_pse(call_log_type, call_log_count)

    # When PCE is connected to PSE, it will download PSE's contacts.
    self.derived_bt_device.pbap_connect()
    self.derived_bt_device.log.info('Downloading %d call logs from PSE...' %
                                    call_log_count)

    current_count = self._wait_and_get_call_log_count(self.derived_bt_device,
                                                      call_log_type,
                                                      call_log_count,
                                                      WAITING_TIMEOUT_SEC)

    return current_count

  def _normalize_phonenumber(self, phone_number: str) -> str:
    """Removes all non-digits from phone_number.

    Args:
      phone_number: the string number from device. Could be call logs or contact

    Returns:
      phone_number: the phone number with digital only
    """

    return re.sub(r'\D', '', phone_number)

  def _compare_call_logs(self, call_log_type: str) -> bool:
    """Compares the call logs between PSE and PCE.

    This method shall be used to compare the call logs between PSE and PCE after
    PBAP connects.

    Args:
      call_log_type: type of call log

    Returns:
      True: the call logs between PSE and PCE are the same.
      False: the call logs between PSE and PCE are different.
    """
    # Get PSE and PCE call logs
    pse_call_logs = self.pri_phone.sl4a.callLogsGet(call_log_type)
    pce_call_logs = self.derived_bt_device.sl4a.callLogsGet(call_log_type)

    # Normalize phone number
    for i in range(len(pse_call_logs)):
      pse_call_logs[i]['number'] = self._normalize_phonenumber(
          pse_call_logs[i]['number'])
      pce_call_logs[i]['number'] = self._normalize_phonenumber(
          pce_call_logs[i]['number'])

    # Normalize date
    for i in range(len(pse_call_logs)):
      pse_call_logs[i]['date'] = str(int(pse_call_logs[i]['date'])//1000)
      pce_call_logs[i]['date'] = str(int(pce_call_logs[i]['date'])//1000)

    # Compare diff between PSE and PCE
    diff_in_pse = [i for i in pse_call_logs if i not in pce_call_logs]
    diff_in_pce = [j for j in pce_call_logs if j not in pse_call_logs]

    # Log the difference
    self.pri_phone.log.debug('Call log exits in PSE not in PCE: %s',
                             diff_in_pse)
    self.derived_bt_device.log.debug('Call log exits in PCE not in PSE: %s',
                                     diff_in_pce)

    return True if not diff_in_pse and not diff_in_pce else False

  def _export_device_contacts_to_vcf(self, device: android_device.AndroidDevice,
                                     vcf_file: str) -> str:
    """Exports device contacts to VCF file.

    This method shall be used to export device contacts to a VCF file.
    The exported file will be copied to the MH running server.

    Args:
      device: Mobly Android controller class. It should be PSE or PCE
      vcf_file: VCF file base name

    Returns:
      destination path: exported VCF path in MH server.
    """

    source_path = os.path.join(STORAGE_PATH, vcf_file)
    dest_path = os.path.join(tempfile.gettempdir(), vcf_file)
    # Export to VCF and copy file to dest_path
    device.sl4a.exportVcf(source_path)
    device.adb.pull([source_path, dest_path])
    # Remove file after copying
    device.adb.shell('rm -rf {}'.format(source_path))

    return dest_path

  def _compare_contacts(self) -> bool:
    """Compares PSE and PCE contacts.

    This method shall be used for PSE and PCE contacts comparison.

    Conditions:
    1. Number of contacts should be the same.
    2. VCF should be encoded with UTF-8.
    3. The content of contacts should be the same.

    Returns:
      True: All contacts between PSE and PCE are the same.
      False: Some contacts between PSE and PCE are different.
    """

    # Get the exported VCF file
    pse_path = self._export_device_contacts_to_vcf(self.pri_phone, 'pse.vcf')
    pce_path = self._export_device_contacts_to_vcf(self.derived_bt_device,
                                                   'pce.vcf')

    # Compare length and exported VCF file
    with open(
        pse_path, encoding='utf-8', errors='strict') as f_pse, open(
            pce_path, encoding='utf-8', errors='strict') as f_pce:
      if len(f_pse.readlines()) != len(f_pce.readlines()):
        self.pri_phone.log.error('PSE and PCE count are difference')
        return False
      else:
        difference_not_found = True
        for i, j in zip(f_pce, f_pse):
          if i != j:
            self.pri_phone.log.error('%s in pse is different is %s in pce', i,
                                     j)
            difference_not_found = False

    return difference_not_found
