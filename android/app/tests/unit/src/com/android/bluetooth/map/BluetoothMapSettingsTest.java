/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.bluetooth.map;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BluetoothMapSettingsTest {

    Context mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Intent mIntent;

    @Before
    public void setUp() {
        mIntent = new Intent();
        mIntent.setClass(mTargetContext, BluetoothMapSettings.class);
    }

    @Test
    public void initialize() throws Exception {
        enableActivity(true);
        ActivityScenario<BluetoothMapSettings> activityScenario = null;

        try {
            activityScenario = ActivityScenario.launch(mIntent);
        } finally {
            if (activityScenario != null) {
                // Workaround for b/159805732. Without this, test hangs for 45 seconds.
                activityScenario.close();
            }
            enableActivity(false);
        }
    }

    private void enableActivity(boolean enable) {
        int enabledState = enable ? COMPONENT_ENABLED_STATE_ENABLED
                : COMPONENT_ENABLED_STATE_DEFAULT;

        mTargetContext.getPackageManager().setApplicationEnabledSetting(
                mTargetContext.getPackageName(), enabledState, DONT_KILL_APP);

        ComponentName activityName = new ComponentName(mTargetContext, BluetoothMapSettings.class);
        mTargetContext.getPackageManager().setComponentEnabledSetting(
                activityName, enabledState, DONT_KILL_APP);
    }
}
