package com.android.bluetooth.myinstrumentation;

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

import com.google.protobuf.Empty;
import com.google.protobuf.ByteString;

import io.grpc.Context.CancellableContext;
import io.grpc.Context.CancellationListener;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
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

    private static BluetoothAdapter mBluetoothAdapter;

    private static ManagedChannel mChannel;

    private static HostGrpc.HostBlockingStub mHostBlockingStub;

    private static HostGrpc.HostStub mHostStub;

    private SecurityGrpc.SecurityStub mSecurityStub;

    private SecurityStorageGrpc.SecurityStorageBlockingStub mSecurityStorageBlockingStub;

    private BluetoothDevice refDev;

    private Connection refConnection;

    private ByteString mDutAddressByteString;

    private ByteString mRefAddressByteString;

    private PairingEvent mPairingEvent;

    private StreamObserver<PairingEventAnswer> answerObserver;

    @BeforeClass
    public static void setUpClass() throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity();
        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager mBluetoothManager = context.getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // latches
        CountDownLatch onLatch = new CountDownLatch(1);
        CountDownLatch offLatch = new CountDownLatch(1);

        // intent filter
        IntentFilter mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                              BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            Log.i(LOG_TAG, "Bluetooth state changed: STATE_ON");
                            onLatch.countDown();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.i(LOG_TAG, "Bluetooth state changed: STATE_TURNING_OFF");
                            offLatch.countDown();
                            break;
                    }
                }
            }
        };

        context.registerReceiver(bluetoothReceiver, mIntentFilter);

        // Stop bluetooth
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            offLatch.await();

            // TODO: b/234892968
            Thread.sleep(3000);
        }

        // Start bluetooth
        if (mBluetoothAdapter.isEnabled() == false) {
            mBluetoothAdapter.enable();
            onLatch.await();
        }

        context.unregisterReceiver(bluetoothReceiver);
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

        ReadLocalAddressResponse response =
                mHostBlockingStub.withWaitForReady().readLocalAddress(Empty.getDefaultInstance());

        mRefAddressByteString = response.getAddress();

        MacAddress localMacAddress = MacAddress.fromString(mBluetoothAdapter.getAddress());
        Log.i(LOG_TAG, "DUT address: " + localMacAddress);
        mDutAddressByteString = ByteString.copyFrom(localMacAddress.toByteArray());

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

        android.content.Context context = ApplicationProvider.getApplicationContext();
        StringBuilder refAddrBuilder = new StringBuilder();
        for (int i = 0; i < mRefAddressByteString.size(); i++) {
            if (i != 0) {
              refAddrBuilder.append(':');
            }
            refAddrBuilder.append(String.format("%02X", mRefAddressByteString.byteAt(i)));
        }
        String refAddr = refAddrBuilder.toString();

        // delete previous bonding from REF device
        DeleteBondRequest deleteBondReq =
                DeleteBondRequest.newBuilder().setPublic(mDutAddressByteString).build();
        mSecurityStorageBlockingStub.deleteBond(deleteBondReq);

        CountDownLatch waitPairingRequestLatch = new CountDownLatch(1);
        CountDownLatch waitPairingEventLatch = new CountDownLatch(1);
        // add pairing handler
        StreamObserver<PairingEvent> pairingEventObserver = new StreamObserver<PairingEvent>(){
            public void onNext(PairingEvent event) {
                Log.i(LOG_TAG, "pairing event : " + event);
                mPairingEvent = event;
                waitPairingEventLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onCompleted() {}
        };

        CancellableContext withCancellation = io.grpc.Context.current().withCancellation();

        withCancellation.run(new Runnable() {
            public void run() {
                answerObserver = mSecurityStub.onPairing(pairingEventObserver);
            }
        });

        // latches
        CountDownLatch waitConnectionLatch = new CountDownLatch(1);
        CountDownLatch waitDisconnectionLatch = new CountDownLatch(1);
        CountDownLatch waitBondedLatch = new CountDownLatch(1);

        // DUT intent filter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();

                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED: {
                        BluetoothDevice dev = intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice.class);
                        if (dev.getAddress().equals(refAddr) == false) {break;}

                        Log.i(LOG_TAG, "ACL CONNECTED " + dev);
                        refDev = dev;
                        waitConnectionLatch.countDown();
                        break;
                    }

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                        BluetoothDevice dev = intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice.class);
                        if (dev.getAddress().equals(refAddr) == false) {break;}

                        Log.i(LOG_TAG, "ACL DISCONNECTED " + dev);
                        waitDisconnectionLatch.countDown();
                        break;
                    }

                    case BluetoothDevice.ACTION_PAIRING_REQUEST: {
                        BluetoothDevice dev = intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice.class);
                        if (dev.getAddress().equals(refAddr) == false) {break;}

                        Log.i(LOG_TAG, "PAIRING REQUEST " + dev);

                        // set pairing confirmation on DUT
                        dev.setPairingConfirmation(true);
                        waitPairingRequestLatch.countDown();
                        break;
                    }

                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                        BluetoothDevice dev = intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice.class);
                        if (dev.getAddress().equals(refAddr) == false) {break;}

                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                BluetoothAdapter.ERROR);
                        Log.i(LOG_TAG, "BOND STATE CHANGED " + dev + " state: " + state);
                        if (state == BluetoothDevice.BOND_BONDED) {
                            waitBondedLatch.countDown();
                        }
                        break;
                    }
                }
            }
        };

        context.registerReceiver(bluetoothReceiver, intentFilter);

        // create connection
        CountDownLatch connectLatch = new CountDownLatch(1);
        ConnectRequest connReq = ConnectRequest.newBuilder()
                .setAddress(mDutAddressByteString).build();
        StreamObserver<ConnectResponse> connectResponseObserver =
            new StreamObserver<ConnectResponse>(){
                public void onNext(ConnectResponse response) {
                    refConnection = response.getConnection();
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onCompleted() {
                    connectLatch.countDown();
                }
            };

        mHostStub.connect(connReq, connectResponseObserver);

        connectLatch.await();

        waitConnectionLatch.await();

        // change security level
        CountDownLatch secureLatch = new CountDownLatch(1);
        SecureRequest secureReq = SecureRequest.newBuilder().setConnection(refConnection)
                .setClassic(SecurityLevel.LEVEL2).build();
        StreamObserver<SecureResponse> secureResponseObserver =
            new StreamObserver<SecureResponse>(){
                public void onNext(SecureResponse response) {
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onCompleted() {
                    // close pairing event answer stream;
                    answerObserver.onCompleted();
                    secureLatch.countDown();
                }
            };


        // start security
        withCancellation.run(new Runnable() {
            public void run() {
                mSecurityStub.secure(secureReq, secureResponseObserver);
            }
        });

        waitPairingEventLatch.await();

        waitPairingRequestLatch.await();

        // stream pairing event answer to REF
        answerObserver.onNext(PairingEventAnswer.newBuilder().setEvent(mPairingEvent)
                .setConfirm(true).build());

        secureLatch.await();

        waitBondedLatch.await();

        // Disconnect devices
        DisconnectRequest disconnectReq = DisconnectRequest.newBuilder()
                .setConnection(refConnection).build();
        mHostBlockingStub.disconnect(disconnectReq);

        // TODO: why disconnect from android is not working?
        //refDev.disconnect();

        waitDisconnectionLatch.await();

        context.unregisterReceiver(bluetoothReceiver);

        // cancel grpc streaming
        CountDownLatch cancelLatch = new CountDownLatch(1);
        CancellationListener cancellationListener = new CancellationListener() {
            public void cancelled(io.grpc.Context context) {
                cancelLatch.countDown();
            }
        };

        Executor executor = new Executor(){
            @Override
            public void execute(Runnable command) {
                new Thread(command).start();
            }
        };

        withCancellation.addListener(cancellationListener, executor);
        withCancellation.cancel(null);
        cancelLatch.await();
    }
}
