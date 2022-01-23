/*
 * Copyright 2022 The Android Open Source Project
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
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
public class BluetoothLeBroadcastSubgroup implements Parcelable {
    private final long mCodecId;
    private final BluetoothLeAudioCodecConfigMetadata mCodecSpecificConfig;
    private final BluetoothLeAudioContentMetadata mContentMetadata;
    private final boolean mNoChannelPreference;
    private List<BluetoothLeBroadcastChannel> mChannels;

    public BluetoothLeBroadcastSubgroup(long codecId,
            BluetoothLeAudioCodecConfigMetadata codecSpecificConfig,
            BluetoothLeAudioContentMetadata contentMetadata, boolean noChannelPreference,
            List<BluetoothLeBroadcastChannel> channels) {
        mCodecId = codecId;
        mCodecSpecificConfig = codecSpecificConfig;
        mContentMetadata = contentMetadata;
        mNoChannelPreference = noChannelPreference;
        mChannels = channels;
    }

    /**
     * Get the codec ID field as defined by the Basic Audio Profile.
     *
     * The codec ID field has 5 octets, with
     * - Octet 0: Coding_Format as defined in Bluetooth Assigned Numbers
     * - Octet 1-2: Company ID as defined in Bluetooth Assigned Numbers
     *              Shall be 0x0000 if octet 0 != 0xFF
     * - Octet 3-4: Vendor-specific codec ID
     *              Shall be 0x0000 if octet 0 != 0xFF
     *
     * @return 5-byte codec ID field in Java long format
     * @hide
     */
    @SystemApi
    public long getCodecId() {
        return mCodecId;
    }

    /**
     * Get codec specific config metadata for this subgroup
     *
     * @return codec specific config metadata for this subgroup
     * @hide
     */
    @SystemApi
    public BluetoothLeAudioCodecConfigMetadata getCodecSpecificConfig() {
        return mCodecSpecificConfig;
    }

    /**
     * Get content metadata for this Broadcast Source subgroup
     *
     * @return content metadata for this Broadcast Source subgroup
     * @hide
     */
    @SystemApi
    public BluetoothLeAudioContentMetadata getContentMetadata() {
        return mContentMetadata;
    }

    /**
     * Indicate if Broadcast Sink should have no Broadcast Channel (BIS) preference
     *
     * Only used by Broadcast Assistant and Sink. Ignored by Broadcast Source
     *
     * @return true if Broadcast Sink should have no Broadcast Channel (BIS) preference
     * @hide
     */
    @SystemApi
    public boolean isNoChannelPreference() {
        return mNoChannelPreference;
    }

    /**
     * Get list of Broadcast Channels included in this Broadcast subgroup
     *
     * Each Broadcast Channel represents a Broadcast Isochronous Stream (BIS)
     *
     * A Broadcast subgroup should contain at least 1 Broadcast Channel
     *
     * @return list of Broadcast Channels included in this Broadcast subgroup
     * @hide
     */
    @SystemApi
    public List<BluetoothLeBroadcastChannel> getChannels() {
        return mChannels;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mCodecId);
        out.writeTypedObject(mCodecSpecificConfig, 0);
        out.writeTypedObject(mContentMetadata, 0);
        out.writeBoolean(mNoChannelPreference);
        out.writeTypedList(mChannels);
    }

    /**
     * @hide
     */
    @SystemApi
    public static final Parcelable.Creator<BluetoothLeBroadcastSubgroup> CREATOR =
            new Parcelable.Creator<BluetoothLeBroadcastSubgroup>() {
                @NonNull
                public BluetoothLeBroadcastSubgroup createFromParcel(@NonNull Parcel in) {
                    Builder builder = new Builder();
                    builder.setCodecId(in.readLong());
                    builder.setCodecSpecificConfig(in.readTypedObject(
                            BluetoothLeAudioCodecConfigMetadata.CREATOR));
                    builder.setNoChannelPreference(in.readBoolean());
                    List<BluetoothLeBroadcastChannel> channels = new ArrayList<>();
                    in.readTypedList(channels, BluetoothLeBroadcastChannel.CREATOR);
                    for (BluetoothLeBroadcastChannel channel : channels) {
                        builder.addChannel(channel);
                    }
                    return builder.build();
                }

                public @NonNull BluetoothLeBroadcastSubgroup[] newArray(int size) {
                    return new BluetoothLeBroadcastSubgroup[size];
                }
    };

    private static final int UNKNOWN_VALUE_PLACEHOLDER = -1;

    /**
     * Builder for {@link BluetoothLeBroadcastSubgroup}
     * @hide
     */
    @SystemApi
    public static class Builder {
        private long mCodecId = UNKNOWN_VALUE_PLACEHOLDER;
        private BluetoothLeAudioCodecConfigMetadata mCodecSpecificConfig = null;
        private BluetoothLeAudioContentMetadata mContentMetadata = null;
        private boolean mNoChannelPreference = false;
        private List<BluetoothLeBroadcastChannel> mChannels = new ArrayList<>();

        /**
         * Create an empty constructor
         * @hide
         */
        @SystemApi
        public Builder() {}

        /**
         * Create a builder with copies of information from original object
         *
         * @param original original object
         * @hide
         */
        @SystemApi
        public Builder(BluetoothLeBroadcastSubgroup original) {
            mCodecId = original.getCodecId();
            mCodecSpecificConfig = original.getCodecSpecificConfig();
            mContentMetadata = original.getContentMetadata();
            mNoChannelPreference = original.isNoChannelPreference();
            mChannels = original.getChannels();
        }


        /**
         * Set the codec ID field as defined by the Basic Audio Profile.
         *
         * The codec ID field has 5 octets, with
         * - Octet 0: Coding_Format as defined in Bluetooth Assigned Numbers
         * - Octet 1-2: Company ID as defined in Bluetooth Assigned Numbers
         *              Shall be 0x0000 if octet 0 != 0xFF
         * - Octet 3-4: Vendor-specific codec ID
         *              Shall be 0x0000 if octet 0 != 0xFF
         *
         * @param codecId 5-byte codec ID field in Java long format
         * @return this builder
         * @hide
         */
        @SystemApi
        public Builder setCodecId(long codecId) {
            mCodecId = codecId;
            return this;
        }

        /**
         * Set codec specific config metadata for this subgroup
         *
         * @param codecSpecificConfig codec specific config metadata for this subgroup
         * @return this builder
         * @hide
         */
        @SystemApi
        public Builder setCodecSpecificConfig(
                BluetoothLeAudioCodecConfigMetadata codecSpecificConfig) {
            mCodecSpecificConfig = codecSpecificConfig;
            return this;
        }

        /**
         * Set content metadata for this Broadcast Source subgroup
         *
         * @param contentMetadata content metadata for this Broadcast Source subgroup
         * @return this builder
         * @hide
         */
        @SystemApi
        public Builder setContentMetadata(BluetoothLeAudioContentMetadata contentMetadata) {
            mContentMetadata = contentMetadata;
            return this;
        }

        /**
         * Set if Broadcast Sink should have no Broadcast Channel (BIS) preference
         *
         * Only used by Broadcast Assistant and Sink. Ignored by Broadcast Source
         *
         * @param isNoChannelPreference true if Broadcast Sink should have no Broadcast Channel
         *                              (BIS) preference
         * @return this builder
         * @hide
         */
        @SystemApi
        public Builder setNoChannelPreference(boolean isNoChannelPreference) {
            mNoChannelPreference = isNoChannelPreference;
            return this;
        }

        /**
         * Add a Broadcast Channel to this Broadcast subgroup
         *
         * Each Broadcast Channel represents a Broadcast Isochronous Stream (BIS)
         *
         * A Broadcast subgroup should contain at least 1 Broadcast Channel
         *
         * @param channel  a Broadcast Channel to be added to this Broadcast subgroup
         * @hide
         */
        @SystemApi
        public Builder addChannel(BluetoothLeBroadcastChannel channel) {
            mChannels.add(channel);
            return this;
        }

        /**
         * Build {@link BluetoothLeBroadcastSubgroup}
         *
         * @return constructed {@link BluetoothLeBroadcastSubgroup}
         * @throws BluetoothLeBroadcastSubgroup if the object cannot be built
         * @hide
         */
        @SystemApi
        public BluetoothLeBroadcastSubgroup build() {
            return new BluetoothLeBroadcastSubgroup(mCodecId, mCodecSpecificConfig,
                    mContentMetadata, mNoChannelPreference, mChannels);
        }
    }
}
