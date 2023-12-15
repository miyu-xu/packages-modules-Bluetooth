/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.bluetooth;

import static com.google.common.io.BaseEncoding.base16;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class Utils {
    // From bumble_config.json
    public static final String BUMBLE_RANDOM_ADDRESS = "51:F7:A8:75:AC:5E";
    public static final byte[] BUMBLE_IRK = base16().decode("1F66F4B5F0C742F807DD0DDBF64E9213");

    public static String addressStringFromByteString(ByteString bs) {
        StringBuilder refAddrBuilder = new StringBuilder();
        for (int i = 0; i < bs.size(); i++) {
            if (i != 0) {
                refAddrBuilder.append(':');
            }
            refAddrBuilder.append(String.format("%02X", bs.byteAt(i)));
        }
        return refAddrBuilder.toString();
    }

    /**
     * @param address String representing Bluetooth address (case insensitive).
     * @return Decoded address.
     */
    public static byte[] addressBytesFromString(String address) {
        return base16().upperCase().withSeparator(":", 2).decode(address.toUpperCase(Locale.US));
    }

    /**
     * Creates list of parameters using all combinations of given input based on each input's
     * variations.
     *
     * @param variationsPerParam list of inputs with variations its variations
     * @return list of all combinations of input parameters
     */
    public static Collection<Object[]> createParams(List<Object[]> variationsPerParam) {
        List<Object[]> params = new ArrayList<>();

        createParams(0, variationsPerParam, params);

        return params;
    }

    private static void createParams(
            int startIndex, List<Object[]> variationsPerParam, List<Object[]> result) {
        if (variationsPerParam.isEmpty() || startIndex > variationsPerParam.size() - 1) {
            return;
        }

        Object[] currentParamVariations = variationsPerParam.get(startIndex);

        for (Object param : currentParamVariations) {
            Object[] currentParams;

            if (result.isEmpty()) {
                currentParams = new Object[variationsPerParam.size()];
                result.add(currentParams);
            } else {
                currentParams = result.get(result.size() - 1);
            }

            if (currentParams[startIndex] == null) {
                currentParams[startIndex] = param;
            } else {
                Object[] newParams = new Object[variationsPerParam.size()];
                System.arraycopy(currentParams, 0, newParams, 0, startIndex + 1);
                newParams[startIndex] = param;
                result.add(newParams);
            }

            createParams(startIndex + 1, variationsPerParam, result);
        }
    }
}
