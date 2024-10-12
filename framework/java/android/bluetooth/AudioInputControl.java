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

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.flags.Flags;

import bluetooth.constants.AudioInputType;
import bluetooth.constants.aics.AudioInputStatus;
import bluetooth.constants.aics.GainModeField;
import bluetooth.constants.aics.MuteField;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
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
    public static final int MUTE_NOT_MUTED = MuteField.NOT_MUTED;

    /** Muted */
    public static final int MUTE_MUTED = MuteField.MUTED;

    /** Disabled */
    public static final int MUTE_DISABLED = MuteField.DISABLED;

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
    public static final int GAIN_MODE_MANUAL_ONLY = GainModeField.MANUAL_ONLY;

    /** Automatic Only */
    public static final int GAIN_MODE_AUTOMATIC_ONLY = GainModeField.AUTOMATIC_ONLY;

    /** Manual */
    public static final int GAIN_MODE_MANUAL = GainModeField.MANUAL;

    /** Automatic */
    public static final int GAIN_MODE_AUTOMATIC = GainModeField.AUTOMATIC;

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
    @SuppressLint("AndroidFrameworkRequiresPermission") // Consumer wrongly report permission
    public AudioInputControl(
            @NonNull Descriptor descriptor,
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source) {
        mDescriptor = requireNonNull(descriptor);
        mService = requireNonNull(service);
        mAttributionSource = requireNonNull(source);
        Consumer<IBluetoothVolumeControl> registerCallbackFn =
                (IBluetoothVolumeControl vcs) -> {
                    try {
                        vcs.registerAudioInputControlCallback(
                                mCallback, mDescriptor, mAttributionSource);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                    }
                };
        Consumer<IBluetoothVolumeControl> unregisterCallbackFn =
                (IBluetoothVolumeControl vcs) -> {
                    try {
                        vcs.registerAudioInputControlCallback(
                                mCallback, mDescriptor, mAttributionSource);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                    }
                };
        mCallbackWrapper = new CallbackWrapper(registerCallbackFn, unregisterCallbackFn);
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
                public void onGainStatusChanged(int status) {
                    mCallbackWrapper.forEach(cb -> cb.onGainStatusChanged(status));
                }

                @Override
                @RequiresNoPermission
                public void onGainModeChanged(int gainMode) {
                    mCallbackWrapper.forEach(cb -> cb.onGainModeChanged(gainMode));
                }

                @Override
                @RequiresNoPermission
                public void onMuteChanged(int mute) {
                    mCallbackWrapper.forEach(cb -> cb.onMuteChanged(mute));
                }

                @Override
                @RequiresNoPermission
                public void onGainSettingChanged(int gainSetting) {
                    mCallbackWrapper.forEach(cb -> cb.onGainStatusChanged(gainSetting));
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
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
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
        default void onGainStatusChanged(@Status int status) {}

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
    public @Type int getType() {
        return mDescriptor.mType;
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     */
    @RequiresNoPermission
    public @Status int getStatus() {
        return mDescriptor.mStatus;
    }

    /**
     * @return something
     */
    @RequiresNoPermission
    public int getGainSettingUnit() {
        return mDescriptor.mGainSettingProperties.mUnits;
    }

    /** @hide */
    public static final class Descriptor implements Parcelable {
        public final @NonNull BluetoothDevice mDevice;
        public final int mInstanceId;

        /** 3.3. Audio Input Type */
        public final @Type int mType;

        /** 3.4. Audio Input Status */
        public @Status int mStatus;

        /** 3.1. Audio Input State */
        public final AudioInputState mAudioInputState;

        public final GainSettingProperties mGainSettingProperties;

        public Descriptor(
                @NonNull BluetoothDevice device,
                int instanceId,
                @Type int type,
                @Status int status,
                AudioInputState audioInputState,
                GainSettingProperties gainSettingProperties) {
            mDevice = requireNonNull(device);
            mInstanceId = instanceId;
            mType = type;
            mStatus = status;
            mAudioInputState = audioInputState;
            mGainSettingProperties = gainSettingProperties;
        }

        /** 3.1. Audio Input State */
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

        /** 3.2. Gain Setting Properties */
        public static final class GainSettingProperties implements Parcelable {
            /** 3.2.1. Gain_Setting_Units field */
            final int mUnits;

            /** 3.2.2. Gain_Setting_Minimum field */
            final int mMinimum;

            /** 3.2.3. Gain_Setting_Maximum field */
            final int mMaximum;

            public GainSettingProperties(int units, int minimum, int maximum) {
                mUnits = units;
                mMinimum = minimum;
                mMaximum = maximum;
            }

            private GainSettingProperties(Parcel in) {
                this(in.readInt(), in.readInt(), in.readInt());
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final @NonNull Creator<GainSettingProperties> CREATOR =
                    new Creator<>() {
                        public GainSettingProperties createFromParcel(Parcel in) {
                            return new GainSettingProperties(in);
                        }

                        public GainSettingProperties[] newArray(int size) {
                            return new GainSettingProperties[size];
                        }
                    };

            @Override
            public void writeToParcel(@NonNull Parcel out, int flags) {
                out.writeInt(mUnits);
                out.writeInt(mMinimum);
                out.writeInt(mMaximum);
            }
        }

        private Descriptor(Parcel in) {
            this(
                    BluetoothDevice.CREATOR.createFromParcel(in),
                    in.readInt(),
                    in.readInt(),
                    in.readInt(),
                    AudioInputState.CREATOR.createFromParcel(in),
                    GainSettingProperties.CREATOR.createFromParcel(in));
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
            out.writeInt(mType);
            out.writeInt(mStatus);
            mAudioInputState.writeToParcel(out, flags);
            mGainSettingProperties.writeToParcel(out, flags);
        }
    }
}
