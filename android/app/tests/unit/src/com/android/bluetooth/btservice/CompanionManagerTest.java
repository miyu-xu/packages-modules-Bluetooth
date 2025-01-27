/*
 * Copyright 2022 The Android Open Source Project
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

import static com.android.bluetooth.TestUtils.getTestDevice;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.HandlerThread;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;

import com.google.common.truth.Expect;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class CompanionManagerTest {

    private static final String TEST_DEVICE = "11:22:33:44:55:66";

    private Context mTargetContext;
    private CompanionManager mCompanionManager;
    private final BluetoothAdapter mAdapter;
    private final BluetoothDevice mTestDevice;

    private HandlerThread mHandlerThread;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AdapterService mAdapterService;
    @Mock SharedPreferences mSharedPreferences;
    @Mock SharedPreferences.Editor mEditor;

    @Rule public Expect expect = Expect.create();

    public CompanionManagerTest() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mTestDevice = getTestDevice(mAdapter, 59);
    }

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        // Prepare the TestUtils
        TestUtils.setAdapterService(mAdapterService);
        // Start handler thread for this test
        mHandlerThread = new HandlerThread("CompanionManagerTestHandlerThread");
        mHandlerThread.start();
        // Mock the looper
        doReturn(mHandlerThread.getLooper()).when(mAdapterService).getMainLooper();
        // Mock SharedPreferences
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        doReturn(mSharedPreferences)
                .when(mAdapterService)
                .getSharedPreferences(
                        eq(CompanionManager.COMPANION_INFO), eq(Context.MODE_PRIVATE));
        // Use the resources in the instrumentation instead of the mocked AdapterService
        when(mAdapterService.getResources()).thenReturn(mTargetContext.getResources());

        // Must be called to initialize services
        mCompanionManager = new CompanionManager(mAdapterService, null);
    }

    @After
    public void tearDown() throws Exception {
        mHandlerThread.quit();
        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void testLoadCompanionInfo_hasCompanionDeviceKey() {
        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_PRIMARY);
    }

    @Test
    public void testLoadCompanionInfo_noCompanionDeviceSetButHaveBondedDevices_shouldNotCrash() {
        BluetoothDevice[] devices = new BluetoothDevice[2];
        doReturn(devices).when(mAdapterService).getBondedDevices();
        doThrow(new IllegalArgumentException())
                .when(mSharedPreferences)
                .getInt(eq(CompanionManager.COMPANION_TYPE_KEY), anyInt());
        mCompanionManager.loadCompanionInfo();
    }

    @Test
    public void testIsCompanionDevice() {
        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_NONE);
        expect.that(mCompanionManager.isCompanionDevice(TEST_DEVICE)).isTrue();

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_PRIMARY);
        expect.that(mCompanionManager.isCompanionDevice(TEST_DEVICE)).isTrue();

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_SECONDARY);
        expect.that(mCompanionManager.isCompanionDevice(TEST_DEVICE)).isTrue();
    }

    @Test
    public void testGetGattConnParameterPrimary() {
        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_PRIMARY);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_SECONDARY);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_NONE);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
        checkReasonableConnParameterHelper(BluetoothGatt.CONNECTION_PRIORITY_DCK);
    }

    @Test
    public void testGetGattSubrateParameters() {
        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_PRIMARY);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_HIGH);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_BALANCED);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_LOW);

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_SECONDARY);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_HIGH);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_BALANCED);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_LOW);

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_NONE);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_HIGH);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_BALANCED);
        checkReasonableSubrateParameterHelper(BluetoothGatt.SUBRATE_MODE_LOW);
    }

    @Test
    public void testGetGattSubratePriorities() {
        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_PRIMARY);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_HIGH);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_BALANCED);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_LOW);

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_SECONDARY);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_HIGH);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_BALANCED);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_LOW);

        loadCompanionInfoHelper(TEST_DEVICE, CompanionManager.COMPANION_TYPE_NONE);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_HIGH);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_BALANCED);
        getReasonableSubrateParametersHelper(BluetoothGatt.SUBRATE_MODE_LOW);
    }

    private void loadCompanionInfoHelper(String address, int companionType) {
        doReturn(address)
                .when(mSharedPreferences)
                .getString(eq(CompanionManager.COMPANION_DEVICE_KEY), anyString());
        doReturn(companionType)
                .when(mSharedPreferences)
                .getInt(eq(CompanionManager.COMPANION_TYPE_KEY), anyInt());
        mCompanionManager.loadCompanionInfo();
    }

    private void checkReasonableConnParameterHelper(int priority) {
        // Max/Min values from the Bluetooth spec Version 5.3 | Vol 4, Part E | 7.8.18
        final int minInterval = 6; // 0x0006
        final int maxInterval = 3200; // 0x0C80
        final int minLatency = 0; // 0x0000
        final int maxLatency = 499; // 0x01F3

        int min =
                mCompanionManager.getGattConnParameters(
                        TEST_DEVICE, CompanionManager.GATT_CONN_INTERVAL_MIN, priority);
        int max =
                mCompanionManager.getGattConnParameters(
                        TEST_DEVICE, CompanionManager.GATT_CONN_INTERVAL_MAX, priority);
        int latency =
                mCompanionManager.getGattConnParameters(
                        TEST_DEVICE, CompanionManager.GATT_CONN_LATENCY, priority);

        expect.that(max).isAtLeast(min);
        expect.that(max).isAtLeast(minInterval);
        expect.that(min).isAtLeast(minInterval);
        expect.that(max).isAtMost(maxInterval);
        expect.that(min).isAtMost(maxInterval);
        expect.that(latency).isAtLeast(minLatency);
        expect.that(latency).isAtMost(maxLatency);
    }

    private void checkReasonableSubrateParameterHelper(int priority) {
        // Max/Min values from the Bluetooth spec Version 5.3 | Vol 4, Part E | 7.8.123
        final int minSubrateFactorLimit = 1; // 0x0001
        final int maxSubrateFactorLimit = 500; // 0x01F4
        final int minSubrateLatencyLimit = 0; // 0x0000
        final int maxSubrateLatencyLimit = 499; // 0x01F3
        final int minSubrateContNumLimit = 0; // 0x0000
        final int maxSubrateContNumLimit = 499; // 0x01F3

        int minSubrateFactor =
                mCompanionManager.getGattSubratingParameters(
                        mTestDevice, CompanionManager.GATT_SUBRATE_MIN_SUBRATE_FACTOR, priority);
        int maxSubrateFactor =
                mCompanionManager.getGattSubratingParameters(
                        mTestDevice, CompanionManager.GATT_SUBRATE_MAX_SUBRATE_FACTOR, priority);
        int subrateLatency =
                mCompanionManager.getGattSubratingParameters(
                        mTestDevice, CompanionManager.GATT_SUBRATE_LATENCY, priority);
        int subrateContNum =
                mCompanionManager.getGattSubratingParameters(
                        mTestDevice, CompanionManager.GATT_SUBRATE_CONT_NUM, priority);

        expect.that(maxSubrateFactor).isAtLeast(minSubrateFactor);
        expect.that(maxSubrateFactor).isAtLeast(minSubrateFactorLimit);
        expect.that(minSubrateFactor).isAtLeast(minSubrateFactorLimit);
        expect.that(maxSubrateFactor).isAtMost(maxSubrateFactorLimit);
        expect.that(minSubrateFactor).isAtMost(maxSubrateFactorLimit);
        expect.that(subrateLatency).isAtLeast(minSubrateLatencyLimit);
        expect.that(subrateLatency).isAtMost(maxSubrateLatencyLimit);
        expect.that(subrateContNum).isAtLeast(minSubrateContNumLimit);
        expect.that(subrateContNum).isAtMost(maxSubrateContNumLimit);
    }

    private void getReasonableSubrateParametersHelper(int priority) {
        int minSubrateFactor = 0;
        int maxSubrateFactor = 0;
        int subrateLatency = 0;
        int subrateContNum = 0;

        switch (priority) {
            case BluetoothGatt.SUBRATE_MODE_HIGH:
                minSubrateFactor = 9;
                maxSubrateFactor = 12;
                subrateLatency = 0;
                subrateContNum = 2;
                break;
            case BluetoothGatt.SUBRATE_MODE_BALANCED:
                minSubrateFactor = 5;
                maxSubrateFactor = 8;
                subrateLatency = 0;
                subrateContNum = 2;
                break;
            case BluetoothGatt.SUBRATE_MODE_LOW:
                minSubrateFactor = 2;
                maxSubrateFactor = 4;
                subrateLatency = 0;
                subrateContNum = 2;
                break;
        }

        int priorityMinSubrateFactor =
                mCompanionManager.verifyGattSubratingPriority(
                        TEST_DEVICE, minSubrateFactor, subrateLatency, subrateContNum);

        int priorityMaxSubrateFactor =
                mCompanionManager.verifyGattSubratingPriority(
                        TEST_DEVICE, maxSubrateFactor, subrateLatency, subrateContNum);

        expect.that(priorityMinSubrateFactor).isEqualTo(priority);
        expect.that(priorityMaxSubrateFactor).isEqualTo(priority);
    }
}
