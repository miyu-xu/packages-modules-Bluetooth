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

package android.bluetooth.pairing;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.PandoraDevice;
import android.bluetooth.StreamObserverSpliterator;
import android.bluetooth.test_utils.EnableBluetoothRule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import pandora.GattProto;
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

    private static final ParcelUuid BATTERY_UUID =
        ParcelUuid.fromString("0000180F-0000-1000-8000-00805F9B34FB");

    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Rule
    public final EnableBluetoothRule mEnableBluetoothRule =
            new EnableBluetoothRule(false /* enableTestMode */, true /* toggleBluetooth */);

    @Mock private BroadcastReceiver mReceiver;
    private final Map<String, Integer> mActionRegistrationCounts = new HashMap<>();
    private InOrder mInOrder = null;
    private BluetoothDevice mBumbleDevice;

    private final StreamObserverSpliterator<PairingEvent> mPairingEventStreamObserver =
            new StreamObserverSpliterator<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mInOrder = inOrder(mReceiver);

        mBumbleDevice = mBumble.getRemoteDevice();
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
    }

    @After
    public void tearDown() throws Exception {
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
        mBumbleDevice = null;
        if (getTotalActionRegistrationCounts() > 0) {
            sTargetContext.unregisterReceiver(mReceiver);
            mActionRegistrationCounts.clear();
        }
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
        registerIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED, BluetoothDevice.ACTION_PAIRING_REQUEST);

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

        unregisterIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED, BluetoothDevice.ACTION_PAIRING_REQUEST);
    }

    /**
     * Test if parallel GATT service discovery interrupts cancelling LE pairing
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     *   <li>Bumble has GATT services in addition to GAP and GATT services
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
        registerIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        // Outgoing GATT service discovery and incoming LE pairing in parallel
        helper_OutgoingGattServiceDiscoveryWithIncomingLePairing();

        // Cancel pairing from Android
        assertThat(mBumbleDevice.cancelBondProcess()).isTrue();

        // Pairing should be cancelled in a moment instead of timing out in 30
        // seconds
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        verifyNoMoreInteractions(mReceiver);

        unregisterIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    }

    /**
     * Test if parallel GATT service discovery interrupts the LE pairing
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     *   <li>Bumble has GATT services in addition to GAP and GATT services
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
        registerIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        // Outgoing GATT service discovery and incoming LE pairing in parallel
        helper_OutgoingGattServiceDiscoveryWithIncomingLePairing();

        // Approve pairing from Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        // Ensure that pairing succeeds
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        verifyNoMoreInteractions(mReceiver);

        unregisterIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    }

    /* Starts outgoing GATT service discovery and incoming LE pairing in parallel */
    private void helper_OutgoingGattServiceDiscoveryWithIncomingLePairing() {
        // Setup intent filters
        registerIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_PAIRING_REQUEST,
                BluetoothDevice.ACTION_UUID,
                BluetoothDevice.ACTION_ACL_CONNECTED);

        // Register an interesting GATT service on Bumble
        mBumble.gattBlocking()
                .registerService(
                        GattProto.RegisterServiceRequest.newBuilder()
                                .setService(
                                        GattProto.GattServiceParams.newBuilder()
                                                .setUuid(BATTERY_UUID.toString())
                                                .build())
                                .build());

        // Start GATT service discovery, this will establish LE ACL
        assertThat(mBumbleDevice.fetchUuidsWithSdp(BluetoothDevice.TRANSPORT_LE)).isTrue();

        // Make Bumble connectable
        AdvertiseResponse advertiseResponse =
                mBumble.hostBlocking()
                        .advertise(
                                AdvertiseRequest.newBuilder()
                                        .setLegacy(true)
                                        .setConnectable(true)
                                        .setOwnAddressType(OwnAddressType.PUBLIC)
                                        .build())
                        .next();

        // Wait for connection on Android
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));

        // Start pairing from Bumble
        StreamObserverSpliterator<SecureResponse> responseObserver =
                new StreamObserverSpliterator<>();
        mBumble.security()
                .secure(
                        SecureRequest.newBuilder()
                                .setConnection(advertiseResponse.getConnection())
                                .setLe(LESecurityLevel.LE_LEVEL3)
                                .build(),
                        responseObserver);

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
        // so that ACTION_UUID is received here.
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_UUID),
                hasExtra(
                        BluetoothDevice.EXTRA_UUID,
                        Matchers.arrayContainingInAnyOrder(BATTERY_UUID)));

        unregisterIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_PAIRING_REQUEST,
                BluetoothDevice.ACTION_UUID,
                BluetoothDevice.ACTION_ACL_CONNECTED);
    }

    private void removeBond(BluetoothDevice device) {
        registerIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        assertThat(device.removeBond()).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        unregisterIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    }

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    /**
     * Helper function to add reference count to registered intent actions
     *
     * @param actions new intent actions to add. If the array is empty, it is a no-op.
     */
    private void registerIntentActions(String... actions) {
        if (actions.length == 0) {
            return;
        }
        if (getTotalActionRegistrationCounts() > 0) {
            sTargetContext.unregisterReceiver(mReceiver);
        }
        for (String action : actions) {
            mActionRegistrationCounts.merge(action, 1, Integer::sum);
        }
        IntentFilter filter = new IntentFilter();
        mActionRegistrationCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .forEach(entry -> filter.addAction(entry.getKey()));
        sTargetContext.registerReceiver(mReceiver, filter);
    }

    /**
     * Helper function to reduce reference count to registered intent actions If total reference
     * count is zero after removal, no broadcast receiver will be registered.
     *
     * @param actions intent actions to be removed. If some action is not registered, it is no-op
     *     for that action. If the actions array is empty, it is also a no-op.
     */
    private void unregisterIntentActions(String... actions) {
        if (actions.length == 0) {
            return;
        }
        if (getTotalActionRegistrationCounts() <= 0) {
            return;
        }
        sTargetContext.unregisterReceiver(mReceiver);
        for (String action : actions) {
            if (!mActionRegistrationCounts.containsKey(action)) {
                continue;
            }
            mActionRegistrationCounts.put(action, mActionRegistrationCounts.get(action) - 1);
            if (mActionRegistrationCounts.get(action) <= 0) {
                mActionRegistrationCounts.remove(action);
            }
        }
        if (getTotalActionRegistrationCounts() > 0) {
            IntentFilter filter = new IntentFilter();
            mActionRegistrationCounts.entrySet().stream()
                    .filter(v -> v.getValue() > 0)
                    .forEach(t -> filter.addAction(t.getKey()));
            sTargetContext.registerReceiver(mReceiver, filter);
        }
    }

    /**
     * Get sum of reference count from all registered actions
     *
     * @return sum of reference count from all registered actions
     */
    private int getTotalActionRegistrationCounts() {
        return mActionRegistrationCounts.values().stream().reduce(0, Integer::sum);
    }
}
