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

package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.bluetooth.R;

import java.util.HashSet;
import java.util.Set;

/**
 * 1.  A paired device is recognized as a companion device if its METADATA_SOFTWARE_VERSION is
 *     set to BluetoothDevice.COMPANION_TYPE_PRIMARY or BluetoothDevice.COMPANION_TYPE_SECONDARY.
 * 2.  Only can have one companion device at a time.
 * 3.  Remove bond does not remove the companion device record.
 * 4.  Reset factory reset Bluetooth removes the companion device.
 * 5.  Companion device has individual GATT connection parameters.
 */
public class CompanionGattDeviceManager {
    private static final String TAG = "BluetoothCompanionGattDeviceManager";

    private BluetoothDevice mCompanionDevice;
    private int mCompanionType;

    /**
     * Contain the parameters for a gatt connection
     */
    public static final class ConnectionParameters {
        public final int mMinInterval;
        public final int mMaxInterval;
        public final int mLatency;
        ConnectionParameters(int minInterval, int maxInterval, int latency) {
            mMinInterval = minInterval;
            mMaxInterval = maxInterval;
            mLatency = latency;
        }
    }

    static final class CompanionParameters {
        final ConnectionParameters mHigh;
        final ConnectionParameters mBalanced;
        final ConnectionParameters mLowPower;

        CompanionParameters(ConnectionParameters high, ConnectionParameters balanced,
                ConnectionParameters lowPower) {
            mHigh = high;
            mBalanced = balanced;
            mLowPower = lowPower;
        }
    }

    private static final CompanionParameters DEFAULT_PARAMS = new CompanionParameters(
            new ConnectionParameters(
                R.integer.gatt_high_priority_min_interval,
                R.integer.gatt_high_priority_max_interval,
                R.integer.gatt_high_priority_latency),
            new ConnectionParameters(
                R.integer.gatt_balanced_priority_min_interval,
                R.integer.gatt_balanced_priority_max_interval,
                R.integer.gatt_balanced_priority_latency),
            new ConnectionParameters(
                R.integer.gatt_low_power_min_interval,
                R.integer.gatt_low_power_max_interval,
                R.integer.gatt_low_power_latency));

    private static final CompanionParameters PRIMARY_PARAMS = new CompanionParameters(
            new ConnectionParameters(
                R.integer.gatt_high_priority_min_interval_primary,
                R.integer.gatt_high_priority_max_interval_primary,
                R.integer.gatt_high_priority_latency_primary),
            new ConnectionParameters(
                R.integer.gatt_balanced_priority_min_interval_primary,
                R.integer.gatt_balanced_priority_max_interval_primary,
                R.integer.gatt_balanced_priority_latency_primary),
            new ConnectionParameters(
                R.integer.gatt_low_power_min_interval_primary,
                R.integer.gatt_low_power_max_interval_primary,
                R.integer.gatt_low_power_latency_primary));

    private static final CompanionParameters SECONDARY_PARAMS = new CompanionParameters(
            new ConnectionParameters(
                R.integer.gatt_high_priority_min_interval_secondary,
                R.integer.gatt_high_priority_max_interval_secondary,
                R.integer.gatt_high_priority_latency_secondary),
            new ConnectionParameters(
                R.integer.gatt_balanced_priority_min_interval_secondary,
                R.integer.gatt_balanced_priority_max_interval_secondary,
                R.integer.gatt_balanced_priority_latency_secondary),
            new ConnectionParameters(
                R.integer.gatt_low_power_min_interval_secondary,
                R.integer.gatt_low_power_max_interval_secondary,
                R.integer.gatt_low_power_latency_secondary));

    private static final int COMPANION_TYPE_NONE      = 0;
    private static final int COMPANION_TYPE_PRIMARY   = 1;
    private static final int COMPANION_TYPE_SECONDARY = 2;

    private static final String COMPANION_INFO = "bluetooth_companion_info";
    private static final String COMPANION_DEVICE_KEY = "companion_device";
    private static final String COMPANION_TYPE_KEY = "companion_type";

    private final AdapterService mAdapterService;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Set<BluetoothDevice> mMetadataListeningDevices = new HashSet<>();

    CompanionGattDeviceManager(AdapterService service, ServiceFactory factory) {
        mAdapterService = service;

        loadCompanionInfo();
    }

    private void loadCompanionInfo() {
        synchronized (mMetadataListeningDevices) {
            String address = getCompanionPreferences().getString(COMPANION_DEVICE_KEY, "");

            try {
                mCompanionDevice = mAdapter.getRemoteDevice(address);
                mCompanionType = getCompanionPreferences().getInt(
                        COMPANION_TYPE_KEY, COMPANION_TYPE_NONE);
            } catch (IllegalArgumentException e) {
                mCompanionDevice = null;
                mCompanionType = COMPANION_TYPE_NONE;
            }
        }

        if (mCompanionDevice == null) {
            // We don't have any companion phone registered, try look from the bonded devices
            for (BluetoothDevice device : mAdapter.getBondedDevices()) {
                String valueStr =
                        new String(device.getMetadata(BluetoothDevice.METADATA_SOFTWARE_VERSION));
                if ((valueStr.equals(BluetoothDevice.COMPANION_TYPE_PRIMARY)
                        || valueStr.equals(BluetoothDevice.COMPANION_TYPE_SECONDARY))) {
                    // found the companion device, store and unregister all listeners
                    setCompanionDevice(device, valueStr);
                    break;
                }
                registerMetadataListener(device);
            }
        }
        Log.i(TAG, "Companion device is " + mCompanionDevice + ", type=" + mCompanionType);
    }

    final BluetoothAdapter.OnMetadataChangedListener mMetadataListener =
            new BluetoothAdapter.OnMetadataChangedListener() {
                @Override
                public void onMetadataChanged(BluetoothDevice device, int key, byte[] value) {
                    String valueStr = new String(value);
                    Log.d(TAG, String.format("Metadata updated in Device %s: %d = %s.", device,
                            key, value == null ? null : valueStr));
                    if (key == BluetoothDevice.METADATA_SOFTWARE_VERSION
                            && (valueStr.equals(BluetoothDevice.COMPANION_TYPE_PRIMARY)
                            || valueStr.equals(BluetoothDevice.COMPANION_TYPE_SECONDARY))) {
                        setCompanionDevice(device, valueStr);
                    }
                }
            };

    private void setCompanionDevice(BluetoothDevice companionDevice, String type) {
        synchronized (mMetadataListeningDevices) {
            Log.i(TAG, "setCompanionDevice: " + companionDevice + ", type=" + type);
            mCompanionDevice = companionDevice;
            mCompanionType = type.equals(BluetoothDevice.COMPANION_TYPE_PRIMARY)
                    ? COMPANION_TYPE_PRIMARY : COMPANION_TYPE_SECONDARY;

            // unregister all metadata listeners
            for (BluetoothDevice device : mMetadataListeningDevices) {
                try {
                    mAdapter.removeOnMetadataChangedListener(device, mMetadataListener);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "failed to unregister metadata listener for " + device + " " + e);
                }
            }

            SharedPreferences.Editor pref = getCompanionPreferences().edit();
            pref.putString(COMPANION_DEVICE_KEY, mCompanionDevice.getAddress());
            pref.putInt(COMPANION_TYPE_KEY, mCompanionType);
            pref.apply();
        }
    }

    private SharedPreferences getCompanionPreferences() {
        return mAdapterService.getSharedPreferences(COMPANION_INFO, Context.MODE_PRIVATE);
    }

    /**
     * register listener in case there is not yet a companion device connected.
     * This use the onBondStateChanged as a connection event
     */
    public void onBondStateChanged(BluetoothDevice device, int state) {
        synchronized (mMetadataListeningDevices) {
            if (mCompanionDevice != null) {
                // We already have the companion device, do not care bond state change any more.
                return;
            }
            if (state == BluetoothDevice.BOND_BONDED) {
                registerMetadataListener(device);
            }
        }
    }

    private void registerMetadataListener(BluetoothDevice device) {
        synchronized (mMetadataListeningDevices) {
            try {
                mAdapter.addOnMetadataChangedListener(
                        device, mAdapterService.getMainExecutor(), mMetadataListener);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed to unregister metadata listener for " + device + " " + e);
            }
            mMetadataListeningDevices.add(device);
        }
    }

    public BluetoothDevice getCompanionDevice() {
        return mCompanionDevice;
    }

    /**
     * Return true is the {@code address} is the address of the companion device
     */
    public boolean isCompanionDevice(String address) {
        try {
            return isCompanionDevice(mAdapter.getRemoteDevice(address));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Return true is the {@code device} is the companion device
     */
    public boolean isCompanionDevice(BluetoothDevice device) {
        if (device == null) return false;
        return device.equals(mCompanionDevice);
    }

    /**
     * Remove alls companion device and clear SharedPreferences
     */
    public void factoryReset() {
        synchronized (mMetadataListeningDevices) {
            mCompanionDevice = null;
            mCompanionType = COMPANION_TYPE_NONE;

            SharedPreferences.Editor pref = getCompanionPreferences().edit();
            pref.remove(COMPANION_DEVICE_KEY);
            pref.remove(COMPANION_TYPE_KEY);
            pref.apply();
        }
    }

    /**
     * Return the gatt connection parameters for a specific device and priority
     */
    public ConnectionParameters getGattConnParameters(String address, int priority) {
        int companionType = isCompanionDevice(address) ? mCompanionType : COMPANION_TYPE_NONE;
        switch (companionType) {
            case COMPANION_TYPE_PRIMARY:
                return getGattConnParameters(PRIMARY_PARAMS, priority);
            case COMPANION_TYPE_SECONDARY:
                return getGattConnParameters(SECONDARY_PARAMS, priority);
            default:
                return getGattConnParameters(DEFAULT_PARAMS, priority);
        }
    }

    private ConnectionParameters getGattConnParameters(CompanionParameters params, int priority) {
        switch (priority) {
            case BluetoothGatt.CONNECTION_PRIORITY_HIGH:
                return params.mHigh;
            case BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER:
                return params.mLowPower;
            default:
                return params.mBalanced;
        }
    }
}
