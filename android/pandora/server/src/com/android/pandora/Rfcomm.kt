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
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import pandora.RFCOMMGrpc.RFCOMMImplBase
import pandora.RfcommProto.*


@kotlinx.coroutines.ExperimentalCoroutinesApi
class Rfcomm(val context: Context) : RFCOMMImplBase() {

  private val _bufferSize = 512

  private val TAG = "PandoraRfcomm"

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  private var currentCookie = 0x12FC0 // Non-zero cookie RFCo(mm)

  data class Connection(val connection: BluetoothSocket, val inputStream: InputStream, val outputStream: OutputStream)

  private var serverMap: HashMap<Int,BluetoothServerSocket> = hashMapOf()
  private var connectionMap: HashMap<Int,Connection> = hashMapOf()

  fun deinit() {
    scope.cancel()
  }

  override fun connect(
    request: RfcommConnectRequest,
    responseObserver: StreamObserver<RfcommConnectResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "RFCOMM: connect: request=${request.address}")
      val device = request.address.toBluetoothDevice(bluetoothAdapter)
      val clientSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(request.uuid))
      try {
        clientSocket.connect()
      } catch(e: IOException) {
        Log.e(TAG, "connect threw ${e}.")
        throw Status.UNKNOWN.asException()
      }
      Log.i(TAG, "connected.")
      val connectedClientSocket = currentCookie++
      // Get the BluetoothSocket input and output streams
      try {
        val tmpIn = clientSocket.inputStream!!
        val tmpOut = clientSocket.outputStream!!
        connectionMap[connectedClientSocket] = Connection(clientSocket, tmpIn, tmpOut)
      } catch (e: IOException) {
        Log.e(TAG, "temp sockets not created", e)
      }

      RfcommConnectResponse.newBuilder()
        .setConnection(RfcommConnection.newBuilder().setId(connectedClientSocket).build())
        .build()
    }
  }

  override fun disconnect(
    request: RfcommDisconnectRequest,
    responseObserver: StreamObserver<RfcommDisconnectResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      val id = request.connection.id
      Log.i(TAG, "RFCOMM: disconnect: request=${id}")
      if (connectionMap.containsKey(id)) {
        connectionMap[id]!!.connection.close()
        connectionMap.remove(id)
      } else {
        throw Status.UNKNOWN.asException()
      }
      RfcommDisconnectResponse.newBuilder().build()
    }
  }

  override fun startServer(
    request: RfcommServerOptions,
    responseObserver: StreamObserver<RfcommStartServerResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "startServer")
      val serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(request.name, UUID.fromString(request.uuid))
      val serverSocketCookie = currentCookie++
      serverMap[serverSocketCookie] = serverSocket

      RfcommStartServerResponse.newBuilder().setServer(
      RfcommServer.newBuilder().setId(serverSocketCookie).build()).build()
    }
  }

  override fun acceptConnection(
    request: RfcommAcceptConnectionRequest,
    responseObserver: StreamObserver<RfcommAcceptConnectionResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "accepting: serverSocket= $(request.id)")
      val acceptedSocketCookie = currentCookie++
      try {
        val acceptedSocket : BluetoothSocket = serverMap[request.server.id]!!.accept(2000)
        Log.i(TAG, "accepted: acceptedSocket= $acceptedSocket")
        val tmpIn = acceptedSocket.inputStream!!
        val tmpOut = acceptedSocket.outputStream!!
        connectionMap[acceptedSocketCookie] = Connection(acceptedSocket, tmpIn, tmpOut)
      } catch (e: IOException) {
        Log.e(TAG, "Caught an IOException while trying to accept and create streams.")
      }

      Log.i(TAG, "after accept")
      RfcommAcceptConnectionResponse.newBuilder().setConnection(
      RfcommConnection.newBuilder().setId(acceptedSocketCookie).build()
      ).build()
    }
  }

  override fun send(
    request: RfcommTxRequest,
    responseObserver: StreamObserver<RfcommTxResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      if (request.data.isEmpty) {
        throw Status.UNKNOWN.asException()
      }
      val data = request.data!!.toByteArray()

      val socketOut = connectionMap[request.connection.id]!!.outputStream
      withContext(Dispatchers.IO) {
        try {
          socketOut.write(data)
          socketOut.flush()
        } catch (e: IOException) {
          Log.e(TAG, "Exception while writing output stream", e)
        }
      }
      Log.i(TAG, "Sent data")
      RfcommTxResponse.newBuilder().build()
    }
  }

  override fun receive(
    request: RfcommRxRequest,
    responseObserver: StreamObserver<RfcommRxResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      val data = ByteArray(_bufferSize)

      val socketIn = connectionMap[request.connection.id]!!.inputStream
      withContext(Dispatchers.IO) {
        try {
          socketIn.read(data)
        } catch (e: IOException) {
          Log.e(TAG, "Exception while reading from input stream", e)
        }
      }
      Log.i(TAG, "Read data")
      RfcommRxResponse.newBuilder().setData(ByteString.copyFrom(data)).build()
    }
  }
}
