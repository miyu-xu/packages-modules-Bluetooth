/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.HostProto.*
import pandora.RFCOMMGrpc.RFCOMMImplBase
import pandora.RfcommProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Rfcomm(val context: Context) : RFCOMMImplBase() {
  private val TAG = "PandoraRfcomm"

  private val scope: CoroutineScope

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  private var currentCookie = 0x12FC // Non-zero cookie RFC(omm)

  private lateinit var clientSocket: BluetoothSocket
  private var connectedClientSocketCookie = 0
  // The UUID for Serial-Port Profile
  private val kSppUuid = "00001101-0000-1000-8000-00805f9b34fb"

  init {
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    scope.cancel()
  }

  fun createClientSppSocket(address: ByteString): BluetoothSocket {
    val device = address.toBluetoothDevice(bluetoothAdapter)
    return device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(kSppUuid))
  }

  override fun connect(
    request: ConnectRequest,
    responseObserver: StreamObserver<RfcommConnectRsp>
  ) {
    grpcUnary<RfcommConnectRsp>(scope, responseObserver) {
      if (request.address == null || request.address.size() != 6) {
        Log.e(TAG, "RFCOMM: connect: Bad address parameter.")
        throw Status.UNKNOWN.asException()
      }
      Log.i(TAG, "RFCOMM: connect: request=${request.address}")
      val address = request.address!!
      clientSocket = createClientSppSocket(address)
      clientSocket.connect()
      Log.i(TAG, "connected.")
      connectedClientSocketCookie = currentCookie++

      RfcommConnectRsp.newBuilder()
        .setCookie(RfcommCookie.newBuilder().setId(connectedClientSocketCookie).build())
        .build()
    }
  }

  override fun startConnection(
    request: ConnectRequest,
    responseObserver: StreamObserver<RfcommStartConnectionRsp>
  ) {
    grpcUnary<RfcommStartConnectionRsp>(scope, responseObserver) {
      Log.i(TAG, "startConnectRfcommDevice: request=${request.address}")
      clientSocket = createClientSppSocket(request.address)
      assertFailsWith<java.io.IOException> { clientSocket.connect() }
      RfcommStartConnectionRsp.newBuilder()
        .setCookie(RfcommCookie.newBuilder().setId(0).build())
        .build()
    }
  }
}
