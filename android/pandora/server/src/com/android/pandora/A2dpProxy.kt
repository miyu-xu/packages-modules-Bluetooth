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

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothA2dpSink
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.media.*
import android.util.Log
import io.grpc.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pandora.A2dpProto.*

private const val TAG = "PandoraA2dpProxy"

class A2dpSourceProxy(context: Context, flow: Flow<Intent>) : A2dpProxy {
  val a2dpSource: BluetoothA2dp = getProfileProxy<BluetoothA2dp>(context, BluetoothProfile.A2DP)
  override val ACTION_CONNECTION_STATE_CHANGED: String =
    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED
  override val flow: Flow<Intent> = flow

  override fun connect(device: BluetoothDevice) {
    a2dpSource.connect(device)
  }

  override fun isA2dpPlaying(device: BluetoothDevice): Boolean {
    return a2dpSource.isA2dpPlaying(device)
  }

  override fun getConnectionState(device: BluetoothDevice): Int {
    return a2dpSource.getConnectionState(device)
  }

  override fun setConnectionPolicy(device: BluetoothDevice, policy: Int) {
    a2dpSource.setConnectionPolicy(device, policy)
  }

  override fun deinit(bluetoothAdapter: BluetoothAdapter) {
    bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpSource)
  }
}

class A2dpSinkProxy(context: Context, flow: Flow<Intent>) : A2dpProxy {
  val a2dpSink: BluetoothA2dpSink =
    getProfileProxy<BluetoothA2dpSink>(context, BluetoothProfile.A2DP_SINK)
  override val ACTION_CONNECTION_STATE_CHANGED: String =
    BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED
  override val flow: Flow<Intent> = flow

  override fun connect(device: BluetoothDevice) {
    a2dpSink.connect(device)
  }

  override fun isA2dpPlaying(device: BluetoothDevice): Boolean {
    return a2dpSink.isAudioPlaying(device)
  }

  override fun getConnectionState(device: BluetoothDevice): Int {
    return a2dpSink.getConnectionState(device)
  }

  override fun setConnectionPolicy(device: BluetoothDevice, policy: Int) {
    a2dpSink.setConnectionPolicy(device, policy)
  }

  override fun deinit(bluetoothAdapter: BluetoothAdapter) {
    bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP_SINK, a2dpSink)
  }
}

interface A2dpProxy {

  val ACTION_CONNECTION_STATE_CHANGED: String
  val flow: Flow<Intent>

  fun connect(device: BluetoothDevice)
  fun isA2dpPlaying(device: BluetoothDevice): Boolean
  fun getConnectionState(device: BluetoothDevice): Int
  fun setConnectionPolicy(device: BluetoothDevice, policy: Int)
  fun deinit(bluetoothAdapter: BluetoothAdapter)

  suspend fun waitStream(device: BluetoothDevice) {
    Log.i(TAG, "waitStream: device=$device")

    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
      Log.e(TAG, "Device is not bonded, cannot wait for stream")
      throw Status.UNKNOWN.asException()
    }

    if (getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
      val state =
        flow
          .filter { it.getAction() == ACTION_CONNECTION_STATE_CHANGED }
          .filter { it.getBluetoothDeviceExtra() == device }
          .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
          .filter {
            it == BluetoothProfile.STATE_CONNECTED || it == BluetoothProfile.STATE_DISCONNECTED
          }
          .first()

      if (state == BluetoothProfile.STATE_DISCONNECTED) {
        Log.e(TAG, "waitStream failed, A2DP has been disconnected")
        throw Status.UNKNOWN.asException()
      }
    }

    // TODO: b/234891800, AVDTP start request sometimes never sent if playback starts too early.
    delay(2000L)
  }

  suspend fun close(device: BluetoothDevice) {
    Log.i(TAG, "close: device=$device")

    if (getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
      Log.e(TAG, "Device is not connected, cannot close")
      throw Status.UNKNOWN.asException()
    }

    val a2dpConnectionStateChangedFlow =
      flow
        .filter { it.getAction() == ACTION_CONNECTION_STATE_CHANGED }
        .filter { it.getBluetoothDeviceExtra() == device }
        .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
    setConnectionPolicy(device, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN)
    a2dpConnectionStateChangedFlow.filter { it == BluetoothProfile.STATE_DISCONNECTED }.first()
  }
}
