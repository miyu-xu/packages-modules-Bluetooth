/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.accounts.Account;
import android.util.Log;

import com.android.bluetooth.ObexAppParameters;
import com.android.obex.ClientSession;
import com.android.obex.HeaderSet;
import com.android.vcard.VCardEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RequestPullPhonebook extends PbapClientRequest {
    private static final String TAG = RequestPullPhonebook.class.getSimpleName();

    private static final String TYPE = "x-bt/phonebook";

    private final String mPhonebook;

    private final Account mAccount;

    // Remember the format we requested so we can inform the response parser on how to parse
    private final byte mFormat;

    private final int mListStartOffset;
    private final int mMaxListCount;
    // private final int mMaxIndex;
    // private int mEndIndex;

    private PbapPhonebook mResponse;

    @Override
    public int getType() {
        return TYPE_PULL_PHONEBOOK;
    }

    RequestPullPhonebook(String phonebook, PbapApplicationParameters params, Account account) {
        byte format = params.getVcardFormat();
        int listStartOffset = params.getListStartOffset();
        int maxListCount = params.getMaxListCount();
        long properties = params.getPropertySelectorMask();

        if (format != PbapPhonebook.FORMAT_VCARD_21
                && format != PbapPhonebook.FORMAT_VCARD_30) {
            throw new IllegalArgumentException("Format should be v2.1 or v3.0");
        }

        mPhonebook = phonebook;
        mAccount = account;
        mFormat = format;
        mListStartOffset = listStartOffset;
        mMaxListCount = maxListCount;
        // mMaxIndex = mListStartOffset + maxListCount;
        // mEndIndex = -1;

        mHeaderSet.setHeader(HeaderSet.NAME, phonebook);
        mHeaderSet.setHeader(HeaderSet.TYPE, TYPE);

        ObexAppParameters oap = new ObexAppParameters();

        oap.add(PbapApplicationParameters.HEADER_FORMAT, format);

        if (mListStartOffset > 0) {
            oap.add(PbapApplicationParameters.HEADER_LIST_START_OFFSET, (short) mListStartOffset);
        }

        if (properties != PbapApplicationParameters.PROPERTIES_ALL) {
            oap.add(PbapApplicationParameters.HEADER_PROPERTY_SELECTOR, properties);
        }

        // maxListCount == 0 indicates to fetch all, in which case we set it to the upper bound
        // Note that Java has no unsigned types. To capture an unsigned value in the range [0, 2^16)
        // we need to use an int and cast to a short (2 bytes). This packs the bits we want.
        if (mMaxListCount > 0) {
            oap.add(PbapApplicationParameters.HEADER_MAX_LIST_COUNT, (short) mMaxListCount);
        } else {
            oap.add(PbapApplicationParameters.HEADER_MAX_LIST_COUNT, (short) 65535);
        }

        oap.addToHeaderSet(mHeaderSet);
    }

    @Override
    public void execute(ClientSession session) throws IOException {
        executeGet(session);
    }

    @Override
    protected void readResponse(InputStream stream) throws IOException {
        mResponse = new PbapPhonebook(mPhonebook, mFormat, mListStartOffset, mAccount, stream);
    }

    public String getPhonebook() {
        return mPhonebook;
    }

    public PbapPhonebook getContacts() {
        return mResponse;
    }
}
