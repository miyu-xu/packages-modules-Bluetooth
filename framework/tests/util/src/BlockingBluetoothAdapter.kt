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
package android.bluetooth.test_utils

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_PRIVILEGED
import android.app.UiAutomation
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_BLE_STATE_CHANGED
import android.bluetooth.BluetoothAdapter.STATE_BLE_ON
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.bluetooth.BluetoothAdapter.STATE_ON
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG: String = "BlockingBluetoothAdapter"

object BlockingBluetoothAdapter {
    private val context = InstrumentationRegistry.getInstrumentation().getContext()
    @JvmStatic val adapter = context.getSystemService(BluetoothManager::class.java).getAdapter()

    private val state = AdapterStateListener(context, adapter)
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()

    // BLE_START_TIMEOUT_DELAY + BREDR_START_TIMEOUT_DELAY + (10 seconds of additional delay)
    private val stateChangeTimeout = 18.seconds

    init {
        Log.d(TAG, "Started with initial state to $state")
    }

    /** Set Bluetooth in BLE mode. Only works if it was OFF before */
    @JvmStatic
    fun enableBLE(): Boolean {
        if (!state.oneOf(STATE_OFF)) {
            throw IllegalStateException("Invalid call to enableBLE while current state is: $state")
        }
        Log.d(TAG, "Call to enableBLE")
        if (!withPermission(BLUETOOTH_CONNECT).use { adapter.enableBLE() }) {
            Log.e(TAG, "enableBLE: Failed")
            return false
        }
        return state.waitForStateWithTimeout(stateChangeTimeout, STATE_BLE_ON)
    }

    /** Restore Bluetooth to OFF. Only works if it was in BLE_ON due to enableBLE call */
    @JvmStatic
    fun disableBLE(): Boolean {
        if (!state.oneOf(STATE_BLE_ON)) {
            throw IllegalStateException("Invalid call to disableBLE while current state is: $state")
        }
        Log.d(TAG, "Call to disableBLE")
        if (!withPermission(BLUETOOTH_CONNECT).use { adapter.disableBLE() }) {
            Log.e(TAG, "disableBLE: Failed")
            return false
        }
        return state.waitForStateWithTimeout(stateChangeTimeout, STATE_OFF)
    }

    /** Turn Bluetooth ON and wait for state change */
    @JvmStatic
    fun enable(): Boolean {
        if (state.oneOf(STATE_ON)) {
            Log.i(TAG, "enable: state is already $state")
            return true
        }
        Log.d(TAG, "Call to enable")
        if (
            !withPermission(BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED).use {
                @Suppress("DEPRECATION") adapter.enable()
            }
        ) {
            Log.e(TAG, "enable: Failed")
            return false
        }
        return state.waitForStateWithTimeout(stateChangeTimeout, STATE_ON)
    }

    /** Turn Bluetooth OFF and wait for state change */
    @JvmStatic
    fun disable(persist: Boolean = true): Boolean {
        if (state.oneOf(STATE_OFF)) {
            Log.i(TAG, "disable: state is already $state")
            return true
        }
        Log.d(TAG, "Call to disable($persist)")
        if (
            !withPermission(BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED).use {
                adapter.disable(persist)
            }
        ) {
            Log.e(TAG, "disable: Failed")
            return false
        }
        // When the test started it couldn't differentiate between OFF or ble turning ON/OFF.
        // If it was OFF and we asked to disable Bluetooth, there will be no state transition.
        // Setting it to OFF now is safe as we already initiated the transition
        if (state.get() == state.STATE_UNKNOWN) {
            state.set(STATE_OFF)
        }
        return state.waitForStateWithTimeout(stateChangeTimeout, STATE_OFF)
    }

    private fun restorePermissions(permissions: Set<String>) {
        if (UiAutomation.ALL_PERMISSIONS.equals(permissions)) {
            uiAutomation.adoptShellPermissionIdentity()
        } else {
            uiAutomation.adoptShellPermissionIdentity(*permissions.map { it }.toTypedArray())
        }
    }

    private fun replacePermissionsWith(vararg newPermissions: String): Set<String> {
        val currentPermissions = uiAutomation.getAdoptedShellPermissions()
        if (newPermissions.size == 0) {
            // Throw even if the code support it as we are not expecting this by design
            throw IllegalArgumentException("Invalid permissions replacement with no permissions.")
        }
        uiAutomation.adoptShellPermissionIdentity(*newPermissions)
        return currentPermissions
    }

    // Set permissions to be used as long as the resource is open.
    // Restore initial permissions after closing resource.
    private fun withPermission(
        vararg newPermissions: String,
    ): AutoCloseable {
        val savedPermissions = replacePermissionsWith(*newPermissions)
        return AutoCloseable { restorePermissions(savedPermissions) }
    }
}

private class AdapterStateListener(context: Context, adapter: BluetoothAdapter) {
    // MutableStateFlow cannot be used because it is conflated (See official doc)
    private val _state = MutableSharedFlow<Int>(1 /* replay only most recent value*/)
    val STATE_UNKNOWN = -42

    init {
        set(getBluetoothState(adapter))
        context.registerReceiver(BluetoothAdapterReceiver(), IntentFilter(ACTION_BLE_STATE_CHANGED))
    }

    fun set(s: Int) = runBlocking { _state.emit(s) }

    fun get(): Int = _state.replayCache.get(0)

    fun oneOf(vararg states: Int): Boolean = states.contains(get())

    override fun toString() = BluetoothAdapter.nameForState(get())

    suspend fun waitForState(vararg states: Int) = _state.filter { states.contains(it) }.first()

    fun waitForStateWithTimeout(timeout: Duration, vararg states: Int): Boolean = runBlocking {
        withTimeoutOrNull(timeout) { waitForState(*states) } != null
    }

    private inner class BluetoothAdapterReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            set(newState)
            Log.d(TAG, "State changed to ${this@AdapterStateListener}")
        }
    }

    private fun getBluetoothState(adapter: BluetoothAdapter): Int {
        val state: Int = adapter.getState()
        return if (state != STATE_OFF) {
            state
        } else if (adapter.isLeEnabled()) {
            STATE_BLE_ON
        } else {
            // Bluetooth may be turning on/off BLE. Prefer initializing with an impossible value
            STATE_UNKNOWN
        }
    }
}
