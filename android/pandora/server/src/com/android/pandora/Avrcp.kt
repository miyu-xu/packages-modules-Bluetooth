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

import android.bluetooth.BluetoothManager
import android.content.Context

import pandora.AVRCPGrpc.AVRCPImplBase
import pandora.AvrcpProto.*
import android.media.*
import com.google.protobuf.Empty
import android.util.Log
import android.content.Intent
import io.grpc.stub.StreamObserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@kotlinx.coroutines.ExperimentalCoroutinesApi
class Avrcp(val context: Context) : AVRCPImplBase() {
  private val TAG = "PandoraAvrcp"

  private val scope: CoroutineScope

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  private var audioTrack: AudioTrack? = null

  init {
    Log.i(TAG,"Starting AVRCP.kt")
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
    context.startService(Intent(context, AvrcpBrowserService::class.java))
    scope.launch {
      initAudio()

    }
  }

  suspend fun initAudio() {
    if (audioTrack == null) {
      audioTrack = buildAudioTrack()
    }
    audioTrack?.play()
    delay(100)
    audioTrack?.pause()
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
  }

  fun buildAudioTrack(): AudioTrack? {
    audioTrack =
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(44100 * 2 * 2)
        .build()
    return audioTrack
  }

  override fun setPlaybackState(request: SetRequest, responseObserver: StreamObserver<GetResponse>) {
    grpcUnary<GetResponse>(scope, responseObserver) {
      val state = request.state
      Log.i(TAG, "required playback state $state")
      // Todo: Need to handle passing the state received from request
      AvrcpBrowserService.instance.setPlaybackState()
      GetResponse.getDefaultInstance()
    }
  }
}