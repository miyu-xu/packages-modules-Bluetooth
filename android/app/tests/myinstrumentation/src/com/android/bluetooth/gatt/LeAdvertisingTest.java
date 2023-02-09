package com.android.bluetooth.myinstrumentation.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
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
import pandora.HostProto.ScanRequest;
import pandora.HostProto.ScanningResponse;


/**
 * Test cases for {@link AdvertiseManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LeAdvertisingTest {

    private static final String LOG_TAG = "LeAdvertisingTest";

    private static String mAdvertiserAddress;

    private static ManagedChannel mChannel;

    private static HostGrpc.HostBlockingStub mHostBlockingStub;

    private static HostGrpc.HostStub mHostStub;

    @BeforeClass
    public static void setUpClass() throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity();
        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        IntentFilter mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        CountDownLatch onLatch = new CountDownLatch(1);
        CountDownLatch offLatch = new CountDownLatch(1);

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
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            offLatch.await();

            // TODO: b/234892968
            Thread.sleep(3000);
        }

        // Start bluetooth
        if (bluetoothAdapter.isEnabled() == false) {
            bluetoothAdapter.enable();
            onLatch.await();
            Log.i(LOG_TAG, "bluetooth enabled");
        }


        // Start advertising
        BluetoothLeAdvertiser mLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder().build();
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();
        AdvertiseData scanResponse = new AdvertiseData.Builder().build();

        CountDownLatch advertisingLatch = new CountDownLatch(1);

        AdvertisingSetCallback mAdvertisingSetCallback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower,
                    int status) {
                Log.i(LOG_TAG, "onAdvertisingSetStarted "+ "advertisingSet:" + advertisingSet
                        + " txPower:" + txPower + " status:"+ status);
                advertisingSet.getOwnAddress();
            }

            @Override
            public void onOwnAddressRead(AdvertisingSet advertisingSet, int addressType,
                    String address) {
                Log.i(LOG_TAG, "onOwnAddressRead " + "advertisingSet:" + advertisingSet
                        + " addressType:" + addressType + " address:" + address);
                mAdvertiserAddress = address;
                advertisingSet.enableAdvertising(true, 10, 0);
            }

            @Override
            public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enabled,
                    int status) {
                Log.i(LOG_TAG, "onAdvertisingEnabled " + " enabled:" + enabled
                        + " status:" + status);
                advertisingLatch.countDown();
            }
        };

        mLeAdvertiser.startAdvertisingSet(parameters, advertiseData, scanResponse,
          null, null, 0, 0, mAdvertisingSetCallback);

        // wait for advertising set enabled
        advertisingLatch.await();

        // unregister receiver
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
        mHostBlockingStub.withWaitForReady().readLocalAddress(Empty.getDefaultInstance());
    }

    @After
    public void tearDown() throws Exception {
        if (mChannel != null) {
            // terminate the channel
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void advertisingSet() throws Exception {
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
