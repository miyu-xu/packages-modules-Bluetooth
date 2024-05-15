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

package com.android.bluetooth.pbapclient;

import android.util.Log;

import com.android.obex.ClientOperation;
import com.android.obex.ClientSession;
import com.android.obex.HeaderSet;
import com.android.obex.ResponseCodes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.InterruptedException;
import java.util.Objects;

abstract class PbapClientRequest {
    static final String TAG = PbapClientRequest.class.getSimpleName();

    // Request Types
    public static final int TYPE_PULL_PHONEBOOK_METADATA = 0;
    public static final int TYPE_PULL_PHONEBOOK = 1;

    protected HeaderSet mHeaderSet;
    private ClientOperation mOperation = null;
    protected int mResponseCode;

    PbapClientRequest() {
        mHeaderSet = new HeaderSet();
        mResponseCode = -1;
    }

    /**
     * A function that returns the type of the request.
     *
     * Used to determine type instead of using 'instanceof'
     */
    public abstract int getType();

    public final boolean isSuccess() {
        return (mResponseCode == ResponseCodes.OBEX_HTTP_OK);
    }

    /**
     * A single point of entry for kicking off a PBAP Client request.
     *
     * Child classes are expected to implement this interface, filling in the details of the request
     * (headers, operation type, error handling, etc).
     */
    public abstract void execute(ClientSession session) throws IOException;

    /**
     * A generica GET operation, providing overridable hooks to read response headers and content.
     */
    protected void executeGet(ClientSession session) throws IOException {
        Log.d(TAG, "Executing GET");
        ClientOperation operation = null;
        try {
            operation = (ClientOperation) session.get(mHeaderSet);

            /* make sure final flag for GET is used (PBAP spec 6.2.2) */
            operation.setGetFinalFlag(true);

            /*
             * this will trigger ClientOperation to use non-buffered stream so
             * we can abort operation
             */
            operation.continueOperation(true, false);

            readResponseHeaders(operation.getReceivedHeader());
            InputStream inputStream = operation.openInputStream();
            readResponse(inputStream);
            inputStream.close();
            mResponseCode = operation.getResponseCode();
        } catch (IOException e) {
            mResponseCode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
            Log.e(TAG, "GET request failed complete", e);
            throw e;
        } finally {
            if (operation != null) {
                operation.close();
            }
        }
        Log.d(TAG, "GET final response code is '" + mResponseCode + "'");
    }

    /**
     * A generica PUT operation, providing overridable hooks to read response headers.
     */
    protected void executePut(ClientSession session, byte[] body) throws IOException {
        Log.d(TAG, "Executing PUT");
        mHeaderSet.setHeader(HeaderSet.LENGTH, Long.valueOf(body.length));
        ClientOperation operation = null;
        try {
            operation = (ClientOperation) session.put(mHeaderSet);
            DataOutputStream outputStream = mOperation.openDataOutputStream();
            outputStream.write(body);
            outputStream.close();
            readResponseHeaders(operation.getReceivedHeader());
            mResponseCode = operation.getResponseCode();
        } catch (IOException e) {
            mResponseCode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
            Log.e(TAG, "PUT request failed to complete", e);
            throw e;
        } finally {
            if (operation != null) {
                operation.close();
            }
        }
        Log.d(TAG, "PUT final response code is '" + mResponseCode + "'");
    }

    protected void readResponseHeaders(HeaderSet headerset) {
        /* nothing here by dafault */
    }

    protected void readResponse(InputStream stream) throws IOException {
        /* nothing here by default */
    }

    /**
     * Get the actual response code associated with the request
     *
     * @return The response code as in integer
     */
    public final int getResponseCode() {
        return mResponseCode;
    }

    public static String typeToString(int type) {
        switch (type) {
            case TYPE_PULL_PHONEBOOK_METADATA:
                return "TYPE_PULL_PHONEBOOK_METADATA";
            case TYPE_PULL_PHONEBOOK:
                return "TYPE_PULL_PHONEBOOK";
            default:
                return "TYPE_RESERVED (" + type + ")";
        }
    }

    @Override
    public String toString() {
        return "<" + TAG + " type=" + typeToString(getType()) + ", responseCode=" + getResponseCode() + ">";
    }
}
