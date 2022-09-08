package com.android.pandora

import android.bluetooth.BluetoothDevice.ADDRESS_TYPE_PUBLIC
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.util.Log
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pandora.L2CAPGrpc.L2CAPImplBase
import pandora.L2capProto.*


@kotlinx.coroutines.ExperimentalCoroutinesApi
class L2cap(val context: Context) : L2CAPImplBase() {
  private val TAG = "PandoraL2cap"
  private val scope: CoroutineScope
  private val receiveScope: CoroutineScope
  private val BLUETOOTH_SERVER_SOCKET_TIMEOUT: Int = 10000
  private val BUFFER_SIZE = 512

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  private lateinit var bluetoothSocket: BluetoothSocket
  private var inStream: InputStream? = null
  private var outputStream: OutputStream? = null

  init {
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
    receiveScope = CoroutineScope(Dispatchers.IO)
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
    receiveScope.cancel()
  }

  suspend fun receive(): ByteArray {
    return withContext(Dispatchers.IO) {
      val buf = ByteArray(BUFFER_SIZE)
      inStream!!.read(buf, 0, BUFFER_SIZE) // blocking
      buf
    }
  }

  /**
   * Open a BluetoothServerSocket to accept connections
   */
  override fun startBluetoothServerSocket(
    request: StartBluetoothServerSocketRequest,
    responseObserver: StreamObserver<StartBluetoothServerSocketResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "startBluetoothServerSocket: secure=${request.secure}")
      val bluetoothServerSocket = if (request.secure) {
        bluetoothAdapter.listenUsingL2capChannel()
      } else {
        bluetoothAdapter.listenUsingInsecureL2capChannel()
      }

      val psm = bluetoothServerSocket.getPsm()
      try {
        bluetoothSocket = bluetoothServerSocket.accept(BLUETOOTH_SERVER_SOCKET_TIMEOUT)
      } catch (e: IOException) {
        Log.e(TAG, "bluetoothServerSocket not accepted", e)
        return@grpcUnary StartBluetoothServerSocketResponse.newBuilder().build()
      }

      // Get the BluetoothSocket input and output streams
      try {
        inStream = bluetoothSocket.getInputStream()!!
        outputStream = bluetoothSocket.getOutputStream()!!
      } catch (e: IOException) {
        Log.e(TAG, "bluetoothSocket not created", e)
      }

      // for the purpose of PTS testing,
      // create a separate thread to read what received from the socket
      receiveScope.launch {
        withContext(Dispatchers.IO) {
          val result = receive()
          Log.i(TAG, "message size: ${result.size}, message content: $result")
        }
      }

      StartBluetoothServerSocketResponse.newBuilder().build()
    }
  }


  /**
   * Set device to send LE based connection request
   */
  override fun createLECreditBasedChannel(
    request: CreateLECreditBasedChannelRequest,
    responseObserver: StreamObserver<CreateLECreditBasedChannelResponse>,
  ) {
    // Creates a gRPC coroutine in a given coroutine scope which executes a given suspended function
    // returning a gRPC response and sends it on a given gRPC stream observer.
    grpcUnary(scope, responseObserver) {
      Log.d(TAG, "StartLEConnection: secure=${request.secure}, psm=${request.psm}")
      val device = request.connection.toBluetoothDevice(bluetoothAdapter)
      val psm = request.psm

      try {
        bluetoothSocket = if (request.secure) {
          device.createL2capChannel(psm)
        } else {
          device.createInsecureL2capChannel(psm)
        }
        bluetoothSocket.connect()

      } catch (e: IOException) {
        Log.d(TAG, "bluetoothSocket not connected: $e")
        throw e
      }

      // Get the BluetoothSocket input and output streams
      try {
        inStream = bluetoothSocket.getInputStream()!!
        outputStream = bluetoothSocket.getOutputStream()!!
      } catch (e: IOException) {
        Log.e(TAG, "bluetooth sockets not created", e)
      }

      // Response sent to client
      CreateLECreditBasedChannelResponse.newBuilder().build()
    }
  }

  /**
   * send LE data packet
   */
  override fun sendLEDataPacket(
    request: SendLEDataPacketRequest,
    responseObserver: StreamObserver<SendLEDataPacketResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "sendLEDataPacket")
      val buffer = request.data!!.toByteArray()
      try {
        outputStream!!.write(buffer)
        outputStream!!.flush()
      } catch (e: IOException) {
        Log.e(TAG, "Exception during writing to sendLEDataPacket output stream", e)
      }

      // Response sent to client
      SendLEDataPacketResponse.newBuilder().build()
    }
  }

  override fun receiveData(
    request: ReceiveDataRequest,
    responseObserver: StreamObserver<ReceiveDataResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      Log.d(TAG, "receiveData")
      val buf = receive()

      ReceiveDataResponse.newBuilder().setData(ByteString.copyFrom(buf)).build()
    }
  }
}

  

