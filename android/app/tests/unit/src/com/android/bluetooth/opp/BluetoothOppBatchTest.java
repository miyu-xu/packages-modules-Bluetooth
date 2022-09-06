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
import android.content.Context;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
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
public class BluetoothOppBatchTest {
    private BluetoothOppBatch mBluetoothOppBatch;
    private Context mContext;

    @Mock
    private BluetoothOppShareInfo mMockBluetoothOppShareInfo;
    @Mock
    private BluetoothOppShareInfo mMockBluetoothOppShareInfo2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mMockBluetoothOppShareInfo.mDestination = "00:11:22:33:44:55";
        mMockBluetoothOppShareInfo.mStatus = BluetoothShare.STATUS_PENDING;
        mMockBluetoothOppShareInfo.mId = 0;

        mContext = InstrumentationRegistry.getInstrumentation().getContext();

        mBluetoothOppBatch = new BluetoothOppBatch(mContext, mMockBluetoothOppShareInfo);
    }

    @Test
    public void testConstructor() {
        assertThat(mBluetoothOppBatch.mTimestamp).isEqualTo(mMockBluetoothOppShareInfo.mTimestamp);
        assertThat(mBluetoothOppBatch.mDirection).isEqualTo(mMockBluetoothOppShareInfo.mDirection);
        assertThat(mBluetoothOppBatch.mStatus).isEqualTo(Constants.BATCH_STATUS_PENDING);
        assertThat(mBluetoothOppBatch.mDestination.getAddress()).isEqualTo(mMockBluetoothOppShareInfo.mDestination);
        assertThat(mBluetoothOppBatch.hasShare(mMockBluetoothOppShareInfo)).isTrue();
    }

    @Test
    public void testAddShare() {
        mBluetoothOppBatch.registerListern(new BluetoothOppBatchListenerTest());
        assertThat(mBluetoothOppBatch.isEmpty()).isFalse();
        assertThat(mBluetoothOppBatch.getNumShares()).isEqualTo(1);
        assertThat(mBluetoothOppBatch.hasShare(mMockBluetoothOppShareInfo)).isTrue();
        assertThat(mBluetoothOppBatch.hasShare(mMockBluetoothOppShareInfo2)).isFalse();
        mBluetoothOppBatch.addShare(mMockBluetoothOppShareInfo2);
        assertThat(mBluetoothOppBatch.getNumShares()).isEqualTo(2);
        assertThat(mBluetoothOppBatch.hasShare(mMockBluetoothOppShareInfo)).isTrue();
        assertThat(mBluetoothOppBatch.hasShare(mMockBluetoothOppShareInfo2)).isTrue();
    }

    @Test
    public void testCancelBatch() {
        mBluetoothOppBatch.registerListern(new BluetoothOppBatchListenerTest());
        assertThat(mBluetoothOppBatch.getPendingShare()).isEqualTo(mMockBluetoothOppShareInfo);

        // Mock Id doesn't work
        mBluetoothOppBatch.cancelBatch();
        assertThat(mBluetoothOppBatch.isEmpty()).isTrue();
    }

    private class BluetoothOppBatchListenerTest implements
            BluetoothOppBatch.BluetoothOppBatchListener {
        @Override
        public void onShareAdded(int id) {
            assertThat(id).isEqualTo(mMockBluetoothOppShareInfo2.mId);
        }

        @Override
        public void onShareDeleted(int id) {
        }

        @Override
        public void onBatchCanceled() {
        }
    }
}
