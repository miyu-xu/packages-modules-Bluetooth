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

import android.os.ParcelUuid
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseCallback
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.util.UUID

import com.google.protobuf.Empty
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

import pandora.SMGrpc.SMImplBase
import pandora.HostProto.*
import pandora.SmProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Sm(private val context: Context) : SMImplBase() {
  private val TAG = "PandoraSm"

  private val scope: CoroutineScope
  private val flow: Flow<Intent>
  private var passkey = 0

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  init {
    scope = CoroutineScope(Dispatchers.Default)

    val intentFilter = IntentFilter()
    intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
    intentFilter.addAction(BluetoothDevice.ACTION_UUID)

    flow = intentFlow(context, intentFilter).shareIn(scope, SharingStarted.Eagerly)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun pair(request: PairRequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      val bluetoothDevice: BluetoothDevice = if (request.hasConnection()) {
        request.connection.toBluetoothDevice(bluetoothAdapter)
      } else {
        request.address.toBluetoothDevice(bluetoothAdapter)
      }
      Log.i(TAG, "pair: ${bluetoothDevice.getAddress()}")
      bluetoothDevice.createBond()
      Empty.getDefaultInstance()
    }
  }

  override fun providePairingConfirmation(
      request: PairingConfirmationRequest,
      responseObserver: StreamObserver<Empty>
  ) {
    grpcUnary<Empty>(scope, responseObserver) {
      var bluetoothDevice: BluetoothDevice = if (request.hasConnection()) {
        request.connection.toBluetoothDevice(bluetoothAdapter)
      } else {
        request.address.toBluetoothDevice(bluetoothAdapter)
      }
      Log.i(TAG, "Confirm pairing for: address=${bluetoothDevice.getAddress()}")
      passkey = flow
        .filter { it.getAction() == BluetoothDevice.ACTION_PAIRING_REQUEST }
        .filter { it.getBluetoothDeviceExtra() == bluetoothDevice }
        .first()
        .getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 0)
      Log.i(TAG, "Got passkey: $passkey for: address=${bluetoothDevice.getAddress()}")
      bluetoothDevice.setPairingConfirmation(request.pairingConfirmationValue)
      Empty.getDefaultInstance()
    }
  }

  override fun getPasskey(request: PasskeyRequest, responseObserver: StreamObserver<PasskeyResponse>) {
    grpcUnary<PasskeyResponse>(scope, responseObserver) {
      // val bluetoothDevice = request.address.toBluetoothDevice(bluetoothAdapter)
      // val passkey = flow
      //   .filter { it.getAction() == BluetoothDevice.ACTION_PAIRING_REQUEST }
      //   .filter { it.getBluetoothDeviceExtra() == bluetoothDevice }
      //   .first()
      //   .getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 0)

      PasskeyResponse.newBuilder()
        .setPasskey(passkey)
        .build()
    }
  }

  override fun createClassicConnection(request: ConnectionRequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      val bluetoothDevice = request.address.toBluetoothDevice(bluetoothAdapter)

      // bluetoothDevice.fetchUuidsWithSdp()
      // val uuids = flow
      //   .filter { it.getAction() == BluetoothDevice.ACTION_UUID }
      //   .first()
      //   .getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
      
      // Log.i(TAG, "Done fetching UUIDs: $uuids")
      // val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
      // // for(uuid in uuids) {
      // //   Log.i(TAG, "$uuid")
      // // }
      // val bSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
      // bSocket.connect()
      // val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
      // pairedDevices?.forEach { device ->
      //   val deviceName = device.name
      //   val deviceHardwareAddress = device.address // MAC address
      //   Log.i(TAG, "Mac address: $deviceHardwareAddress")
      // }
      // Log.i(TAG, "Creating GATT over classic")
      // val mCallback = object : BluetoothGattCallback() {
      //   override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt,
      //     status: Int, newState: Int) {
      //     Log.i(TAG, "Connection state changed to $newState")
      //   }
      // }

      // bluetoothDevice.connectGatt (context, 
      //           false, 
      //           mCallback, 
      //           BluetoothDevice.TRANSPORT_BREDR)
      // bluetoothDevice.createBond()

      Empty.getDefaultInstance()
    }
  }

  override fun enableConnectableMode(request: EnableConnectableModeRequest, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      // val bluetoothDevice = request.address.toBluetoothDevice(bluetoothAdapter)
      // val requestCode = 1;
      // val discoverableIntent: Intent = Intent(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
      // startActivityForResult(discoverableIntent, requestCode)

      // val method = bluetoothAdapter.getClass().getMethod("setScanMode", Integer.TYPE);
      // method.invoke(bluetoothAdapter, scanMode);
      Log.i(TAG, "Starting advertising")
      // bluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
      val advertiser = bluetoothAdapter.getBluetoothLeAdvertiser()
      val advSettings = AdvertiseSettings.Builder().setConnectable(true).setTimeout(120000).build()
      val advData = AdvertiseData.Builder().build()
      val advCallback = object: AdvertiseCallback() {
        override fun onStartFailure (errorCode: Int) {
          Log.i(TAG, "Advertising failed: $errorCode")
        }
        override fun onStartSuccess (settingsInEffect: AdvertiseSettings) {
          Log.i(TAG, "Advertising success")
        }
      }
      advertiser.startAdvertising(advSettings, advData, advCallback)
      Empty.getDefaultInstance()
    }
  }
}
