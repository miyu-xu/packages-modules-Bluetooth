/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.bluetooth.le_audio;

import com.android.internal.annotations.VisibleForTesting;

public class LeAudioMetricsWriter {

    static LeAudioMetricsWriter sLeAudioMetricsWriter = null;

    public static synchronized LeAudioMetricsWriter getInstance() {
        if (sLeAudioMetricsWriter == null) {
            sLeAudioMetricsWriter = new LeAudioMetricsWriter();
        }
        return sLeAudioMetricsWriter;
    }

    private static void setInstance(LeAudioMetricsWriter writer) {
        sLeAudioMetricsWriter = writer;
    }

    private LeAudioMetricsWriter() {
    }

    public void write(long[] connectingOffsets, long[] connectedOffsets, long[] durations,
            int[] connectionStatuses, int[] disconnectionStatuses, int[] metricIds) {
        // TODO(207811438): Log LeAudioMetrics in AOSP
    }
}
