/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothHapClient
import android.bluetooth.BluetoothHapClient.Callback
import android.bluetooth.BluetoothHapPresetInfo
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.io.Closeable
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import pandora.HAPGrpc.HAPImplBase
import pandora.HapProto.*
import pandora.HostProto.Connection

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hap(val context: Context) : HAPImplBase(), Closeable {
    private val TAG = "PandoraHap"

    private val scope: CoroutineScope

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val audioManager = context.getSystemService(AudioManager::class.java)!!

    private val bluetoothHapClient =
        getProfileProxy<BluetoothHapClient>(context, BluetoothProfile.HAP_CLIENT)

    private val bluetoothLeAudio =
        getProfileProxy<BluetoothLeAudio>(context, BluetoothProfile.LE_AUDIO)

    private var audioTrack: AudioTrack? = null

    private class PresetInfoChanged(
        var connection: Connection,
        var presetInfoList: List<BluetoothHapPresetInfo>,
        var reason: Int
    ) {}

    private val mPresetChanged = callbackFlow {
        val callback =
            object : BluetoothHapClient.Callback {
                override fun onPresetSelected(
                    device: BluetoothDevice,
                    presetIndex: Int,
                    reason: Int
                ) {
                    Log.i(TAG, "$device preset info changed")
                }

                override fun onPresetSelectionFailed(device: BluetoothDevice, reason: Int) {
                    trySend(null)
                }

                override fun onPresetSelectionForGroupFailed(hapGroupId: Int, reason: Int) {
                    trySend(null)
                }

                override fun onPresetInfoChanged(
                    device: BluetoothDevice,
                    presetInfoList: List<BluetoothHapPresetInfo>,
                    reason: Int
                ) {
                    Log.i(TAG, "$device preset info changed")

                    var infoChanged =
                        PresetInfoChanged(device.toConnection(TRANSPORT_LE), presetInfoList, reason)

                    trySend(infoChanged)
                }

                override fun onSetPresetNameFailed(device: BluetoothDevice, reason: Int) {
                    trySend(null)
                }

                override fun onSetPresetNameForGroupFailed(hapGroupId: Int, reason: Int) {
                    trySend(null)
                }
            }

        bluetoothHapClient.registerCallback(Executors.newSingleThreadExecutor(), callback)

        awaitClose { bluetoothHapClient.unregisterCallback(callback) }
    }

    init {
        // Init the CoroutineScope
        scope = CoroutineScope(Dispatchers.Default)
    }

    override fun close() {
        // Deinit the CoroutineScope
        scope.cancel()
    }

    override fun getPresetInfo(
        request: GetPresetInfoRequest,
        responseObserver: StreamObserver<GetPresetInfoResponse>
    ) {
        grpcUnary<GetPresetInfoResponse>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "getPresetInfo")

            val presetInfo: BluetoothHapPresetInfo? =
                bluetoothHapClient.getPresetInfo(device, request.index)

            GetPresetInfoResponse.newBuilder()
                .setPresetInfo(
                    PresetInfo.newBuilder()
                        .setPresetIndex(presetInfo?.getIndex() ?: 0)
                        .setPresetName(presetInfo?.getName() ?: "")
                        .setIsWritable(presetInfo?.isWritable() ?: false)
                        .setIsAvailable(presetInfo?.isAvailable() ?: false)
                )
                .build()
        }
    }

    override fun getAllPresetsInfo(
        request: GetAllPresetsInfoRequest,
        responseObserver: StreamObserver<GetAllPresetsInfoResponse>
    ) {
        grpcUnary<GetAllPresetsInfoResponse>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "getAllPresetsInfo")

            val presetInfoList = arrayListOf<PresetInfo>()
            val receivedPresetInfoList = bluetoothHapClient.getAllPresetInfo(device)

            for (presetInfo in receivedPresetInfoList) {
                presetInfoList.add(
                    PresetInfo.newBuilder()
                        .setPresetIndex(presetInfo.getIndex())
                        .setPresetName(presetInfo.getName())
                        .setIsWritable(presetInfo.isWritable())
                        .setIsAvailable(presetInfo.isAvailable())
                        .build()
                )
            }

            GetAllPresetsInfoResponse.newBuilder().addAllPresetInfoList(presetInfoList).build()
        }
    }

    override fun setPresetName(
        request: SetPresetNameRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "setPresetName index=${request.index} name=${request.name}")

            bluetoothHapClient.setPresetName(device, request.index, request.name)

            Empty.getDefaultInstance()
        }
    }

    override fun selectPreset(
        request: SelectPresetRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "selectPreset")

            bluetoothHapClient.selectPreset(device, request.index)

            Empty.getDefaultInstance()
        }
    }

    override fun selectNextPreset(
        request: SelectNextPresetRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "selectNextPreset")

            bluetoothHapClient.switchToNextPreset(device)

            Empty.getDefaultInstance()
        }
    }

    override fun selectPreviousPreset(
        request: SelectPreviousPresetRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "selectNextPreset")

            bluetoothHapClient.switchToPreviousPreset(device)

            Empty.getDefaultInstance()
        }
    }

    override fun haPlaybackAudio(
        responseObserver: StreamObserver<HaPlaybackAudioResponse>
    ): StreamObserver<HaPlaybackAudioRequest> {
        Log.i(TAG, "haPlaybackAudio")

        if (audioTrack == null) {
            audioTrack = buildAudioTrack()
        }

        // Play an audio track.
        audioTrack!!.play()

        if (audioTrack!!.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            responseObserver.onError(
                Status.UNKNOWN.withDescription("AudioTrack is not started").asException()
            )
        }

        // Volume is maxed out to avoid any amplitude modification of the provided audio data,
        // enabling the test runner to do comparisons between input and output audio signal.
        // Any volume modification should be done before providing the audio data.
        if (audioManager.isVolumeFixed) {
            Log.w(TAG, "Volume is fixed, cannot max out the volume")
        } else {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < maxVolume) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume,
                    AudioManager.FLAG_SHOW_UI
                )
            }
        }

        return object : StreamObserver<HaPlaybackAudioRequest> {
            override fun onNext(request: HaPlaybackAudioRequest) {
                val data = request.data.toByteArray()
                val written = synchronized(audioTrack!!) { audioTrack!!.write(data, 0, data.size) }
                if (written != data.size) {
                    responseObserver.onError(
                        Status.UNKNOWN.withDescription("AudioTrack write failed").asException()
                    )
                }
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
                val sw = StringWriter()
                t.printStackTrace(PrintWriter(sw))
                responseObserver.onError(
                    Status.UNKNOWN.withCause(t).withDescription(sw.toString()).asException()
                )
            }

            override fun onCompleted() {
                responseObserver.onNext(HaPlaybackAudioResponse.getDefaultInstance())
                responseObserver.onCompleted()
            }
        }
    }

    override fun waitPresetChanged(
        request: Empty,
        responseObserver: StreamObserver<WaitPresetChangedResponse>
    ) {
        grpcUnary<WaitPresetChangedResponse>(scope, responseObserver) {
            val presetChangedReceived = mPresetChanged.first()!!
            val presetInfoList = arrayListOf<PresetInfo>()

            for (presetInfo in presetChangedReceived.presetInfoList) {
                presetInfoList.add(
                    PresetInfo.newBuilder()
                        .setPresetIndex(presetInfo.getIndex())
                        .setPresetName(presetInfo.getName())
                        .setIsWritable(presetInfo.isWritable())
                        .setIsAvailable(presetInfo.isAvailable())
                        .build()
                )
            }

            WaitPresetChangedResponse.newBuilder()
                .setConnection(presetChangedReceived.connection)
                .addAllPresetInfoList(presetInfoList)
                .setReason(presetChangedReceived.reason)
                .build()
        }
    }
}
