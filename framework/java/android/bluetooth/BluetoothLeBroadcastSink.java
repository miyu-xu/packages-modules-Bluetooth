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

import static android.Manifest.permission.BLUETOOTH_SCAN;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothScanPermission;
import android.bluetooth.le.ScanFilter;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs to control the Bluetooth LE Audio Broadcast Sink profile.
 *
 * <p>BluetoothLeBroadcastSink is a proxy object for controlling the Bluetooth LE Audio Broadcast
 * Sink Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the
 * BluetoothLeBroadcastSink proxy object.
 *
 * <p>Android devices can act as LE Audio broadcast sinks to receive and play broadcast audio
 * from LE Audio broadcasters. This enables use cases like:
 * <ul>
 *   <li>Public announcements in airports, train stations</li>
 *   <li>Personal audio sharing between devices</li>
 *   <li>Assistive listening applications</li>
 * </ul>
 *
 * @hide
 */
@SuppressLint("UnflaggedApi")
@FlaggedApi(Flags.FLAG_LEAUDIO_BROADCAST_SINK_API)
@SystemApi
public final class BluetoothLeBroadcastSink implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothLeBroadcastSink";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private final CloseGuard mCloseGuard;
    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothLeBroadcastSink mService;
    private final Map<Callback, Executor> mCallbackExecutorMap = new HashMap<>();

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothLeBroadcastSinkCallback mCallback =
            new IBluetoothLeBroadcastSinkCallback.Stub() {
                @Override
                public void onBroadcastFound(BluetoothLeBroadcastMetadata source) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onBroadcastFound(source));
                    }
                }

                @Override
                public void onSearchStarted(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStarted(reason));
                    }
                }

                @Override
                public void onSearchStartFailed(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStartFailed(reason));
                    }
                }

                @Override
                public void onSearchStopped(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStopped(reason));
                    }
                }

                @Override
                public void onSearchStopFailed(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStopFailed(reason));
                    }
                }

                @Override
                public void onSyncStarted(int reason, int broadcastId) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSyncStarted(reason, broadcastId));
                    }
                }

                @Override
                public void onSyncStartFailed(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSyncStartFailed(reason));
                    }
                }

                @Override
                public void onSyncStopped(int reason, int broadcastId) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSyncStopped(reason, broadcastId));
                    }
                }

                @Override
                public void onSyncStopFailed(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSyncStopFailed(reason));
                    }
                }

                @Override
                public void onCaptureStarted(int reason, int broadcastId) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onCaptureStarted(reason, broadcastId));
                    }
                }

                @Override
                public void onCaptureStopped(int reason, int broadcastId) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onCaptureStopped(reason, broadcastId));
                    }
                }

                @Override
                public void onBroadcastMetadataChanged(int broadcastId, BluetoothLeBroadcastMetadata metadata) {
                    for (Map.Entry<BluetoothLeBroadcastSink.Callback, Executor> callbackExecutorEntry :
                            mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastSink.Callback callback = callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onBroadcastMetadataChanged(broadcastId, metadata));
                    }
                }
            };

    /**
     * Interface for receiving events related to Broadcast Sink
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    BluetoothStatusCodes.SUCCESS,
                    BluetoothStatusCodes.ERROR_UNKNOWN,
                    BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST,
                    BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                    BluetoothStatusCodes.REASON_SYSTEM_POLICY,
                    BluetoothStatusCodes.ERROR_HARDWARE_GENERIC,
                    BluetoothStatusCodes.ERROR_BAD_PARAMETERS,
                    BluetoothStatusCodes.ERROR_LOCAL_NOT_ENOUGH_RESOURCES,
                    BluetoothStatusCodes.ERROR_LE_BROADCAST_INVALID_CODE,
                    BluetoothStatusCodes.ERROR_LE_BROADCAST_INVALID_BROADCAST_ID,
                })
        @interface Reason {}

        /**
         * Called when a broadcast source is found during scanning.
         *
         * @param source the broadcast metadata of the found source
         * @hide
         */
        @SystemApi
        void onBroadcastFound(@NonNull BluetoothLeBroadcastMetadata source);

        /**
         * Called when broadcast source scanning has started.
         *
         * @param reason the reason for starting the search
         * @hide
         */
        @SystemApi
        void onSearchStarted(@Reason int reason);

        /**
         * Called when broadcast source scanning failed to start.
         *
         * @param reason the reason for the failure
         * @hide
         */
        @SystemApi
        void onSearchStartFailed(@Reason int reason);

        /**
         * Called when broadcast source scanning has stopped.
         *
         * @param reason the reason for stopping the search
         * @hide
         */
        @SystemApi
        void onSearchStopped(@Reason int reason);

        /**
         * Called when broadcast source scanning failed to stop.
         *
         * @param reason the reason for the failure
         * @hide
         */
        @SystemApi
        void onSearchStopFailed(@Reason int reason);

        /**
         * Called when sync to a broadcast source has started.
         *
         * @param reason the reason for starting the sync
         * @param broadcastId the broadcast ID of the synced source
         * @hide
         */
        @SystemApi
        void onSyncStarted(@Reason int reason, int broadcastId);

        /**
         * Called when sync to a broadcast source failed to start.
         *
         * @param reason the reason for the failure
         * @hide
         */
        @SystemApi
        void onSyncStartFailed(@Reason int reason);

        /**
         * Called when sync to a broadcast source has stopped.
         *
         * @param reason the reason for stopping the sync
         * @param broadcastId the broadcast ID of the synced source
         * @hide
         */
        @SystemApi
        void onSyncStopped(@Reason int reason, int broadcastId);

        /**
         * Called when sync to a broadcast source failed to stop.
         *
         * @param reason the reason for the failure
         * @hide
         */
        @SystemApi
        void onSyncStopFailed(@Reason int reason);

        /**
         * Called when audio capture from a broadcast source has started.
         *
         * @param reason the reason for starting the capture
         * @param broadcastId the broadcast ID of the source
         * @hide
         */
        @SystemApi
        void onCaptureStarted(@Reason int reason, int broadcastId);

        /**
         * Called when audio capture from a broadcast source has stopped.
         *
         * @param reason the reason for stopping the capture
         * @param broadcastId the broadcast ID of the source
         * @hide
         */
        @SystemApi
        void onCaptureStopped(@Reason int reason, int broadcastId);

        /**
         * Called when broadcast metadata has changed.
         *
         * @param broadcastId the broadcast ID of the source
         * @param metadata the updated broadcast metadata
         * @hide
         */
        @SystemApi
        void onBroadcastMetadataChanged(int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata);
    }

    /**
     * Create a BluetoothLeBroadcastSink proxy object for interacting with the local
     * LE Audio Broadcast Sink service.
     *
     * @param context the context
     * @param adapter the BluetoothAdapter
     *
     * @hide
     */
    /*package*/ BluetoothLeBroadcastSink(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = mAdapter.getAttributionSource();
        mService = null;

        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /** @hide */
    @SuppressWarnings("Finalize") // TODO(b/314811467)
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public void close() {
        if (VDBG) log("close()");
        mAdapter.closeProfileProxy(this);
    }

    /**
     * Not supported since LE Audio Broadcast Sinks do not establish a connection.
     *
     * @hide
     */
    @Override
    @RequiresNoPermission
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException("LE Audio Broadcast Sinks are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcast Sinks do not establish a connection.
     *
     * @hide
     */
    @Override
    @RequiresNoPermission
    public @NonNull List<BluetoothDevice> getDevicesMatchingConnectionStates(@Nullable int[] states) {
        throw new UnsupportedOperationException("LE Audio Broadcast Sinks are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcast Sinks do not establish a connection.
     *
     * @hide
     */
    @Override
    @RequiresNoPermission
    public @BluetoothProfile.BtProfileState int getConnectionState(@Nullable BluetoothDevice device) {
        throw new UnsupportedOperationException("LE Audio Broadcast Sinks are not connection-oriented.");
    }

    /**
     * Register a {@link Callback} that will be invoked during the operation of this profile.
     *
     * <p>Repeated registration of the same <var>callback</var> object after the first call to this
     * method will result with IllegalArgumentException being thrown, even when the
     * <var>executor</var> is different. API caller must call {@link #unregisterCallback(Callback)}
     * with the same callback object before registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException if a null executor, or callback is given, or
     *     IllegalArgumentException if the same <var>callback<var> is already registered.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public void registerCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull Callback callback) {
        requireNonNull(executor);
        requireNonNull(callback);

        if (DBG) log("registerCallback");

        synchronized (mCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mCallbackExecutorMap.isEmpty()) {
                if (!mAdapter.isEnabled()) {
                    /* If Bluetooth is off, just store callback and it will be registered
                     * when Bluetooth is on
                     */
                    mCallbackExecutorMap.put(callback, executor);
                    return;
                }
                try {
                    final IBluetoothLeBroadcastSink service = getService();
                    if (service != null) {
                        service.registerCallback(mAttributionSource, mCallback);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                }
            }

            // Adds the passed in callback to our map of callbacks to executors
            if (mCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            }
            mCallbackExecutorMap.put(callback, executor);
        }
    }

    /**
     * Unregister the specified {@link Callback}
     *
     * <p>The same {@link Callback} object used when calling {@link #registerCallback(Executor,
     * Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when the application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException when callback is null or IllegalArgumentException when no
     *     callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public void unregisterCallback(@NonNull Callback callback) {
        requireNonNull(callback);

        if (DBG) log("unregisterCallback");

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.remove(callback) == null) {
                throw new IllegalArgumentException("This callback has not been registered");
            }
        }

        // If the callback map is empty, we unregister the service-to-app callback
        if (mCallbackExecutorMap.isEmpty()) {
            try {
                final IBluetoothLeBroadcastSink service = getService();
                if (service != null) {
                    service.unregisterCallback(mAttributionSource, mCallback);
                }
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Start searching for LE Audio broadcast sources.
     *
     * <p>This method starts scanning for LE Audio broadcast sources that are advertising
     * broadcast audio. Found sources will be reported via the {@link Callback#onBroadcastFound}
     * callback.
     *
     * <p>On success, {@link Callback#onSearchStarted(int)} will be invoked with {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} reason code. On failure, the callback
     * will be invoked with appropriate error reason code.
     *
     * @param filters optional list of scan filters to apply during scanning
     * @throws IllegalStateException if callback was not registered
     * @throws NullPointerException if <var>filters</var> contains null elements
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public void startSearchingForSources(@Nullable List<ScanFilter> filters) {
        if (mCallbackExecutorMap.isEmpty()) {
            throw new IllegalStateException("No callback was ever registered");
        }

        if (DBG) log("startSearchingForSources()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.startSearchingForSources(mAttributionSource, filters);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Stop searching for LE Audio broadcast sources.
     *
     * <p>This method stops the ongoing scan for LE Audio broadcast sources.
     *
     * <p>On success, {@link Callback#onSearchStopped(int)} will be invoked with {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} reason code. On failure, the callback
     * will be invoked with appropriate error reason code.
     *
     * @throws IllegalStateException if callback was not registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public void stopSearchingForSources() {
        if (mCallbackExecutorMap.isEmpty()) {
            throw new IllegalStateException("No callback was ever registered");
        }

        if (DBG) log("stopSearchingForSources()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.stopSearchingForSources(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Sync to a broadcast source to receive its audio.
     *
     * <p>This method establishes synchronization with the specified broadcast source
     * to receive and play its audio content.
     *
     * <p>On success, {@link Callback#onSyncStarted(int, int)} will be invoked with {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} reason code and the broadcast ID.
     * On failure, {@link Callback#onSyncStartFailed(int)} will be invoked with appropriate
     * error reason code.
     *
     * @param metadata the broadcast metadata of the source to sync to
     * @param broadcastCode optional broadcast code for encrypted broadcasts
     * @throws IllegalStateException if callback was not registered
     * @throws NullPointerException if <var>metadata</var> is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public void syncToBroadcast(@NonNull BluetoothLeBroadcastMetadata metadata,
            @Nullable byte[] broadcastCode) {
        requireNonNull(metadata);
        if (mCallbackExecutorMap.isEmpty()) {
            throw new IllegalStateException("No callback was ever registered");
        }

        if (DBG) log("syncToBroadcast()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.syncToBroadcast(mAttributionSource, metadata, broadcastCode);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Terminate sync to a broadcast source.
     *
     * <p>This method stops synchronization with the specified broadcast source
     * and stops receiving its audio content.
     *
     * <p>On success, {@link Callback#onSyncStopped(int, int)} will be invoked with {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} reason code and the broadcast ID.
     * On failure, {@link Callback#onSyncStopFailed(int)} will be invoked with appropriate
     * error reason code.
     *
     * @param broadcastId the broadcast ID of the source to stop syncing to
     * @throws IllegalStateException if callback was not registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public void terminateSync(int broadcastId) {
        if (mCallbackExecutorMap.isEmpty()) {
            throw new IllegalStateException("No callback was ever registered");
        }

        if (DBG) log("terminateSync()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.terminateSync(mAttributionSource, broadcastId);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Check if currently capturing audio from a broadcast source.
     *
     * @param broadcastId the broadcast ID to check
     * @return true if capturing audio from the specified broadcast, false otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public boolean isCapturing(int broadcastId) {
        if (VDBG) log("isCapturing()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return false;
        } else if (isEnabled()) {
            try {
                return service.isCapturing(mAttributionSource, broadcastId);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Get all currently synced broadcasts.
     *
     * @return list of broadcast metadata for all synced broadcasts
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public @NonNull List<BluetoothLeBroadcastMetadata> getAllSyncedBroadcasts() {
        if (VDBG) log("getAllSyncedBroadcasts()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return Collections.emptyList();
        } else if (isEnabled()) {
            try {
                return service.getAllSyncedBroadcasts(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the maximum number of broadcasts that can be synced simultaneously.
     *
     * @return maximum number of concurrent syncs supported
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(BLUETOOTH_SCAN)
    public int getMaximumNumberOfSyncs() {
        if (VDBG) log("getMaximumNumberOfSyncs()");

        final IBluetoothLeBroadcastSink service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return 0;
        } else if (isEnabled()) {
            try {
                return service.getMaximumNumberOfSyncs(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return 1;
    }

    private boolean isEnabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    /** @hide */
    @Override
    @SuppressLint("AndroidFrameworkRequiresPermission") // Unexposed re-entrant callback
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothLeBroadcastSink.Stub.asInterface(service);
        // re-register the service-to-app callback
        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                return;
            }
            try {
                if (service != null) {
                    mService.registerCallback(mAttributionSource, mCallback);
                }
            } catch (RemoteException e) {
                Log.e(
                        TAG,
                        "onServiceConnected: Failed to register " + "Le Broadcast Sink callback",
                        e);
            }
        }
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothLeBroadcastSink getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
