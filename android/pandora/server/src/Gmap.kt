/*
 * Copyright (C) 2025 The Android Open Source Project
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
import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.GMAPGrpc.GMAPImplBase
import pandora.GmapProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gmap(val context: Context) : GMAPImplBase() {
    private val TAG = "PandoraGmap"

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
    private val audioManager = context.getSystemService(AudioManager::class.java)!!
    private var audioTrack: AudioTrack? = null
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeAudio =
        getProfileProxy<BluetoothLeAudio>(context, BluetoothProfile.LE_AUDIO)

    fun deinit() {
        // Deinit the CoroutineScope
        scope.cancel()
    }

    override fun gmaStart(request: GmaStartRequest, responseObserver: StreamObserver<Empty>) {
        grpcUnary<Empty>(scope, responseObserver) {
            if (audioTrack == null) {
                audioTrack = buildAudioTrack()
            }
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "start: device=$device")

            if (bluetoothLeAudio.getConnectionState(device) != BluetoothLeAudio.STATE_CONNECTED) {
                throw RuntimeException("Device is not connected, cannot start")
            }

            // Configure the selected device as active device if it is not
            // already.
            bluetoothLeAudio.setActiveDevice(device)

            // Play an audio track.
            audioTrack!!.play()

            Empty.getDefaultInstance()
        }
    }

    override fun gmaPlaybackAudio(
        responseObserver: StreamObserver<GmaPlaybackAudioResponse>
    ): StreamObserver<GmaPlaybackAudioRequest> {
        Log.i(TAG, "GmaPlaybackAudio")

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
                    AudioManager.FLAG_SHOW_UI,
                )
            }
        }

        return object : StreamObserver<GmaPlaybackAudioRequest> {
            override fun onNext(request: GmaPlaybackAudioRequest) {
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
                responseObserver.onNext(GmaPlaybackAudioResponse.getDefaultInstance())
                responseObserver.onCompleted()
            }
        }
    }
}
