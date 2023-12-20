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

package android.bluetooth

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.Rule
import pandora.HostProto
import pandora.HostProto.AdvertiseRequest
import pandora.HostProto.AdvertiseResponse
import pandora.HostProto.OwnAddressType

/** Base class for LE scan tests */
abstract class LeScanningTestBase {
    @Rule @JvmField val mPermissionRule = AdoptShellPermissionsRule()
    @Rule @JvmField val mBumble = PandoraDevice()

    @JvmField val mContext: Context = ApplicationProvider.getApplicationContext()
    @JvmField val mBluetoothManager = mContext.getSystemService(BluetoothManager::class.java)!!
    @JvmField val mBluetoothAdapter = mBluetoothManager.adapter
    @JvmField val mLeScanner = mBluetoothAdapter.bluetoothLeScanner

    @JvmOverloads
    fun advertiseWithBumble(
        addressType: OwnAddressType,
        serviceUuid: String? = null,
        isConnectable: Boolean = false
    ) {
        val requestBuilder =
            AdvertiseRequest.newBuilder()
                .setOwnAddressType(addressType)
                .setConnectable(isConnectable)

        if (serviceUuid != null) {
            val dataTypeBuilder = HostProto.DataTypes.newBuilder()
            dataTypeBuilder.addCompleteServiceClassUuids128(serviceUuid)
            requestBuilder.setData(dataTypeBuilder.build())
        }

        advertiseWithBumble(requestBuilder)
    }

    fun advertiseWithBumble(requestBuilder: AdvertiseRequest.Builder) {
        // Bumble currently only supports legacy advertising.
        requestBuilder.setLegacy(true)

        // Collect and ignore responses.
        val responseObserver = StreamObserverSpliterator<AdvertiseResponse>()

        mBumble.host().advertise(requestBuilder.build(), responseObserver)
    }

    fun scanWithCallback(scanFilter: ScanFilter, scanSettings: ScanSettings): List<ScanResult> {
        val future = CompletableFuture<List<ScanResult>>()
        val scanResults = mutableListOf<ScanResult>()

        val scanCallback =
            object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    Log.i(
                        TAG,
                        "onScanResult " +
                            "address: " +
                            result.device.address +
                            ", connectable: " +
                            result.isConnectable +
                            ", callbackType: " +
                            callbackType +
                            ", service uuids: " +
                            result.scanRecord?.serviceUuids
                    )

                    if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                        if (scanResults.size < 2) {
                            scanResults.add(result)
                        } else {
                            future.complete(scanResults)
                        }
                    } else {
                        scanResults.add(result)
                        future.complete(scanResults)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.i(TAG, "onScanFailed errorCode: $errorCode")
                    future.complete(listOf())
                }
            }

        mLeScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)

        val results =
            future.completeOnTimeout(listOf(), TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS).join()

        mLeScanner.stopScan(scanCallback)

        return results
    }

    companion object {
        const val TAG = "LeScanningTest"
        const val TIMEOUT_SCANNING_MS = 2000L
        const val TEST_UUID_STRING = "00001805-0000-1000-8000-00805f9b34fb"
        const val TEST_ADDRESS_RANDOM_STATIC = "F0:43:A8:23:10:11"
        const val ACTION_DYNAMIC_RECEIVER_SCAN_RESULT =
            "android.bluetooth.test.ACTION_DYNAMIC_RECEIVER_SCAN_RESULT"
    }
}
