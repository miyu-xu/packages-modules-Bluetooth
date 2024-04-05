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

import android.bluetooth.BluetoothAdapter
import android.content.AttributionSource
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.sysprop.BluetoothProperties

private const val TAG = "ServiceMessenger"

internal class ServiceMessenger(
    private val managerService: BluetoothManagerService,
    private val checker: PermissionChecker,
    looper: Looper
) : Handler(looper) {
    val messenger = Messenger(this)

    override fun handleMessage(msg: Message) {
        Log.i(TAG, "handleMessage: ${msg}")
        val reply = Message.obtain()
        try {
            reply.obj = handleMessage(msg.sendingUid, msg.obj)
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

    private fun handleMessage(sendingUid: Int, obj: Any?): Any? {
        return when (obj) {
            is BluetoothServiceMessages.RegisterAdapter -> {
                BluetoothServiceMessages.BluetoothBinder().apply {
                    binder = managerService.registerAdapter_sync(obj.binder)
                    Log.e(TAG, "returning ${binder}")
                }
            }
            is BluetoothServiceMessages.UnregisterAdapter -> {
                managerService.unregisterAdapter_sync(obj.binder)
            }
            is BluetoothServiceMessages.Enable -> {
                val source = obj.attributionSource
                val isQuiet = obj.isQuiet
                val bleToken = obj.bleToken

                val foregroundRequired = isQuiet == false || bleToken == null
                try {
                    checker.enableAllowed(sendingUid, source, foregroundRequired)
                } catch (e: PermissionChecker.BluetoothPermissionException) {
                    Log.e(TAG, "${obj}: FAILED", e)
                    return false
                }

                if (bleToken != null) {
                    managerService.enableBle_sync(source.getPackageName(), bleToken)
                } else {
                    managerService.enable_sync(source.getPackageName(), isQuiet)
                }
            }
            is BluetoothServiceMessages.Disable -> {
                val source = obj.attributionSource
                val persist = obj.persist
                val bleToken = obj.bleToken
                val foregroundRequired = bleToken == null
                try {
                    checker.disableAllowed(sendingUid, source, foregroundRequired)
                } catch (e: PermissionChecker.BluetoothPermissionException) {
                    Log.e(TAG, "${obj}: FAILED", e)
                    return false
                }
                if (bleToken != null) {
                    managerService.disableBle_sync(source.getPackageName(), bleToken)
                } else {
                    managerService.disable_sync(source.getPackageName(), persist)
                }
            }
            is BluetoothServiceMessages.FactoryReset -> {
                checker.enforcePrivileged(sendingUid)

                val source = obj.attributionSource
                try {
                    checker.factoryAllowed(source)
                } catch (e: PermissionChecker.BluetoothPermissionException) {
                    Log.e(TAG, "${obj}: FAILED", e)
                    return false
                }
                managerService.onFactoryReset_sync()
            }
            is BluetoothServiceMessages.IsBleScanAvailable -> {
                managerService.isBleScanAvailable()
            }
            is BluetoothServiceMessages.IsHearingAidSupported -> {
                BluetoothServiceMessages.BooleanValue().apply {
                    value = managerService.isHearingAidProfileSupported()
                }
            }
            is BluetoothServiceMessages.SetSnoopLog -> {
                checker.enforcePrivileged(sendingUid)

                val mode = obj.mode

                BluetoothProperties.snoop_log_mode(
                    when (mode) {
                        BluetoothAdapter.BT_SNOOP_LOG_MODE_DISABLED ->
                            BluetoothProperties.snoop_log_mode_values.DISABLED
                        BluetoothAdapter.BT_SNOOP_LOG_MODE_FILTERED ->
                            BluetoothProperties.snoop_log_mode_values.FILTERED
                        BluetoothAdapter.BT_SNOOP_LOG_MODE_FULL ->
                            BluetoothProperties.snoop_log_mode_values.FULL
                        else ->
                            throw IllegalArgumentException(
                                "Invalid Bluetooth HCI snoop log mode param value"
                            )
                    }
                )
            }
            is BluetoothServiceMessages.GetSnoopLog -> {
                checker.enforcePrivileged(sendingUid)

                when (
                    BluetoothProperties.snoop_log_mode()
                        .orElse(BluetoothProperties.snoop_log_mode_values.DISABLED)
                ) {
                    BluetoothProperties.snoop_log_mode_values.FILTERED ->
                        BluetoothAdapter.BT_SNOOP_LOG_MODE_FILTERED
                    BluetoothProperties.snoop_log_mode_values.FULL ->
                        BluetoothAdapter.BT_SNOOP_LOG_MODE_FULL
                    else -> BluetoothAdapter.BT_SNOOP_LOG_MODE_DISABLED
                }
            }
            is BluetoothServiceMessages.IsAutoSupported -> {
                checker.enforcePrivileged(sendingUid)
                managerService.isAutoOnSupported_sync()
            }
            is BluetoothServiceMessages.SetAutoOnEnabled -> {
                checker.enforcePrivileged(sendingUid)
                managerService.setAutoOnEnabled_sync(obj.enabledStatus)
            }
            is BluetoothServiceMessages.IsAutoEnabled -> {
                checker.enforcePrivileged(sendingUid)
                managerService.isAutoOnEnabled_sync()
            }
            else -> throw IllegalArgumentException("Command does not exist: ${obj}")
        }
    }
}
