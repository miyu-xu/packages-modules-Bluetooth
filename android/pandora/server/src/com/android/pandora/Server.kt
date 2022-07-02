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

import android.content.Context
import android.util.Log
import io.grpc.Server as GrpcServer
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Server(context: Context) {

  private val TAG = "PandoraServer"
  private val GRPC_PORT = 8999

  private var host: Host
  private var a2dp: A2dp
  private var hfp: Hfp
  private var grpcServer: GrpcServer

  // fun <T> CreateProfileServiceOrNull(context: Context, profile: Class<*>) : T? {
  //   val actualRuntimeClassName : String = T::class.qualifiedName!!
  //   return try { profile(context, this) }
  //   catch (err: RuntimeException) { Log.e(TAG, "Failed to create " + T); null }
  // }

  init {
    // try { host = Host(context, this) }
    // catch (err: RuntimeException) { Log.e(TAG, "Failed to create Host") }

    // try { a2dp = A2dp(context) }
    // catch (err: RuntimeException) { Log.e(TAG, "Failed to create A2dp") }

    // try { hfp = Hfp(context) }
    // catch (err: RuntimeException) { Log.e(TAG, "Failed to create Hfp") }

    // var builder = NettyServerBuilder.forPort(GRPC_PORT)

    // host?.let { builder.addService(host) }
    // a2dp?.let { builder.addService(a2dp) }
    // hfp?.let { builder.addService(hfp) }

    // grpcServer = builder.build()
    host = Host(context, this)
    a2dp = A2dp(context)
    hfp = Hfp(context)
    grpcServer = NettyServerBuilder.forPort(GRPC_PORT)
          .addService(a2dp)
          .addService(host)
          .addService(hfp)
          .build()

    Log.d(TAG, "Starting Pandora Server")
    grpcServer.start()
    Log.d(TAG, "Pandora Server started at $GRPC_PORT")
  }

  fun shutdownNow() {
    host.deinit()
    a2dp.deinit()
    hfp.deinit()
    grpcServer.shutdownNow()
  }

  fun awaitTermination() = grpcServer.awaitTermination()
}
