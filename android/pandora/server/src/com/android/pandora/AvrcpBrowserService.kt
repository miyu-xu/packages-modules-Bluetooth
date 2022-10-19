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
  private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaItem>>()
  private var metadataItems = mutableMapOf<String, MediaMetadata>()
  private var queue = mutableListOf<MediaSession.QueueItem>()
  private var currentTrack = -1

  override fun onCreate() {
    super.onCreate()
    setupMediaSession()
    initBrowseFolderList()
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
    mediaSession.setQueue(queue)
    mediaSession.setQueueTitle(NOW_PLAYING_PREFIX)
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

  fun play() {
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_PLAYING))
        .build()
    mediaSession.setPlaybackState(playbackState)
    if (currentTrack == -1) {
      currentTrack = 1
      initQueue()
      mediaSession.setQueue(queue)
      mediaSession.setMetadata(metadataItems.get("" + currentTrack))
    }
  }

  fun stop() {
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_STOPPED, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_STOPPED))
        .build()
    mediaSession.setPlaybackState(playbackState)
    mediaSession.setMetadata(null)
  }

  fun pause() {
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_PAUSED, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_PAUSED))
        .build()
    mediaSession.setPlaybackState(playbackState)
  }

  fun rewind() {
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_REWINDING, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_REWINDING))
        .build()
    mediaSession.setPlaybackState(playbackState)
  }

  fun fastForward() {
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_FAST_FORWARDING, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_FAST_FORWARDING))
        .build()
    mediaSession.setPlaybackState(playbackState)
  }

  fun forward() {
    if (currentTrack == 6 || currentTrack == -1) currentTrack = 1 else currentTrack += 1
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_SKIPPING_TO_NEXT, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_SKIPPING_TO_NEXT))
        .build()
    mediaSession.setPlaybackState(playbackState)
    mediaSession.setMetadata(metadataItems.get("" + currentTrack))
  }

  fun backward() {
    if (currentTrack == 1 || currentTrack == -1) currentTrack = 6 else currentTrack -= 1
    playbackState =
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_SKIPPING_TO_PREVIOUS, 0, 1.0f)
        .setActions(getAvailableActions(PlaybackState.STATE_SKIPPING_TO_PREVIOUS))
        .build()
    mediaSession.setPlaybackState(playbackState)
    mediaSession.setMetadata(metadataItems.get("" + currentTrack))
  }

  fun setLargeMetadata() {
    mediaSession.setMetadata(
      MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "MEDIA_ID")
        .putString(MediaMetadata.METADATA_KEY_TITLE, generateAlphanumericString(512))
        .putString(MediaMetadata.METADATA_KEY_ARTIST, generateAlphanumericString(512))
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
    if (parentId == ROOT) {
      val map = mediaIdToChildren[ROOT]
      Log.i(TAG, "onloadchildren $map")
      result.sendResult(map)
    } else if (parentId == NOW_PLAYING_PREFIX) {
      result.sendResult(mediaIdToChildren[NOW_PLAYING_PREFIX])
    } else {
      Log.i(TAG, "onloadchildren inside else")
      result.sendResult(null)
    }
  }

  fun initMediaItems() {
    var mediaItems = mutableListOf<MediaItem>()
    for (item in 1..6) {
      val metaData: MediaMetadata =
        MediaMetadata.Builder()
          .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, NOW_PLAYING_PREFIX + item)
          .putString(MediaMetadata.METADATA_KEY_TITLE, "Title$item")
          .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist$item")
          .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, item.toLong())
          .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, 6.toLong())
          .build()
      val mediaItem = MediaItem(metaData.description, MediaItem.FLAG_PLAYABLE)
      mediaItems.add(mediaItem)
      metadataItems.put("" + item, metaData)
    }
    mediaIdToChildren[NOW_PLAYING_PREFIX] = mediaItems
  }

  fun initQueue() {
    for ((key, value) in metadataItems.entries) {
      val mediaItem = MediaItem(value.description, MediaItem.FLAG_PLAYABLE)
      queue.add(MediaSession.QueueItem(mediaItem.description, key.toLong()))
    }
  }

  fun initBrowseFolderList() {
    var rootList = mediaIdToChildren[ROOT] ?: mutableListOf()

    val emptyFolderMetaData =
      MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, EMPTY_FOLDER)
        .putString(MediaMetadata.METADATA_KEY_TITLE, EMPTY_FOLDER)
        .putLong(
          MediaMetadata.METADATA_KEY_BT_FOLDER_TYPE,
          MediaDescription.BT_FOLDER_TYPE_PLAYLISTS
        )
        .build()
    val emptyFolderMediaItem = MediaItem(emptyFolderMetaData.description, MediaItem.FLAG_BROWSABLE)

    val playlistMetaData =
      MediaMetadata.Builder()
        .apply {
          putString(MediaMetadata.METADATA_KEY_MEDIA_ID, NOW_PLAYING_PREFIX)
          putString(MediaMetadata.METADATA_KEY_TITLE, NOW_PLAYING_PREFIX)
          putLong(
            MediaMetadata.METADATA_KEY_BT_FOLDER_TYPE,
            MediaDescription.BT_FOLDER_TYPE_PLAYLISTS
          )
        }
        .build()

    val playlistsMediaItem = MediaItem(playlistMetaData.description, MediaItem.FLAG_BROWSABLE)

    rootList += emptyFolderMediaItem
    rootList += playlistsMediaItem
    mediaIdToChildren[ROOT] = rootList
    initMediaItems()
  }

  companion object {
    lateinit var instance: AvrcpBrowserService
    const val ROOT = "__ROOT__"
    const val EMPTY_FOLDER = "@empty@"
    const val NOW_PLAYING_PREFIX = "NowPlayingId"
  }
}