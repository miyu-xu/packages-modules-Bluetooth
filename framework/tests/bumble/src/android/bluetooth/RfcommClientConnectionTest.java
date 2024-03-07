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

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import pandora.RfcommProto;


@RunWith(AndroidJUnit4.class)
public class RfcommClientConnectionTest {
    private static final String TAG = RfcommClientConnectionTest.class.getSimpleName();
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);
    private static final String TEST_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();

    // Gives shell permissions during the test.
    @Rule public final AdoptShellPermissionsRule mPermissionsRule = new AdoptShellPermissionsRule();

    // Set up a Bumble Pandora device for the duration of the test.
    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Mock private BroadcastReceiver mReceiver;

    private BluetoothDevice mBumbleDevice;
    private int mServerId;

    @Before
    public void SetUp() throws Exception {
        Log.wtf(TAG, "asdf Setup");
        MockitoAnnotations.initMocks(this);

        doAnswer(inv -> {
                    Log.d(TAG, "onReceive(): intent=" + Arrays.toString(inv.getArguments()));
                    Intent intent = inv.getArgument(1);
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                        int bondState =
                                intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                        Log.d(TAG, "onReceive(): bondState=" + bondState);
                    }
                    return null;
                })
                .when(mReceiver)
                .onReceive(any(), any());

        mBumbleDevice = mBumble.getRemoteDevice();
        removeBondIfBonded(mBumbleDevice);

        mServerId = mBumble.rfcommBlocking()
                .startServer(
                        RfcommProto.ServerOptions.newBuilder()
                                .setUuid(TEST_UUID)
                                .setName("RFCOMM Server")
                                .build()
                )
                .getServer()
                .getId();
    }

    @After
    public void TearDown() throws Exception {
        Log.i(TAG,"asdf TearDown");
        removeBondIfBonded(mBumbleDevice);
        RfcommProto.ServerId server = RfcommProto.ServerId.newBuilder().setId(mServerId).build();
        mBumble.rfcommBlocking().stopServer(
                RfcommProto.StopServerRequest.newBuilder()
                        .setServer(server)
                        .build()
        );
        mBumbleDevice = null;
    }
    @Test
    public void ConnectToOpenServerSocketBondedInsecure() throws Exception {
        Log.d(TAG, "asdf ConnectToOpenServerSocketBondedInsecure");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        // create bond between DUT and Ref
        assertThat(mBumbleDevice.createBond()).isTrue();
        Matcher<Intent> bondedMatcher = AllOf.allOf(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED)
        );
        verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(bondedMatcher));

        // Insecure connection to RFCOMM Server
        BluetoothSocket insecureSocket =
                mBumbleDevice.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(TEST_UUID));
        RfcommProto.ServerId server = RfcommProto.ServerId.newBuilder().setId(mServerId).build();
        RfcommProto.AcceptConnectionResponse connectionResponse = mBumble.rfcommBlocking()
                .acceptConnection(
                        RfcommProto.AcceptConnectionRequest.newBuilder()
                                .setServer(server)
                                .build()
                );
        assertThat(connectionResponse.getConnection().getId()).isEqualTo(1);
    }

    private void removeBondIfBonded(BluetoothDevice deviceToRemove) {
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        if (bondedDevices == null) {
            Log.d(TAG, "asdf removeBondIfBonded(): no devices bonded");
            return;
        } else if (!bondedDevices.contains(deviceToRemove)) {
            Log.d(TAG, "asdf removeBondIfBonded(): Tried to remove a device that isn't bonded");
            return;
        }
        if (bondedDevices.contains(deviceToRemove)) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
            assertThat(deviceToRemove.removeBond()).isTrue();
            Matcher<Intent> unbondedMatcher = AllOf.allOf(
                    hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                    hasExtra(BluetoothDevice.EXTRA_DEVICE, deviceToRemove),
                    hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
            );
            verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                    .onReceive(any(Context.class), MockitoHamcrest.argThat(unbondedMatcher));
        }
        mContext.unregisterReceiver(mReceiver);
    }

}
