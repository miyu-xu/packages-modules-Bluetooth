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
package com.android.server.bluetooth

import android.bluetooth.BluetoothAdapter.STATE_ON
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

public class TemporaryOffListener private constructor() {
    companion object {

        /** @return true if an alarm is scheduled */
        @JvmStatic
        public var isScheduled = false
            internal set

        /**
         * Listen on settings value and trigger callback as needed for the temporary off / auto_on
         * feature
         *
         * Known limitation:
         * * If the phone is off during the time the alarm would expire, it will skip a day: b/XXX
         * * If changing the time / timezone skip the alarm, it will skip a day: b/XXX
         *
         * @param callback_on: The callback to trigger when bluetooth need to be started
         * @param callback_off: The callback to trigger when bluetooth need to be stopped
         */
        @JvmStatic
        public fun initialize(
            context: Context,
            looper: Looper,
            resolver: ContentResolver,
            state: BluetoothAdapterState,
            callback_on: () -> Unit,
            callback_off: () -> Unit
        ) {
            val handler = Handler(looper)
            mHandler = handler

            val observer =
                object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean) {
                        val newMode = isTemporaryOff(resolver)

                        if (isScheduled == newMode) {
                            if (newMode) {
                                Log.d(TAG, "Reschedule alarm because trigger time has changed")
                                setupTimer(resolver, handler, callback_on)
                            } else {
                                Log.d(TAG, "Ignore content change: Timer is already disabled")
                            }
                            return
                        }
                        if (newMode && !state.oneOf(STATE_ON)) {
                            // In order to turn on bt, a proper API should be used.
                            Log.d(TAG, "Ignore content change: Bluetooth is not STATE_ON")
                            return
                        }

                        if (newMode) {
                            setupTimer(resolver, handler, callback_on)
                            Log.i(TAG, "Shutting down Bluetooth now")
                            callback_off()
                        } else {
                            Log.i(TAG, "Restarting Bluetooth now")
                            callback_on()
                            stopTimer(handler)
                        }
                    }
                }

            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (!isScheduled) {
                            return
                        }
                        // TODO: b/XXX - Define what to do if the time change skipped the alarm
                        Log.i(TAG, "Received ${intent.action} that trigger a new alarm scheduling")
                        setupTimer(resolver, handler, callback_on)
                    }
                }

            context.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_TIME_CHANGED)
                },
                null,
                handler
            )

            val notifyForDescendants = false

            resolver.registerContentObserver(
                Settings.Global.getUriFor(SETTINGS_TEMPORARY_OFF),
                notifyForDescendants,
                observer
            )

            // Nice to have: Reschedule timer when time to wake is updated
            resolver.registerContentObserver(
                Settings.Global.getUriFor(SETTINGS_AUTO_ON_HOUR),
                notifyForDescendants,
                observer
            )
            resolver.registerContentObserver(
                Settings.Global.getUriFor(SETTINGS_AUTO_ON_DAYS),
                notifyForDescendants,
                observer
            )

            if (!isTemporaryOff(resolver)) {
                isScheduled = false
            } else {
                // This case happen when the bluetooth was temporary off and there been a reboot.

                // TODO: b/XXX - Define what to do if alarm expired while the phone was shutdown
                setupTimer(resolver, handler, callback_on)
            }

            Log.i(TAG, "Initialized successfully with state: $isScheduled")
        }

        @JvmStatic
        public fun notifyUserToggledBluetooth(resolver: ContentResolver) {
            if (!isScheduled) {
                return
            }

            // This case happen when Bluetooth is manually toggled while the timer was active.
            // This will override the state of the feature since:
            //   * if Bluetooth is on, the timer doesn't make sens anymore
            //   * if Bluetooth is complete off, the user decision will override the timer

            stopTimer(mHandler!!)
            disableTemporaryOff(resolver)
        }

        private var mHandler: Handler? = null
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////// PRIVATE METHODS /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

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
internal const val SETTINGS_AUTO_ON_DAYS = "bluetooth_auto_on_days"

private val msgToken = object {}

private fun setupTimer(
    resolver: ContentResolver,
    handler: Handler,
    callback_on: () -> Unit
): Boolean {
    val now = LocalDateTime.now()
    // Set wake-up time to be Tomorrow at X.
    // If being toggle between midnight and X, the sleep time will be more than 24 hours
    val then =
        LocalDateTime.of(now.toLocalDate(), LocalTime.of(getAutoOnHour(resolver), 0)).plusDays(1)

    // Using duration is overkill for the logic, but it give a better logging
    val timeToSleep = now.until(then, ChronoUnit.NANOS).toDuration(DurationUnit.NANOSECONDS)

    handler.postDelayed(
        {
            TemporaryOffListener.isScheduled = false
            disableTemporaryOff(resolver)
            Log.i(TAG, "Bluetooth starting after ${timeToSleep}. Was scheduled ${now}")
            callback_on()
        },
        msgToken,
        timeToSleep.inWholeMilliseconds
    )
    TemporaryOffListener.isScheduled = true
    Log.i(TAG, "Bluetooth will be restarted ${then}, in ${timeToSleep}")
    return true
}

private fun stopTimer(handler: Handler) {
    handler.removeCallbacksAndMessages(msgToken)
    TemporaryOffListener.isScheduled = false
    Log.i(TAG, "Timer to restart Bluetooth is now stopped")
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
private fun getAutoOnDays(resolver: ContentResolver): Set<Int> {
    return (Settings.Global.getString(resolver, SETTINGS_AUTO_ON_DAYS) ?: "0,1,2,3,4,5,6")
        .split(",")
        .map { it.toInt().coerceIn(0, 6) }
        .toSet()
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return wether the bluetooth is currently in temporary off state
 */
private fun isTemporaryOff(resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(resolver, SETTINGS_TEMPORARY_OFF, 0) == 1
}

/** *Do not use outside of this file to avoid async issues* */
private fun disableTemporaryOff(resolver: ContentResolver) {
    Settings.Global.putInt(resolver, SETTINGS_TEMPORARY_OFF, 0)
}
