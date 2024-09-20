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
import android.bluetooth.test_utils.EnableBluetoothRule
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.google.protobuf.Any
import com.google.protobuf.Empty
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pandora.HostProto.OwnAddressType;
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pandora.HostProto.Connection
import pandora.l2cap.L2CAPProto.CreditBasedChannelRequest
import pandora.l2cap.L2CAPProto.ReceiveRequest
import pandora.l2cap.L2CAPProto.ReceiveResponse
import pandora.l2cap.L2CAPProto.WaitConnectionRequest
import pandora.l2cap.L2CAPProto.WaitConnectionResponse
import pandora.l2cap.L2CAPProto.WaitDisconnectionRequest
import pandora.HostProto.AdvertiseRequest
import pandora.HostProto.AdvertiseResponse
import io.grpc.Context as GrpcContext

/** L2CAP Client Tests */
@RunWith(TestParameterInjector::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
public class L2capClientSocketTests() : Closeable {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val openedGatts: MutableList<BluetoothGatt> = mutableListOf()
    private var serviceDiscoveredFlow = MutableStateFlow(false)
    private var connectionStateFlow = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    private var connectionHandle = BluetoothDevice.ERROR
    private lateinit var advertiseContext: GrpcContext.CancellableContext
    private lateinit var connectionResponse: WaitConnectionResponse
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mHost = Host(mContext)
    private val mManager = mContext.getSystemService(BluetoothManager::class.java)
    private val mAdapter = mManager!!.adapter
    //private val mAdvertiseResponse : AdvertiseResponse;

    // Gives shell permissions during the test.
    @Rule(order = 0)
    @JvmField
    val mPermissionRule =
        AdoptShellPermissionsRule(
            InstrumentationRegistry.getInstrumentation().getUiAutomation(),
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        )

    // Setup a Bumble Pandora device for the duration of the test.
    @Rule(order = 1) @JvmField val mBumble = PandoraDevice()

    // Toggles Bluetooth.
    @Rule(order = 2) @JvmField val EnableBluetoothRule = EnableBluetoothRule(false, true)

    override fun close() {
        scope.cancel("Cancelling test scope")
    }

    @Before
    fun setUp() {
        // Advertise the Bumble
        //advertiseContext = mBumble.advertise()


        // Connect to LE
        //val remoteDevice =
        //    bluetoothAdapter.getRemoteLeDevice(
        //        Utils.BUMBLE_RANDOM_ADDRESS,
        //        BluetoothDevice.ADDRESS_TYPE_RANDOM
        //    )
        //val gatt = connectGatt(remoteDevice)
    }


    @After
    fun tearDown() {
        //advertiseContext.cancel(null)
    }

    private fun setupAclConnection () : Connection {
        // Start GATT service discovery, this will establish LE ACL
        assertThat(mBumble.getRemoteDevice().fetchUuidsWithSdp(BluetoothDevice.TRANSPORT_LE)).isTrue();

        // Make Bumble connectable
        val advertiseResponse =
            mBumble.hostBlocking()
                .advertise(
                    AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(OwnAddressType.PUBLIC)
                        .build())
                    .next()

        return advertiseResponse.getConnection()
    }

    private suspend fun waitConnection(
        psm: Int,
        remoteDevice: BluetoothDevice,
        conn: Connection
    ): WaitConnectionResponse {
        Log.d(TAG, "waitConnection")
        val connectionHandle = remoteDevice.getConnectionHandle(BluetoothDevice.TRANSPORT_LE)
        val handle = intToByteArray(connectionHandle, ByteOrder.BIG_ENDIAN)
        val cookie = Any.newBuilder().setValue(ByteString.copyFrom(handle)).build()
        val connection = Connection.newBuilder().setCookie(cookie).build()
        val leCreditBased =
            CreditBasedChannelRequest.newBuilder()
                .setSpsm(psm)
                .setInitialCredit(INITIAL_CREDITS)
                .setMtu(MTU)
                .setMps(MPS)
                .build()
        val waitConnectionRequest =
            WaitConnectionRequest.newBuilder()
                .setConnection(conn)
                .setLeCreditBased(leCreditBased)
                .build()
        Log.i(TAG, "Sending request to Bumble to create server and wait for connection")
        return mBumble.l2capBlocking().waitConnection(waitConnectionRequest)
    }

    private fun createSocket(
        psm: Int,
        remoteDevice: BluetoothDevice,
        isSecure: Boolean = false,
        isEncryptedOnly: Boolean = false
    ): BluetoothSocket {
        var socket: BluetoothSocket

        if (isEncryptedOnly) {
            //socket = remoteDevice.createEncryptedL2capChannel(psm)
            socket = remoteDevice.createL2capChannel(psm)
        } else if (isSecure) {
            socket = remoteDevice.createL2capChannel(psm)
        } else {
            socket = remoteDevice.createInsecureL2capChannel(psm)
        }
        return socket
    }

    private fun intToByteArray(value: Int, order: ByteOrder): ByteArray {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(order)
        buffer.putInt(value)
        return buffer.array()
    }

    fun sendDataOnL2capSocket(
        isSecure: Boolean = false,
        isEncryptedOnly: Boolean = false) {
        Log.d(TAG, "sendDataOnL2capSocket")
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )

        val conn = setupAclConnection()

        if (isSecure || isEncryptedOnly) {
            Log.d(TAG, "sendDataOnL2capSocket: ensure devices are bonded");
            mHost.createBondAndVerify(remoteDevice)
        }
        Log.d(TAG, "sendDataOnL2capSocket: Connect L2CAP")
        val bluetoothSocket = createSocket(TEST_PSM, remoteDevice, isSecure, isEncryptedOnly)
        runBlocking {
            val waitFlow = flow { emit(waitConnection(TEST_PSM, remoteDevice, conn)) }
            val connectJob =
                scope.launch {
                    Log.d(TAG, "calling connect");
                    bluetoothSocket.connect()
                    Log.d(TAG, "sendDataOnL2capSocket: Bluetooth socket connected")
                }
            connectionResponse = waitFlow.first()
            // Wait for the connection to complete
            connectJob.join()
        }
        assertThat(connectionResponse).isNotNull()
        assertThat(connectionResponse.hasChannel()).isTrue()

        val channel = connectionResponse.channel
        val sampleData = "SAMPLE SOCKET DATA".toByteArray()

        val receiveObserver = StreamObserverSpliterator<ReceiveResponse>()
        mBumble
            .l2cap()
            .receive(ReceiveRequest.newBuilder().setChannel(channel).build(), receiveObserver)

        Log.d(TAG, "sendDataOnL2capSocket: Send data from Android to Bumble")
        val outputStream = bluetoothSocket.outputStream
        outputStream.write(sampleData)
        outputStream.flush()

        Log.d(TAG, "sendDataOnL2capSocket: waitReceive data on Bumble")
        val receiveData = receiveObserver.iterator().next()
        Log.d(TAG, "sendDataOnL2capSocket: rcvd data at bumble: " + receiveData.data.toByteArray());
        assertThat(receiveData.data.toByteArray()).isEqualTo(sampleData)

        bluetoothSocket.close()
        Log.d(TAG, "sendDataOnL2capSocket: waitDisconnection")
        val waitDisconnectionRequest =
            WaitDisconnectionRequest.newBuilder().setChannel(channel).build()
        val disconnectionResponse =
            mBumble.l2capBlocking().waitDisconnection(waitDisconnectionRequest)
        assertThat(disconnectionResponse.hasSuccess()).isTrue()

        if (mAdapter.bondedDevices.contains(remoteDevice)) {
            mHost.removeBondAndVerify(remoteDevice)
        }
        Log.d(TAG, "sendDataOnL2capSocket: done")
    }

    @Test
    fun testSendDataOnInsecureSockets() {
        Log.d(TAG, "testSendDataOnInsecureSockets")
        sendDataOnL2capSocket(false, false);
        Log.d(TAG, "testSendDataOnInsecureSockets: done")
    }

    @Test
    @Ignore
    fun testSendDataOnSecureSocket() {
        Log.d(TAG, "testSendDataOnSecureSocket")
        sendDataOnL2capSocket(true, false);
        Log.d(TAG, "testSendDataOnSecureSocket: done")
    }

    @Test
    @Ignore
    fun testSendDataOnEncryptedSocket() {
        Log.d(TAG, "testSendDataOnEncryptedSocket")
        sendDataOnL2capSocket(false, true);
        Log.d(TAG, "testSendDataOnEncryptedSocket: done")
    }

    companion object {
        private const val TAG = "L2capClientSocketTests"
        private const val INITIAL_CREDITS = 256
        private const val MTU = 2048 // Default Maximum Transmission Unit.
        private const val MPS = 2048 // Default Maximum payload size.
        private const val TEST_PSM = 0x80

        private val GRPC_TIMEOUT = 10.seconds
    }
}
