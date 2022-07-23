package com.android.pandora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.HID11Grpc.HID11ImplBase
import pandora.Hid11Proto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hid11(val context: Context) : HID11ImplBase() {
  private val TAG = "PandoraHid11"

  private val scope: CoroutineScope

  private val bluetoothManager =
      context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  
  init {
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
  }
}
