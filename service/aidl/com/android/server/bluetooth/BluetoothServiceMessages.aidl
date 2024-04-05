/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.server.bluetooth;

import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothManagerCallback;
import android.content.AttributionSource;


/** {@hide} */
interface BluetoothServiceMessages {

    parcelable RegisterAdapter {
        IBluetoothManagerCallback binder;
    }

    parcelable UnregisterAdapter {
        IBluetoothManagerCallback binder;
    }

    parcelable Enable {
        AttributionSource attributionSource;
        IBinder bleToken;
        boolean isQuiet;
    }

    parcelable Disable {
        AttributionSource attributionSource;
        IBinder bleToken;
        boolean persist;
    }

    parcelable FactoryReset {
        AttributionSource attributionSource;
    }

    parcelable IsBleScanAvailable {}

    parcelable IsHearingAidSupported {}

    parcelable SetSnoopLog {
        int mode;
    }

    parcelable GetSnoopLog {}

    parcelable IsAutoSupported {}

    parcelable IsAutoEnabled {}

    parcelable SetAutoOnEnabled {
        boolean enabledStatus;
    }

    parcelable BluetoothBinder {
        IBluetooth binder;
    }

    parcelable BooleanValue {
        boolean value;
    }
}

