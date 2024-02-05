/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("AutoOnFeature")

package com.android.server.bluetooth

import android.bluetooth.BluetoothAdapter.STATE_ON
import android.content.ContentResolver
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val TAG = "AutoOnFeature"

public fun setupNewTimer(
    looper: Looper,
    context: Context,
    state: BluetoothAdapterState,
    callback_on: () -> Unit
) {
    if (!isFeatureEnabledForUser(context.contentResolver)) {
        Log.d(TAG, "Not Enabled for current user: ${context.getUser()}")
        return
    }
    if (state.oneOf(STATE_ON)) {
        Log.d(TAG, "Bluetooth already on, no need for timer")
        return
    }
    Timer.start(looper, context, callback_on)
}

public fun pause() = Timer.pause()

public fun cancel(resolver: ContentResolver) {
    Timer.cancel()

    if (!isFeatureSupportedForUser(resolver)) {
        setFeatureEnabledForUserUnchecked(resolver, true)
    }
}

public fun isUserSupported(resolver: ContentResolver) = isFeatureSupportedForUser(resolver)

public fun isUserEnabled(resolver: ContentResolver): Boolean {
    if (!isUserSupported(resolver)) {
        throw IllegalStateException("AutoOnFeature is not supported for current user: ${resolver}")
    }
    return isFeatureEnabledForUser(resolver)
}

public fun setUserEnabled(resolver: ContentResolver, status: Boolean) {
    if (!isUserSupported(resolver)) {
        throw IllegalStateException("AutoOnFeature is not supported for current user: ${resolver}")
    }
    setFeatureEnabledForUserUnchecked(resolver, status)
    if (!status) {
        Timer.cancel()
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////// PRIVATE METHODS /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

private class Timer
private constructor(
    looper: Looper,
    val context: Context,
    callback_on: () -> Unit,
    val now: LocalDateTime,
    val target: LocalDateTime,
    val timeToSleep: Duration
) {
    private val handler = Handler(looper)

    init {
        handler.postDelayed(
            {
                Log.i(TAG, "Starting after ${timeToSleep}. Was scheduled ${now} for ${target}")
                callback_on()
            },
            timeToSleep.inWholeMilliseconds
        )
        Log.i(TAG, "Will restarted at ${target} (in ${timeToSleep})")
    }

    companion object {
        private var timer: Timer? = null

        private val STORAGE_KEY = "bluetooth_internal_automatic_turn_on_timer"

        private fun writeDateToStorage(date: LocalDateTime, resolver: ContentResolver): Boolean {
            return Settings.Secure.putString(resolver, STORAGE_KEY, date.toString())
        }

        private fun getDateFromStorage(resolver: ContentResolver): LocalDateTime? {
            val date = Settings.Secure.getString(resolver, STORAGE_KEY)
            return date?.let { LocalDateTime.parse(it) }
        }

        private fun resetStorage(resolver: ContentResolver) {
            Settings.Secure.resetToDefaults(resolver, STORAGE_KEY)
        }

        fun start(looper: Looper, context: Context, callback_on: () -> Unit) {
            timer?.let {
                // This case should never happen
                Log.e(TAG, "Concurrent timer already scheduled for ${it.target}. Cancelling it")
                cancel()
            }

            val now = LocalDateTime.now()
            val target = getDateFromStorage(context.contentResolver) ?: freshTimer(now)
            val timeToSleep =
                now.until(target, ChronoUnit.NANOS).toDuration(DurationUnit.NANOSECONDS)

            if (timeToSleep.isNegative()) {
                Log.i(TAG, "Starting now (${now}) as it was scheduled for ${target}")
                callback_on()
                return
            }

            timer = Timer(looper, context, callback_on, now, target, timeToSleep)
        }

        fun pause() {
            timer?.pause()
            timer = null
        }

        fun cancel() {
            timer?.cancel()
            timer = null
        }

        /** Return a LocalDateTime for tomorrow 5 am */
        private fun freshTimer(now: LocalDateTime) =
            LocalDateTime.of(now.toLocalDate(), LocalTime.of(5, 0)).plusDays(1)
    }

    /** Save timer to storage and stop it */
    fun pause() {
        Log.i(TAG, "Pausing timer for ${target}")
        handler.removeCallbacksAndMessages(null)
        writeDateToStorage(target, context.contentResolver)
    }

    /** Stop timer and reset storage */
    fun cancel() {
        Log.i(TAG, "Cancelling timer for ${target}")
        handler.removeCallbacksAndMessages(null)
        resetStorage(context.contentResolver)
    }
}

val USER_SETTINGS_KEY = "bluetooth_automatic_turn_on"

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return whether the auto on feature is enabled for this user
 */
private fun isFeatureEnabledForUser(resolver: ContentResolver): Boolean {
    return Settings.Secure.getInt(resolver, USER_SETTINGS_KEY, 0) == 1
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return whether the auto on feature is supported for the user
 */
private fun isFeatureSupportedForUser(resolver: ContentResolver): Boolean {
    return Settings.Secure.getInt(resolver, USER_SETTINGS_KEY, -1) != -1
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return whether the auto on feature is enabled for this user
 */
private fun setFeatureEnabledForUserUnchecked(resolver: ContentResolver, status: Boolean) {
    Settings.Secure.putInt(resolver, USER_SETTINGS_KEY, if (status) 1 else 0)
}
