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
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import blueberry.GAPGrpc.GAPImplBase
import blueberry.GapProto.*
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gap(val context: Context) : GAPImplBase() {
  private val TAG = "BlueberryGap"

  private val scope: CoroutineScope

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter

  private val SCAN_DURATION_MILLIS: Long = 60000

  init {
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun makeDiscoverable(
    request: MakeDiscoverableRequest,
    responseObserver: StreamObserver<MakeDiscoverableResponse>
  ) {
    grpcUnary<MakeDiscoverableResponse>(scope, responseObserver) {
      Log.i(TAG, "makeDiscoverable")
      bluetoothAdapter.setScanMode(
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
        SCAN_DURATION_MILLIS
      )
      MakeDiscoverableResponse.getDefaultInstance()
    }
  }
}
