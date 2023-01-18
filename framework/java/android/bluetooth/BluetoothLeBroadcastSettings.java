/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a Broadcast Source group and the associated information that is needed by
 * Broadcast Audio Scan Service (BASS) to set up a Broadcast Sink.
 *
 * <p>For example, an LE Audio Broadcast Sink can use the information contained within an instance
 * of this class to synchronize with an LE Audio Broadcast group in order to listen to audio from
 * Broadcast group using one or more Broadcast Channels.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastSettings implements Parcelable {
    private final boolean mIsPublicBroadcast;
    private final String mBroadcastName;
    private final byte[] mBroadcastCode;
    private final List<BluetoothLeBroadcastSubgroupSettings> mSubgroupSettings;

    private BluetoothLeBroadcastSettings(
            boolean isPublicBroadcast,
            String broadcastName,
            byte[] broadcastCode,
            List<BluetoothLeBroadcastSubgroupSettings> subgroupSettings) {
        mIsPublicBroadcast = isPublicBroadcast;
        mBroadcastName = broadcastName;
        mBroadcastCode = broadcastCode;
        mSubgroupSettings = subgroupSettings;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof BluetoothLeBroadcastSettings)) {
            return false;
        }
        final BluetoothLeBroadcastSettings other = (BluetoothLeBroadcastSettings) o;
        return mIsPublicBroadcast == other.isPublicBroadcast()
                && Objects.equals(mBroadcastName, other.getBroadcastName())
                && Arrays.equals(mBroadcastCode, other.getBroadcastCode())
                && mSubgroupSettings.equals(other.getSubgroupSettings());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mIsPublicBroadcast,
                mBroadcastName,
                Arrays.hashCode(mBroadcastCode),
                mSubgroupSettings);
    }

    /**
     * Return true if public broadcast is on.
     *
     * @return whether the Public Broadcast is enabled
     * @hide
     */
    @SystemApi
    public boolean isPublicBroadcast() {
        return mIsPublicBroadcast;
    }

    /**
     * Return the broadcast code for this broadcast group.
     *
     * @return Broadcast name for this broadcast group, null if no name provided
     * @hide
     */
    @SystemApi
    public @Nullable String getBroadcastName() {
        return mBroadcastName;
    }

    /**
     * Get the Broadcast Code currently set for this broadcast group.
     *
     * <p>Only needed when encryption is enabled
     *
     * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
     * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
     *
     * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
     *
     * @return Broadcast Code currently set for this broadcast group, null if code is not required
     *     or code is currently unknown
     * @hide
     */
    @SystemApi
    public @Nullable byte[] getBroadcastCode() {
        return mBroadcastCode;
    }

    /**
     * Get available subgroup settings in the broadcast group.
     *
     * @return list of subgroup settings in the broadcast group
     * @hide
     */
    @SystemApi
    public @NonNull List<BluetoothLeBroadcastSubgroupSettings> getSubgroupSettings() {
        return mSubgroupSettings;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBoolean(mIsPublicBroadcast);
        out.writeString(mBroadcastName);
        if (mBroadcastCode != null) {
            out.writeInt(mBroadcastCode.length);
            out.writeByteArray(mBroadcastCode);
        } else {
            // -1 indicates missing broadcast code
            out.writeInt(-1);
        }
        out.writeTypedList(mSubgroupSettings);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeBroadcastSettings} from parcel.
     *
     * @hide
     */
    @SystemApi @NonNull
    public static final Creator<BluetoothLeBroadcastSettings> CREATOR =
            new Creator<>() {
                public @NonNull BluetoothLeBroadcastSettings createFromParcel(@NonNull Parcel in) {
                    Builder builder = new Builder();
                    builder.setPublicBroadcast(in.readBoolean());
                    builder.setBroadcastName(in.readString());
                    final int codeLen = in.readInt();
                    byte[] broadcastCode = null;
                    if (codeLen != -1) {
                        broadcastCode = new byte[codeLen];
                        if (codeLen > 0) {
                            in.readByteArray(broadcastCode);
                        }
                    }
                    builder.setBroadcastCode(broadcastCode);
                    final List<BluetoothLeBroadcastSubgroupSettings> subgroupSettings =
                            new ArrayList<>();
                    in.readTypedList(
                            subgroupSettings, BluetoothLeBroadcastSubgroupSettings.CREATOR);
                    for (BluetoothLeBroadcastSubgroupSettings setting : subgroupSettings) {
                        builder.addSubgroupSettings(setting);
                    }
                    return builder.build();
                }

                public @NonNull BluetoothLeBroadcastSettings[] newArray(int size) {
                    return new BluetoothLeBroadcastSettings[size];
                }
            };

    /**
     * Builder for {@link BluetoothLeBroadcastSettings}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private boolean mIsPublicBroadcast = false;
        private String mBroadcastName = null;
        private byte[] mBroadcastCode = null;
        private List<BluetoothLeBroadcastSubgroupSettings> mSubgroupSettings = new ArrayList<>();
        /**
         * Create an empty builder.
         *
         * @hide
         */
        @SystemApi
        public Builder() {}

        /**
         * Create a builder with copies of information from original object.
         *
         * @param original original object
         * @hide
         */
        @SystemApi
        public Builder(@NonNull BluetoothLeBroadcastSettings original) {
            mIsPublicBroadcast = original.isPublicBroadcast();
            mBroadcastName = original.getBroadcastName();
            mBroadcastCode = original.getBroadcastCode();
            mSubgroupSettings = original.getSubgroupSettings();
        }

        /**
         * Set whether the Public Broadcast is on for this broadcast group.
         *
         * @param isPublicBroadcast whether the Public Broadcast is enabled
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setPublicBroadcast(boolean isPublicBroadcast) {
            mIsPublicBroadcast = isPublicBroadcast;
            return this;
        }

        /**
         * Set broadcast name for the broadcast group.
         *
         * @param broadcastName Broadcast name for this broadcast group, null if no name provided
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setBroadcastName(@Nullable String broadcastName) {
            mBroadcastName = broadcastName;
            return this;
        }

        /**
         * Set the Broadcast Code currently set for this broadcast group.
         *
         * <p>Only needed when encryption is enabled
         *
         * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
         * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
         *
         * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
         *
         * @param broadcastCode Broadcast Code for this broadcast group, null if code is not
         *     required
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setBroadcastCode(@Nullable byte[] broadcastCode) {
            mBroadcastCode = broadcastCode;
            return this;
        }

        /**
         * Add a subgroup settings to the broadcast group.
         *
         * @param subgroupSettings contains subgroup's setting data
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addSubgroupSettings(
                @NonNull BluetoothLeBroadcastSubgroupSettings subgroupSettings) {
            Objects.requireNonNull(subgroupSettings, "subgroupSettings cannot be null");
            mSubgroupSettings.add(subgroupSettings);
            return this;
        }

        /**
         * Clear subgroup settings list so that one can reset the builder
         *
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder clearSubgroupSettings() {
            mSubgroupSettings.clear();
            return this;
        }

        /**
         * Build {@link BluetoothLeBroadcastSettings}.
         *
         * @return {@link BluetoothLeBroadcastSettings}
         * @throws IllegalArgumentException if the object cannot be built
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothLeBroadcastSettings build() {
            if (mSubgroupSettings.isEmpty()) {
                throw new IllegalArgumentException("Must contain at least one subgroup");
            }
            return new BluetoothLeBroadcastSettings(
                    mIsPublicBroadcast, mBroadcastName, mBroadcastCode, mSubgroupSettings);
        }
    }
}
