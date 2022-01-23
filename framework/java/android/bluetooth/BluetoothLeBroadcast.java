/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.Context;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs to control the Bluetooth LE Broadcast Source profile.
 *
 * <p>BluetoothLeBroadcast is a proxy object for controlling the Bluetooth LE Broadcast Source
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothLeBroadcast
 * proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcast implements BluetoothProfile {
    private static final String TAG = "BluetoothLeBroadcast";
    private static final boolean DBG = true;

    /**
     * Constants used by the LE Audio Broadcast profile for the Broadcast state
     *
     * @hide
     */
    @IntDef(
            prefix = {"LE_AUDIO_BROADCAST_STATE_"},
            value = {
                    LE_AUDIO_BROADCAST_STATE_DISABLED,
                    LE_AUDIO_BROADCAST_STATE_ENABLING,
                    LE_AUDIO_BROADCAST_STATE_ENABLED,
                    LE_AUDIO_BROADCAST_STATE_DISABLING,
                    LE_AUDIO_BROADCAST_STATE_PLAYING,
                    LE_AUDIO_BROADCAST_STATE_NOT_PLAYING
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LeAudioBroadcastState {}

    /**
     * Indicates that LE Audio Broadcast mode is currently disabled
     *
     * @hide
     */
    @SystemApi
    public static final int LE_AUDIO_BROADCAST_STATE_DISABLED = 10;

    /**
     * Indicates that LE Audio Broadcast mode is being enabled
     *
     * @hide
     */
    @SystemApi
    public static final int LE_AUDIO_BROADCAST_STATE_ENABLING = 11;

    /**
     * Indicates that LE Audio Broadcast mode is currently enabled
     *
     * @hide
     */
    @SystemApi
    public static final int LE_AUDIO_BROADCAST_STATE_ENABLED = 12;

    /**
     * Indicates that LE Audio Broadcast mode is being disabled
     *
     * @hide
     */
    @SystemApi
    public static final int LE_AUDIO_BROADCAST_STATE_DISABLING = 13;

    /**
     * Indicates that an LE Audio Broadcast mode is currently playing
     *
     * @hide
     */
    @SystemApi
    public static final int LE_AUDIO_BROADCAST_STATE_PLAYING = 14;

    /**
     * Indicates that LE Audio Broadcast is currently not playing
     *
     * @hide
     */
    @SystemApi
    public static final int LE_AUDIO_BROADCAST_STATE_NOT_PLAYING = 15;

    /**
     * Interface for receiving events related to broadcasts
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /**
         * Called when broadcast state has changed
         *
         * @param prevState broadcast state before the change
         * @param newState  broadcast state after the change
         */
        void onBroadcastStateChange(@LeAudioBroadcastState int prevState,
                @LeAudioBroadcastState int newState);

        /**
         * Called when broadcast code has been updated
         */
        void onBroadcastCodeSet(@SetBroadcastCodeReturnValues int status);
    }

    /**
     * Create a BluetoothLeBroadcast proxy object for interacting with the local LE Audio Broadcast
     * Source service.
     *
     * @param context  for to operate this API class
     * @param listener listens for service callbacks across binder
     * @hide
     */
    /*package*/ BluetoothLeBroadcast(Context context, BluetoothProfile.ServiceListener listener) {}

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @hide
     */
    @SystemApi
    @Override
    public int getConnectionState(@NonNull BluetoothDevice device) {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @hide
     */
    @SystemApi
    @NonNull
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @hide
     */
    @SystemApi
    @Override
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Register a {@link Callback} that will be invoked during the
     * operation of this profile.
     *
     * Repeated registration of the same <var>callback</var> object will have no effect after
     * the first call to this method, even when the <var>executor</var> is different. API caller
     * would have to call {@link #unregisterCallback(Callback)} with
     * the same callback object before registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws IllegalArgumentException if a null executor, sink, or callback is given
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void registerCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        log("registerCallback");
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Unregister the specified {@link Callback}
     * <p>The same {@link Callback} object used when calling
     * {@link #registerCallback(Executor, Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws IllegalArgumentException when callback is null or when no callback is registered
     * @hide
     */
    @SystemApi
    public void unregisterCallback(@NonNull Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        log("unregisterCallback");
        throw new UnsupportedOperationException("Not Implemented");
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_SET_BROADCAST_MODE_FAILED,
            BluetoothStatusCodes.SUCCESS
    })
    public @interface ChangeBroadcastModeReturnValues {}

    /**
     * Enable LE Audio Broadcast mode.
     *
     * <p>Generates a new broadcast ID and enables sending of encrypted or unencrypted isochronous
     * PDUs
     *
     * @hide
     */
    @SystemApi
    public @ChangeBroadcastModeReturnValues int enableBroadcastMode() {
        if (DBG) log("enableBroadcastMode");
        return BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_SET_BROADCAST_MODE_FAILED;
    }

    /**
     * Disable LE Audio Broadcast mode.
     *
     * @hide
     */
    @SystemApi
    public @ChangeBroadcastModeReturnValues int disableBroadcastMode() {
        if (DBG) log("disableBroadcastMode");
        return BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_SET_BROADCAST_MODE_FAILED;
    }

    /**
     * Get the current LE Audio broadcast state
     *
     * @hide
     */
    @SystemApi
    public @LeAudioBroadcastState int getBroadcastState() {
        if (DBG) log("getBroadcastState");
        return LE_AUDIO_BROADCAST_STATE_DISABLED;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_ENABLE_ENCRYPTION_FAILED,
            BluetoothStatusCodes.SUCCESS
    })
    public @interface EnableEncryptionReturnValues {}

    /**
     * Enable LE Audio broadcast encryption
     *
     * @hide
     */
    @SystemApi
    public @EnableEncryptionReturnValues int enableEncryption() {
        if (DBG) log("enableEncryption");
        return BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_ENABLE_ENCRYPTION_FAILED;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_DISABLE_ENCRYPTION_FAILED,
            BluetoothStatusCodes.SUCCESS
    })
    public @interface DisableEncryptionReturnValues {}

    /**
     * Disable LE Audio broadcast encryption
     *
     * @param removeExisting true, if the existing key should be removed false, otherwise
     * @hide
     */
    @SystemApi
    public @DisableEncryptionReturnValues int disableEncryption(boolean removeExisting) {
        if (DBG) log("disableEncryption removeExisting=" + removeExisting);
        return BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_DISABLE_ENCRYPTION_FAILED;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_SET_BROADCAST_CODE_FAILED,
            BluetoothStatusCodes.SUCCESS
    })
    public @interface SetBroadcastCodeReturnValues {}

    /**
     * Set the broadcast code
     *
     * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
     * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
     * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
     * If the provided string is non-null and does not meet the above requirements, an error
     * will be returned.
     *
     * @param code if non-null, use the provided broadcast, generate a new code if null
     * @hide
     */
    @SystemApi
    public @SetBroadcastCodeReturnValues int setBroadcastCode(@Nullable String code) {
        if (DBG) log("setBroadcastCode code=" + code);
        return BluetoothStatusCodes.ERROR_LE_BROADCAST_SOURCE_SET_BROADCAST_CODE_FAILED;
    }

    /**
     * Get the that was set before
     *
     * @return encryption key as a byte array or null if no encryption key was set
     * @hide
     */
    @SystemApi
    public @Nullable String getBroadcastCode() {
        if (DBG) log("getBroadcastCode");
        return null;
    }

    /**
     * Get the {@link BluetoothLeBroadcastGroup} information needed to setup Broadcast Sink
     *
     * @return {@link BluetoothLeBroadcastGroup} information
     * @hide
     */
    @SystemApi
    public BluetoothLeBroadcastGroup getBroadcastGroup() {
        return null;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
