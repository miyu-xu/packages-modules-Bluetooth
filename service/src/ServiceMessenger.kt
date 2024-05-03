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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Parcelable
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
            reply.obj = handleMessage(msg.sendingUid, msg.obj as Parcelable)
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

    private fun handleMessage(sendingUid: Int, obj: Parcelable): Parcelable {
        return when (obj) {
            is SystemServiceMessage.RegisterAdapter -> {
                SystemServiceMessage.RegisterAdapter.Reply().apply {
                    value = managerService.registerAdapter_sync(obj.binder)?.asBinder()
                }
            }
            is SystemServiceMessage.UnregisterAdapter -> {
                managerService.unregisterAdapter_sync(obj.binder)
                SystemServiceMessage.UnregisterAdapter.Reply()
            }
            is SystemServiceMessage.Enable -> {
                val source = obj.attributionSource
                val isQuiet = obj.isQuiet
                val bleToken = obj.bleToken

                val foregroundRequired = isQuiet == false || bleToken == null
                SystemServiceMessage.Enable.Reply().apply {
                    value =
                        try {
                            checker.enableAllowed(sendingUid, source, foregroundRequired)
                            if (bleToken != null) {
                                managerService.enableBle_sync(source.getPackageName(), bleToken)
                            } else {
                                managerService.enable_sync(source.getPackageName(), isQuiet)
                            }
                        } catch (e: PermissionChecker.BluetoothPermissionException) {
                            Log.e(TAG, "${obj}: FAILED", e)
                            false
                        }
                }
            }
            is SystemServiceMessage.Disable -> {
                val source = obj.attributionSource
                val persist = obj.persist
                val bleToken = obj.bleToken
                val foregroundRequired = bleToken == null
                SystemServiceMessage.Disable.Reply().apply {
                    value =
                        try {
                            checker.disableAllowed(sendingUid, source, foregroundRequired)
                            if (bleToken != null) {
                                managerService.disableBle_sync(source.getPackageName(), bleToken)
                            } else {
                                managerService.disable_sync(source.getPackageName(), persist)
                            }
                        } catch (e: PermissionChecker.BluetoothPermissionException) {
                            Log.e(TAG, "${obj}: FAILED", e)
                            false
                        }
                }
            }
            is SystemServiceMessage.FactoryReset -> {
                checker.enforcePrivileged(sendingUid)

                val source = obj.attributionSource
                SystemServiceMessage.FactoryReset.Reply().apply {
                    value =
                        try {
                            checker.factoryAllowed(source)
                            managerService.onFactoryReset_sync()
                        } catch (e: PermissionChecker.BluetoothPermissionException) {
                            Log.e(TAG, "${obj}: FAILED", e)
                            false
                        }
                }
            }
            is SystemServiceMessage.GetAddress -> {
                val source = obj.attributionSource

                SystemServiceMessage.GetAddress.Reply().apply {
                    value =
                        try {
                            checker.getAddressAllowed(sendingUid, source)
                            managerService.getAddress_sync()
                        } catch (e: PermissionChecker.BluetoothPermissionException) {
                            Log.e(TAG, "${obj}: FAILED", e)
                            BluetoothAdapter.DEFAULT_MAC_ADDRESS
                        }
                }
            }
            is SystemServiceMessage.GetName -> {
                val source = obj.attributionSource

                SystemServiceMessage.GetName.Reply().apply {
                    value =
                        try {
                            checker.getNameAllowed(sendingUid, source)
                            managerService.getName_sync()
                        } catch (e: PermissionChecker.BluetoothPermissionException) {
                            Log.e(TAG, "${obj}: FAILED", e)
                            null
                        }
                }
            }
            is SystemServiceMessage.IsBleScanAvailable -> {
                SystemServiceMessage.IsBleScanAvailable.Reply().apply {
                    value = managerService.isBleScanAvailable()
                }
            }
            is SystemServiceMessage.IsHearingAidSupported -> {
                SystemServiceMessage.IsHearingAidSupported.Reply().apply {
                    value = managerService.isHearingAidProfileSupported()
                }
            }
            is SystemServiceMessage.SetSnoopLog -> {
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
                SystemServiceMessage.SetSnoopLog.Reply()
            }
            is SystemServiceMessage.GetSnoopLog -> {
                checker.enforcePrivileged(sendingUid)
                SystemServiceMessage.GetSnoopLog.Reply().apply {
                    value =
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
            }
            is SystemServiceMessage.IsAutoSupported -> {
                checker.enforcePrivileged(sendingUid)
                SystemServiceMessage.IsAutoSupported.Reply().apply {
                    value = managerService.isAutoOnSupported_sync()
                }
            }
            is SystemServiceMessage.SetAutoOnEnabled -> {
                checker.enforcePrivileged(sendingUid)
                managerService.setAutoOnEnabled_sync(obj.enabledStatus)
                SystemServiceMessage.SetAutoOnEnabled.Reply()
            }
            is SystemServiceMessage.IsAutoEnabled -> {
                checker.enforcePrivileged(sendingUid)
                SystemServiceMessage.IsAutoEnabled.Reply().apply {
                    value = managerService.isAutoOnEnabled_sync()
                }
            }
            else -> throw IllegalArgumentException("Invalid command: [${obj}] from ${sendingUid}")
        }
    }
}
