/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @hide
 */
public class BluetoothLeAudioStreamMetadata implements Parcelable {
    private String mProgramInfo;
    private String mLanguage;
    private byte[] mRawMetadata;

    /**
     * @hide
     */
    public BluetoothLeAudioStreamMetadata(String programInfo, String language, byte[] rawMetadata) {
        mProgramInfo = programInfo;
        mLanguage = language;
        mRawMetadata = rawMetadata;
    }

    /**
     * Get the title and/or summary of Audio Stream content in UTF-8 format
     *
     * @return title and/or summary of Audio Stream content in UTF-8 format
     *
     * @hide
     */
    @SystemApi
    public String getProgramInfo() {
        return mProgramInfo;
    }

    /**
     * Get language of the audio stream in 3-byte, lower case language code as defined in ISO 639-3.
     *
     * @return ISO 639-3 formatted language code
     *
     * @hide
     */
    @SystemApi
    public String getLanguage() {
        return mLanguage;
    }

    /**
     * Get the raw bytes of stream metadata in Bluetooth LTV format as defined in the Generic Audio
     * section of <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>,
     * including metadata that was not covered by the getter methods in this class
     *
     * @return raw bytes of stream metadata in Bluetooth LTV format
     */
    public byte[] getRawMetadata() {
        return mRawMetadata;
    }


    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation. For example, if the object will
     * include a file descriptor in the output of {@link #writeToParcel(Parcel, int)},
     * the return value of this method must include the
     * {@link #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled
     * by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param out  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mProgramInfo);
        out.writeString(mLanguage);
        out.writeInt(mRawMetadata.length);
        out.writeByteArray(mRawMetadata);
    }

    @NonNull
    public static final Parcelable.Creator<BluetoothLeAudioStreamMetadata> CREATOR =
            new Parcelable.Creator<BluetoothLeAudioStreamMetadata>() {
                @NonNull
                public BluetoothLeAudioStreamMetadata createFromParcel(@NonNull Parcel in) {
                    final String programInfo = in.readString();
                    final String language = in.readString();
                    final int rawMetadataLength = in.readInt();
                    byte[] rawMetadata = new byte[rawMetadataLength];
                    in.readByteArray(rawMetadata);
                    return new BluetoothLeAudioStreamMetadata(programInfo, language, rawMetadata);
                }

                public @NonNull BluetoothLeAudioStreamMetadata[] newArray(int size) {
                    return new BluetoothLeAudioStreamMetadata[size];
                }
            };
}
