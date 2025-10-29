/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.bluetooth;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.IBluetoothLeBroadcastSinkCallback;
import android.bluetooth.le.ScanFilter;
import android.content.AttributionSource;

/**
 * APIs for Bluetooth LE Audio Broadcast Sink service
 *
 * @hide
 */
interface IBluetoothLeBroadcastSink {
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    void registerCallback(in AttributionSource source, in IBluetoothLeBroadcastSinkCallback callback);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    void unregisterCallback(in AttributionSource source, in IBluetoothLeBroadcastSinkCallback callback);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    void startSearchingForSources(in AttributionSource source, in List<ScanFilter> filters);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    void stopSearchingForSources(in AttributionSource source);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    void syncToBroadcast(in AttributionSource source, in BluetoothLeBroadcastMetadata metadata, in byte[] broadcastCode);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    void terminateSync(in AttributionSource source, in int broadcastId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    boolean isCapturing(in AttributionSource source, in int broadcastId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    List<BluetoothLeBroadcastMetadata> getAllSyncedBroadcasts(in AttributionSource source);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN})")
    int getMaximumNumberOfSyncs(in AttributionSource source);
}
