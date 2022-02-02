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

package com.android.blueberry

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.MacAddress
import android.util.Log
import blueberry.HostGrpc.HostImplBase
import blueberry.HostProto.*
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class Host(private val context: Context) : HostImplBase() {
  private val TAG = "BlueberryHost"

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  // If running on an AVD, use default Rootcanal MAC address
  private val localMacAddress: MacAddress = MacAddress.fromString("DA:4C:10:DE:17:02")

  private lateinit var bluetoothDevice: BluetoothDevice

  override fun reset(request: Empty, responseObserver: StreamObserver<Empty>) {
    Log.i(TAG, "reset")

    runBlocking {
      val flow = callbackFlow {
        // Register broadcast receiver for callbacks
        val bluetoothBroadcastReceiver: BroadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
              when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_OFF -> {
                  bluetoothAdapter.enable()
                }
                BluetoothAdapter.STATE_ON -> {
                  trySendBlocking(null)
                }
              }
            }
          }
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothBroadcastReceiver, intentFilter)

        awaitClose { context.unregisterReceiver(bluetoothBroadcastReceiver) }
      }

      if (!bluetoothAdapter.isEnabled) {
        bluetoothAdapter.enable()
      } else {
        bluetoothAdapter.disable()
      }

      flow.first()

      responseObserver.onNext(Empty.getDefaultInstance())
      responseObserver.onCompleted()
    }
  }

  override fun readLocalAddress(
    request: Empty,
    responseObserver: StreamObserver<ReadLocalAddressResponse>
  ) {
    val res =
      ReadLocalAddressResponse.newBuilder()
        .setAddress(ByteString.copyFrom(localMacAddress.toByteArray()))
        .build()
    responseObserver.onNext(res)
    responseObserver.onCompleted()
  }

  override fun waitConnection(
    request: WaitConnectionRequest,
    responseObserver: StreamObserver<WaitConnectionResponse>
  ) {
    try {
      val address = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()

      Log.d(TAG, "waitConnection: address=$address")

      if (bluetoothAdapter.isEnabled) {
        runBlocking {
          val flow = callbackFlow {
            // Register broadcast receiver for callbacks
            val bluetoothBroadcastReceiver: BroadcastReceiver =
              object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                  val deviceState =
                    intent.getIntExtra(
                      BluetoothAdapter.EXTRA_CONNECTION_STATE,
                      BluetoothAdapter.ERROR
                    )
                  if (deviceState == BluetoothAdapter.STATE_CONNECTED) {
                    val device =
                      intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)!!
                    if (device.address == address) {
                      bluetoothDevice = device
                      trySendBlocking(true)
                    }
                  }
                  val pairingConfirmation =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR)
                  if (pairingConfirmation == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION) {
                    val device =
                      intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)!!
                    device.setPairingConfirmation(true)
                  }
                }
              }
            val intentFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            context.registerReceiver(bluetoothBroadcastReceiver, intentFilter)

            awaitClose { context.unregisterReceiver(bluetoothBroadcastReceiver) }
          }

          if (flow.first()) {
            val connection =
              Connection.newBuilder().setCookie(ByteString.copyFromUtf8(address)).build()

            val waitConnectionResponse =
              WaitConnectionResponse.newBuilder().setConnection(connection).build()

            responseObserver.onNext(waitConnectionResponse)
            responseObserver.onCompleted()
          }
        }

        return
      }
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, e.toString())
    }

    responseObserver.onError(Status.UNKNOWN.asException())
  }

  fun getConnectedBluetoothDevice() = bluetoothDevice
}
