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

import android.bluetooth.BluetoothProfile
import android.bluetooth.IBluetooth
import android.bluetooth.IBluetoothCallback
import android.content.AttributionSource
import android.os.IBinder
import android.os.RemoteException
import com.android.modules.utils.SynchronousResultReceiver
import java.time.Duration
import java.util.concurrent.TimeoutException

class AdapterBinder(private val mBinder: IBinder) {
    companion object {
        private val syncTimeout = Duration.ofSeconds(3)
    }

    private val mBluetooth: IBluetooth = IBluetooth.Stub.asInterface(mBinder)

    @JvmName("getAdapterBinder")
    internal fun getAdapterBinder(): IBluetooth {
        return mBluetooth
    }

    @JvmName("getRawBinder")
    internal fun getRawBinder(): IBinder {
        return mBinder
    }

    @JvmName("disable")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun disable(source: AttributionSource): Boolean {
        val recv: SynchronousResultReceiver<Boolean> = SynchronousResultReceiver.get()
        mBluetooth.disable(source, recv)
        return recv.awaitResultNoInterrupt(syncTimeout).getValue(false)
    }

    @JvmName("enable")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun enable(quietMode: Boolean, source: AttributionSource): Boolean {
        val recv: SynchronousResultReceiver<Boolean> = SynchronousResultReceiver.get()
        mBluetooth.enable(quietMode, source, recv)
        return recv.awaitResultNoInterrupt(syncTimeout).getValue(false)
    }

    @JvmName("getAddress")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun getAddress(source: AttributionSource): String? {
        val recv: SynchronousResultReceiver<String> = SynchronousResultReceiver.get()
        mBluetooth.getAddress(source, recv)
        return recv.awaitResultNoInterrupt(syncTimeout).getValue(null)
    }

    @JvmName("getName")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun getName(source: AttributionSource): String? {
        val recv: SynchronousResultReceiver<String> = SynchronousResultReceiver.get()
        mBluetooth.getName(source, recv)
        return recv.awaitResultNoInterrupt(syncTimeout).getValue(null)
    }

    @JvmName("onBrEdrDown")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun onBrEdrDown(source: AttributionSource) {
        val recv: SynchronousResultReceiver<Any> = SynchronousResultReceiver.get()
        mBluetooth.onBrEdrDown(source, recv)
        recv.awaitResultNoInterrupt(syncTimeout).getValue(null)
    }

    @JvmName("onLeServiceUp")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun onLeServiceUp(source: AttributionSource) {
        val recv: SynchronousResultReceiver<Any> = SynchronousResultReceiver.get()
        mBluetooth.onLeServiceUp(source, recv)
        recv.awaitResultNoInterrupt(syncTimeout).getValue(null)
    }

    @JvmName("registerCallback")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun registerCallback(callback: IBluetoothCallback, source: AttributionSource) {
        val recv: SynchronousResultReceiver<Any> = SynchronousResultReceiver.get()
        mBluetooth.registerCallback(callback, source, recv)
        recv.awaitResultNoInterrupt(syncTimeout).getValue(null)
    }

    @JvmName("unregisterCallback")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun unregisterCallback(callback: IBluetoothCallback, source: AttributionSource) {
        val recv: SynchronousResultReceiver<Any> = SynchronousResultReceiver.get()
        mBluetooth.unregisterCallback(callback, source, recv)
        recv.awaitResultNoInterrupt(syncTimeout).getValue(null)
    }

    @JvmName("getSupportedProfiles")
    @Throws(RemoteException::class, TimeoutException::class)
    internal fun getSupportedProfiles(source: AttributionSource): MutableList<Int> {
        val supportedProfiles = ArrayList<Int>()
        val recv: SynchronousResultReceiver<Long> = SynchronousResultReceiver.get()
        mBluetooth.getSupportedProfiles(source, recv)
        val supportedProfilesBitMask: Long = recv.awaitResultNoInterrupt(syncTimeout).getValue(0L)
        for (i in 0..BluetoothProfile.MAX_PROFILE_ID) {
            if (supportedProfilesBitMask and (1 shl i).toLong() != 0L) {
                supportedProfiles.add(i)
            }
        }
        return supportedProfiles
    }
}
