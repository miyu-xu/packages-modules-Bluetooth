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


// Binds to a MediaBrowser, then retrieves the content.

// Connect to player, wait for Media cb, execute event, wait for Media cb, execute AVRCP cb

class BrowsedPlayerWrapper {

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

    private final MediaBrowser mWrappedBrowser;
    private final Context mContext;
    private final Looper mLooper;
    private final String mPackageName;
    private final Handler mDisconnectHandler;

    private ConnectionState mBrowserConnectionState = ConnectionState.DISCONNECTED;

    private final ArrayList<RequestCallback> mRequestsList = new ArrayList<>();

    // GetFolderItems also works with a callback, so we need to store all requests made before we
    // got the results and prevent new subscribtions.
    private final HashMap<Integer, ArrayList<GetFolderItemsCallback>> mSubscribedIds = new ArrayList<>();

    private final Runnable mDisconnectRunnable = new Runnable() {
        @Override
        public void run() {
            disconnect();
        }
    }


    public BrowsedPlayerWrapper(Context context,
                                Looper looper,
                                String packageName,
                                String className,
                                BrowseCallback callback) {
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

    public void getRootId(GetRootIdCallback callback) {
        browseRequest(() -> {
            if (mBrowserBoundState != ConnectionState.CONNECTED) {
                Log.e(TAG, "getRootId: Callback triggered before binding done.");
                // TODO: handle connection fail
            }
            setDisconnectDelay();
            callback.run(mWrappedBrowser.getRoot());
        });
    }

    public void playItem(String mediaId) {
        browseRequest(() -> {
            if (mBrowserBoundState != ConnectionState.CONNECTED) {
                Log.e(TAG, "playItem: Callback triggered before binding done.");
                // TODO: handle connection fail
            }
            setDisconnectDelay();
            // Retrieve the MediaController linked with this MediaBrowser 
            MediaController controller = MediaControllerFactory.make(mContext,
                    mWrappedBrowser.getSessionToken());
            // Retrieve TransportControls from this MediaController and play mediaId
            MediaController.TransportControls ctrl = controller.getTransportControls();
            ctrl.playFromMediaId(mediaId, null);

        });
    }

    public void getFolderItems(String mediaId, GetFolderItemsCallback callback) {
        browseRequest(() -> {
            if (mBrowserBoundState != ConnectionState.CONNECTED) {
                Log.e(TAG, "playItem: Callback triggered before binding done.");
                // TODO: handle connection fail
                return;
            }
            setDisconnectDelay();
            if (mSubscribedIds.containsKey(mediaId)) {
                mSubscribedIds.put(mediaId, ((ArrayList) mSubscribedIds.get(mediaId)).clone().add(callback));
                return;
            }
            mSubscribedIds.put(mediaId, new ArrayList<>(Arrays.asList(callback)));
            mWrappedBrowser.subscribe(mediaId, new BrowserSubscriptionCallback());
        })
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
        switch (mBrowserBoundState) {
            case ConnectionState.CONNECTED:
                callback.run();
                break;
            case ConnectionState.DISCONNECTED:
                connect();
                mRequestsList.add(callback);
                break;
            case ConnectionState.CONNECTING:
                mRequestsList.add(callback);
                break;
        }
    }

    /** Binds to the {@link MediaBrowser} this instance wraps around. */
    private void connect() {
        if (mBrowserBoundState != ConnectionState.DISCONNECTED) {
            Log.e(TAG, "Trying to bind to a player that is not disconnected: "
                    + mBrowserBoundState);
            return;
        }
        mBrowserBoundState = ConnectionState.CONNECTING;
        mWrappedBrowser.connect();
    }

    /** Disconnects from the {@link MediaBrowser} */
    private void disconnect() {
        if (mBrowserBoundState != ConnectionState.DISCONNECTED) {
            Log.e(TAG, "Trying to disconnect to a player that is not connected: "
                    + mBrowserBoundState);
            return;
        }
        mBrowserBoundState = ConnectionState.DISCONNECTED;
        mWrappedBrowser.disconnect();
    }

    /**
     * Sets the delay before the disconnection from the {@link MediaBrowser} happens.
     *
     * <p>If there are a lot of pending requests in the queue, it could potentially disconnect
     * before all the requests are satisfied, so we increase the delay by the number of requests.
     */
    private void setDisconnectDelay() {
        mDisconnectHandler.removeCallbacks(mDisconnectRunnable);
        mDisconnectHandler.postDelayed(mDisconnectRunnable, BROWSER_DISCONNECT_TIMEOUT_MS);
    }

    /** Callback for {@link MediaBrowser} binding. */
    private class MediaConnectionCallback extends MediaBrowser.ConnectionCallback {
        @Override
        public void onConnected() {
            mBrowserBoundState = ConnectionState.CONNECTED;
            for (RequestCallback callback : mRequestsList) {
                callback.run();
            }
            mRequestsList.clear();
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

            for (GetFolderItemsCallback callback : mSubscribedIds.get(parentId)) {
                callback.run(STATUS_SUCCESS, parentId, Util.cloneList(browsableContent));
            }

            mSubscribedIds.remove(parentId);
            mWrappedBrowser.unsubscribe(parentId);
        }

        @Override
        public void onError(String id) {
        }
    }
}