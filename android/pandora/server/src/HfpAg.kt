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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.CallLog
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
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
class HfpAg(val context: Context) : HFPImplBase(), Closeable {
    private val TAG = "PandoraHfpAg"

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
    private val flow: Flow<Intent>

    private val telecomManager = context.getSystemService(TelecomManager::class.java)!!

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothHfp = getProfileProxy<BluetoothHeadset>(context, BluetoothProfile.HEADSET)

    companion object {
        @SuppressLint("StaticFieldLeak") private lateinit var inCallService: InCallService
    }

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
        flow = intentFlow(context, intentFilter, scope).shareIn(scope, SharingStarted.Eagerly)

        telecomManager.endCall()

        shell("su root setprop persist.bluetooth.disableinbandringing false")
    }

    override fun close() {
        telecomManager.endCall()
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHfp)
        scope.cancel()
    }

    class PandoraInCallService : InCallService() {
        override fun onBind(intent: Intent?): IBinder? {
            inCallService = this
            return super.onBind(intent)
        }
    }

    // Helper to get the device for an optional service level connection.
    fun getBluetoothDevice(slc: ServiceLevelConnection?): BluetoothDevice? {
        return if (slc != null)
            bluetoothAdapter.getRemoteDevice(slc.audiogateway.cookie.toString("UTF-8"))
        else null
    }

    override fun openAudioGateway(
        request: OpenAudioGatewayRequest,
        responseObserver: StreamObserver<OpenAudioGatewayResponse>
    ) {
        grpcUnary<OpenAudioGatewayResponse>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "openAudioGateway: device=$device")

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadset.STATE_CONNECTED) {
                bluetoothHfp.connect(device)
                val state =
                    flow
                        .filter {
                            it.getAction() == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
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
                    throw RuntimeException("openAudioGateway failed, HFP has been disconnected")
                }
            }

            val audiogateway =
                AudioGateway.newBuilder()
                    .setCookie(ByteString.copyFrom(device.getAddress(), "UTF-8"))
            OpenAudioGatewayResponse.newBuilder().setAudiogateway(audiogateway).build()
        }
    }

    override fun close(request: CloseRequest, responseObserver: StreamObserver<CloseResponse>) {
        grpcUnary<CloseResponse>(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "close: device=$device")

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadset.STATE_CONNECTED) {
                throw RuntimeException("Device is not connected, cannot close")
            }

            val hfpConnectionStateChangedFlow =
                flow
                    .filter { it.getAction() == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothAdapter.ERROR) }

            bluetoothHfp.disconnect(device)
            hfpConnectionStateChangedFlow
                .filter { it == BluetoothHeadset.STATE_DISCONNECTED }
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
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "openAudio: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadset.STATE_CONNECTED) {
                return@grpcUnary OpenAudioResponse.newBuilder()
                    .setDisconnected(Empty.getDefaultInstance())
                    .build()
            }

            if (bluetoothHfp.getAudioState(device) == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                return@grpcUnary OpenAudioResponse.newBuilder()
                    .setAlreadyOpened(Empty.getDefaultInstance())
                    .build()
            }

            val hfpAudioStateChangedFlow =
                flow
                    .filter { it.getAction() == BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothAdapter.ERROR) }

            bluetoothHfp.connectAudio()
            hfpAudioStateChangedFlow.filter { it == BluetoothHeadset.STATE_AUDIO_CONNECTED }.first()

            OpenAudioResponse.newBuilder().setOpened(Empty.getDefaultInstance()).build()
        }
    }

    override fun closeAudio(
        request: CloseAudioRequest,
        responseObserver: StreamObserver<CloseAudioResponse>
    ) {
        grpcUnary<CloseAudioResponse>(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "closeAudio: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

            if (bluetoothHfp.getConnectionState(device) != BluetoothHeadset.STATE_CONNECTED) {
                return@grpcUnary CloseAudioResponse.newBuilder()
                    .setDisconnected(Empty.getDefaultInstance())
                    .build()
            }

            if (bluetoothHfp.getAudioState(device) == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                return@grpcUnary CloseAudioResponse.newBuilder()
                    .setAlreadyClosed(Empty.getDefaultInstance())
                    .build()
            }

            val hfpAudioStateChangedFlow =
                flow
                    .filter { it.getAction() == BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED }
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothAdapter.ERROR) }

            bluetoothHfp.disconnectAudio()
            hfpAudioStateChangedFlow
                .filter { it == BluetoothHeadset.STATE_AUDIO_DISCONNECTED }
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
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "isAudioOpen: device=$device")

            BoolValue.newBuilder().setValue(bluetoothHfp.isAudioConnected(device)).build()
        }
    }

    override fun placeCall(
        request: PlaceCallRequest,
        responseObserver: StreamObserver<PlaceCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            Log.i(TAG, "placeCall")

            when (request.getDialCase()) {
                PlaceCallRequest.DialCase.NUMBER -> {
                    telecomManager.placeCall(Uri.fromParts("tel", request.number, null), Bundle())
                }
                PlaceCallRequest.DialCase.MEMORY -> {
                    telecomManager.placeCall(
                        Uri.fromParts("tel", ">" + request.memory, null),
                        Bundle()
                    )
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
            Log.i(TAG, "answerCall")

            telecomManager.acceptRingingCall()
            AnswerCallResponse.getDefaultInstance()
        }
    }

    override fun terminateCall(
        request: TerminateCallRequest,
        responseObserver: StreamObserver<TerminateCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device = getBluetoothDevice(request.slc)
            Log.i(TAG, "terminateCall: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

            telecomManager.endCall()
            TerminateCallResponse.getDefaultInstance()
        }
    }

    override fun rejectCall(
        request: RejectCallRequest,
        responseObserver: StreamObserver<RejectCallResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device = getBluetoothDevice(request.slc)
            Log.i(TAG, "rejectCall: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

            telecomManager.endCall()
            RejectCallResponse.getDefaultInstance()
        }
    }

    override fun swapCall(
        request: SwapCallRequest,
        responseObserver: StreamObserver<SwapCallResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            val device = getBluetoothDevice(request.slc)
            Log.i(TAG, "swapCall: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

            val callsToActivate = mutableListOf<Call>()
            for (call in inCallService.calls) {
                if (call.details.state == Call.STATE_ACTIVE) {
                    call.hold()
                } else {
                    callsToActivate.add(call)
                }
            }
            for (call in callsToActivate) {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
            }
            inCallService.calls[0].hold()
            inCallService.calls[1].unhold()
            SwapCallResponse.getDefaultInstance()
        }
    }

    override fun activateVoiceRecognition(
        request: ActivateVoiceRecognitionRequest,
        responseObserver: StreamObserver<ActivateVoiceRecognitionResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            val device =
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "activateVoiceRecognition: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

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
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "deactivateVoiceRecognition: device=$device")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

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
                bluetoothAdapter.getRemoteDevice(request.slc.audiogateway.cookie.toString("UTF-8"))
            Log.i(TAG, "setBatteryLevel: device=$device percentage=${request.batteryPercentage}")

            if (bluetoothHfp.getActiveDevice() != device) {
                throw RuntimeException("Device is not the active device.")
            }

            val action = "android.intent.action.BATTERY_CHANGED"
            shell("am broadcast -a $action --ei level ${request.batteryPercentage} --ei scale 100")

            SetBatteryLevelResponse.getDefaultInstance()
        }
    }

    override fun clearCallHistory(
        request: ClearCallHistoryRequest,
        responseObserver: StreamObserver<ClearCallHistoryResponse>
    ) {
        grpcUnary(scope, responseObserver) {
            context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
            ClearCallHistoryResponse.getDefaultInstance()
        }
    }

    override fun setInBandRingtone(
        request: SetInBandRingtoneRequest,
        responseObserver: StreamObserver<SetInBandRingtoneResponse>,
    ) {
        grpcUnary(scope, responseObserver) {
            shell(
                "su root setprop persist.bluetooth.disableinbandringing " +
                    (!request.enabled).toString()
            )
            SetInBandRingtoneResponse.getDefaultInstance()
        }
    }
}
