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

package com.android.bluetooth.opp;

import static com.android.bluetooth.opp.BluetoothOppManager.OPP_PREFERENCE_FILE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BluetoothOppPreferenceTest {
    private static final String ADDRESS = "AA:BB:CC:DD:EE:FF";
    private final Context mContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Mock private AdapterService mAdapterService;
    private BluetoothMethodProxy mCallProxy;
    private BluetoothOppPreference mOppPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mContext).when(mAdapterService).asContext();
        doReturn(ADDRESS).when(mAdapterService).getIdentityAddress(any());

        mCallProxy = spy(BluetoothMethodProxy.getInstance());
        BluetoothMethodProxy.setInstanceForTesting(mCallProxy);
        mOppPreference = BluetoothOppPreference.getInstance(mAdapterService);

        doReturn(null).when(mCallProxy).contentResolverInsert(
                any(), eq(BluetoothShare.CONTENT_URI), any());
    }

    @After
    public void tearDown() {
        BluetoothMethodProxy.setInstanceForTesting(null);
        BluetoothOppUtility.sSendFileMap.clear();
        mContext.getSharedPreferences(OPP_PREFERENCE_FILE, 0).edit().clear().apply();
        BluetoothOppManager.sInstance = null;
    }

    @Test
    public void dump_shouldNotThrow() {
        mOppPreference.dump();
    }

    @Test
    public void setNameAndGetNameAndRemoveName_setsAndGetsAndRemovesNameCorrectly() {
        String name = "randomName";
        BluetoothDevice device =
                mContext.getSystemService(BluetoothManager.class)
                        .getAdapter()
                        .getRemoteDevice(ADDRESS);
        mOppPreference.setName(device, name);

        assertThat(mOppPreference.getName(device)).isEqualTo(name);

        // Undo the change so this will not be saved on share preference
        mOppPreference.removeName(device);
        assertThat(mOppPreference.getName(device)).isNull();
    }

    @Test
    public void setChannelAndGetAndRemoveChannel_setsAndGetsAndRemovesChannelCorrectly() {
        int uuid = 1234;
        int channel = 78910;
        BluetoothDevice device =
                (mContext.getSystemService(BluetoothManager.class))
                        .getAdapter()
                        .getRemoteDevice(ADDRESS);
        mOppPreference.setChannel(device, uuid, channel);
        assertThat(mOppPreference.getChannel(device, uuid)).isEqualTo(channel);

        // Undo the change so this will not be saved on share preference
        mOppPreference.removeChannel(device, uuid);
        assertThat(mOppPreference.getChannel(device, uuid)).isEqualTo(-1);
    }
}
