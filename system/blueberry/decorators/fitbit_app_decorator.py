"""Android Device decorator to control functionality of the Fitbit companion App."""

import logging
from typing import Any, Dict, Tuple

import immutabledict  # pylint: disable=no-name-in-module,import-error
from mobly.controllers import android_device

# Internal import
from blueberry.controllers import derived_bt_device
from blueberry.utils.ui_pages import fitbit_companion  # pylint: disable=no-name-in-module,import-error
from blueberry.utils.ui_pages.fitbit_companion import account_pages  # pylint: disable=no-name-in-module,import-error
from blueberry.utils.ui_pages.fitbit_companion import context  # pylint: disable=no-name-in-module,import-error
from blueberry.utils.ui_pages.fitbit_companion import other_pages  # pylint: disable=no-name-in-module,import-error

_FITBIT_PACKAGE_NAME = 'com.fitbit.FitbitMobile'
_LOG_PREFIX_MESSAGE = 'Fitbit Companion App'
_DEBUG_PREFIX_TEMPLATE = f'[{_LOG_PREFIX_MESSAGE}|{{tag}}] {{msg}}'
_MODEL_TO_PRODUCT_NAME_MAPPING = immutabledict.immutabledict({
    'Buzz': 'Luxe',
    'Luxe': 'Luxe',
    'Morgan': 'Charge 5',
    'Charge_5': 'Charge 5',
})
_INVALID_PAIRING_CODE_MESSAGE = "Sorry, this code isn't valid."
_MAX_PAIRING_RETRIES = 10


class FitbitAppDecorator(android_device.AndroidDevice):
  """Decorates Android Device with the Fitbit Companion App's operations.

  Attributes:
    ui_context: The UI context of Fitbit companion App.
  """

  def __init__(self, ad: android_device.AndroidDevice):  # pylint: disable=super-init-not-called
    self._ad = ad
    self._target_device = None
    self.ui_context = fitbit_companion.get_context(
        self._ad, do_go_home=False, safe_get=True)

    self.ui_context.regr_page_call(other_pages.LocationPermissionSync, 'enable')
    self.ui_context.regr_page_call(other_pages.PixelBudConnectPopup, 'cancel')
    self.ui_context.regr_page_call(other_pages.DownloadAppPopup, 'done')
    self.ui_context.regr_page_call(other_pages.FitbitSmartLockPage, 'save')
    self.ui_context.regr_page_call(other_pages.GooglePasswordSavePage, 'save')
    self.ui_context.regr_page_call(other_pages.GooglePlayAccountCompletePage,
                                   'go')
    self.ui_context.regr_page_call(other_pages.GoogleSmartLockSavePage, 'no')
    self.ui_context.regr_page_call(other_pages.GooglePlayTermOfServicePage,
                                   'accept')
    self.ui_context.regr_page_call(other_pages.NotificationPopup, 'not_allow')
    if not apk_utils.is_apk_installed(self._ad, _FITBIT_PACKAGE_NAME):
      # Fitbit App is not installed, install it now.
      self.ui_context.log.info('Installing Fitbit App...')
      fitbit_companion.go_google_play_page(self.ui_context)
      self.ui_context.expect_page(other_pages.GooglePlayPage)
      self.ui_context.page.install()

    self.ui_context.go_home_page()
    fitbit_app_account = self._ad._user_params.get('fitbit_app_account', 'test')

    if self.ui_context.is_page(other_pages.LoginInputPage):
      self.ui_context.page.input(
          fitbit_app_account,
          self._ad._user_params.get('fitbit_app_password', 'test'))
    elif self.ui_context.is_page(other_pages.LoginPage2):
      self.ui_context.page.login(
          fitbit_app_account,
          self._ad._user_params.get('fitbit_app_password', 'test'))
    elif self.ui_context.is_page(other_pages.LoginPage):
      self.ui_context.log.info('Login Fitbit App with account=%s...',
                               fitbit_app_account)
      self.ui_context.page.login(
          fitbit_app_account,
          self._ad._user_params.get('fitbit_app_password', 'test'))

    self.ui_context.expect_page(context.HomePage)

  def __getattr__(self, name: str):
    return getattr(self._ad, name)

  def set_target(self, bt_device: derived_bt_device.BtDevice) -> None:
    """Allows for use to get target device object for target interaction.

    Args:
      bt_device: The testing target.
    """
    self._target_device = bt_device

  def factory_reset_bluetooth(self):
    logging.info('Removing all paired device(s) after testing...')
    removed_count = fitbit_companion.remove_all_paired_devices(self.ui_context)
    logging.info('Total %d device(s) being removed!', removed_count)
    logging.info('Delegate the BT reset down to %s...', self._ad)
    self._ad.factory_reset_bluetooth()

  def pair_and_connect_bluetooth(self, mac_address: str) -> None:
    """Pairs and connects Android device with Fitbit device.

    Args:
      mac_address: MAC address of the Fitbit device to be paired with.

    Raises:
      signals.TestError: Fail in pairing and connection process.
      AssertionError: Fail in evaluation after pairing.
    """
    log = FitbitCompanionAppLoggerAdapter(logging.getLogger(),
                                          {'tag': mac_address})
    fitbit_device = self._target_device
    target_device_mac_address = fitbit_device.get_bluetooth_mac_address()
    if target_device_mac_address != mac_address:
      raise ValueError(
          (f'Target BT device has MAC address={target_device_mac_address}',
           f'which is different than given MAC address={mac_address} !'))

    try:
      log.info('Entering account page...')
      self.ui_context.go_page(account_pages.AccountPage)

      log.info('Registering default page action...')
      self.ui_context.regr_page_call(other_pages.FitbitManagePopup, 'allow')

      log.info('Firmware version: %s', fitbit_device.firmware_version)
      log.info('Removing all paired device(s) before testing...')
      removed_count = fitbit_companion.remove_all_paired_devices(
          self.ui_context)
      log.info('Total %d device(s) being removed!', removed_count)
      # Removed registered page action
      self.ui_context.regr_page_calls.pop(other_pages.FitbitManagePopup, None)

      log.info('Pairing %s ...', fitbit_device)
      fitbit_companion.pair_device(self.ui_context, fitbit_device)
      log.info('Pairing and connection with %s(%s) is all done!', fitbit_device,
               mac_address)
    finally:
      removed_count = fitbit_companion.remove_all_paired_devices(
          self.ui_context)
      logging.info('Total %d device(s) being removed after testing!',
                   removed_count)


class FitbitCompanionAppLoggerAdapter(logging.LoggerAdapter):
  """A wrapper class that adds a prefix to each log line.

  Usage:
  .. code-block:: python
    my_log = FitbitCompanionAppLoggerAdapter(logging.getLogger(), {
      'tag': <custom tag>
    })

  Then each log line added by my_log will have a prefix
  '[Fitbit Companion App|<tag>]'
  """

  def process(self, msg: str, kwargs: Dict[Any,
                                           Any]) -> Tuple[str, Dict[Any, Any]]:
    new_msg = _DEBUG_PREFIX_TEMPLATE.format(tag=self.extra['tag'], msg=msg)
    return (new_msg, kwargs)
