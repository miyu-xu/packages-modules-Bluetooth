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
 * This class provides the public APIs to control the BAP Broadcast Source profile.
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
     * Interface for receiving events related to Broadcast Source
     * @hide
     */
    @SystemApi
    public interface Callback {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                REASON_UNKNOWN,
                REASON_LOCAL_APP_REQUEST,
                REASON_SYSTEM_POLICY,
                REASON_HARDWARE_ERROR,
                REASON_INVALID_CODE,
                REASON_ALREADY_BROADCASTING,
                REASON_ALREADY_ENCRYPTED,
                REASON_BROADCAST_ALREADY_STOPPED,
                REASON_ENCRYPTION_ALREADY_DISABLED,
        })
        @interface Reason {}

        /**
         * Indicates that the callback happened due to unknown reason
         * @hide
         */
        @SystemApi
        int REASON_UNKNOWN = 0;

        /**
         * Indicates that some local application caused the change
         * @hide
         */
        @SystemApi
        int REASON_LOCAL_APP_REQUEST = 1;

        /**
         * Indicates that the local system policy caused the change, such
         * as privacy policy, power management policy, permissions, and more.
         * @hide
         */
        @SystemApi
        int REASON_SYSTEM_POLICY = 2;

        /**
         * Indicates that the underlying hardware incurred some error when processing this request,
         * maybe try again later or toggle the hardware state
         * @hide
         */
        @SystemApi
        int REASON_HARDWARE_ERROR = 3;

        /**
         * Indicates that encryption code entered is not valid
         * @hide
         */
        @SystemApi
        int REASON_INVALID_CODE = 4;

        /**
         * Indicates that system is already broadcasting. In encryption context, please stop
         * broadcasting before changing encryption state
         * @hide
         */
        @SystemApi
        int REASON_ALREADY_BROADCASTING = 5;

        /**
         * Indicates that encryption is already enabled and cannot be enabled with a different code
         * @hide
         */
        @SystemApi
        int REASON_ALREADY_ENCRYPTED = 6;

        /**
         * Indicates that broadcast has already stopped before trying to stop it again
         * @hide
         */
        @SystemApi
        int REASON_BROADCAST_ALREADY_STOPPED = 7;

        /**
         * Indicates that encryption is already disabled before trying to disable it again
         * @hide
         */
        @SystemApi
        int REASON_ENCRYPTION_ALREADY_DISABLED = 8;

        /**
         * Callback invoked when broadcast is started, but audio may not be playing.
         *
         * @param reason for broadcast start
         * @hide
         */
        @SystemApi
        void onBroadcastStarted(@Reason int reason);

        /**
         * Callback invoked when broadcast failed to start
         *
         * @param reason for broadcast start failure
         * @hide
         */
        @SystemApi
        void onBroadcastStartFailed(@Reason int reason);

        /**
         * Callback invoked when broadcast is stopped
         *
         * @param reason for broadcast stop
         * @hide
         */
        @SystemApi
        void onBroadcastStopped(@Reason int reason);

        /**
         * Callback invoked when broadcast failed to stop
         *
         * @param reason for broadcast stop failure
         * @hide
         */
        @SystemApi
        void onBroadcastStopFailed(@Reason int reason);

        /**
         * Callback invoked when broadcast audio is playing
         *
         * @param reason for playback start
         * @hide
         */
        @SystemApi
        void onPlaybackStarted(@Reason int reason);

        /**
         * Callback invoked when broadcast audio is not playing
         *
         * @param reason for playback stop
         * @hide
         */
        @SystemApi
        void onPlaybackStopped(@Reason int reason);

        /**
         * Callback invoked when encryption is enabled
         *
         * @param reason for encryption enable
         * @hide
         */
        @SystemApi
        void onEncryptionEnabled(@Reason int reason);

        /**
         * Callback invoked when broadcast audio is not playing
         *
         * @param reason for encryption enable failure
         * @hide
         */
        @SystemApi
        void onEncryptionEnableFailed(int reason);

        /**
         * Callback invoked when broadcast audio is not playing
         *
         * @param reason for encryption disable
         * @hide
         */
        @SystemApi
        void onEncryptionDisabled(int reason);

        /**
         * Callback invoked when broadcast audio is not playing
         *
         * @param reason for encryption disable failure
         * @hide
         */
        @SystemApi
        void onEncryptionDisableFailed(int reason);

        /**
         * Callback invoked when Broadcast Source metadata is updated
         *
         * @param metadata updated Broadcast Source metadata
         * @hide
         */
        @SystemApi
        void onBroadcastMetadataChanged(BluetoothLeBroadcastMetadata metadata);
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
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public int getConnectionState(@NonNull BluetoothDevice device) {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @hide
     */
    @NonNull
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @hide
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
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
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
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
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void unregisterCallback(@NonNull Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        log("unregisterCallback");
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Start broadcasting to nearby devices using current encryption setting and system audio and
     * media policy settings
     *
     * On success, {@link Callback#onBroadcastStarted(int)} will be invoked with
     * {@link Callback#REASON_LOCAL_APP_REQUEST} reason code.
     * On failure, {@link Callback#onBroadcastStartFailed(int)} will be invoked  with reason code.
     *
     * After broadcast is started,
     * {@link Callback#onBroadcastMetadataChanged(BluetoothLeBroadcastMetadata)}
     * will be invoked to expose the latest Broadcast Group metadata that can be shared out of band
     * to set up Broadcast Sink without scanning.
     *
     * Alternatively, one can also get the latest Broadcast Source meta via
     * {@link #getBroadcastMetadata()}
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void startBroadcasting() {
        if (DBG) log("startBroadcasting");
    }

    /**
     * Stop broadcasting.
     *
     * On success, {@link Callback#onBroadcastStopped(int)} will be invoked with reason code
     * {@link Callback#REASON_LOCAL_APP_REQUEST}
     * On failure, {@link Callback#onBroadcastStopFailed(int)} will be invoked with reason code
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void stopBroadcasting() {
        if (DBG) log("disableBroadcastMode");
    }

    /**
     * Return true if broadcasting is enabled
     *
     * @return true if broadcasting is enabled
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean isBroadcasting() {
        if (DBG) log("getBroadcastState");
        return false;
    }

    /**
     * Return true if audio is being broadcasted
     *
     * @return true if audio is being broadcasted
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean isPlaying() {
        return false;
    }

    /**
     * Enable encryption with a Broadcast Code
     *
     * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
     * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
     * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
     *
     * If the provided string is non-null and does not meet the above requirements, encryption will
     * fail to enable with reason code {@link Callback#REASON_INVALID_CODE}
     *
     * On success, {@link Callback#onEncryptionEnabled(int)} will be invoked with reason code
     * {@link Callback#REASON_LOCAL_APP_REQUEST}.
     * On failure, {@link Callback#onEncryptionEnableFailed(int)} will be invoked with reason code.
     *
     * @param customizedCode if non-null, use the provided broadcast, generate a new code if null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void enableEncryption(@Nullable byte[] customizedCode) {
        if (DBG) log("enableEncryptionWithCode code=" + customizedCode);
    }

    /**
     * Disable LE Audio broadcast encryption and clear the broadcast code
     *
     * On success, {@link Callback#onEncryptionDisabled(int)} will be invoked with reason code
     * {@link Callback#REASON_LOCAL_APP_REQUEST}.
     * On failure, {@link Callback#onEncryptionDisableFailed(int)} will be invoked with reason code.
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void disableEncryption() {
        if (DBG) log("disableEncryption");
    }

    /**
     * Return true if encryption is currently enabled
     *
     * @return true if encryption is currently enabled
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean isEncryptionEnabled() {
        return false;
    }

    /**
     * Get the that was set before
     *
     * @return encryption key as a byte array or null if encryption is disabled
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @Nullable byte[] getBroadcastCode() {
        if (DBG) log("getBroadcastCode");
        return null;
    }

    /**
     * Get the {@link BluetoothLeBroadcastMetadata} information needed to set up Broadcast Sink
     *
     * @return {@link BluetoothLeBroadcastMetadata} information
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public BluetoothLeBroadcastMetadata getBroadcastMetadata() {
        return null;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
