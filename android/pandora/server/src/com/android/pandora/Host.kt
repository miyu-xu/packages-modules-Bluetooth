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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.MacAddress
import android.util.Log
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pandora.HostGrpc.HostImplBase
import pandora.HostProto.*

private const val TAG = "PandoraHost"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Host(private val context: Context, private val server: Server) : HostImplBase() {

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  init {
    IntentUtils.register(context)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun reset(request: Empty, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
        Log.i(TAG, "reset")

        bluetoothAdapter.clearBluetooth()

        if (bluetoothAdapter.isEnabled) {
          bluetoothAdapter.disable()
          IntentUtils.STATE_CHANGED.popIntent {
            BluetoothAdapter.STATE_OFF ==
              it.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
          }
        }

        // TODO: b/234892968
        delay(2000L)

        bluetoothAdapter.enable()
        IntentUtils.STATE_CHANGED.popIntent {
          BluetoothAdapter.STATE_ON ==
            it.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        }

        // The last expression is the return value.
        Empty.getDefaultInstance()
      }
      .invokeOnCompletion {
        Log.i(TAG, "Shutdown the gRPC Server")
        server.shutdownNow()
      }
  }

  override fun readLocalAddress(
    request: Empty,
    responseObserver: StreamObserver<ReadLocalAddressResponse>
  ) {
    grpcUnary<ReadLocalAddressResponse>(scope, responseObserver) {
      Log.i(TAG, "readLocalAddress")
      val localMacAddress = MacAddress.fromString(bluetoothAdapter.address)
      ReadLocalAddressResponse.newBuilder()
        .setAddress(ByteString.copyFrom(localMacAddress.toByteArray()))
        .build()
    }
  }

  private suspend fun waitPairingRequestIntent(bluetoothDevice: BluetoothDevice) {
    Log.i(TAG, "waitPairingRequestIntent: device=$bluetoothDevice")

    val pairingVariant =
      IntentUtils.PAIRING_REQUEST.popIntent(bluetoothDevice)
        .getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR)

    val confirmationCases =
      intArrayOf(
        BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION,
        BluetoothDevice.PAIRING_VARIANT_CONSENT,
        BluetoothDevice.PAIRING_VARIANT_PIN,
      )

    if (pairingVariant in confirmationCases) {
      bluetoothDevice.setPairingConfirmation(true)
    }
  }

  private fun waitBondIntent(bluetoothDevice: BluetoothDevice) {
    // We only wait for bonding to be completed since we only need the ACL connection to be
    // established with the peer device (on Android state connected is sent when all profiles
    // have been connected).
    Log.i(TAG, "waitBondIntent: device=$bluetoothDevice")
    IntentUtils.BOND_STATE_CHANGED.popIntent(bluetoothDevice) {
      BluetoothDevice.BOND_BONDED ==
        it.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR)
    }
  }

  private suspend fun waitConnectionIntent(bluetoothDevice: BluetoothDevice) {
    val acceptPairingJob = scope.launch { waitPairingRequestIntent(bluetoothDevice) }
    waitBondIntent(bluetoothDevice)
    if (acceptPairingJob.isActive) {
      acceptPairingJob.cancel()
    }
  }

  override fun waitConnection(
    request: WaitConnectionRequest,
    responseObserver: StreamObserver<WaitConnectionResponse>
  ) {
    grpcUnary<WaitConnectionResponse>(scope, responseObserver) {
      val bluetoothDevice = request.address.toBluetoothDevice(bluetoothAdapter)

      Log.i(TAG, "waitConnection: device=$bluetoothDevice")

      if (!bluetoothAdapter.isEnabled) {
        Log.e(TAG, "Bluetooth is not enabled, cannot waitConnection")
        throw Status.UNKNOWN.asException()
      }

      waitConnectionIntent(bluetoothDevice)

      WaitConnectionResponse.newBuilder()
        .setConnection(
          Connection.newBuilder()
            .setCookie(ByteString.copyFromUtf8(bluetoothDevice.address))
            .build()
        )
        .build()
    }
  }

  override fun connect(request: ConnectRequest, responseObserver: StreamObserver<ConnectResponse>) {
    grpcUnary<ConnectResponse>(scope, responseObserver) {
      val bluetoothDevice = request.address.toBluetoothDevice(bluetoothAdapter)

      Log.i(TAG, "connect: address=$bluetoothDevice")

      if (!bluetoothDevice.isConnected()) {
        bluetoothDevice.createBond()
        waitConnectionIntent(bluetoothDevice)
      }

      ConnectResponse.newBuilder()
        .setConnection(
          Connection.newBuilder()
            .setCookie(ByteString.copyFromUtf8(bluetoothDevice.address))
            .build()
        )
        .build()
    }
  }

  override fun deletePairing(
    request: DeletePairingRequest,
    responseObserver: StreamObserver<DeletePairingResponse>
  ) {
    grpcUnary<DeletePairingResponse>(scope, responseObserver) {
      val bluetoothDevice = request.address.toBluetoothDevice(bluetoothAdapter)
      Log.i(TAG, "DeletePairing: device=$bluetoothDevice")

      if (bluetoothDevice.removeBond()) {
        Log.i(TAG, "DeletePairing: device=$bluetoothDevice - wait BOND_NONE intent")
        IntentUtils.BOND_STATE_CHANGED.popIntent(bluetoothDevice) {
          BluetoothDevice.BOND_NONE ==
            it.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR) &&
            BluetoothDevice.BOND_SUCCESS ==
              it.getIntExtra(BluetoothDevice.EXTRA_REASON, BluetoothAdapter.ERROR)
        }
      } else {
        Log.i(TAG, "DeletePairing: device=$bluetoothDevice - Already unpaired")
      }
      DeletePairingResponse.getDefaultInstance()
    }
  }

  override fun disconnect(
    request: DisconnectRequest,
    responseObserver: StreamObserver<DisconnectResponse>
  ) {
    grpcUnary<DisconnectResponse>(scope, responseObserver) {
      val bluetoothDevice = request.connection.toBluetoothDevice(bluetoothAdapter)
      Log.i(TAG, "disconnect: device=$bluetoothDevice")

      if (!bluetoothDevice.isConnected()) {
        Log.e(TAG, "Device is not connected, cannot disconnect")
        throw Status.UNKNOWN.asException()
      }

      bluetoothDevice.disconnect()

      IntentUtils.CONNECTION_STATE_CHANGED.popIntent(bluetoothDevice) {
        BluetoothAdapter.STATE_DISCONNECTED ==
          it.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)
      }

      DisconnectResponse.getDefaultInstance()
    }
  }

  override fun connectLE(
      request: ConnectLERequest,
      responseObserver: StreamObserver<ConnectLEResponse>
  ) {
    grpcUnary<ConnectLEResponse>(scope, responseObserver) {
      val ptsAddress = request.address.decodeToString()
      Log.i(TAG, "connect LE: $ptsAddress")
      val device = scanLeDevice(ptsAddress)
      GattInstance(device!!, TRANSPORT_LE, context).waitForState(BluetoothProfile.STATE_CONNECTED)
      ConnectLEResponse.newBuilder()
        .setConnection(
          Connection.newBuilder()
            .setCookie(ByteString.copyFromUtf8(device.address))
            .build()
        )
        .build()
    }
  }

  override fun disconnectLE(request: DisconnectLERequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      val ptsAddress = request.connection.cookie.toByteArray().decodeToString()
      Log.i(TAG, "disconnect: $ptsAddress")
      GattInstance.get(ptsAddress).disconnectInstance()
      Empty.getDefaultInstance()
    }
  }

  private fun scanLeDevice(ptsAddress: String): BluetoothDevice? {
    Log.d(TAG, "scanLeDevice")
    var bluetoothDevice: BluetoothDevice? = null
    runBlocking {
      val flow = callbackFlow {
        val leScanCallback =
          object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
              super.onScanFailed(errorCode)
              Log.d(TAG, "onScanFailed: errorCode: $errorCode")
              trySendBlocking(null)
            }

            override fun onScanResult(callbackType: Int, result: ScanResult) {
              super.onScanResult(callbackType, result)
              val deviceAddress = result.device.address
              if (deviceAddress == ptsAddress) {
                Log.d(TAG, "found device address: $deviceAddress")
                trySendBlocking(result.device)
              }
            }
          }
          val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
          bluetoothLeScanner?.startScan(leScanCallback) ?: run { trySendBlocking(null) }
          awaitClose { bluetoothLeScanner?.stopScan(leScanCallback) }
      }
      bluetoothDevice = flow.first()
    }
    return bluetoothDevice
  }
}
