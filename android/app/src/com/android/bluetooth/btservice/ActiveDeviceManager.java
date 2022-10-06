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
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
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
import android.os.Message;
import android.util.Log;

import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.internal.annotations.VisibleForTesting;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The active device manager is responsible for keeping track of the
 * connected A2DP/HFP/AVRCP/HearingAid/LE audio devices and select which device is
 * active (for each profile).
 * The active device manager selects a fallback device when the currently active device
 * is disconnected, and it selects BT devices that are lastly activated one.
 */
class ActiveDeviceManager {
    private static final String TAG = "ActiveDeviceManager";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    // Message types for the handler
    private static final int MESSAGE_ADAPTER_ACTION_STATE_CHANGED = 1;
    private static final int MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED = 2;
    private static final int MESSAGE_A2DP_ACTION_ACTIVE_DEVICE_CHANGED = 3;
    private static final int MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED = 4;
    private static final int MESSAGE_HFP_ACTION_ACTIVE_DEVICE_CHANGED = 5;
    private static final int MESSAGE_HEARING_AID_ACTION_CONNECTION_STATE_CHANGED = 6;
    private static final int MESSAGE_HEARING_AID_ACTION_ACTIVE_DEVICE_CHANGED = 7;
    private static final int MESSAGE_LE_AUDIO_ACTION_CONNECTION_STATE_CHANGED = 8;
    private static final int MESSAGE_LE_AUDIO_ACTION_ACTIVE_DEVICE_CHANGED = 9;

    private final AdapterService mAdapterService;
    private final ServiceFactory mFactory;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private final AudioManager mAudioManager;
    private final AudioManagerAudioDeviceCallback mAudioManagerAudioDeviceCallback;

    private final List<BluetoothDevice> mA2dpConnectedDevices = new LinkedList<>();
    private final List<BluetoothDevice> mHfpConnectedDevices = new LinkedList<>();
    private final List<BluetoothDevice> mHearingAidConnectedDevices = new LinkedList<>();
    private final List<BluetoothDevice> mLeAudioConnectedDevices = new LinkedList<>();
    private BluetoothDevice mA2dpActiveDevice = null;
    private BluetoothDevice mHfpActiveDevice = null;
    private BluetoothDevice mHearingAidActiveDevice = null;
    private BluetoothDevice mLeAudioActiveDevice = null;

    // Broadcast receiver for all changes
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Received intent with null action");
                return;
            }
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_ADAPTER_ACTION_STATE_CHANGED,
                                           intent).sendToTarget();
                    break;
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED,
                                           intent).sendToTarget();
                    break;
                case BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_A2DP_ACTION_ACTIVE_DEVICE_CHANGED,
                                           intent).sendToTarget();
                    break;
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED,
                                           intent).sendToTarget();
                    break;
                case BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_HFP_ACTION_ACTIVE_DEVICE_CHANGED,
                        intent).sendToTarget();
                    break;
                case BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_HEARING_AID_ACTION_CONNECTION_STATE_CHANGED,
                            intent).sendToTarget();
                    break;
                case BluetoothHearingAid.ACTION_ACTIVE_DEVICE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_HEARING_AID_ACTION_ACTIVE_DEVICE_CHANGED,
                            intent).sendToTarget();
                    break;
                case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_LE_AUDIO_ACTION_CONNECTION_STATE_CHANGED,
                            intent).sendToTarget();
                    break;
                case BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED:
                    mHandler.obtainMessage(MESSAGE_LE_AUDIO_ACTION_ACTIVE_DEVICE_CHANGED,
                            intent).sendToTarget();
                    break;
                default:
                    Log.e(TAG, "Received unexpected intent, action=" + action);
                    break;
            }
        }
    };

    class ActiveDeviceManagerHandler extends Handler {
        ActiveDeviceManagerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ADAPTER_ACTION_STATE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (DBG) {
                        Log.d(TAG, "handleMessage(MESSAGE_ADAPTER_ACTION_STATE_CHANGED): newState="
                                + newState);
                    }
                    if (newState == BluetoothAdapter.STATE_ON) {
                        resetState();
                    }
                }
                break;

                case MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                    int nextState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    if (prevState == nextState) {
                        // Nothing has changed
                        break;
                    }
                    if (nextState == BluetoothProfile.STATE_CONNECTED) {
                        // Device connected
                        if (DBG) {
                            Log.d(TAG,
                                    "handleMessage(MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED): "
                                    + "device " + device + " connected");
                        }
                        if (mA2dpConnectedDevices.contains(device)) {
                            break;      // The device is already connected
                        }
                        mA2dpConnectedDevices.add(device);
                        if (mHearingAidActiveDevice == null) {
                            // New connected device: select it as active
                            // Do not deactivate HFP. HFP can be activated at the same time.
                            setA2dpActiveDevice(device);
                            setLeAudioActiveDevice(null);
                        }
                        break;
                    }
                    if (prevState == BluetoothProfile.STATE_CONNECTED) {
                        // Device disconnected
                        if (DBG) {
                            Log.d(TAG,
                                    "handleMessage(MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED): "
                                    + "device " + device + " disconnected");
                        }
                        mA2dpConnectedDevices.remove(device);
                        if (mA2dpConnectedDevices.isEmpty()
                                && Objects.equals(mA2dpActiveDevice, device)) {
                            setA2dpActiveDevice(null);
                            setFallbackDeviceActive();
                        }
                    }
                }
                break;

                case MESSAGE_A2DP_ACTION_ACTIVE_DEVICE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (DBG) {
                        Log.d(TAG, "handleMessage(MESSAGE_A2DP_ACTION_ACTIVE_DEVICE_CHANGED): "
                                + "device= " + device);
                    }
                    if (device != null && !Objects.equals(mA2dpActiveDevice, device)) {
                        // Do not deactivate HFP. HFP can be activated at the same time.
                        setHearingAidActiveDevice(null);
                        setLeAudioActiveDevice(null);
                    }
                    // Just assign locally the new value
                    mA2dpActiveDevice = device;
                }
                break;

                case MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                    int nextState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    if (prevState == nextState) {
                        // Nothing has changed
                        break;
                    }
                    if (nextState == BluetoothProfile.STATE_CONNECTED) {
                        // Device connected
                        if (DBG) {
                            Log.d(TAG,
                                    "handleMessage(MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED): "
                                    + "device " + device + " connected");
                        }
                        if (mHfpConnectedDevices.contains(device)) {
                            break;      // The device is already connected
                        }
                        mHfpConnectedDevices.add(device);
                        if (mHearingAidActiveDevice == null) {
                            // New connected device: select it as active
                            // Do not deactivate A2DP. A2DP can be activated at the same time.
                            setHfpActiveDevice(device);
                            setLeAudioActiveDevice(null);
                        }
                        break;
                    }
                    if (prevState == BluetoothProfile.STATE_CONNECTED) {
                        // Device disconnected
                        if (DBG) {
                            Log.d(TAG,
                                    "handleMessage(MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED): "
                                    + "device " + device + " disconnected");
                        }
                        mHfpConnectedDevices.remove(device);
                        if (mHfpConnectedDevices.isEmpty()
                                && Objects.equals(mHfpActiveDevice, device)) {
                            setHfpActiveDevice(null);
                            setFallbackDeviceActive();
                        }
                    }
                }
                break;

                case MESSAGE_HFP_ACTION_ACTIVE_DEVICE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (DBG) {
                        Log.d(TAG, "handleMessage(MESSAGE_HFP_ACTION_ACTIVE_DEVICE_CHANGED): "
                                + "device= " + device);
                    }
                    if (device != null && !Objects.equals(mHfpActiveDevice, device)) {
                        // Do not deactivate A2DP. A2DP can be activated at the same time.
                        setHearingAidActiveDevice(null);
                        setLeAudioActiveDevice(null);
                    }
                    // Just assign locally the new value
                    mHfpActiveDevice = device;
                }
                break;

                case MESSAGE_HEARING_AID_ACTION_CONNECTION_STATE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                    int nextState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    if (prevState == nextState) {
                        // Nothing has changed
                        break;
                    }
                    if (nextState == BluetoothProfile.STATE_CONNECTED) {
                        // Device connected
                        if (DBG) {
                            Log.d(TAG, "handleMessage(MESSAGE_HEARING_AID_ACTION_CONNECTION_STATE"
                                    + "_CHANGED): device " + device + " connected");
                        }
                        if (mHearingAidConnectedDevices.contains(device)) {
                            break;      // The device is already connected
                        }
                        mHearingAidConnectedDevices.add(device);
                        // New connected device: select it as active
                        setHearingAidActiveDevice(device);
                        setA2dpActiveDevice(null);
                        setHfpActiveDevice(null);
                        setLeAudioActiveDevice(null);
                        break;
                    }
                    if (prevState == BluetoothProfile.STATE_CONNECTED) {
                        // Device disconnected
                        if (DBG) {
                            Log.d(TAG, "handleMessage(MESSAGE_HEARING_AID_ACTION_CONNECTION_STATE"
                                    + "_CHANGED): device " + device + " disconnected");
                        }
                        mHearingAidConnectedDevices.remove(device);
                        if (mHearingAidConnectedDevices.isEmpty()
                                && Objects.equals(mHearingAidActiveDevice, device)) {
                            setHearingAidActiveDevice(null);
                            setFallbackDeviceActive();
                        }
                    }
                }
                break;

                case MESSAGE_HEARING_AID_ACTION_ACTIVE_DEVICE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (DBG) {
                        Log.d(TAG, "handleMessage(MESSAGE_HA_ACTION_ACTIVE_DEVICE_CHANGED): "
                                + "device= " + device);
                    }
                    // Just assign locally the new value
                    mHearingAidActiveDevice = device;
                    if (device != null) {
                        setA2dpActiveDevice(null);
                        setHfpActiveDevice(null);
                        setLeAudioActiveDevice(null);
                    }
                }
                break;

                case MESSAGE_LE_AUDIO_ACTION_CONNECTION_STATE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                    int nextState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    if (prevState == nextState) {
                        // Nothing has changed
                        break;
                    }
                    if (nextState == BluetoothProfile.STATE_CONNECTED) {
                        // Device connected
                        if (DBG) {
                            Log.d(TAG, "handleMessage(MESSAGE_LE_AUDIO_ACTION_CONNECTION_STATE"
                                    + "_CHANGED): device " + device + " connected");
                        }
                        if (mLeAudioConnectedDevices.contains(device)) {
                            break;      // The device is already connected
                        }
                        mLeAudioConnectedDevices.add(device);
                        if (mHearingAidActiveDevice == null) {
                            // New connected device: select it as active
                            // Do not deactivate A2DP. A2DP can be activated at the same time.
                            setLeAudioActiveDevice(device);
                            setA2dpActiveDevice(null);
                            setHfpActiveDevice(null);
                        }
                        break;
                    }
                    if (prevState == BluetoothProfile.STATE_CONNECTED) {
                        // Device disconnected
                        if (DBG) {
                            Log.d(TAG, "handleMessage(MESSAGE_LE_AUDIO_ACTION_CONNECTION_STATE"
                                    + "_CHANGED): device " + device + " disconnected");
                        }
                        mLeAudioConnectedDevices.remove(device);
                        if (mLeAudioConnectedDevices.isEmpty()
                                && Objects.equals(mLeAudioActiveDevice, device)) {
                            setLeAudioActiveDevice(null);
                            setFallbackDeviceActive();
                        }
                    }
                }
                break;

                case MESSAGE_LE_AUDIO_ACTION_ACTIVE_DEVICE_CHANGED: {
                    Intent intent = (Intent) msg.obj;
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && !mLeAudioConnectedDevices.contains(device)) {
                        mLeAudioConnectedDevices.add(device);
                    }
                    if (DBG) {
                        Log.d(TAG, "handleMessage(MESSAGE_LE_AUDIO_ACTION_ACTIVE_DEVICE_CHANGED): "
                                + "device= " + device);
                    }
                    // Just assign locally the new value
                    if (device != null && !Objects.equals(mLeAudioActiveDevice, device)) {
                        setA2dpActiveDevice(null);
                        setHfpActiveDevice(null);
                        setHearingAidActiveDevice(null);
                    }
                    mLeAudioActiveDevice = device;
                }
                break;
            }
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
    }

    void start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }

        mHandlerThread = new HandlerThread("BluetoothActiveDeviceManager");
        mHandlerThread.start();
        mHandler = new ActiveDeviceManagerHandler(mHandlerThread.getLooper());

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
        mAdapterService.registerReceiver(mReceiver, filter);

        mAudioManager.registerAudioDeviceCallback(mAudioManagerAudioDeviceCallback, mHandler);
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

    private void setA2dpActiveDevice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "setA2dpActiveDevice(" + device + ")");
        }
        final A2dpService a2dpService = mFactory.getA2dpService();
        if (a2dpService == null) {
            return;
        }
        if (!a2dpService.setActiveDevice(device)) {
            return;
        }
        mA2dpActiveDevice = device;
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    private void setHfpActiveDevice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "setHfpActiveDevice(" + device + ")");
        }
        final HeadsetService headsetService = mFactory.getHeadsetService();
        if (headsetService == null) {
            return;
        }
        if (!headsetService.setActiveDevice(device)) {
            return;
        }
        mHfpActiveDevice = device;
    }

    private void setHearingAidActiveDevice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "setHearingAidActiveDevice(" + device + ")");
        }
        final HearingAidService hearingAidService = mFactory.getHearingAidService();
        if (hearingAidService == null) {
            return;
        }
        if (!hearingAidService.setActiveDevice(device)) {
            return;
        }
        mHearingAidActiveDevice = device;
    }

    private void setLeAudioActiveDevice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "setLeAudioActiveDevice(" + device + ")");
        }
        final LeAudioService leAudioService = mFactory.getLeAudioService();
        if (leAudioService == null) {
            return;
        }
        if (!leAudioService.setActiveDevice(device)) {
            return;
        }
        mLeAudioActiveDevice = device;
    }

    private void setFallbackDeviceActive() {
        if (DBG) {
            Log.d(TAG, "setFallbackDeviceActive");
        }
        DatabaseManager dbManager = mAdapterService.getDatabase();
        if (dbManager == null) {
            return;
        }

        if (!mHearingAidConnectedDevices.isEmpty()) {
            BluetoothDevice device =
                    dbManager.getMostRecentlyConnectedDevicesInList(mHearingAidConnectedDevices);
            if (device != null) {
                if (DBG) {
                    Log.d(TAG, "set hearing aid device active: " + device);
                }
                setHearingAidActiveDevice(device);
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

        List<BluetoothDevice> connectedDevices = new LinkedList<>();
        connectedDevices.addAll(mLeAudioConnectedDevices);
        switch (mAudioManager.getMode()) {
            case AudioManager.MODE_NORMAL:
                if (a2dpFallbackDevice != null) {
                    connectedDevices.add(a2dpFallbackDevice);
                }
                break;
            case AudioManager.MODE_RINGTONE:
                if (headsetService != null && headsetService.isInbandRingingEnabled()) {
                    connectedDevices.add(headsetFallbackDevice);
                }
                break;
            default:
                if (headsetService != null) {
                    connectedDevices.add(headsetFallbackDevice);
                }
        }
        BluetoothDevice device = dbManager.getMostRecentlyConnectedDevicesInList(connectedDevices);
        if (device != null) {
            if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) {
                if (Objects.equals(a2dpFallbackDevice, device)) {
                    if (DBG) {
                        Log.d(TAG, "set A2DP device active: " + device);
                    }
                    setA2dpActiveDevice(device);
                    if (headsetFallbackDevice != null) {
                        setHfpActiveDevice(device);
                    }
                } else {
                    if (DBG) {
                        Log.d(TAG, "set LE audio device active: " + device);
                    }
                    setLeAudioActiveDevice(device);
                }
            } else {
                if (Objects.equals(headsetFallbackDevice, device)) {
                    if (DBG) {
                        Log.d(TAG, "set HFP device active: " + device);
                    }
                    setHfpActiveDevice(device);
                    if (a2dpFallbackDevice != null) {
                        setA2dpActiveDevice(a2dpFallbackDevice);
                    }
                } else {
                    if (DBG) {
                        Log.d(TAG, "set LE audio device active: " + device);
                    }
                    setLeAudioActiveDevice(device);
                }
            }
        }
    }

    private void resetState() {
        mA2dpConnectedDevices.clear();
        mA2dpActiveDevice = null;

        mHfpConnectedDevices.clear();
        mHfpActiveDevice = null;

        mHearingAidActiveDevice = null;
        mLeAudioActiveDevice = null;
    }

    @VisibleForTesting
    BroadcastReceiver getBroadcastReceiver() {
        return mReceiver;
    }

    @VisibleForTesting
    BluetoothDevice getA2dpActiveDevice() {
        return mA2dpActiveDevice;
    }

    @VisibleForTesting
    BluetoothDevice getHfpActiveDevice() {
        return mHfpActiveDevice;
    }

    @VisibleForTesting
    BluetoothDevice getHearingAidActiveDevice() {
        return mHearingAidActiveDevice;
    }

    @VisibleForTesting
    BluetoothDevice getLeAudioActiveDevice() {
        return mLeAudioActiveDevice;
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
        setA2dpActiveDevice(null);
        setHfpActiveDevice(null);
        setHearingAidActiveDevice(null);
        setLeAudioActiveDevice(null);
    }
}
