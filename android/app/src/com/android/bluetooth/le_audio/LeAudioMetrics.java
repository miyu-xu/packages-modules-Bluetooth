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
import android.util.Log;

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

    private enum AtomField {
        CONNECTING_OFFSET,
        CONNECTED_OFFSET,
        DURATION,
        CONNECTION_STATUS,
        DISCONNECTION_STATUS,
    }

    ;

    private static class DeviceMetrics {
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

    private static class GroupMetrics {
        int mGroupId;
        List<DeviceMetrics> mDeviceMetrics = new ArrayList<>();
        Map<BluetoothDevice, DeviceMetrics> mOpenedDevices = new HashMap<>();
        long beginningTimestampNanos = Long.MAX_VALUE;

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
            beginningTimestampNanos = Math.min(beginningTimestampNanos, timestamp);
        }

        boolean isClosed() {
            return mOpenedDevices.isEmpty();
        }

        long[] buildLongArray(AtomField field) {
            long[] fields = new long[mDeviceMetrics.size()];
            for (int i = 0; i < mDeviceMetrics.size(); i++) {
                DeviceMetrics deviceMetrics = mDeviceMetrics.get(i);
                switch (field) {
                    case CONNECTING_OFFSET:
                        fields[i] =
                                deviceMetrics.mConnectingTimestampNanos - beginningTimestampNanos;
                        break;
                    case CONNECTED_OFFSET:
                        fields[i] =
                                deviceMetrics.mConnectedTimeStampNanos - beginningTimestampNanos;
                        break;
                    case DISCONNECTION_STATUS:
                        fields[i] = deviceMetrics.mDisconnectedTimestampNanos
                                - deviceMetrics.mConnectedTimeStampNanos;
                        break;
                    case CONNECTION_STATUS:
                    case DURATION:
                        Log.e(TAG, "Unexpected field:" + field.name());
                        return null;
                }
            }
            return fields;
        }

        int[] buildIntArray(AtomField field) {
            int[] fields = new int[mDeviceMetrics.size()];
            for (int i = 0; i < mDeviceMetrics.size(); i++) {
                DeviceMetrics deviceMetrics = mDeviceMetrics.get(i);
                switch (field) {
                    case CONNECTION_STATUS:
                        fields[i] = deviceMetrics.mConnectionStatus;
                        break;
                    case DURATION:
                        fields[i] = deviceMetrics.mDisconnectionStatus;
                        break;
                    case CONNECTING_OFFSET:
                    case CONNECTED_OFFSET:
                    case DISCONNECTION_STATUS:
                        Log.e(TAG, "Unexpected field:" + field.name());
                        return null;
                }
            }
            return fields;
        }
    }

    Map<Integer, GroupMetrics> mMetricsMap = new HashMap<>();
    LeAudioService mService;

    LeAudioMetrics(LeAudioService leAudioService) {
        mService = leAudioService;
    }

    public void addStateChangedEvent(BluetoothDevice device, int state, int status) {
        int groupId = mService.getGroupId(device);
        if (!mMetricsMap.containsKey(groupId)) {
            mMetricsMap.put(groupId, new GroupMetrics(groupId));
        }
        GroupMetrics groupMetrics = mMetricsMap.get(groupId);
        groupMetrics.addStateChangedEvent(device, state, status);

        if (groupMetrics.isClosed()) {
            // TODO(207811438): Log LeAudioMetrics in AOSP
            mMetricsMap.remove(groupId);
        }
    }
}
