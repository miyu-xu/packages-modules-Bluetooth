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

package pl.codecoup.ehima.leaudio;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothBroadcastAudioScan;
import android.bluetooth.BluetoothBroadcastAudioScanBaseConfig;
import android.bluetooth.BluetoothBroadcastAudioScanResult;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BroadcastScanViewModel extends AndroidViewModel {
    boolean mIsActivityScanning = false;
    BluetoothDevice mOnBehalfDevice;

    // AddBroadcast context
    BluetoothDevice mSetSrcTargetDevice;
    List<BluetoothBroadcastAudioScanBaseConfig> mSetSrcConfigs;
    boolean mSetSrcSyncPa;

    BluetoothRepository mBluetooth;
    Application mApplication;
    private MutableLiveData<List<AudioBroadcast>> mAllBroadcasts = new MutableLiveData<>();

    private HashMap<String, AudioBroadcast> mScanSessionBroadcasts = new HashMap<>();
    private final BroadcastReceiver mBassIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothBroadcastAudioScan.ACTION_BASS_BROADCAST_ANNONCEMENT_AVAILABLE:
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothBroadcastAudioScanResult result = intent
                            .getParcelableExtra(BluetoothBroadcastAudioScan.EXTRA_BASS_SCAN_RESULT);

                    AudioBroadcast broadcast = new AudioBroadcast(device, result);
                    mScanSessionBroadcasts.put(Arrays.toString(broadcast.getBroadcastId()),
                            broadcast);

                    break;
                case BluetoothBroadcastAudioScan.ACTION_BASS_BROADCAST_SCANNING_STATE:
                    boolean isScanning = intent.getBooleanExtra(
                            BluetoothBroadcastAudioScan.EXTRA_BASS_SCAN_STATE, false);
                    if (!isScanning) {
                        // Update the live broadcast list and clear scan session results
                        List<AudioBroadcast> localSessionBroadcasts =
                                mBluetooth.getAllBroadcasts().getValue();
                        ArrayList<AudioBroadcast> new_arr;
                        if (localSessionBroadcasts != null) {
                            new_arr = new ArrayList<>(localSessionBroadcasts);
                        } else {
                            new_arr = new ArrayList<>();
                        }
                        new_arr.addAll(mScanSessionBroadcasts.values());
                        mAllBroadcasts.postValue(new_arr);

                        // Continue as long as the main activity wants
                        if (mIsActivityScanning) {
                            if (mOnBehalfDevice != null) {
                                mBluetooth.scanForBroadcasts(mOnBehalfDevice, true);
                            }
                        }
                    } else {
                        // FIXME: Clear wont work - it would auto-update the mutable and clear it as
                        // mutable uses reference to it's values
                        mScanSessionBroadcasts = new HashMap<>();
                    }
                    break;
            }
        }
    };

    private final BroadcastReceiver mBroadcasterIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Integer instance_id =
                    intent.getIntExtra(BluetoothLeAudio.EXTRA_LE_AUDIO_BROADCAST_INSTANCE_ID, 0);
            AudioBroadcast ab = null;

            // FIXME: We need a finer grain control over updating individual broadcast state and not
            // just
            // the entire list of broadcasts
            switch (action) {
                case BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_CREATED: {
                    boolean success = intent.getBooleanExtra(
                            BluetoothLeAudio.EXTRA_LE_AUDIO_BROADCAST_INSTANCE_STATUS, false);
                    if (success) {
                        // Concatenate local broadcasts to the scanned broadcast list
                        List<AudioBroadcast> localSessionBroadcasts =
                                mBluetooth.getAllBroadcasts().getValue();
                        ArrayList<AudioBroadcast> new_arr;
                        if (localSessionBroadcasts != null) {
                            new_arr = new ArrayList<>(localSessionBroadcasts);
                        } else {
                            new_arr = new ArrayList<>();
                        }
                        new_arr.addAll(mScanSessionBroadcasts.values());
                        mAllBroadcasts.postValue(new_arr);
                    }
                }
                    break;

                case BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_DESTROYED: {
                    // Concatenate local broadcasts to the scanned broadcast list
                    List<AudioBroadcast> localSessionBroadcasts =
                            mBluetooth.getAllBroadcasts().getValue();
                    ArrayList<AudioBroadcast> new_arr = new ArrayList<>(localSessionBroadcasts);
                    new_arr.addAll(mScanSessionBroadcasts.values());
                    mAllBroadcasts.postValue(new_arr);
                }
                    break;

                case BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_STATE:
                    int state =
                            intent.getIntExtra(BluetoothLeAudio.EXTRA_LE_AUDIO_BROADCAST_STATE, -1);
                    if (state != -1) {
                        // Concatenate local broadcasts to the scanned broadcast list
                        List<AudioBroadcast> localSessionBroadcasts =
                                mBluetooth.getAllBroadcasts().getValue();
                        ArrayList<AudioBroadcast> new_arr;
                        if (localSessionBroadcasts != null) {
                            new_arr = new ArrayList<>(localSessionBroadcasts);
                        } else {
                            new_arr = new ArrayList<>();
                        }
                        new_arr.addAll(mScanSessionBroadcasts.values());
                        mAllBroadcasts.postValue(new_arr);
                    }
                    break;

                case BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_ID:
                    byte[] broadcast_id =
                            intent.getByteArrayExtra(BluetoothLeAudio.EXTRA_LE_AUDIO_BROADCAST_ID);

                    List<AudioBroadcast> localSessionBroadcasts =
                            mBluetooth.getAllBroadcasts().getValue();
                    ab = localSessionBroadcasts.get(instance_id);
                    if (ab != null) {
                        // Concatenate local broadcasts to the scanned broadcast list
                        ArrayList<AudioBroadcast> new_arr = new ArrayList<>(localSessionBroadcasts);
                        new_arr.addAll(mScanSessionBroadcasts.values());
                        mAllBroadcasts.postValue(new_arr);
                    }

                    if (mSetSrcTargetDevice != null) {
                        mBluetooth.addBroadcastSource(mSetSrcTargetDevice, broadcast_id,
                                mSetSrcSyncPa, mSetSrcConfigs);
                        mSetSrcTargetDevice = null;
                        mSetSrcConfigs = null;
                        mSetSrcSyncPa = false;
                    }

                    break;
            }
        }
    };

    private IntentFilter mIntentFilter;
    private IntentFilter mBroadcasterIntentFilter;

    public BroadcastScanViewModel(@NonNull Application application) {
        super(application);
        mApplication = application;
        mBluetooth = BluetoothRepository.getBluetoothRepository(application);

        mIntentFilter = new IntentFilter();
        mIntentFilter
                .addAction(BluetoothBroadcastAudioScan.ACTION_BASS_BROADCAST_ANNONCEMENT_AVAILABLE);
        mIntentFilter.addAction(BluetoothBroadcastAudioScan.ACTION_BASS_BROADCAST_SCANNING_STATE);
        mApplication.registerReceiver(mBassIntentReceiver, mIntentFilter);

        mBroadcasterIntentFilter = new IntentFilter();
        mBroadcasterIntentFilter.addAction((BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_CREATED));
        mBroadcasterIntentFilter.addAction((BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_DESTROYED));
        mBroadcasterIntentFilter.addAction((BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_STATE));
        mBroadcasterIntentFilter.addAction((BluetoothLeAudio.ACTION_LE_AUDIO_BROADCAST_ID));
        mApplication.registerReceiver(mBroadcasterIntentReceiver, mBroadcasterIntentFilter);
    }

    @Override
    public void onCleared() {
        mApplication.unregisterReceiver(mBassIntentReceiver);
        mApplication.unregisterReceiver(mBroadcasterIntentReceiver);
    }

    public LiveData<List<AudioBroadcast>> getAllBroadcasts() {
        return mAllBroadcasts;
    }

    public void scanForBroadcasts(BluetoothDevice device, boolean scan) {
        mIsActivityScanning = scan;
        mOnBehalfDevice = scan ? device : null;

        // First update the live broadcast list
        List<AudioBroadcast> localSessionBroadcasts = mBluetooth.getAllBroadcasts().getValue();
        ArrayList<AudioBroadcast> new_arr;
        if (localSessionBroadcasts != null) {
            new_arr = new ArrayList<>(localSessionBroadcasts);
        } else {
            new_arr = new ArrayList<>();
        }
        new_arr.addAll(mScanSessionBroadcasts.values());
        mAllBroadcasts.postValue(new_arr);

        mBluetooth.scanForBroadcasts(device, scan);
    }

    public void addBroadcastSource(BluetoothDevice device, byte[] broadcast_id, boolean sync_pa,
            List<BluetoothBroadcastAudioScanBaseConfig> configs) {
        mBluetooth.addBroadcastSource(device, broadcast_id, sync_pa, configs);
    }

    public void setLocalBroadcastSource(BluetoothDevice device, int local_instance_id,
            boolean sync_pa, List<BluetoothBroadcastAudioScanBaseConfig> configs) {
        // Store sync_pa, and configs with metadata for later, once we know the current advertizing
        // address
        mSetSrcTargetDevice = device;
        mSetSrcConfigs = configs;
        mSetSrcSyncPa = sync_pa;

        // We get the current advertizer address and then set the src.
        mBluetooth.requestBroadcastId(local_instance_id);
    }

    public void getAllLocalBroadcasts() {
        mBluetooth.getAllLocalBroadcastStates();
    }
}
