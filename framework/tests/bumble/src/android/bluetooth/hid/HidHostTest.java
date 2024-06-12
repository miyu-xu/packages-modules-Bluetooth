/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HIDGrpc;

/** Test cases for {@link Hid Host}. */
@RunWith(AndroidJUnit4.class)
public class HidHostTest {
    private static final String TAG = "HidHostTest";
    private SettableFuture<Integer> mFutureConnectionIntent,
            mFutureAdapterStateIntent,
            mFutureBondIntent,
            mFutureHandShakeIntent,
            mFutureProtocolModeIntent,
            mFutureVirtualUnplugIntent,
            mFutureReportIntent;
    private BluetoothDevice mDevice;
    private BluetoothHidHost mService;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private HIDGrpc.HIDBlockingStub mHidBlockingStub;
    private byte mReportId;
    private static final int KEYBD_RPT_ID = 1;
    private static final int KEYBD_RPT_SIZE = 9;
    private static final int MOUSE_RPT_ID = 2;
    private static final int MOUSE_RPT_SIZE = 4;
    private static final int INVALID_RPT_ID = 3;

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    private BroadcastReceiver mConnectionStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED.equals(
                            intent.getAction())) {
                        int state =
                                intent.getIntExtra(
                                        BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
                        Log.i(TAG, "Connection state change:" + state);
                        if (state == BluetoothProfile.STATE_CONNECTED
                                || state == BluetoothProfile.STATE_DISCONNECTED) {
                            if (mFutureConnectionIntent != null) {
                                mFutureConnectionIntent.set(state);
                            }
                        }
                    } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
                        mBumble.getRemoteDevice().setPairingConfirmation(true);
                    } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
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
                    } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(
                            intent.getAction())) {
                        int bondState =
                                intent.getIntExtra(
                                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        Log.i(TAG, "Bond state change:" + bondState);
                        if (bondState == BluetoothDevice.BOND_BONDED
                                || bondState == BluetoothDevice.BOND_NONE) {
                            if (mFutureBondIntent != null) {
                                mFutureBondIntent.set(bondState);
                            }
                        }
                    } else if (BluetoothHidHost.ACTION_PROTOCOL_MODE_CHANGED.equals(
                            intent.getAction())) {
                        int protocolMode =
                                intent.getIntExtra(
                                        BluetoothHidHost.EXTRA_PROTOCOL_MODE,
                                        BluetoothHidHost.PROTOCOL_UNSUPPORTED_MODE);
                        Log.i(TAG, "Protocol mode:" + protocolMode);
                        if (mFutureProtocolModeIntent != null) {
                            mFutureProtocolModeIntent.set(protocolMode);
                        }
                    } else if (BluetoothHidHost.ACTION_HANDSHAKE.equals(intent.getAction())) {
                        int handShake =
                                intent.getIntExtra(
                                        BluetoothHidHost.EXTRA_STATUS,
                                        BluetoothHidDevice.ERROR_RSP_UNKNOWN);
                        Log.i(TAG, "Handshake status:" + handShake);
                        if (mFutureHandShakeIntent != null) {
                            mFutureHandShakeIntent.set(handShake);
                        }
                    } else if (BluetoothHidHost.ACTION_VIRTUAL_UNPLUG_STATUS.equals(
                            intent.getAction())) {
                        int virtualUnplug =
                                intent.getIntExtra(
                                        BluetoothHidHost.EXTRA_VIRTUAL_UNPLUG_STATUS,
                                        BluetoothHidHost.VIRTUAL_UNPLUG_STATUS_FAIL);
                        Log.i(TAG, "Virtual Unplug status:" + virtualUnplug);
                        if (mFutureVirtualUnplugIntent != null) {
                            mFutureVirtualUnplugIntent.set(virtualUnplug);
                        }
                    } else if (BluetoothHidHost.ACTION_REPORT.equals(intent.getAction())) {
                        byte[] report = intent.getByteArrayExtra(BluetoothHidHost.EXTRA_REPORT);
                        int reportSize =
                                intent.getIntExtra(BluetoothHidHost.EXTRA_REPORT_BUFFER_SIZE, 0);
                        mReportId = report[0];
                        if (mFutureReportIntent != null) {
                            mFutureReportIntent.set((reportSize - 1));
                        }
                    }
                }
            };

    // These callbacks run on the main thread.
    private final class HidHostServiceListener implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothHidHost) proxy;
        }

        public void onServiceDisconnected(int profile) {}
    }

    @Before
    public void setUp() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver,
                new IntentFilter(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED));
        mContext.registerReceiver(
                mConnectionStateReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        mAdapter.getProfileProxy(mContext, new HidHostServiceListener(), BluetoothProfile.HID_HOST);
        mHidBlockingStub = mBumble.hidBlocking();
        mFutureConnectionIntent = SettableFuture.create();

        mDevice = mBumble.getRemoteDevice();
        assertThat(mDevice.createBond()).isTrue();

        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
    }

    @After
    public void tearDown() throws Exception {
        mDevice.removeBond();
        mContext.unregisterReceiver(mConnectionStateReceiver);
    }

    /**
     * Test HID Connection and Disconnection:
     *
     * <ol>
     *   <li>1. Android tries to create bond, emitting bonding intent 4. Android confirms the
     *       pairing via pairing request intent
     *   <li>2. Bumble confirms the pairing internally
     *   <li>3. Android tries to HID connect and verifies Connection state intent
     *   <li>4. Bumble Disconnect the HID and Android verifies Connection state intent
     * </ol>
     */
    @Test
    public void disconnectHidDeviceTest() throws Exception {

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.disconnectHidHost(Empty.getDefaultInstance());

        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    /**
     * Test HID Device reconnection when connection policy change:
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android verifies the connection policy
     *   <li>3. Bumble disconnect HID and Android verifies Connection state intent
     *   <li>4. Bumble reconnects and Android verifies Connection state intent
     *   <li>5. Bumble disconnect HID and Android verifies Connection state intent
     *   <li>6. Android disable connection policy
     *   <li>7. Bumble connect the HID and Android verifies Connection state intent
     *   <li>8. Android enable connection policy
     *   <li>9. Bumble disconnect HID and Android verifies Connection state intent
     *   <li>10. Bumble connect the HID and Android verifies Connection state intent
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hidReconnectionWhenConnectionPolicyChangeTest() throws Exception {

        assertThat(mService.getConnectionPolicy(mDevice))
                .isEqualTo(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.disconnectHidHost(Empty.getDefaultInstance());
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.connectHidHost(Empty.getDefaultInstance());
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.disconnectHidHost(Empty.getDefaultInstance());
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        assertThat(
                        mService.setConnectionPolicy(
                                mDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                .isTrue();

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.connectHidHost(Empty.getDefaultInstance());
        assertThat(mService.getConnectionState(mDevice))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        mFutureConnectionIntent = SettableFuture.create();
        assertThat(
                        mService.setConnectionPolicy(
                                mDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED))
                .isTrue();
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.disconnectHidHost(Empty.getDefaultInstance());
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.connectHidHost(Empty.getDefaultInstance());
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
    }

    /**
     * Test HID Device reconnection after BT restart with connection policy allowed
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android verifies the connection policy
     *   <li>3. BT restart on Android
     *   <li>4. Bumble reconnects and Android verifies Connection state intent
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hidReconnectionAfterBTrestartWithConnectionPolicyAllowedTest() throws Exception {

        assertThat(mService.getConnectionPolicy(mDevice))
                .isEqualTo(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        bluetoothRestart();

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.connectHidHost(Empty.getDefaultInstance());
        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
    }

    /**
     * Test HID Device reconnection after BT restart with connection policy disallowed
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android verifies the connection policy
     *   <li>3. Android disable the connection policy
     *   <li>4. BT restart on Android
     *   <li>5. Bumble reconnects and Android verifies Connection state intent
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hidReconnectionAfterBTrestartWithConnectionPolicyiDisallowedTest()
            throws Exception {

        assertThat(mService.getConnectionPolicy(mDevice))
                .isEqualTo(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        assertThat(
                        mService.setConnectionPolicy(
                                mDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                .isTrue();

        bluetoothRestart();

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.connectHidHost(Empty.getDefaultInstance());
        assertThat(mService.getConnectionState(mDevice))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    /**
     * Test HID Device reconnection when device is removed
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android verifies the connection policy
     *   <li>3. Android disconnect and remove the bond
     *   <li>4. Bumble reconnects and Android verifies Connection state intent
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hidReconnectionAfterDeviceRemovedTest() throws Exception {

        assertThat(mService.getConnectionPolicy(mDevice))
                .isEqualTo(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.disconnectHidHost(Empty.getDefaultInstance());

        assertThat(mFutureConnectionIntent.get()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        mDevice.removeBond();

        mFutureConnectionIntent = SettableFuture.create();
        mHidBlockingStub.connectHidHost(Empty.getDefaultInstance());
        assertThat(mService.getConnectionState(mDevice))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    /**
     * Test Virtual Unplug from Hid Host
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Virtual Unplug and verifies Bonding
     * </ol>
     */
    @Test
    public void hidVirtualUnplugFromHidHostTest() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        mService.virtualUnplug(mDevice);
        mFutureBondIntent = SettableFuture.create();
        assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_NONE);
    }

    /**
     * Test Virtual Unplug from Hid Device
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Bumble Virtual Unplug and Android verifies Bonding
     * </ol>
     */
    @Test
    public void hidVirtualUnplugFromHidDeviceTest() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver,
                new IntentFilter(BluetoothHidHost.ACTION_VIRTUAL_UNPLUG_STATUS));

        mHidBlockingStub.virtualCableUnplugHidHost(Empty.getDefaultInstance());
        mFutureVirtualUnplugIntent = SettableFuture.create();
        assertThat(mFutureVirtualUnplugIntent.get())
                .isEqualTo(BluetoothHidHost.VIRTUAL_UNPLUG_STATUS_SUCCESS);
    }

    /**
     * Test Get Protocol mode
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Gets the Protocol mode and and verifies the mode
     * </ol>
     */
    @Test
    public void hidGetProtocolModeTest() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver,
                new IntentFilter(BluetoothHidHost.ACTION_PROTOCOL_MODE_CHANGED));

        mService.getProtocolMode(mDevice);
        mFutureProtocolModeIntent = SettableFuture.create();
        assertThat(mFutureProtocolModeIntent.get())
                .isEqualTo(BluetoothHidHost.PROTOCOL_REPORT_MODE);
    }

    /**
     * Test Set Protocol mode
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Sets the Protocol mode and and verifies the mode
     * </ol>
     */
    @Test
    public void hidSetProtocolModeTest() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver, new IntentFilter(BluetoothHidHost.ACTION_HANDSHAKE));

        mService.setProtocolMode(mDevice, BluetoothHidHost.PROTOCOL_BOOT_MODE);
        mFutureHandShakeIntent = SettableFuture.create();
        assertThat(mFutureHandShakeIntent.get())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
    }

    /**
     * Test Get Report
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android get report and and verifies the report
     * </ol>
     */
    @Test
    public void hidGetReportTest() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver, new IntentFilter(BluetoothHidHost.ACTION_REPORT));
        mContext.registerReceiver(
                mConnectionStateReceiver, new IntentFilter(BluetoothHidHost.ACTION_HANDSHAKE));

        // Keyboard report
        byte id = KEYBD_RPT_ID;
        mService.getReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, id, (int) 0);
        mFutureReportIntent = SettableFuture.create();
        assertThat(mFutureReportIntent.get()).isEqualTo(KEYBD_RPT_SIZE);
        assertThat(mReportId).isEqualTo(KEYBD_RPT_ID);

        // Mouse report
        id = MOUSE_RPT_ID;
        mService.getReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, id, (int) 0);
        mFutureReportIntent = SettableFuture.create();
        assertThat(mFutureReportIntent.get()).isEqualTo(MOUSE_RPT_SIZE);
        assertThat(mReportId).isEqualTo(MOUSE_RPT_ID);

        // Invalid report
        id = INVALID_RPT_ID;
        mService.getReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, id, (int) 0);
        mFutureHandShakeIntent = SettableFuture.create();
        assertThat(mFutureHandShakeIntent.get())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
    }

    /**
     * Test Set Report
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Set report and and verifies the report
     * </ol>
     */
    @Test
    public void hidSetReportTest() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver, new IntentFilter(BluetoothHidHost.ACTION_HANDSHAKE));

        // Keyboard report
        mService.setReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, "010203040506070809");
        mFutureHandShakeIntent = SettableFuture.create();
        assertThat(mFutureHandShakeIntent.get()).isEqualTo(BluetoothHidDevice.ERROR_RSP_SUCCESS);
        // Keyboard report - Invalid param
        mService.setReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, "0102030405");
        mFutureHandShakeIntent = SettableFuture.create();
        assertThat(mFutureHandShakeIntent.get())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_INVALID_PARAM);
        // Mouse report
        mService.setReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, "02030405");
        mFutureHandShakeIntent = SettableFuture.create();
        assertThat(mFutureHandShakeIntent.get()).isEqualTo(BluetoothHidDevice.ERROR_RSP_SUCCESS);
        // Invalid report id
        mService.setReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, "0304");
        mFutureHandShakeIntent = SettableFuture.create();
        assertThat(mFutureHandShakeIntent.get())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
    }

    private void bluetoothRestart() throws Exception {
        mContext.registerReceiver(
                mConnectionStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        mAdapter.disable();
        mFutureAdapterStateIntent = SettableFuture.create();
        assertThat(mFutureAdapterStateIntent.get()).isEqualTo(BluetoothAdapter.STATE_OFF);

        mAdapter.enable();
        mFutureAdapterStateIntent = SettableFuture.create();
        assertThat(mFutureAdapterStateIntent.get()).isEqualTo(BluetoothAdapter.STATE_ON);
    }
}
