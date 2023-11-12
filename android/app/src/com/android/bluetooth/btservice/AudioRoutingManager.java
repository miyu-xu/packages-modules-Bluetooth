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

package com.android.bluetooth.btservice;

import static android.bluetooth.IBluetoothLeAudio.LE_AUDIO_GROUP_ID_INVALID;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSinkAudioPolicy;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.flags.FeatureFlags;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AudioRoutingManager extends ActiveDeviceManager {
    private static final String TAG = AudioRoutingManager.class.getSimpleName();
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);
    @VisibleForTesting static final int A2DP_HFP_SYNC_CONNECTION_TIMEOUT_MS = 5_000;

    private final AdapterService mAdapterService;
    private DatabaseManager mDbManager;
    private final ServiceFactory mFactory;
    private HandlerThread mHandlerThread = null;
    private AudioRoutingHandler mHandler = null;
    private final AudioManager mAudioManager;
    private final AudioManagerAudioDeviceCallback mAudioManagerAudioDeviceCallback;

    private final Object mLock = new Object();
    // TODO: remove mA2dpConnectedDevices
    @GuardedBy("mLock")
    private final List<BluetoothDevice> mA2dpConnectedDevices = new ArrayList<>();
    // TODO: remove mHfpConnectedDevices
    @GuardedBy("mLock")
    private final List<BluetoothDevice> mHfpConnectedDevices = new ArrayList<>();

    @GuardedBy("mLock")
    private final List<BluetoothDevice> mHearingAidConnectedDevices = new ArrayList<>();

    @GuardedBy("mLock")
    private final List<BluetoothDevice> mLeAudioConnectedDevices = new ArrayList<>();

    @GuardedBy("mLock")
    private BluetoothDevice mA2dpActiveDevice = null;

    @GuardedBy("mLock")
    private BluetoothDevice mHfpActiveDevice = null;

    @GuardedBy("mLock")
    private final Set<BluetoothDevice> mHearingAidActiveDevices = new ArraySet<>();

    @GuardedBy("mLock")
    private BluetoothDevice mLeAudioActiveDevice = null;

    @GuardedBy("mLock")
    private BluetoothDevice mPendingActiveDevice = null;

    private BluetoothDevice mClassicDeviceToBeActivated = null;
    private BluetoothDevice mClassicDeviceNotToBeActivated = null;

    @Override
    public void onBluetoothStateChange(int prevState, int newState) {
        mHandler.post(() -> handleAdapterStateChanged(newState));
    }

    /**
     * Called when audio profile connection state changed
     *
     * @param profile The Bluetooth profile of which connection state changed
     * @param device The device of which connection state was changed
     * @param fromState The previous connection state of the device
     * @param toState The new connection state of the device
     */
    @Override
    public void profileConnectionStateChanged(
            int profile, BluetoothDevice device, int fromState, int toState) {
        if (toState == BluetoothProfile.STATE_CONNECTED) {
            mHandler.post(() -> mHandler.handleProfileConnected(profile, device));
        } else if (fromState == BluetoothProfile.STATE_CONNECTED) {
            mHandler.post(() -> mHandler.handleProfileDisconnected(profile, device));
        }
    }

    /**
     * Called when active state of audio profiles changed
     *
     * @param profile The Bluetooth profile of which active state changed
     * @param device The device currently activated. {@code null} if no device is active
     */
    @Override
    public void profileActiveDeviceChanged(int profile, BluetoothDevice device) {
        mHandler.post(() -> mHandler.handleProfileActiveDeviceChanged(profile, device));
    }

    private void handleAdapterStateChanged(int currentState) {
        if (DBG) {
            Log.d(TAG, "handleAdapterStateChanged: currentState=" + currentState);
        }
        if (currentState == BluetoothAdapter.STATE_ON) {
            resetState();
        }
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
                    Log.d(
                            TAG,
                            "Audio device added: "
                                    + deviceInfo.getProductName()
                                    + " type: "
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
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {}
    }

    AudioRoutingManager(AdapterService service, ServiceFactory factory, FeatureFlags featureFlags) {
        super(service, factory, featureFlags);
        mAdapterService = service;
        mDbManager = mAdapterService.getDatabase();
        mFactory = factory;
        mAudioManager = service.getSystemService(AudioManager.class);
        mAudioManagerAudioDeviceCallback = new AudioManagerAudioDeviceCallback();
    }

    @Override
    void start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }

        mHandlerThread = new HandlerThread("BluetoothActiveDeviceManager");
        BluetoothMethodProxy mp = BluetoothMethodProxy.getInstance();
        mp.threadStart(mHandlerThread);
        mHandler = new AudioRoutingHandler(mp.handlerThreadGetLooper(mHandlerThread));

        mAudioManager.registerAudioDeviceCallback(mAudioManagerAudioDeviceCallback, mHandler);
        mAdapterService.registerBluetoothStateCallback((command) -> mHandler.post(command), this);
    }

    @Override
    void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }

        mAudioManager.unregisterAudioDeviceCallback(mAudioManagerAudioDeviceCallback);
        mAdapterService.unregisterBluetoothStateCallback(this);
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        resetState();
    }

    /**
     * Get the {@link Looper} for the handler thread. This is used in testing and helper objects
     *
     * @return {@link Looper} for the handler thread
     */
    @VisibleForTesting
    @Override
    public Looper getHandlerLooper() {
        if (mHandler == null) {
            return null;
        }
        return mHandler.getLooper();
    }

    private boolean setA2dpActiveDevice(@NonNull BluetoothDevice device) {
        return setA2dpActiveDevice(device, false);
    }

    private boolean setA2dpActiveDevice(
            @Nullable BluetoothDevice device, boolean hasFallbackDevice) {
        if (DBG) {
            Log.d(
                    TAG,
                    "setA2dpActiveDevice("
                            + device
                            + ")"
                            + (device == null ? " hasFallbackDevice=" + hasFallbackDevice : ""));
        }
        synchronized (mLock) {
            if (mPendingActiveDevice != null) {
                mHandler.removeCallbacksAndMessages(mPendingActiveDevice);
                mPendingActiveDevice = null;
            }
        }

        final A2dpService a2dpService = mFactory.getA2dpService();
        if (a2dpService == null) {
            return false;
        }

        boolean success = false;
        if (device == null) {
            success = a2dpService.removeActiveDevice(!hasFallbackDevice);
        } else {
            success = a2dpService.setActiveDevice(device);
        }

        if (!success) {
            return false;
        }

        synchronized (mLock) {
            mA2dpActiveDevice = device;
        }
        return true;
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    private boolean setHfpActiveDevice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "setHfpActiveDevice(" + device + ")");
        }
        synchronized (mLock) {
            if (mPendingActiveDevice != null) {
                mHandler.removeCallbacksAndMessages(mPendingActiveDevice);
                mPendingActiveDevice = null;
            }
        }
        final HeadsetService headsetService = mFactory.getHeadsetService();
        if (headsetService == null) {
            return false;
        }
        BluetoothSinkAudioPolicy audioPolicy = headsetService.getHfpCallAudioPolicy(device);
        if (audioPolicy != null
                && audioPolicy.getActiveDevicePolicyAfterConnection()
                        == BluetoothSinkAudioPolicy.POLICY_NOT_ALLOWED) {
            return false;
        }
        if (!headsetService.setActiveDevice(device)) {
            return false;
        }
        synchronized (mLock) {
            mHfpActiveDevice = device;
        }
        return true;
    }

    private boolean setHearingAidActiveDevice(@NonNull BluetoothDevice device) {
        return setHearingAidActiveDevice(device, false);
    }

    private boolean setHearingAidActiveDevice(
            @Nullable BluetoothDevice device, boolean hasFallbackDevice) {
        if (DBG) {
            Log.d(
                    TAG,
                    "setHearingAidActiveDevice("
                            + device
                            + ")"
                            + (device == null ? " hasFallbackDevice=" + hasFallbackDevice : ""));
        }

        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService == null) {
            return false;
        }

        synchronized (mLock) {
            if (device == null) {
                if (!hearingAidService.removeActiveDevice(!hasFallbackDevice)) {
                    return false;
                }
                mHearingAidActiveDevices.clear();
                return true;
            }

            long hiSyncId = hearingAidService.getHiSyncId(device);
            if (getHearingAidActiveHiSyncIdLocked() == hiSyncId) {
                mHearingAidActiveDevices.add(device);
                return true;
            }

            if (!hearingAidService.setActiveDevice(device)) {
                return false;
            }
            mHearingAidActiveDevices.clear();
            mHearingAidActiveDevices.addAll(hearingAidService.getConnectedPeerDevices(hiSyncId));
        }
        return true;
    }

    private boolean setLeAudioActiveDevice(@NonNull BluetoothDevice device) {
        return setLeAudioActiveDevice(device, false);
    }

    private boolean setLeAudioActiveDevice(
            @Nullable BluetoothDevice device, boolean hasFallbackDevice) {
        if (DBG) {
            Log.d(
                    TAG,
                    "setLeAudioActiveDevice("
                            + device
                            + ")"
                            + (device == null ? " hasFallbackDevice=" + hasFallbackDevice : ""));
        }
        final LeAudioService leAudioService = mFactory.getLeAudioService();
        if (leAudioService == null) {
            return false;
        }
        boolean success;
        if (device == null) {
            success = leAudioService.removeActiveDevice(hasFallbackDevice);
        } else {
            success = leAudioService.setActiveDevice(device);
        }

        if (!success) {
            return false;
        }

        synchronized (mLock) {
            mLeAudioActiveDevice = device;
        }
        return true;
    }

    private void resetState() {
        synchronized (mLock) {
            mA2dpConnectedDevices.clear();
            mA2dpActiveDevice = null;

            mHfpConnectedDevices.clear();
            mHfpActiveDevice = null;

            mHearingAidConnectedDevices.clear();
            mHearingAidActiveDevices.clear();

            mLeAudioConnectedDevices.clear();
            mLeAudioActiveDevice = null;
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    List<BluetoothDevice> removeWatchDevices(List<BluetoothDevice> devices) {
        List<BluetoothDevice> fallbackCandidates;
        synchronized (mLock) {
            fallbackCandidates = new ArrayList<>(devices);
        }
        List<BluetoothDevice> uninterestedCandidates = new ArrayList<>();
        for (BluetoothDevice device : fallbackCandidates) {
            byte[] deviceType =
                    mDbManager.getCustomMeta(device, BluetoothDevice.METADATA_DEVICE_TYPE);
            BluetoothClass deviceClass = device.getBluetoothClass();
            if ((deviceClass != null
                            && deviceClass.getMajorDeviceClass()
                                    == BluetoothClass.Device.WEARABLE_WRIST_WATCH)
                    || (deviceType != null
                            && BluetoothDevice.DEVICE_TYPE_WATCH.equals(new String(deviceType)))) {
                uninterestedCandidates.add(device);
            }
        }
        for (BluetoothDevice device : uninterestedCandidates) {
            fallbackCandidates.remove(device);
        }
        return fallbackCandidates;
    }

    @VisibleForTesting
    BluetoothDevice getA2dpActiveDevice() {
        synchronized (mLock) {
            return mA2dpActiveDevice;
        }
    }

    @VisibleForTesting
    BluetoothDevice getHfpActiveDevice() {
        synchronized (mLock) {
            return mHfpActiveDevice;
        }
    }

    @VisibleForTesting
    Set<BluetoothDevice> getHearingAidActiveDevices() {
        synchronized (mLock) {
            return mHearingAidActiveDevices;
        }
    }

    @VisibleForTesting
    BluetoothDevice getLeAudioActiveDevice() {
        synchronized (mLock) {
            return mLeAudioActiveDevice;
        }
    }

    @GuardedBy("mLock")
    private long getHearingAidActiveHiSyncIdLocked() {
        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService != null && !mHearingAidActiveDevices.isEmpty()) {
            return hearingAidService.getHiSyncId(mHearingAidActiveDevices.iterator().next());
        }
        return BluetoothHearingAid.HI_SYNC_ID_INVALID;
    }

    /**
     * Checks CoD and metadata to determine if the device is a watch
     *
     * @param device the remote device
     * @return {@code true} if it's a watch, {@code false} otherwise
     */
    private boolean isWatch(BluetoothDevice device) {
        // Check CoD
        BluetoothClass deviceClass = device.getBluetoothClass();
        if (deviceClass != null
                && deviceClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_WRIST_WATCH) {
            return true;
        }

        // Check metadata
        byte[] deviceType = mDbManager.getCustomMeta(device, BluetoothDevice.METADATA_DEVICE_TYPE);
        if (deviceType == null) {
            return false;
        }
        String deviceTypeStr = new String(deviceType);
        if (deviceTypeStr.equals(BluetoothDevice.DEVICE_TYPE_WATCH)) {
            return true;
        }

        return false;
    }

    /**
     * Called when a wired audio device is connected. It might be called multiple times each time a
     * wired audio device is connected.
     */
    @VisibleForTesting
    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    @Override
    void wiredAudioDeviceConnected() {
        if (DBG) {
            Log.d(TAG, "wiredAudioDeviceConnected");
        }
        setA2dpActiveDevice(null, true);
        setHfpActiveDevice(null);
        setHearingAidActiveDevice(null, true);
        setLeAudioActiveDevice(null, true);
    }

    // TODO: make AudioRoutingHandler private
    class AudioRoutingHandler extends Handler {
        // TODO: make mConnectedDevices private
        public final ArrayMap<BluetoothDevice, AudioRoutingDevice> mConnectedDevices =
                new ArrayMap<>();
        // TODO: make mActiveDevices private
        public final SparseArray<List<BluetoothDevice>> mActiveDevices = new SparseArray<>();

        AudioRoutingHandler(Looper looper) {
            super(looper);
        }

        public void handleProfileConnected(int profile, BluetoothDevice device) {
            if (DBG) {
                Log.d(
                        TAG,
                        "handleProfileConnected(device="
                                + device
                                + ", profile="
                                + BluetoothProfile.getProfileName(profile)
                                + ")");
            }
            AudioRoutingDevice arDevice = getAudioRoutingDevice(device);
            if (arDevice.connectedProfiles.contains(profile)) {
                if (DBG) {
                    Log.d(TAG, "This device is already connected: " + device);
                }
                return;
            }
            arDevice.connectedProfiles.add(profile);
            // TODO: remove the following switch-case statement
            synchronized (mLock) {
                switch (profile) {
                    case BluetoothProfile.HEADSET -> mHfpConnectedDevices.add(device);
                    case BluetoothProfile.A2DP -> mA2dpConnectedDevices.add(device);
                    case BluetoothProfile.LE_AUDIO -> mLeAudioConnectedDevices.add(device);
                    case BluetoothProfile.HEARING_AID -> mHearingAidConnectedDevices.add(device);
                }
            }
            if (isWatch(device)) {
                Log.i(TAG, "Do not set profile active for watch device when connected: " + device);
                return;
            }
            if (!arDevice.canActivateNow(profile)) {
                if (DBG) {
                    Log.d(TAG, "Can not activate now: " + BluetoothProfile.getProfileName(profile));
                }
                mHandler.postDelayed(
                        () -> arDevice.activateProfile(profile),
                        A2DP_HFP_SYNC_CONNECTION_TIMEOUT_MS);
                return;
            }
            arDevice.activateProfile(profile);
        }

        public void handleProfileDisconnected(int profile, BluetoothDevice device) {
            if (DBG) {
                Log.d(
                        TAG,
                        "handleProfileDisconnected(device="
                                + device
                                + ", profile="
                                + BluetoothProfile.getProfileName(profile)
                                + ")");
            }
            AudioRoutingDevice arDevice = getAudioRoutingDevice(device);
            arDevice.connectedProfiles.remove(profile);
            if (arDevice.connectedProfiles.isEmpty()) {
                mConnectedDevices.remove(device);
            }
            // TODO: remove the following switch-case statement
            synchronized (mLock) {
                switch (profile) {
                    case BluetoothProfile.HEADSET -> mHfpConnectedDevices.remove(device);
                    case BluetoothProfile.A2DP -> mA2dpConnectedDevices.remove(device);
                    case BluetoothProfile.LE_AUDIO -> mLeAudioConnectedDevices.remove(device);
                    case BluetoothProfile.HEARING_AID -> mHearingAidConnectedDevices.remove(device);
                }
            }
            List<BluetoothDevice> activeDevices = mActiveDevices.get(profile);
            if (activeDevices != null && activeDevices.contains(device)) {
                activeDevices.remove(device);
                if (activeDevices.size() == 0) {
                    if (!setFallbackDeviceActive()) {
                        arDevice.deactivate(profile, false);
                    }
                }
            }
        }

        // TODO: check the IllegalStateException is not happening after refactoring.
        public void handleProfileActiveDeviceChanged(int profile, BluetoothDevice device) {
            if (device == null && !mActiveDevices.contains(profile)) return;
            List<BluetoothDevice> activeDevices = mActiveDevices.get(profile);
            if (activeDevices != null && activeDevices.contains(device)) return;
            throw new IllegalStateException(
                    "Unexpected deactivation events: "
                            + BluetoothProfile.getProfileName(profile) + ", device=" + device);
        }

        private boolean setFallbackDeviceActive() {
            if (DBG) {
                Log.d(TAG, "setFallbackDeviceActive");
            }
            List<BluetoothDevice> candidates = new ArrayList<>();
            int audioMode = mAudioManager.getMode();
            for (AudioRoutingDevice arDevice : mConnectedDevices.values()) {
                for (int profile : arDevice.connectedProfiles) {
                    if (audioMode == AudioManager.MODE_NORMAL) {
                        if (profile != BluetoothProfile.HEADSET) {
                            candidates.add(arDevice.device);
                            break;
                        }
                    } else {
                        if (profile != BluetoothProfile.A2DP) {
                            candidates.add(arDevice.device);
                            break;
                        }
                    }
                }
            }
            AudioRoutingDevice deviceToActivate =
                    getAudioRoutingDevice(
                            mDbManager.getMostRecentlyConnectedDevicesInList(candidates));
            if (deviceToActivate != null) {
                return deviceToActivate.activateDevice();
            }
            return false;
        }

        // TODO: make getAudioRoutingDevice private
        // TODO: handle the connection policy change events.
        public AudioRoutingDevice getAudioRoutingDevice(@NonNull BluetoothDevice device) {
            Objects.nonNull(device);
            AudioRoutingDevice arDevice = mConnectedDevices.get(device);
            if (arDevice != null) {
                return arDevice;
            }
            arDevice = new AudioRoutingDevice();
            arDevice.device = device;
            arDevice.supportedProfiles = new HashSet<>();
            arDevice.connectedProfiles = new HashSet<>();
            if (mDbManager.getProfileConnectionPolicy(device, BluetoothProfile.HEADSET)
                    == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                arDevice.supportedProfiles.add(BluetoothProfile.HEADSET);
            } else {
                arDevice.supportedProfiles.remove(BluetoothProfile.HEADSET);
            }
            if (mDbManager.getProfileConnectionPolicy(device, BluetoothProfile.A2DP)
                    == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                arDevice.supportedProfiles.add(BluetoothProfile.A2DP);
            } else {
                arDevice.supportedProfiles.remove(BluetoothProfile.A2DP);
            }
            if (mDbManager.getProfileConnectionPolicy(device, BluetoothProfile.HEARING_AID)
                    == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                arDevice.supportedProfiles.add(BluetoothProfile.HEARING_AID);
            } else {
                arDevice.supportedProfiles.remove(BluetoothProfile.HEARING_AID);
            }
            if (mDbManager.getProfileConnectionPolicy(device, BluetoothProfile.LE_AUDIO)
                    == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                arDevice.supportedProfiles.add(BluetoothProfile.LE_AUDIO);
            } else {
                arDevice.supportedProfiles.remove(BluetoothProfile.LE_AUDIO);
            }
            mConnectedDevices.put(device, arDevice);
            return arDevice;
        }

        // TODO: make AudioRoutingDevice private
        public class AudioRoutingDevice {
            public BluetoothDevice device;
            public Set<Integer> supportedProfiles;
            public Set<Integer> connectedProfiles;

            public boolean canActivateNow(int profile) {
                if (!connectedProfiles.contains(profile)) return false;
                // TODO: Return false if there are another active remote streaming an audio.
                // TODO: consider LE audio and HearingAid.
                return switch (profile) {
                    case BluetoothProfile.HEADSET -> !supportedProfiles.contains(
                                    BluetoothProfile.A2DP)
                            || connectedProfiles.contains(BluetoothProfile.A2DP);
                    case BluetoothProfile.A2DP -> !supportedProfiles.contains(
                                    BluetoothProfile.HEADSET)
                            || connectedProfiles.contains(BluetoothProfile.HEADSET);
                    default -> true;
                };
            }

            /**
             * Activate the device. If supported, this will activate hearing aid and LE audio first,
             * then A2DP and HFP.
             *
             * @return true if any profile was activated.
             */
            public boolean activateDevice() {
                if (DBG) {
                    Log.d(TAG, "activateDevice: device=" + device);
                }

                // Try to activate hearing aid and LE audio first
                if (connectedProfiles.contains(BluetoothProfile.HEARING_AID)) {
                    return activateProfile(BluetoothProfile.HEARING_AID);
                } else if (connectedProfiles.contains(BluetoothProfile.LE_AUDIO)) {
                    return activateProfile(BluetoothProfile.LE_AUDIO);
                } else if (connectedProfiles.contains(BluetoothProfile.A2DP)) {
                    return activateProfile(BluetoothProfile.A2DP);
                } else if (connectedProfiles.contains(BluetoothProfile.HEADSET)) {
                    return activateProfile(BluetoothProfile.HEADSET);
                }
                Log.w(
                        TAG,
                        "Fail to activate the device: " + device + ", no connected audio profiles");
                return false;
            }

            /**
             * Activate the given profile and related profiles if possible. A2DP and HFP would be
             * activated together if possible. If there are any activated profiles that can't be
             * activated together, they will be deactivated.
             *
             * @param profile the profile requited to be activated
             * @return true if any profile was activated or the given profile was already active.
             */
            @SuppressLint("MissingPermission")
            public boolean activateProfile(int profile) {
                List<BluetoothDevice> activeDevices = mActiveDevices.get(profile);
                if (activeDevices != null && activeDevices.contains(device)) {
                    return true;
                }
                HashSet<Integer> profilesToActivate = new HashSet<>();
                HashSet<Integer> profilesToDeactivate = new HashSet<>();
                for (int i = 0; i < mActiveDevices.size(); i++) {
                    profilesToDeactivate.add(mActiveDevices.keyAt(i));
                }

                profilesToActivate.add(profile);
                profilesToDeactivate.remove(profile);

                switch (profile) {
                    case BluetoothProfile.A2DP:
                        profilesToDeactivate.remove(BluetoothProfile.HEADSET);
                        if (connectedProfiles.contains(BluetoothProfile.HEADSET)) {
                            activeDevices = mActiveDevices.get(BluetoothProfile.HEADSET);
                            if (activeDevices == null || !activeDevices.contains(device)) {
                                profilesToActivate.add(BluetoothProfile.HEADSET);
                            }
                        }
                        if (Utils.isDualModeAudioEnabled()) {
                            activeDevices = mActiveDevices.get(BluetoothProfile.LE_AUDIO);
                            if (activeDevices != null && activeDevices.contains(device)) {
                                profilesToDeactivate.remove(BluetoothProfile.LE_AUDIO);
                            }
                        }
                        break;
                    case BluetoothProfile.HEADSET:
                        profilesToDeactivate.remove(BluetoothProfile.A2DP);
                        if (connectedProfiles.contains(BluetoothProfile.A2DP)) {
                            activeDevices = mActiveDevices.get(BluetoothProfile.A2DP);
                            if (activeDevices == null || !activeDevices.contains(device)) {
                                profilesToActivate.add(BluetoothProfile.A2DP);
                            }
                        }
                        if (Utils.isDualModeAudioEnabled()) {
                            activeDevices = mActiveDevices.get(BluetoothProfile.LE_AUDIO);
                            if (activeDevices != null && activeDevices.contains(device)) {
                                profilesToDeactivate.remove(BluetoothProfile.LE_AUDIO);
                            }
                        }
                        break;
                    case BluetoothProfile.LE_AUDIO:
                        if (Utils.isDualModeAudioEnabled()) {
                            activeDevices = mActiveDevices.get(BluetoothProfile.HEADSET);
                            if (activeDevices != null && activeDevices.contains(device)) {
                                profilesToDeactivate.remove(BluetoothProfile.HEADSET);
                            }
                            activeDevices = mActiveDevices.get(BluetoothProfile.A2DP);
                            if (activeDevices != null && activeDevices.contains(device)) {
                                profilesToDeactivate.remove(BluetoothProfile.A2DP);
                            }
                        }
                }
                boolean isAnyProfileActivated = false;
                for (Integer p : profilesToActivate) {
                    if (DBG) {
                        Log.d(TAG, "Activate profile: " + BluetoothProfile.getProfileName(p));
                    }
                    boolean activated = switch (p) {
                        case BluetoothProfile.A2DP -> setA2dpActiveDevice(device);
                        case BluetoothProfile.HEADSET -> setHfpActiveDevice(device);
                        case BluetoothProfile.LE_AUDIO -> setLeAudioActiveDevice(device);
                        case BluetoothProfile.HEARING_AID -> setHearingAidActiveDevice(device);
                        default -> false;
                    };
                    if (activated) {
                        // TODO: handle this inside of setXxxActiveDevice() method
                        activeDevices = mActiveDevices.get(p);
                        if (activeDevices == null) {
                            activeDevices = new ArrayList<>();
                            mActiveDevices.put(p, activeDevices);
                        }
                        if (!canActivateTogether(p, activeDevices)) {
                            activeDevices.clear();
                        }
                        activeDevices.add(device);
                    }
                    isAnyProfileActivated |= activated;
                }
                // Do not deactivate profiles if no profiles were activated.
                if (!isAnyProfileActivated) return false;
                for (Integer p : profilesToDeactivate) {
                    Log.d(TAG, "Deactivate profile: " + BluetoothProfile.getProfileName(p));
                    mActiveDevices.remove(p);
                    switch (p) {
                        case BluetoothProfile.A2DP -> setA2dpActiveDevice(null, true);
                        case BluetoothProfile.HEADSET -> setHfpActiveDevice(null);
                        case BluetoothProfile.LE_AUDIO -> setLeAudioActiveDevice(null, true);
                        case BluetoothProfile.HEARING_AID -> setHearingAidActiveDevice(null, true);
                    }
                }
                return true;
            }

            @SuppressLint("MissingPermission")
            public void deactivate(int profile, boolean hasFallbackDevice) {
                if (!mActiveDevices.contains(profile)) return;
                mActiveDevices.remove(profile);
                switch (profile) {
                    case BluetoothProfile.A2DP -> setA2dpActiveDevice(null, hasFallbackDevice);
                    case BluetoothProfile.HEADSET -> setHfpActiveDevice(null);
                    case BluetoothProfile.LE_AUDIO -> setLeAudioActiveDevice(null, false);
                    case BluetoothProfile.HEARING_AID -> setHearingAidActiveDevice(null, false);
                }
            }

            public boolean canActivateTogether(int profile, List<BluetoothDevice> group) {
                if (group == null || group.isEmpty()) {
                    return true;
                }
                switch (profile) {
                    case BluetoothProfile.LE_AUDIO: {
                        final LeAudioService leAudioService = mFactory.getLeAudioService();
                        if (leAudioService == null) {
                            return false;
                        }
                        int groupId = leAudioService.getGroupId(device);
                        if (groupId != LE_AUDIO_GROUP_ID_INVALID
                                && groupId == leAudioService.getGroupId(group.get(0))) {
                            return true;
                        }
                        break;
                    }
                    case BluetoothProfile.HEARING_AID: {
                        final HearingAidService hearingAidService = mFactory.getHearingAidService();
                        if (hearingAidService == null) {
                            return false;
                        }
                        long hiSyncId = hearingAidService.getHiSyncId(device);
                        if (hiSyncId != BluetoothHearingAid.HI_SYNC_ID_INVALID
                                && hiSyncId == hearingAidService.getHiSyncId(group.get(0))) {
                            return true;
                        }
                        break;
                    }
                }
                return false;
            }
        }
    }
}
