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
package com.android.server.bluetooth

import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.util.Log
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothAdapterStateTest {

    lateinit var mState: BluetoothAdapterState

    @Before
    fun setUp() {
        mState = BluetoothAdapterState()
    }

    @Test
    fun testStateIsProperlyInit() {
        Log.d("BluetoothAdapterStateTest", "Initial state is " + mState)
        assertThat(mState.get()).isEqualTo(STATE_OFF)
    }

    @Test
    fun testStateReturnOnlyLastValue() {
        val max = 10
        for (i in 0..max) mState.set(i)
        assertThat(mState.get()).isEqualTo(max)
    }

    @Test
    fun testStateDoesNotTimeoutWhenStateIsAlreadyCorrect() {
        val state = 10
        mState.set(state)
        assertThat(runBlocking { mState.waitForState(10.milliseconds, state) }).isTrue()
    }

    @Test
    fun testStateTimeout() {
        assertThat(runBlocking { mState.waitForState(10.milliseconds, -1) }).isFalse()
    }

    @Test
    fun testStateConcurrent() {
        val state = 42
        runBlocking<Unit> {
            coroutineScope {
                val waiter = async { mState.waitForState(10.milliseconds, state) }
                mState.set(state)
                assertThat(waiter.await()).isTrue()
            }
        }
    }

    @Test
    fun testStateMultipleWaiters() {
        val state0 = 42
        val state1 = 50
        val state2 = 65
        runBlocking<Unit> {
            coroutineScope {
                val waiter0 =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        mState.waitForState(10.milliseconds, state0)
                    }
                val waiter1 =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        mState.waitForState(10.milliseconds, state1)
                    }
                val waiter2 =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        mState.waitForState(10.milliseconds, state2)
                    }
                val waiter3 =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        mState.waitForState(10.milliseconds, -1)
                    }
                mState.set(state0)
                mState.set(state1)
                mState.set(state2)
                assertThat(waiter0.await()).isTrue()
                assertThat(waiter1.await()).isTrue()
                assertThat(waiter2.await()).isTrue()
                assertThat(waiter3.await()).isFalse()
            }
        }
    }

    @Test
    fun testStateTimeoutFromJava() {
        assertThat(mState.waitForState(java.time.Duration.ofMillis(10), -1)).isFalse()
    }

    @Test
    fun testStateCycle() {
        val state0 = 42
        val state1 = 50
        runBlocking<Unit> {
            mState.set(state0)
            mState.set(state1)
            assertThat(mState.waitForState(10.milliseconds, state0)).isFalse()
        }
    }

    @Test
    fun testStateOneOf() {
        val state0 = 42
        val state1 = 50
        val state2 = 65
        mState.set(state0)
        assertThat(mState.oneOf(state0, state1)).isTrue()
        assertThat(mState.oneOf(state1, state2)).isFalse()
    }
}
