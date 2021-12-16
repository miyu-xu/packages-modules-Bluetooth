/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.bluetooth.mapclient;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class EventReportTest {
  private static final String EVENT_READ_STATUS_CHANGED =
      "<MAP-event-report version=\"1.1\"><event type=\"ReadStatusChanged\""
          + " handle=\"0400000000000001\" folder=\"telecom/msg/inbox\""
          + " msg_type=\"SMS_GSM\"/></MAP-event-report>";
  private static final String EVENT_MESSAGE_SHIFT =
      "<MAP-event-report version=\"1.1\"><event type=\"MessageShift\" handle=\"0400000000000001\""
          + " folder=\"telecom/msg/inbox\" old_folder =\"telecom/msg/draft\" msg_type=\"SMS_GSM\""
          + " /></MAP-event-report>";
  private static final String EVENT_MEMORY_FULL =
      "<MAP-event-report version=\"1.1\"><event type=\"MemoryFull\" handle=\"0400000000000001\""
          + " folder=\"telecom/msg/inbox\" old_folder =\"telecom/msg/draft\" msg_type=\"SMS_GSM\""
          + " /></MAP-event-report>";
  private static final String EVENT_BAD_HANDLE =
      "<MAP-event-report version=\"1.1\"><event type=\"ReadStatusChanged\""
          + " handle=\"040000000000000G\" folder=\"telecom/msg/inbox\"/></MAP-event-report>";
  private static final String EVENT_BAD_MSG_TYPE =
      "<MAP-event-report version=\"1.1\"><event type=\"ReadStatusChanged\""
          + " handle=\"040000000000000G\""
          + " folder=\"telecom/msg/inbox\"msg_type=\"SMS\"/></MAP-event-report>";
  private static final String EVENT_BAD_EVENT_TYPE =
      "<MAP-event-report version=\"1.1\"><event type=\"ReChanged\" handle=\"040000000000000G\""
          + " folder=\"telecom/msg/inbox\"/></MAP-event-report>";

    private static final String EXPECTED_HANDLE = "0400000000000001";
    private static final String EXPECTED_FOLDER = "telecom/msg/inbox";
    private static final String EXPECTED_OLD_FOLDER = "telecom/msg/draft";
    private static final Bmessage.Type EXPECTED_MSG_TYPE = Bmessage.Type.SMS_GSM;

    DataInputStream mDataInputStream;

    @Before
    public void setUp() throws IOException {
    }

    @Test
    public void EventReport_nullStream() {
        EventReport testEventReport = EventReport.fromStream(null);
        assertThat(testEventReport).isNull();
    }

    @Test
    public void EventReport_emptyStream() {
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport).isNull();
    }

    @Test
    public void EventReport_ReadStatusChanged() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_READ_STATUS_CHANGED.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport.getType()).isEqualTo(EventReport.Type.READ_STATUS_CHANGED);
        assertThat(testEventReport.getHandle()).isEqualTo(EXPECTED_HANDLE);
        assertThat(testEventReport.getFolder()).isEqualTo(EXPECTED_FOLDER);
        assertThat(testEventReport.getOldFolder()).isNull();
        assertThat(testEventReport.getMsgType()).isEqualTo(EXPECTED_MSG_TYPE);
    }

    @Test
    public void EventReport_MessageShift() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_MESSAGE_SHIFT.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport.getType()).isEqualTo(EventReport.Type.MESSAGE_SHIFT);
        assertThat(testEventReport.getHandle()).isEqualTo(EXPECTED_HANDLE);
        assertThat(testEventReport.getFolder()).isEqualTo(EXPECTED_FOLDER);
        assertThat(testEventReport.getOldFolder()).isEqualTo(EXPECTED_OLD_FOLDER);
        assertThat(testEventReport.getMsgType()).isEqualTo(EXPECTED_MSG_TYPE);
    }

    @Test
    public void EventReport_MemoryFull() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_MEMORY_FULL.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport.getType()).isEqualTo(EventReport.Type.MEMORY_FULL);
        assertThat(testEventReport.getHandle()).isNull();
        assertThat(testEventReport.getFolder()).isEqualTo(EXPECTED_FOLDER);
        assertThat(testEventReport.getOldFolder()).isEqualTo(EXPECTED_OLD_FOLDER);
        assertThat(testEventReport.getMsgType()).isNull();
    }

    @Test
    public void EventReport_BadHandle() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_BAD_HANDLE.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport).isNull();
    }

    @Test
    public void EventReport_BadMsgType() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_BAD_MSG_TYPE.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport).isNull();
    }

    @Test
    public void EventReport_BadEventType() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_BAD_EVENT_TYPE.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport).isNull();
    }

    @Test
    public void EventReport_toString() {
        InputStream inputStream = new ByteArrayInputStream(EVENT_MEMORY_FULL.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream);
        EventReport testEventReport = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport.toString()).isNotEqualTo("");

        InputStream inputStream2 = new ByteArrayInputStream(EVENT_READ_STATUS_CHANGED.getBytes(
                StandardCharsets.UTF_8));
        mDataInputStream = new DataInputStream(inputStream2);
        EventReport testEventReport2 = EventReport.fromStream(mDataInputStream);
        assertThat(testEventReport.toString()).isNotEqualTo(testEventReport2.toString());
    }
}