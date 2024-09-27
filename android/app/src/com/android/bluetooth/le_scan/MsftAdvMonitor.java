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

package com.android.bluetooth.le_scan;

import android.bluetooth.le.ScanFilter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

/** Helper class used to manage MSFT Advertisement Monitors. */
class MsftAdvMonitor {
    /* Only pattern filtering is supported currently */
    // private static final int MSFT_CONDITION_TYPE_ALL = 0x00;
    private static final int MSFT_CONDITION_TYPE_PATTERNS = 0x01;
    // private static final int MSFT_CONDITION_TYPE_UUID = 0x02;
    // private static final int MSFT_CONDITION_TYPE_IRK = 0x03;
    // private static final int MSFT_CONDITION_TYPE_ADDRESS = 0x04;

    private static final int FILTER_PATTERN_START_POSITION = 0;

    static class Monitor {
        public byte rssi_threshold_high;
        public byte rssi_threshold_low;
        public byte rssi_threshold_low_time_interval;
        public byte rssi_sampling_period;
        public byte condition_type;
    }

    static class Pattern {
        public byte ad_type;
        public byte start_byte;
        public byte[] pattern;
    }

    static class Address {
        byte addr_type;
        String bd_addr;
    }

    private final Monitor mMonitor = new Monitor();
    private final ArrayList<Pattern> mPatterns = new ArrayList<>();
    private final Address mAddress = new Address();

    // Constructor that converts an APCF-friendly filter to an MSFT-friendly format
    public MsftAdvMonitor(ScanFilter filter) {
        // Hardcoded values taken from CrOS defaults
        mMonitor.rssi_threshold_high = (byte) 0xBF; // hardcoded to 191
        mMonitor.rssi_threshold_low = (byte) 0xB0; // hardcoded to 176
        mMonitor.rssi_threshold_low_time_interval = 0x28; // hardcoded to 40s
        mMonitor.rssi_sampling_period = 0x05; // hardcoded to 500 ms
        mMonitor.condition_type = MSFT_CONDITION_TYPE_PATTERNS;

        if (filter.getServiceDataUuid() != null) {
            Pattern pattern = new Pattern();
            pattern.ad_type = (byte) 0x16; // Bluetooth Core Spec Part A, Section 1
            pattern.start_byte = FILTER_PATTERN_START_POSITION;

            // Extract the 16-bit UUID (third and fourth bytes) from the 128-bit
            // UUID in reverse endianness
            UUID uuid = filter.getServiceDataUuid().getUuid();
            ByteBuffer bb = ByteBuffer.allocate(16); // 16 byte (128 bit) UUID
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            pattern.pattern = new byte[] {bb.get(3), bb.get(2)};

            mPatterns.add(pattern);
        } else if (filter.getAdvertisingData() != null && filter.getAdvertisingData().length != 0) {
            Pattern pattern = new Pattern();
            pattern.ad_type = (byte) filter.getAdvertisingDataType();
            pattern.start_byte = FILTER_PATTERN_START_POSITION;
            pattern.pattern = filter.getAdvertisingData();
            mPatterns.add(pattern);
        }

        if (filter.getDeviceAddress() != null) {
            mAddress.addr_type = (byte) filter.getAddressType();
            mAddress.bd_addr = filter.getDeviceAddress();
        }
    }

    Monitor getMonitor() {
        return mMonitor;
    }

    Pattern[] getPatterns() {
        return mPatterns.toArray(new Pattern[mPatterns.size()]);
    }

    Address getAddress() {
        return mAddress;
    }
}
