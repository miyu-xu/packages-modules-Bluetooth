/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.bluetooth;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AvrcpTest {
    private static final String TAG = "AvrcpTest";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothAdapter mAdapter =
            mContext.getSystemService(BluetoothManager.class).getAdapter();

    private final Host mHost = new Host(mContext);

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Before
    public void setup() throws Exception {
        mHost.createBondAndVerify(mBumble);
    }

    @After
    public void tearDown() throws Exception {
        BluetoothDevice device = mBumble.getRemoteDevice();
        if (mAdapter.getBondedDevices().contains(device)) {
            Log.d(TAG, "Calling removeBondAndVerify");
            mHost.removeBondAndVerify(device);
        }
        Log.d(TAG, "close the host");
        mHost.close();
    }

    @Test
    public void avrcpTestExample() throws Exception {
        Log.d(TAG, "avrcp test example");
        Thread.sleep(5000);
    }
}
