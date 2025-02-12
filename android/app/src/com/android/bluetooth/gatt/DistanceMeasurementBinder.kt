/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.bluetooth.gatt

import android.Manifest
import android.annotation.RequiresPermission
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.IDistanceMeasurement
import android.bluetooth.le.ChannelSoundingParams
import android.bluetooth.le.DistanceMeasurementMethod
import android.bluetooth.le.DistanceMeasurementParams
import android.bluetooth.le.IDistanceMeasurementCallback
import android.content.AttributionSource
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.android.bluetooth.Utils
import com.android.bluetooth.flags.Flags
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

class DistanceMeasurementBinder(
    private val mContext: Context,
    private val mDistanceMeasurementManager: DistanceMeasurementManager,
    private val mLooper: Looper,
) : IDistanceMeasurement.Stub() {

    private val WAIT_TIMEOUT_MS = 100L

    private val mHandler = Handler(mLooper)

    @Volatile private var mIsAvailable = true

    fun cleanup() {
        mIsAvailable = false
    }

    @RequiresPermission(
        allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_PRIVILEGED]
    )
    private fun getManager(source: AttributionSource, method: String): DistanceMeasurementManager? {
        if (
            !mIsAvailable ||
                !Utils.callerIsSystemOrActiveOrManagedUser(
                    mContext,
                    TAG,
                    "DistanceMeasurement $method",
                ) ||
                !Utils.checkConnectPermissionForDataDelivery(
                    mContext,
                    source,
                    "DistanceMeasurement $method",
                )
        ) {
            return null
        }
        mContext.enforceCallingOrSelfPermission(Manifest.permission.BLUETOOTH_PRIVILEGED, null)
        return mDistanceMeasurementManager
    }

    override fun getSupportedDistanceMeasurementMethods(
        source: AttributionSource
    ): List<DistanceMeasurementMethod> {
        val manager: DistanceMeasurementManager =
            getManager(source, "getSupportedDistanceMeasurementMethods") ?: return emptyList()

        if (Flags.advertiseThread() && !mLooper.isCurrentThread()) {
            val latch = CountDownLatch(1)
            var result: List<DistanceMeasurementMethod> = ArrayList()

            mHandler.post {
                result = manager.getSupportedDistanceMeasurementMethods()
                latch.countDown()
            }
            latch.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            return result
        } else {
            return manager.getSupportedDistanceMeasurementMethods()
        }
    }

    override fun startDistanceMeasurement(
        uuid: ParcelUuid,
        distanceMeasurementParams: DistanceMeasurementParams,
        callback: IDistanceMeasurementCallback,
        source: AttributionSource,
    ) {
        val manager: DistanceMeasurementManager =
            getManager(source, "startDistanceMeasurement") ?: return

        if (Flags.advertiseThread() && !mLooper.isCurrentThread()) {
            mHandler.post {
                manager.startDistanceMeasurement(uuid.uuid, distanceMeasurementParams, callback)
            }
        } else {
            manager.startDistanceMeasurement(uuid.uuid, distanceMeasurementParams, callback)
        }
    }

    override fun stopDistanceMeasurement(
        uuid: ParcelUuid,
        device: BluetoothDevice,
        method: Int,
        source: AttributionSource,
    ): Int {
        if (!mIsAvailable) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED
        } else if (
            !Utils.callerIsSystemOrActiveOrManagedUser(mContext, TAG, "stopDistanceMeasurement")
        ) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED
        } else if (
            !Utils.checkConnectPermissionForDataDelivery(
                mContext,
                source,
                "DistanceMeasurement stopDistanceMeasurement",
            )
        ) {
            return BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION
        }
        mContext.enforceCallingOrSelfPermission(Manifest.permission.BLUETOOTH_PRIVILEGED, null)

        if (Flags.advertiseThread() && !mLooper.isCurrentThread()) {
            val latch = CountDownLatch(1)
            var result: Int = BluetoothStatusCodes.ERROR_UNKNOWN

            mHandler.post {
                result =
                    mDistanceMeasurementManager.stopDistanceMeasurement(
                        uuid.uuid,
                        device,
                        method,
                        false,
                    )
                latch.countDown()
            }
            latch.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            return result
        } else {
            return mDistanceMeasurementManager.stopDistanceMeasurement(
                uuid.uuid,
                device,
                method,
                false,
            )
        }
    }

    override fun getChannelSoundingMaxSupportedSecurityLevel(
        remoteDevice: BluetoothDevice,
        source: AttributionSource,
    ): Int {
        val manager: DistanceMeasurementManager =
            getManager(source, "getChannelSoundingMaxSupportedSecurityLevel")
                ?: return ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN

        if (Flags.advertiseThread() && !mLooper.isCurrentThread()) {
            val latch = CountDownLatch(1)
            var result: Int = ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN

            mHandler.post {
                result = manager.getChannelSoundingMaxSupportedSecurityLevel(remoteDevice)
                latch.countDown()
            }
            try {
                latch.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Log.w(TAG, "InterruptedException happened", e)
            }
            return result
        } else {
            return manager.getChannelSoundingMaxSupportedSecurityLevel(remoteDevice)
        }
    }

    override fun getLocalChannelSoundingMaxSupportedSecurityLevel(source: AttributionSource): Int {
        val manager: DistanceMeasurementManager =
            getManager(source, "getLocalChannelSoundingMaxSupportedSecurityLevel")
                ?: return ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN

        if (Flags.advertiseThread() && !mLooper.isCurrentThread()) {
            val latch = CountDownLatch(1)
            var result: Int = ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN

            mHandler.post {
                result = manager.getLocalChannelSoundingMaxSupportedSecurityLevel()
                latch.countDown()
            }
            try {
                latch.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Log.w(TAG, "InterruptedException happened", e)
            }
            return result
        } else {
            return manager.getLocalChannelSoundingMaxSupportedSecurityLevel()
        }
    }

    override fun getChannelSoundingSupportedSecurityLevels(source: AttributionSource): IntArray {
        val manager: DistanceMeasurementManager =
            getManager(source, "getChannelSoundingSupportedSecurityLevels") ?: return IntArray(0)

        var channelSoundSecurityLevels: Set<Int> = HashSet()

        if (Flags.advertiseThread() && !mLooper.isCurrentThread()) {
            val latch = CountDownLatch(1)

            mHandler.post {
                channelSoundSecurityLevels = manager.getChannelSoundingSupportedSecurityLevels()
                latch.countDown()
            }
            try {
                latch.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Log.w(TAG, "InterruptedException happened", e)
            }
        } else {
            channelSoundSecurityLevels = manager.getChannelSoundingSupportedSecurityLevels()
        }

        return channelSoundSecurityLevels.stream().mapToInt { i -> i }.toArray()
    }

    companion object {
        private val TAG: String = DistanceMeasurementBinder::class.java.simpleName
    }
}
