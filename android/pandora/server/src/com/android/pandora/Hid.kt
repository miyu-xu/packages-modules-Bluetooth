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
import pandora.HIDGrpc.HIDImplBase
import pandora.HidProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hid(val context: Context) : HIDImplBase() {
  private val TAG = "PandoraHid"

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
