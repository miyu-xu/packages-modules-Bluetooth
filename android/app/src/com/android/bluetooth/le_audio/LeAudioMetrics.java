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
    public static int STATUS_UNKNOWN = 0;
    public static int STATUS_SUCCESS = 1;
    public static int STATUS_FAILED = 2;

    private static class DeviceMetrics {
        BluetoothDevice mBluetoothDevice;
        long mConnectingTimestampNanos = -1L;
        long mConnectedTimeStampNanos = -1L;
        long mDisconnectedTimestampNanos = -1L;

        DeviceMetrics(BluetoothDevice device) {
            this.mBluetoothDevice = device;
        }

        void setConnectingTimestampNanos(long timestampNanos) {
            mConnectingTimestampNanos = timestampNanos;
        }

        void setConnectedTimeStampNanos(long timestampNanos) {
            mConnectedTimeStampNanos = timestampNanos;
        }

        void setDisconnectedTimestampNanos(long timestampNanos) {
            mDisconnectedTimestampNanos = timestampNanos;
        }
    }

    private static class GroupMetrics {
        int mGroupId;
        List<DeviceMetrics> mDeviceMetrics = new ArrayList<>();
        Map<BluetoothDevice, DeviceMetrics> mOpenedDevices = new HashMap<>();

        GroupMetrics(int groupId) {
            mGroupId = groupId;
        }

        void addConnectingEvent(BluetoothDevice device) {
            if (!mOpenedDevices.containsKey(device)) {
                DeviceMetrics deviceMetrics = new DeviceMetrics(device);
                mOpenedDevices.put(device, deviceMetrics);
                mDeviceMetrics.add(deviceMetrics);
            }
            DeviceMetrics deviceMetrics = mOpenedDevices.get(device);
            deviceMetrics.setConnectingTimestampNanos(System.nanoTime());
        }

        void addConnectedEvent(BluetoothDevice device) {
            if (!mOpenedDevices.containsKey(device)) {
                DeviceMetrics deviceMetrics = new DeviceMetrics(device);
                mOpenedDevices.put(device, deviceMetrics);
                mDeviceMetrics.add(deviceMetrics);
            }
            DeviceMetrics deviceMetrics = mOpenedDevices.get(device);
            deviceMetrics.setConnectedTimeStampNanos(System.nanoTime());
        }

        void addDisconnectedEvent(BluetoothDevice device) {
            if (!mOpenedDevices.containsKey(device)) {
                DeviceMetrics deviceMetrics = new DeviceMetrics(device);
                mOpenedDevices.put(device, deviceMetrics);
                mDeviceMetrics.add(deviceMetrics);
            }
            DeviceMetrics deviceMetrics = mOpenedDevices.get(device);
            deviceMetrics.setDisconnectedTimestampNanos(System.nanoTime());
        }

        boolean isClosed() {
            return mOpenedDevices.isEmpty();
        }

        long[] buildConnectingTimestampArray() {
            long[] connectingTimestamps = new long[mDeviceMetrics.size()];
            for (int i = 0; i < mDeviceMetrics.size(); i++) {
                connectingTimestamps[i] = mDeviceMetrics.get(i).mConnectingTimestampNanos;
            }
            return connectingTimestamps;
        }

        long[] buildConnectedTimestampArray() {
            long[] connectedTimestamps = new long[mDeviceMetrics.size()];
            for (int i = 0; i < mDeviceMetrics.size(); i++) {
                connectedTimestamps[i] = mDeviceMetrics.get(i).mConnectedTimeStampNanos;
            }
            return connectedTimestamps;
        }

        long[] buildDisconnectedTimestampArray() {
            long[] disconnectedTimestamps = new long[mDeviceMetrics.size()];
            for (int i = 0; i < mDeviceMetrics.size(); i++) {
                disconnectedTimestamps[i] = mDeviceMetrics.get(i).mDisconnectedTimestampNanos;
            }
            return disconnectedTimestamps;
        }
    }

    Map<Integer, GroupMetrics> mMetricsMap = new HashMap<>();
    LeAudioService mService;

    LeAudioMetrics(LeAudioService leAudioService) {
        mService = leAudioService;
    }

    public void addConnectingEvent(BluetoothDevice device) {
        int groupId = mService.getGroupId(device);
        if (!mMetricsMap.containsKey(groupId)) {
            mMetricsMap.put(groupId, new GroupMetrics(groupId));
        }
        GroupMetrics groupMetrics = mMetricsMap.get(groupId);
        groupMetrics.addConnectingEvent(device);
    }

    public void addConnectedEvent(BluetoothDevice device) {
        int groupId = mService.getGroupId(device);
        if (!mMetricsMap.containsKey(groupId)) {
            mMetricsMap.put(groupId, new GroupMetrics(groupId));
        }
        GroupMetrics groupMetrics = mMetricsMap.get(groupId);
        groupMetrics.addConnectedEvent(device);
    }

    public void addDisconnectedEvent(BluetoothDevice device) {
        int groupId = mService.getGroupId(device);
        if (!mMetricsMap.containsKey(groupId)) {
            mMetricsMap.put(groupId, new GroupMetrics(groupId));
        }
        GroupMetrics groupMetrics = mMetricsMap.get(groupId);
        groupMetrics.addDisconnectedEvent(device);

        if (groupMetrics.isClosed()) {
            mMetricsMap.remove(groupId);
        }
    }
}
