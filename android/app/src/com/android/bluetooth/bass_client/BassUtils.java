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

package com.android.bluetooth.bass_client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothLeBroadcastAssistantCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.btservice.ServiceFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bass Utility functions
 */
class BassUtils {
    private static final String TAG = "BassUtils";
    /*LE Scan related members*/
    private boolean mBroadcastersAround = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner mLeScanner = null;
    private BassClientService mBCService = null;
    private ServiceFactory mFactory = new ServiceFactory();
    // Using ArrayList as KEY to hashmap. May be not risk
    // in this case as It is used to track the callback to cancel Scanning later
    private final Map<ArrayList<IBluetoothLeBroadcastAssistantCallback>, ScanCallback>
            mLeAudioSourceScanCallbacks;
    private final Map<BluetoothDevice, ScanCallback> mBassAutoAssist;

    BassUtils(BassClientService service) {
        mBCService = service;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mLeAudioSourceScanCallbacks =
                new HashMap<ArrayList<IBluetoothLeBroadcastAssistantCallback>, ScanCallback>();
        mBassAutoAssist = new HashMap<BluetoothDevice, ScanCallback>();
    }

    private ScanCallback mPaSyncScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    log("onScanResult:" + result);
                }
            };

    void cleanUp() {
        if (mLeAudioSourceScanCallbacks != null) {
            mLeAudioSourceScanCallbacks.clear();
        }
        if (mBassAutoAssist != null) {
            mBassAutoAssist.clear();
        }
    }

    boolean leScanControl(boolean on) {
        log("leScanControl:" + on);
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mLeScanner == null) {
            Log.e(TAG, "LeScan handle not available");
            return false;
        }
        if (on) {
            mLeScanner.startScan(mPaSyncScanCallback);
        } else {
            mLeScanner.stopScan(mPaSyncScanCallback);
        }
        return true;
    }

    Handler mAutoAssistScanHandler =
            new Handler() {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case BassConstants.AA_START_SCAN:
                            BluetoothDevice dev = (BluetoothDevice) msg.obj;
                            Message m = obtainMessage(BassConstants.AA_SCAN_TIMEOUT);
                            m.obj = dev;
                            sendMessageDelayed(m, BassConstants.AA_SCAN_TIMEOUT_MS);
                            searchforBroadcastSources(dev, null);
                            break;
                        case BassConstants.AA_SCAN_SUCCESS:
                            // Able to find to desired desired Source Device
                            ScanResult scanRes = (ScanResult) msg.obj;
                            dev = scanRes.getDevice();
                            stopSearchforBroadcastSources(dev, null);
                            mBCService.selectBroadcastSource(dev, scanRes, false, true);
                            break;
                        case BassConstants.AA_SCAN_FAILURE:
                            // Not able to find the given source
                            break;
                        case BassConstants.AA_SCAN_TIMEOUT:
                            dev = (BluetoothDevice) msg.obj;
                            stopSearchforBroadcastSources(dev, null);
                            break;
                    }
                }
            };

    public boolean searchforBroadcastSources(
            BluetoothDevice srcDevice, ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs) {
        log("searchforBroadcastSources: ");
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "searchforBroadcastSources: Adapter is NULL");
            return false;
        }
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "startLeScan: cannot get BluetoothLeScanner");
            return false;
        }
        synchronized (mLeAudioSourceScanCallbacks) {
            if (mLeAudioSourceScanCallbacks.containsKey(cbs)) {
                Log.e(TAG, "LE Scan has already started");
                return false;
            }
            ScanCallback mSearchScanCallback =
                    new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            log("onScanResult:" + result);
                            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                                Log.e(TAG, "LE Scan has already started");
                                return;
                            }
                            ScanRecord scanRecord = result.getScanRecord();
                            if (scanRecord == null) {
                                Log.e(TAG, "Ignore no UUID");
                                return;
                            }
                            Map<ParcelUuid, byte[]> listOfUuids = scanRecord.getServiceData();
                            if (listOfUuids == null) {
                                Log.e(TAG, "Scan record is null, ignoring this Scan res");
                                return;
                            }
                            if (!listOfUuids.containsKey(BassConstants.BASS_UUID)) {
                                log("Broadcast Source UUID not preset, ignore");
                                return;
                            }
                            log("Broadcast Source Found:" + result.getDevice());
                            if (cbs != null) {
                                for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
                                    try {
                                        cb.onBluetoothLeBroadcastSourceFound(result);
                                    } catch (RemoteException e) {
                                        Log.e(TAG, "Exception while calling "
                                                + "onBluetoothLeBroadcastSourceFound");
                                    }
                                }
                            } else {
                                if (srcDevice.equals(result.getDevice())) {
                                    log("matching src Device found");
                                    Message msg =
                                            mAutoAssistScanHandler.obtainMessage(
                                                    BassConstants.AA_SCAN_SUCCESS);
                                    msg.obj = result;
                                    mAutoAssistScanHandler.sendMessage(msg);
                                }
                            }
                        }
                        public void onScanFailed(int errorCode) {
                            Log.e(TAG, "Scan Failure:" + errorCode);
                        }
                    };
            if (cbs != null) {
                mLeAudioSourceScanCallbacks.put(cbs, mSearchScanCallback);
            } else {
                mBassAutoAssist.put(srcDevice, mSearchScanCallback);
            }
            ScanSettings settings =
                    new ScanSettings.Builder()
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setLegacy(false)
                            .build();
            ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
            ScanFilter srcFilter =
                    filterBuilder.setServiceUuid(BassConstants.BASS_UUID).build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            scanner.startScan(filters, settings, mSearchScanCallback);
            return true;
        }
    }

    public boolean stopSearchforBroadcastSources(
            BluetoothDevice srcDev, ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs) {
        log("stopSearchforBroadcastSources");
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            return false;
        }
        ScanCallback scanCallback = null;
        if (cbs != null) {
            scanCallback = mLeAudioSourceScanCallbacks.remove(cbs);
        } else {
            scanCallback = mLeAudioSourceScanCallbacks.remove(srcDev);
        }
        if (scanCallback == null) {
            log("scan not started yet");
            return false;
        }
        scanner.stopScan(scanCallback);
        return true;
    }

    static void log(String msg) {
        if (BassConstants.BASS_DBG) {
            Log.d(TAG, msg);
        }
    }

    static void printByteArray(byte[] array) {
        log("Entire byte Array as string: " + Arrays.toString(array));
        log("printitng byte by bte");
        for (int i = 0; i < array.length; i++) {
            log("array[" + i + "] :" + Byte.toUnsignedInt(array[i]));
        }
    }
}
