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

import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

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
                            LeAudioProfileConfig.PBP_SOURCE,
                            LeAudioProfileConfig::isPbpPublicBroadcastSourceEnabled),
                    Map.entry(
                            LeAudioProfileConfig.PBP_ASSISTANT,
                            LeAudioProfileConfig::isPbpPublicBroadcastAssistantEnabled),
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
        BooleanSupplier profileIsEnabledFnc;

        // For each ("profile", "isEnabledFnc") in the map, if the "profile" is expected to be
        // enabled, then assert that the profile's "isEnabledFnc" returns "true", otherwise, assert
        // that it returns "false".
        for (int profileId : PROFILE_ISENABLED_MAP.keySet()) {
            profileIsEnabledFnc = PROFILE_ISENABLED_MAP.get(profileId);

            if (expectedProfilesEnabled.contains(profileId)) {
                assertThat(profileIsEnabledFnc.getAsBoolean()).isTrue();
            } else {
                assertThat(profileIsEnabledFnc.getAsBoolean()).isFalse();
            }
        }
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
        LeAudioProfileConfig.enableTmapCallGateway();

        Set<Integer> expectedProfilesEnabled =
                Set.of(
                        LeAudioProfileConfig.TMAP_CALL_GATEWAY,
                        LeAudioProfileConfig.BAP_UNICAST_CLIENT,
                        LeAudioProfileConfig.BAP_AUDIO_SOURCE,
                        LeAudioProfileConfig.BAP_AUDIO_SINK,
                        LeAudioProfileConfig.CCP_CALL_CONTROL_SERVER,
                        LeAudioProfileConfig.VCP_VOLUME_CONTROLLER,
                        LeAudioProfileConfig.CSIP_SET_COORDINATOR,
                        LeAudioProfileConfig.BAP_BROADCAST_ASSISTANT);
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
        LeAudioProfileConfig.enableTmapUnicastMediaSender();

        Set<Integer> expectedProfilesEnabled =
                Set.of(
                        LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER,
                        LeAudioProfileConfig.BAP_UNICAST_CLIENT,
                        LeAudioProfileConfig.BAP_AUDIO_SOURCE,
                        LeAudioProfileConfig.CSIP_SET_COORDINATOR,
                        LeAudioProfileConfig.MCP_MEDIA_CONTROL_SERVER,
                        LeAudioProfileConfig.VCP_VOLUME_CONTROLLER,
                        LeAudioProfileConfig.BAP_BROADCAST_ASSISTANT);
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
        LeAudioProfileConfig.enableTmapBroadcastMediaSender();

        Set<Integer> expectedProfilesEnabled =
                Set.of(
                        LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER,
                        LeAudioProfileConfig.BAP_BROADCAST_SOURCE,
                        LeAudioProfileConfig.BAP_BROADCAST_ASSISTANT,
                        LeAudioProfileConfig.VCP_VOLUME_CONTROLLER,
                        LeAudioProfileConfig.CSIP_SET_COORDINATOR);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable PBP Public Broadcast Source profile.
     *
     * <p>Outcome: - Relevant profiles are enabled.
     */
    @Test
    public void enablePbpPublicBroadcastSource() {
        LeAudioProfileConfig.enablePbpPublicBroadcastSource();

        Set<Integer> expectedProfilesEnabled =
                Set.of(LeAudioProfileConfig.PBP_SOURCE, LeAudioProfileConfig.BAP_BROADCAST_SOURCE);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable PBP Public Broadcast Assistant profile.
     *
     * <p>Outcome: - Relevant profiles are enabled.
     */
    @Test
    public void enablePbpPublicBroadcastAssistant() {
        LeAudioProfileConfig.enablePbpPublicBroadcastAssistant();

        Set<Integer> expectedProfilesEnabled =
                Set.of(
                        LeAudioProfileConfig.PBP_ASSISTANT,
                        LeAudioProfileConfig.BAP_BROADCAST_ASSISTANT,
                        LeAudioProfileConfig.CSIP_SET_COORDINATOR,
                        LeAudioProfileConfig.VCP_VOLUME_CONTROLLER);
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
        LeAudioProfileConfig.enableHapHearingAidUnicastClient();

        Set<Integer> expectedProfilesEnabled =
                Set.of(
                        LeAudioProfileConfig.HAP_UNICAST_CLIENT,
                        LeAudioProfileConfig.BAP_UNICAST_CLIENT,
                        LeAudioProfileConfig.BAP_AUDIO_SOURCE,
                        LeAudioProfileConfig.CSIP_SET_COORDINATOR);
        assertOnlyTheseProfilesAreEnabled(expectedProfilesEnabled);
    }

    /**
     * Preconditions: - No "Use Case Profiles" are enabled.
     *
     * <p>Actions: - Enable TMAP Unicast Media Sender profile. - Enable TMAP Broadcast Media Sender
     * profile.
     *
     * <p>Outcome: - BapUnicastClient is enabled. - BapBroadcastSource is enabled. -
     * BapUnicastClient is enabled for TmapUnicastMediaSender, but disabled for
     * TmapBroadcastMediaSender. - BapBroadcastSource is enabled for TmapBroadcastMediaSender, but
     * disabled for TmapUnicastMediaSender.
     */
    @Test
    public void isFrameworkProfileEnabledForUseCase() {
        LeAudioProfileConfig.enableTmapUnicastMediaSender();
        LeAudioProfileConfig.enableTmapBroadcastMediaSender();

        // Assert that both TMAP UMS and BMS are enabled.
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isTrue();

        // Assert that BAP Unicast Client and BAP Broadcast Source are enabled as a result.
        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isTrue();

        // Assert that BAP Unicast Client is only enabled for TMAP UMS, and not for TMAP BMS.
        assertThat(
                        LeAudioProfileConfig.isFrameworkProfileEnabledForUseCase(
                                LeAudioProfileConfig.BAP_UNICAST_CLIENT,
                                LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER))
                .isTrue();
        assertThat(
                        LeAudioProfileConfig.isFrameworkProfileEnabledForUseCase(
                                LeAudioProfileConfig.BAP_UNICAST_CLIENT,
                                LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER))
                .isFalse();

        // Assert that BAP Broadcast Source is only enabled for TMAP BMS, and not for TMAP UMS.
        assertThat(
                        LeAudioProfileConfig.isFrameworkProfileEnabledForUseCase(
                                LeAudioProfileConfig.BAP_BROADCAST_SOURCE,
                                LeAudioProfileConfig.TMAP_UNICAST_MEDIA_SENDER))
                .isFalse();
        assertThat(
                        LeAudioProfileConfig.isFrameworkProfileEnabledForUseCase(
                                LeAudioProfileConfig.BAP_BROADCAST_SOURCE,
                                LeAudioProfileConfig.TMAP_BROADCAST_MEDIA_SENDER))
                .isTrue();
    }
}
