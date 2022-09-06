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

    private BluetoothOppShareInfo mMockBluetoothOppShareInfo;
    private BluetoothOppShareInfo mMockBluetoothOppShareInfo2;

    @Before
    public void setUp() throws Exception {
        mMockBluetoothOppShareInfo = new BluetoothOppShareInfo(0, null, null, null, null, 0,
            "00:11:22:33:44:55", 0, 0, BluetoothShare.STATUS_PENDING, 0, 0, 0, false);
        mMockBluetoothOppShareInfo2 = new BluetoothOppShareInfo(1, null, null, null, null, 0,
            "AA:BB:22:CD:E0:55", 0, 0, BluetoothShare.STATUS_PENDING, 0, 0, 0, false);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mBluetoothOppBatch = new BluetoothOppBatch(mContext, mMockBluetoothOppShareInfo);
    }

    @Test
    public void testConstructor() {
        assertThat(mBluetoothOppBatch.mTimestamp).isEqualTo(mMockBluetoothOppShareInfo.mTimestamp);
        assertThat(mBluetoothOppBatch.mDirection).isEqualTo(mMockBluetoothOppShareInfo.mDirection);
        assertThat(mBluetoothOppBatch.mStatus).isEqualTo(Constants.BATCH_STATUS_PENDING);
        assertThat(mBluetoothOppBatch.mDestination.getAddress())
            .isEqualTo(mMockBluetoothOppShareInfo.mDestination);
        assertThat(mBluetoothOppBatch.hasShare(mMockBluetoothOppShareInfo)).isTrue();
    }

    @Test
    public void testAddShare() {
        class BluetoothOppBatchListenerTest implements
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
        // Array can be access and edit by the inner class
        final boolean[] batchCancelCalled = {false};
        class BluetoothOppBatchListenerTest implements
            BluetoothOppBatch.BluetoothOppBatchListener {
            @Override
            public void onShareAdded(int id) {
            }

            @Override
            public void onShareDeleted(int id) {
            }

            @Override
            public void onBatchCanceled() {
                batchCancelCalled[0] = true;
            }
        }
        mBluetoothOppBatch.registerListern(new BluetoothOppBatchListenerTest());
        assertThat(mBluetoothOppBatch.getPendingShare()).isEqualTo(mMockBluetoothOppShareInfo);
        try {
            mBluetoothOppBatch.cancelBatch();
        } catch (IllegalArgumentException e) {
            // the id for BluetoothOppShareInfo id is made up, so the link is invalid,
            // leading to IllegalArgumentException
            assertThat(e).hasMessageThat().isEqualTo(
                "Unknown URI content://com.android.bluetooth.opp/btopp/0");
        }

        assertThat(mBluetoothOppBatch.isEmpty()).isTrue();
        assertThat(batchCancelCalled[0]).isTrue();
    }
}
