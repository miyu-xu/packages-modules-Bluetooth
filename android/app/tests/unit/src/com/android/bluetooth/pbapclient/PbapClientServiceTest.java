/*
 * Copyright 2018 The Android Open Source Project
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
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import android.content.pm.PackageManager;
import android.os.Looper;
import android.provider.CallLog;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;

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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapClientServiceTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String ACCOUNT_TYPE = "com.android.bluetooth.pbapclient.account";
    private static final String REMOTE_DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF";

    // Constants for SDP. Note that these values come from the native stack, but no centralized
    // constants exist for them as part of the various SDP APIs.
    public static final int SDP_SUCCESS = 0;
    public static final int SDP_FAILED = 1;
    public static final int SDP_BUSY = 2;

    // Constant for testing ACL disconnection events with a bad transport
    public static final int TRANSPORT_UNKNOWN = -1;

    private PbapClientService mService = null;
    private BluetoothAdapter mAdapter = null;
    private BluetoothDevice mRemoteDevice;
    private PbapClientService.PbapClientStateMachineCallback mDeviceCallback;

    @Mock private Context mMockContext;
    @Mock private AdapterService mAdapterService;
    @Mock private DatabaseManager mDatabaseManager;
    @Mock private PackageManager mMockPackageManager;
    @Mock private ContentResolver mMockContentResolver;
    @Mock private PbapClientContactsStorage mMockStorage;
    private Map<BluetoothDevice, PbapClientStateMachine> mMockDeviceMap = new HashMap<BluetoothDevice, PbapClientStateMachine>();
    @Mock private PbapClientStateMachine mMockDeviceStateMachine;
    @Mock private SdpPseRecord mMockSdpRecord;

    @Before
    public void setUp() throws Exception {
        TestUtils.setAdapterService(mAdapterService);

        doReturn(mMockPackageManager).when(mMockContext).getPackageManager();
        doReturn("").when(mMockContext).getPackageName();
        doReturn(mMockContentResolver).when(mMockContext).getContentResolver();

        doReturn(mDatabaseManager).when(mAdapterService).getDatabase();
        doReturn(true).when(mDatabaseManager).setProfileConnectionPolicy(any(BluetoothDevice.class), anyInt(), anyInt());
        doReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED).when(mDatabaseManager).getProfileConnectionPolicy(any(BluetoothDevice.class), anyInt());

        doAnswer(invocation -> {
            BluetoothDevice device = (BluetoothDevice) invocation.getArgument(0);
            return getAccountForDevice(device);
        }).when(mMockStorage).getStorageAccountForDevice(any(BluetoothDevice.class));

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);
        mRemoteDevice = mAdapter.getRemoteDevice(REMOTE_DEVICE_ADDRESS);

        mMockDeviceMap.put(mRemoteDevice, mMockDeviceStateMachine);

        mService = new PbapClientService(mMockContext, mMockStorage, mMockDeviceMap);
        mService.start();
        mService.setAvailable(true);

        mDeviceCallback = mService.new PbapClientStateMachineCallback(mRemoteDevice);
    }

    @After
    public void tearDown() throws Exception {
        if (mService != null) {
            mService.stop();
            mService = PbapClientService.getPbapClientService();
            Assert.assertNull(mService);
        }
        TestUtils.clearAdapterService(mAdapterService);
    }

    //********************************************************************************************//
    // Incoming Events
    //********************************************************************************************//

    // PbapClientStateMachineCallback events from devices

    @Test
    public void onConnectionStateChanged_DisconnectedToConnecting_eventIgnored() {
        doReturn(BluetoothProfile.STATE_CONNECTING).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_CONNECTING);
        assertThat(mMockDeviceMap.containsKey(mRemoteDevice)).isTrue();
    }

    @Test
    public void onConnectionStateChanged_ConnectingToConnected_eventIgnored() {
        doReturn(BluetoothProfile.STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_CONNECTING);
        assertThat(mMockDeviceMap.containsKey(mRemoteDevice)).isTrue();
    }

    @Test
    public void onConnectionStateChanged_ConnectingToDisconnected_deviceCleanedUp() {
        doReturn(BluetoothProfile.STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_DISCONNECTED);
        assertThat(mMockDeviceMap.containsKey(mRemoteDevice)).isFalse();
    }

    @Test
    public void onConnectionStateChanged_ConnectedToDisonnecting_eventIgnored() {
        doReturn(BluetoothProfile.STATE_DISCONNECTING).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_DISCONNECTING);
        assertThat(mMockDeviceMap.containsKey(mRemoteDevice)).isTrue();
    }

    @Test
    public void onConnectionStateChanged_DisconnectingToDisonnected_deviceCleanedUp() {
        doReturn(BluetoothProfile.STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        mDeviceCallback.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED);
        assertThat(mMockDeviceMap.containsKey(mRemoteDevice)).isFalse();
    }

    // ACL state changes from AdapterService

    @Test
    public void testOnBrEdrAclDisconnectedForConnectedDevice_deviceCleanedUp() {
        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_BREDR);
        verify(mMockDeviceStateMachine, times(1)).disconnect();
    }

    @Test
    public void testOnBrEdrAclDisconnectedForDisconnectedDevice_eventDropped() {
        mMockDeviceMap.clear();
        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_BREDR);
        verify(mMockDeviceStateMachine, never()).disconnect();
    }

    @Test
    public void testOnLeAclDisconnectedForConnectedDevice_eventDropped() {
        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_LE);
        verify(mMockDeviceStateMachine, never()).disconnect();
    }

    @Test
    public void testOnUnknownAclDisconnectedForConnectedDevice_deviceCleanedUp() {
        mService.aclDisconnected(mRemoteDevice, TRANSPORT_UNKNOWN);
        verify(mMockDeviceStateMachine, never()).disconnect();
    }

    // HFP HF State changes

    @Test
    public void testOnHfpClientDisconnectedForConnectedDevice_callLogsCleanedUp() {
        mService.handleHeadsetClientConnectionStateChanged(mRemoteDevice, BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED);
        Account account = getAccountForDevice(mRemoteDevice);
        verify(mMockStorage, times(1)).removeCallHistory(eq(account));
    }

    @Test
    public void testOnHfpClientDisconnectedForDisconnectedDevice_callLogsCleanedUp() {
        mMockDeviceMap.clear();
        mService.handleHeadsetClientConnectionStateChanged(mRemoteDevice, BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED);
        Account account = getAccountForDevice(mRemoteDevice);
        verify(mMockStorage, times(1)).removeCallHistory(eq(account));
    }

    // SDP Events from AdapterService

    @Test
    public void testOnSdpRecordReceivedForConnectedDevice_recordForwarded() {
        mService.receiveSdpSearchRecord(mRemoteDevice, SDP_SUCCESS, mMockSdpRecord, BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, times(1)).onSdpRecordReceived(any(PbapSdpRecord.class));
    }

    @Test
    public void testOnSdpRecordReceivedForDisconnectedDevice_recordDropped() {
        mMockDeviceMap.clear();
        mService.receiveSdpSearchRecord(mRemoteDevice, SDP_SUCCESS, mMockSdpRecord, BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, never()).onSdpRecordReceived(any(PbapSdpRecord.class));
    }

    @Test
    public void testOnNullSdpRecordReceivedForConnectedDevice_recordDropped() {
        mService.receiveSdpSearchRecord(mRemoteDevice, SDP_SUCCESS, null, BluetoothUuid.PBAP_PSE);
        verify(mMockDeviceStateMachine, never()).onSdpRecordReceived(any(PbapSdpRecord.class));
    }

    @Test
    public void testOnWrongSdpRecordReceivedForConnectedDevice_recordDropped() {
        mService.receiveSdpSearchRecord(mRemoteDevice, SDP_SUCCESS, mMockSdpRecord, /* wrong */ BluetoothUuid.MNS);
        verify(mMockDeviceStateMachine, never()).onSdpRecordReceived(any(PbapSdpRecord.class));
    }

    //********************************************************************************************//
    // API Methods
    //********************************************************************************************//

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


    // getPbapClientService (unset after stop) -> null
    @Test
    public void testGetService_serviceStopped_returnsNull() {
        mService.stop();
        mService = null;
        assertThat(PbapClientService.getPbapClientService()).isNull();
    }

    // connect (policy allowed) -> connect/true
    @Test
    public void testConnect_onAllowedAndUnconnectedDevice_deviceCreatedAndIsConnecting() {
        mMockDeviceMap.clear();
        assertThat(mService.connect(mRemoteDevice)).isTrue();

        // trying to make and connect a real device here is uuuugly. Do we want a
        // factory pattern for testing instead of the injected map?
        // If not, the test will have to carefully shutdown the device state machine
        // when its done.

        // verify(mMockDeviceStateMachine, times(1)).start();
        // verify(mMockDeviceStateMachine, times(1)).connect();
    }

    // connect (device null) -> false
    @Test
    public void testConnect_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.connect(null));
    }

    // connect (policy forbidden) -> false
    @Test
    public void testConnect_onForbiddenAndUnconnectedDevice_deviceNotCreated() {
        mMockDeviceMap.clear();
        doReturn(BluetoothProfile.CONNECTION_POLICY_FORBIDDEN).when(mDatabaseManager).getProfileConnectionPolicy(any(BluetoothDevice.class), anyInt());
        assertThat(mService.connect(mRemoteDevice)).isFalse();
        assertThat(mService.getConnectionState(mRemoteDevice)).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    // connect (policy unknown) -> false
    @Test
    public void testConnect_onUnknownAndUnconnectedDevice_deviceNotCreated() {
        mMockDeviceMap.clear();
        doReturn(BluetoothProfile.CONNECTION_POLICY_UNKNOWN).when(mDatabaseManager).getProfileConnectionPolicy(any(BluetoothDevice.class), anyInt());
        assertThat(mService.connect(mRemoteDevice)).isFalse();
    }

    // connect (already connected) -> false
    @Test
    public void testConnect_onAllowedAndConnectedDevice_connectNotCalled() {
        // existing/previous connection setup in setUp()
        assertThat(mService.connect(mRemoteDevice)).isFalse();
    }

    // connect (at device limit) -> false
    @Test
    public void testConnect_onAllowedAndUnconnectedDeviceWithTenConnected_connectNotCalled() {
        // Create 10 connected devices
        for(int i = 1; i <= 10; i++) {
            BluetoothDevice remoteDevice = TestUtils.getTestDevice(mAdapter, i);
            mMockDeviceMap.put(remoteDevice, mMockDeviceStateMachine);
        }

        assertThat(mService.connect(mRemoteDevice)).isFalse();
    }

    // disconnect (device connected) -> disconnect/true
    @Test
    public void testDisconnect_onConnectedDevice_deviceDisconnectRequested() {
        assertThat(mService.disconnect(mRemoteDevice)).isTrue();
        verify(mMockDeviceStateMachine, times(1)).disconnect();
    }

    // disconnect (device DNE) -> false
    @Test
    public void testDisconnect_onUnknownDevice_deviceNotCreatedAndDisconnectNotCalled() {
        mMockDeviceMap.clear();
        assertThat(mService.disconnect(mRemoteDevice)).isFalse();
    }

    // disconnect (device null) -> false
    @Test
    public void testDisconnect_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.disconnect(null));
    }

    // getConnectedDevices (device connected) -> has devices
    @Test
    public void testGetConnectedDevices_oneDeviceConnected_returnsConnectedDevice() {
        doReturn(BluetoothProfile.STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getConnectedDevices()).isEqualTo(Arrays.asList(new BluetoothDevice[] {mRemoteDevice}));
    }

    // getConnectedDevices (no device connected) -> empty
    @Test
    public void testGetConnectedDevices_noDevicesConnected_returnsNoDevices() {
        doReturn(BluetoothProfile.STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getConnectedDevices()).isEmpty();
    }

    // getDevicesMatchingConnectionStates (connected, one device connected)
    @Test
    public void testGetDevicesMatchingConnectionStates_connectedWithDevice_returnsDevice() {
        doReturn(BluetoothProfile.STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getDevicesMatchingConnectionStates(new int[]{BluetoothProfile.STATE_CONNECTED})).isEqualTo(Arrays.asList(new BluetoothDevice[] {mRemoteDevice}));
    }

    // getDevicesMatchingConnectionStates (connected, no device connected) -> empty
    @Test
    public void testGetDevicesMatchingConnectionStates_connectedWithNoDevice_returnsEmptyList() {
        doReturn(BluetoothProfile.STATE_DISCONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getDevicesMatchingConnectionStates(new int[]{BluetoothProfile.STATE_CONNECTED})).isEmpty();
    }

    // getConnectionState (device connected) -> has device
    @Test
    public void testGetConnectionState_onConnectedDevice_returnsConnected() {
        doReturn(BluetoothProfile.STATE_CONNECTED).when(mMockDeviceStateMachine).getConnectionState();
        assertThat(mService.getConnectionState(mRemoteDevice)).isEqualTo(BluetoothProfile.STATE_CONNECTED);
    }

    // getConnectionState (device null) -> exception
    @Test
    public void testGetConnectionState_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.getConnectionState(null));
    }

    // getConnectionState (device DNE) -> disconnected
    @Test
    public void testGetConnectionState_onDevice_returns() {
        mMockDeviceMap.clear();
        assertThat(mService.getConnectionState(mRemoteDevice)).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    // setConnectionPolicy (allowed -> connect) -> connect/true
    @Test
    public void testSetConnectionPolicy_toAllowed_whateverSet() {
        assertThat(mService.setConnectionPolicy(mRemoteDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED)).isTrue();
    }

    // setConnectionPolicy (device null) -> exception
    @Test
    public void testSetConnectionPolicy_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.setConnectionPolicy(null, BluetoothProfile.CONNECTION_POLICY_ALLOWED));
    }

    // setConnectionPolicy (database call fails) -> false
    @Test
    public void testSetConnectionPolicy_databaseCallFails_whateverSet() {
        doReturn(false).when(mDatabaseManager).setProfileConnectionPolicy(any(BluetoothDevice.class), anyInt(), anyInt());
        assertThat(mService.setConnectionPolicy(mRemoteDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED)).isFalse();
    }

    // setConnectionPolicy (forbidden -> disconnect) -> discount/true
    @Test
    public void testSetConnectionPolicy_device_whateverSet() {
        assertThat(mService.setConnectionPolicy(mRemoteDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN)).isTrue();
        verify(mMockDeviceStateMachine, times(1)).disconnect();
    }

    // getConnectionPolicy -> returns what we set in setup() (allowed)
    @Test
    public void testGetConnectionPolicy() {
        assertThat(mService.getConnectionPolicy(mRemoteDevice)).isEqualTo(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
    }

    // getConnectionPolicy (device null) -> exception
    @Test
    public void testGetConnectionPolicy_onNullDevice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mService.getConnectionPolicy(null));
    }

    //********************************************************************************************//
    // Debug/Dump/toString()
    //********************************************************************************************//

    @Test
    public void testAclTransportToString_transportAuto_returnsNonNull() {
        assertThat(mService.aclTransportToString(BluetoothDevice.TRANSPORT_AUTO)).isNotNull();
    }

    @Test
    public void testAclTransportToString_transportBrEdr_returnsNonNull() {
        assertThat(mService.aclTransportToString(BluetoothDevice.TRANSPORT_BREDR)).isNotNull();
    }

    @Test
    public void testAclTransportToString_transportLe_returnsNonNull() {
        assertThat(mService.aclTransportToString(BluetoothDevice.TRANSPORT_LE)).isNotNull();
    }

    @Test
    public void testAclTransportToString_transportUnknown_returnsNonNull() {
        assertThat(mService.aclTransportToString(TRANSPORT_UNKNOWN)).isNotNull();
    }

    @Test
    public void testDump() {
        StringBuilder sb = new StringBuilder();
        mService.dump(sb);
        String dumpContents = sb.toString();
        assertThat(dumpContents).isNotNull();
        assertThat(dumpContents.length()).isNotEqualTo(0);
    }

    //********************************************************************************************//
    // Utilities
    //********************************************************************************************//

    private Account getAccountForDevice(BluetoothDevice device) {
        return new Account(device.getAddress(), ACCOUNT_TYPE);
    }
}
