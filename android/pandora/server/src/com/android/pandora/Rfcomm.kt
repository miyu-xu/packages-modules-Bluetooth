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
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.UUID
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.HostProto.*
import pandora.RFCOMMGrpc.RFCOMMImplBase
import pandora.RfcommProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Rfcomm(val context: Context) : RFCOMMImplBase() {
  // The UUID for Serial-Port Profile
  private val kSppUuid = "00001101-0000-1000-8000-00805f9b34fb"
  // TSPX_SERVICE_NAME_TESTER
  private val kRfcommServiceName = "COM5"

  private val kInvalidCookie = -(0xBAD)

  private val TAG = "PandoraRfcomm"

  private val scope: CoroutineScope

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  private var currentCookie = 0x12FC0 // Non-zero cookie RFCo(mm)

  private lateinit var serverSocket: BluetoothServerSocket
  private var serverSocketCookie = kInvalidCookie
  private lateinit var clientSocket: BluetoothSocket
  private var clientSocketCookie = kInvalidCookie
  private lateinit var acceptedSocket: BluetoothSocket
  private var acceptedSocketCookie = kInvalidCookie

  init {
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun connect(
    request: ConnectRequest,
    responseObserver: StreamObserver<RfcommConnectRsp>
  ) {
    grpcUnary<RfcommConnectRsp>(scope, responseObserver) {
      Log.i(TAG, "RFCOMM: connect: request=${request.address}")
      val device = request.address.toBluetoothDevice(bluetoothAdapter)
      clientSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(kSppUuid))
      try {
        clientSocket.connect()
      } catch (e: java.io.IOException) {
        Log.i(TAG, "connect threw ${e}.")
        throw Status.UNKNOWN.asException()
      }
      Log.i(TAG, "connected.")
      clientSocketCookie = currentCookie++

      RfcommConnectRsp.newBuilder()
        .setCookie(RfcommCookie.newBuilder().setId(clientSocketCookie).build())
        .build()
    }
  }

  override fun disconnect(
    request: RfcommDisconnect,
    responseObserver: StreamObserver<RfcommDisconnectRsp>
  ) {
    grpcUnary(scope, responseObserver) {
      val id = request.cookie.id
      Log.i(TAG, "RFCOMM: disconnect: request=${id}")
      if (id == kInvalidCookie) {
        throw Status.UNKNOWN.asException()
      }
      Log.i(TAG, "RFCOMM: disconnect: clientSocketCookie = ${clientSocketCookie}")
      Log.i(TAG, "RFCOMM: disconnect: acceptedSocketCookie = ${acceptedSocketCookie}")
      if (clientSocketCookie == id) {
        clientSocket.close()
        clientSocketCookie = kInvalidCookie
      } else if (acceptedSocketCookie == id) {
        acceptedSocket.close()
        acceptedSocketCookie = kInvalidCookie
      } else {
        throw Status.UNKNOWN.asException()
      }
      RfcommDisconnectRsp.newBuilder().build()
    }
  }

  override fun startServer(
    request: RfcommServerOptions,
    responseObserver: StreamObserver<RfcommStartServerRsp>
  ) {
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "startServer:")
      serverSocket =
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
          kRfcommServiceName,
          UUID.fromString(kSppUuid)
        )
      Log.i(TAG, "listening: serverSocket= $serverSocket")
      serverSocketCookie = currentCookie++

      RfcommStartServerRsp.newBuilder()
        .setCookie(RfcommCookie.newBuilder().setId(serverSocketCookie).build())
        .build()
    }
  }

  override fun acceptConnection(
    request: RfcommServerCookie,
    responseObserver: StreamObserver<RfcommAcceptConnectionRsp>
  ) {
    grpcUnary(scope, responseObserver) {
      assertEquals(request.cookie.id, serverSocketCookie)
      Log.i(TAG, "accepting: serverSocket= $serverSocket")
      try {
        acceptedSocket = serverSocket.accept(2000)
        Log.i(TAG, "accepted: acceptedSocket= $acceptedSocket")
        acceptedSocketCookie = currentCookie++
      } catch (e: java.io.IOException) {
        Log.i(TAG, "Caught an IOException while trying to accept.")
        acceptedSocketCookie = kInvalidCookie
      }

      Log.i(TAG, "after accept acceptedSocketCookie = $acceptedSocketCookie")
      RfcommAcceptConnectionRsp.newBuilder()
        .setCookie(RfcommCookie.newBuilder().setId(acceptedSocketCookie).build())
        .build()
    }
  }
}
