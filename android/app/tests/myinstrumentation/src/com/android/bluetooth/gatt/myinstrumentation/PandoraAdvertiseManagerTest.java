package com.android.bluetooth.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
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
import pandora.HostProto.ScanRequest;
import pandora.HostProto.ScanningResponse;


/**
 * Test cases for {@link AdvertiseManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PandoraAdvertiseManagerTest {

    private static final String LOG_TAG = "BluetoothPandoraTest";

    private static AdvertiseManager mAdvertiseManager;

    private static String mAdvertiserAddress;

    private static int mAdvertiserId;

    private static ManagedChannel mChannel;

    private static HostGrpc.HostBlockingStub mHostBlockingStub;

    private static HostGrpc.HostStub mHostStub;

    private static BroadcastReceiver mBluetoothReceiver;

    private static CountDownLatch mAdvertisingLatch;

    private static IAdvertisingSetCallback mAdvertisingSetCallback = new IAdvertisingSetCallback.Stub() {
            @Override
            public void onAdvertisingSetStarted(int advertiserId, int txPower, int status) {
                Log.i(LOG_TAG, "onAdvertisingSetStarted "+ "advId:" + advertiserId + " txPower:" + txPower + " status:"+ status);
                mAdvertiserId = advertiserId;
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onOwnAddressRead(int advertiserId, int addressType, String address) {
                Log.i(LOG_TAG, "onOwnAddressRead " + " advId:" + advertiserId + " addressType:" + addressType + " address:" + address);
                mAdvertiserAddress = address;
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onAdvertisingSetStopped(int advertiserId) {
                Log.i(LOG_TAG, "onAdvertisingSetStopped " + "advId:" + advertiserId);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onAdvertisingEnabled(int advertiserId, boolean enabled, int status) {
                Log.i(LOG_TAG, "onAdvertisingEnabled " + " enabled:" + enabled + " status:" + status);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onPeriodicAdvertisingEnabled(int advertiserId, boolean enable, int status) {
                Log.i(LOG_TAG, "onPeriodicAdvertisingEnabled advId:" + advertiserId + " enable:" + enable + " status:" + status);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onAdvertisingDataSet(int advertiserId, int status) {
                Log.i(LOG_TAG, "onAdvertisingDataSet " + " advId:" + advertiserId + " status:" + status);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onScanResponseDataSet(int advertiserId, int status) {
                Log.i(LOG_TAG, "onScanResponseDataSet advId:"+advertiserId + " status:"+ status);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onAdvertisingParametersUpdated(int advertiserId, int txPower, int status) {
                Log.i(LOG_TAG, "onAdvertisingParametersUpdated advId:" + advertiserId + " txPower:" + txPower + " status:" + status);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onPeriodicAdvertisingParametersUpdated(int advertiserId, int status) {
                Log.i(LOG_TAG, "onPeriodicAdvertisingParametersUpdated advId:" + advertiserId + " status:" + status);
                mAdvertisingLatch.countDown();
            }

            @Override
            public void onPeriodicAdvertisingDataSet(int advertiserId, int status) {
                Log.i(LOG_TAG, "onPeriodicAdvertisingDataSet advId:" + advertiserId + " status:" + status);
                mAdvertisingLatch.countDown();
            }

            };

    @BeforeClass
    public static void setUpClass() throws Exception {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

        IntentFilter mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        CountDownLatch latch = new CountDownLatch(1);

        mBluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            Log.i(LOG_TAG, "Bluetooth state changed: STATE_ON");
                            latch.countDown();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.i(LOG_TAG, "Bluetooth state changed: STATE_TURNING_OFF");
                            break;
                    }
                }
            }
        };

        context.registerReceiver(mBluetoothReceiver, mIntentFilter);

        mBluetoothAdapter.enableBLE();

        latch.await();

        mAdvertiseManager = GattService.getGattService().mAdvertiseManager;
        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder().build();
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();
        AdvertiseData scanResponse = new AdvertiseData.Builder().build();
        PeriodicAdvertisingParameters periodicParameters =
                new PeriodicAdvertisingParameters.Builder().build();
        AdvertiseData periodicData = new AdvertiseData.Builder().build();
        int duration = 10;
        int maxExtAdvEvents = 15;

        mAdvertisingLatch = new CountDownLatch(1);

        mAdvertiseManager.startAdvertisingSet(parameters, advertiseData, scanResponse,
          periodicParameters, periodicData, duration, maxExtAdvEvents, mAdvertisingSetCallback);

        // wait for advertising set started
        mAdvertisingLatch.await();

        mAdvertisingLatch = new CountDownLatch(1);
        mAdvertiseManager.getOwnAddress(mAdvertiserId);

        // wait for own address read
        mAdvertisingLatch.await();
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

        // Create a new channel for all succesive grpc calls
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
        if (mChannel != null) {
            // terminate the channel
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.unregisterReceiver(mBluetoothReceiver);
    }

    @Test
    public void advertisingSet() throws Exception {
        mAdvertisingLatch = new CountDownLatch(1);
        boolean enable = true;
        int duration = 60;
        int maxExtAdvEvents = 100;
        mAdvertiseManager.enableAdvertisingSet(mAdvertiserId, enable, duration, maxExtAdvEvents);

        // wait for advertising set enabled
        mAdvertisingLatch.await();

        CountDownLatch latch = new CountDownLatch(1);

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

                if (result.toString().equals(mAdvertiserAddress)) {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG,"scan observer: on error " + e);
            }

            @Override
            public void onCompleted() {
                Log.i(LOG_TAG,"scan observer: on completed");
            }
        };

        CancellableContext withCancellation = io.grpc.Context.current().withCancellation();
        withCancellation.run(new Runnable() {
            public void run() {
                mHostStub.scan(request, responseObserver);
            }
        });

        // wait for scan result;
        latch.await();

        // cancel grpc streaming
        CountDownLatch cancelLatch = new CountDownLatch(1);
        CancellationListener cancellationListener = new CancellationListener() {
            public void cancelled(io.grpc.Context context) {
                Log.i(LOG_TAG,"context cancelled");
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
