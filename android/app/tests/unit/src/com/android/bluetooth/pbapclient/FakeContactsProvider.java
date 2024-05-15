/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.pbapclient;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.SdpPseRecord;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.util.Log;
import android.util.SparseArray;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardConstants;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardProperty;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public  class FakeContactsProvider extends MockContentProvider {
    private static final String TAG = "FakeContactsProvider";

    private static final String MIMETYPE_PBAP_PHONEBOOK =
            "vnd.android.cursor.item/vnd.com.android.bluetooth.phonebook";

    public static class FakeAccount {
        int mId;
        String mType;
        String mName;
        boolean mVisible;

        public FakeAccount(int id, String type, String name, boolean visible) {
            mId = id;
            mType = type;
            mName = name;
            mVisible = visible;
        }

        public int getId() {
            return mId;
        }

        public String getAccountType() {
            return mType;
        }

        public String getAccountName() {
            return mName;
        }

        public void setVisibility(boolean visible) {
            mVisible = visible;
        }
    }

    public static class FakeRawContact {
        Uri mUri;
        int mId;
        String mAccountType;
        String mAccountName;
        boolean mStarred;
        String mPhonebook;

        public FakeRawContact(int id, String accountType, String accountName, boolean starred, String phonebook) {
            mId = id;
            mAccountType = accountType;
            mAccountName = accountName;
            mStarred = starred;
            mPhonebook = phonebook;
            mUri = RawContacts.CONTENT_URI.buildUpon().appendPath(String.valueOf(mId)).build();
        }

        public int getId() {
            return mId;
        }

        public String getAccountType() {
            return mAccountType;
        }

        public String getAccountName() {
            return mAccountName;
        }

        public String getPhonebook() {
            return mPhonebook;
        }

        public boolean isFavorite() {
            return mStarred;
        }

        public Uri getUri() {
            return mUri;
        }

        public void setPhonebook(String phonebook) {
            mPhonebook = phonebook;
        }
    }

    public static class FakeData {
        Uri mUri;
        int mId;
        int mRawContactId;
        Map<String, String> mData = new HashMap<>();

        public FakeData(int id, ContentValues values) {
            mId = id;
            for (String key : values.keySet()) {
                String value = values.getAsString(key);
                setField(key, value);
            }
            mUri = Data.CONTENT_URI.buildUpon().appendPath(String.valueOf(mId)).build();
        }

        public int getId() {
            return mId;
        }

        public int getRawContactId() {
            return mRawContactId;
        }

        public String getField(String field) {
            return mData.get(field);
        }

        public Map<String, String> getFields() {
            return mData;
        }

        public void setField(String field, String value) {
            if (Data.RAW_CONTACT_ID.equals(field)) {
                    mRawContactId = Integer.parseInt(value);
            } else {
                mData.put(field, value);
            }
        }

        public Uri getUri() {
            return mUri;
        }
    }

    public static class FakeCallLog {
        int mId;
        String mPhoneAccount;
        String mType;
        String mTimestamp;
        String mWho;
        Uri mUri;

        public FakeCallLog(int id, String phoneAccount, String type, String timestamp, String who) {
            mId = id;
            mPhoneAccount = phoneAccount;
            mType = type;
            mTimestamp = timestamp;
            mWho = who;
            mUri = CallLog.Calls.CONTENT_URI.buildUpon().appendPath(String.valueOf(mId)).build();
        }

        public int getId() {
            return mId;
        }

        public String getPhoneAccount() {
            return mPhoneAccount;
        }

        public Uri getUri() {
            return mUri;
        }
    }

    private int mNextDataId = 0;
    private SparseArray<FakeAccount> mAccounts = new SparseArray<>();
    private SparseArray<FakeRawContact> mRawContacts = new SparseArray<>();
    private SparseArray<FakeData> mData = new SparseArray<>();
    private SparseArray<FakeCallLog> mCallHistory = new SparseArray<>();

    //********************************************************************************************//
    // Get Data (for validation)
    //********************************************************************************************//

    public SparseArray<FakeAccount> getAccounts() {
        return mAccounts;
    }

    public SparseArray<FakeRawContact> getFavorites() {
        SparseArray<FakeRawContact> favorites = new SparseArray<>();
        for (int i = mRawContacts.size() - 1; i >= 0; i--) {
                int key = mRawContacts.keyAt(i);
                FakeRawContact contact = mRawContacts.valueAt(i);
                if (contact.isFavorite()) {
                    favorites.put(key, contact);
                }
            }
        return favorites;
    }

    public SparseArray<FakeRawContact> getRawContacts() {
        return mRawContacts;
    }

    public SparseArray<FakeData> getData() {
        return mData;
    }

    public SparseArray<FakeCallLog> getCallHistory() {
        return mCallHistory;
    }

    //********************************************************************************************//
    // Set Data (for mocking starting state)
    //********************************************************************************************//

    public void addAccount(String type, String name) {
        int id = getNextId();
        FakeAccount account = new FakeAccount(id, type, name, true);
        mAccounts.put(account.getId(), account);
    }

    public void addContact(String accountType, String accountName, boolean favorite, String phonebook, String phone) {
        int rawContactId = getNextId();
        FakeRawContact contact = new FakeRawContact(rawContactId, accountType, accountName, favorite, phonebook);
        mRawContacts.put(contact.getId(), contact);

        int id = getNextId();
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Data.RAW_CONTACT_ID, rawContactId);
        phoneValues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        phoneValues.put(Phone.DATA, phone);
        phoneValues.put(Phone.TYPE, Phone.TYPE_HOME);
        FakeData phoneData = new FakeData(id, phoneValues);
        mData.put(id, phoneData);

        id = getNextId();
        ContentValues phonebookValues = new ContentValues();
        phonebookValues.put(Data.RAW_CONTACT_ID, rawContactId);
        phonebookValues.put(Data.MIMETYPE, MIMETYPE_PBAP_PHONEBOOK);
        phonebookValues.put(Data.DATA1, phonebook);
        FakeData phonebookData = new FakeData(id, phonebookValues);
        mData.put(id, phonebookData);
    }

    public void addCallLog(String phoneAccount, int type, String time, String who) {
        int id = getNextId();
        FakeCallLog callLog = new FakeCallLog(id, phoneAccount, String.valueOf(type), time, who);
        mCallHistory.put(id, callLog);
    }

    //********************************************************************************************//
    // Query Operations
    //********************************************************************************************//

    @Override
    public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        Log.i(TAG, "APPLY: authority=" + authority + ", operations=" + operations.size());

        ContentProviderResult[] backRefs = new ContentProviderResult[operations.size()];
        int numBackRefs = 0;

        for (ContentProviderOperation operation : operations) {
            ContentProviderResult result = operation.apply(this, backRefs, numBackRefs);
            if (result != null) {
                backRefs[numBackRefs] = result;
                numBackRefs++;
            }
        }

        return backRefs;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.i(TAG, "INSERT: uri=" + uri + ", values=" + values);

        if (uri == null || values == null) {
            return null;
        }

        Uri base = uri.buildUpon().query(null).build();

        // Insert type 1: raw contacts
        if (RawContacts.CONTENT_URI.equals(base)) {
            String accountType = values.getAsString(ContactsContract.RawContacts.ACCOUNT_TYPE);
            String accountName = values.getAsString(ContactsContract.RawContacts.ACCOUNT_NAME);
            Boolean starred = values.getAsBoolean(ContactsContract.Contacts.STARRED);

            int id = getNextId();
            FakeRawContact contact = new FakeRawContact(id, accountType, accountName, (starred == null ? false : starred.booleanValue()), null);
            mRawContacts.put(id, contact);

            ensureAccount(accountType, accountName);

            return contact.getUri();
        }

        // Insert type 2: data
        if (Data.CONTENT_URI.equals(base)) {
            int id = getNextId();
            FakeData data = new FakeData(id, values);
            mData.put(id, data);
            return data.getUri();
        }

        // Insert type 3: call logs
        if (Calls.CONTENT_URI.equals(base)) {
            String type = values.getAsString(CallLog.Calls.TYPE);
            String phoneAccount = values.getAsString(Calls.PHONE_ACCOUNT_ID);
            String who = values.getAsString(CallLog.Calls.NUMBER);
            String time = values.getAsString(CallLog.Calls.DATE);

            int id = getNextId();
            FakeCallLog callLog = new FakeCallLog(id, phoneAccount, type, time, who);
            mCallHistory.put(id, callLog);

            return callLog.getUri();
        }

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
        Log.i(TAG, "UPDATE: uri=" + uri + ", where=" + where + ", args=" + Arrays.toString(selectionArgs));

        // Update type 1: by where clause, raw contact ID, updates the SYNC1 field with a
        // phonebook type
        if (where.contains(RawContacts._ID)) {
            int rawContactId = Integer.parseInt(selectionArgs[0]);
            FakeRawContact contact = mRawContacts.get(rawContactId);
            String phonebook = values.getAsString(RawContacts.SYNC1);
            contact.setPhonebook(phonebook);
            mRawContacts.put(rawContactId, contact);
            return 1;
        }

        // Update type 2: by query parameter, account name and type, updates the account
        // visibility "show" value of a given account name and type
        // TODO: Caching -- // ContactsContract.Settings.UNGROUPED_VISIBLE,
        Set<String> paramNames = uri.getQueryParameterNames();
        if (paramNames != null && !paramNames.isEmpty()) {
            String accountName = uri.getQueryParameter(RawContacts.ACCOUNT_NAME);
            String accountType = uri.getQueryParameter(RawContacts.ACCOUNT_TYPE);
            boolean visible = values.getAsBoolean(ContactsContract.Settings.UNGROUPED_VISIBLE);

            FakeAccount account = findAccount(accountType, accountName);
            if (account != null) {
                account.setVisibility(visible);
                return 1;
            }
        }

        return 0;
    }

    @Override
    public int delete(Uri uri, String where, String[] selectionArgs) {
        Log.i(TAG, "DELETE: uri=" + uri + ", where=" + where + ", args=" + Arrays.toString(selectionArgs));

        // Delete type 1: by query parameter, account name and type, delete account and all data
        // associated with it
        Set<String> paramNames = uri.getQueryParameterNames();
        if (paramNames != null && !paramNames.isEmpty()) {
            String accountName = uri.getQueryParameter(RawContacts.ACCOUNT_NAME);
            String accountType = uri.getQueryParameter(RawContacts.ACCOUNT_TYPE);

            // Remove raw contact
            List<Integer> rawContactsToRemove = new ArrayList<>();
            for (int i = mRawContacts.size() - 1; i >= 0; i--) {
                int key = mRawContacts.keyAt(i);
                FakeRawContact contact = mRawContacts.valueAt(i);
                if (accountType.equals(contact.getAccountType())
                        && accountName.equals(contact.getAccountName())) {
                    mRawContacts.remove(key);
                    rawContactsToRemove.add(key);
                }
            }

            // remove data for raw contact
            for (int i = mData.size() - 1; i >= 0; i--) {
                int key = mData.keyAt(i);
                FakeData data = mData.valueAt(i);
                if (rawContactsToRemove.contains(data.getRawContactId())) {
                    mData.remove(key);
                }
            }

            return rawContactsToRemove.size();
        }

        // Delete type 2: by where clause, account and phonebook type
        if (where.contains(ContactsContract.RawContacts.ACCOUNT_TYPE)
                && where.contains(ContactsContract.RawContacts.ACCOUNT_NAME)
                && where.contains(ContactsContract.RawContacts.SYNC1)) {
            String accountType = selectionArgs[0];
            String accountName = selectionArgs[1];
            String phonebook = selectionArgs[2];

            List<Integer> rawContactsToRemove = new ArrayList<>();
            for (int i = mRawContacts.size() - 1; i >= 0; i--) {
                int key = mRawContacts.keyAt(i);
                FakeRawContact contact = mRawContacts.valueAt(i);
                if (phonebook.equals(contact.getPhonebook())
                        && accountType.equals(contact.getAccountType())
                        && accountName.equals(contact.getAccountName())) {
                    mRawContacts.remove(key);
                    rawContactsToRemove.add(key);
                }
            }

            // remove data for raw contact
            for (int i = mData.size() - 1; i >= 0; i--) {
                int key = mData.keyAt(i);
                FakeData data = mData.valueAt(i);
                if (rawContactsToRemove.contains(data.getRawContactId())) {
                    mData.remove(key);
                }
            }

            return rawContactsToRemove.size();
        }

        // Delete type 3: by where clause, phone account id for Call logs
        // CallLog.Calls.PHONE_ACCOUNT_ID
        if (where.contains(CallLog.Calls.PHONE_ACCOUNT_ID)) {
            String phoneAccountId = selectionArgs[0];

            int removed = 0;
            for (int i = mCallHistory.size() - 1; i >= 0; i--) {
                int key = mCallHistory.keyAt(i);
                FakeCallLog call = mCallHistory.valueAt(i);
                if (phoneAccountId.equals(call.getPhoneAccount())) {
                    mCallHistory.remove(key);
                    removed++;
                }
            }

            return removed;
        }

        return 0;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        Log.i(TAG, "QUERY: uri=" + uri + ", select=" + projection + ", where=" + selection + ", args=" + selectionArgs + ", order=" + sortOrder);

        Cursor cursor = mock(Cursor.class);
        // when(cursor.moveToFirst()).thenReturn(true);
        // when(cursor.moveToNext()).thenReturn(true).thenReturn(false);
        // when(cursor.getLong(anyInt())).thenReturn((long) mContentValues.size());
        // when(cursor.getString(anyInt())).thenReturn(String.valueOf(mContentValues.size()));
        return cursor;
    }

    //********************************************************************************************//
    // Utilities
    //********************************************************************************************//

    private int getNextId() {
        int id = mNextDataId;
        mNextDataId++;
        return id;
    }

    private FakeAccount findAccount(String type, String name) {
        for (int i = 0; i < mAccounts.size(); i++) {
            FakeAccount account = mAccounts.valueAt(i);
            if (type.equals(account.getAccountType()) && name.equals(account.getAccountName())) {
                return account;
            }
        }
        return null;
    }

    private void ensureAccount(String name, String type) {
        if (findAccount(type, name) == null) {
            int id = getNextId();
            FakeAccount account = new FakeAccount(id, type, name, true);
            mAccounts.put(id, account);
        }
    }

    public void dump() {
        Log.i(TAG, "Accounts (" + mAccounts.size() + "):");
        for (int i = 0; i < mAccounts.size(); i++) {
            int id = mAccounts.keyAt(i);
            FakeAccount account = mAccounts.valueAt(i);
            Log.i(TAG, "    [" + id + "]: type=" + account.getAccountType() + ", name=" + account.getAccountName());
        }

        Log.i(TAG, "Contacts (raw=" + mRawContacts.size() + ", data=" + mData.size() + "):");
        for (int i = 0; i < mRawContacts.size(); i++) {
            int id = mRawContacts.keyAt(i);
            FakeRawContact contact = mRawContacts.valueAt(i);
            Log.i(TAG, "    [" + id + "]: phonebook=" + contact.getPhonebook() + ", favorite=" + contact.isFavorite());

            for (int j = 0; j < mData.size(); j++) {
                int dataId = mData.keyAt(j);
                FakeData data = mData.valueAt(j);
                if (id == data.getRawContactId()) {
                    StringBuilder sb = new StringBuilder();
                    for (String key : data.getFields().keySet()) {
                        String value = data.getFields().get(key);
                        sb.append(" " + key + "='" + value + "'");
                    }
                    Log.i(TAG, "        [" + dataId + "]:" + sb.toString());
                }
            }
        }
    }
}