/*
 * Copyright 2025 The Android Open Source Project
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

import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import android.annotation.RequiresPermission;
import android.bluetooth.IBluetoothAdvertise;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.AttributionSource;
import android.content.Context;

import com.android.bluetooth.Utils;

class AdvertiseBinder extends IBluetoothAdvertise.Stub {
    private final Context mContext;
    private AdvertiseManager mAdvertiseManager;

    AdvertiseBinder(Context context, AdvertiseManager manager) {
        mContext = context;
        mAdvertiseManager = manager;
    }

    void cleanup() {
        mAdvertiseManager = null;
    }

    @RequiresPermission(
            allOf = {
                BLUETOOTH_ADVERTISE,
                BLUETOOTH_PRIVILEGED,
            },
            conditional = true)
    @Override
    public void startAdvertisingSet(
            AdvertisingSetParameters parameters,
            AdvertiseData advertiseData,
            AdvertiseData scanResponse,
            PeriodicAdvertisingParameters periodicParameters,
            AdvertiseData periodicData,
            int duration,
            int maxExtAdvEvents,
            int serverIf,
            IAdvertisingSetCallback callback,
            AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager startAdvertisingSet")) {
            return;
        }
        if (parameters.getOwnAddressType() != AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT
                || serverIf != 0
                || parameters.isDirected()) {
            mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        }
        manager.startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                duration,
                maxExtAdvEvents,
                serverIf,
                callback,
                attributionSource);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void stopAdvertisingSet(
            IAdvertisingSetCallback callback, AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager stopAdvertisingSet")) {
            return;
        }
        manager.stopAdvertisingSet(callback);
    }

    @RequiresPermission(
            allOf = {
                BLUETOOTH_ADVERTISE,
                BLUETOOTH_PRIVILEGED,
            })
    @Override
    public void getOwnAddress(int advertiserId, AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager getOwnAddress")) {
            return;
        }
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        manager.getOwnAddress(advertiserId);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void enableAdvertisingSet(
            int advertiserId,
            boolean enable,
            int duration,
            int maxExtAdvEvents,
            AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager enableAdvertisingSet")) {
            return;
        }
        manager.enableAdvertisingSet(advertiserId, enable, duration, maxExtAdvEvents);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void setAdvertisingData(
            int advertiserId, AdvertiseData data, AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager setAdvertisingData")) {
            return;
        }
        manager.setAdvertisingData(advertiserId, data);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void setScanResponseData(
            int advertiserId, AdvertiseData data, AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager setScanResponseData")) {
            return;
        }
        manager.setScanResponseData(advertiserId, data);
    }

    @RequiresPermission(
            allOf = {
                BLUETOOTH_ADVERTISE,
                BLUETOOTH_PRIVILEGED,
            },
            conditional = true)
    @Override
    public void setAdvertisingParameters(
            int advertiserId,
            AdvertisingSetParameters parameters,
            AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager setAdvertisingParameters")) {
            return;
        }
        if (parameters.getOwnAddressType() != AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT
                || parameters.isDirected()) {
            mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        }
        manager.setAdvertisingParameters(advertiserId, parameters);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void setPeriodicAdvertisingParameters(
            int advertiserId,
            PeriodicAdvertisingParameters parameters,
            AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager setPeriodicAdvertisingParameters")) {
            return;
        }
        manager.setPeriodicAdvertisingParameters(advertiserId, parameters);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void setPeriodicAdvertisingData(
            int advertiserId, AdvertiseData data, AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager setPeriodicAdvertisingData")) {
            return;
        }
        manager.setPeriodicAdvertisingData(advertiserId, data);
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Override
    public void setPeriodicAdvertisingEnable(
            int advertiserId, boolean enable, AttributionSource attributionSource) {
        AdvertiseManager manager = mAdvertiseManager;
        if (manager == null) {
            return;
        }
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, attributionSource, "AdvertiseManager setPeriodicAdvertisingEnable")) {
            return;
        }
        manager.setPeriodicAdvertisingEnable(advertiserId, enable);
    }
}
