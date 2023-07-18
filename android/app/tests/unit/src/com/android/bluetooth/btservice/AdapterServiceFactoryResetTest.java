/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.bluetooth.btservice;

import static android.Manifest.permission.BLUETOOTH_SCAN;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothCallback;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.BatteryStatsManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.test.TestLooper;
import android.provider.Settings;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.a2dpsink.A2dpSinkService;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;
import com.android.bluetooth.bas.BatteryService;
import com.android.bluetooth.bass_client.BassClientService;
import com.android.bluetooth.csip.CsipSetCoordinatorService;
import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.hap.HapClientService;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.hfpclient.HeadsetClientService;
import com.android.bluetooth.hid.HidDeviceService;
import com.android.bluetooth.hid.HidHostService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.bluetooth.map.BluetoothMapService;
import com.android.bluetooth.mapclient.MapClientService;
import com.android.bluetooth.mcp.McpService;
import com.android.bluetooth.opp.BluetoothOppService;
import com.android.bluetooth.pan.PanService;
import com.android.bluetooth.pbap.BluetoothPbapService;
import com.android.bluetooth.pbapclient.PbapClientService;
import com.android.bluetooth.sap.SapService;
import com.android.bluetooth.tbs.TbsService;
import com.android.bluetooth.vc.VolumeControlService;
import com.android.internal.app.IBatteryStats;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.HashMap;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AdapterServiceFactoryResetTest {
    private static final String TAG = AdapterServiceFactoryResetTest.class.getSimpleName();

    private AdapterService mAdapterService;
    private AdapterService.AdapterServiceBinder mServiceBinder;

    private @Spy Context mMockContext =
            new ContextWrapper(ApplicationProvider.getApplicationContext());
    private @Mock ApplicationInfo mMockApplicationInfo;
    private @Mock AlarmManager mMockAlarmManager;
    private @Mock Resources mMockResources;
    private @Mock UserManager mMockUserManager;
    private @Mock DevicePolicyManager mMockDevicePolicyManager;
    private @Mock IBluetoothCallback mIBluetoothCallback;
    private @Mock Binder mBinder;
    private @Mock AudioManager mAudioManager;
    private @Mock android.app.Application mApplication;
    private @Mock MetricsLogger mMockMetricsLogger;

    // BatteryStatsManager is final and cannot be mocked with regular mockito, so just mock the
    // underlying binder calls.
    final BatteryStatsManager mBatteryStatsManager =
            new BatteryStatsManager(mock(IBatteryStats.class));

    private static final int CONTEXT_SWITCH_MS = 100;
    private static final int PROFILE_SERVICE_TOGGLE_TIME_MS = 200;
    private static final int GATT_START_TIME_MS = 1000;
    private static final int ONE_SECOND_MS = 1000;
    private static final int NATIVE_INIT_MS = 8000;
    private static final int NATIVE_DISABLE_MS = 8000;

    private final AttributionSource mAttributionSource = new AttributionSource.Builder(
            Process.myUid()).build();

    private TestLooper mLooper;

    private PackageManager mMockPackageManager;
    private MockContentResolver mMockContentResolver;
    private HashMap<String, HashMap<String, String>> mAdapterConfig;
    private int mForegroundUserId;

    private void configureEnabledProfiles() {
        Log.e(TAG, "configureEnabledProfiles");
        Config.setProfileEnabled(PanService.class, true);
        Config.setProfileEnabled(BluetoothPbapService.class, true);
        Config.setProfileEnabled(GattService.class, true);

        Config.setProfileEnabled(A2dpService.class, false);
        Config.setProfileEnabled(A2dpSinkService.class, false);
        Config.setProfileEnabled(AvrcpTargetService.class, false);
        Config.setProfileEnabled(AvrcpControllerService.class, false);
        Config.setProfileEnabled(BassClientService.class, false);
        Config.setProfileEnabled(BatteryService.class, false);
        Config.setProfileEnabled(CsipSetCoordinatorService.class, false);
        Config.setProfileEnabled(HapClientService.class, false);
        Config.setProfileEnabled(HeadsetService.class, false);
        Config.setProfileEnabled(HeadsetClientService.class, false);
        Config.setProfileEnabled(HearingAidService.class, false);
        Config.setProfileEnabled(HidDeviceService.class, false);
        Config.setProfileEnabled(HidHostService.class, false);
        Config.setProfileEnabled(LeAudioService.class, false);
        Config.setProfileEnabled(TbsService.class, false);
        Config.setProfileEnabled(BluetoothMapService.class, false);
        Config.setProfileEnabled(MapClientService.class, false);
        Config.setProfileEnabled(McpService.class, false);
        Config.setProfileEnabled(BluetoothOppService.class, false);
        Config.setProfileEnabled(PbapClientService.class, false);
        Config.setProfileEnabled(SapService.class, false);
        Config.setProfileEnabled(VolumeControlService.class, false);
    }

    @BeforeClass
    public static void setupClass() {
        Log.e(TAG, "setupClass");
        // Bring native layer up and down to make sure config files are properly loaded
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Assert.assertNotNull(Looper.myLooper());
        AdapterService adapterService = new AdapterService(Looper.myLooper());
        adapterService.initNative(false /* is_restricted */, false /* is_common_criteria_mode */,
                0 /* config_compare_result */, new String[0], false, "");
        adapterService.cleanupNative();
        HashMap<String, HashMap<String, String>> adapterConfig = TestUtils.readAdapterConfig();
        Assert.assertNotNull(adapterConfig);
        Assert.assertNotNull("metrics salt is null: " + adapterConfig.toString(),
                AdapterServiceTest.getMetricsSalt(adapterConfig));
    }

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        Log.e(TAG, "setUp()");
        MockitoAnnotations.initMocks(this);
        mLooper = new TestLooper();
        Handler handler = new Handler(mLooper.getLooper());

        // Post the creation of AdapterService since it rely on Looper.myLooper()
        handler.post(() -> mAdapterService = new AdapterService(mLooper.getLooper()));
        mLooper.dispatchAll();
        Assert.assertNotNull(mAdapterService);

        mServiceBinder = new AdapterService.AdapterServiceBinder(mAdapterService);
        mMockPackageManager = mock(PackageManager.class);
        when(mMockPackageManager.getPermissionInfo(any(), anyInt()))
                .thenReturn(new PermissionInfo());

        mMockContentResolver = new MockContentResolver(InstrumentationRegistry.getTargetContext());
        mMockContentResolver.addProvider(Settings.AUTHORITY, new MockContentProvider() {
            @Override
            public Bundle call(String method, String request, Bundle args) {
                return Bundle.EMPTY;
            }
        });

        when(mMockContext.getApplicationInfo()).thenReturn(mMockApplicationInfo);
        when(mMockContext.getContentResolver()).thenReturn(mMockContentResolver);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);
        when(mMockContext.createContextAsUser(UserHandle.SYSTEM, /* flags= */ 0)).thenReturn(
                mMockContext);
        when(mMockContext.getResources()).thenReturn(mMockResources);
        when(mMockContext.getUserId()).thenReturn(Process.BLUETOOTH_UID);
        when(mMockContext.getPackageManager()).thenReturn(mMockPackageManager);
        when(mMockContext.getSystemService(eq(Context.USER_SERVICE))).thenReturn(mMockUserManager);
        when(mMockContext.getSystemServiceName(UserManager.class)).thenReturn(Context.USER_SERVICE);
        when(mMockContext.getSystemService(eq(Context.DEVICE_POLICY_SERVICE)))
                .thenReturn(mMockDevicePolicyManager);
        when(mMockContext.getSystemServiceName(DevicePolicyManager.class))
                .thenReturn(Context.DEVICE_POLICY_SERVICE);
        when(mMockContext.getSystemService(eq(Context.ALARM_SERVICE)))
                .thenReturn(mMockAlarmManager);
        when(mMockContext.getSystemServiceName(AlarmManager.class))
                .thenReturn(Context.ALARM_SERVICE);
        when(mMockContext.getSystemService(eq(Context.AUDIO_SERVICE))).thenReturn(mAudioManager);
        when(mMockContext.getSystemServiceName(AudioManager.class))
                .thenReturn(Context.AUDIO_SERVICE);
        when(mMockContext.getSystemService(eq(Context.BATTERY_STATS_SERVICE)))
                .thenReturn(mBatteryStatsManager);
        when(mMockContext.getSystemServiceName(BatteryStatsManager.class))
                .thenReturn(Context.BATTERY_STATS_SERVICE);
        when(mMockContext.getSharedPreferences(anyString(), anyInt()))
                .thenReturn(InstrumentationRegistry.getTargetContext()
                        .getSharedPreferences("AdapterServiceTestPrefs", Context.MODE_PRIVATE));

        when(mMockContext.getAttributionSource()).thenReturn(mAttributionSource);

        // Sets the foreground user id to match that of the tests (restored in tearDown)
        mForegroundUserId = Utils.getForegroundUserId();
        int callingUid = Binder.getCallingUid();
        UserHandle callingUser = UserHandle.getUserHandleForUid(callingUid);
        Utils.setForegroundUserId(callingUser.getIdentifier());

        when(mMockDevicePolicyManager.isCommonCriteriaModeEnabled(any())).thenReturn(false);

        when(mIBluetoothCallback.asBinder()).thenReturn(mBinder);

        doReturn(Process.BLUETOOTH_UID).when(mMockPackageManager)
                .getPackageUidAsUser(any(), anyInt(), anyInt());

        when(mMockMetricsLogger.init(any())).thenReturn(true);
        when(mMockMetricsLogger.close()).thenReturn(true);

        configureEnabledProfiles();
        Config.init(mMockContext);

        mAdapterService.setMetricsLogger(mMockMetricsLogger);

        // Attach a context to the service for permission checks.
        mAdapterService.attach(mMockContext, null, null, null, mApplication, null);
        mAdapterService.onCreate();

        mLooper.dispatchAll();

        mServiceBinder.registerCallback(mIBluetoothCallback, mAttributionSource);

        mAdapterConfig = TestUtils.readAdapterConfig();
        Assert.assertNotNull(mAdapterConfig);
    }

    @After
    public void tearDown() {
        Log.e(TAG, "tearDown()");

        // Enable the stack to re-create the config. Next tests rely on it.
        doEnable();

        // Restores the foregroundUserId to the ID prior to the test setup
        Utils.setForegroundUserId(mForegroundUserId);

        doDisable();

        mServiceBinder.unregisterCallback(mIBluetoothCallback, mAttributionSource);
        mAdapterService.cleanup();
        mLooper.dispatchAll();
    }

    private void verifyStateChange(int prevState, int currState, int callNumber) {
        try {
            verify(mIBluetoothCallback, times(callNumber))
                    .onBluetoothStateChange(prevState, currState);
        } catch (RemoteException e) {
            // the mocked onBluetoothStateChange doesn't throw RemoteException
        }
    }
    private void verifyStateChange(int prevState, int currState, int callNumber, int timeoutMs) {
        try {
            verify(mIBluetoothCallback, timeout(timeoutMs).times(callNumber))
                .onBluetoothStateChange(prevState, currState);
        } catch (RemoteException e) {
            // the mocked onBluetoothStateChange doesn't throw RemoteException
        }
    }

    private void doEnable() {
        Log.e(TAG, "doEnable() start");
        Assert.assertFalse(mAdapterService.getState() == BluetoothAdapter.STATE_ON);

        mAdapterService.enable(false);
        mLooper.dispatchNext(); // BLE_TURN_ON

        verifyStateChange(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_BLE_TURNING_ON, 1);

        // Need to wait for some execution code in native. Autodispatch will execute the message
        mLooper.startAutoDispatch();
        verifyStateChange(
                BluetoothAdapter.STATE_BLE_TURNING_ON,
                BluetoothAdapter.STATE_BLE_ON,
                1,
                NATIVE_INIT_MS);
        // stop auto dispatch ASAP
        mLooper.stopAutoDispatch();

        mServiceBinder.startBrEdr(mAttributionSource);
        mLooper.dispatchNext(); // USER_TURN_ON

        verifyStateChange(BluetoothAdapter.STATE_BLE_ON, BluetoothAdapter.STATE_TURNING_ON, 1);

        // Start Mock PBAP and PAN services
        verify(mMockContext, times(2)).startService(any());

        mLooper.startAutoDispatch(); // Pbap has its own handler that we do not control here yet
        verifyStateChange(
                BluetoothAdapter.STATE_TURNING_ON,
                BluetoothAdapter.STATE_ON,
                1,
                PROFILE_SERVICE_TOGGLE_TIME_MS);
        mLooper.stopAutoDispatch(); // stop auto disaptch ASAP

        verify(mMockContext, timeout(CONTEXT_SWITCH_MS).times(2))
                .sendBroadcast(any(), eq(BLUETOOTH_SCAN), any(Bundle.class));
        final int scanMode = mServiceBinder.getScanMode(mAttributionSource);
        Assert.assertTrue(scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE
                || scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        Assert.assertTrue(mAdapterService.getState() == BluetoothAdapter.STATE_ON);

        Log.e(TAG, "doEnable() complete success");
    }

    private void doDisable() {
        Assert.assertTrue(mAdapterService.getState() == BluetoothAdapter.STATE_ON);

        mAdapterService.disable();
        mLooper.dispatchNext(); // USER_TURN_OFF

        // Need to wait for some execution code in native. Autodispatch will execute all the message
        mLooper.startAutoDispatch();

        verifyStateChange(
                BluetoothAdapter.STATE_ON,
                BluetoothAdapter.STATE_TURNING_OFF,
                1,
                CONTEXT_SWITCH_MS);

        // Stop PBAP and PAN services
        verify(mMockContext, timeout(ONE_SECOND_MS).times(4)).startService(any());

        verifyStateChange(
                BluetoothAdapter.STATE_TURNING_OFF,
                BluetoothAdapter.STATE_BLE_ON,
                1,
                PROFILE_SERVICE_TOGGLE_TIME_MS);

        mLooper.stopAutoDispatch();
        mServiceBinder.stopBle(mAttributionSource);
        // process all message up to the one sent by stopBle
        Message msg;
        do {
            msg = mLooper.nextMessage();
            Assert.assertNotNull(msg);
            msg.getTarget().dispatchMessage(msg);
        } while (msg.what != AdapterState.BLE_TURN_OFF);

        verifyStateChange(BluetoothAdapter.STATE_BLE_ON, BluetoothAdapter.STATE_BLE_TURNING_OFF, 1);

        mLooper.startAutoDispatch();
        verifyStateChange(
                BluetoothAdapter.STATE_BLE_TURNING_OFF,
                BluetoothAdapter.STATE_OFF,
                1,
                NATIVE_DISABLE_MS);
        mLooper.stopAutoDispatch();

        Assert.assertTrue(mAdapterService.getState() == BluetoothAdapter.STATE_OFF);
    }

    /**
     * Test: Verify that obfuscated Bluetooth address changes after factory reset
     *
     * There are 4 types of factory reset that we are talking about:
     * 1. Factory reset all user data from Settings -> Will restart phone
     * 2. Factory reset WiFi and Bluetooth from Settings -> Will only restart WiFi and BT
     * 3. Call BluetoothAdapter.factoryReset() -> Will disable Bluetooth and reset config in
     * memory and disk
     * 4. Call AdapterService.factoryReset() -> Will only reset config in memory
     *
     * We can only use No. 4 here
     */
    @Ignore("AdapterService.factoryReset() does not reload config into memory and hence old salt"
            + " is still used until next time Bluetooth library is initialized. However Bluetooth"
            + " cannot be used until Bluetooth process restart any way. Thus it is almost"
            + " guaranteed that user has to re-enable Bluetooth and hence re-generate new salt"
            + " after factory reset")
    @Test
    public void testObfuscateBluetoothAddress_FactoryReset() {
        Assert.assertFalse(mAdapterService.getState() == BluetoothAdapter.STATE_ON);
        BluetoothDevice device = TestUtils.getTestDevice(BluetoothAdapter.getDefaultAdapter(), 0);
        byte[] obfuscatedAddress1 = mAdapterService.obfuscateAddress(device);
        Assert.assertTrue(obfuscatedAddress1.length > 0);
        Assert.assertFalse(AdapterServiceTest.isByteArrayAllZero(obfuscatedAddress1));
        mServiceBinder.factoryReset(mAttributionSource);
        byte[] obfuscatedAddress2 = mAdapterService.obfuscateAddress(device);
        Assert.assertTrue(obfuscatedAddress2.length > 0);
        Assert.assertFalse(AdapterServiceTest.isByteArrayAllZero(obfuscatedAddress2));
        Assert.assertFalse(Arrays.equals(obfuscatedAddress2,
                obfuscatedAddress1));
        doEnable();
        byte[] obfuscatedAddress3 = mAdapterService.obfuscateAddress(device);
        Assert.assertTrue(obfuscatedAddress3.length > 0);
        Assert.assertFalse(AdapterServiceTest.isByteArrayAllZero(obfuscatedAddress3));
        Assert.assertArrayEquals(obfuscatedAddress3,
                obfuscatedAddress2);
        mServiceBinder.factoryReset(mAttributionSource);
        byte[] obfuscatedAddress4 = mAdapterService.obfuscateAddress(device);
        Assert.assertTrue(obfuscatedAddress4.length > 0);
        Assert.assertFalse(AdapterServiceTest.isByteArrayAllZero(obfuscatedAddress4));
        Assert.assertFalse(Arrays.equals(obfuscatedAddress4,
                obfuscatedAddress3));
    }

    /**
     * Test: Verify that obfuscated Bluetooth address changes after factory reset and reloading
     *       native layer
     */
    @Test
    public void testObfuscateBluetoothAddress_FactoryResetAndReloadNativeLayer() throws
            PackageManager.NameNotFoundException {
        byte[] metricsSalt1 = AdapterServiceTest.getMetricsSalt(mAdapterConfig);
        Assert.assertNotNull(metricsSalt1);
        Assert.assertFalse(mAdapterService.getState() == BluetoothAdapter.STATE_ON);
        BluetoothDevice device = TestUtils.getTestDevice(BluetoothAdapter.getDefaultAdapter(), 0);
        byte[] obfuscatedAddress1 = mAdapterService.obfuscateAddress(device);
        Assert.assertTrue(obfuscatedAddress1.length > 0);
        Assert.assertFalse(AdapterServiceTest.isByteArrayAllZero(obfuscatedAddress1));
        Assert.assertArrayEquals(AdapterServiceTest.obfuscateInJava(metricsSalt1, device),
                obfuscatedAddress1);
        mServiceBinder.factoryReset(mAttributionSource);
        tearDown();
        setUp();
        // Cannot verify metrics salt since it is not written to disk until native cleanup
        byte[] obfuscatedAddress2 = mAdapterService.obfuscateAddress(device);
        Assert.assertTrue(obfuscatedAddress2.length > 0);
        Assert.assertFalse(AdapterServiceTest.isByteArrayAllZero(obfuscatedAddress2));
        Assert.assertFalse(Arrays.equals(obfuscatedAddress2,
                obfuscatedAddress1));
    }
}
