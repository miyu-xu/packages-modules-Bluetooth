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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.google.common.collect.Sets
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import pandora.HostProto.OwnAddressType

/** Parameterized LE scan tests */
@RunWith(Parameterized::class)
class ParameterizedLeScanningTest(
    private val isBleToggled: Boolean,
    private val remoteIdentityAddressType: Int,
    private val isRemoteAdvertisingWithUuid: Boolean,
    private val isRemoteConnected: Boolean,
) : LeScanningTestBase() {

    private var bumbleGatt: BluetoothGatt? = null

    @Before
    fun setUp() {
        if (isBleToggled) {
            toggleBluetooth()
        }

        // TODO(315852141): Use supported Bumble for the given address type
        val uuid = if (isRemoteAdvertisingWithUuid) TEST_UUID_STRING else null
        advertiseWithBumble(OwnAddressType.RANDOM, uuid, isRemoteConnected)

        bumbleGatt =
            if (isRemoteConnected) {
                connectGatt()
            } else {
                null
            }
    }

    @Test
    fun scanForIrkAndIdentityAddress() {
        // TODO(316001793): Retrieve identity address from Bumble
        val bleAddress =
            if (remoteIdentityAddressType == BluetoothDevice.ADDRESS_TYPE_RANDOM) {
                TEST_ADDRESS_RANDOM_STATIC
            } else {
                TEST_ADDRESS_PUBLIC
            }
        val scanFilter =
            ScanFilter.Builder()
                .setDeviceAddress(bleAddress, remoteIdentityAddressType, Utils.BUMBLE_IRK)
                .build()
        val scanSettings =
            try {
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_AMBIENT_DISCOVERY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .build()
            } catch (e: IllegalArgumentException) {
                Log.i(
                    TAG,
                    "SCAN_MODE_AMBIENT_DISCOVERY not supported, using SCAN_MODE_LOW_POWER instead"
                )
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .build()
            }

        val results =
            scanWithPendingIntent(
                scanFilter,
                scanSettings,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        if (isRemoteConnected) {
            bumbleGatt?.disconnect()
        }
        assertThat(results).isNotEmpty()
        assertThat(results[0].device.address).isEqualTo(bleAddress)
    }

    companion object {
        // TODO(315852141): Include variations for LE only vs. Dual mode Bumble when supported
        // TODO(315852141): Include variations for two advertisements at the same time
        // TODO(303502437): Include variations for other callback types when supported in rootcanal
        @Parameters(
            name =
                "{index}: isBleToggled = {0}, remoteIdentityAddressType = {1}," +
                    " isRemoteAdvertisingWithUuid = {2}, isRemoteConnected = {3}"
        )
        @JvmStatic
        fun parameters(): Iterable<Array<Any>> {
            val booleanVariations = setOf(true, false)
            val addressTypeVariations =
                setOf(BluetoothDevice.ADDRESS_TYPE_PUBLIC, BluetoothDevice.ADDRESS_TYPE_RANDOM)

            return Sets.cartesianProduct(
                    listOf(
                        /* isBleToggled */ booleanVariations,
                        /* remoteIdentityAddressType */ addressTypeVariations,
                        /* isRemoteAdvertisingWithUuid */ booleanVariations,
                        /* isRemoteConnected */ booleanVariations
                    )
                )
                .map { it.toTypedArray() }
        }
    }
}
