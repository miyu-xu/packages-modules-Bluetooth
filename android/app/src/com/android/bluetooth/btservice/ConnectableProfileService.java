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

import static com.android.bluetooth.Utils.enforceBluetoothPrivilegedPermission;

import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.bluetooth.btservice.storage.DatabaseManager;

import java.util.Objects;

/**
 * Base class for a background service that runs a Bluetooth profile
 */
public abstract class ConnectableProfileService extends ProfileService {
    private static final boolean DBG = false;

    private final int mBluetoothProfile;
    private final String mTAG;

    private AdapterService mAdapterService;
    private DatabaseManager mDatabaseManager;

    protected ConnectableProfileService(int profile, String tag) {
        mBluetoothProfile = profile;
        mTAG = tag;
    }

    @Override
    protected boolean start() {
        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                "AdapterService cannot be null when "
                + BluetoothProfile.getProfileName(mBluetoothProfile) + " service starts");
        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),
                "DatabaseManager cannot be null when "
                + BluetoothProfile.getProfileName(mBluetoothProfile) + " service starts");
        return true;
    }

    /**
     * Set connection policy of the profile and connects it if connectionPolicy is
     * {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED} or disconnects if connectionPolicy is
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}
     *
     * <p> The device should already be paired.
     * Connection policy can be one of:
     * {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED},
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN},
     * {@link BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     */
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy) {
        enforceBluetoothPrivilegedPermission(this);
        if (DBG) {
            Log.d(mTAG, "Saved connectionPolicy " + device.getAnonymizedAddress()
                    + " = " + connectionPolicy);
        }

        if (!mDatabaseManager.setProfileConnectionPolicy(device, mBluetoothProfile,
                  connectionPolicy)) {
            return false;
        }
        if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return true;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED},
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN},
     * {@link BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     */
    public int getConnectionPolicy(BluetoothDevice device) {
        enforceBluetoothPrivilegedPermission(this);
        return mDatabaseManager.getProfileConnectionPolicy(device, mBluetoothProfile);
    }

    /**
     * Verifies whether the profile is supported by the local bluetooth adapter by checking a
     * bitmask of its supported profiles
     *
     * @param remoteDeviceUuids is an array of all supported profiles by the remote device
     * @param localDeviceUuids  is an array of all supported profiles by the local device
     * @param device            is the remote device we wish to connect to
     * @return true if the profile is supported by both the local and remote device, false otherwise
     * @hide
     */
    public abstract boolean isSupported(ParcelUuid[] localDeviceUuids,
            ParcelUuid[] remoteDeviceUuids, BluetoothDevice device);

    /**
     * @hide
     */
    public abstract boolean connect(BluetoothDevice device);
    /**
     * @hide
     */
    public abstract boolean disconnect(BluetoothDevice device);
    /**
     * @hide
     */
    public abstract int getConnectionState(BluetoothDevice device);

    @Override
    public String toString() {
        return BluetoothProfile.getProfileName(mBluetoothProfile);
    }
}
