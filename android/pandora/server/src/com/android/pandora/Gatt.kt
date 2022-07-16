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

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null

    private var servicesDiscovered = false
    private var deviceConnected = false

    private val baseUUID: String = "-0000-1000-8000-00805f9b34fb"
    private val handlePadding: Int = 4
    private val uuidPadding: Int = 8

    private fun String.toBaseUuid(): UUID =
        UUID.fromString(this.lowercase().padStart(uuidPadding, '0').plus(baseUUID))

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "onConnectionStateChange : " + newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                deviceConnected = true
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt.close()
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "onServicesDiscovered : " + status)
            servicesDiscovered = true
        }
    }

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
            var device = request.address.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "ConnectLe: connectGatt() on device $device")
            gatt = device?.connectGatt(context, false, gattCallback,
                    BluetoothDevice.TRANSPORT_LE)
            while(!deviceConnected) {
                delay(100L)
            }
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
}