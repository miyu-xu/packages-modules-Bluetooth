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

package com.android.bluetooth.btservice.storage;

import android.bluetooth.BluetoothAudioPolicy;

import androidx.room.Entity;

@Entity
class AudioPolicyEntity {
    public int call_pick_up_audio_policy;
    public int connecting_audio_policy;
    public int in_band_ring_audio_policy;

    AudioPolicyEntity() {
        call_pick_up_audio_policy = BluetoothAudioPolicy.CALL_AUDIO_DEFAULT;
        connecting_audio_policy = BluetoothAudioPolicy.CALL_AUDIO_DEFAULT;
        in_band_ring_audio_policy = BluetoothAudioPolicy.CALL_AUDIO_DEFAULT;
    }

    AudioPolicyEntity(int call_pick_up_audio_policy, int connecting_audio_policy,
            int in_band_ring_audio_policy) {
        this.call_pick_up_audio_policy = call_pick_up_audio_policy;
        this.connecting_audio_policy = connecting_audio_policy;
        this.in_band_ring_audio_policy = in_band_ring_audio_policy;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("call_pick_up_audio_policy=")
                .append(metadataToString(call_pick_up_audio_policy))
                .append("|connecting_audio_policy=")
                .append(metadataToString(connecting_audio_policy))
                .append("|in_band_ring_audio_policy=")
                .append(metadataToString(in_band_ring_audio_policy));

        return builder.toString();
    }

    private String metadataToString(int metadata) {
        return String.valueOf(metadata);
    }
}
