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
package android.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.truth.Truth
import io.grpc.stub.StreamObserver
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.timeout
import org.mockito.MockitoAnnotations
import org.mockito.hamcrest.MockitoHamcrest
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import pandora.RfcommProto
import pandora.RfcommProto.ServerId
import pandora.RfcommProto.StartServerRequest
import pandora.SecurityProto.PairingEvent
import pandora.SecurityProto.PairingEventAnswer

@RunWith(AndroidJUnit4::class)
class RfcommClientTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mManager = mContext.getSystemService(BluetoothManager::class.java)
    private val mAdapter = mManager!!.adapter

    // Gives shell permissions during the test.
    @Rule @JvmField val mPermissionsRule = AdoptShellPermissionsRule()

    // Set up a Bumble Pandora device for the duration of the test.
    @Rule @JvmField val mBumble = PandoraDevice()

    @Mock private val mReceiver: BroadcastReceiver? = null

    private var mBumbleDevice: BluetoothDevice? = null
    private var mServerId: ServerId? = null
    private var mInOrder: InOrder? = null
    private val mPairingEventStreamObserver: StreamObserverSpliterator<PairingEvent> =
        StreamObserverSpliterator()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Log.d(TAG, "Setup")
        MockitoAnnotations.initMocks(this)
        val answer: Answer<*> = Answer { inv: InvocationOnMock ->
            Log.d(TAG, "onReceive(): intent=" + inv.getArguments().contentToString())
            val intent = inv.getArgument<Intent>(1)
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                Log.d(TAG, "onReceive(): bondState=$bondState")
            }
            null
        }

        (doAnswer(answer).`when`(mReceiver))!!.onReceive(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
        )

        mInOrder = inOrder(mReceiver)

        mBumbleDevice = mBumble.remoteDevice
        removeBondIfBonded(mBumbleDevice)

        val request =
            StartServerRequest.newBuilder().setName("RFCOMM Server").setUuid(TEST_UUID).build()
        val response = mBumble.rfcommBlocking().startServer(request)
        mServerId = response.server
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        Log.d(TAG, "TearDown")
        mBumble
            .rfcommBlocking()
            .stopServer(RfcommProto.StopServerRequest.newBuilder().setServer(mServerId).build())
        removeBondIfBonded(mBumbleDevice)
        mBumbleDevice = null
    }

    @Test
    @Throws(Exception::class)
    fun connectToOpenServerSocketBondedInsecure() {
        Log.d(TAG, "ConnectToOpenServerSocketBondedInsecure")
        bondDevice(mBumbleDevice)

        // Insecure connection to RFCOMM Server
        val insecureSocket =
            mBumbleDevice!!.createInsecureRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        insecureSocket.connect()

        val connectionResponse =
            mBumble
                .rfcommBlocking()
                .acceptConnection(
                    RfcommProto.AcceptConnectionRequest.newBuilder().setServer(mServerId).build()
                )
        Truth.assertThat(connectionResponse.connection.id).isEqualTo(1)
        Truth.assertThat(insecureSocket.isConnected).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun connectToOpenServerSocketBondedSecure() {
        Log.d(TAG, "ConnectToOpenServerSocketBondedSecure")
        bondDevice(mBumbleDevice)

        // Secure connection to RFCOMM Server
        val secureSocket =
            mBumbleDevice!!.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        secureSocket.connect()

        val connectionResponse =
            mBumble
                .rfcommBlocking()
                .acceptConnection(
                    RfcommProto.AcceptConnectionRequest.newBuilder().setServer(mServerId).build()
                )
        Truth.assertThat(connectionResponse.connection.id).isEqualTo(1)
        Truth.assertThat(secureSocket.isConnected).isTrue()
    }

    private fun bondDevice(remoteDevice: BluetoothDevice?) {
        val bondingFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        mContext.registerReceiver(mReceiver, bondingFilter)
        val pairingFilter = IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
        mContext.registerReceiver(mReceiver, pairingFilter)

        val pairingEventAnswerObserver: StreamObserver<PairingEventAnswer>? =
            mBumble
                .security()
                .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .onPairing(mPairingEventStreamObserver)

        // create bond between DUT and Ref and verify we enter state BONDING
        Truth.assertThat(remoteDevice!!.createBond()).isTrue()
        verifyIntentReceived(
            AllOf.allOf(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, remoteDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING)
            )
        )

        // Pair
        verifyIntentReceived(
            AllOf.allOf(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, remoteDevice),
                hasExtra(
                    BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.PAIRING_VARIANT_CONSENT
                )
            )
        )
        remoteDevice.setPairingConfirmation(true)

        val pairingEvent: PairingEvent = mPairingEventStreamObserver.iterator().next()
        Truth.assertThat(pairingEvent.hasJustWorks()).isTrue()
        pairingEventAnswerObserver!!.onNext(
            PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build()
        )

        // Verify we are now bonded
        verifyIntentReceived(
            AllOf.allOf(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, remoteDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED)
            )
        )

        mContext.unregisterReceiver(mReceiver)
    }

    private fun removeBondIfBonded(deviceToRemove: BluetoothDevice?) {
        val bondedDevices = mAdapter.getBondedDevices()
        if (bondedDevices == null) {
            Log.d(TAG, "removeBondIfBonded(): no devices bonded")
            return
        } else if (!bondedDevices.contains(deviceToRemove)) {
            Log.d(TAG, "removeBondIfBonded(): Tried to remove a device that isn't bonded")
            return
        }
        if (bondedDevices.contains(deviceToRemove)) {
            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            mContext.registerReceiver(mReceiver, filter)
            Truth.assertThat(deviceToRemove!!.removeBond()).isTrue()
            verifyIntentReceived(
                AllOf.allOf(
                    hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                    hasExtra(BluetoothDevice.EXTRA_DEVICE, deviceToRemove),
                    hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                )
            )
        }
        mContext.unregisterReceiver(mReceiver)
    }

    private fun verifyIntentReceived(matcher: Matcher<Intent>) {
        mInOrder!!
            .verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))!!
            .onReceive(ArgumentMatchers.any(Context::class.java), MockitoHamcrest.argThat(matcher))
    }

    companion object {
        private val TAG = RfcommClientTest::class.java.getSimpleName()
        private val BOND_INTENT_TIMEOUT = Duration.ofSeconds(10)
        private const val TEST_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}
