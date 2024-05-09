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

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.truth.Truth
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.timeout
import org.mockito.MockitoAnnotations
import org.mockito.hamcrest.MockitoHamcrest
import pandora.RfcommProto
import pandora.RfcommProto.ServerId
import pandora.RfcommProto.StartServerRequest
import pandora.SecurityProto.PairingEvent
import pandora.SecurityProto.PairingEventAnswer

@RunWith(AndroidJUnit4::class)
class RfcommTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mManager = mContext.getSystemService(BluetoothManager::class.java)
    private val mAdapter = mManager!!.adapter

    // Gives shell permissions during the test.
    @Rule
    @JvmField
    val mPermissionsRule =
        AdoptShellPermissionsRule(
            InstrumentationRegistry.getInstrumentation().getUiAutomation(),
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )

    // Set up a Bumble Pandora device for the duration of the test.
    @Rule @JvmField val mBumble = PandoraDevice()

    @Mock private val mReceiver: BroadcastReceiver? = null

    private lateinit var mBumbleDevice: BluetoothDevice
    private lateinit var mServer: ServerId
    private lateinit var mInOrder: InOrder
    private val mPairingEventStreamObserver: StreamObserverSpliterator<PairingEvent> =
        StreamObserverSpliterator()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mInOrder = inOrder(mReceiver)
        mBumbleDevice = mBumble.remoteDevice
        removeBondIfBonded(mBumbleDevice)
    }

    @Test
    @Throws(Exception::class)
    fun clientConnectToOpenServerSocketBondedInsecure() {
        mServer = startServer()
        bondDevice(mBumbleDevice)

        // Insecure connection to RFCOMM Server
        val insecureSocket =
            mBumbleDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        insecureSocket.connect()

        mBumble
            .rfcommBlocking()
            .acceptConnection(
                RfcommProto.AcceptConnectionRequest.newBuilder().setServer(mServer).build()
            )
        Truth.assertThat(insecureSocket.isConnected).isTrue()

        cleanUp()
    }

    @Test
    @Throws(Exception::class)
    fun clientConnectToOpenServerSocketBondedSecure() {
        mServer = startServer()

        bondDevice(mBumbleDevice)

        // Secure connection to RFCOMM Server
        val secureSocket =
            mBumbleDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        secureSocket.connect()

        mBumble
            .rfcommBlocking()
            .acceptConnection(
                RfcommProto.AcceptConnectionRequest.newBuilder().setServer(mServer).build()
            )
        Truth.assertThat(secureSocket.isConnected).isTrue()

        cleanUp()
    }

    @Test
    @Throws(Exception::class)
    fun clientSendDataOverInsecureSocket() {
        mServer = startServer()

        val (insecureSocket, connection) = createAndConnectSocket(isSecure = false)
        val data: ByteArray = "Test data for clientSendDataOverInsecureSocket".toByteArray()
        val socketOs = insecureSocket.outputStream

        socketOs.write(data)
        val rxResponse: RfcommProto.RxResponse =
            mBumble
                .rfcommBlocking()
                .receive(RfcommProto.RxRequest.newBuilder().setConnection(connection).build())
        Truth.assertThat(rxResponse.data).isEqualTo(ByteString.copyFrom(data))

        cleanUp()
    }

    @Test
    @Throws(Exception::class)
    fun clientSendDataOverSecureSocket() {
        mServer = startServer()

        val (secureSocket, connection) = createAndConnectSocket(isSecure = true)
        val data: ByteArray = "Test data for clientSendDataOverSecureSocket".toByteArray()
        val socketOs = secureSocket.outputStream

        socketOs.write(data)
        val rxResponse: RfcommProto.RxResponse =
            mBumble
                .rfcommBlocking()
                .receive(RfcommProto.RxRequest.newBuilder().setConnection(connection).build())
        Truth.assertThat(rxResponse.data).isEqualTo(ByteString.copyFrom(data))

        cleanUp()
    }

    @Test
    @Throws(Exception::class)
    fun clientReceiveDataOverInsecureSocket() {
        mServer = startServer()

        val (insecureSocket, connection) = createAndConnectSocket(isSecure = false)
        val buffer = ByteArray(64)
        val socketIs = insecureSocket.inputStream
        val data: ByteString =
            ByteString.copyFromUtf8("Test data for clientReceiveDataOverInsecureSocket")

        val txRequest =
            RfcommProto.TxRequest.newBuilder().setConnection(connection).setData(data).build()
        mBumble.rfcommBlocking().send(txRequest)
        val numBytesFromBumble = socketIs.read(buffer)
        Truth.assertThat(ByteString.copyFrom(buffer).substring(0, numBytesFromBumble))
            .isEqualTo(data)

        cleanUp()
    }

    @Test
    @Throws(Exception::class)
    fun clientReceiveDataOverSecureSocket() {
        mServer = startServer()

        val (secureSocket, connection) = createAndConnectSocket(isSecure = true)
        val buffer = ByteArray(64)
        val socketIs = secureSocket.inputStream
        val data: ByteString =
            ByteString.copyFromUtf8("Test data for clientReceiveDataOverSecureSocket")

        val txRequest =
            RfcommProto.TxRequest.newBuilder().setConnection(connection).setData(data).build()
        mBumble.rfcommBlocking().send(txRequest)
        val numBytesFromBumble = socketIs.read(buffer)
        Truth.assertThat(ByteString.copyFrom(buffer).substring(0, numBytesFromBumble))
            .isEqualTo(data)

        cleanUp()
    }

    private fun createAndConnectSocket(
        isSecure: Boolean
    ): Pair<BluetoothSocket, RfcommProto.RfcommConnection> {
        bondDevice(mBumbleDevice)

        val socket =
            if (isSecure) {
                mBumbleDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
            } else {
                mBumbleDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
            }
        socket.connect()

        val connectionResponse =
            mBumble
                .rfcommBlocking()
                .acceptConnection(
                    RfcommProto.AcceptConnectionRequest.newBuilder().setServer(mServer).build()
                )
        Truth.assertThat(socket.isConnected).isTrue()

        val connection = connectionResponse.connection
        return Pair(socket, connection)
    }

    private fun bondDevice(remoteDevice: BluetoothDevice) {
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
        Truth.assertThat(remoteDevice.createBond()).isTrue()
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

    private fun cleanUp() {
        mBumble
            .rfcommBlocking()
            .stopServer(RfcommProto.StopServerRequest.newBuilder().setServer(mServer).build())

        removeBondIfBonded(mBumbleDevice)
    }

    private fun removeBondIfBonded(deviceToRemove: BluetoothDevice) {
        val bondedDevices = mAdapter.getBondedDevices()
        if (!bondedDevices.contains(deviceToRemove)) {
            Log.d(TAG, "removeBondIfBonded(): Tried to remove a device that isn't bonded")
            return
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        mContext.registerReceiver(mReceiver, filter)
        Truth.assertThat(deviceToRemove.removeBond()).isTrue()
        verifyIntentReceived(
            AllOf.allOf(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, deviceToRemove),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
            )
        )
        mContext.unregisterReceiver(mReceiver)
    }

    private fun startServer(): ServerId {
        val request =
            StartServerRequest.newBuilder().setName(TEST_SERVER_NAME).setUuid(TEST_UUID).build()
        val response = mBumble.rfcommBlocking().startServer(request)

        return response.server
    }

    private fun verifyIntentReceived(matcher: Matcher<Intent>) {
        mInOrder
            .verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))!!
            .onReceive(ArgumentMatchers.any(Context::class.java), MockitoHamcrest.argThat(matcher))
    }

    companion object {
        private val TAG = RfcommTest::class.java.getSimpleName()
        private val BOND_INTENT_TIMEOUT = Duration.ofSeconds(10)
        private const val TEST_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val TEST_SERVER_NAME = "RFCOMM Server"
    }
}
