package com.android.blueberry

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.util.Log
import blueberry.A2DPGrpc.A2DPImplBase
import blueberry.A2dpProto.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
      if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
        audioTrack.pause()
        audioTrack.flush()
        audioTrack.stop()
      }
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

  fun BluetoothA2dp.connect(device: BluetoothDevice) =
    this.javaClass.getMethod("connect", BluetoothDevice::class.java).invoke(this, device)

  override fun openSource(
    request: OpenSourceRequest,
    responseObserver: StreamObserver<OpenSourceResponse>
  ) {
    Log.d(TAG, "openSource")
    val address = request.connection.cookie.toByteArray().decodeToString()
    val bluetoothDevice = host.getConnectedBluetoothDevice()!!
    if (address != bluetoothDevice.address) {
      Log.e(TAG, "error !! addr: $address | bluetoothDevice: ${bluetoothDevice.address}")
      responseObserver.onError(Status.UNKNOWN.asException())
    } else {
      val a2dpState = bluetoothA2dp!!.getConnectionState(bluetoothDevice)
      if (a2dpState == BluetoothProfile.STATE_CONNECTED) {
        Log.d(TAG, "a2dp is already open")
        val source = Source.newBuilder().setCookie(request.connection.cookie).build()
        responseObserver.onNext(OpenSourceResponse.newBuilder().setSource(source).build())
        responseObserver.onCompleted()
      } else if (a2dpState == BluetoothProfile.STATE_CONNECTING) {
        Log.d(TAG, "connecting: waiting to be connected")
        runBlocking {
          val flow = callbackFlow {
            val a2dpBroadcastReceiver: BroadcastReceiver =
              object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                  val state =
                    intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
                  Log.d(TAG, "state: $state")
                  if (state == BluetoothProfile.STATE_CONNECTED) {
                    val source = Source.newBuilder().setCookie(request.connection.cookie).build()
                    responseObserver.onNext(
                      OpenSourceResponse.newBuilder().setSource(source).build()
                    )
                    responseObserver.onCompleted()
                    trySendBlocking(null)
                  } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    responseObserver.onNext(OpenSourceResponse.getDefaultInstance())
                    responseObserver.onCompleted()
                    trySendBlocking(null)
                  }
                }
              }
            val intentFilter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            mContext.registerReceiver(a2dpBroadcastReceiver, intentFilter)

            awaitClose { mContext.unregisterReceiver(a2dpBroadcastReceiver) }
          }

          flow.first()
        }
      } else if (bluetoothA2dp!!.connect(bluetoothDevice)) {
        val source = Source.newBuilder().setCookie(request.connection.cookie).build()
        responseObserver.onNext(OpenSourceResponse.newBuilder().setSource(source).build())
        responseObserver.onCompleted()
      } else {
        Log.d(TAG, "failed to connect")
        responseObserver.onError(Status.UNKNOWN.asException())
      }
    }
  }

  override fun waitSource(
    request: WaitSourceRequest,
    responseObserver: StreamObserver<WaitSourceResponse>
  ) {
    val address = request.connection.cookie.toByteArray().decodeToString()
    Log.i(TAG, "waitSource: $address")
    val bluetoothDevice = host.getConnectedBluetoothDevice()!!
    if (address != bluetoothDevice.address) {
      Log.d(TAG, "address doesn't match")
      responseObserver.onError(Status.UNKNOWN.asException())
    } else {
      val resp = { success: Boolean ->
        val source =
          if (success) {
            Source.newBuilder().setCookie(request.connection.cookie).build()
          } else {
            Source.getDefaultInstance()
          }
        responseObserver.onNext(WaitSourceResponse.newBuilder().setSource(source).build())
        responseObserver.onCompleted()
      }
      val a2dpState = bluetoothA2dp!!.getConnectionState(bluetoothDevice)
      Log.d(TAG, "a2dpState: $a2dpState")
      if (a2dpState == BluetoothProfile.STATE_CONNECTED) {
        resp(true)
      } else {
        runBlocking {
          val flow = callbackFlow {
            val a2dpBroadcastReceiver: BroadcastReceiver =
              object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                  val state =
                    intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
                  Log.d(TAG, "state: $state")
                  if (state == BluetoothProfile.STATE_CONNECTED ||
                      state == BluetoothProfile.STATE_CONNECTING
                  ) {
                    resp(true)
                    trySendBlocking(null)
                  } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    resp(false)
                    trySendBlocking(null)
                  }
                }
              }
            val intentFilter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            mContext.registerReceiver(a2dpBroadcastReceiver, intentFilter)

            awaitClose { mContext.unregisterReceiver(a2dpBroadcastReceiver) }
          }

          flow.first()
        }
      }
    }
  }

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
    if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
      audioTrack.pause()
      audioTrack.flush()
    }
    audioTrack.play()
    responseObserver.onNext(StartResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }

  override fun suspend(request: SuspendRequest, responseObserver: StreamObserver<SuspendResponse>) {
    Log.d(TAG, "suspend")
    audioTrack.pause()
    responseObserver.onNext(SuspendResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }

  override fun isSuspended(
    request: IsSuspendedRequest,
    responseObserver: StreamObserver<IsSuspendedResponse>
  ) {
    Log.d(TAG, "isSuspended")
    val state = audioTrack.getPlayState()
    val isSuspended = state == AudioTrack.PLAYSTATE_STOPPED || state == AudioTrack.PLAYSTATE_PAUSED
    val resp = IsSuspendedResponse.newBuilder().setIsSuspended(isSuspended).build()
    responseObserver.onNext(resp)
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
        if (bluetoothA2dp!!.getConnectionState(host.getConnectedBluetoothDevice()) ==
            BluetoothProfile.STATE_CONNECTED
        ) {
          audioTrack.write(data, 0, data.size)
        }
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

  override fun getAudioEncoding(
    request: GetAudioEncodingRequest,
    responseObserver: StreamObserver<GetAudioEncodingResponse>
  ) {
    Log.d(TAG, "captureAudio")
    val sampleRate = audioTrack.getSampleRate()
    val encoding = audioTrack.getFormat().getEncoding()
    if (sampleRate != 44100 || sampleRate != 4800 || encoding != AudioFormat.ENCODING_PCM_16BIT) {
      responseObserver.onError(Status.UNKNOWN.asException())
    } else {
      val audioEncoding =
        if (sampleRate == 44100) {
          AudioEncoding.PCM_S16_LE_44K1_STEREO
        } else {
          AudioEncoding.PCM_S16_LE_48K_STEREO
        }
      responseObserver.onNext(
        GetAudioEncodingResponse.newBuilder().setEncoding(audioEncoding).build()
      )
      responseObserver.onCompleted()
    }
  }
}
