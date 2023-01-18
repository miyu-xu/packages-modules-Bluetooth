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

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import io.grpc.BindableService
import io.grpc.Server as GrpcServer
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import java.io.Closeable

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Server(context: Context) {

  private val TAG = "PandoraServer"
  private val GRPC_PORT = 8999

  private var host: Host
  private var a2dp: A2dp? = null
  private var a2dpSink: A2dpSink? = null
  private var avrcp: Avrcp
  private var gatt: Gatt
  private var hfp: Hfp? = null
  private var hfpHandsfree: HfpHandsfree? = null
  private var hid: Hid
  private var l2cap: L2cap
  private var mediaplayer: MediaPlayer
  private var pan: Pan
  private var pbap: Pbap
  private var rfcomm: Rfcomm
  private var security: Security
  private var securityStorage: SecurityStorage
  private var androidInternal: AndroidInternal
  private var grpcServer: GrpcServer
  private var services: List<BindableService>

  init {
    security = Security(context)
    host = Host(context, security, this)
    avrcp = Avrcp(context)
    gatt = Gatt(context)
    hid = Hid(context)
    l2cap = L2cap(context)
    mediaplayer = MediaPlayer(context)
    pan = Pan(context)
    pbap = Pbap(context)
    rfcomm = Rfcomm(context)
    securityStorage = SecurityStorage(context)
    androidInternal = AndroidInternal(context)

    val grpcServerBuilder =
      NettyServerBuilder.forPort(GRPC_PORT)
        .addService(host)
        .addService(avrcp)
        .addService(gatt)
        .addService(hid)
        .addService(l2cap)
        .addService(mediaplayer)
        .addService(pan)
        .addService(pbap)
        .addService(rfcomm)
        .addService(security)
        .addService(securityStorage)
        .addService(androidInternal)

    val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java)!!.adapter

    val security = Security(context)
    services =
      listOf(
        security,
        Host(context, security, this),
        L2cap(context),
        MediaPlayer(context),
        Rfcomm(context),
        SecurityStorage(context),
        AndroidInternal(context),
      ) +
        mapOf(
            BluetoothProfile.A2DP to ::A2dp,
            BluetoothProfile.A2DP_SINK to ::A2dpSink,
            BluetoothProfile.AVRCP to ::Avrcp,
            BluetoothProfile.GATT to ::Gatt,
            BluetoothProfile.HEADSET to ::Hfp,
            BluetoothProfile.HEADSET_CLIENT to ::HfpHandsfree,
            BluetoothProfile.HID_HOST to ::Hid,
            BluetoothProfile.PBAP to ::Pbap,
          )
          .filter { bluetoothAdapter.getSupportedProfiles().contains(it.key) == true }
          .map { it.value(context) }

    val grpcServerBuilder = NettyServerBuilder.forPort(GRPC_PORT)

    services.forEach { grpcServerBuilder.addService(it) }

    grpcServer = grpcServerBuilder.build()

    Log.d(TAG, "Starting Pandora Server")
    grpcServer.start()
    Log.d(TAG, "Pandora Server started at $GRPC_PORT")
  }

  fun shutdown() = grpcServer.shutdown()

  fun awaitTermination() = grpcServer.awaitTermination()

  fun deinit() = services.forEach { if (it is Closeable) it.close() }
}
