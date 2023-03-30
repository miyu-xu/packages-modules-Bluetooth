package com.android.framework.bluetooth.tests;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.Context.CancellableContext;
import io.grpc.Context.CancellationListener;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostGrpc;
import pandora.HostProto.ScanRequest;
import pandora.HostProto.ScanningResponse;


/**
 * Test cases for {@link AdvertiseManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LeAdvertisingTest {

    private static final String LOG_TAG = "LeAdvertisingTest";

    private static final int TIMEOUT_ADVERTISING_MS = 1000;
    private static final long TIMEOUT_FUTURE_ADV_S = 1;
    private static final long TIMEOUT_FUTURE_SCAN_S = 1;

    private static ManagedChannel mChannel;

    private static HostGrpc.HostBlockingStub mHostBlockingStub;

    private static HostGrpc.HostStub mHostStub;

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
        mHostBlockingStub.withWaitForReady().readLocalAddress(Empty.getDefaultInstance());
    }

    @After
    public void tearDown() throws Exception {
        // terminate the channel
        mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void advertisingSet() throws Exception {
        CompletableFuture<String> advFuture = startAdvertising();
        // wait for advertising enabled
        String advAddress = advFuture.get(TIMEOUT_FUTURE_ADV_S, TimeUnit.SECONDS);

        // start scanning
        // wait for scan result;
        CompletableFuture<Void> scanFuture = scan(advAddress);
        scanFuture.get(TIMEOUT_FUTURE_SCAN_S, TimeUnit.SECONDS);
    }

    private CompletableFuture<String> startAdvertising() throws InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<String>();

        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Start advertising
        BluetoothLeAdvertiser mLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder().build();
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();
        AdvertiseData scanResponse = new AdvertiseData.Builder().build();
        AdvertisingSetCallback mAdvertisingSetCallback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower,
                    int status) {
                Log.i(LOG_TAG, "onAdvertisingSetStarted " + " txPower:" + txPower
                    + " status:"+ status);
                advertisingSet.enableAdvertising(true, TIMEOUT_ADVERTISING_MS, 0);
            }
            @Override
            public void onOwnAddressRead(AdvertisingSet advertisingSet, int addressType,
                    String address) {
                Log.i(LOG_TAG, "onOwnAddressRead " + " addressType:" + addressType
                    + " address:" + address);
                future.complete(address);
            }
            @Override
            public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enabled,
                    int status) {
                Log.i(LOG_TAG, "onAdvertisingEnabled " + " enabled:" + enabled
                        + " status:" + status);
                advertisingSet.getOwnAddress();
            }
        };
        mLeAdvertiser.startAdvertisingSet(parameters, advertiseData, scanResponse,
          null, null, 0, 0, mAdvertisingSetCallback);

        return future;
    }

    private CompletableFuture<Void> scan(String address) throws InterruptedException {
        final CompletableFuture<Void> future = new CompletableFuture<Void>();
        CancellableContext withCancellation = io.grpc.Context.current().withCancellation();

        ScanRequest request = ScanRequest.newBuilder().build();
        StreamObserver<ScanningResponse> responseObserver = new StreamObserver<ScanningResponse>(){
            public void onNext(ScanningResponse response) {
                StringBuilder result = new StringBuilder();
                ByteString bs = response.getRandom();
                for (int i = 0; i < bs.size(); i++) {
                    if (i != 0) {
                      result.append(':');
                    }
                    result.append(String.format("%02X", bs.byteAt(i)));
                }
                Log.i(LOG_TAG,"scan observer: scan response address: " + result);

                if (result.toString().equals(address)) {
                    future.complete(null);
                    cancelContext(withCancellation);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG,"scan observer: on error " + e);
                future.cancel(true);
                cancelContext(withCancellation);
            }

            @Override
            public void onCompleted() {
                Log.i(LOG_TAG,"scan observer: on completed");
                future.cancel(true);
                cancelContext(withCancellation);
            }
        };

        withCancellation.run(new Runnable() {
            public void run() {
                mHostStub.scan(request, responseObserver);
            }
        });

        return future;
    }

    private void cancelContext(CancellableContext context) {
        final CompletableFuture<Void> future = new CompletableFuture<Void>();
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
