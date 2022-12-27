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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.IBluetoothOobDataCallback;
import android.bluetooth.IncomingRfcommSocketInfo;
import android.content.AttributionSource;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import com.android.bluetooth.x.com.android.modules.utils.SynchronousResultReceiver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.FileDescriptor;

public class AdapterServiceBinderTest {
    @Mock private AdapterService mService;
    @Mock private PackageManager mPackageManager;
    @Mock private AdapterProperties mAdapterProperties;

    private AdapterService.AdapterServiceBinder mBinder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mService.mAdapterProperties = mAdapterProperties;
        doReturn(true).when(mService).isAvailable();
        doReturn(mPackageManager).when(mService).getPackageManager();
        doReturn(new String[] { "com.android.bluetooth.btservice" })
                .when(mPackageManager).getPackagesForUid(anyInt());
        mBinder = new AdapterService.AdapterServiceBinder(mService);
    }

    @After
    public void cleaUp() {
        mBinder.cleanup();
    }

    @Test
    public void getAddress() {
        mBinder.getAddress();
        verify(mService.mAdapterProperties).getAddress();
    }

    @Test
    public void dump() {
        FileDescriptor fd = new FileDescriptor();
        String[] args = new String[] { };
        mBinder.dump(fd, args);
        verify(mService).dump(any(), any(), any());

        Mockito.clearInvocations(mService);
        mBinder.cleanup();
        mBinder.dump(fd, args);
        verify(mService, never()).dump(any(), any(), any());
    }

    @Test
    public void generateLocalOobData() {
        int transport = 0;
        IBluetoothOobDataCallback cb = Mockito.mock(IBluetoothOobDataCallback.class);
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();

        mBinder.generateLocalOobData(transport, cb, source, recv);
        verify(mService).generateLocalOobData(transport, cb);

        Mockito.clearInvocations(mService);
        mBinder.cleanup();
        recv = SynchronousResultReceiver.get();
        mBinder.generateLocalOobData(transport, cb, source, recv);
        verify(mService, never()).generateLocalOobData(transport, cb);
    }

    @Test
    public void getBluetoothClass() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.getBluetoothClass(source, recv);
        verify(mService.mAdapterProperties).getBluetoothClass();
    }

    @Test
    public void getIoCapability() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.getIoCapability(source, recv);
        verify(mService.mAdapterProperties).getIoCapability();
    }

    @Test
    public void getLeIoCapability() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.getLeIoCapability(source, recv);
        verify(mService.mAdapterProperties).getLeIoCapability();
    }

    @Test
    public void getLeMaximumAdvertisingDataLength() {
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.getLeMaximumAdvertisingDataLength(recv);
        verify(mService).getLeMaximumAdvertisingDataLength();
    }

    @Test
    public void getScanMode() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.getScanMode(source, recv);
        verify(mService.mAdapterProperties).getScanMode();
    }

    @Test
    public void isA2dpOffloadEnabled() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.isA2dpOffloadEnabled(source, recv);
        verify(mService).isA2dpOffloadEnabled();
    }

    @Test
    public void isActivityAndEnergyReportingSupported() {
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.isActivityAndEnergyReportingSupported(recv);
        verify(mService.mAdapterProperties).isActivityAndEnergyReportingSupported();
    }

    @Test
    public void isLe2MPhySupported() {
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.isLe2MPhySupported(recv);
        verify(mService).isLe2MPhySupported();
    }

    @Test
    public void isLeCodedPhySupported() {
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.isLeCodedPhySupported(recv);
        verify(mService).isLeCodedPhySupported();
    }

    @Test
    public void isLeExtendedAdvertisingSupported() {
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.isLeExtendedAdvertisingSupported(recv);
        verify(mService).isLeExtendedAdvertisingSupported();
    }

    @Test
    public void removeActiveDevice() {
        int profiles = BluetoothAdapter.ACTIVE_DEVICE_ALL;
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        AttributionSource source = new AttributionSource.Builder(0).build();
        mBinder.removeActiveDevice(profiles, source, recv);
        verify(mService).setActiveDevice(null, profiles);
    }

    @Test
    public void reportActivityInfo() {
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Void> recv = SynchronousResultReceiver.get();
        mBinder.reportActivityInfo(source, recv);
        verify(mService).reportActivityInfo();
    }

    @Test
    public void retrievePendingSocketForServiceRecord() {
        ParcelUuid uuid = ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB");
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<IncomingRfcommSocketInfo> recv = SynchronousResultReceiver.get();
        mBinder.retrievePendingSocketForServiceRecord(uuid, source, recv);
        verify(mService).retrievePendingSocketForServiceRecord(uuid, source);
    }

    @Test
    public void setBluetoothClass() {
        BluetoothClass btClass = new BluetoothClass(0);
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        mBinder.setBluetoothClass(btClass, source, recv);
        verify(mService.mAdapterProperties).setBluetoothClass(btClass);
    }

    @Test
    public void setIoCapability() {
        int capability = BluetoothAdapter.IO_CAPABILITY_MAX - 1;
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        mBinder.setIoCapability(capability, source, recv);
        verify(mService.mAdapterProperties).setIoCapability(capability);
    }

    @Test
    public void setLeIoCapability() {
        int capability = BluetoothAdapter.IO_CAPABILITY_MAX - 1;
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        mBinder.setLeIoCapability(capability, source, recv);
        verify(mService.mAdapterProperties).setLeIoCapability(capability);
    }

    @Test
    public void stopRfcommListener() {
        ParcelUuid uuid = ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB");
        AttributionSource source = new AttributionSource.Builder(0).build();
        SynchronousResultReceiver<IncomingRfcommSocketInfo> recv = SynchronousResultReceiver.get();
        mBinder.stopRfcommListener(uuid, source, recv);
        verify(mService).stopRfcommListener(uuid, source);
    }
}
