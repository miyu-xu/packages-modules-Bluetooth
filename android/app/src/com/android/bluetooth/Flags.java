/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.bluetooth;

import com.android.bluetooth.flags.FeatureFlags;
import com.android.bluetooth.flags.FeatureFlagsImpl;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Device config flags for Bluetooth app.
 *
 * @hide
 */
public final class Flags {
    private static FeatureFlags sFeatureFlags = new FeatureFlagsImpl();

    @VisibleForTesting
    public static void setFeatureFlags(FeatureFlags featureFlags) {
        sFeatureFlags = featureFlags;
    }

    /** A flag for centralizing audio routing of Bluetooth module. (b/299023147) */
    public static boolean audioRoutingCentralization() {
        return sFeatureFlags.audioRoutingCentralization();
    }
}
