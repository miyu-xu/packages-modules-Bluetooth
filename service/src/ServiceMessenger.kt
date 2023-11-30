/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.server.bluetooth

import android.bluetooth.IBluetoothManagerCallback
import android.content.AttributionSource
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import java.util.Objects.requireNonNull

private const val TAG = "ServiceMessenger"

internal class ServiceMessenger(
    private val mBluetoothManagerService: BluetoothManagerService,
    looper: Looper
) : Handler(looper) {
    val messenger = Messenger(this)

    override fun handleMessage(msg: Message) {
        Log.d(TAG, "handleMessage: ${msg}")
        val reply = Message.obtain()
        try {
            reply.setData(handleMessage(mBluetoothManagerService, msg))
        } catch (e: RuntimeException) {
            reply.setData(Bundle().apply { putSerializable("exception", e) })
        } finally {
            try {
                msg.replyTo?.send(reply)
            } catch (e: RemoteException) {
                Log.e(TAG, "registerAdapter: Failed to send reply=${reply}", e)
            }
        }
    }
}

private fun handleMessage(bms: BluetoothManagerService, msg: Message): Bundle {
    val what = BluetoothServiceMessages.`$`.toString(msg.what)
    return when (msg.what) {
        BluetoothServiceMessages.REGISTER_ADAPTER -> {
            val callback = IBluetoothManagerCallback.Stub.asInterface(msg.data.getBinder("callback")!!)

            val adapterBinder = bms.registerAdapter_sync(callback)
            Bundle().apply { putBinder("service", adapterBinder?.asBinder()) }
        }
        BluetoothServiceMessages.UNREGISTER_ADAPTER -> {
            val callback = IBluetoothManagerCallback.Stub.asInterface(msg.data.getBinder("callback")!!)

            bms.unregisterAdapter_sync(callback)
            Bundle.EMPTY
        }
        BluetoothServiceMessages.ENABLE -> {
            val source= msg.data.getParcelable("source", AttributionSource::class.java)!!
            if (!enableAllowed(source)) {
                Bundle().apply { putBoolean("enable", false) }
            }  else {
                Bundle().apply { putBoolean("enable", bms.enable_sync(source.getPackageName())) }
            }
        }
        else -> throw IllegalArgumentException("command not implemented: ${msg.what}")
    }
}

private fun enableAllowed(_source: AttributionSource) : Boolean {
    return true;
}
