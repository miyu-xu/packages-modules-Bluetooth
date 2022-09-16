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

package com.android.bluetooth.opp;


import static com.android.bluetooth.opp.BluetoothOppTransferActivity.DIALOG_SEND_COMPLETE_FAIL;
import static com.android.bluetooth.opp.BluetoothOppTransferActivity.DIALOG_SEND_ONGOING;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BluetoothOppTransferActivityTest {
    @Mock
    ContentResolver mContentResolver;
    @Mock
    Cursor mCursor;
    @Mock
    BluetoothMethodProxy mBluetoothMethodProxy = BluetoothMethodProxy.getInstance();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        BluetoothMethodProxy.setInstanceForTesting(mBluetoothMethodProxy);
    }

    @After
    public void tearDown() {
        BluetoothMethodProxy.setInstanceForTesting(null);
    }

    @Test
    public void onCreate_showCorrectDialog() {
        Uri dataUrl = Uri.parse("content://com.android.bluetooth.opp.test/random");

        Intent intent = new Intent();
        intent.setClass(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                BluetoothOppTransferActivity.class);
        intent.setData(dataUrl);

        doReturn(mCursor).when(mBluetoothMethodProxy).contentResolverQuery(any(), eq(dataUrl), eq(null), eq(null),
                eq(null), eq(null));
        doReturn(1).when(mCursor).getColumnIndexOrThrow(any());
        doReturn(1).when(mCursor).getInt(anyInt());
        doReturn("AA:BB:CC:DD:11:22").when(mCursor).getString(anyInt());

        int idValue = 1234;
        int directionValue = BluetoothShare.DIRECTION_OUTBOUND;
        long totalBytesValue = 10;
        long currentBytesValue = 1;
        int statusValue = BluetoothShare.STATUS_PENDING;
        Long timestampValue = 123456789L;
        String destinationValue = "AA:BB:CC:00:11:22";
        String fileTypeValue = "text/plain";

        Map<String, BluetoothOppTestUtils.BluetoothShareMockData> nameToDataMap = Map.of(
                BluetoothShare._ID, new BluetoothOppTestUtils.BluetoothShareMockData(0, idValue),
                BluetoothShare.STATUS, new BluetoothOppTestUtils.BluetoothShareMockData(1, statusValue),
                BluetoothShare.DIRECTION, new BluetoothOppTestUtils.BluetoothShareMockData(2, directionValue),
                BluetoothShare.TOTAL_BYTES, new BluetoothOppTestUtils.BluetoothShareMockData(3, totalBytesValue),
                BluetoothShare.CURRENT_BYTES, new BluetoothOppTestUtils.BluetoothShareMockData(4, currentBytesValue),
                BluetoothShare.TIMESTAMP, new BluetoothOppTestUtils.BluetoothShareMockData(5, timestampValue),
                BluetoothShare.DESTINATION, new BluetoothOppTestUtils.BluetoothShareMockData(6, destinationValue),
                BluetoothShare._DATA, new BluetoothOppTestUtils.BluetoothShareMockData(7, null),
                BluetoothShare.FILENAME_HINT, new BluetoothOppTestUtils.BluetoothShareMockData(8, null),
                BluetoothShare.MIMETYPE, new BluetoothOppTestUtils.BluetoothShareMockData(9, fileTypeValue)
        );

        BluetoothOppTestUtils.setUpMockCursor(mCursor, nameToDataMap);

        ActivityScenario<BluetoothOppTransferActivity> activityScenario = ActivityScenario.launch(intent);

        activityScenario.onActivity(activity -> {
            assertThat(activity.mWhichDialog).isEqualTo(DIALOG_SEND_ONGOING);
        });

    }
}
