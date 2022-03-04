/*
 * Copyright 2022 HIMSA II K/S - www.himsa.dk.
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

package com.android.bluetooth.le_audio;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@VisibleForTesting
public class TmapGatt {
    private static final boolean DBG = true;
    private static final String TAG = "TmapGatt";

    /* Telephony and Media Audio Profile Role Characteristic UUID */
    private static final UUID UUID_TMAP_ROLE =
            UUID.fromString("00002B51-0000-1000-8000-00805f9b34fb");
    /* Common Audio Service UUID */

    /* TMAP Role: Call Gateway */
    public static final int TMAP_ROLE_FLAG_CG = 1 << 0;
    /* TMAP Role: Call Terminal */
    public static final int TMAP_ROLE_FLAG_CT = 1 << 1;
    /* TMAP Role: Unicast Media Sender */
    public static final int TMAP_ROLE_FLAG_UMS = 1 << 2;
    /* TMAP Role: Unicast Media Receiver */
    public static final int TMAP_ROLE_FLAG_UMR = 1 << 3;
    /* TMAP Role: Broadcast Media Sender */
    public static final int TMAP_ROLE_FLAG_BMS = 1 << 4;
    /* TMAP Role: Broadcast Media Receiver */
    public static final int TMAP_ROLE_FLAG_BMR = 1 << 5;

    private final Context mContext;
    private BluetoothGattServerProxy mBluetoothGattServer;

    /*package*/ TmapGatt(Context context) {
        mContext = context;
        mBluetoothGattServer = null;
    }

    @VisibleForTesting
    void setBluetoothGattServerForTesting(BluetoothGattServerProxy proxy) {
        mBluetoothGattServer = proxy;
    }

    /*
     * Init Tmap service
     * @param tmapRoleMask bit mask of supported roles.
     */
    @VisibleForTesting
    public void init(int tmapRoleMask) {
        if (DBG) {
            Log.d(TAG, "init(tmap:" + tmapRoleMask + ")");
        }

        if (mBluetoothGattServer == null) {
            mBluetoothGattServer = new BluetoothGattServerProxy(mContext);
        }

        if (!mBluetoothGattServer.open(mBluetoothGattServerCallback)) {
            Log.e(TAG, " Could not open Gatt server");
            return;
        }

        BluetoothGattService service =
                new BluetoothGattService(BluetoothUuid.TELEPHONY_AND_MEDIA_AUDIO.getUuid(),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                UUID_TMAP_ROLE,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);

        characteristic.setValue(tmapRoleMask, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        service.addCharacteristic(characteristic);
        mBluetoothGattServer.addService(service);
    }

    @VisibleForTesting
    public void stop() {
    }

    @VisibleForTesting
    public void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }

        mBluetoothGattServer.close();
        mBluetoothGattServer = null;
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private final BluetoothGattServerCallback mBluetoothGattServerCallback =
            new BluetoothGattServerCallback() {

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            if (DBG) {
                Log.d(TAG, "value " + value);
            }
            if (value != null) {
                Log.e(TAG, "value null");
                value = Arrays.copyOfRange(value, offset, value.length);
            }
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, value);
        }
    };

     /**
     * A proxy class that facilitates testing.
     *
     * This is necessary due to the "final" attribute of the BluetoothGattServer class.
     * @hide
     */
    public class BluetoothGattServerProxy {
        private final Context mContext;
        private BluetoothGattServer mBluetoothGattServer;
        private BluetoothManager mBluetoothManager;

        public BluetoothGattServerProxy(Context context) {
            mContext = context;
            mBluetoothManager = (BluetoothManager) context.getSystemService(
                    Context.BLUETOOTH_SERVICE);
            mBluetoothGattServer = null;
        }

        public boolean open(BluetoothGattServerCallback callback) {
            mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, callback);
            return (mBluetoothGattServer != null);
        }

        public void close() {
            if (mBluetoothGattServer == null) {
                return;
            }
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }

        public boolean addService(BluetoothGattService service) {
            return mBluetoothGattServer.addService(service);
        }

        public boolean sendResponse(
                BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
            return mBluetoothGattServer.sendResponse(device, requestId, status, offset, value);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
        }
    }
}
