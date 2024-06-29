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

package com.android.bluetooth.gatt;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;

import android.content.pm.PackageManager;

import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.UUID;

/** Test cases for {@link ContextMap}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ContextMapTest {
    private static final String APP_NAME = "com.android.what.a.name";
    private static final int ID = 123;

    @Rule public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AdapterService mAdapterService;
    @Mock private GattService mMockGatt;
    @Mock private PackageManager mMockPackageManager;

    @Spy private BluetoothMethodProxy mMapMethodProxy = BluetoothMethodProxy.getInstance();

    @Before
    public void setUp() throws Exception {
        BluetoothMethodProxy.setInstanceForTesting(mMapMethodProxy);

        TestUtils.setAdapterService(mAdapterService);

        doReturn(mMockPackageManager).when(mMockGatt).getPackageManager();
        doReturn(APP_NAME).when(mMockPackageManager).getNameForUid(anyInt());
    }

    @After
    public void tearDown() throws Exception {
        BluetoothMethodProxy.setInstanceForTesting(null);

        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void getByMethods() {
        ContextMap contextMap = new ContextMap<>();
        UUID uuid = UUID.randomUUID();
        ContextMap.App app = contextMap.add(uuid, null, mMockGatt);
        app.id = ID;

        ContextMap.App contextMapById = contextMap.getById(ID);
        assertThat(contextMapById.name).isEqualTo(APP_NAME);
        ContextMap.App contextMapByUuid = contextMap.getByUuid(uuid);
        assertThat(contextMapByUuid.name).isEqualTo(APP_NAME);
    }

    @Test
    public void testDump_doesNotCrash() throws Exception {
        StringBuilder sb = new StringBuilder();
        ContextMap contextMap = new ContextMap<>();
        contextMap.add(UUID.randomUUID(), null, mMockGatt);
        contextMap.dump(sb);
    }
}
