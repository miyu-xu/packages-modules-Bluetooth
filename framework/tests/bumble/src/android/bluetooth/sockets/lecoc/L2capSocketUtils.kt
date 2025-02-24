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

package android.bluetooth

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import pandora.HostProto.Connection
import pandora.l2cap.L2CAPProto.Channel
import pandora.l2cap.L2CAPProto.ConnectRequest
import pandora.l2cap.L2CAPProto.ConnectResponse
import pandora.l2cap.L2CAPProto.CreditBasedChannelRequest
import pandora.l2cap.L2CAPProto.DisconnectRequest
import pandora.l2cap.L2CAPProto.ReceiveRequest
import pandora.l2cap.L2CAPProto.ReceiveResponse
import pandora.l2cap.L2CAPProto.SendRequest
import pandora.l2cap.L2CAPProto.WaitConnectionRequest
import pandora.l2cap.L2CAPProto.WaitConnectionResponse
import pandora.l2cap.L2CAPProto.WaitDisconnectionRequest

/** L2CAP Socket Utilities */
public class L2capSocketUtils() : Closeable {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private var serviceDiscoveredFlow = MutableStateFlow(false)
    private var connectionStateFlow = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    private var dckSpsmFlow = MutableStateFlow(0)
    private var dckSpsm = 0
    private lateinit var connectionResponse: WaitConnectionResponse

    private fun clientSocketConnectUtil(isSecure: Boolean = false): Pair<BluetoothSocket, Channel> {
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM,
            )
        Log.d(TAG, "clientConnect: Connect L2CAP")
        val bluetoothSocket = createSocket(dckSpsm, remoteDevice, isSecure)
        runBlocking {
            val waitFlow = flow { emit(waitConnection(dckSpsm, remoteDevice)) }
            val connectJob =
                scope.launch {
                    // give some time for Bumble to host the socket server
                    Thread.sleep(200)
                    bluetoothSocket.connect()
                    Log.d(TAG, "clientConnect: Bluetooth socket connected")
                }
            connectionResponse = waitFlow.first()
            // Wait for the connection to complete
            connectJob.join()
        }
        assertThat(connectionResponse).isNotNull()
        assertThat(connectionResponse.hasChannel()).isTrue()
        assertThat((bluetoothSocket).isConnected()).isTrue()

        val channel = connectionResponse.channel
        return Pair(bluetoothSocket, channel)
    }

    private fun disconnectSocketAndWaitForDisconnectUtil(
        bluetoothSocket: BluetoothSocket,
        channel: Channel,
        isRemote: Boolean = false,
    ) {
        if (isRemote == false) {
            bluetoothSocket.close()
        } else {
            val disconnectRequest = DisconnectRequest.newBuilder().setChannel(channel).build()
            val disconnectResponse = mBumble.l2capBlocking().disconnect(disconnectRequest)
            assertThat(disconnectResponse.hasSuccess()).isTrue()
        }
        Log.d(TAG, "disconnectSocketAndWaitForDisconnectUtil: waitDisconnection")
        val waitDisconnectionRequest =
            WaitDisconnectionRequest.newBuilder().setChannel(channel).build()
        val disconnectionResponse =
            mBumble.l2capBlocking().waitDisconnection(waitDisconnectionRequest)
        assertThat(disconnectionResponse.hasSuccess()).isTrue()
    }

    private fun l2capServerOnPhoneAndConnectionFromBumbleUtil(
        isSecure: Boolean = false
    ): Pair<BluetoothSocket, Channel> {
        var bluetoothSocket: BluetoothSocket
        val channel: Channel
        val l2capServer = bluetoothAdapter.listenUsingInsecureL2capChannel()
        val socketFlow = flow { emit(l2capServer.accept()) }
        val connectResponse = createAndConnectL2capChannelWithBumble(l2capServer.psm)
        runBlocking {
            bluetoothSocket = socketFlow.first()
            assertThat(connectResponse.hasChannel()).isTrue()
        }

        return Pair(bluetoothSocket, connectResponse.channel)
    }

    private fun sendDataFromPhoneToBumbleAndVerifyUtil(
        bluetoothSocket: BluetoothSocket,
        channel: Channel,
    ) {
        val sampleData = "cafe-baguette".toByteArray()

        val receiveObserver = StreamObserverSpliterator<ReceiveResponse>()
        mBumble
            .l2cap()
            .receive(ReceiveRequest.newBuilder().setChannel(channel).build(), receiveObserver)

        Log.d(TAG, "sendDataFromPhoneToBumbleAndVerify: Send data from Android to Bumble")
        val outputStream = bluetoothSocket.outputStream
        outputStream.write(sampleData)
        outputStream.flush()

        Log.d(TAG, "sendDataFromPhoneToBumbleAndVerify: waitReceive data on Bumble")
        val receiveData = receiveObserver.iterator().next()
        assertThat(receiveData.data.toByteArray()).isEqualTo(sampleData)
        outputStream.close()
    }

    private fun sendDataFromBumbleToPhoneAndVerifyUtil(
        bluetoothSocket: BluetoothSocket,
        channel: Channel,
    ) {
        val inputStream = bluetoothSocket!!.inputStream
        val sampleData: ByteString = ByteString.copyFromUtf8("cafe-baguette")
        val buffer = ByteArray(sampleData.size())

        val sendRequest = SendRequest.newBuilder().setChannel(channel).setData(sampleData).build()
        Log.d(TAG, "sendDataFromBumbleToPhoneAndVerifyUtil: Send data from Bumble to Android")
        mBumble.l2capBlocking().send(sendRequest)

        Log.d(TAG, "sendDataFromBumbleToPhoneAndVerifyUtil: Receive data on Android")
        val read = inputStream.read(buffer)
        assertThat(ByteString.copyFrom(buffer).substring(0, read)).isEqualTo(sampleData)
        inputStream.close()
    }

    private fun createAndConnectL2capChannelWithBumble(psm: Int): ConnectResponse {
        Log.d(TAG, "createAndConnectL2capChannelWithBumble")
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM,
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

    private fun readDckSpsm(gatt: BluetoothGatt) = runBlocking {
        Log.d(TAG, "readDckSpsm")
        launch {
            withTimeout(GRPC_TIMEOUT) {
                connectionStateFlow.first { it == BluetoothProfile.STATE_CONNECTED }
            }
            Log.i(TAG, "Connected to GATT")
            gatt.discoverServices()
            withTimeout(GRPC_TIMEOUT) { serviceDiscoveredFlow.first { it == true } }
            Log.i(TAG, "GATT services discovered")
            val service = gatt.getService(CCC_DK_UUID)
            assertThat(service).isNotNull()
            val characteristic = service.getCharacteristic(SPSM_UUID)
            gatt.readCharacteristic(characteristic)
            withTimeout(GRPC_TIMEOUT) { dckSpsmFlow.first { it != 0 } }
            dckSpsm = dckSpsmFlow.value
            Log.i(TAG, "spsm read, spsm=$dckSpsm")
        }
    }

    private suspend fun waitConnection(
        psm: Int,
        remoteDevice: BluetoothDevice,
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
                .setConnection(connection)
                .setLeCreditBased(leCreditBased)
                .build()
        Log.i(TAG, "Sending request to Bumble to create server and wait for connection")
        return mBumble.l2capBlocking().waitConnection(waitConnectionRequest)
    }

    private fun createListeningChannelUsingSocketSettings(
        isEncrypted: Boolean = false,
        isAuthenticated: Boolean = false,
    ): BluetoothServerSocket {
        var socket: BluetoothServerSocket

        socket =
            bluetoothAdapter.listenUsingSocketSettings(
                BluetoothSocketSettings.Builder()
                    .setSocketType(BluetoothSocket.TYPE_L2CAP_LE)
                    .setEncryptionRequired(isEncrypted)
                    .setAuthenticationRequired(isAuthenticated)
                    .build()
            )

        return socket
    }

    private fun createClientSocketUsingSocketSettings(
        psm: Int,
        remoteDevice: BluetoothDevice,
        isEncrypted: Boolean = false,
        isAuthenticated: Boolean = false,
    ): BluetoothSocket {
        var socket: BluetoothSocket

        socket =
            remoteDevice.createUsingSocketSettings(
                BluetoothSocketSettings.Builder()
                    .setSocketType(BluetoothSocket.TYPE_L2CAP_LE)
                    .setEncryptionRequired(isEncrypted)
                    .setAuthenticationRequired(isAuthenticated)
                    .setL2capPsm(psm)
                    .build()
            )

        return socket
    }

    private fun createSocket(
        psm: Int,
        remoteDevice: BluetoothDevice,
        isSecure: Boolean = false,
    ): BluetoothSocket {
        var socket: BluetoothSocket
        var expectedType: Int
        if (isSecure) {
            socket = remoteDevice.createL2capChannel(psm)
            expectedType = BluetoothSocket.TYPE_L2CAP_LE
        } else {
            socket = remoteDevice.createInsecureL2capChannel(psm)
            expectedType = BluetoothSocket.TYPE_L2CAP
        }
        assertThat(socket.getConnectionType()).isEqualTo(expectedType)
        return socket
    }

    private fun connectGatt(remoteDevice: BluetoothDevice): BluetoothGatt {
        Log.d(TAG, "connectGatt")
        val gattCallback =
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int,
                ) {
                    Log.i(TAG, "Connection state changed to $newState.")
                    connectionStateFlow.value = newState
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

                    Log.i(TAG, "Discovering services status=$status")
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "Services have been discovered")
                        serviceDiscoveredFlow.value = true
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int,
                ) {
                    Log.i(TAG, "onCharacteristicRead, status: $status")

                    if (characteristic.getUuid() == SPSM_UUID) {
                        // CCC Specification Digital-Key R3-1.2.3
                        // 19.2.1.6 DK Service
                        dckSpsmFlow.value = byteArrayToInt(value, ByteOrder.BIG_ENDIAN)
                    }
                }
            }

        return remoteDevice.connectGatt(context, false, gattCallback)
    }

    fun byteArrayToInt(byteArray: ByteArray, order: ByteOrder): Int {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(order)
        return buffer.short.toInt()
    }

    private fun intToByteArray(value: Int, order: ByteOrder): ByteArray {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(order)
        buffer.putInt(value)
        return buffer.array()
    }

    companion object {
        private const val TAG = "L2capSocketUtils"
        private const val INITIAL_CREDITS = 256
        private const val MTU = 2048 // Default Maximum Transmission Unit.
        private const val MPS = 2048 // Default Maximum payload size.

        private val GRPC_TIMEOUT = 10.seconds
        private val CHANNEL_READ_TIMEOUT = 30.seconds

        // CCC DK Specification R3 1.2.0 r14 section 19.2.1.2 Bluetooth Le Pairing
        private val CCC_DK_UUID = UUID.fromString("0000FFF5-0000-1000-8000-00805f9b34fb")
        // Vehicule SPSM
        private val SPSM_UUID = UUID.fromString("D3B5A130-9E23-4B3A-8BE4-6B1EE5F980A3")
    }
}
