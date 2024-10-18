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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;

import com.android.bluetooth.flags.Flags;

import java.util.UUID;

/**
 * The {@link BluetoothSocketSettings} are passed to {@link BluetoothAdapter#createListeningChannel}
 * and {@link BluetoothDevice#createClientSocket} to define the parameters for the Bluetooth Server
 * and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {
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
                + "}";
    }

    private BluetoothSocketSettings(
            int socketType,
            int channel,
            boolean encryption,
            boolean authentication,
            String serviceName,
            UUID uuid) {
        mSocketType = socketType;
        mChannel = channel;
        mEncryptionEnabled = encryption;
        mAuthenticationEnabled = authentication;
        mUuid = uuid;
        mServiceName = serviceName;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    @FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mChannel = -1;
        private boolean mEncryptionEnabled = false;
        private boolean mAuthenticationEnabled = false;
        private String mServiceName = "DEF_SERVICE_NAME";
        private UUID mUuid = null;

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
         * Build {@link BluetoothSocketSettings}.
         *
         * @throws IllegalArgumentException if the settings cannot be built.
         */
        @NonNull
        @RequiresNoPermission
        public BluetoothSocketSettings build() {
            return new BluetoothSocketSettings(
                    mSocketType,
                    mChannel,
                    mEncryptionEnabled,
                    mAuthenticationEnabled,
                    mServiceName,
                    mUuid);
        }
    }
}
