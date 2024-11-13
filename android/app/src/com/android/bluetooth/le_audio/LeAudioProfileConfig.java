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

import android.sysprop.BluetoothProperties;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @VisibleForTesting
    static final Set<Integer> TMAP_CALL_GATEWAY_DEPENDENCIES =
            Set.of(
                    BAP_UNICAST_CLIENT,
                    CCP_CALL_CONTROL_SERVER,
                    VCP_VOLUME_CONTROLLER,
                    CSIP_SET_COORDINATOR);

    @VisibleForTesting
    static final Set<Integer> TMAP_UNICAST_MEDIA_SENDER_DEPENDENCIES =
            Set.of(
                    BAP_UNICAST_CLIENT,
                    MCP_MEDIA_CONTROL_SERVER,
                    VCP_VOLUME_CONTROLLER,
                    CSIP_SET_COORDINATOR);

    @VisibleForTesting
    static final Set<Integer> TMAP_BROADCAST_MEDIA_SENDER_DEPENDENCIES =
            Set.of(
                    BAP_BROADCAST_SOURCE,
                    BAP_BROADCAST_ASSISTANT,
                    VCP_VOLUME_CONTROLLER,
                    CSIP_SET_COORDINATOR);

    @VisibleForTesting
    static final Set<Integer> HAP_HEARING_AID_UNICAST_CLIENT_DEPENDENCIES =
            Set.of(BAP_UNICAST_CLIENT, CSIP_SET_COORDINATOR, VCP_VOLUME_CONTROLLER);

    private static final Map<Integer, Set<Integer>> PROFILE_ROLE_DEPENDENCIES =
            Map.ofEntries(
                    Map.entry(TMAP_CALL_GATEWAY, TMAP_CALL_GATEWAY_DEPENDENCIES),
                    Map.entry(TMAP_UNICAST_MEDIA_SENDER, TMAP_UNICAST_MEDIA_SENDER_DEPENDENCIES),
                    Map.entry(
                            TMAP_BROADCAST_MEDIA_SENDER, TMAP_BROADCAST_MEDIA_SENDER_DEPENDENCIES),
                    Map.entry(HAP_UNICAST_CLIENT, HAP_HEARING_AID_UNICAST_CLIENT_DEPENDENCIES));

    private static final Set<Integer> sEnabledUseCases = new HashSet();
    private static final Set<Integer> sEnabledDependencies = new HashSet();

    // "Use Case Profiles" are enabled according to stable sysprops.
    static {
        Log.v(TAG, "Initializing, reading in sysprops");

        if (BluetoothProperties.isProfileTmapCallGatewayEnabled().orElse(false)) {
            enableUseCase(TMAP_CALL_GATEWAY);
        }

        if (BluetoothProperties.isProfileTmapUnicastMediaSenderEnabled().orElse(false)) {
            enableUseCase(TMAP_UNICAST_MEDIA_SENDER);
        }

        if (BluetoothProperties.isProfileTmapBroadcastMediaSenderEnabled().orElse(false)) {
            enableUseCase(TMAP_BROADCAST_MEDIA_SENDER);
        }

        if (BluetoothProperties.isProfileHapHearingAidUnicastClientEnabled().orElse(false)) {
            enableUseCase(HAP_UNICAST_CLIENT);
        }

        StringBuilder sb = new StringBuilder("Enabled use cases:");
        for (int profile : sEnabledUseCases) {
            sb.append(" ").append(idToString(profile)).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        Log.v(TAG, sb.toString());
        sb = new StringBuilder("Enabled dependencies:");
        for (int profile : sEnabledDependencies) {
            sb.append(" ").append(idToString(profile)).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        Log.v(TAG, sb.toString());
    }

    @VisibleForTesting
    static void enableUseCase(int profile) {
        sEnabledUseCases.add(profile);
        sEnabledDependencies.addAll(PROFILE_ROLE_DEPENDENCIES.get(profile));
    }

    public static boolean isTmapCallGatewayEnabled() {
        return sEnabledUseCases.contains(TMAP_CALL_GATEWAY);
    }

    public static boolean isTmapUnicastMediaSenderEnabled() {
        return sEnabledUseCases.contains(TMAP_UNICAST_MEDIA_SENDER);
    }

    public static boolean isTmapBroadcastMediaSenderEnabled() {
        return sEnabledUseCases.contains(TMAP_BROADCAST_MEDIA_SENDER);
    }

    public static boolean isHapHearingAidUnicastClientEnabled() {
        return sEnabledUseCases.contains(HAP_UNICAST_CLIENT);
    }

    public static boolean isBapUnicastClientEnabled() {
        return sEnabledDependencies.contains(BAP_UNICAST_CLIENT);
    }

    public static boolean isBapBroadcastSourceEnabled() {
        return sEnabledDependencies.contains(BAP_BROADCAST_SOURCE);
    }

    public static boolean isBapBroadcastAssistantEnabled() {
        return sEnabledDependencies.contains(BAP_BROADCAST_ASSISTANT);
    }

    public static boolean isBapScanDelegatorEnabled() {
        return sEnabledDependencies.contains(BAP_SCAN_DELEGATOR);
    }

    public static boolean isBapAudioSourceEnabled() {
        return sEnabledDependencies.contains(BAP_AUDIO_SOURCE);
    }

    public static boolean isBapAudioSinkEnabled() {
        return sEnabledDependencies.contains(BAP_AUDIO_SINK);
    }

    public static boolean isCcpCallControlServerEnabled() {
        return sEnabledDependencies.contains(CCP_CALL_CONTROL_SERVER);
    }

    public static boolean isMcpMediaControlServerEnabled() {
        return sEnabledDependencies.contains(MCP_MEDIA_CONTROL_SERVER);
    }

    public static boolean isVcpVolumeControllerEnabled() {
        return sEnabledDependencies.contains(VCP_VOLUME_CONTROLLER);
    }

    public static boolean isCsipSetCoordinatorEnabled() {
        return sEnabledDependencies.contains(CSIP_SET_COORDINATOR);
    }

    /** Helps reset the internal representation for unit testing. */
    @VisibleForTesting
    static void clear() {
        sEnabledUseCases.clear();
        sEnabledDependencies.clear();
    }

    private static String idToString(int profileId) {
        return switch (profileId) {
            case TMAP_CALL_GATEWAY -> "TMAP_CALL_GATEWAY";
            case TMAP_CALL_TERMINAL -> "TMAP_CALL_TERMINAL";
            case TMAP_UNICAST_MEDIA_SENDER -> "TMAP_UNICAST_MEDIA_SENDER";
            case TMAP_UNICAST_MEDIA_RECEIVER -> "TMAP_UNICAST_MEDIA_RECEIVER";
            case TMAP_BROADCAST_MEDIA_SENDER -> "TMAP_BROADCAST_MEDIA_SENDER";
            case TMAP_BROADCAST_MEDIA_RECEIVER -> "TMAP_BROADCAST_MEDIA_RECEIVER";
            case PBP_SOURCE -> "PBP_SOURCE";
            case PBP_SINK -> "PBP_SINK";
            case PBP_ASSISTANT -> "PBP_ASSISTANT";
            case HAP_HEARING_AID -> "HAP_HEARING_AID";
            case HAP_UNICAST_CLIENT -> "HAP_UNICAST_CLIENT";
            case HAP_REMOTE_CONTROLLER -> "HAP_REMOTE_CONTROLLER";
            case BAP_UNICAST_SERVER -> "BAP_UNICAST_SERVER";
            case BAP_UNICAST_CLIENT -> "BAP_UNICAST_CLIENT";
            case BAP_BROADCAST_SOURCE -> "BAP_BROADCAST_SOURCE";
            case BAP_BROADCAST_SINK -> "BAP_BROADCAST_SINK";
            case BAP_SCAN_DELEGATOR -> "BAP_SCAN_DELEGATOR";
            case BAP_BROADCAST_ASSISTANT -> "BAP_BROADCAST_ASSISTANT";
            case BAP_AUDIO_SOURCE -> "BAP_AUDIO_SOURCE";
            case BAP_AUDIO_SINK -> "BAP_AUDIO_SINK";
            case CSIP_SET_COORDINATOR -> "CSIP_SET_COORDINATOR";
            case CSIP_SET_MEMBER -> "CSIP_SET_MEMBER";
            case CCP_CALL_CONTROL_SERVER -> "CCP_CALL_CONTROL_SERVER";
            case CCP_CALL_CONTROL_CLIENT -> "CCP_CALL_CONTROL_CLIENT";
            case MCP_MEDIA_CONTROL_SERVER -> "MCP_MEDIA_CONTROL_SERVER";
            case MCP_MEDIA_CONTROL_CLIENT -> "MCP_MEDIA_CONTROL_CLIENT";
            case VCP_VOLUME_RENDERER -> "VCP_VOLUME_RENDERER";
            case VCP_VOLUME_CONTROLLER -> "VCP_VOLUME_CONTROLLER";
            case MICP_MICROPHONE_DEVICE -> "MICP_MICROPHONE_DEVICE";
            case MICP_MICROPHONE_CONTROLLER -> "MICP_MICROPHONE_CONTROLLER";
            default -> "UNKNOWN_PROFILE";
        };
    }
}
