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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.bluetooth.test_utils.EnableBluetoothRule;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import java.time.Duration;
import java.util.Set;

import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;

@RunWith(AndroidJUnit4.class)
public class PairingTest {
    private static final String TAG = PairingTest.class.getSimpleName();
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);

    @Rule
    public final AdoptShellPermissionsRule mPermissionRule =
            new AdoptShellPermissionsRule(
                    InstrumentationRegistry.getInstrumentation().getUiAutomation(),
                    BLUETOOTH_CONNECT,
                    BLUETOOTH_PRIVILEGED);

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Rule
    public final EnableBluetoothRule mEnableBluetoothRule =
            new EnableBluetoothRule(false /* enableTestMode */, true /* toggleBluetooth */);

    private BluetoothDevice mBumbleDevice;
    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();

    @Mock private BroadcastReceiver mReceiver;
    @Mock private StreamObserver<PairingEvent> mPairingEventStreamObserver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        sTargetContext.registerReceiver(mReceiver, filter);

        mBumbleDevice = mBumble.getRemoteDevice();
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!sAdapter.isEnabled()) {
            // Handle error when adapter was not enabled successfully
            Log.w(TAG, "Skipping teardown as adapter is not enabled");
            return;
        }
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            // no need to wait for intent since we are turning off adapter
            mBumbleDevice.removeBond();
        }
        mBumbleDevice = null;
        sTargetContext.unregisterReceiver(mReceiver);
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
        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security().onPairing(mPairingEventStreamObserver);

        assertThat(mBumbleDevice.createBond()).isTrue();
        verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(
                        any(Context.class),
                        MockitoHamcrest.argThat(
                                AllOf.allOf(
                                        IntentMatchers.hasAction(
                                                BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_BOND_STATE,
                                                BluetoothDevice.BOND_BONDING))));

        verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(
                        any(Context.class),
                        MockitoHamcrest.argThat(
                                AllOf.allOf(
                                        IntentMatchers.hasAction(
                                                BluetoothDevice.ACTION_PAIRING_REQUEST),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_PAIRING_VARIANT,
                                                BluetoothDevice.PAIRING_VARIANT_CONSENT))));
        mBumbleDevice.setPairingConfirmation(true);

        ArgumentCaptor<PairingEvent> pairingEventArgumentCaptor =
                ArgumentCaptor.forClass(PairingEvent.class);
        verify(mPairingEventStreamObserver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onNext(pairingEventArgumentCaptor.capture());
        PairingEvent pairingEvent = pairingEventArgumentCaptor.getValue();
        assertThat(pairingEvent).isNotNull();
        assertThat(pairingEvent.getJustWorks()).isNotNull();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(
                        any(Context.class),
                        MockitoHamcrest.argThat(
                                AllOf.allOf(
                                        IntentMatchers.hasAction(
                                                BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_BOND_STATE,
                                                BluetoothDevice.BOND_BONDED))));

        removeBond(mBumbleDevice);
    }

    private void removeBond(BluetoothDevice device) {
        assertThat(device.removeBond()).isTrue();
        verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(
                        any(Context.class),
                        MockitoHamcrest.argThat(
                                AllOf.allOf(
                                        IntentMatchers.hasAction(
                                                BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_DEVICE, device),
                                        IntentMatchers.hasExtra(
                                                BluetoothDevice.EXTRA_BOND_STATE,
                                                BluetoothDevice.BOND_NONE))));
    }
}
