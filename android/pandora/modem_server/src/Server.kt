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
import android.util.Log
import io.grpc.Server as GrpcServer
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Server(context: Context) {

    private val TAG = "PandoraModemServer"
    private val GRPC_PORT = 8900

    private var grpcServer: GrpcServer
    private var service: Modem

    init {
        service = Modem(context)
        val grpcServerBuilder = NettyServerBuilder.forPort(GRPC_PORT)
        grpcServerBuilder.addService(service)
        grpcServer = grpcServerBuilder.build()

        Log.d(TAG, "Starting modem server.")
        grpcServer.start()
        Log.d(TAG, "Modem server started at $GRPC_PORT.")
    }

    fun shutdown() = grpcServer.shutdown()

    fun awaitTermination() = grpcServer.awaitTermination()

    fun deinit() = service.close()
}
