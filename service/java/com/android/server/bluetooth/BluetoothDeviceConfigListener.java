/*
 * Copyright 2020 The Android Open Source Project
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

import android.provider.DeviceConfig;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The BluetoothDeviceConfigListener handles system device config change callback and checks
 * whether we need to inform BluetoothManagerService on this change.
 *
 * The information of device config change would not be passed to the BluetoothManagerService
 * when Bluetooth is on and Bluetooth is in one of the following situations:
 *   1. Bluetooth A2DP is connected.
 *   2. Bluetooth Hearing Aid profile is connected.
 */
public class BluetoothDeviceConfigListener {
    private static final String TAG = "BluetoothDeviceConfigListener";

    private final BluetoothManagerService mService;
    private final boolean mLogDebug;
    private final HashMap<String, String> mCurrFlags;

    BluetoothDeviceConfigListener(BluetoothManagerService service, boolean logDebug) {
        mService = service;
        mLogDebug = logDebug;
        mCurrFlags = getFlags();
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_BLUETOOTH,
                (Runnable r) -> r.run(), mDeviceConfigChangedListener);
    }

    private final DeviceConfig.OnPropertiesChangedListener mDeviceConfigChangedListener =
            new DeviceConfig.OnPropertiesChangedListener() {
                @Override
                public void onPropertiesChanged(DeviceConfig.Properties newProperties) {
                    if (!newProperties.getNamespace().equals(DeviceConfig.NAMESPACE_BLUETOOTH)) {
                        return;
                    }
                    if (mLogDebug) {
                        ArrayList<String> flags = new ArrayList<>();
                        for (String name : newProperties.getKeyset()) {
                            flags.add(name + "='" + newProperties.getString(name, "") + "'");
                        }
                        Log.d(TAG, "onPropertiesChanged: " + String.join(",", flags));
                    }
                    boolean foundChangedInit = false;
                    for (String name : newProperties.getKeyset()) {
                        var oldValue = mCurrFlags.get(name);
                        var newValue = newProperties.getString(name, "");
                        if (!isInitFlag(name) || oldValue.equals(newValue)) {
                            continue;
                        }
                        Log.d(TAG,
                                "Property " + name + " changed from " + oldValue + " -> "
                                        + newValue);
                        mCurrFlags.put(name, newValue);
                        foundChangedInit = true;
                    }
                    if (!foundChangedInit) {
                        Log.d(TAG, "All properties unchanged, skipping restart");
                        return;
                    }
                    Log.d(TAG, "Properties changed, restarting");
                    mService.onInitFlagsChanged();
                }
            };

    private HashMap<String, String> getFlags() {
        var properties = DeviceConfig.getProperties(DeviceConfig.NAMESPACE_BLUETOOTH);
        var out = new HashMap();
        for (var name : properties.getKeyset()) {
            if (isInitFlag(name)) {
                out.put(name, properties.getString(name, ""));
            }
        }
        return out;
    }

    private Boolean isInitFlag(String flagName) {
        return flagName.startsWith("INIT_");
    }
}
