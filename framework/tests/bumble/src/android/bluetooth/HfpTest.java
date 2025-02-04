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

import static android.bluetooth.BluetoothDevice.TRANSPORT_BREDR;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.PandoraDevice;
import android.bluetooth.StreamObserverSpliterator;
import android.bluetooth.test_utils.EnableBluetoothRule;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.protobuf.Empty;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.stubbing.Answer;

import pandora.HfGrpc;
import pandora.SecurityProto.PairingEvent;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(TestParameterInjector.class)
public class HfpTest {
    private static final String TAG = "HfpTest";
    private static final Duration INTENT_TIMEOUT = Duration.ofSeconds(10);
    private static final int TEST_DELAY_MS = 1000;
    private static final int BT_ON_DELAY_MS = 3000;

    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();
    private HfGrpc.HfBlockingStub mHfBlockingStub;

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Rule(order = 3)
    public final EnableBluetoothRule mEnableBluetoothRule =
            new EnableBluetoothRule(false /* enableTestMode */, true /* toggleBluetooth */);

    private final Map<String, Integer> mActionRegistrationCounts = new HashMap<>();
    private final StreamObserverSpliterator<PairingEvent> mPairingEventStreamObserver =
            new StreamObserverSpliterator<>();
    @Mock private BroadcastReceiver mReceiver;
    @Mock private BluetoothProfile.ServiceListener mProfileServiceListener;
    private InOrder mInOrder = null;
    private BluetoothDevice mBumbleDevice;
    private BluetoothHeadset mHfpService;

    private final Answer<Void> mIntentHandler =
            inv -> {
                Log.i(TAG, "onReceive(): intent=" + Arrays.toString(inv.getArguments()));
                Intent intent = inv.getArgument(1);
                String action = intent.getAction();
                BluetoothDevice device;
                switch (action) {
                    case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                        device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        int state =
                                intent.getIntExtra(
                                        BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
                        int transport =
                                intent.getIntExtra(
                                        BluetoothDevice.EXTRA_TRANSPORT,
                                        BluetoothDevice.TRANSPORT_AUTO);
                        Log.i(
                                TAG,
                                "Connection state change: device="
                                        + device
                                        + " "
                                        + BluetoothProfile.getConnectionStateName(state)
                                        + "("
                                        + state
                                        + "), transport: "
                                        + transport);
                        break;
                    case BluetoothDevice.ACTION_PAIRING_REQUEST:
                        device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        mBumble.getRemoteDevice().setPairingConfirmation(true);
                        Log.i(TAG, "onReceive(): setPairingConfirmation(true) for " + device);
                        break;
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int adapterState =
                                intent.getIntExtra(
                                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        Log.i(TAG, "Adapter state change:" + adapterState);
                        break;
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        int bondState =
                                intent.getIntExtra(
                                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR);
                        int prevBondState =
                                intent.getIntExtra(
                                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                                        BluetoothAdapter.ERROR);
                        Log.i(
                                TAG,
                                "onReceive(): device "
                                        + device
                                        + " bond state changed from "
                                        + prevBondState
                                        + " to "
                                        + bondState);
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        Log.i(TAG, "onReceive(): ACL Disconnected with device: " + device);
                        break;
                    case BluetoothDevice.ACTION_UUID:
                        device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        Parcelable[] uuidsRaw =
                                intent.getParcelableArrayExtra(
                                        BluetoothDevice.EXTRA_UUID, ParcelUuid.class);
                        if (uuidsRaw == null) {
                            Log.e(TAG, "onReceive(): device " + device + " null uuid list");
                        } else if (uuidsRaw.length == 0) {
                            Log.e(TAG, "onReceive(): device " + device + " 0 length uuid list");
                        } else {
                            ParcelUuid[] uuids =
                                    Arrays.copyOf(uuidsRaw, uuidsRaw.length, ParcelUuid[].class);
                            Log.d(
                                    TAG,
                                    "onReceive(): device "
                                            + device
                                            + ", UUID="
                                            + Arrays.toString(uuids));
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

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        sTargetContext.registerReceiver(mReceiver, filter);

        sAdapter.getProfileProxy(sTargetContext, mProfileServiceListener, BluetoothProfile.HEADSET);
        mHfpService = (BluetoothHeadset) verifyProfileServiceConnected(BluetoothProfile.HEADSET);

        mHfBlockingStub = mBumble.hfBlocking();
        mBumbleDevice = mBumble.getRemoteDevice();

        // Remove bond if the device is already bonded
        if (mBumbleDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            removeBond(mBumbleDevice);
        }

        // Create bond with bumble
        assertThat(mBumbleDevice.createBond(TRANSPORT_BREDR)).isTrue();

        // Allow headset service policy
        mHfpService.setConnectionPolicy(mBumbleDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        // Verify that bonding with bumble
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));
        // Verify that pairing request received from bumble
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice));
        // Verify that device is bonded
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));
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
     * Test HFP connection and disconnection from AG
     *
     * <ol>
     *   <li>1. Android initiates connection to the HF service of bumble
     *   <li>2. Check if profile state is changed to connecting
     *   <li>3. Check if profile state is changed to connected
     *   <li>4. Disconnect HFP connection from DUT
     *   <li>5. Check if profile state is changed to disconnecting
     *   <li>6. Check if profile state is changed to disconnected
     * </ol>
     */
    @Test
    public void HfpConnectionDisconnectionTestFromAG() {
        // Connect to Headset Services
        assertThat(mHfpService.connect(mBumbleDevice)).isTrue();

        // Verify that headset is in STATE_CONNECTING state
        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothProfile.EXTRA_STATE, STATE_CONNECTING));
        // Verify that headset is in STATE_CONNECTED state
        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothHeadset.EXTRA_STATE, STATE_CONNECTED));

        // Disconnect from the device
        assertThat(mHfpService.disconnect(mBumbleDevice)).isTrue();
        // Verify that headset is in STATE_DISCONNECTING state
        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothProfile.EXTRA_STATE, STATE_DISCONNECTING));
        // Verify that headset is in STATE_DISCONNECTED state
        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothHeadset.EXTRA_STATE, STATE_DISCONNECTED));
    }

    /**
     * Test HFP connection and disconnection from HF
     *
     * <ol>
     *   <li>1. HF initiates connection to the AG
     *   <li>2. Check if profile state is changed to connecting
     *   <li>3. Check if profile state is changed to connected
     *   <li>4. Disconnect HFP connection from HFs
     *   <li>5. Check if profile state is changed to disconnecting
     *   <li>6. Check if profile state is changed to disconnected
     * </ol>
     */
    @Test
    public void HfpConnectionDisconnectionFromHF() {
        // Connect HFP service from HF service
        mHfBlockingStub.connect(Empty.getDefaultInstance());

        // Verify that headset is in STATE_CONNECTED state
        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothHeadset.EXTRA_STATE, STATE_CONNECTED));

        // Disconnect HF service from HF
        mHfBlockingStub.disconnect(Empty.getDefaultInstance());

        // Verify that headset is in STATE_DISCONNECTED state
        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothHeadset.EXTRA_STATE, STATE_DISCONNECTED));
    }

    private void removeBond(BluetoothDevice device) {
        registerIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        assertThat(device.removeBond()).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, device),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        unregisterIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    }

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(INTENT_TIMEOUT.toMillis()))
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
            Log.d(TAG, "registerIntentActions(): unregister ALL intents");
            sTargetContext.unregisterReceiver(mReceiver);
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
        Log.d(TAG, "unregisterIntentActions(): unregister ALL intents");
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
                    .filter(entry -> entry.getValue() > 0)
                    .forEach(
                            entry -> {
                                Log.d(
                                        TAG,
                                        "unregisterIntentActions(): Registering action = "
                                                + entry.getKey());
                                filter.addAction(entry.getKey());
                            });
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

    private BluetoothProfile getProfileProxy(int profile) {
        sAdapter.getProfileProxy(sTargetContext, mProfileServiceListener, profile);
        ArgumentCaptor<BluetoothProfile> proxyCaptor =
                ArgumentCaptor.forClass(BluetoothProfile.class);
        verify(mProfileServiceListener, timeout(INTENT_TIMEOUT.toMillis()))
                .onServiceConnected(eq(profile), proxyCaptor.capture());
        return proxyCaptor.getValue();
    }

    private void verifyConnectionState(
            BluetoothDevice device, Matcher<Integer> transport, Matcher<Integer> state) {

        verifyIntentReceived(
                hasAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, device),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, transport),
                hasExtra(BluetoothProfile.EXTRA_STATE, state));
    }

    private BluetoothProfile verifyProfileServiceConnected(int profile) {
        ArgumentCaptor<BluetoothProfile> proxyCaptor =
                ArgumentCaptor.forClass(BluetoothProfile.class);
        verify(mProfileServiceListener, timeout(INTENT_TIMEOUT.toMillis()))
                .onServiceConnected(eq(profile), proxyCaptor.capture());
        return proxyCaptor.getValue();
    }
}
