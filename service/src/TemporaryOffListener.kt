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
@file:JvmName("TemporaryOffListener")

package com.android.server.bluetooth

import android.bluetooth.BluetoothAdapter.STATE_ON
import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val TAG = "TemporaryOffListener"

/** @return true if Bluetooth state is impacted by autoOn feature */
public var isOn = false
    private set

/**
 * constant copied from {@link Settings.Global}
 *
 * value will be true if the feature is supported on the device (sysUi implementation & trunk stable
 * flag are release)
 *
 * TODO: b/XXX - Migrate to official API in Android V / delete once the feature has shipped
 */
internal const val SETTINGS_TEMPORARY_OFF_IS_SUPPORTED = "bluetooth_temporary_off_is_supported"

/**
 * constant copied from {@link Settings.Global}
 *
 * value will be true if the bluetooth is currently temporary off and need to be restart
 * automatically later
 *
 * TODO: b/XXX - Migrate to official API in Android V.
 */
internal const val SETTINGS_TEMPORARY_OFF = "bluetooth_temporary_off"

/**
 * constant copied from {@link Settings.Global}
 *
 * value will be an int corresponding to the hour of the day to restart the bluetooth
 *
 * TODO: b/XXX - Migrate to official API in Android V.
 */
internal const val SETTINGS_AUTO_ON_HOUR = "bluetooth_auto_on_hour"

// TODO: b/XXX - allow Bluetooth to stay off certain days of the week
/**
 * constant copied from {@link Settings.Global}
 *
 * value will be an string of comma separated value for days to restart the bluetooth
 *
 * TODO: b/XXX - Migrate to official API in Android V.
 */
// internal const val SETTINGS_AUTO_ON_DAYS = "bluetooth_auto_on_days"

/**
 * Listen on settings value and trigger callback as needed for the temporary off / auto_on feature
 *
 * @param callback_on: The callback to trigger when bluetooth need to be started
 * @param callback_off: The callback to trigger when bluetooth need to be stopped
 */
public fun initialize(
    looper: Looper,
    resolver: ContentResolver,
    state: BluetoothAdapterState,
    callback_on: () -> Unit,
    callback_off: () -> Unit
) {
    handler = Handler(looper)

    val observer =
        object : ContentObserver(Handler(looper)) {
            override fun onChange(selfChange: Boolean) {
                val previousMode = isOn
                isOn = getTemporaryOffValue(resolver) && state.oneOf(STATE_ON)
                if (previousMode == isOn) {
                    Log.d(TAG, "Ignore change because is already: ${isOn}")
                    return
                }
                handleTemporaryOff(resolver, callback_on, callback_off)
            }
        }

    // TODO: b/XXX - Handle timezone and device time change with:
    // IntentFilter().apply {
    //     addAction(Intent.ACTION_TIME_TICK)
    //     addAction(Intent.ACTION_TIMEZONE_CHANGED)
    //     addAction(Intent.ACTION_TIME_CHANGED)
    // }

    val notifyForDescendants = false

    resolver.registerContentObserver(
        Settings.Global.getUriFor(SETTINGS_TEMPORARY_OFF_IS_SUPPORTED),
        notifyForDescendants,
        observer
    )
    resolver.registerContentObserver(
        Settings.Global.getUriFor(SETTINGS_TEMPORARY_OFF),
        notifyForDescendants,
        observer
    )
    isOn = getTemporaryOffValue(resolver)
    if (isOn) {
        // This case happen when the bluetooth was temporary off and there been a reboot.

        // Known limitation:
        // If the phone is off during the restart period, the timer will be set for the next day.
        // TODO: b/XXX - Check if we skipped the restart period and override setting

        setupTimer(resolver, callback_on)
    }

    Log.i(TAG, "Initialized successfully with state: $isOn")
}

public fun notifyUserToggledBluetooth(resolver: ContentResolver) {
    if (!isOn) {
        return
    }
    // This case happen when Bluetooth is manually toggled while the timer was active.
    // This will override the state of the feature since:
    //   * if Bluetooth is on, the timer doesn't make sens anymore
    //   * if Bluetooth is complete off, the user decision will override the timer

    isOn = false
    stopTimer(resolver)
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////// PRIVATE METHODS /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

var handler: Handler? = null

private fun handleTemporaryOff(
    resolver: ContentResolver,
    callback_on: () -> Unit,
    callback_off: () -> Unit
) {
    if (isOn) {
        setupTimer(resolver, callback_on)
        callback_off()
    } else {
        Log.i(TAG, "Restarting Bluetooth now because of listener")
        callback_on()
        stopTimer(resolver)
    }
}

private fun turnOn(resolver: ContentResolver, callback_on: () -> Unit) {
    Log.i(TAG, "Restarting Bluetooth now because it is the expected time")
    isOn = false
    Settings.Global.putInt(resolver, SETTINGS_TEMPORARY_OFF, 0)
    callback_on()
}

private fun setupTimer(resolver: ContentResolver, callback_on: () -> Unit): Boolean {
    val now = LocalDateTime.now()
    var then = LocalDateTime.of(now.toLocalDate(), LocalTime.of(getAutoOnHour(resolver), 0))
    if (then.isBefore(now)) {
        then = then.plusDays(1)
    }
    val timeToSleep = now.until(then, ChronoUnit.NANOS).toDuration(DurationUnit.NANOSECONDS)

    Log.i(TAG, "Bluetooth will be restarted in ${timeToSleep}, at ${then}")
    handler?.let {
        it.postDelayed({ turnOn(resolver, callback_on) }, timeToSleep.inWholeMilliseconds)
    }

    return true
}

private fun stopTimer(resolver: ContentResolver) {
    handler?.let { it.removeCallbacksAndMessages(null) }
    isOn = false
    Settings.Global.putInt(resolver, SETTINGS_TEMPORARY_OFF, 0)
}
/**
 * Check if Bluetooth is impacted by the radio and fetch global mode status
 *
 * @return weither Bluetooth should consider this radio or not
 */
private fun getTemporaryOffValue(resolver: ContentResolver): Boolean {
    return if (isTemporaryOffSupported(resolver)) {
        isTemporaryOff(resolver)
    } else {
        Log.d(TAG, "Temporary off feature is not supported. Value forced to false")
        false
    }
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return false if autoOn feature is not supported on the device
 */
private fun isTemporaryOffSupported(resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(resolver, SETTINGS_TEMPORARY_OFF_IS_SUPPORTED, 0) != 0
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return The hour of the day to restart the bluetooth between 0 and 23, default is 5
 */
private fun getAutoOnHour(resolver: ContentResolver): Int {
    return Settings.Global.getInt(resolver, SETTINGS_AUTO_ON_HOUR, 5).coerceIn(0, 23)
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return The days to re-enable the bluetooth
 */
// private fun getAutoOnDays(resolver: ContentResolver): Set<Int> {
//     return Settings.Global.getString(resolver, SETTINGS_AUTO_ON_DAYS,
// "0,1,2,3,4,5,6").split(",").map { it.toInt().coerceIn(0,6)}.toSet()
// }

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return wether the bluetooth is currently in temporary off state
 */
private fun isTemporaryOff(resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(resolver, SETTINGS_TEMPORARY_OFF, 0) == 1
}
