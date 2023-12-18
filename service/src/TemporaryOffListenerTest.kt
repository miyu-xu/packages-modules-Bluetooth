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
package com.android.server.bluetooth.test

import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.server.bluetooth.BluetoothAdapterState
import com.android.server.bluetooth.Log
import com.android.server.bluetooth.SETTINGS_TEMPORARY_OFF
import com.android.server.bluetooth.TemporaryOffListener
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TemporaryOffListenerTest {
    private val looper: Looper = Looper.getMainLooper()
    private val state = BluetoothAdapterState()
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val resolver: ContentResolver = mContext.contentResolver

    private enum class CallbackMode {
        ON,
        OFF
    }

    private lateinit var mode: ArrayList<CallbackMode>

    @JvmField @Rule val testName = TestName()

    @Before
    public fun setup() {
        Log.i("TemporaryOffListenerTest", "\t--> setup of " + testName.getMethodName())

        mode = ArrayList()
    }

    private fun initializeTemporaryOff() {
        TemporaryOffListener.initialize(
            looper,
            resolver,
            state,
            this::callback_on,
            this::callback_off,
        )
    }

    private fun setTemporaryOff(status: Boolean) {
        Settings.Global.putInt(resolver, SETTINGS_TEMPORARY_OFF, if (status) 1 else 0)
        shadowOf(looper).idle()
    }

    private fun getTemporaryOff(): Boolean {
        shadowOf(looper).idle()
        return Settings.Global.getInt(resolver, SETTINGS_TEMPORARY_OFF, 0) == 1
    }

    private fun callback_on() = mode.add(CallbackMode.ON)

    private fun callback_off() = mode.add(CallbackMode.OFF)

    @Test
    fun initialize_whenItWasNeverUsed_isNotScheduled() {
        initializeTemporaryOff()

        assertThat(TemporaryOffListener.isScheduled).isFalse()
        assertThat(mode).isEmpty()
        assertThat(getTemporaryOff()).isFalse()
    }

    @Test
    fun initialize_whenNotScheduled_isNotScheduled() {
        // For now, this will produce same behavior as initialize_neverSet_isNotScheduled

        setTemporaryOff(false)

        initializeTemporaryOff()

        assertThat(TemporaryOffListener.isScheduled).isFalse()
        assertThat(mode).isEmpty()
        assertThat(getTemporaryOff()).isFalse()
    }

    @Test
    fun initialize_whenScheduled_isScheduled() {
        setTemporaryOff(true)

        initializeTemporaryOff()

        assertThat(TemporaryOffListener.isScheduled).isTrue()
        assertThat(mode).isEmpty()
        assertThat(getTemporaryOff()).isTrue()
    }

    @Test
    fun enable_whenBtNotOn_isNotScheduled() {
        initializeTemporaryOff()

        setTemporaryOff(true)

        assertThat(TemporaryOffListener.isScheduled).isFalse()
        assertThat(mode).isEmpty()
        assertThat(getTemporaryOff()).isTrue()
    }

    @Test
    fun enable_whenBtOn_isScheduledAndBtOff() {
        state.set(BluetoothAdapter.STATE_ON)
        initializeTemporaryOff()

        setTemporaryOff(true)

        assertThat(TemporaryOffListener.isScheduled).isTrue()
        assertThat(mode).containsExactly(CallbackMode.OFF)
        assertThat(getTemporaryOff()).isTrue()
    }

    @Test
    fun enable_whenScheduled_doNothing() {
        state.set(BluetoothAdapter.STATE_ON)
        initializeTemporaryOff()
        setTemporaryOff(true)
        mode = ArrayList()

        setTemporaryOff(true)

        assertThat(TemporaryOffListener.isScheduled).isTrue()
        assertThat(mode).isEmpty()
        assertThat(getTemporaryOff()).isTrue()
    }

    @Test
    fun disable_whenScheduled_isNotScheduledAndBtOn() {
        state.set(BluetoothAdapter.STATE_ON)
        initializeTemporaryOff()
        setTemporaryOff(true)
        mode = ArrayList()

        setTemporaryOff(false)

        assertThat(TemporaryOffListener.isScheduled).isFalse()
        assertThat(mode).containsExactly(CallbackMode.ON)
        assertThat(getTemporaryOff()).isFalse()
    }

    @Test
    fun nothing_whenTimerExpired_turnBluetoothOn() {
        state.set(BluetoothAdapter.STATE_ON)
        initializeTemporaryOff()
        setTemporaryOff(true)
        mode = ArrayList()

        shadowOf(looper).runToEndOfTasks()

        assertThat(TemporaryOffListener.isScheduled).isFalse()
        assertThat(mode).containsExactly(CallbackMode.ON)
        assertThat(getTemporaryOff()).isFalse()
    }
}
