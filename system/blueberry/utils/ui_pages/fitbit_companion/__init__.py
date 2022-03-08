"""Gets context of Fitbit Companion App."""

import time
from typing import Any, Callable

import immutabledict
from mobly import asserts
from mobly import signals
from mobly.controllers import android_device

# Internal import
# Internal import
from blueberry.controllers import fitbit_tracker_device
from blueberry.utils.ui_pages import errors
from blueberry.utils.ui_pages import ui_core
from blueberry.utils.ui_pages import ui_node
from blueberry.utils.ui_pages.fitbit_companion import account_pages
from blueberry.utils.ui_pages.fitbit_companion import constants
from blueberry.utils.ui_pages.fitbit_companion import context
from blueberry.utils.ui_pages.fitbit_companion import other_pages
from blueberry.utils.ui_pages.fitbit_companion import pairing_pages

MODEL_TO_PRODUCT_NAME_MAPPING = immutabledict.immutabledict({
    'Buzz': 'Luxe',
    'Luxe': 'Luxe',
    'Morgan': 'Charge 5',
    'Charge_5': 'Charge 5',
})

_INVALID_PAIRING_CODE_MESSAGE = "Sorry, this code isn't valid."


def get_context(ad: android_device.AndroidDevice,
                safe_get: bool = False,
                do_go_home: bool = True) -> context.Context:
  """Gets context of Fitbit Companion App.

  Args:
    ad: The Android device where the UI pages are derived from.
    safe_get: If True, use `safe_get_page` to get the page; otherwise, use
      `get_page`.
    do_go_home: If False the context object will stay in the App's current page.

  Returns:
    Context of Fitbit Companion App.
  """
  ctx = context.Context(ad, safe_get=safe_get, do_go_home=do_go_home)
  ctx.known_pages.extend((
      other_pages.PixelBudConnectPopup,
      other_pages.DownloadAppPopup,
      other_pages.LoginInputPage,
      other_pages.LoginPage2,
      other_pages.LoginPage,
      other_pages.FitbitLocationPermissionPopup,
      other_pages.FitbitSmartLockPage,
      other_pages.GooglePasswordSavePage,
      other_pages.GooglePlayPage,
      other_pages.GooglePlayNotAvailablePage,
      other_pages.GooglePlayTermOfServicePage,
      other_pages.GooglePlayAccountCompletePage,
      other_pages.GoogleSmartLockPage,
      other_pages.AllowLocationPermissionConfirmPopup,
      other_pages.AllowLocationPermissionPopup,
      other_pages.LocationPermissionSync,
      other_pages.PurchaseFail,
      other_pages.AllowNotification,
      other_pages.SettingLocation,
      other_pages.LocationDisabledPage,
      other_pages.LinkConfirmPage,
      other_pages.NetworkOpFailPage,
      other_pages.PlayfulPage,
      other_pages.NotificationPopup,
      other_pages.FitbitManagePopup,
      other_pages.WebPageNotReadyPage,
      account_pages.AccountPage,
      account_pages.PairedDeviceDetailPage,
      account_pages.UnpairConfirmPage,
      pairing_pages.BTPermissionRequestPopup,
      pairing_pages.PurchasePage,
      pairing_pages.PairRetryPage,
      pairing_pages.Pairing4DigitPage,
      pairing_pages.PairingConfirmPage,
      pairing_pages.PairingIntroPage,
      pairing_pages.PairAndLinkPage,
      pairing_pages.PremiumPage,
      pairing_pages.PairPrivacyConfirmPage,
      pairing_pages.CancelPairPage,
      pairing_pages.CancelPair2Page,
      pairing_pages.ConfirmReplaceSmartWatchPage,
      pairing_pages.ConfirmChargePage,
      pairing_pages.ChooseTrackerPage,
      pairing_pages.ConfirmDevicePage,
      pairing_pages.SearchDevicePage,
      pairing_pages.SkipInfoPage,
      pairing_pages.UpdateDevicePage,
      pairing_pages.VoicePrivacyPage,
      pairing_pages.AmazonAlexaPage,
      pairing_pages.SetupOnWristCallPage,
      pairing_pages.MonitorOxygenSetupPage,
      pairing_pages.AllsetPage,
      pairing_pages.OnWristCallLoadingPage,
      other_pages.ConfirmLocationPermissionPopup,  # b/213124134
  ))

  return ctx


def go_google_play_page(ctx: context.Context) -> None:
  """Goes to Google play page of Fitbit companion app.

  This function will leverage adb shell command to launch Fitbit app's
  Google play page by searching the package of it. Then it will confirm
  the result by checking the expected page as `GooglePlayPage` by `ctx`.

  Args:
    ctx: Context object of Fitbit Companion App.

  Raises:
    errors.ContextError: Fail to reach target page.
  """
  ctx.ad.adb.shell(
      'am start -a android.intent.action.VIEW -d market://details?id=com.fitbit.FitbitMobile'
  )
  ctx.expect_page(other_pages.GooglePlayPage)


@retry.logged_retry_on_exception(
    retry_value=(errors.ContextError),
    retry_intervals=retry.FuzzedExponentialIntervals(
        initial_delay_sec=1, num_retries=5, factor=1.1))
def _click_unpair_button_on_device(ctx: context.Context,
                                   device_node: ui_node.UINode) -> None:
  """Unpairs given device.

  Args:
    ctx: Context object of Fitbit Companion App.
    device_node: Node of Fitbit device to be unpaired.
  """
  ctx.page.click(device_node)
  ctx.expect_pages([
      pairing_pages.PairAndLinkPage, account_pages.PairedDeviceDetailPage,
      other_pages.ConfirmLocationPermissionPopup
  ])
  if ctx.is_page(pairing_pages.PairAndLinkPage):
    ctx.page.cancel()
  elif ctx.is_page(other_pages.ConfirmLocationPermissionPopup):
    ctx.page.cancel()

  ctx.expect_page(account_pages.PairedDeviceDetailPage)
  ctx.page.unpair()
  ctx.expect_page(account_pages.UnpairConfirmPage)
  ctx.page.confirm()
  ctx.expect_page(account_pages.AccountPage)


def remove_all_paired_devices(ctx: context.Context) -> int:
  """Removes all paired devices.

  Args:
    ctx: Context object of Fitbit Companion App.

  Returns:
    The number of paired device being removed.

  Raises:
    errors.ContextError: Fail to reach target page.
    AssertionError: Fail in evaluation after pairing.
  """
  removed_count = 0
  ctx.go_page(account_pages.AccountPage)
  paired_device_nodes = ctx.page.get_paired_devices()
  while paired_device_nodes:
    _click_unpair_button_on_device(ctx, paired_device_nodes[0])
    removed_count += 1
    paired_device_nodes = ctx.page.get_paired_devices()

  return removed_count


def pair_pin_dec(
    func: Callable[[context.Context, fitbit_tracker_device.FitbitTracker], None]
) -> Callable[[context.Context, fitbit_tracker_device.FitbitTracker], None]:

  def wrapper(*args, **kw):
    device = args[1]
    device.log.debug('Starting the pair-pin subscription...')
    try:
      device._device.bt.pair_pin_start()
    except fitbit_tracker_cli.CliError as err:
      if err and 'Already subscribed on pubsub' in err.output[0]:
        device.log.warning('Fitbit device already subscribed on pubsub!')
      else:
        raise err

    try:
      func(*args, **kw)
    finally:
      device.log.debug('Stopping the pair-pin subscription...')
      device._device.bt.pair_pin_stop()

  return wrapper


@retry.logged_retry_on_exception(
    retry_value=(errors.ContextError),
    retry_intervals=retry.FuzzedExponentialIntervals(
        initial_delay_sec=1, num_retries=5, factor=1.1))
def _trigger_pairing(ctx: context.Context, fitbit_prod_name: str) -> None:
  """Triggers the pairing process by Fitbit production name.

  Args:
    ctx: Context object of Fitbit Companion App.
    fitbit_prod_name: Fitbit product name to select.
  """
  ctx.go_page(account_pages.AccountPage)

  def _eval_existence_of_fitbit_product_name(node, name=fitbit_prod_name):
    return name in node.text

  ctx.log.info('Selecting device=%s...', fitbit_prod_name)
  ctx.page.add_device()
  ctx.expect_page(
      pairing_pages.ChooseTrackerPage,
      node_eval=_eval_existence_of_fitbit_product_name)
  ctx.page.select_device(fitbit_prod_name)
  ctx.page.confirm()

  ctx.log.info('Accept pairing privacy requirement...')
  ctx.expect_page(pairing_pages.PairPrivacyConfirmPage)
  ctx.page.accept()
  ctx.expect_page(pairing_pages.ConfirmChargePage)
  ctx.page.next()
  if ctx.is_page(other_pages.LocationDisabledPage):
    # Optional page when you are required to enable location
    # permission for Fitbit device.
    ctx.log.info('Enabling location permission...')
    ctx.page.enable()
    ctx.expect_page(other_pages.SettingLocation)
    ctx.page.set(True)
    ctx.page.back()


def _handle_pairing_4digit_page(ctx: context.Context,
                                device: fitbit_tracker_device.FitbitTracker,
                                max_pairing_retries: int = 10) -> None:
  """Handles the input of 4-digit pairing pins.

  Args:
    ctx: Context object of Fitbit Companion App.
    device: Fitbit tracker device.
    max_pairing_retries: Number of retry in pairing step.

  Raises:
    signals.TestError: Fail to input 4-digit pairing pins.
  """
  ctx.expect_page(pairing_pages.Pairing4DigitPage, wait_sec=300)
  pins = device.pair_pin_show()
  ctx.log.info('Pairing pins=%s...', pins)
  ctx.page.input_pins(pins)
  pair_retry = 0
  while (ctx.is_page(pairing_pages.Pairing4DigitPage) and
         ctx.page.get_node_by_func(
             lambda n: _INVALID_PAIRING_CODE_MESSAGE in n.text) is not None):
    pair_retry += 1
    if pair_retry >= max_pairing_retries:
      raise signals.TestError(
          f'Failed in pairing pins matching after {pair_retry} tries!')
    pins = device.pair_pin_show()
    ctx.log.warning('Retrying on pairing pins=%s...', pins)
    ctx.page.input_pins(pins)
    time.sleep(1)


def _handle_post_pairing_flow(ctx: context.Context,
                              max_pairing_retries: int = 10) -> None:
  """Handles the pages after pairing process.

  Args:
    ctx: Context object of Fitbit Companion App.
    max_pairing_retries: Number of retry in pairing step.

  Raises:
    signals.TestError: Fail complete the post pairing flow.
  """
  ctx.log.info('Entering post pairing flow...')
  pair_retry = 0
  while True:
    ctx.expect_pages([
        account_pages.AccountPage,
        pairing_pages.PairRetryPage,
        pairing_pages.PairAndLinkPage,
        pairing_pages.PairingIntroPage,
        pairing_pages.PairingConfirmPage,
        pairing_pages.CancelPairPage,
        pairing_pages.CancelPair2Page,
        other_pages.AllowNotification,
        other_pages.LinkConfirmPage,
        other_pages.FitbitManagePopup,
    ],
                     wait_sec=90)
    if ctx.is_page(pairing_pages.PairingConfirmPage):
      ctx.log.info('Accept pairing confirm page...')
      ctx.page.confirm()
    elif ctx.is_page(pairing_pages.PairRetryPage):
      ctx.log.warning('Skip pair retry page...')
      ctx.back()
    elif ctx.is_page(pairing_pages.PairAndLinkPage):
      ctx.log.warning('Skip pair and link page...')
      ctx.page.cancel()
    elif (ctx.is_page(pairing_pages.CancelPairPage) or
          ctx.is_page(pairing_pages.CancelPair2Page)):
      ctx.log.warning('Skip pair-cancel page...')
      ctx.page.yes()
    elif ctx.is_page(other_pages.AllowNotification):
      ctx.log.warning('Allow notification page...')
      ctx.page.allow()
    elif ctx.is_page(other_pages.LinkConfirmPage):
      ctx.log.warning('Allow Fitbit to manage device page...')
      ctx.page.ok()
    elif ctx.is_page(pairing_pages.PairingIntroPage):
      ctx.log.info('Passing through Fitbit introduction pages...')
      break
    elif ctx.is_page(other_pages.FitbitManagePopup):
      ctx.log.info('Allow Fitbit to manage device...')
      ctx.page.allow()
    elif ctx.is_page(account_pages.AccountPage):
      ctx.log.info(
          'Completed pairing process (shortcut)!')
      return

    pair_retry += 1
    if pair_retry >= max_pairing_retries:
      raise signals.TestError(
          f'Failed in pairing process after {pair_retry} tries!')

  ctx.expect_page(pairing_pages.PairingIntroPage)
  while ctx.is_page(pairing_pages.PairingIntroPage):
    ctx.page.next()

  ctx.expect_pages([
      pairing_pages.PremiumPage, other_pages.PurchaseFail,
      account_pages.AccountPage
  ])

  if ctx.is_page(pairing_pages.PremiumPage):
    # Preminum page is optional.
    ctx.page.done()
  elif ctx.is_page(other_pages.PurchaseFail):
    # Optional page observed during manual pairing experiment.
    ctx.page.ok()

  ctx.expect_page(account_pages.AccountPage)
  ctx.log.info('Completing pairing process and start evaluation process...')


@pair_pin_dec
def pair_device(ctx: context.Context,
                device: fitbit_tracker_device.FitbitTracker,
                max_pairing_retries: int = 10) -> None:
  """Pairs with given Fitbit tracker device.

  Args:
    ctx: Context object of Fitbit Companion App.
    device: Fitbit tracker device.
    max_pairing_retries: Number of retry in pairing step.

  Raises:
    fitbit_tracker_cli.CliError: Fail in device's CLI operation.
    signals.TestError: Fail in pairing process.
  """
  ctx.regr_page_call(pairing_pages.BTPermissionRequestPopup, 'ok')
  ctx.regr_page_call(pairing_pages.CancelPairPage, 'yes')
  ctx.regr_page_call(other_pages.FitbitLocationPermissionPopup, 'back')
  ctx.regr_page_call(ui_core.NonePage, 'swipe_left')
  ctx.regr_page_call(pairing_pages.PairRetryPage, 'retry')
  ctx.regr_page_call(other_pages.NetworkOpFailPage, 'cancel')
  ctx.regr_page_call(other_pages.PlayfulPage, 'skip')
  ctx.regr_page_call(other_pages.LinkConfirmPage, 'ok')
  ctx.regr_page_call(other_pages.PurchaseFail, 'ok')
  ctx.regr_page_call(pairing_pages.PremiumPage, 'done')
  ctx.regr_page_call(other_pages.PurchaseFail, 'ok')
  ctx.regr_page_call(pairing_pages.UpdateDevicePage, 'update_later')

  ctx.go_page(account_pages.AccountPage)
  fitbit_prod_name = MODEL_TO_PRODUCT_NAME_MAPPING[device.model]
  paired_device_names = [node.text for node in ctx.page.get_paired_devices()]
  if fitbit_prod_name in paired_device_names:
    ctx.log.info('Device %s is already paired!', device)
    return

  # Select Fitbit device to trigger pairing process.
  _trigger_pairing(ctx, fitbit_prod_name)

  # Pairing process will begin with input of 4 digit pins.
  _handle_pairing_4digit_page(ctx, device, max_pairing_retries)

  # Handle post flow after pairing process.
  _handle_post_pairing_flow(ctx, max_pairing_retries)

  paired_device_nodes = ctx.page.get_paired_devices()
  asserts.assert_true(
      fitbit_prod_name in [node.text for node in paired_device_nodes],
      f'Unexpected paired device nodes={paired_device_nodes}')
