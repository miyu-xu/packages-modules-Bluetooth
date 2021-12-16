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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class MessageTest {
    private static final String HANDLE = "handle";
    private static final String SUBJECT = "subject";
    private static final String DATE_TIME = "datetime";
    private static final String SENDER_NAME = "sender_name";
    private static final String SENDER_ADDRESS = "sender_addressing";
    private static final String REPLYTO_ADDRESS = "replyto_addressing";
    private static final String RECIPIENT_NAME = "recipient_name";
    private static final String RECIPIENT_ADDRESS = "recipient_addressing";
    private static final String TYPE = "type";
    private static final String SIZE = "size";
    private static final String TEXT = "text";
    private static final String RECEPTION_STATUS = "reception_status";
    private static final String ATTACHMENT_SIZE = "attachment_size";
    private static final String PRIORITY = "priority";
    private static final String READ = "read";
    private static final String SENT = "sent";
    private static final String PROTECTED = "protected";
    private static final String YES = "yes";

    private static final String BAD_FIELD = "this field doesn't exist";

    private static final String HANDLE_STRING = "00000AB";
    private static final int HANDLE_VALUE = 0xAB;
    private static final String DATE_TIME_FORMAT = "YYYYMMdd'T'HHmmss";

    private static final String TYPE_EMAIL = "EMAIL";
    private static final String TYPE_SMS_GSM = "SMS_GSM";
    private static final String TYPE_SMS_CDMA = "SMS_CDMA";
    private static final String TYPE_MMS = "MMS";
    private static final String SIZE_STRING = "27";
    private static final int SIZE_INT = 27;
    private static final String RECEPTION_STATUS_COMPLETE = "complete";
    private static final String RECEPTION_STATUS_FRACTIONED = "fractioned";
    private static final String RECEPTION_STATUS_NOTIFICATION = "notification";

    private static final Date TEST_TIME = new Date();

    private HashMap<String, String> attrs;

    @Before
    public void setUp() throws IOException {
    }

    @Test(expected = NullPointerException.class)
    public void MessageConstructor_nullAttrs() {
        Message testMessage = new Message(attrs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void MessageConstructor_badHandle() {
        attrs = new HashMap<String, String>();
        attrs.put(HANDLE, HANDLE);
        Message testMessage = new Message(attrs);
    }

    @Test
    public void MessageConstructor_noAttributes() {
        attrs = new HashMap<String, String>();
        attrs.put(HANDLE, HANDLE_STRING);
        Message testMessage = new Message(attrs);
        assertThat(testMessage.getSubject()).isNull();
    }

    @Test
    public void MessageConstructor_wellFormatted() {
        attrs = getDefaultMessageAttributes();
        Message testMessage = new Message(attrs);

        assertThat(Integer.valueOf(testMessage.getHandle(), 16)).isEqualTo(HANDLE_VALUE);
        assertThat(testMessage.getSubject()).isEqualTo(SUBJECT);
        assertThat(testMessage.getDateTime().toString()).isEqualTo(TEST_TIME.toString());
        assertThat(testMessage.getSenderName()).isEqualTo(SENDER_NAME);
        assertThat(testMessage.getSenderAddressing()).isEqualTo(SENDER_ADDRESS);
        assertThat(testMessage.getReplytoAddressing()).isEqualTo(REPLYTO_ADDRESS);
        assertThat(testMessage.getRecipientName()).isEqualTo(RECIPIENT_NAME);
        assertThat(testMessage.getRecipientAddressing()).isEqualTo(RECIPIENT_ADDRESS);
        assertThat(testMessage.getType()).isEqualTo(Message.Type.EMAIL);
        assertThat(testMessage.getSize()).isEqualTo(SIZE_INT);
        assertThat(testMessage.isText()).isTrue();
        assertThat(testMessage.getReceptionStatus()).isEqualTo(Message.ReceptionStatus.COMPLETE);
        assertThat(testMessage.getAttachmentSize()).isEqualTo(SIZE_INT);
        assertThat(testMessage.isPriority()).isTrue();
        assertThat(testMessage.isRead()).isTrue();
        assertThat(testMessage.isSent()).isTrue();
        assertThat(testMessage.isProtected()).isTrue();
    }

    @Test
    public void MessageConstructor_extraAttributes() {
        attrs = getDefaultMessageAttributes();
        attrs.put(BAD_FIELD, BAD_FIELD);
        Message testMessage = new Message(attrs);
        assertThat(testMessage.getSubject()).isEqualTo(SUBJECT);
    }

    @Test
    public void MessageConstructor_poorFieldValues() {
        attrs = getDefaultMessageAttributes();
        attrs.put(DATE_TIME, DATE_TIME);
        attrs.put(TYPE, TYPE);
        attrs.put(SIZE, SIZE);
        attrs.put(RECEPTION_STATUS, RECEPTION_STATUS);
        attrs.put(ATTACHMENT_SIZE, ATTACHMENT_SIZE);
        attrs.put(PRIORITY, PRIORITY);

        Message testMessage = new Message(attrs);

        assertThat(testMessage.getDateTime()).isNull();
        assertThat(testMessage.getType()).isEqualTo(Message.Type.UNKNOWN);
        assertThat(testMessage.getSize()).isEqualTo(0);
        assertThat(testMessage.getReceptionStatus()).isEqualTo(Message.ReceptionStatus.UNKNOWN);
        assertThat(testMessage.getAttachmentSize()).isEqualTo(0);
        assertThat(testMessage.isPriority()).isFalse();
    }

    @Test
    public void Message_toString() {
        attrs = getDefaultMessageAttributes();
        Message testMessage = new Message(attrs);
        assertThat(testMessage.toString()).isNotEqualTo("");

        attrs.put(DATE_TIME, DATE_TIME);
        Message testMessage2 = new Message(attrs);
        assertThat(testMessage.toString()).isNotEqualTo(testMessage2.toString());
    }

    @Test
    public void Message_Types() {
        attrs = getDefaultMessageAttributes();
        Message testMessage = new Message(attrs);
        assertThat(testMessage.getType()).isEqualTo(Message.Type.EMAIL);

        attrs.put(TYPE, TYPE_SMS_GSM);
        testMessage = new Message(attrs);
        assertThat(testMessage.getType()).isEqualTo(Message.Type.SMS_GSM);

        attrs.put(TYPE, TYPE_SMS_CDMA);
        testMessage = new Message(attrs);
        assertThat(testMessage.getType()).isEqualTo(Message.Type.SMS_CDMA);

        attrs.put(TYPE, TYPE_MMS);
        testMessage = new Message(attrs);
        assertThat(testMessage.getType()).isEqualTo(Message.Type.MMS);
    }

    @Test
    public void Message_receptionStatus() {
        attrs = getDefaultMessageAttributes();
        Message testMessage = new Message(attrs);
        assertThat(testMessage.getReceptionStatus()).isEqualTo(Message.ReceptionStatus.COMPLETE);

        attrs.put(RECEPTION_STATUS, RECEPTION_STATUS_FRACTIONED);
        testMessage = new Message(attrs);
        assertThat(testMessage.getReceptionStatus()).isEqualTo(Message.ReceptionStatus.FRACTIONED);

        attrs.put(RECEPTION_STATUS, RECEPTION_STATUS_NOTIFICATION);
        testMessage = new Message(attrs);
        assertThat(testMessage.getReceptionStatus())
                .isEqualTo(Message.ReceptionStatus.NOTIFICATION);

        attrs.put(RECEPTION_STATUS, RECEPTION_STATUS);
        testMessage = new Message(attrs);
        assertThat(testMessage.getReceptionStatus()).isEqualTo(Message.ReceptionStatus.UNKNOWN);
    }

    private HashMap<String, String> getDefaultMessageAttributes() {
        attrs = new HashMap<String, String>();
        attrs.put(HANDLE, HANDLE_STRING);
        attrs.put(SUBJECT, SUBJECT);
        attrs.put(DATE_TIME, new SimpleDateFormat(DATE_TIME_FORMAT).format(TEST_TIME));
        attrs.put(SENDER_NAME, SENDER_NAME);
        attrs.put(SENDER_ADDRESS, SENDER_ADDRESS);
        attrs.put(REPLYTO_ADDRESS, REPLYTO_ADDRESS);
        attrs.put(RECIPIENT_NAME, RECIPIENT_NAME);
        attrs.put(RECIPIENT_ADDRESS, RECIPIENT_ADDRESS);
        attrs.put(TYPE, TYPE_EMAIL);
        attrs.put(SIZE, SIZE_STRING);
        attrs.put(TEXT, YES);
        attrs.put(RECEPTION_STATUS, RECEPTION_STATUS_COMPLETE);
        attrs.put(ATTACHMENT_SIZE, SIZE_STRING);
        attrs.put(PRIORITY, YES);
        attrs.put(READ, YES);
        attrs.put(SENT, YES);
        attrs.put(PROTECTED, YES);
        return attrs;
    }
}
