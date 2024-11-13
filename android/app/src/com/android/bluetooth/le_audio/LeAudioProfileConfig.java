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

package com.android.bluetooth.le_audio;

import android.annotation.IntDef;
import android.sysprop.BluetoothProperties;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/** tbd */
public class LeAudioProfileConfig {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "STATUS_",
            value = {
                STATUS_UNDEFINED,
                STATUS_DISABLED,
                STATUS_ENABLED
            })
    public @interface Status {}

    public static final int STATUS_UNDEFINED = -1;
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                TMAP_CALL_GATEWAY,
                TMAP_CALL_TERMINAL,
                TMAP_UNICAST_MEDIA_SENDER,
                TMAP_UNICAST_MEDIA_RECEIVER,
                TMAP_BROADCAST_MEDIA_SENDER,
                TMAP_BROADCAST_MEDIA_RECEIVER,
                PBP_SOURCE,
                PBP_SINK,
                PBP_ASSISTANT,
                HAP_HEARING_AID,
                HAP_UNICAST_CLIENT,
                HAP_REMOTE_CONTROLLER
            })
    public @interface UseCaseProfile {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BAP_UNICAST_SERVER,
                BAP_UNICAST_CLIENT,
                BAP_BROADCAST_SOURCE,
                BAP_BROADCAST_SINK,
                BAP_SCAN_DELEGATOR,
                BAP_BROADCAST_ASSISTANT,
                BAP_AUDIO_SOURCE,
                BAP_AUDIO_SINK,
                CSIP_SET_COORDINATOR,
                CSIP_SET_MEMBER,
                CCP_CALL_CONTROL_SERVER,
                CCP_CALL_CONTROL_CLIENT,
                MCP_MEDIA_CONTROL_SERVER,
                MCP_MEDIA_CONTROL_CLIENT,
                VCP_VOLUME_RENDERER,
                VCP_VOLUME_CONTROLLER,
                MICP_MICROPHONE_DEVICE,
                MICP_MICROPHONE_CONTROLLER
            })
    public @interface FrameworkProfile {}

    public static final int TMAP_CALL_GATEWAY = 1;
    public static final int TMAP_CALL_TERMINAL = 2;
    public static final int TMAP_UNICAST_MEDIA_SENDER = 3;
    public static final int TMAP_UNICAST_MEDIA_RECEIVER = 4;
    public static final int TMAP_BROADCAST_MEDIA_SENDER = 5;
    public static final int TMAP_BROADCAST_MEDIA_RECEIVER = 6;
    public static final int PBP_SOURCE = 7;
    public static final int PBP_SINK = 8;
    public static final int PBP_ASSISTANT = 9;
    public static final int HAP_HEARING_AID = 10;
    public static final int HAP_UNICAST_CLIENT = 11;
    public static final int HAP_REMOTE_CONTROLLER = 12;

    public static final int BAP_UNICAST_SERVER = 20;
    public static final int BAP_UNICAST_CLIENT = 21;
    public static final int BAP_BROADCAST_SOURCE = 22;
    public static final int BAP_BROADCAST_SINK = 23;
    public static final int BAP_SCAN_DELEGATOR = 24;
    public static final int BAP_BROADCAST_ASSISTANT =25;
    public static final int BAP_AUDIO_SOURCE = 26;
    public static final int BAP_AUDIO_SINK = 27;

    public static final int CSIP_SET_COORDINATOR = 30;
    public static final int CSIP_SET_MEMBER = 31;
    public static final int CCP_CALL_CONTROL_SERVER = 32;
    public static final int CCP_CALL_CONTROL_CLIENT = 33;
    public static final int MCP_MEDIA_CONTROL_SERVER = 34;
    public static final int MCP_MEDIA_CONTROL_CLIENT = 35;
    public static final int VCP_VOLUME_RENDERER = 36;
    public static final int VCP_VOLUME_CONTROLLER = 37;
    public static final int MICP_MICROPHONE_DEVICE = 38;
    public static final int MICP_MICROPHONE_CONTROLLER = 39;

    private static class SparseMatrix {
        private final SparseArray<SparseIntArray> mA;

        public SparseMatrix() {
            mA = new SparseArray<SparseIntArray>();
        }

        // Assign A[i][j] := value
        public void put(@UseCaseProfile int i, @FrameworkProfile int j, @Status int value) {
            SparseIntArray row = mA.get(i);
            if (row == null) {
                row = new SparseIntArray();
                mA.put(i, row);
            }
            row.put(j, value);
        }

        // // return A[i][j]
        // public @Status int get(@UseCaseProfile int i, @FrameworkProfile int j) {
        //     SparseIntArray row = mA.get(i);
        //     if (row == null) {
        //         return STATUS_UNDEFINED;
        //     }
        //     return row.get(j, STATUS_UNDEFINED);
        // }

        // return {@code true} if A[k][j] == val for some k.
        public boolean atLeastOneInstanceOfInColumn(@FrameworkProfile int j, @Status int val) {
            boolean result = false;
            for (int i = 0; i < mA.size(); i++) {
                result = result || (mA.valueAt(i).get(j, STATUS_UNDEFINED) == val);
            }
            return result;
        }

        public void deleteRow(@UseCaseProfile int i) {
            mA.delete(i);
        }

        public boolean isRowExists(@UseCaseProfile int i) {
            return mA.get(i) != null;
        }
    }

    private static final SparseMatrix sEnabled = new SparseMatrix();

    static {
        if (BluetoothProperties.isProfileTmapCallGatewayEnabled().orElse(false)) {
            enableTmapCallGateway();
        }

        if (BluetoothProperties.isProfileTmapUnicastMediaSenderEnabled().orElse(false)) {
            enableTmapUnicastMediaSender();
        }

        if (BluetoothProperties.isProfileTmapBroadcastMediaSenderEnabled().orElse(false)) {
            enableTmapBroadcastMediaSender();
        }
    }

    static void enableTmapCallGateway() {
        sEnabled.put(TMAP_CALL_GATEWAY, BAP_UNICAST_CLIENT, STATUS_ENABLED);
        sEnabled.put(TMAP_CALL_GATEWAY, BAP_AUDIO_SOURCE, STATUS_ENABLED);
        sEnabled.put(TMAP_CALL_GATEWAY, BAP_AUDIO_SINK, STATUS_ENABLED);
        sEnabled.put(TMAP_CALL_GATEWAY, CCP_CALL_CONTROL_SERVER, STATUS_ENABLED);
        sEnabled.put(TMAP_CALL_GATEWAY, VCP_VOLUME_CONTROLLER, STATUS_ENABLED);

        List<BluetoothProperties.TmapCallGatewayOptions_values> options =
                BluetoothProperties.TmapCallGatewayOptions();

        if (options.contains(BluetoothProperties.TmapCallGatewayOptions_values
                        .BAP_BROADCAST_ASSISTANT)
                || options.contains(BluetoothProperties.TmapCallGatewayOptions_values
                        .BAP_SCAN_DELEGATOR)) {
            sEnabled.put(TMAP_CALL_GATEWAY, BAP_BROADCAST_ASSISTANT, STATUS_ENABLED);
        }

        if (options.contains(BluetoothProperties.TmapCallGatewayOptions_values
                .BAP_SCAN_DELEGATOR)) {
            sEnabled.put(TMAP_CALL_GATEWAY, BAP_SCAN_DELEGATOR, STATUS_ENABLED);
        }

        sEnabled.put(TMAP_CALL_GATEWAY, CSIP_SET_COORDINATOR, STATUS_ENABLED);
    }

    static void enableTmapUnicastMediaSender() {
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_UNICAST_CLIENT, STATUS_ENABLED);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_AUDIO_SOURCE, STATUS_ENABLED);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, MCP_MEDIA_CONTROL_SERVER, STATUS_ENABLED);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, VCP_VOLUME_CONTROLLER, STATUS_ENABLED);

        List<BluetoothProperties.TmapUnicastMediaSenderOptions_values> options =
                BluetoothProperties.TmapUnicastMediaSenderOptions();

        if (options.contains(BluetoothProperties.TmapUnicastMediaSenderOptions_values
                        .BAP_BROADCAST_ASSISTANT)
                || options.contains(BluetoothProperties.TmapUnicastMediaSenderOptions_values
                        .BAP_SCAN_DELEGATOR)) {
            sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_BROADCAST_ASSISTANT, STATUS_ENABLED);
        }

        if (options.contains(BluetoothProperties.TmapUnicastMediaSenderOptions_values
                .BAP_SCAN_DELEGATOR)) {
            sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_SCAN_DELEGATOR, STATUS_ENABLED);
        }

        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, CSIP_SET_COORDINATOR, STATUS_ENABLED);
    }

    static void enableTmapBroadcastMediaSender() {
        sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, BAP_BROADCAST_SOURCE, STATUS_ENABLED);

        List<BluetoothProperties.TmapBroadcastMediaSenderOptions_values> options =
                BluetoothProperties.TmapBroadcastMediaSenderOptions();

        if (options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                        .BAP_BROADCAST_ASSISTANT)
                || options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                        .BAP_SCAN_DELEGATOR)) {
            sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, BAP_BROADCAST_ASSISTANT, STATUS_ENABLED);
        }

        if (options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                .BAP_SCAN_DELEGATOR)) {
            sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, BAP_SCAN_DELEGATOR, STATUS_ENABLED);
        }

        if (options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                .VCP_VOLUME_CONTROLLER)) {
            sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, VCP_VOLUME_CONTROLLER, STATUS_ENABLED);
        }

        if (options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                        .BAP_BROADCAST_ASSISTANT)
                || options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                        .BAP_SCAN_DELEGATOR)
                || options.contains(BluetoothProperties.TmapBroadcastMediaSenderOptions_values
                        .VCP_VOLUME_CONTROLLER)) {
            sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, CSIP_SET_COORDINATOR, STATUS_ENABLED);
        }
    }

    private static void disableUseCaseProfile(@UseCaseProfile int profile) {
        sEnabled.deleteRow(profile);
    }

    static void disableTmapCallGateway() {
        disableUseCaseProfile(TMAP_CALL_GATEWAY);
    }

    static void disableTmapUnicastMediaSender() {
        disableUseCaseProfile(TMAP_UNICAST_MEDIA_SENDER);
    }

    static void disableTmapBroadcastMediaSender() {
        disableUseCaseProfile(TMAP_BROADCAST_MEDIA_SENDER);
    }

    private static boolean isUseCaseProfileEnabled(@UseCaseProfile int profile) {
        return sEnabled.isRowExists(profile);
    }

    private static boolean isFrameworkProfileEnabled(@FrameworkProfile int profile) {
        return sEnabled.atLeastOneInstanceOfInColumn(profile, STATUS_ENABLED);
    }

    public static boolean isTmapCallGatewayEnabled() {
        return isUseCaseProfileEnabled(TMAP_CALL_GATEWAY);
    }

    public static boolean isTmapUnicastMediaSenderEnabled() {
        return isUseCaseProfileEnabled(TMAP_UNICAST_MEDIA_SENDER);
    }

    public static boolean isTmapBroadcastMediaSenderEnabled() {
        return isUseCaseProfileEnabled(TMAP_BROADCAST_MEDIA_SENDER);
    }

    public static boolean isBapUnicastClientEnabled() {
        return isFrameworkProfileEnabled(BAP_UNICAST_CLIENT);
    }

    public static boolean isBapBroadcastSourceEnabled() {
        return isFrameworkProfileEnabled(BAP_BROADCAST_SOURCE);
    }

    public static boolean isBapBroadcastAssistantEnabled() {
        return isFrameworkProfileEnabled(BAP_BROADCAST_ASSISTANT);
    }

    public static boolean isBapScanDelegatorEnabled() {
        return isFrameworkProfileEnabled(BAP_SCAN_DELEGATOR);
    }

    public static boolean isBapAudioSourceEnabled() {
        return isFrameworkProfileEnabled(BAP_AUDIO_SOURCE);
    }

    public static boolean isBapAudioSinkEnabled() {
        return isFrameworkProfileEnabled(BAP_AUDIO_SINK);
    }

    public static boolean isCcpCallControlServerEnabled() {
        return isFrameworkProfileEnabled(CCP_CALL_CONTROL_SERVER);
    }

    public static boolean isMcpMediaControlServerEnabled() {
        return isFrameworkProfileEnabled(MCP_MEDIA_CONTROL_SERVER);
    }

    public static boolean isVcpVolumeControllerEnabled() {
        return isFrameworkProfileEnabled(VCP_VOLUME_CONTROLLER);
    }

    public static boolean isCsipSetCoordinatorEnabled() {
        return isFrameworkProfileEnabled(CSIP_SET_COORDINATOR);
    }
}
