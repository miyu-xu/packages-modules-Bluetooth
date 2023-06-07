/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.server.bluetooth.satellite

// import android.test.mock.MockContentResolver
// import com.android.internal.util.test.FakeSettingsProvider
// import com.android.server.bluetooth.satellite as satellite
import android.content.ContentResolver
import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BluetoothSatelliteModeListenerTest {
    private val resolver: ContentResolver =
        ApplicationProvider.getApplicationContext<Context>().getContentResolver()

    private val looper: Looper = Looper.getMainLooper()

    private var mode: Boolean? = null

    private fun enableSensitive() {
        Settings.Global.putString(resolver, SETTINGS_SATELLITE_MODE_RADIOS, "foo,bluetooth,bar")
        shadowOf(looper).idle()
    }

    private fun disableSensitive() {
        Settings.Global.putString(resolver, SETTINGS_SATELLITE_MODE_RADIOS, "foo,bar")
        shadowOf(looper).idle()
    }

    private fun disableMode() {
        Settings.Global.putInt(resolver, SETTINGS_SATELLITE_MODE_ENABLED, 0)
        shadowOf(looper).idle()
    }

    private fun enableMode() {
        Settings.Global.putInt(resolver, SETTINGS_SATELLITE_MODE_ENABLED, 1)
        shadowOf(looper).idle()
    }

    private fun callback(newMode: Boolean) {
        assertThat(mode).isNull()
        mode = newMode
    }

    private fun assertValueAndCallback(value: Boolean, isServiceCall: Boolean) {
        assertThat(isOn).isEqualTo(value)
        if (isServiceCall) {
            assertThat(mode).isEqualTo(value)
            mode = null
        } else {
            assertThat(mode).isNull()
        }
    }

    @Test
    fun nullSensitiveInitialValue() {
        Settings.Global.putString(resolver, SETTINGS_SATELLITE_MODE_RADIOS, null)
        enableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(false, false)
    }

    @Test
    fun notSensitiveInitialValue() {
        disableSensitive()
        enableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(false, false)
    }

    @Test
    fun notSensitiveDiscardModeChange() {
        disableSensitive()
        disableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(false, false)

        enableMode()
        assertValueAndCallback(false, false)
    }

    @Test
    fun sensitiveInitialValueOff() {
        enableSensitive()
        disableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(false, false)
    }

    @Test
    fun sensitiveInitialValueOn() {
        enableSensitive()
        enableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(true, false)
    }

    @Test
    fun togglingSensitiveValue() {
        enableSensitive()
        enableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(true, false)
        // -- end of setup

        disableSensitive()
        assertValueAndCallback(false, true)

        enableSensitive()
        assertValueAndCallback(true, true)
    }

    @Test
    fun togglingEnableValue() {
        enableSensitive()
        disableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(false, false)
        // -- end of setup

        enableMode()
        assertValueAndCallback(true, true)

        disableMode()
        assertValueAndCallback(false, true)
    }

    @Test
    fun discardDuplicateEventOff() {
        enableSensitive()
        disableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(false, false)
        // -- end of setup

        disableMode()
        assertValueAndCallback(false, false)
    }

    @Test
    fun discardDuplicateEventOn() {
        enableSensitive()
        enableMode()
        initialize(looper, resolver, this::callback)
        assertValueAndCallback(true, false)
        // -- end of setup

        enableMode()
        assertValueAndCallback(true, false)
    }
}
