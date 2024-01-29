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
package com.android.server.bluetooth

import android.bluetooth.IBluetoothManagerCallback
import android.content.AttributionSource
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException

private const val TAG = "ServiceMessenger"

internal class ServiceMessenger(
    private val managerService: BluetoothManagerService,
    private val checker: PermissionChecker,
    looper: Looper
) : Handler(looper) {
    val messenger = Messenger(this)

    override fun handleMessage(msg: Message) {
        Log.d(TAG, "handleMessage: ${msg}")
        val reply = Message.obtain()
        try {
            reply.setData(handleMessage(msg.sendingUid, msg.what, msg.data))
        } catch (e: RuntimeException) {
            reply.setData(Bundle().apply { putSerializable("exception", e) })
        } finally {
            try {
                msg.replyTo?.send(reply)
            } catch (e: RemoteException) {
                Log.e(TAG, "handleMessage($msg): Failed to send reply=${reply}", e)
            }
        }
    }

    private fun handleMessage(sendingUid: Int, what: Int, data: Bundle): Bundle {
        return when (what) {
            BluetoothServiceMessages.REGISTER_ADAPTER -> {
                val callback =
                    IBluetoothManagerCallback.Stub.asInterface(data.getBinder("callback")!!)

                val adapterBinder = managerService.registerAdapter_sync(callback)
                Bundle().apply { putBinder("service", adapterBinder?.asBinder()) }
            }
            BluetoothServiceMessages.UNREGISTER_ADAPTER -> {
                val callback =
                    IBluetoothManagerCallback.Stub.asInterface(data.getBinder("callback")!!)

                managerService.unregisterAdapter_sync(callback)
                Bundle.EMPTY
            }
            BluetoothServiceMessages.ENABLE -> {
                val source = data.getParcelable("source", AttributionSource::class.java)!!
                val quiet = data.getBoolean("quiet")
                val foregroundRequired = quiet == false
                val enable =
                    try {
                        checker.enableAllowed(sendingUid, source, foregroundRequired)
                        managerService.enable_sync(source.getPackageName(), quiet)
                    } catch (e: PermissionChecker.BluetoothPermissionException) {
                        Log.e(TAG, "enable(): FAILED", e)
                        false
                    }
                Bundle().apply { putBoolean("enable", enable) }
            }
            BluetoothServiceMessages.DISABLE -> {
                val source = data.getParcelable("source", AttributionSource::class.java)!!
                val persist = data.getBoolean("persist")
                val disable =
                    try {
                        checker.disableAllowed(sendingUid, source)
                        managerService.disable_sync(source.getPackageName(), persist)
                    } catch (e: PermissionChecker.BluetoothPermissionException) {
                        Log.e(TAG, "disable(): FAILED", e)
                        false
                    }
                Bundle().apply { putBoolean("disable", disable) }
            }
            else -> throw IllegalArgumentException("command not implemented: ${what} - ${data}")
        }
    }
}
