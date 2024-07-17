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

package android.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.common.truth.Truth.assertThat
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import pandora.SecurityProto.PairingEvent
import pandora.SecurityProto.PairingEventAnswer

@kotlinx.coroutines.ExperimentalCoroutinesApi
public class Host(context: Context) : Closeable {
    private val TAG = "PandoraHost"

    private val flow: Flow<Intent>
    private val scope: CoroutineScope

    init {
        scope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)

        flow = intentFlow(context, intentFilter, scope).shareIn(scope, SharingStarted.Eagerly)
    }

    override fun close() {
        scope.cancel()
    }

    public fun createAndValidatePairing(bumblePandoraDevice: PandoraDevice) {
        val bumbleBluetoothDevice = bumblePandoraDevice.getRemoteDevice()
        val pairingEventStreamObserver: StreamObserverSpliterator<PairingEvent> =
            StreamObserverSpliterator()
        val pairingEventAnswerObserver =
            bumblePandoraDevice
                .security()
                .withDeadlineAfter(10 * 1000, TimeUnit.MILLISECONDS)
                .onPairing(pairingEventStreamObserver)

        assertThat(bumbleBluetoothDevice.createBond()).isTrue()
        runBlocking {
            withTimeout(10 * 1000) {
                scope.launch {
                    flow
                        .filter { it.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED }
                        .filter { it.getBluetoothDeviceExtra() == bumbleBluetoothDevice }
                        .first()

                    flow
                        .filter { it.getAction() == BluetoothDevice.ACTION_PAIRING_REQUEST }
                        .filter { it.getBluetoothDeviceExtra() == bumbleBluetoothDevice }
                        .first()
                    bumbleBluetoothDevice.setPairingConfirmation(true)

                    val pairingEvent = pairingEventStreamObserver.iterator().next()
                    assertThat(pairingEvent.hasJustWorks()).isTrue()
                    pairingEventAnswerObserver.onNext(
                        PairingEventAnswer.newBuilder()
                            .setEvent(pairingEvent)
                            .setConfirm(true)
                            .build()
                    )

                    flow
                        .filter { it.getAction() == BluetoothDevice.ACTION_BOND_STATE_CHANGED }
                        .filter { it.getBluetoothDeviceExtra() == bumbleBluetoothDevice }
                        .filter {
                            it.getIntExtra(
                                BluetoothDevice.EXTRA_BOND_STATE,
                                BluetoothAdapter.ERROR
                            ) == BluetoothDevice.BOND_BONDED
                        }
                        .first()
                }
            }
        }
        scope.cancel()
    }

    fun Intent.getBluetoothDeviceExtra(): BluetoothDevice =
        this.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)!!

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun intentFlow(context: Context, intentFilter: IntentFilter, scope: CoroutineScope) =
        callbackFlow {
            val broadcastReceiver: BroadcastReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        scope.launch { trySendBlocking(intent) }
                    }
                }
            context.registerReceiver(broadcastReceiver, intentFilter)

            awaitClose { context.unregisterReceiver(broadcastReceiver) }
        }
}
