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

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.DataTypes;
import pandora.HostProto.DiscoverabilityMode;
import pandora.HostProto.OwnAddressType;
import pandora.HostProto.SetDiscoverabilityModeRequest;

import java.time.Duration;

/** Test cases for {@link Hid Host}. */
@RunWith(AndroidJUnit4.class)
public class AddressMappingTest {
    private static final String TAG = AddressMappingTest.class.getSimpleName();
    private static final String BUMBLE_DEVICE_NAME = "Bumble";
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);

    private SettableFuture<Boolean> mDeviceFoundIntent, mDiscoveryStarted;
    private BluetoothDevice mDevice, mDeviceFirst, mDeviceSecond;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    private BroadcastReceiver mStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
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
                                    mDeviceFoundIntent.set(true);
                                }
                            }
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                            if (mDiscoveryStarted != null) {
                                mDiscoveryStarted.set(true);
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

    @Before
    public void setUp() throws Exception {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mContext.registerReceiver(mStateReceiver, filter);
        mDevice = mBumble.getRemoteDevice();
    }

    /**
     * Test pairing when RPA rotates on remote device
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     *   <li>Bumble uses RPA for LE advertisements
     * </ol>
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Bumble is discoverable and connectable over LE
     *   <li>Android connects and bonds to Bumble over LE
     *   <li>Android disconnects from the Bumble device
     *   <li>Android removes the Bumble device
     *   <li>Restart bumble with address rotation
     *   <li>Android connects and bonds to Bumble over LE
     * </ol>
     *
     * <p>Expectation: Pairing is successful after address rotation
     */
    @Test
    public void testLePairing_whenRpaRotates() throws Exception {
        pairAndConnect();
        mDeviceFirst = mDevice;
        mDevice.disconnect();
        // Forget the device
        Host mHost = new android.bluetooth.Host(mContext);
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mHost.removeBondAndVerify(mDevice);
        }
        // Reset remote device
        mBumble.hostBlocking().factoryReset(Empty.getDefaultInstance());
        pairAndConnect();
        mDeviceSecond = mDevice;
        // Verify RPA rotated and Identity address same
        assertThat(mDeviceFirst.getAddress()).isNotEqualTo(mDeviceSecond.getAddress());
        assertThat(mDeviceFirst.getIdentityAddress()).isEqualTo(mDeviceSecond.getIdentityAddress());
    }

    private void pairAndConnect() throws Exception {
        // Make Bumble non-discoverable over BR/EDR
        mBumble.hostBlocking()
                .setDiscoverabilityMode(
                        SetDiscoverabilityModeRequest.newBuilder()
                                .setMode(DiscoverabilityMode.NOT_DISCOVERABLE)
                                .build());

        // Make Bumble connectable using RPA
        DataTypes.Builder dataTypeBuilder = DataTypes.newBuilder();
        dataTypeBuilder.setCompleteLocalName(BUMBLE_DEVICE_NAME);
        dataTypeBuilder.setLeDiscoverabilityModeValue(
                DiscoverabilityMode.DISCOVERABLE_GENERAL_VALUE);
        mBumble.hostBlocking()
                .advertise(
                        AdvertiseRequest.newBuilder()
                                .setLegacy(true)
                                .setConnectable(true)
                                .setOwnAddressType(OwnAddressType.RANDOM)
                                .setData(dataTypeBuilder.build())
                                .build());
        mDiscoveryStarted = SettableFuture.create();
        mDeviceFoundIntent = SettableFuture.create();
        // Start Discovery
        assertThat(mAdapter.startDiscovery()).isTrue();
        assertThat(mDiscoveryStarted.get()).isTrue();
        assertThat(mDeviceFoundIntent.get()).isTrue();
        assertThat(mAdapter.cancelDiscovery()).isTrue();

        // Start pairing
        Host mHost = new android.bluetooth.Host(mContext);
        mHost.createBondAndVerify(mDevice);
        Log.i(
                TAG,
                "testLePairing_AddressMapping: Device > addr:"
                        + mDevice.getAddress()
                        + ", identity:"
                        + mDevice.getIdentityAddress());
    }

    @After
    public void tearDown() throws Exception {
        Host mHost = new android.bluetooth.Host(mContext);
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mHost.removeBondAndVerify(mDevice);
        }
        mHost.close();
        mContext.unregisterReceiver(mStateReceiver);
    }
}
