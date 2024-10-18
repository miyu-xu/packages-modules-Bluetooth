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
import android.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import com.android.bluetooth.flags.Flags;

import java.util.UUID;

/**
 * The {@link BluetoothSocketSettings} are passed to {@link BluetoothAdapter#createListeningChannel}
 * and {@link BluetoothDevice#createClientSocket} to define the parameters for the Bluetooth Server
 * and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {
    /** For Sockets with NO security requirements */
    public static final int SOCKET_SEC_LEVEL_INSECURE = 0;

    /** For Sockets with only ENCRYPTION as requirement */
    public static final int SOCKET_SEC_LEVEL_ENCRYPTION_NO_AUTHENTICATION = 1;

    /** For Sockets with both ENCRYPTION and AUTHENTICATION as requirement */
    public static final int SOCKET_SEC_LEVEL_ENCRYPTION_WITH_AUTHENTICATION = 2;

    /** @hide */
    @IntDef(
            prefix = {"SOCKET_SEC_"},
            value = {
                     SOCKET_SEC_LEVEL_INSECURE,
                     SOCKET_SEC_LEVEL_ENCRYPTION_NO_AUTHENTICATION,
                     SOCKET_SEC_LEVEL_ENCRYPTION_WITH_AUTHENTICATION,
                    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SocketSecurityLevel {}

    /** For Sockets on classic RFCOMM transport */
    public static final int SOCKET_TYPE_RFCOMM = 1;

    /** For Sockets on classic L2cap transport */
    public static final int SOCKET_TYPE_L2CAP = 3;

    /** For Sockets on LE COC transport */
    public static final int SOCKET_TYPE_LE = 4;

    /** @hide */
    @IntDef(
            prefix = {"SOCKET_TYPE_"},
            value = {
                     SOCKET_TYPE_RFCOMM,
                     SOCKET_TYPE_L2CAP,
                     SOCKET_TYPE_LE,
                    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SocketType {}
    /**
     * Type of the socket
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

    /** Returns the bluetooth socket type this socket will be created for*/
    @RequiresNoPermission
    @SocketType
    public int getSocketType() {
        return mSocketType;
    }

    /** Returns the RFCOMM channel or L2CAP psm on which socket will be created*/
    @RequiresNoPermission
    public int getChannel() {
        return mChannel;
    }

    /** Returns the current security level of the socket*/
    @RequiresNoPermission
    @SocketSecurityLevel
    public int getSecurityLevel() {
        return mSecurityLevel;
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

    /*package*/boolean isEncrypted() {
        boolean encrypted = false;
        switch (mSecurityLevel) {
            case SOCKET_SEC_LEVEL_INSECURE:
                encrypted = false;
                break;
            case SOCKET_SEC_LEVEL_ENCRYPTION_NO_AUTHENTICATION:
            case SOCKET_SEC_LEVEL_ENCRYPTION_WITH_AUTHENTICATION:
                encrypted = true;
                break;
        }
        return encrypted;
    }

    /*package*/boolean isAuthenticated() {
        boolean authenticated = false;
        switch (mSecurityLevel) {
            case SOCKET_SEC_LEVEL_INSECURE:
            case SOCKET_SEC_LEVEL_ENCRYPTION_NO_AUTHENTICATION:
                authenticated = false;
                break;
            case SOCKET_SEC_LEVEL_ENCRYPTION_WITH_AUTHENTICATION:
                authenticated = true;
                break;
        }
        return authenticated;
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
                + ", mServiceName="
                + mServiceName
                + ", mUuid="
                + mUuid
                + "}";
    }

    private BluetoothSocketSettings(
            int socketType, int channel, int securityLevel, String serviceName, UUID uuid) {
        mSocketType = socketType;
        mChannel = channel;
        mSecurityLevel = securityLevel;
        mUuid = uuid;
        mServiceName = serviceName;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    public static final class Builder {
        private int mSocketType = SOCKET_TYPE_RFCOMM;
        private int mChannel = -1;
        private int mSecurityLevel = SOCKET_SEC_LEVEL_INSECURE;
        private String mServiceName = "DEF_SERVICE_NAME";
        private UUID mUuid = null;

        /**
         * Set socket Type.
         *
         * @param socketType type of socket
         * @throws IllegalArgumentException If the {@code socketType} is invalid.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setSocketType(@SocketType int socketType) {
            if (socketType != SOCKET_TYPE_RFCOMM
                    && socketType != SOCKET_TYPE_L2CAP
                    && socketType != SOCKET_TYPE_LE) {
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
         * @param securityLevel desired security level
         * @return @Nonnull
         * @throws IllegalArgumentException If the {@code securityLevel} is invalid.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setSecurityLevel(@SocketSecurityLevel int securityLevel) {
            if (securityLevel < SOCKET_SEC_LEVEL_INSECURE
                    || securityLevel
                            > SOCKET_SEC_LEVEL_ENCRYPTION_WITH_AUTHENTICATION) {
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
            if (mSocketType == SOCKET_TYPE_RFCOMM
                    && mUuid == null
                    && mChannel != BluetoothAdapter.SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
                if (mChannel < 1 || mChannel > BluetoothSocket.MAX_RFCOMM_CHANNEL) {
                    throw new IllegalArgumentException("Invalid RFCOMM channel: " + mChannel);
                }
            }
            if (mSocketType == SOCKET_TYPE_L2CAP
                    || mSocketType == SOCKET_TYPE_LE) {
                if (mChannel < 0) {
                    throw new IllegalArgumentException("Invalid L2CAP channel: " + mChannel);
                }
            }
            return new BluetoothSocketSettings(
                    mSocketType, mChannel, mSecurityLevel, mServiceName, mUuid);
        }
    }
}
