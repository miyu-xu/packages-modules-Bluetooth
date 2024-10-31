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

    private static final int L2CAP_PSM_UNSPECIFIED = -1;
    private static final String DEFAULT_RFCOMM_SERVICE_NAME = "DEF_RFC_SERVICE_NAME";
    private static final String DEFAULT_SOCKET_NAME = "DEF_SOCKET_NAME";

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

    /** Maximum packet size for {@link #DATA_PATH_HW_OFFLOAD}. */
    private static final int MAX_PACKET_SIZE = 65535;

    /**
     * Indicates that the hub ID is invalid.
     *
     * @hide
     */
    private static final long INVALID_HUB_ID = 0;

    /**
     * Indicates that the hub endpoint ID is invalid.
     *
     * @hide
     */
    private static final long INVALID_ENDPOINT_ID = 0;

    /** Type of the socket */
    @SocketType private int mSocketType;

    /** Encryption requirement for socket. */
    private boolean mEncryptionRequired;

    /** Authentication requirement for socket. */
    private boolean mAuthenticationRequired;

    /** L2CAP Protocol/Service Multiplexer (PSM). */
    private int mL2capPsm;

    /** Service name for SDP record. */
    private String mRfcommServiceName;

    /** Service UUID for the SDP Record. */
    private UUID mRfcommUuid;

    /** Socket data path, {@link #DATA_PATH_NO_OFFLOAD} or {@link #DATA_PATH_HW_OFFLOAD}. */
    @SocketDataPath private int mDataPath;

    /** Descriptive socket name provided by the host app. */
    private String mSocketName;

    /** The ID of the Hub to which the endpoint belongs for {@link #DATA_PATH_HW_OFFLOAD}. */
    private long mHubId;

    /** The ID of the Hub endpoint for hardware offload data path {@link #DATA_PATH_HW_OFFLOAD}. */
    private long mEndpointId;

    /**
     * The maximum packet size of {@code socketType} that can be received from endpoint for {@link
     * #DATA_PATH_HW_OFFLOAD}.
     */
    private int mMaximumPacketSize;

    /** Returns the type of the Bluetooth socket. */
    @RequiresNoPermission
    @SocketType
    public int getSocketType() {
        return mSocketType;
    }

    /** Returns the L2CAP PSM value used for a BluetoothSocket#TYPE_LE socket. */
    @RequiresNoPermission
    public int getChannel() {
        return mL2capPsm;
    }

    /** Returns the service name used for a BluetoothSocket#TYPE_RFCOMM socket. */
    @NonNull
    @RequiresNoPermission
    public String getServiceName() {
        return mRfcommServiceName;
    }

    /** Returns the service UUID used for a BluetoothSocket#TYPE_RFCOMM socket. */
    @Nullable
    @RequiresNoPermission
    public UUID getUuid() {
        return mRfcommUuid;
    }

    /** Checks if encryption is enabled for the Bluetooth socket. */
    @RequiresNoPermission
    public boolean isEncryptionEnabled() {
        return mEncryptionRequired;
    }

    /** Checks if authentication is enabled for the Bluetooth socket. */
    @RequiresNoPermission
    public boolean isAuthenticationEnabled() {
        return mAuthenticationRequired;
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
     */
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @NonNull
    @RequiresNoPermission
    public String getSocketName() {
        return mSocketName;
    }

    /**
     * Get the Hub ID for {@link #DATA_PATH_HW_OFFLOAD}.
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
     * Get the Hub endpoint ID for {@link #DATA_PATH_HW_OFFLOAD}.
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
     * Get the maximum packet size of {@code socketType} that can be received from endpoint for
     * {@link #DATA_PATH_HW_OFFLOAD}.
     *
     * @return The maximum packet size
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
    @RequiresNoPermission
    public int getMaximumPacketSize() {
        return mMaximumPacketSize;
    }

    /**
     * Returns a {@link String} that describes each BluetoothSocketSettings parameter current value.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("BluetoothSocketSettings{");
        builder.append("mSocketType=");
        builder.append(mSocketType);
        builder.append(", mEncryptionRequired=");
        builder.append(mEncryptionRequired);
        builder.append(", mAuthenticationRequired=");
        builder.append(mAuthenticationRequired);
        builder.append(", mSocketName=");
        builder.append(mSocketName);
        if (mSocketType == BluetoothSocket.TYPE_RFCOMM) {
            builder.append(", mRfcommServiceName=");
            builder.append(mRfcommServiceName);
            builder.append(", mRfcommUuid=");
            builder.append(mRfcommUuid);
        } else {
            builder.append(", mL2capPsm=");
            builder.append(mL2capPsm);
        }
        if (mDataPath == DATA_PATH_HW_OFFLOAD) {
            builder.append(", mDataPath=");
            builder.append(mDataPath);
            builder.append(", mHubId=");
            builder.append(mHubId);
            builder.append(", mEndpointId=");
            builder.append(mEndpointId);
            builder.append(", mMaximumPacketSize=");
            builder.append(mMaximumPacketSize);
        }
        builder.append("}");
        return builder.toString();
    }

    private BluetoothSocketSettings(
            int socketType,
            int channel,
            boolean encryptionRequired,
            boolean authenticationRequired,
            String serviceName,
            UUID uuid,
            int dataPath,
            String socketName,
            long hubId,
            long endpointId,
            int maximumPacketSize) {
        mSocketType = socketType;
        mL2capPsm = channel;
        mEncryptionRequired = encryptionRequired;
        mAuthenticationRequired = authenticationRequired;
        mRfcommUuid = uuid;
        mRfcommServiceName = serviceName;
        mDataPath = dataPath;
        mSocketName = socketName;
        mHubId = hubId;
        mEndpointId = endpointId;
        mMaximumPacketSize = maximumPacketSize;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    @FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mL2capPsm = L2CAP_PSM_UNSPECIFIED;
        private boolean mEncryptionRequired = false;
        private boolean mAuthenticationRequired = false;
        private String mRfcommServiceName = DEFAULT_RFCOMM_SERVICE_NAME;
        private UUID mRfcommUuid = null;
        private int mDataPath = DATA_PATH_NO_OFFLOAD;
        private String mSocketName = DEFAULT_SOCKET_NAME;
        private long mHubId = INVALID_HUB_ID;
        private long mEndpointId = INVALID_ENDPOINT_ID;
        private int mMaximumPacketSize = MAX_PACKET_SIZE;

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
            mL2capPsm = channel;
            return this;
        }

        /**
         * Set Encryption requirement for Bluetoth connection.
         *
         * @param encryptionRequired true if encryption is needed for this socket false otherwise
         * @return BluetoothSocketSettings.Builder object
         */
        @NonNull
        @RequiresNoPermission
        public Builder setEncryptionEnabled(boolean encryptionRequired) {
            mEncryptionRequired = encryptionRequired;
            return this;
        }

        /**
         * Set Authentication requirement for Bluetoth connection.
         *
         * @param authenticationRequired true if authentication is needed for this socket false
         *     otherwise
         * @return BluetoothSocketSettings.Builder object
         */
        @NonNull
        @RequiresNoPermission
        public Builder setAuthenticationEnabled(boolean authenticationRequired) {
            mAuthenticationRequired = authenticationRequired;
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
            mRfcommServiceName = serviceName;
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
            mRfcommUuid = uuid;
            return this;
        }

        /**
         * Set the socket data path. If used to set data path anything other than {@link
         * #DATA_PATH_NO_OFFLOAD}, then it will require BLUETOOTH_PRIVILEGED permission and will be
         * checked at the time of creating socket connection or channel.
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
         * Set the descriptive socket name.
         *
         * @param socketName The socket name
         */
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setSocketName(@NonNull String socketName) {
            mSocketName = socketName;
            return this;
        }

        /**
         * Set the Hub ID for {@link #DATA_PATH_HW_OFFLOAD}.
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
         * Set the Hub endpoint ID for {@link #DATA_PATH_HW_OFFLOAD}.
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
         * Set the maximum packet size of {@code socketType} that can be received from endpoint for
         * {@link #DATA_PATH_HW_OFFLOAD}. The Bluetooth host stack may update the value considering
         * the capability of endpoint. When the socket is connected, the value actually negotiated
         * with peer device can be retrieved by {@link BluetoothSocket#getMaxReceivePacketSize}.
         *
         * @param packetSize The maximum packet size
         * @hide
         */
        @SystemApi
        @FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
        @NonNull
        @RequiresNoPermission
        public Builder setMaximumPacketSize(int packetSize) {
            mMaximumPacketSize = packetSize;
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
            if (mDataPath == DATA_PATH_HW_OFFLOAD) {
                if (mHubId == INVALID_HUB_ID || mEndpointId == INVALID_ENDPOINT_ID) {
                    throw new IllegalArgumentException(
                            "Hub ID and endpoint ID must be set for hardware data path");
                }
                if (mMaximumPacketSize < 0 || mMaximumPacketSize > MAX_PACKET_SIZE) {
                    throw new IllegalArgumentException("invalid packet size " + mMaximumPacketSize);
                }
            } else {
                if (mHubId != INVALID_HUB_ID || mEndpointId != INVALID_ENDPOINT_ID) {
                    throw new IllegalArgumentException(
                            "Hub ID and endpoint ID may not be set for software data path");
                }
            }

            return new BluetoothSocketSettings(
                    mSocketType,
                    mL2capPsm,
                    mEncryptionRequired,
                    mAuthenticationRequired,
                    mRfcommServiceName,
                    mRfcommUuid,
                    mDataPath,
                    mSocketName,
                    mHubId,
                    mEndpointId,
                    mMaximumPacketSize);
        }
    }
}
