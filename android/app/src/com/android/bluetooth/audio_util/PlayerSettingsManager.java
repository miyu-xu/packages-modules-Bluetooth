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

import android.annotation.Nullable;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.IBluetoothAvrcpPlayerSettingsCallback;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.avrcp.AvrcpTargetService;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for player apps.
 */
public class PlayerSettingsManager {
    private static final String TAG = "PlayerSettingsManager";

    private static final int MAX_QUEUE_SIZE = 100;
    private static final Duration PLAYER_CALLBACK_TIMEOUT = Duration.ofMillis(2000);

    private MediaPlayerList mMediaPlayerList;
    private AvrcpTargetService mService;

    private Map<String, PlayerSettings> mPlayerSettings;
    private final Object mNativeLock = new Object();

    private NativeCallbackQueue mCallbackQueue = new NativeCallbackQueue();

    /**
     * Interface used by AVRCP Native Interface to call responses of
     * previous requests.
     */
    public interface PlayerSettingsNativeCallback {

        /**
         * Called when the player has answered the native request.
         *
         * @param settings active player settings or null
         */
        void onPlayerSettingsAvailable(@Nullable BluetoothAvrcpPlayerSettings settings);
    }

    /**
     * Contains the player settings and callback.
     */
    public class PlayerSettings {
        BluetoothAvrcpPlayerSettings mSettings;
        IBluetoothAvrcpPlayerSettingsCallback mCallback;

        public PlayerSettings(BluetoothAvrcpPlayerSettings settings,
                IBluetoothAvrcpPlayerSettingsCallback callback) {
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
        public IBluetoothAvrcpPlayerSettingsCallback getPlayerSettingsCallback() {
            return mCallback;
        }
    }

    /**
     * Instantiates a new PlayerSettingsManager.
     *
     * @param mediaPlayerList is used to retrieve the current active player.
     */
    public PlayerSettingsManager(MediaPlayerList mediaPlayerList, AvrcpTargetService service) {
        mService = service;
        mMediaPlayerList = mediaPlayerList;
        mMediaPlayerList.setPlayerSettingsCallback((pkgName) -> activePlayerChanged(pkgName));
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
    private IBluetoothAvrcpPlayerSettingsCallback getPlayerCallback(String packageName) {
        if (getPlayer(packageName) != null) {
            return getPlayer(packageName).getPlayerSettingsCallback();
        }
        return null;
    }

    /**
     * Called from Player apps to register the callback and set initial parameters.
     */
    public void registerPlayerSettingsCallback(BluetoothAvrcpPlayerSettings settings,
            IBluetoothAvrcpPlayerSettingsCallback callback,
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

        // Update the current state of the player, to send informations to
        // native in case of timeout or active player change.
        mPlayerSettings.get(playerPackageName).updatePlayerSettings(settings);

        synchronized (mNativeLock) {
            // If the update comes from the active player, reply to the native callbacks
            // or send the update to the remote device.
            // playerPackageName comes from AttributionSource and can't be null here.
            if (playerPackageName.equals(getActivePlayerPackageName())) {
                if (mCallbackQueue.hasCallbacks()) {
                    mCallbackQueue.consumeCallbacks(settings);
                } else {
                    // TODO: call update to native
                }
            }
        }
    }

    private void activePlayerChanged(String packageName) {
        synchronized (mNativeLock) {
            if (!mPlayerSettings.containsKey(packageName)) {
                Log.i(TAG, "New active player not registered for updates");
            }
            if (mCallbackQueue.hasCallbacks()) {
                mCallbackQueue.consumeCallbacks(getPlayerSettings(packageName));
            } else {
                Log.i(TAG, "Sending new player settings to remote");
                // TODO: call update to native
            }
        }
    }

    /**
     * Called from remote device to set the list of active player current settings.
     */
    public boolean setPlayerSettings(BluetoothAvrcpPlayerSettings settings) {
        if (settings == null) {
            Log.w(TAG, "Remote device asking to update with no parameters");
            return false;
        }
        synchronized (mNativeLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            // Updates should only go to the current active player.
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                return false;
            }
            BluetoothAvrcpPlayerSettings.Builder psBuilder =
                    new BluetoothAvrcpPlayerSettings.Builder(
                            getPlayerSettings(activePlayerPkgName));
            Map<Integer, Integer> settingsValue = settings.getPlayerSettingValues();
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
                BluetoothAvrcpPlayerSettings newSettings = psBuilder.build();
                getPlayerCallback(activePlayerPkgName).onSetPlayerSettings(newSettings);
                mPlayerSettings.get(activePlayerPkgName).updatePlayerSettings(newSettings);
                return true;
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, "Failed to build player settings: " + e);
                return false;
            }
        }
    }

    /**
     * Called from remote device to request the list of active player current settings.
     */
    public void onPlayerSettingsRequest(PlayerSettingsNativeCallback nativeCallback) {
        synchronized (mNativeLock) {
            String activePlayerPkgName = getActivePlayerPackageName();
            if (!mPlayerSettings.containsKey(activePlayerPkgName)) {
                Log.i(TAG, "Active player not registered for updates");
                nativeCallback.onPlayerSettingsAvailable(null);
                return;
            }
            mCallbackQueue.queueCallback(nativeCallback);
            try {
                getPlayerCallback(activePlayerPkgName).onRequestPlayerSettings();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to request player settings: " + e);
            }
        }
    }

    /**
     * Native callback manager.
     *
     * Stores a Queue of native callbacks to be called when remote device asks
     * for player settings informations and handles the timeout of callbacks.
     *
     * Each call to this class should be synchronized.
     */
    private class NativeCallbackQueue {

        private final ArrayDeque<PlayerSettingsNativeCallback> mQueue;

        private final Handler mHandler;

        NativeCallbackQueue() {
            mQueue = new ArrayDeque();
            mHandler = new Handler();
        }

        /**
         * Adds callback to the queue and starts timeout timer.
         *
         * If the queue is full, will dequeue the first callback, acting like a timeout.
         */
        public void queueCallback(PlayerSettingsNativeCallback callback) {
            if (callback == null) {
                return;
            }
            // If the queue is full, dequeue the first callback to fit the new one.
            if (mQueue.size() >= MAX_QUEUE_SIZE) {
                dequeueCallback();
            }
            mQueue.offer(callback);
            mHandler.postDelayed(() -> dequeueCallback(), callback,
                    PLAYER_CALLBACK_TIMEOUT.toMillis());
        }

        /**
         * Removes the head of the queue and its timeout, and sends native saved settings.
         */
        private void dequeueCallback() {
            synchronized (mNativeLock) {
                PlayerSettingsNativeCallback callback = mQueue.poll();

                // Cancel timeout for this callback
                mHandler.removeCallbacksAndMessages(callback);

                // Send saved settings for active player, or null.
                callback.onPlayerSettingsAvailable(getPlayerSettings(getActivePlayerPackageName()));
            }
        }

        /**
         * Checks if there is any callbacks available.
         */
        public boolean hasCallbacks() {
            return !mQueue.isEmpty();
        }

        /**
         * Dequeues all callbacks and send new settings to remote device.
         */
        public void consumeCallbacks(BluetoothAvrcpPlayerSettings settings) {
            mHandler.removeCallbacksAndMessages(null);
            while (!mQueue.isEmpty()) {
                mQueue.poll().onPlayerSettingsAvailable(settings);
            }
        }
    }

    // TODO: Add more logging
}
