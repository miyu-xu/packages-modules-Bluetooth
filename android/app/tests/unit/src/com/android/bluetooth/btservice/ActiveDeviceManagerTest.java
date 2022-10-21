/*
 * Copyright 2018 The Android Open Source Project
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

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.le_audio.LeAudioService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ActiveDeviceManagerTest {
    private static final int TEST_A2DP_DEVICE_INDEX = 0;
    private static final int TEST_HFP_DEVICE_INDEX = 1;
    private static final int TEST_A2DP_HFP_DEVICE_INDEX = 2;
    private static final int TEST_HA_DEVICE_INDEX = 3;
    private static final int TEST_LEA_DEVICE_INDEX = 4;
    private static final int TEST_HAP_DEVICE_INDEX = 5;
    private static final int TEST_SECONDARY_DEVICE_INDEX = 6;
    private static final int TEST_DEVICE_INDEX_MAX = 6;

    private BluetoothAdapter mAdapter;
    private Context mContext;

    private List<BluetoothDevice> mTestDevices = new ArrayList<>();
    private int mMostRecentDeviceIndex;
    private boolean mFallbackToA2dp;
    private boolean mFallbackToHfp;
    private ActiveDeviceManager mActiveDeviceManager;
    private static final int TIMEOUT_MS = 1000;

    @Mock private AdapterService mAdapterService;
    @Mock private ServiceFactory mServiceFactory;
    @Mock private A2dpService mA2dpService;
    @Mock private HeadsetService mHeadsetService;
    @Mock private HearingAidService mHearingAidService;
    @Mock private LeAudioService mLeAudioService;
    @Mock private AudioManager mAudioManager;
    @Mock private DatabaseManager mDatabaseManager;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        Assume.assumeTrue("Ignore test when A2dpService is not enabled", A2dpService.isEnabled());
        Assume.assumeTrue("Ignore test when HeadsetService is not enabled",
                HeadsetService.isEnabled());

        // Set up mocks and test assets
        MockitoAnnotations.initMocks(this);
        TestUtils.setAdapterService(mAdapterService);

        when(mAdapterService.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        when(mAdapterService.getSystemServiceName(AudioManager.class))
                .thenReturn(Context.AUDIO_SERVICE);
        when(mAdapterService.getDatabase()).thenReturn(mDatabaseManager);
        when(mServiceFactory.getA2dpService()).thenReturn(mA2dpService);
        when(mServiceFactory.getHeadsetService()).thenReturn(mHeadsetService);
        when(mServiceFactory.getHearingAidService()).thenReturn(mHearingAidService);
        when(mServiceFactory.getLeAudioService()).thenReturn(mLeAudioService);

        mActiveDeviceManager = new ActiveDeviceManager(mAdapterService, mServiceFactory);
        mActiveDeviceManager.start();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get devices for testing
        for (int i = 0; i <= TEST_DEVICE_INDEX_MAX; i++) {
            mTestDevices.add(TestUtils.getTestDevice(mAdapter, i));
        }

        when(mA2dpService.setActiveDevice(any())).thenAnswer(invocation -> {
            BluetoothDevice device = invocation.getArgument(0);
            BluetoothDevice fallbackDevice = device;
            if (device == null && mFallbackToA2dp) {
                fallbackDevice = mTestDevices.get(TEST_A2DP_DEVICE_INDEX);
            }
            when(mA2dpService.getFallbackDevice()).thenReturn(fallbackDevice);
            return true;
        });
        when(mHeadsetService.setActiveDevice(any())).thenAnswer(invocation -> {
            BluetoothDevice device = invocation.getArgument(0);
            BluetoothDevice fallbackDevice = device;
            if (device == null && mFallbackToHfp) {
                fallbackDevice = mTestDevices.get(TEST_HFP_DEVICE_INDEX);
            }
            when(mHeadsetService.getFallbackDevice()).thenReturn(fallbackDevice);
            return true;
        });
        when(mHearingAidService.setActiveDevice(any())).thenReturn(true);
        when(mLeAudioService.setActiveDevice(any())).thenReturn(true);
        when(mDatabaseManager.getMostRecentlyConnectedDevicesInList(any())).thenAnswer(
                invocation -> {
                    List<BluetoothDevice> devices = invocation.getArgument(0);
                    if (devices == null || devices.size() == 0) {
                        return null;
                    } else if (mMostRecentDeviceIndex >= 0
                            && devices.contains(mTestDevices.get(mMostRecentDeviceIndex))) {
                        return mTestDevices.get(mMostRecentDeviceIndex);
                    } else if (devices.contains(mTestDevices.get(TEST_HAP_DEVICE_INDEX))) {
                        return mTestDevices.get(TEST_HAP_DEVICE_INDEX);
                    } else if (devices.contains(mTestDevices.get(TEST_HA_DEVICE_INDEX))) {
                        return mTestDevices.get(TEST_HA_DEVICE_INDEX);
                    } else {
                        return devices.get(0);
                    }
                }
        );
    }

    @After
    public void tearDown() throws Exception {
        if (!HeadsetService.isEnabled() || !A2dpService.isEnabled()) {
            return;
        }
        mActiveDeviceManager.cleanup();
        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void testSetUpAndTearDown() {}

    /**
     * One A2DP is connected.
     */
    @Test
    public void onlyA2dpConnected_setA2dpActive() {
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));
    }

    /**
     * Two A2DP are connected. Should set the second one active.
     */
    @Test
    public void secondA2dpConnected_setSecondA2dpActive() {
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
    }

    /**
     * One A2DP is connected and disconnected later. Should then set active device to null.
     */
    @Test
    public void lastA2dpDisconnected_clearA2dpActive() {
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        mMostRecentDeviceIndex = -1;
        a2dpDisconnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
    }

    /**
     * Two A2DP are connected and active device is explicitly set.
     */
    @Test
    public void a2dpActiveDeviceSelected_setActive() {
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));

        a2dpActiveDeviceChanged(TEST_A2DP_DEVICE_INDEX);
        // Don't call mA2dpService.setActiveDevice()
        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mA2dpService, times(1))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_A2DP_DEVICE_INDEX),
                mActiveDeviceManager.getA2dpActiveDevice());
    }

    /**
     * Two A2DP devices are connected and the current active is then disconnected.
     * Should then set active device to fallback device.
     */
    @Test
    public void a2dpSecondDeviceDisconnected_fallbackDeviceActive() {
        a2dpConnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));

        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        a2dpDisconnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));
    }

    /**
     * One Headset is connected.
     */
    @Test
    public void onlyHeadsetConnected_setHeadsetActive() {
        headsetConnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));
    }

    /**
     * Two Headset are connected. Should set the second one active.
     */
    @Test
    public void secondHeadsetConnected_setSecondHeadsetActive() {
        headsetConnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));

        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
    }

    /**
     * One Headset is connected and disconnected later. Should then set active device to null.
     */
    @Test
    public void lastHeadsetDisconnected_clearHeadsetActive() {
        headsetConnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));

        headsetDisconnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
    }

    /**
     * Two Headset are connected and active device is explicitly set.
     */
    @Test
    public void headsetActiveDeviceSelected_setActive() {
        headsetConnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));

        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));

        headsetActiveDeviceChanged(TEST_HFP_DEVICE_INDEX);
        // Don't call mHeadsetService.setActiveDevice()
        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mHeadsetService, times(1))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_HFP_DEVICE_INDEX),
                mActiveDeviceManager.getHfpActiveDevice());
    }

    /**
     * Two Headsets are connected and the current active is then disconnected.
     * Should then set active device to fallback device.
     */
    @Test
    public void headsetSecondDeviceDisconnected_fallbackDeviceActive() {
        headsetConnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));

        headsetConnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));

        headsetDisconnected(TEST_HFP_DEVICE_INDEX);
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));
    }

    /**
     * A combo (A2DP + Headset) device is connected. Then a Hearing Aid is connected.
     */
    @Test
    public void hearingAidActive_clearA2dpAndHeadsetActive() {
        Assume.assumeTrue("Ignore test when HearingAidService is not enabled",
                HearingAidService.isEnabled());

        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));

        hearingAidActiveDeviceChanged(TEST_HA_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
        verify(mHeadsetService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
    }

    /**
     * A Hearing Aid is connected. Then a combo (A2DP + Headset) device is connected.
     */
    @Test
    public void hearingAidActive_dontSetA2dpAndHeadsetActive() {
        Assume.assumeTrue("Ignore test when HearingAidService is not enabled",
                HearingAidService.isEnabled());

        hearingAidActiveDeviceChanged(TEST_HA_DEVICE_INDEX);
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);

        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mA2dpService, never())
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        verify(mHeadsetService, never())
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
    }

    /**
     * A Hearing Aid is connected. Then an A2DP active device is explicitly set.
     */
    @Test
    public void hearingAidActive_setA2dpActiveExplicitly() {
        Assume.assumeTrue("Ignore test when HearingAidService is not enabled",
                HearingAidService.isEnabled());

        hearingAidActiveDeviceChanged(TEST_HA_DEVICE_INDEX);
        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        a2dpActiveDeviceChanged(TEST_A2DP_HFP_DEVICE_INDEX);

        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mHearingAidService).setActiveDevice(isNull());
        // Don't call mA2dpService.setActiveDevice()
        verify(mA2dpService, never()).setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX),
                mActiveDeviceManager.getA2dpActiveDevice());
        Assert.assertEquals(null, mActiveDeviceManager.getHearingAidActiveDevice());
    }

    /**
     * A Hearing Aid is connected. Then a Headset active device is explicitly set.
     */
    @Test
    public void hearingAidActive_setHeadsetActiveExplicitly() {
        Assume.assumeTrue("Ignore test when HearingAidService is not enabled",
                HearingAidService.isEnabled());

        hearingAidActiveDeviceChanged(TEST_HA_DEVICE_INDEX);
        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        headsetActiveDeviceChanged(TEST_A2DP_HFP_DEVICE_INDEX);

        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mHearingAidService).setActiveDevice(isNull());
        // Don't call mHeadsetService.setActiveDevice()
        verify(mHeadsetService, never())
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX),
                mActiveDeviceManager.getHfpActiveDevice());
        Assert.assertEquals(null, mActiveDeviceManager.getHearingAidActiveDevice());
    }

    /**
     * One LE Audio is connected.
     */
    @Test
    public void onlyLeAudioConnected_setHeadsetActive() {
        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));
    }

    /**
     * Two LE Audio are connected. Should set the second one active.
     */
    @Test
    public void secondLeAudioConnected_setSecondLeAudioActive() {
        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));

        leAudioConnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));
    }

    /**
     * One LE Audio  is connected and disconnected later. Should then set active device to null.
     */
    @Test
    public void lastLeAudioDisconnected_clearLeAudioActive() {
        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));

        leAudioDisconnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
    }

    /**
     * Two LE Audio are connected and active device is explicitly set.
     */
    @Test
    public void leAudioActiveDeviceSelected_setActive() {
        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));

        leAudioConnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));

        leAudioActiveDeviceChanged(TEST_LEA_DEVICE_INDEX);
        // Don't call mLeAudioService.setActiveDevice()
        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mLeAudioService, times(1))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_LEA_DEVICE_INDEX),
                mActiveDeviceManager.getLeAudioActiveDevice());
    }

    /**
     * Two LE Audio are connected and the current active is then disconnected.
     * Should then set active device to fallback device.
     */
    @Test
    public void leAudioSecondDeviceDisconnected_fallbackDeviceActive() {
        leAudioConnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));

        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));

        leAudioDisconnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));
    }

    /**
     * A combo (A2DP + Headset) device is connected. Then an LE Audio is connected.
     */
    @Test
    public void leAudioActive_clearA2dpAndHeadsetActive() {
        Assume.assumeTrue("Ignore test when LeAudioService is not enabled",
                LeAudioService.isEnabled());

        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));

        leAudioActiveDeviceChanged(TEST_LEA_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
        verify(mHeadsetService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
    }

    /**
     * An LE Audio is connected. Then a combo (A2DP + Headset) device is connected.
     */
    @Test
    public void leAudioActive_dontSetA2dpAndHeadsetActive() {
        Assume.assumeTrue("Ignore test when LeAudioService is not enabled",
                LeAudioService.isEnabled());

        leAudioActiveDeviceChanged(TEST_LEA_DEVICE_INDEX);
        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);

        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mA2dpService).setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        verify(mHeadsetService).setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
    }

    /**
     * An LE Audio is connected. Then an A2DP active device is explicitly set.
     */
    @Test
    public void leAudioActive_setA2dpActiveExplicitly() {
        Assume.assumeTrue("Ignore test when LeAudioService is not enabled",
                LeAudioService.isEnabled());

        leAudioActiveDeviceChanged(TEST_LEA_DEVICE_INDEX);
        a2dpConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        a2dpActiveDeviceChanged(TEST_A2DP_HFP_DEVICE_INDEX);

        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mLeAudioService).setActiveDevice(isNull());
        verify(mA2dpService).setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX),
                mActiveDeviceManager.getA2dpActiveDevice());
        Assert.assertEquals(null, mActiveDeviceManager.getLeAudioActiveDevice());
    }

    /**
     * An LE Audio is connected. Then a Headset active device is explicitly set.
     */
    @Test
    public void leAudioActive_setHeadsetActiveExplicitly() {
        Assume.assumeTrue("Ignore test when LeAudioService is not enabled",
                LeAudioService.isEnabled());

        leAudioActiveDeviceChanged(TEST_LEA_DEVICE_INDEX);
        headsetConnected(TEST_A2DP_HFP_DEVICE_INDEX);
        headsetActiveDeviceChanged(TEST_A2DP_HFP_DEVICE_INDEX);

        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mLeAudioService).setActiveDevice(isNull());
        verify(mHeadsetService).setActiveDevice(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX));
        Assert.assertEquals(mTestDevices.get(TEST_A2DP_HFP_DEVICE_INDEX),
                mActiveDeviceManager.getHfpActiveDevice());
        Assert.assertEquals(null, mActiveDeviceManager.getLeAudioActiveDevice());
    }

    /**
     * An LE Audio connected. An A2DP connected. The A2DP disconnected.
     * Then the LE Audio should be the active one.
     */
    @Test
    public void leAudioAndA2dpConnectedThenA2dpDisconnected_fallbackToLeAudio() {
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_NORMAL);

        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));

        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        mMostRecentDeviceIndex = TEST_LEA_DEVICE_INDEX;
        a2dpDisconnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS).atLeast(1)).setActiveDevice(isNull());
        verify(mLeAudioService, timeout(TIMEOUT_MS).times(2))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));
    }

    /**
     * An A2DP connected. An LE Audio connected. The LE Audio disconnected.
     * Then the A2DP should be the active one.
     */
    @Test
    public void a2dpAndLeAudioConnectedThenLeAudioDisconnected_fallbackToA2dp() {
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_NORMAL);

        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));

        Mockito.clearInvocations(mA2dpService);
        mMostRecentDeviceIndex = TEST_A2DP_DEVICE_INDEX;
        leAudioDisconnected(TEST_LEA_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS).atLeast(1)).setActiveDevice(isNull());
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));
    }

    /**
     * Two Hearing Aid are connected and the current active is then disconnected.
     * Should then set active device to fallback device.
     */
    @Test
    public void hearingAidSecondDeviceDisconnected_fallbackDeviceActive() {
        hearingAidConnected(TEST_SECONDARY_DEVICE_INDEX);
        verify(mHearingAidService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));

        hearingAidConnected(TEST_HA_DEVICE_INDEX);
        verify(mHearingAidService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HA_DEVICE_INDEX));

        leAudioDisconnected(TEST_HA_DEVICE_INDEX);
        verify(mHearingAidService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_SECONDARY_DEVICE_INDEX));
    }

    /**
     * Hearing aid is connected, but active device is different BT.
     * When the active device is disconnected, the hearing aid should be the active one.
     */
    @Test
    public void activeDeviceDisconnected_fallbackToHearingAid() {
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_NORMAL);

        hearingAidConnected(TEST_HA_DEVICE_INDEX);
        verify(mHearingAidService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HA_DEVICE_INDEX));

        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);

        a2dpActiveDeviceChanged(TEST_A2DP_DEVICE_INDEX);
        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());

        verify(mHearingAidService).setActiveDevice(isNull());
        verify(mLeAudioService, never()).setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));
        verify(mA2dpService, never()).setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        a2dpDisconnected(TEST_A2DP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS).atLeast(1)).setActiveDevice(isNull());
        verify(mHearingAidService, timeout(TIMEOUT_MS).times(2))
                .setActiveDevice(mTestDevices.get(TEST_HA_DEVICE_INDEX));
    }

    /**
     * One LE Hearing Aid is connected.
     */
    @Test
    public void onlyLeHearingAIdConnected_setLeAudioActive() {
        leAudioConnected(TEST_HAP_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HAP_DEVICE_INDEX));
    }

    /**
     * LE audio is connected after LE Hearing Aid device.
     * Keep LE hearing Aid active.
     */
    @Test
    public void leAudioConnectedAfterLeHearingAid_setLeAudioActiveShouldNotBeCalled() {
        leHearingAidConnected(TEST_HAP_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HAP_DEVICE_INDEX));

        leAudioConnected(TEST_LEA_DEVICE_INDEX);
        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mLeAudioService, never()).setActiveDevice(mTestDevices.get(TEST_LEA_DEVICE_INDEX));
    }

    /**
     * Test connect/disconnect of devices.
     * Hearing Aid, LE Hearing Aid, A2DP connected, then LE hearing Aid and hearing aid
     * disconnected.
     */
    @Test
    public void activeDeviceChange_withHearingAidLeHearingAidAndA2dpDevices() {
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_NORMAL);

        hearingAidConnected(TEST_HA_DEVICE_INDEX);
        verify(mHearingAidService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HA_DEVICE_INDEX));

        leHearingAidConnected(TEST_HAP_DEVICE_INDEX);
        verify(mLeAudioService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HAP_DEVICE_INDEX));

        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        TestUtils.waitForLooperToFinishScheduledTask(mActiveDeviceManager.getHandlerLooper());
        verify(mA2dpService, never()).setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));

        Mockito.clearInvocations(mHearingAidService);
        leHearingAidDisconnected(TEST_HAP_DEVICE_INDEX);
        verify(mHearingAidService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HA_DEVICE_INDEX));

        hearingAidDisconnected(TEST_HA_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));
    }

    /**
     * A wired audio device is connected. Then all active devices are set to null.
     */
    @Test
    public void wiredAudioDeviceConnected_setAllActiveDevicesNull() {
        a2dpConnected(TEST_A2DP_DEVICE_INDEX);
        headsetConnected(TEST_HFP_DEVICE_INDEX);
        verify(mA2dpService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_A2DP_DEVICE_INDEX));
        verify(mHeadsetService, timeout(TIMEOUT_MS))
                .setActiveDevice(mTestDevices.get(TEST_HFP_DEVICE_INDEX));

        mActiveDeviceManager.wiredAudioDeviceConnected();
        verify(mA2dpService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
        verify(mHeadsetService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
        verify(mHearingAidService, timeout(TIMEOUT_MS)).setActiveDevice(isNull());
    }

    /**
     * Helper to indicate A2dp connected for a device.
     */
    private void a2dpConnected(int index) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
        mFallbackToA2dp = true;
    }

    /**
     * Helper to indicate A2dp disconnected for a device.
     */
    private void a2dpDisconnected(int index) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_CONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mFallbackToA2dp = false;
    }

    /**
     * Helper to indicate A2dp active device changed for a device.
     */
    private void a2dpActiveDeviceChanged(int index) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate Headset connected for a device.
     */
    private void headsetConnected(int index) {
        Intent intent = new Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
        mFallbackToHfp = true;
    }

    /**
     * Helper to indicate Headset disconnected for a device.
     */
    private void headsetDisconnected(int index) {
        Intent intent = new Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_CONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mFallbackToHfp = false;
    }

    /**
     * Helper to indicate Headset active device changed for a device.
     */
    private void headsetActiveDeviceChanged(int index) {
        Intent intent = new Intent(BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate Hearing Aid connected for a device.
     */
    private void hearingAidConnected(int index) {
        Intent intent = new Intent(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate Hearing Aid disconnected for a device.
     */
    private void hearingAidDisconnected(int index) {
        Intent intent = new Intent(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_CONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
    }

    /**
     * Helper to indicate Hearing Aid active device changed for a device.
     */
    private void hearingAidActiveDeviceChanged(int index) {
        Intent intent = new Intent(BluetoothHearingAid.ACTION_ACTIVE_DEVICE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate LE Audio connected for a device.
     */
    private void leAudioConnected(int index) {
        Intent intent = new Intent(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate LE Audio disconnected for a device.
     */
    private void leAudioDisconnected(int index) {
        Intent intent = new Intent(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_CONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
    }

    /**
     * Helper to indicate LE Audio active device changed for a device.
     */
    private void leAudioActiveDeviceChanged(int index) {
        Intent intent = new Intent(BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate LE Hearing Aid connected for a device.
     */
    private void leHearingAidConnected(int index) {
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }

    /**
     * Helper to indicate LE Hearing Aid disconnected for a device.
     */
    private void leHearingAidDisconnected(int index) {
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_CONNECTED);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
    }

    /**
     * Helper to indicate LE Audio Hearing Aid device changed for a device.
     */
    private void leHearingAidActiveDeviceChanged(int index) {
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_DEVICE_AVAILABLE);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mTestDevices.get(index));
        mActiveDeviceManager.getBroadcastReceiver().onReceive(mContext, intent);
        mMostRecentDeviceIndex = index;
    }
}
