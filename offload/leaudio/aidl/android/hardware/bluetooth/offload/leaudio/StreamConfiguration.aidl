/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.bluetooth.offload.leaudio;

/**
 * Configuration relative to a stream
 */
parcelable StreamConfiguration {

    /**
     * The group identifier to which belongs
     */
    int groupId;

    /**
     * Maximum size of a PDU
     */
    int maxPduSize;

    /**
     * Constant time transmission interval, in micro-seconds
     */
    int isoIntervalUs;

    /**
     * Number of PDU's transmitted by ISO-Intervals
     */
    int burstNumber;

    /**
     * How many consecutive Isochronous Intervals can be used to transmit a PDU
     */
    int flushTimeout;
}
