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

package com.android.blueberry

import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.content.Context
import androidx.test.runner.MonitoringInstrumentation
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import androidx.test.core.app.ApplicationProvider.getApplicationContext

class Server : MonitoringInstrumentation() {

  private val TAG = "BlueberryServer"
  private val GRPC_PORT = 8999

  private lateinit var host: Host

  override fun onCreate(arguments: Bundle) {
    super.onCreate(arguments)

    // Activate debugger
    if (arguments.getString("debug").toBoolean()) {
      Log.i(TAG, "Waiting for debugger to connect...")
      Debug.waitForDebugger()
      Log.i(TAG, "Debugger connected")
    }

    // Start instrumentation thread
    start()
  }

  override fun onStart() {
    super.onStart()

    val context: Context = getApplicationContext()
    host = Host(context)

    Log.d(TAG, "Starting Blueberry Server")
    NettyServerBuilder
      .forPort(GRPC_PORT)
      .addService(host)
      .build()
      .start()
    Log.d(TAG, "Blueberry Server started at $GRPC_PORT")
  }
}
