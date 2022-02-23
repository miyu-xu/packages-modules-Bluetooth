package com.android.blueberry

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.util.Log
import blueberry.A2DPGrpc.A2DPImplBase
import blueberry.A2dpProto.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@kotlinx.coroutines.ExperimentalCoroutinesApi
class A2dp(val mContext: Context) : A2DPImplBase() {
  private val TAG = "BlueberryA2dp"

  private val scope: CoroutineScope
  private val flow: Flow<Intent>

  private var audioManager: AudioManager =
    mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

  private val bluetoothManager =
    mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  private var bluetoothA2dp: BluetoothA2dp? = null

  private lateinit var ptsDevice: BluetoothDevice

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

    scope = CoroutineScope(Dispatchers.Default)
    val intentFilter = IntentFilter()
    intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
    intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

    flow = intentFlow(mContext, intentFilter).shareIn(scope, SharingStarted.Eagerly)

  }

  fun deinit() {
    bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
    scope.cancel()
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
    val address = request.connection.cookie.toByteArray().decodeToString()
    Log.i(TAG, "openSource: $address")
    grpcUnary<OpenSourceResponse>(scope, responseObserver) {
      ptsDevice = getPTSDevice(address)
      if (address == ptsDevice.address) {
        // CHECK DISCONNECTED TO ANSWER AN ERROR AS WELL
        val a2dpState = bluetoothA2dp!!.getConnectionState(ptsDevice)
        if (a2dpState != BluetoothA2dp.STATE_CONNECTED) {
          if (a2dpState != BluetoothA2dp.STATE_CONNECTING) {
            bluetoothA2dp!!.connect(ptsDevice)
          }
          flow
            .filter { it.getAction() == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED }
            .filter {
              it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                BluetoothProfile.STATE_CONNECTED
            }
            .first()
        }
        val source = Source.newBuilder().setCookie(request.connection.cookie).build()
        OpenSourceResponse.newBuilder().setSource(source).build()
      } else {
        Log.e(TAG, "wrong address, found: $address expected: ${ptsDevice.address}")
        throw Status.UNKNOWN.asException()
      }
    }
  }

  override fun waitSource(
    request: WaitSourceRequest,
    responseObserver: StreamObserver<WaitSourceResponse>
  ) {
    val address = request.connection.cookie.toByteArray().decodeToString()
    Log.i(TAG, "waitSource: $address")

    grpcUnary<WaitSourceResponse>(scope, responseObserver) {
      ptsDevice = getPTSDevice(address)
      if (address == ptsDevice.address) {
        val a2dpState = bluetoothA2dp!!.getConnectionState(ptsDevice)
        Log.d(TAG, "a2dpState: $a2dpState")
        if (a2dpState != BluetoothProfile.STATE_CONNECTING) {
          val stateFlow =
          // TODO: ANSWER AN ERROR ON DISCONNECTED
          flow
              .filter { it.getAction() == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED }
              .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
              .filter { it == BluetoothProfile.STATE_CONNECTED }
              .first()
        }
        val source = Source.newBuilder().setCookie(request.connection.cookie).build()
        WaitSourceResponse.newBuilder().setSource(source).build()
      } else {
        Log.e(TAG, "wrong address, found: $address expected: ${ptsDevice.address}")
        throw Status.UNKNOWN.asException()
      }
    }
  }

  override fun start(request: StartRequest, responseObserver: StreamObserver<StartResponse>) {
    Log.i(TAG, "start")
    grpcUnary<StartResponse>(scope, responseObserver) {
      if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
        audioTrack.play()
        val stateFlow =
          flow
            .filter { it.getAction() == BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED }
            .filter {
              it.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                BluetoothA2dp.STATE_PLAYING
            }
            .first()
      }
      StartResponse.getDefaultInstance()
    }
  }

  override fun suspend(request: SuspendRequest, responseObserver: StreamObserver<SuspendResponse>) {
    Log.i(TAG, "suspend")
    grpcUnary<SuspendResponse>(scope, responseObserver) {
      audioTrack.pause()
      val stateFlow =
        flow
          .filter { it.getAction() == BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED }
          .filter {
            it.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothAdapter.ERROR) ==
              BluetoothA2dp.STATE_NOT_PLAYING
          }
          .first()
      SuspendResponse.getDefaultInstance()
    }
  }

  override fun isSuspended(
    request: IsSuspendedRequest,
    responseObserver: StreamObserver<IsSuspendedResponse>
  ) {
    Log.d(TAG, "isSuspended")
    val isSuspended = bluetoothA2dp!!.isA2dpPlaying(ptsDevice)
    val resp = IsSuspendedResponse.newBuilder().setIsSuspended(isSuspended).build()
    responseObserver.onNext(resp)
    responseObserver.onCompleted()
  }

  fun BluetoothA2dp.disconnect(device: BluetoothDevice): Boolean =
    this.javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(this, device) as
      Boolean

  override fun close(request: CloseRequest, responseObserver: StreamObserver<CloseResponse>) {
    Log.i(TAG, "close")
    grpcUnary<CloseResponse>(scope, responseObserver) {
      bluetoothA2dp!!.disconnect(ptsDevice)
      val stateFlow =
        flow
          .filter { it.getAction() == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED }
          .filter {
            it.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothAdapter.ERROR) ==
              BluetoothA2dp.STATE_DISCONNECTED
          }
          .first()
      CloseResponse.getDefaultInstance()
    }
  }

  override fun playbackAudio(
    responseObserver: StreamObserver<PlaybackAudioResponse>
  ): StreamObserver<PlaybackAudioRequest> {
    Log.d(TAG, "playbackAudio")
    // Volume is maxed out to avoid any amplitude modification of the provided audio data,
    // enabling the test runner to do comparisons between input and output audio signal.
    // Any volume modification should be done before providing the audio data.
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
    return object : StreamObserver<PlaybackAudioRequest> {
      override fun onNext(value: PlaybackAudioRequest) {
        val data = value.data.toByteArray()
        if (bluetoothA2dp!!.getConnectionState(ptsDevice) == BluetoothProfile.STATE_CONNECTED) {
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

  fun getPTSDevice(address: String): BluetoothDevice {
    return bluetoothAdapter.getBondedDevices()?.first { bondedDevice ->
      bondedDevice.address == address
    }!!
  }
}
