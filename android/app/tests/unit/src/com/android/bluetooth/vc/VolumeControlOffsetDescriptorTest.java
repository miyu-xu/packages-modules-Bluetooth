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

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class VolumeControlOffsetDescriptorTest {

    @Before
    public void setUp() throws Exception {
        // placeholder
    }

    @After
    public void tearDown() throws Exception {
        // placeholder
    }

    @Test
    public void testVolumeControlOffsetDescriptor() {
        VolumeControlOffsetDescriptor descriptor = new VolumeControlOffsetDescriptor();
        int invalidId = -1;
        int validId = 10;
        int testValue = 100;
        String testDesc = "testDescription";
        int testLocation = 10000;

        Assert.assertEquals(0, descriptor.size());
        descriptor.add(validId);
        Assert.assertEquals(1, descriptor.size());

        Assert.assertFalse(descriptor.setValue(invalidId, testValue));
        Assert.assertTrue(descriptor.setValue(validId, testValue));
        Assert.assertEquals(0, descriptor.getValue(invalidId));
        Assert.assertEquals(testValue, descriptor.getValue(validId));

        Assert.assertFalse(descriptor.setDescription(invalidId, testDesc));
        Assert.assertTrue(descriptor.setDescription(validId, testDesc));
        Assert.assertEquals(null, descriptor.getDescription(invalidId));
        Assert.assertEquals(testDesc, descriptor.getDescription(validId));

        Assert.assertFalse(descriptor.setLocation(invalidId, testLocation));
        Assert.assertTrue(descriptor.setLocation(validId, testLocation));
        Assert.assertEquals(0, descriptor.getLocation(invalidId));
        Assert.assertEquals(testLocation, descriptor.getLocation(validId));

        StringBuilder sb = new StringBuilder();
        descriptor.dump(sb);
        Assert.assertTrue(sb.toString().contains(testDesc));

        descriptor.add(validId + 1);
        Assert.assertEquals(2, descriptor.size());
        descriptor.remove(validId);
        Assert.assertEquals(1, descriptor.size());
        descriptor.clear();
        Assert.assertEquals(0, descriptor.size());
    }
}
