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
import android.bluetooth.AudioInputControl.GainMode;
import android.bluetooth.AudioInputControl.Mute;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IAudioInputCallback;
import android.os.RemoteCallbackList;
import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;

import bluetooth.constants.AudioInputType;
import bluetooth.constants.aics.AudioInputStatus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class VolumeControlInputDescriptor {
    private static final String TAG = VolumeControlInputDescriptor.class.getSimpleName();

    final Descriptor[] mVolumeInputs;

    VolumeControlInputDescriptor(int numberOfExternalInputs) {
        mVolumeInputs = new Descriptor[numberOfExternalInputs];
        // Stack delivers us number of audio inputs. ids are countinous from [0;n[
        for (int i = 0; i < numberOfExternalInputs; i++) {
            mVolumeInputs[i] = new Descriptor(i);
        }
    }

    private static class Descriptor {
        @AudioInputControl.Status int mStatus = AudioInputStatus.INACTIVE;

        @AudioInputControl.Type int mType = AudioInputType.UNSPECIFIED;

        int mGainSetting = 0;

        @GainMode int mGainMode = bluetooth.constants.aics.GainMode.MANUAL_ONLY;

        @Mute int mMute = bluetooth.constants.aics.Mute.DISABLED;

        /* See AICS 1.0
         * The Gain_Setting (mGainSetting) field is a signed value for which a single increment or
         * decrement should result in a corresponding increase or decrease of the input amplitude by
         * the value of the Gain_Setting_Units (mGainSettingsUnits) field of the Gain Setting
         * Properties characteristic value.
         */
        int mGainSettingsUnits = 0;

        int mGainSettingsMax = 0;
        int mGainSettingsMin = 0;

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
                                        i.mIndex
                                        // i.mDescription,
                                        // i.mType,
                                        // i.mStatus,
                                        // new AudioInputControl.Descriptor.AudioInputState(
                                        //         i.mGainSetting, i.mMute, i.mGainMode),
                                        // new AudioInputControl.Descriptor.GainSettingProperties(
                                        //         i.mGainSettingsUnits,
                                        //         i.mGainSettingsMin,
                                        //         i.mGainSettingsMax)
                                        ))
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
        if (!isValidId(id)) return;
        mVolumeInputs[id].registerCallback(callback);
    }

    void unregisterCallback(int id, IAudioInputCallback callback) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].unregisterCallback(callback);
    }

    void setStatus(int id, int status) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mStatus = status;
        mVolumeInputs[id].broadcast("onGainStatus", (c) -> c.onStatusChanged(status));
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

    int getMute(int id) {
        if (!isValidId(id)) return bluetooth.constants.aics.Mute.DISABLED;
        return mVolumeInputs[id].mMute;
    }

    void setPropSettings(int id, int gainUnit, int gainMin, int gainMax) {
        if (!isValidId(id)) return;

        mVolumeInputs[id].mGainSettingsUnits = gainUnit;
        mVolumeInputs[id].mGainSettingsMin = gainMin;
        mVolumeInputs[id].mGainSettingsMax = gainMax;
    }

    int getGainSettingUnit(int id) {
        if (!isValidId(id)) return 0;
        return mVolumeInputs[id].mGainSettingsUnits;
    }
    int getGainSettingMin(int id) {
        if (!isValidId(id)) return 0;
        return mVolumeInputs[id].mGainSettingsMin;
    }
    int getGainSettingMax(int id) {
        if (!isValidId(id)) return 0;
        return mVolumeInputs[id].mGainSettingsMax;
    }

    void setState(int id, int gainSetting, int mute, int gainMode) {
        if (!isValidId(id)) return;

        Descriptor desc = mVolumeInputs[id];

        if (gainSetting > desc.mGainSettingsMax
                || gainSetting < desc.mGainSettingsMin) {
            Log.e(TAG, "Request fail. Illegal gainSetting argument: " + gainSetting);
            return;
        }

        desc.mGainSetting = gainSetting;
        desc.mGainMode = gainMode;
        desc.mMute = mute;

        mVolumeInputs[id].broadcast(
                "onAudioInputStateChanged",
                (c) ->
                        c.onAudioInputStateChanged(
                                new AudioInputControl.AudioInputState(
                                        gainSetting, mute, gainMode)));
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
            ProfileService.println(sb, "        minGain:" + desc.mGainSettingsMin);
            ProfileService.println(sb, "        maxGain:" + desc.mGainSettingsMax);
        }
    }
}
