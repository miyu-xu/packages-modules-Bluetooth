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

import android.bluetooth.BluetoothA2dpSink
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.*
import pandora.A2DPGrpc.A2DPImplBase
import pandora.A2dpProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class A2dpSink(val context: Context) : A2DPImplBase() {
  private val TAG = "PandoraA2dpSink"

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter
  private lateinit var bluetoothA2dpSink: BluetoothA2dpSink

  init {
    bluetoothA2dpSink = getProfileProxy<BluetoothA2dpSink>(context, BluetoothProfile.A2DP_SINK)
  }

  fun deinit() {
    bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP_SINK, bluetoothA2dpSink)
  }
}
