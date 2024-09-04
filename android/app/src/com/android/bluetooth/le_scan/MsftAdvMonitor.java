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

import java.util.ArrayList;

/** Helper class used to manage MSFT Advertisement Monitors. */
/* package */ class MsftAdvMonitor {
    //private static final int MSFT_CONDITION_TYPE_ALL = 0x00;
    private static final int MSFT_CONDITION_TYPE_PATTERNS = 0x01;
    //private static final int MSFT_CONDITION_TYPE_UUID = 0x02;
    //private static final int MSFT_CONDITION_TYPE_IRK = 0x03;
    //private static final int MSFT_CONDITION_TYPE_ADDRESS = 0x04;

    private static final int FILTER_PATTERN_START_POSITION = 0;

    static class Monitor {
        public int rssi_threshold_high;
        public int rssi_threshold_low;
        public int rssi_threshold_low_time_interval;
        public int rssi_sampling_period;
        public int condition_type;
    }

    static class Pattern {
        public int ad_type;
        public int start_byte;
        public byte[] pattern;
    }

    static class Address {
        int addr_type;
        String bd_addr;
    }

    private Monitor mMonitor;
    private ArrayList<Pattern> mPatterns;
    private Address mAddress;

    // Constructor that converts an APCF-friendly filter to an MSFT-friendly format
    public MsftAdvMonitor(ScanFilter filter, int scanIntervalMs, int scanWindowMs) {
        mMonitor = new Monitor();
        mPatterns = new ArrayList<>();
        mAddress = new Address();

        mMonitor.rssi_threshold_high = Byte.MIN_VALUE;
        mMonitor.rssi_threshold_low = Byte.MIN_VALUE;
        mMonitor.rssi_threshold_low_time_interval = scanIntervalMs * 1000;
        mMonitor.rssi_sampling_period = scanWindowMs * 1000;
        mMonitor.condition_type = MSFT_CONDITION_TYPE_PATTERNS;

        Pattern pattern = new Pattern();
        pattern.ad_type = filter.getAdvertisingDataType();
        pattern.start_byte = FILTER_PATTERN_START_POSITION;
        pattern.pattern = filter.getServiceData();
        mPatterns.add(pattern);

        mAddress.addr_type = filter.getAddressType();
        mAddress.bd_addr = filter.getDeviceAddress();
    }

    public Monitor getMonitor() {
      return mMonitor;
    }

    public Pattern[] getPatterns() {
      return mPatterns.toArray(new Pattern[mPatterns.size()]);
    }

    public Address getAddress() {
      return mAddress;
    }
}
