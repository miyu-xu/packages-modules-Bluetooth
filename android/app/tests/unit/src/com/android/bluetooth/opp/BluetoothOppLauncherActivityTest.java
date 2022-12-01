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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothDevicePicker;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.MonitoringInstrumentation;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class BluetoothOppLauncherActivityTest extends
        ActivityInstrumentationTestCase2<BluetoothOppLauncherActivity> {
    Context mTargetContext;
    Intent mIntent;

    BluetoothMethodProxy mMethodProxy;

    public BluetoothOppLauncherActivityTest() {
        super(BluetoothOppLauncherActivity.class);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mTargetContext = spy(new ContextWrapper(
                ApplicationProvider.getApplicationContext()));
        mMethodProxy = spy(BluetoothMethodProxy.getInstance());
        BluetoothMethodProxy.setInstanceForTesting(mMethodProxy);
        MonitoringInstrumentation monitoringInstrumentation =
                (MonitoringInstrumentation) InstrumentationRegistry.getInstrumentation();
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(
                BluetoothOppTestUtils.mActivityStageObserver.mActivityLifecycleCallback);
        injectInstrumentation(monitoringInstrumentation);

        mIntent = new Intent();
        mIntent.setClass(mTargetContext, BluetoothOppLauncherActivity.class);

        BluetoothOppTestUtils.enableOppActivities(true, mTargetContext);
        Intents.init();
    }

    @After
    public void tearDown() {
        BluetoothOppTestUtils.enableOppActivities(false, mTargetContext);
        BluetoothMethodProxy.setInstanceForTesting(null);
        Intents.release();
    }

    @Test
    public void onCreate_withNoAction_returnImmediately() throws Exception {
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(BluetoothOppTestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED,
                100)).isTrue();
    }

    @Test
    public void onCreate_withActionSend_withoutMetadata_finishImmediately() throws Exception {
        mIntent.setAction(Intent.ACTION_SEND);
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(BluetoothOppTestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED,
                100)).isTrue();
    }

    @Test
    public void onCreate_withActionSendMultiple_withoutMetadata_finishImmediately() {
        mIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(BluetoothOppTestUtils.mActivityStageObserver.waitForStage(Stage.DESTROYED,
                100)).isTrue();
    }

    @Test
    public void onCreate_withActionOpen_sendBroadcast() throws Exception {
        mIntent.setAction(Constants.ACTION_OPEN);
        mIntent.setData(Uri.EMPTY);
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);

        verify(mMethodProxy).contextSendBroadcast(any(), argument.capture());

        assertThat(argument.getValue().getAction()).isEqualTo(Constants.ACTION_OPEN);
        assertThat(argument.getValue().getComponent().getClassName())
                .isEqualTo(BluetoothOppReceiver.class.getName());
        assertThat(argument.getValue().getData()).isEqualTo(Uri.EMPTY);
    }

    @Test
    public void launchDevicePicker_bluetoothNotEnabled_launchEnableActivity() throws Exception {
        doReturn(false).when(mMethodProxy).bluetoothAdapterIsEnabled(any());
        // Unsupported action, the activity will stay without being finished right the way
        mIntent.setAction("unsupported-action");
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        activity.launchDevicePicker();

        onView(withText(mTargetContext.getText(R.string.bt_enable_cancel).toString())).inRoot(
                isDialog()).check(matches(isDisplayed())).perform(click());
        intended(hasComponent(BluetoothOppBtEnableActivity.class.getName()));
    }

    @Test
    public void launchDevicePicker_bluetoothEnabled_launchActivity() throws Exception {
        doReturn(true).when(mMethodProxy).bluetoothAdapterIsEnabled(any());
        // Unsupported action, the activity will stay without being finished right the way
        mIntent.setAction("unsupported-action");
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        activity.launchDevicePicker();

        intended(hasAction(BluetoothDevicePicker.ACTION_LAUNCH));
    }

    @Test
    public void createFileForSharedContent_returnFile() throws Exception {
        doReturn(true).when(mMethodProxy).bluetoothAdapterIsEnabled(any());
        // Unsupported action, the activity will stay without being finished right the way
        mIntent.setAction("unsupported-action");
        ActivityScenario<BluetoothOppLauncherActivity> scenario = ActivityScenario.launch(mIntent);


        final String shareContent =
                "a string to trigger pattern match with url: www.google.com, phone number: "
                        + "+821023456798, and email: abc@test.com";
        setActivityIntent(mIntent);
        BluetoothOppLauncherActivity activity = getActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        final Uri fileUri = activity.createFileForSharedContent(activity, shareContent);

        assertThat(fileUri.toString().endsWith(".html")).isTrue();

        File file = new File(fileUri.getPath());
        // new file is in html format that include the shared content, so length should increase
        assertThat(file.length()).isGreaterThan(shareContent.length());
    }
}
