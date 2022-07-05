/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Factory class for object initialization to help with unit testing
 */
public class BluetoothPbapObjectsFactory {
    private static final String TAG = BluetoothPbapObjectsFactory.class.getSimpleName();
    private static BluetoothPbapObjectsFactory sInstance;
    private static final Object INSTANCE_LOCK = new Object();

    private BluetoothPbapObjectsFactory() {}

    /**
     * Get the singleton instance of object factory
     *
     * @return the singleton instance, guaranteed not null
     */
    public static BluetoothPbapObjectsFactory getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new BluetoothPbapObjectsFactory();
            }
        }
        return sInstance;
    }

    /**
     * Allow unit tests to substitute BluetoothPbapObjectsFactory with a test instance
     *
     * @param objectsFactory a test instance of the BluetoothPbapObjectsFactory
     */
    @VisibleForTesting
    public static void setInstanceForTesting(BluetoothPbapObjectsFactory objectsFactory) {
        Utils.enforceInstrumentationTestMode();
        synchronized (INSTANCE_LOCK) {
            Log.d(TAG, "setInstanceForTesting(), set to " + objectsFactory);
            sInstance = objectsFactory;
        }
    }

    /**
     * Return the result of {@link ContentResolver#query(Uri, String[], String, String[], String)}.
     */
    public Cursor queryContentUri(ContentResolver contentResolver, final Uri contentUri,
            final String[] projection, final String selection, final String[] selectionArgs,
            final String sortOrder) {
        return contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
    }
}
