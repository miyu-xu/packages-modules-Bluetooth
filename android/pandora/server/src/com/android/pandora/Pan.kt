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

import pandora.PANGrpc.PANImplBase
import android.content.Context

import android.net.ConnectivityManager.TETHERING_BLUETOOTH
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log

import android.bluetooth.BluetoothPan
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_BREDR
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile

import io.grpc.stub.StreamObserver
import pandora.PanProto.*
import pandora.HostProto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Pan(private val context: Context) : PANImplBase() {
  private val TAG = "PandoraPan"
  private val mScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter
  private val bluetoothPan = getProfileProxy<BluetoothPan>(context, BluetoothProfile.PAN)

  private var mTetheringEnabled = MutableStateFlow(false)


  private val mConnectivityManager: ConnectivityManager
  private val mOnStartTetheringCallback =
    object : ConnectivityManager.OnStartTetheringCallback() {
      override fun onTetheringStarted() {
        Log.e(TAG, "onTetheringStarted")
        mTetheringEnabled.value = true
      }

      override fun onTetheringFailed() {
        Log.e(TAG, "onTetheringFailed")
        mTetheringEnabled.value = false
      }
    }

  init {
    mConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
  }

  fun deinit() {
    bluetoothAdapter.closeProfileProxy(BluetoothProfile.PAN, bluetoothPan)
    mScope.cancel()
  }

  override fun enableTethering(
    request: EnableTetheringRequest,
    responseObserver: StreamObserver<EnableTetheringResponse>
  ) {
    grpcUnary<EnableTetheringResponse>(mScope, responseObserver) {
      Log.e(TAG, "enableTethering")
      if (mTetheringEnabled.value != true) {
        mConnectivityManager.startTethering(
          TETHERING_BLUETOOTH,
          true,
          mOnStartTetheringCallback,
          Handler(Looper.getMainLooper()));
        mTetheringEnabled.first { it == true }
      }
      EnableTetheringResponse.newBuilder().build()
    }
  }

  override fun connectPan(
    request: ConnectPanRequest,
    responseObserver: StreamObserver<ConnectPanResponse>
  ) {
    grpcUnary<ConnectPanResponse>(mScope, responseObserver) {
      Log.e(TAG, "connectPan")
      val device = request.addr.toBluetoothDevice(bluetoothAdapter)
      bluetoothPan.setConnectionPolicy(device, BluetoothProfile.CONNECTION_POLICY_ALLOWED)
      bluetoothPan.connect(device)
      ConnectPanResponse.newBuilder().setConnection(device.toConnection(TRANSPORT_BREDR)).build()
    }
  }
}