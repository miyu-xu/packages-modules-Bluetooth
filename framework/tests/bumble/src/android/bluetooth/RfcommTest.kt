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

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.test_utils.BlockingBluetoothAdapter
import android.bluetooth.test_utils.EnableBluetoothRule
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.truth.Truth
import com.google.protobuf.ByteString
import java.io.IOException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import pandora.HostProto
import pandora.RfcommProto
import pandora.RfcommProto.RfcommConnection
import pandora.RfcommProto.ServerId
import pandora.RfcommProto.StartServerRequest

@SuppressLint("MissingPermission")
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RfcommTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mManager = mContext.getSystemService(BluetoothManager::class.java)
    private val mAdapter = mManager!!.adapter

    // Gives shell permissions during the test.
    @Rule(order = 0)
    @JvmField
    val mPermissionsRule =
        AdoptShellPermissionsRule(
            InstrumentationRegistry.getInstrumentation().getUiAutomation(),
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.WRITE_SECURE_SETTINGS,
        )

    // Set up a Bumble Pandora device for the duration of the test.
    @Rule(order = 1) @JvmField val mBumble = PandoraDevice()

    @Rule(order = 2) @JvmField val enableBluetoothRule = EnableBluetoothRule(false, true)

    private lateinit var mRemoteDevice: BluetoothDevice
    private lateinit var host: Host
    private var mConnectionCounter = 1
    private var mProfileServiceListener = mock<BluetoothProfile.ServiceListener>()

    @Before
    fun setUp() {
        mRemoteDevice = mBumble.remoteDevice
        host = Host(mContext)
        val bluetoothA2dp = getProfileProxy(mContext, BluetoothProfile.A2DP) as BluetoothA2dp
        bluetoothA2dp.setConnectionPolicy(
            mRemoteDevice,
            BluetoothProfile.CONNECTION_POLICY_FORBIDDEN,
        )
        val bluetoothHfp = getProfileProxy(mContext, BluetoothProfile.HEADSET) as BluetoothHeadset
        bluetoothHfp.setConnectionPolicy(
            mRemoteDevice,
            BluetoothProfile.CONNECTION_POLICY_FORBIDDEN,
        )
        val bluetoothHidHost =
            getProfileProxy(mContext, BluetoothProfile.HID_HOST) as BluetoothHidHost
        bluetoothHidHost.setConnectionPolicy(
            mRemoteDevice,
            BluetoothProfile.CONNECTION_POLICY_FORBIDDEN,
        )
        host.createBondAndVerify(mRemoteDevice)
        if (mRemoteDevice.isConnected) {
            host.disconnectAndVerify(mRemoteDevice)
        }
    }

    @After
    fun tearDown() {
        if (mAdapter.bondedDevices.contains(mRemoteDevice)) {
            host.removeBondAndVerify(mRemoteDevice)
        }
        host.close()
    }

    @Test
    fun clientConnectToOpenServerSocketBondedInsecure() {
        startServer { serverId -> createConnectAcceptSocket(isSecure = false, serverId) }
    }

    @Test
    fun clientConnectToOpenServerSocketBondedSecure() {
        startServer { serverId -> createConnectAcceptSocket(isSecure = true, serverId) }
    }

    @Test
    fun clientSendDataOverInsecureSocket() {
        startServer { serverId ->
            val (insecureSocket, connection) = createConnectAcceptSocket(isSecure = false, serverId)
            val data: ByteArray = "Test data for clientSendDataOverInsecureSocket".toByteArray()
            val socketOs = insecureSocket.outputStream

            socketOs.write(data)
            val rxResponse: RfcommProto.RxResponse =
                mBumble
                    .rfcommBlocking()
                    .withDeadlineAfter(GRPC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .receive(RfcommProto.RxRequest.newBuilder().setConnection(connection).build())
            Truth.assertThat(rxResponse.data).isEqualTo(ByteString.copyFrom(data))
        }
    }

    @Test
    fun clientSendDataOverSecureSocket() {
        startServer { serverId ->
            val (secureSocket, connection) = createConnectAcceptSocket(isSecure = true, serverId)
            val data: ByteArray = "Test data for clientSendDataOverSecureSocket".toByteArray()
            val socketOs = secureSocket.outputStream

            socketOs.write(data)
            val rxResponse: RfcommProto.RxResponse =
                mBumble
                    .rfcommBlocking()
                    .withDeadlineAfter(GRPC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .receive(RfcommProto.RxRequest.newBuilder().setConnection(connection).build())
            Truth.assertThat(rxResponse.data).isEqualTo(ByteString.copyFrom(data))
        }
    }

    @Test
    fun clientReceiveDataOverInsecureSocket() {
        startServer { serverId ->
            val (insecureSocket, connection) = createConnectAcceptSocket(isSecure = false, serverId)
            val buffer = ByteArray(64)
            val socketIs = insecureSocket.inputStream
            val data: ByteString =
                ByteString.copyFromUtf8("Test data for clientReceiveDataOverInsecureSocket")

            val txRequest =
                RfcommProto.TxRequest.newBuilder().setConnection(connection).setData(data).build()
            mBumble.rfcommBlocking().send(txRequest)
            val numBytesFromBumble = socketIs.read(buffer)
            Truth.assertThat(ByteString.copyFrom(buffer).substring(0, numBytesFromBumble))
                .isEqualTo(data)
        }
    }

    @Test
    fun clientReceiveDataOverSecureSocket() {
        startServer { serverId ->
            val (secureSocket, connection) = createConnectAcceptSocket(isSecure = true, serverId)
            val buffer = ByteArray(64)
            val socketIs = secureSocket.inputStream
            val data: ByteString =
                ByteString.copyFromUtf8("Test data for clientReceiveDataOverSecureSocket")

            val txRequest =
                RfcommProto.TxRequest.newBuilder().setConnection(connection).setData(data).build()
            mBumble.rfcommBlocking().send(txRequest)
            val numBytesFromBumble = socketIs.read(buffer)
            Truth.assertThat(ByteString.copyFrom(buffer).substring(0, numBytesFromBumble))
                .isEqualTo(data)
        }
    }

    @Test
    fun connectTwoInsecureClientsSimultaneously() {
        startServer("ServerPort1", TEST_UUID) { serverId1 ->
            startServer("ServerPort2", SERIAL_PORT_UUID) { serverId2 ->
                val socket1 = createSocket(mRemoteDevice, isSecure = false, TEST_UUID)
                val socket2 = createSocket(mRemoteDevice, isSecure = false, SERIAL_PORT_UUID)

                acceptSocket(serverId1)
                Truth.assertThat(socket1.isConnected).isTrue()

                acceptSocket(serverId2)
                Truth.assertThat(socket2.isConnected).isTrue()
            }
        }
    }

    @Test
    fun connectTwoInsecureClientsSequentially() {
        startServer("ServerPort1", TEST_UUID) { serverId1 ->
            startServer("ServerPort2", SERIAL_PORT_UUID) { serverId2 ->
                val socket1 = createSocket(mRemoteDevice, isSecure = false, TEST_UUID)
                acceptSocket(serverId1)
                Truth.assertThat(socket1.isConnected).isTrue()

                val socket2 = createSocket(mRemoteDevice, isSecure = false, SERIAL_PORT_UUID)
                acceptSocket(serverId2)
                Truth.assertThat(socket2.isConnected).isTrue()
            }
        }
    }

    @Test
    fun connectTwoSecureClientsSimultaneously() {
        startServer("ServerPort1", TEST_UUID) { serverId1 ->
            startServer("ServerPort2", SERIAL_PORT_UUID) { serverId2 ->
                val socket2 = createSocket(mRemoteDevice, isSecure = true, SERIAL_PORT_UUID)
                val socket1 = createSocket(mRemoteDevice, isSecure = true, TEST_UUID)

                acceptSocket(serverId1)
                Truth.assertThat(socket1.isConnected).isTrue()

                acceptSocket(serverId2)
                Truth.assertThat(socket2.isConnected).isTrue()
            }
        }
    }

    @Test
    fun connectTwoSecureClientsSequentially() {
        startServer("ServerPort1", TEST_UUID) { serverId1 ->
            startServer("ServerPort2", SERIAL_PORT_UUID) { serverId2 ->
                val socket1 = createSocket(mRemoteDevice, isSecure = true, TEST_UUID)
                acceptSocket(serverId1)
                Truth.assertThat(socket1.isConnected).isTrue()

                val socket2 = createSocket(mRemoteDevice, isSecure = true, SERIAL_PORT_UUID)
                acceptSocket(serverId2)
                Truth.assertThat(socket2.isConnected).isTrue()
            }
        }
    }

    @Test
    fun connectTwoMixedClientsInsecureThenSecure() {
        startServer("ServerPort1", TEST_UUID) { serverId1 ->
            startServer("ServerPort2", SERIAL_PORT_UUID) { serverId2 ->
                val socket2 = createSocket(mRemoteDevice, isSecure = false, SERIAL_PORT_UUID)
                acceptSocket(serverId2)
                Truth.assertThat(socket2.isConnected).isTrue()

                val socket1 = createSocket(mRemoteDevice, isSecure = true, TEST_UUID)
                acceptSocket(serverId1)
                Truth.assertThat(socket1.isConnected).isTrue()
            }
        }
    }

    @Test
    fun connectTwoMixedClientsSecureThenInsecure() {
        startServer("ServerPort1", TEST_UUID) { serverId1 ->
            startServer("ServerPort2", SERIAL_PORT_UUID) { serverId2 ->
                val socket2 = createSocket(mRemoteDevice, isSecure = true, SERIAL_PORT_UUID)
                acceptSocket(serverId2)
                Truth.assertThat(socket2.isConnected).isTrue()

                val socket1 = createSocket(mRemoteDevice, isSecure = false, TEST_UUID)
                acceptSocket(serverId1)
                Truth.assertThat(socket1.isConnected).isTrue()
            }
        }
    }

    @Test
    fun clientConnectToOpenServerSocketBondedInsecurePageTimeout() {
        // Disable inquiry and page scan mode
        Log.i(TAG, "Test start")
        mBumble
            .hostBlocking()
            .setDiscoverabilityMode(
                HostProto.SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(HostProto.DiscoverabilityMode.NOT_DISCOVERABLE)
                    .build()
            )
        Log.i(TAG, "Disabled inquiry scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.NOT_CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Disabled page scan")
        val socket = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object")

        val t = thread {
            Log.i(TAG, "Connecting to socket")
            try {
                socket.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure $e")
            }
            Log.i(TAG, "Done connecting to socket")
        }

        Log.i(TAG, "Waiting for 7 seconds after page timeout of 5 seconds")
        Thread.sleep(7000)
        Log.i(TAG, "Waited 3 seconds to cancel socket connection before page timeout at 5 seconds")

        Truth.assertThat(socket.isConnected).isFalse()

        Log.i(TAG, "Close socket")
        socket.close()

        Log.i(TAG, "Join the thread")
        t.join()

        Log.i(TAG, "Enabling page scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Enabled page scan, reconnecting")

        startServer { serverId -> createConnectAcceptSocket(isSecure = false, serverId) }
        Log.i(TAG, "Connected, test end")
    }

    @Test
    fun clientConnectToOpenServerSocketBondedInsecurePrematureClosure() {
        // Disable inquiry and page scan mode
        Log.i(TAG, "Test start")
        mBumble
            .hostBlocking()
            .setDiscoverabilityMode(
                HostProto.SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(HostProto.DiscoverabilityMode.NOT_DISCOVERABLE)
                    .build()
            )
        Log.i(TAG, "Disabled inquiry scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.NOT_CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Disabled page scan")
        val socket = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object")

        val t = thread {
            Log.i(TAG, "Connecting to socket")
            try {
                socket.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure $e")
            }
            Log.i(TAG, "Done connecting to socket")
        }

        Log.i(TAG, "Waiting for 1 seconds before page timeout")
        Thread.sleep(1000)
        Log.i(TAG, "Waited 1 second to cancel socket connection before page timeout at 5 seconds")

        Truth.assertThat(socket.isConnected).isFalse()

        Log.i(TAG, "Close socket")
        socket.close()

        Log.i(TAG, "Join the thread")
        t.join()

        Log.i(TAG, "Immediate retry won't even trigger ACL connection")

        val socket2 = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object again")

        val t2 = thread {
            Log.i(TAG, "Connecting to socket again")
            try {
                socket2.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure again $e")
            }
            Log.i(TAG, "Done connecting to socket again")
        }

        Log.i(TAG, "Waiting for 5 seconds for page timeout from previous attempt")
        Thread.sleep(5000)

        Log.i(TAG, "Close socket after 5 seconds")
        socket2.close()

        Log.i(TAG, "Join the thread")
        t2.join()

        Log.i(TAG, "Waited 5 seconds, page timeout should happen, enabling page scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Enabled page scan, reconnecting")

        startServer { serverId ->
            val (insecureSocket, _) = createConnectAcceptSocket(isSecure = false, serverId)
            insecureSocket.close()
        }
        Log.i(TAG, "Connected, test end")
    }

    @Test
    fun clientConnectToOpenServerSocketBondedInsecurePrematureClosureSuccessfulWrongConnection() {
        // Disable inquiry and page scan mode
        Log.i(TAG, "Test start")
        mBumble
            .hostBlocking()
            .setDiscoverabilityMode(
                HostProto.SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(HostProto.DiscoverabilityMode.NOT_DISCOVERABLE)
                    .build()
            )
        Log.i(TAG, "Disabled inquiry scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.NOT_CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Disabled page scan")
        val socket = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object")

        val t = thread {
            Log.i(TAG, "Connecting to socket")
            try {
                socket.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure $e")
            }
            Log.i(TAG, "Done connecting to socket")
        }

        Log.i(TAG, "Waiting for 1 seconds before page timeout")
        Thread.sleep(1000)
        Log.i(TAG, "Waited 1 second to cancel socket connection before page timeout at 5 seconds")

        Truth.assertThat(socket.isConnected).isFalse()

        Log.i(TAG, "Close socket")
        socket.close()

        Log.i(TAG, "Join the thread")
        t.join()

        Log.i(TAG, "Immediate retry won't even trigger ACL connection")

        val socket2 = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object again")

        val t2 = thread {
            Log.i(TAG, "Connecting to socket again")
            try {
                socket2.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure again $e")
            }
            Log.i(TAG, "Done connecting to socket again")
        }

        Log.i(TAG, "Waiting for 1 seconds for SDP to start")
        Thread.sleep(1000)

        Log.i(TAG, "Close socket after 1 second")
        socket2.close()

        Log.i(TAG, "Join the thread")
        t2.join()

        Log.i(
            TAG,
            "Enable page scan to make previous ACL attempt successful as we are still within 5 seconds",
        )
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Enabled page scan, reconnecting")

        Log.i(TAG, "Waiting 10 seconds for ACL to connect and disconnect due to timeout")
        Thread.sleep(10000)

        Log.i(TAG, "Reconnecting")

        startServer { serverId ->
            val (insecureSocket, _) = createConnectAcceptSocket(isSecure = false, serverId)
            insecureSocket.close()
        }
        Log.i(TAG, "Connected, test end")
    }

    @Test
    fun clientConnectToOpenServerSocketBondedInsecurePrematureClosureCancelAcl() {
        Log.i(TAG, "Enter BLE_ON mode")
        BlockingBluetoothAdapter.disable()
        BlockingBluetoothAdapter.enableBLE(true)
        Truth.assertThat(mAdapter.leState).isEqualTo(BluetoothAdapter.STATE_BLE_ON)
        BlockingBluetoothAdapter.enable()
        // Disable inquiry and page scan mode
        Log.i(TAG, "Test start")
        mBumble
            .hostBlocking()
            .setDiscoverabilityMode(
                HostProto.SetDiscoverabilityModeRequest.newBuilder()
                    .setMode(HostProto.DiscoverabilityMode.NOT_DISCOVERABLE)
                    .build()
            )
        Log.i(TAG, "Disabled inquiry scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.NOT_CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Disabled page scan")
        val socket = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object")

        val t = thread {
            Log.i(TAG, "Connecting to socket")
            try {
                socket.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure $e")
            }
            Log.i(TAG, "Done connecting to socket")
        }

        Log.i(TAG, "Waiting for 1 seconds before page timeout")
        Thread.sleep(1000)
        Log.i(TAG, "Waited 1 second to cancel socket connection before page timeout at 5 seconds")

        Truth.assertThat(socket.isConnected).isFalse()

        Log.i(TAG, "Close socket")
        socket.close()

        Log.i(TAG, "Join the thread")
        t.join()

        Log.i(TAG, "Immediate retry won't even trigger ACL connection")

        val socket2 = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(TEST_UUID))
        Log.i(TAG, "Created socket object again")

        val t2 = thread {
            Log.i(TAG, "Connecting to socket again")
            try {
                socket2.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure again $e")
            }
            Log.i(TAG, "Done connecting to socket again")
        }

        Log.i(TAG, "Waiting for 1 seconds for SDP to start")
        Thread.sleep(1000)

        Log.i(TAG, "Close socket after 1 second")
        socket2.close()

        Log.i(TAG, "Join the thread")
        t2.join()

        Log.i(TAG, "Disable and enters BLE_ON mode")
        mAdapter.disable(false)
        Thread.sleep(5000)
        Truth.assertThat(mAdapter.leState).isEqualTo(BluetoothAdapter.STATE_BLE_ON)

        Log.i(TAG, "Re-enable BT")
        BlockingBluetoothAdapter.enable()

        Log.i(TAG, "Enable page scan")
        mBumble
            .hostBlocking()
            .setConnectabilityMode(
                HostProto.SetConnectabilityModeRequest.newBuilder()
                    .setMode(HostProto.ConnectabilityMode.CONNECTABLE)
                    .build()
            )
        Log.i(TAG, "Enabled page scan, reconnecting")

        startServer { serverId ->
            val (insecureSocket, _) = createConnectAcceptSocket(isSecure = false, serverId)
            insecureSocket.close()
        }
        Log.i(TAG, "Connected, test end")
        BlockingBluetoothAdapter.disable(false)
        BlockingBluetoothAdapter.disableBLE()
    }

    private fun createConnectAcceptSocket(
        isSecure: Boolean,
        server: ServerId,
        uuid: String = TEST_UUID,
    ): Pair<BluetoothSocket, RfcommConnection> {
        val socket = createSocket(mRemoteDevice, isSecure, uuid)

        Truth.assertThat(socket.isConnected).isTrue()
        val connection = acceptSocket(server)

        return Pair(socket, connection)
    }

    private fun createConnectAcceptSocketWithoutVerification(
        isSecure: Boolean,
        server: ServerId,
        uuid: String = TEST_UUID,
    ): Pair<BluetoothSocket, RfcommConnection> {
        val socket = createSocketAsync(mRemoteDevice, isSecure, uuid)

        if (socket.isConnected) {
            Log.i(TAG, "socket connected")
            return Pair(socket, acceptSocketWithoutVerification(server))
        } else {
            Log.i(TAG, "socket not connected")
            return Pair(socket, RfcommConnection.newBuilder().setId(0).build())
        }
    }

    private fun createSocket(
        device: BluetoothDevice,
        isSecure: Boolean,
        uuid: String,
    ): BluetoothSocket {
        val socket =
            if (isSecure) {
                device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
            } else {
                device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
            }
        socket.connect()
        return socket
    }

    private fun createSocketAsync(
        device: BluetoothDevice,
        isSecure: Boolean,
        uuid: String,
    ): BluetoothSocket {
        val socket =
            if (isSecure) {
                device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
            } else {
                device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
            }

        val t = thread {
            Log.i(TAG, "Connecting to socket")
            try {
                socket.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Expect socket connection failure $e")
            }
            Log.i(TAG, "Done connecting to socket")
        }
        Log.i(TAG, "Waiting for 7 seconds for page timeout")
        Thread.sleep(6000)
        Log.i(TAG, "Waited 7 seconds to cancel socket connection after page timeout")

        Log.i(TAG, "Close socket if not connected")
        if (!socket.isConnected) {
            socket.close()
        }

        Log.i(TAG, "Join the thread")
        t.join()
        return socket
    }

    private fun acceptSocket(server: ServerId): RfcommConnection {
        val connectionResponse =
            mBumble
                .rfcommBlocking()
                .withDeadlineAfter(GRPC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .acceptConnection(
                    RfcommProto.AcceptConnectionRequest.newBuilder().setServer(server).build()
                )
        Truth.assertThat(connectionResponse.connection.id).isEqualTo(mConnectionCounter)

        mConnectionCounter += 1
        return connectionResponse.connection
    }

    private fun acceptSocketWithoutVerification(server: ServerId): RfcommConnection {
        val connectionResponse =
            mBumble
                .rfcommBlocking()
                .withDeadlineAfter(GRPC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .acceptConnection(
                    RfcommProto.AcceptConnectionRequest.newBuilder().setServer(server).build()
                )

        mConnectionCounter += 1
        return connectionResponse.connection
    }

    private fun startServer(
        name: String = TEST_SERVER_NAME,
        uuid: String = TEST_UUID,
        block: (ServerId) -> Unit,
    ) {
        val request = StartServerRequest.newBuilder().setName(name).setUuid(uuid).build()
        Truth.assertThat(request).isNotNull()
        Truth.assertThat(request.uuid).isNotNull()
        Truth.assertThat(request.uuid).isNotEmpty()
        val response = mBumble.rfcommBlocking().startServer(request)

        try {
            block(response.server)
        } finally {
            mBumble
                .rfcommBlocking()
                .withDeadlineAfter(GRPC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .stopServer(
                    RfcommProto.StopServerRequest.newBuilder().setServer(response.server).build()
                )
        }
    }

    private fun getProfileProxy(context: Context, profile: Int): BluetoothProfile {
        mAdapter.getProfileProxy(context, mProfileServiceListener, profile)
        val proxyCaptor = argumentCaptor<BluetoothProfile>()
        verify(mProfileServiceListener, timeout(GRPC_TIMEOUT.toMillis()))
            .onServiceConnected(eq(profile), proxyCaptor.capture())
        return proxyCaptor.lastValue
    }

    companion object {
        private val TAG = RfcommTest::class.java.getSimpleName()
        private val GRPC_TIMEOUT = Duration.ofSeconds(10)
        private const val TEST_UUID = "2ac5d8f1-f58d-48ac-a16b-cdeba0892d65"
        private const val SERIAL_PORT_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val TEST_SERVER_NAME = "RFCOMM Server"
    }
}
