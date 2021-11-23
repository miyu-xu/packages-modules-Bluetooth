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

package com.android.bluetooth.tbs;

import android.bluetooth.BluetoothTbs;
import android.bluetooth.BluetoothTbsCall;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/*
 * A proxy class that facilitates testing of the BluetoothInCallService class.
 *
 * This is necessary due to the "final" attribute of the BluetoothTbs class. In order to test the
 * correct functioning of the BluetoothInCallService class, the final class must be put into a
 * container that can be mocked correctly.
 */
public class BluetoothTbsProxy {

    private BluetoothTbs mBluetoothTbs;

    public BluetoothTbsProxy(BluetoothTbs tbs) {
        mBluetoothTbs = tbs;
    }

    public boolean registerBearer(String uci, List<String> uriSchemes, int featureFlags,
            String provider, int technology, Executor executor, BluetoothTbs.Callback callback) {
        return mBluetoothTbs.registerBearer(uci, uriSchemes, featureFlags, provider, technology,
                executor, callback);
    }

    public void unregisterBearer() {
        mBluetoothTbs.unregisterBearer();
    }

    public int getContentControlId() {
        return mBluetoothTbs.getContentControlId();
    }

    public void requestResult(int requestId, int result) {
        mBluetoothTbs.requestResult(requestId, result);
    }

    public void onCallAdded(BluetoothTbsCall call) {
        mBluetoothTbs.onCallAdded(call);
    }

    public void onCallRemoved(UUID callId, int reason) {
        mBluetoothTbs.onCallRemoved(callId, reason);
    }

    public void onCallStateChanged(UUID callId, int state) {
        mBluetoothTbs.onCallStateChanged(callId, state);
    }

    public void currentCallsList(List<BluetoothTbsCall> calls) {
        mBluetoothTbs.currentCallsList(calls);
    }

    public void networkStateChanged(String providerName, int technology) {
        mBluetoothTbs.networkStateChanged(providerName, technology);
    }
}
