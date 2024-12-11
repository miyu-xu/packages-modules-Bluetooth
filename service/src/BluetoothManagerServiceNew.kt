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

package com.android.server.bluetooth

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.UserHandle;
import android.os.Handler
import android.bluetooth.IBluetoothManager;
import android.os.Looper
import android.os.IBinder;
import android.provider.Settings
import android.content.Context;

private const val TAG = "BluetoothManagerServiceNew"

class BluetoothManagerServiceNew(private val context: Context, looper: Looper) {
    private val handler: Handler
    private val binder: BluetoothServiceBinder
    init {
        handler= BluetoothHandler(looper)
        binder = BluetoothServiceBinder(context, looper);
    }

    public fun onSwitchUser(userHandle: UserHandle) {
        Log.d(TAG, "onSwitchUser(" + userHandle + ")");
        handler.post({handleSwitchUser(userHandle)});
    }
    public fun onUserStarting(userHandle: UserHandle) {
        Log.d(TAG, "onUserStarting(" + userHandle + ")");
    }
    public fun onUserUnlocking(userHandle: UserHandle) {
        Log.d(TAG, "onUserUnlocking(" + userHandle + ")");
    }
    public fun getBinder() : IBinder {
        return binder
    }
    private class BluetoothHandler(looper: Looper) :Handler(looper) {
    }
    private fun handleSwitchUser(userHandle: UserHandle) {
        Log.d(TAG, "handleSwitchUser(" + userHandle + "): Bluetooth boot completed");
    }
    class BinderInterface {
    }
}
