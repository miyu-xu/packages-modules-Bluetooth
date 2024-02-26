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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.PandoraDevice;
import android.bluetooth.StreamObserverSpliterator;
import android.bluetooth.pairing.utils.IntentReceiver;
import android.bluetooth.test_utils.EnableBluetoothRule;
import android.content.Context;
import android.os.ParcelUuid;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
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
    private static final String TAG = "PairingTest";
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);

    private static final String BATTERY_UUID_STRING = "0000180F-0000-1000-8000-00805F9B34FB";
    private static final ParcelUuid BATTERY_UUID = ParcelUuid.fromString(BATTERY_UUID_STRING);

    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Rule(order = 3)
    public final EnableBluetoothRule mEnableBluetoothRule =
            new EnableBluetoothRule(false /* enableTestMode */, true /* toggleBluetooth */);

    private BluetoothDevice mBumbleDevice;

    private final StreamObserverSpliterator<PairingEvent> mPairingEventStreamObserver =
            new StreamObserverSpliterator<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

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
        IntentReceiver intentReceiver =
                new IntentReceiver(
                        sTargetContext,
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                        BluetoothDevice.ACTION_PAIRING_REQUEST);

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security()
                        .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .onPairing(mPairingEventStreamObserver);

        assertThat(mBumbleDevice.createBond()).isTrue();
        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));

        intentReceiver.verifyReceivedOrdered(
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

        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        intentReceiver.verifyNoMoreInteractions();
        intentReceiver.remove();
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
     * <p>Expectation: Pairing gets cancelled instead of getting timed out
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_RESET_PAIRING_ONLY_FOR_RELATED_SERVICE_DISCOVERY)
    public void testCancelBondLe_WithGattServiceDiscovery() {
        IntentReceiver intentReceiver =
                new IntentReceiver(sTargetContext, BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        // Outgoing GATT service discovery and incoming LE pairing in parallel
        StreamObserverSpliterator<SecureResponse> responseObserver =
                helper_OutgoingGattServiceDiscoveryWithIncomingLePairing();

        // Cancel pairing from Android
        assertThat(mBumbleDevice.cancelBondProcess()).isTrue();

        SecureResponse secureResponse = responseObserver.iterator().next();
        assertThat(secureResponse.hasPairingFailure()).isTrue();

        // Pairing should be cancelled in a moment instead of timing out in 30
        // seconds
        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        intentReceiver.verifyNoMoreInteractions();

        intentReceiver.remove();
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
     * <p>Expectation: Pairing succeeds
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_RESET_PAIRING_ONLY_FOR_RELATED_SERVICE_DISCOVERY)
    public void testBondLe_WithGattServiceDiscovery() {
        IntentReceiver intentReceiver =
                new IntentReceiver(sTargetContext, BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        // Outgoing GATT service discovery and incoming LE pairing in parallel
        StreamObserverSpliterator<SecureResponse> responseObserver =
                helper_OutgoingGattServiceDiscoveryWithIncomingLePairing();

        // Approve pairing from Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        SecureResponse secureResponse = responseObserver.iterator().next();
        assertThat(secureResponse.hasSuccess()).isTrue();

        // Ensure that pairing succeeds
        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        intentReceiver.verifyNoMoreInteractions();

        intentReceiver.remove();
    }

    /* Starts outgoing GATT service discovery and incoming LE pairing in parallel */
    private StreamObserverSpliterator<SecureResponse>
            helper_OutgoingGattServiceDiscoveryWithIncomingLePairing() {
        // Setup intent filters
        IntentReceiver intentReceiver =
                new IntentReceiver(
                        sTargetContext,
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                        BluetoothDevice.ACTION_PAIRING_REQUEST,
                        BluetoothDevice.ACTION_UUID,
                        BluetoothDevice.ACTION_ACL_CONNECTED);

        // Register lots of interesting GATT services on Bumble
        for (int i = 0; i < 40; i++) {
            mBumble.gattBlocking()
                    .registerService(
                            GattProto.RegisterServiceRequest.newBuilder()
                                    .setService(
                                            GattProto.GattServiceParams.newBuilder()
                                                    .setUuid(BATTERY_UUID.toString())
                                                    .build())
                                    .build());
        }

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

        // Todo: Unexpected empty ACTION_UUID intent is generated
        intentReceiver.verifyReceivedOrdered(hasAction(BluetoothDevice.ACTION_UUID));

        // Wait for connection on Android
        intentReceiver.verifyReceivedOrdered(
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
        // TODO: Order of these events is not deterministic
        intentReceiver.verifyReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        intentReceiver.verifyReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));

        // Allow participating in the incoming pairing on Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        // Wait for pairing approval notification on Android
        intentReceiver.verifyReceived(
                2,
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));

        // Wait for GATT service discovery to complete on Android
        // so that ACTION_UUID is received here.
        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_UUID),
                hasExtra(
                        BluetoothDevice.EXTRA_UUID,
                        Matchers.arrayContainingInAnyOrder(BATTERY_UUID)));

        intentReceiver.remove();

        return responseObserver;
    }

    private void removeBond(BluetoothDevice device) {
        IntentReceiver intentReceiver =
                new IntentReceiver(sTargetContext, BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        assertThat(device.removeBond()).isTrue();
        intentReceiver.verifyReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        intentReceiver.remove();
    }
}
