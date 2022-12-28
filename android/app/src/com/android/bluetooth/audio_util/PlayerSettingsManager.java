/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.bluetooth.audio_util;

import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothAvrcpPlayerSettingsCallback;
import android.bluetooth.BluetoothDevice;
import android.util.HashMap;
import android.util.Log;
import android.util.Map;

/**
 * Manager class for player apps.
 */
public class PlayerSettingsManager {
    private static final String TAG = "PlayerSettingsManager";

    private MediaPlayerList mMediaPlayerList;
    private Map<String, PlayerSettings> mPlayerSettings;

    /**
     * Contains the player settings and callback.
     */
    public class PlayerSettings {
        BluetoothAvrcpPlayerSettings mSettings;
        BluetoothAvrcpPlayerSettingsCallback mCallback;

        public PlayerSettings(BluetoothAvrcpPlayerSettings settings,
                BluetoothAvrcpPlayerSettingsCallback mCallback) {
            mSettings = settings;
            mCallback = callback;
        }

        /**
         * Replaces the current settings by new ones.
         */
        public void updatePlayerSettings(BluetoothAvrcpPlayerSettings settings) {
            mSettings = settings;
        }

        /**
         * Retrieves the current settings.
         */
        public BluetoothAvrcpPlayerSettings getPlayerSettings() {
            return mSettings;
        }

        /**
         * Retrieves the callback associated with this object.
         */
        public BluetoothAvrcpPlayerSettingsCallback getPlayerSettingsCallback() {
            return mCallback;
        }
    }

    public PlayerSettingsManager(MediaPlayerList mediaPlayerList) {
        mMediaPlayerList = mediaPlayerList;
        mPlayerSettings = new HashMap();
    }

    private String getActivePlayerPackageName() {
        return mMediaPlayerList.getActivePlayer().getPackageName();
    }

    /**
     * Called from Player apps to register the callback and set initial parameters.
     */
    public void registerPlayerSettingsCallback(BluetoothAvrcpPlayerSettings settings,
            BluetoothAvrcpPlayerSettingsCallback callback,
            String playerPackageName) {
        mPlayerSettings.put(playerPackageName, new PlayerSettings(settings, callback));
    }

    /**
     * Called from Player apps to unregister the callback.
     */
    public void unregisterPlayerSettingsCallback(String playerPackageName) {
        mPlayerSettings.remove(playerPackageName);
    }

    /**
     * Called from Player apps to update the current settings.
     */
    public void updatePlayerSettings(BluetoothAvrcpPlayerSettings settings,
            String playerPackageName) {
        if (mPlayerSettings.containsKey(playerPackageName)) {
            Log.w(TAG, "Player not registered");
            mPlayerSettings.get(playerPackageName).updatePlayerSettings(settings);
        }
        mPlayerSettings.put(playerPackageName, new PlayerSettings(settings, callback));
        // Check if remote device update is needed (see methods below)
    }

    /**
     * Called from remote device to get the list of the active player supported settings.
     */
    void onListPlayerAttributeRequest(BluetoothDevice device) {
    }

    /**
     * Called from remote device to get the value of active player given setting.
     */
    void onListPlayerAttributeValues(byte setting, BluetoothDevice device) {
    }

    /**
     * Called from remote device to get the list of active player current settings.
     */
    void onGetPlayerAttributeValues(byte numberSettings, int[] settings,
            BluetoothDevice device) {
    }

    /**
     * Called from remote device to set the list of active player current settings.
     */
    void setPlayerAppSetting(byte numberSettings, byte[] settings, byte[] settingsValue,
            BluetoothDevice device) {
        if (!mPlayerSettings.containsKey(getActivePlayerPackageName())) {
            Log.i(TAG, "No players registered for updates");
        }
        // TODO create settings from raw values
        mPlayerSettings.get(getActivePlayerPackageName())
                .getPlayerSettingsCallback()
                .onSetPlayerSettings();
    }

    /**
     * Called from remote device to get the list of the active player supported settings as text.
     */
    void getPlayerAttributeText(byte numberSettings, byte[] settings,
            BluetoothDevice device) {
    }

    /**
     * Called from remote device to get the list of the active player settings values as text.
     */
    void getPlayerValueText(byte setting, byte numberValues, byte[] values,
            BluetoothDevice device) {
    }

    // TODO: Add callback management between remote device request and player app

    // TODO: Add opcode conversion between framework settings and AVRCP settings
}
