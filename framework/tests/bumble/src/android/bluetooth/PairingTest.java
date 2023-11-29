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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.bluetooth.test_utils.EnableBluetoothRule;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.protobuf.Empty;

import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.OwnAddressType;
import pandora.SecurityProto.LESecurityLevel;
import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;
import pandora.SecurityProto.SecureRequest;
import pandora.SecurityProto.SecureResponse;

@RunWith(AndroidJUnit4.class)
public class PairingTest {
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);
    private static final int SERVICE_REGISTRATION_TIMEOUT = 2;
    private static final int SERVICE_DISCOVERY_TIMEOUT = 2;

    private final Context mTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final BluetoothAdapter mAdapter =
            mTargetContext.getSystemService(BluetoothManager.class).getAdapter();

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Rule
    public final EnableBluetoothRule mEnableBluetoothRule =
            new EnableBluetoothRule(false /* enableTestMode */, true /* toggleBluetooth */);

    @Mock private BroadcastReceiver mReceiver;
    private boolean mReceiverRegistered = false;
    private InOrder mInOrder = null;
    private BluetoothDevice mBumbleDevice;

    private final StreamObserverSpliterator<PairingEvent> mPairingEventStreamObserver =
            new StreamObserverSpliterator<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mInOrder = inOrder(mReceiver);

        mBumbleDevice = mBumble.getRemoteDevice();
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
    }

    @After
    public void tearDown() throws Exception {
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
        mBumbleDevice = null;
        mTargetContext.unregisterReceiver(mReceiver);
        mReceiverRegistered = false;
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
        // Update intent filters
        updateIntentFilter(
                Set.of(
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                        BluetoothDevice.ACTION_PAIRING_REQUEST));

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security()
                        .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .onPairing(mPairingEventStreamObserver);

        assertThat(mBumbleDevice.createBond()).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));
        mBumbleDevice.setPairingConfirmation(true);

        PairingEvent pairingEvent = mPairingEventStreamObserver.iterator().next();
        assertThat(pairingEvent.hasJustWorks()).isTrue();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        verifyNoMoreInteractions(mReceiver);
    }

    /**
     * Test if parallel GATT service discovery interrupts cancelling LE pairing
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     *   <li>Bumble has GATT services other than GAP and GATT
     * </ol>
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Bumble is discoverable and connectable over LE
     *   <li>Android connects to Bumble over LE
     *   <li>Android starts GATT service discovery
     *   <li>Bumble initiates pairing
     *   <li>Android does not confirm the pairing immediately
     *   <li>Service discovery completes
     *   <li>Android cancels the pairing
     * </ol>
     *
     * Expectation: Pairing gets cancelled instead of getting timed out
     */
    @Test
    public void testCancelBondLe_WithGattServiceDiscovery() {
        // Outgoing GATT sevice discovery and incoming LE pairing in parallel
        testStep_OutgoingGattServiceDiscoveryWithIncomingLePairing();

        // Cancel pairing from Android
        assertThat(mBumbleDevice.cancelBondProcess()).isTrue();

        // Pairing should be be cancelled in a moment instead of timing out in 30
        // seconds
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));
    }

    /**
     * Test if parallel GATT service discovery interrupts the LE pairing
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     *   <li>Bumble has GATT services other than GAP and GATT
     * </ol>
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Bumble is discoverable and connectable over LE
     *   <li>Android connects to Bumble over LE
     *   <li>Android starts GATT service discovery
     *   <li>Bumble starts pairing
     *   <li>Service discovery completes
     *   <li>Android does confirms the pairing
     *   <li>Pairing is successful
     * </ol>
     *
     * Expectation: Pairing succeeds
     */
    @Test
    public void testBondLe_WithGattServiceDiscovery() {
        // Outgoing GATT sevice discovery and incoming LE pairing in parallel
        testStep_OutgoingGattServiceDiscoveryWithIncomingLePairing();

        // Approve pairing from Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        // Ensure that pairing succeeds
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));
    }

    /* Starts outgoing GATT sevice discovery and incoming LE pairing in parallel */
    private void testStep_OutgoingGattServiceDiscoveryWithIncomingLePairing() {
        // Update intent filters
        Set<String> intentFilterStrings =
                Set.of(
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                        BluetoothDevice.ACTION_PAIRING_REQUEST,
                        BluetoothDevice.ACTION_UUID,
                        BluetoothDevice.ACTION_ACL_CONNECTED);
        updateIntentFilter(intentFilterStrings);

        // Register a GATT service on Bumble to ensure that GATT service discovery takes longer
        // Todo: Register a GATT service that Android BT is interested in
        mBumble.dckBlocking()
                .withDeadline(Deadline.after(SERVICE_REGISTRATION_TIMEOUT, TimeUnit.SECONDS))
                .register(Empty.getDefaultInstance());

        // Start GATT service discovery, this will establish LE ACL
        assertThat(mBumbleDevice.fetchUuidsWithSdp(BluetoothDevice.TRANSPORT_LE)).isTrue();

        // Make Bumble connectable
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(OwnAddressType.PUBLIC)
                        .build();
        AdvertiseResponse advResp = mBumble.hostBlocking().advertise(request).next();

        // Wait for connection on Android
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));

        // Start pairing from Bumble
        SecureRequest.Builder securityRequest =
                SecureRequest.newBuilder()
                        .setConnection(advResp.getConnection())
                        .setLe(LESecurityLevel.LE_LEVEL3);
        StreamObserverSpliterator<SecureResponse> responseObserver =
                new StreamObserverSpliterator<>();
        mBumble.security().secure(securityRequest.build(), responseObserver);

        // Wait for incoming pairing notification on Android
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));

        // Allow participating in the incoming pairing on Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        // Wait for pairing approval notification on Android
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));

        // Wait for GATT service discovery to complete on Android
        // Todo: Bumble does not have interesting GATT services,
        // so ACTION_UUID is not received.
        // Add interesting services in Bumble and wait for ACTION_UUID
        // verifyIntentReceived(hasAction(BluetoothDevice.ACTION_UUID));
        wait(SERVICE_DISCOVERY_TIMEOUT);
    }

    private void removeBond(BluetoothDevice device) {
        updateIntentFilter(Set.of(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        assertThat(device.removeBond()).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));
    }

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    private void wait(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateIntentFilter(Set<String> intentStrings) {
        IntentFilter filter = new IntentFilter();
        for (String intenString : intentStrings) {
            filter.addAction(intenString);
        }

        if (mReceiverRegistered) {
            mTargetContext.unregisterReceiver(mReceiver);
        }
        mTargetContext.registerReceiver(mReceiver, filter);
        mReceiverRegistered = true;
    }
}
