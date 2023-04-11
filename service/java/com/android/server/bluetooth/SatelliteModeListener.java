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

package com.android.server.bluetooth;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

/**
 * The SatelliteModeListener handles system satellite mode change callback and inform
 * BluetoothManagerService on this change.
 */
public class SatelliteModeListener {
    private static final String TAG = SatelliteModeListener.class.getSimpleName();

    /**
     * @hide constant copied from {@link Settings.Global}
     * TODO(b/274636414): Migrate to official API in Android V.
     */
    public static final String SETTINGS_SATELLITE_MODE_RADIOS = "satellite_mode_radios";
    /**
     * @hide constant copied from {@link Settings.Global}
     * TODO(b/274636414): Migrate to official API in Android V.
     */
    public static final String SETTINGS_SATELLITE_MODE_ENABLED = "satellite_mode_enabled";

    private final BluetoothManagerService mBluetoothManagerService;
    private final BluetoothSatelliteModeHandler mHandler;

    private static final int MSG_SATELLITE_MODE_CHANGED = 0;

    BluetoothSatelliteModeListener(BluetoothManagerService service, Looper looper, 
              Context context) {
        mBluetoothManagerService = service;
        mHandler = new BluetoothSatelliteModeHandler(looper);

        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(BluetoothManagerService.SETTINGS_SATELLITE_MODE_RADIOS),
                false, mSatelliteModeObserver);
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(BluetoothManagerService.SETTINGS_SATELLITE_MODE_ENABLED),
                false, mSatelliteModeObserver);
    }

    private final ContentObserver mSatelliteModeObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean unused) {
            // Post from system main thread to android_io thread.
            mHandler.sendEmptyMessage(MSG_SATELLITE_MODE_CHANGED);
        }
    };

    private class BluetoothSatelliteModeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_SATELLITE_MODE_CHANGED) {
                Log.e(TAG, "Invalid message: " + msg.what);
                return;
            }
            mBluetoothManagerService.onSatelliteModeChanged();
        }
    }
}
