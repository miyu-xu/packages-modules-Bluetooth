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
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.os.RemoteException;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class provides APIs to control a remote AICS (Audio Input Control Service)
 *
 * <p>Each {@link AudioInputControl} object represents an instance of the Audio Input Control
 * Service (AICS) on the remote device. A device may have multiple instances of the AICS, as
 * described in the <a href="https://www.bluetooth.com/specifications/specs/aics-1-0/">Audio Input
 * Control Service Specification (AICS 1.0)</a>.
 *
 * @see BluetoothVolumeControl#getAudioInputControlPoints
 * @hide
 */
@FlaggedApi(Flags.FLAG_AICS_API)
@SystemApi
public final class AudioInputControl {
    private static final String TAG = AudioInputControl.class.getSimpleName();

    /** Unspecified Input @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_UNSPECIFIED =
            bluetooth.constants.AudioInputType.UNSPECIFIED;

    /** Bluetooth Audio Stream @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_BLUETOOTH =
            bluetooth.constants.AudioInputType.BLUETOOTH;

    /** Microphone @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_MICROPHONE =
            bluetooth.constants.AudioInputType.MICROPHONE;

    /** Analog Interface @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_ANALOG = bluetooth.constants.AudioInputType.ANALOG;

    /** Digital Interface @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_DIGITAL = bluetooth.constants.AudioInputType.DIGITAL;

    /** AM/FM/XM/etc. @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_RADIO = bluetooth.constants.AudioInputType.RADIO;

    /** Streaming Audio Source @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_STREAMING =
            bluetooth.constants.AudioInputType.STREAMING;

    /** Transparency/Pass-through @hide */
    @SystemApi
    public static final int AUDIO_INPUT_TYPE_AMBIENT = bluetooth.constants.AudioInputType.AMBIENT;

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
    public @interface AudioInputType {}

    /** Inactive @hide */
    @SystemApi
    public static final int AUDIO_INPUT_STATUS_INACTIVE =
            bluetooth.constants.aics.AudioInputStatus.INACTIVE;

    /** Active @hide */
    @SystemApi
    public static final int AUDIO_INPUT_STATUS_ACTIVE =
            bluetooth.constants.aics.AudioInputStatus.ACTIVE;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"AUDIO_INPUT_STATUS_"},
            value = {
                AUDIO_INPUT_STATUS_INACTIVE,
                AUDIO_INPUT_STATUS_ACTIVE,
            })
    public @interface AudioInputStatus {}

    /** Not Muted @hide */
    @SystemApi public static final int MUTE_NOT_MUTED = bluetooth.constants.aics.Mute.NOT_MUTED;

    /** Muted @hide */
    @SystemApi public static final int MUTE_MUTED = bluetooth.constants.aics.Mute.MUTED;

    /** Disabled @hide */
    @SystemApi public static final int MUTE_DISABLED = bluetooth.constants.aics.Mute.DISABLED;

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

    /** Manual Only @hide */
    @SystemApi
    public static final int GAIN_MODE_MANUAL_ONLY = bluetooth.constants.aics.GainMode.MANUAL_ONLY;

    /** Automatic Only @hide */
    @SystemApi
    public static final int GAIN_MODE_AUTOMATIC_ONLY =
            bluetooth.constants.aics.GainMode.AUTOMATIC_ONLY;

    /** Manual @hide */
    @SystemApi public static final int GAIN_MODE_MANUAL = bluetooth.constants.aics.GainMode.MANUAL;

    /** Automatic @hide */
    @SystemApi
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

    /** Local identifier of the AICS */
    private final int mInstanceId;

    private final IBluetoothVolumeControl mService;
    private final @NonNull BluetoothDevice mDevice;
    private final AttributionSource mAttributionSource;
    private final CallbackWrapper<AudioInputCallback, IBluetoothVolumeControl> mCallbackWrapper;

    /** @hide */
    AudioInputControl(
            @NonNull BluetoothDevice device,
            int id,
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source) {
        mDevice = requireNonNull(device);
        mInstanceId = id;
        mService = requireNonNull(service);
        mAttributionSource = requireNonNull(source);
        mCallbackWrapper =
                new CallbackWrapper<AudioInputCallback, IBluetoothVolumeControl>(
                        this::registerCallbackFn, this::unregisterCallbackFn);
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    private void registerCallbackFn(IBluetoothVolumeControl vcs) {
        try {
            vcs.registerAudioInputControlCallback(
                    mAttributionSource, mDevice, mInstanceId, mCallback);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    private void unregisterCallbackFn(IBluetoothVolumeControl vcs) {
        try {
            vcs.unregisterAudioInputControlCallback(
                    mAttributionSource, mDevice, mInstanceId, mCallback);
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
                    mCallbackWrapper.forEach(cb -> cb.onAudioInputStateChanged(status));
                }

                @Override
                @RequiresNoPermission
                public void onStateChanged(int gainSetting, int mute, int gainMode) {
                    mCallbackWrapper.forEach(cb -> cb.onGainSettingChanged(gainSetting));
                    mCallbackWrapper.forEach(cb -> cb.onMuteChanged(mute));
                    mCallbackWrapper.forEach(cb -> cb.onGainModeChanged(gainMode));
                }

                @Override
                @RequiresNoPermission
                public void onSetGainSettingFailed() {
                    mCallbackWrapper.forEach(cb -> cb.onSetGainSettingFailed());
                }

                @Override
                @RequiresNoPermission
                public void onSetGainModeFailed() {
                    mCallbackWrapper.forEach(cb -> cb.onSetGainModeFailed());
                }

                @Override
                @RequiresNoPermission
                public void onSetMuteFailed() {
                    mCallbackWrapper.forEach(cb -> cb.onSetMuteFailed());
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
        int numberOfAics = 0;
        try {
            numberOfAics = service.getAudioInputControlPointsCountForDevice(source, device);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
        return IntStream.range(0, numberOfAics)
                .mapToObj(i -> new AudioInputControl(device, i, service, source))
                .collect(Collectors.toList());
    }

    /**
     * This callback is invoked when the remote AICS is notifying local client of a value changed.
     * It can also be invoked when a locally initiated operation failed.
     *
     * @hide
     */
    @SystemApi
    public interface AudioInputCallback {
        /** see {@link #setDescription(String)} @hide */
        @SystemApi
        default void onDescriptionChanged(@NonNull String description) {}

        /** see {@link #getStatus()} @hide */
        @SystemApi
        default void onAudioInputStateChanged(@AudioInputStatus int status) {}

        /** see {@link #setGainSetting(int)} @hide */
        @SystemApi
        default void onGainSettingChanged(int gainSetting) {}

        /** see {@link #setGainSetting(int)} @hide */
        @SystemApi
        default void onSetGainSettingFailed() {}

        /** see {@link #setMute(int)} @hide */
        @SystemApi
        default void onMuteChanged(@Mute int mute) {}

        /** see {@link #setMute(int)} @hide */
        @SystemApi
        default void onSetMuteFailed() {}

        /** see {@link #setGainMode(int)} @hide */
        @SystemApi
        default void onGainModeChanged(@GainMode int gainMode) {}

        /** see {@link #setGainMode(int)} @hide */
        @SystemApi
        default void onSetGainModeFailed() {}
    }

    /**
     * Register an {@link AudioInputCallback} to receive callbacks when the state of the AICS on the
     * remote device changes.
     *
     * <p>Repeated registration of the same callback object will have no effect after the first call
     * to this method, even when the executor is different. API caller would have to call {@link
     * #unregisterCallback(AudioInputCallback)} with the same callback object before registering it
     * again.
     *
     * <p>Callbacks are automatically unregistered when application process goes away.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link AudioInputCallback}
     * @throws IllegalArgumentException if callback is already registered
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
     * Unregister the {@link AudioInputCallback}.
     *
     * <p>The same {@link AudioInputCallback} object used when calling {@link
     * #registerCallback(Executor, AudioInputCallback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away.
     *
     * @param callback user implementation of the {@link AudioInputCallback}
     * @throws IllegalArgumentException when no callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void unregisterCallback(@NonNull AudioInputCallback callback) {
        mCallbackWrapper.unregisterCallback(mService, callback);
    }

    /**
     * Gets the Audio Input Type.
     *
     * @return The Audio Input Type as defined in AICS 1.0 - 3.3.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @AudioInputType int getAudioInputType() {
        return callService(
                mService,
                s -> s.getAudioInputType(mAttributionSource, mDevice, mInstanceId),
                bluetooth.constants.AudioInputType.UNSPECIFIED);
    }

    /**
     * Gets the unit of the gain setting.
     *
     * @return The Gain Setting Units as defined in AICS 1.0 - 3.2.1.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @IntRange(from = 0, to = 0xFF) int getGainSettingUnit() {
        return callService(
                mService,
                s -> s.getAudioInputGainSettingUnit(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * Gets the minimum value for the gain setting.
     *
     * @return The minimum Gain Setting as defined in AICS 1.0 - 3.2.2.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @IntRange(from = -128, to = 127) int getGainSettingMin() {
        return callService(
                mService,
                s -> s.getAudioInputGainSettingMin(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * Gets the maximum value for the gain setting.
     *
     * @return The maximum Gain Setting as defined in AICS 1.0 - 3.2.3.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @IntRange(from = -128, to = 127) int getGainSettingMax() {
        return callService(
                mService,
                s -> s.getAudioInputGainSettingMax(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * Gets the description.
     *
     * <p>Register an {@link AudioInputCallback} to be notified via {@link
     * AudioInputCallback#onDescriptionChanged} when the description changed.
     *
     * @return The description as defined in AICS 1.0 - 3.6.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull String getDescription() {
        return callService(
                mService,
                s -> s.getAudioInputDescription(mAttributionSource, mDevice, mInstanceId),
                "");
    }

    /**
     * Check whether the description is writable as defined in AICS 1.0 - 3.6.
     *
     * @return true if the description can be written to, false otherwise.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean isDescriptionWritable() {
        return callService(
                mService,
                s -> s.isAudioInputDescriptionWritable(mAttributionSource, mDevice, mInstanceId),
                false);
    }

    /**
     * Sets the description as defined in AICS 1.0 - 3.6.
     *
     * <p>The operation will fail if the description is not writable. This can be verified with
     * {@link #isDescriptionWritable}
     *
     * <p>Register an {@link AudioInputCallback} to be notified via {@link
     * AudioInputCallback#onDescriptionChanged} when the description changed is applied on remote
     * device.
     *
     * @param description The description of the AICS.
     * @return true if the operation is successfully initiated, false otherwise.
     * @throws IllegalStateException if the description is not writable
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setDescription(@NonNull String description) {
        requireNonNull(description);
        return callService(
                mService,
                s ->
                        s.setAudioInputDescription(
                                mAttributionSource, mDevice, mInstanceId, description),
                false);
    }

    /**
     * Gets the Audio Input Status.
     *
     * <p>Register an {@link AudioInputCallback} to be notified via {@link
     * AudioInputCallback#onAudioInputStateChanged} when the status changed.
     *
     * @return The Audio Input Status as defined in AICS 1.0 - 3.4.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @AudioInputStatus int getAudioInputStatus() {
        return callService(
                mService,
                s -> s.getAudioInputStatus(mAttributionSource, mDevice, mInstanceId),
                (int) bluetooth.constants.aics.AudioInputStatus.INACTIVE);
    }

    /**
     * Gets the gain setting.
     *
     * <p>Register an {@link AudioInputCallback} to be notified via {@link
     * AudioInputCallback#onGainSettingChanged} when the gain setting changed.
     *
     * @return The current gain setting as defined in AICS 1.0 - 2.2.1.1.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @IntRange(from = -128, to = 127) int getGainSetting() {
        return callService(
                mService,
                s -> s.getAudioInputGainSetting(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * Sets the gain setting as defined in AICS 1.0 - 3.5.2.1.
     *
     * <p>This method is only applicable when the gain mode is set to {@link #MANUAL} or {@link
     * #MANUAL_ONLY}. If the gain mode is {@link #AUTOMATIC} or {@link #AUTOMATIC_ONLY} or if an
     * invalid gain setting value is provided, the operation will fail.
     *
     * <p>Register an {@link AudioInputControl.AudioInputCallback} to be notified via
     *
     * <ul>
     *   <li>{@link AudioInputCallback#onGainSettingChanged()} when the gain setting is changed by
     *       the remote device.
     *   <li>{@link AudioInputCallback#onSetGainSettingFailed} if the gain setting cannot be set.
     * </ul>
     *
     * @param gainSetting The desired gain setting value. Refer to {@link #getGainSettingMin()} and
     *     {@link #getGainSettingMax()} for the allowable range.
     * @return true if the operation is successfully initiated, false otherwise. The callback {@link
     *     AudioInputCallback#onSetGainSettingFailed()} will not be call if false is returned
     * @throws IllegalStateException if the gain mode is {@link #AUTOMATIC} or {@link
     *     #AUTOMATIC_ONLY}
     * @throws IllegalArgumentException if the gain setting is not in range
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setGainSetting(@IntRange(from = -128, to = 127) int gainSetting) {
        return callService(
                mService,
                s ->
                        s.setAudioInputGainSetting(
                                mAttributionSource, mDevice, mInstanceId, gainSetting),
                false);
    }

    /**
     * Gets the gain mode.
     *
     * <p>Register an {@link AudioInputCallback} to be notified via {@link
     * AudioInputCallback#onGainModeChanged} when the gain mode changed.
     *
     * @return The current gain mode as defined in AICS 1.0 - 2.2.1.3.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @GainMode int getGainMode() {
        return callService(
                mService,
                s -> s.getAudioInputGainMode(mAttributionSource, mDevice, mInstanceId),
                (int) bluetooth.constants.aics.GainMode.AUTOMATIC_ONLY);
    }

    /**
     * Sets the gain mode as defined in AICS 1.0 - 3.5.2.4/5.
     *
     * <p>This method is only applicable when the gain mode is set to {@link #MANUAL} or {@link
     * #AUTOMATIC}. If the gain mode is {@link #MANUAL_ONLY} or {@link #AUTOMATIC_ONLY} the
     * operation will fail.
     *
     * <p>Register an {@link AudioInputControl.AudioInputCallback} to be notified via
     *
     * <ul>
     *   <li>{@link AudioInputCallback#onGainModeChanged()} when the gain setting is changed by the
     *       remote device.
     *   <li>{@link AudioInputCallback#onSetGainModeFailed} if the gain mode cannot be set.
     * </ul>
     *
     * @param gainMode The desired gain mode, one of:
     *     <ul>
     *       <li>{@link #GAIN_MODE_MANUAL}
     *       <li>{@link #GAIN_MODE_AUTOMATIC}
     *     </ul>
     *
     * @return true if the operation is successfully initiated, false otherwise. The callback {@link
     *     AudioInputCallback#onSetGainModeFailed()} will not be call if false is returned
     * @throws IllegalStateException if the gain mode is {@link #MANUAL_ONLY} or {@link
     *     #AUTOMATIC_ONLY}
     * @throws IllegalArgumentException if the gain mode value is invalid.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setGainMode(@GainMode int gainMode) {
        if (gainMode != GAIN_MODE_MANUAL && gainMode != GAIN_MODE_AUTOMATIC) {
            throw new IllegalArgumentException("Illegal GainMode value: " + gainMode);
        }
        return callService(
                mService,
                s -> s.setAudioInputGainMode(mAttributionSource, mDevice, mInstanceId, gainMode),
                false);
    }

    /**
     * Gets the mute state.
     *
     * <p>Register an {@link AudioInputCallback} to be notified via {@link
     * AudioInputCallback#onMuteChanged} when the mute state changed.
     *
     * @return The current mute state as defined in AICS 1.0 - 2.2.1.2.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Mute int getMute() {
        return callService(
                mService,
                s -> s.getAudioInputMute(mAttributionSource, mDevice, mInstanceId),
                (int) bluetooth.constants.aics.Mute.DISABLED);
    }

    /**
     * Sets the mute state as defined in AICS 1.0 - 3.5.2.2/3.
     *
     * <p>The operation will fail if the mute is {@link #MUTE_DISABLED}.
     *
     * <p>Register an {@link AudioInputControl.AudioInputCallback} to be notified via
     *
     * <ul>
     *   <li>{@link AudioInputCallback#onMuteChanged()} when the mute state is changed by the remote
     *       device.
     *   <li>{@link AudioInputCallback#onSetMuteFailed} if the mute state cannot be set.
     * </ul>
     *
     * @param mute the new mute state. One of
     *     <ul>
     *       <li>{@link #MUTE_MUTED}
     *       <li>{@link #MUTE_NOT_MUTED}
     *     </ul>
     *
     * @return true on success, false otherwise.
     * @throws IllegalStateException if the mute state is {@link #MUTE_DISABLED}
     * @throws IllegalArgumentException if the provided {@code mute} is not valid
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setMute(@Mute int mute) {
        if (mute != MUTE_NOT_MUTED && mute != MUTE_MUTED) {
            throw new IllegalArgumentException("Illegal mute value: " + mute);
        }
        return callService(
                mService,
                s -> s.setAudioInputMute(mAttributionSource, mDevice, mInstanceId, mute),
                false);
    }
}
