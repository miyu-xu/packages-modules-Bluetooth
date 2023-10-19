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

package android.bluetooth;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a supported source codec type for a Bluetooth A2DP device. See {@link
 * BluetoothA2dp#getSupportedCodecTypes}.
 */
@FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
public final class BluetoothCodecType implements Parcelable {
    private final int mNativeCodecType;
    private final String mCodecName;

    private BluetoothCodecType(Parcel in) {
        mNativeCodecType = in.readInt();
        mCodecName = in.readString();
    }

    /**
     * Create the bluetooth codec type from the static codec type index.
     *
     * @param codecType the static codec type
     */
    private BluetoothCodecType(@BluetoothCodecConfig.SourceCodecType int codecType) {
        mNativeCodecType = codecType;
        mCodecName = BluetoothCodecConfig.getCodecName(codecType);
    }

    /** Returns if the codec type is mandatory in the Bluetooth specification. */
    @FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
    public boolean isMandatoryCodec() {
        return mNativeCodecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
    }

    /** Returns the codec name. */
    @FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
    public @NonNull String getCodecName() {
        return mCodecName;
    }

    /**
     * Returns the native codec type.
     *
     * @hide
     */
    public int getNativeCodecType() {
        return mNativeCodecType;
    }

    @Override
    public String toString() {
        return mCodecName;
    }

    @Override
    public int hashCode() {
        return mNativeCodecType;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof BluetoothCodecType) {
            BluetoothCodecType other = (BluetoothCodecType) o;
            return other.mNativeCodecType == mNativeCodecType;
        }
        return false;
    }

    /** @hide */
    public static @NonNull BluetoothCodecType createFromParcel(Parcel in) {
        return new BluetoothCodecType(in);
    }

    /** @hide */
    @Override
    @FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mNativeCodecType);
        dest.writeString(mCodecName);
    }

    /**
     * Create the bluetooth codec type from the static codec type index.
     *
     * @param codecType the static codec type
     * @return the codec type if valid
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
    public static @Nullable BluetoothCodecType createFromType(
            @BluetoothCodecConfig.SourceCodecType int codecType) {
        if (codecType < BluetoothCodecConfig.SOURCE_CODEC_TYPE_MAX) {
            return new BluetoothCodecType(codecType);
        }
        return null;
    }

    /**
     * @return 0
     * @hide
     */
    @Override
    @FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
    public int describeContents() {
        return 0;
    }

    @FlaggedApi("com.android.bluetooth.flags.a2dp_offload_codec_extensibility")
    public static final @NonNull Creator<BluetoothCodecType> CREATOR =
            new Creator<>() {
                public BluetoothCodecType createFromParcel(Parcel in) {
                    return new BluetoothCodecType(in);
                }

                public BluetoothCodecType[] newArray(int size) {
                    return new BluetoothCodecType[size];
                }
            };
}
