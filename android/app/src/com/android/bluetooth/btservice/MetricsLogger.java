/*
 * Copyright 2018 The Android Open Source Project
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

import static com.android.bluetooth.BtRestrictedStatsLog.RESTRICTED_BLUETOOTH_DEVICE_NAME_REPORTED;

import android.app.AlarmManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.rfcomm.BluetoothRfcommProtoEnums;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.util.proto.ProtoOutputStream;

import com.android.bluetooth.BluetoothMetricsProto.BluetoothLog;
import com.android.bluetooth.BluetoothMetricsProto.BluetoothRemoteDeviceInformation;
import com.android.bluetooth.BluetoothMetricsProto.ProfileConnectionStats;
import com.android.bluetooth.BluetoothMetricsProto.ProfileId;
import com.android.bluetooth.BluetoothStatsLog;
import com.android.bluetooth.BtRestrictedStatsLog;
import com.android.bluetooth.Utils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.modules.utils.build.SdkLevel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Class of Bluetooth Metrics
 */
public class MetricsLogger {
    private static final String TAG = "BluetoothMetricsLogger";
    private static final String BLOOMFILTER_PATH = "/data/misc/bluetooth";
    private static final String BLOOMFILTER_FILE = "/devices_for_metrics";
    public static final String BLOOMFILTER_FULL_PATH = BLOOMFILTER_PATH + BLOOMFILTER_FILE;

    public static final boolean DEBUG = false;

    // 6 hours timeout for counter metrics
    private static final long BLUETOOTH_COUNTER_METRICS_ACTION_DURATION_MILLIS = 6L * 3600L * 1000L;
    private static final int MAX_WORDS_ALLOWED_IN_DEVICE_NAME = 7;

    private static final HashMap<ProfileId, Integer> sProfileConnectionCounts = new HashMap<>();

    HashMap<Integer, Long> mCounters = new HashMap<>();
    private static volatile MetricsLogger sInstance = null;
    private Context mContext = null;
    private AlarmManager mAlarmManager = null;
    private boolean mInitialized = false;
    static final private Object mLock = new Object();
    private BloomFilter<byte[]> mBloomFilter = null;
    protected boolean mBloomFilterInitialized = false;

    private AlarmManager.OnAlarmListener mOnAlarmListener = new AlarmManager.OnAlarmListener () {
        @Override
        public void onAlarm() {
            drainBufferedCounters();
            scheduleDrains();
        }
    };

    public static MetricsLogger getInstance() {
        if (sInstance == null) {
            synchronized (mLock) {
                if (sInstance == null) {
                    sInstance = new MetricsLogger();
                }
            }
        }
        return sInstance;
    }

    /**
     * Allow unit tests to substitute MetricsLogger with a test instance
     *
     * @param instance a test instance of the MetricsLogger
     */
    @VisibleForTesting
    public static void setInstanceForTesting(MetricsLogger instance) {
        Utils.enforceInstrumentationTestMode();
        synchronized (mLock) {
            Log.d(TAG, "setInstanceForTesting(), set to " + instance);
            sInstance = instance;
        }
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public boolean initBloomFilter(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                Log.w(TAG, "MetricsLogger is creating a new Bloomfilter file");
                DeviceBloomfilterGenerator.generateDefaultBloomfilter(path);
            }

            FileInputStream in = new FileInputStream(new File(path));
            mBloomFilter = BloomFilter.readFrom(in, Funnels.byteArrayFunnel());
            mBloomFilterInitialized = true;
        } catch (IOException e1) {
            Log.w(TAG, "MetricsLogger can't read the BloomFilter file.");
            byte[] bloomfilterData = DeviceBloomfilterGenerator.hexStringToByteArray(
                    DeviceBloomfilterGenerator.BLOOM_FILTER_DEFAULT);
            try {
                mBloomFilter = BloomFilter.readFrom(
                        new ByteArrayInputStream(bloomfilterData), Funnels.byteArrayFunnel());
                mBloomFilterInitialized = true;
                Log.i(TAG, "The default bloomfilter is used");
                return true;
            } catch (IOException e2) {
                Log.w(TAG, "The default bloomfilter can't be used.");
            }
            return false;
        }
        return true;
    }

    protected void setBloomfilter(BloomFilter bloomfilter) {
        mBloomFilter = bloomfilter;
    }

    public boolean init(Context context) {
        if (mInitialized) {
            return false;
        }
        mInitialized = true;
        mContext = context;
        scheduleDrains();
        if (!initBloomFilter(BLOOMFILTER_FULL_PATH)) {
            Log.w(TAG, "MetricsLogger can't initialize the bloomfilter");
            // The class is for multiple metrics tasks.
            // We still want to use this class even if the bloomfilter isn't initialized
            // so still return true here.
        }
        return true;
    }

    public boolean cacheCount(int key, long count) {
        if (!mInitialized) {
            Log.w(TAG, "MetricsLogger isn't initialized");
            return false;
        }
        if (count <= 0) {
            Log.w(TAG, "count is not larger than 0. count: " + count + " key: " + key);
            return false;
        }
        long total = 0;

        synchronized (mLock) {
            if (mCounters.containsKey(key)) {
                total = mCounters.get(key);
            }
            if (Long.MAX_VALUE - total < count) {
                Log.w(TAG, "count overflows. count: " + count + " current total: " + total);
                mCounters.put(key, Long.MAX_VALUE);
                return false;
            }
            mCounters.put(key, total + count);
        }
        return true;
    }

    /**
     * Log profile connection event by incrementing an internal counter for that profile.
     * This log persists over adapter enable/disable and only get cleared when metrics are
     * dumped or when Bluetooth process is killed.
     *
     * @param profileId Bluetooth profile that is connected at this event
     */
    public static void logProfileConnectionEvent(ProfileId profileId) {
        synchronized (sProfileConnectionCounts) {
            sProfileConnectionCounts.merge(profileId, 1, Integer::sum);
        }
    }

    /**
     * Dump collected metrics into proto using a builder.
     * Clean up internal data after the dump.
     *
     * @param metricsBuilder proto builder for {@link BluetoothLog}
     */
    public static void dumpProto(BluetoothLog.Builder metricsBuilder) {
        synchronized (sProfileConnectionCounts) {
            sProfileConnectionCounts.forEach(
                    (key, value) -> metricsBuilder.addProfileConnectionStats(
                            ProfileConnectionStats.newBuilder()
                                    .setProfileId(key)
                                    .setNumTimesConnected(value)
                                    .build()));
            sProfileConnectionCounts.clear();
        }
    }

    protected void scheduleDrains() {
        Log.i(TAG, "setCounterMetricsAlarm()");
        if (mAlarmManager == null) {
            mAlarmManager = mContext.getSystemService(AlarmManager.class);
        }
        mAlarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + BLUETOOTH_COUNTER_METRICS_ACTION_DURATION_MILLIS,
                TAG,
                mOnAlarmListener,
                null);
    }

    public boolean count(int key, long count) {
        if (!mInitialized) {
            Log.w(TAG, "MetricsLogger isn't initialized");
            return false;
        }
        if (count <= 0) {
            Log.w(TAG, "count is not larger than 0. count: " + count + " key: " + key);
            return false;
        }
        BluetoothStatsLog.write(
                BluetoothStatsLog.BLUETOOTH_CODE_PATH_COUNTER, key, count);
        return true;
    }

    protected void drainBufferedCounters() {
        Log.i(TAG, "drainBufferedCounters().");
        synchronized (mLock) {
            // send mCounters to statsd
            for (int key : mCounters.keySet()) {
                count(key, mCounters.get(key));
            }
            mCounters.clear();
        }
    }

    public boolean close() {
        if (!mInitialized) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "close()");
        }
        cancelPendingDrain();
        drainBufferedCounters();
        mAlarmManager = null;
        mContext = null;
        mInitialized = false;
        mBloomFilterInitialized = false;
        return true;
    }
    protected void cancelPendingDrain() {
        mAlarmManager.cancel(mOnAlarmListener);
    }

    /**
     * Retrieves a ProtoOutputStream containing serialized device information (name, address, class)
     * for the specified BluetoothDevice. This data can be used for remote device identification and
     * logging.
     *
     * @param device The BluetoothDevice for which to retrieve device information.
     * @return A ProtoOutputStream containing the serialized device information.
     */
    public ProtoOutputStream getRemoteDeviceInfoProto(BluetoothDevice device) {
        ProtoOutputStream proto = new ProtoOutputStream();
        long remoteDeviceInformationToken =
                proto.start(BluetoothRemoteDeviceInformation.DEVICE_NAME_HASH_FIELD_NUMBER);
        proto.write(
                BluetoothRemoteDeviceInformation.CLASS_OF_DEVICE_FIELD_NUMBER,
                device.getBluetoothClass().getClassOfDevice());
        proto.end(remoteDeviceInformationToken);
        Log.d(TAG, "Bluetooth Remote Device Information Proto: " + proto.getBytes());
        return proto;
    }

    /**
     * Logs a Bluetooth RFCOMM connection attempt with detailed metrics.
     *
     * @param metricId An integer identifier for the specific metric being logged.
     * @param device The BluetoothDevice involved in the connection attempt.
     * @param isSecured True if the connection attempt used a secured channel, false otherwise.
     * @param resultCode An integer result code indicating the outcome of the connection attempt.
     * @param socketCreationTimeNanos The time (in nanoseconds) taken to create the RFCOMM socket.
     * @param isSerialPort True if the RFCOMM connection is for a serial port profile, false
     *     otherwise.
     * @param appUid The UID of the application initiating the connection attempt.
     */
    public void statsLogBluetoothRfcommConnectionAttempt(
            int metricId,
            BluetoothDevice device,
            boolean isSecured,
            int resultCode,
            long socketCreationTimeNanos,
            boolean isSerialPort,
            int appUid) {
        long currentTime = System.nanoTime();
        long endToEndLatencyNanos = currentTime - socketCreationTimeNanos;
        ProtoOutputStream remoteDeviceInfoOutput = getRemoteDeviceInfoProto(device);
        BluetoothStatsLog.write(
                BluetoothStatsLog.BLUETOOTH_RFCOMM_CONNECTION_ATTEMPTED,
                metricId,
                endToEndLatencyNanos,
                isSecured
                        ? BluetoothRfcommProtoEnums.SOCKET_SECURITY_SECURE
                        : BluetoothRfcommProtoEnums.SOCKET_SECURITY_INSECURE,
                resultCode,
                isSerialPort,
                appUid,
                remoteDeviceInfoOutput.getBytes());
    }

    private void uploadRestrictedBluetothDeviceName(ArrayList<String> wordBreakdownList) {
        for (String word : wordBreakdownList) {
            BtRestrictedStatsLog.write(RESTRICTED_BLUETOOTH_DEVICE_NAME_REPORTED, word);
        }
    }

    private String getMatchedString(ArrayList<String> wordBreakdownList) {
        if (!mBloomFilterInitialized || wordBreakdownList.isEmpty()) {
            return "";
        }

        String matchedString = "";
        for (String word : wordBreakdownList) {
            byte[] sha256 = getSha256(word);
            if (mBloomFilter.mightContain(sha256) && word.length() > matchedString.length()) {
                matchedString = word;
            }
        }
        return matchedString;
    }

    protected String logDeviceNameSha(int metricId, String deviceName, boolean logRestrictedNames) {
        ArrayList<String> wordBreakdownList = getWordBreakdownList(deviceName);
        String matchedString = getMatchedString(wordBreakdownList);
        if (logRestrictedNames) {
            // Log the restricted bluetooth device name
            if (SdkLevel.isAtLeastU()) {
                uploadRestrictedBluetothDeviceName(wordBreakdownList);
        }
            if (!matchedString.isEmpty()) {
                statslogBluetoothDeviceNames(metricId, matchedString);
            }
        }
        return getSha256String(matchedString);
    }

    protected void statslogBluetoothDeviceNames(int metricId, String matchedString) {
        String sha256 = getSha256String(matchedString);
        Log.d(TAG,
                "Uploading sha256 hash of matched bluetooth device name: " + sha256);
        BluetoothStatsLog.write(
                BluetoothStatsLog.BLUETOOTH_HASHED_DEVICE_NAME_REPORTED, metricId, sha256);
    }

    protected static byte[] getSha256(String name) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "No SHA-256 in MessageDigest");
            return null;
        }
        return digest.digest(name.getBytes(StandardCharsets.UTF_8));
    }
}
