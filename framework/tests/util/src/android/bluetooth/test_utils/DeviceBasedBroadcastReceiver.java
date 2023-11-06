/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.bluetooth.test_utils;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class DeviceBasedBroadcastReceiver extends BroadcastReceiver {
    private final HashMap<BluetoothDevice, LinkedBlockingQueue<Intent>> mDeviceQueueMap =
            new HashMap<>();
    private final LinkedBlockingQueue<Intent> mDefaultQueue = new LinkedBlockingQueue<>();

    /**
     * Add a device into the tracker
     *
     * @param device to be added
     */
    public void addDevice(BluetoothDevice device) {
        mDeviceQueueMap.put(device, new LinkedBlockingQueue<>());
    }

    /**
     * Get the blocking queue for the device
     *
     * @param device device must be added before
     * @return null if device wasn't added earlier, the blocking queue if device was added
     */
    public LinkedBlockingQueue<Intent> getQueue(BluetoothDevice device) {
        return mDeviceQueueMap.get(device);
    }

    /**
     * Get the default queue when no EXTRA_DEVICE is included
     *
     * @return the default queue
     */
    public LinkedBlockingQueue<Intent> getDefaultQueue() {
        return mDefaultQueue;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        if (device == null) {
            assertThat(mDefaultQueue.add(intent)).isTrue();
        } else if (mDeviceQueueMap.containsKey(device)) {
            LinkedBlockingQueue<Intent> queue = mDeviceQueueMap.get(device);
            assertThat(queue.add(intent)).isTrue();
        }
    }
}
