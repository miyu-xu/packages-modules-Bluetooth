/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs for the LE Audio Broadcast Assistant role, which implements
 * client side protocols for Broadcast Audio Scan Service (BASS)
 *
 * <p>BluetoothLeBroadcastAssistant is a proxy object for controlling the Broadcast Assistant
 * service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the
 * BluetoothLeBroadcastAssistant proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastAssistant implements BluetoothProfile {
    private static final String TAG = "BluetoothLeBroadcastAssistant";
    private static final boolean DBG = true;

    /**
     * This class provides a set of callbacks that are invoked when scanning for Broadcast Sources
     * is
     * offloaded to a Broadcast Assistant.
     *
     * <p>An LE Audio Broadcast Assistant can help a Broadcast Sink to scan for available Broadcast
     * Sources. The Broadcast Sink achieves this by offloading the scan to a Broadcast Assistant.
     * This
     * is facilitated by the Broadcast Audio Scan Service (BASS). A BASS server is a GATT server
     * that is
     * part of the Scan Delegator on a Broadcast Sink. A BASS client instead runs on the Broadcast
     * Assistant.
     *
     * <p>Once a GATT connection is established between the BASS client and the BASS server, the
     * Broadcast Sink can offload the scans to the Broadcast Assistant. Upon finding new Broadcast
     * Sources, the Broadcast Assistant then notifies the Broadcast Sink about these over the
     * established GATT connection. The Scan Delegator on the Broadcast Sink can also notify the
     * Assistant about changes such as addition and removal of Broadcast Sources.
     *
     * @hide
     */
    @SystemApi
    interface Callback {
        /**
         * Callback invoked when the implementation stopped searching for nearby Broadcast Sources
         *
         * @param reason reason code on why search has stopped
         * @hide
         */
        @SystemApi
        void onSearchStopped(int reason);

        /**
         * Callback invoked when a new LE Audio Broadcast Source is found together with the
         * Broadcast
         * Group metadata
         *
         * @param sink   BASS server device
         * @param source {@link BluetoothLeBroadcastGroup} representing a Broadcast Source group
         *               with
         *               metadata
         * @hide
         */
        @SystemApi
        void onSourceFound(@NonNull BluetoothDevice sink,
                @NonNull BluetoothLeBroadcastGroup source);

        /** @hide */
        @IntDef(value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_BAD_CODE,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_CODE_REQUIRED,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_DUPLICATE_ADDITION,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_BASS_UPDATE_TIMEOUT,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_SOURCE_SYNC_TIMEOUT,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_NO_EMPTY_SLOT,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_INVALID_GROUP_OPERATION,
        })
        @Retention(RetentionPolicy.SOURCE)
        @interface SourceAddedStatus {
        }

        /**
         * Callback invoked when a new LE Audio Broadcast Source has been successfully added to the
         * Scan
         * Delegator (within a Broadcast Sink, for example).
         *
         * @param sink     Scan Delegator device on which a new Broadcast Source has been added
         * @param sourceId source ID as defined in the BASS specification
         * @param status   status of source addition
         */
        void onSourceAdded(
                @NonNull BluetoothDevice sink, int sourceId, @SourceAddedStatus int status);

        /** @hide */
        @IntDef(value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_BAD_CODE,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_CODE_REQUIRED,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_BASS_UPDATE_TIMEOUT,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_SOURCE_SYNC_TIMEOUT,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_INVALID_GROUP_OPERATION,
        })
        @Retention(RetentionPolicy.SOURCE)
        @interface SourceModifiedStatus {}

        /**
         * Callback invoked when an existing LE Audio Broadcast Source within a remote Scan
         * Delegator
         * has been updated. This updates happens anytime when the
         *
         * @param sink     Scan Delegator device on which a Broadcast Source has been updated
         * @param sourceId source ID as defined in the BASS specification
         * @param status   status of source modification
         */
        void onSourceModified(
                @NonNull BluetoothDevice sink, int sourceId, @SourceModifiedStatus int status);

        /** @hide */
        @IntDef(value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_BASS_UPDATE_TIMEOUT,
                BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_INVALID_GROUP_OPERATION,
        })
        @Retention(RetentionPolicy.SOURCE)
        @interface SourceRemovedStatus { }

        /**
         * Callback invoked when an LE Audio Broadcast Source has been successfully removed from the
         * Scan Delegator (within a Broadcast Sink, for example).
         *
         * @param sink     Scan Delegator device from which a Broadcast Source has been removed
         * @param sourceId source id of the removed source
         * @param status   status of the source removal
         */
        void onSourceRemoved(
                @NonNull BluetoothDevice sink, int sourceId, @SourceRemovedStatus int status);

        /**
         * Callback invoked when the Broadcast Receive State information of a BASS server device
         * changes.
         *
         * @param sink  BASS server device that is also a Broadcast Sink device
         * @param state latest state information between the Broadcast Sink and a Broadcast Source
         * @hide
         */
        @SystemApi
        void onReceiveStateChanged(
                @NonNull BluetoothDevice sink, @NonNull BluetoothLeBroadcastReceiveState state);
    }

    /**
     * Intent used to broadcast the change in connection state of devices via Broadcast Audio Scan
     * Service (BASS). Please note that in a coordinated set, each set member will connect via BASS
     * individually. Group operations on a single set member will propagate to the entire set.
     *
     * For example, in the binaural case, there will be two different LE devices for the left and
     * right side and each device will have their own connection state changes. If both devices
     * belongs to on Coordinated Set, operating on one of them will affect both devices
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.CONNECTION_STATE_CHANGED";

    /**
     * Create a new instance of an LE Audio Broadcast Assistant.
     *
     * @hide
     */
    /*package*/ BluetoothLeBroadcastAssistant(
            @NonNull Context context, @NonNull ServiceListener listener) {
    }


    /**
     * {@inheritDoc}
     */
    @SystemApi
    @Override
    public @BluetoothProfile.BtProfileState int getConnectionState(@NonNull BluetoothDevice sink) {
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * {@inheritDoc}
     */
    @SystemApi
    @Override
    @NonNull
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @SystemApi
    @Override
    @NonNull public List<BluetoothDevice> getConnectedDevices() {
        return Collections.emptyList();
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device           Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setConnectionPolicy(@NonNull BluetoothDevice device,
            @ConnectionPolicy int connectionPolicy) {
        return false;
    }

    /**
     * Register a {@link Callback} that will be invoked during the
     * operation of this profile.
     *
     * Repeated registration of the same <var>callback</var> object will have no effect after
     * the first call to this method, even when the <var>executor</var> is different. API caller
     * would have to call {@link #unregisterCallback(Callback)} with
     * the same callback object before registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws IllegalArgumentException if a null executor, sink, or callback is given
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void registerCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        log("registerCallback");
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Unregister the specified {@link Callback}
     * <p>The same {@link Callback} object used when calling
     * {@link #registerCallback(Executor, Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws IllegalArgumentException when callback is null or when no callback is registered
     */
    @SystemApi
    public void unregisterCallback(
            @NonNull Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        log("unregisterCallback");
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Search for LE Audio Broadcast Sources on behalf of all devices connected via Broadcast Audio
     * Scan Service, filtered by <var>filters</var>.
     *
     * The implementation will also synchronize with discovered Broadcast Sources and get their
     * metadata before passing the Broadcast Source metadata back to the application using {@link
     * Callback#onSourceFound}.
     *
     * Please disconnect the BASS server by calling {@link #setConnectionPolicy(BluetoothDevice,
     * int)}
     * to {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN} if you do not want the BASS server
     * to receive notifications about this search before calling this method.
     *
     * <var>filters</var> will be AND'ed with internal filters in the implementation and
     * {@link android.bluetooth.le.ScanSettings} will be managed by the implementation.
     *
     * @param filters {@link ScanFilter}s for finding exact Broadcast Source, if no filter is
     *                                  needed, please use an empty list instead
     * @throws IllegalArgumentException when <var>filters</var> argument is null
     * @throws IllegalStateException    when no callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void startSearchingForSources(@NonNull List<ScanFilter> filters) {
        log("searchForBroadcastSources");
        if (filters == null) {
            throw new IllegalArgumentException("filters can be empty, but not null");
        }
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Stops an ongoing search for nearby LE Audio Broadcast Sources
     *
     * When the search is stopped, callback {@link Callback#onSearchStopped(int)} will be invoked
     * with the stop reason.
     *
     * If the search is already stopped when this method is called,
     * {@link Callback#onSearchStopped(int)} will still be called.
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void stopSearchingForSources() {
        log("stopSearchingForSources:");
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Asks BASS server to add the provided Broadcast Source and stream from it to the Broadcast
     * Sink.
     *
     * <p>Upon addition of the Broadcast Source, {@link Callback#onSourceAdded} will be invoked with
     * reason code and added Broadcast Group
     *
     * <p>If there are multiple members in the coordinated set the sink belongs to, and isGroupOp is
     * set to true, the source information will be added to each sink in the coordinated set and a
     * separate {@link Callback#onSourceAdded} callback will be invoked
     * for each member of the coordinated set.
     *
     * @param sink      {@link BluetoothDevice} representing the Broadcast Sink to which the
     *                  Broadcast
     *                  Source should be added
     * @param source    Broadcast Source to be added to the Scan Delegator
     * @param isGroupOp set to true If Application wants to perform this operation for all
     *                  coordinated set members, False otherwise
     * @throws IllegalArgumentException if <var>sink</var> or <var>source</var> are null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void addSource(@NonNull BluetoothDevice sink, @NonNull BluetoothLeBroadcastGroup source,
            boolean isGroupOp) {
        log("addBroadcastSource: " + source + " on " + sink);
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Updates Broadcast Source information on a Scan Delegator.
     *
     * <p>After updating the Broadcast Source on the Scan Delegator, the callback {@link
     * Callback#onSourceModified(BluetoothDevice, int, int)} will be
     * invoked.
     *
     * <p>In case of Group Operation, if there are no matching sources among any coordinated set
     * members, this operation will fail and the callback {@link
     * Callback#onSourceModified(BluetoothDevice, int, int)} will be
     * invoked.
     *
     * <p>If there are multiple members in the coordinated set the sink belongs to, and isGroupOp
     * is
     * set to true, the source information will be updated on each sink in the coordinated set and
     * a
     * separate {@link Callback#onSourceModified(BluetoothDevice, int,
     * int)}
     * callback will be invoked for each member of the coordinated set.
     *
     * @param sink      {@link BluetoothDevice} representing the Broadcast Sink to which the
     *                  Broadcast
     *                  Source should be updated
     * @param sourceId  source ID as delivered in
     *                  {@link Callback#onSourceAdded(BluetoothDevice,
     *                  int, int)}
     * @param source    Broadcast Source to be updated on the Scan Delegator
     * @param isGroupOp set to true if the application wants to perform this operation for all the
     *                  coordinated set members, false otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void modifySource(@NonNull BluetoothDevice sink, int sourceId,
            @NonNull BluetoothLeBroadcastGroup source,
            boolean isGroupOp) {
        log("updateBroadcastSource: " + source + " on " + sink);
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Removes the Broadcast Source Information from a Scan delegator.
     *
     * <p>Upon removal of Broadcast Source information from the Scan Delegator, the callback {@link
     * Callback#onSourceRemoved} will be invoked.
     *
     * <p>If there are multiple members in the coordinated set the sink belongs to, and isGroupOp is
     * set to true, the source information will be removed from each sink in the coordinated set
     * and a separate {@link Callback#onSourceRemoved} callback will
     * be invoked for each member of the coordinated set.
     *
     * @param sink      {@link BluetoothDevice} representing the Broadcast Sink from which a
     *                  Broadcast
     *                  Source should be removed
     * @param sourceId  source ID of the Broadcast Source which needs to be removed
     * @param isGroupOp true if an application wants to perform this operation for all the
     *                  coordinated set members, false otherwise
     * @throws IllegalArgumentException when the <var>sink</var> is null
     * @hide
     */
    @SystemApi
    public void removeSource(
            @NonNull BluetoothDevice sink, int sourceId, boolean isGroupOp) {
        log("removeBroadcastSource: " + sourceId + " from " + sink);
        return;
    }


    /**
     * Get information about all the Broadcast Sources that a Scan Delegator knows about.
     *
     * @param sink {@link BluetoothDevice} representing the Broadcast Sink from which to get all
     *             Broadcast Sources
     * @return the list of Broadcast Receive State {@link BluetoothLeBroadcastReceiveState}
     * stored in the Scan Delegator
     * @throws IllegalArgumentException when <var>sink</var> is null
     * @hide
     */
    @SystemApi
    @NonNull
    public List<BluetoothLeBroadcastReceiveState> getAllSources(@NonNull BluetoothDevice sink) {
        return Collections.emptyList();
    }

    /**
     * Get maximum number of sources can be added to this Broadcast Sink
     *
     * @param sink Broadcast Sink device that is also the BASS server
     * @return maximum number of sources can be added to this Broadcast Sink
     * @throws IllegalArgumentException when <var>sink</var> is null
     * @hide
     */
    @SystemApi
    public int getMaximumSourceCapacity(@NonNull BluetoothDevice sink) {
        return 0;
    }

    private static void log(@NonNull String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}
