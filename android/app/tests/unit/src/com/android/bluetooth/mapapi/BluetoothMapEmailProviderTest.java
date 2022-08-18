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

package com.android.bluetooth.mapapi;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class BluetoothMapEmailProviderTest {

    private static final String AUTHORITY = "com.test";
    private static final String ACCOUNT_ID = "12345";
    private static final String MESSAGE_ID = "987654321";

    private static final Uri MESSAGE_URI =
            BluetoothMapContract.buildMessageUriWithId(AUTHORITY, ACCOUNT_ID, MESSAGE_ID);

    private Context mContext;

    @Spy
    private BluetoothMapEmailProvider mProvider = new TestBluetoothMapEmailProvider();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testAttachInfo_whenProviderIsNotExported() throws Exception {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = AUTHORITY;
        providerInfo.exported = false;

        assertThrows(SecurityException.class,
                () -> mProvider.attachInfo(mContext, providerInfo));
    }

    @Test
    public void testAttachInfo_whenNoPermission() throws Exception {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = AUTHORITY;
        providerInfo.exported = true;
        providerInfo.writePermission = "some.random.permission";

        assertThrows(SecurityException.class,
                () -> mProvider.attachInfo(mContext, providerInfo));
    }

    @Test
    public void testAttachInfo_success() throws Exception {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = AUTHORITY;
        providerInfo.exported = true;
        providerInfo.writePermission = android.Manifest.permission.BLUETOOTH_MAP;

        try {
            mProvider.attachInfo(mContext, providerInfo);
        } catch (Exception e) {
            assertWithMessage("Exception should not happen.").fail();
        }
    }

    @Test
    public void testGetType() throws Exception {
        try {
            mProvider.getType(/*uri=*/ null);
        } catch (Exception e) {
            assertWithMessage("Exception should not happen.").fail();
        }
    }

    @Test
    public void testDelete_whenTableNameIsWrong() throws Exception {
        Uri uriWithWrongTable = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ACCOUNT_ID)
                .appendPath("some_random_table_name")
                .appendPath(MESSAGE_ID)
                .build();

        // No rows are impacted.
        assertThat(mProvider.delete(uriWithWrongTable, /*where=*/null, /*selectionArgs=*/null))
                .isEqualTo(0);
    }

    @Test
    public void testDelete_success() throws Exception {
        mProvider.delete(MESSAGE_URI, /*where=*/null, /*selectionArgs=*/null);
        verify(mProvider).deleteMessage(ACCOUNT_ID, MESSAGE_ID);
    }

    @Test
    public void testInsert_whenFolderIdIsNull() throws Exception {
        ContentValues values = new ContentValues();

        assertThrows(IllegalArgumentException.class, () -> mProvider.insert(MESSAGE_URI, values));
    }

    @Test
    public void testInsert_whenTableNameIsWrong() throws Exception {
        Uri uriWithWrongTable = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ACCOUNT_ID)
                .appendPath("some_random_table_name")
                .appendPath(MESSAGE_ID)
                .build();
        final long folderId = 12345;
        ContentValues values = new ContentValues();
        values.put(BluetoothMapContract.MessageColumns.FOLDER_ID, folderId);

        assertThat(mProvider.insert(uriWithWrongTable, values)).isNull();
    }

    @Test
    public void testInsert_success() throws Exception {
        final long folderId = 12345;
        ContentValues values = new ContentValues();
        values.put(BluetoothMapContract.MessageColumns.FOLDER_ID, folderId);

        mProvider.insert(MESSAGE_URI, values);
        verify(mProvider).deleteMessage(ACCOUNT_ID, MESSAGE_ID);
    }

    @Test
    public void testQuery_forAccountUri() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = AUTHORITY;
        providerInfo.exported = true;
        providerInfo.writePermission = android.Manifest.permission.BLUETOOTH_MAP;
        mProvider.attachInfo(mContext, providerInfo);

        Uri accountUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ACCOUNT_ID)
                .appendPath(BluetoothMapContract.TABLE_ACCOUNT)
                .appendPath(MESSAGE_ID)
                .build();

        mProvider.query(accountUri, /*projection=*/ null, /*selection=*/ null,
                /*selectionArgs=*/ null, /*sortOrder=*/ null);
        verify(mProvider).queryAccount(any(), any(), any(), any());
    }

    @Test
    public void testQuery_forFolderUri() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = AUTHORITY;
        providerInfo.exported = true;
        providerInfo.writePermission = android.Manifest.permission.BLUETOOTH_MAP;
        mProvider.attachInfo(mContext, providerInfo);

        final int folderId = 13579;
        Uri folderUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ACCOUNT_ID)
                .appendPath(BluetoothMapContract.TABLE_FOLDER)
                .appendPath(Integer.toString(folderId))
                .build();

        mProvider.query(folderUri, /*projection=*/ null, /*selection=*/ null,
                /*selectionArgs=*/ null, /*sortOrder=*/ null);
        verify(mProvider).queryFolder(eq(ACCOUNT_ID), any(), any(), any(), any());
    }

    @Test
    public void testQuery_forMessageUri() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = AUTHORITY;
        providerInfo.exported = true;
        providerInfo.writePermission = android.Manifest.permission.BLUETOOTH_MAP;
        mProvider.attachInfo(mContext, providerInfo);

        mProvider.query(MESSAGE_URI, /*projection=*/ null, /*selection=*/ null,
                /*selectionArgs=*/ null, /*sortOrder=*/ null);
        verify(mProvider).queryMessage(eq(ACCOUNT_ID), any(), any(), any(), any());
    }

    @Test
    public void testUpdate_whenTableIsNull() {
        Uri uriWithoutTable = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .build();
        ContentValues values = new ContentValues();

        assertThrows(IllegalArgumentException.class,
                () -> mProvider.update(uriWithoutTable, values, /*selection=*/ null,
                        /*selectionArgs=*/ null));
    }

    @Test
    public void testUpdate_whenSelectionIsNotNull() {
        Uri uriWithTable = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ACCOUNT_ID)
                .appendPath(BluetoothMapContract.TABLE_ACCOUNT)
                .build();
        ContentValues values = new ContentValues();

        String nonNullSelection = "id = 1234";

        assertThrows(IllegalArgumentException.class,
                () -> mProvider.update(uriWithTable, values, nonNullSelection,
                        /*selectionArgs=*/ null));
    }

    @Test
    public void testUpdate_forAccountUri_success() {
        Uri accountUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ACCOUNT_ID)
                .appendPath(BluetoothMapContract.TABLE_ACCOUNT)
                .appendPath(MESSAGE_ID)
                .build();

        ContentValues values = new ContentValues();
        final int flagValue = 1;
        values.put(BluetoothMapContract.AccountColumns._ID, ACCOUNT_ID);
        values.put(BluetoothMapContract.AccountColumns.FLAG_EXPOSE, flagValue);

        mProvider.update(accountUri, values, /*selection=*/ null, /*selectionArgs=*/ null);
        verify(mProvider).updateAccount(ACCOUNT_ID, flagValue);
    }

    @Test
    public void testCall_whenMethodIsWrong() {
        String method = "some_random_method";
        assertThat(mProvider.call(method, /*arg=*/ null, /*extras=*/ null)).isNull();
    }

    @Test
    public void testCall_whenExtrasDoesNotHaveAccountId() {
        Bundle extras = new Bundle();
        extras.putLong(BluetoothMapContract.EXTRA_UPDATE_FOLDER_ID, 12345);

        assertThat(mProvider.call(BluetoothMapContract.METHOD_UPDATE_FOLDER, /*arg=*/ null, extras))
                .isNull();
    }

    @Test
    public void testCall_whenExtrasDoesNotHaveFolderId() {
        Bundle extras = new Bundle();
        extras.putLong(BluetoothMapContract.EXTRA_UPDATE_ACCOUNT_ID, 12345);

        assertThat(mProvider.call(BluetoothMapContract.METHOD_UPDATE_FOLDER, /*arg=*/ null, extras))
                .isNull();
    }

    @Test
    public void testCall_success() {
        final long accountId = 12345;
        final long folderId = 6789;

        Bundle extras = new Bundle();
        extras.putLong(BluetoothMapContract.EXTRA_UPDATE_ACCOUNT_ID, accountId);
        extras.putLong(BluetoothMapContract.EXTRA_UPDATE_FOLDER_ID, folderId);

        mProvider.call(BluetoothMapContract.METHOD_UPDATE_FOLDER, /*arg=*/ null, extras);
        verify(mProvider).syncFolder(accountId, folderId);
    }

    @Test
    public void testShutdown() {
        try {
            mProvider.shutdown();
        } catch (Exception e) {
            assertWithMessage("Exception should not happen.").fail();
        }
    }

    @Test
    public void testGetAccountId_whenNotEnoughSegments() {
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> BluetoothMapEmailProvider.getAccountId(uri));
    }

    @Test
    public void testGetAccountId_success() {
        assertThat(BluetoothMapEmailProvider.getAccountId(MESSAGE_URI)).isEqualTo(ACCOUNT_ID);
    }

    public static class TestBluetoothMapEmailProvider extends BluetoothMapEmailProvider {
        @Override
        protected void WriteMessageToStream(long accountId, long messageId,
                boolean includeAttachment, boolean download, FileOutputStream out)
                throws IOException {

        }

        @Override
        protected Uri getContentUri() {
            return null;
        }

        @Override
        protected void UpdateMimeMessageFromStream(FileInputStream input, long accountId,
                long messageId) throws IOException {

        }

        @Override
        protected int deleteMessage(String accountId, String messageId) {
            return 0;
        }

        @Override
        protected String insertMessage(String accountId, String folderId) {
            return null;
        }

        @Override
        protected Cursor queryAccount(String[] projection, String selection, String[] selectionArgs,
                String sortOrder) {
            return null;
        }

        @Override
        protected Cursor queryFolder(String accountId, String[] projection, String selection,
                String[] selectionArgs, String sortOrder) {
            return null;
        }

        @Override
        protected Cursor queryMessage(String accountId, String[] projection, String selection,
                String[] selectionArgs, String sortOrder) {
            return null;
        }

        @Override
        protected int updateAccount(String accountId, int flagExpose) {
            return 0;
        }

        @Override
        protected int updateMessage(String accountId, Long messageId, Long folderId,
                Boolean flagRead) {
            return 0;
        }

        @Override
        protected int syncFolder(long accountId, long folderId) {
            return 0;
        }

        @Override
        public boolean onCreate() {
            return false;
        }
    };

}
