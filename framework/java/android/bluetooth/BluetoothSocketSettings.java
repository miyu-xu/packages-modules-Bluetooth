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
 * The {@link BluetoothSocketSettings} are passed to {@link
 * BluetoothAdapter#listenUsingSocketSettings} and {@link BluetoothDevice#createUsingSocketSettings}
 * to define the parameters for the Bluetooth Server and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {

    private static final int L2CAP_PSM_UNSPECIFIED = -1;

    /** Type of the socket */
    @SocketType private int mSocketType;

    /** Encryption requirement for socket. */
    private boolean mEncryptionRequired;

    /** Authentication requirement for socket. */
    private boolean mAuthenticationRequired;

    /** L2CAP Protocol/Service Multiplexer (PSM). */
    private int mL2capPsm;

    /** RFCOMM Service name for SDP record. */
    private String mRfcommServiceName;

    /** RFCOMM Service UUID for the SDP Record. */
    private UUID mRfcommUuid;

    /** Returns the type of the Bluetooth socket. */
    @RequiresNoPermission
    @SocketType
    public int getSocketType() {
        return mSocketType;
    }

    /** Returns the L2CAP PSM value used for a BluetoothSocket#TYPE_LE socket. */
    @RequiresNoPermission
    public int getL2capPsm() {
        return mL2capPsm;
    }

    /** Returns the RFCOMM service name used for a BluetoothSocket#TYPE_RFCOMM socket. */
    @NonNull
    @RequiresNoPermission
    public String getRfcommServiceName() {
        return mRfcommServiceName;
    }

    /** Returns the RFCOMM service UUID used for a BluetoothSocket#TYPE_RFCOMM socket. */
    @Nullable
    @RequiresNoPermission
    public UUID getRfcommUuid() {
        return mRfcommUuid;
    }

    /** Checks if encryption is enabled for the Bluetooth socket. */
    @RequiresNoPermission
    public boolean isEncryptionRequired() {
        return mEncryptionRequired;
    }

    /** Checks if authentication is enabled for the Bluetooth socket. */
    @RequiresNoPermission
    public boolean isAuthenticationRequired() {
        return mAuthenticationRequired;
    }

    /**
     * Returns a {@link String} that describes each BluetoothSocketSettings parameter current value.
     */
    @Override
    public String toString() {
        if (mSocketType == BluetoothSocket.TYPE_RFCOMM) {
            return "BluetoothSocketSettings{"
                    + "mSocketType="
                    + mSocketType
                    + ", mEncryptionRequired="
                    + mEncryptionRequired
                    + ", mAuthenticationRequired="
                    + mAuthenticationRequired
                    + ", mRfcommServiceName="
                    + mRfcommServiceName
                    + ", mRfcommUuid="
                    + mRfcommUuid
                    + "}";
        } else {
            return "BluetoothSocketSettings{"
                    + "mSocketType="
                    + mSocketType
                    + ", mL2capPsm="
                    + mL2capPsm
                    + ", mEncryptionRequired="
                    + mEncryptionRequired
                    + ", mAuthenticationRequired="
                    + mAuthenticationRequired
                    + "}";
        }
    }

    private BluetoothSocketSettings(
            int socketType,
            int l2capPsm,
            boolean encryptionRequired,
            boolean authenticationRequired,
            String rfcommServiceName,
            UUID rfcommUuid) {
        mSocketType = socketType;
        mL2capPsm = l2capPsm;
        mEncryptionRequired = encryptionRequired;
        mAuthenticationRequired = authenticationRequired;
        mRfcommUuid = rfcommUuid;
        mRfcommServiceName = rfcommServiceName;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    @FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mL2capPsm = L2CAP_PSM_UNSPECIFIED;
        private boolean mEncryptionRequired = false;
        private boolean mAuthenticationRequired = false;
        private String mRfcommServiceName = null;
        private UUID mRfcommUuid = null;

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
         * Set the L2CAP PSM for Bluetooth connection. This is used only for the sockets of type
         * BluetoothSocket#TYPE_LE when establishing connection to remote server
         *
         * @param l2capPsm channel for Bluetooth connection.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setL2capPsm(int l2capPsm) {
            mL2capPsm = l2capPsm;
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
        public Builder setEncryptionRequired(boolean encryptionRequired) {
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
        public Builder setAuthenticationRequired(boolean authenticationRequired) {
            mAuthenticationRequired = authenticationRequired;
            return this;
        }

        /**
         * Set the RFCOMM Service name for SDP.
         *
         * @param rfcommServiceName service name for SDP record
         */
        @NonNull
        @RequiresNoPermission
        public Builder setRfcommServiceName(@NonNull String rfcommServiceName) {
            mRfcommServiceName = rfcommServiceName;
            return this;
        }

        /**
         * Set the RFCOMM service UUID for SDP.
         *
         * @param rfcommUuid uuid for SDP record
         */
        @NonNull
        @RequiresNoPermission
        public Builder setRfcommUuid(@NonNull UUID rfcommUuid) {
            mRfcommUuid = rfcommUuid;
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
                    mL2capPsm,
                    mEncryptionRequired,
                    mAuthenticationRequired,
                    mRfcommServiceName,
                    mRfcommUuid);
        }
    }
}
