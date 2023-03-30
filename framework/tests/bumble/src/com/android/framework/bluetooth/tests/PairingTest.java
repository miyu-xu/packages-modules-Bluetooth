package com.android.framework.bluetooth.tests;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.MacAddress;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;
import com.google.protobuf.Empty;
import com.google.protobuf.ByteString;

import io.grpc.Context.CancellableContext;
import io.grpc.Context.CancellationListener;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostGrpc;
import pandora.HostProto.ConnectRequest;
import pandora.HostProto.ConnectResponse;
import pandora.HostProto.Connection;
import pandora.HostProto.DisconnectRequest;
import pandora.HostProto.ReadLocalAddressResponse;
import pandora.HostProto.ScanRequest;
import pandora.HostProto.ScanningResponse;
import pandora.SecurityGrpc;
import pandora.SecurityProto.DeleteBondRequest;
import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;
import pandora.SecurityProto.SecureRequest;
import pandora.SecurityProto.SecureResponse;
import pandora.SecurityProto.SecurityLevel;
import pandora.SecurityStorageGrpc;


/**
 * Test cases for {@link AdvertiseManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PairingTest {

    private static final String LOG_TAG = "PairingTest";

    private static final long TIMEOUT_BLUETOOTH_DISABLE = 5;
    private static final long TIMEOUT_BLUETOOTH_ENABLE = 5;
    private static final long TIMEOUT_CONNECT = 5;
    private static final long TIMEOUT_WAIT_CONNECTION = 5;
    private static final long TIMEOUT_WAIT_PAIRING_EVENT = 5;
    private static final long TIMEOUT_WAIT_PAIRING_REQUEST = 5;
    private static final long TIMEOUT_SECURE = 5;
    private static final long TIMEOUT_BONDING = 5;
    private static final long TIMEOUT_DISCONNECTION = 1;

    private static ManagedChannel mChannel;

    private static HostGrpc.HostBlockingStub mHostBlockingStub;

    private static HostGrpc.HostStub mHostStub;

    private SecurityGrpc.SecurityStub mSecurityStub;

    private SecurityStorageGrpc.SecurityStorageBlockingStub mSecurityStorageBlockingStub;

    private StreamObserver<PairingEventAnswer> answerObserver;

    @BeforeClass
    public static void setUpClass() throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity();
    }

    @Before
    public void setUp() throws Exception {
        // FactorReset is killing the server and restart
        // all channel created before the server restarted
        // cannot be reused
        ManagedChannel channel = OkHttpChannelBuilder
              .forAddress("localhost", 7999)
              .usePlaintext()
              .build();

        HostGrpc.HostBlockingStub stub = HostGrpc.newBlockingStub(channel);
        stub.factoryReset(Empty.getDefaultInstance());

        // terminate the channel
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);

        // Create a new channel for all successive grpc calls
        mChannel = OkHttpChannelBuilder
              .forAddress("localhost", 7999)
              .usePlaintext()
              .build();

        mHostBlockingStub = HostGrpc.newBlockingStub(mChannel);
        mHostStub = HostGrpc.newStub(mChannel);
        mSecurityStub = SecurityGrpc.newStub(mChannel);
        mSecurityStorageBlockingStub = SecurityStorageGrpc.newBlockingStub(mChannel);
    }

    @After
    public void tearDown() throws Exception {
        if (mChannel != null) {
            // terminate the channel
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void classicPairing() throws Exception {
        ReadLocalAddressResponse response =
                mHostBlockingStub.withWaitForReady().readLocalAddress(Empty.getDefaultInstance());

        ByteString addrByteString = response.getAddress();
        StringBuilder refAddrBuilder = new StringBuilder();
        for (int i = 0; i < addrByteString.size(); i++) {
            if (i != 0) {
              refAddrBuilder.append(':');
            }
            refAddrBuilder.append(String.format("%02X", addrByteString.byteAt(i)));
        }
        String refAddress = refAddrBuilder.toString();

        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        MacAddress localMacAddress = MacAddress.fromString(bluetoothAdapter.getAddress());
        Log.i(LOG_TAG, "REF address:" + refAddress + " DUT address: " + localMacAddress);
        ByteString dutAddressByteString = ByteString.copyFrom(localMacAddress.toByteArray());

        // delete previous bonding from REF device
        DeleteBondRequest deleteBondReq =
                DeleteBondRequest.newBuilder().setPublic(dutAddressByteString).build();
        mSecurityStorageBlockingStub.deleteBond(deleteBondReq);

        // Need to be initiated before creating any connections
        CompletableFuture<PairingEvent> waitPairingEventFuture = refWaitPairingEvent();

        CompletableFuture<Void> waitPairingRequestFuture = waitPairingRequest(refAddress);
        CompletableFuture<BluetoothDevice> waitConnectionFuture = waitACLConnection(refAddress);
        CompletableFuture<Void> waitDisconnectionFuture = waitACLDisconnection(refAddress);
        CompletableFuture<Void> waitBondingFuture = waitBonding(refAddress);

        // Create connection
        CompletableFuture<Connection> createConnectionFuture =
            refCreateConnection(dutAddressByteString);
        Connection conn = createConnectionFuture.get();
        BluetoothDevice dev = waitConnectionFuture.get();

        // change security level
        CompletableFuture<Void> secureConnectionFuture = refSecureConnection(conn);
        PairingEvent mPairingEvent = waitPairingEventFuture.get();

        // stream pairing event answer to REF
        answerObserver.onNext(PairingEventAnswer.newBuilder().setEvent(mPairingEvent)
                .setConfirm(true).build());

        secureConnectionFuture.get();
        answerObserver.onCompleted();

        // Wait bonding
        waitBondingFuture.get();

        // Disconnect devices
        DisconnectRequest disconnectReq = DisconnectRequest.newBuilder()
                .setConnection(conn).build();
        mHostBlockingStub.disconnect(disconnectReq);


        // Wait disconnection
        waitDisconnectionFuture.get();
    }

    private CompletableFuture<Connection> refCreateConnection(ByteString addr) throws InterruptedException {
        CompletableFuture<Connection> future = new CompletableFuture<Connection>();
        // create connection
        ConnectRequest connReq = ConnectRequest.newBuilder()
                .setAddress(addr).build();
        StreamObserver<ConnectResponse> connectResponseObserver =
            new StreamObserver<ConnectResponse>(){
                public void onNext(ConnectResponse response) {
                    future.complete(response.getConnection());
                }

                @Override
                public void onError(Throwable e) {
                  future.cancel(true);
                }

                @Override
                public void onCompleted() {
                  future.cancel(true);
                }
            };

        mHostStub.connect(connReq, connectResponseObserver);
        return future;
    }

    private CompletableFuture<Void> refSecureConnection(Connection conn) throws InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        CancellableContext withCancellation = io.grpc.Context.current().withCancellation();
        SecureRequest secureReq = SecureRequest.newBuilder().setConnection(conn)
                .setClassic(SecurityLevel.LEVEL2).build();
        StreamObserver<SecureResponse> secureResponseObserver =
            new StreamObserver<SecureResponse>(){
                public void onNext(SecureResponse response) {
                }

                @Override
                public void onError(Throwable e) {
                    future.cancel(true);
                }

                @Override
                public void onCompleted() {
                    // close pairing event answer stream;
                    future.complete(null);
                }
            };

        // start security
        withCancellation.run(new Runnable() {
            public void run() {
                mSecurityStub.secure(secureReq, secureResponseObserver);
            }
        });
        return future;
    }


    private CompletableFuture<PairingEvent> refWaitPairingEvent() throws InterruptedException {
        CompletableFuture<PairingEvent> future = new CompletableFuture<PairingEvent>();
        CancellableContext withCancellation = io.grpc.Context.current().withCancellation();

        // add pairing handler
        StreamObserver<PairingEvent> pairingEventObserver = new StreamObserver<PairingEvent>(){
            public void onNext(PairingEvent event) {
                Log.i(LOG_TAG, "pairing event : " + event);
                future.complete(event);
            }

            @Override
            public void onError(Throwable e) {
                Log.i(LOG_TAG, "pairing event exception : " + e);
                future.cancel(true);
            }

            @Override
            public void onCompleted() {
                future.cancel(true);
            }
        };

        withCancellation.run(new Runnable() {
            public void run() {
                answerObserver = mSecurityStub.onPairing(pairingEventObserver);
            }
        });

        return future;
    }

    private CompletableFuture<Void> waitPairingRequest(String addr) throws InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);

        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                    BluetoothDevice dev = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice.class);
                    if (dev.getAddress().equals(addr) == false) {return;}

                    Log.i(LOG_TAG, "PAIRING REQUEST " + dev);

                    // set pairing confirmation on DUT
                    dev.setPairingConfirmation(true);

                    future.complete(null);
                }
            }
        };

        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.registerReceiver(bluetoothReceiver, intentFilter);
        return future;
    }

    private CompletableFuture<BluetoothDevice> waitACLConnection(String addr) throws InterruptedException {
        CompletableFuture<BluetoothDevice> future = new CompletableFuture<BluetoothDevice>();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    BluetoothDevice dev = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice.class);
                    if (dev.getAddress().equals(addr) == false) {return;}

                    Log.i(LOG_TAG, "ACL CONNECTED " + dev);
                    future.complete(dev);
                }
            }
        };

        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.registerReceiver(bluetoothReceiver, intentFilter);
        return future;
    }

    private CompletableFuture<Void> waitACLDisconnection(String addr) throws InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();
                if( action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    BluetoothDevice dev = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice.class);
                    if (dev.getAddress().equals(addr) == false) {return;}

                    Log.i(LOG_TAG, "ACL DISCONNECTED " + dev);
                    future.complete(null);
                }
            }
        };

        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.registerReceiver(bluetoothReceiver, intentFilter);
        return future;
    }

    private CompletableFuture<Void> waitBonding(String addr) throws InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                   BluetoothDevice dev = intent.getParcelableExtra(
                           BluetoothDevice.EXTRA_DEVICE,
                           BluetoothDevice.class);
                    if (dev.getAddress().equals(addr) == false) {return;}

                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothAdapter.ERROR);
                    Log.i(LOG_TAG, "BOND STATE CHANGED " + dev + " state: " + state);
                    if (state == BluetoothDevice.BOND_BONDED) {
                        future.complete(null);
                    }
                }
            }
        };

        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.registerReceiver(bluetoothReceiver, intentFilter);
        return future;
    }

    private void cancelContext(CancellableContext context) {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        // cancel grpc streaming
        CancellationListener cancellationListener = new CancellationListener() {
            public void cancelled(io.grpc.Context context) {
                future.complete(null);
            }
        };

        Executor executor = new Executor(){
            @Override
            public void execute(Runnable command) {
                new Thread(command).start();
            }
        };

        context.addListener(cancellationListener, executor);
        context.cancel(null);
        try {
              future.get();
        }
        catch(Exception e) {}
    }
}
