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
package com.android.bluetooth.avrcpcontroller;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Looper;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.MediaBrowserServiceCompat.Result;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BluetoothMediaBrowserServiceTest {

    private final boolean mUseRule = true;

    // For mocking
    public static class PlaybackControls extends MediaSessionCompat.Callback {}

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    // Mock out AVRCP Controller Service, who we receive data from and send requests back to
    @Mock private AvrcpControllerService mAvrcpControllerService;
    @Mock private PlaybackControls mPlaybackControls;
    @Mock private PlaybackControls mPlaybackControlsOther;

    // Effectively the public facing API surface used by media clients
    private static final String TEST_PACKAGE = "com.android.bluetooth.tests";
    private static final int TEST_CLIENT_UID = 1234;
    MediaSessionCompat mSession;
    MediaControllerCompat mController;
    @Mock Result<List<MediaItem>> mResults;
    @Mock MediaSessionCompat mMockMediaSessionCompat;

    // Service under test
    @Rule public final ServiceTestRule mMediaBrowserServiceTestRule = new ServiceTestRule();
    private Context mTargetContext;
    private BluetoothMediaBrowserService mService;

    @Before
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        assertThat(Looper.myLooper()).isNotNull();
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AvrcpControllerService.setAvrcpControllerService(mAvrcpControllerService);
    }

    @After
    public void tearDown() throws Exception {
        if (!mUseRule) {
            if (mService != null) {
                mService.onDestroy();
            }
        }
        AvrcpControllerService.setAvrcpControllerService(null);
        mService = null;
    }

    private void startMediaBrowserService() {
        if (mUseRule) {
            Log.i(TEST_PACKAGE, "Starting service using rule");
            startMediaBrowserServiceWithRule();
        } else {
            Log.i(TEST_PACKAGE, "Starting service manually");
            startMediaBrowserServiceManually();
        }
    }

    private void startMediaBrowserServiceWithRule() {
        final Intent intent = new Intent(mTargetContext, BluetoothMediaBrowserService.class);
        intent.setAction(MediaBrowserService.SERVICE_INTERFACE);

        try {
            mMediaBrowserServiceTestRule.startService(intent);
        } catch (Exception e) {
            // Any failures to start the service will lead to the instance being null
            // and the test failing below, so nothing to do here.
        }

        mService = BluetoothMediaBrowserService.getInstance();
        assertThat(mService).isNotNull();

        BluetoothMediaBrowserService.reset();

        mSession = mService.getSession();
        assertThat(mSession).isNotNull();

        mController = mSession.getController();
        assertThat(mController).isNotNull();
    }

    private void startMediaBrowserServiceManually() {
        // Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context applicationContext = ApplicationProvider.getApplicationContext();

        // Context spyTargetContext = spy(new ContextWrapper(targetContext));
        Context spyApplicationContext = spy(new ContextWrapper(applicationContext));

        mService = new BluetoothMediaBrowserService(spyApplicationContext, mMockMediaSessionCompat);
        mService.onCreate();

        assertThat(BluetoothMediaBrowserService.getInstance()).isEqualTo(mService);

        BluetoothMediaBrowserService.reset();

        mSession = mService.getSession();
        assertThat(mSession).isNotNull();

        mController = mSession.getController();
        assertThat(mController).isNotNull();
    }

    @Test
    public void testGetSession() {
        startMediaBrowserService();
        // The above function starts the service, gets a session and asserts its not null
    }

    @Test
    public void testGetSession_serviceNotStarted() {
        // Service not started
        mService = BluetoothMediaBrowserService.getInstance();
        assertThat(mService).isNull();
    }

    @Test
    public void testSetActive() {
        startMediaBrowserService();
        BluetoothMediaBrowserService.setActive(true);
        assertThat(BluetoothMediaBrowserService.isActive()).isTrue();
        assertThat(mSession.isActive()).isTrue();

        BluetoothMediaBrowserService.setActive(false);
        assertThat(BluetoothMediaBrowserService.isActive()).isFalse();
        assertThat(mSession.isActive()).isFalse();
    }

    @Test
    public void testSetActive_serviceNotStarted() {
        // Service not started
        BluetoothMediaBrowserService.setActive(true);
        assertThat(BluetoothMediaBrowserService.isActive()).isFalse();
        BluetoothMediaBrowserService.setActive(false);
        assertThat(BluetoothMediaBrowserService.isActive()).isFalse();
    }

    @Test
    public void testPlaybackControls() {
        startMediaBrowserService();
        BluetoothMediaBrowserService.onAddressedPlayerChanged(mPlaybackControls);

        mController.getTransportControls().play();
        // verify(mPlaybackControls, timeout(2000 /* ms */)).onPlay();

        mController.getTransportControls().pause();
        // verify(mPlaybackControls, timeout(2000 /* ms */)).onPause();

        mController.getTransportControls().skipToNext();
        // verify(mPlaybackControls, timeout(2000 /* ms */)).onSkipToNext();

        mController.getTransportControls().skipToPrevious();
        // verify(mPlaybackControls, timeout(2000 /* ms */)).onSkipToPrevious();
    }

    @Test
    public void testOnAddressedPlayerChanged() {
        startMediaBrowserService();
        BluetoothMediaBrowserService.onAddressedPlayerChanged(mPlaybackControls);

        mController.getTransportControls().play();
        // verify(mPlaybackControls).onPlay();

        BluetoothMediaBrowserService.onAddressedPlayerChanged(mPlaybackControlsOther);

        mController.getTransportControls().play();
        // verify(mPlaybackControlsOther).onPlay();

        // verifyNoMoreInteractions(mPlaybackControls);
    }

    @Test
    public void testOnAddressedPlayerChanged_serviceNotStarted() {
        // Service not started
        BluetoothMediaBrowserService.onAddressedPlayerChanged(mPlaybackControls);
        // change return type -> returns false? How to verify?
    }

    @Test
    public void testOnAudioFocusStateChanged_focusGained() {
        startMediaBrowserService();
        BluetoothMediaBrowserService.onAudioFocusStateChanged(AudioManager.AUDIOFOCUS_GAIN);
        // CONNECTING -> PREVIOUS PLAYBACK STATE
    }

    @Test
    public void testOnAudioFocusStateChanged_focusLost() {
        startMediaBrowserService();
        BluetoothMediaBrowserService.onAudioFocusStateChanged(AudioManager.AUDIOFOCUS_LOSS);
        // NO PLAYBACK STATE CHANGES
    }

    @Test
    public void testOnAudioFocusStateChanged_serviceNotStarted() {
        // Service not started
        BluetoothMediaBrowserService.onAudioFocusStateChanged(AudioManager.AUDIOFOCUS_GAIN);
        // change return type -> returns false? How to verify?
    }

    @Test
    public void testOnTrackChanged() {
        startMediaBrowserService();
        AvrcpItem track =
                new AvrcpItem.Builder()
                        .setTitle("test-title")
                        .setArtistName("test-artist")
                        .setAlbumName("test-album")
                        .setPlayingTime(1234)
                        .setTrackNumber(1)
                        .setTotalNumberOfTracks(12)
                        .setGenre("test-genre")
                        .build();

        BluetoothMediaBrowserService.onTrackChanged(track);

        MediaMetadataCompat metadata = mController.getMetadata();
        assertThat(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .isEqualTo(track.getTitle());
        assertThat(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .isEqualTo(track.getArtistName());
        assertThat(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .isEqualTo(track.getAlbumName());
        assertThat(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
                .isEqualTo(track.getPlayingTime());
        assertThat(metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
                .isEqualTo(track.getTrackNumber());
        assertThat(metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS))
                .isEqualTo(track.getTotalNumberOfTracks());
        assertThat(metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE))
                .isEqualTo(track.getGenre());
    }

    @Test
    public void testOnTrackChanged_serviceNotStarted() {
        // Service not started
        AvrcpItem track =
                new AvrcpItem.Builder()
                        .setTitle("test-title")
                        .setArtistName("test-artist")
                        .setAlbumName("test-album")
                        .setPlayingTime(1234)
                        .setTrackNumber(1)
                        .setTotalNumberOfTracks(12)
                        .setGenre("test-genre")
                        .build();

        BluetoothMediaBrowserService.onTrackChanged(track);
        // change return type -> returns false?
    }

    @Test
    public void testOnPlaybackStateChanged() {
        startMediaBrowserService();
        PlaybackStateCompat expectedPlaybackState =
                new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .setActions(
                                PlaybackStateCompat.ACTION_PREPARE
                                        | PlaybackStateCompat.ACTION_PLAY)
                        .setActiveQueueItemId(0)
                        .build();
        BluetoothMediaBrowserService.onPlaybackStateChanged(expectedPlaybackState);

        PlaybackStateCompat playbackState = mController.getPlaybackState();
        assertThat(playbackState.getState()).isEqualTo(expectedPlaybackState.getState());
        assertThat(playbackState.getActiveQueueItemId())
                .isEqualTo(expectedPlaybackState.getActiveQueueItemId());
    }

    @Test
    public void testOnPlaybackStateChanged_serviceNotStarted() {
        // Service not started
        PlaybackStateCompat expectedPlaybackState =
                new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .setActions(
                                PlaybackStateCompat.ACTION_PREPARE
                                        | PlaybackStateCompat.ACTION_PLAY)
                        .setActiveQueueItemId(0)
                        .build();
        BluetoothMediaBrowserService.onPlaybackStateChanged(expectedPlaybackState);
        // change return type -> returns false?
    }

    @Test
    public void testOnNowPlayingQueueChanged() {
        startMediaBrowserService();
        // Send new now playing list
        // check session for now playing list
    }

    @Test
    public void testOnNowPlayingQueueChanged_serviceNotStarted() {}

    @Test
    public void testOnBrowseNodeChanged() {
        startMediaBrowserService();
        // Send new browse node
        // check to see if browse node was updated
    }

    @Test
    public void testOnBrowseNodeChanged_serviceNotStarted() {
        // Service not started
        // Send browse node changed
        // nothing should happen
    }

    @Test
    public void testOnGetRoot() {
        startMediaBrowserService();
        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();
    }

    @Test
    public void testOnLoadChildren_resultsAvailableNow() {
        startMediaBrowserService();

        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();

        List<MediaItem> results = new ArrayList<MediaItem>();
        setBrowseResult(
                root.getRootId(), BluetoothMediaBrowserService.BrowseResult.SUCCESS, results);
        mService.onLoadChildren(root.getRootId(), mResults);

        verify(mAvrcpControllerService).getContents(eq(root.getRootId()));
        verify(mResults).sendResult(eq(results));
    }

    @Test
    public void testOnLoadChildren_resultToBeDownloaded() {
        startMediaBrowserService();

        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();

        setBrowseResult(
                root.getRootId(), BluetoothMediaBrowserService.BrowseResult.DOWNLOAD_PENDING, null);
        mService.onLoadChildren(root.getRootId(), mResults);

        verify(mAvrcpControllerService).getContents(eq(root.getRootId()));
        verify(mResults).detach();
    }

    @Test
    public void testOnLoadChildren_resultToBeDownloadedButSomeAvailable() {
        startMediaBrowserService();

        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();

        List<MediaItem> results = new ArrayList<MediaItem>();
        setBrowseResult(
                root.getRootId(),
                BluetoothMediaBrowserService.BrowseResult.DOWNLOAD_PENDING,
                results);
        mService.onLoadChildren(root.getRootId(), mResults);

        verify(mAvrcpControllerService).getContents(eq(root.getRootId()));
        verify(mResults).sendResult(eq(results));
    }

    @Test
    public void testOnLoadChildren_resultsDoNotExistForMediaId() {
        startMediaBrowserService();

        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();

        List<MediaItem> results = new ArrayList<MediaItem>();
        setBrowseResult(
                root.getRootId(),
                BluetoothMediaBrowserService.BrowseResult.ERROR_MEDIA_ID_INVALID,
                results);
        mService.onLoadChildren(root.getRootId(), mResults);

        verify(mAvrcpControllerService).getContents(eq(root.getRootId()));
        verify(mResults).sendResult(eq(results));
    }

    @Test
    public void testOnLoadChildren_noDeviceConnected() {
        startMediaBrowserService();

        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();

        setBrowseResult(
                root.getRootId(),
                BluetoothMediaBrowserService.BrowseResult.NO_DEVICE_CONNECTED,
                null);
        mService.onLoadChildren(root.getRootId(), mResults);

        verify(mAvrcpControllerService).getContents(eq(root.getRootId()));
        verify(mResults).sendResult(eq(null));
    }

    @Test
    public void testOnLoadChildren_serviceUnavailable() {
        AvrcpControllerService.setAvrcpControllerService(null);
        startMediaBrowserService();
        BrowserRoot root = mService.onGetRoot(TEST_PACKAGE, TEST_CLIENT_UID, null);
        assertThat(root).isNotNull();
        assertThat(root.getRootId()).isNotNull();

        mService.onLoadChildren(root.getRootId(), mResults);

        verify(mAvrcpControllerService, times(0)).getContents(anyString());
        verify(mResults).sendResult(eq(null));
    }

    @Test
    public void testDump_isNotNull() {
        startMediaBrowserService();
        assertThat(BluetoothMediaBrowserService.dump()).isNotNull();
    }

    @Test
    public void testDump_serviceNotStarted_isNotNull() {
        assertThat(BluetoothMediaBrowserService.dump()).isNotNull();
    }

    private void setBrowseResult(String mediaId, byte status, List<MediaItem> results) {
        BluetoothMediaBrowserService.BrowseResult result =
                new BluetoothMediaBrowserService.BrowseResult(results, status);
        // when(mAvrcpControllerService.getContents(eq(mediaId))).thenReturn(result);
        doReturn(result).when(mAvrcpControllerService).getContents(eq(mediaId));
    }
}
