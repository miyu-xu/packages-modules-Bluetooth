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

package com.android.bluetooth.vc;

import static com.android.bluetooth.Utils.RemoteExceptionIgnoringConsumer;

import android.bluetooth.AudioInputControl;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IAudioInputCallback;
import android.os.RemoteCallbackList;
import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;

import bluetooth.constants.AudioInputType;
import bluetooth.constants.aics.AudioInputStatus;
import bluetooth.constants.aics.GainModeField;
import bluetooth.constants.aics.MuteField;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class VolumeControlInputDescriptor {
    private static final String TAG = VolumeControlInputDescriptor.class.getSimpleName();

    final Descriptor[] mVolumeInputs;

    VolumeControlInputDescriptor(int numberOfExternalInputs) {
        mVolumeInputs = new Descriptor[numberOfExternalInputs];
        for (int i = 0; i < numberOfExternalInputs; i++) {
            mVolumeInputs[i] = new Descriptor(i);
        }
    }

    private static class Descriptor {
        @AudioInputControl.Status int mStatus = AudioInputStatus.INACTIVE;

        @AudioInputControl.Type int mType = AudioInputType.UNSPECIFIED;

        int mGainSetting = 0;

        /* See AICS 1.0 - 3.1.3. Gain_Mode field
         * The Gain_Mode field shall be set to a value that reflects whether gain modes are manual
         * or automatic.
         * - Manual Only, the server allows only manual gain.
         * - Automatic Only, the server allows only automatic gain.
         *
         * For all other Gain_Mode field values, the server allows switchable automatic/manual gain.
         */
        @AudioInputControl.GainMode int mGainMode = GainModeField.MANUAL_ONLY;

        @AudioInputControl.Mute int mMute = MuteField.DISABLED;

        /* See AICS 1.0
         * The Gain_Setting (mGainSetting) field is a signed value for which a single increment or
         * decrement should result in a corresponding increase or decrease of the input amplitude by
         * the value of the Gain_Setting_Units (mGainSettingsUnits) field of the Gain Setting
         * Properties characteristic value.
         */
        int mGainSettingsUnits = 0;

        int mGainSettingsMaxSetting = 0;
        int mGainSettingsMinSetting = 0;

        String mDescription = "";

        final int mIndex;

        private final RemoteCallbackList<IAudioInputCallback> mCallbacks =
                new RemoteCallbackList<>();

        Descriptor(int index) {
            mIndex = index;
        }

        void registerCallback(IAudioInputCallback callback) {
            mCallbacks.register(callback);
        }

        void unregisterCallback(IAudioInputCallback callback) {
            mCallbacks.unregister(callback);
        }

        // need to be synchronized to prevent calling beginBroadcast while the first is not finished
        synchronized void broadcast(
                String logAction, RemoteExceptionIgnoringConsumer<IAudioInputCallback> action) {
            final int itemCount = mCallbacks.beginBroadcast();
            Log.d(TAG, "Broadcasting " + logAction + "() to " + itemCount + " receivers.");
            for (int i = 0; i < itemCount; i++) {
                action.accept(mCallbacks.getBroadcastItem(i));
            }
            mCallbacks.finishBroadcast();
        }
    }

    List<AudioInputControl.Descriptor> toAudioInputControlDescriptor(BluetoothDevice device) {
        return Arrays.stream(mVolumeInputs)
                .map(
                        i ->
                                new AudioInputControl.Descriptor(
                                        device,
                                        i.mIndex,
                                        i.mDescription,
                                        i.mType,
                                        i.mStatus,
                                        new AudioInputControl.Descriptor.AudioInputState(
                                                i.mGainSetting, i.mMute, i.mGainMode),
                                        new AudioInputControl.Descriptor.GainSettingProperties(
                                                i.mGainSettingsUnits,
                                                i.mGainSettingsMinSetting,
                                                i.mGainSettingsMaxSetting)))
                .collect(Collectors.toList());
    }

    int size() {
        return mVolumeInputs.length;
    }

    private boolean isValidId(int id) {
        if (id >= size() || id < 0) {
            Log.e(TAG, "Request fail. Illegal id argument: " + id);
            return false;
        }
        return true;
    }

    void registerCallback(int id, IAudioInputCallback callback) {
        if (!validateId(id)) return;
        mVolumeInputs[id].registerCallback(callback);
    }

    void unregisterCallback(int id, IAudioInputCallback callback) {
        if (!validateId(id)) return;
        mVolumeInputs[id].unregisterCallback(callback);
    }

    void setStatus(int id, int status) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mStatus = status;
        mVolumeInputs[id].broadcast("onGainStatus", (c) -> c.onGainStatusChanged(status));
    }

    int getStatus(int id) {
        if (!isValidId(id)) return AudioInputStatus.INACTIVE;
        return mVolumeInputs[id].mStatus;
    }

    void setDescription(int id, String description) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mDescription = description;
        mVolumeInputs[id].broadcast("onDescription", (c) -> c.onDescriptionChanged(description));
    }

    String getDescription(int id) {
        if (!isValidId(id)) return null;
        return mVolumeInputs[id].mDescription;
    }

    void setType(int id, int type) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mType = type;
    }

    int getType(int id) {
        if (!isValidId(id)) return AudioInputType.UNSPECIFIED;
        return mVolumeInputs[id].mType;
    }

    int getGain(int id) {
        if (!isValidId(id)) return 0;
        return mVolumeInputs[id].mGainSetting;
    }

    boolean isMuted(int id) {
        if (!isValidId(id)) return true;
        return mVolumeInputs[id].mMute != MuteField.NOT_MUTED;
    }

    void setPropSettings(int id, int gainUnit, int gainMin, int gainMax) {
        if (!isValidId(id)) return;

        mVolumeInputs[id].mGainSettingsUnits = gainUnit;
        mVolumeInputs[id].mGainSettingsMinSetting = gainMin;
        mVolumeInputs[id].mGainSettingsMaxSetting = gainMax;
    }

    void setState(int id, int gainSetting, int mute, int gainMode) {
        if (!isValidId(id)) return;

        Descriptor desc = mVolumeInputs[id];

        if (gainSetting > desc.mGainSettingsMaxSetting
                || gainSetting < desc.mGainSettingsMinSetting) {
            Log.e(TAG, "Request fail. Illegal gainSetting argument: " + gainSetting);
            return;
        }

        boolean broadcast = false;
        if (desc.mGainSetting != gainSetting) {
            desc.mGainSetting = gainSetting;
            broadcast = true;
        }
        if (desc.mGainMode != gainMode) {
            desc.mGainMode = gainMode;
            broadcast = true;
        }
        if (desc.mMute != mute) {
            desc.mMute = mute;
            broadcast = true;
        }
        if (broadcast) {
            mVolumeInputs[id].broadcast(
                    "onAudioInputStateChanged",
                    (c) ->
                            c.onAudioInputStateChanged(
                                    new AudioInputControl.Descriptor.AudioInputState(
                                            gainSetting, mute, gainMode)));
        }
    }

    void dump(StringBuilder sb) {
        for (int i = 0; i < mVolumeInputs.length; i++) {
            Descriptor desc = mVolumeInputs[i];
            ProfileService.println(sb, "      id: " + i);
            ProfileService.println(sb, "        description: " + desc.mDescription);
            ProfileService.println(sb, "        type: " + desc.mType);
            ProfileService.println(sb, "        status: " + desc.mStatus);
            ProfileService.println(sb, "        gainSetting: " + desc.mGainSetting);
            ProfileService.println(sb, "        gainMode: " + desc.mGainMode);
            ProfileService.println(sb, "        mute: " + desc.mMute);
            ProfileService.println(sb, "        units:" + desc.mGainSettingsUnits);
            ProfileService.println(sb, "        minGain:" + desc.mGainSettingsMinSetting);
            ProfileService.println(sb, "        maxGain:" + desc.mGainSettingsMaxSetting);
        }
    }
}
