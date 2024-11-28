/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.vc;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothVolumeControlCallback;
import android.content.AttributionSource;
import android.os.Handler;
import android.os.test.TestLooper;
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.filters.SmallTest;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import platform.test.runner.parameterized.ParameterizedAndroidJunit4;
import platform.test.runner.parameterized.Parameters;

import java.util.List;

@SmallTest
@RunWith(ParameterizedAndroidJunit4.class)
public class VolumeControlBinderTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule;

    @Mock private VolumeControlService mService;

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final AttributionSource mAttributionSource = mAdapter.getAttributionSource();
    private final BluetoothDevice mDevice = TestUtils.getTestDevice(mAdapter, 0);

    private TestLooper mLooper;
    private VolumeControlBinder mBinder;

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.allCombinationsOf(Flags.FLAG_VCP_ON_MAIN_LOOPER);
    }

    public VolumeControlBinderTest(FlagsParameterization flags) {
        mSetFlagsRule = new SetFlagsRule(flags);
    }

    @Before
    public void setUp() throws Exception {
        mLooper = new TestLooper();
        mLooper.startAutoDispatch();
        doReturn(new Handler(mLooper.getLooper())).when(mService).getHandler();
        mBinder = new VolumeControlBinder(mService);
    }

    @After
    public void tearDown() {
        mLooper.stopAutoDispatchAndIgnoreExceptions();
    }

    @Test
    public void getConnectedDevices() {
        assertThrows(NullPointerException.class, () -> mBinder.getConnectedDevices(null));
        mBinder.getConnectedDevices(mAttributionSource);
        verify(mService).getConnectedDevices();
    }

    @Test
    public void getDevicesMatchingConnectionStates() {
        assertThrows(
                NullPointerException.class,
                () -> mBinder.getDevicesMatchingConnectionStates(null, null));
        mBinder.getDevicesMatchingConnectionStates(null, mAttributionSource);
        verify(mService).getDevicesMatchingConnectionStates(any());
    }

    @Test
    public void getConnectionState() {
        assertThrows(NullPointerException.class, () -> mBinder.getConnectionState(mDevice, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.getConnectionState(null, mAttributionSource));

        mBinder.getConnectionState(mDevice, mAttributionSource);
        verify(mService).getConnectionState(eq(mDevice));
    }

    @Test
    public void setConnectionPolicy() {
        assertThrows(
                NullPointerException.class,
                () ->
                        mBinder.setConnectionPolicy(
                                mDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED, null));
        assertThrows(
                NullPointerException.class,
                () ->
                        mBinder.setConnectionPolicy(
                                null,
                                BluetoothProfile.CONNECTION_POLICY_ALLOWED,
                                mAttributionSource));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mBinder.setConnectionPolicy(
                                mDevice,
                                BluetoothProfile.CONNECTION_POLICY_UNKNOWN,
                                mAttributionSource));

        mBinder.setConnectionPolicy(
                mDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED, mAttributionSource);
        verify(mService)
                .setConnectionPolicy(eq(mDevice), eq(BluetoothProfile.CONNECTION_POLICY_ALLOWED));
    }

    @Test
    public void getConnectionPolicy() {
        assertThrows(NullPointerException.class, () -> mBinder.getConnectionPolicy(mDevice, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.getConnectionPolicy(null, mAttributionSource));
        mBinder.getConnectionPolicy(mDevice, mAttributionSource);
        verify(mService).getConnectionPolicy(eq(mDevice));
    }

    @Test
    public void isVolumeOffsetAvailable() {
        assertThrows(
                NullPointerException.class, () -> mBinder.isVolumeOffsetAvailable(mDevice, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.isVolumeOffsetAvailable(null, mAttributionSource));
        mBinder.isVolumeOffsetAvailable(mDevice, mAttributionSource);
        verify(mService).isVolumeOffsetAvailable(eq(mDevice));
    }

    @Test
    public void getNumberOfVolumeOffsetInstances() {
        assertThrows(
                NullPointerException.class,
                () -> mBinder.getNumberOfVolumeOffsetInstances(mDevice, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.getNumberOfVolumeOffsetInstances(null, mAttributionSource));
        mBinder.getNumberOfVolumeOffsetInstances(mDevice, mAttributionSource);
        verify(mService).getNumberOfVolumeOffsetInstances(eq(mDevice));
    }

    @Test
    public void setVolumeOffset() {
        int id = 1;
        int vol = 42;
        assertThrows(
                NullPointerException.class, () -> mBinder.setVolumeOffset(mDevice, id, vol, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.setVolumeOffset(null, id, vol, mAttributionSource));
        mBinder.setVolumeOffset(mDevice, id, vol, mAttributionSource);
        verify(mService).setVolumeOffset(eq(mDevice), eq(id), eq(vol));
    }

    @Test
    public void setDeviceVolume() {
        int vol = 42;
        boolean isGroupOp = true;
        assertThrows(
                NullPointerException.class,
                () -> mBinder.setDeviceVolume(mDevice, vol, isGroupOp, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.setDeviceVolume(null, vol, isGroupOp, mAttributionSource));
        mBinder.setDeviceVolume(mDevice, vol, isGroupOp, mAttributionSource);
        verify(mService).setDeviceVolume(eq(mDevice), eq(vol), eq(isGroupOp));
    }

    @Test
    public void setGroupVolume() {
        int id = 1;
        int vol = 42;
        assertThrows(NullPointerException.class, () -> mBinder.setGroupVolume(id, vol, null));
        mBinder.setGroupVolume(id, vol, mAttributionSource);
        verify(mService).setGroupVolume(eq(id), eq(vol));
    }

    @Test
    public void getGroupVolume() {
        int id = 1;
        assertThrows(NullPointerException.class, () -> mBinder.getGroupVolume(id, null));
        mBinder.getGroupVolume(id, mAttributionSource);
        verify(mService).getGroupVolume(eq(id));
    }

    @Test
    public void setGroupActive() {
        int id = 1;
        boolean active = true;
        assertThrows(NullPointerException.class, () -> mBinder.setGroupActive(id, active, null));
        mBinder.setGroupActive(id, active, mAttributionSource);
        verify(mService).setGroupActive(eq(id), eq(active));
    }

    @Test
    public void mute() {
        assertThrows(NullPointerException.class, () -> mBinder.mute(mDevice, null));
        assertThrows(NullPointerException.class, () -> mBinder.mute(null, mAttributionSource));
        mBinder.mute(mDevice, mAttributionSource);
        verify(mService).mute(eq(mDevice));
    }

    @Test
    public void muteGroup() {
        int id = 1;
        assertThrows(NullPointerException.class, () -> mBinder.muteGroup(id, null));
        mBinder.muteGroup(id, mAttributionSource);
        verify(mService).muteGroup(eq(id));
    }

    @Test
    public void unmute() {
        assertThrows(NullPointerException.class, () -> mBinder.unmute(mDevice, null));
        assertThrows(NullPointerException.class, () -> mBinder.unmute(null, mAttributionSource));
        mBinder.unmute(mDevice, mAttributionSource);
        verify(mService).unmute(eq(mDevice));
    }

    @Test
    public void unmuteGroup() {
        int id = 1;
        assertThrows(NullPointerException.class, () -> mBinder.unmuteGroup(id, null));
        mBinder.unmuteGroup(id, mAttributionSource);
        verify(mService).unmuteGroup(eq(id));
    }

    @Test
    public void registerCallback() {
        IBluetoothVolumeControlCallback callback =
                Mockito.mock(IBluetoothVolumeControlCallback.class);
        assertThrows(NullPointerException.class, () -> mBinder.registerCallback(callback, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.registerCallback(null, mAttributionSource));
        mBinder.registerCallback(callback, mAttributionSource);
        verify(mService).registerCallback(eq(callback));
    }

    @Test
    public void unregisterCallback() {
        IBluetoothVolumeControlCallback callback =
                Mockito.mock(IBluetoothVolumeControlCallback.class);
        assertThrows(NullPointerException.class, () -> mBinder.unregisterCallback(callback, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.unregisterCallback(null, mAttributionSource));
        mBinder.unregisterCallback(callback, mAttributionSource);
        verify(mService).unregisterCallback(eq(callback));
    }

    @Test
    public void notifyNewRegisteredCallback() {
        IBluetoothVolumeControlCallback callback =
                Mockito.mock(IBluetoothVolumeControlCallback.class);
        assertThrows(
                NullPointerException.class,
                () -> mBinder.notifyNewRegisteredCallback(callback, null));
        assertThrows(
                NullPointerException.class,
                () -> mBinder.notifyNewRegisteredCallback(null, mAttributionSource));
        mBinder.notifyNewRegisteredCallback(callback, mAttributionSource);
        verify(mService).notifyNewRegisteredCallback(eq(callback));
    }
}
