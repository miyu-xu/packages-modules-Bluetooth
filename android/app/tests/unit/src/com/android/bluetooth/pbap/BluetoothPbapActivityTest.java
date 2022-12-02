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

package com.android.bluetooth.pbap;

import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.text.Editable;
import android.text.SpannableStringBuilder;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.Stage;

import com.android.bluetooth.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BluetoothPbapActivityTest extends
        ActivityInstrumentationTestCase2<BluetoothPbapActivity> {
    Context mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Intent mIntent;

    public BluetoothPbapActivityTest() {
        super(BluetoothPbapActivity.class);
    }

    @Before
    public void setUp() {
        mIntent = new Intent();
        mIntent.setClass(mTargetContext, BluetoothPbapActivity.class);
        mIntent.setAction(BluetoothPbapService.AUTH_CHALL_ACTION);
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        enableActivity(true);
    }

    @After
    public void tearDown() throws Exception {
        enableActivity(false);
    }

    @Test
    public void activityIsDestroyed_whenLaunchedWithoutIntentAction() {
        mIntent.setAction(null);
        setActivityIntent(mIntent);
        BluetoothPbapActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(TestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED, 0)).isTrue();
    }

    @Test
    public void onPreferenceChange_returnsTrue() {
        setActivityIntent(mIntent);
        BluetoothPbapActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(activity.onPreferenceChange(null, null)).isTrue();
    }

    @Test
    public void onPositive_finishesActivity() {
        setActivityIntent(mIntent);
        BluetoothPbapActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::onPositive);

        assertThat(TestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED, 3_000)).isTrue();
    }

    @Test
    public void onNegative_finishesActivity() {
        setActivityIntent(mIntent);
        BluetoothPbapActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::onNegative);

        assertThat(TestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED, 3_000)).isTrue();
    }

    @Test
    public void onReceiveTimeoutIntent_finishesActivity() throws Exception {
        Intent intent = new Intent(BluetoothPbapService.USER_CONFIRM_TIMEOUT_ACTION);
        setActivityIntent(mIntent);
        BluetoothPbapActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.mReceiver.onReceive(activity, intent);
        });

        assertThat(TestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED, 3_000)).isTrue();
    }

    @Test
    public void afterTextChanged() throws Exception {
        Editable editable = new SpannableStringBuilder("An editable text");

        setActivityIntent(mIntent);
        BluetoothPbapActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.afterTextChanged(editable);
        });

        assertThat(activity.getButton(BUTTON_POSITIVE).isEnabled()).isTrue();
    }

    // TODO: Test onSaveInstanceState and onRestoreInstanceState.
    // Note: Activity.recreate() fails. The Activity just finishes itself when recreated.
    //       Fix the bug and test those methods.

    @Test
    public void emptyMethods_doesNotThrowException() throws Exception {
        try {
            setActivityIntent(mIntent);
            BluetoothPbapActivity activity = getActivity();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                activity.beforeTextChanged(null, 0, 0, 0);
                activity.onTextChanged(null, 0, 0, 0);
            });
        } catch (Exception ex) {
            assertWithMessage("Exception should not happen!").fail();
        }
    }

    private void enableActivity(boolean enable) {
        int enabledState = enable ? COMPONENT_ENABLED_STATE_ENABLED
                : COMPONENT_ENABLED_STATE_DEFAULT;

        mTargetContext.getPackageManager().setApplicationEnabledSetting(
                mTargetContext.getPackageName(), enabledState, DONT_KILL_APP);

        ComponentName activityName = new ComponentName(mTargetContext, BluetoothPbapActivity.class);
        mTargetContext.getPackageManager().setComponentEnabledSetting(
                activityName, enabledState, DONT_KILL_APP);
    }
}
