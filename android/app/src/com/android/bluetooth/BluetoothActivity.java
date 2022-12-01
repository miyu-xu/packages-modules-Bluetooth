/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An activity wrapper that help tracking states.
 * This is useful for testing using ActivityInstrumentationTestCase.
 * See b/260295342 for reason why testing require ActivityInstrumentationTestCase2.
 */
public class BluetoothActivity extends Activity {
    private static final String TAG = "BluetoothActivity";

    public static int STATE_DESTROYED = 0;
    public static int STATE_STARTED = 1;
    public static int STATE_RESUMED = 2;
    public static int STATE_CREATED = 3;

    private final ReentrantLock mStateLock = new ReentrantLock();
    private final Condition mStateLockCondition = mStateLock.newCondition();
    private int mState = STATE_DESTROYED;
    private int mTargetState;

    /**
     * @return whether {stateToWait} was entered during {timeoutMs}
     */
    public boolean waitForState(int stateToWait, int timeoutMs) {
        mStateLock.lock();
        long deadline = System.currentTimeMillis() + timeoutMs;
        mTargetState = stateToWait;
        try {
            while (System.currentTimeMillis() < deadline && mState != stateToWait) {
                long waitTime = Math.max(deadline - System.currentTimeMillis(), 1);
                mStateLockCondition.await(waitTime, TimeUnit.MILLISECONDS);
            }
            return mState == stateToWait;
        } catch (InterruptedException e) {
            Log.d(TAG, String.valueOf(e));
            return mState == stateToWait;
        } finally {
            mStateLock.unlock();
        }
    }

    private void updateState(int state) {
        mStateLock.lock();
        mState = state;
        try {
            if (mTargetState == state) {
                mStateLockCondition.signal();
            }
        } finally {
            mStateLock.unlock();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateState(STATE_CREATED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateState(STATE_STARTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateState(STATE_RESUMED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateState(STATE_STARTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateState(STATE_CREATED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateState(STATE_DESTROYED);
    }
}
