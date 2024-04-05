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

package com.android.server.bluetooth

class NativeInterface {
    private val TAG = "BluetoothServerNativeInterface"

    init {
        Log.i(TAG, "Load Bluetooth System Server JNI")
        System.loadLibrary("bluetooth_server_jni") // Loads libbluetooth_server_jni.so
    }

    /**
     * This function calls the native Android liblog library to set the process minimum log level
     *
     * The log level is set based on the value of the 'log.tag.bluetooth` log tag. This matches the
     * behavior used in the platform as well, such that the platform and system server code can
     * agree on default log level.
     */
    fun updateProcessMinimumLogLevel() {
        updateProcessMinimumLogLevelNative()
    }

    // Native functions
    private external fun updateProcessMinimumLogLevelNative()
}
