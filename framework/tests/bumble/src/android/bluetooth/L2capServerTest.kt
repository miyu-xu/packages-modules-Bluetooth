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

package android.bluetooth

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.grpc.Context as GrpcContext
import io.grpc.Deadline
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import pandora.HostProto
import pandora.HostProto.AdvertiseRequest
import pandora.HostProto.Connection
import pandora.HostProto.OwnAddressType
import pandora.l2cap.L2CAPProto.ConnectRequest
import pandora.l2cap.L2CAPProto.CreditBasedChannelRequest

/** L2CAP Server Tests */
@RunWith(TestParameterInjector::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
public class L2capServerTest() : Closeable {

    private val scope: CoroutineScope
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private var connectionHandle = BluetoothDevice.ERROR
    private val gattCaptor = argumentCaptor<BluetoothGatt>()
    private val gattCallbackMock =
        mock<BluetoothGattCallback> {
            on { onConnectionStateChange(gattCaptor.capture(), any(), any()) } doAnswer {}
        }

    // A Rule live from a test setup through it's teardown.
    // Gives shell permissions during the test.
    @Rule
    @JvmField
    val mPermissionRule =
        AdoptShellPermissionsRule(
            InstrumentationRegistry.getInstrumentation().getUiAutomation(),
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        )

    // Setup a Bumble Pandora device for the duration of the test.
    // Acting as a Pandora client, it can be interacted with through the Pandora APIs.
    @Rule @JvmField val mBumble = PandoraDevice()

    init {
        scope = CoroutineScope(Dispatchers.Default)
    }

    override fun close() {
        scope.cancel("Cancelling test scope")
    }

    @Before
    fun setUp() {
        // 1. Register Bumble's DCK (Digital Car Key) service via a gRPC call:
        // - `dckBlocking()` is likely a stub that accesses the DCK service over gRPC in a
        //   blocking/synchronous manner.
        // - `withDeadline(Deadline.after(TIMEOUT, TimeUnit.MILLISECONDS))` sets a timeout for the
        //   gRPC call.
        // - `register(Empty.getDefaultInstance())` sends a registration request to the server.
        mBumble
            .dckBlocking()
            .withDeadline(Deadline.after(SERVICE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
            .register(Empty.getDefaultInstance())

        val advertiseContext = advertiseWithBumble()

        // Connect DUT to Ref as prerequisite
        val device =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )
        val gatt = device.connectGatt(context, false, gattCallbackMock)
        verify(gattCallbackMock, timeout(SERVICE_TIMEOUT.toMillis()))
            .onConnectionStateChange(
                eq(gatt),
                eq(BluetoothGatt.GATT_SUCCESS),
                eq(BluetoothProfile.STATE_CONNECTED)
            )
        advertiseContext.cancel(null)

        // Wait a bit for the advertising to stop.
        // b/332322761
        Thread.sleep(1000)

        clearInvocations(gattCallbackMock)
    }

    @After fun tearDown() {}

    /** Tests creating an L2CAP channel on a Bumble l2cap server. */
    @Test
    fun testConnect() {

        Log.i(TAG, "In testConnect")

        val l2capServer = createL2capServer()
        acceptConnections(l2capServer)
        createL2capChannelWithBumble(l2capServer.psm)
        Log.i(TAG, "End testConnect")
    }

    private fun createL2capServer(secure: Boolean = false): BluetoothServerSocket {
        return if (secure) {
            bluetoothAdapter.listenUsingL2capChannel()
        } else {
            bluetoothAdapter.listenUsingInsecureL2capChannel()
        }
    }

    private fun acceptConnections(serverSocket: BluetoothServerSocket) {

        val psm = serverSocket.psm
        Log.i(TAG, "LE PSM = $psm")

        scope.launch {
            var keepAlive = true
            while (keepAlive) {
                try {
                    Log.i(TAG, "Waiting for connection...")
                    val socket: BluetoothSocket? = serverSocket.accept()
                    socket?.let {
                        Log.i(TAG, "Accepted connection from ${socket.remoteDevice.address}")
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "No longer accepting connections: ${e.message}")
                    keepAlive = false
                } finally {
                    try {
                        serverSocket.close()
                    } catch (e: IOException) {
                        Log.w(TAG, "Error closing the server socket")
                    }
                }
            }
        }
    }

    private fun createL2capChannelWithBumble(psm: Int) {
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )

        val connectionHandle = remoteDevice.getConnectionHandle(BluetoothDevice.TRANSPORT_LE)
        val handle = intToByteArray(connectionHandle, ByteOrder.BIG_ENDIAN)
        val cookie = Any.newBuilder().setValue(ByteString.copyFrom(handle)).build()
        val connection = Connection.newBuilder().setCookie(cookie).build()
        Log.i(TAG, "Connecting from Bumble.")
        val leCreditBased =
            CreditBasedChannelRequest.newBuilder()
                .setSpsm(psm)
                .setInitialCredit(INITIAL_CREDITS)
                .setMtu(MTU)
                .setMps(MPS)
                .build()
        val connectRequest =
            ConnectRequest.newBuilder()
                .setConnection(connection)
                .setLeCreditBased(leCreditBased)
                .build()

        val connectResponse = mBumble.l2capBlocking().connect(connectRequest)
        Log.i(TAG, "ConnectResponse: $connectResponse")
        assertThat(connectResponse.hasChannel()).isTrue()
    }

    private fun advertiseWithBumble(
        ownAddressType: OwnAddressType = OwnAddressType.RANDOM,
        withUuid: Boolean = false
    ): GrpcContext.CancellableContext {
        val requestBuilder =
            AdvertiseRequest.newBuilder()
                .setLegacy(true)
                .setConnectable(true)
                .setOwnAddressType(ownAddressType)

        if (withUuid) {
            requestBuilder.data =
                HostProto.DataTypes.newBuilder()
                    .addCompleteServiceClassUuids128(TEST_SERVICE_UUID.toString())
                    .build()
        }

        val cancellableContext = GrpcContext.current().withCancellation()
        with(cancellableContext) {
            run { mBumble.hostBlocking().advertise(requestBuilder.build()) }
        }

        return cancellableContext
    }

    fun byteArrayToInt(byteArray: ByteArray, order: ByteOrder): Int {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(order)
        return buffer.short.toInt()
    }

    private fun intToByteArray(value: Int, order: ByteOrder): ByteArray {
        val buffer = ByteBuffer.allocate(4) // Allocate 4 bytes for an Int
        buffer.order(order)
        buffer.putInt(value)
        return buffer.array()
    }

    companion object {
        private const val TAG = "L2capServerTest"
        private const val TEST_ADDRESS_RANDOM_STATIC = "F0:43:A8:23:10:11"
        private const val TEST_PSM = 128
        private const val INITIAL_CREDITS = 256
        private const val MTU = 2048 // Default Maximum Transmission Unit.
        private const val MPS = 2048 // Default Maximum payload size.

        private val SERVICE_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val ADVERTISING_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val TEST_SERVICE_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000")
        private val TEST_CHARACTERISTIC_UUID =
            UUID.fromString("00010001-0000-0000-0000-000000000000")

        // CCC DK Specification R3 1.2.0 r14 section 19.2.1.2 Bluetooth Le Pairing
        private val CCC_DK_UUID = UUID.fromString("0000FFF5-0000-1000-8000-00805f9b34fb")
        // Vehicule SPSM
        private val SPSM_UUID = UUID.fromString("D3B5A130-9E23-4B3A-8BE4-6B1EE5F980A3")
    }
}
