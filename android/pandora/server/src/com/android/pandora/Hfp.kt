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
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.MacAddress
import android.util.Log

import io.grpc.Status
import io.grpc.stub.StreamObserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

import pandora.HFPGrpc.HFPImplBase
import pandora.HfpProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hfp(val context: Context) : HFPImplBase() {
  private val TAG = "PandoraHfp"

  private val scope: CoroutineScope

  private val bluetoothManager =
    context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  init {
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun deletePairing(
    request: DeletePairingRequest,
    responseObserver: StreamObserver<DeletePairingResponse>
  ) {
    grpcUnary<DeletePairingResponse>(scope, responseObserver) {
      val address = decodeAddressToString(request.address)
      val device = bluetoothAdapter.getRemoteDevice(address)
      Log.i(TAG, "DeletePairing: address=$address")

      device.removeBond()
      DeletePairingResponse.getDefaultInstance()
    }
  }
}
