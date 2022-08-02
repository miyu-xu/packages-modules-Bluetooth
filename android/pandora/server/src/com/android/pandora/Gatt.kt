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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice.TRANSPORT_BREDR
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

import com.google.protobuf.Empty

import io.grpc.Status
import io.grpc.stub.StreamObserver

import java.lang.StringBuilder
import java.util.UUID

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import pandora.GATTGrpc.GATTImplBase
import pandora.GattProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gatt(private val context: Context) : GATTImplBase() {
  private val TAG = "PandoraGatt"

  private val mScope: CoroutineScope

  private val mBluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val mBluetoothAdapter = mBluetoothManager.adapter

  private val baseUUID: String = "-0000-1000-8000-00805f9b34fb"
  private val handlePadding: Int = 4
  private val uuidPadding: Int = 8

  private fun String.toBaseUuid(): UUID {
    if (this.length == 4) {
      return UUID.fromString(this.lowercase().padStart(uuidPadding, '0').plus(baseUUID))
    } else {
      return UUID.fromString(StringBuilder(this.replace("-", "").lowercase())
          .insert(8, "-").insert(13, "-").insert(18, "-").insert(23, "-").toString())
    }
  }

  private fun Int.toBaseUuid(): UUID =
      UUID.fromString(String.format("%04X", this).lowercase().padStart(uuidPadding, '0').plus(baseUUID))

  init {
    mScope = CoroutineScope(Dispatchers.Default)
  }

  fun deinit() {
    mScope.cancel()
  }

  override fun exchangeMTU(request: ExchangeMTURequest,
      responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(mScope, responseObserver) {
      val mtu = request.mtu
      val addr = request.connection.cookie.toByteArray().decodeToString()
      if (!GattInstance.get(addr).mGatt.requestMtu(mtu)) {
        Log.e(TAG, "Error on requesting MTU for $addr")
        throw Status.UNKNOWN.asException()
      }
      Empty.getDefaultInstance()
    }
  }

  override fun writeCharacteristicFromHandle(request: WriteCharacteristicRequest,
      responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(mScope, responseObserver) {
      val addr = request.connection.cookie.toByteArray().decodeToString()
      val gattInstance = GattInstance.get(addr)
      val characteristic: BluetoothGattCharacteristic? =
          getCharacteristicWithHandle(request.handle, gattInstance)
      if (characteristic != null) {
        gattInstance.mGatt.writeCharacteristic(characteristic,
            request.value.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
      } else {
        Log.e(TAG,
            "Error while writing characteristic for $gattInstance")
        throw Status.UNKNOWN.asException()
      }
      Empty.getDefaultInstance()
    }
  }

  override fun connectGattBrEdr(request: ConnectGattBrEdrRequest,
      responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(mScope, responseObserver) {
      val device = request.connection.toBluetoothDevice(mBluetoothAdapter)
      GattInstance(device, TRANSPORT_BREDR, context).waitForState(BluetoothProfile.STATE_CONNECTED)
      Empty.getDefaultInstance()
    }
  }

  override fun discoverServiceByUuid(request: DiscoverServiceByUuidRequest,
      responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(mScope, responseObserver) {
      val addr = request.connection.cookie.toByteArray().decodeToString()
      val gattInstance = GattInstance.get(addr)
      if (!gattInstance.isBLETransport()) {
        // Non BLE transport GATT needs 1s delay before being able to discover services
        delay(1000L)
      } else {
        // BLE transport GATT starts a discovery immediately after being connected, and
        // in some cases no service is found and we can start immediately, but in most cases
        // we need to wait until the service discovery is finished to be able to discover again.
        // This takes about 24s, and there is no way to know if the service is busy or not.
        delay(30000L)
      }
      check(gattInstance.mGatt.discoverServiceByUuid(request.uuid.toBaseUuid()))
      Empty.getDefaultInstance()
    }
  }

  override fun discoverServices(request: DiscoverServicesRequest,
      responseObserver: StreamObserver<DiscoverServicesResponse>) {
    grpcUnary<DiscoverServicesResponse>(mScope, responseObserver) {
      val addr = request.connection.cookie.toByteArray().decodeToString()
      val gattInstance = GattInstance.get(addr)
      check(gattInstance.mGatt.discoverServices())
      gattInstance.waitForDiscoveryEnd()
      DiscoverServicesResponse.newBuilder()
          .addAllServices(generateServicesList(gattInstance.mGatt.services)).build()
    }
  }

  private suspend fun getCharacteristicWithHandle(handle: Int,
      gattInstance: GattInstance): BluetoothGattCharacteristic? {
    if (!gattInstance.servicesDiscovered() && !gattInstance.mGatt.discoverServices()) {
      Log.e(TAG, "Error on discovering services for $gattInstance")
      throw Status.UNKNOWN.asException()
    } else {
      gattInstance.waitForDiscoveryEnd()
    }
    for (service: BluetoothGattService in gattInstance.mGatt.services.orEmpty()) {
      for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
        if (characteristic.instanceId == handle) {
          return characteristic
        }
      }
    }
    return null
  }

  private fun generateServicesList(servicesList: List<BluetoothGattService>)
      : ArrayList<GattService> {
    val newServicesList = arrayListOf<GattService>()
    for (service in servicesList) {
      val serviceBuilder = GattService.newBuilder()
          .setHandle(service.getInstanceId())
          .setType(service.getType())
          .setUuid(service.getUuid().toString())
          .addAllIncludedServices(generateServicesList(service.getIncludedServices()))
          .addAllCharacteristics(generateCharacteristicsList(service.characteristics))
      newServicesList.add(serviceBuilder.build())
    }
    return newServicesList
  }

  private fun generateCharacteristicsList(characteristicsList : List<BluetoothGattCharacteristic>)
      : ArrayList<GattCharacteristic> {
    val newCharacteristicsList = arrayListOf<GattCharacteristic>()
    for (characteristic in characteristicsList) {
      val characteristicBuilder = GattCharacteristic.newBuilder()
          .setProperties(characteristic.getProperties())
          .setPermissions(characteristic.getPermissions())
          .setUuid(characteristic.getUuid().toString())
          .addAllDescriptors(generateDescriptorsList(characteristic.getDescriptors()))
      newCharacteristicsList.add(characteristicBuilder.build())
    }
    return newCharacteristicsList
  }

  private fun generateDescriptorsList(descriptorsList : List<BluetoothGattDescriptor>)
      : ArrayList<GattDescriptor> {
    val newDescriptorsList = arrayListOf<GattDescriptor>()
    for (descriptor in descriptorsList) {
      val descriptorBuilder = GattDescriptor.newBuilder()
          .setHandle(descriptor.getInstanceId())
          .setPermissions(descriptor.getPermissions())
          .setUuid(descriptor.getUuid().toString())
      newDescriptorsList.add(descriptorBuilder.build())
    }
    return newDescriptorsList
  }
}