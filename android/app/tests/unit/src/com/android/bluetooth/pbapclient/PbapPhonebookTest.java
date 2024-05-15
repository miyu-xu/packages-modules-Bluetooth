/*
 * Copyright 2024 The Android Open Source Project
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
package com.android.bluetooth.pbapclient;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapPhonebookTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private Account mMockAccount;

    private static final String VERSION_21 = "2.1";
    private static final String VERSION_30 = "3.0";
    private static final String VERSION_UNSUPPORTED = "4.0";

    // “LastName;FirstName;MiddleName;Prefix;Suffix”
    private static final String N = "N";

    private static final String FN = "FN";

    private static final String ADDR = "ADR;TYPE=HOME";
    private static final String CELL = "TEL;TYPE=CELL";
    private static final String EMAIL = "EMAIL;INTERNET";

    private static final String TEL = "TEL;TYPE=0";

    // Time format: YYYYMMDDTHHMMSS -> 20050320T100000
    private static final String CALL_HISTORY = "X-IRMC-CALL-DATETIME";
    private static final String MISSED_CALL = "MISSED";
    private static final String INCOMING_CALL = "RECEIVED";
    private static final String OUTGOING_CALL = "DIALED";

    //********************************************************************************************//
    // Create Phonebook
    //********************************************************************************************//

    @Test
    public void testCreatePhonebook_forFavorites_emptyFavoritesPhonebookeCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.FAVORITES_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.FAVORITES_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forLocalPhonebook_emptyLocalPhonebookeCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forIncomingCallHistory_emptyIncomingCallHistoryCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.ICH_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.ICH_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forOutgoingCallHistory_emptyOutgoingCallHistoryCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.OCH_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.OCH_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forMissedCallHistory_emptyMissedCallHistoryCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.MCH_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.MCH_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forSimIncomingCallHistory_emptySimIncomingCallHistoryCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_ICH_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_ICH_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forSimOutgoingCallHistory_emptySimOutgoingCallHistoryCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_OCH_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_OCH_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    @Test
    public void testCreatePhonebook_forSimMissedCallHistory_emptyMiSimssedCallHistoryCreated() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_MCH_PATH);
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_MCH_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(0);
        assertThat(phonebook.getList()).isEmpty();
    }

    //********************************************************************************************//
    // Parse Phonebook
    //********************************************************************************************//

    @Test
    public void testParsePhonebook_forFavorites_favoritesParsed() throws IOException {
        String vcard1 = createVcard(VERSION_21, "Foo", "Bar", "+12345678901", "111 Test Street;Test Town;CA;90210;USA", "Foo@email.com");
        String vcard2 = createVcard(VERSION_21, "Baz", "Bar", "+12345678902", "112 Test Street;Test Town;CA;90210;USA", "Baz@email.com");
        String phonebookString = createPhonebook(Arrays.asList(new String[]{vcard1, vcard2}));

        InputStream stream = toUtf8Stream(phonebookString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.FAVORITES_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.FAVORITES_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forLocalPhonebook_localPhonebookParsed() throws IOException {
        String vcard1 = createVcard(VERSION_21, "Foo", "Bar", "+12345678901", "111 Test Street;Test Town;CA;90210;USA", "Foo@email.com");
        String vcard2 = createVcard(VERSION_21, "Baz", "Bar", "+12345678902", "112 Test Street;Test Town;CA;90210;USA", "Baz@email.com");
        String phonebookString = createPhonebook(Arrays.asList(new String[]{vcard1, vcard2}));

        InputStream stream = toUtf8Stream(phonebookString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.LOCAL_PHONEBOOK_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forSimPhonebook_simPhonebookParsed() throws IOException {
        String vcard1 = createVcard(VERSION_21, "Foo", "Bar", "+12345678901", "111 Test Street;Test Town;CA;90210;USA", "Foo@email.com");
        String vcard2 = createVcard(VERSION_21, "Baz", "Bar", "+12345678902", "112 Test Street;Test Town;CA;90210;USA", "Baz@email.com");
        String phonebookString = createPhonebook(Arrays.asList(new String[]{vcard1, vcard2}));

        InputStream stream = toUtf8Stream(phonebookString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_PHONEBOOK_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_PHONEBOOK_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    //********************************************************************************************//
    // Parse Call History
    //********************************************************************************************//

    @Test
    public void testParsePhonebook_forIncomingCallHistory_incomingCallHistoryParsed() throws IOException {
        String call1 = createCallHistory(VERSION_21, INCOMING_CALL, "20240101T100000", "Foo", "Bar", "+12345678901");
        String call2 = createCallHistory(VERSION_21, INCOMING_CALL, "20240101T110000", "Baz", "Bar", "+12345678902");
        String historyString = createPhonebook(Arrays.asList(new String[]{call1, call2}));

        InputStream stream = toUtf8Stream(historyString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.ICH_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.ICH_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forOutgoingCallHistory_outgoingCallHistoryParsed() throws IOException {
        String call1 = createCallHistory(VERSION_21, OUTGOING_CALL, "20240101T100000", "Foo", "Bar", "+12345678901");
        String call2 = createCallHistory(VERSION_21, OUTGOING_CALL, "20240101T110000", "Baz", "Bar", "+12345678902");
        String historyString = createPhonebook(Arrays.asList(new String[]{call1, call2}));

        InputStream stream = toUtf8Stream(historyString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.OCH_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.OCH_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forMissedCallHistory_missedCallHistoryParsed() throws IOException {
        String call1 = createCallHistory(VERSION_21, MISSED_CALL, "20240101T100000", "Foo", "Bar", "+12345678901");
        String call2 = createCallHistory(VERSION_21, MISSED_CALL, "20240101T110000", "Baz", "Bar", "+12345678902");
        String historyString = createPhonebook(Arrays.asList(new String[]{call1, call2}));

        InputStream stream = toUtf8Stream(historyString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.MCH_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.MCH_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forSimIncomingCallHistory_simIncomingCallHistoryParsed() throws IOException {
        String call1 = createCallHistory(VERSION_21, INCOMING_CALL, "20240101T100000", "Foo", "Bar", "+12345678901");
        String call2 = createCallHistory(VERSION_21, INCOMING_CALL, "20240101T110000", "Baz", "Bar", "+12345678902");
        String historyString = createPhonebook(Arrays.asList(new String[]{call1, call2}));

        InputStream stream = toUtf8Stream(historyString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_ICH_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_ICH_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forSimOutgoingCallHistory_simOutgoingCallHistoryParsed() throws IOException {
        String call1 = createCallHistory(VERSION_21, OUTGOING_CALL, "20240101T100000", "Foo", "Bar", "+12345678901");
        String call2 = createCallHistory(VERSION_21, OUTGOING_CALL, "20240101T110000", "Baz", "Bar", "+12345678902");
        String historyString = createPhonebook(Arrays.asList(new String[]{call1, call2}));

        Log.i("PbapPhonebookTest", "Create phonebook with data=" + historyString);

        InputStream stream = toUtf8Stream(historyString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_OCH_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_OCH_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParsePhonebook_forSimMissedCallHistory_simMissedCallHistoryParsed() throws IOException {
        String call1 = createCallHistory(VERSION_21, MISSED_CALL, "20240101T100000", "Foo", "Bar", "+12345678901");
        String call2 = createCallHistory(VERSION_21, MISSED_CALL, "20240101T110000", "Baz", "Bar", "+12345678902");
        String historyString = createPhonebook(Arrays.asList(new String[]{call1, call2}));

        InputStream stream = toUtf8Stream(historyString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_MCH_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_MCH_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    //********************************************************************************************//
    // Parsing Edge Cases
    //********************************************************************************************//

    @Test
    public void testParse21Phonebook_reportedAs30_parsedCorrectly() throws IOException {
        String vcard1 = createVcard(VERSION_21, "Foo", "Bar", "+12345678901", "111 Test Street;Test Town;CA;90210;USA", "Foo@email.com");
        String vcard2 = createVcard(VERSION_21, "Baz", "Bar", "+12345678902", "112 Test Street;Test Town;CA;90210;USA", "Baz@email.com");
        String phonebookString = createPhonebook(Arrays.asList(new String[]{vcard1, vcard2}));

        InputStream stream = toUtf8Stream(phonebookString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_PHONEBOOK_PATH, PbapPhonebook.FORMAT_VCARD_30, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_PHONEBOOK_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

    @Test
    public void testParse30Phonebook_reportedAs21_parsedCorrectly() throws IOException {
        String vcard1 = createVcard(VERSION_30, "Foo", "Bar", "+12345678901", "111 Test Street;Test Town;CA;90210;USA", "Foo@email.com");
        String vcard2 = createVcard(VERSION_30, "Baz", "Bar", "+12345678902", "112 Test Street;Test Town;CA;90210;USA", "Baz@email.com");
        String phonebookString = createPhonebook(Arrays.asList(new String[]{vcard1, vcard2}));

        InputStream stream = toUtf8Stream(phonebookString);
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.SIM_PHONEBOOK_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream);

        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.SIM_PHONEBOOK_PATH);
        assertThat(phonebook.getCount()).isEqualTo(2);
    }

     @Test
    public void testParseUnsupportedPhonebook_reportedAs21_parsingFailswithException() throws IOException {
        String vcard1 = createVcard(VERSION_UNSUPPORTED, "Foo", "Bar", "+12345678901", "111 Test Street;Test Town;CA;90210;USA", "PORTED@email.com");
        String vcard2 = createVcard(VERSION_UNSUPPORTED, "Baz", "Bar", "+12345678902", "112 Test Street;Test Town;CA;90210;USA", "PORTED@email.com");
        String phonebookString = createPhonebook(Arrays.asList(new String[]{vcard1, vcard2}));

        InputStream stream = toUtf8Stream(phonebookString);
        assertThrows(ParseException.class, () -> new PbapPhonebook(PbapPhonebook.SIM_PHONEBOOK_PATH, PbapPhonebook.FORMAT_VCARD_21, 0, mMockAccount, stream));
    }

    //********************************************************************************************//
    // Debug/Dump/toString()
    //********************************************************************************************//

    @Test
    public void testPhonebookToString() throws IOException {
        PbapPhonebook phonebook = new PbapPhonebook(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        String str = phonebook.toString();
        assertThat(str).isNotNull();
        assertThat(str.length()).isNotEqualTo(0);
    }

    //********************************************************************************************//
    // Utilities
    //********************************************************************************************//

    private String createPhonebook(List<String> vcardStrings) {
        StringBuilder sb = new StringBuilder();
        for (String vcard : vcardStrings) {
            sb.append(vcard).append("\n");
        }
        return sb.toString();
    }

    private String createVcard(String version, String first, String last, String phone, String addr, String email) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:").append(version).append("\n");

        sb.append(FN).append(":").append(first).append(" ").append(last).append("\n");

        sb.append(N).append(":").append(last).append(";").append(first).append("\n");

        if (phone != null) {
            sb.append(CELL).append(":").append(phone).append("\n");
        }

        if (addr != null) {
            sb.append(ADDR).append(":").append(addr).append("\n");
        }

        if (email != null) {
            sb.append(EMAIL).append(":").append(email).append("\n");
        }

        sb.append("END:VCARD");

        return sb.toString();
    }

    private String createCallHistory(String version, String type, String time, String first, String last, String phone) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:").append(version).append("\n");

        if (version == VERSION_30) {
            sb.append(FN).append(":").append(first).append(" ").append(last).append("\n");
        }

        sb.append(N).append(":").append(last).append(";").append(first).append("\n");

        sb.append(TEL).append(":").append(phone).append("\n");

        if (version == VERSION_30) {
            sb.append(CALL_HISTORY).append(";TYPE=").append(type).append(":").append(time).append("\n");
        } else {
            sb.append(CALL_HISTORY).append(";").append(type).append(":").append(time).append("\n");
        }

        sb.append("END:VCARD");

        return sb.toString();
    }

    private InputStream toUtf8Stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
