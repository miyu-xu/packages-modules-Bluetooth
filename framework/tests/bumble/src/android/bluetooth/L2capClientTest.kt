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

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.grpc.Context as GrpcContext
import io.grpc.Deadline
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pandora.GattProto.GattCharacteristicParams
import pandora.GattProto.GattServiceParams
import pandora.GattProto.RegisterServiceRequest
import pandora.HostProto
import pandora.HostProto.AdvertiseRequest
import pandora.HostProto.Connection
import pandora.HostProto.OwnAddressType
import pandora.l2cap.L2CAPProto.CreditBasedChannelRequest
import pandora.l2cap.L2CAPProto.WaitConnectionRequest

/** L2CAP Client Tests */
@RunWith(TestParameterInjector::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
public class L2capClientTest() : Closeable {

    private val scope: CoroutineScope
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter
    private var serviceDiscoveredFlow = MutableStateFlow(false)
    private var connectionStateFlow = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    private var dckSpsmFlow = MutableStateFlow(0)
    private var dckSpsm = 0
    private var connectionHandle = BluetoothDevice.ERROR
    private lateinit var bumbleGatt: BluetoothGatt
    private lateinit var advertiseStreamObserver: GrpcContext.CancellableContext

    // A Rule live from a test setup through it's teardown.
    // Gives shell permissions during the test.
    @Rule
    @JvmField
    val mPermissionRule =
        AdoptShellPermissionsRule(
            InstrumentationRegistry.getInstrumentation().getUiAutomation(),
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        )

    // Setup a Bumble Pandora device for the duration of the test.
    // Acting as a Pandora client, it can be interacted with through the Pandora APIs.
    @Rule @JvmField val mBumble = PandoraDevice()

    /** Wrapper for [BluetoothGatt] along with its [state] and [status] */
    data class GattState(val gatt: BluetoothGatt, val status: Int, val state: Int)

    init {
        scope = CoroutineScope(Dispatchers.Default)
    }

    override fun close() {
        scope.cancel("Cancelling test scope")
    }

    @Before
    fun setUp() {
        registerGattService()

        // Advertise the Bumble
        advertiseStreamObserver = advertiseWithBumble()

        // Connect to GATT (Generic Attribute Profile) on Bumble.
        connectGatt()
        // Wait a bit for connection to sync on Bumble. ##TODO##
        Thread.sleep(500)
    }

    @After
    fun tearDown() {

        advertiseStreamObserver.cancel(null)
        bumbleGatt.close()
    }

    /** Tests creating an L2CAP channel on a Bumble l2cap server. */
    @Test
    fun testWaitConnection() {
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )
        val psm: Int
        if (useExistingDckSpsm) {
            readDckSpsm()
            psm = dckSpsm
        } else {
            waitGattConnection()
            psm = TEST_PSM
        }
        setupRemoteL2capServer(remoteDevice, psm)
        val socket = createChannel(psm)
        assertThat(socket).isNotNull()
    }

    private fun waitGattConnection() = runBlocking {
        launch {
            withTimeout(SERVICE_TIMEOUT.toMillis()) {
                connectionStateFlow.first { it == BluetoothProfile.STATE_CONNECTED }
                Log.i(TAG, "Connected to GATT")
            }
        }
    }

    private fun readDckSpsm() = runBlocking {
        var timeoutMs = SERVICE_TIMEOUT.toMillis()
        launch {
            withTimeout(timeoutMs) {
                connectionStateFlow.first { it == BluetoothProfile.STATE_CONNECTED }
            }
            Log.i(TAG, "Connected to GATT")
            bumbleGatt.discoverServices()
            withTimeout(timeoutMs) { serviceDiscoveredFlow.first { it == true } }
            Log.i(TAG, "GATT services discovered")
            val service = bumbleGatt.getService(CCC_DK_UUID)
            assertThat(service).isNotNull()
            val characteristic = service.getCharacteristic(SPSM_UUID)
            bumbleGatt.readCharacteristic(characteristic)
            withTimeout(timeoutMs) { dckSpsmFlow.first { it != 0 } }
            dckSpsm = dckSpsmFlow.value
            Log.i(TAG, "spsm read, spsm=$dckSpsm")
        }
    }

    private fun setupRemoteL2capServer(remoteDevice: BluetoothDevice, psm: Int) {
        val connectionHandle = remoteDevice.getConnectionHandle(BluetoothDevice.TRANSPORT_LE)
        Log.i(TAG, "Connection handle=$connectionHandle")
        val handle = intToByteArray(connectionHandle, ByteOrder.BIG_ENDIAN)
        val cookie = Any.newBuilder().setValue(ByteString.copyFrom(handle)).build()
        val connection = Connection.newBuilder().setCookie(cookie).build()
        val leCreditBased =
            CreditBasedChannelRequest.newBuilder()
                .setSpsm(psm)
                .setInitialCredit(INITIAL_CREDITS)
                .setMtu(MTU)
                .setMps(MPS)
                .build()
        val waitConnectionRequest =
            WaitConnectionRequest.newBuilder()
                .setConnection(connection)
                .setLeCreditBased(leCreditBased)
                .build()
        runBlocking(scope.coroutineContext) {
            launch {
                withTimeout(SERVICE_TIMEOUT.toMillis()) {
                    val waitResponse = mBumble.l2capBlocking().waitConnection(waitConnectionRequest)
                    Log.i(TAG, "createL2Cap WaitConnection response: $waitResponse")
                    assertThat(waitResponse.hasChannel()).isTrue()
                }
            }
        }
    }

    private fun createChannel(psm: Int, isSecure: Boolean = false): BluetoothSocket {
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )
        var socket: BluetoothSocket
        var expectedType: Int
        if (isSecure) {
            socket = remoteDevice.createL2capChannel(psm)
            expectedType = BluetoothSocket.TYPE_L2CAP_LE
        } else {
            socket = remoteDevice.createInsecureL2capChannel(psm)
            expectedType = BluetoothSocket.TYPE_L2CAP
        }

        socket.connect()

        assertThat(socket.getConnectionType()).isEqualTo(expectedType)
        return socket
    }

    private fun connectGatt() {
        val remoteDevice =
            bluetoothAdapter.getRemoteLeDevice(
                Utils.BUMBLE_RANDOM_ADDRESS,
                BluetoothDevice.ADDRESS_TYPE_RANDOM
            )
        val gattCallback =
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    Log.i(TAG, "Connection state changed to $newState.")
                    connectionStateFlow.value = newState
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

                    Log.i(TAG, "Discovering services status=$status")
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "Services have been discovered")
                        serviceDiscoveredFlow.value = true
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
                ) {
                    Log.i(TAG, "onCharacteristicRead, status: $status")

                    if (characteristic.getUuid() == SPSM_UUID) {
                        // CCC Specification Digital-Key R3-1.2.3
                        // 19.2.1.6 DK Service
                        dckSpsmFlow.value = byteArrayToInt(value, ByteOrder.BIG_ENDIAN)
                    }
                }
            }

        bumbleGatt = remoteDevice.connectGatt(context, false, gattCallback)
    }

    fun byteArrayToInt(byteArray: ByteArray, order: ByteOrder): Int {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(order)
        return buffer.short.toInt()
    }

    private fun intToByteArray(value: Int, order: ByteOrder): ByteArray {
        val buffer = ByteBuffer.allocate(4) // Allocate 4 bytes for an Int
        buffer.order(order)
        buffer.putInt(value)
        return buffer.array()
    }

    private fun registerGattService() {
        if (useExistingDckSpsm) {
            // Register Bumble's DCK (Digital Car Key) service
            mBumble
                .dckBlocking()
                .withDeadline(Deadline.after(SERVICE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .register(Empty.getDefaultInstance())
        } else {
            // Register a generic GATT service
            val characteristicParams =
                GattCharacteristicParams.newBuilder()
                    .setProperties(BluetoothGattCharacteristic.PROPERTY_WRITE)
                    .setUuid(TEST_CHARACTERISTIC_UUID.toString())
                    .build()

            val serviceParams =
                GattServiceParams.newBuilder()
                    .addCharacteristics(characteristicParams)
                    .setUuid(TEST_SERVICE_UUID.toString())
                    .build()

            val request = RegisterServiceRequest.newBuilder().setService(serviceParams).build()

            mBumble.gattBlocking().registerService(request)
        }
    }

    private fun advertiseWithBumble(
        ownAddressType: OwnAddressType = OwnAddressType.RANDOM,
        withUuid: Boolean = false
    ): GrpcContext.CancellableContext {
        val requestBuilder =
            AdvertiseRequest.newBuilder()
                .setLegacy(true)
                .setConnectable(true)
                .setOwnAddressType(ownAddressType)

        if (withUuid) {
            requestBuilder.data =
                HostProto.DataTypes.newBuilder()
                    .addCompleteServiceClassUuids128(TEST_SERVICE_UUID.toString())
                    .build()
        }

        val cancellableContext = GrpcContext.current().withCancellation()
        with(cancellableContext) {
            run { mBumble.hostBlocking().advertise(requestBuilder.build()) }
        }

        return cancellableContext
    }

    companion object {
        private const val TAG = "L2capClientTest"
        private const val TEST_ADDRESS_RANDOM_STATIC = "F0:43:A8:23:10:11"
        private const val TEST_PSM = 131
        private const val INITIAL_CREDITS = 256
        private const val MTU = 2048 // Default Maximum Transmission Unit.
        private const val MPS = 2048 // Default Maximum payload size.

        private val SERVICE_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val TEST_SERVICE_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000")
        private val TEST_CHARACTERISTIC_UUID =
            UUID.fromString("00010001-0000-0000-0000-000000000000")

        // CCC DK Specification R3 1.2.0 r14 section 19.2.1.2 Bluetooth Le Pairing
        private val CCC_DK_UUID = UUID.fromString("0000FFF5-0000-1000-8000-00805f9b34fb")
        // Vehicule SPSM
        private val SPSM_UUID = UUID.fromString("D3B5A130-9E23-4B3A-8BE4-6B1EE5F980A3")
        @TestParameter private val useExistingDckSpsm: Boolean = true
    }
}
