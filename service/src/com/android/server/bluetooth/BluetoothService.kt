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
package com.android.server.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.HandlerThread
import android.os.UserManager
import com.android.bluetooth.flags.Flags;
import com.android.server.SystemService
import com.android.server.SystemService.TargetUser

class BluetoothService(context: Context) : SystemService(context) {
    private val mHandlerThread: HandlerThread?
    private val bluetoothServiceImpl: BluetoothServiceImpl?
    private val mBluetoothManagerService: BluetoothManagerService?
    private var mInitialized = false

    init {
        if (Flags.systemServerMessenger()) {
            mHandlerThread = null
            mBluetoothManagerService = null
            bluetoothServiceImpl = BluetoothServiceImpl(context)
        } else {
            mHandlerThread = HandlerThread("BluetoothManagerService")
            mHandlerThread.start()
            mBluetoothManagerService = BluetoothManagerService(context, mHandlerThread.getLooper())
            bluetoothServiceImpl = null
        }
    }

    private fun initialize(user: TargetUser) {
        if (!mInitialized) {
            mBluetoothManagerService?.handleOnBootPhase(user.userHandle)
            mInitialized = true
        }
    }

    override fun onStart() {
        if (Flags.systemServerMessenger()) {
            bluetoothServiceImpl!!.onStart()
            return;
        }
    }

    override fun onBootPhase(phase: Int) {
        if (Flags.systemServerMessenger()) {
            bluetoothServiceImpl!!.onBootPhase(phase);
            return;
        }
        if (phase == SystemService.PHASE_SYSTEM_SERVICES_READY) {
            publishBinderService(
                BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE,
                mBluetoothManagerService!!.getBinder()
            )
        }
    }

    override fun onUserStarting(user: TargetUser) {
        if (Flags.systemServerMessenger()) {
            bluetoothServiceImpl!!.onUserStarting(user)
            return;
        }
        if (!UserManager.isHeadlessSystemUserMode()) {
            initialize(user)
        }
    }

    override fun onUserSwitching(from: TargetUser?, to: TargetUser) {
        if (Flags.systemServerMessenger()) {
            bluetoothServiceImpl!!.onUserSwitching(from, to)
            return;
        }
        if (!mInitialized) {
            initialize(to)
        } else {
            mBluetoothManagerService!!.onSwitchUser(to.userHandle)
        }
    }

    override fun onUserUnlocking(user: TargetUser) {
        if (Flags.systemServerMessenger()) {
            bluetoothServiceImpl!!.onUserUnlocking(user)
            return;
        }
        mBluetoothManagerService!!.handleOnUnlockUser(user.userHandle)
    }
}
