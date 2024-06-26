/*
 * Copyright 2012 The Android Open Source Project
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

import android.bluetooth.IBluetoothManagerCallback;
import android.content.AttributionSource;

/**
 * System private API for talking with the Bluetooth service.
 *
 * {@hide}
 */
interface IBluetoothManager
{
    IBinder registerAdapter(in IBluetoothManagerCallback callback);
    void unregisterAdapter(in IBluetoothManagerCallback callback);
    boolean enable(in AttributionSource attributionSource);
    boolean enableNoAutoConnect(in AttributionSource attributionSource);
    boolean disable(in AttributionSource attributionSource, boolean persist);
    int getState();

    String getAddress(in AttributionSource attributionSource);
    String getName(in AttributionSource attributionSource);

    boolean onFactoryReset(in AttributionSource attributionSource);

    boolean isBleScanAvailable();
    boolean enableBle(in AttributionSource attributionSource, IBinder b);
    boolean disableBle(in AttributionSource attributionSource, IBinder b);
    boolean isHearingAidProfileSupported();

    int setBtHciSnoopLogMode(int mode);
    int getBtHciSnoopLogMode();

    // AutoOnFeature
    boolean isAutoOnSupported();
    boolean isAutoOnEnabled();
    void setAutoOnEnabled(boolean status);
}
