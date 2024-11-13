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

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LeAudioProfileConfigTest {

    // Store each profile's "isEnabled" function in a map, keyed by profileId.
    private static final Map<Integer, BooleanSupplier> PROFILE_ISENABLED_MAP =
            Map.ofEntries(
                    Map.entry(
                            LeAudioProfileConfig.TMAP_CALL_GATEWAY,
                            LeAudioProfileConfig::isTmapCallGatewayEnabled),
                    Map.entry(
                            LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER,
                            LeAudioProfileConfig::isTmapUnicastMediaSenderEnabled),
                    Map.entry(
                            LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER,
                            LeAudioProfileConfig::isTmapBroadcastMediaSenderEnabled),
                    Map.entry(
                            LeAudioProfileConfig.HAP_UNICAST_CLIENT,
                            LeAudioProfileConfig::isHapHearingAidUnicastClientEnabled),
                    Map.entry(
                            LeAudioProfileConfig.BAP_UNICAST_CLIENT,
                            LeAudioProfileConfig::isBapUnicastClientEnabled),
                    Map.entry(
                            LeAudioProfileConfig.BAP_BROADCAST_SOURCE,
                            LeAudioProfileConfig::isBapBroadcastSourceEnabled),
                    Map.entry(
                            LeAudioProfileConfig.BAP_SCAN_DELEGATOR,
                            LeAudioProfileConfig::isBapScanDelegatorEnabled),
                    Map.entry(
                            LeAudioProfileConfig.BAP_BROADCAST_ASSISTANT,
                            LeAudioProfileConfig::isBapBroadcastAssistantEnabled),
                    Map.entry(
                            LeAudioProfileConfig.BAP_AUDIO_SOURCE,
                            LeAudioProfileConfig::isBapAudioSourceEnabled),
                    Map.entry(
                            LeAudioProfileConfig.BAP_AUDIO_SINK,
                            LeAudioProfileConfig::isBapAudioSinkEnabled),
                    Map.entry(
                            LeAudioProfileConfig.CSIP_SET_COORDINATOR,
                            LeAudioProfileConfig::isCsipSetCoordinatorEnabled),
                    Map.entry(
                            LeAudioProfileConfig.CCP_CALL_CONTROL_SERVER,
                            LeAudioProfileConfig::isCcpCallControlServerEnabled),
                    Map.entry(
                            LeAudioProfileConfig.MCP_MEDIA_CONTROL_SERVER,
                            LeAudioProfileConfig::isMcpMediaControlServerEnabled),
                    Map.entry(
                            LeAudioProfileConfig.VCP_VOLUME_CONTROLLER,
                            LeAudioProfileConfig::isVcpVolumeControllerEnabled));

    @Before
    public void setUp() throws Exception {
        LeAudioProfileConfig.clear();
    }

    @After
    public void cleanUp() {
        LeAudioProfileConfig.clear();
    }

    private void assertOnlyTheseProfilesAreEnabled(Set expectedProfilesEnabled) {
        // For each ("profile", "isEnabledFnc") in the map, filter each "profile" based on its
        // corresponding "isEnabledFnc".
        List<Integer> enabledProfiles =
                PROFILE_ISENABLED_MAP.entrySet().stream()
                        .filter(entry -> entry.getValue().getAsBoolean())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        assertThat(enabledProfiles).containsExactlyElementsIn(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - None.
     *
     * <p>Outcome: - No profiles are enabled.
     */
    @Test
    public void nothingEnabled() {
        Set<Integer> noProfilesEnabled = Set.of();
        assertOnlyTheseProfilesAreEnabled(noProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable TMAP Call Gateway profile.
     *
     * <p>Outcome: - Relevant profiles are enabled.
     */
    @Test
    public void enableTmapCallGateway() {
        LeAudioProfileConfig.enableUseCase(LeAudioProfileConfig.TMAP_CALL_GATEWAY);

        Set<Integer> expectedProfilesEnabled = new HashSet();
        expectedProfilesEnabled.add(LeAudioProfileConfig.TMAP_CALL_GATEWAY);
        expectedProfilesEnabled.addAll(LeAudioProfileConfig.TMAP_CALL_GATEWAY_DEPENDENCIES);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable TMAP Unicast Media Sender profile.
     *
     * <p>Outcome: - Relevant profiles are enabled.
     */
    @Test
    public void enableTmapUnicastMediaSender() {
        LeAudioProfileConfig.enableUseCase(LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER);

        Set<Integer> expectedProfilesEnabled = new HashSet();
        expectedProfilesEnabled.add(LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER);
        expectedProfilesEnabled.addAll(LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER_DEPENDENCIES);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable TMAP Broadcast Media Sender profile.
     *
     * <p>Outcome: - Relevant profiles are enabled.
     */
    @Test
    public void enableTmapBroadcastMediaSender() {
        LeAudioProfileConfig.enableUseCase(LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER);

        Set<Integer> expectedProfilesEnabled = new HashSet();
        expectedProfilesEnabled.add(LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER);
        expectedProfilesEnabled.addAll(
                LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER_DEPENDENCIES);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable HAP Hearing Aid Unicast Client profile.
     *
     * <p>Outcome: - Relevant profiles are enabled.
     */
    @Test
    public void enableHapHearingAidUnicastClient() {
        LeAudioProfileConfig.enableUseCase(LeAudioProfileConfig.HAP_UNICAST_CLIENT);

        Set<Integer> expectedProfilesEnabled = new HashSet();
        expectedProfilesEnabled.add(LeAudioProfileConfig.HAP_UNICAST_CLIENT);
        expectedProfilesEnabled.addAll(
                LeAudioProfileConfig.HAP_HEARING_AID_UNICAST_CLIENT_DEPENDENCIES);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }
}
