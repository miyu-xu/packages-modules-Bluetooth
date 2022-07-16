package com.android.pandora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log

import io.grpc.Status
import io.grpc.stub.StreamObserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

import pandora.GATTGrpc.GATTImplBase
import pandora.GattProto.*
import pandora.HostProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gatt(private val context: Context, private val host: Host) : GATTImplBase() {
  private val TAG = "PandoraGatt"

  private val mScope: CoroutineScope

  private val mBluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val mBluetoothAdapter: BluetoothAdapter = mBluetoothManager.adapter

  init {
    mScope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    mScope.cancel()
  }

  override fun exchangeMTU(request: ExchangeMTURequest,
      responseObserver: StreamObserver<ExchangeMTUResult>) {
    grpcUnary<ExchangeMTUResult>(mScope, responseObserver) {
      val mtu = request.mtu
      val addr = request.connection.cookie.toByteArray().decodeToString()
      val gattInstance: GattInstance? = host.gattClients.get(addr)
      var result = true
      if (gattInstance == null) {
        Log.e(TAG, "Trying to request MTU on a device that is not in the instances map")
        throw Status.UNKNOWN.asException()
      } else if (!gattInstance.requestMtu(mtu)) {
        result = false
        Log.e(TAG, "Error on requesting MTU for $gattInstance")
      }
      ExchangeMTUResult.newBuilder().setSuccess(result).build()
    }
  }

  override fun writeCharacteristicFromHandle(request: WriteCharacteristicRequest,
      responseObserver: StreamObserver<WriteCharacteristicResult>) {
    grpcUnary<WriteCharacteristicResult>(mScope, responseObserver) {
      val addr = request.connection.cookie.toByteArray().decodeToString()
      val gattInstance: GattInstance? = host.gattClients.get(addr)
      var characteristicWritten = false
      if (gattInstance == null) {
        Log.e(TAG, "Trying to request MTU on a device that is not in the instances map")
        throw Status.UNKNOWN.asException()
      } else {
        val characteristic: BluetoothGattCharacteristic? =
            getCharacteristicWithHandle(request.handle, gattInstance)
        if (characteristic != null) {
          Log.e(TAG,
              "Writing characteristic for $gattInstance")
          gattInstance.mGatt?.writeCharacteristic(characteristic,
              request.value.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
          characteristicWritten = true
        } else {
          Log.e(TAG,
              "Error while writing characteristic for $gattInstance")
        }
      }
      WriteCharacteristicResult.newBuilder().setSuccess(characteristicWritten).build()
    }
  }

  private fun getCharacteristicWithHandle(handle: Int,
      gattInstance: GattInstance): BluetoothGattCharacteristic? {
    if (!gattInstance.servicesDiscovered() && !gattInstance.discoverServices()) {  
      Log.e(TAG, "Error on discovering services for $gattInstance")
      throw Status.UNKNOWN.asException()
    } else {
      runBlocking {
        gattInstance.waitForDiscoveryEnd()
      }
    }

    for (service: BluetoothGattService in gattInstance.mGatt?.services.orEmpty()) {
      for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
        if (characteristic.instanceId == handle) {
          return characteristic
        }
      }
    }
    return null
  }
}