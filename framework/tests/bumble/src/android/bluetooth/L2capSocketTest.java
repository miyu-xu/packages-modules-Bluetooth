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
import static org.mockito.Mockito.any;
import java.io.IOException;
import android.bluetooth.BluetoothSocketException;
import org.mockito.MockitoAnnotations;

import org.mockito.InOrder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HIDGrpc;
import pandora.HidProto.ProtocolModeEvent;
import pandora.HidProto.ReportEvent;

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

import pandora.L2capProto.CreateLECreditBasedChannelRequest;
import pandora.L2capProto.CreateLECreditBasedChannelResponse;

/** Test cases for {@link Hid Host}. */
@RunWith(AndroidJUnit4.class)
public class L2capSocketTest {
    private static final String TAG = "L2capSocketTest";
    private SettableFuture<Integer> mFutureConnectionIntent,
            mFutureAdapterStateIntent,
            mFutureBondIntent,
            mFutureHandShakeIntent,
            mFutureProtocolModeIntent,
            mFutureVirtualUnplugIntent,
            mFutureReportIntent;
    private SettableFuture<Boolean> mAclConnectionIntent;
    private BluetoothDevice mDevice;
    private BluetoothHidHost mHidService;
    private BluetoothHeadset mHfpService;
    private BluetoothA2dp mA2dpService;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private byte mReportId;
    private static final int TEST_PSM = 0x80;
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);
    private InOrder mInOrder = null;
    private BluetoothDevice mDUT = null;

    private static final Duration PROTO_MODE_TIMEOUT = Duration.ofSeconds(10);

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Mock
    private BroadcastReceiver mGeneralBluetoothStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothDevice.ACTION_PAIRING_REQUEST:
                            mBumble.getRemoteDevice().setPairingConfirmation(true);
                            break;
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            int adapterState =
                                    intent.getIntExtra(
                                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                            Log.i(TAG, "Adapter state change:" + adapterState);
                            if (adapterState == BluetoothAdapter.STATE_ON
                                    || adapterState == BluetoothAdapter.STATE_OFF) {
                                if (mFutureAdapterStateIntent != null) {
                                    mFutureAdapterStateIntent.set(adapterState);
                                }
                            }
                            break;
                        case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                            int bondState =
                                    intent.getIntExtra(
                                            BluetoothDevice.EXTRA_BOND_STATE,
                                            BluetoothDevice.ERROR);
                            Log.i(TAG, "Bond state change:" + bondState);
                            if (bondState == BluetoothDevice.BOND_BONDED
                                    || bondState == BluetoothDevice.BOND_NONE) {
                                if (mFutureBondIntent != null) {
                                    mFutureBondIntent.set(bondState);
                                }
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            if (mAclConnectionIntent != null) {
                                mAclConnectionIntent.set(true);
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

    // These callbacks run on the main thread.
    private final BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
            new BluetoothProfile.ServiceListener() {

                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    switch (profile) {
                        case BluetoothProfile.HEADSET:
                            mHfpService = (BluetoothHeadset) proxy;
                            break;
                        case BluetoothProfile.A2DP:
                            mA2dpService = (BluetoothA2dp) proxy;
                            break;
                        case BluetoothProfile.HID_HOST:
                            mHidService = (BluetoothHidHost) proxy;
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {}
    };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final IntentFilter filter = new IntentFilter();
        //filter.addAction(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //filter.addAction(BluetoothHidHost.ACTION_PROTOCOL_MODE_CHANGED);
        //filter.addAction(BluetoothHidHost.ACTION_HANDSHAKE);
        //filter.addAction(BluetoothHidHost.ACTION_VIRTUAL_UNPLUG_STATUS);
        //filter.addAction(BluetoothHidHost.ACTION_REPORT);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);

        mContext.registerReceiver(mGeneralBluetoothStateReceiver, filter);
        mDUT = mBumble.getRemoteDevice();
        //mHidBlockingStub = mBumble.hidBlocking();
        mFutureConnectionIntent = SettableFuture.create();
        mDevice = mBumble.getRemoteDevice();
        mInOrder = inOrder(mGeneralBluetoothStateReceiver);
        Log.d(TAG, "set Up done >> ");
        /*
         * No bonding for insecure L2CAP
        mFutureBondIntent = SettableFuture.create();

        assertThat(mDevice.createBond()).isTrue();
        assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_BONDED);

         */
    }

    @After
    public void tearDown() throws Exception {

        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mFutureBondIntent = SettableFuture.create();
            mDevice.removeBond();
            assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_NONE);
        }

        if (mDevice.isConnected()) {
            mAclConnectionIntent = SettableFuture.create();
            mDevice.disconnect();
            assertThat(mAclConnectionIntent.get()).isTrue();
        }

        mContext.unregisterReceiver(mGeneralBluetoothStateReceiver);
    }

    @SafeVarargs
    private void verifyIntentReceivedUnordered(int num, Matcher<Intent>... matchers) {
        verify(mGeneralBluetoothStateReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()).times(num))
            .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    @SafeVarargs
    private void verifyIntentReceivedUnordered(Matcher<Intent>... matchers) {
        verifyIntentReceivedUnordered(1, matchers);
    }

    private void advertiseWithBumble() {
        AdvertiseRequest request =
            AdvertiseRequest.newBuilder()
                .setLegacy(true)
                .setConnectable(true)
                .setOwnAddressType(OwnAddressType.RANDOM)
                .build();

        StreamObserverSpliterator<AdvertiseResponse> responseObserver =
            new StreamObserverSpliterator<>();

        mBumble.host().advertise(request, responseObserver);
    }

    private void startServerSocketwithBumble(Connection conn, boolean isSecure, int psm) {
        CreateLECreditBasedChannelRequest req =
            CreateLECreditBasedChannelRequest.newBuilder()
                .setConnection(conn)
                .setPsm(psm)
                .setSecure(isSecure)
                .build();

        StreamObserverSpliterator<CreateLECreditBasedChannelResponse> responseObserver =
            new StreamObserverSpliterator<>();

        Log.d(TAG, "Creating the L2CAP channel from bumble side");
        mBumble.l2capBlocking().createLECreditBasedChannel(req);
    }

    public void createConnectAcceptSocket(boolean isSecure, int psm) {
        //advertiseWithBumble();
        Log.d(TAG, "createConnectAcceptSocket: start fetchUuid");
        // Start GATT service discovery, this will establish LE ACL
        assertThat(mBumble.getRemoteDevice().fetchUuidsWithSdp(BluetoothDevice.TRANSPORT_LE)).isTrue();

        Log.d(TAG, "createConnectAcceptSocket: advertise on Bumble");
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

        Log.d(TAG, "Wait for Connection");
        // Wait for connection on Android
        verifyIntentReceivedUnordered(
            hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
            hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));

        Log.d(TAG, "createConnectAcceptSocket: start the server socket with Bumble");
        startServerSocketwithBumble(advertiseResponse.getConnection(), false, TEST_PSM);
    }

    private void createClientConnection(boolean isSecure, int psm) {
        Log.d(TAG, "createClientConnection: isSucre " + isSecure + "psm: " + psm);
        BluetoothSocket sock = null;
        try {
            sock = mDUT.createL2capChannel(psm);
        } catch (IOException e) {
            //declare testcase failure
            Log.d(TAG, "IOEXCEPT while creating LECOC channel");
        }
        try {
            sock.connect();
        } catch (IOException e) {
            //declare testcase failure
            Log.d(TAG, "IOEXCEPT while connecting to LECOC channel");
        }

        try {
            sock.close();
        } catch (IOException e) {
            //declare testcase failure
            Log.d(TAG, "IOEXCEPT while closing to LECOC channel");
        }
    }

    @Test
    public void testLecocsocketClientconnectInsecureServer() throws Exception {
        Log.d(TAG, "testLecocsocketClientconnectInsecureServer >> ");

        createConnectAcceptSocket(false, TEST_PSM);

        createClientConnection(false, TEST_PSM);
        Log.d(TAG, "Exit: testLecocsocketClientconnectInsecureServer");
    }

    private void bluetoothRestart() throws Exception {
        mAdapter.disable();
        mFutureAdapterStateIntent = SettableFuture.create();
        assertThat(mFutureAdapterStateIntent.get()).isEqualTo(BluetoothAdapter.STATE_OFF);

        mAdapter.enable();
        mFutureAdapterStateIntent = SettableFuture.create();
        assertThat(mFutureAdapterStateIntent.get()).isEqualTo(BluetoothAdapter.STATE_ON);
    }
}
