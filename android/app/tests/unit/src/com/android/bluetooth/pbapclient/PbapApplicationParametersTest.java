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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
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
import com.android.obex.ResponseCodes;
import com.android.vcard.VCardEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PbapApplicationParametersTest {

    public static final int TEST_LIST_SIZE = 250;
    public static final int TEST_LIST_SIZE_TOO_SMALL = -1;
    public static final int TEST_LIST_SIZE_TOO_LARGE = 65536;

    public static final int TEST_LIST_OFFSET = 0;
    public static final int TEST_LIST_OFFSET_TOO_SMALL = -1;
    public static final int TEST_LIST_OFFSET_TOO_LARGE = 65536;

    public static final String ALL_PROPERTIES_TO_STRING = "<PbapApplicationParameters properties=PROPERTIES_ALL format=1 maxListCount=250 listStartOffset=0>";
    public static final String FILTER_PROPERTIES_TO_STRING = "<PbapApplicationParameters properties=[PROPERTY_VERSION PROPERTY_FN PROPERTY_N] format=1 maxListCount=250 listStartOffset=0>";
    public static final String FILTER_INDIVIDUAL_PROPERTIES_TO_STRING = "<PbapApplicationParameters properties=[PROPERTY_VERSION PROPERTY_FN PROPERTY_N PROPERTY_PHOTO PROPERTY_ADR PROPERTY_TEL PROPERTY_EMAIL PROPERTY_NICKNAME] format=1 maxListCount=250 listStartOffset=0>";
    public static final String FORMAT_21_TO_STRING = "<PbapApplicationParameters properties=PROPERTIES_ALL format=0 maxListCount=250 listStartOffset=0>";

    @Test
    public void testCreateParams_paramsWellFormed() {
        PbapApplicationParameters params = new PbapApplicationParameters(
                PbapApplicationParameters.PROPERTIES_ALL,
                PbapPhonebook.FORMAT_VCARD_30,
                TEST_LIST_SIZE,
                TEST_LIST_OFFSET);

        assertThat(params.getPropertySelectorMask()).isEqualTo(PbapApplicationParameters.PROPERTIES_ALL);
        assertThat(params.getVcardFormat()).isEqualTo(PbapPhonebook.FORMAT_VCARD_30);
        assertThat(params.getMaxListCount()).isEqualTo(TEST_LIST_SIZE);
        assertThat(params.getListStartOffset()).isEqualTo(TEST_LIST_OFFSET);
        assertThat(params.toString()).isEqualTo(ALL_PROPERTIES_TO_STRING);
    }

    @Test
    public void testCreateParams_withPropertyFilter_paramsWellFormed() {
        PbapApplicationParameters params = new PbapApplicationParameters(
                (PbapApplicationParameters.PROPERTY_VERSION | PbapApplicationParameters.PROPERTY_FN | PbapApplicationParameters.PROPERTY_N),
                PbapPhonebook.FORMAT_VCARD_30,
                TEST_LIST_SIZE,
                TEST_LIST_OFFSET);

        assertThat(params.getPropertySelectorMask()).isEqualTo((PbapApplicationParameters.PROPERTY_VERSION | PbapApplicationParameters.PROPERTY_FN | PbapApplicationParameters.PROPERTY_N));
        assertThat(params.getVcardFormat()).isEqualTo(PbapPhonebook.FORMAT_VCARD_30);
        assertThat(params.getMaxListCount()).isEqualTo(TEST_LIST_SIZE);
        assertThat(params.getListStartOffset()).isEqualTo(TEST_LIST_OFFSET);
        assertThat(params.toString()).isEqualTo(FILTER_PROPERTIES_TO_STRING);
    }

    @Test
    public void testCreateParams_withAllPropertiesIndividually_paramsWellFormed() {
        PbapApplicationParameters params = new PbapApplicationParameters(
                (PbapApplicationParameters.PROPERTY_VERSION | PbapApplicationParameters.PROPERTY_FN | PbapApplicationParameters.PROPERTY_N | PbapApplicationParameters.PROPERTY_PHOTO | PbapApplicationParameters.PROPERTY_ADR | PbapApplicationParameters.PROPERTY_TEL | PbapApplicationParameters.PROPERTY_EMAIL | PbapApplicationParameters.PROPERTY_NICKNAME),
                PbapPhonebook.FORMAT_VCARD_30,
                TEST_LIST_SIZE,
                TEST_LIST_OFFSET);

        assertThat(params.getPropertySelectorMask()).isEqualTo((PbapApplicationParameters.PROPERTY_VERSION | PbapApplicationParameters.PROPERTY_FN | PbapApplicationParameters.PROPERTY_N | PbapApplicationParameters.PROPERTY_PHOTO | PbapApplicationParameters.PROPERTY_ADR | PbapApplicationParameters.PROPERTY_TEL | PbapApplicationParameters.PROPERTY_EMAIL | PbapApplicationParameters.PROPERTY_NICKNAME));
        assertThat(params.getVcardFormat()).isEqualTo(PbapPhonebook.FORMAT_VCARD_30);
        assertThat(params.getMaxListCount()).isEqualTo(TEST_LIST_SIZE);
        assertThat(params.getListStartOffset()).isEqualTo(TEST_LIST_OFFSET);
        assertThat(params.toString()).isEqualTo(FILTER_INDIVIDUAL_PROPERTIES_TO_STRING);
    }

    @Test
    public void testCreateParams_withFormat21_paramsWellFormed() {
        PbapApplicationParameters params = new PbapApplicationParameters(
                PbapApplicationParameters.PROPERTIES_ALL,
                PbapPhonebook.FORMAT_VCARD_21,
                TEST_LIST_SIZE,
                TEST_LIST_OFFSET);

        assertThat(params.getPropertySelectorMask()).isEqualTo(PbapApplicationParameters.PROPERTIES_ALL);
        assertThat(params.getVcardFormat()).isEqualTo(PbapPhonebook.FORMAT_VCARD_21);
        assertThat(params.getMaxListCount()).isEqualTo(TEST_LIST_SIZE);
        assertThat(params.getListStartOffset()).isEqualTo(TEST_LIST_OFFSET);
        assertThat(params.toString()).isEqualTo(FORMAT_21_TO_STRING);
    }

    @Test
    public void testCreateParams_maxListCountTooLarge_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () ->
            new PbapApplicationParameters(
                    PbapApplicationParameters.PROPERTIES_ALL,
                    PbapPhonebook.FORMAT_VCARD_30,
                    TEST_LIST_SIZE_TOO_LARGE,
                    TEST_LIST_OFFSET));
    }

    @Test
    public void testCreateParams_maxListCountTooSmall_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () ->
            new PbapApplicationParameters(
                    PbapApplicationParameters.PROPERTIES_ALL,
                    PbapPhonebook.FORMAT_VCARD_30,
                    TEST_LIST_SIZE_TOO_SMALL,
                    TEST_LIST_OFFSET));
    }

    @Test
    public void testCreateParams_OffsetTooLarge_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () ->
            new PbapApplicationParameters(
                    PbapApplicationParameters.PROPERTIES_ALL,
                    PbapPhonebook.FORMAT_VCARD_30,
                    TEST_LIST_SIZE,
                    TEST_LIST_OFFSET_TOO_LARGE));
    }

    @Test
    public void testCreateParams_OffsetTooSmall_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () ->
            new PbapApplicationParameters(
                    PbapApplicationParameters.PROPERTIES_ALL,
                    PbapPhonebook.FORMAT_VCARD_30,
                    TEST_LIST_SIZE,
                    TEST_LIST_OFFSET_TOO_LARGE));
    }
}
