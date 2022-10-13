/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents Bluetooth Audio Policies of a Handsfree (HF) device (if HFP is used)
 * and Call Terminal (CT) device (if BLE Audio is used), which describes the
 * preferences of allowing or disallowing audio based on the use cases. The HF/CT
 * devices shall send objects of this class to send its preference to the AG/CG
 * devices.
 *
 * <p> HF/CT side applications on can use {@link BluetoothDevice#setAudioPolicy}
 * API to set and send a {@link BluetoothAudioPolicy} object containing the
 * preference/policy values. This object will be stored in the memory of HF/CT
 * side, will be send to the AG/CG side using Android Specific AT Commands and will
 * be stored in the AG side memory and database.
 *
 * <p> HF/CT side API {@link BluetoothDevice#getAudioPolicy} can be used to retrieve
 * the stored audio policies currently.
 *
 * <p> Note that the setter APIs of this class will only set the values of the
 * object. To actually set the policies, API {@link BluetoothDevice#setAudioPolicy}
 * must need to be invoked with the {@link BluetoothAudioPolicy} object.
 *
 *
 *
 * @hide
 */
@SystemApi
public final class BluetoothAudioPolicy implements Parcelable {

    /* Call audio preferences */
    /**
     * @hide
    */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = {"POLICY_"},
        value = {
            /** Call audio behavior not configured. */
            POLICY_DEFAULT,
            /** Call audio is preferred. */
            POLICY_ALLOWED,
            /** Call audio is not preferred. */
            POLICY_NOT_ALLOWED,
        }
    )
    public @interface AudioPolicyValues{}

    /** Call audio behavior not configured. */
    public static final int POLICY_DEFAULT = 0;
    /** Call audio is preferred. */
    public static final int POLICY_ALLOWED = 1;
    /** Call audio is not preferred. */
    public static final int POLICY_NOT_ALLOWED = 2;

    /**
     * Remote support status of audio policy feature is unknown/unconfigured
     *
     * @hide
     */
    @SystemApi
    public static final int REMOTE_STATUS_UNKNOWN = 0;

    /**
     * Remote support status of audio policy feature is supported
     *
     * @hide
     */
    @SystemApi
    public static final int REMOTE_STATUS_SUPPORTED = 1;

    /**
     * Remote support status of audio policy feature is not supported
     *
     * @hide
     */
    @SystemApi
    public static final int REMOTE_STATUS_NOT_SUPPORTED = 2;

    @AudioPolicyValues private int mCallPickUpPolicy;
    @AudioPolicyValues private int mConnectingPolicy;
    @AudioPolicyValues private int mInBandRingPolicy;

    public BluetoothAudioPolicy() {
        mCallPickUpPolicy = POLICY_DEFAULT;
        mConnectingPolicy = POLICY_DEFAULT;
        mInBandRingPolicy = POLICY_DEFAULT;
    }

    /**
     * @hide
     */
    public BluetoothAudioPolicy(int callPickUpPolicy,
            int connectingPolicy, int inBandRingPolicy) {
        mCallPickUpPolicy = callPickUpPolicy;
        mConnectingPolicy = connectingPolicy;
        mInBandRingPolicy = inBandRingPolicy;
    }

    /**
     * Set Call pick up policy
     *
     * @return reference to the current object
     *
     */
    public @NonNull BluetoothAudioPolicy setCallPickUpPolicy(int callPickUpPolicy) {
        mCallPickUpPolicy = callPickUpPolicy;
        return this;
    }

    /**
     * Get Call pick up audio policy
     *
     * @return the call pick up audio policy value
     *
     */
    public @AudioPolicyValues int getCallPickUpPolicy() {
        return mCallPickUpPolicy;
    }

    /**
     * Set during connection audio up policy
     *
     * @return reference to the current object
     *
     */
    public @NonNull BluetoothAudioPolicy setConnectingPolicy(int connectingPolicy) {
        mConnectingPolicy = connectingPolicy;
        return this;
    }

    /**
     * Get during connection audio up policy
     *
     * @return the during connection audio policy value
     *
     */
    public @AudioPolicyValues int getConnectingPolicy() {
        return mConnectingPolicy;
    }

    /**
     * Set In band ringtone audio up policy
     *
     * @return reference to the current object
     *
     */
    public @NonNull BluetoothAudioPolicy setInBandRingPolicy(int inBandRingPolicy) {
        mInBandRingPolicy = inBandRingPolicy;
        return this;
    }

    /**
     * Get In band ringtone audio up policy
     *
     * @return the in band ringtone audio policy value
     *
     */
    public @AudioPolicyValues int getInBandRingPolicy() {
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
    public static final @android.annotation.NonNull Parcelable.Creator<BluetoothAudioPolicy>
            CREATOR = new Parcelable.Creator<BluetoothAudioPolicy>() {
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof BluetoothAudioPolicy) {
            BluetoothAudioPolicy other = (BluetoothAudioPolicy) o;
            return (other.mCallPickUpPolicy == mCallPickUpPolicy
                    && other.mConnectingPolicy == mConnectingPolicy
                    && other.mInBandRingPolicy == mInBandRingPolicy);
        }
        return false;
    }

    /**
     * Returns a hash representation of this BluetoothCodecConfig
     * based on all the config values.
     */
    @Override
    public int hashCode() {
        return Objects.hash(mCallPickUpPolicy, mConnectingPolicy, mInBandRingPolicy);
    }
}
