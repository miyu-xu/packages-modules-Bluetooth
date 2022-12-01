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

import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An activity wrapper that help tracking stages.
 * This is useful for testing using ActivityInstrumentationTestCase.
 * See b/260295342 for reason why testing require ActivityInstrumentationTestCase2.
 */
public class TestableActivities {
    public static int STATE_DESTROYED = 0;
    public static int STATE_STARTED = 1;
    public static int STATE_RESUMED = 2;
    public static int STATE_CREATED = 3;

    /**
     * See {@link android.bluetooth.AlertActivity}
     */
    public static class AlertActivity extends android.bluetooth.AlertActivity {
        private final ReentrantLock mStateLock = new ReentrantLock();
        private final Condition mStateLockCondition = mStateLock.newCondition();
        private int mState = STATE_DESTROYED;

        /**
         * @return whether {stageToWait} was entered during {timeoutMs}
         */
        public boolean waitForStage(int stageToWait, int timeoutMs) {
            mStateLock.lock();
            long deadline = System.currentTimeMillis() + timeoutMs;
            try {
                while (System.currentTimeMillis() < deadline && mState != stageToWait) {
                    long waitTime = Math.max(deadline - System.currentTimeMillis(), 1);
                    mStateLockCondition.await(waitTime, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Log.d("BluetoothTest", String.valueOf(e));
            } finally {
                mStateLock.unlock();
            }
            return mState == stageToWait;
        }

        private void updateState(int state) {
            mState = state;
            mStateLock.lock();

            try {
                mStateLockCondition.signal();
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

    /**
     * See {@link android.app.Activity}
     */
    public static class Activity extends android.app.Activity {
        private final ReentrantLock mStateLock = new ReentrantLock();
        private final Condition mStateLockCondition = mStateLock.newCondition();
        private int mState = STATE_DESTROYED;

        /**
         * @return whether {stageToWait} was entered during {timeoutMs}
         */
        public boolean waitForStage(int stageToWait, int timeoutMs) {
            mStateLock.lock();
            long deadline = System.currentTimeMillis() + timeoutMs;
            try {
                while (System.currentTimeMillis() < deadline && mState != stageToWait) {
                    long waitTime = Math.max(deadline - System.currentTimeMillis(), 1);
                    mStateLockCondition.await(waitTime, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Log.d("BluetoothTest", String.valueOf(e));
            } finally {
                mStateLock.unlock();
            }
            return true;
        }

        private void updateState(int state) {
            mState = state;
            mStateLock.lock();

            try {
                mStateLockCondition.signal();
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
}
