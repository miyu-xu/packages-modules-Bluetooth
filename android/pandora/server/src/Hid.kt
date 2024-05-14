/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.pandora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHidHost
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import io.grpc.stub.StreamObserver
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import pandora.HIDGrpc.HIDImplBase
import pandora.HidProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Hid(val context: Context) : HIDImplBase(), Closeable {
    private val TAG = "PandoraHid"

    private val scope: CoroutineScope
    private val flow: Flow<Intent>

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothHidHost =
        getProfileProxy<BluetoothHidHost>(context, BluetoothProfile.HID_HOST)

    init {
        scope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED)

        flow = intentFlow(context, intentFilter, scope).shareIn(scope, SharingStarted.Eagerly)
    }

    override fun close() {
        // Deinit the CoroutineScope
        scope.cancel()
    }

    override fun connectHost(
        request: ConnectHostRequest,
        responseObserver: StreamObserver<ConnectHostResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            bluetoothHidHost.connect(request.address.toBluetoothDevice(bluetoothAdapter))
            val device = request.address.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "wait for connection to complete : device=$device")
            val connectionState =
                flow
                    .filter { it.getAction() == BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
                    .filter {
                        it == BluetoothProfile.STATE_CONNECTED ||
                            it == BluetoothProfile.STATE_DISCONNECTED
                    }
                    .first()
            val state =
                if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                    ConnectionState.HID_HOST_CONNECTED
                } else {
                    ConnectionState.HID_HOST_DISCONNECTED
                }
            ConnectHostResponse.newBuilder().setState(state).build()
        }
    }

    override fun disconnectHost(
        request: DisconnectHostRequest,
        responseObserver: StreamObserver<DisconnectHostResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            bluetoothHidHost.disconnect(request.address.toBluetoothDevice(bluetoothAdapter))
            val device = request.address.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "wait for disconnection to complete : device=$device")
            val connectionState =
                flow
                    .filter { it.getAction() == BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
                    .filter {
                        it == BluetoothProfile.STATE_CONNECTED ||
                            it == BluetoothProfile.STATE_DISCONNECTED
                    }
                    .first()
            val state =
                if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                    ConnectionState.HID_HOST_CONNECTED
                } else {
                    ConnectionState.HID_HOST_DISCONNECTED
                }
            DisconnectHostResponse.newBuilder().setState(state).build()
        }
    }

    override fun getHostConnectionState(
        request: GetHostConnectionStateRequest,
        responseObserver: StreamObserver<GetHostConnectionStateResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            val connectionState =
                bluetoothHidHost.getConnectionState(
                    request.address.toBluetoothDevice(bluetoothAdapter)
                )
            val state =
                when (connectionState) {
                    BluetoothProfile.STATE_DISCONNECTED -> ConnectionState.HID_HOST_DISCONNECTED
                    BluetoothProfile.STATE_CONNECTING -> ConnectionState.HID_HOST_CONNECTING
                    BluetoothProfile.STATE_CONNECTED -> ConnectionState.HID_HOST_CONNECTED
                    else -> ConnectionState.HID_HOST_DISCONNECTING
                }
            GetHostConnectionStateResponse.newBuilder().setState(state).build()
        }
    }

    override fun sendHostReport(
        request: SendHostReportRequest,
        responseObserver: StreamObserver<SendHostReportResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            bluetoothHidHost.setReport(
                request.address.toBluetoothDevice(bluetoothAdapter),
                request.reportType.number.toByte(),
                request.report
            )
            SendHostReportResponse.getDefaultInstance()
        }
    }
}
