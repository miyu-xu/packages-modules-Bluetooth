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
import android.os.ParcelUuid;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.DataTypes;
import pandora.HostProto.DiscoverabilityMode;
import pandora.HostProto.OwnAddressType;
import pandora.HostProto.SetDiscoverabilityModeRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Test cases for {@link Hid Host}. */
@RunWith(AndroidJUnit4.class)
public class HidHostDualModeTest {
    private static final String TAG = HidHostDualModeTest.class.getSimpleName();
    private static final String BUMBLE_DEVICE_NAME = "Bumble";
    private CompletableFuture<Integer> mFutureConnectionIntent,
            mFutureBondIntent,
            mFutureHandShakeIntent,
            mFutureReportIntent,
            mFutureProtocolModeIntent,
            mFutureTransportIntent;
    private CompletableFuture<Boolean> mDeviceFoundIntent, mFutureHogpServiceIntent;
    private BluetoothDevice mDevice;
    private BluetoothHidHost mHidService;
    private BluetoothHeadset mHfpService;
    private BluetoothA2dp mA2dpService;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private byte mReportId;
    private static final int KEYBD_RPT_ID = 1;
    private static final int KEYBD_RPT_SIZE = 9;
    private static final int MOUSE_RPT_ID = 2;
    private static final int MOUSE_RPT_SIZE = 4;
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int BOND_TIMEOUT_MS = 5000;
    private static final int DISCOVERY_TIMEOUT_MS = 30000;
    private static final int TIMEOUT_MS = 2000;

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    private BroadcastReceiver mHidStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED:
                            int state =
                                    intent.getIntExtra(
                                            BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
                            int transport =
                                    intent.getIntExtra(
                                            BluetoothDevice.EXTRA_TRANSPORT,
                                            BluetoothDevice.TRANSPORT_AUTO);
                            Log.i(
                                    TAG,
                                    "Connection state change: "
                                            + state
                                            + "transport: "
                                            + transport);
                            if (state == BluetoothProfile.STATE_CONNECTED
                                    || state == BluetoothProfile.STATE_DISCONNECTED) {
                                if (mFutureConnectionIntent != null) {
                                    mFutureConnectionIntent.complete(state);
                                }
                                if (state == BluetoothProfile.STATE_CONNECTED
                                        && mFutureTransportIntent != null) {
                                    mFutureTransportIntent.complete(transport);
                                }
                            }
                            break;
                        case BluetoothDevice.ACTION_PAIRING_REQUEST:
                            mBumble.getRemoteDevice().setPairingConfirmation(true);
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
                                    mFutureBondIntent.complete(bondState);
                                }
                            }
                            break;
                        case BluetoothDevice.ACTION_UUID:
                            ParcelUuid[] parcelUuids =
                                    intent.getParcelableArrayExtra(
                                            BluetoothDevice.EXTRA_UUID, ParcelUuid.class);
                            for (int i = 0; i < parcelUuids.length; i++) {
                                Log.d(TAG, "UUIDs : index=" + i + " uuid=" + parcelUuids[i]);
                                if (parcelUuids[i].equals(BluetoothUuid.HOGP)) {
                                    if (mFutureHogpServiceIntent != null) {
                                        mFutureHogpServiceIntent.complete(true);
                                    }
                                }
                            }
                            break;
                        case BluetoothHidHost.ACTION_PROTOCOL_MODE_CHANGED:
                            int protocolMode =
                                    intent.getIntExtra(
                                            BluetoothHidHost.EXTRA_PROTOCOL_MODE,
                                            BluetoothHidHost.PROTOCOL_UNSUPPORTED_MODE);
                            Log.i(TAG, "Protocol mode:" + protocolMode);
                            if (mFutureProtocolModeIntent != null) {
                                mFutureProtocolModeIntent.complete(protocolMode);
                            }
                            break;
                        case BluetoothHidHost.ACTION_HANDSHAKE:
                            int handShake =
                                    intent.getIntExtra(
                                            BluetoothHidHost.EXTRA_STATUS,
                                            BluetoothHidDevice.ERROR_RSP_UNKNOWN);
                            Log.i(TAG, "Handshake status:" + handShake);
                            if (mFutureHandShakeIntent != null) {
                                mFutureHandShakeIntent.complete(handShake);
                            }
                            break;
                        case BluetoothHidHost.ACTION_REPORT:
                            byte[] report = intent.getByteArrayExtra(BluetoothHidHost.EXTRA_REPORT);
                            int reportSize =
                                    intent.getIntExtra(
                                            BluetoothHidHost.EXTRA_REPORT_BUFFER_SIZE, 0);
                            mReportId = report[0];
                            if (mFutureReportIntent != null) {
                                mFutureReportIntent.complete((reportSize - 1));
                            }
                            break;
                        case BluetoothDevice.ACTION_FOUND:
                            BluetoothDevice device =
                                    intent.getParcelableExtra(
                                            BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                            String deviceName =
                                    String.valueOf(
                                            intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
                            Log.i(
                                    TAG,
                                    "Discovered device: " + device + " with name: " + deviceName);
                            if (deviceName != null && BUMBLE_DEVICE_NAME.equals(deviceName)) {
                                if (mDeviceFoundIntent != null) {
                                    mDevice = device;
                                    mDeviceFoundIntent.complete(true);
                                }
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
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothHidHost.ACTION_PROTOCOL_MODE_CHANGED);
        filter.addAction(BluetoothHidHost.ACTION_HANDSHAKE);
        filter.addAction(BluetoothHidHost.ACTION_REPORT);
        mContext.registerReceiver(mHidStateReceiver, filter);
        mAdapter.getProfileProxy(
                mContext, mBluetoothProfileServiceListener, BluetoothProfile.HID_HOST);
        mAdapter.getProfileProxy(mContext, mBluetoothProfileServiceListener, BluetoothProfile.A2DP);
        mAdapter.getProfileProxy(
                mContext, mBluetoothProfileServiceListener, BluetoothProfile.HEADSET);
        mBumble.hostBlocking()
                .setDiscoverabilityMode(
                        SetDiscoverabilityModeRequest.newBuilder()
                                .setMode(DiscoverabilityMode.DISCOVERABLE_GENERAL)
                                .build());

        DataTypes.Builder dataTypeBuilder = DataTypes.newBuilder();
        dataTypeBuilder.setCompleteLocalName(BUMBLE_DEVICE_NAME);
        dataTypeBuilder.setLeDiscoverabilityModeValue(
                DiscoverabilityMode.DISCOVERABLE_GENERAL_VALUE);
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(OwnAddressType.RANDOM)
                        .setData(dataTypeBuilder.build())
                        .build();
        mBumble.hostBlocking().advertise(request);
        mDeviceFoundIntent = new CompletableFuture<>();
        // Start Discovery
        assertThat(mAdapter.startDiscovery()).isTrue();
        assertThat(
                        mDeviceFoundIntent
                                .completeOnTimeout(
                                        null, DISCOVERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isTrue();
        assertThat(mAdapter.cancelDiscovery()).isTrue();

        mFutureConnectionIntent = new CompletableFuture<>();
        mFutureBondIntent = new CompletableFuture<>();
        mFutureHogpServiceIntent = new CompletableFuture<>();

        assertThat(mDevice.createBond()).isTrue();
        assertThat(
                        mFutureBondIntent
                                .completeOnTimeout(
                                        BluetoothDevice.BOND_NONE,
                                        BOND_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothDevice.BOND_BONDED);
        if (mA2dpService != null
                && mA2dpService.getConnectionPolicy(mDevice)
                        == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            assertThat(
                            mA2dpService.setConnectionPolicy(
                                    mDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                    .isTrue();
        }
        if (mHfpService != null
                && mHfpService.getConnectionPolicy(mDevice)
                        == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            assertThat(
                            mHfpService.setConnectionPolicy(
                                    mDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN))
                    .isTrue();
        }
        assertThat(
                        mFutureConnectionIntent
                                .completeOnTimeout(
                                        BluetoothProfile.STATE_DISCONNECTED,
                                        CONNECT_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothProfile.STATE_CONNECTED);
        assertThat(
                        mFutureHogpServiceIntent
                                .completeOnTimeout(null, CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isTrue();
        assertThat(mHidService.getPreferredTransport(mDevice))
                .isEqualTo(BluetoothDevice.TRANSPORT_BREDR);
        // LE transport
        mFutureConnectionIntent = new CompletableFuture<>();
        mFutureTransportIntent = new CompletableFuture<>();
        mHidService.setPreferredTransport(mDevice, BluetoothDevice.TRANSPORT_LE);
        // Verifies BREDR transport Disconnected
        assertThat(
                        mFutureConnectionIntent
                                .completeOnTimeout(
                                        BluetoothProfile.STATE_CONNECTED,
                                        CONNECT_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        assertThat(
                        mFutureTransportIntent
                                .completeOnTimeout(null, CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothDevice.TRANSPORT_LE);
        assertThat(mHidService.getPreferredTransport(mDevice))
                .isEqualTo(BluetoothDevice.TRANSPORT_LE);
    }

    @After
    public void tearDown() throws Exception {
        if (mDevice != null && mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mFutureBondIntent = new CompletableFuture<>();
            mDevice.removeBond();
            assertThat(
                            mFutureBondIntent
                                    .completeOnTimeout(
                                            BluetoothDevice.BOND_BONDED,
                                            BOND_TIMEOUT_MS,
                                            TimeUnit.MILLISECONDS)
                                    .join())
                    .isEqualTo(BluetoothDevice.BOND_NONE);
        }
        mContext.unregisterReceiver(mHidStateReceiver);
    }

    /**
     * Test HID Preferred transport selection Test case
     *
     * <ol>
     *   <li>1. Android to creates bonding and HID connected with default transport.
     *   <li>2. Android switch the transport to LE and Verifies the transport
     *   <li>3. Android switch the transport to BR/EDR and Verifies the transport
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void setPreferredTransportTest() throws Exception {

        // BREDR transport
        mFutureTransportIntent = new CompletableFuture<>();
        mFutureConnectionIntent = new CompletableFuture<>();
        mHidService.setPreferredTransport(mDevice, BluetoothDevice.TRANSPORT_BREDR);
        // Verifies LE transport Disconnected
        assertThat(
                        mFutureConnectionIntent
                                .completeOnTimeout(
                                        BluetoothProfile.STATE_CONNECTED,
                                        CONNECT_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

        assertThat(
                        mFutureTransportIntent
                                .completeOnTimeout(null, CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothDevice.TRANSPORT_BREDR);
        assertThat(mHidService.getPreferredTransport(mDevice))
                .isEqualTo(BluetoothDevice.TRANSPORT_BREDR);
    }

    /**
     * Test Get Report
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android get report and verifies the report
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hogpGetReportTest() throws Exception {

        // Keyboard report
        byte id = KEYBD_RPT_ID;
        mHidService.getReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, id, (int) 0);
        mFutureReportIntent = new CompletableFuture<>();
        assertThat(
                        mFutureReportIntent
                                .completeOnTimeout(null, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(KEYBD_RPT_SIZE);
        assertThat(mReportId).isEqualTo(KEYBD_RPT_ID);

        // Mouse report
        id = MOUSE_RPT_ID;
        mHidService.getReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, id, (int) 0);
        mFutureReportIntent = new CompletableFuture<>();
        assertThat(
                        mFutureReportIntent
                                .completeOnTimeout(null, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(MOUSE_RPT_SIZE);
        assertThat(mReportId).isEqualTo(MOUSE_RPT_ID);
    }

    /**
     * Test Get Protocol mode
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Gets the Protocol mode and verifies the mode
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hogpGetProtocolModeTest() throws Exception {
        mFutureProtocolModeIntent = new CompletableFuture<>();
        mHidService.getProtocolMode(mDevice);
        assertThat(
                        mFutureProtocolModeIntent
                                .completeOnTimeout(null, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothHidHost.PROTOCOL_REPORT_MODE);
    }

    /**
     * Test Set Protocol mode
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Sets the Protocol mode and verifies the mode
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hogpSetProtocolModeTest() throws Exception {
        mFutureHandShakeIntent = new CompletableFuture<>();
        mHidService.setProtocolMode(mDevice, BluetoothHidHost.PROTOCOL_BOOT_MODE);
        assertThat(
                        mFutureHandShakeIntent
                                .completeOnTimeout(null, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_SUCCESS);
    }

    /**
     * Test Set Report
     *
     * <ol>
     *   <li>1. Android creates bonding and connect the HID Device
     *   <li>2. Android Set report and verifies the report
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hogpSetReportTest() throws Exception {
        // Keyboard report
        mFutureHandShakeIntent = new CompletableFuture<>();
        mHidService.setReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, "010203040506070809");
        assertThat(
                        mFutureHandShakeIntent
                                .completeOnTimeout(null, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_SUCCESS);
        // Mouse report
        mFutureHandShakeIntent = new CompletableFuture<>();
        mHidService.setReport(mDevice, BluetoothHidHost.REPORT_TYPE_INPUT, "02030405");
        assertThat(
                        mFutureHandShakeIntent
                                .completeOnTimeout(null, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothHidDevice.ERROR_RSP_SUCCESS);
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
    @RequiresFlagsEnabled({
        Flags.FLAG_ALLOW_SWITCHING_HID_AND_HOGP,
        Flags.FLAG_SAVE_INITIAL_HID_CONNECTION_POLICY
    })
    public void hogpVirtualUnplugFromHidHostTest() throws Exception {
        mFutureBondIntent = new CompletableFuture<>();
        mHidService.virtualUnplug(mDevice);
        assertThat(
                        mFutureBondIntent
                                .completeOnTimeout(
                                        BluetoothDevice.BOND_BONDED,
                                        BOND_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS)
                                .join())
                .isEqualTo(BluetoothDevice.BOND_NONE);
    }
}
