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
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * LE Audio Profiles can be categorized as either "Use Case Profiles" or "Framework Profiles". The
 * LE Audio Framework is modular, and different profiles/modules can be enabled to deliver
 * end-to-end user journeys. "Framework Profiles" correspond to the lower level modules in the
 * Framework, such as BAP, CSIP, MCP, CCP, VCP. "Use Case Profiles" are the high level profiles that
 * determine the correct configuration of low level "Framework Profiles" to enable to deliver
 * particular user journeys. Examples are TMAP, PBP, and HAP.
 *
 * <p>This class serves as a mapping from "Use Case Profiles" to "Framework Profiles", i.e., if a
 * given "Use Case Profile" is enabled, then which "Framework Profiles" must be enabled in order to
 * deliver that user journey.
 *
 * <p>"Use Case Profiles" are read from stable sysprops which a vendor can set to declare which user
 * journeys they would like enabled. This class then translates that into which "Framework Profiles"
 * are enabled. For example, if a vendor enables the TMAP Unicast Media Sender role, then {@link
 * #isBapUnicastClientEnabled} would return {@code true}, while {@link #isBapBroadcastSourceEnabled}
 * would return {@code false}.
 */
public class LeAudioProfileConfig {
    private static final String TAG = LeAudioProfileConfig.class.getSimpleName();

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
    public static final int BAP_BROADCAST_ASSISTANT = 25;
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

    // A matrix to capture the dependencies between "Use Case Profiles" on "Framework Profiles"
    // ("Use Case Profiles" along one dimension, "Framework Profiles" along the other).
    private static final SparseMatrix sEnabled = new SparseMatrix();

    // "Use Case Profiles" are enabled according to stable sysprops.
    static {
        Log.v(TAG, "Initializing, reading in sysprops");

        if (BluetoothProperties.isProfileTmapCallGatewayEnabled().orElse(false)) {
            enableTmapCallGateway();
        }

        if (BluetoothProperties.isProfileTmapUnicastMediaSenderEnabled().orElse(false)) {
            enableTmapUnicastMediaSender();
        }

        if (BluetoothProperties.isProfileTmapBroadcastMediaSenderEnabled().orElse(false)) {
            enableTmapBroadcastMediaSender();
        }

        if (BluetoothProperties.isProfilePbpPublicBroadcastSourceEnabled().orElse(false)) {
            enablePbpPublicBroadcastSource();
        }

        if (BluetoothProperties.isProfilePbpPublicBroadcastAssistantEnabled().orElse(false)) {
            enablePbpPublicBroadcastAssistant();
        }

        if (BluetoothProperties.isProfileHapHearingAidUnicastClientEnabled().orElse(false)) {
            enableHapHearingAidUnicastClient();
        }
    }

    // Enabling "Framework Profiles" for the given "Use Case Profile".
    @VisibleForTesting
    static void enableTmapCallGateway() {
        Log.v(TAG, "enableTmapCallGateway()");

        sEnabled.put(TMAP_CALL_GATEWAY, BAP_UNICAST_CLIENT, true);
        sEnabled.put(TMAP_CALL_GATEWAY, BAP_AUDIO_SOURCE, true);
        sEnabled.put(TMAP_CALL_GATEWAY, BAP_AUDIO_SINK, true);
        sEnabled.put(TMAP_CALL_GATEWAY, CCP_CALL_CONTROL_SERVER, true);
        sEnabled.put(TMAP_CALL_GATEWAY, VCP_VOLUME_CONTROLLER, true);
        sEnabled.put(TMAP_CALL_GATEWAY, CSIP_SET_COORDINATOR, true);
        // spec-optional, but we'll enable it
        sEnabled.put(TMAP_CALL_GATEWAY, BAP_BROADCAST_ASSISTANT, true);
    }

    // Enabling "Framework Profiles" for the given "Use Case Profile".
    @VisibleForTesting
    static void enableTmapUnicastMediaSender() {
        Log.v(TAG, "enableTmapUnicastMediaSender()");

        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_UNICAST_CLIENT, true);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_AUDIO_SOURCE, true);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, MCP_MEDIA_CONTROL_SERVER, true);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, VCP_VOLUME_CONTROLLER, true);
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, CSIP_SET_COORDINATOR, true);
        // spec-optional, but we'll enable it
        sEnabled.put(TMAP_UNICAST_MEDIA_SENDER, BAP_BROADCAST_ASSISTANT, true);
    }

    // Enabling "Framework Profiles" for the given "Use Case Profile".
    @VisibleForTesting
    static void enableTmapBroadcastMediaSender() {
        Log.v(TAG, "enableTmapBroadcastMediaSender()");

        sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, BAP_BROADCAST_SOURCE, true);
        // spec-optional, but we'll enable it
        sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, BAP_BROADCAST_ASSISTANT, true);
        sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, VCP_VOLUME_CONTROLLER, true);
        sEnabled.put(TMAP_BROADCAST_MEDIA_SENDER, CSIP_SET_COORDINATOR, true);
    }

    // Enabling "Framework Profiles" for the given "Use Case Profile".
    @VisibleForTesting
    static void enablePbpPublicBroadcastSource() {
        Log.v(TAG, "enablePbpPublicBroadcastSource()");

        sEnabled.put(PBP_SOURCE, BAP_BROADCAST_SOURCE, true);
    }

    // Enabling "Framework Profiles" for the given "Use Case Profile".
    @VisibleForTesting
    static void enablePbpPublicBroadcastAssistant() {
        Log.v(TAG, "enablePbpPublicBroadcastAssistant()");

        sEnabled.put(PBP_ASSISTANT, BAP_BROADCAST_ASSISTANT, true);
        sEnabled.put(PBP_ASSISTANT, CSIP_SET_COORDINATOR, true);
        // spec-optional, but we'll enable it
        sEnabled.put(PBP_ASSISTANT, VCP_VOLUME_CONTROLLER, true);
    }

    // Enabling "Framework Profiles" for the given "Use Case Profile".
    @VisibleForTesting
    static void enableHapHearingAidUnicastClient() {
        Log.v(TAG, "enableHapHearingAidUnicastClient()");

        sEnabled.put(HAP_UNICAST_CLIENT, BAP_UNICAST_CLIENT, true);
        sEnabled.put(HAP_UNICAST_CLIENT, BAP_AUDIO_SOURCE, true);
        sEnabled.put(HAP_UNICAST_CLIENT, CSIP_SET_COORDINATOR, true);
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

    public static boolean isPbpPublicBroadcastSourceEnabled() {
        return isUseCaseProfileEnabled(PBP_SOURCE);
    }

    public static boolean isPbpPublicBroadcastAssistantEnabled() {
        return isUseCaseProfileEnabled(PBP_ASSISTANT);
    }

    public static boolean isHapHearingAidUnicastClientEnabled() {
        return isUseCaseProfileEnabled(HAP_UNICAST_CLIENT);
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

    private static boolean isUseCaseProfileEnabled(@UseCaseProfile int profile) {
        return sEnabled.isRowExists(profile);
    }

    private static boolean isFrameworkProfileEnabled(@FrameworkProfile int profile) {
        return sEnabled.atLeastOneTrueInColumn(profile);
    }

    /**
     * Determines whether a given "Framework Profile" has been enabled for a particular "Use Case
     * Profile". Useful in situations where a "Framework Profile" is optional by spec for a given
     * "Use Case Profile".
     *
     * @param profile The "Framework Profile".
     * @param useCase The "Use Case Profile".
     * @return {@code true} if the "Framework Profile" is enabled, {@code false} otherwise.
     */
    public static boolean isFrameworkProfileEnabledForUseCase(
            @FrameworkProfile int profile, @UseCaseProfile int useCase) {
        return sEnabled.get(useCase, profile);
    }

    /** Helps reset the internal matrix representation for unit testing. */
    @VisibleForTesting
    static void clear() {
        sEnabled.clear();
    }

    /**
     * A matrix is an easy way of capturing the dependencies of "Use Case Profiles" on "Framework
     * Profiles". Rows correspond to "Use Case Profiles", and are indexed by {@link
     * #UseCaseProfile}. Columns correspond to "Framework Profiles", and are indexed by {@link
     * #FrameworkProfile}. Values are Boolean.
     *
     * <p>Because rows are represented by a {@link SparseArray}, a {@code nonnull} row represents
     * the given "Use Case Profile" is enabled; otherwise, the "Use Case Profile" is considered
     * disabled.
     *
     * <p>{@code A[i][j] = true} represents that "Framework Profile" {@code j} is enabled for "Use
     * Case Profile" {@code i}.
     *
     * <p>"Framework Profile" {@code j} is enabled if it is enabled for at least one "Use Case
     * Profile" (i.e., there exists a {@code i} such that {@code A[i][j] = true}).
     */
    private static class SparseMatrix {
        private final SparseArray<SparseBooleanArray> mA;

        SparseMatrix() {
            mA = new SparseArray<SparseBooleanArray>();
        }

        // Assign A[i][j] := value
        public void put(@UseCaseProfile int i, @FrameworkProfile int j, boolean value) {
            Log.v(TAG, "Setting " + idToString(j) + " to " + value + " for " + idToString(i));

            SparseBooleanArray row = mA.get(i);
            if (row == null) {
                row = new SparseBooleanArray();
                mA.put(i, row);
            }
            row.put(j, value);
        }

        // return A[i][j]
        public boolean get(@UseCaseProfile int i, @FrameworkProfile int j) {
            SparseBooleanArray row = mA.get(i);
            if (row == null) {
                return false;
            }
            return row.get(j, false);
        }

        // return {@code true} if A[k][j] == true for some k.
        public boolean atLeastOneTrueInColumn(@FrameworkProfile int j) {
            boolean result = false;
            for (int i = 0; i < mA.size(); i++) {
                result = result || mA.valueAt(i).get(j, false);
            }
            return result;
        }

        public boolean isRowExists(@UseCaseProfile int i) {
            return mA.get(i) != null;
        }

        @VisibleForTesting
        public void clear() {
            mA.clear();
        }
    }

    private static String idToString(int profileId) {
        switch (profileId) {
            case TMAP_CALL_GATEWAY:
                return "TMAP_CALL_GATEWAY";
            case TMAP_CALL_TERMINAL:
                return "TMAP_CALL_TERMINAL";
            case TMAP_UNICAST_MEDIA_SENDER:
                return "TMAP_UNICAST_MEDIA_SENDER";
            case TMAP_UNICAST_MEDIA_RECEIVER:
                return "TMAP_UNICAST_MEDIA_RECEIVER";
            case TMAP_BROADCAST_MEDIA_SENDER:
                return "TMAP_BROADCAST_MEDIA_SENDER";
            case TMAP_BROADCAST_MEDIA_RECEIVER:
                return "TMAP_BROADCAST_MEDIA_RECEIVER";
            case PBP_SOURCE:
                return "PBP_SOURCE";
            case PBP_SINK:
                return "PBP_SINK";
            case PBP_ASSISTANT:
                return "PBP_ASSISTANT";
            case HAP_HEARING_AID:
                return "HAP_HEARING_AID";
            case HAP_UNICAST_CLIENT:
                return "HAP_UNICAST_CLIENT";
            case HAP_REMOTE_CONTROLLER:
                return "HAP_REMOTE_CONTROLLER";
            case BAP_UNICAST_SERVER:
                return "BAP_UNICAST_SERVER";
            case BAP_UNICAST_CLIENT:
                return "BAP_UNICAST_CLIENT";
            case BAP_BROADCAST_SOURCE:
                return "BAP_BROADCAST_SOURCE";
            case BAP_BROADCAST_SINK:
                return "BAP_BROADCAST_SINK";
            case BAP_SCAN_DELEGATOR:
                return "BAP_SCAN_DELEGATOR";
            case BAP_BROADCAST_ASSISTANT:
                return "BAP_BROADCAST_ASSISTANT";
            case BAP_AUDIO_SOURCE:
                return "BAP_AUDIO_SOURCE";
            case BAP_AUDIO_SINK:
                return "BAP_AUDIO_SINK";
            case CSIP_SET_COORDINATOR:
                return "CSIP_SET_COORDINATOR";
            case CSIP_SET_MEMBER:
                return "CSIP_SET_MEMBER";
            case CCP_CALL_CONTROL_SERVER:
                return "CCP_CALL_CONTROL_SERVER";
            case CCP_CALL_CONTROL_CLIENT:
                return "CCP_CALL_CONTROL_CLIENT";
            case MCP_MEDIA_CONTROL_SERVER:
                return "MCP_MEDIA_CONTROL_SERVER";
            case MCP_MEDIA_CONTROL_CLIENT:
                return "MCP_MEDIA_CONTROL_CLIENT";
            case VCP_VOLUME_RENDERER:
                return "VCP_VOLUME_RENDERER";
            case VCP_VOLUME_CONTROLLER:
                return "VCP_VOLUME_CONTROLLER";
            case MICP_MICROPHONE_DEVICE:
                return "MICP_MICROPHONE_DEVICE";
            case MICP_MICROPHONE_CONTROLLER:
                return "MICP_MICROPHONE_CONTROLLER";
            default:
                return "UNKNOWN_PROFILE";
        }
    }
}
