/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import android.accounts.Account;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;

import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.R;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.obex.ResponseCodes;
import com.android.vcard.VCardEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This object represents a connection over PBAP with a given remote device. It manages the account,
 * SDP Record and PBAP OBEX Client for the remote device. It also uses the OBEX client to make
 * simple requests, driving the overall contact download process.
 */
class PbapClientStateMachine extends StateMachine {
    private static final String TAG = PbapClientStateMachine.class.getSimpleName();

    // Messages for handling connect/disconnect requests.
    private static final int MSG_CONNECT = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_SDP_COMPLETE = 3;
    private static final int MSG_OBEX_CLIENT_CONNECTED = 4;
    private static final int MSG_OBEX_CLIENT_DISCONNECTED = 5;
    private static final int MSG_STORAGE_READY = 6;
    private static final int MSG_ACCOUNT_ADDED = 7;
    private static final int MSG_ACCOUNT_REMOVED = 8;
    private static final int MSG_DOWNLOAD = 9;
    private static final int MSG_PHONEBOOK_METADATA_RECEIVED = 10;
    private static final int MSG_PHONEBOOK_CONTACTS_RECEIVED = 11;

    // Messages for handling error conditions.
    public static final int MSG_CONNECT_TIMEOUT = 12;
    public static final int MSG_DISCONNECT_TIMEOUT = 13;

    // Configurable Timeouts
    @VisibleForTesting static final int CONNECT_TIMEOUT_MS = 12000;
    @VisibleForTesting static final int DISCONNECT_TIMEOUT_MS = 3000;

    // Supported features of our OBEX client
    private static final int LOCAL_SUPPORTED_FEATURES =
             PbapSdpRecord.FEATURE_DOWNLOADING | PbapSdpRecord.FEATURE_DATABASE_IDENTIFIER
             | PbapSdpRecord.FEATURE_FOLDER_VERSION_COUNTERS
             | PbapSdpRecord.FEATURE_DEFAULT_IMAGE_FORMAT;

    // Default configuration for VCard format -> prefer 3.0 to 2.1
    private static final byte DEFAULT_VCARD_VERSION = PbapPhonebook.FORMAT_VCARD_30;

    // Default property filter for downloaded contacts
    private static final long DEFAULT_PROPERTIES = PbapApplicationParameters.PROPERTY_VERSION
            | PbapApplicationParameters.PROPERTY_FN | PbapApplicationParameters.PROPERTY_N
            | PbapApplicationParameters.PROPERTY_PHOTO | PbapApplicationParameters.PROPERTY_ADR
            | PbapApplicationParameters.PROPERTY_TEL | PbapApplicationParameters.PROPERTY_EMAIL
            | PbapApplicationParameters.PROPERTY_NICKNAME;

    // Our internal batch size when downloading contacts. Batches let us deliver contacts sooner to
    // the UI and applications that want contacts, and make our individual download operations
    // shorter running, but come with the trade off of a greater overall time to download.
    private static final int CONTACT_DOWNLOAD_BATCH_SIZE = 250;

    /**
     * A Callback interface for object creators to get events related to this state machine
     */
    interface Callback {
        /**
         * Receive connection state changes for this state machines so you can know when to clean it
         * up.
         *
         * @param oldState The old state of the device state machine
         * @param newState The new state of the device state machine
         */
        void onConnectionStateChanged(int oldState, int newState);
    }

    /**
     * Internal Phonebook object to help drive downloads with batching and record download process
     * metrics.
     */
    private static final class Phonebook {
        private final String mName;
        private PbapPhonebookMetadata mMetadata;
        private int mNumDownloaded;
        private boolean mUsingCached;

        Phonebook(String name) {
            mName = name;
            mMetadata = null;
            mNumDownloaded = 0;
            mUsingCached = false;
        }

        public PbapPhonebookMetadata getMetadata() {
            return mMetadata;
        }

        public void setMetadata(PbapPhonebookMetadata metadata) {
            mMetadata = metadata;
        }

        public void onContactsDownloaded(int numDownloaded) {
            mNumDownloaded += numDownloaded;
        }

        public void setUsingCached(boolean usingCached) {
            mUsingCached = usingCached;
        }

        public boolean getUsingCached() {
            return mUsingCached;
        }

        public String getName() {
            return mName;
        }

        public int getTotalNumberOfContacts() {
            return (mMetadata == null || mMetadata.getSize() == PbapPhonebookMetadata.INVALID_SIZE)
                    ? 0
                    : mMetadata.getSize();
        }

        public int getNumberOfContactsDownloaded() {
            if (mUsingCached) {
                return getTotalNumberOfContacts();
            }
            return mNumDownloaded;
        }

        @Override
        public String toString() {
            if (mMetadata == null) {
                return mName + " [" + getNumberOfContactsDownloaded()
                        + "/ UNKNOWN] (db:UNKNOWN, pc:UNKNOWN, sc:UNKNOWN)";
            }

            String databaseIdentifier = mMetadata.getDatabaseIdentifier();
            if (databaseIdentifier == PbapPhonebookMetadata.INVALID_DATABASE_IDENTIFIER) {
                databaseIdentifier = "UNKNOWN";
            }

            String primaryVersionCounter = mMetadata.getPrimaryVersionCounter();
            if (primaryVersionCounter == PbapPhonebookMetadata.INVALID_VERSION_COUNTER) {
                primaryVersionCounter = "UNKNOWN";
            }

            String secondaryVersionCounter = mMetadata.getSecondaryVersionCounter();
            if (secondaryVersionCounter == PbapPhonebookMetadata.INVALID_VERSION_COUNTER) {
                secondaryVersionCounter = "UNKNOWN";
            }

            String totalContactsExpected = "UNKNOWN";
            if (mMetadata.getSize() != PbapPhonebookMetadata.INVALID_SIZE) {
                totalContactsExpected = Integer.toString(mMetadata.getSize());
            }

            return mName + " [" + (mUsingCached ? "CACHED" : getNumberOfContactsDownloaded())
                    + "/" + totalContactsExpected + "] (db:" + databaseIdentifier + ", pc:"
                    + primaryVersionCounter + ", sc:" + secondaryVersionCounter + ")";
        }
    }

    private final BluetoothDevice mDevice;
    private final Context mContext;
    private PbapSdpRecord mSdpRecord = null;
    private final Account mAccount;
    private Map<String, Phonebook> mPhonebooks = new HashMap<String, Phonebook>();
    private final PbapClientObexClient mObexClient;
    private final PbapClientContactsStorage mContactsStorage;

    private final PbapClientContactsStorage.Callback mStorageCallback = new PbapClientContactsStorage.Callback() {
        @Override
        public void onStorageReady() {
            onPbapClientStorageReady();
        }

        @Override
        public void onStorageAccountsChanged(List<Account> oldAccounts, List<Account> newAccounts) {
            boolean inOld = oldAccounts.contains(mAccount);
            boolean inNew = newAccounts.contains(mAccount);
            if (!inOld && inNew) {
                Log.i(TAG, "Storage accounts changed, account added");
                onPbapClientAccountAdded();
            } else if (inOld && !inNew) {
                Log.i(TAG, "Storage accounts changed, account removed");
                onPbapClientAccountRemoved();
            } else {
                Log.i(TAG, "Storage accounts changed, but no impact to our account");
            }
        }
    };

    private int mCurrentState = BluetoothProfile.STATE_DISCONNECTED;
    private State mDisconnected;
    private State mConnecting;
    private State mConnected;
    private State mDownloading;
    private State mDisconnecting;

    private final Callback mCallback;

    PbapClientStateMachine(BluetoothDevice device, PbapClientContactsStorage storage, Context context, Callback callback) {
        super(TAG);

        mDevice = device;
        mContext = context;
        mContactsStorage = storage;
        mCallback = callback;
        mAccount = mContactsStorage.getStorageAccountForDevice(mDevice);
        mObexClient = new PbapClientObexClient(device, LOCAL_SUPPORTED_FEATURES, new PbapClientObexClientCallback());

        initializeStates();
    }

    @VisibleForTesting
    PbapClientStateMachine(BluetoothDevice device, PbapClientContactsStorage storage, Context context, Looper looper, Callback callback, PbapClientObexClient obexClient) {
        super(TAG, looper);

        mDevice = device;
        mContext = context;
        mContactsStorage = storage;
        mCallback = callback;
        mAccount = mContactsStorage.getStorageAccountForDevice(mDevice);
        mObexClient = obexClient;

        initializeStates();
    }

    private void initializeStates() {
        mDisconnected = new Disconnected();
        mConnecting = new Connecting();
        mDisconnecting = new Disconnecting();
        mConnected = new Connected();
        mDownloading = new Downloading();

        addState(mDisconnected);
        addState(mConnecting);
        addState(mDisconnecting);
        addState(mConnected);
        addState(mDownloading, mConnected);

        setInitialState(mDisconnected);
    }

    /**
     * Request to connect the device this state machine represents
     */
    public void connect() {
        debug("connect requested");
        sendMessage(MSG_CONNECT);
    }

    /**
     * Request to disconnect the device this state machine represents
     */
    public void disconnect() {
        debug("disconnect requested");
        sendMessage(MSG_DISCONNECT);
    }

    /**
     * Request to start the contacts download process
     */
    private void download() {
        sendMessage(MSG_DOWNLOAD);
    }

    /**
     * Notify this device state machine of a newly received SDP record
     */
    public void onSdpRecordReceived(PbapSdpRecord record) {
        sendMessage(MSG_SDP_COMPLETE, record);
    }

    /**
     * Notify this device state machine of a newly added device account
     */
    private void onPbapClientStorageReady() {
        obtainMessage(MSG_STORAGE_READY).sendToTarget();
    }

    /**
     * Notify this device state machine of a newly added device account
     */
    private void onPbapClientAccountAdded() {
        obtainMessage(MSG_ACCOUNT_ADDED).sendToTarget();
    }

    /**
     * Notify this device state machine of that its device account was removed
     */
    private void onPbapClientAccountRemoved() {
        obtainMessage(MSG_ACCOUNT_REMOVED).sendToTarget();
    }

    /**
     * Notify this device state machine of downloaded metadata from our OBEX client
     */
    private void onPhonebookMetadataReceived(PbapPhonebookMetadata metadata) {
        obtainMessage(MSG_PHONEBOOK_METADATA_RECEIVED, metadata).sendToTarget();
    }

    /**
     * Notify this device state machine that a download metadata request failed
     */
    private void onPhonebookMetadataDownloadFailed(String phonebook) {
        PbapPhonebookMetadata emptyMetadata = new PbapPhonebookMetadata(phonebook, 0, null, null, null);
        obtainMessage(MSG_PHONEBOOK_METADATA_RECEIVED, emptyMetadata).sendToTarget();
    }

    /**
     * Notify this device state machine of downloaded contacts from our OBEX client
     */
    private void onPhonebookContactsReceived(PbapPhonebook contacts) {
        obtainMessage(MSG_PHONEBOOK_CONTACTS_RECEIVED, contacts).sendToTarget();
    }

    /**
     * Notify this device state machine that a download contacts request failed
     */
    private void onPhonebookContactsDownloadFailed(String phonebook) {
        PbapPhonebook emptyContacts = new PbapPhonebook(phonebook);
        obtainMessage(MSG_PHONEBOOK_CONTACTS_RECEIVED, emptyContacts).sendToTarget();
    }

    /**
     * Get the current connection state
     */
    public int getConnectionState() {
        return mCurrentState;
    }

    class Disconnected extends State {
        @Override
        public void enter() {
            debug("Disconnected: Enter, from=" + eventToString(getCurrentMessage().what));
            if (mCurrentState != BluetoothProfile.STATE_DISCONNECTED) {
                // Only broadcast a state change that came from something other than disconnected
                onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED);

                // Quit processing on this handler. This makes this object one time use. The
                // connection state changed callback event will trigger the service to clean up
                // their state machine reference if they still have one.
                quit();
            }
        }

        @Override
        public boolean processMessage(Message message) {
            debug("Disconnected: process message, what=" + eventToString(message.what));
            switch (message.what) {
                case MSG_CONNECT:
                    transitionTo(mConnecting);
                    break;
                default:
                    warn("Disconnected: Received unhandled message, what=" + eventToString(message.what));
                    return NOT_HANDLED;
            }
            return true;
        }
    }

    class Connecting extends State {
        @Override
        public void enter() {
            debug("Connecting: Enter from=" + eventToString(getCurrentMessage().what));
            onConnectionStateChanged(BluetoothProfile.STATE_CONNECTING);

            // We can't connect over OBEX until we known where/how to connect. We need the SDP
            // record details to do this. Thus, being connected means we received a valid SDP record
            // and properly connected our OBEX Client afterwards.
            mDevice.sdpSearch(BluetoothUuid.PBAP_PSE);

            // Wait up to CONNECT_TIMEOUT for SDP to complete and our OBEX client to connect
            sendMessageDelayed(MSG_CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
        }

        @Override
        public boolean processMessage(Message message) {
            debug("Connecting: process message, what=" + eventToString(message.what));
            switch (message.what) {
                case MSG_DISCONNECT:
                    transitionTo(mDisconnecting);
                    break;

                case MSG_OBEX_CLIENT_CONNECTED:
                    transitionTo(mConnected);
                    break;

                case MSG_OBEX_CLIENT_DISCONNECTED:
                case MSG_CONNECT_TIMEOUT:
                    transitionTo(mDisconnecting);
                    break;

                case MSG_SDP_COMPLETE:
                    mSdpRecord = (PbapSdpRecord) message.obj;

                    info("Connecting: received SDP record, record=" + mSdpRecord);

                    if (!mDevice.equals(mSdpRecord.getDevice())) {
                        warn("Connecting: received SDP record for improper device. Ignoring.");
                        return HANDLED;
                    }

                    // Use SDP contents to determine whether we connect on L2CAP or RFCOMM
                    if (mSdpRecord.getL2capPsm() != /* L2CAP_INVALID_PSM */ -1) {
                        mObexClient.connectL2cap(mSdpRecord.getL2capPsm());
                    } else if (mSdpRecord.getRfcommChannelNumber() != /* RFCOMM_INVALID_CHANNEL */ -1) {
                        mObexClient.connectRfcomm(mSdpRecord.getRfcommChannelNumber());
                    } else {
                        error("Connecting: SDP record did not contain a valid L2CAP PSM or RFCOMM channel");
                        mDevice.sdpSearch(BluetoothUuid.PBAP_PSE);
                    }

                    if (mSdpRecord.isRepositorySupported(PbapSdpRecord.REPOSITORY_FAVORITES)) {
                        mPhonebooks.put(PbapPhonebook.FAVORITES_PATH, new Phonebook(PbapPhonebook.FAVORITES_PATH));
                    }
                    if (mSdpRecord.isRepositorySupported(PbapSdpRecord.REPOSITORY_LOCAL_PHONEBOOK)) {
                        mPhonebooks.put(PbapPhonebook.LOCAL_PHONEBOOK_PATH, new Phonebook(PbapPhonebook.LOCAL_PHONEBOOK_PATH));
                        mPhonebooks.put(PbapPhonebook.MCH_PATH, new Phonebook(PbapPhonebook.MCH_PATH));
                        mPhonebooks.put(PbapPhonebook.ICH_PATH, new Phonebook(PbapPhonebook.ICH_PATH));
                        mPhonebooks.put(PbapPhonebook.OCH_PATH, new Phonebook(PbapPhonebook.OCH_PATH));
                    }
                    if (mSdpRecord.isRepositorySupported(PbapSdpRecord.REPOSITORY_SIM_CARD)) {
                        mPhonebooks.put(PbapPhonebook.SIM_PHONEBOOK_PATH, new Phonebook(PbapPhonebook.SIM_PHONEBOOK_PATH));
                        mPhonebooks.put(PbapPhonebook.SIM_MCH_PATH, new Phonebook(PbapPhonebook.SIM_MCH_PATH));
                        mPhonebooks.put(PbapPhonebook.SIM_ICH_PATH, new Phonebook(PbapPhonebook.SIM_ICH_PATH));
                        mPhonebooks.put(PbapPhonebook.SIM_OCH_PATH, new Phonebook(PbapPhonebook.SIM_OCH_PATH));
                    }
                    break;

                default:
                    warn("Connecting: Received unhandled message, what=" + eventToString(message.what));
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        @Override
        public void exit() {
            removeMessages(MSG_CONNECT_TIMEOUT);
        }
    }

    class Connected extends State {
        private boolean mHasDownloaded = false;

        @Override
        public void enter() {
            debug("Connected: Enter, from=" + eventToString(getCurrentMessage().what));
            if (mCurrentState != BluetoothProfile.STATE_CONNECTING) {
                return;
            }

            onConnectionStateChanged(BluetoothProfile.STATE_CONNECTED);

            mHasDownloaded = false;

            mContactsStorage.registerCallback(mStorageCallback);
            if (mContactsStorage.isStorageReady()) {
                onPbapClientStorageReady();
            } else {
                Log.i(TAG, "Awaiting storage to be ready");
            }
        }

        @Override
        public boolean processMessage(Message message) {
            debug("Connected: process message, what=" + eventToString(message.what));
            switch (message.what) {
                case MSG_OBEX_CLIENT_DISCONNECTED:
                case MSG_DISCONNECT:
                    transitionTo(mDisconnecting);
                    break;

                case MSG_STORAGE_READY:
                    if (mContactsStorage.getStorageAccounts().contains(mAccount)) {
                        info("Connected: Account already exists, time to download");
                        if (!mHasDownloaded) {
                            download();
                            mHasDownloaded = true;
                        }
                    } else {
                        info("Connected: Account not found. Requesting to add it.");
                        mContactsStorage.addAccount(mAccount);
                    }
                    break;

                case MSG_ACCOUNT_ADDED:
                    info("Connected: account was added, time to download");
                    if (!mHasDownloaded) {
                        download();
                        mHasDownloaded = true;
                    }
                    break;

                case MSG_ACCOUNT_REMOVED:
                    info("Connected: account was removed, time to disconnect");
                    transitionTo(mDisconnecting);
                    break;

                case MSG_DOWNLOAD:
                    transitionTo(mDownloading);
                    break;

                default:
                    warn("Connected: received unhandled message, what=" + eventToString(message.what));
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class Downloading extends State {
        List<String> mPhonebooksToDownload = new ArrayList<String>();

        @Override
        public void enter() {

            info("Downloading: Start download process, caching=" + useContactsCachingFeature());

            // When caching, we should delete cached contacts for repositorys we have that are no
            // longer supported by the remote device.
            if (useContactsCachingFeature()) {
                List<String> cachedPhonebooks = mContactsStorage.getCachedPhonebooks(mAccount);
                for (String pb : cachedPhonebooks) {
                    if (!mPhonebooks.containsKey(pb)) {
                        warn("Downloading: Cached phonebook=" + pb + " no longer supported by device. Clean up");
                        deleteStoredContacts(pb);
                    }
                }
            }

            // Initialize our list of phonebooks to download based on supported respositories
            initializePhonebooksToDownload();

            String currentPhonebook = getCurrentPhonebook();
            if (currentPhonebook != null) {
                downloadPhonebookMetadata(currentPhonebook);
            } else {
                warn("Downloading: no supported respositories to download");
                transitionTo(mConnected);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            String currentPhonebook = getCurrentPhonebook();
            String phonebook = null;
            debug("Downloading: process message, what=" + eventToString(message.what));
            switch (message.what) {
                case MSG_DISCONNECT:
                    transitionTo(mDisconnecting);
                    break;

                case MSG_PHONEBOOK_METADATA_RECEIVED:
                    PbapPhonebookMetadata metadata = (PbapPhonebookMetadata) message.obj;
                    phonebook = metadata.getPhonebook();
                    if (currentPhonebook != null && currentPhonebook.equals(phonebook)) {
                        info("Downloading: received metadata=" + metadata);

                        // Process Metadata
                        mPhonebooks.get(phonebook).setMetadata(metadata);

                        // If version we have is different than version they have, or the version
                        // is invalid, then delete any cached contacts if we have them

                        if (useContactsCachingFeature()) {
                            if (shouldClearCachedContacts(metadata)) {
                                deleteStoredContacts(phonebook);
                            } else {
                                info("Downloading: contacts up to date for phonebook=" + phonebook + ", use cached version and skip the download");
                                mPhonebooks.get(phonebook).setUsingCached(true);
                                setNextPhonebookOrComplete();
                                break;
                            }
                        }

                        // If phonebook has contacts, begin downloading them
                        if (metadata.getSize() > 0) {
                            downloadPhonebook(currentPhonebook, 0, CONTACT_DOWNLOAD_BATCH_SIZE);
                        } else {
                            warn("Downloading: no contacts for phonebook=" + currentPhonebook + ", skipping");
                            setNextPhonebookOrComplete();
                            break;
                        }
                    } else {
                        warn("Downloading: dropped metadata event for phonebook=" + phonebook + ", current=" + currentPhonebook);
                    }
                    break;

                case MSG_PHONEBOOK_CONTACTS_RECEIVED:
                    PbapPhonebook contacts = (PbapPhonebook) message.obj;
                    phonebook = contacts.getPhonebook();
                    if (currentPhonebook != null && currentPhonebook.equals(phonebook)) {
                        int numReceived = contacts.getCount();
                        mPhonebooks.get(phonebook).onContactsDownloaded(numReceived);
                        int totalContactDownloaded = mPhonebooks.get(phonebook).getNumberOfContactsDownloaded();
                        int totalContactsExpected = mPhonebooks.get(phonebook).getTotalNumberOfContacts();

                        info("Downloading: received contacts, phonebook=" + phonebook + ", entries=" + numReceived + ", total=" + totalContactDownloaded + "/" + totalContactsExpected);
                        if (numReceived != 0) {
                            storeDownloadedContacts(phonebook, contacts);
                        } else {
                            warn("Downloading: contacts empty for phonebook=" + phonebook + ", proceed to next phonebook");
                            setNextPhonebookOrComplete();
                            break;
                        }

                        if (totalContactDownloaded >= totalContactsExpected) {
                            info("Downloading: download complete, phonebook=" + phonebook);
                            storePhonebookMetadata(phonebook, mPhonebooks.get(phonebook).getMetadata());
                            setNextPhonebookOrComplete();
                        } else {
                            downloadPhonebook(currentPhonebook, totalContactDownloaded, CONTACT_DOWNLOAD_BATCH_SIZE);
                        }
                    } else {
                        warn("Downloading: dropped received contacts, phonebook=" + phonebook);
                    }
                    break;

                default:
                    debug("Downloading: passing message to parent state, type="
                            + eventToString(message.what));
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        /**
         * Initialize our prioritized list of phonebooks we want to download
         */
        private void initializePhonebooksToDownload() {
            mPhonebooksToDownload.clear();

            if (mPhonebooks.containsKey(PbapPhonebook.FAVORITES_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.FAVORITES_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.LOCAL_PHONEBOOK_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.SIM_PHONEBOOK_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.SIM_PHONEBOOK_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.MCH_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.MCH_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.ICH_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.ICH_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.OCH_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.OCH_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.SIM_MCH_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.SIM_MCH_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.SIM_ICH_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.SIM_ICH_PATH);
            }
            if (mPhonebooks.containsKey(PbapPhonebook.SIM_OCH_PATH)) {
                mPhonebooksToDownload.add(PbapPhonebook.SIM_OCH_PATH);
            }

            info("Downloading: intialized download process, phonebooks=" + mPhonebooksToDownload);
        }

        /**
         * Get the currently downloading/processing phonebook path
         */
        private String getCurrentPhonebook() {
            return mPhonebooksToDownload.size() != 0 ? mPhonebooksToDownload.get(0) : null;
        }

        /**
         * Complete operation on one phonebook and update to the next one, if available.
         *
         * If there's further phonebooks to download, this will trigger the process to download the
         * next phonebook. If there are no more phonebooks to download, this will return us to the
         * Connected state.
         */
        private void setNextPhonebookOrComplete() {
            String currentPhonebook = getCurrentPhonebook();
            if (currentPhonebook == null) {
                warn("Downloading: No phonebooks left to download");
                transitionTo(mConnected);
                return;
            }

            mPhonebooksToDownload.remove(0);
            if (mPhonebooksToDownload.size() != 0) {
                String nextPhonebook = getCurrentPhonebook();
                debug("Downloading: Phonebook changed, old=" + currentPhonebook + ", new=" + nextPhonebook);
                downloadPhonebookMetadata(nextPhonebook);
            } else {
                info("Downloading: All phonebooks downloaded");
                transitionTo(mConnected);
            }
        }

        /**
         * Request the size and version counters for a specific phonebook, by path.
         *
         * Downloads are in two parts. First we get the metadata and then we use that to create
         * batches to download. Downloaded contacts are handed to the Contacts Client for storage
         */
        private void downloadPhonebookMetadata(String path) {
            info("Downloading: Request metadata, phonebook=" + path);
            mObexClient.requestPhonebookMetadata(path, new PbapApplicationParameters(DEFAULT_PROPERTIES, DEFAULT_VCARD_VERSION, PbapApplicationParameters.RETURN_SIZE_ONLY, 0));
        }

        /**
         * Compare the incoming database identifier and version counters with our cached values to
         * see if we should clear any cached contacts in preparation for replacement.
         */
        private boolean shouldClearCachedContacts(PbapPhonebookMetadata metadata) {
            String phonebook = metadata.getPhonebook();
            String databaseIdentifier = metadata.getDatabaseIdentifier();
            String primaryVersion = metadata.getPrimaryVersionCounter();
            String secondaryVersion = metadata.getSecondaryVersionCounter();

            if (!useContactsCachingFeature()) {
                info("Downloading: Caching not enabled. Do not use cached contacts or metadata");
                return true;
            }

            // No need to use cached call history. Always clean that up if it's there and redownload
            if (!PbapPhonebook.FAVORITES_PATH.equals(phonebook) && !PbapPhonebook.LOCAL_PHONEBOOK_PATH.equals(phonebook) && !PbapPhonebook.SIM_PHONEBOOK_PATH.equals(phonebook)) {
                info("Downloading: Clear cached data for phonebook=" + phonebook);
                mContactsStorage.setCachedPhonebookMetadata(mAccount, phonebook, null);
                return true;
            }

            // Get current cached data versions for this phonebook
            PbapPhonebookMetadata cachedMetadata = mContactsStorage.getCachedPhonebookMetadata(mAccount, phonebook);
            String cachedDatabaseIdentifier = null;
            String cachedPrimaryVersion = null;
            String cachedSecondaryVersion = null;
            if (cachedMetadata != null) {
                cachedDatabaseIdentifier = cachedMetadata.getDatabaseIdentifier();
                cachedPrimaryVersion = cachedMetadata.getPrimaryVersionCounter();
                cachedSecondaryVersion = cachedMetadata.getSecondaryVersionCounter();
            }

            // Database Identifiers indicate whether or not folder version counters or contact UIDs
            // from a previous session can be reused. Changes in value imply any previous folder
            // counters no longer apply and that we should delete any stored contacts we have. A
            // Database identifier of "0" (default) means that, while the feature is supported,
            // the server doesn't actually implement it and the resulting primary and
            // secondary version counters are not valid. This means we always need to re-download.
            if (databaseIdentifier == PbapPhonebookMetadata.INVALID_DATABASE_IDENTIFIER || databaseIdentifier.equals(PbapPhonebookMetadata.DEFAULT_DATABASE_IDENTIFIER)) {
                info("Downloading: Database identifier is 0 or missing for phonebook=" + phonebook + ", clear any cached data");
                return true;
            }
            if (cachedDatabaseIdentifier == null || !cachedDatabaseIdentifier.equals(databaseIdentifier)) {
                info("Downloading: Database Identifiers do not match (cached=" + cachedDatabaseIdentifier + ", remote=" + databaseIdentifier + "), clear any cached data");
                return true;
            }

            // The Primary Version Counter will change on insertion or removal of entries and
            // updates to _any_ vCard properties. Primary changing implies anyhthing at all has
            // updated.
            if (primaryVersion == PbapPhonebookMetadata.INVALID_VERSION_COUNTER) {
                info("Downloading: Primary version counter has changed for phonebook=" + phonebook + ", clear any cached data");
                return true;
            }
            if (cachedPrimaryVersion == null || !cachedPrimaryVersion.equals(primaryVersion)) {
                info("Downloading: Primary versions do not match (cached=" + cachedPrimaryVersion + ", remote=" + primaryVersion + "), clear any cached data");
                return true;
            }

            // The Secondary Version Counter will change on insertion or removal of entries and
            // updates to a subset of vCard properties, specifically N, FN, TEL, EMAIL, MAILER, ADR
            // or x-bt-UCI properties. Secondary chaning implies a normal, potentially more
            // meaningful set of changes have occurred to the folder.
            if (secondaryVersion == PbapPhonebookMetadata.INVALID_VERSION_COUNTER) {
                info("Downloading: Secondary version counter has changed for phonebook=" + phonebook + ", clear any cached data");
                return true;
            }
            if (cachedSecondaryVersion == null || !cachedSecondaryVersion.equals(secondaryVersion)) {
                info("Downloading: Secondary versions do not match (cached=" + cachedSecondaryVersion + ", remote=" + secondaryVersion + "), clear any cached data");
                return true;
            }

            info("Downloading: Contact data up to date for phonebook=" + phonebook);
            return false;
        }

        /**
         * Download a specific phonebook, by path, using the given batching parameters
         *
         * Downloads are in two parts. First we get the metadata and then we use that to create
         * batches to download. Downloaded contacts are handed to the Contacts Client for storage
         */
        private void downloadPhonebook(String path, int batchStart, int numToFetch) {
            int batchEnd =  (batchStart + numToFetch - 1);
            info("Downloading: Download contents, phonebook=" + path + ", start=" + batchStart + ", end=" + batchEnd);
            PbapApplicationParameters params = new PbapApplicationParameters(DEFAULT_PROPERTIES, DEFAULT_VCARD_VERSION, numToFetch, batchStart);
            mObexClient.requestDownloadPhonebook(mPhonebooksToDownload.get(0), params, mAccount);
        }
    }

    class Disconnecting extends State {
        @Override
        public void enter() {
            debug("Disconnecting: Enter, from=" + eventToString(getCurrentMessage().what));
            onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTING);

            // Disconnect
            if (mObexClient.getConnectionState() != BluetoothProfile.STATE_DISCONNECTED) {
                mObexClient.disconnect();
                sendMessageDelayed(MSG_DISCONNECT_TIMEOUT, DISCONNECT_TIMEOUT_MS);
            } else {
                transitionTo(mDisconnected);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            debug("Disconnecting: process message, what=" + eventToString(message.what));
            switch (message.what) {
                case MSG_OBEX_CLIENT_DISCONNECTED:
                    removeMessages(MSG_DISCONNECT_TIMEOUT);
                    transitionTo(mDisconnected);
                    break;

                case MSG_DISCONNECT:
                    deferMessage(message);
                    break;

                case MSG_DISCONNECT_TIMEOUT:
                    warn("Disconnecting: Timeout, Forcing");
                    // TODO: Force disconnect?
                    // mObexClient.close();
                    mObexClient.disconnect();
                    transitionTo(mDisconnected);
                    break;

                default:
                    warn("Disconnecting: Received unhandled message, what=" + eventToString(message.what));
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        @Override
        public void exit() {
            mContactsStorage.unregisterCallback(mStorageCallback);

            // Always remove data as a last step. If caching, remove contacts if no metadata, and
            // remove account if all contacts removed.
            cleanup();
        }
    }

    /**
     * Force this state machine to stop immediately
     *
     * This function quits the state machine operation by broadcasting the proper connection state
     * changes and properly cleaning up data that may be exist.
     */
    @Override
    protected void onQuitting() {
        Log.d(TAG, "State machine is force quitting");
        switch (mCurrentState) {
            case BluetoothProfile.STATE_CONNECTED:
                onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTING);
                // intentional fallthrough-- we want to broadcast both state changes
            case BluetoothProfile.STATE_DISCONNECTING:
                onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED);
                cleanup();
                break;
            default:
                Log.i(TAG, "Force quit a disconnected state machine. No state to broadcast");
        }
    }

    private void cleanup() {
        info("cleanup: evaluate data to cleanup");
        if (useContactsCachingFeature()) {
            cleanupUncachedDataAndAccounts();
        } else {
            cleanupContactsDataAndAccounts();
        }
    }

    private void cleanupContactsDataAndAccounts() {
        info("cleanupContactsDataAndAccounts: clear saved contacts, call history and account");
        mContactsStorage.removeAllContacts(mAccount);
        mContactsStorage.removeCallHistory(mAccount);
        mContactsStorage.removeAccount(mAccount);
    }

    private void cleanupUncachedDataAndAccounts() {
        info("cleanupUncachedDataAndAccounts: Check cached metadata");
        List<String> cachedPhonebooks = mContactsStorage.getCachedPhonebooks(mAccount);

        // If nothing was cached, delete everything!
        if (cachedPhonebooks.size() == 0) {
            info("cleanupUncachedDataAndAccounts: No cached contacts. Clear saved contacts, call history and account");
            cleanupContactsDataAndAccounts();
            return;
        }

        // Otherwise, compare what was downloaded and what was cached to see if we need to
        // clear contacts
        for (Phonebook pb : mPhonebooks.values()) {
            String phonebook = pb.getName();

            if (pb.getUsingCached()) {
                debug("cleanupUncachedDataAndAccounts: phonebook=" + phonebook + " used cached version and wasn't downloaded. Nothing to clean up");
                continue;
            }

            // Wasn't supported, wasn't downloaded, or had no meaningful contacts
            if (pb.getTotalNumberOfContacts() == 0 || pb.getNumberOfContactsDownloaded() == 0) {
                debug("cleanupUncachedDataAndAccounts: phonebook=" + phonebook + " wasn't downloaded. Clean up any contacts and metadata");
                deleteStoredContacts(phonebook);
                continue;
            }

            // Wasn't downloaded all the way
            if (pb.getNumberOfContactsDownloaded() < pb.getTotalNumberOfContacts()) {
                debug("cleanupUncachedDataAndAccounts: phonebook=" + phonebook + " wasn't downloaded all the way. Clean up any contacts and metadata");
                mContactsStorage.setCachedPhonebookMetadata(mAccount, phonebook, null);
                deleteStoredContacts(phonebook);
                continue;
            }

            // Wasn't cached
            if (!cachedPhonebooks.contains(phonebook)) {
                debug("cleanupUncachedDataAndAccounts: phonebook=" + phonebook + " was downloaded, but wasn't cached. Clean up any contacts and metadata");
                deleteStoredContacts(phonebook);
                continue;
            }

            debug("cleanupUncachedDataAndAccounts: phonebook=" + phonebook + " was downloaded and cached");
        }

        // Always clear call history
        info("cleanupUncachedDataAndAccounts: clear saved call history");
        mContactsStorage.removeCallHistory(mAccount);

        // If we have no metadata left over, then remove the account
        cachedPhonebooks = mContactsStorage.getCachedPhonebooks(mAccount);
        if (cachedPhonebooks.size() == 0) {
            info("cleanupUncachedDataAndAccounts: All cached contacts cleaned up, remove account");
            mContactsStorage.removeAccount(mAccount);
        } else {
            info("cleanupUncachedDataAndAccounts: Persisting phonebooks=" + cachedPhonebooks);
        }
    }

    private void storePhonebookMetadata(String phonebook, PbapPhonebookMetadata metadata) {
        if (!useContactsCachingFeature()) {
            info("Caching not enabled. Do not store metadata for phonebook=" + phonebook);
            return;
        }

        if (PbapPhonebook.FAVORITES_PATH.equals(phonebook) || PbapPhonebook.LOCAL_PHONEBOOK_PATH.equals(phonebook) || PbapPhonebook.SIM_PHONEBOOK_PATH.equals(phonebook)) {
            info("Cache phonebook=" + phonebook);
            mContactsStorage.setCachedPhonebookMetadata(mAccount, phonebook, metadata);
            return;
        }

        info("Caching not supported for phonebook=" + phonebook);
    }

    /**
     * Request to insert downloaded contacts into storage
     */
    private void storeDownloadedContacts(String phonebook, PbapPhonebook contacts) {
        info("Request to store contacts for phonebook=" + phonebook);
        if (phonebook.equals(PbapPhonebook.FAVORITES_PATH)) {
            mContactsStorage.insertFavorites(mAccount, contacts.getList());
        } else if (phonebook.equals(PbapPhonebook.LOCAL_PHONEBOOK_PATH)) {
            mContactsStorage.insertLocalContacts(mAccount, contacts.getList());
        } else if (phonebook.equals(PbapPhonebook.SIM_PHONEBOOK_PATH)) {
            mContactsStorage.insertSimContacts(mAccount, contacts.getList());
        } else if (phonebook.equals(PbapPhonebook.MCH_PATH) || phonebook.equals(PbapPhonebook.SIM_MCH_PATH)) {
            mContactsStorage.insertMissedCallHistory(mAccount, contacts.getList());
        } else if (phonebook.equals(PbapPhonebook.ICH_PATH) || phonebook.equals(PbapPhonebook.SIM_ICH_PATH)) {
            mContactsStorage.insertIncomingCallHistory(mAccount, contacts.getList());
        } else if (phonebook.equals(PbapPhonebook.OCH_PATH) || phonebook.equals(PbapPhonebook.SIM_OCH_PATH)) {
            mContactsStorage.insertOutgoingCallHistory(mAccount, contacts.getList());
        } else {
            warn("Received unknown phonebook to store, phonebook=" + phonebook);
        }
    }

    private void deleteStoredMetadata(String phonebook) {
        if (!useContactsCachingFeature()) {
            info("Caching not enabled. Do not delete metadata for phonebook=" + phonebook);
            return;
        }

        if (PbapPhonebook.FAVORITES_PATH.equals(phonebook) || PbapPhonebook.LOCAL_PHONEBOOK_PATH.equals(phonebook) || PbapPhonebook.SIM_PHONEBOOK_PATH.equals(phonebook)) {
            info("Delete any cached metadata for phonebook=" + phonebook);
            mContactsStorage.setCachedPhonebookMetadata(mAccount, phonebook, null);
            return;
        }

        warn("Caching not supported for phonebook=" + phonebook);
    }

    private void deleteStoredContacts(String phonebook) {
        info("Delete stored contacts for phonebook=" + phonebook);
        if (phonebook.equals(PbapPhonebook.FAVORITES_PATH)) {
            deleteStoredMetadata(PbapPhonebook.FAVORITES_PATH);
            mContactsStorage.removeFavorites(mAccount);
        } else if (phonebook.equals(PbapPhonebook.LOCAL_PHONEBOOK_PATH)) {
            deleteStoredMetadata(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
            mContactsStorage.removeLocalContacts(mAccount);
        } else if (phonebook.equals(PbapPhonebook.SIM_PHONEBOOK_PATH)) {
            deleteStoredMetadata(PbapPhonebook.SIM_PHONEBOOK_PATH);
            mContactsStorage.removeSimContacts(mAccount);
        } else if (phonebook.equals(PbapPhonebook.MCH_PATH) || phonebook.equals(PbapPhonebook.SIM_MCH_PATH)) {
            mContactsStorage.removeCallHistory(mAccount);
        } else if (phonebook.equals(PbapPhonebook.ICH_PATH) || phonebook.equals(PbapPhonebook.SIM_ICH_PATH)) {
            mContactsStorage.removeCallHistory(mAccount);
        } else if (phonebook.equals(PbapPhonebook.OCH_PATH) || phonebook.equals(PbapPhonebook.SIM_OCH_PATH)) {
            mContactsStorage.removeCallHistory(mAccount);
        } else {
            warn("Received unknown phonebook to delete, phonebook=" + phonebook);
        }
    }

    private void onConnectionStateChanged(int state) {
        int prevState = mCurrentState;
        if (prevState != state && state == BluetoothProfile.STATE_CONNECTED) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.PBAP_CLIENT);
        }

        Intent intent = new Intent(BluetoothPbapClient.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, state);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);

        // Update the state, notify our service, AdapterService, and send the broadcast all at once
        mCurrentState = state;

        info("Connection state changed, prev=" + prevState + ", new=" + state);

        AdapterService adapterService = AdapterService.getAdapterService();
        mCallback.onConnectionStateChanged(prevState, state);
        if (adapterService != null) {
            adapterService.updateProfileConnectionAdapterProperties(
                    mDevice, BluetoothProfile.PBAP_CLIENT, state, prevState);
        }
        mContext.sendBroadcastMultiplePermissions(intent,
                new String[] {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED},
                Utils.getTempBroadcastOptions());
    }

    /**
     * Callback for getting events back from our OBEX Client
     */
    class PbapClientObexClientCallback implements PbapClientObexClient.Callback {

        PbapClientObexClientCallback() {
        }

        @Override
        public void onConnectionStateChanged(int oldState, int newState) {
            info("Obex client connection state changed: " + oldState + " -> " + newState);
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                obtainMessage(MSG_OBEX_CLIENT_DISCONNECTED).sendToTarget();
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                obtainMessage(MSG_OBEX_CLIENT_CONNECTED).sendToTarget();
            }
        }

        @Override
        public void onGetPhonebookMetadataComplete(int responseCode, String phonebook, PbapPhonebookMetadata metadata) {
            if (responseCode != ResponseCodes.OBEX_HTTP_OK) {
                warn("PullPhonebook for metadata failed, phonebook=" + phonebook + ", code=" + responseCode);
                onPhonebookMetadataDownloadFailed(phonebook);
                return;
            }
            debug("Received phonebook metadata, phonebook=" + phonebook + ", metadata=" + metadata);
            onPhonebookMetadataReceived(metadata);
        }

        @Override
        public void onPhonebookContactsDownloaded(int responseCode, String phonebook, PbapPhonebook contacts) {
            if (responseCode != ResponseCodes.OBEX_HTTP_OK) {
                warn("PullPhonebook failed, phonebook=" + phonebook + ", code=" + responseCode);
                onPhonebookContactsDownloadFailed(phonebook);
                return;
            }
            debug("Received contacts, phonebook=" + phonebook + ", count=" + contacts.getCount());
            onPhonebookContactsReceived(contacts);
        }
    }

    private boolean useContactsCachingFeature() {
        return SystemProperties.getBoolean("persist.bluetooth.profile.pbap.client.feature.caching.enabled", false);
    }

    private static String eventToString(int message) {
        switch (message) {
            case -2 /* Special, from StateMachine.java */:
                return "SM_INIT_CMD";
            case -1 /* Special, from StateMachine.java */:
                return "SM_QUIT_CMD";
            case MSG_CONNECT:
                return "MSG_CONNECT";
            case MSG_DISCONNECT:
                return "MSG_DISCONNECT";
            case MSG_SDP_COMPLETE:
                return "MSG_SDP_COMPLETE";
            case MSG_OBEX_CLIENT_CONNECTED:
                return "MSG_OBEX_CLIENT_CONNECTED";
            case MSG_OBEX_CLIENT_DISCONNECTED:
                return "MSG_OBEX_CLIENT_DISCONNECTED";
            case MSG_STORAGE_READY:
                return "MSG_STORAGE_READY";
            case MSG_ACCOUNT_ADDED:
                return "MSG_ACCOUNT_ADDED";
            case MSG_ACCOUNT_REMOVED:
                return "MSG_ACCOUNT_REMOVED";
            case MSG_DOWNLOAD:
                return "MSG_DOWNLOAD";
            case MSG_PHONEBOOK_METADATA_RECEIVED:
                return "MSG_PHONEBOOK_METADATA_RECEIVED";
            case MSG_PHONEBOOK_CONTACTS_RECEIVED:
                return "MSG_PHONEBOOK_CONTACTS_RECEIVED";
            case MSG_CONNECT_TIMEOUT:
                return "MSG_CONNECT_TIMEOUT";
            case MSG_DISCONNECT_TIMEOUT:
                return "MSG_DISCONNECT_TIMEOUT";
            default:
                return "Unknown (" + message + ")";
        }
    }

    private void debug(String message) {
        Log.d(TAG, "[" + mDevice + "] " + message);
    }

    private void info(String message) {
        Log.i(TAG, "[" + mDevice + "] " + message);
    }

    private void warn(String message) {
        Log.w(TAG, "[" + mDevice + "] " + message);
    }

    private void error(String message) {
        Log.e(TAG, "[" + mDevice + "] " + message);
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "  mDevice: " + mDevice.getAddress() + "("
                + Utils.getName(mDevice) + ") " + this.toString());

        if (mSdpRecord != null) {
            ProfileService.println(sb, "    Server Version: " + PbapSdpRecord.versionToString(mSdpRecord.getProfileVersion()));
        } else {
            ProfileService.println(sb, "    Server Version: Unknown, no SDP record");
        }

        ProfileService.println(sb, "    OBEX Client: " + mObexClient);

        ProfileService.println(sb, "    Download Batch Size: " + CONTACT_DOWNLOAD_BATCH_SIZE);
        ProfileService.println(sb, "    Use Caching: " + useContactsCachingFeature());

        int totalContacts = 0;
        int totalContactDownloaded = 0;
        ProfileService.println(sb, "    Supported Repositories:");
        for (Phonebook pb : mPhonebooks.values()) {
            ProfileService.println(sb, "      " + pb);
            totalContacts += pb.getTotalNumberOfContacts();
            totalContactDownloaded += pb.getNumberOfContactsDownloaded();
        }
        ProfileService.println(sb, "    Total Contacts: " + totalContacts);
        ProfileService.println(sb, "    Download Progress: " + totalContactDownloaded + "/" + totalContacts);
    }
}
