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

package android.bluetooth;

import static com.google.common.io.BaseEncoding.base16;
import static com.google.common.truth.Truth.assertThat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.protobuf.ByteString;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static final String BUMBLE_RANDOM_ADDRESS = "51:F7:A8:75:AC:5E";

    public static String addressStringFromByteString(ByteString bs) {
        StringBuilder refAddrBuilder = new StringBuilder();
        for (int i = 0; i < bs.size(); i++) {
            if (i != 0) {
                refAddrBuilder.append(':');
            }
            refAddrBuilder.append(String.format("%02X", bs.byteAt(i)));
        }
        return refAddrBuilder.toString();
    }

    /**
     * @param address String representing Bluetooth address (case insensitive).
     * @return Decoded address.
     */
    public static byte[] addressBytesFromString(String address) {
        return base16().upperCase().withSeparator(":", 2).decode(address.toUpperCase(Locale.US));
    }

    /**
     * Wait and verify that an item has been received.
     *
     * @param timeoutMs the time (in milliseconds) to wait for the item
     * @param queue the queue for the item
     * @return the received intent
     */
    public static <T> T waitForItem(int timeoutMs, BlockingQueue<T> queue) {
        try {
            return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Cannot obtain an item from the queue: " + e.getMessage());
        }
        return null;
    }

    /** Device based broadcast receiver */
    public static class DeviceBasedBroadcastReceiver extends BroadcastReceiver {
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
}
