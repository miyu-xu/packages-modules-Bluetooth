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
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.UUID
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

  private var currentCookie = 25

  private lateinit var serverSocket: BluetoothServerSocket
  private var serverSocketCookie = 0
  private lateinit var clientSocket: BluetoothSocket
  private var clientSocketCookie = 0
  private var connectedClientSocketCookie = 0
  private lateinit var acceptedSocket: BluetoothSocket
  private var acceptedSocketCookie = 0
  // The UUID for Serial-Port Profile
  private val kSppUuid = "00001101-0000-1000-8000-00805f9b34fb"

  init {
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    scope.cancel()
  }

  fun createClientSppSocket(address: ByteString) {
    Log.i(TAG, "RFCOMM: createSocket: cookie=${address}")
    val device = address.toBluetoothDevice(bluetoothAdapter)
    clientSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(kSppUuid))
    clientSocketCookie = currentCookie++
  }

  override fun connect(request: ConnectRequest, responseObserver: StreamObserver<RfcommCookie>) {
    grpcUnary<RfcommCookie>(scope, responseObserver) {
      if (request.address == null || request.address.size() != 6) {
        Log.e(TAG, "RFCOMM: connect: Bad address parameter.")
        throw Status.UNKNOWN.asException()
      }
      Log.i(TAG, "RFCOMM: connect: request=${request.address}")
      val address = request.address!!
      createClientSppSocket(address)
      clientSocket.connect()
      Log.i(TAG, "connected.")
      connectedClientSocketCookie = currentCookie++

      RfcommCookie.newBuilder().setId(connectedClientSocketCookie).build()
    }
  }

  override fun startConnection(request: ConnectRequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      var remoteDisconnected = false
      if (request.address == null || request.address.size() != 6) {
        Log.e(TAG, "RFCOMM: startConnection: Bad address parameter.")
        throw Status.UNKNOWN.asException()
      }
      Log.i(TAG, "startConnectRfcommDevice: request=${request.address}")
      createClientSppSocket(request.address)
      try {
        clientSocket.connect()
        Log.w(TAG, "connected.")
      } catch (e: java.io.IOException) {
        Log.i(TAG, "Caught an IOException while trying to connect.")
        remoteDisconnected = true
      }

      if (!remoteDisconnected) {
        Log.e(TAG, "Remote should not have connected.")
        throw Status.UNKNOWN.asException()
      }
      Empty.getDefaultInstance()
    }
  }
}
