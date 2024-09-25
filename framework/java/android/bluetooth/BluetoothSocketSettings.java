/*
 * Copyright 2024 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.SystemApi;

import com.android.bluetooth.flags.Flags;

import java.util.UUID;

/**
 * The {@link BluetoothSocketSettings} are passed to {@link BluetoothAdapter#createListeningChannel}
 * and {@link BluetoothDevice#createListeningChannel} to define the parameters for the Bluetooth
 * Server and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
public final class BluetoothSocketSettings {

    /** For Sockets with NO security requirements */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_0 = 0;

    /** For Sockets with only ENCRYPTION as requirement */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_1 = 1;

    /**
     * For Sockets with both ENCRYPTION and AUTHENTICATION as requirement This ensures no MITM
     * attack
     */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_2 = 2;

    /** Non-offload data path where app's socket data flows through the Bluetooth stack. */
    public static final int DATA_PATH_OFFLOAD_OFF = 0;

    /** Software offload data path where app's socket data flows through the vendor process. */
    public static final int DATA_PATH_SOFTWARE_OFFLOAD = 1;

    /** Hardware offload data path where app's socket data flows through the low power processor. */
    public static final int DATA_PATH_HARDWARE_OFFLOAD = 2;

    /**
     * Type of the socket, {@link BluetoothSocket#TYPE_RFCOMM}, {@link BluetoothSocket#TYPE_L2CAP},
     * or {@link BluetoothSocket#TYPE_LE}
     */
    private int mSocketType;

    /** RFCOMM channel or L2CAP psm. */
    private int mChannel;

    /** Desired Bluetooth security level. */
    private int mSecurityLevel;

    /** Service name for SDP record. */
    private String mServiceName;

    /** Service Uuid for the Sdp Record. */
    private UUID mUuid;

    /** Socket data path. */
    private int mDataPath;

    /** Descriptive socket name. */
    private String mSocketName;

    /** The ID of the Hub to which the end point belongs. */
    private int mHubId;

    /** The ID of the Hub end point. */
    private int mEndPointId;

    /**
     * Flag to indicate if data path will be switched to offload end point when connection is
     * completed.
     */
    private boolean mAutoDataPathSwitch;

    /**
     * Return the bluetooth socket type this socket will be created for.
     *
     * @return bluetooth socket type
     */
    @RequiresNoPermission
    public int getSocketType() {
        return mSocketType;
    }

    /**
     * Return RFCOMM channel or L2CAP psm on which this socket will be created.
     *
     * @return RFCOMM channel or L2CAP psm
     */
    @RequiresNoPermission
    public int getChannel() {
        return mChannel;
    }

    /**
     * Return the desired security level at which this socket will be created.
     *
     * @return the security level
     */
    @RequiresNoPermission
    public int getSecurityLevel() {
        return mSecurityLevel;
    }

    /**
     * Return the service name for SDP.
     *
     * @return the service name
     */
    @NonNull
    @RequiresNoPermission
    public String getServiceName() {
        return mServiceName;
    }

    /**
     * Return the service UUID for SDP.
     *
     * @return the service UUID
     */
    @Nullable
    @RequiresNoPermission
    public UUID getUuid() {
        return mUuid;
    }

    /**
     * Get the socket data path.
     *
     * @return the socket data path
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresNoPermission
    public int getDataPath() {
        return mDataPath;
    }

    /**
     * Get the descriptive socket name.
     *
     * @return the socket name
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresNoPermission
    public String getSocketName() {
        return mSocketName;
    }

    /**
     * Get the hub ID.
     *
     * @return The ID of the Hub to which the end point belongs
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresNoPermission
    public int getHubId() {
        return mHubId;
    }

    /**
     * Get the hub end point ID.
     *
     * @return The ID of the Hub end point
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresNoPermission
    public int getEndPointId() {
        return mEndPointId;
    }

    /**
     * Check if the auto data path switching flag is enabled.
     *
     * @return true, if the flag is enabled
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresNoPermission
    public boolean isAutoDataPathSwitch() {
        return mAutoDataPathSwitch;
    }

    private BluetoothSocketSettings(
            int socketType,
            int channel,
            int securityLevel,
            String serviceName,
            UUID uuid,
            int dataPath,
            String socketName,
            int hubId,
            int endPointId,
            boolean autoSwitch) {
        mSocketType = socketType;
        mChannel = channel;
        mSecurityLevel = securityLevel;
        mUuid = uuid;
        mServiceName = serviceName;
        mDataPath = dataPath;
        mSocketName = socketName;
        mHubId = hubId;
        mEndPointId = endPointId;
        mAutoDataPathSwitch = autoSwitch;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mChannel = 0;
        private int mSecurityLevel = BLUETOOTH_SOCKET_SECURITY_LEVEL_0;
        private String mServiceName = "DEF_SERVICE_NAME";
        private UUID mUuid = null;
        private int mDataPath = DATA_PATH_OFFLOAD_OFF;
        private String mSocketName = "DEF_SOCKET_NAME";
        private int mHubId = -1;
        private int mEndPointId = -1;
        private boolean mAutoDataPathSwitch = true;

        /**
         * Set socket Type. This can be of type BluetoothDevice.TYPE_RFCOMM,
         * BluetoothDevice.TYPE_L2CAP or BluetoothDevice.TYPE_LE
         *
         * @param socketType type of socket, one of {@link BluetoothSocket#TYPE_RFCOMM}, {@link
         *     BluetoothSocket#TYPE_L2CAP}
         * @throws IllegalArgumentException If the {@code socketType} is invalid.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setSocketType(int socketType) {
            if (socketType != BluetoothSocket.TYPE_RFCOMM
                    && socketType != BluetoothSocket.TYPE_L2CAP
                    && socketType != BluetoothSocket.TYPE_L2CAP_LE) {
                throw new IllegalArgumentException("invalid socketType - " + socketType);
            }
            mSocketType = socketType;
            return this;
        }

        /**
         * Set the channel for Bluetooth connection. This can serve as either RFCOMM channel or
         * L2CAP PSM
         *
         * @param channel channel for Bluetooth connection.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setChannel(int channel) {
            mChannel = channel;
            return this;
        }

        /**
         * Set Security level for Bluetoth connection.
         *
         * @param securityLevel desired security level, could be either {@link
         *     BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_0} or {@link
         *     BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_1} Or {@link
         *     BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_2}
         * @return @Nonnull
         * @throws IllegalArgumentException If the {@code securityLevel} is invalid.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setSecurityLevel(int securityLevel) {
            if (securityLevel < BLUETOOTH_SOCKET_SECURITY_LEVEL_0
                    || securityLevel > BLUETOOTH_SOCKET_SECURITY_LEVEL_2) {
                throw new IllegalArgumentException("invalid securityLevel - " + securityLevel);
            }
            mSecurityLevel = securityLevel;
            return this;
        }

        /**
         * Set the Service name for SDP.
         *
         * @param serviceName service name for SDP record
         */
        @NonNull
        @RequiresNoPermission
        public Builder setServiceName(@NonNull String serviceName) {
            mServiceName = serviceName;
            return this;
        }

        /**
         * Set the service UUID for SDP.
         *
         * @param uuid uuid for SDP record
         */
        @NonNull
        @RequiresNoPermission
        public Builder setUuid(@NonNull UUID uuid) {
            mUuid = uuid;
            return this;
        }

        /**
         * Set the socket data path. If used to set data path anything other than {@link
         * BluetoothSocketSettings#DATA_PATH_OFFLOAD_OFF}, then it will require BLUETOOTH_PRIVILEGED
         * permission and will be checked at the time of creating socket connection or channel.
         *
         * @param dataPath The socket data path
         * @throws IllegalArgumentException If the {@code dataPath} is invalid.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setDataPath(int dataPath) {
            if (dataPath < DATA_PATH_OFFLOAD_OFF || dataPath > DATA_PATH_HARDWARE_OFFLOAD) {
                throw new IllegalArgumentException("invalid dataPath - " + dataPath);
            }
            mDataPath = dataPath;
            return this;
        }

        /**
         * Set the socket name.
         *
         * @param socketName The descriptive socket name
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setSocketName(@NonNull String socketName) {
            mSocketName = socketName;
            return this;
        }

        /**
         * Set the hub ID.
         *
         * @param hubId The ID of the Hub to which the end point belongs.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setHubId(int hubId) {
            mHubId = hubId;
            return this;
        }

        /**
         * Set the hub end point ID.
         *
         * @param endPointId The ID of the Hub end point.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setEndPointId(int endPointId) {
            mEndPointId = endPointId;
            return this;
        }

        /**
         * Set if the auto data path switch option is enabled.
         *
         * @param autoSwitch If the auto data path switch option is enabled
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setAutoDataPathSwitch(boolean autoSwitch) {
            mAutoDataPathSwitch = autoSwitch;
            return this;
        }

        /**
         * Build {@link BluetoothSocketSettings}.
         *
         * @throws IllegalArgumentException if the settings cannot be built.
         */
        @NonNull
        @RequiresNoPermission
        public BluetoothSocketSettings build() {
            if (mSocketType == BluetoothSocket.TYPE_RFCOMM
                    && mUuid == null
                    && mChannel != BluetoothAdapter.SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
                if (mChannel < 1 || mChannel > BluetoothSocket.MAX_RFCOMM_CHANNEL) {
                    throw new IllegalArgumentException("Invalid RFCOMM channel: " + mChannel);
                }
            }
            if (mDataPath == DATA_PATH_HARDWARE_OFFLOAD && (mHubId == -1 || mEndPointId == -1)) {
                throw new IllegalArgumentException(
                        "Hub ID and end point ID should be set for hardware offload mode");
            }
            return new BluetoothSocketSettings(
                    mSocketType,
                    mChannel,
                    mSecurityLevel,
                    mServiceName,
                    mUuid,
                    mDataPath,
                    mSocketName,
                    mHubId,
                    mEndPointId,
                    mAutoDataPathSwitch);
        }
    }
}
