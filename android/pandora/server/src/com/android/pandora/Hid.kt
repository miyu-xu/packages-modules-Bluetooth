package com.android.pandora
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDevice.SUBCLASS1_COMBO
import android.bluetooth.BluetoothHidDevice.SUBCLASS2_UNCATEGORIZED
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidHost
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import io.grpc.stub.StreamObserver
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.HIDGrpc.HIDImplBase
import pandora.HidProto.SendHostReportRequest
import pandora.HidProto.SendHostReportResponse
@kotlinx.coroutines.ExperimentalCoroutinesApi

class Hid(val context: Context) : HIDImplBase() {
  private val TAG = "PandoraHid"

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

  private val bluetoothManager =
      context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter = bluetoothManager.adapter
  private val bluetoothHidHost = getProfileProxy<BluetoothHidHost>(context, BluetoothProfile.HID_HOST)
  private val bluetoothHidDevice = getProfileProxy<BluetoothHidDevice>(context, BluetoothProfile.HID_DEVICE)

  val HIDD_REPORT_DESC = byteArrayOf(0x05.toByte(),
                                     0x01.toByte(),
                                     0x09.toByte(),
                                     0x06.toByte(),
                                     0xA1.toByte(),
                                     0x01.toByte(),
                                     0x85.toByte(),
                                     1,
                                     0x05.toByte(),
                                     0x07.toByte(),
                                     0x19.toByte(),
                                     0xE0.toByte(),
                                     0x29.toByte(),
                                     0xE7.toByte(),
                                     0x15.toByte(),
                                     0x00.toByte(),
                                     0x25.toByte(),
                                     0x01.toByte(),
                                     0x75.toByte(),
                                     0x01.toByte(),
                                     0x95.toByte(),
                                     0x08.toByte(),
                                     0x81.toByte(),
                                     0x02.toByte(),
                                     0x75.toByte(),
                                     0x08.toByte(),
                                     0x95.toByte(),
                                     0x01.toByte(),
                                     0x81.toByte(),
                                     0x01.toByte(),
                                     0x75.toByte(),
                                     0x08.toByte(),
                                     0x95.toByte(),
                                     0x06.toByte(),
                                     0x15.toByte(),
                                     0x00.toByte(),
                                     0x25.toByte(),
                                     0x65.toByte(),
                                     0x05.toByte(),
                                     0x07.toByte(),
                                     0x19.toByte(),
                                     0x00.toByte(),
                                     0x29.toByte(),
                                     0x65.toByte(),
                                     0x81.toByte(),
                                     0x00.toByte(),
                                     0xC0.toByte(),
                                     0x05.toByte(),
                                     0x01.toByte(),
                                     0x09.toByte(),
                                     0x02.toByte(),
                                     0xA1.toByte(),
                                     0x01.toByte(),
                                     0x85.toByte(),
                                     2,
                                     0x09.toByte(),
                                     0x01.toByte(),
                                     0xA1.toByte(),
                                     0x00.toByte(),
                                     0x05.toByte(),
                                     0x09.toByte(),
                                     0x19.toByte(),
                                     0x01.toByte(),
                                     0x29.toByte(),
                                     0x03.toByte(),
                                     0x15.toByte(),
                                     0x00.toByte(),
                                     0x25.toByte(),
                                     0x01.toByte(),
                                     0x75.toByte(),
                                     0x01.toByte(),
                                     0x95.toByte(),
                                     0x03.toByte(),
                                     0x81.toByte(),
                                     0x02.toByte(),
                                     0x75.toByte(),
                                     0x05.toByte(),
                                     0x95.toByte(),
                                     0x01.toByte(),
                                     0x81.toByte(),
                                     0x01.toByte(),
                                     0x05.toByte(),
                                     0x01.toByte(),
                                     0x09.toByte(),
                                     0x30.toByte(),
                                     0x09.toByte(),
                                     0x31.toByte(),
                                     0x09.toByte(),
                                     0x38.toByte(),
                                     0x15.toByte(),
                                     0x81.toByte(),
                                     0x25.toByte(),
                                     0x7F.toByte(),
                                     0x75.toByte(),
                                     0x08.toByte(),
                                     0x95.toByte(),
                                     0x03.toByte(),
                                     0x81.toByte(),
                                     0x06.toByte(),
                                     0xC0.toByte(),
                                     0xC0.toByte())

  init {
    bluetoothHidDevice.registerApp(BluetoothHidDeviceAppSdpSettings("pts-device", "a test device for PTS", "PandoraServer", SUBCLASS1_COMBO, HIDD_REPORT_DESC), null, null, { runnable -> runnable.run() }, object : BluetoothHidDevice.Callback() {
      override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
        bluetoothHidDevice.replyReport(device, type, id, byteArrayOf(1))
      }
    })
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
  }

  override fun sendHostReport(
    request: SendHostReportRequest,
    responseObserver: StreamObserver<SendHostReportResponse>,
  ) {
    grpcUnary(scope, responseObserver) {
      bluetoothHidHost.setReport(
        request.address.toBluetoothDevice(bluetoothAdapter),
        request.reportType.number.toByte(),
        request.report)
      SendHostReportResponse.getDefaultInstance()
    }
  }
}
