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
        byte type = 1;
        byte[] irk = new byte[]{0x02};
        queue.addDeviceAddress(address, type, irk);

        queue.addServiceChanged();

        UUID uuid = UUID.randomUUID();
        queue.addUuid(uuid);

        String name = "name";
        queue.addName(name);

        int company = 2;
        byte[] data = new byte[]{0x04};
        queue.addManufacturerData(company, data);

        byte[] serviceData = new byte[]{0x06};
        byte[] serviceDataMask = new byte[]{0x08};
        queue.addServiceData(serviceData, serviceDataMask);

        int adType = 3;
        byte[] adData = new byte[]{0x10};
        byte[] adDataMask = new byte[]{0x12};
        queue.addAdvertisingDataType(adType, adData, adDataMask);

        ScanFilterQueue.Entry[] entries = queue.toArray();
        int entriesLength = 7;
        assertThat(entries.length).isEqualTo(entriesLength);

        ScanFilterQueue.Entry entry0 = entries[0];
        assertThat(entry0.type).isEqualTo(ScanFilterQueue.TYPE_DEVICE_ADDRESS);
        assertThat(entry0.address).isEqualTo(address);
        assertThat(entry0.addr_type).isEqualTo(type);
        assertThat(entry0.irk).isEqualTo(irk);

        ScanFilterQueue.Entry entry1 = entries[1];
        assertThat(entry1.type).isEqualTo(ScanFilterQueue.TYPE_SERVICE_DATA_CHANGED);

        ScanFilterQueue.Entry entry2 = entries[2];
        assertThat(entry2.type).isEqualTo(ScanFilterQueue.TYPE_SERVICE_UUID);
        assertThat(entry2.uuid).isEqualTo(uuid);

        ScanFilterQueue.Entry entry3 = entries[3];
        assertThat(entry3.type).isEqualTo(ScanFilterQueue.TYPE_LOCAL_NAME);
        assertThat(entry3.name).isEqualTo(name);

        ScanFilterQueue.Entry entry4 = entries[4];
        assertThat(entry4.type).isEqualTo(ScanFilterQueue.TYPE_MANUFACTURER_DATA);
        assertThat(entry4.company).isEqualTo(company);
        assertThat(entry4.data).isEqualTo(data);

        ScanFilterQueue.Entry entry5 = entries[5];
        assertThat(entry5.type).isEqualTo(ScanFilterQueue.TYPE_SERVICE_DATA);
        assertThat(entry5.data).isEqualTo(serviceData);
        assertThat(entry5.data_mask).isEqualTo(serviceDataMask);

        ScanFilterQueue.Entry entry6 = entries[6];
        assertThat(entry6.type).isEqualTo(ScanFilterQueue.TYPE_ADVERTISING_DATA_TYPE);
        assertThat(entry6.ad_type).isEqualTo(adType);
        assertThat(entry6.data).isEqualTo(adData);
        assertThat(entry6.data_mask).isEqualTo(adDataMask);
    }

    @Test
    public void popFromQueue() {
        ScanFilterQueue queue = new ScanFilterQueue();

        byte[] serviceData = new byte[]{0x02};
        byte[] serviceDataMask = new byte[]{0x04};
        queue.addServiceData(serviceData, serviceDataMask);

        ScanFilterQueue.Entry entry = queue.pop();
        assertThat(entry.data).isEqualTo(serviceData);
        assertThat(entry.data_mask).isEqualTo(serviceDataMask);
    }

    @Test
    public void checkFeatureSelection() {
        ScanFilterQueue queue = new ScanFilterQueue();

        byte[] serviceData = new byte[]{0x02};
        byte[] serviceDataMask = new byte[]{0x04};
        queue.addServiceData(serviceData, serviceDataMask);

        int feature = 1 << ScanFilterQueue.TYPE_SERVICE_DATA;
        assertThat(queue.getFeatureSelection()).isEqualTo(feature);
    }

    @Test
    public void convertQueueToArray() {
        ScanFilterQueue queue = new ScanFilterQueue();

        byte[] serviceData = new byte[]{0x02};
        byte[] serviceDataMask = new byte[]{0x04};
        queue.addServiceData(serviceData, serviceDataMask);

        ScanFilterQueue.Entry[] entries = queue.toArray();
        int entriesLength = 1;
        assertThat(entries.length).isEqualTo(entriesLength);

        ScanFilterQueue.Entry entry = entries[0];
        assertThat(entry.data).isEqualTo(serviceData);
        assertThat(entry.data_mask).isEqualTo(serviceDataMask);
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

        int numOfEntries = 7;
        assertThat(queue.toArray().length).isEqualTo(numOfEntries);
    }
}
