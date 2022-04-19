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

/*
 * Defines the native interface that is used by state machine/service to
 * send or receive messages from the native stack. This file is registered
 * for the native methods in the corresponding JNI C++ file.
 */
package com.android.bluetooth.le_audio;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le_audio.LeAudioMetricsWriter;
import android.util.Log;

import com.android.bluetooth.btservice.AdapterService;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collecting data for LeAudio Metrics, assembling them and send to Statsd.
 *
 * @hide
 */
public class LeAudioMetrics {
    private static final String TAG = "LeAudioMetrics";
    public static int STATUS_UNKNOWN = 0;
    public static int STATUS_SUCCESS = 1;
    public static int STATUS_FAILED = 2;

    static class DeviceMetrics {
        BluetoothDevice mBluetoothDevice;
        long mConnectingTimestampNanos = -1L;
        long mConnectedTimeStampNanos = -1L;
        long mDisconnectedTimestampNanos = -1L;
        int mConnectionStatus = STATUS_UNKNOWN;
        int mDisconnectionStatus = STATUS_UNKNOWN;

        DeviceMetrics(BluetoothDevice device) {
            this.mBluetoothDevice = device;
        }

        void addStateChangedEvent(int state, long timestampNanos, int status) {
            switch (state) {
                case BluetoothProfile.STATE_CONNECTING:
                    mConnectingTimestampNanos = timestampNanos;
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    mConnectedTimeStampNanos = timestampNanos;
                    mConnectionStatus = status;
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mDisconnectedTimestampNanos = timestampNanos;
                    mDisconnectionStatus = status;
                    break;
                default:
                    Log.w(TAG, "Unexpected state " + state);
                    break;
            }
        }
    }

    static class GroupMetrics {
        int mGroupId;
        List<DeviceMetrics> mDeviceMetrics = new ArrayList<>();
        Map<BluetoothDevice, DeviceMetrics> mOpenedDevices = new HashMap<>();
        long mBeginningTimestampNanos = Long.MAX_VALUE;
        AdapterService mAdapterService = AdapterService.getAdapterService();

        GroupMetrics(int groupId) {
            mGroupId = groupId;
        }

        void addStateChangedEvent(BluetoothDevice device, int state, int status) {
            if (!mOpenedDevices.containsKey(device)) {
                DeviceMetrics deviceMetrics = new DeviceMetrics(device);
                mOpenedDevices.put(device, deviceMetrics);
                mDeviceMetrics.add(deviceMetrics);
            }
            long timestamp = System.nanoTime();
            DeviceMetrics deviceMetrics = mOpenedDevices.get(device);
            deviceMetrics.addStateChangedEvent(state, timestamp, status);
            mBeginningTimestampNanos = Math.min(mBeginningTimestampNanos, timestamp);
            if (state == BluetoothProfile.STATE_DISCONNECTED) {
                mOpenedDevices.remove(device);
            }
        }

        boolean isClosed() {
            return mOpenedDevices.isEmpty();
        }

        void writeStats() {
            int size = mDeviceMetrics.size();
            long[] connectingOffsets = new long[size];
            long[] connectedOffsets = new long[size];
            long[] durations = new long[size];
            int[] connectionStatuses = new int[size];
            int[] disconnectionStatuses = new int[size];
            int[] metricIds = new int[size];

            for (int i = 0; i < size; i++) {
                DeviceMetrics deviceMetrics = mDeviceMetrics.get(i);
                connectingOffsets[i] = deviceMetrics.mConnectingTimestampNanos == -1L ? -1L :
                        deviceMetrics.mConnectingTimestampNanos - mBeginningTimestampNanos;
                connectedOffsets[i] = deviceMetrics.mConnectingTimestampNanos == -1L ? -1L :
                        deviceMetrics.mConnectedTimeStampNanos - mBeginningTimestampNanos;
                durations[i] = deviceMetrics.mDisconnectedTimestampNanos
                        - deviceMetrics.mConnectedTimeStampNanos;
                connectionStatuses[i] = deviceMetrics.mConnectionStatus;
                disconnectionStatuses[i] = deviceMetrics.mDisconnectionStatus;
                metricIds[i] = mAdapterService.getMetricId(deviceMetrics.mBluetoothDevice);
            }
            LeAudioMetricsWriter.getInstance().write(connectingOffsets, connectedOffsets, durations,
                    connectionStatuses, disconnectionStatuses, metricIds);
        }
    }

    Map<Integer, GroupMetrics> mMetricsMap = new HashMap<>();
    LeAudioService mLeAudioService;

    LeAudioMetrics(LeAudioService leAudioService) {
        mLeAudioService = leAudioService;
    }

    public void addStateChangedEvent(BluetoothDevice device, int state, int status) {
        int groupId = mLeAudioService.getGroupId(device);
        if (!mMetricsMap.containsKey(groupId)) {
            mMetricsMap.put(groupId, new GroupMetrics(groupId));
        }
        GroupMetrics groupMetrics = mMetricsMap.get(groupId);
        groupMetrics.addStateChangedEvent(device, state, status);

        if (groupMetrics.isClosed()) {
            groupMetrics.writeStats();
            mMetricsMap.remove(groupId);
        }
    }

    @VisibleForTesting
    Map<Integer, GroupMetrics> getMetricsMap() {
        return mMetricsMap;
    }
}
