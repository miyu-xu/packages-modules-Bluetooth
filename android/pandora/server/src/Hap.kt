// Copyright (C) 2024 The Android Open Source Project
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.pandora

import android.bluetooth.BluetoothHapClient
import android.bluetooth.BluetoothManager
import android.content.Context
import io.grpc.stub.StreamObserver
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import pandora.hap.HAPGrpc.HAPImplBase
import pandora.hap.HapProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hap(private val context: Context) : HAPImplBase(), Closeable {
    private val TAG = "PandoraHap"

    private val mScope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val BluetoothHapClient =
        getProfileProxy<BluetoothHapClient>(context, BluetoothProfile.HAP_CLIENT)

    init {
        android.util.Log.e("WILLIAM", "init babe")
    }

    override fun close() {
        android.util.Log.e("WILLIAM", "close me close me close me")
    }

    override fun getFeatures(
        request: GetFeaturesRequest,
        responseObserver: StreamObserver<GetFeaturesResponse>
    ) {
        grpcUnary<GetFeaturesResponse>(mScope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)
            android.util.Log.e("WILLIAM", "Processing grpc call of getFeature for ${device}")
            GetFeaturesResponse.newBuilder().setFeatures(0x00).build()
        }
    }
}
