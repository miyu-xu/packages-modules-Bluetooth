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

package com.android.server.bluetooth;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BluetoothModeChangeHelperTest {

    @Mock
    BluetoothManagerService mService;

    Context mContext;
    BluetoothModeChangeHelper mHelper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getTargetContext()));

        mHelper = new BluetoothModeChangeHelper(mContext);
    }

    @Test
    public void isMediaProfileConnected() {
        assertThat(mHelper.isMediaProfileConnected()).isFalse();
    }

    @Test
    public void isBluetoothOn() {
        assertThat(mHelper.isBluetoothOn()).isFalse();
    }

    @Test
    public void isAirplaneModeOn() {
        assertThat(mHelper.isAirplaneModeOn()).isFalse();
    }

    @Test
    public void onAirplaneModeChanged() {
        mHelper.onAirplaneModeChanged(mService);

        verify(mService).onAirplaneModeChanged();
    }

    @Test
    public void setSettingsInt() {
        String testSettingsName = "BluetoothModeChangeHelperTest_test_settings_name";
        int value = 9876;

        try {
            mHelper.setSettingsInt(testSettingsName, value);
            assertWithMessage("SecurityException should happen!").fail();
        } catch (SecurityException ex) {
            // Expected
        }
    }

    @Test
    public void setSettingsSecureInt_throwsSecurityException() {
        String testSettingsName = "BluetoothModeChangeHelperTest_test_settings_name";
        int value = 9876;

        try {
            mHelper.setSettingsSecureInt(testSettingsName, value);
            assertWithMessage("SecurityException should happen!").fail();
        } catch (SecurityException ex) {
            // Expected
        }
    }

    @Test
    public void isBluetoothOnAPM_throwsSecurityException() {
        try {
            mHelper.isBluetoothOnAPM();
            assertWithMessage("SecurityException should happen!").fail();
        } catch (SecurityException ex) {
            // Expected
        }
    }

    @UiThreadTest
    @Test
    public void showToastMessage_doesNotCrash() {
        mHelper.showToastMessage();
    }

    @Test
    public void getBluetoothPackageName() {
        // TODO: Find a good way to specify the exact name of bluetooth package.
        //       mContext.getPackageName() does not work as this is not an instrumentation for it.
        assertThat(mHelper.getBluetoothPackageName()).isNotNull();
    }
}
