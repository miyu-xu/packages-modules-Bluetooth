/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.bluetooth.le_scan;

import static com.android.bluetooth.le_scan.ScanController.DEFAULT_REPORT_DELAY_FLOOR;

import android.provider.DeviceConfig;

import com.android.bluetooth.Utils;

import java.util.Set;

class BatchScanThrottler {
    private static final long SCREEN_OFF_MINIMUM_DELAY_FLOOR_MS = 20000L;
    private static final long UNFILTERED_DELAY_FLOOR_MS = 20000L;
    private static final long UNFILTERED_SCREEN_OFF_DELAY_FLOOR_MS = 60000L;
    private static final int[] BACKOFF_MULTIPLIERS = {1, 1, 2, 2, 4};
    private static final long SCREEN_OFF_DELAY_MS = 60000L;
    private final long mDelayFloor;
    private final long mScreenOffDelayFloor;
    private int mBackoffStage = 0;
    private long mScreenOffTriggerTime = 0L;
    private boolean mScreenOffThrottling = false;

    BatchScanThrottler(boolean screenOn) {
        mDelayFloor =
                DeviceConfig.getLong(
                        DeviceConfig.NAMESPACE_BLUETOOTH,
                        "report_delay",
                        DEFAULT_REPORT_DELAY_FLOOR);
        mScreenOffDelayFloor = Math.max(mDelayFloor, SCREEN_OFF_MINIMUM_DELAY_FLOOR_MS);
        onScreenOn(screenOn);
    }

    void resetBackoff() {
        mBackoffStage = 0;
    }

    void onScreenOn(boolean screenOn) {
        if (screenOn) {
            mScreenOffTriggerTime = 0L;
            mScreenOffThrottling = false;
            resetBackoff();
        } else {
            mScreenOffTriggerTime = Utils.getSystemClock().elapsedRealtime() + SCREEN_OFF_DELAY_MS;
        }
    }

    long getBatchTriggerIntervalMillis(Set<ScanClient> batchClients) {
        if (!mScreenOffThrottling
                && mScreenOffTriggerTime != 0
                && Utils.getSystemClock().elapsedRealtime() > mScreenOffTriggerTime) {
            mScreenOffThrottling = true;
            resetBackoff();
        }
        long unfilteredFloor =
                mScreenOffThrottling
                        ? UNFILTERED_SCREEN_OFF_DELAY_FLOOR_MS
                        : UNFILTERED_DELAY_FLOOR_MS;
        long intervalMillis = Long.MAX_VALUE;
        for (ScanClient client : batchClients) {
            if (client.settings != null && client.settings.getReportDelayMillis() > 0) {
                long clientIntervalMillis = client.settings.getReportDelayMillis();
                if ((client.filters == null || client.filters.isEmpty())
                        && clientIntervalMillis > unfilteredFloor) {
                    clientIntervalMillis = unfilteredFloor;
                }
                intervalMillis = Math.min(intervalMillis, clientIntervalMillis);
            }
        }
        int backoffIndex =
                mBackoffStage >= BACKOFF_MULTIPLIERS.length
                        ? BACKOFF_MULTIPLIERS.length - 1
                        : mBackoffStage++;
        return Math.max(
                intervalMillis,
                (mScreenOffThrottling ? mScreenOffDelayFloor : mDelayFloor)
                        * BACKOFF_MULTIPLIERS[backoffIndex]);
    }
}
