/*
 * Copyright (C) 2024 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.pandora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHapClient
import android.bluetooth.BluetoothHapPresetInfo
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.CONNECTION_POLICY_ALLOWED
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import pandora.hap.HAPGrpc.HAPImplBase
import pandora.hap.HapProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hap(private val context: Context) : HAPImplBase(), Closeable {
    private val TAG = "PandoraHap"

    private val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothHapClient =
        getProfileProxy<BluetoothHapClient>(context, BluetoothProfile.HAP_CLIENT)

    private val flow =
        intentFlow(
                context,
                IntentFilter().apply {
                    addAction(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED)
                },
                scope
            )
            .shareIn(scope, SharingStarted.Eagerly)

    override fun close() {
        scope.cancel()
    }

    override fun getFeatures(
        request: GetFeaturesRequest,
        responseObserver: StreamObserver<GetFeaturesResponse>
    ) {
        val device = request.connection.toBluetoothDevice(bluetoothAdapter)
        Log.i(TAG, "getFeatures(${device})")
        grpcUnary<GetFeaturesResponse>(scope, responseObserver) {
            GetFeaturesResponse.newBuilder()
                .setFeatures(bluetoothHapClient.getFeatures(device))
                .build()
        }
    }

    override fun getAllPresetsInfo(
        request: GetAllPresetsInfoRequest,
        responseObserver: StreamObserver<GetAllPresetsInfoResponse>
    ) {
        val device = request.connection.toBluetoothDevice(bluetoothAdapter)
        Log.i(TAG, "getAllPresetsInfo(${device})")
        grpcUnary<GetAllPresetsInfoResponse>(scope, responseObserver) {
            GetAllPresetsInfoResponse.newBuilder()
                .addAllPresetInfoList(
                    bluetoothHapClient
                        .getAllPresetInfo(device)
                        .stream()
                        .map { it: BluetoothHapPresetInfo ->
                            PresetInfo.newBuilder()
                                .setPresetIndex(it.getIndex())
                                .setPresetName(it.getName())
                                .setIsWritable(it.isWritable())
                                .setIsAvailable(it.isAvailable())
                                .build()
                        }
                        .toList()
                )
                .build()
        }
    }

    override fun waitPeripheral(
        request: WaitPeripheralRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        val device = request.connection.toBluetoothDevice(bluetoothAdapter)
        Log.i(TAG, "waitPeripheral(${device}")
        grpcUnary<Empty>(scope, responseObserver) {
            if (bluetoothHapClient.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Manual call to setConnectionPolicy")
                bluetoothHapClient.setConnectionPolicy(device, CONNECTION_POLICY_ALLOWED)
                Log.d(TAG, "now waiting for bluetoothHapClient profile connection")
                flow
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
                    .filter { it == BluetoothProfile.STATE_CONNECTED }
                    .first()
            }

            Empty.getDefaultInstance()
        }
    }
}
