/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.bluetooth.BluetoothProfile.CONNECTION_POLICY_ALLOWED;
import static android.bluetooth.BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
import static android.bluetooth.BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;

import static com.android.bluetooth.TestUtils.MockitoRule;
import static com.android.bluetooth.TestUtils.getTestDevice;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.accounts.Account;
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
=======
import android.accounts.AccountManager;
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.SdpPseRecord;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Looper;

import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
import androidx.test.runner.AndroidJUnit4;
||||||| BASE
=======
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapClientServiceTest {
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
    @Rule public final MockitoRule mMockitoRule = new MockitoRule();

    @Mock private AdapterService mAdapterService;
    @Mock private DatabaseManager mDatabaseManager;
    @Mock private PackageManager mPackageManager;
    @Mock private Resources mResources;
||||||| BASE
=======
    @Rule public final SetFlagsRule mSetFlagsRule;
    @Rule public final MockitoRule mMockitoRule = new MockitoRule();

    @Mock private AdapterService mAdapterService;
    @Mock private DatabaseManager mDatabaseManager;
    @Mock private PackageManager mPackageManager;
    @Mock private Resources mResources;
    @Mock private UserManager mUserManager;
    @Mock private AccountManager mAccountManager;
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    @Mock private SdpPseRecord mMockSdpRecord;
    @Mock private PbapClientContactsStorage mMockStorage;
    @Mock private PbapClientStateMachine mMockDeviceStateMachine;

    // Constants for SDP. Note that these values come from the native stack, but no centralized
    // constants exist for them as part of the various SDP APIs.
    public static final int SDP_SUCCESS = 0;
    public static final int SDP_FAILED = 1;
    public static final int SDP_BUSY = 2;

    // Constant for testing ACL disconnection events with a bad transport
    public static final int TRANSPORT_UNKNOWN = -1;

    private final Context mTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final BluetoothDevice mDevice = getTestDevice(56);
    private final Map<BluetoothDevice, PbapClientStateMachine> mDeviceMap =
            new HashMap<BluetoothDevice, PbapClientStateMachine>();

<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    private PbapClientService mService = null;
    private BluetoothAdapter mAdapter = null;
    private BluetoothDevice mRemoteDevice;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private Context mMockContext;
    @Mock private AdapterService mMockAdapterService;
    @Mock private DatabaseManager mDatabaseManager;
    @Mock private PackageManager mMockPackageManager;
    private MockContentResolver mMockContentResolver;
    private MockCallLogProvider mMockCallLogProvider;
    @Mock private Resources mMockResources;
    @Mock private UserManager mMockUserManager;
    @Mock private AccountManager mMockAccountManager;
=======
    private MockContentResolver mMockContentResolver;
    private MockCallLogProvider mMockCallLogProvider;
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    private PbapClientService mService;

    // NEW: Objects for new state machine implementation
    private PbapClientService.PbapClientStateMachineCallback mDeviceCallback;
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @Mock private PbapClientContactsStorage mMockStorage;
    private Map<BluetoothDevice, PbapClientStateMachine> mMockDeviceMap =
            new HashMap<BluetoothDevice, PbapClientStateMachine>();
    @Mock private PbapClientStateMachine mMockDeviceStateMachine;

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.progressionOf(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR);
    }

    public PbapClientServiceTest(FlagsParameterization flags) {
        mSetFlagsRule = new SetFlagsRule(flags);
    }
=======

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.progressionOf(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR);
    }

    public PbapClientServiceTest(FlagsParameterization flags) {
        mSetFlagsRule = new SetFlagsRule(flags);
    }
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)

    @Before
    public void setUp() throws Exception {
        doReturn(mDatabaseManager).when(mAdapterService).getDatabase();
        doReturn(CONNECTION_POLICY_ALLOWED)
                .when(mDatabaseManager)
                .getProfileConnectionPolicy(any(), anyInt());
        doReturn(true).when(mDatabaseManager).setProfileConnectionPolicy(any(), anyInt(), anyInt());

        doReturn(mTargetContext.getPackageName()).when(mAdapterService).getPackageName();
        doReturn(mPackageManager).when(mAdapterService).getPackageManager();

        doReturn(mResources).when(mAdapterService).getResources();
        doReturn(Utils.ACCOUNT_TYPE).when(mResources).getString(anyInt());
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE

        mMockContentResolver = new MockContentResolver();
        mMockCallLogProvider = new MockCallLogProvider();
        mMockContentResolver.addProvider(CallLog.AUTHORITY, mMockCallLogProvider);
        doReturn(mMockContentResolver).when(mMockContext).getContentResolver();

        doReturn(AccountManager.VISIBILITY_VISIBLE)
                .when(mMockAccountManager)
                .getAccountVisibility(any(Account.class), anyString());
        doReturn(new Account[] {})
                .when(mMockAccountManager)
                .getAccountsByType(eq(Utils.ACCOUNT_TYPE));
        TestUtils.mockGetSystemService(
                mMockContext, Context.ACCOUNT_SERVICE, AccountManager.class, mMockAccountManager);

        doReturn(false).when(mMockUserManager).isUserUnlocked();
        TestUtils.mockGetSystemService(
                mMockContext, Context.USER_SERVICE, UserManager.class, mMockUserManager);
=======

        mMockContentResolver = new MockContentResolver();
        mMockCallLogProvider = new MockCallLogProvider();
        mMockContentResolver.addProvider(CallLog.AUTHORITY, mMockCallLogProvider);
        doReturn(mMockContentResolver).when(mAdapterService).getContentResolver();

        doReturn(AccountManager.VISIBILITY_VISIBLE)
                .when(mAccountManager)
                .getAccountVisibility(any(Account.class), anyString());
        doReturn(new Account[] {}).when(mAccountManager).getAccountsByType(eq(Utils.ACCOUNT_TYPE));
        TestUtils.mockGetSystemService(
                mAdapterService, Context.ACCOUNT_SERVICE, AccountManager.class, mAccountManager);

        TestUtils.mockGetSystemService(
                mAdapterService, Context.USER_SERVICE, UserManager.class, mUserManager);
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)

        // new for mock storage
        doAnswer(
                        invocation -> {
                            BluetoothDevice device = (BluetoothDevice) invocation.getArgument(0);
                            return Utils.getAccountForDevice(device);
                        })
                .when(mMockStorage)
                .getStorageAccountForDevice(any(BluetoothDevice.class));

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mService = new PbapClientService(mAdapterService, mMockStorage, mDeviceMap);
        mService.setAvailable(true);

        // new
        doReturn(STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceMap.put(mDevice, mMockDeviceStateMachine);
        mDeviceCallback = mService.new PbapClientStateMachineCallback(mDevice);
    }

    @After
    public void tearDown() {
        mService.cleanup();
        assertThat(PbapClientService.getPbapClientService()).isNull();
    }

    // *********************************************************************************************
    // * Initialize Service
    // *********************************************************************************************

    @Test
    public void testInitialize() {
        assertThat(PbapClientService.getPbapClientService()).isNotNull();
    }

    @Test
    public void testSetPbapClientService_withNull() {
        PbapClientService.setPbapClientService(null);

        assertThat(PbapClientService.getPbapClientService()).isNull();
    }

    // *********************************************************************************************
    // * Incoming Events
    // *********************************************************************************************

    // PbapClientStateMachineCallback events from devices

    @Test
    public void onConnectionStateChanged_DisconnectedToConnecting_eventIgnored() {
        doReturn(STATE_CONNECTING).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(STATE_DISCONNECTED, STATE_CONNECTING);
        assertThat(mDeviceMap.containsKey(mDevice)).isTrue();
    }

    @Test
    public void onConnectionStateChanged_ConnectingToConnected_eventIgnored() {
        doReturn(STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(STATE_DISCONNECTED, STATE_CONNECTING);
        assertThat(mDeviceMap.containsKey(mDevice)).isTrue();
    }

    @Test
    public void onConnectionStateChanged_ConnectingToDisconnected_deviceCleanedUp() {
        doReturn(STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(STATE_CONNECTING, STATE_DISCONNECTED);
        assertThat(mDeviceMap.containsKey(mDevice)).isFalse();
    }

    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void onConnectionStateChanged_ConnectedToDisonnecting_eventIgnored() {
        doReturn(BluetoothProfile.STATE_DISCONNECTING)
                .when(mMockDeviceStateMachine)
=======
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void onConnectionStateChanged_ConnectedToDisconnecting_eventIgnored() {
        doReturn(STATE_DISCONNECTING).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(STATE_CONNECTED, STATE_DISCONNECTING);
        assertThat(mDeviceMap.containsKey(mDevice)).isTrue();
    }

    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void onConnectionStateChanged_DisconnectingToDisonnected_deviceCleanedUp() {
        doReturn(BluetoothProfile.STATE_DISCONNECTED)
                .when(mMockDeviceStateMachine)
=======
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void onConnectionStateChanged_DisconnectingToDisconnected_deviceCleanedUp() {
        doReturn(STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(STATE_DISCONNECTING, STATE_DISCONNECTED);
        assertThat(mDeviceMap.containsKey(mDevice)).isFalse();
    }

    // ACL state changes from AdapterService

    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void onAccountsChanged_fromNulltoEmpty_tryDownloadIfConnectedCalled() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        PbapClientService.PbapClientAccountManagerCallback callback =
                mService.new PbapClientAccountManagerCallback();
        callback.onAccountsChanged(null, new ArrayList<Account>());

        verify(sm).tryDownloadIfConnected();
    }

    @Test
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void onAccountsChanged_fromEmptyToOne_tryDownloadIfConnectedNotCalled() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        PbapClientService.PbapClientAccountManagerCallback callback =
                mService.new PbapClientAccountManagerCallback();
        Account acc = mock(Account.class);
        callback.onAccountsChanged(new ArrayList<Account>(), new ArrayList<>(Arrays.asList(acc)));

        verify(sm, never()).tryDownloadIfConnected();
    }

    // BOTH: ACL state changes from AdapterService

    // old

    @Test
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void aclDisconnected_withLeTransport_doesNotCallDisconnect() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState(mRemoteDevice)).thenReturn(BluetoothProfile.STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_LE);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());

        verify(sm, never()).disconnect(mRemoteDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void aclDisconnected_withBrEdrTransport_callsDisconnect() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState(mRemoteDevice)).thenReturn(BluetoothProfile.STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_BREDR);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());

        verify(sm).disconnect(mRemoteDevice);
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void onAccountsChanged_fromNullToEmpty_tryDownloadIfConnectedCalled() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        PbapClientService.PbapClientAccountManagerCallback callback =
                mService.new PbapClientAccountManagerCallback();
        callback.onAccountsChanged(null, new ArrayList<Account>());

        verify(sm).tryDownloadIfConnected();
    }

    @Test
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void onAccountsChanged_fromEmptyToOne_tryDownloadIfConnectedNotCalled() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        PbapClientService.PbapClientAccountManagerCallback callback =
                mService.new PbapClientAccountManagerCallback();
        Account acc = mock(Account.class);
        callback.onAccountsChanged(new ArrayList<Account>(), new ArrayList<>(Arrays.asList(acc)));

        verify(sm, never()).tryDownloadIfConnected();
    }

    // BOTH: ACL state changes from AdapterService

    // old

    @Test
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void aclDisconnected_withLeTransport_doesNotCallDisconnect() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState(mDevice)).thenReturn(STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        mService.aclDisconnected(mDevice, BluetoothDevice.TRANSPORT_LE);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());

        verify(sm, never()).disconnect(mDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void aclDisconnected_withBrEdrTransport_callsDisconnect() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState(mDevice)).thenReturn(STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        mService.aclDisconnected(mDevice, BluetoothDevice.TRANSPORT_BREDR);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());

        verify(sm).disconnect(mDevice);
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testOnBrEdrAclDisconnected_forConnectedDevice_deviceCleanedUp() {
        mService.aclDisconnected(mDevice, BluetoothDevice.TRANSPORT_BREDR);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());
        verify(mMockDeviceStateMachine, times(1)).disconnect();
    }

    @Test
    public void testOnBrEdrAclDisconnected_forDisconnectedDevice_eventDropped() {
        mDeviceMap.clear();
        mService.aclDisconnected(mDevice, BluetoothDevice.TRANSPORT_BREDR);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());
        verify(mMockDeviceStateMachine, never()).disconnect();
    }

    @Test
    public void testOnLeAclDisconnected_forConnectedDevice_eventDropped() {
        mService.aclDisconnected(mDevice, BluetoothDevice.TRANSPORT_LE);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());
        verify(mMockDeviceStateMachine, never()).disconnect();
    }

    @Test
    public void testOnUnknownAclDisconnected_forConnectedDevice_deviceCleanedUp() {
        mService.aclDisconnected(mDevice, TRANSPORT_UNKNOWN);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());
        verify(mMockDeviceStateMachine, never()).disconnect();
    }

    // HFP HF State changes

    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void headsetClientConnectionStateChanged_hfpCallLogIsRemoved() {
        mService.handleHeadsetClientConnectionStateChanged(
                mRemoteDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.STATE_DISCONNECTED);

        assertThat(mMockCallLogProvider.getMostRecentlyDeletedDevice())
                .isEqualTo(mRemoteDevice.getAddress());
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void headsetClientConnectionStateChanged_hfpCallLogIsRemoved() {
        mService.handleHeadsetClientConnectionStateChanged(
                mDevice, STATE_CONNECTED, STATE_DISCONNECTED);

        assertThat(mMockCallLogProvider.getMostRecentlyDeletedDevice())
                .isEqualTo(mDevice.getAddress());
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testOnHfpClientDisconnectedForConnectedDevice_callLogsCleanedUp() {
        mService.handleHeadsetClientConnectionStateChanged(
                mDevice, STATE_DISCONNECTING, STATE_DISCONNECTED);
        Account account = Utils.getAccountForDevice(mDevice);
        verify(mMockStorage, times(1)).removeCallHistory(eq(account));
    }

    @Test
    public void testOnHfpClientDisconnectedForDisconnectedDevice_callLogsCleanedUp() {
        mDeviceMap.clear();
        mService.handleHeadsetClientConnectionStateChanged(
                mDevice, STATE_DISCONNECTING, STATE_DISCONNECTED);
        Account account = Utils.getAccountForDevice(mDevice);
        verify(mMockStorage, times(1)).removeCallHistory(eq(account));
    }

    // SDP Events from AdapterService

    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void cleanUpDevice() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        mService.cleanupDevice(mRemoteDevice);

        assertThat(mService.mPbapClientStateMachineOldMap).doesNotContainKey(mRemoteDevice);
    }

    // NEW: SDP Events from AdapterService

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void cleanUpDevice() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        mService.cleanupDevice(mDevice);

        assertThat(mService.mPbapClientStateMachineOldMap).doesNotContainKey(mDevice);
    }

    // NEW: SDP Events from AdapterService

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testOnSdpRecordReceived_deviceConnected_eventForwarded() {
        mService.receiveSdpSearchRecord(
                mDevice, SDP_SUCCESS, mMockSdpRecord, BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, times(1))
                .onSdpResultReceived(eq(SDP_SUCCESS), any(PbapSdpRecord.class));
    }

    @Test
    public void testOnSdpResultReceived_deviceDisconnected_eventDropped() {
        mDeviceMap.clear();
        mService.receiveSdpSearchRecord(
                mDevice, SDP_SUCCESS, mMockSdpRecord, BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, never())
                .onSdpResultReceived(anyInt(), any(PbapSdpRecord.class));
    }

    @Test
    public void testOnSdpResultReceived_nullRecord_eventDropped() {
        mService.receiveSdpSearchRecord(mDevice, SDP_SUCCESS, null, BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, never())
                .onSdpResultReceived(anyInt(), any(PbapSdpRecord.class));
    }

    @Test
    public void testOnSdpResultReceived_wrongUuid_eventDropped() {
        mService.receiveSdpSearchRecord(
                mDevice, SDP_SUCCESS, mMockSdpRecord, /* wrong */ BluetoothUuid.MNS);
        verify(mMockDeviceStateMachine, never())
                .onSdpResultReceived(anyInt(), any(PbapSdpRecord.class));
    }

    @Test
    public void testOnSdpResultReceived_statusFailed_eventForwarded() {
        mService.receiveSdpSearchRecord(
                mDevice, SDP_FAILED, mMockSdpRecord, /* wrong */ BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, times(1))
                .onSdpResultReceived(eq(SDP_FAILED), any(PbapSdpRecord.class));
    }

    @Test
    public void testOnSdpResultReceived_statusBusy_eventForwarded() {
        mService.receiveSdpSearchRecord(
                mDevice, SDP_BUSY, mMockSdpRecord, /* wrong */ BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, times(1))
                .onSdpResultReceived(eq(SDP_BUSY), any(PbapSdpRecord.class));
    }

    // *********************************************************************************************
    // * API Methods
    // *********************************************************************************************

    // getPbapClientService (available) -> this
    @Test
    public void testGetService_serviceAvailable_returnsThis() {
        assertThat(PbapClientService.getPbapClientService()).isEqualTo(mService);
    }

    // getPbapClientService (unavailable) -> null
    @Test
    public void testGetService_serviceUnavailable_returnsNull() {
        mService.setAvailable(false);
        assertThat(PbapClientService.getPbapClientService()).isNull();
    }


    // connect (policy allowed) -> connect/true
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testConnect_onOld_onAllowedAndUnconnectedDevice_deviceCreatedAndIsConnecting() {
        mService.mPbapClientStateMachineOldMap.clear();
        assertThat(mService.connect(mRemoteDevice)).isTrue();

        // Clean up and wait for it to complete
        PbapClientStateMachineOld smOld = mService.mPbapClientStateMachineOldMap.get(mRemoteDevice);
        assertThat(smOld).isNotNull();
        smOld.doQuit();
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testConnect_onOld_onAllowedAndUnconnectedDevice_deviceCreatedAndIsConnecting() {
        mService.mPbapClientStateMachineOldMap.clear();
        assertThat(mService.connect(mDevice)).isTrue();

        // Clean up and wait for it to complete
        PbapClientStateMachineOld smOld = mService.mPbapClientStateMachineOldMap.get(mDevice);
        assertThat(smOld).isNotNull();
        smOld.doQuit();
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testConnect_onAllowedAndUnconnectedDevice_deviceCreatedAndIsConnecting() {
        mDeviceMap.clear();
        assertThat(mService.connect(mDevice)).isTrue();

        // Clean up and wait for it to complete
        PbapClientStateMachine sm = mDeviceMap.get(mDevice);
        assertThat(sm).isNotNull();

        Looper looper = sm.getHandler().getLooper();
        sm.disconnect();
        TestUtils.waitForLooperToFinishScheduledTask(looper);
    }

    // connect (device null) -> false
    @Test
    public void testConnect_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.connect(null));
    }

    // connect (policy forbidden) -> false
    @Test
    public void testConnect_onForbiddenAndUnconnectedDevice_deviceNotCreated() {
        mDeviceMap.clear();
        doReturn(CONNECTION_POLICY_FORBIDDEN)
                .when(mDatabaseManager)
                .getProfileConnectionPolicy(any(BluetoothDevice.class), anyInt());
        assertThat(mService.connect(mDevice)).isFalse();
        assertThat(mService.getConnectionState(mDevice)).isEqualTo(STATE_DISCONNECTED);
    }

    // connect (policy unknown) -> false
    @Test
    public void testConnect_onUnknownAndUnconnectedDevice_deviceNotCreated() {
        mDeviceMap.clear();
        doReturn(CONNECTION_POLICY_UNKNOWN)
                .when(mDatabaseManager)
                .getProfileConnectionPolicy(any(BluetoothDevice.class), anyInt());
        assertThat(mService.connect(mDevice)).isFalse();
    }

    // connect (already connected) -> false
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testConnect_onOld_onAllowedAndConnectedDevice_connectNotCalled() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);
        assertThat(mService.connect(mRemoteDevice)).isFalse();
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testConnect_onOld_onAllowedAndConnectedDevice_connectNotCalled() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);
        assertThat(mService.connect(mDevice)).isFalse();
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testConnect_onAllowedAndConnectedDevice_connectNotCalled() {
        // existing/previous connection setup in setUp()
        assertThat(mService.connect(mDevice)).isFalse();
    }

    // connect (at device limit) -> false
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void
            testConnect_onOld_donAllowedAndUnconnectedDeviceWithTenConnected_connectNotCalled() {
        // Create 10 connected devices
        for (int i = 1; i <= 10; i++) {
            BluetoothDevice remoteDevice = TestUtils.getTestDevice(mAdapter, i);
            PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
            mService.mPbapClientStateMachineOldMap.put(remoteDevice, sm);
        }

        assertThat(mService.connect(mRemoteDevice)).isFalse();
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void
            testConnect_onOld_donAllowedAndUnconnectedDeviceWithTenConnected_connectNotCalled() {
        // Create 10 connected devices
        for (int i = 1; i <= 10; i++) {
            BluetoothDevice remoteDevice = getTestDevice(i);
            PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
            mService.mPbapClientStateMachineOldMap.put(remoteDevice, sm);
        }

        assertThat(mService.connect(mDevice)).isFalse();
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testConnect_onAllowedAndUnconnectedDeviceWithTenConnected_connectNotCalled() {
        // Create 10 connected devices
        for (int i = 1; i <= 10; i++) {
            BluetoothDevice remoteDevice = getTestDevice(i);
            mDeviceMap.put(remoteDevice, mMockDeviceStateMachine);
        }

        assertThat(mService.connect(mDevice)).isFalse();
    }

    // disconnect (device connected) -> disconnect/true
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testDisconnect_onOld_onConnectedDevice_deviceDisconnectRequested() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState()).thenReturn(BluetoothProfile.STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);
        assertThat(mService.disconnect(mRemoteDevice)).isTrue();
        verify(sm, times(1)).disconnect(eq(mRemoteDevice));
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testDisconnect_onOld_onConnectedDevice_deviceDisconnectRequested() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState()).thenReturn(STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);
        assertThat(mService.disconnect(mDevice)).isTrue();
        verify(sm, times(1)).disconnect(eq(mDevice));
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testDisconnect_onConnectedDevice_deviceDisconnectRequested() {
        assertThat(mService.disconnect(mDevice)).isTrue();
        verify(mMockDeviceStateMachine, times(1)).disconnect();
    }

    // disconnect (device DNE) -> false
    @Test
    public void testDisconnect_onUnknownDevice_deviceNotCreatedAndDisconnectNotCalled() {
        mDeviceMap.clear();
        assertThat(mService.disconnect(mDevice)).isFalse();
    }

    // disconnect (device null) -> false
    @Test
    public void testDisconnect_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.disconnect(null));
    }

    // getConnectedDevices (device connected) -> has devices
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testGetConnectedDevices_onOld_oneDeviceConnected_returnsConnectedDevice() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState()).thenReturn(BluetoothProfile.STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        assertThat(mService.getConnectedDevices()).contains(mRemoteDevice);
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testGetConnectedDevices_onOld_oneDeviceConnected_returnsConnectedDevice() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState()).thenReturn(STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        assertThat(mService.getConnectedDevices()).contains(mDevice);
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testGetConnectedDevices_oneDeviceConnected_returnsConnectedDevice() {
        doReturn(STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getConnectedDevices())
                .isEqualTo(Arrays.asList(new BluetoothDevice[] {mDevice}));
    }

    // getConnectedDevices (no device connected) -> empty
    @Test
    public void testGetConnectedDevices_noDevicesConnected_returnsNoDevices() {
        doReturn(STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getConnectedDevices()).isEmpty();
    }

    // getDevicesMatchingConnectionStates (connected, one device connected)
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testGetDevicesMatchingConnectionStates_onOld_connectedWithDevice_returnsDevice() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState()).thenReturn(BluetoothProfile.STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        assertThat(
                        mService.getDevicesMatchingConnectionStates(
                                new int[] {BluetoothProfile.STATE_CONNECTED}))
                .isEqualTo(Arrays.asList(new BluetoothDevice[] {mRemoteDevice}));
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testGetDevicesMatchingConnectionStates_onOld_connectedWithDevice_returnsDevice() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState()).thenReturn(STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        assertThat(mService.getDevicesMatchingConnectionStates(new int[] {STATE_CONNECTED}))
                .isEqualTo(Arrays.asList(new BluetoothDevice[] {mDevice}));
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testGetDevicesMatchingConnectionStates_connectedWithDevice_returnsDevice() {
        doReturn(STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getDevicesMatchingConnectionStates(new int[] {STATE_CONNECTED}))
                .isEqualTo(Arrays.asList(new BluetoothDevice[] {mDevice}));
    }

    // getDevicesMatchingConnectionStates (connected, no device connected) -> empty
    @Test
    public void testGetDevicesMatchingConnectionStates_connectedWithNoDevice_returnsEmptyList() {
        doReturn(STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getDevicesMatchingConnectionStates(new int[] {STATE_CONNECTED}))
                .isEmpty();
    }

    // getConnectionState (device connected) -> has device
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testGetConnectionState_onOld_onConnectedDevice_returnsConnected() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState(eq(mRemoteDevice))).thenReturn(BluetoothProfile.STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);

        assertThat(mService.getConnectionState(mRemoteDevice))
                .isEqualTo(BluetoothProfile.STATE_CONNECTED);
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testGetConnectionState_onOld_onConnectedDevice_returnsConnected() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        when(sm.getConnectionState(eq(mDevice))).thenReturn(STATE_CONNECTED);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);

        assertThat(mService.getConnectionState(mDevice)).isEqualTo(STATE_CONNECTED);
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testGetConnectionState_onConnectedDevice_returnsConnected() {
        doReturn(STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getConnectionState(mDevice)).isEqualTo(STATE_CONNECTED);
    }

    // getConnectionState (device null) -> exception
    @Test
    public void testGetConnectionState_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.getConnectionState(null));
    }

    // getConnectionState (device DNE) -> disconnected
    @Test
    public void testGetConnectionState_onDeviceDoesNotExist_returnsDisconnected() {
        mDeviceMap.clear();
        assertThat(mService.getConnectionState(mDevice)).isEqualTo(STATE_DISCONNECTED);
    }

    // setConnectionPolicy (allowed -> connect) -> connect/true

    @Test
    public void testSetConnectionPolicy_toAllowed_connectIssued() {
        assertThat(mService.setConnectionPolicy(mDevice, CONNECTION_POLICY_ALLOWED)).isTrue();
    }

    // setConnectionPolicy (forbidden -> disconnect) -> discount/true
    @Test
<<<<<<< PATCH SET (f60b6c Remove PBAP Client storage refactor flag and old tests)
||||||| BASE
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testSetConnectionPolicy_onOld_toForbidden_disconnectIssued() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mRemoteDevice, sm);
        assertThat(
                        mService.setConnectionPolicy(
                                mRemoteDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                .isTrue();
        verify(sm, times(1)).disconnect(eq(mRemoteDevice));
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
=======
    @DisableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
    public void testSetConnectionPolicy_onOld_toForbidden_disconnectIssued() {
        PbapClientStateMachineOld sm = mock(PbapClientStateMachineOld.class);
        mService.mPbapClientStateMachineOldMap.put(mDevice, sm);
        assertThat(mService.setConnectionPolicy(mDevice, CONNECTION_POLICY_FORBIDDEN)).isTrue();
        verify(sm, times(1)).disconnect(eq(mDevice));
    }

    // new

    @Test
    @EnableFlags(Flags.FLAG_PBAP_CLIENT_STORAGE_REFACTOR)
>>>>>>> BASE      (99fb7d Merge "LE-Audio Software Offload: Allow Setup/Remove ISO Dat)
    public void testSetConnectionPolicy_toForbidden_disconnectIssued() {
        assertThat(mService.setConnectionPolicy(mDevice, CONNECTION_POLICY_FORBIDDEN)).isTrue();
        verify(mMockDeviceStateMachine, times(1)).disconnect();
    }

    // setConnectionPolicy (device null) -> exception
    @Test
    public void testSetConnectionPolicy_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mService.setConnectionPolicy(null, CONNECTION_POLICY_ALLOWED));
    }

    // setConnectionPolicy (database call fails) -> false
    @Test
    public void testSetConnectionPolicy_databaseCallFails_returnsFalse() {
        doReturn(false)
                .when(mDatabaseManager)
                .setProfileConnectionPolicy(any(BluetoothDevice.class), anyInt(), anyInt());
        assertThat(mService.setConnectionPolicy(mDevice, CONNECTION_POLICY_ALLOWED)).isFalse();
    }

    // getConnectionPolicy -> returns what we set in setup() (allowed)
    @Test
    public void testGetConnectionPolicy_onKnownDevice_returnsAllowed() {
        assertThat(mService.getConnectionPolicy(mDevice)).isEqualTo(CONNECTION_POLICY_ALLOWED);
    }

    // getConnectionPolicy (device null) -> exception
    @Test
    public void testGetConnectionPolicy_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.getConnectionPolicy(null));
    }

    // *********************************************************************************************
    // * Debug/Dump/toString()
    // *********************************************************************************************

    @Test
    public void testDump() {
        StringBuilder sb = new StringBuilder();
        mService.dump(sb);
        String dumpContents = sb.toString();
        assertThat(dumpContents).isNotNull();
        assertThat(dumpContents.length()).isNotEqualTo(0);
    }
}
