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
    private final Object mLock = new Object();

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
     * If the current active player is not registered for updates, will return null.
     */
    private PlayerSettings getPlayer(String packageName) {
        return mPlayerSettings.get(packageName);
    }

    /**
     * If the current active player is not registered for updates, will return null.
     */
    private BluetoothAvrcpPlayerSettings getPlayerSettings(String packageName) {
        if (getPlayer(packageName) != null) {
            return getPlayer(packageName).getPlayerSettings();
        }
        return null;
    }

    /**
     * If the current active player is not registered for updates, will return null.
     */
    private BluetoothAvrcpPlayerSettingsCallback getPlayerCallback(String packageName) {
        if (getPlayer(packageName) != null) {
            return getPlayer(packageName).getPlayerSettingsCallback();
        }
        return null;
    }

    /**
     * Called from Player apps to register the callback and set initial parameters.
     */
    public void registerPlayerSettingsCallback(BluetoothAvrcpPlayerSettings settings,
            BluetoothAvrcpPlayerSettingsCallback callback,
            String playerPackageName) {
        if (settings == null || callback == null) {
            throw new IllegalStateException("Player settings and callback should not be null");
        }
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
        if (!mPlayerSettings.containsKey(playerPackageName)) {
            Log.w(TAG, "Player is not registered for updates");
            return;
        }
        mPlayerSettings.get(playerPackageName).updatePlayerSettings(settings);
        //TODO Callbacks
        // Check if remote device update is needed (see methods below)
        // Do we have one method to send the remote device the update or multiple?
        // If we have only one maybe we don't need to register that the remote asked and simply send
        // when received info by the active player?
        // mAvrcpService.sendPlayerSettings(settings)
    }

    /**
     * Called from remote device to get the list of the active player supported settings.
     */
    void onListPlayerAttributeRequest(BluetoothDevice device) {
        synchronized (mLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                //TODO Callbacks
                // mAvrcpService.sendPlayerSettings(null);
                return;
            }
            getPlayerCallback(activePlayerPkgName).onRequestPlayerSettings();
        }
    }

    /**
     * Called from remote device to get the possible values of active player given setting.
     */
    void onListPlayerAttributeValues(int setting, BluetoothDevice device) {
        synchronized (mLock) {
            if (!mPlayerSettings.containsKey(getActivePlayerPackageName())) {
                Log.i(TAG, "Active player not registered for updates");
                //TODO Callbacks
                return;
            }
            if (!BluetoothAvrcpPlayerSettings.isValidPlayerSetting(setting)) {
                Log.w(TAG, "Remote device requesting unsupported setting");
                //TODO Callbacks
                return;
            }
            //TODO Callbacks BluetoothAvrcpPlayerSettings.getSettingPossibleValues(setting)
        }
    }

    /**
     * Called from remote device to get the list of active player current settings.
     */
    void onGetPlayerAttributeValues(List<Integer> settings, BluetoothDevice device) {
        synchronized (mLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                //TODO Callbacks
                // mAvrcpService.sendPlayerSettings(null);
                return;
            }
            getPlayerCallback(activePlayerPkgName).onRequestPlayerSettings();
            //TODO Callbacks -> use the settings list when calling remote
        }
    }

    /**
     * Called from remote device to set the list of active player current settings.
     */
    void setPlayerAppSetting(Map<Integer, Integer> settingsValue, BluetoothDevice device) {
        if (settingsValue == null || settingsValue.isEmpty()) {
            Log.w(TAG, "Remote device asking to update with no parameters");
            return;
        }
        synchronized (mLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            // Updates should only go to the current active player.
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                return;
            }
            BluetoothAvrcpPlayerSettings.Builder psBuilder =
                    new BluetoothAvrcpPlayerSettings.Builder(
                            getPlayerSettings(activePlayerPkgName));
            for (Map.Entry<Integer, Integer> entry : settingsValue.entrySet()) {
                if (getPlayerSettings(activePlayerPkgName).isPlayerSettingSet(entry.getKey())) {
                    try {
                        psBuilder.addPlayerSettingValue(entry.getKey(), entry.getValue());
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Player setting not supported");
                    }
                }
            }
            try {
                getPlayerCallback(activePlayerPkgName).onSetPlayerSettings(psBuilder.build());
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to build player settings: " + e);
            }
        }
    }

    /**
     * Called from remote device to get the list of the active player supported settings as text.
     */
    void getPlayerAttributeText(List<Integer> settings, BluetoothDevice device) {
        synchronized (mLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                //TODO Callbacks
                // mAvrcpService.sendPlayerSettings(null);
                return;
            }
            getPlayerCallback(activePlayerPkgName).onRequestPlayerSettings();
            //TODO Callbacks -> use the settings list when answering remote
        }
    }

    /**
     * Called from remote device to get the list of the active player settings supported values
     * as text.
     */
    void getPlayerValueText(int setting, List<Integer> values, BluetoothDevice device) {
        synchronized (mLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                //TODO Callbacks
                // mAvrcpService.sendPlayerSettings(null);
                return;
            }
            getPlayerCallback(activePlayerPkgName).onRequestPlayerSettings();
            //TODO Callbacks -> use the setting and list values when answering remote
        }
    }

    // TODO: Add callback management between remote device request and player app

    // TODO: Add active player state listener to update remote device
}
