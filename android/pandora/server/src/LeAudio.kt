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
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothLeAudioContentMetadata
import android.bluetooth.BluetoothLeBroadcast
import android.bluetooth.BluetoothLeBroadcastSettings
import android.bluetooth.BluetoothLeBroadcastSubgroupSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.util.Log
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import pandora.LeAudioGrpc.LeAudioImplBase
import pandora.LeAudioProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LeAudio(val context: Context) : LeAudioImplBase(), Closeable {

    private val TAG = "PandoraLeAudio"

    private val scope: CoroutineScope
    private val flow: Flow<Intent>
    private val bluetoothLeBroadcastFlow: Flow<LeBroadcastCallback>

    private val audioManager = context.getSystemService(AudioManager::class.java)!!

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeAudio =
        getProfileProxy<BluetoothLeAudio>(context, BluetoothProfile.LE_AUDIO)
    private val bluetoothLeBroadcast =
        getProfileProxy<BluetoothLeBroadcast>(context, BluetoothProfile.LE_AUDIO_BROADCAST)

    init {
        scope = CoroutineScope(Dispatchers.Default)
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED)

        flow = intentFlow(context, intentFilter, scope).shareIn(scope, SharingStarted.Eagerly)
        bluetoothLeBroadcastFlow =
            flowFromBluetoothLeBroadcastCallbacks(scope, bluetoothLeBroadcast)
                .shareIn(scope, SharingStarted.Eagerly)
    }

    override fun close() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.LE_AUDIO, bluetoothLeAudio)
        bluetoothAdapter.closeProfileProxy(
            BluetoothProfile.LE_AUDIO_BROADCAST,
            bluetoothLeBroadcast
        )
        scope.cancel()
    }

    override fun open(request: OpenRequest, responseObserver: StreamObserver<Empty>) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "open: device=$device")

            if (bluetoothLeAudio.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
                bluetoothLeAudio.connect(device)
                val state =
                    flow
                        .filter {
                            it.getAction() ==
                                BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED
                        }
                        .filter { it.getBluetoothDeviceExtra() == device }
                        .map {
                            it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
                        }
                        .filter {
                            it == BluetoothProfile.STATE_CONNECTED ||
                                it == BluetoothProfile.STATE_DISCONNECTED
                        }
                        .first()

                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    throw RuntimeException("open failed, LE_AUDIO has been disconnected")
                }
            }

            Empty.getDefaultInstance()
        }
    }

    override fun startBroadcast(
        request: StartBroadcastRequest,
        responseObserver: StreamObserver<StartBroadcastResponse>
    ) {
        grpcUnary<StartBroadcastResponse>(scope, responseObserver) {
            val contentMetadata = BluetoothLeAudioContentMetadata.Builder().build()
            val subgroupSettings =
                BluetoothLeBroadcastSubgroupSettings.Builder()
                    .setContentMetadata(contentMetadata)
                    .build()
            val broadcastSettings =
                BluetoothLeBroadcastSettings.Builder().addSubgroupSettings(subgroupSettings).build()

            bluetoothLeBroadcast.startBroadcast(broadcastSettings)
            val broadcastStarted =
                waitForLeBroadcastCallbackResult<
                        LeBroadcastCallback.BroadcastStarted,
                        LeBroadcastCallback.BroadcastStartFailed
                    >()
                    .valueOr {
                        throw RuntimeException("start broadcast failed, reason: " + it.reason)
                    }

            StartBroadcastResponse.newBuilder().setBroadcastId(broadcastStarted.broadcastId).build()
        }
    }

    private inline suspend fun <
        reified Success : LeBroadcastCallback,
        reified Failure : LeBroadcastCallback
    > waitForLeBroadcastCallbackResult() =
        LeBroadcastCallbackResult.wrap<Success, Failure>(
            bluetoothLeBroadcastFlow.filter { it is Success || it is Failure }.first()
        )
}
