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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice.TRANSPORT_BREDR
import android.bluetooth.BluetoothManager
import android.bluetooth.Utils
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import androidx.core.util.Pair
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4


import com.android.pandora.intentFlow
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Empty

import io.grpc.Context.CancellableContext
import io.grpc.Deadline
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import pandora.HostGrpc
import pandora.HostProto.ScanRequest
import pandora.HostProto.ScanningResponse

@RunWith(AndroidJUnit4::class)
class AclConnectionTest {

    companion object {
        private const val TAG = "AclConnectionTest"

        private var mChannel:  ManagedChannel? = null
        private var mHostBlockingStub:  HostGrpc.HostBlockingStub? = null
        private var mHostStub:  HostGrpc.HostStub? = null

        @BeforeClass
        fun setUpClass() {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .adoptShellPermissionIdentity();
        }

    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val context = InstrumentationRegistry.getInstrumentation().getContext()

    @Before
    fun setUp() {
        // FactorReset is killing the server and restart
        // all channel created before the server restarted
        // cannot be reused
        val channel = OkHttpChannelBuilder
              .forAddress("localhost", 7999)
              .usePlaintext()
              .build()

        val stub = HostGrpc.newBlockingStub(channel)
        stub.factoryReset(Empty.getDefaultInstance())

        // terminate the channel
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)

        // Create a new channel for all successive grpc calls
        mChannel = OkHttpChannelBuilder
              .forAddress("localhost", 7999)
              .usePlaintext()
              .build()

        mHostBlockingStub = HostGrpc.newBlockingStub(mChannel)
        mHostStub = HostGrpc.newStub(mChannel)
        mHostBlockingStub?.withWaitForReady()?.readLocalAddress(Empty.getDefaultInstance())
    }

    @After
    fun tearDown() {
        // terminate the channel
        mChannel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
    }

    @Test
    fun aclConnectionTest() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        val flow: Flow<Intent> = intentFlow(context, intentFilter).shareIn(testScope, SharingStarted.Eagerly)
        
        testScope.runTest {
            flow
                .filter { it.action == BluetoothDevice.ACTION_ACL_CONNECTED }
                .filter {
                    it.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.ERROR) == TRANSPORT_BREDR
                }
                .first()
        }

    }
}