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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.provider.CallLog;

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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapClientContactsStorageTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String ACCOUNT_TYPE = "com.android.bluetooth.pbapclient.account";

    private BluetoothAdapter mAdapter = null;

    @Mock private Context mMockContext;
    @Mock private ContentResolver mMockContentResolver;
    @Mock private File mMockDirectory;
    @Mock private PbapClientAccountManager mMockAccountManager;
    private List<Account> mMockedAccounts = new ArrayList<>();
    @Mock private PbapClientContactsStorage.Callback mMockStorageCallback;
    private PbapClientContactsStorage.PbapClientAccountManagerCallback mAccountManagerCallback;

    private PbapClientContactsStorage mStorage;

    @Before
    public void setUp() throws Exception {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);

        // Mock PbapClientAccountManager to add/remove from a locally managed list
        doAnswer(invocation -> {
            BluetoothDevice device = (BluetoothDevice) invocation.getArgument(0);
            return getAccountForDevice(device);
        }).when(mMockAccountManager).getAccountForDevice(any(BluetoothDevice.class));

        doAnswer(invocation -> {
            Account account = (Account) invocation.getArgument(0);
            mMockedAccounts.add(account);
            return true;
        }).when(mMockAccountManager).addAccount(any(Account.class));

        doAnswer(invocation -> {
            Account account = (Account) invocation.getArgument(0);
            mMockedAccounts.remove(account);
            return true;
        }).when(mMockAccountManager).removeAccount(any(Account.class));

        doAnswer(invocation -> {
            return mMockedAccounts;
        }).when(mMockAccountManager).getAccounts();

        doReturn(mMockDirectory).when(mMockContext).getFilesDir();
        doReturn(new File[]{}).when(mMockDirectory).listFiles();

        doReturn(mMockContentResolver).when(mMockContext).getContentResolver();

        mStorage = new PbapClientContactsStorage(mMockContext, mMockAccountManager);
        mAccountManagerCallback = mStorage.new PbapClientAccountManagerCallback();
        mStorage.registerCallback(mMockStorageCallback);
    }

    @After
    public void tearDown() throws Exception {
        if (mStorage != null) {
            mStorage.unregisterCallback(mMockStorageCallback);
            mStorage.stop();
            mStorage = null;
        }
    }

    //********************************************************************************************//
    // Incoming Events
    //********************************************************************************************//

    // TODO: Start/stop/init

    // init with no accounts -> everything fine
    // init with accounts -> accounts deleted
    // init with previous data -> data deleted

    @Test
    public void testStartStorage_withoutExistingAccounts_storageReadyWithNoAccounts() {
        startStorage(new ArrayList<Account>());

        verify(mMockStorageCallback, times(1)).onStorageReady();
        verify(mMockStorageCallback, times(1)).onStorageAccountsChanged(new ArrayList<Account>(), new ArrayList<Account>());
        assertThat(mStorage.isStorageReady()).isTrue();
        assertThat(mStorage.getStorageAccounts()).isEmpty();
    }

    @Test
    public void testStartStorage_withExistingAccountsNoCaching_accountsCleanedUp() {
        BluetoothDevice device1 = TestUtils.getTestDevice(mAdapter, 1);
        Account account1 = getAccountForDevice(device1);
        BluetoothDevice device2 = TestUtils.getTestDevice(mAdapter, 2);
        Account account2 = getAccountForDevice(device2);
        List<Account> existingAccounts = Arrays.asList(new Account[]{account1, account2});

        startStorage(existingAccounts);

        verify(mMockAccountManager, times(1)).removeAccount(eq(account1));
        verify(mMockAccountManager, times(1)).removeAccount(eq(account2));

        verify(mMockStorageCallback, times(1)).onStorageReady();
        verify(mMockStorageCallback, times(1)).onStorageAccountsChanged(new ArrayList<Account>(), new ArrayList<Account>());
        assertThat(mStorage.isStorageReady()).isTrue();
        assertThat(mStorage.getStorageAccounts()).isEmpty();
    }

    @Test
    public void testGetStorageAccountForDevice() {
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 1);
        Account expected = getAccountForDevice(device);

        assertThat(mStorage.getStorageAccountForDevice(device)).isEqualTo(expected);
    }

    @Test
    public void testGetStorageAccounts_accountsExist_accountsReturned() {
        mMockedAccounts.add(getAccountForDevice(TestUtils.getTestDevice(mAdapter, 1)));
        mMockedAccounts.add(getAccountForDevice(TestUtils.getTestDevice(mAdapter, 2)));

        assertThat(mStorage.getStorageAccounts()).isEqualTo(mMockedAccounts);
    }

    @Test
    public void testGetStorageAccounts_noAccountsExist_emptyListReturned() {
        assertThat(mStorage.getStorageAccounts()).isEmpty();
    }

    @Test
    public void testAddAccount_accountAddedAndInAccountsList() {
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 1);
        Account account = mStorage.getStorageAccountForDevice(device);
        mStorage.addAccount(account);
        assertThat(mStorage.getStorageAccounts()).contains(account);
    }

    @Test
    public void testRemoveAccount_accountRemovedAndNotInAccountsList() {
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 1);
        Account account = mStorage.getStorageAccountForDevice(device);

        mMockedAccounts.add(account);
        assertThat(mStorage.getStorageAccounts()).contains(account);

        mStorage.removeAccount(account);
        assertThat(mStorage.getStorageAccounts()).doesNotContain(account);
    }

    @Test
    public void testRemoveAccount_accountDoesNotExist_accountsUnchanged() {
        BluetoothDevice device1 = TestUtils.getTestDevice(mAdapter, 1);
        Account account1 = mStorage.getStorageAccountForDevice(device1);

        BluetoothDevice device2 = TestUtils.getTestDevice(mAdapter, 2);
        Account account2 = mStorage.getStorageAccountForDevice(device2);

        mMockedAccounts.add(account1);
        assertThat(mStorage.getStorageAccounts()).contains(account1);

        mStorage.removeAccount(account2);
        assertThat(mStorage.getStorageAccounts().size()).isEqualTo(mMockedAccounts.size());
        assertThat(mStorage.getStorageAccounts()).contains(account1);
        assertThat(mStorage.getStorageAccounts()).doesNotContain(account2);
    }

    // Contacts DB interfaces

    // public boolean insertFavorites(Account account, List<VCardEntry> contacts)

    @Test
    public void testInsertFavorites_validFavoritesList_contactsInserted() {
        testStartStorage_withoutExistingAccounts_storageReadyWithNoAccounts();
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 1);
        Account account = mStorage.getStorageAccountForDevice(device);
        mStorage.addAccount(account);

        mStorage.insertFavorites(account, getMockContacts(account, PbapPhonebook.FAVORITES_PATH, 200));

        // ...
    }

    // public boolean insertLocalContacts(Account account, List<VCardEntry> contacts)

    @Test
    public void testInsertLocalContacts() {
        testStartStorage_withoutExistingAccounts_storageReadyWithNoAccounts();
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 1);
        Account account = mStorage.getStorageAccountForDevice(device);
        mStorage.addAccount(account);

        mStorage.insertLocalContacts(account, getMockContacts(account, PbapPhonebook.LOCAL_PHONEBOOK_PATH, 200));

        // ...
    }

    // public boolean insertSimContacts(Account account, List<VCardEntry> contacts)

    @Test
    public void testInsertSimContacts() {
        testStartStorage_withoutExistingAccounts_storageReadyWithNoAccounts();
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 1);
        Account account = mStorage.getStorageAccountForDevice(device);
        mStorage.addAccount(account);

        mStorage.insertSimContacts(account, getMockContacts(account, PbapPhonebook.SIM_PHONEBOOK_PATH, 200));

        // ...
    }

    // public void insertIncomingCallHistory(Account account, List<VCardEntry> history)
    // public void insertOutgoingCallHistory(Account account, List<VCardEntry> history)
    // public void insertMissedCallHistory(Account account, List<VCardEntry> history)

    // public boolean removeFavorites(Account account)
    // public boolean removeLocalContacts(Account account)
    // public boolean removeSimContacts(Account account)
    // public boolean removeAllContacts(Account account)
    // public void removeCallHistory(Account account)

    // error cases

    @Test
    public void testInsertContacts_storageNotReady_insertFails() {

    }

    @Test
    public void testInsertContacts_accountNull_insertFails() {

    }

    @Test
    public void testInsertContacts_contactsNull_insertFails() {

    }

    @Test
    public void testInsertContacts_contactsEmpty_insertFails() {

    }

    @Test
    public void testInsertCallHistory_storageNotReady_insertFails() {

    }

    @Test
    public void testInsertCallHistory_accountNull_insertFails() {

    }

    @Test
    public void testInsertCallHistory_historyNull_insertFails() {

    }

    @Test
    public void testInsertCallHistory_historyEmpty_insertFails() {

    }

    // Caching (start/init + Metadata + "hidden" contacts)

    // Caching - init with all combinations of account, metadata, and data (including integrity checks)
    // init with account:true,  metadata:true,  data:true  -> Account, metadata, data exist
    // init with account:true,  metadata:true,  data:false -> Account deleted, metadata deleted
    // init with account:true,  metadata:false, data:true  -> account deleted, data deleted
    // init with account:true,  metadata:false, data:false -> account deleted
    // init with account:false, metadata:true,  data:true  -> Metadata deleted, account deleted
    // init with account:false, metadata:true,  data:false -> Metadata deleted
    // init with account:false, metadata:false, data:true  -> Data deleted
    // init with account:false, metadata:false, data:false -> Everything fine
    //
    // init with account:true,  metadata:true,  data:some  -> Account deleted, metadata deleted, data deleted (I)
    // init with account:true,  metadata:old,   data:true  -> Account deleted, metadata deleted, data deleted (I/age)

    // public PbapPhonebookMetadata getCachedPhonebookMetadata(Account account, String phonebook)
    // public boolean setCachedPhonebookMetadata(Account account, String phonebook, PbapPhonebookMetadata metadata)
    // public List<String> getCachedPhonebooks(Account account)
    // public List<File> getCachedMetadataFiles()
    // public void setContactsHidden(Account account, boolean show)

    //********************************************************************************************//
    // Debug/Dump/toString()
    //********************************************************************************************//

    @Test
    public void testToString() {
        String str = mStorage.toString();
        assertThat(str).isNotNull();
        assertThat(str.length()).isNotEqualTo(0);
    }

    @Test
    public void testDump() {
        String dumpContents = mStorage.dump();
        assertThat(dumpContents).isNotNull();
        assertThat(dumpContents.length()).isNotEqualTo(0);
    }

    //********************************************************************************************//
    // Testing Utilities
    //********************************************************************************************//

    private void startStorage(List<Account> existingAccounts /*, List<PbapPhonebookMetadata> cachedMetadata */) {
        mMockedAccounts.addAll(existingAccounts);
        mStorage.start();
        verify(mMockAccountManager).start();
        mAccountManagerCallback.onAccountsChanged(null, existingAccounts);
        verify(mMockStorageCallback, times(1)).onStorageReady();
        assertThat(mStorage.isStorageReady()).isTrue();
    }

    private Account getAccountForDevice(BluetoothDevice device) {
        return new Account(device.getAddress(), ACCOUNT_TYPE);
    }

    private List<VCardEntry> getMockContacts(Account account, String phonebook, int numContacts) {
        List<VCardEntry> contacts = new ArrayList<VCardEntry>();
        for (int i = 0; i < numContacts; i++) {
            VCardEntry card = new VCardEntry(VCardConfig.VCARD_TYPE_V21_GENERIC, account);
            VCardProperty property = new VCardProperty();
            property.setName(VCardConstants.PROPERTY_TEL);
            property.addValues(String.valueOf(i));
            card.addProperty(property);
            contacts.add(card);
        }

        return contacts;
    }
}
