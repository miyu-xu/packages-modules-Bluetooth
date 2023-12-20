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

import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
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

    fun toggleBluetooth() {
        val disableFuture = CompletableFuture<Boolean>()
        val enableFuture = CompletableFuture<Boolean>()

        val bluetoothAdapterStateReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                        val prevState =
                            intent.getIntExtra(
                                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                                BluetoothAdapter.ERROR
                            )
                        val currState =
                            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        Log.i(TAG, "Bluetooth state changed from " + prevState + " to " + currState)

                        if (currState == BluetoothAdapter.STATE_OFF) {
                            disableFuture.complete(true)
                        } else if (currState == BluetoothAdapter.STATE_ON) {
                            enableFuture.complete(true)
                        }
                    }
                }
            }

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        mContext.registerReceiver(bluetoothAdapterStateReceiver, intentFilter)

        // Disable Bluetooth
        mBluetoothAdapter.disable()
        var toggled =
            disableFuture
                .completeOnTimeout(false, TIMEOUT_BLE_TOGGLE_MS, TimeUnit.MILLISECONDS)
                .join()

        if (toggled) {
            // Enable Bluetooth
            mBluetoothAdapter.enable()
            toggled =
                enableFuture
                    .completeOnTimeout(false, TIMEOUT_BLE_TOGGLE_MS, TimeUnit.MILLISECONDS)
                    .join()
        }

        mContext.unregisterReceiver(bluetoothAdapterStateReceiver)
        check(toggled) { "Bluetooth could not be toggled!" }
    }

    @JvmOverloads
    fun advertiseWithBumble(
        addressType: OwnAddressType,
        serviceUuid: String? = null,
        isConnectable: Boolean = false,
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

    fun scanWithPendingIntent(
        scanFilter: ScanFilter,
        scanSettings: ScanSettings,
        pendingIntentFlags: Int,
    ): List<ScanResult> {
        val future = CompletableFuture<List<ScanResult>>()
        val scanResults = mutableListOf<ScanResult>()

        val scanResultReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (ACTION_DYNAMIC_RECEIVER_SCAN_RESULT == intent.action) {
                        val results =
                            intent.getParcelableArrayListExtra<ScanResult>(
                                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
                            )
                        if (results == null) {
                            Log.i(TAG, "onScanResult results: null")
                            return
                        }

                        val callbackType =
                            intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
                        Log.i(
                            TAG,
                            "onScanResult " +
                                "callbackType: " +
                                callbackType +
                                ", results: " +
                                results
                        )
                        if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                            for (result in results) {
                                if (scanResults.size < 2) {
                                    scanResults.add(result)
                                } else {
                                    future.complete(scanResults)
                                }
                            }
                        } else {
                            future.complete(results)
                        }
                    }
                }
            }

        val intentFilter = IntentFilter(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT)
        mContext.registerReceiver(scanResultReceiver, intentFilter)

        val scanIntent = Intent(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT)
        val pendingIntent = PendingIntent.getBroadcast(mContext, 0, scanIntent, pendingIntentFlags)

        mLeScanner.startScan(listOf(scanFilter), scanSettings, pendingIntent)

        val results =
            future.completeOnTimeout(listOf(), TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS).join()

        mLeScanner.stopScan(pendingIntent)
        mContext.unregisterReceiver(scanResultReceiver)

        return results
    }

    fun connectGatt(): BluetoothGatt {
        val gattCallback = mock<BluetoothGattCallback>()
        val bumbleDevice =
            mBluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )

        val gatt = bumbleDevice.connectGatt(mContext, false, gattCallback)

        verify(gattCallback, timeout(TIMEOUT_CONNECT_MS))
            .onConnectionStateChange(
                any(),
                eq(BluetoothGatt.GATT_SUCCESS),
                eq(BluetoothProfile.STATE_CONNECTED)
            )

        return gatt
    }

    companion object {
        const val TAG = "LeScanningTest"
        const val TIMEOUT_SCANNING_MS = 2000L
        const val TIMEOUT_CONNECT_MS = 2000L
        const val TIMEOUT_BLE_TOGGLE_MS = 3000L
        const val TEST_UUID_STRING = "00001805-0000-1000-8000-00805f9b34fb"
        const val TEST_ADDRESS_RANDOM_STATIC = "F0:43:A8:23:10:11"
        const val TEST_ADDRESS_PUBLIC = "F0:43:A8:23:10:11"
        const val ACTION_DYNAMIC_RECEIVER_SCAN_RESULT =
            "android.bluetooth.test.ACTION_DYNAMIC_RECEIVER_SCAN_RESULT"
    }
}
