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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.BluetoothActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@RunWith(AndroidJUnit4.class)
public class BluetoothOppBtEnablingActivityTest extends
        ActivityInstrumentationTestCase2<BluetoothOppBtEnablingActivity> {
    @Spy
    BluetoothMethodProxy mBluetoothMethodProxy;

    Intent mIntent;
    Context mTargetContext;

    int mRealTimeoutValue;

    public BluetoothOppBtEnablingActivityTest() {
        super(BluetoothOppBtEnablingActivity.class);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mBluetoothMethodProxy = Mockito.spy(BluetoothMethodProxy.getInstance());
        BluetoothMethodProxy.setInstanceForTesting(mBluetoothMethodProxy);

        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        mIntent = new Intent();
        mIntent.setClass(mTargetContext, BluetoothOppBtEnablingActivity.class);

        mRealTimeoutValue = BluetoothOppBtEnablingActivity.sBtEnablingTimeoutMs;
        BluetoothOppTestUtils.enableOppActivities(true, mTargetContext);
    }

    @After
    public void tearDown() {
        BluetoothMethodProxy.setInstanceForTesting(null);
        BluetoothOppBtEnablingActivity.sBtEnablingTimeoutMs = mRealTimeoutValue;
        BluetoothOppTestUtils.enableOppActivities(false, mTargetContext);
    }

    @Test
    public void onCreate_bluetoothEnableTimeout_finishAfterTimeout() throws Exception {
        int spedUpTimeoutValue = 1000;
        // To speed up the test
        BluetoothOppBtEnablingActivity.sBtEnablingTimeoutMs = spedUpTimeoutValue;
        doReturn(false).when(mBluetoothMethodProxy).bluetoothAdapterIsEnabled(any());

        setActivityIntent(mIntent);
        BluetoothOppBtEnablingActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();


        final BluetoothOppManager mOppManager = BluetoothOppManager.getInstance(activity);

        assertThat(activity.waitForState(BluetoothActivity.STATE_DESTROYED,
                3_000 + spedUpTimeoutValue)).isTrue();
        assertThat(mOppManager.mSendingFlag).isEqualTo(false);
    }

    @Test
    public void onKeyDown_cancelProgress() throws Exception {
        doReturn(false).when(mBluetoothMethodProxy).bluetoothAdapterIsEnabled(any());

        setActivityIntent(mIntent);
        BluetoothOppBtEnablingActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        activity.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));

        BluetoothOppManager mOppManager = BluetoothOppManager.getInstance(activity);
        assertThat(mOppManager.mSendingFlag).isEqualTo(false);
        assertThat(activity.waitForState(BluetoothActivity.STATE_DESTROYED, 3_000)).isTrue();
    }

    @Test
    public void onCreate_bluetoothAlreadyEnabled_finishImmediately() throws Exception {
        doReturn(true).when(mBluetoothMethodProxy).bluetoothAdapterIsEnabled(any());
        ActivityScenario<BluetoothOppBtEnablingActivity> activityScenario = ActivityScenario.launch(
                mIntent);

        setActivityIntent(mIntent);
        BluetoothOppBtEnablingActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(activity.waitForState(BluetoothActivity.STATE_DESTROYED, 3_000)).isTrue();
    }
}
