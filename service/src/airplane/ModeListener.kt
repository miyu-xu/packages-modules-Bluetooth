/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("AirplaneModeListener")

package com.android.server.bluetooth.airplane

import android.bluetooth.BluetoothAdapter.STATE_BLE_ON
import android.bluetooth.BluetoothAdapter.STATE_ON
import android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF
import android.bluetooth.BluetoothAdapter.STATE_TURNING_ON
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.android.bluetooth.BluetoothStatsLog
import com.android.server.bluetooth.BluetoothAdapterState
import com.android.server.bluetooth.initializeRadioModeListener

private const val TAG = "BluetoothAirplaneModeListener"

public var isOn = false
    private set

// TODO ?
private lateinit var MyuserContext: Context

/**
 * The AirplaneModeListener handles system airplane mode change callback and checks whether we need
 * to inform BluetoothManagerService on this change.
 *
 * <p>The information of airplane mode turns on would not be passed to the BluetoothManagerService
 * when Bluetooth is on and Bluetooth is in one of the following situations:
 * <ul>
 * <li>Bluetooth A2DP is connected.
 * <li>Bluetooth Hearing Aid profile is connected.
 * <li>Bluetooth LE Audio is connected
 * <li>Bluetooth AirplaneEnhancement is activated
 * </ul>
 */
public fun initialize(
    looper: Looper,
    resolver: ContentResolver,
    bluetoothAdapterState: BluetoothAdapterState,
    callback: (m: Boolean) -> Unit
) {
    bluetoothState = bluetoothAdapterState

    // Initialize local variable
    isApmEnhancementEnabled = Settings.Global.getInt(resolver, APM_ENHANCEMENT, 0) == 1

    val airplane_callback =
        fun(newMode: Boolean) {
            val previousMode = isOn
            isOn = overrideSettingValue(MyuserContext, resolver, newMode)
            if (previousMode == isOn) {
                Log.d(TAG, "Ignore airplane mode change because is already: " + isOn)
                return
            }
            if (!isOn) {
                // airplaneSession is guarantee to be set if mode was on
                airplaneSession!!.terminate(MyuserContext)
                airplaneSession = null
                // reportEndOfAirplaneModeSession()
            }
            callback(isOn)
        }
    isOn =
        initializeRadioModeListener(
            looper,
            resolver,
            Settings.Global.AIRPLANE_MODE_RADIOS,
            Settings.Global.AIRPLANE_MODE_ON,
            airplane_callback
        )
    if (isOn) {
        airplaneSession = AirplaneSession(resolver)
    }
}

public fun notifyUserToggledBluetooth(userContext: Context, isBluetoothOn: Boolean) {
    airplaneSession?.let { it.notifyUserToggledBluetooth(userContext, isBluetoothOn) }
}

/**
 * ***********************************************************************************************
 */
private lateinit var bluetoothState: BluetoothAdapterState
// private lateinit var userContext: Context
private var isApmEnhancementEnabled: Boolean = false

private var airplaneSession: AirplaneSession? = null

private fun overrideSettingValue(
    userContext: Context,
    resolver: ContentResolver,
    newMode: Boolean
): Boolean {
    if (newMode) {
        if (shouldSkipAirplaneModeChange(userContext, resolver)) {
            Settings.Global.putInt(
                resolver,
                Settings.Global.BLUETOOTH_ON,
                2
                // TODO: BluetoothManagerService.BLUETOOTH_ON_AIRPLANE
            )
            displayUserNotificationIfNeeded(userContext, resolver)
            return false
        }
        airplaneSession = AirplaneSession(resolver)
    }
    return newMode
}

private const val TOAST_COUNT = "bluetooth_airplane_toast_count"
private const val MAX_TOAST_COUNT = 10

private fun shouldPopToast(resolver: ContentResolver): Boolean {
    val currentToastCount = Settings.Global.getInt(resolver, TOAST_COUNT, 0)

    if (currentToastCount >= MAX_TOAST_COUNT) {
        return false
    }

    Settings.Global.putInt(resolver, TOAST_COUNT, currentToastCount + 1)
    return true
}

// keeps track of whether wifi and bt remains on notification was shown
private const val APM_WIFI_BT_NOTIFICATION = "apm_wifi_bt_notification"

// keeps track of whether bt remains on notification was shown
private const val APM_BT_NOTIFICATION = "apm_bt_notification"

private fun displayUserNotificationIfNeeded(userContext: Context, resolver: ContentResolver) {
    if (!isApmEnhancementEnabled || !isBluetoothToggledOnApm(userContext)) {
        if (shouldPopToast(resolver)) {
            // TODO does this work with userContext?
            Log.e(
                TAG,
                "WILLIAM - DO NOT SUBMIT - displayUserNotificationIfNeeded attempt on userContext"
            )
            val r = userContext.getResources()
            val text: CharSequence =
                r.getString(
                    Resources.getSystem()
                        .getIdentifier("bluetooth_airplane_mode_toast", "string", "android")
                )
            // TODO does this work with userContext?
            Log.e(
                TAG,
                "WILLIAM - DO NOT SUBMIT - displayUserNotificationIfNeeded attempt on userContext for Toast"
            )
            Toast.makeText(userContext, text, Toast.LENGTH_LONG).show()
        }
        return
    }
    if (isWifiEnabledOnApm(userContext, resolver)) {
        if (isFirstTimeNotification(userContext, APM_WIFI_BT_NOTIFICATION)) {
            sendNotification(
                userContext,
                "bluetooth_and_wifi_stays_on_title",
                "bluetooth_and_wifi_stays_on_message",
                APM_WIFI_BT_NOTIFICATION
            )
        }
    } else {
        if (isFirstTimeNotification(userContext, APM_BT_NOTIFICATION)) {
            sendNotification(
                userContext,
                "bluetooth_stays_on_title",
                "bluetooth_stays_on_message",
                APM_BT_NOTIFICATION
            )
        }
    }
}

private fun shouldSkipAirplaneModeChange(userContext: Context, resolver: ContentResolver): Boolean {
    // APM feature enabled and user has used the feature by changing BT state in APM
    // BT will only remain on in APM based on user's last action in APM
    if (isApmEnhancementEnabled && isBluetoothToggledOnApm(userContext)) {
        if (isBluetoothOn() && isBluetoothOnAPM(userContext)) {
            Log.i(TAG, "Skip airplane mode change because of settings secure")
            return true
        }
        return false
    }

    // APM feature disabled or user has not used the feature yet by changing BT state in APM
    // BT will only remain on when media profile is connected
    if (
        isBluetoothOn() && false
    ) { // TODO MEDIA PROFILE && mAirplaneHelper.isMediaProfileConnected()) {
        Log.i(TAG, "Skip airplane mode change because media are connected")
        return true
    }
    return false
}

// TODO(b/290403852): Do not rely on application ressource within system server
private fun sendNotification(
    userContext: Context,
    titleId: String,
    messageId: String,
    notificationState: String
) {
    val btPackageName: String? =
        "foo" // TODO BluetoothModeChangeHelper.getBluetoothPackageName(userContext)
    if (btPackageName == null) {
        Log.e(TAG, "Unable to find Bluetooth package name")
        return
    }

    // TODO is userContext expected to works here
    Log.e(TAG, "WILLIAM - DO NOT SUBMIT - sendNotification attempt on userContext")

    val resources = userContext.packageManager.getResourcesForApplication(btPackageName)
    val title = resources.getIdentifier(titleId, "string", btPackageName)
    val message = resources.getIdentifier(messageId, "string", btPackageName)
    sendApmNotification(userContext, resources.getString(title), resources.getString(message))
    setUserSettingsSecure(userContext, notificationState, 1)
}

// keeps track of whether user enabling bt notification was shown
private const val APM_BT_ENABLED_NOTIFICATION = "apm_bt_enabled_notification"

private class AirplaneSession(val resolver: ContentResolver) {
    private val sessionStartTime: Long
    private val isBluetoothOnBeforeApmToggle: Boolean
    private val isBluetoothOnAfterApmToggle: Boolean
    private val isMediaProfileConnectedBeforeApmToggle: Boolean
    private var userToggledBluetoothDuringApm = false
    private var userToggledBluetoothDuringApmWithinMinute = false

    init {
        sessionStartTime = SystemClock.elapsedRealtime()
        // TODO
        isBluetoothOnBeforeApmToggle = isBluetoothOn()
        isBluetoothOnAfterApmToggle = false
        isMediaProfileConnectedBeforeApmToggle = false
    }

    /** userContext is NOT the system server context */
    fun notifyUserToggledBluetooth(userContext: Context, isBluetoothOn: Boolean) {
        if (
            !userToggledBluetoothDuringApm &&
                SystemClock.elapsedRealtime() - sessionStartTime < 60000
        ) {
            userToggledBluetoothDuringApmWithinMinute = true
        }
        userToggledBluetoothDuringApm = true
        if (!isApmEnhancementEnabled) {
            return
        }
        setUserSettingsSecure(
            userContext,
            BLUETOOTH_APM_STATE,
            if (isBluetoothOn) {
                1
            } else {
                0
            }
        )
        setUserSettingsSecure(userContext, APM_USER_TOGGLED_BLUETOOTH, 1)
        if (isBluetoothOn && isFirstTimeNotification(userContext, APM_BT_ENABLED_NOTIFICATION)) {
            sendNotification(
                userContext,
                "bluetooth_enabled_apm_title",
                "bluetooth_enabled_apm_message",
                APM_BT_ENABLED_NOTIFICATION
            )
        }
    }

    /** Log current airplaneSession. Session cannot be re-use */
    fun terminate(userContext: Context) {
        BluetoothStatsLog.write(
            BluetoothStatsLog.AIRPLANE_MODE_SESSION_REPORTED,
            BluetoothStatsLog.AIRPLANE_MODE_SESSION_REPORTED__PACKAGE_NAME__BLUETOOTH,
            isBluetoothOnBeforeApmToggle,
            isBluetoothOnAfterApmToggle,
            isBluetoothOn(),
            isBluetoothToggledOnApm(userContext),
            userToggledBluetoothDuringApm,
            userToggledBluetoothDuringApmWithinMinute,
            isMediaProfileConnectedBeforeApmToggle
        )
    }
}

// keeps track of whether bt should remain on in airplane mode
private const val BLUETOOTH_APM_STATE = "bluetooth_apm_state"

internal const val APM_ENHANCEMENT = "apm_enhancement_enabled"
internal const val APM_USER_TOGGLED_BLUETOOTH = "apm_user_toggled_bluetooth"

// keeps track of whether wifi should remain on in airplane mode
private const val WIFI_APM_STATE = "wifi_apm_state"

private fun isWifiEnabledOnApm(userContext: Context, resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(resolver, Settings.Global.WIFI_ON, 0) != 0 &&
        Settings.Secure.getInt(userContext.contentResolver, WIFI_APM_STATE, 0) == 1
}

/** The Airplane Enhancement Mode is defined for each user and require an explicit userContext */
private fun isFirstTimeNotification(userContext: Context, name: String): Boolean {
    // TODO: Clearing Identity should not be needed if we are on correct handler
    return Settings.Secure.getInt(userContext.contentResolver, name, 0) == 1
}

/** The Airplane Enhancement Mode is defined for each user and require an explicit userContext */
private fun isBluetoothToggledOnApm(userContext: Context): Boolean {
    return Settings.Secure.getInt(userContext.contentResolver, APM_USER_TOGGLED_BLUETOOTH, 0) == 1
}

/** The Airplane Enhancement Mode is defined for each user and require an explicit userContext */
private fun setUserSettingsSecure(userContext: Context, name: String, value: Int) {
    // TODO: Clearing Identity should not be needed if we are on correct handler
    Settings.Secure.putInt(userContext.contentResolver, name, value)
}

private fun isBluetoothOnAPM(userContext: Context): Boolean {
    return Settings.Secure.getInt(userContext.contentResolver, BLUETOOTH_APM_STATE, 0) == 1
}

private fun isBluetoothOn(): Boolean {
    return bluetoothState.oneOf(STATE_ON, STATE_BLE_ON, STATE_TURNING_ON, STATE_TURNING_OFF)
}
