package com.android.pandora

import android.bluetooth.BluetoothAvrcp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import pandora.AVRCPGrpc.AVRCPImplBase
import pandora.AvrcpProto.*
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel


@kotlinx.coroutines.ExperimentalCoroutinesApi
class Avrcp(val context: Context) : AVRCPImplBase() {
  private val TAG = "PandoraAvrcp"

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