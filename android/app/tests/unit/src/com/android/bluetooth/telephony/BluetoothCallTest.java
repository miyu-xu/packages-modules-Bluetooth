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

package com.android.bluetooth.telephony;

import static org.junit.Assert.assertThrows;

import static com.google.common.truth.Truth.assertThat;

import android.telecom.Call;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BluetoothCallTest {
    private BluetoothCall mBluetoothCall;

    @Before
    public void setUp() {
        mBluetoothCall = new BluetoothCall(null);
    }

    @Test
    public void getCall() {
        assertThat(mBluetoothCall.getCall()).isNull();
    }

    @Test
    public void isCallNull() {
        assertThat(mBluetoothCall.isCallNull()).isTrue();
    }

    @Test
    public void setCall() {
        mBluetoothCall.setCall(null);

        assertThat(mBluetoothCall.isCallNull()).isTrue();
    }

    @Test
    public void constructor_withUuid() {
        UUID uuid = UUID.randomUUID();

        BluetoothCall bluetoothCall = new BluetoothCall(null, uuid);

        assertThat(bluetoothCall.getTbsCallId()).isEqualTo(uuid);
    }

    @Test
    public void setTbsCallId() {
        UUID uuid = UUID.randomUUID();

        mBluetoothCall.setTbsCallId(uuid);

        assertThat(mBluetoothCall.getTbsCallId()).isEqualTo(uuid);
    }

    @Test
    public void getRemainingPostDialSequence() {
        assertThrows(NullPointerException.class,
                () -> mBluetoothCall.getRemainingPostDialSequence());
    }

    @Test
    public void answer() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.answer(1));
    }

    @Test
    public void deflect() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.deflect(null));
    }

    @Test
    public void reject() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.reject(true, "text"));
    }

    @Test
    public void disconnect() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.disconnect());
    }

    @Test
    public void hold() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.hold());
    }

    @Test
    public void unhold() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.unhold());
    }

    @Test
    public void enterBackgroundAudioProcessing() {
        assertThrows(NullPointerException.class,
                () -> mBluetoothCall.enterBackgroundAudioProcessing());
    }

    @Test
    public void exitBackgroundAudioProcessing() {
        assertThrows(NullPointerException.class,
                () -> mBluetoothCall.exitBackgroundAudioProcessing(true));
    }

    @Test
    public void playDtmfTone() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.playDtmfTone('c'));
    }

    @Test
    public void stopDtmfTone() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.stopDtmfTone());
    }

    @Test
    public void postDialContinue() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.postDialContinue(true));
    }

    @Test
    public void phoneAccountSelected() {
        assertThrows(NullPointerException.class,
                () -> mBluetoothCall.phoneAccountSelected(null, true));
    }

    @Test
    public void conference() {
        BluetoothCall bluetoothCall = new BluetoothCall(null);

        assertThrows(NullPointerException.class, () -> mBluetoothCall.conference(bluetoothCall));
    }

    @Test
    public void splitFromConference() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.splitFromConference());
    }

    @Test
    public void mergeConference() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.mergeConference());
    }

    @Test
    public void swapConference() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.swapConference());
    }

    @Test
    public void pullExternalCall() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.pullExternalCall());
    }

    @Test
    public void sendCallEvent() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.sendCallEvent("event", null));
    }

    @Test
    public void sendRttRequest() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.sendRttRequest());
    }

    @Test
    public void respondToRttRequest() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.respondToRttRequest(1, true));
    }

    @Test
    public void handoverTo() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.handoverTo(null, 1, null));
    }

    @Test
    public void stopRtt() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.stopRtt());
    }

    @Test
    public void removeExtras_withArrayListOfStrings() {
        ArrayList<String> strings = new ArrayList<>();

        assertThrows(NullPointerException.class, () -> mBluetoothCall.removeExtras(strings));
    }

    @Test
    public void removeExtras_withString() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.removeExtras("text"));
    }

    @Test
    public void getParentId() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getParentId());
    }

    @Test
    public void getChildrenIds() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getChildrenIds());
    }

    @Test
    public void getConferenceableCalls() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getConferenceableCalls());
    }

    @Test
    public void getState() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getState());
    }

    @Test
    public void getCannedTextResponses() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getCannedTextResponses());
    }

    @Test
    public void getVideoCall() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getVideoCall());
    }

    @Test
    public void getDetails() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getDetails());
    }

    @Test
    public void getRttCall() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getRttCall());
    }

    @Test
    public void isRttActive() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.isRttActive());
    }

    @Test
    public void registerCallback() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.registerCallback(null));
    }

    @Test
    public void registerCallback_withHandler() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.registerCallback(null, null));
    }

    @Test
    public void unregisterCallback() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.unregisterCallback(null));
    }

    @Test
    public void toString_throwsException() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.toString());
    }

    @Test
    public void addListener() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.addListener(null));
    }

    @Test
    public void removeListener() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.removeListener(null));
    }

    @Test
    public void getGenericConferenceActiveChildCallId() {
        assertThrows(NullPointerException.class,
                () -> mBluetoothCall.getGenericConferenceActiveChildCallId());
    }

    @Test
    public void getContactDisplayName() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getContactDisplayName());
    }

    @Test
    public void getAccountHandle() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getAccountHandle());
    }

    @Test
    public void getVideoState() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getVideoState());
    }

    @Test
    public void getCallerDisplayName() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getCallerDisplayName());
    }

    @Test
    public void equals_withNull() {
        assertThat(mBluetoothCall.equals(null)).isTrue();
    }

    @Test
    public void equals_withBluetoothCall() {
        BluetoothCall bluetoothCall = new BluetoothCall(null);

        assertThat(mBluetoothCall).isEqualTo(bluetoothCall);
    }

    @Test
    public void isSilentRingingRequested() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.isSilentRingingRequested());
    }

    @Test
    public void isConference() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.isConference());
    }

    @Test
    public void can() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.can(1));
    }

    @Test
    public void getHandle() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getHandle());
    }

    @Test
    public void getGatewayInfo() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getGatewayInfo());
    }

    @Test
    public void isIncoming() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.isIncoming());
    }

    @Test
    public void isExternalCall() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.isExternalCall());
    }

    @Test
    public void getId() {
        assertThat(mBluetoothCall.getId()).isEqualTo(System.identityHashCode(null));
    }

    @Test
    public void wasConferencePreviouslyMerged() {
        assertThrows(NullPointerException.class,
                () -> mBluetoothCall.wasConferencePreviouslyMerged());
    }

    @Test
    public void getDisconnectCause() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.getDisconnectCause());
    }

    @Test
    public void getIds_withEmptyList() {
        List<Call> calls = new ArrayList<>();

        List<Integer> result = BluetoothCall.getIds(calls);

        assertThat(result).isEmpty();
    }

    @Test
    public void hasProperty() {
        assertThrows(NullPointerException.class, () -> mBluetoothCall.hasProperty(1));
    }
}
