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

package com.android.bluetooth.avrcp;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioDeviceCallback;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Looper;
import android.os.UserManager;
import android.os.test.TestLooper;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.audio_util.Image;
import com.android.bluetooth.audio_util.Metadata;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AvrcpTargetServiceTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    private @Mock AdapterService mMockAdapterService;
    private @Mock AudioManager mMockAudioManager;
    private @Mock AvrcpNativeInterface mMockNativeInterface;
    private @Mock UserManager mMockUserManager;
    private @Mock Resources mMockResources;
    private @Mock SharedPreferences mMockSharedPreferences;
    private @Mock SharedPreferences.Editor mMockSharedPreferencesEditor;

    private @Captor ArgumentCaptor<AudioDeviceCallback> mAudioDeviceCb;

    private MediaSessionManager mMediaSessionManager;
    private TestLooper mLooper;

    private static final String TEST_DATA = "-1";

    @Before
    public void setUp() throws Exception {
        mLooper = new TestLooper();
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mLooper.startAutoDispatch();

        when(mMockAdapterService.getSystemService(Context.AUDIO_SERVICE))
                .thenReturn(mMockAudioManager);
        when(mMockAdapterService.getSystemServiceName(AudioManager.class))
                .thenReturn(Context.AUDIO_SERVICE);

        mMediaSessionManager =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext()
                        .getSystemService(MediaSessionManager.class);
        when(mMockAdapterService.getSystemService(Context.MEDIA_SESSION_SERVICE))
                .thenReturn(mMediaSessionManager);
        when(mMockAdapterService.getSystemServiceName(MediaSessionManager.class))
                .thenReturn(Context.MEDIA_SESSION_SERVICE);

        when(mMockAdapterService.getMainExecutor()).thenReturn(mLooper.getNewExecutor());

        when(mMockAdapterService.getApplicationContext()).thenReturn(mMockAdapterService);
        when(mMockAdapterService.getSystemService(Context.USER_SERVICE))
                .thenReturn(mMockUserManager);
        when(mMockAdapterService.getSystemServiceName(UserManager.class))
                .thenReturn(Context.USER_SERVICE);
        when(mMockAdapterService.getResources()).thenReturn(mMockResources);

        when(mMockSharedPreferences.edit()).thenReturn(mMockSharedPreferencesEditor);
        when(mMockAdapterService.getSharedPreferences(anyString(), anyInt()))
                .thenReturn(mMockSharedPreferences);
    }

    @After
    public void tearDown() throws Exception {
        mLooper.stopAutoDispatchAndIgnoreExceptions();
    }

    @Test
    public void testQueueUpdateData() {
        List<Metadata> firstQueue = new ArrayList<Metadata>();
        List<Metadata> secondQueue = new ArrayList<Metadata>();

        firstQueue.add(createEmptyMetadata());
        secondQueue.add(createEmptyMetadata());
        assertThat(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue)).isFalse();

        secondQueue.add(createEmptyMetadata());
        assertThat(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue)).isTrue();

        firstQueue.add(createEmptyMetadata());
        firstQueue.get(1).album = TEST_DATA;
        firstQueue.get(1).genre = TEST_DATA;
        firstQueue.get(1).mediaId = TEST_DATA;
        firstQueue.get(1).trackNum = TEST_DATA;
        firstQueue.get(1).numTracks = TEST_DATA;
        firstQueue.get(1).duration = TEST_DATA;
        firstQueue.get(1).image =
                new Image(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(), Uri.EMPTY);
        assertThat(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue)).isFalse();

        secondQueue.get(1).title = TEST_DATA;
        assertThat(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue)).isTrue();

        secondQueue.set(1, createEmptyMetadata());
        secondQueue.get(1).artist = TEST_DATA;
        assertThat(AvrcpTargetService.isQueueUpdated(firstQueue, secondQueue)).isTrue();
    }

    private Metadata createEmptyMetadata() {
        Metadata.Builder builder = new Metadata.Builder();
        return builder.useDefaults().build();
    }

    @Test
    public void testServiceInstance() {
        AvrcpVolumeManager volumeManager =
                new AvrcpVolumeManager(
                        mMockAdapterService, mMockAudioManager, mMockNativeInterface);
        AvrcpTargetService service =
                new AvrcpTargetService(
                        mMockAdapterService,
                        mMockNativeInterface,
                        mMockAudioManager,
                        volumeManager,
                        mLooper.getLooper());

        service.start();
        verify(mMockAudioManager)
                .registerAudioDeviceCallback(mAudioDeviceCb.capture(), anyObject());

        service.stop();
        service.cleanup();
        verify(mMockAudioManager).unregisterAudioDeviceCallback(mAudioDeviceCb.getValue());
    }
}
