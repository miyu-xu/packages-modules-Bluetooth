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

package com.android.bluetooth.map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class BluetoothMapContentTest {
    @Mock
    private Context mTargetContext;
    private BluetoothMapAccountItem mAccountItem;
    @Mock
    private BluetoothMapMasInstance mMasInstance;
    @Mock
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createBluetoothMapContent() {
        when(mTargetContext.getContentResolver()).thenReturn(null);
        BluetoothMapContent mMapContentWithAccount = new BluetoothMapContent(mTargetContext, mAccountItem, mMasInstance);
//        assertThat(mMapContentWithAccount.mAccount).isEqualTo(mAccountItem);
        BluetoothMapContent mMapContentWithoutAccount = new BluetoothMapContent(mTargetContext, null, mMasInstance);
//        assertThat(mMapContentWithoutAccount.mAccount).isNull();
    }

    @Test
    public void getTextPartsMms() {
        final long id = 1111;
        assertThat(BluetoothMapContent.getTextPartsMms(mContentResolver, id)).isEqualTo("");
    }

    @Test
    public void getContactNameFromPhone() {
        String TEST_PHONE = "testPhone";
        assertThat(BluetoothMapContent.getContactNameFromPhone(TEST_PHONE, mContentResolver)).isEqualTo(null);
    }

    @Test
    public void getCanonicalAddressSms() {
        int threadId = 0;
        assertThat(BluetoothMapContent.getCanonicalAddressSms(mContentResolver, threadId)).isEqualTo("");
    }
}
