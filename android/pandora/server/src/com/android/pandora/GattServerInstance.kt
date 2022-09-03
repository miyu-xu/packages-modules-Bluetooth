package com.android.pandora

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow

class GattServerInstance(bluetoothManager: BluetoothManager, context: Context) {
  companion object GattServerManager {
    val instances = HandleManager<GattServerInstance>()
  }

  val handle = instances.register(this)
  val newServiceFlow = MutableSharedFlow<BluetoothGattService>(extraBufferCapacity = 8)

  val callback =
    object : BluetoothGattServerCallback() {
      override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
        check(status == BluetoothGatt.GATT_SUCCESS)
        check(newServiceFlow.tryEmit(service!!))
      }
    }

  val server: BluetoothGattServer = bluetoothManager.openGattServer(context, callback)
}
