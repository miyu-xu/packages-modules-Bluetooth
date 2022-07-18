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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import com.google.protobuf.Empty
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import pandora.SMGrpc.SMImplBase
import pandora.HostProto.*
import pandora.SmProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Sm(private val context: Context) : SMImplBase() {
  private val TAG = "PandoraSm"

  private val scope: CoroutineScope
  private val flow: Flow<Intent>

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter
  private val gattClients: MutableMap<String, BluetoothGatt> = mutableMapOf<String, BluetoothGatt>()

  init {
    scope = CoroutineScope(Dispatchers.Default)

    val intentFilter = IntentFilter()
    intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)

    flow = intentFlow(context, intentFilter).shareIn(scope, SharingStarted.Eagerly)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun connectLE(
      request: ConnectLERequest,
      responseObserver: StreamObserver<ConnectLEResponse>
  ) {
    grpcUnary<ConnectLEResponse>(scope, responseObserver) {
      val ptsAddress = request.address.decodeToString()
      Log.i(TAG, "connect: $ptsAddress")
      val device = scanLeDevice(ptsAddress)
      gattConnect(device!!)
      ConnectLEResponse.newBuilder()
        .setConnection(
          Connection.newBuilder()
            .setCookie(ByteString.copyFromUtf8(device.address))
            .build()
        )
        .build()
    }
  }

  override fun acceptPairingConfirmationDialog(
      request: PairingConfirmRequest,
      responseObserver: StreamObserver<Empty>
  ) {
    grpcUnary<Empty>(scope, responseObserver) {
      val ptsAddress = request.connection.cookie.toByteArray().decodeToString()
      Log.i(TAG, "Accept Pairing Request Action: address=$ptsAddress")
      val acceptPairingJob =
        scope.launch {
          var pairingVariant =
            flow
              .filter { it.getAction() == BluetoothDevice.ACTION_PAIRING_REQUEST }
              .filter { it.getBluetoothDeviceExtra().address == ptsAddress }
              .map {
                it.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothAdapter.ERROR)
              }
              .first()

          if (pairingVariant == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION ||
              pairingVariant == BluetoothDevice.PAIRING_VARIANT_CONSENT ||
              pairingVariant == BluetoothDevice.PAIRING_VARIANT_PIN
          ) {
            val bluetoothDevice = ptsAddress.toBluetoothDevice(bluetoothAdapter)
            bluetoothDevice.setPairingConfirmation(true)
          }
        }
      delay(30_000L) // wait for 30sec to accept pairing request
      acceptPairingJob.cancel()
      Empty.getDefaultInstance()
    }
  }

  override fun pair(request: PairRequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      val ptsAddress = request.connection.cookie.toByteArray().decodeToString()
      Log.i(TAG, "pair: $ptsAddress")
      val bluetoothDevice = ptsAddress.toBluetoothDevice(bluetoothAdapter)
      bluetoothDevice.createBond()
      Empty.getDefaultInstance()
    }
  }

  override fun disconnectLE(request: DisconnectLERequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      val ptsAddress = request.connection.cookie.toByteArray().decodeToString()
      Log.i(TAG, "disconnect: $ptsAddress")
      val gatt = gattClients[ptsAddress]
      gatt?.close()
      gatt?.disconnect()
      gattClients.remove(ptsAddress)
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

  private fun gattConnect(device: BluetoothDevice): Boolean {
    Log.d(TAG, "gattConnect")
    var isConnected = false
    runBlocking {
      val flow = callbackFlow {
        val gattCallback =
          object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
              bluetoothGatt: BluetoothGatt?,
              status: Int,
              newState: Int
            ) {
              Log.d(TAG, "status: $status newState: $newState")
              trySendBlocking(status == GATT_SUCCESS)
            }
          }
        val bluetoothGatt: BluetoothGatt =
          device.connectGatt(context, false, gattCallback, TRANSPORT_LE)
        gattClients[device.address] = bluetoothGatt
        awaitClose {}
      }
      isConnected = flow.first()
    }
    return isConnected
  }
}
