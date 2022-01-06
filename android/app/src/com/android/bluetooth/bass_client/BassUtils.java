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

package com.android.bluetooth.bc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastSourceInfo;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bass Utility functions */
final class BassUtils {

    private static final String TAG = "BassUtils";
    /*LE Scan related members*/
    private boolean mBroadcastersAround = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner mLeScanner = null;
    private BCService mBCService = null;
    public static final String BAAS_UUID = "00001852-0000-1000-8000-00805F9B34FB";
    private ServiceFactory mFactory = new ServiceFactory();
    // Using ArrayList as KEY to hashmap. May be not risk
    // in this case as It is used to track the callback to cancel Scanning later
    private final Map<ArrayList<IBluetoothLeBroadcastAssistantCallback>, ScanCallback>
            mLeAudioSourceScanCallbacks;
    private final Map<BluetoothDevice, ScanCallback> mBassAutoAssist;
    private static final int AA_START_SCAN = 1;
    private static final int AA_SCAN_SUCCESS = 2;
    private static final int AA_SCAN_FAILURE = 3;
    private static final int AA_SCAN_TIMEOUT = 4;
    // timeout for internal scan
    private static final int AA_SCAN_TIMEOUT_MS = 1000;

    /** Stanadard Codec param types */
    static final int LOCATION = 3;
    // sample rate
    static final int SAMPLE_RATE = 1;
    // frame duration
    static final int FRAME_DURATION = 2;
    // Octets per frame
    static final int OCTETS_PER_FRAME = 8;

    BassUtils(BCService service) {
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
                        case AA_START_SCAN:
                            BluetoothDevice dev = (BluetoothDevice) msg.obj;
                            Message m = obtainMessage(AA_SCAN_TIMEOUT);
                            m.obj = dev;
                            sendMessageDelayed(m, AA_SCAN_TIMEOUT_MS);
                            searchforBroadcastSources(dev, null);
                            break;
                        case AA_SCAN_SUCCESS:
                            // Able to find to desired desired Source Device
                            ScanResult scanRes = (ScanResult) msg.obj;
                            dev = scanRes.getDevice();
                            stopSearchforBroadcastSources(dev, null);
                            mBCService.selectBroadcastSource(dev, scanRes, false, true);
                            break;
                        case AA_SCAN_FAILURE:
                            // Not able to find the given source
                            // ignore
                            break;
                        case AA_SCAN_TIMEOUT:
                            dev = (BluetoothDevice) msg.obj;
                            stopSearchforBroadcastSources(dev, null);
                            break;
                    }
                }
            };

    private void notifyLocalBroadcastSourceFound(
            ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs) {
        BluetoothDevice localDev =
                BluetoothAdapter.getDefaultAdapter()
                        .getRemoteDevice(mBluetoothAdapter.getAddress());
        String localName = BluetoothAdapter.getDefaultAdapter().getName();
        ScanRecord record = null;
        if (localName != null) {
            byte name_len = (byte) localName.length();
            byte[] bd_name = localName.getBytes(StandardCharsets.US_ASCII);
            byte[] name_key = new byte[] {++name_len, 0x09}; // 0x09 TYPE:Name
            byte[] scan_r = new byte[name_key.length + bd_name.length];
            System.arraycopy(name_key, 0, scan_r, 0, name_key.length);
            System.arraycopy(bd_name, 0, scan_r, name_key.length, bd_name.length);
            record = ScanRecord.parseFromBytes(scan_r);
            log("Local name populated in fake Scan res:" + record.getDeviceName());
        }
        ScanResult scanRes = new ScanResult(localDev, 1, 1, 1, 2, 0, 0, 0, record, 0);
        if (cbs != null) {
            for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
                try {
                    cb.onBluetoothLeBroadcastSourceFound(scanRes);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception while calling onBluetoothLeBroadcastSourceFound");
                }
            }
        }
    }

    public boolean searchforBroadcastSources(
            BluetoothDevice srcDevice, ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs) {
        log("searchforBroadcastSources: ");
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
            ScanCallback scanCallback =
                    new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            log("onScanResult:" + result);
                            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                                // Should not happen.
                                Log.e(TAG, "LE Scan has already started");
                                return;
                            }
                            ScanRecord scanRecord = result.getScanRecord();
                            if (scanRecord != null) {
                                Map<ParcelUuid, byte[]> listOfUuids = scanRecord.getServiceData();
                                if (listOfUuids != null) {
                                    boolean isBroadcastSource =
                                            listOfUuids.containsKey(
                                                    ParcelUuid.fromString(BAAS_UUID));
                                    log("isBroadcastSource:" + isBroadcastSource);
                                    if (isBroadcastSource) {
                                        log("Broadcast Source Found:" + result.getDevice());
                                        if (cbs != null) {
                                            for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
                                                try {
                                                    cb.onBluetoothLeBroadcastSourceFound(result);
                                                } catch (RemoteException e) {
                                                    Log.e(TAG,
                                                        "Exception while calling "
                                                            + "onBluetoothLeBroadcastSourceFound");
                                                }
                                            }
                                        } else {
                                            if (srcDevice.equals(result.getDevice())) {
                                                log("matching src Device found");
                                                Message msg =
                                                        mAutoAssistScanHandler.obtainMessage(
                                                                AA_SCAN_SUCCESS);
                                                msg.obj = result;
                                                mAutoAssistScanHandler.sendMessage(msg);
                                            }
                                        }
                                    } else {
                                        log("Broadcast Source UUID not preset, ignore");
                                    }
                                } else {
                                    Log.e(TAG, "Ignore no UUID");
                                    return;
                                }
                            } else {
                                Log.e(TAG, "Scan record is null, ignoring this Scan res");
                                return;
                            }
                        }

                        public void onScanFailed(int errorCode) {
                            Log.e(TAG, "Scan Failure:" + errorCode);
                        }
                    };
            if (mBluetoothAdapter != null) {
                if (cbs != null) {
                    mLeAudioSourceScanCallbacks.put(cbs, scanCallback);
                } else {
                    // internal auto assist trigger remember it
                    // based on device
                    mBassAutoAssist.put(srcDevice, scanCallback);
                }

                ScanSettings settings =
                        new ScanSettings.Builder()
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setLegacy(false)
                                .build();
                ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
                ScanFilter srcFilter =
                        filterBuilder.setServiceUuid(ParcelUuid.fromString(BAAS_UUID)).build();
                List<ScanFilter> filters = new ArrayList<ScanFilter>();
                scanner.startScan(filters, settings, scanCallback);
                return true;
            } else {
                Log.e(TAG, "searchforBroadcastSources: Adapter is NULL");
                return false;
            }
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

    private int convertConfigurationSRToCapabilitySR(byte sampleRate) {
        int ret = BluetoothCodecConfig.SAMPLE_RATE_NONE;
        switch (sampleRate) {
            case 1:
                ret = BluetoothCodecConfig.SAMPLE_RATE_NONE;
                break;
            case 2:
                ret = BluetoothCodecConfig.SAMPLE_RATE_NONE;
                break;
            case 3:
                ret = BluetoothCodecConfig.SAMPLE_RATE_NONE;
                break;
            case 4:
                // ret = BluetoothCodecConfig.SAMPLE_RATE_32000; break;
            case 5:
                ret = BluetoothCodecConfig.SAMPLE_RATE_44100;
                break;
            case 6:
                ret = BluetoothCodecConfig.SAMPLE_RATE_48000;
                break;
        }
        log("convertConfigurationSRToCapabilitySR returns:" + ret);
        return ret;
    }

    public void triggerAutoAssist(BluetoothLeBroadcastSourceInfo srcInfo) {
        BluetoothDevice dev = srcInfo.getSourceDevice();

        Message msg = mAutoAssistScanHandler.obtainMessage(AA_START_SCAN);
        msg.obj = srcInfo.getSourceDevice();
        mAutoAssistScanHandler.sendMessage(msg);
    }

    static void log(String msg) {
        if (BassClientStateMachine.BASS_DBG) {
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
