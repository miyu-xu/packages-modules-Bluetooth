/*
 * Copyright (C) 2023 The Android Open Source Project
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

/**
 * This abstract class is used to implement {@link BluetoothAvrcpPlayerSettings} callbacks.
 */
public interface BluetoothAvrcpPlayerSettingsCallback {

    /**
     * Callback triggered as result of {@link BluetoothA2dp#registerPlayerSettingsCallback}.
     *
     * Indicates that the callback is now registered for updates from the AVRCP device.
     */
    void onPlayerSettingsRegistered() {};

    /**
     * Callback triggered when the device requests a change in Player settings.
     *
     * See {@link BluetoothAvrcpPlayerSettings}.
     *
     * @param settings the settings requested by the Bluetooth device
     */
    void onSetPlayerSettings(
            @NonNull BluetoothAvrcpPlayerSettings settings) {};

    /**
     * Callback triggered when the device requests the current Player settings.
     *
     * Player apps should call {@link BluetoothA2dp#updatePlayerSettings} to send their current
     * settings to the bluetooth device.
     */
    void onRequestPlayerSettings() {};
}
