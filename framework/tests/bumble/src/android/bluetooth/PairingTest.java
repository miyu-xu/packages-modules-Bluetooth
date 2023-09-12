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

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.cts.BTAdapterUtils;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pandora.HostGrpc;
import pandora.HostProto;
import pandora.SecurityGrpc;
import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;

@RunWith(AndroidJUnit4.class)
public class PairingTest {
    private static final String TAG = PairingTest.class.getSimpleName();
    private static final int TIMEOUT_MS = 10000;
    @Rule public final PandoraDevice mBumble = new PandoraDevice();
    private HostGrpc.HostBlockingStub mHostBlockingStub;
    private SecurityGrpc.SecurityStub mSecurityStub;

    private BluetoothDevice mBumbleDevice;
    private BluetoothAdapter mAdapter;
    private Context mTargetContext;
    private Utils.DeviceBasedBroadcastReceiver mReceiver;

    /** Before class */
    @BeforeClass
    public static void setUpClass() {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();
    }

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mHostBlockingStub = mBumble.hostBlocking();
        mSecurityStub = mBumble.security();

        HostProto.ReadLocalAddressResponse readLocalAddressResponse =
                mHostBlockingStub.withWaitForReady().readLocalAddress(Empty.getDefaultInstance());
        ByteString bumbleBrEdrAddress = readLocalAddressResponse.getAddress();
        assertThat(bumbleBrEdrAddress).isNotNull();

        Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        mAdapter = Objects.requireNonNull(bluetoothManager).getAdapter();
        assertThat(mAdapter).isNotNull();

        mBumbleDevice = mAdapter.getRemoteDevice(bumbleBrEdrAddress.toByteArray());

        assertThat(BTAdapterUtils.enableAdapter(mAdapter, mTargetContext)).isTrue();

        mReceiver = new Utils.DeviceBasedBroadcastReceiver();
        mReceiver.addDevice(mBumbleDevice);

        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mTargetContext.registerReceiver(mReceiver, filter);

        if (Objects.requireNonNull(mAdapter.getBondedDevices()).contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (Objects.requireNonNull(mAdapter.getBondedDevices()).contains(mBumbleDevice)) {
            // no need to wait for intent since we are turning off adapter
            mBumbleDevice.removeBond();
        }
        assertThat(BTAdapterUtils.disableAdapter(mAdapter, mTargetContext)).isTrue();
        if (mReceiver != null) {
            mTargetContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    /** After class */
    @AfterClass
    public static void tearDownClass() {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .dropShellPermissionIdentity();
    }

    @Test
    public void testClassicPairing() {
        final BlockingQueue<PairingEvent> pairingEvents = new LinkedBlockingQueue<>();
        StreamObserver<PairingEvent> pairingEventObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(PairingEvent value) {
                        try {
                            pairingEvents.put(value);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {}
                };
        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mSecurityStub.onPairing(pairingEventObserver);

        assertThat(mBumbleDevice.createBond()).isTrue();
        Intent bondingIntent = Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(bondingIntent).isNotNull();
        assertThat(bondingIntent.getAction()).isEqualTo(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        assertThat(bondingIntent.hasExtra(BluetoothDevice.EXTRA_BOND_STATE)).isTrue();
        assertThat(
                        bondingIntent.getIntExtra(
                                BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE))
                .isEqualTo(BluetoothDevice.BOND_BONDING);

        Intent pairingRequestIntent =
                Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(pairingRequestIntent).isNotNull();
        assertThat(pairingRequestIntent.getAction())
                .isEqualTo(BluetoothDevice.ACTION_PAIRING_REQUEST);
        assertThat(pairingRequestIntent.hasExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT)).isTrue();
        assertThat(
                        pairingRequestIntent.getIntExtra(
                                BluetoothDevice.EXTRA_PAIRING_VARIANT,
                                BluetoothDevice.PAIRING_VARIANT_PASSKEY))
                .isEqualTo(BluetoothDevice.PAIRING_VARIANT_CONSENT);
        mBumbleDevice.setPairingConfirmation(true);

        PairingEvent pairingEvent = Utils.waitForItem(TIMEOUT_MS, pairingEvents);
        assertThat(pairingEvent).isNotNull();
        assertThat(pairingEvent.getJustWorks()).isNotNull();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        Intent bondedIntent = Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(bondedIntent).isNotNull();
        assertThat(bondedIntent.getAction()).isEqualTo(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        assertThat(bondedIntent.hasExtra(BluetoothDevice.EXTRA_BOND_STATE)).isTrue();
        assertThat(
                        bondedIntent.getIntExtra(
                                BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE))
                .isEqualTo(BluetoothDevice.BOND_BONDED);

        removeBond(mBumbleDevice);
    }

    private void removeBond(BluetoothDevice device) {
        assertThat(device.removeBond()).isTrue();
        Intent unbondIntent = Utils.waitForItem(TIMEOUT_MS, mReceiver.getQueue(mBumbleDevice));
        assertThat(unbondIntent).isNotNull();
        assertThat(unbondIntent.getAction()).isEqualTo(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        assertThat(unbondIntent.hasExtra(BluetoothDevice.EXTRA_BOND_STATE)).isTrue();
        assertThat(
                        unbondIntent.getIntExtra(
                                BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE))
                .isEqualTo(BluetoothDevice.BOND_NONE);
    }
}
