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

package android.bluetooth.pairing;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;

class IntentReceiver {
    private static final String TAG = "PairingTest.IntentReceiver";
    private static final Duration INTENT_TIMEOUT = Duration.ofSeconds(10);

    private final Context mContext;
    private final InOrder mInOrder;
    private final String[] mIntentStrings;

    // Mock
    private final BroadcastReceiver mMockReceiver;

    IntentReceiver(@NonNull Context context, String... intentStrings) {
        mContext = requireNonNull(context);
        if (intentStrings.length == 0) {
            throw new IllegalArgumentException("IntentReceiver has no intents to register");
        }
        mIntentStrings = intentStrings;
        mMockReceiver = Mockito.mock(BroadcastReceiver.class);
        mInOrder = inOrder(mMockReceiver);
        Answer receiverAnswer =
                inv -> {
                    Log.d(TAG, "onReceive(): intent=" + Arrays.toString(inv.getArguments()));
                    return null;
                };
        doAnswer(receiverAnswer).when(mMockReceiver).onReceive(any(), any());
        start();
    }

    void verifyReceivedOrdered(Matcher<Intent>... matchers) {
        mInOrder.verify(mMockReceiver, timeout(INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    void verifyReceived(int num, Matcher<Intent>... matchers) {
        verify(mMockReceiver, timeout(INTENT_TIMEOUT.toMillis()).times(num))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    void verifyReceived(Matcher<Intent>... matchers) {
        verifyReceived(1, matchers);
    }

    private IntentReceiver start() {
        Log.d(TAG, "start(): Intents:" + Arrays.toString(mIntentStrings));
        IntentFilter filter = new IntentFilter();
        Arrays.stream(mIntentStrings).forEach(filter::addAction);
        mContext.registerReceiver(mMockReceiver, filter);
        return this;
    }

    IntentReceiver stop() {
        Log.d(TAG, "stop()");
        mContext.unregisterReceiver(mMockReceiver);
        Mockito.verifyNoMoreInteractions(mMockReceiver);
        return this;
    }

    <T, R> R performStep(Function<Context, R> testStep) {
        stop();
        R result = testStep.apply(mContext);
        start();
        return result;
    }
}
