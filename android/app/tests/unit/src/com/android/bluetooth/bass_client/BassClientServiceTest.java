/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.bluetooth.bass_client;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioCodecConfigMetadata;
import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastChannel;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastSubgroup;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Message;
import android.os.ParcelUuid;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ServiceFactory;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.csip.CsipSetCoordinatorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tests for {@link BassClientService}
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class BassClientServiceTest {
    private final String mFlagDexmarker = System.getProperty("dexmaker.share_classloader", "false");

    private static final int MAX_HEADSET_CONNECTIONS = 5;
    private static final ParcelUuid[] FAKE_SERVICE_UUIDS = {BluetoothUuid.BASS};
    private static final int ASYNC_CALL_TIMEOUT_MILLIS = 250;

    private static final String TEST_MAC_ADDRESS = "00:11:22:33:44:55";
    private static final int TEST_BROADCAST_ID = 42;
    private static final int TEST_ADVERTISER_SID = 1234;
    private static final int TEST_PA_SYNC_INTERVAL = 100;
    private static final int TEST_PRESENTATION_DELAY_MS = 345;

    private static final int TEST_CODEC_ID = 42;
    private static final int TEST_CHANNEL_INDEX = 56;

    // For BluetoothLeAudioCodecConfigMetadata
    private static final long TEST_AUDIO_LOCATION_FRONT_LEFT = 0x01;
    private static final long TEST_AUDIO_LOCATION_FRONT_RIGHT = 0x02;

    // For BluetoothLeAudioContentMetadata
    private static final String TEST_PROGRAM_INFO = "Test";
    // German language code in ISO 639-3
    private static final String TEST_LANGUAGE = "deu";
    private static final int TEST_SOURCE_ID = 2;
    private static final int TEST_NUM_SOURCES = 2;


    private static final int TEST_MAX_NUM_DEVICES = 3;

    private final HashMap<BluetoothDevice, BassClientStateMachine> mStateMachines = new HashMap<>();
    private final List<BassClientStateMachine> mStateMachinePool = new ArrayList<>();

    private Context mTargetContext;
    private BassClientService mBassClientService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mCurrentDevice;
    private BluetoothDevice mCurrentDevice1;

    @Rule public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Spy private BassObjectsFactory mObjectsFactory = BassObjectsFactory.getInstance();
    @Mock private AdapterService mAdapterService;
    @Mock private DatabaseManager mDatabaseManager;
    @Mock private BluetoothLeScannerWrapper mBluetoothLeScannerWrapper;
    @Mock private ServiceFactory mServiceFactory;
    @Mock private CsipSetCoordinatorService mCsipService;

    BluetoothLeBroadcastSubgroup createBroadcastSubgroup() {
        BluetoothLeAudioCodecConfigMetadata codecMetadata =
                new BluetoothLeAudioCodecConfigMetadata.Builder()
                        .setAudioLocation(TEST_AUDIO_LOCATION_FRONT_LEFT).build();
        BluetoothLeAudioContentMetadata contentMetadata =
                new BluetoothLeAudioContentMetadata.Builder()
                        .setProgramInfo(TEST_PROGRAM_INFO).setLanguage(TEST_LANGUAGE).build();
        BluetoothLeBroadcastSubgroup.Builder builder = new BluetoothLeBroadcastSubgroup.Builder()
                .setCodecId(TEST_CODEC_ID)
                .setCodecSpecificConfig(codecMetadata)
                .setContentMetadata(contentMetadata);

        BluetoothLeAudioCodecConfigMetadata channelCodecMetadata =
                new BluetoothLeAudioCodecConfigMetadata.Builder()
                        .setAudioLocation(TEST_AUDIO_LOCATION_FRONT_RIGHT).build();

        // builder expect at least one channel
        BluetoothLeBroadcastChannel channel =
                new BluetoothLeBroadcastChannel.Builder()
                        .setSelected(true)
                        .setChannelIndex(TEST_CHANNEL_INDEX)
                        .setCodecMetadata(channelCodecMetadata)
                        .build();
        builder.addChannel(channel);
        return builder.build();
    }

    BluetoothLeBroadcastMetadata createBroadcastMetadata() {
        BluetoothDevice testDevice =
                mBluetoothAdapter.getRemoteLeDevice(TEST_MAC_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);

        BluetoothLeBroadcastMetadata.Builder builder = new BluetoothLeBroadcastMetadata.Builder()
                        .setEncrypted(false)
                        .setSourceDevice(testDevice, BluetoothDevice.ADDRESS_TYPE_RANDOM)
                        .setSourceAdvertisingSid(TEST_ADVERTISER_SID)
                        .setBroadcastId(TEST_BROADCAST_ID)
                        .setBroadcastCode(null)
                        .setPaSyncInterval(TEST_PA_SYNC_INTERVAL)
                        .setPresentationDelayMicros(TEST_PRESENTATION_DELAY_MS);
        // builder expect at least one subgroup
        builder.addSubgroup(createBroadcastSubgroup());
        return builder.build();
    }

    @Before
    public void setUp() throws Exception {
        if (!mFlagDexmarker.equals("true")) {
            System.setProperty("dexmaker.share_classloader", "true");
        }

        mTargetContext = InstrumentationRegistry.getTargetContext();
        MockitoAnnotations.initMocks(this);
        TestUtils.setAdapterService(mAdapterService);
        BassObjectsFactory.setInstanceForTesting(mObjectsFactory);

        doReturn(new ParcelUuid[]{BluetoothUuid.BASS}).when(mAdapterService)
                .getRemoteUuids(any(BluetoothDevice.class));
        // This line must be called to make sure relevant objects are initialized properly
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Mock methods in AdapterService
        doReturn(FAKE_SERVICE_UUIDS).when(mAdapterService)
                .getRemoteUuids(any(BluetoothDevice.class));
        doReturn(BluetoothDevice.BOND_BONDED).when(mAdapterService)
                .getBondState(any(BluetoothDevice.class));
        doReturn(mDatabaseManager).when(mAdapterService).getDatabase();
        doReturn(true, false).when(mAdapterService).isStartedProfile(anyString());
        doAnswer(invocation -> {
            Set<BluetoothDevice> keys = mStateMachines.keySet();
            return keys.toArray(new BluetoothDevice[keys.size()]);
        }).when(mAdapterService).getBondedDevices();

        // Mock methods in BassObjectsFactory
        doAnswer(invocation -> {
            assertThat(mCurrentDevice).isNotNull();
            final BassClientStateMachine stateMachine = mock(BassClientStateMachine.class);
            doReturn(new ArrayList<>()).when(stateMachine).getAllSources();
            doReturn(TEST_NUM_SOURCES).when(stateMachine).getMaximumSourceCapacity();
            mStateMachines.put((BluetoothDevice)invocation.getArgument(0), stateMachine);
            return stateMachine;
        }).when(mObjectsFactory).makeStateMachine(any(), any(), any());
        doReturn(mBluetoothLeScannerWrapper).when(mObjectsFactory)
                .getBluetoothLeScannerWrapper(any());

        TestUtils.startService(mServiceRule, BassClientService.class);
        mBassClientService = BassClientService.getBassClientService();
        assertThat(mBassClientService).isNotNull();

        mBassClientService.mServiceFactory = mServiceFactory;
        doReturn(mCsipService).when(mServiceFactory).getCsipSetCoordinatorService();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.stopService(mServiceRule, BassClientService.class);
        mBassClientService = BassClientService.getBassClientService();
        assertThat(mBassClientService).isNull();
        mStateMachines.clear();
        mCurrentDevice = null;
        mCurrentDevice1 = null;
        BassObjectsFactory.setInstanceForTesting(null);
        TestUtils.clearAdapterService(mAdapterService);

        if (!mFlagDexmarker.equals("true")) {
            System.setProperty("dexmaker.share_classloader", mFlagDexmarker);
        }
    }

    /**
     * Test to verify that BassClientService can be successfully started
     */
    @Test
    public void testGetBassClientService() {
        assertThat(mBassClientService).isEqualTo(BassClientService.getBassClientService());
        // Verify default connection and audio states
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);
        assertThat(mBassClientService.getConnectionState(mCurrentDevice))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    /**
     * Test connecting to a test device.
     *  - service.connect() should return false
     *  - bassClientStateMachine.sendMessage(CONNECT) should be called.
     */
    @Test
    public void testConnect() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);

        assertThat(mBassClientService.connect(mCurrentDevice)).isTrue();
        verify(mObjectsFactory).makeStateMachine(
                eq(mCurrentDevice), eq(mBassClientService), any());
        BassClientStateMachine stateMachine = mStateMachines.get(mCurrentDevice);
        assertThat(stateMachine).isNotNull();
        verify(stateMachine).sendMessage(BassClientStateMachine.CONNECT);
    }

    /**
     * Test connecting to a null device.
     *  - service.connect() should return false.
     */
    @Test
    public void testConnect_nullDevice() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        BluetoothDevice nullDevice = null;

        assertThat(mBassClientService.connect(nullDevice)).isFalse();
    }

    /**
     * Test connecting to a device when the connection policy is unknown.
     *  - service.connect() should return false.
     */
    @Test
    public void testConnect_whenConnectionPolicyIsUnknown() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_UNKNOWN);
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);
        assertThat(mCurrentDevice).isNotNull();

        assertThat(mBassClientService.connect(mCurrentDevice)).isFalse();
    }

    /**
     * Test whether service.startSearchingForSources() calls BluetoothLeScannerWrapper.startScan().
     */
    @Test
    public void testStartSearchingForSources() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        mBassClientService.startSearchingForSources(scanFilters);

        verify(mBluetoothLeScannerWrapper).startScan(notNull(), notNull(), notNull());
    }

    /**
     * Test whether service.startSearchingForSources() does not call
     * BluetoothLeScannerWrapper.startScan() when the scanner instance cannot be achieved.
     */
    @Test
    public void testStartSearchingForSources_whenScannerIsNull() {
        doReturn(null).when(mObjectsFactory).getBluetoothLeScannerWrapper(any());
        List<ScanFilter> scanFilters = new ArrayList<>();

        mBassClientService.startSearchingForSources(scanFilters);

        verify(mBluetoothLeScannerWrapper, never()).startScan(any(), any(), any());
    }

    /**
     * Test whether service.addSource() does send proper messages to all the
     * state machines within the Csip coordinated group
     */
    @Test
    public void testAddSourceForGroup() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                        eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                        .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);
        mCurrentDevice1 = TestUtils.getTestDevice(mBluetoothAdapter, 1);

        // Mock the CSIP group
        List<BluetoothDevice> groupDevices = new ArrayList<>();
        groupDevices.add(mCurrentDevice);
        groupDevices.add(mCurrentDevice1);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice, BluetoothUuid.CAP);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice1, BluetoothUuid.CAP);

        // Prepare connected devices
        assertThat(mBassClientService.connect(mCurrentDevice)).isTrue();
        assertThat(mBassClientService.connect(mCurrentDevice1)).isTrue();

        verify(mObjectsFactory, times(2)).makeStateMachine(any(), any(), any());
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            doReturn(BluetoothProfile.STATE_CONNECTED).when(sm).getConnectionState();
        }

        // Add broadcast source
        BluetoothLeBroadcastMetadata meta = createBroadcastMetadata();
        mBassClientService.addSource(mCurrentDevice, meta, true);

        // Verify all group members getting ADD_BCAST_SOURCE message
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(sm).sendMessage(messageCaptor.capture());

            Message msg = messageCaptor.getAllValues().stream()
                    .filter(m -> m.what == BassClientStateMachine.ADD_BCAST_SOURCE)
                    .findFirst()
                    .get();
            assertThat(msg).isNotNull();
            assertThat(msg.obj).isEqualTo(meta);
        }
    }

   /**
     * Test whether service.modifySource() does send proper messages to all the
     * state machines within the Csip coordinated group
     */
    @Test
    public void testModifySourceForGroup() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                        eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                        .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);
        mCurrentDevice1 = TestUtils.getTestDevice(mBluetoothAdapter, 1);

        // Mock the CSIP group
        List<BluetoothDevice> groupDevices = new ArrayList<>();
        groupDevices.add(mCurrentDevice);
        groupDevices.add(mCurrentDevice1);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice, BluetoothUuid.CAP);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice1, BluetoothUuid.CAP);

        // Prepare connected devices
        assertThat(mBassClientService.connect(mCurrentDevice)).isTrue();
        assertThat(mBassClientService.connect(mCurrentDevice1)).isTrue();

        verify(mObjectsFactory, times(2)).makeStateMachine(any(), any(), any());
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            doReturn(BluetoothProfile.STATE_CONNECTED).when(sm).getConnectionState();
        }

        // Add broadcast source
        BluetoothLeBroadcastMetadata meta = createBroadcastMetadata();
        mBassClientService.addSource(mCurrentDevice, meta, true);

        // Update broadcast source
        BluetoothLeBroadcastMetadata metaUpdate =
                new BluetoothLeBroadcastMetadata.Builder(meta)
                        .setBroadcastId(TEST_BROADCAST_ID + 1).build();
        mBassClientService.modifySource(mCurrentDevice, TEST_SOURCE_ID, metaUpdate);

        // Verify all group members getting UPDATE_BCAST_SOURCE message
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(sm, atLeast(1)).sendMessage(messageCaptor.capture());

            Message msg = messageCaptor.getAllValues().stream()
                    .filter(m -> m.what == BassClientStateMachine.UPDATE_BCAST_SOURCE)
                    .findFirst()
                    .get();
            assertThat(msg).isNotNull();
            assertThat(msg.obj).isEqualTo(metaUpdate);
        }
    }

    /**
     * Test whether service.removeSource() does send proper messages to all the
     * state machines within the Csip coordinated group
     */
    @Test
    public void testRemoveSourceForGroup() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                        eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                        .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);
        mCurrentDevice1 = TestUtils.getTestDevice(mBluetoothAdapter, 1);

        // Mock the CSIP group
        List<BluetoothDevice> groupDevices = new ArrayList<>();
        groupDevices.add(mCurrentDevice);
        groupDevices.add(mCurrentDevice1);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice, BluetoothUuid.CAP);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice1, BluetoothUuid.CAP);

        // Prepare connected devices
        assertThat(mBassClientService.connect(mCurrentDevice)).isTrue();
        assertThat(mBassClientService.connect(mCurrentDevice1)).isTrue();

        verify(mObjectsFactory, times(2)).makeStateMachine(any(), any(), any());
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            doReturn(BluetoothProfile.STATE_CONNECTED).when(sm).getConnectionState();
        }

        // Add broadcast source
        BluetoothLeBroadcastMetadata meta = createBroadcastMetadata();
        mBassClientService.addSource(mCurrentDevice, meta, true);

        // Remove broadcast source
        mBassClientService.removeSource(mCurrentDevice, TEST_SOURCE_ID);

        // Verify all group members getting REMOVE_BCAST_SOURCE message
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(sm, atLeast(1)).sendMessage(messageCaptor.capture());

            Message msg = messageCaptor.getAllValues().stream()
                    .filter(m -> m.what == BassClientStateMachine.REMOVE_BCAST_SOURCE)
                    .findFirst()
                    .get();
            assertThat(msg).isNotNull();
            assertThat(msg.arg1).isEqualTo(TEST_SOURCE_ID);
        }
    }

    /**
     * Test whether the group operation flag is set on addSource() and removed on removeSource
     */
    @Test
    public void testGroupStickyFlagSetUnset() {
        when(mDatabaseManager.getProfileConnectionPolicy(any(BluetoothDevice.class),
                        eq(BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT)))
                        .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        mCurrentDevice = TestUtils.getTestDevice(mBluetoothAdapter, 0);
        mCurrentDevice1 = TestUtils.getTestDevice(mBluetoothAdapter, 1);

        // Mock the CSIP group
        List<BluetoothDevice> groupDevices = new ArrayList<>();
        groupDevices.add(mCurrentDevice);
        groupDevices.add(mCurrentDevice1);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice, BluetoothUuid.CAP);
        doReturn(groupDevices).when(mCsipService)
                .getGroupDevices(mCurrentDevice1, BluetoothUuid.CAP);

        // Prepare connected devices
        assertThat(mBassClientService.connect(mCurrentDevice)).isTrue();
        assertThat(mBassClientService.connect(mCurrentDevice1)).isTrue();

        verify(mObjectsFactory, times(2)).makeStateMachine(any(), any(), any());
        assertThat(mStateMachines.size()).isEqualTo(2);
        for (BassClientStateMachine sm: mStateMachines.values()) {
            doReturn(BluetoothProfile.STATE_CONNECTED).when(sm).getConnectionState();
        }

        // Add broadcast source
        BluetoothLeBroadcastMetadata meta = createBroadcastMetadata();
        mBassClientService.addSource(mCurrentDevice, meta, true);

        // Remove broadcast source
        mBassClientService.removeSource(mCurrentDevice, TEST_SOURCE_ID);

        // Update broadcast source
        BluetoothLeBroadcastMetadata metaUpdate =
                new BluetoothLeBroadcastMetadata.Builder(meta)
                        .setBroadcastId(TEST_BROADCAST_ID + 1).build();
        mBassClientService.modifySource(mCurrentDevice, TEST_SOURCE_ID, metaUpdate);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        Optional<Message> msg;

        // Verrify that one device got the message...
        verify(mStateMachines.get(mCurrentDevice), atLeast(1)).sendMessage(messageCaptor.capture());
        msg = messageCaptor.getAllValues().stream()
                    .filter(m -> m.what == BassClientStateMachine.UPDATE_BCAST_SOURCE)
                    .findFirst();
        assertThat(msg.isPresent()).isTrue();
        assertThat(msg.get()).isNotNull();

        //... but not the other one, since the sticky group flag should have been removed
        messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mStateMachines.get(mCurrentDevice), atLeast(1)).sendMessage(messageCaptor.capture());
        msg = messageCaptor.getAllValues().stream()
                    .filter(m -> m.what == BassClientStateMachine.UPDATE_BCAST_SOURCE)
                    .findFirst();
        assertThat(msg.isPresent()).isTrue();
    }
}
