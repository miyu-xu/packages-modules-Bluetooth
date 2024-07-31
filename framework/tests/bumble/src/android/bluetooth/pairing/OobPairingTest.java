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

import android.bluetooth.BluetoothAdapter.OobDataCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.OwnAddressType;
import pandora.OobProto.OobDataRequest;
import pandora.OobProto.OobDataResponse;

@RunWith(AndroidJUnit4.class)
public class OobPairingTest {
    private static final String TAG = "OobPairingTest";
    private BluetoothDevice mDevice;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();
    private OobDataResponse mRemoteOobData;
    private SettableFuture<Integer> mFutureBondIntent;

    private static final int HASH_START_POSITION = 0;
    private static final int HASH_END_POSITION = 16;
    private static final int RANDOMIZER_START_POSITION = 16;
    private static final int RANDOMIZER_END_POSITION = 32;

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();
    private BroadcastReceiver mConnectionStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "Intent:" + intent.getAction());
                    switch (intent.getAction()) {
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
                        default:
                            break;
                    }
                }
            };

    private OobData buildOobData() {

        byte[] confirmationHash =
                mRemoteOobData
                        .getOob()
                        .substring(HASH_START_POSITION, HASH_END_POSITION)
                        .toByteArray();
        byte[] randomizer =
                mRemoteOobData
                        .getOob()
                        .substring(RANDOMIZER_START_POSITION, RANDOMIZER_END_POSITION)
                        .toByteArray();
        byte[] address = Utils.addressBytesFromString(Utils.BUMBLE_RANDOM_ADDRESS);
        byte[] addressType = {BluetoothDevice.ADDRESS_TYPE_RANDOM};

        mDevice =
                mAdapter.getRemoteLeDevice(
                        Utils.BUMBLE_RANDOM_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);
        OobData p256 =
                new OobData.LeBuilder(
                                confirmationHash,
                                Bytes.concat(address, addressType),
                                OobData.LE_DEVICE_ROLE_BOTH_PREFER_CENTRAL)
                        .setRandomizerHash(randomizer)
                        .build();
        return p256;
    }

    private void startAdvertise() throws Exception {
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(OwnAddressType.RANDOM)
                        .build();
        mBumble.hostBlocking().advertise(request);
    }

    private final OobDataCallback mGenerateOobDataCallback =
            new OobDataCallback() {
                @Override
                public void onError(int error) {
                    Log.i(TAG, "onError: " + error);
                }

                @Override
                public void onOobData(int transport, OobData data) {
                    Log.d(TAG, "OobData: " + data);
                    data.getConfirmationHash();
                    data.getRandomizerHash();
                    byte[] localData =
                            Bytes.concat(data.getConfirmationHash(), data.getRandomizerHash());
                    OobDataRequest localOobData =
                            OobDataRequest.newBuilder()
                                    .setOob(ByteString.copyFrom(localData))
                                    .build();
                    mRemoteOobData = mBumble.oobBlocking().shareOobData(localOobData);
                    OobData p256 = buildOobData();
                    mDevice.createBondOutOfBand(BluetoothDevice.TRANSPORT_LE, null, p256);
                }
            };

    @Before
    public void setUp() throws Exception {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mConnectionStateReceiver, filter);
    }

    @After
    public void tearDown() throws Exception {
        mDevice.removeBond();
        mContext.unregisterReceiver(mConnectionStateReceiver);
    }

    /**
     * Test OOB pairing: Configuration: Initiator: Locali, Local OOB: No, Remote OOB: Yes ,Secure
     * Connections: Yes
     *
     * <ol>
     *   <li>1. Android gets OOB Data from Bumble.
     *   <li>2. Android creates bond with remote OOB data
     *   <li>5. Android verifies bonded intent
     * </ol>
     */
    @Test
    public void createBondWithRemoteOob() throws Exception {

        mFutureBondIntent = SettableFuture.create();
        startAdvertise();
        OobDataRequest noLocalOobData =
                OobDataRequest.newBuilder().setOob(ByteString.EMPTY).build();
        mRemoteOobData = mBumble.oobBlocking().shareOobData(noLocalOobData);
        OobData p256 = buildOobData();
        mDevice.createBondOutOfBand(BluetoothDevice.TRANSPORT_LE, null, p256);
        assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_BONDED);
    }

    /**
     * Test OOB pairing: Configuration: Initiator - Local, Local OOB - Yes, Remote OOB - Yes, Secure
     * Connections - Yes
     *
     * <ol>
     *   <li>1. Android gets OOB Data from Bumble.
     *   <li>2. Android creates bond with remote OOB data
     *   <li>5. Android verifies bonded intent
     * </ol>
     */
    @Test
    public void createBondWithRemoteAndLocalOob() throws Exception {

        mFutureBondIntent = SettableFuture.create();
        startAdvertise();
        mAdapter.generateLocalOobData(
                BluetoothDevice.TRANSPORT_LE, mContext.getMainExecutor(), mGenerateOobDataCallback);
        assertThat(mFutureBondIntent.get()).isEqualTo(BluetoothDevice.BOND_BONDED);
    }
}
