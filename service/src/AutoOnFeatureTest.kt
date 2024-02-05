/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.server.bluetooth.test

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.server.bluetooth.BluetoothAdapterState
import com.android.server.bluetooth.Log
import com.android.server.bluetooth.Timer
import com.android.server.bluetooth.USER_SETTINGS_KEY
import com.android.server.bluetooth.cancel
import com.android.server.bluetooth.isUserEnabled
import com.android.server.bluetooth.isUserSupported
import com.android.server.bluetooth.setUserEnabled
import com.android.server.bluetooth.setupNewTimer
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
class AutoOnFeatureTest {
    private val looper = Looper.getMainLooper()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resolver = context.contentResolver

    private lateinit var state: BluetoothAdapterState
    private var callback_count = 0

    @JvmField @Rule val testName = TestName()

    @Before
    fun setUp() {
        Log.i("AutoOnFeatureTest", "\t--> setup of " + testName.getMethodName())

        callback_count = 0
        state = BluetoothAdapterState()

        enableUserSettings()
    }

    private fun setupTimer() {
        setupNewTimer(looper, context, state, this::callback_on)
    }

    private fun enableUserSettings() {
        Settings.Secure.putInt(resolver, USER_SETTINGS_KEY, 1)
        shadowOf(looper).idle()
    }

    private fun disableUserSettings() {
        Settings.Secure.putInt(resolver, USER_SETTINGS_KEY, 0)
        shadowOf(looper).idle()
    }

    private fun restoreSettings() {
        Settings.Secure.putInt(resolver, USER_SETTINGS_KEY, -1)
        shadowOf(looper).idle()
    }

    private fun callback_on() {
        callback_count++
    }

    @Test
    fun setupTimer_whenItWasNeverUsed_isNotScheduled() {
        disableUserSettings()

        setupTimer()

        assertThat(Timer.timer).isNull()
        assertThat(callback_count).isEqualTo(0)
    }

    @Test
    fun setupTimer_whenBtOn_isNotScheduled() {
        state.set(BluetoothAdapter.STATE_ON)

        setupTimer()

        assertThat(Timer.timer).isNull()
        assertThat(callback_count).isEqualTo(0)
    }

    @Test
    fun setupTimer_whenBtOffAndUserEnabled_isScheduled() {
        setupTimer()

        assertThat(Timer.timer).isNotNull()
    }

    @Test
    fun setupTimer_whenBtOffAndUserEnabled_triggerCallback() {
        setupTimer()

        shadowOf(looper).runToEndOfTasks()
        assertThat(callback_count).isEqualTo(1)
        assertThat(Timer.timer).isNull()
    }

    @Test
    fun setupTimer_whenAlreadySetup_triggerCallbackOnce() {
        setupTimer()
        setupTimer()
        setupTimer()

        shadowOf(looper).runToEndOfTasks()
        assertThat(callback_count).isEqualTo(1)
        assertThat(Timer.timer).isNull()
    }

    @Test
    fun cancel_whenNoTimer_noCrash() {
        cancel(resolver)

        assertThat(Timer.timer).isNull()
    }

    @Test
    fun cancel_whenTimer_isNotScheduled() {
        setupTimer()
        cancel(resolver)

        shadowOf(looper).runToEndOfTasks()
        assertThat(callback_count).isEqualTo(0)
        assertThat(Timer.timer).isNull()
    }

    @Test
    fun cancel_whenSettingsUnset_enableSettings() {
        restoreSettings()

        cancel(resolver)

        assertThat(isUserSupported(resolver)).isTrue()
    }

    @Test
    fun apiIsUserEnable_whenNotSupported_throwException() {
        restoreSettings()

        assertFailsWith<IllegalStateException> { isUserEnabled(resolver) }
    }

    @Test
    fun apiSetUserEnabled_whenNotSupported_throwException() {
        restoreSettings()

        assertFailsWith<IllegalStateException> { setUserEnabled(resolver, true) }
    }

    @Test
    fun apiIsUserEnable_whenEnabled_isTrue() {
        assertThat(isUserEnabled(resolver)).isTrue()
    }

    @Test
    fun apiIsUserEnable_whenDisabled_isFalse() {
        disableUserSettings()
        assertThat(isUserEnabled(resolver)).isFalse()
    }

    @Test
    fun apiSetUserEnableToFalse_whenScheduled_isNotScheduled() {
        setupTimer()

        setUserEnabled(resolver, false)

        assertThat(isUserEnabled(resolver)).isFalse()
        assertThat(callback_count).isEqualTo(0)
        assertThat(Timer.timer).isNull()
    }

    @Test
    fun apiSetUserEnableToTrue_whenIdle_canSchedule() {
        disableUserSettings()

        setUserEnabled(resolver, true)
        setupTimer()

        assertThat(Timer.timer).isNotNull()
    }
}
