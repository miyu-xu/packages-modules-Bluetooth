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

package android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.ExternalResource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class RestartBluetoothTestRule extends ExternalResource {
    private static final String TAG = RestartBluetoothTestRule.class.getSimpleName();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mBluetoothManager =
            mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

    private final IntentFilter mIntentFilter =
            new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    private CompletableFuture<Integer> mAdapterStateFuture = null;

    private final BroadcastReceiver mAdapterReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state =
                            intent.getIntExtra(
                                    BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (mAdapterStateFuture != null && !mAdapterStateFuture.isDone()) {
                        mAdapterStateFuture.complete(state);
                    }
                }
            };

    public RestartBluetoothTestRule() {}

    @Override
    protected void before() throws Exception {
        Log.i(TAG, "Restarting BluetoothAdapter");

        mContext.registerReceiver(mAdapterReceiver, mIntentFilter);

        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            waitForAdapterState(BluetoothAdapter.STATE_OFF);
        }

        // TODO: b/234892968
        Thread.sleep(3000);

        mBluetoothAdapter.enable();
        waitForAdapterState(BluetoothAdapter.STATE_ON);

        mContext.unregisterReceiver(mAdapterReceiver);
    }

    private void waitForAdapterState(int state) throws InterruptedException, ExecutionException {
        int currentState = mBluetoothAdapter.getState();
        while (currentState != state) {
            mAdapterStateFuture = new CompletableFuture<>();
            currentState = mAdapterStateFuture.get().intValue();
        }
    }
}
