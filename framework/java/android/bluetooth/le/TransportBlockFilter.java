/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.bluetooth.le;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothAssignedNumbers.OrganizationId;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Wrapper for filter input for Transport Discovery Data Transport Blocks.
 * This class represents the filter for a Transport Block from a Transport Discovery Data
 * advertisement data.
 *
 * @see ScanFilter
 * @hide
 */
public class TransportBlockFilter implements Parcelable {

    private final int mOrgId;
    private final int mTdsFlags;
    private final int mTdsFlagsMask;
    private final byte[] mTransportData;
    private final byte[] mTransportDataMask;
    private final byte[] mWifiNanHash;

    private TransportBlockFilter(int orgId, int tdsFlags, int tdsFlagsMask,
            @Nullable byte[] transportData, @Nullable byte[] transportDataMask,
            @Nullable byte[] wifiNanHash) {
        mOrgId = orgId;
        mTdsFlags = tdsFlags;
        mTdsFlagsMask = tdsFlagsMask;
        mTransportData = transportData;
        mTransportDataMask = transportDataMask;
        mWifiNanHash = wifiNanHash;
    }

    /**
     * Get Organization ID assigned by Bluetooth SIG. For more details refer to Transport Discovery
     * Service Organization IDs in
     * <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>
     *
     * @return Organization ID assigned by Bluetooth SIG.
     * @hide
     */
    @SystemApi
    public int getOrgId() {
        return mOrgId;
    }


    /**
     * Get Transport Discovery Service (TDS) flags to filter Transport Discovery Blocks
     *
     * @return Transport Discovery Service (TDS) flags to filter Transport Discovery Blocks
     * @hide
     */
    @SystemApi
    public int getTdsFlags() {
        return mTdsFlags;
    }

    /**
     * Get masks for filtering Transport Discovery Service (TDS) flags in Transport Discovery Blocks
     *
     * @return Masks for filtering Transport Discovery Service (TDS) flags in Transport Discovery
     * Blocks
     * @hide
     */
    @SystemApi
    public int getTdsFlagsMask() {
        return mTdsFlagsMask;
    }

    /**
     * Get data to filter Transport Discovery Blocks
     *
     * Cannot be used when {@code orgId} is {@link BluetoothAssignedNumbers.OrganizationId
     * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
     *
     * @return Data to filter Transport Discovery Blocks, null if not used
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getTransportData() {
        return mTransportData;
    }

    /**
     * Get masks for filtering data in Transport Discovery Blocks
     *
     * Cannot be used when {@code orgId} is {@link BluetoothAssignedNumbers.OrganizationId
     * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
     *
     * @return a byte array with matching length to {@code transportData} to
     * select which bit to use in filter, null is not used
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getTransportDataMask() {
        return mTransportDataMask;
    }

    /**
     * Get hashed bloom filter value to filter {@link BluetoothAssignedNumbers.OrganizationId
     * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING} services in Transport Discovery Blocks.
     *
     * Can only be used when {@code orgId} is {@link BluetoothAssignedNumbers.OrganizationId
     * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}.
     *
     * @return 8 octets Wi-Fi NAN defined bloom filter hash, null if not used
     */
    @SystemApi
    @Nullable
    public byte[] getWifiNanHash() {
        return mWifiNanHash;
    }

    /**
     * Check if a scan result matches this transport block filter
     *
     * @param scanResult scan result to match
     * @return true if matches
     * @hide
     */
    boolean matches(ScanResult scanResult) {
        ScanRecord scanRecord = scanResult.getScanRecord();
        // Transport Discovery data match
        TransportDiscoveryData transportDiscoveryData = scanRecord.getTransportDiscoveryData();

        if ((transportDiscoveryData != null)) {
            for (TransportBlock transportBlock : transportDiscoveryData.getTransportBlocks()) {
                int orgId = transportBlock.getOrgId();
                int tdsFlags =  transportBlock.getTdsFlags();
                int transportDataLength = transportBlock.getTransportDataLength();
                byte[] transportData = transportBlock.getTransportData();

                if (mOrgId != orgId) {
                    continue;
                }
                if ((mTdsFlags & mTdsFlagsMask) != (tdsFlags & mTdsFlagsMask)) {
                    continue;
                }
                if ((mOrgId != BluetoothAssignedNumbers.OrganizationId
                        .WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING)
                        && (mTransportData != null) && (mTransportDataMask != null)) {
                    if (transportDataLength != 0) {
                        if (!ScanFilter.matchesPartialData(
                                mTransportData, mTransportDataMask, transportData)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mOrgId);
        dest.writeInt(mTdsFlags);
        dest.writeInt(mTdsFlagsMask);
        dest.writeInt(mTransportData == null ? 0 : 1);
        if (mTransportData != null) {
            dest.writeInt(mTransportData.length);
            dest.writeByteArray(mTransportData);
            dest.writeInt(mTransportDataMask == null ? 0 : 1);
            if (mTransportDataMask != null) {
                dest.writeInt(mTransportDataMask.length);
                dest.writeByteArray(mTransportDataMask);
            }
        }
        dest.writeInt(mWifiNanHash == null ? 0 : 1);
        if (mWifiNanHash != null) {
            dest.writeInt(mWifiNanHash.length);
            dest.writeByteArray(mWifiNanHash);
        }
    }

    /**
     * @return printable string
     * @hide
     */
    @Override
    public String toString() {
        return "TransportBlockFilter [mOrgId=" + mOrgId + ", mTdsFlags=" + mTdsFlags
                + ", mTdsFlagsMask=" + mTdsFlagsMask + ", mTransportData="
                + Arrays.toString(mTransportData) + ", mTransportDataMask="
                + Arrays.toString(mTransportDataMask) + ", mWifiNanHash="
                + Arrays.toString(mWifiNanHash) + "]";
    }

    /**
     * @return hash code of this object
     * @hide
     */
    @Override
    public int hashCode() {
        return Objects.hash(mOrgId, mTdsFlags, mTdsFlagsMask, Arrays.hashCode(mTransportData),
                Arrays.hashCode(mTransportDataMask), Arrays.hashCode(mWifiNanHash));
    }

    /**
     * @return hash code of this object
     * @hide
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TransportBlockFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final TransportBlockFilter other = (TransportBlockFilter) obj;
        return mOrgId == other.getOrgId()
                && mTdsFlags == other.getTdsFlags()
                && mTdsFlagsMask == other.getTdsFlagsMask()
                && Arrays.equals(mTransportData, other.getTransportData())
                && Arrays.equals(mTransportDataMask, other.getTransportDataMask())
                && Arrays.equals(mWifiNanHash, other.getWifiNanHash());
    }

    public static final Creator<TransportBlockFilter> CREATOR = new Creator<>() {
        @Override
        public TransportBlockFilter createFromParcel(Parcel source) {
            final int orgId = source.readInt();
            Builder builder = new Builder(orgId);
            builder.setTdsFlags(source.readInt(), source.readInt());
            if (source.readInt() == 1) {
                int transportDataLength = source.readInt();
                byte[] transportData = new byte[transportDataLength];
                source.readByteArray(transportData);
                byte[] transportDataMask = null;
                if (source.readInt() == 1) {
                    int transportDataMaskLength = source.readInt();
                    transportDataMask = new byte[transportDataMaskLength];
                    source.readByteArray(transportDataMask);
                }
                builder.setTransportData(transportData, transportDataMask);
            }
            if (source.readInt() == 1) {
                int wifiNanHashLength = source.readInt();
                byte[] wifiNanHash = new byte[wifiNanHashLength];
                source.readByteArray(wifiNanHash);
                builder.setWifiNanHash(wifiNanHash);
            }
            return builder.build();
        }

        @Override
        public TransportBlockFilter[] newArray(int size) {
            return new TransportBlockFilter[0];
        }
    }


    /**
     * Builder class for {@link TransportBlockFilter}.
     */
    public static final class Builder {

        private final int mOrgId;
        private int mTdsFlags = 0;
        private int mTdsFlagsMask = 0;
        private byte[] mTransportData;
        private byte[] mTransportDataMask;
        private byte[] mWifiNanHash;

        /**
         * Builder for {@link TransportBlockFilter}
         *
         * @param orgId Organization ID assigned by Bluetooth SIG. For more details refer to
         * Transport Discovery Service Organization IDs in
         * <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>
         * @throws IllegalArgumentException If the {@code orgId} is invalid
         * @see OrganizationId
         * @hide
         */
        @SystemApi
        public Builder(int orgId) {
            if (orgId < 1) {
                throw new IllegalArgumentException("invalid organization id " + orgId);
            }
            mOrgId = orgId;
        }

        /**
         * Set Transport Discovery Service (TDS) flags to filter Transport Discovery Blocks
         *
         * @param tdsFlags 1 octet value that represents the role of the device and information
         * about its state and supported features. -1 is invalid for this argument. Default to 0.See
         * Transport Discovery Service specification for more details
         * @param tdsFlagsMask 0 if not used, or a bitmask to select which bits in {@code tdsFlag}
         * to match. Default to 0. -1 is invalid for this argument.
         * @throws IllegalArgumentException if either {@code tdsFlags} or {@code tdsFlagsMask} is
         *                                  invalid
         * @return this builder
         */
        @SystemApi
        @NonNull
        public Builder setTdsFlags(int tdsFlags, int tdsFlagsMask) {
            if (tdsFlags == -1) {
                throw new IllegalArgumentException("tdsFlag is invalid");
            }
            if (tdsFlagsMask == -1) {
                throw new IllegalArgumentException("tdsFlagsMask is invalid");
            }
            mTdsFlags = tdsFlags;
            mTdsFlagsMask = tdsFlagsMask;
            return this;
        }

        /**
         * Set data to filter Transport Discovery Blocks
         *
         * Cannot be used when {@code orgId} is {@link BluetoothAssignedNumbers.OrganizationId
         * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
         *
         * @param transportData must be valid value for the particular {@code orgId}. See
         * Transport Discovery Service specification for more details.
         * @param transportDataMask a byte array with matching length to {@code transportData} to
         * select which bit to use in filter.
         * @throws IllegalArgumentException when {@code orgId} is {@link BluetoothAssignedNumbers
         * #OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
         * @throws NullPointerException if {@code transportData} or {@code transportDataMask} is
         * {@code null}
         * @throws IllegalArgumentException if {@code transportData} or {@code transportDataMask} is
         * empty
         * @throws IllegalArgumentException if length of {@code transportData} and
         * {@code transportDataMask} do not match
         * @return this builder
         */
        @SystemApi
        @NonNull
        public Builder setTransportData(@NonNull byte[] transportData,
                @NonNull byte[] transportDataMask) {
            if (mOrgId == BluetoothAssignedNumbers.OrganizationId
                    .WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING) {
                throw new IllegalArgumentException(
                        "setWifiNanHash() should be used instead of setTransportData() when orgId "
                                + "is WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING");
            }
            Objects.requireNonNull(transportData);
            Objects.requireNonNull(transportDataMask);
            if (transportData.length == 0) {
                throw new IllegalArgumentException("transportData is empty");
            }
            if (transportDataMask.length == 0) {
                throw new IllegalArgumentException("transportDataMask is empty");
            }
            if (transportData.length != transportDataMask.length) {
                throw new IllegalArgumentException(
                        "Length of transportData and transportDataMask do not match");
            }
            mTransportData = transportData;
            mTransportDataMask = transportDataMask;
            return this;
        }

        /**
         * Set hashed bloom filter value to filter {@link BluetoothAssignedNumbers.OrganizationId
         * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING} services in Transport Discovery Blocks.
         *
         * Can only be used when {@code orgId} is {@link BluetoothAssignedNumbers.OrganizationId
         * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}.
         *
         * Cannot be used together with {@link #setTransportData(byte[], byte[])}
         *
         * @param wifiNanHash 8 octets Wi-Fi NAN defined bloom filter hash
         * @throws IllegalArgumentException when {@code orgId} is not
         * {@link BluetoothAssignedNumbers.OrganizationId
         * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
         * @throws IllegalArgumentException when {@code wifiNanHash} is not 8 bytes long
         * @throws NullPointerException when {@code wifiNanHash} is null
         * @return this builder
         */
        public Builder setWifiNanHash(@NonNull byte[] wifiNanHash) {
            if (mOrgId != BluetoothAssignedNumbers.OrganizationId
                    .WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING) {
                throw new IllegalArgumentException("setWifiNanHash() can only be used when orgId is"
                        + " WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING");
            }
            Objects.requireNonNull(wifiNanHash);
            if (wifiNanHash.length != 8) {
                throw new IllegalArgumentException("Wi-Fi NAN hash must be 8 octets long");
            }
            mWifiNanHash = wifiNanHash;
            return this;
        }

        /**
         * Build {@link TransportBlockFilter}
         *
         * @return {@link TransportBlockFilter}
         * @throws IllegalStateException if the filter cannot be built
         */
        @SystemApi
        @NonNull
        public TransportBlockFilter build() {
            return new TransportBlockFilter(mOrgId, mTdsFlags, mTdsFlagsMask, mTransportData,
                    mTransportDataMask, mWifiNanHash);
        }
    }
}
