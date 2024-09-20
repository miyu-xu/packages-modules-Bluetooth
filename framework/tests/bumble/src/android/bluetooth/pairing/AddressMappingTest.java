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

package android.bluetooth.pairing;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.Host;
import android.bluetooth.PandoraDevice;
import android.bluetooth.cts.EnableBluetoothRule;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Test cases for {@link Hid Host}. */
@RunWith(AndroidJUnit4.class)
public class AddressMappingTest {
    private static final String TAG = AddressMappingTest.class.getSimpleName();
    private static final String BUMBLE_DEVICE_NAME = "Bumble";
    private static final Duration INTENT_TIMEOUT = Duration.ofSeconds(10);
    private static final int DISCOVERY_TIMEOUT = 2000; // 2 seconds
    private CompletableFuture<BluetoothDevice> mDeviceFound;
    private BluetoothDevice mDevice;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private Host mHost;

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Rule(order = 3)
    public final EnableBluetoothRule enableBluetoothRule = new EnableBluetoothRule(false, true);

    private BroadcastReceiver mStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                        BluetoothDevice device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        String deviceName =
                                String.valueOf(intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
                        Log.i(TAG, "Discovered device: " + device + " with name: " + deviceName);

                        if (deviceName != null && BUMBLE_DEVICE_NAME.equals(deviceName)) {
                            mDeviceFound.complete(device);
                        }
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
        mHost = new Host(mContext);
    }

    @After
    public void tearDown() throws Exception {
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mHost.removeBondAndVerify(mDevice);
        }
        mHost.close();
        mContext.unregisterReceiver(mStateReceiver);
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
        String firstDevAddr = mDevice.getAddress();
        String firstDevIdAddr = mDevice.getIdentityAddress();

        assertThat(mDevice.getAddressType()).isEqualTo(BluetoothDevice.ADDRESS_TYPE_RANDOM);
        mDevice.disconnect();
        // Forget the device
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mHost.removeBondAndVerify(mDevice);
        }
        // Reset remote device
        mBumble.hostBlocking().factoryReset(Empty.getDefaultInstance());
        pairAndConnect();
        String secondDevAddr = mDevice.getAddress();
        String secondDevIdAddr = mDevice.getIdentityAddress();
        assertThat(mDevice.getAddressType()).isEqualTo(BluetoothDevice.ADDRESS_TYPE_RANDOM);
        // Verify RPA rotated address is not same and Identity address is same
        assertThat(firstDevAddr).isNotEqualTo(secondDevAddr);
        assertThat(firstDevIdAddr).isEqualTo(secondDevIdAddr);
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

        // Start Discovery
        mDeviceFound = new CompletableFuture<>();
        assertThat(mAdapter.startDiscovery()).isTrue();
        mDevice =
                mDeviceFound
                        .completeOnTimeout(null, DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
                        .join();
        // Start pairing
        mHost.createBondAndVerify(mDevice);
        Log.i(
                TAG,
                "testLePairing_AddressMapping: Device > addr:"
                        + mDevice.getAddress()
                        + ", identity:"
                        + mDevice.getIdentityAddress());
    }
}
