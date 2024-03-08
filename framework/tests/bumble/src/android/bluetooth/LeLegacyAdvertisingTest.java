/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class LeLegacyAdvertisingTest {
    private static final int TIMEOUT_SET_SCAN_RESPONSE_DATA_MS = 5_000;

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Test
    public void setScanResponseDataOver31Bytes() throws Exception {
        final BluetoothLeAdvertiser advertiser =
                BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        // Set legacy scan mode
        AdvertisingSetParameters params =
                new AdvertisingSetParameters.Builder()
                        .setLegacyMode(true)
                        .setScannable(true)
                        .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                        .build();

        AdvertiseData advertiseData =
                new AdvertiseData.Builder()
                        .addServiceUuid(new ParcelUuid(UUID.randomUUID()))
                        .build();

        final CountDownLatch latch = new CountDownLatch(1);

        AdvertisingSetCallback callback =
                new AdvertisingSetCallback() {
                    @Override
                    public void onAdvertisingSetStarted(
                            AdvertisingSet advertisingSet, int txPower, int status) {
                        // Should be greater than 31
                        int scanResponseDataLengthWhichExccedsLimit = 50;
                        advertisingSet.setScanResponseData(
                                createAdvertiseData(scanResponseDataLengthWhichExccedsLimit));
                    }

                    @Override
                    public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
                        if (status == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                            advertiser.stopAdvertisingSet(this);
                            latch.countDown();
                        }
                    }
                };

        advertiser.startAdvertisingSet(params, advertiseData, null, null, null, callback);
        assertThat(latch.await(TIMEOUT_SET_SCAN_RESPONSE_DATA_MS, TimeUnit.MILLISECONDS)).isTrue();
    }

    private AdvertiseData createAdvertiseData(int length) {
        if (length <= 4) {
            return null;
        }

        // Create an arbitrary manufacturer specific data
        int manufacturerId = 13;
        byte[] manufacturerSpecificData = new byte[length - 4];
        for (int i = 0; i < manufacturerSpecificData.length; i++) {
            manufacturerSpecificData[i] = (byte) i;
        }

        return new AdvertiseData.Builder()
                .addManufacturerData(manufacturerId, manufacturerSpecificData)
                .build();
    }
}
