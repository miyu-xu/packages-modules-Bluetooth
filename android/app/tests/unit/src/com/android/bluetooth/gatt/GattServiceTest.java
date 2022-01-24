package com.android.bluetooth.gatt;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import android.content.Context;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.os.ParcelUuid;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.R;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test cases for {@link GattService}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class GattServiceTest {
    private static final int TIMES_UP_AND_DOWN = 3;
    private Context mTargetContext;
    private GattService mService;
    private static final ParcelUuid UUID =
            ParcelUuid.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    private static final int REG_ID = -1;
    private static final int ADVERTISER_ID = 1;
    private static final int DURATION = 0;
    private static final int MAX_EXT_ADV_EVENTS = 0;
    private static final int MANUFACTURE_ID = 224;
    @Rule public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Mock private AdapterService mAdapterService;

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        Assume.assumeTrue("Ignore test when GattService is not enabled",
                mTargetContext.getResources().getBoolean(R.bool.profile_supported_gatt));
        MockitoAnnotations.initMocks(this);
        TestUtils.setAdapterService(mAdapterService);
        doReturn(true).when(mAdapterService).isStartedProfile(anyString());
        TestUtils.startService(mServiceRule, GattService.class);
        mService = GattService.getGattService();
        Assert.assertNotNull(mService);
    }

    @After
    public void tearDown() throws Exception {
        if (!mTargetContext.getResources().getBoolean(R.bool.profile_supported_gatt)) {
            return;
        }
        doReturn(false).when(mAdapterService).isStartedProfile(anyString());
        TestUtils.stopService(mServiceRule, GattService.class);
        mService = GattService.getGattService();
        Assert.assertNull(mService);
        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void testInitialize() {
        Assert.assertNotNull(GattService.getGattService());
    }

    @Test
    public void testServiceUpAndDown() throws Exception {
        for (int i = 0; i < TIMES_UP_AND_DOWN; i++) {
            GattService gattService = GattService.getGattService();
            doReturn(false).when(mAdapterService).isStartedProfile(anyString());
            TestUtils.stopService(mServiceRule, GattService.class);
            mService = GattService.getGattService();
            Assert.assertNull(mService);
            gattService.cleanup();
            TestUtils.clearAdapterService(mAdapterService);
            reset(mAdapterService);
            TestUtils.setAdapterService(mAdapterService);
            doReturn(true).when(mAdapterService).isStartedProfile(anyString());
            TestUtils.startService(mServiceRule, GattService.class);
            mService = GattService.getGattService();
            Assert.assertNotNull(mService);
        }
    }

    @Test
    public void testParseBatchTimestamp() {
        long timestampNanos = mService.parseTimestampNanos(new byte[]{
                -54, 7
        });
        Assert.assertEquals(99700000000L, timestampNanos);
    }

    @Test
    public void testAdvertisingLogger() throws Exception {
        byte[] testData = new byte[1];
        StringBuilder sb = new StringBuilder();
        AdvertisingSetParameters.Builder parametersBuilder = new AdvertisingSetParameters.Builder();
        AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData.Builder();
        AdvertiseData.Builder scanResponseBuilder = new AdvertiseData.Builder();
        PeriodicAdvertisingParameters.Builder periodicParametersBuilder =
                new PeriodicAdvertisingParameters.Builder();
        AdvertiseData.Builder periodicDataBuilder = new AdvertiseData.Builder();

        parametersBuilder.setConnectable(true);
        parametersBuilder.setScannable(false);
        parametersBuilder.setPrimaryPhy(BluetoothDevice.PHY_LE_1M);
        testData[0] = 55;
        advertiseDataBuilder.setIncludeDeviceName(true);
        advertiseDataBuilder.addManufacturerData(MANUFACTURE_ID, testData);
        advertiseDataBuilder.addServiceUuid(UUID);
        scanResponseBuilder.setIncludeTxPowerLevel(true);
        scanResponseBuilder.addServiceData(UUID, testData);
        scanResponseBuilder.addServiceSolicitationUuid(UUID);

        mService.mAdvertiserMap.add(REG_ID, null, mService);
        mService.mAdvertiserMap.recordAdvertiseStart(REG_ID, parametersBuilder.build(),
                advertiseDataBuilder.build(), scanResponseBuilder.build(),
                periodicParametersBuilder.build(), periodicDataBuilder.build(),
                DURATION, MAX_EXT_ADV_EVENTS);
        mService.mAdvertiserMap.setAdvertiserIdByRegId(REG_ID, ADVERTISER_ID);
        for (int i = 0; i < 6; i++) {
            Thread.sleep(1000);
            mService.mAdvertiserMap.enableAdvertisingSet(ADVERTISER_ID,
                    false, DURATION, MAX_EXT_ADV_EVENTS);
            Thread.sleep(3000);
            mService.mAdvertiserMap.enableAdvertisingSet(ADVERTISER_ID,
                    true, DURATION, MAX_EXT_ADV_EVENTS);
        }
        mService.mAdvertiserMap.recordAdvertiseStop(ADVERTISER_ID);
        mService.mAdvertiserMap.dumpAdvertiser(sb);

        assertThat(sb.toString()).contains("Interval(0.625ms)                              : " +
                parametersBuilder.build().getInterval());
        assertThat(sb.toString()).contains("TX POWER(dbm)                                  : " +
                parametersBuilder.build().getTxPowerLevel());
        assertThat(sb.toString()).contains("Primary Phy                                    : LE_1M");
        assertThat(sb.toString()).contains("Connectable                                    : " +
                parametersBuilder.build().isConnectable());
        assertThat(sb.toString()).contains("Scannable                                      : " +
                parametersBuilder.build().isScannable());
        assertThat(sb.toString()).contains("Include Device Name                          : " +
                advertiseDataBuilder.build().getIncludeDeviceName());
        assertThat(sb.toString()).contains("Include Tx Power Level                       : " +
                scanResponseBuilder.build().getIncludeTxPowerLevel());
        //after this tester enables/disables advertisingset 6 times, there are 5 records but 6.
        assertThat(sb.toString()).contains("5:");
        assertThat(sb.toString()).doesNotContain("6:");
        //check the Manufacturer Data
        assertThat(sb.toString()).contains("[e0, 37]");
        //check the service UUID
        assertThat(sb.toString()).contains("[0000180f-0000-1000-8000-00805f9b34fb]");
        //check the service data
        assertThat(sb.toString()).contains("[0000180f-0000-1000-8000-00805f9b34fb, 1]");
    }
}
