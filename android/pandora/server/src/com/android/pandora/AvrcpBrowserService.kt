/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.pandora

import android.content.Intent
import android.media.*
import android.media.browse.MediaBrowser.MediaItem
import android.media.session.*
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.service.media.MediaBrowserService.BrowserRoot
import android.util.Log

/* MediaBrowserService to handle MediaButton and Browsing */
class AvrcpBrowserService : MediaBrowserService() {
  private val TAG = "PandoraAvrcpBrowserService"

  private lateinit var mediaSession: MediaSession
  private lateinit var playbackState: PlaybackState
  private val alphanumeric = ('A'..'Z') + ('a'..'z') + ('0'..'9')

  override fun onCreate() {
    super.onCreate()
    setupMediaSession()
    instance = this
  }

  fun deinit() {
    // Releasing MediaSession instance
    mediaSession.release()
  }

  fun setupMediaSession() {
    mediaSession = MediaSession(this, "MediaSession")

    mediaSession.setFlags(
      MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
    )
    mediaSession.setCallback(mSessionCallback)
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_NONE, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_NONE))
        .build()
    mediaSession.setPlaybackState(playbackState)
    mediaSession.setMetadata(null)
    mediaSession.isActive = true
    sessionToken = mediaSession.sessionToken
  }

  private fun getAvailableActions(state: Int): Long {
    var actions: Long =
      (PlaybackState.ACTION_SKIP_TO_PREVIOUS or
        PlaybackState.ACTION_SKIP_TO_NEXT or
        PlaybackState.ACTION_REWIND or
        PlaybackState.ACTION_FAST_FORWARD)

    actions =
      if (state == PlaybackState.STATE_PLAYING) {
        actions or PlaybackState.ACTION_PAUSE
      } else {
        actions or PlaybackState.ACTION_PLAY
      }
    return actions
  }

  fun setPlaybackState(state: Int) {
    playbackState =
      PlaybackState.Builder()
        .setState(state, 0, 1.0f)
        .setActions(getAvailableActions(state))
        .build()
    mediaSession.setPlaybackState(playbackState)
    // TODO: Need to check for which states metadata is required to set
    mediaSession.setMetadata(
      MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "MEDIA_ID")
        .putString(MediaMetadata.METADATA_KEY_TITLE, "MEDIA_TITLE")
        .putString(MediaMetadata.METADATA_KEY_ARTIST, "ARTIST")
        .build()
    )
  }

  fun generateAlphanumericString(length: Int): String {
    // The buildString function will create a StringBuilder
    return buildString {
      // We will repeat length times and will append a random character each time
      repeat(length) { append(alphanumeric.random()) }
    }
  }

  fun setLargeMetadata() {
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_PLAYING))
        .build()
    mediaSession.setPlaybackState(playbackState)
    mediaSession.setMetadata(
      MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "MEDIA_ID")
        .putString(MediaMetadata.METADATA_KEY_TITLE, generateAlphanumericString(512))
        .putString(MediaMetadata.METADATA_KEY_ARTIST, generateAlphanumericString(512))
        .build()
    )
  }

  fun getPlaybackState(): PlaybackState {
    return playbackState
  }

  private fun handlePlay() {
    if (playbackState.state == PlaybackState.STATE_PAUSED) {
      playbackState =
        PlaybackState.Builder()
          .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
          .setActions(getAvailableActions(PlaybackState.STATE_PLAYING))
          .build()
      mediaSession.setPlaybackState(playbackState)
    }
  }

  private fun handlePause() {
    if (playbackState.state == PlaybackState.STATE_PLAYING) {
      playbackState =
        PlaybackState.Builder()
          .setState(PlaybackState.STATE_PAUSED, 0, 1.0f)
          .setActions(getAvailableActions(PlaybackState.STATE_PAUSED))
          .build()
      mediaSession.setPlaybackState(playbackState)
    }
  }

  private val mSessionCallback: MediaSession.Callback =
    object : MediaSession.Callback() {
      override fun onPlay() {
        Log.i(TAG, "onPlay")
        handlePlay()
      }

      override fun onPause() {
        Log.i(TAG, "onPause")
        handlePause()
      }

      override fun onSkipToPrevious() {
        Log.i(TAG, "onSkipToPrevious")
        // TODO : Need to handle to play previous audio in the list
      }

      override fun onSkipToNext() {
        Log.i(TAG, "onSkipToNext")
        // TODO : Need to handle to play next audio in the list
      }

      override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
        Log.i(TAG, "MediaSessionCallback——》onMediaButtonEvent $mediaButtonEvent")
        return super.onMediaButtonEvent(mediaButtonEvent)
      }
    }

  override fun onGetRoot(p0: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
    Log.i(TAG, "onGetRoot")
    return BrowserRoot(ROOT, null)
  }

  override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
    Log.i(TAG, "onLoadChildren")
    result.sendResult(getMediaItems())
  }

  fun getMediaItems(): MutableList<MediaItem> {
    val mediaItems = mutableListOf<MediaItem>()
    for (item in 1..5) {
      val metaData: MediaMetadata =
        MediaMetadata.Builder()
          .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "Media ID $item")
          .putString(MediaMetadata.METADATA_KEY_TITLE, "Media Title $item")
          .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist $item")
          .build()
      mediaItems.add(MediaItem(metaData.description, MediaItem.FLAG_PLAYABLE))
    }
    return mediaItems
  }

  companion object {
    lateinit var instance: AvrcpBrowserService
    const val ROOT = "__ROOT__"
  }
}