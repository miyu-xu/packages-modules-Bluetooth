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

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity
class AudioPolicyEntity {
    @ColumnInfo(name = "call_pick_up_audio_policy")
    public int callPickUpAudioPolicy;
    @ColumnInfo(name = "connecting_audio_policy")
    public int connectingAudioPolicy;
    @ColumnInfo(name = "in_band_ring_audio_policy")
    public int inBandRingAudioPolicy;

    AudioPolicyEntity() {
        callPickUpAudioPolicy = BluetoothAudioPolicy.POLICY_DEFAULT;
        connectingAudioPolicy = BluetoothAudioPolicy.POLICY_DEFAULT;
        inBandRingAudioPolicy = BluetoothAudioPolicy.POLICY_DEFAULT;
    }

    AudioPolicyEntity(int callPickUpAudioPolicy, int connectingAudioPolicy,
            int inBandRingAudioPolicy) {
        this.callPickUpAudioPolicy = callPickUpAudioPolicy;
        this.connectingAudioPolicy = connectingAudioPolicy;
        this.inBandRingAudioPolicy = inBandRingAudioPolicy;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("callPickUpAudioPolicy=")
                .append(metadataToString(callPickUpAudioPolicy))
                .append("|connectingAudioPolicy=")
                .append(metadataToString(connectingAudioPolicy))
                .append("|inBandRingAudioPolicy=")
                .append(metadataToString(inBandRingAudioPolicy));

        return builder.toString();
    }

    private String metadataToString(int metadata) {
        return String.valueOf(metadata);
    }
}
