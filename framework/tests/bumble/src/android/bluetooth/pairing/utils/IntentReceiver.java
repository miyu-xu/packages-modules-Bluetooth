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

package android.bluetooth.pairing.utils;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;

public class IntentReceiver {
    private static final String TAG = IntentReceiver.class.getSimpleName();
    private static final Duration INTENT_TIMEOUT = Duration.ofSeconds(10);

    /** Interface for listening to the received intents */
    public interface IntentListener {
        /**
         * Callback for receiving intents
         *
         * @param intent Received intent
         */
        void onReceive(Intent intent);
    }

    @Mock private BroadcastReceiver mReceiver;

    private final InOrder mInOrder;

    private final Context mContext;

    private final String[] mIntentStrings;

    private final IntentListener mIntentListener;

    /**
     * Creates an Intent receiver for the list of intents
     *
     * @param context Context
     * @param intentListener Intent listener callback
     * @param intentStrings Array of intents to filter
     */
    public IntentReceiver(
            @NonNull Context context, IntentListener intentListener, String... intentStrings) {
        mIntentStrings = requireNonNull(intentStrings);
        if (mIntentStrings.length == 0) {
            throw new RuntimeException("IntentReceiver(): No intents to register");
        }
        mContext = context;
        mIntentListener = intentListener;
        MockitoAnnotations.initMocks(this);
        mInOrder = inOrder(mReceiver);
        setupListener();
        resume();
    }

    /**
     * Creates an Intent receiver for the list of intents
     *
     * @param context Context
     * @param intentStrings Array of intents to filter
     */
    public IntentReceiver(Context context, String... intentStrings) {
        this(context, null, intentStrings);
    }

    /** Removes the Intent receiver */
    public void close() {
        Log.d(TAG, "close()");
        verifyNoMoreInteractions();
        mContext.unregisterReceiver(mReceiver);
    }

    /**
     * Verifies if the intent is received in order
     *
     * @param matchers Matchers
     */
    public void verifyReceivedOrdered(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    /**
     * Verifies if requested number of intents are received
     *
     * @param num Number of intents
     * @param matchers Matchers
     */
    public void verifyReceived(int num, Matcher<Intent>... matchers) {
        verify(mReceiver, timeout(INTENT_TIMEOUT.toMillis()).times(num))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    /**
     * Verifies if the intent is received
     *
     * @param matchers Matchers
     */
    public void verifyReceived(Matcher<Intent>... matchers) {
        verifyReceived(1, matchers);
    }

    /** Verifies that no more intents are received */
    public void verifyNoMoreInteractions() {
        Mockito.verifyNoMoreInteractions(mReceiver);
    }

    private boolean resume() {
        IntentFilter filter = new IntentFilter();
        Log.d(TAG, "resume(): Registering for intents: " + Arrays.toString(mIntentStrings));
        for (String intentString : mIntentStrings) {
            filter.addAction(intentString);
        }
        mContext.registerReceiver(mReceiver, filter);

        return true;
    }

    private void pause() {
        Log.d(TAG, "pause()");
        mContext.unregisterReceiver(mReceiver);
    }

    /**
     * Performs the test step
     *
     * @param testStep Test step object
     * @return Result of the test step
     * @param <T> context
     * @param <R> Result of the test step
     */
    public <T, R> R performStep(Function<Context, R> testStep) {
        pause();
        R result = testStep.apply(mContext);
        resume();
        return result;
    }

    private void setupListener() {
        doAnswer(
                        inv -> {
                            Log.d(
                                    TAG,
                                    "onReceive(): intent=" + Arrays.toString(inv.getArguments()));

                            if (mIntentListener == null) {
                                return null;
                            }

                            Intent intent = inv.getArgument(1);
                            mIntentListener.onReceive(intent);
                            return null;
                        })
                .when(mReceiver)
                .onReceive(any(), any());
    }
}
