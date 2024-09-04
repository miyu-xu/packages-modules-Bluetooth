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

    private Monitor mMonitor;
    private ArrayList<Pattern> mPatterns;
    private Address mAddress;

    // Constructor that converts an APCF-friendly filter to an MSFT-friendly format
    public MsftAdvMonitor(ScanFilter filter) {
        mMonitor = new Monitor();
        mPatterns = new ArrayList<>();
        mAddress = new Address();

        mMonitor.rssi_threshold_high = Byte.MIN_VALUE;
        mMonitor.rssi_threshold_low = Byte.MIN_VALUE;
        mMonitor.rssi_threshold_low_time_interval = 1; // hard coded to 1 s
        mMonitor.rssi_sampling_period = 5; // hard coded to 500 ms
        mMonitor.condition_type = MSFT_CONDITION_TYPE_PATTERNS;

        if (filter.getServiceData() != null && filter.getServiceData().length != 0) {
          Pattern pattern = new Pattern();
          pattern.ad_type = (byte) filter.getAdvertisingDataType();
          pattern.start_byte = FILTER_PATTERN_START_POSITION;
          pattern.pattern = filter.getServiceData();    // TODO ensure is valid
          mPatterns.add(pattern);
        }

        if (filter.getDeviceAddress() != null) {
          mAddress.addr_type = (byte) filter.getAddressType();
          mAddress.bd_addr = filter.getDeviceAddress();
        }
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
