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

package com.android.server.bluetooth;

import static android.os.PowerExemptionManager.REASON_BLUETOOTH_BROADCAST;
import static android.os.PowerExemptionManager.TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_ALLOWED;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

final class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();
    private static final boolean DBG = true;//Log.isLoggable(TAG, Log.DEBUG);

    // private static final int SERVICE_IBLUETOOTH = 1;
    // private static final int SERVICE_IBLUETOOTHGATT = 2;

    private ServiceUtils() {}

    static boolean doBind(Context ctx, Intent intent, ServiceConnection conn, int flags, UserHandle user) {
        ComponentName comp = resolveSystemService(intent, ctx.getPackageManager(), 0);
        if (comp == null) {
            Log.e(TAG, "Fail to resolve component: " + intent);
            return false;
        }
        intent.setComponent(comp);
        if (!ctx.bindServiceAsUser(intent, conn, flags, user)) {
            ctx.unbindService(conn);
            Log.e(TAG, "Fail to bind to: " + intent);
            return false;
        }
        return true;
    }

    private static ComponentName resolveSystemService(Intent intent, PackageManager pm, int flags) {
        List<ResolveInfo> results = pm.queryIntentServices(intent, flags);
        // TODO results can not be null, use a for each iterator
        if (results == null) {
            return null;
        }
        ComponentName comp = null;
        for (int i = 0; i < results.size(); i++) {
            ResolveInfo ri = results.get(i);
            if ((ri.serviceInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                continue;
            }
            ComponentName foundComp = new ComponentName(ri.serviceInfo.applicationInfo.packageName,
                    ri.serviceInfo.name);
            if (comp != null) {
                throw new IllegalStateException("Multiple system services handle " + intent
                        + ": " + comp + ", " + foundComp);
            }
            comp = foundComp;
        }
        return comp;
    }

    static @NonNull Bundle getTempAllowlistBroadcastOptions() {
        final long duration = 10_000;
        final BroadcastOptions bOptions = BroadcastOptions.makeBasic();
        bOptions.setTemporaryAppAllowlist(duration,
                TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_ALLOWED, REASON_BLUETOOTH_BROADCAST,
                "");
        return bOptions.toBundle();
    }

    /**
     *  Save the Bluetooth on/off state
     */
    static void persistBluetoothSetting(Context ctx, int value) {
        logd("Persisting Bluetooth Setting: " + value);
        // waive WRITE_SECURE_SETTINGS permission check
        final long callingIdentity = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(ctx.getContentResolver(), Settings.Global.BLUETOOTH_ON, value);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    // static class BluetoothServiceConnection implements ServiceConnection {
    //     // static final Map<String, Integer> MY_MAP = Map.of(
    //     //     "com.android.bluetooth.btservice.AdapterService", SERVICE_IBLUETOOTH,
    //     //     "com.android.bluetooth.gatt.GattService", SERVICE_IBLUETOOTHGATT
    //     // );
    //     final Handler mHandler;
    //     final int mArg;
    //     BluetoothServiceConnection(Looper looper, int arg) {
    //         mHandler = new Handler(requireNonNull(looper));
    //         mArg = arg;
    //     }

    //     public void onServiceConnected(ComponentName componentName, IBinder service) {
    //         final String name = componentName.getClassName();
    //         logd("BluetoothServiceConnection.onServiceConnected: " + name);
    //         Message msg = mHandler.obtainMessage(MESSAGE_BLUETOOTH_SERVICE_CONNECTED);
    //         msg.arg1 = mArg;
    //         // msg.arg1 = MY_MAP.get(name);
    //         // if (msg.arg1 == null) {
    //         //     Log.e(TAG, "Unknown service connected: " + name);
    //         //     return;
    //         // }
    //         // if (name.equals("com.android.bluetooth.btservice.AdapterService")) {
    //         //     msg.arg1 = SERVICE_IBLUETOOTH;
    //         // } else if (name.equals("com.android.bluetooth.gatt.GattService")) {
    //         //     msg.arg1 = SERVICE_IBLUETOOTHGATT;
    //         // } else {
    //         //     Log.e(TAG, "Unknown service connected: " + name);
    //         //     return;
    //         // }
    //         msg.obj = service;
    //         mHandler.sendMessage(msg);
    //     }

    //     public void onServiceDisconnected(ComponentName componentName) {
    //         // Called if we unexpectedly disconnect.
    //         final String name = componentName.getClassName();
    //         logd("BluetoothServiceConnection.onServiceDisconnected: " + name);
    //         Message msg = mHandler.obtainMessage(MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED);
    //         msg.arg1 = mArg;
    //         // if (name.equals("com.android.bluetooth.btservice.AdapterService")) {
    //         //     msg.arg1 = SERVICE_IBLUETOOTH;
    //         // } else if (name.equals("com.android.bluetooth.gatt.GattService")) {
    //         //     msg.arg1 = SERVICE_IBLUETOOTHGATT;
    //         // } else {
    //         //     Log.e(TAG, "Unknown service disconnected: " + name);
    //         //     return;
    //         // }
    //         mHandler.sendMessage(msg);
    //     }
    // }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    static void enforceBluetoothPrivilegedPermission(Context context) {
        context.enforceCallingOrSelfPermission(
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
                "Need BLUETOOTH PRIVILEGED permission");
    }

    static boolean isDeviceProvisioned(ContentResolver contentResolver) {
        return Settings.Global.getInt(contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) != 0;
    }

    private static void logd(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}
