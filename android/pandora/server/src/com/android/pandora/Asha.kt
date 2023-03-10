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
import android.bluetooth.BluetoothHearingAid
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRouting
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.asha.AshaGrpc.AshaImplBase
import pandora.asha.AshaProto.*


@kotlinx.coroutines.ExperimentalCoroutinesApi
class Asha(val context: Context) : AshaImplBase(), Closeable {
  private val TAG = "PandoraAsha"
  private val scope: CoroutineScope

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothHearingAid =
    getProfileProxy<BluetoothHearingAid>(context, BluetoothProfile.HEARING_AID)
  private val bluetoothAdapter = bluetoothManager.adapter
  private val audioManager = context.getSystemService(AudioManager::class.java)!!

  private var audioTrack: AudioTrack? = null

  init {
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
  }

  override fun close() {
    // Deinit the CoroutineScope
    scope.cancel()
  }

  override fun start(request: StartRequest,responseObserver: StreamObserver<StartResponse>) {
    grpcUnary<StartResponse>(scope, responseObserver){
      Log.i(TAG, "play")

      val latch = CountDownLatch(1) // Signal the count down latch

      if (audioTrack == null) {
        audioTrack = buildAudioTrack()
        Log.i(TAG, "buildAudioTrack")
      }
      audioTrack!!.play()

      val audioRoutingListener = AudioRouting.OnRoutingChangedListener {
        Log.i(TAG,"OnRoutingChangedListener triggered")
        if(it?.routedDevice?.type == AudioDeviceInfo.TYPE_HEARING_AID){
          latch.countDown()
        }
      }

      // wait for audio routing
      if (audioTrack!!.routedDevice?.type != AudioDeviceInfo.TYPE_HEARING_AID) {
        audioTrack!!.addOnRoutingChangedListener(
          audioRoutingListener,
          Handler(Looper.getMainLooper())
        )
        latch.await(10, TimeUnit.SECONDS) // Wait until the count down latch has been signaled
        audioTrack!!.removeOnRoutingChangedListener(audioRoutingListener)
      }

      val minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
      audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        minVolume,
        AudioManager.FLAG_SHOW_UI
      )

      StartResponse.getDefaultInstance()
    }
  }

  override fun stop(request: StopRequest, responseObserver: StreamObserver<StopResponse>) {
    grpcUnary<StopResponse>(scope, responseObserver){
      Log.i(TAG, "stop")
      audioTrack!!.pause()

      StopResponse.getDefaultInstance()
    }
  }

  override fun playbackAudio(responseObserver: StreamObserver<PlaybackAudioResponse>):StreamObserver<PlaybackAudioRequest>{
    Log.i(TAG, "playbackAudio")
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

    return object : StreamObserver<PlaybackAudioRequest> {
      override fun onNext(request: PlaybackAudioRequest) {
        val data = request.data.toByteArray()
        Log.d(TAG,"audio track writes data=$data")
        val written = synchronized(audioTrack!!) { audioTrack!!.write(data, 0, data.size) }
        if (written != data.size) {
          Log.e(TAG,"AudioTrack write failed")
          responseObserver.onError(
            Status.UNKNOWN.withDescription("AudioTrack write failed").asException()
          )
        }
      }
      override fun onError(t: Throwable?) {
        Log.e(TAG, t.toString())
        responseObserver.onError(t)
      }
      override fun onCompleted() {
        Log.i(TAG, "onCompleted")
        responseObserver.onNext(PlaybackAudioResponse.getDefaultInstance())
        responseObserver.onCompleted()
      }
    }
  }
}
