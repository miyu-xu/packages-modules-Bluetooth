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
@file:JvmName("SatelliteModeListener")

package com.android.server.bluetooth.satellite

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/**
 * constant copied from {@link Settings.Global}
 *
 * TODO(b/274636414): Migrate to official API in Android V.
 */
internal const val SETTINGS_SATELLITE_MODE_RADIOS = "satellite_mode_radios"

/**
 * constant copied from {@link Settings.Global}
 *
 * TODO(b/274636414): Migrate to official API in Android V.
 */
internal const val SETTINGS_SATELLITE_MODE_ENABLED = "satellite_mode_enabled"

private const val TAG = "BluetoothSatelliteModeListener"

public var isOn = false
    private set

/** Listen on satellite mode and trigger the callback if it has changed */
public fun initialize(looper: Looper, resolver: ContentResolver, callback: (m: Boolean) -> Unit) {
    val observer =
        object : ContentObserver(Handler(looper)) {
            override fun onChange(selfChange: Boolean) {
                if (!updateMode(resolver)) {
                    Log.d(TAG, "Ignore satellite mode change. Mode is already:" + isOn)
                    return
                }
                callback(isOn)
            }
        }

    val notifyForDescendants = false
    resolver.registerContentObserver(
        Settings.Global.getUriFor(SETTINGS_SATELLITE_MODE_RADIOS),
        notifyForDescendants,
        observer
    )
    resolver.registerContentObserver(
        Settings.Global.getUriFor(SETTINGS_SATELLITE_MODE_ENABLED),
        notifyForDescendants,
        observer
    )
    updateMode(resolver)
}

/**
 * Fetch global satellite mode status and update local cache to the expected value.
 *
 * @return true only if the value has changed
 */
private fun updateMode(resolver: ContentResolver): Boolean {
    val prevMode = isOn
    isOn =
        if (isSensitive(resolver)) {
            isGlobalModeOn(resolver)
        } else {
            Log.d(TAG, "Not sensitive to satellite mode change. Forcing newMode to be false")
            false
        }
    return prevMode != isOn
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return false if Bluetooth should not listen for satellite mode change
 */
private fun isSensitive(resolver: ContentResolver): Boolean {
    val radios = Settings.Global.getString(resolver, SETTINGS_SATELLITE_MODE_RADIOS)
    return radios != null && radios.contains(Settings.Global.RADIO_BLUETOOTH)
}

/**
 * *Do not use outside of this file to avoid async issues*
 *
 * @return whether satellite mode is on or off in Global settings
 */
private fun isGlobalModeOn(resolver: ContentResolver): Boolean {
    return Settings.Global.getInt(resolver, SETTINGS_SATELLITE_MODE_ENABLED, 0) == 1
}
