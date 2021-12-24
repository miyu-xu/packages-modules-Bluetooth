/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.SparseArray;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScanStats class helps keep track of information about scans
 * on a per application basis.
 * @hide
 */
/*package*/ class AppAdvertiseStats {
    private static final String TAG = AppAdvertiseStats.class.getSimpleName();

    static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss");

    static final String[] PHY_LE_STRINGS = {"LE_1M", "LE_2M", "LE_CODED"};

    // ContextMap here is needed to grab Apps and Connections
    ContextMap mContextMap;

    // GattService is needed to add scan event protos to be dumped later
    GattService mGattService;

    class AppAdvertiserData {
        public boolean includeDeviceName = false;
        public boolean includeTxPowerLevel = false;
        public SparseArray<byte[]> manufacturerData;
        public Map<ParcelUuid, byte[]> serviceData;
        public List<ParcelUuid> serviceUuids;
        public List<ParcelUuid> serviceSolicitationUuids;
        AppAdvertiserData(boolean includeDeviceName, boolean includeTxPowerLevel,
                SparseArray<byte[]> manufacturerData, Map<ParcelUuid, byte[]> serviceData,
                List<ParcelUuid> serviceUuids, List<ParcelUuid> serviceSolicitationUuids) {
            this.includeDeviceName = includeDeviceName;
            this.includeTxPowerLevel = includeTxPowerLevel;
            this.manufacturerData = manufacturerData;
            this.serviceData = serviceData;
            this.serviceUuids = serviceUuids;
            this.serviceSolicitationUuids = serviceSolicitationUuids;
        }
    }

    class AppAdvertiserRecord {
        public long startTime = 0;
        public long stopTime = 0;
        public long elapsedTime = 0;
        AppAdvertiserRecord(long startTime) {
            this.startTime = startTime;
        }
    }

    public int appUid;
    public String appName;
    public int id;
    private int mPrimaryPhy = BluetoothDevice.PHY_LE_1M;
    private int mSecondaryPhy = BluetoothDevice.PHY_LE_1M;
    private int mInterval = 0;
    private int mTxPowerLevel = 0;
    private boolean mLegacy = false;
    private boolean mAnonymous = false;
    private boolean mConnectable = false;
    private boolean mScannable = false;
    private AppAdvertiserData mAdvertisingData = null;
    private AppAdvertiserData mScanResponseData = null;
    private AppAdvertiserData mPeriodicAdvertisingData = null;
    private boolean mPeriodicIncludeTxPower = false;
    private int mPeriodicInterval = 0;
    private int mDuration = 0;
    private int mMaxExtendedAdvertisingEvents = 0;
    private AppAdvertiserRecord mAdvertiserRecord;

    private static List<AppAdvertiseStats> sLastAdvertises = new ArrayList<AppAdvertiseStats>();
    private static HashMap<Integer, AppAdvertiseStats> sOngoingAdvertises =
            new HashMap<Integer, AppAdvertiseStats>();

    AppAdvertiseStats(int appUid, int id, String name, ContextMap map, GattService service) {
        this.appUid = appUid;
        this.id = id;
        this.appName = name;
        this.mContextMap = map;
        this.mGattService = service;
    }

    synchronized void recordAdvertiseStart(AdvertisingSetParameters parameters,
            AdvertiseData advertiseData, AdvertiseData scanResponse,
            PeriodicAdvertisingParameters periodicParameters, AdvertiseData periodicData,
            int duration, int maxExtAdvEvents) {
        mAdvertiserRecord = new AppAdvertiserRecord(SystemClock.elapsedRealtime());
        mPrimaryPhy = parameters.getPrimaryPhy();
        mSecondaryPhy = parameters.getSecondaryPhy();
        mInterval = parameters.getInterval();
        mTxPowerLevel = parameters.getTxPowerLevel();
        mLegacy = parameters.isLegacy();
        mAnonymous = parameters.isAnonymous();
        mConnectable = parameters.isConnectable();
        mScannable = parameters.isScannable();
        if (advertiseData != null) {
            mAdvertisingData = new AppAdvertiserData(advertiseData.getIncludeDeviceName(),
                    advertiseData.getIncludeTxPowerLevel(),
                    advertiseData.getManufacturerSpecificData(),
                    advertiseData.getServiceData(),
                    advertiseData.getServiceUuids(),
                    advertiseData.getServiceSolicitationUuids());
        }

        if (scanResponse != null) {
            mScanResponseData = new AppAdvertiserData(scanResponse.getIncludeDeviceName(),
                    scanResponse.getIncludeTxPowerLevel(),
                    scanResponse.getManufacturerSpecificData(),
                    scanResponse.getServiceData(),
                    scanResponse.getServiceUuids(),
                    scanResponse.getServiceSolicitationUuids());
        }

        if (periodicData != null) {
            mPeriodicAdvertisingData = new AppAdvertiserData(periodicData.getIncludeDeviceName(),
                    periodicData.getIncludeTxPowerLevel(),
                    periodicData.getManufacturerSpecificData(),
                    periodicData.getServiceData(),
                    periodicData.getServiceUuids(),
                    periodicData.getServiceSolicitationUuids());
        }

        if (periodicParameters != null) {
            mPeriodicIncludeTxPower = periodicParameters.getIncludeTxPower();
            mPeriodicInterval = periodicParameters.getInterval();
        }

        mDuration = duration;
        mMaxExtendedAdvertisingEvents = maxExtAdvEvents;

        synchronized (sOngoingAdvertises) {
            sOngoingAdvertises.put(id, this);
        }
    }

    synchronized void recordAdvertiseStop() {
        synchronized (sOngoingAdvertises) {
            AppAdvertiseStats stats = sOngoingAdvertises.get(id);
            stats.mAdvertiserRecord.stopTime = SystemClock.elapsedRealtime();
            sOngoingAdvertises.remove(id);
            sLastAdvertises.add(stats);
            if (sLastAdvertises.size() > 5) {
                sLastAdvertises.remove(0);
            }
        }
    }

    synchronized void setAdvertierIdByRegId(int regId, int advertiserId) {
        synchronized (sOngoingAdvertises) {
            AppAdvertiseStats stats = sOngoingAdvertises.get(regId);
            sOngoingAdvertises.remove(regId);
            sOngoingAdvertises.put(advertiserId, stats);
            this.id = advertiserId;
        }
    }

    static String printByteArrayInHex(byte[] data) {
        final StringBuilder hex = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    static synchronized void dumpAppAdvertiserData(StringBuilder sb, AppAdvertiserData advData) {
        sb.append("\n          └Include Device Name                          : "
                + advData.includeDeviceName);
        sb.append("\n          └Include Tx Power Level                       : "
                + advData.includeTxPowerLevel);

        if (advData.manufacturerData.size() > 0) {
            sb.append("\n          └Manufacturer Data                            : ");
            for (int i = 0; i < advData.manufacturerData.size(); i++) {
                sb.append("\n            [" + Integer.toHexString(advData.manufacturerData.keyAt(i))
                        + ", " + printByteArrayInHex(advData.manufacturerData.valueAt(i)) + "]");
            }
        }

        if (!advData.serviceData.isEmpty()) {
            sb.append("\n          └Service Data(UUID, length of data)           : ");
            for (ParcelUuid uuid : advData.serviceData.keySet()) {
                sb.append("\n            [" + uuid + ", "
                        + advData.serviceData.get(uuid).length + "]");
            }
        }

        if (!advData.serviceUuids.isEmpty()) {
            sb.append("\n          └Service Uuids                                : \n            "
                    + advData.serviceUuids.toString());
        }

        if (!advData.serviceSolicitationUuids.isEmpty()) {
            sb.append("\n          └serviceSolicitationUuids                     : \n            "
                    + advData.serviceSolicitationUuids.toString());
        }
    }

    static synchronized void dumpAppAdvertiseStats(StringBuilder sb, AppAdvertiseStats stats) {
        sb.append("\n      └Duration(10ms unit)                              : "
                + stats.mDuration);
        sb.append("\n      └Maximum number of extended advertising events    : "
                + stats.mMaxExtendedAdvertisingEvents);
        sb.append("\n      └Advertising:");
        sb.append("\n        └Interval(0.625ms)                              : "
                + stats.mInterval);
        sb.append("\n        └TX POWER(dbm)                                  : "
                + stats.mTxPowerLevel);
        sb.append("\n        └Primary Phy                                    : "
                + PHY_LE_STRINGS[stats.mPrimaryPhy - 1]);
        sb.append("\n        └Secondary Phy                                  : "
                + PHY_LE_STRINGS[stats.mSecondaryPhy - 1]);
        sb.append("\n        └Legacy                                         : "
                + stats.mLegacy);
        sb.append("\n        └Anonymous                                      : "
                + stats.mAnonymous);
        sb.append("\n        └Connectable                                    : "
                + stats.mConnectable);
        sb.append("\n        └Scannable                                      : "
                + stats.mScannable);

        if (stats.mAdvertisingData != null) {
            sb.append("\n        └Advertise Data:");
            dumpAppAdvertiserData(sb, stats.mAdvertisingData);
        }

        if (stats.mScanResponseData != null) {
            sb.append("\n        └Scan Response:");
            dumpAppAdvertiserData(sb, stats.mScanResponseData);
        }

        if (stats.mPeriodicAdvertisingData != null) {
            sb.append("\n        └Periodic Advertise Data:");
            dumpAppAdvertiserData(sb, stats.mPeriodicAdvertisingData);
        }

        if (stats.mPeriodicInterval > 0) {
            sb.append("\n        └Periodic Include TxPower                       : "
                    + stats.mPeriodicIncludeTxPower);
            sb.append("\n        └Periodic Interval(1.25ms)                      : "
                    + stats.mPeriodicInterval);
        }

        sb.append("\n");
    }

    static synchronized void dumpToString(StringBuilder sb) {
        long currentTime = System.currentTimeMillis();
        long currentRealTime = SystemClock.elapsedRealtime();

        if (!sLastAdvertises.isEmpty()) {
            sb.append("\n  last " + sLastAdvertises.size() + " advertising:");
            for (int i = 0; i < sLastAdvertises.size(); i++) {
                AppAdvertiseStats stats = sLastAdvertises.get(i);
                Date timestamp = new Date(currentTime - currentRealTime
                        + stats.mAdvertiserRecord.startTime);
                Date StopTimestamp = new Date(currentTime - currentRealTime
                        + stats.mAdvertiserRecord.stopTime);

                sb.append("\n    " + stats.appName);
                sb.append("\n     Advertising ID                                     : "
                        + stats.id);
                sb.append("\n      └Start time                                       : "
                        + DATE_FORMAT.format(timestamp));
                sb.append("\n      └Stop time                                        : "
                        + DATE_FORMAT.format(StopTimestamp));
                dumpAppAdvertiseStats(sb, stats);
            }
            sb.append("\n");
        }

        if (!sOngoingAdvertises.isEmpty()) {
            sb.append("  Total number of ongoing advertising                   : "
                    + sOngoingAdvertises.size());
            sb.append("\n  Ongoing advertising:");
            for (Integer key : sOngoingAdvertises.keySet()) {
                AppAdvertiseStats stats = sOngoingAdvertises.get(key);
                Date timestamp = new Date(currentTime - currentRealTime
                        + stats.mAdvertiserRecord.startTime);
                sb.append("\n    " + stats.appName);
                sb.append("\n     Advertising ID                                     : "
                        + stats.id);
                sb.append("\n      └Start time                                       : "
                        + DATE_FORMAT.format(timestamp));
                sb.append("\n      └Elapsed time                                     : "
                        + (currentRealTime - stats.mAdvertiserRecord.startTime) + "ms ");
                dumpAppAdvertiseStats(sb, stats);
            }
        }
        sb.append("\n");
    }
}
