/*
 * Copyright 2023 The Android Open Source Project
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

import android.bluetooth.BluetoothSinkAudioPolicy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class AudioPolicyEntityTest {
    @Test
    public void constructor() {
        AudioPolicyEntity entity = new AudioPolicyEntity();
        Assert.assertEquals(
                BluetoothSinkAudioPolicy.POLICY_UNCONFIGURED, entity.callEstablishAudioPolicy);
        Assert.assertEquals(
                BluetoothSinkAudioPolicy.POLICY_UNCONFIGURED, entity.connectingTimeAudioPolicy);
        Assert.assertEquals(
                BluetoothSinkAudioPolicy.POLICY_UNCONFIGURED, entity.inBandRingtoneAudioPolicy);
    }

    @Test
    public void toString_containsExpectedStrings() {
        AudioPolicyEntity entity = new AudioPolicyEntity();
        String entityStr = entity.toString();
        Assert.assertTrue(entityStr.contains("callEstablishAudioPolicy="));
        Assert.assertTrue(entityStr.contains("connectingTimeAudioPolicy="));
        Assert.assertTrue(entityStr.contains("inBandRingtoneAudioPolicy="));
    }
}
