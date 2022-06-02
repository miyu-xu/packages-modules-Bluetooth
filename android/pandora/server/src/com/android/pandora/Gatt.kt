package com.android.pandora

import android.content.Context

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCallback

import pandora.GATTGrpc.GATTImplBase
import pandora.GattProto.*

import android.net.MacAddress
import android.util.Log

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver

// coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.channels.awaitClose

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gatt(private val context: Context) : GATTImplBase() {
    private val TAG = "PandoraGatt"

    private val scope: CoroutineScope

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == 2) {
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

    init {
        scope = CoroutineScope(Dispatchers.Default)
    }

    fun deinit() {
        scope.cancel()
    }

    override fun connectLe(request: ConnectLeRequest, responseObserver: StreamObserver<ConnectLeResponse>) {
        grpcUnary<ConnectLeResponse>(scope, responseObserver) {
            //var isConnected = false

            val address = MacAddress.fromBytes(request.address.toByteArray()).toString().uppercase()
            val device = bluetoothAdapter.getRemoteDevice(address)
            Log.i(TAG, "ConnectLe: connectGatt() on device $device")
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            while(!deviceConnected) {
                delay(100L)
            }
            ConnectLeResponse.getDefaultInstance()
        }
    }

    override fun disconnect(request: Empty, responseObserver: StreamObserver<Empty>) {
        grpcUnary<Empty>(scope, responseObserver) {
            Log.i(TAG, "disconnect")
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

    override fun writeCharacteristic(request: WriteCharacteristicRequest, responseObserver: StreamObserver<WriteCharacteristicResponse>) {
        grpcUnary<WriteCharacteristicResponse>(scope, responseObserver) {
            Log.i(TAG, "writeCharacteristic")
            val requestedHandle = request.handle
            for (service: BluetoothGattService in gatt?.services.orEmpty()) {
                for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
                    val charhandle = Integer.toHexString(characteristic.instanceId).padStart(4, '0').uppercase()
                    if (charhandle == requestedHandle) {
                        characteristic.setValue(ByteArray(request.size))
                        gatt?.writeCharacteristic(characteristic)
                        break;
                    }
                }
            }
            WriteCharacteristicResponse.newBuilder().setSuccess(true).build()
        }
    }



}