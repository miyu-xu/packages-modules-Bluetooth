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

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Periodic Advertisement Result
 */
public class PeriodicAdvertisementResult {
    private static final String TAG = PeriodicAdvertisementResult.class.getSimpleName();
    public BluetoothDevice mDevice;
    public int mAddressType;
    public int mAdvSid;
    public int mSyncHandle;
    public byte metaDataLength;
    public byte[] metaData;
    public int mPAInterval;
    public int mBroadcastId;

    PeriodicAdvertisementResult(BluetoothDevice device,
                                int addressType,
                                int syncHandle,
                                int advSid,
                                int paInterval,
                                int broadcastId) {
        mDevice = device;
        mAddressType = addressType;
        mAdvSid = advSid;
        mSyncHandle = syncHandle;
        mPAInterval = paInterval;
        mBroadcastId = broadcastId;
    }

    /**
     * Update Sync handle
     */
    public void updateSyncHandle(int syncHandle) {
        mSyncHandle = syncHandle;
    }

    /**
     * Update Adv ID
     */
    public void updateAdvSid(int advSid) {
        mAdvSid = advSid;
    }

    /**
     * Update address type
     */
    public void updateAddressType(int addressType) {
        mAddressType = addressType;
    }

    /**
     * Update Adv interval
     */
    public void updateAdvInterval(int advInterval) {
        mPAInterval = advInterval;
    }

    /**
     * Update broadcast ID
     */
    public void updateBroadcastId(int broadcastId) {
        mBroadcastId = broadcastId;
    }

    /**
     * print
     */
    public void print() {
        log("-- PeriodicAdvertisementResult --");
        log("mDevice:" + mDevice);
        log("mAddressType:" + mAddressType);
        log("mAdvSid:" + mAdvSid);
        log("mSyncHandle:" + mSyncHandle);
        log("mPAInterval:" + mPAInterval);
        log("mBroadcastId:" + mBroadcastId);
        log("-- END: PeriodicAdvertisementResult --");
    }

    static void log(String msg) {
        if (BassConstants.BASS_DBG) {
            Log.d(TAG, msg);
        }
    }
}
