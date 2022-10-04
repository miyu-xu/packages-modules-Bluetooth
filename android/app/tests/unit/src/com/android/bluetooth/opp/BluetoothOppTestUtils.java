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

package com.android.bluetooth.opp;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import android.database.Cursor;

import org.mockito.internal.util.MockUtil;

import java.util.Map;

public class BluetoothOppTestUtils {

    /**
     * A class containing the data to be return by a cursor. Intended to be use with setUpMockCursor
     *
     * @attr mIndex should be returned from cursor.getColumnIndexOrThrow
     * @attr mValue should be returned from cursor.getInt() or cursor.getString() or
     * cursor.getLong()
     */
    public static class BluetoothShareMockData {
        public final int mIndex;
        public final Object mValue;

        public BluetoothShareMockData(int index, Object value) {
            mIndex = index;
            mValue = value;
        }
    }

    /**
     * Set up a mock single-row Cursor that work for common use cases in the OPP package.
     * It mocks the database column index and value of the cell in that column of the current row
     *
     * <pre>
     *  assert(nameToDataMap.get(BluetoothShare.DIRECTION).mIndex == 2)
     *  assert(nameToDataMap.get(2).value == BluetoothShare.DIRECTION_INBOUND)
     *  // This will return 2
     *  int index = cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION)
     *  int direction = cursor.getInt(index) // This will return BluetoothShare.DIRECTION_INBOUND
     * </pre>
     *
     * @param cursor a mock/spy cursor to be setup
     * @param nameToDataMap a map representing what cursor will return
     */
    public static void setUpMockCursor(
            Cursor cursor, Map<String, BluetoothShareMockData> nameToDataMap) {
        assert(MockUtil.isMock(cursor));

        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return nameToDataMap.getOrDefault(
                    name,
                    new BluetoothShareMockData(-1, null)
            ).mIndex;
        }).when(cursor).getColumnIndexOrThrow(anyString());

        doAnswer(invocation -> {
            int index = invocation.getArgument(0);
            for (BluetoothShareMockData data : nameToDataMap.values()) {
                if (data.mIndex == index) {
                    return data.mValue;
                }
            }
            return -1;
        }).when(cursor).getInt(anyInt());

        doAnswer(invocation -> {
            int index = invocation.getArgument(0);
            for (BluetoothShareMockData data : nameToDataMap.values()) {
                if (data.mIndex == index) {
                    return data.mValue;
                }
            }
            return -1;
        }).when(cursor).getLong(anyInt());

        doAnswer(invocation -> {
            int index = invocation.getArgument(0);
            for (BluetoothShareMockData data : nameToDataMap.values()) {
                if (data.mIndex == index) {
                    return data.mValue;
                }
            }
            return null;
        }).when(cursor).getString(anyInt());
    }
}

