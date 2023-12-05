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

package com.android.bluetooth;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.util.Log;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BluetoothLoggerTest {
    private static final String TAG = "BluetoothLoggerTest";
    private static final int INVALID_LOG_LEVEL = -1;

    private final BluetoothLogger mLogger = new BluetoothLogger(TAG);

    @Test
    public void testLogLevelToStringWithValidLevels() {
        assertThat(BluetoothLogger.logLevelToString(Log.VERBOSE)).isEqualTo("VERBOSE");
        assertThat(BluetoothLogger.logLevelToString(Log.DEBUG)).isEqualTo("DEBUG");
        assertThat(BluetoothLogger.logLevelToString(Log.INFO)).isEqualTo("INFO");
        assertThat(BluetoothLogger.logLevelToString(Log.WARN)).isEqualTo("WARN");
        assertThat(BluetoothLogger.logLevelToString(Log.ERROR)).isEqualTo("ERROR");
        assertThat(BluetoothLogger.logLevelToString(Log.ASSERT)).isEqualTo("ASSERT");
        return;
    }

    @Test
    public void testLogLevelToStringWithInvalidLevels_returnsUnknown() {
        assertThat(BluetoothLogger.logLevelToString(INVALID_LOG_LEVEL)).isEqualTo("Unknown (-1)");
        return;
    }
}
