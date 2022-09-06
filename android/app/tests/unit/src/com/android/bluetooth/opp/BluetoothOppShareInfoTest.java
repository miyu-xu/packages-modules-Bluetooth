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

import static com.google.common.truth.Truth.assertThat;
import android.net.Uri;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.opp.BluetoothOppBatch;
import com.android.bluetooth.opp.BluetoothOppShareInfo;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.bluetooth.opp.Constants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BluetoothOppShareInfoTest {
    private BluetoothOppShareInfo mMockBluetoothOppShareInfo;

    private Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
    private String hintString = "this is a object that take 4 bytes";
    private String filename = "random.jpg";
    private String mimetype = "image/jpeg";
    private int direction = BluetoothShare.DIRECTION_INBOUND;
    private String destination = "01:23:45:67:89:AB";
    private int visibility = BluetoothShare.VISIBILITY_VISIBLE;
    private int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
    private int status = BluetoothShare.STATUS_PENDING;
    private int totalBytes = 1023;
    private int currentBytes = 42;
    private int timestamp = 123456789;
    private boolean mediaScanned = false;
    @Before
    public void setUp() throws Exception {
        mMockBluetoothOppShareInfo = new BluetoothOppShareInfo(0, uri, hintString, filename,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);
    }

    @Test
    public void testConstructor() {
        assertThat(mMockBluetoothOppShareInfo.mUri).isEqualTo(uri);
        assertThat(mMockBluetoothOppShareInfo.mFilename).isEqualTo(filename);
        assertThat(mMockBluetoothOppShareInfo.mMimetype).isEqualTo(mimetype);
        assertThat(mMockBluetoothOppShareInfo.mDirection).isEqualTo(direction);
        assertThat(mMockBluetoothOppShareInfo.mDestination).isEqualTo(destination);
        assertThat(mMockBluetoothOppShareInfo.mVisibility).isEqualTo(visibility);
        assertThat(mMockBluetoothOppShareInfo.mConfirm).isEqualTo(confirm);
        assertThat(mMockBluetoothOppShareInfo.mStatus).isEqualTo(status);
        assertThat(mMockBluetoothOppShareInfo.mTotalBytes).isEqualTo(totalBytes);
        assertThat(mMockBluetoothOppShareInfo.mCurrentBytes).isEqualTo(currentBytes);
        assertThat(mMockBluetoothOppShareInfo.mTimestamp).isEqualTo(timestamp);
        assertThat(mMockBluetoothOppShareInfo.mMediaScanned).isEqualTo(mediaScanned);
    }

    @Test
    public void testReadyToStart() {
        assertThat(mMockBluetoothOppShareInfo.isReadyToStart()).isTrue();
        mMockBluetoothOppShareInfo.mDirection = BluetoothShare.DIRECTION_OUTBOUND;
        assertThat(mMockBluetoothOppShareInfo.isReadyToStart()).isTrue();
        mMockBluetoothOppShareInfo.mStatus = BluetoothShare.STATUS_RUNNING;
        assertThat(mMockBluetoothOppShareInfo.isReadyToStart()).isFalse();
    }

    @Test
    public void testHasCompletionNotification() {
        assertThat(mMockBluetoothOppShareInfo.hasCompletionNotification()).isFalse();
        mMockBluetoothOppShareInfo.mStatus = BluetoothShare.STATUS_CANCELED;
        assertThat(mMockBluetoothOppShareInfo.hasCompletionNotification()).isTrue();
        mMockBluetoothOppShareInfo.mVisibility = BluetoothShare.VISIBILITY_HIDDEN;
        assertThat(mMockBluetoothOppShareInfo.hasCompletionNotification()).isFalse();
    }

    @Test
    public void testIsObsolete() {
        assertThat(mMockBluetoothOppShareInfo.isObsolete()).isFalse();
        mMockBluetoothOppShareInfo.mStatus = BluetoothShare.STATUS_RUNNING;
        assertThat(mMockBluetoothOppShareInfo.isObsolete()).isTrue();
    }
}
