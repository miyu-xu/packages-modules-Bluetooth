/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.annotation.SystemApi;
import android.annotation.FlaggedApi;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.UUID;
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.bluetooth.flags.Flags;
import android.annotation.FlaggedApi;
/**
 * Bluetooth Socket settings are passed to {@link BluetoothAdapter#createListeningChannel} and
 * {@link BluetoothDevice#createListeningChannel} to define the
 * parameters for the Bluetooth Server and Client socket channel creation.
 */
@FlaggedApi(Flags.FLAG_BT_SOCKET_API_L2CAP_CID)
public final class BluetoothSocketSettings implements Parcelable {

    /**
     * For Sockets with NO security requirements
     */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_0 = 0;

    /**
     * For Sockets with only ENCRYPTION as requirement
     */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_1 = 1;

    /**
     * For Sockets with both ENCRYPTION and AUTHENTICATION as requirement
     * This ensures no MITM attack
     */
    public static final int BLUETOOTH_SOCKET_SECURITY_LEVEL_2 = 2;

    //type of the socket,
    // BluetoothSocket.TYPE_RFCOMM, BluetoothSocket.TYPE_L2CAP or BluetoothSocket.TYPE_LE
    private int mSocketType;

    // Bluetooth RFCOMM Channel.
    private int mChannel;

    // desired Bluetooth security level
    private int mSecurityLevel;

    // Service name for SDP record.
    private String mServiceName;

    // Service Uuid for the Sdp Record.
    private UUID mUuid;

    /*
        @return one of {@link BluetoothSocket#TYPE_RFCOMM},
                   {@link BluetoothSocket#TYPE_L2CAP}
     */
    public int getSocketType() {
        return mSocketType;
    }

    @NonNull
    public int getChannel() {
        return mChannel;
    }

    @NonNull
    public int getSecurityLevel() {
        return mSecurityLevel;
    }

    @NonNull
    public String getServiceName() {
        return mServiceName;
    }

    @NonNull
    public UUID getUuid() {
        return mUuid;
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

    private BluetoothSocketSettings(Parcel in) {
        mSocketType = in.readInt();
        mChannel = in.readInt();
        mSecurityLevel = in.readInt();
        //mServiceName = in.readString();
        //mUuid = in.readTypedObject();
    }

    @Override
    @NonNull
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mSocketType);
        dest.writeInt(mChannel);
        dest.writeInt(mSecurityLevel);
        //dest.writeString8(dest, mServiceName);
        //dest.writeTypedObject(mUuid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final @android.annotation.NonNull Parcelable.Creator<BluetoothSocketSettings> CREATOR =
            new Creator<BluetoothSocketSettings>() {
                @Override
                public BluetoothSocketSettings[] newArray(int size) {
                    return new BluetoothSocketSettings[size];
                }

                @Override
                public BluetoothSocketSettings createFromParcel(Parcel in) {
                    return new BluetoothSocketSettings(in);
                }
            };

    /** Builder for {@link BluetoothSocketSettings}. */
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mChannel = 0;
        private int mSecurityLevel = BLUETOOTH_SOCKET_SECURITY_LEVEL_0;
        private String mServiceName = "DEF_SERVICE_NAME";
        private UUID mUuid;

        /**
         * Set socket Type.
         * This can be of type BluetoothDevice.TYPE_RFCOMM, BluetoothDevice.TYPE_L2CAP or
         * BluetoothDevice.TYPE_LE
         *
         * @param socketType type of socket, one of {@link BluetoothSocket#TYPE_RFCOMM},
         *                    {@link BluetoothSocket#TYPE_L2CAP}
         * @throws IllegalArgumentException If the {@code socketType} is invalid.
         */
        @NonNull
        public Builder setSocketType(int socketType) {
            mSocketType = socketType;
            //TODO
            //throw new IllegalArgumentException("invalid socket type " + socketType);

            return this;
        }

        /**
         * Sets the channel for Bluetooth connection
         * This can serve as either RFCOMM channel or L2CAP PSM
         *
         * @param channel channel for Bluetooth connection.
         * @throws IllegalArgumentException If the {@code channel} is invalid.
         */
        @NonNull
        public Builder setChannel(int channel) {

            /*
            TODO
            if (!isValid(channel)) {
                throw new IllegalArgumentException("invalid callback type - " + callbackType);
            }
            */
            mChannel = channel;
            return this;
        }

        /**
         * Set Security level for Bluetoth connection.
         *
         * @param securityLevel desired security level, could be either {@link
         *     BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_0} or {@link
         *     BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_1} Or
         *     {@link BluetoothSocketSettings#BLUETOOTH_SOCKET_SECURITY_LEVEL_1}
         * @return @Nonnull
         * @throws IllegalArgumentException If the {@code securityLevel} is invalid.
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setSecurityLevel(int securityLevel) {
            /*
            if (scanResultType < SCAN_RESULT_TYPE_FULL
                    || scanResultType > SCAN_RESULT_TYPE_ABBREVIATED) {
                throw new IllegalArgumentException("invalid scanResultType - " + scanResultType);
            }
            */
            mSecurityLevel = securityLevel;
            return this;
        }

        /**
         * Set the Service name for SDP.
         *
         * @param serviceName service name for SDP record
         * @throws IllegalArgumentException If the {@code matchMode} is invalid.
         */
        @NonNull
        public Builder setServiceName(@NonNull String serviceName) {
            mServiceName = serviceName;
            return this;
        }

        /**
         * Set the service UUID for SDP.
         *
         * @param uuid uuid for SDP record
         * @throws IllegalArgumentException If the {@code matchMode} is invalid.
         */
        @NonNull
        public Builder setUuid(@NonNull UUID uuid) {
            /*
            if (matchMode < MATCH_MODE_AGGRESSIVE || matchMode > MATCH_MODE_STICKY) {
                throw new IllegalArgumentException("invalid matchMode " + matchMode);
            }*/
            mUuid = uuid;
            return this;
        }
        /**
         * Build {@link BluetoothSocketSettings}.
         *
         * @throws IllegalArgumentException if the settings cannot be built.
         */
        @NonNull
        public BluetoothSocketSettings build() {
            return new BluetoothSocketSettings(
                    mSocketType,
                    mChannel,
                    mSecurityLevel,
                    mServiceName,
                    mUuid);
        }
    }
}
