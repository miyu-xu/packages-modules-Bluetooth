/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadsetClient
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telecom.InCallService
import android.util.Log
import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
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
import pandora.hfp.HFPGrpc.HFPImplBase
import pandora.hfp.HfpProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HfpHf(val context: Context) : HFPImplBase(), Closeable {
    private val TAG = "PandoraHfpHf"

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
    private val flow: Flow<Intent>

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothHfp =
        getProfileProxy<BluetoothHeadsetClient>(context, BluetoothProfile.HEADSET_CLIENT)

    companion object {
        @SuppressLint("StaticFieldLeak") private lateinit var inCallService: InCallService
    }

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED)
        flow = intentFlow(context, intentFilter, scope).shareIn(scope, SharingStarted.Eagerly)
    }

    override fun close() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, bluetoothHfp)
        scope.cancel()
    }

    override fun openHandsFree(
        request: OpenHandsFreeRequest,
        responseObserver: StreamObserver<OpenHandsFreeResponse>
    ) {
        grpcUnary<OpenHandsFreeResponse>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "openHandsFree: device=$device")

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadsetClient.STATE_CONNECTED) {
                bluetoothHfp.connect(device)
                val state =
                    flow
                        .filter {
                            it.getAction() == BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED
                        }
                        .filter { it.getBluetoothDeviceExtra() == device }
                        .map {
                            it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
                        }
                        .filter {
                            it == BluetoothProfile.STATE_CONNECTED ||
                                it == BluetoothProfile.STATE_DISCONNECTED
                        }
                        .first()

                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    throw RuntimeException("openHandsFree failed, HFP has been disconnected")
                }
            }

            val handsfree =
                HandsFree.newBuilder().setCookie(ByteString.copyFrom(device.getAddress(), "UTF-8"))
            OpenHandsFreeResponse.newBuilder().setHandsfree(handsfree).build()
        }
    }

    override fun close(request: CloseRequest, responseObserver: StreamObserver<CloseResponse>) {
        grpcUnary<CloseResponse>(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "close: device=$device")

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadsetClient.STATE_CONNECTED) {
                throw RuntimeException("Device is not connected, cannot close")
            }

            val hfpConnectionStateChangedFlow =
                flow
                    .filter {
                        it.getAction() == BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED
                    }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map {
                        it.getIntExtra(BluetoothHeadsetClient.EXTRA_STATE, BluetoothAdapter.ERROR)
                    }

            bluetoothHfp.disconnect(device)
            hfpConnectionStateChangedFlow
                .filter { it == BluetoothHeadsetClient.STATE_DISCONNECTED }
                .first()

            CloseResponse.getDefaultInstance()
        }
    }

    override fun openAudio(
        request: OpenAudioRequest,
        responseObserver: StreamObserver<OpenAudioResponse>
    ) {
        grpcUnary<OpenAudioResponse>(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "openAudio: device=$device")

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadsetClient.STATE_CONNECTED) {
                return@grpcUnary OpenAudioResponse.newBuilder()
                    .setDisconnected(Empty.getDefaultInstance())
                    .build()
            }

            if (
                bluetoothHfp.getAudioState(device) == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED
            ) {
                return@grpcUnary OpenAudioResponse.newBuilder()
                    .setAlreadyOpened(Empty.getDefaultInstance())
                    .build()
            }

            val hfpAudioStateChangedFlow =
                flow
                    .filter { it.getAction() == BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map {
                        it.getIntExtra(BluetoothHeadsetClient.EXTRA_STATE, BluetoothAdapter.ERROR)
                    }

            bluetoothHfp.connectAudio(device)
            hfpAudioStateChangedFlow
                .filter { it == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED }
                .first()

            OpenAudioResponse.newBuilder().setOpened(Empty.getDefaultInstance()).build()
        }
    }

    override fun closeAudio(
        request: CloseAudioRequest,
        responseObserver: StreamObserver<CloseAudioResponse>
    ) {
        grpcUnary<CloseAudioResponse>(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "closeAudio: device=$device")

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadsetClient.STATE_CONNECTED) {
                return@grpcUnary CloseAudioResponse.newBuilder()
                    .setDisconnected(Empty.getDefaultInstance())
                    .build()
            }

            if (
                bluetoothHfp.getAudioState(device) ==
                    BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED
            ) {
                return@grpcUnary CloseAudioResponse.newBuilder()
                    .setAlreadyClosed(Empty.getDefaultInstance())
                    .build()
            }

            val hfpAudioStateChangedFlow =
                flow
                    .filter { it.getAction() == BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map {
                        it.getIntExtra(BluetoothHeadsetClient.EXTRA_STATE, BluetoothAdapter.ERROR)
                    }

            bluetoothHfp.disconnectAudio(device)
            hfpAudioStateChangedFlow
                .filter { it == BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED }
                .first()

            CloseAudioResponse.newBuilder().setClosed(Empty.getDefaultInstance()).build()
        }
    }

    override fun isAudioOpen(
        request: IsAudioOpenRequest,
        responseObserver: StreamObserver<BoolValue>
    ) {
        grpcUnary<BoolValue>(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "isAudioOpen: device=$device")

            BoolValue.newBuilder()
                .setValue(
                    bluetoothHfp.getAudioState(device) ==
                        BluetoothHeadsetClient.STATE_AUDIO_CONNECTED
                )
                .build()
        }
    }

    override fun placeCall(
        request: PlaceCallRequest,
        responseObserver: StreamObserver<PlaceCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "placeCall: device=$device")

            when (request.getDialCase()) {
                PlaceCallRequest.DialCase.NUMBER -> {
                    bluetoothHfp.dial(device, request.number)
                }
                PlaceCallRequest.DialCase.MEMORY -> {
                    bluetoothHfp.dial(device, ">${request.memory}")
                }
                else -> {
                    throw RuntimeException("unsupported dial mode")
                }
            }

            PlaceCallResponse.getDefaultInstance()
        }
    }

    override fun answerCall(
        request: AnswerCallRequest,
        responseObserver: StreamObserver<AnswerCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "answerCall: device=$device")

            bluetoothHfp.acceptCall(device, BluetoothHeadsetClient.CALL_ACCEPT_NONE)
            AnswerCallResponse.getDefaultInstance()
        }
    }

    override fun terminateCall(
        request: TerminateCallRequest,
        responseObserver: StreamObserver<TerminateCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "terminateCall: device=$device")

            bluetoothHfp.terminateCall(device, null)
            TerminateCallResponse.getDefaultInstance()
        }
    }

    override fun rejectCall(
        request: RejectCallRequest,
        responseObserver: StreamObserver<RejectCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "rejectCall: device=$device")

            bluetoothHfp.rejectCall(device)
            RejectCallResponse.getDefaultInstance()
        }
    }

    override fun activateVoiceRecognition(
        request: ActivateVoiceRecognitionRequest,
        responseObserver: StreamObserver<ActivateVoiceRecognitionResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "activateVoiceRecognition: device=$device")

            bluetoothHfp.startVoiceRecognition(device)
            ActivateVoiceRecognitionResponse.getDefaultInstance()
        }
    }

    override fun deactivateVoiceRecognition(
        request: DeactivateVoiceRecognitionRequest,
        responseObserver: StreamObserver<DeactivateVoiceRecognitionResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "deactivateVoiceRecognition: device=$device")

            bluetoothHfp.stopVoiceRecognition(device)
            DeactivateVoiceRecognitionResponse.getDefaultInstance()
        }
    }

    override fun setBatteryLevel(
        request: SetBatteryLevelRequest,
        responseObserver: StreamObserver<SetBatteryLevelResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "setBatteryLevel: device=$device percentage=${request.batteryPercentage}")

            val action = "android.intent.action.BATTERY_CHANGED"
            shell("am broadcast -a $action --ei level ${request.batteryPercentage} --ei scale 100")

            SetBatteryLevelResponse.getDefaultInstance()
        }
    }

    override fun sendDtmf(
        request: SendDtmfRequest,
        responseObserver: StreamObserver<SendDtmfResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.handsfree.cookie.toString("UTF-8"))
            Log.i(TAG, "sendDtmf: device=$device code=${request.code}")

            bluetoothHfp.sendDTMF(device, request.code.toByte())
            SendDtmfResponse.getDefaultInstance()
        }
    }

    override fun callTransferAsHandsFree(
        request: CallTransferAsHandsFreeRequest,
        responseObserver: StreamObserver<CallTransferAsHandsFreeResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.handsfree.cookie.toString("UTF-8"))
            bluetoothHfp.explicitCallTransfer(device)
            CallTransferAsHandsFreeResponse.getDefaultInstance()
        }
    }
}
