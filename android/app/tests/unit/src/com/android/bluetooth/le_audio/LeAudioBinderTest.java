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

package com.android.bluetooth.le_audio;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.AttributionSource;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.x.com.android.modules.utils.SynchronousResultReceiver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

public class LeAudioBinderTest {
    @Mock
    private LeAudioService mMockService;

    private LeAudioService.BluetoothLeAudioBinder mBinder;
    private BluetoothAdapter mAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mBinder = new LeAudioService.BluetoothLeAudioBinder(mMockService);
    }

    @After
    public void cleanUp() {
        mBinder.cleanup();
    }

    @Test
    public void connect() {
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 0);
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();

        mBinder.connect(device, source, recv);
        verify(mMockService).connect(device);
    }

    @Test
    public void disconnect() {
        BluetoothDevice device = TestUtils.getTestDevice(mAdapter, 0);
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();

        mBinder.disconnect(device, source, recv);
        verify(mMockService).disconnect(device);
    }

    @Test
    public void getConnectedDevices() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getConnectedDevices(source, recv);
        verify(mMockService).getConnectedDevices();
    }

    @Test
    public void getConnectedGroupLeadDevice() {
        int groupId = 1;
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getConnectedGroupLeadDevice(groupId, source, recv);
        verify(mMockService).getConnectedGroupLeadDevice(groupId);
    }

    @Test
    public void getDevicesMatchingConnectionStates() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }









    @Test
    public void getConnectionState() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setActiveDevice() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getActiveDevices() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getAudioLocation() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setConnectionPolicy() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getConnectionPolicy() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setCcidInformation() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getGroupId() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void groupAddNode() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setInCall() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setInactiveForHfpHandover() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void groupRemoveNode() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setVolume() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void registerCallback() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void unregisterCallback() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void registerLeBroadcastCallback() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void unregisterLeBroadcastCallback() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void startBroadcast() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void stopBroadcast() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void updateBroadcast() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void isPlaying() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getAllBroadcastMetadata() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getMaximumNumberOfBroadcasts() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void getCodecStatus() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }

    @Test
    public void setCodecConfigPreference() {
        int[] states = new int[] {BluetoothProfile.STATE_DISCONNECTED };
        AttributionSource source = new AttributionSource.Builder(0).build();
        final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                SynchronousResultReceiver.get();

        mBinder.getDevicesMatchingConnectionStates(states, source, recv);
        verify(mMockService).getDevicesMatchingConnectionStates(states);
    }
}
