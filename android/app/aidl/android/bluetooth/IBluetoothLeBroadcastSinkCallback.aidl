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

/**
 * Callback definitions for interacting with LE Audio Broadcast Sink service
 *
 * @hide
 */
interface IBluetoothLeBroadcastSinkCallback {
    void onBroadcastFound(in BluetoothLeBroadcastMetadata source);
    void onSearchStarted(in int reason);
    void onSearchStartFailed(in int reason);
    void onSearchStopped(in int reason);
    void onSearchStopFailed(in int reason);
    void onSyncStarted(in int reason, in int broadcastId);
    void onSyncStartFailed(in int reason);
    void onSyncStopped(in int reason, in int broadcastId);
    void onSyncStopFailed(in int reason);
    void onCaptureStarted(in int reason, in int broadcastId);
    void onCaptureStopped(in int reason, in int broadcastId);
    void onBroadcastMetadataChanged(in int broadcastId, in BluetoothLeBroadcastMetadata metadata);
}
