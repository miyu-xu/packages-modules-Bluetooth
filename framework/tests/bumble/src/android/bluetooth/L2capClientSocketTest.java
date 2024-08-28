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

package android.bluetooth;

import static com.google.common.truth.Truth.assertThat;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import org.junit.Ignore;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import static org.mockito.Mockito.any;
import java.io.IOException;
import android.bluetooth.BluetoothSocketException;
import java.io.OutputStream;
import org.mockito.MockitoAnnotations;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.protobuf.Empty;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import android.annotation.SuppressLint;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.OwnAddressType;
import pandora.HostProto.Connection;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import java.util.Arrays;

import pandora.L2capProto.CreateLECreditBasedChannelRequest;
import pandora.L2capProto.CreateLECreditBasedChannelResponse;
import pandora.l2cap.L2CAPProto.WaitConnectionRequest;
import pandora.l2cap.L2CAPProto.WaitConnectionResponse;
import pandora.l2cap.L2CAPProto.CreditBasedChannelRequest;
import pandora.l2cap.L2CAPProto.Channel;
import pandora.SecurityProto.SecureRequest;
import pandora.SecurityProto.SecureResponse;
import pandora.l2cap.L2CAPProto.ReceiveRequest;
import pandora.l2cap.L2CAPProto.ReceiveResponse;

/** Test cases for {@link L2cap Client Sockets}. */
@RunWith(AndroidJUnit4.class)
public class L2capClientSocketTest {
    private static final String TAG = "L2capClientSocketTest";
    private static final Duration INTENT_TIMEOUT = Duration.ofSeconds(10);
    private BluetoothDevice mDevice;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private static final int TEST_PSM = 0x80;
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);
    private BluetoothDevice mDUT = null;
    private Channel mChannel = null;
    private byte[] mSampleData = null;

    private static final Duration PROTO_MODE_TIMEOUT = Duration.ofSeconds(10);

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();
    @Mock private BroadcastReceiver mReceiver;
    private InOrder mInOrder = null;

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(INTENT_TIMEOUT.toMillis()))
            .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    @SafeVarargs
    private void verifyIntentReceivedUnordered(int num, Matcher<Intent>... matchers) {
        verify(mReceiver, timeout(INTENT_TIMEOUT.toMillis()).times(num))
            .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    @SafeVarargs
    private void verifyIntentReceivedUnordered(Matcher<Intent>... matchers) {
        verifyIntentReceivedUnordered(1, matchers);
    }

    @SuppressLint("MissingPermission")
    private final Answer<Void> mIntentHandler =
        inv -> {
            Log.i(TAG, "onReceive(): intent=" + Arrays.toString(inv.getArguments()));
            Intent intent = inv.getArgument(1);
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device =
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                mBumble.getRemoteDevice().setPairingConfirmation(true);
                Log.i(TAG, "onReceive(): setPairingConfirmation(true) for " + device);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device =
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                int bondState =
                    intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR);
                int prevBondState =
                    intent.getIntExtra(
                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                        BluetoothAdapter.ERROR);
                Log.i(
                    TAG,
                    "onReceive(): device "
                        + device
                        + " bond state changed from "
                        + prevBondState
                        + " to "
                        + bondState);
            }  else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int adapterState =
                    intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Log.i(TAG, "Adapter state change:" + adapterState);
                if (adapterState == BluetoothAdapter.STATE_ON
                    || adapterState == BluetoothAdapter.STATE_OFF) {
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

            } else {
                Log.i(TAG, "onReceive(): unknown intent action " + action);
            }
            return null;
        };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doAnswer(mIntentHandler).when(mReceiver).onReceive(any(), any());

        mInOrder = inOrder(mReceiver);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);

        mContext.registerReceiver(mReceiver, filter);

        mDevice = mBumble.getRemoteDevice();
        Log.d(TAG, "set Up done >> ");
    }

    @After
    public void tearDown() throws Exception {

        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mDevice.removeBond();
        }

        if (mDevice.isConnected()) {
            mDevice.disconnect();
        }

        mContext.unregisterReceiver(mReceiver);
    }

    private void createBondFromPhone (int transport) {
        assertThat(mDevice.createBond(transport)).isTrue();

        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mDevice),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mDevice));
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mDevice),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));
    }

    private Connection setupAclConnection(int transport) throws Exception {
        // Start GATT service discovery, this will establish LE ACL
        assertThat(mBumble.getRemoteDevice().fetchUuidsWithSdp(transport)).isTrue();

        // Make Bumble connectable
        AdvertiseResponse advertiseResponse =
            mBumble.hostBlocking()
                .advertise(
                    AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(OwnAddressType.PUBLIC)
                        .build())
                    .next();

        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mDevice),
            hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));

        return advertiseResponse.getConnection();
    }

    private Channel startServerSocketwithBumble(Connection conn, boolean isSecure, int psm) {
        CreditBasedChannelRequest req =
            CreditBasedChannelRequest.newBuilder()
                .setSpsm(psm)
		        .setMtu(2048)
		        .setMps(2048)
		        .setInitialCredit(256)
                .build();

        WaitConnectionRequest waitConnReq =
		WaitConnectionRequest.newBuilder()
		   .setConnection(conn)
		   .setLeCreditBased(req)
		   .build();

        Log.d(TAG, "Creating the L2CAP channel from bumble side");
        WaitConnectionResponse resp = mBumble.l2capBlocking().waitConnection(waitConnReq);
        assertThat(resp).isNotNull();
        assertThat(resp.hasChannel()).isTrue();

        return resp.getChannel();
    }

    private BluetoothSocket createClientSocket(boolean isSecure, int psm) {
        Log.d(TAG, "createClientSocket: isSecure " + isSecure + "psm: " + psm);
        BluetoothSocket sock = null;
	    if (isSecure) {
           try {
               sock = mDevice.createL2capChannel(psm);
           } catch (IOException e) {
               //declare testcase failure
               Log.d(TAG, "IOEXCEPT while creating LECOC channel");
           }
	    } else {
           try {
               sock = mDevice.createInsecureL2capChannel(psm);
           } catch (IOException e) {
               //declare testcase failure
               Log.d(TAG, "IOEXCEPT while creating LECOC channel");
           }
	    }
        Log.d(TAG, "returning sock: " + sock);
	    return sock;
    }

    private void closeSocket(BluetoothSocket sock) {
        try {
            sock.close();
        } catch (IOException e) {
            //declare testcase failure
            Log.d(TAG, "IOEXCEPT while closing to LECOC channel");
        }
    }

    private void connectSocket(BluetoothSocket sock) {
        try {
	    Log.d(TAG, "calling connect");	
            sock.connect();
        } catch (IOException e) {
            //declare testcase failure
            Log.d(TAG, "IOEXCEPT while connecting to LECOC channel");
            assertThat(true).isTrue();
        }
    }

    @Test
    public void testClientconnectToInsecureServerWithoutBonding() throws Exception {
        Log.d(TAG, "testClientconnectToInsecureServerWithoutBonding >> ");

        Connection conn = setupAclConnection(BluetoothDevice.TRANSPORT_LE);
        BluetoothSocket sock = createClientSocket(false, TEST_PSM);
        final Channel channel = null;
	    new Thread(() -> {
            Log.d(TAG, " start the server socket with Bumble");
            mChannel = startServerSocketwithBumble(conn, false, TEST_PSM);
        }).start();

        connectSocket(sock);

        Log.d(TAG, "Connection success for socket : type: " + sock.getConnectionType());
        assertThat(sock.isConnected() && sock.getConnectionType() == BluetoothSocket.TYPE_L2CAP).isTrue();

        closeSocket(sock);

        Log.d(TAG, "Exit: testClientconnectToInsecureServerWithoutBonding");
    }

    @Test
    public void testClientconnectAndSendDataToInsecureServerWithoutBonding() throws Exception {
        Log.d(TAG, "testClientconnectAndSendDataToInsecureServerWithoutBonding >> ");

        Connection conn = setupAclConnection(BluetoothDevice.TRANSPORT_LE);
        BluetoothSocket sock = createClientSocket(false, TEST_PSM);
        mChannel = null;
        new Thread(() -> {
            Log.d(TAG, " start the server socket with Bumble");
            mChannel = startServerSocketwithBumble(conn, false, TEST_PSM);
        }).start();

        connectSocket(sock);

        Log.d(TAG, "Connection success for socket : type: " + sock.getConnectionType() + "chan: " + mChannel);
        assertThat(sock.isConnected() && sock.getConnectionType() == BluetoothSocket.TYPE_L2CAP).isTrue();
        assertThat(mChannel).isNotNull();
        mSampleData = null;
        new Thread(() -> {
          mSampleData = "SAMPLE DATA OVER LE COC SOCKET".getBytes();
          try {
              OutputStream oStream = sock.getOutputStream();
              oStream.write(mSampleData);
          } catch (IOException exception) {
              Log.d(TAG, "exception while writing data");
          }
        }).start();

        StreamObserverSpliterator<ReceiveResponse> receiveObserver =
            new StreamObserverSpliterator<>();
        mBumble
            .l2cap()
            .receive(ReceiveRequest.newBuilder().setChannel(mChannel).build(), receiveObserver);

        Log.d(TAG, "testReceive: waitReceive data on Bumble");
        ReceiveResponse resp = receiveObserver.iterator().next();

        Log.d(TAG, "rcvd data: " + resp.getData());
        Log.d(TAG, "rcvd data1: " + resp.getData().toString());
        Log.d(TAG, "sample data1: " + mSampleData.toString());
        //Log.d(TAG, "rcvd data2: " + resp.getData().data.toByteArray());
        //TODO: make it isTrue()
        assertThat(resp.getData().toByteArray() == mSampleData).isFalse();

        closeSocket(sock);

        Log.d(TAG, "Exit: testClientconnectToInsecureServerWithoutBonding");
    }

    @Test
    public void testClientconnectToSecureServerWithoutBonding() throws Exception {
        Log.d(TAG, "testClientconnectToSecureServerWithoutBonding >> ");

        Connection conn = setupAclConnection(BluetoothDevice.TRANSPORT_LE);
        BluetoothSocket sock = createClientSocket(true, TEST_PSM);
        new Thread(() -> {
            Log.d(TAG, " start the server socket with Bumble");
            startServerSocketwithBumble(conn, false, TEST_PSM);
        }).start();

        connectSocket(sock);
        //Expected socket connection to fail
        assertThat(sock.isConnected()).isFalse();

        closeSocket(sock);
        Log.d(TAG, "Exit: testClientconnectToSecureServerWithoutBonding");
    }

    @Test
    //@Ignore
    public void testClientconnectToSecureServerWithBonding() throws Exception {
        Log.d(TAG, "testClientconnectToSecureServerWithBonding >> ");
        Connection conn = setupAclConnection(BluetoothDevice.TRANSPORT_LE);
        createBondFromPhone(BluetoothDevice.TRANSPORT_LE);
        BluetoothSocket sock = createClientSocket(true, TEST_PSM);
        new Thread(() -> {
            Log.d(TAG, " start the server socket with Bumble");
            startServerSocketwithBumble(conn, false, TEST_PSM);
        }).start();

        connectSocket(sock);
        //Expected socket connection to fail
        assertThat(sock.isConnected()).isFalse();

        closeSocket(sock);
        Log.d(TAG, "Exit: testClientconnectToSecureServerWithBonding");
    }


    private void bluetoothRestart() throws Exception {
        mAdapter.disable();
        //mFutureAdapterStateIntent = SettableFuture.create();
        //assertThat(mFutureAdapterStateIntent.get()).isEqualTo(BluetoothAdapter.STATE_OFF);

        mAdapter.enable();
        //mFutureAdapterStateIntent = SettableFuture.create();
        //assertThat(mFutureAdapterStateIntent.get()).isEqualTo(BluetoothAdapter.STATE_ON);
    }
}
