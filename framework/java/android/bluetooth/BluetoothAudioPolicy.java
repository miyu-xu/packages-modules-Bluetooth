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
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import java.util.UUID;

/**
 * This class represents a single call, its state and properties.
 * It implements {@link Parcelable} for inter-process message passing.
 *
 * @hide
 */
@SystemApi
public final class BluetoothAudioPolicy implements Parcelable {

    /* Call audio preferences */

    /**
     * Call audio will not be accepted.
     */
    public static final int CALL_AUDIO_DEFAULT = 0;

    /**
     * Call audio will not be accepted.
     */
    public static final int CALL_AUDIO_ALLOWED = 1;
    /**
     * Call audio will be accepted.
     */
    public static final int CALL_AUDIO_NOT_ALLOWED = 2;

    private int mCallPickUpPolicy;
    private int mConnectingPolicy;
    private int mInBandRingPolicy;

    public BluetoothAudioPolicy() {
        mCallPickUpPolicy = CALL_AUDIO_DEFAULT;
        mConnectingPolicy = CALL_AUDIO_DEFAULT;
        mInBandRingPolicy = CALL_AUDIO_DEFAULT;
    }

    /*
     * @hide
     */
    public BluetoothAudioPolicy(int callPickUpPolicy,
            int connectingPolicy, int inBandRingPolicy) {
        mCallPickUpPolicy = callPickUpPolicy;
        mConnectingPolicy = connectingPolicy;
        mInBandRingPolicy = inBandRingPolicy;
    }

    public @NonNull BluetoothAudioPolicy setCallPickUpPolicy(int callPickUpPolicy) {
        mCallPickUpPolicy = callPickUpPolicy;
        return this;
    }

    public int getCallPickUpPolicy() {
        return mCallPickUpPolicy;
    }

    public @NonNull BluetoothAudioPolicy setConnectingPolicy(int connectingPolicy) {
        mConnectingPolicy = connectingPolicy;
        return this;
    }

    public int getConnectingPolicy() {
        return mConnectingPolicy;
    }

    public @NonNull BluetoothAudioPolicy setInBandRingPolicy(int inBandRingPolicy) {
        mInBandRingPolicy = inBandRingPolicy;
        return this;
    }

    public int getInBandRingPolicy() {
        return mInBandRingPolicy;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("BluetoothAudioPolicy{");
        builder.append("mCallPickUpPolicy: ");
        builder.append(mCallPickUpPolicy);
        builder.append(", mConnectingPolicy: ");
        builder.append(mConnectingPolicy);
        builder.append(", mInBandRingPolicy: ");
        builder.append(mInBandRingPolicy);
        builder.append("}");
        return builder.toString();
    }

    /**
     * {@link Parcelable.Creator} interface implementation.
     */
    public static final @android.annotation.NonNull Parcelable.Creator<BluetoothAudioPolicy> CREATOR =
            new Parcelable.Creator<BluetoothAudioPolicy>() {
                @Override
                public BluetoothAudioPolicy createFromParcel(@NonNull Parcel in) {
                    return new BluetoothAudioPolicy(
                            in.readInt(), in.readInt(), in.readInt());
                }

                @Override
                public BluetoothAudioPolicy[] newArray(int size) {
                    return new BluetoothAudioPolicy[size];
                }
            };

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mCallPickUpPolicy);
        out.writeInt(mConnectingPolicy);
        out.writeInt(mInBandRingPolicy);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
