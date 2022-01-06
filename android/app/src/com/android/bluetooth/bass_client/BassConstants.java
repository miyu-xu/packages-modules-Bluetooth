/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.bluetooth.bass_client;

import android.os.ParcelUuid;

/**
 * Broadcast Audio Scan Service constants class
 */
public class BassConstants {
    public static final boolean BASS_DBG = true;
    public static final ParcelUuid BASS_UUID =
            ParcelUuid.fromString("00001852-0000-1000-8000-00805F9B34FB");
    public static final int AA_START_SCAN = 1;
    public static final int AA_SCAN_SUCCESS = 2;
    public static final int AA_SCAN_FAILURE = 3;
    public static final int AA_SCAN_TIMEOUT = 4;
    // timeout for internal scan
    public static final int AA_SCAN_TIMEOUT_MS = 1000;
    public static final int INVALID_SYNC_HANDLE = -1;
    public static final int INVALID_ADV_SID = -1;
    public static final int INVALID_ADV_ADDRESS_TYPE = -1;
    public static final int INVALID_ADV_INTERVAL = -1;
    public static final int INVALID_BROADCAST_ID = -1;
    public static final int BROADCAST_ASSIST_ADDRESS_TYPE_PUBLIC = 0;

    public static final int BassMaxBytes = 100;
}
