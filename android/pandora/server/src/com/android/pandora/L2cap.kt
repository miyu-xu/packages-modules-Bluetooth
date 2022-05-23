package com.android.pandora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import pandora.L2CAPGrpc.GAPImplBase
import pandora.L2capProto.*
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

@kotlinx.coroutines.ExperimentalCoroutinesApi
class L2cap(val context: Context) : L2CAPImplBase() {
  private val TAG = "PandoraL2cap"

  private val scope: CoroutineScope

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter

  private val SCAN_DURATION_MILLIS: Long = 60000

  init {
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
  }

  /**
   * Set the device in discoverable mode for #SCAN_DURATION_MILLIS milliseconds.
   * @param request Request sent by the client.
   * @param responseObserver Response to build and set back to the client.
   */
  override fun initiateAclConnection(
    request: Empty,
    responseObserver: StreamObserver<Empty>
  ) {
    // Creates a gRPC coroutine in a given coroutine scope which executes a given suspended function
    // returning a gRPC response and sends it on a given gRPC stream observer.
    grpcUnary<Empty>(scope, responseObserver) {
      Log.i(TAG, "initiateAclConnection")

      Log.e(TAG, "NOT IMPLEMENTED")

      // Response sent to client
      Empty.getDefaultInstance()
    }
  }
}

