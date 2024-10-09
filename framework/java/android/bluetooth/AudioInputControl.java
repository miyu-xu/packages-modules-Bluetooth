/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides APIs to control a remote AICS(Audio Input Control Service)
 *
 * @see BluetoothVolumeControl#getAudioInputControlPoints
 * @hide
 */
@FlaggedApi(Flags.FLAG_AICS_API)
@SystemApi
public final class AudioInputControl {
    private static final String TAG = AudioInputControl.class.getSimpleName();

    /** Unspecified Input */
    public static final int AUDIO_INPUT_TYPE_UNSPECIFIED = AudioInputType.UNSPECIFIED;

    /** Bluetooth Audio Stream */
    public static final int AUDIO_INPUT_TYPE_BLUETOOTH = AudioInputType.BLUETOOTH;

    /** Microphone */
    public static final int AUDIO_INPUT_TYPE_MICROPHONE = AudioInputType.MICROPHONE;

    /** Analog Interface */
    public static final int AUDIO_INPUT_TYPE_ANALOG = AudioInputType.ANALOG;

    /** Digital Interface */
    public static final int AUDIO_INPUT_TYPE_DIGITAL = AudioInputType.DIGITAL;

    /** AM/FM/XM/etc. */
    public static final int AUDIO_INPUT_TYPE_RADIO = AudioInputType.RADIO;

    /** Streaming Audio Source */
    public static final int AUDIO_INPUT_TYPE_STREAMING = AudioInputType.STREAMING;

    /** Transparency/Pass-through */
    public static final int AUDIO_INPUT_TYPE_AMBIENT = AudioInputType.AMBIENT;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"AUDIO_INPUT_TYPE_"},
            value = {
                AUDIO_INPUT_TYPE_UNSPECIFIED,
                AUDIO_INPUT_TYPE_BLUETOOTH,
                AUDIO_INPUT_TYPE_MICROPHONE,
                AUDIO_INPUT_TYPE_ANALOG,
                AUDIO_INPUT_TYPE_DIGITAL,
                AUDIO_INPUT_TYPE_RADIO,
                AUDIO_INPUT_TYPE_STREAMING,
                AUDIO_INPUT_TYPE_AMBIENT,
            })
    public @interface Type {}

    /** Inactive */
    public static final int AUDIO_INPUT_STATUS_INACTIVE = AudioInputStatus.INACTIVE;

    /** Active */
    public static final int AUDIO_INPUT_STATUS_ACTIVE = AudioInputStatus.ACTIVE;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"AUDIO_INPUT_STATUS_"},
            value = {
                AUDIO_INPUT_STATUS_INACTIVE,
                AUDIO_INPUT_STATUS_ACTIVE,
            })
    public @interface Status {}

    private final IBluetoothVolumeControl mService;
    private final @NonNull AudioInputControlParcel mParcel;
    private AttributionSource mAttributionSource;

    /** @hide */
    // public AudioInputControl(@NonNull UUID uuid) {
    //     mParcel = new AudioInputControlParcel(uuid);
    //     mService = null;
    // }

    /** @hide */
    public AudioInputControl(
            @NonNull AudioInputControlParcel parcel,
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source) {
        mParcel = requireNonNull(parcel);
        mService = requireNonNull(service);
        mAttributionSource = requireNonNull(source);
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    static List<AudioInputControl> getAudioInputControlPoints(
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source,
            @NonNull BluetoothDevice device) {
        requireNonNull(service);
        requireNonNull(source);
        requireNonNull(device);
        try {
            return service.getAudioInputControlPoints(source, device).stream()
                    .map(p -> new AudioInputControl(p, service, source))
                    .collect(Collectors.toList());
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            return Collections.emptyList();
        }
    }

    /**
     * @return The Audio Input Type as defined in Audio Input Control Service 1.0 - 3.3.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Type int getType() {
        try {
            return mService.getAudioInputType(mAttributionSource, mParcel);
        } catch (RemoteException e) {
            return AUDIO_INPUT_TYPE_UNSPECIFIED;
        }
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Status int getStatus() {
        try {
            return mService.getAudioInputStatus(mAttributionSource, mParcel);
        } catch (RemoteException e) {
            return AUDIO_INPUT_STATUS_INACTIVE;
        }
    }

    /** @hide */
    public static final class AudioInputControlParcel implements Parcelable {
        public final @NonNull BluetoothDevice mDevice;
        public final int mInstanceId;

        public AudioInputControlParcel(@NonNull BluetoothDevice device, int instanceId) {
            mDevice = requireNonNull(device);
            mInstanceId = instanceId;
        }

        private AudioInputControlParcel(Parcel in) {
            this(BluetoothDevice.CREATOR.createFromParcel(in), in.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final @NonNull Creator<AudioInputControlParcel> CREATOR =
                new Creator<>() {
                    public AudioInputControlParcel createFromParcel(Parcel in) {
                        return new AudioInputControlParcel(in);
                    }

                    public AudioInputControlParcel[] newArray(int size) {
                        return new AudioInputControlParcel[size];
                    }
                };

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            mDevice.writeToParcel(out, flags);
            out.writeInt(mInstanceId);
        }
    }

    // /** @hide */
    // public void setAttributionSource(@NonNull AttributionSource attributionSource) {
    //     mAttributionSource = requireNonNull(attributionSource);
    // }
}
