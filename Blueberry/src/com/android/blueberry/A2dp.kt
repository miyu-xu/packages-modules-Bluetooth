package com.android.blueberry

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.*
import android.util.Log
import blueberry.A2DPGrpc.A2DPImplBase
import blueberry.A2dpProto.*
import io.grpc.stub.StreamObserver

class A2dp(val mContext: Context, val host: Host) : A2DPImplBase() {
  private val TAG = "BlueberryA2dp"

  private var audioManager: AudioManager =
    mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

  private val bluetoothManager =
    mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  private var bluetoothA2dp: BluetoothA2dp? = null

  inner class A2dpServiceListener : BluetoothProfile.ServiceListener {
    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
      Log.d(TAG, "bluetoothA2dpConnected")
      bluetoothA2dp = proxy as BluetoothA2dp
    }

    override fun onServiceDisconnected(profile: Int) {
      Log.d(TAG, "bluetoothA2dpDisconnected")
      bluetoothA2dp = null
    }
  }

  init {
    bluetoothAdapter.getProfileProxy(mContext, A2dpServiceListener(), BluetoothProfile.A2DP)
  }

  private var audioTrack: AudioTrack =
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

  override fun start(request: StartRequest, responseObserver: StreamObserver<StartResponse>) {
    Log.d(TAG, "start")
    if (audioManager.isVolumeFixed) {
      Log.e(TAG, "volume is fixed, cannot max out the volume")
    } else {
      if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < maxVolume) {
        audioManager.setStreamVolume(
          AudioManager.STREAM_MUSIC,
          maxVolume,
          AudioManager.FLAG_SHOW_UI
        )
      }
    }
    audioTrack.play()
    responseObserver.onNext(StartResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }

  fun BluetoothA2dp.disconnect(device: BluetoothDevice): Boolean =
    this.javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(this, device) as
      Boolean

  override fun close(request: CloseRequest, responseObserver: StreamObserver<CloseResponse>) {
    Log.d(TAG, "close")
    bluetoothA2dp!!.disconnect(host.getConnectedBluetoothDevice())
    responseObserver.onNext(CloseResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }

  override fun playbackAudio(
    responseObserver: StreamObserver<PlaybackAudioResponse>
  ): StreamObserver<PlaybackAudioRequest> {
    Log.d(TAG, "playbackAudio")
    return object : StreamObserver<PlaybackAudioRequest> {
      override fun onNext(value: PlaybackAudioRequest) {
        val data = value.data.toByteArray()
        audioTrack.write(data, 0, data.size)
      }

      override fun onError(t: Throwable?) {
        Log.e(TAG, t.toString())
        responseObserver.onError(t)
      }

      override fun onCompleted() {
        responseObserver.onNext(PlaybackAudioResponse.getDefaultInstance())
        responseObserver.onCompleted()
      }
    }
  }
}
