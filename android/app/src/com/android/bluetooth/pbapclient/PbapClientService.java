/*
 * Copyright (c) 2016 The Android Open Source Project
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

package com.android.bluetooth.pbapclient;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import android.accounts.Account;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothPbapClient;
import android.bluetooth.SdpPseRecord;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.CallLog;
import android.sysprop.BluetoothProperties;
import android.util.Log;

import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.hfpclient.HfpClientConnectionService;
import com.android.bluetooth.sdp.SdpManagerNativeInterface;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Provides Bluetooth Phone Book Access Profile Client profile. */
public class PbapClientService extends ProfileService {
    private static final String TAG = PbapClientService.class.getSimpleName();

    // The component name for the owned authenticator service that allows us to make AccountManager
    // Accounts so we can insert into the Contacts DB
    private static final String AUTHENTICATOR_SERVICE =
            PbapClientAccountAuthenticatorService.class.getCanonicalName();

    // Required values for the PBAP Client SDP Record
    private static final String PBAP_CLIENT_SERVICE_NAME = "Phonebook Access PCE";
    private static final int PBAP_CLIENT_VERSION = PbapSdpRecord.VERSION_1_2;

    // MAXIMUM_DEVICES set to 10 to prevent an excessive number of simultaneous devices.
    private static final int MAXIMUM_DEVICES = 10;

    private static PbapClientService sPbapClientService;
    private DatabaseManager mDatabaseManager;
    private int mSdpHandle = -1;
    private final PbapClientContactsStorage mPbapClientContactsStorage;
    private final Map<BluetoothDevice, PbapClientStateMachine> mPbapClientStateMachineMap;

    class PbapClientStateMachineCallback implements PbapClientStateMachine.Callback {
        private final BluetoothDevice mDevice;

        public PbapClientStateMachineCallback(BluetoothDevice device) {
            mDevice = device;
        }

        @Override
        public void onConnectionStateChanged(int oldState, int newState) {
            Log.v(TAG, "Device connection state changed, device=" + mDevice + ", old=" + oldState + ", new=" + newState);
            if (oldState != newState && newState == BluetoothProfile.STATE_DISCONNECTED) {
                removeDevice(mDevice);
            }
        }
    }

    public static boolean isEnabled() {
        return BluetoothProperties.isProfilePbapClientEnabled().orElse(false);
    }

    public PbapClientService(Context context) {
        this(context, new PbapClientContactsStorage(context), new ConcurrentHashMap<BluetoothDevice, PbapClientStateMachine>());
    }

    @VisibleForTesting
    PbapClientService(Context context, PbapClientContactsStorage storage, Map<BluetoothDevice, PbapClientStateMachine> deviceMap) {
        super(context);
        mPbapClientContactsStorage = storage;
        mPbapClientStateMachineMap = deviceMap;
    }

    @Override
    public IProfileServiceBinder initBinder() {
        return new PbapClientBinder(this);
    }

    @Override
    public void start() {
        Log.v(TAG, "onStart");

        mDatabaseManager =
                Objects.requireNonNull(
                        AdapterService.getAdapterService().getDatabase(),
                        "DatabaseManager cannot be null when PbapClientService starts");

        setComponentAvailable(AUTHENTICATOR_SERVICE, true);

        mPbapClientContactsStorage.start();

        registerSdpRecord();
        setPbapClientService(this);
    }

    @Override
    public void stop() {
        Log.v(TAG, "onStop");
        setPbapClientService(null);
        cleanUpSdpRecord();

        // Try to bring down all the connections gracefully
        synchronized (mPbapClientStateMachineMap) {
            for (PbapClientStateMachine sm : mPbapClientStateMachineMap.values()) {
                sm.disconnect();
            }
            mPbapClientStateMachineMap.clear();
        }

        mPbapClientContactsStorage.stop();

        setComponentAvailable(AUTHENTICATOR_SERVICE, false);
    }

    /**
     * Add our PBAP Client SDP record to the device
     *
     * This allows our client to be recognized by the remove device. The record must be cleaned up
     * when we shutdown.
     */
    private void registerSdpRecord() {
        SdpManagerNativeInterface nativeInterface = SdpManagerNativeInterface.getInstance();
        if (!nativeInterface.isAvailable()) {
            Log.e(TAG, "SdpManagerNativeInterface is not available");
            return;
        }
        mSdpHandle =
                nativeInterface.createPbapPceRecord(PBAP_CLIENT_SERVICE_NAME, PBAP_CLIENT_VERSION);
    }

    /**
     * Remove our SDP record
     *
     * Gracefully removes PBAP Client support from our SDP records. Called when shutting down.
     */
    private void cleanUpSdpRecord() {
        if (mSdpHandle < 0) {
            Log.e(TAG, "cleanUpSdpRecord: SDP record never created, nothing to clean up");
            return;
        }
        int sdpHandle = mSdpHandle;
        mSdpHandle = -1;
        SdpManagerNativeInterface nativeInterface = SdpManagerNativeInterface.getInstance();
        if (!nativeInterface.isAvailable()) {
            Log.e(
                    TAG,
                    "cleanUpSdpRecord: Cleanup failed, SdpManagerNativeInterface is not available,"
                            + " sdpHandle="
                            + sdpHandle);
            return;
        }
        Log.i(TAG, "cleanUpSdpRecord: sdpHandle=" + sdpHandle);
        if (!nativeInterface.removeSdpRecord(sdpHandle)) {
            Log.e(TAG, "cleanUpSdpRecord: removal failed, sdpHandle=" + sdpHandle);
        }
    }

    private PbapClientStateMachine getDeviceStateMachine(BluetoothDevice device) {
        synchronized (mPbapClientStateMachineMap) {
            return mPbapClientStateMachineMap.get(device);
        }
    }

    /**
     * Create a state machine for a device
     *
     * PBAP Client connections are always outgoing. This function creates a device state machine
     * instance, which will manage the connection and data lifecycles of the device.
     */
    private boolean addDevice(BluetoothDevice device) {
        Log.d(TAG, "add device, device=" + device);
        synchronized (mPbapClientStateMachineMap) {
            PbapClientStateMachine pbapClientStateMachine = mPbapClientStateMachineMap.get(device);
            if (pbapClientStateMachine == null) {
                if (mPbapClientStateMachineMap.size() >=  MAXIMUM_DEVICES) {
                    Log.w(TAG, "Cannot connect " + device + ", too many devices connected already");
                    return false;
                }
                pbapClientStateMachine = new PbapClientStateMachine(device, mPbapClientContactsStorage, this, new PbapClientStateMachineCallback(device));
                pbapClientStateMachine.start();
                pbapClientStateMachine.connect();
                mPbapClientStateMachineMap.put(device, pbapClientStateMachine);
                return true;
            } else {
                Log.w(TAG, "Cannot connect " + device + ", already connecting/connected.");
                return false;
            }
        }
    }

    /**
     * Remove a device state machine, if it exists
     *
     * When a device disconnects, we gracefully clean up its state machine instance and drop our
     * reference to it. State machines cannot be reused, so this must be deleted before a device can
     * reconnect.
     */
    private void removeDevice(BluetoothDevice device) {
        Log.d(TAG, "remove device, device=" + device);
        synchronized (mPbapClientStateMachineMap) {
            PbapClientStateMachine pbapClientStateMachine = mPbapClientStateMachineMap.get(device);
            if (pbapClientStateMachine != null) {
                int state = pbapClientStateMachine.getConnectionState();
                if (state != BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG, "Removing connected device, device=" + device + ", state=" + state);
                }
                mPbapClientStateMachineMap.remove(device);
            }
        }
    }

    /**
     * Delete call logs related to phone calls made while we were connected on HFP.
     *
     * When HFP disconnects, we want to preserve the PBAP contacts, but remove the calls that exist.
     */
    // TODO: Should something like this move to HFP? This seems weird to handle here
    private void removeHfpCallLog(BluetoothDevice device) {
        Log.d(TAG, "Removing call logs from " + device);
        // Delete call logs belonging to accountName==BD_ADDR that also match
        // component name "hfpclient".
        // ComponentName componentName = new ComponentName(context, HfpClientConnectionService.class);
        // String selectionFilter =
        //         CallLog.Calls.PHONE_ACCOUNT_ID
        //                 + "=? AND "
        //                 + CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME
        //                 + "=?";
        // String[] selectionArgs = new String[] {accountName, componentName.flattenToString()};
        // try {
        //     getContentResolver().delete(CallLog.Calls.CONTENT_URI, selectionFilter, selectionArgs);
        // } catch (IllegalArgumentException e) {
        //     Log.w(TAG, "Call Logs could not be deleted, they may not exist yet.");
        // }
        Account account = mPbapClientContactsStorage.getStorageAccountForDevice(device);
        mPbapClientContactsStorage.removeCallHistory(account);
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        ProfileService.println(sb, "Client Version: " + PbapSdpRecord.versionToString(PBAP_CLIENT_VERSION));
        synchronized (mPbapClientStateMachineMap) {
            ProfileService.println(sb, "Devices (" + mPbapClientStateMachineMap.size() + "/ " + MAXIMUM_DEVICES + ")");
            for (PbapClientStateMachine stateMachine : mPbapClientStateMachineMap.values()) {
                stateMachine.dump(sb);
                ProfileService.println(sb, "");
            }
        }
        ProfileService.println(sb, mPbapClientContactsStorage.dump());
    }

    /**********************************************************************************************
     * Events from AdapterService
     *********************************************************************************************/

    /*
     * Get notified of incoming ACL disconnections
     *
     * OBEX client's are supposed to be in control of the connection lifecycle, and servers are not
     * supposed to disconnect OBEX sessions. Despite this, its normal/possible the remote device to
     * tear down connections at lower levels than OBEX, mainly the L2CAP/RFCOMM links or the ACL.
     * The OBEX framework isn't setup to be notified of these disconnections, so we must listen for
     * them separately and clean up the device connection and, if necessary, data when this happens.
     */
    public void aclDisconnected(BluetoothDevice device, int transport) {
        Log.i(
                TAG,
                "Received ACL disconnection event, device="
                        + device.toString()
                        + ", transport="
                        + aclTransportToString(transport));

        if (transport == BluetoothDevice.TRANSPORT_BREDR) {
            disconnect(device);
        }
    }

    /**
     * Ensure that after HFP disconnects, we remove call logs.
     *
     * This addresses the situation when PBAP was never connected while calls were made. The goal is
     * remove Bluetooth related call logs when the device goes away.
     */
    public void handleHeadsetClientConnectionStateChanged(
            BluetoothDevice device, int oldState, int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Received intent to disconnect HFP with " + device);
            // HFP client stores entries in calllog.db by BD_ADDR and component name
            // Using the current Service as the context.
            removeHfpCallLog(device);
        }
    }

    /*
     * Get notified of incoming SDP records
     *
     * This function looks for PBAP Server records coming from remote devices, and forwards them to
     * the appropriate device's state machine instance for processing. SDP records are used to
     * determine which L2CAP/RFCOMM psm/channel to connect on, as well as which phonebooks to expect
     */
    public void receiveSdpSearchRecord(
            BluetoothDevice device, int status, Parcelable record, ParcelUuid uuid) {
        Log.v(TAG, "Received SDP record for UUID=" + uuid.toString() + " (expected UUID="
                + BluetoothUuid.PBAP_PSE.toString() + ")");
        if (uuid.equals(BluetoothUuid.PBAP_PSE)) {
            PbapClientStateMachine stateMachine = getDeviceStateMachine(device);
            if (stateMachine == null) {
                Log.e(TAG, "No Statemachine found for the device=" + device.toString());
                return;
            }
            SdpPseRecord pseRecord = (SdpPseRecord) record;
            if (pseRecord != null) {
                stateMachine.onSdpRecordReceived(new PbapSdpRecord(device, pseRecord));
            } else {
                Log.w(TAG, "Received record null PSE record for device=" + device);
            }
        }
    }

    /**********************************************************************************************
     * API methods
     *********************************************************************************************/

    /**
     * Get the singleton instance of PbapClientService, if one exists
     */
    public static synchronized PbapClientService getPbapClientService() {
        if (sPbapClientService == null) {
            Log.w(TAG, "getPbapClientService(): service is null");
            return null;
        }
        if (!sPbapClientService.isAvailable()) {
            Log.w(TAG, "getPbapClientService(): service is not available");
            return null;
        }
        return sPbapClientService;
    }

    /**
     * Set the singleton instance of PbapClientService
     *
     * This function is meant to be used by tests only.
     */
    @VisibleForTesting
    static synchronized void setPbapClientService(PbapClientService instance) {
        Log.v(TAG, "setPbapClientService(): set to: " + instance);
        sPbapClientService = instance;
    }

    /**
     * Requests a connection to the given device's PBAP Server
     *
     * @param device is the device with which we will connect to
     * @return true if we successfully begin the connection process, false otherwise
     */
    public boolean connect(BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Null device");
        }
        Log.d(TAG, "Received request to Connect, device=" + device.getAddress());
        if (getConnectionPolicy(device) <= BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            return false;
        }
        return addDevice(device);
    }

    /**
     * Disconnects the pbap client profile from the passed in device
     *
     * @param device is the device with which we will disconnect the pbap client profile
     * @return true if we disconnected the pbap client profile, false otherwise
     */
    public boolean disconnect(BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Null device");
        }
        PbapClientStateMachine pbapClientStateMachine = getDeviceStateMachine(device);
        if (pbapClientStateMachine != null) {
            pbapClientStateMachine.disconnect();
            return true;
        } else {
            Log.w(TAG, "disconnect() called on unconnected device.");
            return false;
        }
    }

    /**
     * Get the list of PBAP Server devices this PBAP Client device is connected to
     *
     * @return The list of connected PBAP Server devices
     */
    public List<BluetoothDevice> getConnectedDevices() {
        int[] desiredStates = {BluetoothProfile.STATE_CONNECTED};
        return getDevicesMatchingConnectionStates(desiredStates);
    }

    /**
     * Get the list of PBAP Server devices this PBAP Client device know about, who are in a given
     * state.
     *
     * @param states The array of BluutoothProfile states you want to match on
     * @return The list of connected PBAP Server devices
     */
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>(0);
        synchronized (mPbapClientStateMachineMap) {
            for (Map.Entry<BluetoothDevice, PbapClientStateMachine> stateMachineEntry :
                    mPbapClientStateMachineMap.entrySet()) {
                int currentDeviceState = stateMachineEntry.getValue().getConnectionState();
                for (int state : states) {
                    if (currentDeviceState == state) {
                        deviceList.add(stateMachineEntry.getKey());
                        break;
                    }
                }
            }
        }
        return deviceList;
    }

    /**
     * Get the current connection state of the profile
     *
     * @param device is the remote bluetooth device
     * @return {@link BluetoothProfile#STATE_DISCONNECTED} if this profile is disconnected, {@link
     *     BluetoothProfile#STATE_CONNECTING} if this profile is being connected, {@link
     *     BluetoothProfile#STATE_CONNECTED} if this profile is connected, or {@link
     *     BluetoothProfile#STATE_DISCONNECTING} if this profile is being disconnected
     */
    public int getConnectionState(BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Null device");
        }
        PbapClientStateMachine pbapClientStateMachine = getDeviceStateMachine(device);
        if (pbapClientStateMachine == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        } else {
            return pbapClientStateMachine.getConnectionState();
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
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     */
    public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy) {
        if (device == null) {
            throw new IllegalArgumentException("Null device");
        }
        Log.d(TAG, "Saved connectionPolicy " + device + " = " + connectionPolicy);

        if (!mDatabaseManager.setProfileConnectionPolicy(
                device, BluetoothProfile.PBAP_CLIENT, connectionPolicy)) {
            return false;
        }
        if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            // TODO: When caching, remove the device account and data for this user here too.
            disconnect(device);
        }
        return true;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p>The connection policy can be any of: {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED},
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}, {@link
     * BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     */
    public int getConnectionPolicy(BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Null device");
        }
        return mDatabaseManager.getProfileConnectionPolicy(device, BluetoothProfile.PBAP_CLIENT);
    }

    /**********************************************************************************************
     * Utility methods
     *********************************************************************************************/

    public static String aclTransportToString(int transport) {
        switch (transport) {
            case BluetoothDevice.TRANSPORT_AUTO:
                return "TRANSPORT_AUTO";
            case BluetoothDevice.TRANSPORT_BREDR:
                return "TRANSPORT_BREDR";
            case BluetoothDevice.TRANSPORT_LE:
                return "TRANSPORT_LE";
            default:
                return "TRANSPORT_UNKNOWN (" + transport + ")";
        }
    }
}
