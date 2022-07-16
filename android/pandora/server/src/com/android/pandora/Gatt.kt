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
import java.util.UUID
import java.util.HashMap;

import com.google.protobuf.ByteString
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Gatt(private val context: Context) : GATTImplBase() {
    private val TAG = "PandoraGatt"

    private val mScope: CoroutineScope
    private val mContext: Context

    private val mBluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val mBluetoothAdapter: BluetoothAdapter = mBluetoothManager.adapter

    private var mGattInstancesMap = HashMap<String, GattInstance>()

    private val baseUUID: String = "-0000-1000-8000-00805f9b34fb"
    private val handlePadding: Int = 4
    private val uuidPadding: Int = 8

    private inner class GattInstance(val device: BluetoothDevice) {
        public var mGatt: BluetoothGatt? = null
        public var mTransport: Int = -1
        public val mDevice: BluetoothDevice

        private var mServiceDiscovered = MutableStateFlow(false)
        private var mConnectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)


        private var mBondState = BluetoothDevice.BOND_NONE


        init {
            mDevice = device
        }

        public fun isConnected(): Boolean {
            return mConnectionState.value == BluetoothProfile.STATE_CONNECTED
        }

        public fun isDisconnected(): Boolean {
            return mConnectionState.value == BluetoothProfile.STATE_DISCONNECTED
        }

        public fun isBonded(): Boolean {
            return mBondState == BluetoothDevice.BOND_BONDED
        }

        public fun isBLETransport(): Boolean {
            return mTransport == BluetoothDevice.TRANSPORT_LE
        }

        public fun servicesDiscovered(): Boolean {
            return mServiceDiscovered.value
        }

        public fun connectInstance(transport: Int): Boolean {
            mTransport = transport

            if (!isBLETransport() && !isBonded()) {
                Log.w(TAG, "Trying to connect non BLE gatt on a not bonded device")
                return false
            }
            if (!isDisconnected()) {
                Log.w(TAG, "Trying to connect gatt on an already connected device")
                return false
            }

            mGatt = mDevice.connectGatt(mContext, false, mGattCallback, mTransport)
            return mGatt != null
        }

        public fun waitForState(newState: Int) {
            if (mConnectionState.value != newState) {
                mConnectionState.first { it == newState }
            }
        }

        public fun waitForDiscoveryEnd() {
            if (mServiceDiscovered.value != true) {
                mServiceDiscovered.first { it == true }
            }
        }

        public fun disconnectInstance(): Boolean {
            if (mGatt == null || !isConnected()) {
                Log.w(TAG, "Trying to disconnect an already disconnected device")
                return false
            }
            mGatt?.disconnect()
            return true;
        }

        public fun setConnectionState(state: Int) {
            Log.i(TAG, "$mDevice connection state changed to $state")
            mConnectionState.value = state
            if (isDisconnected() && mGatt != null) {
                mGatt?.close()
                reset()
            }
        }

        public fun setBondState(state: Int) {
            Log.i(TAG, "$mDevice bond state changed to $state")
            mBondState = state
        }

        public fun setServicesDiscovered() {
            Log.i(TAG, "Services have been discovered for $mDevice")
            mServiceDiscovered.value = true
        }

        private fun reset() {
            mServiceDiscovered.value = false
            mTransport = -1
            mGatt = null
        }

        override fun toString(): String {
            return mDevice.getAddress()
        }
    }

    private fun String.toBaseUuid(): UUID =
        UUID.fromString(this.lowercase().padStart(uuidPadding, '0').plus(baseUUID))

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt,
                status: Int, newState: Int) {
            val deviceAddr = bluetoothGatt.getDevice()?.getAddress()
            val gattInstance: GattInstance? = mGattInstancesMap.get(deviceAddr)
            if (gattInstance == null) {
                Log.e(TAG,
                        "Received onConnectionStateChange but no corresponding instance in map")
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "$gattInstance onConnectionStateChange status: $status")
            }

            gattInstance.setConnectionState(newState)
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            val deviceAddr = bluetoothGatt.getDevice()?.getAddress()
            val gattInstance: GattInstance? = mGattInstancesMap.get(deviceAddr)
            if (gattInstance == null) {
                Log.e(TAG,
                        "Received onServicesDiscovered but no corresponding instance in map")
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "$gattInstance onServicesDiscovered status: $status")
            } else {
                gattInstance.setServicesDiscovered()
            }
        }
    }

    init {
        mScope = CoroutineScope(Dispatchers.Default)
        mContext = context
    }

    fun deinit() {
        mScope.cancel()
    }

    /**
     * Le connection doesn't need pairing, but PTS-bot wants the MMI to return only when the device
     * is connected.
     */
    override fun connectLe(request: BdAddr, responseObserver: StreamObserver<ConnectLeResult>) {
        grpcUnary<ConnectLeResult>(mScope, responseObserver) {
            val device = request.address.toBluetoothDevice(mBluetoothAdapter)
            val gattInstance = GattInstance(device)
            var result = true
            if (!gattInstance.connectInstance(BluetoothDevice.TRANSPORT_LE)) {
                // TODO throw instead of returning result
                Log.e(TAG, "Error connecting GATT over LE for $gattInstance")
                result = false
            }
            gattInstance.waitForState(BluetoothProfile.STATE_CONNECTED)

            ConnectLeResult.newBuilder().setSuccess(result).build()
        }
    }

    override fun disconnect(request: BdAddr, responseObserver: StreamObserver<DisconnectResult>) {
        grpcUnary<DisconnectResult>(mScope, responseObserver) {
            val gattInstance: GattInstance? = mGattInstancesMap.get(
                    request.address.decodeToString())
            var result = true
            if (gattInstance == null) {
                Log.e(TAG, "Trying to disconnect a device that is not in instances map")
                result = false
            } else if (!gattInstance.disconnectInstance()) {
                Log.e(TAG, "Error while disconnecting $gattInstance")
                result = false
            }

            DisconnectResult.newBuilder().setSuccess(result).build()
        }
    }

    override fun exchangeMTU(request: ExchangeMTURequest,
            responseObserver: StreamObserver<ExchangeMTUResult>) {
        grpcUnary<ExchangeMTUResult>(mScope, responseObserver) {
            val mtu = request.mtu
            val gattInstance: GattInstance? = mGattInstancesMap.get(
                    request.address.decodeToString())
            var result = true
            if (gattInstance == null) {
                result = false
                Log.e(TAG, "Trying to request MTU on a device that is not in the instances map")
            } else if (!gattInstance?.mGatt?.requestMtu(mtu)) {
                result = false
                Log.e(TAG, "Error on requesting MTU for $gattInstance")
            }

            ExchangeMTUResult.newBuilder().setSuccess(result).build()
        }
    }

    override fun discoverServices(request: BdAddr,
            responseObserver: StreamObserver<DiscoverServicesResult>) {
        grpcUnary<DiscoverServicesResult>(mScope, responseObserver) {
            val gattInstance: GattInstance? = mGattInstancesMap.get(
                    request.address.decodeToString())
            var result = true
            if (gattInstance == null) {
                result = false
                Log.e(TAG, "Trying to request MTU on a device that is not in the instances map")
            } else if (!gattInstance?.mGatt?.discoverServices()) {
                // TODO throw instead of returning false
                result = false
                Log.e(TAG, "Error on discovering services for $gattInstance")
            }
            gattInstance?.waitForDiscoveryEnd()

            DiscoverServicesResult.newBuilder().setSuccess(result).build()
        }
    }

    override fun writeCharacteristicFromHandle(request: WriteCharacteristicRequest,
            responseObserver: StreamObserver<WriteCharacteristicResult>) {
        grpcUnary<WriteCharacteristicResult>(mScope, responseObserver) {
            val gattInstance: GattInstance? = mGattInstancesMap.get(
                    request.address.decodeToString())
            var characteristicWritten = false
            if (gattInstance == null) {
                // TODO throw instead of returning false
                Log.e(TAG, "Trying to request MTU on a device that is not in the instances map")
            } else {
                val characteristic: BluetoothGattCharacteristic? =
                        getCharacteristicWithHandle(request.handle, gattInstance)
                if (characteristic != null) {
                    characteristic.setValue(request.value.toByteArray())
                    gattInstance.mGatt?.writeCharacteristic(characteristic)
                    characteristicWritten = true
                } else {
                    Log.e(TAG,
                            "Error while writing characteristic for $gattInstance")
                }
            }
            WriteCharacteristicResult.newBuilder().setSuccess(characteristicWritten).build()
        }
    }

    private fun getCharacteristicWithHandle(handle: Int,
            gattInstance: GattInstance): BluetoothGattCharacteristic? {
        if (!gattInstance?.servicesDiscovered() && !gattInstance?.mGatt?.discoverServices()) {
            // TODO throw instead of returning null
            Log.e(TAG, "Error on discovering services for $gattInstance")
        } else {
            gattInstance.waitForDiscoveryEnd()
        }

        for (service: BluetoothGattService in gattInstance.mGatt?.services.orEmpty()) {
            for (characteristic : BluetoothGattCharacteristic in service.characteristics) {
                if (characteristic.instanceId == handle) {
                    return characteristic
                }
            }
        }
        return null
    }
}