/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.bluetooth.gatt;

import android.bluetooth.le.AdvertiseData;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for {@link AdvertiseHelper}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AdvertiseHelperTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAdvertiseDataToBytes() throws Exception {
        byte[] emptyBytes = AdvertiseHelper.advertiseDataToBytes(null, "");

        Assert.assertEquals(0, emptyBytes.length);

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        String deviceName = "MockDeviceName";

        byte[] advDataBytes = AdvertiseHelper.advertiseDataToBytes(advertiseData, deviceName);

        Assert.assertEquals(16, advDataBytes.length);
    }

    @Test
    public void testCheck_length() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> AdvertiseHelper.check_length(0X00, 256)
        );
    }
}