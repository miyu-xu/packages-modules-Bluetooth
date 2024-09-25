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
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Bluetooth Socket settings are passed to {@link BluetoothLeScanner#startScan} to define the
 * parameters for the scan.
 */
public final class BluetoothScoketSettings implements Parcelable {

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

    //type of the socket, BluetoothDevice.TYPE_RFCOMM, TYPE_L2CAP or TYPE_LE
    private int mScoketType;

    // Bluetooth RFCOMM Channel.
    private int mChannel;

    // desired Bluetooth security level
    private int mSecurityLevel;

    // Service name for SDP record.
    private String mServiceName;

    // Service Uuid for the Sdp Record.
    private UUID mUuid;

    public int getSocketType() {
        return mSocketType;
    }

    public int getChannel() {
        return mChannel;
    }

    public int getSecurityLevel() {
        return mSecurityLevel;
    }

    public String getServiceName() {
        return mServiceName;
    }

    public UUID getUUID() {
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
        //mServiceName = in.readLong();
        //mUuid = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSocketType);
        dest.writeInt(mChannel);
        dest.writeInt(mSecurityLevel);
        dest.writeLong(mServiceName);
        dest.writeInt(mUuid);
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
        private UUID mUuid  = "";

        /**
         * Set socket Type.
         *
         * @param socketType type of socket
         * @throws IllegalArgumentException If the {@code scanMode} is invalid.
         */
        public Builder setSocketType(int socketType) {
            mSocketType = socketType;
            //default:
            //    throw new IllegalArgumentException("invalid scan mode " + scanMode);

            return this;
        }

        /**
         * Sets the socket's RFCOMM channel.
         *
         * @param callbackType The callback type flags for the scan.
         * @throws IllegalArgumentException If the {@code callbackType} is invalid.
         */
        public Builder setChannel(int channel) {

            /*
            if (!isValidCallbackType(callbackType)) {
                throw new IllegalArgumentException("invalid callback type - " + callbackType);
            }
            */
            mChannel = channel;
            return this;
        }

        /**
         * Set scan result type for Bluetooth LE scan.
         *
         * @param scanResultType Type for scan result, could be either {@link
         *     BluetoothSocketSettings#SCAN_RESULT_TYPE_FULL} or {@link
         *     BluetoothSocketSettings#SCAN_RESULT_TYPE_ABBREVIATED}.
         * @throws IllegalArgumentException If the {@code scanResultType} is invalid.
         * @hide
         */
        @SystemApi
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
         * @param numOfMatches The num of matches can be one of {@link
         *     BluetoothSocketSettings#MATCH_NUM_ONE_ADVERTISEMENT} or {@link
         *     BluetoothSocketSettings#MATCH_NUM_FEW_ADVERTISEMENT} or {@link
         *     BluetoothSocketSettings#MATCH_NUM_MAX_ADVERTISEMENT}
         * @throws IllegalArgumentException If the {@code matchMode} is invalid.
         */
        public Builder setServiceName(String serviceName) {
            mServiceName = serviceName;
            return this;
        }

        /**
         * Set the service UUID for SDP.
         *
         * @param matchMode The match mode can be one of {@link BluetoothSocketSettings#MATCH_MODE_AGGRESSIVE}
         *     or {@link BluetoothSocketSettings#MATCH_MODE_STICKY}
         * @throws IllegalArgumentException If the {@code matchMode} is invalid.
         */
        public UUID setUuid(UUID uuid) {
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
