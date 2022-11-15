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

package com.android.bluetooth.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CallInfoTest {

    private static final String TEST_ACCOUNT_ADDRESS = "//foo.com/";
    private static final int TEST_ACCOUNT_INDEX = 0;

    @Rule
    public final ServiceTestRule mServiceRule
            = ServiceTestRule.withTimeout(1, TimeUnit.SECONDS);

    @Mock
    private TelecomManager mMockTelecomManager;

    private TestableBluetoothInCallService mBluetoothInCallService;
    private BluetoothInCallService.CallInfo mMockCallInfo;

    public class TestableBluetoothInCallService extends BluetoothInCallService {
        @Override
        public IBinder onBind(Intent intent) {
            IBinder binder = super.onBind(intent);
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothAdapterReceiver, intentFilter);
            mTelephonyManager = getSystemService(TelephonyManager.class);
            mTelecomManager = getSystemService(TelecomManager.class);
            return binder;
        }

        @Override
        protected void enforceModifyPermission() {
        }

        protected void setOnCreateCalled(boolean called) {
            mOnCreateCalled = called;
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        InstrumentationRegistry.getInstrumentation().getUiAutomation().adoptShellPermissionIdentity();

        // Create the service Intent.
        Intent serviceIntent =
                new Intent(ApplicationProvider.getApplicationContext(),
                        BluetoothInCallServiceTest.TestableBluetoothInCallService.class);
        // Bind the service
        mServiceRule.bindService(serviceIntent);

        mBluetoothInCallService = new TestableBluetoothInCallService();
        mMockCallInfo = spy(mBluetoothInCallService.new CallInfo());
    }

    @After
    public void tearDown() throws Exception {
        mServiceRule.unbindService();
        mBluetoothInCallService = null;
    }

    @Test
    public void getBluetoothCalls() {
        assertThat(mMockCallInfo.getBluetoothCalls()).isEmpty();
    }

    @Test
    public void getActiveCall() {
        BluetoothCall activeCall = getMockCall();
        when(activeCall.getState()).thenReturn(Call.STATE_ACTIVE);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(activeCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getActiveCall()).isEqualTo(activeCall);
    }

    @Test
    public void getHeldCall() {
        BluetoothCall heldCall = getMockCall();
        when(heldCall.getState()).thenReturn(Call.STATE_HOLDING);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(heldCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getHeldCall()).isEqualTo(heldCall);
        assertThat(mMockCallInfo.getNumHeldCalls()).isEqualTo(1);
    }

    @Test
    public void getOutgoingCall() {
        BluetoothCall outgoingCall = getMockCall();
        when(outgoingCall.getState()).thenReturn(Call.STATE_PULLING_CALL);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(outgoingCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getOutgoingCall()).isEqualTo(outgoingCall);
    }

    @Test
    public void getRingingOrSimulatedRingingCall() {
        BluetoothCall ringingCall = getMockCall();
        when(ringingCall.getState()).thenReturn(Call.STATE_SIMULATED_RINGING);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(ringingCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getRingingOrSimulatedRingingCall()).isEqualTo(ringingCall);
    }

    @Test
    public void hasOnlyDisconnectedCalls_withNoCalls() {
        assertThat(mMockCallInfo.getBluetoothCalls()).isEmpty();

        assertThat(mMockCallInfo.hasOnlyDisconnectedCalls()).isFalse();
    }

    @Test
    public void hasOnlyDisconnectedCalls_withConnectedCall() {
        BluetoothCall activeCall = getMockCall();
        when(activeCall.getState()).thenReturn(Call.STATE_ACTIVE);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(activeCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.hasOnlyDisconnectedCalls()).isFalse();
    }

    @Test
    public void hasOnlyDisconnectedCalls_withDisconnectedCallOnly() {
        BluetoothCall disconnectedCall = getMockCall();
        when(disconnectedCall.getState()).thenReturn(Call.STATE_DISCONNECTED);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(disconnectedCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.hasOnlyDisconnectedCalls()).isTrue();
    }

    @Test
    public void getForegroundCall_withConnectingCall() {
        BluetoothCall connectingCall = getMockCall();
        when(connectingCall.getState()).thenReturn(Call.STATE_CONNECTING);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(connectingCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getForegroundCall()).isEqualTo(connectingCall);
    }

    @Test
    public void getForegroundCall_withPullingCall() {
        BluetoothCall pullingCall = getMockCall();
        when(pullingCall.getState()).thenReturn(Call.STATE_PULLING_CALL);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(pullingCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getForegroundCall()).isEqualTo(pullingCall);
    }

    @Test
    public void getForegroundCall_withRingingCall() {
        BluetoothCall ringingCall = getMockCall();
        when(ringingCall.getState()).thenReturn(Call.STATE_CONNECTING);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(ringingCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getForegroundCall()).isEqualTo(ringingCall);
    }

    @Test
    public void getForegroundCall_withNoMatchingCall() {
        BluetoothCall disconnectedCall = getMockCall();
        when(disconnectedCall.getState()).thenReturn(Call.STATE_DISCONNECTED);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(disconnectedCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getForegroundCall()).isNull();
    }

    @Test
    public void getCallByState_withNoMatchingCall() {
        BluetoothCall activeCall = getMockCall();
        when(activeCall.getState()).thenReturn(Call.STATE_ACTIVE);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(activeCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getCallByState(Call.STATE_HOLDING)).isNull();
    }

    @Test
    public void getCallByStates_withNoMatchingCall() {
        LinkedHashSet<Integer> states = new LinkedHashSet<>();
        states.add(Call.STATE_CONNECTING);
        BluetoothCall activeCall = getMockCall();
        when(activeCall.getState()).thenReturn(Call.STATE_ACTIVE);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(activeCall);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getCallByStates(states)).isNull();
    }

    @Test
    public void getCallByCallId() {
        BluetoothCall call = getMockCall();
        UUID uuid = UUID.randomUUID();
        when(call.getTbsCallId()).thenReturn(uuid);
        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(call);

        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        assertThat(mMockCallInfo.getCallByCallId(uuid)).isEqualTo(call);
    }

    @Test
    public void getCallByCallId_withNoCalls() {
        UUID uuid = UUID.randomUUID();
        assertThat(mMockCallInfo.getBluetoothCalls()).isEmpty();

        assertThat(mMockCallInfo.getCallByCallId(uuid)).isNull();
    }

    @Test
    public void getBestPhoneAccount() {
        BluetoothCall foregroundCall = getMockCall();
        when(foregroundCall.getState()).thenReturn(Call.STATE_DIALING);
        when(foregroundCall.getAccountHandle()).thenReturn(null);

        ArrayList<BluetoothCall> calls = new ArrayList<>();
        calls.add(foregroundCall);
        doReturn(calls).when(mMockCallInfo).getBluetoothCalls();

        List<PhoneAccountHandle> handles = new ArrayList<>();
        PhoneAccountHandle testHandle = makeQuickAccountHandle("id0");
        handles.add(testHandle);
        when(mMockTelecomManager.getPhoneAccountsSupportingScheme(
                PhoneAccount.SCHEME_TEL)).thenReturn(handles);

        PhoneAccount fakePhoneAccount = makeQuickAccount("id0", TEST_ACCOUNT_INDEX);
        when(mMockTelecomManager.getPhoneAccount(testHandle)).thenReturn(fakePhoneAccount);
        mBluetoothInCallService.mTelecomManager = mMockTelecomManager;

        assertThat(mMockCallInfo.getBestPhoneAccount()).isEqualTo(fakePhoneAccount);
    }

    private static ComponentName makeQuickConnectionServiceComponentName() {
        return new ComponentName("com.android.server.telecom.tests",
                "com.android.server.telecom.tests.MockConnectionService");
    }

    private static PhoneAccountHandle makeQuickAccountHandle(String id) {
        return new PhoneAccountHandle(makeQuickConnectionServiceComponentName(), id,
                Binder.getCallingUserHandle());
    }

    private PhoneAccount.Builder makeQuickAccountBuilder(String id, int idx) {
        return new PhoneAccount.Builder(makeQuickAccountHandle(id), "label" + idx);
    }

    private PhoneAccount makeQuickAccount(String id, int idx) {
        return makeQuickAccountBuilder(id, idx)
                .setAddress(Uri.parse(TEST_ACCOUNT_ADDRESS + idx))
                .setSubscriptionAddress(Uri.parse("tel:555-000" + idx))
                .setCapabilities(idx)
                .setShortDescription("desc" + idx)
                .setIsEnabled(true)
                .build();
    }

    private BluetoothCall getMockCall() {
        return mock(com.android.bluetooth.telephony.BluetoothCall.class);
    }
}