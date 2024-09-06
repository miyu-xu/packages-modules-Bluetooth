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
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.grpc.Deadline
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
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
import pandora.HostProto.Connection
import pandora.l2cap.L2CAPProto.ConnectRequest
import pandora.l2cap.L2CAPProto.ConnectResponse
import pandora.l2cap.L2CAPProto.CreditBasedChannelRequest
import pandora.l2cap.L2CAPProto.DisconnectRequest
import pandora.l2cap.L2CAPProto.SendRequest

/** Digital car key L2CAP Server Tests */
@RunWith(TestParameterInjector::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
public class DckL2capServerTest() : Closeable {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val gattCaptor = argumentCaptor<BluetoothGatt>()
    private val gattCallbackMock =
        mock<BluetoothGattCallback> {
            on { onConnectionStateChange(gattCaptor.capture(), any(), any()) } doAnswer {}
        }
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

        mBumble
            .dckBlocking()
            .withDeadline(Deadline.after(GRPC_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS))
            .register(Empty.getDefaultInstance())
        val advertiseContext = mBumble.advertise()

        val device =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )
        val gatt = device.connectGatt(context, false, gattCallbackMock)
        verify(gattCallbackMock, timeout(GRPC_TIMEOUT.inWholeMilliseconds))
            .onConnectionStateChange(
                eq(gatt),
                eq(BluetoothGatt.GATT_SUCCESS),
                eq(BluetoothProfile.STATE_CONNECTED)
            )
        advertiseContext.cancel(null)
        clearInvocations(gattCallbackMock)
    }

    @After
    fun tearDown() {
        for (gatt in gattCaptor.allValues.toSet()) {
            gatt.disconnect()
            gatt.close()
        }
    }

    @Test
    fun testSend() {
        Log.d(TAG, "testSend")
        Log.d(TAG, "testSend: Connect L2CAP")
        var bluetoothSocket: BluetoothSocket?
        val l2capServer = createL2capServer()
        val socketFlow = flow { emit(acceptConnection(l2capServer)) }
        val connectResponse = createL2capChannelWithBumble(l2capServer.psm)
        runBlocking {
            bluetoothSocket = socketFlow.first()
            assertThat(connectResponse.hasChannel()).isTrue()
        }

        val buffer = ByteArray(64)
        val inputStream = bluetoothSocket!!.inputStream
        val sampleData: ByteString = ByteString.copyFromUtf8("cafe-baguette")

        val sendRequest =
            SendRequest.newBuilder().setChannel(connectResponse.channel).setData(sampleData).build()
        Log.d(TAG, "testSend: Send data from Bumble to Android")
        mBumble.l2capBlocking().send(sendRequest)

        Log.d(TAG, "testSend: Read data on Android")
        val read = inputStream.read(buffer)
        assertThat(ByteString.copyFrom(buffer).substring(0, read)).isEqualTo(sampleData)

        Log.d(TAG, "testSend: disconnect")
        val disconnectRequest =
            DisconnectRequest.newBuilder().setChannel(connectResponse.channel).build()
        val disconnectResponse = mBumble.l2capBlocking().disconnect(disconnectRequest)
        assertThat(disconnectResponse.hasSuccess()).isTrue()
        inputStream.close()
        bluetoothSocket?.close()
        l2capServer.close()
        Log.d(TAG, "testSend: done")
    }

    private fun createL2capServer(secure: Boolean = false): BluetoothServerSocket {
        Log.d(TAG, "createL2capServer")
        return if (secure) {
            bluetoothAdapter.listenUsingL2capChannel()
        } else {
            bluetoothAdapter.listenUsingInsecureL2capChannel()
        }
    }

    private suspend fun acceptConnection(serverSocket: BluetoothServerSocket): BluetoothSocket {
        Log.d(TAG, "acceptConnection")
        return serverSocket.accept()
    }

    private fun createL2capChannelWithBumble(psm: Int): ConnectResponse {
        Log.d(TAG, "createL2capChannelWithBumble")
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )
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
        val connectRequest =
            ConnectRequest.newBuilder()
                .setConnection(connection)
                .setLeCreditBased(leCreditBased)
                .build()
        return mBumble.l2capBlocking().connect(connectRequest)
    }

    private fun intToByteArray(value: Int, order: ByteOrder): ByteArray {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(order)
        buffer.putInt(value)
        return buffer.array()
    }

    companion object {
        private const val TAG = "DckL2capServerTest"
        private const val INITIAL_CREDITS = 256
        private const val MTU = 2048 // Default Maximum Transmission Unit.
        private const val MPS = 2048 // Default Maximum payload size.
        private val GRPC_TIMEOUT = 10.seconds
    }
}
