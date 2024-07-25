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

package com.android.bluetooth.gmap;

import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.bluetooth.gmap.GmapClientStateMachine;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A profile service that connects to the Gaming Audio Profile (GMAP) of BLE devices */
public class GmapClientService extends ProfileService {
    private static final String TAG = "GmapClientService";

    // Timeout for state machine thread join, to prevent potential ANR.
    private static final int SM_THREAD_JOIN_TIMEOUT_MS = 1_000;

    private static GmapClientService sGmapClientService;
    private AdapterService mAdapterService;
    private DatabaseManager mDatabaseManager;
    private HandlerThread mStateMachinesThread;
    private Handler mHandler;
    private final Map<BluetoothDevice, GmapClientStateMachine> mStateMachines = new HashMap<>();

    public GmapClientService(Context ctx) {
        super(ctx);
    }

    public static boolean isEnabled() {
        return false;
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return null;
    }

    @Override
    public void start() {
        Log.d(TAG, "start()");
        if (sGmapClientService != null) {
            throw new IllegalStateException("start() called twice");
        }

        mAdapterService =
                Objects.requireNonNull(
                        AdapterService.getAdapterService(),
                        "AdapterService cannot be null when GmapClientService starts");
        mDatabaseManager =
                Objects.requireNonNull(
                        mAdapterService.getDatabase(),
                        "DatabaseManager cannot be null when GmapClientService starts");

        mHandler = new Handler(Looper.getMainLooper());
        mStateMachines.clear();
        mStateMachinesThread = new HandlerThread("GmapClientService.StateMachines");
        mStateMachinesThread.start();

        setGamingAudioService(this);
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop()");
        if (sGmapClientService == null) {
            Log.w(TAG, "stop() called before start()");
            return;
        }

        setGamingAudioService(null);

        // Destroy state machines and stop handler thread
        synchronized (mStateMachines) {
            // todo clean up
            for (GmapClientStateMachine sm : mStateMachines.values()) {
                sm.doQuit();
                sm.cleanup();
            }
            mStateMachines.clear();
        }

        if (mStateMachinesThread != null) {
            try {
                mStateMachinesThread.quitSafely();
                mStateMachinesThread.join(SM_THREAD_JOIN_TIMEOUT_MS);
                mStateMachinesThread = null;
            } catch (InterruptedException e) {
                // Do not rethrow as we are shutting down anyway
            }
        }

        // Unregister Handler and stop all queued messages.
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        mAdapterService = null;
    }

    @Override
    public void cleanup() {
        Log.d(TAG, "cleanup()");
    }

    /** Gets the GmapClientService instance */
    public static synchronized GmapClientService getGamingAudioService() {
        if (sGmapClientService == null) {
            Log.w(TAG, "getGamingAudioService(): service is NULL");
            return null;
        }

        if (!sGmapClientService.isAvailable()) {
            Log.w(TAG, "getGamingAudioService(): service is not available");
            return null;
        }
        return sGmapClientService;
    }

    /** Sets the gaming audio service instance. It should be called only for testing purpose. */
    @VisibleForTesting
    public static synchronized void setGamingAudioService(GmapClientService instance) {
        Log.d(TAG, "setGamingAudioService(): set to: " + instance);
        sGmapClientService = instance;
    }

    /** Connects to the GMAP service of the given device. */
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    public boolean connect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        Log.d(TAG, "connect(): " + device);
        if (device == null) {
            Log.w(TAG, "Ignore connecting to null device");
            return false;
        }

        if (getConnectionPolicy(device) == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            Log.w(TAG, "Cannot connect to " + device + " : policy forbidden");
            return false;
        }
        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);
        if (!Utils.arrayContains(featureUuids, BluetoothUuid.GMAP)) {
            Log.e(TAG, "Cannot connect to " + device + " : Remote does not have GMAP UUID");
            return false;
        }

        synchronized (mStateMachines) {
            GmapClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                Log.e(TAG, "Cannot connect to " + device + " : no state machine");
                return false;
            }
            sm.sendMessage(GmapClientStateMachine.CONNECT);
        }

        return true;
    }

    /**
     * Connects to the Gaming Audio Profile service of the given device if possible. If it's impossible, it
     * doesn't try without logging errors.
     */
    public boolean connectIfPossible(BluetoothDevice device) {
        if (device == null
                || getConnectionPolicy(device) == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                || !Utils.arrayContains(
                        mAdapterService.getRemoteUuids(device), BluetoothUuid.GMAP)) {
            return false;
        }
        return connect(device);
    }

    /** Disconnects from the GMAP of the given device. */
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    public boolean disconnect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        Log.d(TAG, "disconnect(): " + device);
        if (device == null) {
            Log.w(TAG, "Ignore disconnecting to null device");
            return false;
        }
        synchronized (mStateMachines) {
            GmapClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm != null) {
                sm.sendMessage(GmapClientStateMachine.DISCONNECT);
            }
        }

        return true;
    }

    /** Gets devices that GMAP service is connected. */
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        synchronized (mStateMachines) {
            List<BluetoothDevice> devices = new ArrayList<>();
            for (GmapClientStateMachine sm : mStateMachines.values()) {
                if (sm.isConnected()) {
                    devices.add(sm.getDevice());
                }
            }
            return devices;
        }
    }

    /**
     * Check whether it can connect to a peer device. The check considers a number of factors during
     * the evaluation.
     *
     * @param device the peer device to connect to
     * @return true if connection is allowed, otherwise false
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean canConnect(BluetoothDevice device) {
        // Check connectionPolicy and accept or reject the connection.
        int connectionPolicy = getConnectionPolicy(device);
        int bondState = mAdapterService.getBondState(device);
        // Allow this connection only if the device is bonded. Any attempt to connect while
        // bonding would potentially lead to an unauthorized connection.
        if (bondState != BluetoothDevice.BOND_BONDED) {
            Log.w(TAG, "canConnect: return false, bondState=" + bondState);
            return false;
        } else if (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_UNKNOWN
                && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            // Otherwise, reject the connection if connectionPolicy is not valid.
            Log.w(TAG, "canConnect: return false, connectionPolicy=" + connectionPolicy);
            return false;
        }
        return true;
    }

    /** Called when the connection state of a state machine is changed */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void handleConnectionStateChanged(GmapClientStateMachine sm, int fromState, int toState) {
        BluetoothDevice device = sm.getDevice();
        if ((sm == null) || (fromState == toState)) {
            Log.e(
                    TAG,
                    "connectionStateChanged: unexpected invocation. device="
                            + device
                            + " fromState="
                            + fromState
                            + " toState="
                            + toState);
            return;
        }

        // Check if the device is disconnected - if unbonded, remove the state machine
        if (toState == BluetoothProfile.STATE_DISCONNECTED) {
            int bondState = mAdapterService.getBondState(device);
            if (bondState == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, device + " is unbonded. Remove state machine");
                removeStateMachine(device);
            }
        }
    }

    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        if (states == null) {
            return devices;
        }
        final BluetoothDevice[] bondedDevices = mAdapterService.getBondedDevices();
        if (bondedDevices == null) {
            return devices;
        }
        synchronized (mStateMachines) {
            for (BluetoothDevice device : bondedDevices) {
                int connectionState = BluetoothProfile.STATE_DISCONNECTED;
                GmapClientStateMachine sm = mStateMachines.get(device);
                if (sm != null) {
                    connectionState = sm.getConnectionState();
                }
                for (int state : states) {
                    if (connectionState == state) {
                        devices.add(device);
                        break;
                    }
                }
            }
            return devices;
        }
    }

    /**
     * Get the list of devices that have state machines.
     *
     * @return the list of devices that have state machines
     */
    @VisibleForTesting
    List<BluetoothDevice> getDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        synchronized (mStateMachines) {
            for (GmapClientStateMachine sm : mStateMachines.values()) {
                devices.add(sm.getDevice());
            }
            return devices;
        }
    }

    /** Gets the connection state of the given device's GMAP service */
    public int getConnectionState(BluetoothDevice device) {
        synchronized (mStateMachines) {
            GmapClientStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getConnectionState();
        }
    }

    /**
     * Set connection policy of the profile and connects it if connectionPolicy is {@link
     * BluetoothProfile#CONNECTION_POLICY_ALLOWED} or disconnects if connectionPolicy is {@link
     * BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}
     *
     * <p>The device should already be paired. Connection policy can be one of: {@link
     * BluetoothProfile#CONNECTION_POLICY_ALLOWED}, {@link
     * BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}, {@link
     * BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device the remote device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true on success, otherwise false
     */
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        Log.d(TAG, "Saved connectionPolicy " + device + " = " + connectionPolicy);
        mDatabaseManager.setProfileConnectionPolicy(
                device, BluetoothProfile.GMAP, connectionPolicy);
        if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return true;
    }

    /** Gets the connection policy for the GMAP service of the given device. */
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    public int getConnectionPolicy(BluetoothDevice device) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        return mDatabaseManager.getProfileConnectionPolicy(device, BluetoothProfile.GMAP);
    }


    private GmapClientStateMachine getOrCreateStateMachine(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "getOrCreateGatt failed: device cannot be null");
            return null;
        }
        synchronized (mStateMachines) {
            GmapClientStateMachine sm = mStateMachines.get(device);
            if (sm != null) {
                return sm;
            }

            Log.d(TAG, "Creating a new state machine for " + device);
            sm = GmapClientStateMachine.make(device, this, mStateMachinesThread.getLooper());
            mStateMachines.put(device, sm);
            return sm;
        }
    }

    /** Process a change in the bonding state for a device */
    public void handleBondStateChanged(BluetoothDevice device, int fromState, int toState) {
        mHandler.post(() -> bondStateChanged(device, toState));
    }

    /**
     * Remove state machine if the bonding for a device is removed
     *
     * @param device the device whose bonding state has changed
     * @param bondState the new bond state for the device. Possible values are: {@link
     *     BluetoothDevice#BOND_NONE}, {@link BluetoothDevice#BOND_BONDING}, {@link
     *     BluetoothDevice#BOND_BONDED}, {@link BluetoothDevice#ERROR}.
     */
    @VisibleForTesting
    void bondStateChanged(BluetoothDevice device, int bondState) {
        Log.d(TAG, "Bond state changed for device: " + device + " state: " + bondState);
        // Remove state machine if the bonding for a device is removed
        if (bondState != BluetoothDevice.BOND_NONE) {
            return;
        }

        synchronized (mStateMachines) {
            GmapClientStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return;
            }
            if (sm.getConnectionState() != BluetoothProfile.STATE_DISCONNECTED) {
                return;
            }
            removeStateMachine(device);
        }
    }

    private void removeStateMachine(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "removeStateMachine failed: device cannot be null");
            return;
        }
        synchronized (mStateMachines) {
            GmapClientStateMachine sm = mStateMachines.remove(device);
            if (sm == null) {
                Log.w(
                        TAG,
                        "removeStateMachine: device " + device + " does not have a state machine");
                return;
            }
            Log.i(TAG, "removeGatt: removing bluetooth gatt for device: " + device);
            sm.doQuit();
            sm.cleanup();
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        for (GmapClientStateMachine sm : mStateMachines.values()) {
            sm.dump(sb);
        }
    }
}
