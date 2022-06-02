package com.android.pandora

import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCallback

import pandora.GATTGrpc.GATTImplBase
import pandora.GattProto.*
import pandora.HostProto.*

import android.net.MacAddress
import android.util.Log
import java.util.UUID;

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver

// coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import android.os.Handler

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gatt(private val context: Context) : GATTImplBase() {
    private val TAG = "PandoraGatt"

    private val scope: CoroutineScope
    private val flow: Flow<Intent>

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = bluetoothGatt
                deviceConnected = true
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            servicesDiscovered = true
        }
    }

    var servicesDiscovered = false
    var deviceConnected = false


    private val baseUUID: String = "-0000-1000-8000-00805f9b34fb"
    private val handlePadding: Int = 4
    private val uuidPadding: Int = 8

    private fun BluetoothGatt.discoverServiceByUuid(serviceUUID: UUID): Boolean =
        this.javaClass
                .getMethod("discoverServiceByUuid", UUID::class.java)
                .invoke(gatt, serviceUUID) as Boolean

    private fun String.toBaseUuid(): UUID =
        UUID.fromString(this.lowercase().padStart(uuidPadding, '0').plus(baseUUID))

    init {
        scope = CoroutineScope(Dispatchers.Default)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        flow = intentFlow(context, intentFilter).shareIn(scope, SharingStarted.Eagerly)
    }

    fun deinit() {
        scope.cancel()
    }

    override fun connectLe(request: AddrRequest, responseObserver: StreamObserver<AddrResponse>) {
        grpcUnary<AddrResponse>(scope, responseObserver) {

            val address = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            val device = bluetoothAdapter.getRemoteDevice(address)
            Log.i(TAG, "ConnectLe: connectGatt() on device $device")
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            while(!deviceConnected) {
                delay(100L)
            }
            AddrResponse.getDefaultInstance()
        }
    }

    override fun connectBrEdr(request: AddrRequest, responseObserver: StreamObserver<AddrResponse>) {
        grpcUnary<AddrResponse>(scope, responseObserver) {
            Log.e(TAG, "connectBrEdr")
            val address = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            val device = bluetoothAdapter.getRemoteDevice(address)
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                Log.e(TAG, "connectBrEdr createBond()")
                device.createBond()
            }
            scope.asyncConnectBrEdr(device)
            AddrResponse.getDefaultInstance()
        }
    }

    fun CoroutineScope.asyncConnectBrEdr(device: BluetoothDevice) = async {
        while(device.getBondState() != BluetoothDevice.BOND_BONDED) {
            delay(100L)
        }
        Log.i(TAG, "asyncConnectBrEdr: connectGatt() on device $device")
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_BREDR)
    }


    override fun confirmPasskey(request: AddrRequest, responseObserver: StreamObserver<AddrResponse>) {
        grpcUnary<AddrResponse>(scope, responseObserver) {
            Log.i(TAG, "confirmPasskey")
            val address = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            val device = bluetoothAdapter.getRemoteDevice(address)
            device.setPairingConfirmation(true)
            AddrResponse.getDefaultInstance()
        }
    }

    override fun disconnect(request: Empty, responseObserver: StreamObserver<Empty>) {
        grpcUnary<Empty>(scope, responseObserver) {
            Log.i(TAG, "disconnect")
            delay(200L)
            gatt?.disconnect()
            Empty.getDefaultInstance()
        }
    }

    override fun exchangeMTU(request: ExchangeMTURequest, responseObserver: StreamObserver<ExchangeMTUResponse>) {
        grpcUnary<ExchangeMTUResponse>(scope, responseObserver) {
            val mtu = request.mtu
            Log.i(TAG, "exchangeMTU: MTU=$mtu")
            gatt?.requestMtu(mtu)
            ExchangeMTUResponse.getDefaultInstance()
        }
    }

    override fun discoverServices(request: Empty, responseObserver: StreamObserver<Empty>) {
        grpcUnary<Empty>(scope, responseObserver) {
            Log.i(TAG, "discoverServices")
            if (!servicesDiscovered) {
                gatt?.discoverServices()
                while(!servicesDiscovered) {
                    delay(100L)
                }
            }
            Empty.getDefaultInstance()
        }
    }

    override fun discoverServicesByUUID(request: DiscoverServicesRequest, responseObserver: StreamObserver<DiscoverServicesResponse>) {
        grpcUnary<DiscoverServicesResponse>(scope, responseObserver) {
            Log.i(TAG, "discoverServicesByUUID")
            var serviceUUID = request.uuidList[0].toBaseUuid()
            delay(1000L)
            gatt?.discoverServiceByUuid(serviceUUID)
            DiscoverServicesResponse.getDefaultInstance()
        }
    }

    override fun discoverServicesByUUID128(request: DiscoverServicesRequest, responseObserver: StreamObserver<DiscoverServicesResponse>) {
        grpcUnary<DiscoverServicesResponse>(scope, responseObserver) {
            Log.i(TAG, "discoverServicesByUUID")
            var serviceUUID = UUID.fromString(request.uuidList[0].lowercase())
            delay(1000L)
            gatt?.discoverServiceByUuid(serviceUUID)
            DiscoverServicesResponse.getDefaultInstance()
        }
    }

    override fun writeCharacteristic(request: WriteCharacteristicRequest, responseObserver: StreamObserver<WriteCharacteristicResponse>) {
        grpcUnary<WriteCharacteristicResponse>(scope, responseObserver) {
            Log.i(TAG, "writeCharacteristic")
            val requestedHandle = request.handle
            var characteristicWritten = false
            for (service: BluetoothGattService in gatt?.services.orEmpty()) {
                for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
                    val charhandle = Integer.toHexString(characteristic.instanceId).padStart(handlePadding, '0').uppercase()
                    if (charhandle == requestedHandle) {
                        characteristic.setValue(ByteArray(request.size))
                        gatt?.writeCharacteristic(characteristic)
                        characteristicWritten = true
                        break;
                    }
                }
                if (characteristicWritten) {
                    break
                }
            }
            WriteCharacteristicResponse.newBuilder().setSuccess(characteristicWritten).build()
        }
    }

    override fun discoverCharacteristics(request: DiscoverCharacteristicsRequest, responseObserver: StreamObserver<DiscoverCharacteristicsResponse>) {
        grpcUnary<DiscoverCharacteristicsResponse>(scope, responseObserver) {
            Log.i(TAG, "discoverCharacteristics")
            var serviceUUID = request.serviceUuidsList[0]
            if (!servicesDiscovered) {
                gatt?.discoverServices()
                while(!servicesDiscovered) {
                    delay(100L)
                }
            }
            for (service: BluetoothGattService in gatt?.services.orEmpty()) {
                if (service.uuid.toString().startsWith(serviceUUID)) {
                    Log.i(TAG, "Found characteristics for service $serviceUUID : " + service.characteristics)
                }
            }

            //TODO include response
            DiscoverCharacteristicsResponse.getDefaultInstance()
        }
    }

    override fun discoverDescriptors(request: DiscoverDescriptorsRequest, responseObserver: StreamObserver<DiscoverDescriptorsResponse>) {
        grpcUnary<DiscoverDescriptorsResponse>(scope, responseObserver) {
            Log.i(TAG, "discoverCharacteristics")
            if (!servicesDiscovered) {
                gatt?.discoverServices()
                while(!servicesDiscovered) {
                    delay(100L)
                }
            }
            /*for (service: BluetoothGattService in gatt?.services.orEmpty()) {
                for (characteristic: BluetoothGattCharacteristic in service.characteristics) {
                    //TODO store characteristics
                }
            }*/

            DiscoverDescriptorsResponse.getDefaultInstance()
        }
    }

    override fun readCharacteristic(request: ReadCharacteristicRequest, responseObserver: StreamObserver<ReadCharacteristicResponse>) {
        grpcUnary<ReadCharacteristicResponse>(scope, responseObserver) {
            val requestedHandle = request.handle
            var characteristicRead = false
            Log.i(TAG, "readCharacteristic")
            if (!servicesDiscovered) {
                gatt?.discoverServices()
                while(!servicesDiscovered) {
                    Log.i("ETIENNE", "readCharacteristic")
                    delay(100L)
                }
            }
            delay(2000L)
            for (service: BluetoothGattService in gatt?.services.orEmpty()) {
                Log.e("ETIENNE", "service readCharacteristic");
                for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
                    Log.e("ETIENNE", "characteristic readCharacteristic");
                    val charhandle = Integer.toHexString(characteristic.instanceId).padStart(handlePadding, '0').uppercase()
                    if (charhandle == requestedHandle) {
                        gatt?.readCharacteristic(characteristic)
                        Log.e("ETIENNE", "sent readCharacteristic");
                        characteristicRead = true
                        break
                    }
                }
                if (characteristicRead) {
                    break
                }
            }
            ReadCharacteristicResponse.getDefaultInstance()
        }
    }
}