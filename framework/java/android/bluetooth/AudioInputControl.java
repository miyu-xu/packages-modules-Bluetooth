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
import static android.bluetooth.BluetoothUtils.callService;
import static android.bluetooth.BluetoothUtils.logRemoteException;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import com.android.bluetooth.flags.Flags;

import bluetooth.constants.AudioInputType;
import bluetooth.constants.aics.AudioInputStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
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

    /** Not Muted */
    public static final int MUTE_NOT_MUTED = bluetooth.constants.aics.Mute.NOT_MUTED;

    /** Muted */
    public static final int MUTE_MUTED = bluetooth.constants.aics.Mute.MUTED;

    /** Disabled */
    public static final int MUTE_DISABLED = bluetooth.constants.aics.Mute.DISABLED;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"MUTE_"},
            value = {
                MUTE_NOT_MUTED,
                MUTE_MUTED,
                MUTE_DISABLED,
            })
    public @interface Mute {}

    /** Manual Only */
    public static final int GAIN_MODE_MANUAL_ONLY = bluetooth.constants.aics.GainMode.MANUAL_ONLY;

    /** Automatic Only */
    public static final int GAIN_MODE_AUTOMATIC_ONLY =
            bluetooth.constants.aics.GainMode.AUTOMATIC_ONLY;

    /** Manual */
    public static final int GAIN_MODE_MANUAL = bluetooth.constants.aics.GainMode.MANUAL;

    /** Automatic */
    public static final int GAIN_MODE_AUTOMATIC = bluetooth.constants.aics.GainMode.AUTOMATIC;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"GAIN_MODE_"},
            value = {
                GAIN_MODE_MANUAL_ONLY,
                GAIN_MODE_AUTOMATIC_ONLY,
                GAIN_MODE_MANUAL,
                GAIN_MODE_AUTOMATIC,
            })
    public @interface GainMode {}

    private final IBluetoothVolumeControl mService;
    private final @NonNull Descriptor mDescriptor;
    private final AttributionSource mAttributionSource;
    private final CallbackWrapper<AudioInputCallback, IBluetoothVolumeControl> mCallbackWrapper;

    /** @hide */
    public AudioInputControl(
            @NonNull Descriptor descriptor,
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source) {
        mDescriptor = requireNonNull(descriptor);
        mService = requireNonNull(service);
        mAttributionSource = requireNonNull(source);
        mCallbackWrapper =
                new CallbackWrapper<AudioInputCallback, IBluetoothVolumeControl>(
                        this::registerCallbackFn, this::unregisterCallbackFn);
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    private void registerCallbackFn(IBluetoothVolumeControl vcs) {
        try {
            vcs.registerAudioInputControlCallback(mCallback, mDescriptor, mAttributionSource);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    private void unregisterCallbackFn(IBluetoothVolumeControl vcs) {
        try {
            vcs.unregisterAudioInputControlCallback(mCallback, mDescriptor, mAttributionSource);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
    }

    private final IAudioInputCallback mCallback =
            new IAudioInputCallback.Stub() {
                @Override
                @RequiresNoPermission
                public void onDescriptionChanged(String description) {
                    mCallbackWrapper.forEach(cb -> cb.onDescriptionChanged(description));
                }

                @Override
                @RequiresNoPermission
                public void onStatusChanged(int status) {
                    mCallbackWrapper.forEach(cb -> cb.onStatusChanged(status));
                }

                @Override
                @RequiresNoPermission
                public void onAudioInputStateChanged(AudioInputState inputState) {
                    mCallbackWrapper.forEach(
                            cb -> cb.onGainSettingChanged(inputState.mGainSetting));
                    mCallbackWrapper.forEach(cb -> cb.onMuteChanged(inputState.mMute));
                    mCallbackWrapper.forEach(cb -> cb.onGainModeChanged(inputState.mGainMode));
                }
            };

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
            logRemoteException(TAG, e);
            return Collections.emptyList();
        }
    }

    /**
     * This class provides a callback that is invoked when value changes on the remote device.
     *
     * @hide
     */
    @SystemApi
    public interface AudioInputCallback {
        /** @hide */
        @SystemApi
        default void onDescriptionChanged(@NonNull String description) {}

        /** @hide */
        @SystemApi
        default void onStatusChanged(@Status int status) {}

        /** @hide */
        @SystemApi
        default void onGainModeChanged(@GainMode int gainMode) {}

        /** @hide */
        @SystemApi
        default void onMuteChanged(@Mute int mute) {}

        /** @hide */
        @SystemApi
        default void onGainSettingChanged(int gainSetting) {}
    }

    /**
     * Register a {@link AudioInputCallback}
     *
     * <p>Repeated registration of the same <var>callback</var> object will have no effect after the
     * first call to this method, even when the <var>executor</var> is different. API caller would
     * have to call {@link #unregisterCallback(Callback)} with the same callback object before
     * registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link AudioInputCallback}
     * @throws IllegalArgumentException if a null executor, or callback is given
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void registerCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull AudioInputCallback callback) {
        mCallbackWrapper.registerCallback(mService, callback, executor);
    }

    /**
     * Unregister the specified {@link AudioInputCallback}.
     *
     * <p>The same {@link AudioInputCallback} object used when calling {@link
     * #registerCallback(Executor, AudioInputCallback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link AudioInputCallback}
     * @throws IllegalArgumentException when callback is null or when no callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void unregisterCallback(@NonNull AudioInputCallback callback) {
        mCallbackWrapper.unregisterCallback(mService, callback);
    }

    /**
     * @return The Audio Input Type as defined in Audio Input Control Service 1.0 - 3.3.
     */
    @RequiresNoPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Type int getType() {
        return callService(
                mService,
                s -> s.getAudioInputType(mDescriptor, mAttributionSource),
                AudioInputType.UNSPECIFIED);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     */
    @RequiresNoPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getGainSettingUnit() {
        return callService(
                mService, s -> s.getAudioInputGainSettingUnit(mDescriptor, mAttributionSource), 0);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     */
    @RequiresNoPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull String getDescription() {
        return callService(
                mService, s -> s.getAudioInputDescription(mDescriptor, mAttributionSource), "");
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     */
    @RequiresNoPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setDescription(@NonNull String description) {
        return callService(
                mService,
                s -> s.setAudioInputDescription(mDescriptor, mAttributionSource, description),
                false);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     */
    @RequiresNoPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Status int getStatus() {
        return callService(
                mService,
                s -> s.getAudioInputStatus(mDescriptor, mAttributionSource),
                (int) AudioInputStatus.INACTIVE);
    }

    /** @hide */
    public static final class Descriptor implements Parcelable {
        public final @NonNull BluetoothDevice mDevice;
        public final int mInstanceId;

        /** 3.3. Audio Input Type */
        public String mDescription;

        public Descriptor(@NonNull BluetoothDevice device, int instanceId) {
            mDevice = requireNonNull(device);
            mInstanceId = instanceId;
        }

        private Descriptor(Parcel in) {
            this(BluetoothDevice.CREATOR.createFromParcel(in), in.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final @NonNull Creator<Descriptor> CREATOR =
                new Creator<>() {
                    public Descriptor createFromParcel(Parcel in) {
                        return new Descriptor(in);
                    }

                    public Descriptor[] newArray(int size) {
                        return new Descriptor[size];
                    }
                };

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            mDevice.writeToParcel(out, flags);
            out.writeInt(mInstanceId);
        }
    }

    /** 3.1. Audio Input State @hide */
    public static final class AudioInputState implements Parcelable {
        /** 3.1.1 Gain_Setting field */
        int mGainSetting;

        /** 3.1.2 Mute field */
        @Mute int mMute;

        /** 3.1.3 Gain_Mode field */
        @GainMode int mGainMode;

        public AudioInputState(int gainSetting, @Mute int mute, @GainMode int gainMode) {
            mGainSetting = gainSetting;
            mMute = mute;
            mGainMode = gainMode;
        }

        private AudioInputState(Parcel in) {
            this(in.readInt(), in.readInt(), in.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final @NonNull Creator<AudioInputState> CREATOR =
                new Creator<>() {
                    public AudioInputState createFromParcel(Parcel in) {
                        return new AudioInputState(in);
                    }

                    public AudioInputState[] newArray(int size) {
                        return new AudioInputState[size];
                    }
                };

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeInt(mGainSetting);
            out.writeInt(mMute);
            out.writeInt(mGainMode);
        }
    }
}
