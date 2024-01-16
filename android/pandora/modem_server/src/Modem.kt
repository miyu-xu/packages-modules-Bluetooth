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

package com.android.pandora.modem

import android.content.Context
import android.system.Os
import android.system.OsConstants.*
import android.system.VmSocketAddress
import android.util.Log
import com.android.pandora.grpcUnary
import io.grpc.stub.StreamObserver
import java.io.Closeable
import java.io.FileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.modem.ModemGrpc.ModemImplBase
import pandora.modem.ModemProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Modem(val context: Context) : ModemImplBase(), Closeable {
    private val TAG = "PandoraModem"

    private val scope: CoroutineScope

    private val socket: FileDescriptor
    private val activeCalls: MutableList<String>

    private val MODEM_SIMULATOR_VSOCK_CID = 2
    private val MODEM_SIMULATOR_VSOCK_PORT = 9600

    init {
        val serverAddr = VmSocketAddress(MODEM_SIMULATOR_VSOCK_PORT, MODEM_SIMULATOR_VSOCK_CID)
        scope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
        socket = Os.socket(AF_VSOCK, SOCK_STREAM, 0)
        activeCalls = arrayListOf<String>()

        Os.connect(socket, serverAddr)
    }

    fun closeAll() {
        for (phoneNumber in activeCalls) {
            val closeRequest = "REM0\r\nAT+REMOTECALL=6,0,0,\"" + phoneNumber + "\",0\r\n"
            val closeRequestBuf = closeRequest.toByteArray()
            Os.write(socket, closeRequestBuf, 0, closeRequestBuf.size)
        }
        Log.i(TAG, "close socket!")
        Os.close(socket)
    }

    override fun close() {
        closeAll()
        scope.cancel()
    }

    override fun close(request: CloseRequest, responseObserver: StreamObserver<CloseResponse>) {
        grpcUnary(scope, responseObserver) {
            closeAll()
            CloseResponse.getDefaultInstance()
        }
    }

    override fun call(request: CallRequest, responseObserver: StreamObserver<CallResponse>) {
        grpcUnary(scope, responseObserver) {
            activeCalls.add(request.phoneNumber)
            val callRequest = "REM0\r\nAT+REMOTECALL=4,0,0,\"" + request.phoneNumber + "\",129\r\n"
            val callRequestBuf = callRequest.toByteArray()
            val sent = Os.write(socket, callRequestBuf, 0, callRequestBuf.size)
            Log.i(TAG, "call: sent " + sent + " bytes")

            CallResponse.getDefaultInstance()
        }
    }

    override fun answerCall(
        request: AnswerCallRequest,
        responseObserver: StreamObserver<AnswerCallResponse>
    ) {

        grpcUnary(scope, responseObserver) {
            val action = request.action
            Log.i(TAG, "answerCall: action=$action")

            when (action) {
                AnswerCallAction.ANSWER -> {
                    activeCalls.add(request.phoneNumber)

                    val answerCallRequest =
                        "REM0\r\nAT+REMOTECALL=0,0,0,\"" + request.phoneNumber + "\",129\r\n"
                    val answerCallRequestBuf = answerCallRequest.toByteArray()
                    val sent = Os.write(socket, answerCallRequestBuf, 0, answerCallRequestBuf.size)
                    Log.i(TAG, "answerCall: sent " + sent + " bytes")
                }
                else -> {
                    throw RuntimeException("Action is not supported")
                }
            }

            AnswerCallResponse.getDefaultInstance()
        }
    }
}
