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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@kotlinx.coroutines.ExperimentalCoroutinesApi
class GattInstance(val device: BluetoothDevice) {
  private val TAG = "GattInstance"
  public var mGatt: BluetoothGatt? = null
  public var mTransport: Int = -1
  public val mDevice: BluetoothDevice

  private var mServiceDiscovered = MutableStateFlow(false)
  private var mConnectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)

  private var mBondState = BluetoothDevice.BOND_NONE

  init {
    mDevice = device
  }

  public fun isConnected(): Boolean {
    return mConnectionState.value == BluetoothProfile.STATE_CONNECTED
  }

  public fun isDisconnected(): Boolean {
    return mConnectionState.value == BluetoothProfile.STATE_DISCONNECTED
  }

  public fun isBonded(): Boolean {
    return mBondState == BluetoothDevice.BOND_BONDED
  }

  public fun isBLETransport(): Boolean {
    return mTransport == BluetoothDevice.TRANSPORT_LE
  }

  public fun servicesDiscovered(): Boolean {
    return mServiceDiscovered.value
  }

  public fun connectInstance(transport: Int, callback: BluetoothGattCallback, context: Context): Boolean {
    mTransport = transport

    if (!isBLETransport() && !isBonded()) {
      Log.w(TAG, "Trying to connect non BLE gatt on a not bonded device")
      return false
    }
    if (!isDisconnected()) {
      Log.w(TAG, "Trying to connect gatt on an already connected device")
      return false
    }

    mGatt = mDevice.connectGatt(context, false, callback, mTransport)
    return mGatt != null
  }

  public suspend fun waitForState(newState: Int) {
    if (mConnectionState.value != newState) {
      mConnectionState.first { it == newState }
    }
  }

  public suspend fun waitForDiscoveryEnd() {
    if (mServiceDiscovered.value != true) {
      mServiceDiscovered.first { it == true }
    }
  }

  public fun disconnectInstance(): Boolean {
    if (mGatt == null || !isConnected()) {
      Log.w(TAG, "Trying to disconnect an already disconnected device")
      return false
    }
    mGatt?.close()
    mGatt?.disconnect()
    return true;
  }

  public fun setConnectionState(state: Int) {
    Log.i(TAG, "$mDevice connection state changed to $state")
    mConnectionState.value = state
    if (isDisconnected() && mGatt != null) {
      reset()
    }
  }

  public fun setBondState(state: Int) {
    Log.i(TAG, "$mDevice bond state changed to $state")
    mBondState = state
  }

  public fun setServicesDiscovered() {
    Log.i(TAG, "Services have been discovered for $mDevice")
    mServiceDiscovered.value = true
  }

  public fun requestMtu(mtu: Int): Boolean {
    val result = mGatt?.requestMtu(mtu)
    return if (result != null) result else false
  }

  public fun discoverServices(): Boolean {
    val result = mGatt?.discoverServices()
    return if (result != null) result else false
  }

  private fun reset() {
    mServiceDiscovered.value = false
    mTransport = -1
    mGatt = null
  }

  override fun toString(): String {
    return mDevice.getAddress()
  }
}