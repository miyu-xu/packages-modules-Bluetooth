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

package com.android.bluetooth.pbap;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

import android.content.Context;
import android.content.res.Resources;

import com.android.obex.Operation;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class HandlerForStringBufferTest {

    @Mock
    private Operation mOperation;

    @Mock
    private OutputStream mOutputStream;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mOperation.openOutputStream()).thenReturn(mOutputStream);
    }

    @Test
    public void onInit_returnsTrue_onSuccess() throws Exception {
        String ownerVcard = "testOwnerVcard";
        HandlerForStringBuffer buffer = new HandlerForStringBuffer(mOperation, ownerVcard);

        assertThat(buffer.onInit()).isTrue();
        verify(mOutputStream).write(ownerVcard.getBytes());
    }

    @Test
    public void onInit_returnsTrue_onSuccess_whenOwnerVcardIsNull() throws Exception {
        String ownerVcard = null;
        HandlerForStringBuffer buffer = new HandlerForStringBuffer(mOperation, ownerVcard);

        assertThat(buffer.onInit()).isTrue();
        verify(mOutputStream, never()).write(any());
    }

    @Test
    public void onInit_returnsFalse_whenIOExceptionHappenedForOpeningStream() throws Exception {
        doThrow(new IOException()).when(mOperation).openOutputStream());

        String ownerVcard = "testOwnerVcard";
        HandlerForStringBuffer buffer = new HandlerForStringBuffer(mOperation, ownerVcard);

        assertThat(buffer.onInit()).isFalse();
    }

    @Test
    public void onInit_returnsFalse_whenIOExceptionHappenedForWritingToStream() throws Exception {
        doThrow(new IOException()).when(mOutputStream).write(any(byte[].class));

        String ownerVcard = "testOwnerVcard";
        HandlerForStringBuffer buffer = new HandlerForStringBuffer(mOperation, ownerVcard);

        assertThat(buffer.onInit()).isFalse();
    }

    @Test
    public void onEntryCreated() throws Exception {
        String ownerVcard = null;
        HandlerForStringBuffer buffer = new HandlerForStringBuffer(mOperation, ownerVcard);
        buffer.onInit();

        String newVcard = "newEntryVcard";

        assertThat(buffer.onEntryCreated(newVcard)).isTrue();
        verify(mOutputStream).write(newVcard.getBytes());
    }

    @Test
    public void onTerminate() throws Exception {
        String ownerVcard = "testOwnerVcard";
        HandlerForStringBuffer buffer = new HandlerForStringBuffer(mOperation, ownerVcard);
        buffer.onInit();

        buffer.onTerminate();

        verify(mOutputStream).close();
    }
}