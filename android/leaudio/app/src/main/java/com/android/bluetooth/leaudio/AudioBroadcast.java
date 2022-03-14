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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothBroadcastAudioScanBaseConfig;
import android.bluetooth.BluetoothBroadcastAudioScanResult;

import java.util.Map;

public class AudioBroadcast {
    BluetoothDevice device;

    // Local Broadcast data
    byte[] broadcastId;
    int local_instance_id = -1;
    int adv_addr_type;
    byte[] code;
    BluetoothBroadcastAudioScanBaseConfig config;

    // Remote Broadcast data:
    int state = -1;
    BluetoothBroadcastAudioScanResult scan_result;

    public AudioBroadcast(BluetoothDevice device, BluetoothBroadcastAudioScanResult scan_result) {
        this.device = device;
        this.scan_result = scan_result;
        local_instance_id = -1;
    }

    public AudioBroadcast(int local_instance_id) {
        this.device = null;
        this.local_instance_id = local_instance_id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setCode(byte[] code) {
        this.code = code;
    }

    public void setLocalConfig(BluetoothBroadcastAudioScanBaseConfig config) {
        this.config = config;
    }

    public byte[] getBroadcastId() {
        return scan_result != null ? scan_result.getBroadcastId() : this.broadcastId;
    }

    public void updateLocalBroadcastData(int local_instance_id, byte[] broadcast_id) {
        this.broadcastId = broadcast_id;
        this.local_instance_id = local_instance_id;

        // This excludes remote broadcast
        scan_result = null;
    }

    public boolean isLocal() { return local_instance_id != -1; }
}
