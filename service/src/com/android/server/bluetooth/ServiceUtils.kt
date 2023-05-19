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
package com.android.server.bluetooth

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Timeout value for synchronous binder call */
val syncTimeout = Duration.ofSeconds(3)

class ServiceUtils private constructor() {
    companion object {

        /** @return timeout value for synchronous binder call */
        @JvmStatic
        fun getSyncTimeout(): Duration {
            return syncTimeout
        }

        /** @return an human readable string of the {@code timestamp} parameter */
        @JvmStatic
        fun timeToLog(timestamp: Long): String {
            return DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp))
        }
    }
}
