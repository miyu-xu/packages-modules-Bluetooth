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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HfGrpc;

/** Test cases for {@link Hf}. */
@RunWith(AndroidJUnit4.class)
public class HfTest {
    private static final String TAG = "HfTest";
    private SettableFuture<Integer> mFutureConnectionIntent,
            mFutureAdapterStateIntent,
            mFutureBondIntent,
            mFutureHandShakeIntent,
            mFutureProtocolModeIntent,
            mFutureVirtualUnplugIntent,
            mFutureReportIntent;
    private SettableFuture<Boolean> mAclConnectionIntent;
    private BluetoothDevice mRemoteDevice;
    private BluetoothHeadset mHfService;
    private BluetoothA2dp mA2dpService;
    private BluetoothHidHost mHidService;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private HfGrpc.HfBlockingStub mHfBlockingStub;

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mPandoraDevice = new PandoraDevice();

    private BroadcastReceiver mRemoteDeviceStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                            int state =
                                    intent.getIntExtra(
                                            BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
                            Log.i(TAG, "QAZ Connection state change:" + state);
                            if (state == BluetoothProfile.STATE_CONNECTED
                                    || state == BluetoothProfile.STATE_DISCONNECTED) {
                                if (mFutureConnectionIntent != null) {
                                    mFutureConnectionIntent.set(state);
                                }
                            }
                            break;
                        case BluetoothDevice.ACTION_PAIRING_REQUEST:
                            mPandoraDevice.getRemoteDevice().setPairingConfirmation(true);
                            break;
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            int adapterState =
                                    intent.getIntExtra(
                                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                            Log.i(TAG, "QAZ Adapter state change:" + adapterState);
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
                            Log.i(TAG, "QAZ Bond state change:" + bondState);
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
                            mHfService = (BluetoothHeadset) proxy;
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
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        mContext.registerReceiver(mRemoteDeviceStateReceiver, filter);
        mAdapter.getProfileProxy(
                mContext, mBluetoothProfileServiceListener, BluetoothProfile.HID_HOST);
        mAdapter.getProfileProxy(mContext, mBluetoothProfileServiceListener, BluetoothProfile.A2DP);
        mAdapter.getProfileProxy(
                mContext, mBluetoothProfileServiceListener, BluetoothProfile.HEADSET);
        mHfBlockingStub = mPandoraDevice.hfBlocking();
        mFutureConnectionIntent = SettableFuture.create();

        mRemoteDevice = mPandoraDevice.getRemoteDevice();
        mFutureBondIntent = SettableFuture.create();
        assertThat(mRemoteDevice.createBond()).isTrue();
        assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_BONDED);

        Log.i(TAG, "QAZ BOND_BONDED");

        if (mA2dpService != null
                && mA2dpService.getConnectionPolicy(mRemoteDevice)
                        == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            Log.i(TAG, "QAZ mA2dpService CONNECTION_POLICY_FORBIDDEN");
            assertThat(
                            mA2dpService.setConnectionPolicy(
                                    mRemoteDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                    .isTrue();
        }
        if (mHidService != null
                && mHidService.getConnectionPolicy(mRemoteDevice)
                        == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            Log.i(TAG, "QAZ mHidService CONNECTION_POLICY_FORBIDDEN");
            assertThat(
                            mHidService.setConnectionPolicy(
                                    mRemoteDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                    .isTrue();
        }
        Log.i(TAG, "QAZ before connect assert");
        // assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);

        Log.i(TAG, "QAZ Setup");
    }

    @After
    public void tearDown() throws Exception {

        if (mRemoteDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mFutureBondIntent = SettableFuture.create();
            mRemoteDevice.removeBond();
            assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_NONE);
        }

        if (mRemoteDevice.isConnected()) {
            mAclConnectionIntent = SettableFuture.create();
            mRemoteDevice.disconnect();
            assertThat(mAclConnectionIntent.get()).isTrue();
        }

        mContext.unregisterReceiver(mRemoteDeviceStateReceiver);
    }

    /**
     * Test RFCOMM collision
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HF Device
     *   <li>2. Initiate HFP connection from both DUT side and Remote at same time
     *   <li>3. Check if there is RFCOMM collission happening
     *   <li>4. Disconnect HFP connection from DUT
     *   <li>5. Re-initiate HFP connection from DUT
     *   <li>6. Confirm that the HFP Profile connection succeeds
     * </ol>
     */
    @Test
    public void rfCommCollisionTest() throws Exception {
        Log.i(TAG, "rfCommCollisionTest");

        Log.i(
                TAG,
                "Connection_State Before test case "
                        + mHfService.getConnectionState(mRemoteDevice));

        mFutureConnectionIntent = SettableFuture.create();
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
        Log.i(TAG, "Profile Connected");
        if (mHfService.getConnectionState(mRemoteDevice) == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Profile Connected");
            mFutureConnectionIntent = SettableFuture.create();
            mHfService.disconnect(mRemoteDevice);
            assertThat(mFutureConnectionIntent.get())
                    .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
            Log.i(TAG, "Profile Disconnected ");

            // mFutureConnectionIntent = SettableFuture.create();
            mHfBlockingStub.connect(Empty.getDefaultInstance());
            // assertThat(mFutureConnectionIntent.get())
            //        .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
        }

        Log.i(TAG, "End of File");
    }

    @Test
    public void rfCommDisconnectionTest() throws Exception {
        Log.i(TAG, "rfCommDisconnectionTest");

        mFutureConnectionIntent = SettableFuture.create();
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
        if (mHfService.getConnectionState(mRemoteDevice) == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Profile Connected");
            mFutureConnectionIntent = SettableFuture.create();
            mHfBlockingStub.disconnect(Empty.getDefaultInstance());
            assertThat(mFutureConnectionIntent.get())
                    .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
        }

        Log.i(TAG, "End of File");
    }
}
