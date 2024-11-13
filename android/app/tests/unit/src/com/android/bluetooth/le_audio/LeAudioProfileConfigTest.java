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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LeAudioProfileConfigTest {

    @Before
    public void setUp() throws Exception {
        LeAudioProfileConfig.clear();
    }

    @After
    public void cleanUp() {
        LeAudioProfileConfig.clear();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - None.
     *
     * <p>Outcome:
     *   - No profiles are enabled.
     */
    @Test
    public void nothingEnabled() {
        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isFalse();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isFalse();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - Enable TMAP Call Gateway profile.
     *
     * <p>Outcome:
     *   - Relevant profiles are enabled.
     */
    @Test
    public void enableTmapCallGateway_noOptions() {
        LeAudioProfileConfig.enableTmapCallGateway();

        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isFalse();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isTrue();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - Enable TMAP Unicast Media Sender profile.
     *
     * <p>Outcome:
     *   - Relevant profiles are enabled.
     */
    @Test
    public void enableTmapUnicastMediaSender_noOptions() {
        LeAudioProfileConfig.enableTmapUnicastMediaSender();

        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isFalse();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isTrue();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - Enable TMAP Broadcast Media Sender profile.
     *
     * <p>Outcome:
     *   - Relevant profiles are enabled.
     */
    @Test
    public void enableTmapBroadcastMediaSender_noOptions() {
        LeAudioProfileConfig.enableTmapBroadcastMediaSender();

        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isFalse();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isFalse();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - Enable PBP Public Broadcast Source profile.
     *
     * <p>Outcome:
     *   - Relevant profiles are enabled.
     */
    @Test
    public void enablePbpPublicBroadcastSource_noOptions() {
        LeAudioProfileConfig.enablePbpPublicBroadcastSource();

        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isFalse();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isFalse();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - Enable PBP Public Broadcast Assistant profile.
     *
     * <p>Outcome:
     *   - Relevant profiles are enabled.
     */
    @Test
    public void enablePbpPublicBroadcastAssistant_noOptions() {
        LeAudioProfileConfig.enablePbpPublicBroadcastAssistant();

        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isFalse();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isTrue();
    }

    /**
     * Preconditions:
     *   - No "Use Case Profiles" are enabled.
     *
     * <p>Actions:
     *   - Enable HAP Hearing Aid Unicast Client profile.
     *
     * <p>Outcome:
     *   - Relevant profiles are enabled.
     */
    @Test
    public void enableHapHearingAidUnicastClient_noOptions() {
        LeAudioProfileConfig.enableHapHearingAidUnicastClient();

        assertThat(LeAudioProfileConfig.isTmapCallGatewayEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapUnicastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isTmapBroadcastMediaSenderEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isPbpPublicBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isHapHearingAidUnicastClientEnabled()).isTrue();

        assertThat(LeAudioProfileConfig.isBapUnicastClientEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapBroadcastSourceEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapBroadcastAssistantEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapScanDelegatorEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isBapAudioSourceEnabled()).isTrue();
        assertThat(LeAudioProfileConfig.isBapAudioSinkEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCcpCallControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isMcpMediaControlServerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isVcpVolumeControllerEnabled()).isFalse();
        assertThat(LeAudioProfileConfig.isCsipSetCoordinatorEnabled()).isTrue();
    }
}
