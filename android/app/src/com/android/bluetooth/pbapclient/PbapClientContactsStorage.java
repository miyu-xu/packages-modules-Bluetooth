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

package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.bluetooth.BluetoothDevice;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.util.Log;
import android.util.Pair;

import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.Utils;

import com.android.internal.annotations.VisibleForTesting;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.PhoneData;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class owns the interface to the contacts and call history storage mechanism, namely the
 * Contacts DB and Contacts Provider. It also owns the list of cached metadata and facilitates the
 * management of the AccountManagerService accounts that are required to store contacts on the
 * device. It provides functions to allow connected devices to create and manage accounts and store
 * and cache contacts and call logs.
 *
 * Exactly one of these objects should exist, created by the PbapClientService at start up.
 *
 * All contacts on Android are stored against an AccountManager Framework Account object. These
 * Accounts should be created by devices upon connecting. This Account is used on many of the
 * functions, in order to target the correct device's contacts.
 */
class PbapClientContactsStorage {
    private static final String TAG = PbapClientContactsStorage.class.getSimpleName();

    private static final String MIMETYPE_PBAP_PHONEBOOK =
            "vnd.android.cursor.item/vnd.com.android.bluetooth.phonebook";

    private static final int CONTACTS_INSERT_BATCH_SIZE = 250;

    private static final String CALL_LOG_TIMESTAMP_PROPERTY = "X-IRMC-CALL-DATETIME";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd'T'HHmmss";

    private final Context mContext;
    private final PbapClientAccountManager mAccountManager;

    private volatile boolean mStorageInitialized = false;

    private final List<Callback> mCallbacks = new ArrayList<Callback>();

    /**
     * TBD
     */
    interface Callback {
        /**
         * Invoked when storage is initialized and ready for interaction
         *
         * Storage realted functions may not work before storage is ready.
         */
        void onStorageReady();

        /**
         * Receive account visibility updates
         *
         * @param oldAccounts The list of previously available accounts
         * @param newAccounts The list of newly available accounts
         */
        void onStorageAccountsChanged(List<Account> oldAccounts, List<Account> newAccounts);
    }

    class PbapClientAccountManagerCallback implements PbapClientAccountManager.Callback {
        @Override
        public void onAccountsChanged(List<Account> oldAccounts, List<Account> newAccounts) {
            if (oldAccounts == null) {
                Log.d(TAG, "Storage accounts initialized, accounts=" + newAccounts);
                initialize(newAccounts);
                notifyStorageReady();
                notifyStorageAccountsChanged(Collections.emptyList(), mAccountManager.getAccounts());
            } else if (mStorageInitialized) {
                Log.d(TAG, "Storage accounts changed, old=" + oldAccounts + ", new=" + newAccounts);
                notifyStorageAccountsChanged(oldAccounts, newAccounts);
            } else {
                Log.d(TAG, "Storage not fully initialized, dropping accounts changed event");
            }
        }
    }
    private PbapClientAccountManagerCallback mAccountManagerCallback = new PbapClientAccountManagerCallback();

    public PbapClientContactsStorage(Context context) {
        mContext = context;
        mAccountManager = new PbapClientAccountManager(context, mAccountManagerCallback);
    }

    @VisibleForTesting
    PbapClientContactsStorage(Context context, PbapClientAccountManager accountManager) {
        mContext = context;
        mAccountManager = accountManager;
    }

    public void start() {
        mStorageInitialized = false;
        mAccountManager.start();
    }

    public void stop() {
        mAccountManager.stop();
    }

    //--------------------------------------------------------------------------------------------//
    // Initialization                                                                             //
    //--------------------------------------------------------------------------------------------//

    /**
     * Determine is storage is ready or not.
     *
     * Many storage functions won't work before storage is ready to be interacted with. Use the
     * callback interface to be told when storage is ready if its not ready upon calling this.
     *
     * @return True is storage is ready, false otherwise.
     */
    public boolean isStorageReady() {
        return mStorageInitialized;
    }

    /**
     * Initialize storage with a set of accounts.
     *
     * This function receives a set of accounts that our PBAP Client implementation knows about and
     * initializes our storage state based on this account list, using the following rules/steps:
     *
     * 1. CHECK ACCOUNTS: If an account exists and,
     *    a. Has cached metadata associated with it -> leave account and contacts
     *    b. Does not have cached metadata associated with it -> delete any contacts, then account
     * 2. CHECK METADATA: If a metadata exists and,
     *    a. Has an associated account -> check metadata against device state, meaning
     *       1. Does the metadata's incidated size match the number of stored contacts
     *    b. Does not have an associated account -> delete the cached metadata, then any contacts
     *
     * These rules help ensure that we (1) clean up accounts that might persist after an ungraceful
     * shutdown, and (2) clean up metadata that might exist after an ungrateful shutdown, so that we
     * can be sure the accounts we're left with after initialization are only those that have valid
     * cached metadata associated with them, and vice versa.
     *
     * @param accounts The list of accounts we're starting with
     */
    private void initialize(List<Account> accounts) {
        Log.i(TAG, "initialize(accounts=" + accounts + ")");
        if (mStorageInitialized) {
            Log.w(TAG, "intialize(accounts=" + accounts + "): Already initialized. Skipping");
            return;
        }

        // TODO: When caching, remove accounts that don't have metadata, remove metadata
        // that doesn't have an account

        for (Account account : accounts) {
            if (getCachedPhonebooks(account).size() != 0) {
                Log.i(TAG, "intialize(): Found metadata for account=" + account + ", do not delete account.");
                continue;
            }

            Log.w(TAG, "initialize(): Remove pre-existing account=" + account);
            mAccountManager.removeAccount(account);
        }

        for (File file : getCachedMetadataFiles()) {
            String path = file.getName();

            // TODO: Add file read/write to PbapPhonebookMetadata object so this is easy
            String[] tokens = path.split("-");
            if (tokens.length != 3) {
                continue;
            }

            String accountName = tokens[0].replace("_", ":");
            Account account = getStorageAccountForDevice(AdapterService.getAdapterService().getDeviceFromByte(Utils.getBytesFromAddress(accountName)));

            List<String> cachedPhonebooks = getCachedPhonebooks(account);
            if (cachedPhonebooks.size() > 0 && !mAccountManager.getAccounts().contains(account)) {
                Log.w(TAG, "Metadata found without a matching storage account. Deleting metadata=" + path);
                file.delete();
            }
        }

        mStorageInitialized = true;
    }

    //--------------------------------------------------------------------------------------------//
    // Storage Accounts                                                                           //
    //--------------------------------------------------------------------------------------------//

    public Account getStorageAccountForDevice(BluetoothDevice device) {
        return mAccountManager.getAccountForDevice(device);
    }

    public List<Account> getStorageAccounts() {
        return mAccountManager.getAccounts();
    }

    public boolean addAccount(Account account) {
        return mAccountManager.addAccount(account);
    }

    public boolean removeAccount(Account account) {
        return mAccountManager.removeAccount(account);
    }

    //--------------------------------------------------------------------------------------------//
    // Metadata (Contacts Caching)                                                                //
    //--------------------------------------------------------------------------------------------//

    /**
     * Gets the file path that a metadata file should be stored at for a given account and phonebook
     *
     * The path name is in the following format:
     * /data/user/<account>-<phonebook>-metadata.xml
     *
     * @param account the device account this phonebook and metadata belong to
     * @param phonebook the phonebook name, based on the PBAP phonebook name constants in the spec
     * @return a String representing the filename/path for a metadata file for this account and
     *         phonebook
     */
    private String getPathForAccountPhonebook(Account account, String phonebook) {
        if (account == null || phonebook == null || account.name == null) {
            return null;
        }
        // TODO: PBAP Client only directory?
        return account.name.replace(":", "_") + "-" + phonebook.replace("/", "_").replace(".", "_") + "-metadata.xml";
    }

    /**
     * Determine if we have cached metadata for a given phonebook
     *
     * This metadata is stored at a file in the files directory of Bluetooth, typically:
     * /data/user/<account>-<phonebook>-metadata.xml
     *
     * i.e.:
     * /data/user/aa_bb_cc_dd_ee_ff-telecom_fav-metadata.xml
     *
     * @param account the device account this phonebook and metadata belong to
     * @param phonebook the phonebook name, based on the PBAP phonebook name constants in the spec
     * @return a PbapPhonebookMetadata object if we have cached contacts for the give phonebook,
     *         null if we do not
     */
    public PbapPhonebookMetadata getCachedPhonebookMetadata(Account account, String phonebook) {
        Log.i(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ")");
        String fileName = getPathForAccountPhonebook(account, phonebook);
        if (fileName == null) {
            Log.w(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + "):  Failed to resolve to path");
            return null;
        }

        try (FileInputStream inputStream = mContext.openFileInput(fileName); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String databaseIdentifier = null;
            String primaryVersionCounter = null;
            String secondaryVersionCounter = null;
            int size = -1;

            String line = null;
            String field = null;
            String value = null;
            while ((line = reader.readLine()) != null) {
                Log.v(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + "): Read line=" + line);
                if (line == null || line.isEmpty()) {
                    continue;
                }

                String[] tokens = line.split("=");
                if (tokens.length != 2) {
                    continue;
                }

                field = tokens[0];
                value = tokens[1];
                Log.v(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + "): Read " + field + "=" + value);

                if (field == null || value == null) {
                    continue;
                }

                switch (field) {
                    case "phonebook":
                        if (!phonebook.equals(value)) {
                            return null;
                        }
                        break;
                    case "databaseIdentifier":
                        databaseIdentifier = value;
                        break;
                    case "primaryVersionCounter":
                        primaryVersionCounter = value;
                        break;
                    case "secondaryVersionCounter":
                        secondaryVersionCounter = value;
                        break;
                    case "size":
                        size = Integer.parseInt(value);
                        break;
                    default:
                        Log.w(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + "): Unrecognized field=" + field);
                        break;
                }
            }

            if (size <= 0 || databaseIdentifier == null || primaryVersionCounter == null || secondaryVersionCounter == null) {
                Log.w(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + "): File malformed, fields missing or incorrect");
                // TODO: Delete file too?
                return null;
            }

            PbapPhonebookMetadata metadata = new PbapPhonebookMetadata(phonebook, size, databaseIdentifier, primaryVersionCounter, secondaryVersionCounter);
            Log.i(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ") -> " + metadata);
            return metadata;
        } catch (NullPointerException | IOException e ) {
            Log.w(TAG, "getCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + "): No valid metadata");
        }
        return null;
    }

    /**
     * Set the cached metadata for a given phonebook. This indicates we should persist this
     * phonebook for this user across adapter lifecycles.
     *
     * @param account the device account this phonebook and metadata belong to
     * @param phonebook the phonebook name, based on the PBAP phonebook name constants in the spec
     * @param metadata The metadata to associate with the given phonebook
     */
    public boolean setCachedPhonebookMetadata(Account account, String phonebook, PbapPhonebookMetadata metadata) {
        Log.i(TAG, "setCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ", metadata=" + metadata + ")");
        String fileName = getPathForAccountPhonebook(account, phonebook);
        if (fileName == null) {
            Log.w(TAG, "setCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ", metadata=" + metadata + "):  Failed to resolve to path");
            return false;
        }

        if (metadata == null) {
            Log.d(TAG, "setCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ", metadata=" + metadata + "): Delete file=" + fileName);
            return deleteCachedPhonebookMetadata(fileName);
        }

        Log.d(TAG, "setCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ", metadata=" + metadata + "): Write to file=" + fileName);
        try (FileOutputStream outputStream = mContext.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("phonebook=").append(phonebook);
            sb.append("\ndatabaseIdentifier=").append(metadata.getDatabaseIdentifier());
            sb.append("\nprimaryVersionCounter=").append(metadata.getPrimaryVersionCounter());
            sb.append("\nsecondaryVersionCounter=").append(metadata.getSecondaryVersionCounter());
            sb.append("\nsize=").append(metadata.getSize());
            byte[] byteBuff = sb.toString().getBytes();
            outputStream.write(byteBuff, 0, byteBuff.length);
        } catch (IOException e) {
            Log.w(TAG, "setCachedPhonebookMetadata(account=" + account + ", phonebook=" + phonebook + ", metadata=" + metadata + "): Failed to write metadata");
            return false;
        }
        return true;
    }

    /**
     * Delete a cached phonebook metadata file
     *
     * @param fileName the filename inside of our applications files directory to delete
     */
    private boolean deleteCachedPhonebookMetadata(String fileName) {
        Log.d(TAG, "deleteCachedPhonebookMetadata(fileName=" + fileName + "): Delete file");
        File file = new File(mContext.getFilesDir(), fileName);

        if (!file.exists()) {
            return true;
        }

        try {
            file.delete();
            Log.v(TAG, "deleteCachedPhonebookMetadata(fileName=" + fileName + "): Deleted file");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "deleteCachedPhonebookMetadata(fileName=" + fileName + "): Falied to delete file", e);
            return false;
        }
    }

    /**
     * Get the list of cached phonebooks for a given account
     */
    public List<String> getCachedPhonebooks(Account account) {
        List<String> cachedPhonebooks = new ArrayList<String>();
        PbapPhonebookMetadata local =
                getCachedPhonebookMetadata(account, PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        if (local != null) {
            cachedPhonebooks.add(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        }

        PbapPhonebookMetadata fav =
                getCachedPhonebookMetadata(account, PbapPhonebook.FAVORITES_PATH);
        if (fav != null) {
            cachedPhonebooks.add(PbapPhonebook.FAVORITES_PATH);
        }

        PbapPhonebookMetadata sim =
                getCachedPhonebookMetadata(account, PbapPhonebook.SIM_PHONEBOOK_PATH);
        if (sim != null) {
            cachedPhonebooks.add(PbapPhonebook.SIM_PHONEBOOK_PATH);
        }

        Log.i(TAG, "getCachedPhonebooks(account=" + account + ") -> " + cachedPhonebooks);
        return cachedPhonebooks;
    }

    public List<File> getCachedMetadataFiles() {
        return Arrays.asList(mContext.getFilesDir().listFiles());
    }

    //--------------------------------------------------------------------------------------------//
    // Contacts DB Operations                                                                     //
    //--------------------------------------------------------------------------------------------//

    /**
     * Insert contacts into the Contacts DB from a remote device's favorites phonebook
     */
    public boolean insertFavorites(Account account, List<VCardEntry> contacts) {
        if (contacts == null) {
            return false;
        }

        for (VCardEntry contact : contacts) {
            contact.setStarred(true);
        }
        return insertContacts(account, PbapPhonebook.FAVORITES_PATH, contacts);
    }

    /**
     * Insert contacts into the Contacts DB from a remote device's local phonebook
     */
    public boolean insertLocalContacts(Account account, List<VCardEntry> contacts) {
        return insertContacts(account, PbapPhonebook.LOCAL_PHONEBOOK_PATH, contacts);
    }

    /**
     * Insert contacts into the Contacts DB from a remote device's sim local phonebook
     */
    public boolean insertSimContacts(Account account, List<VCardEntry> contacts) {
        return insertContacts(account, PbapPhonebook.SIM_PHONEBOOK_PATH, contacts);
    }

    /**
     * Insert a list of contacts into the Contacts Provider/Contacts DB
     *
     * This function also associates the phonebook metadata with the contact for easy per-phonebook
     * cleanup operations.
     */
    private boolean insertContacts(Account account, String phonebook, List<VCardEntry> contacts) {
        if (!mStorageInitialized) {
            Log.w(TAG, "insertContacts: Failed, storage not ready");
            return false;
        }

        if (account == null) {
            Log.e(TAG, "insertContacts: account is null");
            return false;
        }

        if (contacts == null || contacts.size() == 0) {
            Log.e(TAG, "insertContacts: contacts provided are null or empty");
            return false;
        }

        try {
            Log.i(TAG, "insertContacts: inserting contacts, account=" + account + ", count="
                    + contacts.size() + ", taggedAs=" + phonebook);

            ContentResolver contactsProvider = mContext.getContentResolver();
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            ContentProviderResult[] results = null;

            // Group insert operations together to minimize inter process communication and improve
            // processing time.
            for (VCardEntry contact : contacts) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.e(TAG, "Interrupted during insert");
                    break;
                }

                // Append current vcard to list of insert operations.
                int numberOfOperations = operations.size();
                constructInsertOperationsForContact(contact, phonebook, operations, contactsProvider);

                if (operations.size() >= CONTACTS_INSERT_BATCH_SIZE) {
                    Log.i(TAG, "insertContacts: batch full, operations.size()=" + operations.size() + ", batch_size=" + CONTACTS_INSERT_BATCH_SIZE);

                    // If we have exceded the limit to the insert operation remove the latest vcard
                    // and submit.
                    operations.subList(numberOfOperations, operations.size()).clear();

                    results = contactsProvider.applyBatch(ContactsContract.AUTHORITY, operations);
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "insertContacts: batch results=");
                        for (int j = 0; j < results.length; j++) {
                            Log.v(TAG, "    results[" + j + "] = " + results[j]);
                        }
                    }

                    // Re-add the current contact operation(s) to the list
                    operations = constructInsertOperationsForContact(contact, phonebook, null, contactsProvider);

                    Log.i(TAG, "insertContacts: batch complete, operations.size()=" + operations.size());
                }
            }

            // Apply any unsubmitted vcards
            if (operations.size() > 0) {
                results = contactsProvider.applyBatch(ContactsContract.AUTHORITY, operations);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "insertContacts: batch results=");
                    for (int k = 0; k < results.length; k++) {
                        Log.v(TAG, "    results[" + k + "] = " + results[k]);
                    }
                }
                operations.clear();
            }
            Log.i(TAG, "insertContacts: insert complete, count=" + contacts.size());
        } catch (OperationApplicationException | RemoteException | NumberFormatException e) {
            Log.e(TAG, "insertContacts: Exception occurred while processing phonebook pull: ", e);
            return false;
        }
        return true;
    }

    private ArrayList<ContentProviderOperation> constructInsertOperationsForContact(
                VCardEntry contact, String phonebook,
                ArrayList<ContentProviderOperation> operations, ContentResolver contactsProvider) {
        int numberOfOperations = operations == null ? 0 : operations.size();
        operations = contact.constructInsertOperations(contactsProvider, operations);

        // Add Custom PBAP metadata to contact, if one was added
        if (numberOfOperations != operations.size()) {
            operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, /* backRefIndex= */ numberOfOperations)
                .withValue(Data.MIMETYPE, MIMETYPE_PBAP_PHONEBOOK)
                .withValue(Data.DATA1, phonebook)
                .build());

            operations.add(ContentProviderOperation.newUpdate(RawContacts.CONTENT_URI)
                    .withSelection(RawContacts._ID + "=?", new String[1])
                    .withSelectionBackReference(0, numberOfOperations)
                    .withValue(RawContacts.SYNC1, phonebook)
                    .build());

            // (TODO: Remove) Debug insert operations
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "insertContacts: contact=" + contact);
                for (int i = numberOfOperations; i < operations.size(); i++) {
                    Log.v(TAG, "    operation[" + i + "] = " + operations.get(i));
                }
            }
        }

        return operations;
    }

    public boolean removeFavorites(Account account) {
        return removeContacts(account, PbapPhonebook.FAVORITES_PATH);
    }

    public boolean removeLocalContacts(Account account) {
        return removeContacts(account, PbapPhonebook.LOCAL_PHONEBOOK_PATH);
    }

    public boolean removeSimContacts(Account account) {
        return removeContacts(account, PbapPhonebook.SIM_PHONEBOOK_PATH);
    }

    public boolean removeAllContacts(Account account) {
        if (account == null) {
            Log.e(TAG, "removeAllContacts: account is null");
            return false;
        }

        Log.i(TAG, "removeAllContacts: requested for account=" + account);
        Uri contactsToDeleteUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
                .build();

        try {
            mContext.getContentResolver().delete(contactsToDeleteUri, null);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "removeAllContacts(uri=" + contactsToDeleteUri + "): Contacts could not be deleted", e);
            return false;
        }
        return true;
    }

    private boolean removeContacts(Account account, String phonebook) {
        if (account == null) {
            Log.e(TAG, "removeContacts: account is null");
            return false;
        }

        Log.i(TAG, "removeContacts: requested for account=" + account + ", phonebook=" + phonebook);

        try {
            int numRowsDeleted = mContext.getContentResolver().delete(RawContacts.CONTENT_URI,
                    ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_NAME + " = ? AND " + ContactsContract.RawContacts.SYNC1 + "= ?",
                    new String[]{account.type, account.name, phonebook});
            Log.d(TAG, "removeContacts(account=" + account + ", phonebook=" + phonebook + "): Deleted " + numRowsDeleted + " entries");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "removeContacts(account=" + account + ", phonebook=" + phonebook + "): Contacts could not be deleted", e);
            return false;
        }
        return true;
    }

    /**
     * Insert call logs into the incoming calls table
     *
     * @param account The account to insert call logs against
     * @param history The call history to insert
     */
    public boolean insertIncomingCallHistory(Account account, List<VCardEntry> history) {
        return insertCallHistory(account, CallLog.Calls.INCOMING_TYPE, history);
    }

    /**
     * Insert call logs into the outgoing calls table
     *
     * @param account The account to insert call logs against
     * @param history The call history to insert
     */
    public boolean insertOutgoingCallHistory(Account account, List<VCardEntry> history) {
        return insertCallHistory(account, CallLog.Calls.OUTGOING_TYPE, history);
    }

    /**
     * Insert call logs into the missed calls table
     *
     * @param account The account to insert call logs against
     * @param history The call history to insert
     */
    public boolean insertMissedCallHistory(Account account, List<VCardEntry> history) {
        return insertCallHistory(account, CallLog.Calls.MISSED_TYPE, history);
    }

    /**
     * Insert call history entries of a given type
     *
     * @param account The account to insert call logs against
     * @param type The type of call history provided
     * @param history The call history to insert
     */
    private boolean insertCallHistory(Account account, int type, List<VCardEntry> history) {
        if (!mStorageInitialized) {
            Log.w(TAG, "insertCallHistory: Failed, storage not ready");
            return false;
        }

        if (account == null) {
            Log.e(TAG, "insertCallHistory: Account is null");
            return false;
        }

        if (history == null || history.size() == 0) {
            Log.e(TAG, "insertCallHistory: No entries to insert");
            return false;
        }

        if (type != CallLog.Calls.INCOMING_TYPE && type != CallLog.Calls.OUTGOING_TYPE
                && type != CallLog.Calls.MISSED_TYPE) {
            Log.e(TAG, "insertCallHistory: Unknown type=" + type);
            return false;
        }

        try {
            Log.i(TAG, "insertCallHistory: Inserting call history, type=" + type + ", count="
                    + history.size());

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            for (VCardEntry vcard : history) {
                ContentValues values = new ContentValues();
                values.put(CallLog.Calls.TYPE, type);
                values.put(Calls.PHONE_ACCOUNT_ID, account.name);

                List<PhoneData> phones = vcard.getPhoneList();
                if (phones == null || phones.get(0).getNumber().equals(";")
                        || phones.get(0).getNumber().length() == 0) {
                    values.put(CallLog.Calls.NUMBER, "");
                } else {
                    String phoneNumber = phones.get(0).getNumber();
                    values.put(CallLog.Calls.NUMBER, phoneNumber);
                }

                List<Pair<String, String>> irmc = vcard.getUnknownXData();
                SimpleDateFormat parser = new SimpleDateFormat(TIMESTAMP_FORMAT);
                if (irmc != null) {
                    for (Pair<String, String> pair : irmc) {
                        if (pair.first.startsWith(CALL_LOG_TIMESTAMP_PROPERTY)) {
                            try {
                                values.put(CallLog.Calls.DATE, parser.parse(pair.second).getTime());
                            } catch (ParseException e) {
                                Log.d(TAG, "Failed to parse date, value=" + pair.second);
                            }
                        }
                    }
                }

                ops.add(ContentProviderOperation.newInsert(CallLog.Calls.CONTENT_URI)
                        .withValues(values)
                        .withYieldAllowed(true)
                        .build());
            }

            mContext.getContentResolver().applyBatch(CallLog.AUTHORITY, ops);
            Log.d(TAG, "Inserted call logs, type=" + type);
        } catch (RemoteException | OperationApplicationException e) {
            Log.w(TAG, "Failed to insert call log, type=" + type, e);
            return false;
        } finally {
            synchronized (this) {
                this.notify();
            }
        }
        return true;
    }

    /**
     * Remove all call history associated with this client's account
     *
     * @param account The account to remove call history on behalf of
     */
    public boolean removeCallHistory(Account account) {
        if (account == null) {
            Log.e(TAG, "removeCallHistory: account is null");
            return false;
        }

        Log.i(TAG, "removeCallHistory: requested for account=" + account);
        try {
            mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI,
                    CallLog.Calls.PHONE_ACCOUNT_ID + "=?", new String[]{account.name});
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Call Logs could not be deleted, they may not exist yet.", e);
            return false;
        }
        return true;
    }

    public void setContactsHidden(Account account, boolean show) {
        Log.d(TAG, "setContactsHidden(account=" + account.name + ", show=" + show + ")");

        Uri accountUri = ContactsContract.Settings.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.Settings.ACCOUNT_TYPE, account.type)
                .appendQueryParameter(ContactsContract.Settings.ACCOUNT_NAME, account.name)
                .build();

        ContentValues values = new ContentValues();
        values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, (show ? 1 : 0));

        int rowsUpdated = mContext.getContentResolver().update(accountUri, values, null, null);
        if (rowsUpdated > 0) {
            Log.d(TAG, "setContactsHidden(account=" + account.name + ", show=" + show + "): Succeeded, ungrouped_visible=" + (show ? 1 : 0));
        } else {
            Log.d(TAG, "setContactsHidden(account=" + account.name + ", show=" + show + "): Failed to update any rows");
        }
    }

    //--------------------------------------------------------------------------------------------//
    // Callbacks                                                                                  //
    //--------------------------------------------------------------------------------------------//

    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * Notify all client callbacks that the set of storage accounts has changed
     */
    private void notifyStorageReady() {
        Log.d(TAG, "notifyStorageReady");
        for (Callback callback : mCallbacks) {
            callback.onStorageReady();
        }
    }

    /**
     * Notify all client callbacks that the set of storage accounts has changed
     */
    private void notifyStorageAccountsChanged(List<Account> oldAccounts, List<Account> newAccounts) {
        Log.d(TAG, "notifyAccountsChanged, old=" + oldAccounts + ", new=" + newAccounts);
        for (Callback callback : mCallbacks) {
            callback.onStorageAccountsChanged(oldAccounts, newAccounts);
        }
    }

    //--------------------------------------------------------------------------------------------//
    // Debug and Dump Output                                                                      //
    //--------------------------------------------------------------------------------------------//

    // TODO: Delete this
    private String printPbapContacts(Account account, String phonebook) {
        StringBuilder sb = new StringBuilder();

        // Query the Contacts Provider Data table for raw contact ids that below to a given account
        // type and name, where theres our custom phonebook metadata matching the value for the
        // given phonebook.
        //
        // Note that the account type and name columns are implicitly joined. See the "view_data"
        // table in the contacts2.db file for the current user for exact data
        List<Long> rawContactIds = new ArrayList<>();
        try (Cursor cursor = mContext.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Data.RAW_CONTACT_ID },
                ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_NAME + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.Data.DATA1 + " = ?",
                new String[] { account.type, account.name, "vnd.android.cursor.item/vnd.com.android.bluetooth.phonebook", phonebook },
                null)) {

            // process
            if (cursor.moveToFirst()) {
                int rawContactIdIndex = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
                do {
                    long rawContactId = cursor.getLong(rawContactIdIndex);
                    rawContactIds.add(rawContactId);
                } while (cursor.moveToNext());
            }
        }

        sb.append("            ").append(phonebook).append(" (").append(rawContactIds.size()).append(" contacts)\n");
        // for (int i = 0; i < rawContactIds.size(); i++) {
        //     sb.append("                id:").append(rawContactIds.get(i)).append("\n");
        // }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "<" + TAG + " ready=" + isStorageReady() + ">";
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append(TAG + ":\n");
        sb.append("    Storage Ready: ").append(mStorageInitialized).append("\n\n");
        sb.append("    ").append(mAccountManager.dump()).append("\n");

        sb.append("    Cached Metadata:\n");

        // for (Account account : mAccountManager.getAccounts()) {
        //     sb.append("        " + account.name + "\n");
        //     List<String> phonebooks = getCachedPhonebooks(account);
        //     for (String phonebook : phonebooks) {
        //         PbapPhonebookMetadata metadata = getCachedPhonebookMetadata(account, phonebook);
        //         String path = getPathForAccountPhonebook(account, phonebook);
        //         sb.append("            " + path + " => " + metadata + "\n");
        //     }
        // }

        for (File file : getCachedMetadataFiles()) {
            sb.append("        ").append(file.getAbsolutePath()).append("\n");
        }

        sb.append("\n    Database:\n");
        for (Account account : mAccountManager.getAccounts()) {
            sb.append("        Account ").append(account.name).append(":\n");
            sb.append(printPbapContacts(account, PbapPhonebook.LOCAL_PHONEBOOK_PATH));
            sb.append(printPbapContacts(account, PbapPhonebook.SIM_PHONEBOOK_PATH));
            sb.append(printPbapContacts(account, PbapPhonebook.FAVORITES_PATH));
        }

        return sb.toString();
    }
}
