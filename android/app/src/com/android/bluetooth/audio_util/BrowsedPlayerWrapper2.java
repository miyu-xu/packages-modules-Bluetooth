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

package com.android.bluetooth.audio_util;

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser.MediaItem;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


// Binds to a MediaBrowser, then retrieves the content.

// Connect to player, wait for Media cb, execute event, wait for Media cb, execute AVRCP cb

class BrowsedPlayerWrapper2 {

    private static final String TAG = "BrowsedPlayerWrapper";

    /**
     * Some devices will continuously request each item in a folder one at a time.
     *
     * <p>This timeout is here to remove the binding between this class and the {@link MediaBrowser}
     * after a certain time without requests from the remote device to browse the player. If
     * the next request happens soon after, the bound will still exist.
     *
     * <p>Note: Previous implementation was keeping a local list of fetched items, this worked at the
     * cost of not having the items actualized if fetching again the same folder.
     */
    private static final int BROWSER_DISCONNECT_TIMEOUT_MS = 5000;


    enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    public interface RequestCallback {
        void run();
    }

    public interface GetPlayerRootCallback {
        void run(String rootId);
    }

    public interface GetFolderItemsCallback {
        void run(String parentId, List<ListItem> items);
    }

    private final MediaBrowser mWrappedBrowser;
    private final Context mContext;
    private final Looper mLooper;
    private final String mPackageName;
    private final Handler mDisconnectHandler;

    private ConnectionState mBrowserConnectionState = ConnectionState.DISCONNECTED;

    private final ArrayList<RequestCallback> mRequestsList = new ArrayList<>();

    // GetFolderItems also works with a callback, so we need to store all requests made before we
    // got the results and prevent new subscribtions.
    private final HashMap<String, ArrayList<GetFolderItemsCallback>> mSubscribedIds = new HashMap<>();

    private final Runnable mDisconnectRunnable = new Runnable() {
        @Override
        public void run() {
            disconnect();
        }
    };


    public BrowsedPlayerWrapper2(Context context,
                                Looper looper,
                                String packageName,
                                String className) {
        mContext = context;
        mPackageName = packageName;
        mLooper = looper;
        mDisconnectHandler = new Handler(mLooper);
        mWrappedBrowser = MediaBrowserFactory.make(
                context,
                new ComponentName(packageName, className),
                new MediaConnectionCallback(),
                null);
    }


    public final String getPackageName() {
        return mPackageName;
    }

    public void getRootId(GetPlayerRootCallback callback) {
        // Callback is call is synchronous
        browseRequest(() -> {
            if (mBrowserConnectionState != ConnectionState.CONNECTED) {
                Log.e(TAG, "getRootId: Callback triggered before binding done.");
                // TODO: handle connection fail
            }
            setDisconnectDelay();
            callback.run(mWrappedBrowser.getRoot());
        });
    }

    public void playItem(String mediaId) {
        // Callback is call is synchronous
        browseRequest(() -> {
            if (mBrowserConnectionState != ConnectionState.CONNECTED) {
                Log.e(TAG, "playItem: Callback triggered before binding done.");
                // TODO: handle connection fail
            }
            setDisconnectDelay();
            // Retrieve the MediaController linked with this MediaBrowser.
            // Note that the MediaBrowser should be bound for this.
            MediaController controller = MediaControllerFactory.make(mContext,
                    mWrappedBrowser.getSessionToken());
            // Retrieve TransportControls from this MediaController and play mediaId
            MediaController.TransportControls ctrl = controller.getTransportControls();
            ctrl.playFromMediaId(mediaId, null);

        });
    }

    public void getFolderItems(String mediaId, GetFolderItemsCallback callback) {
        // Callback is call is synchronous
        browseRequest(() -> {
            if (mBrowserConnectionState != ConnectionState.CONNECTED) {
                Log.e(TAG, "playItem: Callback triggered before binding done.");
                // TODO: handle connection fail
                return;
            }
            setDisconnectDelay();
            synchronized (mSubscribedIds) {
                if (mSubscribedIds.containsKey(mediaId)) {
                    ArrayList<GetFolderItemsCallback> newList =
                            (ArrayList) mSubscribedIds.get(mediaId);
                    newList.add(callback);
                    mSubscribedIds.put(mediaId, newList);
                    return;
                }
                mSubscribedIds.put(mediaId, new ArrayList<>(Arrays.asList(callback)));
                mWrappedBrowser.subscribe(mediaId, new BrowserSubscriptionCallback());
            }
        });
    }


    /**
     * Requests information from {@link MediaBrowser}.
     *
     * <p>If the {@link MediaBrowser} this instance wraps around is already bound,
     * calls the callback directly.
     * <p>If it is binding, adds the callback to the {@code mRequestsList}, to be called once the
     * binding is done.
     * <p>If the binding isn't started, starts it and adds the callback to the {@code
     * mRequestsList}
     */
    private void browseRequest(RequestCallback callback) {
        synchronized (mRequestsList) {
            switch (mBrowserConnectionState) {
                case CONNECTED:
                    callback.run();
                    break;
                case DISCONNECTED:
                    connect();
                    mRequestsList.add(callback);
                    break;
                case CONNECTING:
                    mRequestsList.add(callback);
                    break;
            }
        }
    }

    /** Binds to the {@link MediaBrowser} this instance wraps around. */
    private void connect() {
        synchronized (mRequestsList) {
            if (mBrowserConnectionState != ConnectionState.DISCONNECTED) {
                Log.e(TAG, "Trying to bind to a player that is not disconnected: "
                        + mBrowserConnectionState);
                return;
            }
            mBrowserConnectionState = ConnectionState.CONNECTING;
        }
        mWrappedBrowser.connect();
    }

    /** Disconnects from the {@link MediaBrowser} */
    public void disconnect() {
        synchronized (mRequestsList) {
            mDisconnectHandler.removeCallbacks(mDisconnectRunnable);
            if (mBrowserConnectionState != ConnectionState.DISCONNECTED) {
                Log.e(TAG, "Trying to disconnect to a player that is not connected: "
                        + mBrowserConnectionState);
                return;
            }
            mBrowserConnectionState = ConnectionState.DISCONNECTED;
        }
        mWrappedBrowser.disconnect();
    }

    /**
     * Sets the delay before the disconnection from the {@link MediaBrowser} happens.
     */
    private void setDisconnectDelay() {
        mDisconnectHandler.removeCallbacks(mDisconnectRunnable);
        mDisconnectHandler.postDelayed(mDisconnectRunnable, BROWSER_DISCONNECT_TIMEOUT_MS);
    }

    /** Callback for {@link MediaBrowser} binding. */
    private class MediaConnectionCallback extends MediaBrowser.ConnectionCallback {
        @Override
        public void onConnected() {
            synchronized (mRequestsList) {
                mBrowserConnectionState = ConnectionState.CONNECTED;
                for (RequestCallback callback : mRequestsList) {
                    callback.run();
                }
                mRequestsList.clear();
            }
        }

        @Override
        public void onConnectionFailed() {
            // TODO: Do we answer something or simply ignore the requests? Retry? -> add return value to cb and handle it in APIs
        }

        @Override
        public void onConnectionSuspended() {
            // TODO: Do we answer something or simply ignore the requests? Need to call disconnect.
            mWrappedBrowser.disconnect();
        }
    }

    private class BrowserSubscriptionCallback extends MediaBrowser.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children) {

            ArrayList<ListItem> browsableContent = new ArrayList<ListItem>();

            for (MediaItem item : children) {
                if (item.isBrowsable()) {
                    CharSequence titleCharSequence = item.getDescription().getTitle();
                    // TODO: Store that in strings resources
                    String title = "Not Provided";
                    if (titleCharSequence != null) {
                        title = titleCharSequence.toString();
                    }
                    Folder f = new Folder(item.getMediaId(), false, title);
                    browsableContent.add(new ListItem(f));
                } else {
                    Metadata data = Util.toMetadata(mContext, item);
                    if (Util.isEmptyData(data)) {
                        continue;
                    }
                    browsableContent.add(new ListItem(data));
                }
            }

            synchronized (mRequestsList) {
                for (GetFolderItemsCallback callback : mSubscribedIds.get(parentId)) {
                    callback.run(parentId, Util.cloneList(browsableContent));
                }

                mSubscribedIds.remove(parentId);
            }
            mWrappedBrowser.unsubscribe(parentId);
        }

        @Override
        public void onError(String id) {
        }

        @Override
        public Handler getTimeoutHandler() {
            return null;
        }
    }
}