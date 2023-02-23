/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSinkAudioPolicy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nonnull;

/**
 * The active device manager is responsible for keeping track of the
 * connected A2DP/HFP/AVRCP/HearingAid/LE audio devices and select which device is
 * active (for each profile).
 * The active device manager selects a fallback device when the currently active device
 * is disconnected, and it selects BT devices that are lastly activated one.
 *
 * Current policy (subject to change):
 * 1) If the maximum number of connected devices is one, the manager doesn't
 * do anything. Each profile is responsible for automatically selecting
 * the connected device as active. Only if the maximum number of connected
 * devices is more than one, the rules below will apply.
 * 2) The selected A2DP active device is the one used for AVRCP as well.
 * 3) The HFP active device might be different from the A2DP active device.
 * 4) The Active Device Manager always listens for ACTION_ACTIVE_DEVICE_CHANGED
 * broadcasts for each profile:
 * - BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED for A2DP
 * - BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED for HFP
 * - BluetoothHearingAid.ACTION_ACTIVE_DEVICE_CHANGED for HearingAid
 * - BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED for LE audio
 * If such broadcast is received (e.g., triggered indirectly by user
 * action on the UI), the device in the received broadcast is marked
 * as the current active device for that profile.
 * 5) If there is a HearingAid active device, then A2DP, HFP and LE audio active devices
 * must be set to null (i.e., A2DP, HFP and LE audio cannot have active devices).
 * The reason is that A2DP, HFP or LE audio cannot be used together with HearingAid.
 * 6) If there are no connected devices (e.g., during startup, or after all
 * devices have been disconnected, the active device per profile
 * (A2DP/HFP/HearingAid/LE audio) is selected as follows:
 * 6.1) The last connected HearingAid device is selected as active.
 * If there is an active A2DP, HFP or LE audio device, those must be set to null.
 * 6.2) The last connected A2DP, HFP or LE audio device is selected as active.
 * However, if there is an active HearingAid device, then the
 * A2DP, HFP, or LE audio active device is not set (must remain null).
 * 7) If the currently active device (per profile) is disconnected, the
 * Active Device Manager just marks that the profile has no active device,
 * and the lastly activated BT device that is still connected would be selected.
 * 8) If there is already an active device, and the corresponding
 * ACTION_ACTIVE_DEVICE_CHANGED broadcast is received, the device
 * contained in the broadcast is marked as active. However, if
 * the contained device is null, the corresponding profile is marked
 * as having no active device.
 * 9) If a wired audio device is connected, the audio output is switched
 * by the Audio Framework itself to that device. We detect this here,
 * and the active device for each profile (A2DP/HFP/HearingAid/LE audio) is set
 * to null to reflect the output device state change. However, if the
 * wired audio device is disconnected, we don't do anything explicit
 * and apply the default behavior instead:
 * 9.1) If the wired headset is still the selected output device (i.e. the
 * active device is set to null), the Phone itself will become the output
 * device (i.e., the active device will remain null). If music was
 * playing, it will stop.
 * 9.2) If one of the Bluetooth devices is the selected active device
 * (e.g., by the user in the UI), disconnecting the wired audio device
 * will have no impact. E.g., music will continue streaming over the
 * active Bluetooth device.
 */
class ActiveDeviceManager {
    private static final String TAG = "ActiveDeviceManager";
    private static final boolean DBG = true; // Log.isLoggable(TAG, Log.DEBUG);

    // Used for built-in audio device
    private static final int PROFILE_USE_BUILTIN_AUDIO_DEVICE = 0;

    private final AdapterService mAdapterService;
    private final ServiceFactory mFactory;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private final AudioManager mAudioManager;
    private final AudioManagerAudioDeviceCallback mAudioManagerAudioDeviceCallback;
    private final AudioManagerOnModeChangedListener mAudioManagerOnModeChangedListener;

    private final List<BluetoothDevice> mA2dpConnectedDevices = new ArrayList<>();
    private final List<BluetoothDevice> mHfpConnectedDevices = new ArrayList<>();
    private final List<BluetoothDevice> mHearingAidConnectedDevices = new ArrayList<>();
    private final List<BluetoothDevice> mLeAudioConnectedDevices = new ArrayList<>();
    private final List<BluetoothDevice> mLeHearingAidConnectedDevices = new ArrayList<>();
    private List<BluetoothDevice> mPendingLeHearingAidActiveDevice = new ArrayList<>();
    @Nonnull
    private ActiveBluetoothProfile mActiveMediaDevice = new ActiveBluetoothProfile();
    @Nonnull
    private ActiveBluetoothProfile mActiveCallDevice = new ActiveBluetoothProfile();


    // Broadcast receiver for all changes
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Received intent with null action");
                return;
            }

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                mHandler.post(() -> handleAdapterStateChanged(currentState));
                return;
            }

            final BluetoothDevice device = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            final int previousState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
            final int currentState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);

            if (action.endsWith("CONNECTION_STATE_CHANGED") && previousState == currentState) {
                return;
            }

            switch (action) {
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    if (currentState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleA2dpConnected(device));
                    } else if (previousState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleA2dpDisconnected(device));
                    }
                    break;
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    if (currentState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleHfpConnected(device));
                    } else if (previousState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleHfpDisconnected(device));
                    }
                    break;
                case BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED:
                    if (currentState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleHearingAidConnected(device));
                    } else if (previousState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleHearingAidDisconnected(device));
                    }
                    break;
                case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED:
                    if (currentState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleLeAudioConnected(device));
                    } else if (previousState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleLeAudioDisconnected(device));
                    }
                    break;
                case BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED:
                    if (currentState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleHapConnected(device));
                    } else if (previousState == BluetoothProfile.STATE_CONNECTED) {
                        mHandler.post(() -> handleHapDisconnected(device));
                    }
                    break;
                case BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED:
                    mHandler.post(() -> handleA2dpActiveDeviceChanged(device));
                    break;
                case BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED:
                    mHandler.post(() -> handleHfpActiveDeviceChanged(device));
                    break;
                case BluetoothHearingAid.ACTION_ACTIVE_DEVICE_CHANGED:
                    mHandler.post(() -> handleHearingAidActiveDeviceChanged(device));
                    break;
                case BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED:
                    mHandler.post(() -> handleLeAudioActiveDeviceChanged(device));
                    break;
                case BluetoothHapClient.ACTION_HAP_DEVICE_AVAILABLE:
                    mHandler.post(() -> handleHapActiveDeviceChanged(device));
                    break;
                default:
                    Log.e(TAG, "Received unexpected intent, action=" + action);
                    break;
            }
        }
    };

    private void handleAdapterStateChanged(int currentState) {
        if (DBG) {
            Log.d(TAG, "handleAdapterStateChanged: currentState=" + currentState);
        }
        if (currentState == BluetoothAdapter.STATE_ON) {
            resetState();
        }
    }

    private void handleA2dpConnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleA2dpConnected: " + device);
        }
        if (mA2dpConnectedDevices.contains(device)) {
            // The device is already connected
            return;
        }
        mA2dpConnectedDevices.add(device);

        if (mActiveMediaDevice.mProfile != BluetoothProfile.HEARING_AID
                && mActiveMediaDevice.mProfile != BluetoothProfile.HAP_CLIENT) {
            mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.A2DP);
            if (isMediaMode(mAudioManager.getMode())) {
                activateMediaProfile();
            }
        }
    }

    private void handleHfpConnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHfpConnected: " + device);
        }
        if (mHfpConnectedDevices.contains(device)) {
            return;      // The device is already connected
        }
        mHfpConnectedDevices.add(device);

        if (mActiveCallDevice.mProfile != BluetoothProfile.HEARING_AID
                && mActiveCallDevice.mProfile != BluetoothProfile.HAP_CLIENT) {
            mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HEADSET);
            if (!isMediaMode(mAudioManager.getMode())) {
                activateCallProfile();
            }
        }
    }

    private void handleHearingAidConnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHearingAidConnected: " + device);
        }
        if (mHearingAidConnectedDevices.contains(device)) {
            return;      // The device is already connected
        }
        mHearingAidConnectedDevices.add(device);

        if (mActiveCallDevice.mProfile == BluetoothProfile.HEARING_AID
                || mActiveMediaDevice.mProfile == BluetoothProfile.HEARING_AID) {
            final HearingAidService hearingAidService = mFactory.getHearingAidService();
            if (hearingAidService == null) {
                return;
            }
            long hiSyncId = hearingAidService.getHiSyncId(device);

            // for now, a HA device should be active for both call and media at the same time
            // checking if the new device has the same hiSync id with the previous connected HA
            // device
            if (getHearingAidActiveHiSyncId(mActiveMediaDevice.mDevice) == hiSyncId) {
                mActiveMediaDevice.mDevice.add(device);
                return;
            }
        }
        // New connected device: select it as active
        mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HEARING_AID);
        mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HEARING_AID);

        if (isMediaMode(mAudioManager.getMode())) {
            activateMediaProfile();
        } else {
            activateCallProfile();
        }
    }

    private void handleLeAudioConnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleLeAudioConnected: " + device);
        }
        if (mLeAudioConnectedDevices.contains(device)) {
            return;      // The device is already connected
        }
        mLeAudioConnectedDevices.add(device);

        boolean hearingAidIsActiveInCurrentMode = false;
        if (isMediaMode(mAudioManager.getMode())) {
            hearingAidIsActiveInCurrentMode =
                    mActiveMediaDevice.mProfile == BluetoothProfile.HEARING_AID
                            || mActiveMediaDevice.mProfile == BluetoothProfile.HAP_CLIENT;
        } else {
            hearingAidIsActiveInCurrentMode =
                    mActiveCallDevice.mProfile == BluetoothProfile.HEARING_AID
                            || mActiveCallDevice.mProfile == BluetoothProfile.HAP_CLIENT;
        }


        if ((!hearingAidIsActiveInCurrentMode && mPendingLeHearingAidActiveDevice.isEmpty())) {
            // New connected device: select it as active
            mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.LE_AUDIO);
            mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.LE_AUDIO);
            boolean isMediaMode = isMediaMode(mAudioManager.getMode());
            if (isMediaMode) {
                activateMediaProfile();
            } else {
                activateCallProfile();
            }
        } else if (mPendingLeHearingAidActiveDevice.contains(device)) {
            mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HAP_CLIENT);
            mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HAP_CLIENT);
            boolean isMediaMode = isMediaMode(mAudioManager.getMode());
            if (isMediaMode) {
                activateMediaProfile();
            } else {
                activateCallProfile();
            }
        }
    }

    private void handleHapConnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHapConnected: " + device);
        }
        if (mLeHearingAidConnectedDevices.contains(device)) {
            return;      // The device is already connected
        }
        mLeHearingAidConnectedDevices.add(device);

        boolean isMediaMode = isMediaMode(mAudioManager.getMode());

        if (!mLeAudioConnectedDevices.contains(device)) {
            mPendingLeHearingAidActiveDevice.add(device);
        } else {
            // New connected device: select it as active
            mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HAP_CLIENT);
            mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HAP_CLIENT);
            if (isMediaMode) {
                activateMediaProfile();
            } else {
                activateCallProfile();
            }

        }
    }

    private void handleA2dpDisconnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleA2dpDisconnected: " + device);
        }
        mA2dpConnectedDevices.remove(device);
        if (Objects.equals(mActiveMediaDevice.mDevice, device) && isMediaMode(
                mAudioManager.getMode())) {
            mActiveMediaDevice = new ActiveBluetoothProfile();
            activateFallbackDevice();
        }
    }

    private void handleHfpDisconnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHfpDisconnected: " + device);
        }
        mHfpConnectedDevices.remove(device);
        if (Objects.equals(mActiveCallDevice.mDevice, device) && !isMediaMode(
                mAudioManager.getMode())) {
            mActiveCallDevice = new ActiveBluetoothProfile();
            activateFallbackDevice();
        }
    }

    private void handleHearingAidDisconnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHearingAidDisconnected: " + device);
        }
        mHearingAidConnectedDevices.remove(device);
        // For now hearing aid must be used for both call/media
        if (mActiveMediaDevice.mDevice.remove(device) && mActiveCallDevice.mDevice.remove(device)
                && mActiveMediaDevice.mDevice.isEmpty() && mActiveCallDevice.mDevice.isEmpty()) {
            mActiveCallDevice = new ActiveBluetoothProfile();
            mActiveMediaDevice = new ActiveBluetoothProfile();
            activateFallbackDevice();
        }
    }

    private void handleLeAudioDisconnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleLeAudioDisconnected: " + device);
        }
        mLeAudioConnectedDevices.remove(device);
        mLeHearingAidConnectedDevices.remove(device);
        // LE audio doesn't support call/media only for now
        if (mActiveMediaDevice.mDevice.contains(device)
                && mActiveCallDevice.mDevice.contains(device)) {
            mActiveCallDevice = new ActiveBluetoothProfile();
            mActiveMediaDevice = new ActiveBluetoothProfile();
            activateFallbackDevice();
        }
    }

    private void handleHapDisconnected(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHapDisconnected: " + device);
        }
        mLeHearingAidConnectedDevices.remove(device);
        mPendingLeHearingAidActiveDevice.remove(device);
        // LE audio doesn't support call/media only for now
        if (mActiveMediaDevice.mDevice.contains(device)
                && mActiveMediaDevice.mProfile == BluetoothProfile.HAP_CLIENT
                && mActiveCallDevice.mDevice.contains(device)
                && mActiveCallDevice.mProfile == BluetoothProfile.HAP_CLIENT) {
            mActiveMediaDevice.mProfile = BluetoothProfile.LE_AUDIO;
            mActiveCallDevice.mProfile = BluetoothProfile.LE_AUDIO;
        }
    }

    private void handleA2dpActiveDeviceChanged(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleA2dpActiveDeviceChanged: " + device);
        }
        if (device != null && !mActiveMediaDevice.mDevice.contains(device)) {
            mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.A2DP);
            deactivateHearingAidDevice();
            deactivateLeAudioDevice();
        }
    }

    private void handleHfpActiveDeviceChanged(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHfpActiveDeviceChanged: " + device);
        }
        if (device != null && !mActiveCallDevice.mDevice.contains(device)) {
            mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HEADSET);
            deactivateHearingAidDevice();
            deactivateLeAudioDevice();
        }
    }

    private void handleHearingAidActiveDeviceChanged(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHearingAidActiveDeviceChanged: " + device);
        }
        // Just assign locally the new value
        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService != null) {
            long hiSyncId = hearingAidService.getHiSyncId(device);
            if (getHearingAidActiveHiSyncId(mActiveMediaDevice.mDevice) == hiSyncId) {
                mActiveCallDevice.mDevice.add(device);
                mActiveMediaDevice.mDevice.add(device);
            } else {
                List<BluetoothDevice> devices = hearingAidService.getConnectedPeerDevices(hiSyncId);
                mActiveMediaDevice = new ActiveBluetoothProfile(devices,
                        BluetoothProfile.HEARING_AID);
                mActiveCallDevice = new ActiveBluetoothProfile(devices,
                        BluetoothProfile.HEARING_AID);
            }
        }
        if (device != null) {
            deactivateA2dpDevice();
            deactivateHfpDevice();
            deactivateLeAudioDevice();
        }
    }

    private void handleLeAudioActiveDeviceChanged(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleLeAudioActiveDeviceChanged: " + device);
        }
        if (device != null && !mLeAudioConnectedDevices.contains(device)) {
            mLeAudioConnectedDevices.add(device);
        }
        // Just assign locally the new value
        // For now, LE audio only support both call and media
        if (device != null && !mActiveCallDevice.mDevice.contains(device)
                && !mActiveMediaDevice.mDevice.contains(device)) {
            deactivateA2dpDevice();
            deactivateHfpDevice();
            deactivateHearingAidDevice();
        }

        mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.LE_AUDIO);
        mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.LE_AUDIO);
    }

    private void handleHapActiveDeviceChanged(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "handleHapActiveDeviceChanged: " + device);
        }
        if (device != null && !mLeHearingAidConnectedDevices.contains(device)) {
            mLeHearingAidConnectedDevices.add(device);
        }
        // Just assign locally the new value
        // For now, LE audio only support both call and media
        if (device != null && !mActiveCallDevice.mDevice.contains(device)
                && !mActiveCallDevice.mDevice.contains(device)) {
            deactivateA2dpDevice();
            deactivateHfpDevice();
            deactivateHearingAidDevice();
        }

        mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HAP_CLIENT);
        mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HAP_CLIENT);
    }

    /** Notifications of audio device connection and disconnection events. */
    @SuppressLint("AndroidFrameworkRequiresPermission")
    private class AudioManagerAudioDeviceCallback extends AudioDeviceCallback {
        private boolean isWiredAudioHeadset(AudioDeviceInfo deviceInfo) {
            switch (deviceInfo.getType()) {
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                    return true;
                default:
                    break;
            }
            return false;
        }

        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            if (DBG) {
                Log.d(TAG, "onAudioDevicesAdded");
            }
            boolean hasAddedWiredDevice = false;
            for (AudioDeviceInfo deviceInfo : addedDevices) {
                if (DBG) {
                    Log.d(TAG, "Audio device added: " + deviceInfo.getProductName() + " type: "
                            + deviceInfo.getType());
                }
                if (isWiredAudioHeadset(deviceInfo)) {
                    hasAddedWiredDevice = true;
                    break;
                }
            }
            if (hasAddedWiredDevice) {
                wiredAudioDeviceConnected();
            }
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
        }
    }

    ActiveDeviceManager(AdapterService service, ServiceFactory factory) {
        mAdapterService = service;
        mFactory = factory;
        mAudioManager = service.getSystemService(AudioManager.class);
        mAudioManagerAudioDeviceCallback = new AudioManagerAudioDeviceCallback();
        mAudioManagerOnModeChangedListener = new AudioManagerOnModeChangedListener();
    }

    void start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }

        mHandlerThread = new HandlerThread("BluetoothActiveDeviceManager");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED);
        filter.addAction(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHearingAid.ACTION_ACTIVE_DEVICE_CHANGED);
        filter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED);
        filter.addAction(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHapClient.ACTION_HAP_DEVICE_AVAILABLE);
        mAdapterService.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);

        mAudioManager.registerAudioDeviceCallback(mAudioManagerAudioDeviceCallback, mHandler);
        mAudioManager.addOnModeChangedListener(command -> {
            if (!mHandler.post(command)) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }, mAudioManagerOnModeChangedListener);
    }

    void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }

        mAudioManager.unregisterAudioDeviceCallback(mAudioManagerAudioDeviceCallback);
        mAdapterService.unregisterReceiver(mReceiver);
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        resetState();
    }

    /**
     * Get the {@link Looper} for the handler thread. This is used in testing and helper
     * objects
     *
     * @return {@link Looper} for the handler thread
     */
    @VisibleForTesting
    public Looper getHandlerLooper() {
        if (mHandlerThread == null) {
            return null;
        }
        return mHandlerThread.getLooper();
    }

    private boolean activateA2dpDevice(@Nonnull BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "activateA2dpDevice(" + device + ")");
        }
        final A2dpService a2dpService = mFactory.getA2dpService();
        if (a2dpService == null) {
            return false;
        }

        if (!a2dpService.setActiveDevice(device)) {
            return false;
        }

        return true;
    }

    private boolean deactivateA2dpDevice() {
        if (DBG) {
            Log.d(TAG, "deactivateActiveDevice");
        }

        final A2dpService a2dpService = mFactory.getA2dpService();
        if (a2dpService == null) {
            return false;
        }

        return a2dpService.setActiveDevice(null);
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    private boolean activateHfpDevice(@Nonnull BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "activateHfpDevice(" + device + ")");
        }
        final HeadsetService headsetService = mFactory.getHeadsetService();
        if (headsetService == null) {
            return false;
        }
        BluetoothSinkAudioPolicy audioPolicy = headsetService.getHfpCallAudioPolicy(device);
        if (audioPolicy == null || audioPolicy.getActiveDevicePolicyAfterConnection()
                != BluetoothSinkAudioPolicy.POLICY_NOT_ALLOWED) {
            if (!headsetService.setActiveDevice(device)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    private boolean deactivateHfpDevice() {
        if (DBG) {
            Log.d(TAG, "deactivateHfpDevice");
        }
        final HeadsetService headsetService = mFactory.getHeadsetService();
        if (headsetService == null) {
            return false;
        }

        return headsetService.setActiveDevice(null);
    }

    private boolean activateHearingAidDevice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "activateHearingAidDevice(" + device + ")");
        }

        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService == null) {
            return false;
        }

        return hearingAidService.setActiveDevice(device);
    }

    private boolean deactivateHearingAidDevice() {
        if (DBG) {
            Log.d(TAG, "deactivateHearingAidDevice");
        }
        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService == null) {
            return false;
        }
        hearingAidService.setActiveDevice(null);
        return true;
    }

    private boolean activateLeAudioDevice(@Nonnull BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "activateLeAudioDevice(" + device + ")");
        }
        final LeAudioService leAudioService = mFactory.getLeAudioService();
        if (leAudioService == null) {
            return false;
        }
        return leAudioService.setActiveDevice(device);
    }

    private boolean deactivateLeAudioDevice() {
        if (DBG) {
            Log.d(TAG, "deactivateLeAudioDevice");
        }
        final LeAudioService leAudioService = mFactory.getLeAudioService();
        if (leAudioService == null) {
            return false;
        }

        return leAudioService.setActiveDevice(null);
    }

    private void activateFallbackDevice() {
        if (DBG) {
            Log.d(TAG, "activateFallbackDevice");
        }
        DatabaseManager dbManager = mAdapterService.getDatabase();
        if (dbManager == null) {
            return;
        }
        boolean isMediaMode = isMediaMode(mAudioManager.getMode());
        List<BluetoothDevice> connectedHearingAidDevices = new ArrayList<>();
        if (!mHearingAidConnectedDevices.isEmpty()) {
            connectedHearingAidDevices.addAll(mHearingAidConnectedDevices);
        }
        if (!mLeHearingAidConnectedDevices.isEmpty()) {
            connectedHearingAidDevices.addAll(mLeHearingAidConnectedDevices);
        }
        if (!connectedHearingAidDevices.isEmpty()) {
            BluetoothDevice device =
                    dbManager.getMostRecentlyConnectedDevicesInList(connectedHearingAidDevices);
            if (device != null) {
                if (mHearingAidConnectedDevices.contains(device)) {
                    if (DBG) {
                        Log.d(TAG, "set hearing aid device active: " + device);
                    }
                    // For now hearing aid must be used for both media/call
                    mActiveCallDevice = new ActiveBluetoothProfile(device,
                            BluetoothProfile.HEARING_AID);
                    mActiveMediaDevice = new ActiveBluetoothProfile(device,
                            BluetoothProfile.HEARING_AID);
                } else {
                    if (DBG) {
                        Log.d(TAG, "set LE hearing aid device active: " + device);
                    }
                    // For now HAP must be used for both media/call
                    mActiveCallDevice = new ActiveBluetoothProfile(device,
                            BluetoothProfile.HAP_CLIENT);
                    mActiveMediaDevice = new ActiveBluetoothProfile(device,
                            BluetoothProfile.HAP_CLIENT);
                }
                if (!isMediaMode) {
                    activateCallProfile();
                } else {
                    activateMediaProfile();
                }
                return;
            }
        }

        A2dpService a2dpService = mFactory.getA2dpService();
        BluetoothDevice a2dpFallbackDevice = null;
        if (a2dpService != null) {
            a2dpFallbackDevice = a2dpService.getFallbackDevice();
        }

        HeadsetService headsetService = mFactory.getHeadsetService();
        BluetoothDevice headsetFallbackDevice = null;
        if (headsetService != null) {
            headsetFallbackDevice = headsetService.getFallbackDevice();
        }

        List<BluetoothDevice> connectedDevices = new ArrayList<>();
        connectedDevices.addAll(mLeAudioConnectedDevices);
        if (isMediaMode) {
            connectedDevices.add(a2dpFallbackDevice);
        } else {
            connectedDevices.add(headsetFallbackDevice);
        }

        BluetoothDevice device = dbManager.getMostRecentlyConnectedDevicesInList(connectedDevices);
        if (device != null) {
            if (isMediaMode && Objects.equals(a2dpFallbackDevice, device)) {
                if (DBG) {
                    Log.d(TAG, "set A2DP device active: " + device);
                }
                mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.A2DP);

            } else if (!isMediaMode && Objects.equals(headsetFallbackDevice, device)) {
                if (DBG) {
                    Log.d(TAG, "set HFP device active: " + device);
                }
                mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.HEADSET);
            } else {
                if (DBG) {
                    Log.d(TAG, "set LE audio device active: " + device);
                }
                mActiveMediaDevice = new ActiveBluetoothProfile(device, BluetoothProfile.LE_AUDIO);
                mActiveCallDevice = new ActiveBluetoothProfile(device, BluetoothProfile.LE_AUDIO);
            }
        }

        if (!isMediaMode) {
            activateCallProfile();
        } else {
            activateMediaProfile();
        }
    }

    private void resetState() {
        mA2dpConnectedDevices.clear();

        mHfpConnectedDevices.clear();

        mHearingAidConnectedDevices.clear();

        mLeAudioConnectedDevices.clear();

        mLeHearingAidConnectedDevices.clear();
        mPendingLeHearingAidActiveDevice.clear();

        mActiveMediaDevice = new ActiveBluetoothProfile();
        mActiveCallDevice = new ActiveBluetoothProfile();
    }

    long getHearingAidActiveHiSyncId(List<BluetoothDevice> hearingAidActiveDevices) {
        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService != null && !hearingAidActiveDevices.isEmpty()) {
            return hearingAidService.getHiSyncId(hearingAidActiveDevices.iterator().next());
        }
        return BluetoothHearingAid.HI_SYNC_ID_INVALID;
    }

    /**
     * Called when a wired audio device is connected.
     * It might be called multiple times each time a wired audio device is connected.
     */
    @VisibleForTesting
    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    void wiredAudioDeviceConnected() {
        if (DBG) {
            Log.d(TAG, "wiredAudioDeviceConnected");
        }
        deactivateHfpDevice();
        deactivateLeAudioDevice();
        deactivateA2dpDevice();
        deactivateHearingAidDevice();
    }

    private boolean isMediaMode(int mode) {
        switch (mode) {
            case AudioManager.MODE_RINGTONE:
                final HeadsetService headsetService = mFactory.getHeadsetService();
                if (headsetService != null && headsetService.isInbandRingingEnabled()) {
                    return false;
                }
                return true;
            case AudioManager.MODE_IN_CALL:
            case AudioManager.MODE_IN_COMMUNICATION:
            case AudioManager.MODE_CALL_SCREENING:
            case AudioManager.MODE_CALL_REDIRECT:
            case AudioManager.MODE_COMMUNICATION_REDIRECT:
                return false;
            default:
                return true;
        }
    }

    private void activateCallProfile() {
        if (mActiveMediaDevice.mProfile == BluetoothProfile.A2DP
                && mActiveCallDevice.mDevice != mActiveMediaDevice.mDevice) {
            if (DBG) {
                Log.d(TAG, "activateCallProfile deactivate A2DP");
            }
            deactivateA2dpDevice();
        }
        if (mActiveCallDevice.mProfile == BluetoothProfile.HEADSET) {
            if (DBG) {
                Log.d(TAG, "activateCallProfile activate HFP");
            }
            activateHfpDevice(mActiveCallDevice.mDevice.get(0));
            deactivateHearingAidDevice();
            deactivateLeAudioDevice();
        } else if (mActiveCallDevice.mProfile == BluetoothProfile.LE_AUDIO
                || mActiveCallDevice.mProfile == BluetoothProfile.HAP_CLIENT) {
            if (DBG) {
                Log.d(TAG, "activateCallProfile activate LE devices");
            }
            activateLeAudioDevice(mActiveCallDevice.mDevice.get(0));
            deactivateHfpDevice();
            deactivateHearingAidDevice();
        } else if (mActiveCallDevice.mProfile == BluetoothProfile.HEARING_AID) {
            if (DBG) {
                Log.d(TAG, "activateCallProfile activate Hearing aid");
            }
            activateHearingAidDevice(mActiveCallDevice.mDevice.get(0));
            deactivateHfpDevice();
            deactivateLeAudioDevice();
        } else { // mActiveCallDevice.mProfile == PROFILE_USE_BUILTIN_AUDIO_DEVICE
            if (DBG) {
                Log.d(TAG, "activateCallProfile use builtin profile");
            }
            deactivateHfpDevice();
            deactivateLeAudioDevice();
            deactivateHearingAidDevice();
        }
    }

    private void activateMediaProfile() {
        if (mActiveCallDevice.mProfile == BluetoothProfile.HEADSET
                && mActiveCallDevice.mDevice != mActiveMediaDevice.mDevice) {
            if (DBG) {
                Log.d(TAG, "activateMediaProfile deactivate Hfp");
            }
            deactivateHfpDevice();
        }
        if (mActiveMediaDevice.mProfile == BluetoothProfile.A2DP) {
            if (DBG) {
                Log.d(TAG, "activateMediaProfile activate A2DP");
            }
            activateA2dpDevice(mActiveMediaDevice.mDevice.get(0));
            deactivateHearingAidDevice();
            deactivateLeAudioDevice();
        } else if (mActiveMediaDevice.mProfile == BluetoothProfile.LE_AUDIO
                || mActiveMediaDevice.mProfile == BluetoothProfile.HAP_CLIENT) {
            if (DBG) {
                Log.d(TAG, "activateMediaProfile activate LE devices");
            }
            activateLeAudioDevice(mActiveMediaDevice.mDevice.get(0));
            deactivateA2dpDevice();
            deactivateHearingAidDevice();
        } else if (mActiveMediaDevice.mProfile == BluetoothProfile.HEARING_AID) {
            if (DBG) {
                Log.d(TAG, "activateMediaProfile activate hearing aid");
            }
            activateHearingAidDevice(mActiveMediaDevice.mDevice.get(0));
            deactivateA2dpDevice();
            deactivateLeAudioDevice();
        } else { // mActiveCallDevice.mProfile == PROFILE_USE_BUILTIN_AUDIO_DEVICE
            if (DBG) {
                Log.d(TAG, "activateMediaProfile use builtin device");
            }
            deactivateA2dpDevice();
            deactivateLeAudioDevice();
            deactivateHearingAidDevice();
        }
    }

    private class AudioManagerOnModeChangedListener implements AudioManager.OnModeChangedListener {
        public void onModeChanged(int mode) {
            if (DBG) {
                Log.d(TAG, "onModeChanged: mode=" + mode);
            }
            if (isMediaMode(mode)) {
                activateMediaProfile();
            } else {
                activateCallProfile();
            }
        }
    }

    private class ActiveBluetoothProfile {
        @Nonnull
        List<BluetoothDevice> mDevice;
        int mProfile;

        ActiveBluetoothProfile(List<BluetoothDevice> devices, int profile) {
            mDevice = devices;
            mProfile = profile;
        }

        ActiveBluetoothProfile(BluetoothDevice device, int profile) {
            this(List.of(device), profile);
        }

        ActiveBluetoothProfile() {
            this(List.of(), PROFILE_USE_BUILTIN_AUDIO_DEVICE);
        }
    }
}
