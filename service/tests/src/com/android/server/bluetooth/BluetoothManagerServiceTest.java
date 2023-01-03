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

import android.content.AttributionSource;
import android.content.ComponentName;
import android.os.Process;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BluetoothManagerServiceTest {
  private static final AttributionSource SHELL_ATTRIBUTION_SOURCE =
      new AttributionSource(Process.SHELL_UID, "com.android.shell", null, (Set<String>) null, null);

  @Test
  public void bindTimeout() throws Exception {
    InstrumentationRegistry.getInstrumentation().getUiAutomation().adoptShellPermissionIdentity();
    BluetoothManagerService service =
        new BluetoothManagerService(InstrumentationRegistry.getTargetContext(), intent -> {
           return new ComponentName(InstrumentationRegistry.getTargetContext(), NeverBoundService.class);
        });
    service.handleOnBootPhase();
    service.enable(AttributionSource.myAttributionSource());
  }
}
