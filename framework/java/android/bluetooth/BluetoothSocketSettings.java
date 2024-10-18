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
 * and {@link BluetoothDevice#createClientSocket} to define the parameters for the Bluetooth Server
 * and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_BT_OFFLOAD_SOCKET_API)
//@FlaggedApi(Flags.FLAG_BT_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {

    /** For Sockets with NO security requirements */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_INSECURE = 0;

    /** For Sockets with only ENCRYPTION as requirement */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_ENCRYPTION_NO_AUTHENTICATION = 1;

    /**
     * For Sockets with both ENCRYPTION and AUTHENTICATION as requirement
     */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_ENCRYPTION_WITH_AUTHENTICATION = 2;

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
     * Returns a {@link String} that describes each BluetoothSocketSettings parameter current value.
     */
    @Override
    public String toString() {
        return "BluetoothSocketSettings{"
                + "mSocketType="
                + mSocketType
                + ", mChannel="
                + mChannel
                + ", mSecurityLevel="
                + mSecurityLevel
                + ", mServiceName='"
                + mServiceName
                + '\''
                + ", mUuid="
                + mUuid
                + '\''
                + '}';
    }

    private BluetoothSocketSettings(
            int socketType,
            int channel,
            int securityLevel,
            String serviceName,
            UUID uuid) {
        mSocketType = socketType;
        mChannel = channel;
        mSecurityLevel = securityLevel;
        mUuid = uuid;
        mServiceName = serviceName;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mChannel = -1;
        private int mSecurityLevel = BLUETOOTH_SOCKET_SECURITY_LEVEL_INSECURE;
        private String mServiceName = "DEF_SERVICE_NAME";
        private UUID mUuid = null;

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
         *  BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_INSECURE} or {@link
         *  BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_ENCRYPTION_NO_AUTHENTICATION}
	 *  Or {@link
         *  BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_ENCRYPTION_WITH_AUTHENTICATION}
         * @return @Nonnull
         * @throws IllegalArgumentException If the {@code securityLevel} is invalid.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setSecurityLevel(int securityLevel) {
            if (securityLevel < BLUETOOTH_SOCKET_SECURITY_LEVEL_INSECURE
               || securityLevel > BLUETOOTH_SOCKET_SECURITY_LEVEL_ENCRYPTION_WITH_AUTHENTICATION) {
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
            if (mSocketType == BluetoothSocket.TYPE_L2CAP
                    || mSocketType == BluetoothSocket.TYPE_L2CAP_LE) {
                if (mChannel < 0) {
                    throw new IllegalArgumentException("Invalid L2CAP channel: " + mChannel);
                }
            }
            return new BluetoothSocketSettings(
                    mSocketType,
                    mChannel,
                    mSecurityLevel,
                    mServiceName,
                    mUuid);
        }
    }
}
