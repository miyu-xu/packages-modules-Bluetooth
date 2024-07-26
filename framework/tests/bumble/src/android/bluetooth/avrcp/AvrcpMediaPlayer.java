/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.bluetooth;

import android.content.Intent;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaSession;
import android.media.session.MediaSession.QueueItem;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AvrcpMediaPlayer extends MediaBrowserService {

    private static final String TAG = "Pandora AvrcpMediaPlayer";

    private static final String DEFAULT_TITLE = "Title";
    private static final String DEFAULT_ARTIST = "Artist";
    private static final String DEFAULT_ALBUM = "Album";
    private static final long DEFAULT_NUM_TRACKS = 5;
    private static final long DEFAULT_TRACK_NUM = 5;
    private static final String DEFAULT_GENRE = "Genre";
    private static final long DEFAULT_DURATION = 1000;
    private static final String ROOT = "__ROOT__";

    long mAvailableActions =
            PlaybackState.ACTION_PLAY
                    | PlaybackState.ACTION_STOP
                    | PlaybackState.ACTION_PAUSE
                    | PlaybackState.ACTION_REWIND
                    | PlaybackState.ACTION_FAST_FORWARD
                    | PlaybackState.ACTION_SKIP_TO_NEXT
                    | PlaybackState.ACTION_SKIP_TO_PREVIOUS;

    private boolean mHadOnNext = false;

    private static MediaSession sMediaSession;
    private PlaybackState.Builder mPlaybackStateBuilder;

    private HashMap<String, MediaMetadata> mMedias;
    private ArrayList<QueueItem> mQueue;
    private static long sIdCount = 0;
    private int mQueuePlayingIndex = 0;

    private MediaPlayer mMediaPlayer;

    private static AvrcpMediaPlayer sInstance;

    public interface EventsReceivedCallback {
        /** Triggered whenever a key event is received by the player */
        void onEventreceived(long event);
    }

    private EventsReceivedCallback mEventsReceivedCallback;

    private MediaSession.Callback mSessionCallback =
            new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    Log.i(TAG, "MediaSessionCallback: onPlay");
                    if (mEventsReceivedCallback != null) {
                        mEventsReceivedCallback.onEventreceived(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    }
                    play();
                }

                @Override
                public void onPause() {
                    Log.i(TAG, "MediaSessionCallback: onPause");
                    if (mEventsReceivedCallback != null) {
                        mEventsReceivedCallback.onEventreceived(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    }
                    pause();
                }

                @Override
                public void onSkipToPrevious() {
                    Log.i(TAG, "MediaSessionCallback: onSkipToPrevious");
                    if (mEventsReceivedCallback != null) {
                        mEventsReceivedCallback.onEventreceived(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    }
                    if (mQueuePlayingIndex > 0) {
                        mQueuePlayingIndex -= 1;
                    }
                    play();
                }

                @Override
                public void onSkipToNext() {
                    Log.e(TAG, "MediaSessionCallback: onSkipToNext");
                    if (mEventsReceivedCallback != null) {
                        mEventsReceivedCallback.onEventreceived(KeyEvent.KEYCODE_MEDIA_NEXT);
                    }
                    if (mQueuePlayingIndex < mQueue.size() - 1) {
                        mQueuePlayingIndex += 1;
                    }
                    play();
                }

                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                    Log.i(TAG, "MediaSessionCallback: onMediaButtonEvent " + mediaButtonEvent);
                    return super.onMediaButtonEvent(mediaButtonEvent);
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        mMedias = new HashMap<String, MediaMetadata>();
        mQueue = new ArrayList<QueueItem>();

        sMediaSession = new MediaSession(this, "AvrcpMediaPlayer");

        sMediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mPlaybackStateBuilder =
                new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_NONE, 0, 1.0f)
                        .setActions(mAvailableActions)
                        .setActiveQueueItemId(mQueuePlayingIndex);

        sMediaSession.setPlaybackState(mPlaybackStateBuilder.build());

        sMediaSession.setCallback(mSessionCallback);

        sMediaSession.setActive(true);

        setSessionToken(sMediaSession.getSessionToken());

        sInstance = this;
    }

    @Override
    public void onDestroy() {
        sMediaSession.release();
        super.onDestroy();
    }

    public static boolean isInitialized() {
        return (sInstance != null);
    }

    public static AvrcpMediaPlayer getInstance() {
        return sInstance;
    }

    @Override
    public BrowserRoot onGetRoot(String p0, int clientUid, Bundle rootHints) {
        Log.i(TAG, "onGetRoot");
        return new BrowserRoot(ROOT, null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        Log.i(TAG, "onLoadChildren");
        result.sendResult(null);
    }

    // Player controls

    private void setPlaybackState(int state) {
        mPlaybackStateBuilder.setState(state, 0, 1.0f).setActiveQueueItemId(mQueuePlayingIndex);
        sMediaSession.setPlaybackState(mPlaybackStateBuilder.build());
    }

    /** Plays current song in queue */
    public void play() {
        setPlaybackState(PlaybackState.STATE_PLAYING);
        sMediaSession.setMetadata(
                mMedias.get(mQueue.get(mQueuePlayingIndex).getDescription().getMediaId()));
        startTestPlayback();
    }

    /** Pauses current song in queue */
    public void pause() {
        setPlaybackState(PlaybackState.STATE_PAUSED);
        stopTestPlayback();
    }

    // Queue methods

    /** Creates a queue from list of MediaMetadata */
    public void createQueue(ArrayList<MediaMetadata> items) {
        mQueue.clear();
        mMedias.clear();
        for (MediaMetadata item : items) {
            mMedias.put(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID), item);
            mQueue.add(
                    new QueueItem(
                            (new MediaItem(item.getDescription(), MediaItem.FLAG_PLAYABLE))
                                    .getDescription(),
                            mQueue.size()));
        }
        sMediaSession.setQueue(mQueue);
    }

    /** Adds item to end of queue */
    public void addQueueItem(MediaMetadata metadata) {
        mMedias.put(metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID), metadata);
        mQueue.add(
                new QueueItem(
                        (new MediaItem(metadata.getDescription(), MediaItem.FLAG_PLAYABLE))
                                .getDescription(),
                        mQueue.size()));
        sMediaSession.setQueue(mQueue);
    }

    /** Edits item at position in queue */
    public boolean setQueueElementAt(int position, MediaMetadata metadata) {
        if (mQueue.size() <= position) {
            return false;
        }
        mMedias.put(metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID), metadata);
        long id = ((QueueItem) mQueue.get(position)).getQueueId();
        mQueue.set(
                position,
                new QueueItem(
                        (new MediaItem(metadata.getDescription(), MediaItem.FLAG_PLAYABLE))
                                .getDescription(),
                        id));
        sMediaSession.setQueue(mQueue);
        return true;
    }

    private static long getNextAvailableId() {
        sIdCount += 1;
        return sIdCount;
    }

    /** Creates default MediaMetadata */
    public static MediaMetadata createDefaultMediaMetadata() {
        return new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, Long.toString(getNextAvailableId()))
                .putString(MediaMetadata.METADATA_KEY_TITLE, DEFAULT_TITLE)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, DEFAULT_ARTIST)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, DEFAULT_ALBUM)
                .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, DEFAULT_NUM_TRACKS)
                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, DEFAULT_TRACK_NUM)
                .putString(MediaMetadata.METADATA_KEY_GENRE, DEFAULT_GENRE)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, DEFAULT_DURATION)
                .build();
    }

    /** Edits a String value of a MediaMetadata */
    public static MediaMetadata editStringMediaMetadataValue(
            MediaMetadata original, String key, String value) {
        return new MediaMetadata.Builder(original).putString(key, value).build();
    }

    /** Edits a Long value of a MediaMetadata */
    public static MediaMetadata editLongMediaMetadataValue(
            MediaMetadata original, String key, long value) {
        return new MediaMetadata.Builder(original).putLong(key, value).build();
    }

    // Audio Utils

    private void startTestPlayback() {
        if (mMediaPlayer == null) {
            // File copied from: development/samples/ApiDemos/res/raw/test_cbr.mp3
            // to: packages/modules/Bluetooth/android/pandora/server/res/raw/test_cbr.mp3
            int resourceId = getResources().getIdentifier("test_cbr", "raw", getPackageName());
            mMediaPlayer = MediaPlayer.create(this, resourceId);
            if (mMediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer.");
                return;
            }
        }
        mMediaPlayer.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        stopTestPlayback();
                    }
                });
        try {
            mMediaPlayer.prepare();
        } catch (Exception e) {
        }
        mMediaPlayer.start();
    }

    private void stopTestPlayback() {
        mMediaPlayer.stop();
        mMediaPlayer.setOnCompletionListener(null);
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    // Test Utils

    /** Registers the callback for key events litening */
    public void checkEventReceived(EventsReceivedCallback cb) {
        mEventsReceivedCallback = cb;
    }
}
