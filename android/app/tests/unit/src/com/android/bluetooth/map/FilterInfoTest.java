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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.database.MatrixCursor;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FilterInfoTest {
    private static final int TEST_INT = 1;

    private BluetoothMapContent.FilterInfo mFilterInfo;

    @Before
    public void setUp() {
        mFilterInfo = new BluetoothMapContent.FilterInfo();
    }

    @Test
    public void setMessageColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setMessageColumns(cursor);

        assertThat(mFilterInfo.mMessageColId).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColDate).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColSubject).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColFolder).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColRead).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColSize).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColFromAddress).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColToAddress).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColAttachment).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColAttachmentSize).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColPriority).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColProtected).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColReception).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColDelivery).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColThreadId).isEqualTo(TEST_INT);
    }

    @Test
    public void setEmailMessageColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setEmailMessageColumns(cursor);

        assertThat(mFilterInfo.mMessageColCcAddress).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColBccAddress).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColReplyTo).isEqualTo(TEST_INT);
    }

    @Test
    public void setImMessageColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setImMessageColumns(cursor);

        assertThat(mFilterInfo.mMessageColThreadName).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColAttachmentMime).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMessageColBody).isEqualTo(TEST_INT);
    }

    @Test
    public void setEmailImConvoColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setEmailImConvoColumns(cursor);

        assertThat(mFilterInfo.mConvoColConvoId).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mConvoColLastActivity).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mConvoColName).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mConvoColRead).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mConvoColVersionCounter).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mConvoColSummary).isEqualTo(TEST_INT);
    }

    @Test
    public void setEmailImConvoContactColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setEmailImConvoContactColumns(cursor);

        assertThat(mFilterInfo.mContactColBtUid).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColChatState).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColContactUci).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColNickname).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColLastActive).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColName).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColPresenceState).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColPresenceText).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mContactColPriority).isEqualTo(TEST_INT);
    }

    @Test
    public void setSmsColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setSmsColumns(cursor);

        assertThat(mFilterInfo.mSmsColId).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColFolder).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColRead).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColSubject).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColAddress).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColDate).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColType).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mSmsColThreadId).isEqualTo(TEST_INT);
    }

    @Test
    public void setMmsColumns() {
        MatrixCursor cursor = mock(MatrixCursor.class);
        when(cursor.getColumnIndex(anyString())).thenReturn(TEST_INT);

        mFilterInfo.setMmsColumns(cursor);

        assertThat(mFilterInfo.mMmsColId).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColFolder).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColRead).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColAttachmentSize).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColTextOnly).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColSize).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColDate).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColSubject).isEqualTo(TEST_INT);
        assertThat(mFilterInfo.mMmsColThreadId).isEqualTo(TEST_INT);
    }
}
