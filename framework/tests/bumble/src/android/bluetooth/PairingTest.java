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

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import static androidx.core.util.Preconditions.checkState;
import static androidx.test.ext.truth.content.IntentSubject.assertThat;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.truth.content.IntentSubject;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;

@RunWith(AndroidJUnit4.class)
public class PairingTest {
    private static final String TAG = PairingTest.class.getSimpleName();
    private static final int TIMEOUT_MS = 10000;

    @Rule
    public final AdoptShellPermissionsRule mPermissionRule =
            new AdoptShellPermissionsRule(
                    InstrumentationRegistry.getInstrumentation().getUiAutomation(),
                    BLUETOOTH_CONNECT,
                    BLUETOOTH_PRIVILEGED);

    @Rule public final PandoraDevice mBumble = new PandoraDevice();
    private BluetoothDevice mBumbleDevice;
    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();
    private Utils.DeviceBasedBroadcastReceiver mReceiver;

    @Before
    public void setUp() throws Exception {
        BluetoothAdapterUtils.disableAdapter(sAdapter, sTargetContext);
        BluetoothAdapterUtils.enableAdapter(sAdapter, sTargetContext);
        checkState(sAdapter.isEnabled(), "BluetoothAdapter has to be enabled");

        mBumbleDevice = sAdapter.getRemoteDevice(mBumble.getPublicBluetoothAddress());

        mReceiver = new Utils.DeviceBasedBroadcastReceiver();
        mReceiver.addDevice(mBumbleDevice);

        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        sTargetContext.registerReceiver(mReceiver, filter);

        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!sAdapter.isEnabled()) {
            Log.w(TAG, "Skipping teardown as adapter is not enabled");
            return;
        }
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            // no need to wait for intent since we are turning off adapter
            mBumbleDevice.removeBond();
        }
        if (mReceiver != null) {
            sTargetContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        mBumbleDevice = null;
        BluetoothAdapterUtils.disableAdapter(sAdapter, sTargetContext);
        BluetoothAdapterUtils.enableAdapter(sAdapter, sTargetContext);
    }

    /** After class */
    @AfterClass
    public static void tearDownClass() {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .dropShellPermissionIdentity();
    }

    /**
     * Test a simple BR/EDR just works pairing flow in the follow steps:
     *
     * <ol>
     *   <li>1. Bumble resets, enables inquiry and page scan, and sets I/O cap to no display no
     *       input
     *   <li>2. Android connects to Bumble via its MAC address
     *   <li>3. Android tries to create bond, emitting bonding intent 4. Android confirms the
     *       pairing via pairing request intent
     *   <li>5. Bumble confirms the pairing internally (optional, added only for test confirmation)
     *   <li>6. Android verifies bonded intent
     * </ol>
     */
    @Test
    public void testBrEdrPairing_phoneInitiatedBrEdrInquiryOnlyJustWorks() {
        final BlockingQueue<PairingEvent> pairingEvents = new LinkedBlockingQueue<>();
        StreamObserver<PairingEvent> pairingEventObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(PairingEvent value) {
                        pairingEvents.add(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "onPairing error: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "onPairing completed");
                    }
                };
        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security().onPairing(pairingEventObserver);

        assertThat(mBumbleDevice.createBond()).isTrue();
        Intent bondingIntent = Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(bondingIntent).isNotNull();
        assertThat(bondingIntent).hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        assertThat(bondingIntent)
                .extras()
                .integer(BluetoothDevice.EXTRA_BOND_STATE)
                .isEqualTo(BluetoothDevice.BOND_BONDING);

        Intent pairingRequestIntent =
                Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(pairingRequestIntent).isNotNull();
        assertThat(pairingRequestIntent).hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        assertThat(pairingRequestIntent)
                .extras()
                .integer(BluetoothDevice.EXTRA_PAIRING_VARIANT)
                .isEqualTo(BluetoothDevice.PAIRING_VARIANT_CONSENT);
        mBumbleDevice.setPairingConfirmation(true);

        PairingEvent pairingEvent = Utils.waitForItem(TIMEOUT_MS, pairingEvents);
        assertThat(pairingEvent).isNotNull();
        assertThat(pairingEvent.getJustWorks()).isNotNull();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        Intent bondedIntent = Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(bondedIntent).isNotNull();
        assertThat(bondedIntent).hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        assertThat(bondedIntent)
                .extras()
                .integer(BluetoothDevice.EXTRA_BOND_STATE)
                .isEqualTo(BluetoothDevice.BOND_BONDED);

        removeBond(mBumbleDevice);
    }

    private void removeBond(BluetoothDevice device) {
        assertThat(device.removeBond()).isTrue();
        Intent unbondIntent = Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(unbondIntent).isNotNull();
        IntentSubject.assertThat(unbondIntent).hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        IntentSubject.assertThat(unbondIntent)
                .extras()
                .integer(BluetoothDevice.EXTRA_BOND_STATE)
                .isEqualTo(BluetoothDevice.BOND_NONE);
    }
}
