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
package com.android.bluetooth.map;

import android.bluetooth.BluetoothProfile;

import com.android.bluetooth.BluetoothStatsLog;

/** Utility class to use BluetoothStatsLog.write() */
public class BluetoothMapMetricsUtils {

    // BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED
    // - fileNameEnum comes from BluetoothProtoEnums
    // - tag values are managed in each java file independently.
    static class ContentProfileErrorReported {
        static void writeException(int fileNameEnum, int tag) {
            BluetoothStatsLog.write(
                    BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED,
                    BluetoothProfile.MAP,
                    fileNameEnum,
                    BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED__TYPE__EXCEPTION,
                    tag);
        }

        static void writeErrorLog(int fileNameEnum, int tag) {
            BluetoothStatsLog.write(
                    BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED,
                    BluetoothProfile.MAP,
                    fileNameEnum,
                    BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED__TYPE__LOG_ERROR,
                    tag);
        }

        static void writeWarnLog(int fileNameEnum, int tag) {
            BluetoothStatsLog.write(
                    BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED,
                    BluetoothProfile.MAP,
                    fileNameEnum,
                    BluetoothStatsLog.BLUETOOTH_CONTENT_PROFILE_ERROR_REPORTED__TYPE__LOG_WARN,
                    tag);
        }
    }
}
