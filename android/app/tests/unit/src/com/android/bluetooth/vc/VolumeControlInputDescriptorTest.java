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

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothVolumeControl;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class VolumeControlInputDescriptorTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() throws Exception {
        // placeholder
    }

    @After
    public void tearDown() throws Exception {
        // placeholder
    }

    @Test
    @EnableFlags(Flags.FLAG_LEAUDIO_ADD_AICS_SUPPORT)
    public void testVolumeControlInputDescriptor() throws Exception {

        VolumeControlInputDescriptor descriptor = new VolumeControlInputDescriptor();

        int validId = 10;
        int invalidId = 1;
        int testGainValue = 100;
        int testGainMode = 1;
        boolean testGainMute = true;
        String defaultDesc = "Unknown";
        String testDesc = "testDescription";
        int testType = BluetoothVolumeControl.AUDIO_INPUT_TYPE_AMBIENT;
        int testGainSettingsMax = 100;
        int testGainSettingsMin = 0;
        int testGainSettingsUnit = 1;

        Assert.assertEquals(0, descriptor.size());
        descriptor.add(validId);
        Assert.assertEquals(1, descriptor.size());
        // Check if adding same id will not increase descriptor count.
        descriptor.add(validId);
        Assert.assertEquals(1, descriptor.size());

        // Test operations on invalid ID
        Assert.assertFalse(descriptor.isActive(invalidId));
        Assert.assertFalse(descriptor.setActive(invalidId, true));
        Assert.assertFalse(descriptor.setDescription(invalidId, testDesc));
        Assert.assertEquals(null, descriptor.getDescription(invalidId));
        Assert.assertFalse(descriptor.setType(invalidId, testType));
        Assert.assertEquals(
                BluetoothVolumeControl.AUDIO_INPUT_TYPE_UNSPECIFIED, descriptor.getType(invalidId));
        Assert.assertEquals(0, descriptor.getGain(invalidId));
        Assert.assertFalse(descriptor.isMuted(invalidId));
        Assert.assertFalse(
                descriptor.setPropSettings(
                        invalidId, testGainSettingsUnit, testGainSettingsMin, testGainSettingsMax));
        Assert.assertFalse(
                descriptor.setState(invalidId, testGainValue, testGainMode, testGainMute));

        // Test valid id
        Assert.assertFalse(descriptor.isActive(validId));
        Assert.assertTrue(descriptor.setActive(validId, true));
        Assert.assertTrue(descriptor.isActive(validId));

        Assert.assertEquals(defaultDesc, descriptor.getDescription(validId));
        Assert.assertTrue(descriptor.setDescription(validId, testDesc));
        Assert.assertEquals(testDesc, descriptor.getDescription(validId));

        Assert.assertEquals(
                BluetoothVolumeControl.AUDIO_INPUT_TYPE_UNSPECIFIED, descriptor.getType(validId));
        Assert.assertTrue(descriptor.setType(validId, testType));
        Assert.assertEquals(testType, descriptor.getType(validId));

        Assert.assertTrue(
                descriptor.setPropSettings(
                        validId, testGainSettingsUnit, testGainSettingsMin, testGainSettingsMax));
        Assert.assertTrue(descriptor.setState(validId, testGainValue, testGainMode, testGainMute));
        Assert.assertEquals(testGainValue, descriptor.getGain(validId));

        descriptor.remove(invalidId);
        Assert.assertEquals(1, descriptor.size());

        descriptor.remove(validId);
        Assert.assertEquals(0, descriptor.size());

        descriptor.add(validId);
        Assert.assertEquals(1, descriptor.size());
        descriptor.clear();
        Assert.assertEquals(0, descriptor.size());
    }
}
