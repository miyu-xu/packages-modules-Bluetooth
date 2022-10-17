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

package com.android.bluetooth.gatt;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

/**
 * Test cases for {@link ScanFilterQueue}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ScanFilterQueueTest {

    @Test
    public void scanFilterQueueParams() {
        ScanFilterQueue queue = new ScanFilterQueue();

        String address = "address";
        byte ttpe = 0;
        byte[] irk = new byte[0];
        queue.addDeviceAddress(address, ttpe, irk);

        queue.addServiceChanged();

        UUID uuid = UUID.randomUUID();
        queue.addUuid(uuid);

        String name = "name";
        queue.addName(name);

        int company = 0;
        byte[] data = new byte[0];
        queue.addManufacturerData(company, data);

        byte[] serviceData = new byte[0];
        byte[] serviceDataMask = new byte[0];
        queue.addServiceData(serviceData, serviceDataMask);

        int adType = 0;
        byte[] adData = new byte[0];
        byte[] adDataMask = new byte[0];
        queue.addAdvertisingDataType(adType, adData, adDataMask);

        assertThat(queue.toArray().length).isEqualTo(7);
    }

    @Test
    public void popFromQueue() {
        ScanFilterQueue queue = new ScanFilterQueue();

        byte[] serviceData = new byte[0];
        byte[] serviceDataMask = new byte[0];
        queue.addServiceData(serviceData, serviceDataMask);

        assertThat(queue.pop()).isNotNull();
    }

    @Test
    public void checkFeatureSelection() {
        ScanFilterQueue queue = new ScanFilterQueue();

        byte[] serviceData = new byte[0];
        byte[] serviceDataMask = new byte[0];
        queue.addServiceData(serviceData, serviceDataMask);

        assertThat(queue.getFeatureSelection()).isEqualTo(64);
    }

    @Test
    public void convertQueueToArray() {
        ScanFilterQueue queue = new ScanFilterQueue();

        byte[] serviceData = new byte[0];
        byte[] serviceDataMask = new byte[0];
        queue.addServiceData(serviceData, serviceDataMask);

        assertThat(queue.toArray().length).isEqualTo(1);
    }

    @Test
    public void queueAddScanFilter() {
        ScanFilterQueue queue = new ScanFilterQueue();

        String name = "name";
        String deviceAddress = "00:11:22:33:FF:EE";
        ParcelUuid serviceUuid = ParcelUuid.fromString(UUID.randomUUID().toString());
        ParcelUuid serviceSolicitationUuid = ParcelUuid.fromString(UUID.randomUUID().toString());
        int manufacturerId = 0;
        byte[] manufacturerData = new byte[0];
        ParcelUuid serviceDataUuid = ParcelUuid.fromString(UUID.randomUUID().toString());
        byte[] serviceData = new byte[0];
        int advertisingDataType = 1;

        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName(name)
                .setDeviceAddress(deviceAddress)
                .setServiceUuid(serviceUuid)
                .setServiceSolicitationUuid(serviceSolicitationUuid)
                .setManufacturerData(manufacturerId, manufacturerData)
                .setServiceData(serviceDataUuid, serviceData)
                .setAdvertisingDataType(advertisingDataType)
                .build();
        queue.addScanFilter(filter);

        assertThat(queue.toArray().length).isEqualTo(7);
    }
}
