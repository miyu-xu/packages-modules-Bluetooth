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
import kotlinx.coroutines.flow.map
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

    private var mBluetoothDevice: BluetoothDevice? = null

    private var mGattOverLe = false;

    private var servicesDiscovered = false
    private var deviceConnected = false

    private val baseUUID: String = "-0000-1000-8000-00805f9b34fb"
    private val handlePadding: Int = 4
    private val uuidPadding: Int = 8

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "onConnectionStateChange : " + newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                deviceConnected = true
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt?.close()
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "onServicesDiscovered : " + status)
            servicesDiscovered = true
        }

        override fun onCharacteristicRead(bluetoothGatt: BluetoothGatt, char: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            Log.i(TAG, "onCharacteristicRead : " + status + " handle " + char.getUuid())
        }
    }

    private fun String.toBaseUuid(): UUID =
        UUID.fromString(this.lowercase().padStart(uuidPadding, '0').plus(baseUUID))

    init {
        scope = CoroutineScope(Dispatchers.Default)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        flow = intentFlow(context, intentFilter).shareIn(scope, SharingStarted.Eagerly)
    }

    fun deinit() {
        scope.cancel()
    }

    /**
     * Le connection doesn't need pairing, but PTS-bot wants the MMI to return only when the device
     * is connected.
     */
    override fun connectLe(request: AddrRequest, responseObserver: StreamObserver<AddrResponse>) {
        grpcUnary<AddrResponse>(scope, responseObserver) {
            if (!mGattOverLe && gatt != null) {
                gatt?.close()
                gatt = null
            }
            val addr = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            mBluetoothDevice = bluetoothAdapter.getRemoteDevice(addr)
            Log.i(TAG, "ConnectLe: connectGatt() on device $mBluetoothDevice")
            mGattOverLe = true;
            gatt = mBluetoothDevice?.connectGatt(context, false, gattCallback,
                    BluetoothDevice.TRANSPORT_LE)
            while(!deviceConnected) {
                delay(100L)
            }
            AddrResponse.getDefaultInstance()
        }
    }

    override fun connectBrEdr(request: AddrRequest,
                responseObserver: StreamObserver<AddrResponse>) {
        grpcUnary<AddrResponse>(scope, responseObserver) {
            if (mGattOverLe && gatt != null) {
                gatt?.close()
                gatt = null
            }
            val addr = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            val device = bluetoothAdapter.getRemoteDevice(addr)
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                Log.e(TAG, "connectBrEdr - device not paired, creating bond.")
                device.createBond()
                AddrResponse.getDefaultInstance()
            } else {
                Log.e(TAG, "connectBrEdr - connecting.")
                scope.asyncConnectBrEdr(device)
                AddrResponse.getDefaultInstance()
            }
        }
    }

    fun CoroutineScope.asyncConnectBrEdr(device: BluetoothDevice) = async {
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Log.i(TAG, "asyncConnectBrEdr: waiting for device to be bonded.")
            flow
                .filter { it.getAction() == BluetoothDevice.ACTION_BOND_STATE_CHANGED }
                .filter {
                    it.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE).address ==
                            device.address
                }
                .map { it.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR) }
                .filter { it == BluetoothDevice.BOND_BONDED }
                .first()
        }
        Log.i(TAG, "connectGatt() on device $device")
        mGattOverLe = false;
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_BREDR)
    }


    override fun confirmPasskey(request: AddrRequest,
            responseObserver: StreamObserver<AddrResponse>) {
        grpcUnary<AddrResponse>(scope, responseObserver) {
            Log.i(TAG, "confirmPasskey and connect device async")
            val addr = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            val device = bluetoothAdapter.getRemoteDevice(addr)
            device.setPairingConfirmation(true)
            scope.asyncConnectBrEdr(device)
            AddrResponse.getDefaultInstance()
        }
    }

    override fun disconnect(request: Empty, responseObserver: StreamObserver<Empty>) {
        grpcUnary<Empty>(scope, responseObserver) {
            Log.i(TAG, "disconnect")
            // Some tests don't wait before calling disconnect so we have to wait manually.
            delay(200L)
            gatt?.disconnect()
            Empty.getDefaultInstance()
        }
    }

    override fun exchangeMTU(request: ExchangeMTURequest,
            responseObserver: StreamObserver<ExchangeMTUResponse>) {
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

    override fun discoverServicesByUUID(request: DiscoverServicesRequest,
            responseObserver: StreamObserver<DiscoverServicesResponse>) {
        grpcUnary<DiscoverServicesResponse>(scope, responseObserver) {
            var serviceUUID = request.uuidList[0].toBaseUuid()
            Log.i(TAG, "discoverServicesByUUID - UUID: " + serviceUUID)
            // TODO find a way to know when internal discovery is done.
            // As GATT over LE initiates a service discovery by its own and no callback is called.
            // We can't know when GATTC_Discover is free so waiting 25s works for now.
            if (mGattOverLe) {
                delay(25000L)
            }
            // discoverServiceByUuid() doesn't trigger the onServicesDiscovered callback so we
            // have manually wait
            delay(1000L)
            gatt?.discoverServiceByUuid(serviceUUID)
            DiscoverServicesResponse.getDefaultInstance()
        }
    }

    override fun discoverServicesByUUID128(request: DiscoverServicesRequest,
            responseObserver: StreamObserver<DiscoverServicesResponse>) {
        grpcUnary<DiscoverServicesResponse>(scope, responseObserver) {
            var serviceUUID = UUID.fromString(request.uuidList[0].lowercase())
            Log.i(TAG, "discoverServicesByUUID128 - UUID: "+ serviceUUID)
            // TODO find a way to know when internal discovery is done.
            // As GATT over LE initiates a service discovery by its own and no callback is called.
            // We can't know when GATTC_Discover is free so waiting 25s works for now.
            if (mGattOverLe) {
                delay(25000L)
            }
            // discoverServiceByUuid() doesn't trigger the onServicesDiscovered callback so we
            // have manually wait
            delay(1000L)
            gatt?.discoverServiceByUuid(serviceUUID)
            DiscoverServicesResponse.getDefaultInstance()
        }
    }

    override fun writeCharacteristic(request: WriteCharacteristicRequest,
            responseObserver: StreamObserver<WriteCharacteristicResponse>) {
        grpcUnary<WriteCharacteristicResponse>(scope, responseObserver) {
            val requestedHandle = request.handle
            var characteristicWritten = false
            Log.i(TAG, "writeCharacteristic, requested handle " + requestedHandle);
            for (service: BluetoothGattService in gatt?.services.orEmpty()) {
                for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
                    val charhandle = Integer.toHexString(
                            characteristic.instanceId).padStart(handlePadding, '0').uppercase()
                    if (charhandle == requestedHandle) {
                        Log.i(TAG, "writeCharacteristic, characteristic found, writing");
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

    override fun discoverCharacteristics(request: DiscoverCharacteristicsRequest,
            responseObserver: StreamObserver<DiscoverCharacteristicsResponse>) {
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
                    Log.i(TAG, "Found characteristics for service $serviceUUID : "
                            + service.characteristics)
                }
            }

            //TODO include response
            DiscoverCharacteristicsResponse.getDefaultInstance()
        }
    }

    override fun discoverDescriptors(request: DiscoverDescriptorsRequest,
            responseObserver: StreamObserver<DiscoverDescriptorsResponse>) {
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

    override fun readCharacteristic(request: ReadCharacteristicRequest,
            responseObserver: StreamObserver<ReadCharacteristicResponse>) {
        grpcUnary<ReadCharacteristicResponse>(scope, responseObserver) {
            val requestedHandle = request.handle
            var characteristicRead = false
            val services = gatt?.getServices().orEmpty()
            Log.i(TAG, "readCharacteristic - services list size: "
                    + services + " handle: " + requestedHandle);
            for (service: BluetoothGattService in services) {
                for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
                    val charhandle = Integer.toHexString(
                            characteristic.instanceId).padStart(handlePadding, '0').uppercase()
                    if (charhandle == requestedHandle) {
                        gatt?.readCharacteristic(characteristic)
                        Log.i(TAG, "readCharacteristic - characteristic read.");
                        characteristicRead = true
                        break
                    }
                }
                if (characteristicRead) {
                    break
                }
            }
            /*var characteristic = BluetoothGattCharacteristic(request.handle.toBaseUuid(), BluetoothGattCharacteristic.PROPERTY_READ, 0)
            gatt?.readCharacteristic(characteristic)
            delay(1000L)
            gatt?.readCharacteristic(characteristic)*/
            ReadCharacteristicResponse.getDefaultInstance()
        }
    }

    private fun waitTimeout(time: Long, timeoutSeconds: Int) {
        
    }
}