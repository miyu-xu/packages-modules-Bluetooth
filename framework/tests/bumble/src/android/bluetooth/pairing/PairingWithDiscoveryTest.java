/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.PandoraDevice;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.Utils;
import android.bluetooth.StreamObserverSpliterator;
import com.android.bluetooth.flags.Flags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.mockito.MockitoAnnotations;
import org.mockito.InOrder;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostProto;
import pandora.HostProto.DiscoverabilityMode;
import pandora.HostProto.SetDiscoverabilityModeRequest;
import pandora.HostProto.ConnectabilityMode;
import pandora.HostProto.SetConnectabilityModeRequest;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.OwnAddressType;
import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

/** Test cases for {@link PairingWithDiscoveryTest}. */
@RunWith(AndroidJUnit4.class)
public class PairingWithDiscoveryTest {
    private static final String TAG = PairingWithDiscoveryTest.class.getSimpleName();
    private static final String BUMBLE_DEVICE_NAME = "Bumble";
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);
    private static final int DISCOVERY_TIMEOUT = 2000; // 2 seconds
    private static final int FIND_NAME_TIMEOUT = 13000; // 13 second
    private static final int LE_GENERAL_DISCOVERABLE = 2;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();

    private final Map<String, Integer> mActionRegistrationCounts = new HashMap<>();
    private final StreamObserverSpliterator<PairingEvent> mPairingEventStreamObserver =
        new StreamObserverSpliterator<>();

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    private BluetoothDevice mBumbleDevice;
    private BluetoothDevice mDeviceWithNoName;
    private InOrder mInOrder = null;
    private CompletableFuture<BluetoothDevice> mDeviceFound;
    private CompletableFuture<BluetoothDevice> mDeviceFoundWithNoName;
    private CompletableFuture<Boolean> mDeviceNameFound;
    @Mock private BroadcastReceiver mReceiver;

    @SuppressLint("MissingPermission")
    private final Answer<Void> mIntentHandler =
        inv -> {
            Log.i(TAG, "onReceive(): intent=" + Arrays.toString(inv.getArguments()));
            Intent intent = inv.getArgument(1);
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device =
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice.class);
                    String deviceName =
                        String.valueOf(
                            intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
                    Log.i(
                        TAG,
                        "Discovered device: "
                            + device
                            + " with name: "
                            + deviceName);
                    if (deviceName != null && BUMBLE_DEVICE_NAME.equals(deviceName) &&
                        mDeviceFound != null) {
                        mDeviceFound.complete(device);
                    }
                    else if(mDeviceFoundWithNoName != null) {
                        // Remote device name is not present
                        mDeviceFoundWithNoName.complete(device);
                    }
                    break;
                case BluetoothDevice.ACTION_NAME_CHANGED:
                    String bumbleDeviceName =
                        String.valueOf(
                            intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
                    Log.i(TAG, "onReceive(): Received device name  " + bumbleDeviceName);
                    if(mDeviceNameFound != null && BUMBLE_DEVICE_NAME.equals(bumbleDeviceName))
                    {
                        mDeviceNameFound.complete(true);
                    }
                    break;
                default:
                    Log.i(TAG, "onReceive(): unknown intent action " + action);
                    break;
            }
            return null;
        };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doAnswer(mIntentHandler).when(mReceiver).onReceive(any(), any());

        mInOrder = inOrder(mReceiver);

        mBumbleDevice = mBumble.getRemoteDevice();

        for (BluetoothDevice device : mAdapter.getBondedDevices()) {
            removeBond(device);
        }
    }

    @After
    public void tearDown() throws Exception {
        for (BluetoothDevice device : mAdapter.getBondedDevices()) {
            removeBond(device);
        }
        mBumbleDevice = null;
        if (getTotalActionRegistrationCounts() > 0) {
            mContext.unregisterReceiver(mReceiver);
            mActionRegistrationCounts.clear();
        }
    }

    /**
     * Test LE pairing flow with Auto transport
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     * </ol>
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Bumble is non discoverable over BR/EDR and discoverable over LE
     *   <li>Bumble LE AD Flags in advertisement support dual mode
     *   <li>Android starts discovery of remote devices
     *   <li>Android initiates pairing with Bumble using Auto transport
     * </ol>
     *
     * <p>Expectation: Pairing succeeds over LE Transport
     */
    @Test
    @RequiresFlagsEnabled({Flags.FLAG_AUTO_TRANSPORT_PAIRING})
    public void testBondLe_AutoTransport() throws Exception {
        registerIntentActions(
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_PAIRING_REQUEST);

        // Make Bumble Non discoverable over BR/EDR
        mBumble.hostBlocking()
            .setDiscoverabilityMode(
                SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(DiscoverabilityMode.NOT_DISCOVERABLE)
                    .build());

        // Make Bumble Non connectable over BR/EDR
        SetConnectabilityModeRequest request =
            SetConnectabilityModeRequest.newBuilder()
                .setMode(ConnectabilityMode.NOT_CONNECTABLE)
                .build();
        mBumble.hostBlocking().setConnectabilityMode(request);

        // Start LE advertisement from Bumble
        AdvertiseRequest.Builder requestBuilder =
            AdvertiseRequest.newBuilder().setLegacy(true)
                .setConnectable(true)
                .setOwnAddressType(OwnAddressType.PUBLIC);

        HostProto.DataTypes.Builder dataTypeBuilder = HostProto.DataTypes.newBuilder();
        dataTypeBuilder.setCompleteLocalName(BUMBLE_DEVICE_NAME);
        dataTypeBuilder.setIncludeCompleteLocalName(true);
        //Set LE AD Flags to be LE General discoverable, also supports dual mode
        dataTypeBuilder.setLeDiscoverabilityModeValue(LE_GENERAL_DISCOVERABLE);
        requestBuilder.setData(dataTypeBuilder.build());

        StreamObserverSpliterator<AdvertiseResponse> responseObserver =
            new StreamObserverSpliterator<>();
        mBumble.host().advertise(requestBuilder.build(), responseObserver);

        // Start Device Discovery from Android
        testStepStartDiscovery();

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
            mBumble.security()
                .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS)
                .onPairing(mPairingEventStreamObserver);

        // Start pairing from Android with Auto transport
        assertThat(mBumbleDevice.createBond(BluetoothDevice.TRANSPORT_AUTO)).isTrue();

        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE));
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(
                BluetoothDevice.EXTRA_PAIRING_VARIANT,
                BluetoothDevice.PAIRING_VARIANT_CONSENT));

        // Approve pairing from Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        PairingEvent pairingEvent = mPairingEventStreamObserver.iterator().next();
        assertThat(pairingEvent.hasJustWorks()).isTrue();
        pairingEventAnswerObserver.onNext(
            PairingEventAnswer.newBuilder().setEvent(pairingEvent)
                .setConfirm(true).build());

        // Ensure that pairing succeeds
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        unregisterIntentActions(
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_PAIRING_REQUEST);
    }

    /**
     * Test BR/EDR pairing flow with Auto transport
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     * </ol>
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Bumble is discoverable over BR/EDR and non discoverable over LE
     *   <li>Android starts discovery of remote devices
     *   <li>Android initiates pairing with Bumble using Auto transport
     * </ol>
     *
     * <p>Expectation: Pairing succeeds over BR/EDR Transport
     */
    @Test
    @RequiresFlagsEnabled({Flags.FLAG_AUTO_TRANSPORT_PAIRING})
    public void testBondBrEdr_AutoTransport() throws Exception {
        registerIntentActions(
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_PAIRING_REQUEST);

        // Make Bumble discoverable over BR/EDR
        mBumble.hostBlocking()
            .setDiscoverabilityMode(
                SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(DiscoverabilityMode.DISCOVERABLE_GENERAL)
                    .build());

        SetConnectabilityModeRequest request =
            SetConnectabilityModeRequest.newBuilder()
                .setMode(ConnectabilityMode.CONNECTABLE)
                .build();
        mBumble.hostBlocking().setConnectabilityMode(request);

        // Start Device Discovery from Android
        testStepStartDiscovery();

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
            mBumble.security()
                .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS)
                .onPairing(mPairingEventStreamObserver);

        // Start pairing from Android with Auto transport
        assertThat(mBumbleDevice.createBond(BluetoothDevice.TRANSPORT_AUTO)).isTrue();

        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_ACL_CONNECTED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR));
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(
                BluetoothDevice.EXTRA_PAIRING_VARIANT,
                BluetoothDevice.PAIRING_VARIANT_CONSENT));

        // Approve pairing from Android
        assertThat(mBumbleDevice.setPairingConfirmation(true)).isTrue();

        PairingEvent pairingEvent = mPairingEventStreamObserver.iterator().next();
        assertThat(pairingEvent.hasJustWorks()).isTrue();
        pairingEventAnswerObserver.onNext(
            PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        // Ensure that pairing succeeds
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        unregisterIntentActions(
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_PAIRING_REQUEST);
    }
    /**
     * Test case for device with no name in inquiry response
     *
     * <p>Prerequisites:
     *
     * <ol>
     *   <li>Bumble and Android are not bonded
     * </ol>
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Put Bumble in pairing mode
     *   <li>Start device discovery on Android/DUT
     *   <li>Bumble should be found and its name should be available
     *   <li>Initiate pairing from Android/DUT
     *   <li>Cancel pairing from Android/DUT
     *   <li>Put Bumble in pairing mode again
     *   <li>Start device discovery on Android/DUT
     * </ol>
     *
     * <p>Expectation: Bumble remote device must be found and its name should be available
     */
    @Test
    public void testDeviceDiscoveryWithNoName() throws Exception {
        registerIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_NAME_CHANGED);
        Boolean isNameFound = false;
        // Put the bumble remote device into pairing mode
        mBumble.hostBlocking()
            .setDiscoverabilityMode(
                SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(DiscoverabilityMode.DISCOVERABLE_GENERAL)
                    .build());
        // Start discovery for the remote devices with no names in IR
        testStepStartDiscoveryDeviceWithNoName();
        // Wait for DUT to finish discovery and fetch name from bumble
        mDeviceNameFound = new CompletableFuture<>();
        isNameFound =
            mDeviceNameFound
                .completeOnTimeout(false, FIND_NAME_TIMEOUT , TimeUnit.MILLISECONDS)
                .join();

        assertThat(isNameFound).isTrue();
        // Verify remote device's name . Indirectly this proves device's presence
        assertThat(mDeviceWithNoName.getName()).isEqualTo(BUMBLE_DEVICE_NAME);
        //assertThat(mBumbleDevice.getName()).isEqualTo(BUMBLE_DEVICE_NAME);
        assertThat(mAdapter.cancelDiscovery()).isTrue();
        // Initiate pairing with the bumble device
        assertThat(mDeviceWithNoName.createBond(BluetoothDevice.TRANSPORT_AUTO)).isTrue();
        // Verify that the DUT started bonding
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, mDeviceWithNoName),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        // Cancel the pairing
        assertThat(mDeviceWithNoName.cancelBondProcess()).isTrue();
        // Put the bumble remote into pairing mode again
        mBumble.hostBlocking()
            .setDiscoverabilityMode(
                SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(DiscoverabilityMode.DISCOVERABLE_GENERAL)
                    .build());
        // Start the discovery again
        testStepStartDiscoveryDeviceWithNoName();
        // Wait for DUT to finish discovery and fetch name from bumble
        isNameFound = false;
        mDeviceNameFound = new CompletableFuture<>();
        isNameFound =
            mDeviceNameFound
                .completeOnTimeout(false, FIND_NAME_TIMEOUT , TimeUnit.MILLISECONDS)
                .join();
        assertThat(isNameFound).isTrue();
        // Verify the bumble device presence by checking its name
        assertThat(mDeviceWithNoName.getName()).isEqualTo(BUMBLE_DEVICE_NAME);
        // Cancel discovery
        assertThat(mAdapter.cancelDiscovery()).isTrue();
        unregisterIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_NAME_CHANGED);
    }

    /**
     * Helper function to start discovery without cancel discovery in it
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Android starts discovery of remote devices with no name in inquiry response
     * </ol>
     *
     * <p>Expectation:
     *
     * <ol>
     *   <li>Android starts device discovery
     *   <li>Wait for the remote device to be found
     * </ol>
     */
    private void testStepStartDiscoveryDeviceWithNoName() throws Exception {
        registerIntentActions(BluetoothDevice.ACTION_FOUND);
        mDeviceWithNoName = null;
        // Start device discovery from Android
        mDeviceFoundWithNoName = new CompletableFuture<>();
        assertThat(mAdapter.startDiscovery()).isTrue();
        mDeviceWithNoName =
            mDeviceFoundWithNoName
                .completeOnTimeout(null, DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
                .join();
        assertThat(mDeviceWithNoName).isNotNull();
        unregisterIntentActions(BluetoothDevice.ACTION_FOUND);
    }

    /** Helper/testStep functions go here */
    /**
     * Helper function to start device discovery
     *
     * <p>Steps:
     *
     * <ol>
     *   <li>Android starts discovery of remote devices
     * </ol>
     *
     * <p>Expectation:
     *
     * <ol>
     *   <li>Android receives discovery started intent
     *   <li>Android receives discovery finished intent
     *   <li>Checks whether Bumble device was found
     * </ol>
     */
    private void testStepStartDiscovery() throws Exception {
        registerIntentActions(BluetoothDevice.ACTION_FOUND);
        mBumbleDevice = null;

        // Start device discovery from Android
        mDeviceFound = new CompletableFuture<>();
        assertThat(mAdapter.startDiscovery()).isTrue();
        mBumbleDevice =
            mDeviceFound
                .completeOnTimeout(null, DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
                .join();
        assertThat(mBumbleDevice).isNotNull();
        assertThat(mAdapter.cancelDiscovery()).isTrue();

        unregisterIntentActions(BluetoothDevice.ACTION_FOUND);
    }

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
            .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    private void removeBond(BluetoothDevice device) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        assertThat(device.removeBond()).isTrue();
        verifyIntentReceived(
            hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            hasExtra(BluetoothDevice.EXTRA_DEVICE, device),
            hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        mContext.unregisterReceiver(mReceiver);
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
            Log.d(TAG, "registerIntentActions(): unregister ALL intents");
            mContext.unregisterReceiver(mReceiver);
        }
        for (String action : actions) {
            mActionRegistrationCounts.merge(action, 1, Integer::sum);
        }
        IntentFilter filter = new IntentFilter();
        mActionRegistrationCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .forEach(
                entry -> {
                    Log.d(
                        TAG,
                        "registerIntentActions(): Registering action = "
                            + entry.getKey());
                    filter.addAction(entry.getKey());
                });
        mContext.registerReceiver(mReceiver, filter);
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
        Log.d(TAG, "unregisterIntentActions(): unregister ALL intents");
        mContext.unregisterReceiver(mReceiver);
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
                .filter(entry -> entry.getValue() > 0)
                .forEach(
                    entry -> {
                        Log.d(
                            TAG,
                            "unregisterIntentActions(): Registering action = "
                                + entry.getKey());
                        filter.addAction(entry.getKey());
                    });
            mContext.registerReceiver(mReceiver, filter);
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
