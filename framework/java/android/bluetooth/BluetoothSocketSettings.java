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

import static android.bluetooth.BluetoothSocket.SocketType;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.SystemApi;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * The {@link BluetoothSocketSettings} are passed to {@link BluetoothAdapter#createListeningChannel}
 * and {@link BluetoothDevice#createClientSocket} to define the parameters for the Bluetooth Server
 * and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {
    /** @hide */
    @IntDef(
            prefix = {"DATA_PATH_"},
            value = {DATA_PATH_NO_OFFLOAD, DATA_PATH_HW_OFFLOAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SocketDataPath {}

    /** Non-offload data path where app's socket data flows through the Bluetooth host stack. */
    public static final int DATA_PATH_NO_OFFLOAD = 0;

    /** Hardware offload data path where app's socket data flows through the low power processor. */
    public static final int DATA_PATH_HW_OFFLOAD = 1;

    /** L2CAP Minimum packet size */
    public static final int L2CAP_MIN_PACKET_SIZE = 1024;

    /** L2CAP Maximum packet size */
    public static final int L2CAP_MAX_PACKET_SIZE = 65535;

    /**
     * Indicates that the hub ID is invalid.
     *
     * @hide
     */
    public static final long INVALID_HUB_ID = 0;

    /**
     * Indicates that the hub endpoint ID is invalid.
     *
     * @hide
     */
    public static final long INVALID_ENDPOINT_ID = 0;

    /** Type of the socket */
    private int mSocketType;

    /** Encryption requirement for socket. */
    private boolean mEncryptionEnabled;

    /** Authentication requirement for socket. */
    private boolean mAuthenticationEnabled;

    /** L2CAP psm. */
    private int mChannel;

    /** Service name for SDP record. */
    private String mServiceName;

    /** Service Uuid for the Sdp Record. */
    private UUID mUuid;

    /**
     * Socket data path, {@link BluetoothSocketSettings#DATA_PATH_NO_OFFLOAD} or {@link
     * BluetoothSocketSettings#DATA_PATH_HW_OFFLOAD}.
     */
    private int mDataPath;

    /**
     * Descriptive socket name provided by the host app for hardware offload data path {@link
     * BluetoothSocketSettings#DATA_PATH_HW_OFFLOAD}.
     */
    private String mSocketName;

    /**
     * The ID of the Hub to which the end point belongs for hardware offload data path {@link
     * BluetoothSocketSettings#DATA_PATH_HW_OFFLOAD}.
     */
    private long mHubId;

    /**
     * The ID of the Hub endpoint for hardware offload data path {@link
     * BluetoothSocketSettings#DATA_PATH_HW_OFFLOAD}.
     */
    private long mEndpointId;

    /**
     * The L2CAP maximum packet size that can be received for hardware offload data path {@link
     * BluetoothSocketSettings#DATA_PATH_HW_OFFLOAD}. When the socket is connected, check the value
     * actually used for the connection with {@link BluetoothSocket#getMaxReceivePacketSize}.
     */
    private int mL2capMaxRxPacketSize;

    /** Returns the bluetooth socket type this socket will be created for */
    @RequiresNoPermission
    @SocketType
    public int getSocketType() {
        return mSocketType;
    }

    /**
     * Returns L2CAP psm on which socket will be created, this is valid for the socket of type
     * BluetoothSocket#TYPE_LE
     */
    @RequiresNoPermission
    public int getChannel() {
        return mChannel;
    }

    /** Returns the service name for SDP */
    @NonNull
    @RequiresNoPermission
    public String getServiceName() {
        return mServiceName;
    }

    /** Returns the service UUID for SDP */
    @Nullable
    @RequiresNoPermission
    public UUID getUuid() {
        return mUuid;
    }

    /** Returns true if the encryption requirement is set, false otherwise */
    @RequiresNoPermission
    public boolean isEncryptionEnabled() {
        return mEncryptionEnabled;
    }

    /** Returns true if the authentication requirement is set, false otherwise */
    @RequiresNoPermission
    public boolean isAuthenticationEnabled() {
        return mAuthenticationEnabled;
    }

    /**
     * Get the socket data path.
     *
     * @return the socket data path
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @RequiresNoPermission
    public @SocketDataPath int getDataPath() {
        return mDataPath;
    }

    /**
     * Get the descriptive socket name.
     *
     * @return the socket name
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @NonNull
    @RequiresNoPermission
    public String getSocketName() {
        return mSocketName;
    }

    /**
     * Get the Hub ID.
     *
     * @return The ID of the Hub to which the end point belongs
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @RequiresNoPermission
    public long getHubId() {
        if (mDataPath != DATA_PATH_HW_OFFLOAD) {
            return INVALID_HUB_ID;
        }
        return mHubId;
    }

    /**
     * Get the Hub endpoint ID.
     *
     * @return The ID of the Hub endpoint
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @RequiresNoPermission
    public long getEndpointId() {
        if (mDataPath != DATA_PATH_HW_OFFLOAD) {
            return INVALID_ENDPOINT_ID;
        }
        return mEndpointId;
    }

    /**
     * Get the L2CAP maximum packet size that can be received.
     *
     * @return The L2CAP maximum packet size
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @RequiresNoPermission
    public int getL2capMaxRxPacketSize() {
        return mL2capMaxRxPacketSize;
    }

    /**
     * Returns a {@link String} that describes each BluetoothSocketSettings parameter current value.
     */
    @Override
    public String toString() {
        return "BluetoothSocketSettings{"
                + "mSocketType="
                + mSocketType
                + ", mChannel="
                + mChannel
                + ", mEncryptionEnabled="
                + mEncryptionEnabled
                + ", mAuthenticationEnabled="
                + mAuthenticationEnabled
                + ", mServiceName="
                + mServiceName
                + ", mUuid="
                + mUuid
                + ", mDataPath="
                + mDataPath
                + ", mSocketName='"
                + mSocketName
                + ", mHubId="
                + mHubId
                + ", mEndpointId="
                + mEndpointId
                + ", mL2capMaxRxPacketSize="
                + mL2capMaxRxPacketSize
                + "}";
    }

    private BluetoothSocketSettings(
            int socketType,
            int channel,
            boolean encryption,
            boolean authentication,
            String serviceName,
            UUID uuid,
            int dataPath,
            String socketName,
            long hubId,
            long endpointId,
            int l2capMaxRxPacketSize) {
        mSocketType = socketType;
        mChannel = channel;
        mEncryptionEnabled = encryption;
        mAuthenticationEnabled = authentication;
        mUuid = uuid;
        mServiceName = serviceName;
        mDataPath = dataPath;
        mSocketName = socketName;
        mHubId = hubId;
        mEndpointId = endpointId;
        mL2capMaxRxPacketSize = l2capMaxRxPacketSize;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mChannel = -1;
        private boolean mEncryptionEnabled = false;
        private boolean mAuthenticationEnabled = false;
        private String mServiceName = "DEF_SERVICE_NAME";
        private UUID mUuid = null;
        private int mDataPath = DATA_PATH_NO_OFFLOAD;
        private String mSocketName = "DEF_SOCKET_NAME";
        private long mHubId = INVALID_HUB_ID;
        private long mEndpointId = INVALID_ENDPOINT_ID;
        private int mL2capMaxRxPacketSize = L2CAP_MAX_PACKET_SIZE;

        /**
         * Set socket Type.
         *
         * <p>This API supports BluetoothSocket#TYPE_RFCOMM and BluetoothSocket#TYPE_LE only.
         *
         * @param socketType type of socket
         * @throws IllegalArgumentException If the {@code socketType} is invalid.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setSocketType(@SocketType int socketType) {
            if (socketType != BluetoothSocket.TYPE_RFCOMM
                    && socketType != BluetoothSocket.TYPE_LE) {
                throw new IllegalArgumentException("invalid socketType - " + socketType);
            }
            mSocketType = socketType;
            return this;
        }

        /**
         * Set the channel for Bluetooth connection. This can serve as L2CAP PSM. This is used only
         * for the sockets of type BluetoothSocket#TYPE_LE.
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
         * Set Encryption requirement for Bluetoth connection.
         *
         * @param encryptionEnabled true if encryption is needed for this socket false otherwise
         * @return BluetoothSocketSettings.Builder object
         */
        @NonNull
        @RequiresNoPermission
        public Builder setEncryptionEnabled(boolean encryptionEnabled) {
            mEncryptionEnabled = encryptionEnabled;
            return this;
        }

        /**
         * Set Authentication requirement for Bluetoth connection.
         *
         * @param authenticationEnabled true if authentication is needed for this socket false
         *     otherwise
         * @return BluetoothSocketSettings.Builder object
         */
        @NonNull
        @RequiresNoPermission
        public Builder setAuthenticationEnabled(boolean authenticationEnabled) {
            mAuthenticationEnabled = authenticationEnabled;
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
         * BluetoothSocketSettings#DATA_PATH_NO_OFFLOAD}, then it will require BLUETOOTH_PRIVILEGED
         * permission and will be checked at the time of creating socket connection or channel.
         *
         * @param dataPath The socket data path
         * @throws IllegalArgumentException If the {@code dataPath} is invalid.
         * @hide
         */
        @SystemApi
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setDataPath(@SocketDataPath int dataPath) {
            if (dataPath < DATA_PATH_NO_OFFLOAD || dataPath > DATA_PATH_HW_OFFLOAD) {
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
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setSocketName(@NonNull String socketName) {
            mSocketName = socketName;
            return this;
        }

        /**
         * Set the Hub ID.
         *
         * @param hubId The ID of the Hub to which the end point belongs.
         * @hide
         */
        @SystemApi
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setHubId(long hubId) {
            mHubId = hubId;
            return this;
        }

        /**
         * Set the Hub endpoint ID.
         *
         * @param endpointId The ID of the Hub endpoint.
         * @hide
         */
        @SystemApi
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setEndpointId(long endpointId) {
            mEndpointId = endpointId;
            return this;
        }

        /**
         * Set the L2CAP maximum packet size that can be received for hardware offload data path.
         *
         * @param packetSize L2CAP maximum packet size
         * @throws IllegalArgumentException If the {@code packetSize} is invalid.
         * @hide
         */
        @SystemApi
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setL2capMaxRxPacketSize(int packetSize) {
            if (packetSize < L2CAP_MIN_PACKET_SIZE || packetSize > L2CAP_MAX_PACKET_SIZE) {
                throw new IllegalArgumentException("invalid packetSize " + packetSize);
            }
            mL2capMaxRxPacketSize = packetSize;
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
            if (mDataPath == DATA_PATH_HW_OFFLOAD
                    && (mHubId == INVALID_HUB_ID || mEndpointId == INVALID_ENDPOINT_ID)) {
                throw new IllegalArgumentException(
                        "Hub ID and endpoint ID should be set for hardware offload mode");
            }

            return new BluetoothSocketSettings(
                    mSocketType,
                    mChannel,
                    mEncryptionEnabled,
                    mAuthenticationEnabled,
                    mServiceName,
                    mUuid,
                    mDataPath,
                    mSocketName,
                    mHubId,
                    mEndpointId,
                    mL2capMaxRxPacketSize);
        }
    }
}
