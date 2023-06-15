/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

package com.android.bluetooth.leaudio;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.os.Handler;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BroadcastScanViewModel extends AndroidViewModel {
    private final String TAG = "BroadcastScanViewModel";
    boolean mIsActivityScanning = false;
    BluetoothDevice mScanDelegatorDevice;

    // TODO: Remove these variables if they are unnecessary
//    // AddBroadcast context
//    BluetoothDevice mSetSrcTargetDevice;
//    List<BluetoothBroadcastAudioScanBaseConfig> mSetSrcConfigs;
//    boolean mSetSrcSyncPa;

    BluetoothProxy mBluetooth;
    Application mApplication;
    private MutableLiveData<List<BluetoothLeBroadcastMetadata>> mAllBroadcasts = new MutableLiveData<>();
    private HashMap<Integer, BluetoothLeBroadcastMetadata> mScanSessionBroadcasts = new HashMap<>();

    // Handler to prevent handler update too frequently.
    // In a crowded place where a lot of broadcasts are present, as all the broadcast list
    // is updated when a broadcast is updated, the list refreshes continuously preventing
    // clicks on items on the list. This handler makes the update happen every x milliseconds.
    private final Handler mAllBroadcastsHandler = new Handler();
    private final int BROADCASTS_UPDATE_DELAY_MS = 1000;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private ArrayList<BluetoothLeBroadcastMetadata> mTempBroadcastList = new ArrayList<>();

    private final BluetoothProxy.OnBassEventListener mBassEventListener =
            new BluetoothProxy.OnBassEventListener() {
                @Override
                public void onSourceFound(BluetoothLeBroadcastMetadata source) {
                    mScanSessionBroadcasts.put(source.getBroadcastId(), source);
                    refreshBroadcasts();
                }

                @Override
                public void onScanningStateChanged(boolean isScanning) {
                    if (!isScanning) {
                        // Update the live broadcast list and clear scan session results
                        refreshBroadcasts();

                        // Continue as long as the main activity wants
                        if (mIsActivityScanning) {
                            if (mScanDelegatorDevice != null) {
                                mBluetooth.scanForBroadcasts(mScanDelegatorDevice, true);
                            }
                        }
                    } else {
                        // FIXME: Clear won't work - it would auto-update the mutable and clear it
                        // as
                        // mutable uses reference to its values
                        mScanSessionBroadcasts = new HashMap<>();
                    }
                }
            };

    private final BluetoothProxy.OnLocalBroadcastEventListener mLocalBroadcastEventListener =
            new BluetoothProxy.OnLocalBroadcastEventListener() {
        @Override
        public void onBroadcastStarted(int broadcastId) {
            // FIXME: We need a finer grain control over updating individual broadcast state
            //        and not just the entire list of broadcasts
            refreshBroadcasts();
        }

        @Override
        public void onBroadcastStopped(int broadcastId) {
            refreshBroadcasts();
        }

        @Override
        public void onBroadcastUpdated(int broadcastId) {
            refreshBroadcasts();
        }

        @Override
        public void onBroadcastMetadataChanged(int broadcastId,
                BluetoothLeBroadcastMetadata metadata) {
            refreshBroadcasts();
        }
    };

    public BroadcastScanViewModel(@NonNull Application application) {
        super(application);
        mApplication = application;
        mBluetooth = BluetoothProxy.getBluetoothProxy(application);

        mBluetooth.setOnBassEventListener(mBassEventListener);
        mBluetooth.setOnLocalBroadcastEventListener(mLocalBroadcastEventListener);

        mAllBroadcastsHandler.postDelayed(
                new Runnable() {
                    public void run() {
                        updateAllBroadcastsList();
                        mAllBroadcastsHandler.postDelayed(this, BROADCASTS_UPDATE_DELAY_MS);
                    }
                },
                BROADCASTS_UPDATE_DELAY_MS);
    }

    @Override
    public void onCleared() {
        mAllBroadcastsHandler.removeCallbacksAndMessages(null);
        mBluetooth.setOnBassEventListener(null);
        mBluetooth.setOnLocalBroadcastEventListener(null);
    }

    public LiveData<List<BluetoothLeBroadcastMetadata>> getAllBroadcasts() {
        return mAllBroadcasts;
    }

    public void scanForBroadcasts(BluetoothDevice delegatorDevice, boolean scan) {
        mIsActivityScanning = scan;
        mScanDelegatorDevice = delegatorDevice;

        // First update the live broadcast list
        refreshBroadcasts();

        mBluetooth.scanForBroadcasts(mScanDelegatorDevice, scan);
    }

    public void addBroadcastSource(BluetoothDevice sink, BluetoothLeBroadcastMetadata sourceMetadata) {
        mBluetooth.addBroadcastSource(sink, sourceMetadata);
    }

    public void refreshBroadcasts() {
        synchronized (mLock) {
            // Concatenate local broadcasts to the scanned broadcast list
            List<BluetoothLeBroadcastMetadata> localSessionBroadcasts =
                    mBluetooth.getAllLocalBroadcasts();
            mTempBroadcastList.clear();
            mTempBroadcastList.addAll(localSessionBroadcasts);
            mTempBroadcastList.addAll(mScanSessionBroadcasts.values());
        }
    }

    public void updateAllBroadcastsList() {
        mAllBroadcasts.postValue(mTempBroadcastList);
    }
}
