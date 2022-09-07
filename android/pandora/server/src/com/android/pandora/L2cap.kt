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
  private val replyScope: CoroutineScope

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  private lateinit var bluetoothSocket: BluetoothSocket
  private var bluetoothGatt: BluetoothGatt? = null
  private val localContext= context
  private val SCAN_DURATION_MILLIS: Long = 60000
  private var mmServerSocketInStream: InputStream? = null
  private var mmServerSocketOutStream: OutputStream? = null

  init {
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
    replyScope= CoroutineScope(Dispatchers.IO)
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
    replyScope.cancel()
  }

  suspend fun receive(): ByteArray {
    return withContext(Dispatchers.IO) {
      val size=512
      val buf = ByteArray(size)
      val numBytesRead=mmServerSocketInStream!!.read(buf, 0, size) // blocking
      Log.d(TAG, "numBytesRead: "+numBytesRead.toString())
      mmServerSocketOutStream!!.write(buf,0,numBytesRead)
      mmServerSocketOutStream!!.flush()
      buf
    }
  }

  /**
   * Set the device in discoverable mode for #SCAN_DURATION_MILLIS milliseconds.
   * @param request Request sent by the client.
   * @param responseObserver Response to build and set back to the client.
   */
  override fun startAdvertisement(
    request: Empty,
    responseObserver: StreamObserver<Empty>,
  ) {
    // Creates a gRPC coroutine in a given coroutine scope which executes a given suspended function
    // returning a gRPC response and sends it on a given gRPC stream observer.
    grpcUnary<Empty>(scope, responseObserver) {

      val bluetoothServerSocket= bluetoothAdapter.listenUsingInsecureL2capChannel()
      val psm=bluetoothServerSocket.getPsm()
      Log.i(TAG, "psm value: "+psm.toString())

      
      Log.i(TAG, "startAdvertisement")
      val advertiser = bluetoothAdapter.getBluetoothLeAdvertiser()
      val advSettings = AdvertiseSettings
                          .Builder()
                          .setConnectable(true)
                          .setOwnAddressType(AdvertisingSetParameters.ADDRESS_TYPE_PUBLIC)
                          .setTimeout(120000).build()
      val advData = AdvertiseData.Builder().build()
      val advCallback = object: AdvertiseCallback() {
        override fun onStartFailure (errorCode: Int) {
          Log.i(TAG, "Advertising failed: $errorCode")
        }
        override fun onStartSuccess (settingsInEffect: AdvertiseSettings) {
          Log.i(TAG, "Advertising success")
        }
      }
      advertiser.startAdvertising(advSettings, advData, advCallback)

      Log.d(TAG, "bluetoothServerSocket start accepting")
      try{
        bluetoothSocket=bluetoothServerSocket.accept(10000)
      }catch( e:IOException){
        Log.e(TAG, "bluetooth sockets not accepted", e)
        return@grpcUnary Empty.getDefaultInstance()
      }

      Log.d(TAG, "bluetoothSocket start getting stream")
      // Get the BluetoothSocket input and output streams
      try {
        mmServerSocketInStream = bluetoothSocket.getInputStream()!!
        mmServerSocketOutStream = bluetoothSocket.getOutputStream()!!
      } catch (e: IOException) {
        Log.e(TAG, "bluetooth sockets not created", e)
      }

      replyScope.launch {
        withContext(Dispatchers.IO){
          Log.d(TAG,"replyScope start")
          var cnt=1
          while(cnt<10){
            val result = receive()
            Log.d(TAG,cnt.toString()+" replyScope size: "+result.size+" result: "+result.toString())
            cnt+=1
          }
        }
      }

      // Response sent to client
      Empty.getDefaultInstance()
    }
  }


  /**
   * TODO
   */
  override fun makeConnection(
    request: MakeConnectionRequest,
    responseObserver: StreamObserver<MakeConnectionResponse>,
  ) {
    // Creates a gRPC coroutine in a given coroutine scope which executes a given suspended function
    // returning a gRPC response and sends it on a given gRPC stream observer.
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "before MakeConnection toBluetoothDevice")
      val device = request.connection.toBluetoothDevice(bluetoothAdapter)
      Log.i(TAG, "MakeConnection: device=$device")


      val psm=0x0025; // try 0x01 for psm
      // Log.i(TAG, "devices size: "+ devices.size.toString())
      // // val bluetoothSocket=devices.elementAt(0).createL2capChannel(25);

      // var bluetoothServerSocket:BluetoothServerSocket?=null
      try{
        // bluetoothGatt = device.connectGatt(localContext, false, bluetoothGattCallback)
        // bluetoothSocket=device.createL2capChannel(psm);
        bluetoothSocket=device.createInsecureL2capChannel(psm);

      }catch( e:IOException){
        Log.d(TAG, "bluetoothSocket: "+e.toString())
        throw e;
      }

      // Get the BluetoothSocket input and output streams
      try {
        // mmInStream = bluetoothSocket.getInputStream()
        mmServerSocketOutStream = bluetoothSocket.getOutputStream()!!
      } catch (e: IOException) {
        Log.e(TAG, "bluetooth sockets not created", e)
      }



      // Response sent to client
      // Empty.getDefaultInstance()
      MakeConnectionResponse.newBuilder().build()
    }
  }


  /**
   * TODO
   */
  override fun sendLEDataPacket(
    request: SendLEDataPacketRequest,
    responseObserver: StreamObserver<SendLEDataPacketResponse>,
  ) {
    // Creates a gRPC coroutine in a given coroutine scope which executes a given suspended function
    // returning a gRPC response and sends it on a given gRPC stream observer.
    grpcUnary(scope, responseObserver) {
      Log.i(TAG, "start sendLEDataPacket")
      val buffer=request.data!!.toByteArray()
      try {
        mmServerSocketOutStream!!.write(buffer)
        mmServerSocketOutStream!!.flush()

      } catch (e: IOException) {
        Log.e(TAG, "Exception during write", e)
      }

      // Response sent to client
      // Empty.getDefaultInstance()
      SendLEDataPacketResponse.newBuilder().build()
    }
  }
}

  

