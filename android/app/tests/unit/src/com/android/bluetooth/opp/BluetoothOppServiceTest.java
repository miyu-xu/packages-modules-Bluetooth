/*
 * Copyright 2018 The Android Open Source Project
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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;

import android.bluetooth.BluetoothAdapter;
import android.net.Uri;

import androidx.test.filters.MediumTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BluetoothOppServiceTest {
    @Rule public final ServiceTestRule mServiceRule = new ServiceTestRule();

    private BluetoothOppService mService = null;
    private BluetoothAdapter mAdapter = null;

    @Mock private AdapterService mAdapterService;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("Ignore test when BluetoothOppService is not enabled",
                BluetoothOppService.isEnabled());
        MockitoAnnotations.initMocks(this);
        TestUtils.setAdapterService(mAdapterService);
        doReturn(true, false).when(mAdapterService).isStartedProfile(anyString());
        TestUtils.startService(mServiceRule, BluetoothOppService.class);
        mService = BluetoothOppService.getBluetoothOppService();
        Assert.assertNotNull(mService);
        // Try getting the Bluetooth adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);
    }

    @After
    public void tearDown() throws Exception {
        if (!BluetoothOppService.isEnabled()) {
            return;
        }
        TestUtils.stopService(mServiceRule, BluetoothOppService.class);
        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void testInitialize() {
        Assert.assertNotNull(BluetoothOppService.getBluetoothOppService());
    }

    @Test
    public void deleteShare_deleteShareAndCorrespondingBatch() {
        Uri infoUri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
        String mimetype = "image/jpeg";
        int infoDir = BluetoothShare.DIRECTION_OUTBOUND;
        String infoDes = "01:23:45:67:89:AB";
        int infoVisibility = BluetoothShare.VISIBILITY_VISIBLE;
        int infoConfirm = BluetoothShare.USER_CONFIRMATION_PENDING;
        int infoStatus = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int infoTimestamp = 123456789;
        int infoTimestamp2 = 123489;
        boolean infoScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, infoUri, hintString, infoFileName,
                mimetype, infoDir, infoDes, infoVisibility, infoConfirm, infoStatus, totalBytes,
                currentBytes, infoTimestamp, infoScanned);
        BluetoothOppShareInfo info2 = new BluetoothOppShareInfo(0, infoUri, hintString,
                infoFileName,
                mimetype, infoDir, infoDes, infoVisibility, infoConfirm, infoStatus, totalBytes,
                currentBytes, infoTimestamp2, infoScanned);

        mService.mShares.clear();
        mService.mShares.add(info);
        mService.mShares.add(info2);

        // batch1 will be removed
        BluetoothOppBatch batch1 = new BluetoothOppBatch(mService, info);
        BluetoothOppBatch batch2 = new BluetoothOppBatch(mService, info2);
        batch2.mStatus = Constants.BATCH_STATUS_FINISHED;
        mService.mBatches.clear();
        mService.mBatches.add(batch1);
        mService.mBatches.add(batch2);

        mService.deleteShare(0);
        assertThat(mService.mShares.size()).isEqualTo(1);
        assertThat(mService.mBatches.size()).isEqualTo(1);
    }

    @Test
    public void dump_shouldNotThrow() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
        String mimetype = "image/jpeg";
        int direction = BluetoothShare.DIRECTION_INBOUND;
        String destination = "01:23:45:67:89:AB";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        int status = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int timestamp = 123456789;
        boolean mediaScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        mService.mShares.add(info);

        // should not throw
        mService.dump(new StringBuilder());
    }
}
