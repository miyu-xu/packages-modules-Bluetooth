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

public class IntentReceiver {
    private static final String TAG = "PairingTest.IntentReceiver";
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

    private InOrder mInOrder = null;

    private Context mContext = null;

    /**
     * Creates an Intent receiver for the list of intents
     *
     * @param context Context
     * @param intentListener Intent listener callback
     * @param intentStrings Array of intents to filter
     */
    public IntentReceiver(
            @NonNull Context context, IntentListener intentListener, String... intentStrings) {
        mContext = context;
        if (registerIntents(intentStrings)) {
            setupListener(intentListener);
        }
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
    public void remove() {
        Log.d(TAG, "Removed");
        mContext.unregisterReceiver(mReceiver);
        mContext = null;
        mReceiver = null;
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

    private boolean registerIntents(String... actions) {
        if (actions.length == 0) {
            Log.w(TAG, "registerIntents(): No intents to register");
            return false;
        }

        IntentFilter filter = new IntentFilter();
        Log.d(TAG, "registerIntents(): Registering for intents: " + Arrays.toString(actions));
        for (String intentString : actions) {
            filter.addAction(intentString);
        }
        MockitoAnnotations.initMocks(this);
        mInOrder = inOrder(mReceiver);
        mContext.registerReceiver(mReceiver, filter);
        return true;
    }

    private void setupListener(IntentListener intentListener) {
        doAnswer(
                        inv -> {
                            Log.d(
                                    TAG,
                                    "onReceive(): intent=" + Arrays.toString(inv.getArguments()));

                            if (intentListener == null) {
                                return null;
                            }

                            Intent intent = inv.getArgument(1);
                            intentListener.onReceive(intent);
                            return null;
                        })
                .when(mReceiver)
                .onReceive(any(), any());
    }
}
