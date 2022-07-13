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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager;
import android.os.SystemProperties
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import pandora.HFPGrpc.HFPImplBase
import pandora.HfpProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hfp(val context: Context, val host: Host) : HFPImplBase() {
  private val TAG = "PandoraHfp"

  private val scope: CoroutineScope
  private val flow: Flow<Intent>

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  private val bluetoothHfp = getProfileProxy<BluetoothHeadset>(context, BluetoothProfile.HEADSET)

  init {
    scope = CoroutineScope(Dispatchers.Default)

    val intentFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
    flow = intentFlow(context, intentFilter).shareIn(scope, SharingStarted.Eagerly)
  }

  fun deinit() {
    scope.cancel()
  }

  override fun enableSlc(
    request: EnableSlcRequest,
    responseObserver: StreamObserver<EnableSlcResponse>
  ) {
    grpcUnary<EnableSlcResponse>(scope, responseObserver) {
      val device = request.address.toBluetoothDevice(bluetoothAdapter)

      host.bondDeviceIfNotConnected(device)

      bluetoothHfp.setConnectionPolicy(device, BluetoothProfile.CONNECTION_POLICY_ALLOWED)

      EnableSlcResponse.getDefaultInstance()
    }
  }

  override fun disableSlc(
    request: DisableSlcRequest,
    responseObserver: StreamObserver<Empty>
  ) {
    grpcUnary<Empty>(scope, responseObserver) {
      val device = request.address.toBluetoothDevice(bluetoothAdapter)

      bluetoothHfp.setConnectionPolicy(device, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN)

      Empty.getDefaultInstance()
    }
  }

  override fun disableInbandRing(
    request: Empty,
    responseObserver: StreamObserver<Empty>
  ) {
    grpcUnary<Empty>(scope, responseObserver) {
      // properties define in
      // com.android.bluetooth.hfp.HeadsetService.DISABLE_INBAND_RINGING_PROPERTY
      SystemProperties.set("persist.bluetooth.disableinbandringing", "0")

      Empty.getDefaultInstance()
    }
  }

  override fun setBatteryLevel(
    request: SetBatteryLevelRequest,
    responseObserver: StreamObserver<Empty>
  ) {
    grpcUnary<Empty>(scope, responseObserver) {
      val sendIntent: Intent = Intent().apply {
          action = Intent.ACTION_BATTERY_CHANGED
          putExtra(BatteryManager.EXTRA_LEVEL, request.batteryPercentage)
          putExtra(BatteryManager.EXTRA_SCALE, 100)
      }
      context.sendBroadcast(sendIntent)

      Empty.getDefaultInstance()
    }
  }
}
