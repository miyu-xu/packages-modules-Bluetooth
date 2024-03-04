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
import android.content.Intent;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.DataTypes;
import pandora.HostProto.DiscoverabilityMode;
import pandora.HostProto.OwnAddressType;
import pandora.HostProto.SetDiscoverabilityModeRequest;
import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;

@RunWith(AndroidJUnit4.class)
public class AddressMapping {
    private static final String TAG = "PairingTest:AddressMapping";

    private static final String BUMBLE_DEVICE_NAME = "Bumble";
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);

    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();

    @Rule(order = 0)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 1)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Rule(order = 2)
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
     * Test if address mapping is removed on bond removal
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     *   <li>Bumble is a dual mode device
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
     *   <li>Bumble becomes discoverable over BR/EDR but not over LE
     *   <li>Android finds the Bumble device via inquiry
     *   <li>Android attempts to bond with the Bumble device over BR/EDR
     * </ol>
     *
     * <p>Expectation: Pairing over BR/EDR is successful
     */
    @Test
    public void testLePairing_AddressMapping() {
        IntentReceiver intentReceiver =
                new IntentReceiver(
                        sTargetContext,
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                        BluetoothDevice.ACTION_PAIRING_REQUEST,
                        BluetoothDevice.ACTION_ACL_CONNECTED,
                        BluetoothDevice.ACTION_ACL_DISCONNECTED);

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

        // Discover the remote device
        BluetoothDevice refDevice1 = discoverDevice();
        assertThat(refDevice1).isNotNull();

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security()
                        .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .onPairing(mPairingEventStreamObserver);
        assertThat(refDevice1.createBond(BluetoothDevice.TRANSPORT_LE)).isTrue();

        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, refDevice1),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));

        // Wait for connection on Android
        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, refDevice1),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));

        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, refDevice1),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));
        refDevice1.setPairingConfirmation(true);

        PairingEvent pairingEvent = mPairingEventStreamObserver.iterator().next();
        assertThat(pairingEvent.hasJustWorks()).isTrue();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, refDevice1),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        String refAddress1 = refDevice1.getIdentityAddress();
        Log.i(
                TAG,
                "testLePairing_AddressMapping: Device 1 > addr:"
                        + refDevice1.getAddress()
                        + ", identity:"
                        + refAddress1);
        refDevice1.disconnect();

        // Wait for connection on Android
        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_ACL_DISCONNECTED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, refDevice1),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));

        // Forget the device
        assertThat(refDevice1.removeBond()).isTrue();

        intentReceiver.verifyReceivedOrdered(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, refDevice1),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        // Make Bumble discoverable over BR/EDR
        mBumble.hostBlocking()
                .setDiscoverabilityMode(
                        SetDiscoverabilityModeRequest.newBuilder()
                                .setMode(DiscoverabilityMode.DISCOVERABLE_GENERAL)
                                .build());

        // Make Bumble discoverable with identity address
        // mBumble.hostBlocking()
        //   .advertise(
        //     AdvertiseRequest.newBuilder()
        //       .setLegacy(true)
        //     .setConnectable(true)
        //   .setOwnAddressType(OwnAddressType.PUBLIC)
        // .setData(dataTypeBuilder.build())
        // .build());

        // Device the Bumble device again
        BluetoothDevice refDevice2 = discoverDevice();
        assertThat(refDevice2).isNotNull();

        // Found device should not be be using pseudo address
        String refAddress2 = refDevice2.getIdentityAddress();
        Log.i(
                TAG,
                "testLePairing_AddressMapping: Device 2 > addr:"
                        + refDevice2.getAddress()
                        + ", identity:"
                        + refAddress2);
        assertThat(refDevice2).isNotEqualTo(refDevice1);
        assertThat(refAddress1).isNotEqualTo(refAddress2);

        intentReceiver.verifyNoMoreInteractions();
        intentReceiver.remove();
    }

    private BluetoothDevice discoverDevice() {
        CompletableFuture<BluetoothDevice> future = new CompletableFuture<>();
        IntentReceiver intentReceiver =
                new IntentReceiver(
                        sTargetContext,
                        new IntentReceiver.IntentListener() {
                            @Override
                            public void onReceive(Intent intent) {
                                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                                    BluetoothDevice device =
                                            intent.getParcelableExtra(
                                                    BluetoothDevice.EXTRA_DEVICE,
                                                    BluetoothDevice.class);
                                    String deviceName =
                                            String.valueOf(
                                                    intent.getStringExtra(
                                                            BluetoothDevice.EXTRA_NAME));
                                    Log.i(
                                            TAG,
                                            "Discovered device: "
                                                    + device
                                                    + " with name: "
                                                    + deviceName);

                                    if (deviceName != null
                                            && BUMBLE_DEVICE_NAME.equals(deviceName)) {
                                        future.complete(device);
                                    }
                                }
                            }
                        },
                        BluetoothDevice.ACTION_FOUND,
                        // BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                        // BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                        BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        assertThat(sAdapter.startDiscovery()).isTrue();
        intentReceiver.verifyReceivedOrdered(hasAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED));

        BluetoothDevice device = future.completeOnTimeout(null, 2000, TimeUnit.MILLISECONDS).join();

        // TODO: Avoid literals
        intentReceiver.verifyReceived(3, hasAction(BluetoothDevice.ACTION_FOUND));
        assertThat(sAdapter.cancelDiscovery()).isTrue();

        // TODO: Why does this intent comes thrice?
        // intentReceiver.verifyReceived(1, hasAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        intentReceiver.verifyNoMoreInteractions();

        intentReceiver.remove();
        return device;
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
