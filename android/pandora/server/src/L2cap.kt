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

package com.android.pandora

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.l2cap.L2CAPGrpc.L2CAPImplBase
import pandora.l2cap.L2CAPProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class L2cap(val context: Context) : L2CAPImplBase(), Closeable {
    private val TAG = "PandoraL2cap"
    private val scope: CoroutineScope
    private val BLUETOOTH_SERVER_SOCKET_TIMEOUT: Int = 10000
    private val channelIdCounter = AtomicLong(1)
    private val BUFFER_SIZE = 512

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val channels: HashMap<Long, BluetoothSocket> = hashMapOf()

    init {
        // Init the CoroutineScope
        scope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
    }

    override fun close() {
        // Deinit the CoroutineScope
        scope.cancel()
    }

    override fun connect(
        request: ConnectRequest,
        responseObserver: StreamObserver<ConnectResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            Log.i(TAG, "connect")
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            val psm =
                when {
                    request.hasBasic() -> request.basic.psm
                    request.hasLeCreditBased() -> request.leCreditBased.spsm
                    request.hasEnhancedCreditBased() -> request.enhancedCreditBased.spsm
                    else -> throw RuntimeException("unreachable")
                }

            val bluetoothSocket = device.createInsecureL2capChannel(psm)
            bluetoothSocket.connect()
            val channelId = fetchNewChannelId()
            channels.put(channelId, bluetoothSocket)

            Log.d(TAG, "connect: channelId=$channelId")
            ConnectResponse.newBuilder().setChannel(craftChannel(channelId)).build()
        }
    }

    override fun waitConnection(
        request: WaitConnectionRequest,
        responseObserver: StreamObserver<WaitConnectionResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            val device: BluetoothDevice? =
                try {
                    request.connection.toBluetoothDevice(bluetoothAdapter)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                    null
                }

            Log.i(TAG, "waitConnection: $device")

            val psm =
                when {
                    request.hasBasic() -> request.basic.psm
                    request.hasLeCreditBased() -> request.leCreditBased.spsm
                    request.hasEnhancedCreditBased() -> request.enhancedCreditBased.spsm
                    else -> throw RuntimeException("unreachable")
                }

            var bluetoothSocket: BluetoothSocket?

            while (true) {
                val bluetoothServerSocket =
                    if (psm == 0) {
                        bluetoothAdapter.listenUsingInsecureL2capChannel()
                    } else {
                        bluetoothAdapter.listenUsingInsecureL2capOn(psm)
                    }
                bluetoothSocket = bluetoothServerSocket.accept()
                bluetoothServerSocket.close()
                if (device != null && !bluetoothSocket.getRemoteDevice().equals(device)) continue
                break
            }

            val channelId = fetchNewChannelId()
            channels.put(channelId, bluetoothSocket!!)

            Log.d(TAG, "waitConnection: channelId=$channelId")
            WaitConnectionResponse.newBuilder().setChannel(craftChannel(channelId)).build()
        }
    }

    override fun send(request: SendRequest, responseObserver: StreamObserver<SendResponse>) {
        grpcUnary(scope, responseObserver) {
            Log.i(TAG, "sendRequest")
            val bluetoothSocket = request.channel.toBluetoothSocket(channels)
            val outputStream = bluetoothSocket.outputStream

            outputStream.write(request.data.toByteArray())
            outputStream.flush()

            SendResponse.newBuilder().build()
        }
    }

    fun fetchNewChannelId(): Long = channelIdCounter.getAndIncrement()

    fun craftChannel(id: Long): Channel {
        val cookie = Any.newBuilder().setValue(ByteString.copyFromUtf8(id.toString())).build()
        val channel = Channel.newBuilder().setCookie(cookie).build()
        return channel
    }

    // Channel.newBuilder().setCookie(Any.parseFrom(ByteString.copyFrom(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(id).array()))).build()

    fun Channel.toBluetoothSocket(channels: HashMap<Long, BluetoothSocket>): BluetoothSocket =
        channels.get(ByteBuffer.wrap(this.cookie.value.toByteArray()).getLong())!!
}
