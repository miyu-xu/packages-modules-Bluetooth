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

import android.graphics.drawable.Drawable;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BluetoothMapAccountItemTest {
    private static final String TEST_NAME = "name";
    private static final String TEST_PACKAGE_NAME = "package_name";
    private static final String TEST_ID = "1111";
    private static final String TEST_PROVIDER_AUTHORITY = "provider_authority";

    private static final Drawable TEST_DRAWABLE = null;
    private static final BluetoothMapUtils.TYPE TEST_TYPE = BluetoothMapUtils.TYPE.NONE;
    private static final String TEST_UCI = "uci";
    private static final String TEST_UCI_PREFIX = "uci_prefix";

    private BluetoothMapAccountItem mAccountItemOne;
    private BluetoothMapAccountItem mAccountItemTwo;
    private BluetoothMapAccountItem mAccountItemThree;

    @Before
    public void setUp() {
        mAccountItemOne = new BluetoothMapAccountItem(TEST_ID, TEST_NAME, TEST_PACKAGE_NAME,
                TEST_PROVIDER_AUTHORITY, TEST_DRAWABLE, TEST_TYPE, TEST_UCI, TEST_UCI_PREFIX);
        mAccountItemTwo = new BluetoothMapAccountItem(TEST_ID, TEST_NAME, TEST_PACKAGE_NAME,
                TEST_PROVIDER_AUTHORITY, TEST_DRAWABLE, TEST_TYPE, TEST_UCI, null);
        mAccountItemThree = new BluetoothMapAccountItem(TEST_ID, TEST_NAME, TEST_PACKAGE_NAME,
                TEST_PROVIDER_AUTHORITY, TEST_DRAWABLE,
                BluetoothMapUtils.TYPE.EMAIL, null, null);
    }

    @Test
    public void createItemWithPartialParameters() {
        BluetoothMapAccountItem mAccountItem = BluetoothMapAccountItem.create(null, TEST_NAME,
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, TEST_DRAWABLE, TEST_TYPE);
        assertThat(mAccountItem.getAccountId()).isEqualTo(-1);
        assertThat(mAccountItem.getName()).isEqualTo(TEST_NAME);
        assertThat(mAccountItem.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(mAccountItem.getProviderAuthority()).isEqualTo(TEST_PROVIDER_AUTHORITY);
        assertThat(mAccountItem.getType()).isEqualTo(TEST_TYPE);
        assertThat(mAccountItem.getUci()).isNull();
        assertThat(mAccountItem.getUciPrefix()).isNull();

    }

    @Test
    public void createItemWithAllParameters() {
        BluetoothMapAccountItem mAccountItem = BluetoothMapAccountItem.create(TEST_ID, TEST_NAME,
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, TEST_DRAWABLE,
                TEST_TYPE, TEST_UCI, TEST_UCI_PREFIX);
        assertThat(mAccountItem.getAccountId()).isEqualTo(Long.parseLong(TEST_ID));
        assertThat(mAccountItem.getName()).isEqualTo(TEST_NAME);
        assertThat(mAccountItem.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(mAccountItem.getProviderAuthority()).isEqualTo(TEST_PROVIDER_AUTHORITY);
        assertThat(mAccountItem.getType()).isEqualTo(TEST_TYPE);
        assertThat(mAccountItem.getUci()).isEqualTo(TEST_UCI);
        assertThat(mAccountItem.getUciPrefix()).isEqualTo(TEST_UCI_PREFIX);
    }

    @Test
    public void getHashCode() {
        assertThat(mAccountItemOne.hashCode()).isEqualTo(-178102499);
    }

    @Test
    public void getUciFull() {
        assertThat(mAccountItemOne.getUciFull()).isEqualTo("uci_prefix:uci");
        assertThat(mAccountItemTwo.getUciFull()).isNull();
        assertThat(mAccountItemThree.getUciFull()).isNull();
    }

    @Test
    public void compareTwoObjectsWithEquals() {
        assertThat(mAccountItemOne.equals(mAccountItemTwo)).isTrue();
        assertThat(mAccountItemOne.equals(mAccountItemThree)).isFalse();
    }

    @Test
    public void compareTwoObjectsWithCompareTo() {
        assertThat(mAccountItemOne.compareTo(mAccountItemTwo)).isEqualTo(0);
        assertThat(mAccountItemOne.compareTo(mAccountItemThree)).isEqualTo(-1);
    }
}