/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.IBluetoothActivityEnergyInfoListener;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothPreferredAudioProfilesCallback;
import android.bluetooth.IBluetoothQualityReportReadyCallback;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothConnectionCallback;
import android.bluetooth.IBluetoothMetadataListener;
import android.bluetooth.IBluetoothOobDataCallback;
import android.bluetooth.IBluetoothSocketManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothSinkAudioPolicy;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothQualityReport;
import android.bluetooth.IncomingRfcommSocketInfo;
import android.bluetooth.OobData;
import android.content.AttributionSource;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;

/**
 * System private API for talking with the Bluetooth service.
 *
 * {@hide}
 */
interface IBluetooth
{
    int getState();

    oneway void enable(boolean quietMode, in AttributionSource attributionSource);
    oneway void disable(in AttributionSource attributionSource);

    String getAddress(in AttributionSource attributionSource);
    boolean isLogRedactionEnabled();
    List<ParcelUuid> getUuids(in AttributionSource attributionSource);
    boolean setName(in String name, in AttributionSource attributionSource);
    String getIdentityAddress(in String address);
    String getName(in AttributionSource attributionSource);
    int getNameLengthForAdvertise(in AttributionSource attributionSource);

    int getScanMode(in AttributionSource attributionSource);
    int setScanMode(int mode, in AttributionSource attributionSource);

    long getDiscoverableTimeout(in AttributionSource attributionSource);
    int setDiscoverableTimeout(long timeout, in AttributionSource attributionSource);

    boolean startDiscovery(in AttributionSource attributionSource);
    boolean cancelDiscovery(in AttributionSource attributionSource);
    boolean isDiscovering(in AttributionSource attributionSource);
    long getDiscoveryEndMillis(in AttributionSource attributionSource);

    int getAdapterConnectionState();
    int getProfileConnectionState(int profile, in AttributionSource source);

    List<BluetoothDevice> getBondedDevices(in AttributionSource attributionSource);
    boolean createBond(in BluetoothDevice device, in int transport, in OobData p192Data, in OobData p256Data, in AttributionSource attributionSource);
    boolean cancelBondProcess(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean removeBond(in BluetoothDevice device, in AttributionSource attributionSource);
    int getBondState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean isBondingInitiatedLocally(in BluetoothDevice device, in AttributionSource attributionSource);
    long getSupportedProfiles(in AttributionSource attributionSource);
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);
    int getConnectionHandle(in BluetoothDevice device, int transport, in AttributionSource attributionSource);
    String getRemoteName(in BluetoothDevice device, in AttributionSource attributionSource);
    int getRemoteType(in BluetoothDevice device, in AttributionSource attributionSource);
    String getRemoteAlias(in BluetoothDevice device, in AttributionSource attributionSource);
    int setRemoteAlias(in BluetoothDevice device, in String name, in AttributionSource attributionSource);
    int getRemoteClass(in BluetoothDevice device, in AttributionSource attributionSource);
    List<ParcelUuid> getRemoteUuids(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean fetchRemoteUuids(in BluetoothDevice device, in int transport, in AttributionSource attributionSource);
    boolean sdpSearch(in BluetoothDevice device, in ParcelUuid uuid, in AttributionSource attributionSource);
    int getBatteryLevel(in BluetoothDevice device, in AttributionSource attributionSource);
    int getMaxConnectedAudioDevices(in AttributionSource attributionSource);

    boolean setPin(in BluetoothDevice device, boolean accept, int len, in byte[] pinCode, in AttributionSource attributionSource);
    boolean setPasskey(in BluetoothDevice device, boolean accept, int len, in byte[] passkey, in AttributionSource attributionSource);
    boolean setPairingConfirmation(in BluetoothDevice device, boolean accept, in AttributionSource attributionSource);

    int getPhonebookAccessPermission(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setSilenceMode(in BluetoothDevice device, boolean silence, in AttributionSource attributionSource);
    boolean getSilenceMode(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setPhonebookAccessPermission(in BluetoothDevice device, int value, in AttributionSource attributionSource);
    int getMessageAccessPermission(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setMessageAccessPermission(in BluetoothDevice device, int value, in AttributionSource attributionSource);
    int getSimAccessPermission(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setSimAccessPermission(in BluetoothDevice device, int value, in AttributionSource attributionSource);

    oneway void registerCallback(in IBluetoothCallback callback, in AttributionSource attributionSource);
    oneway void unregisterCallback(in IBluetoothCallback callback, in AttributionSource attributionSource);

    // For Socket
    void logL2capcocServerConnection(in BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, long timeoutMillis);

    IBluetoothSocketManager getSocketManager();

    void logL2capcocClientConnection(in BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeNanos, long socketCreationLatencyNanos, long socketConnectionTimeNanos);
    void logRfcommConnectionAttempt(in BluetoothDevice device, boolean isSecured, int resultCode, long socketCreationTimeNanos, boolean isSerialPort);

    boolean factoryReset(in AttributionSource attributionSource);

    boolean isMultiAdvertisementSupported();
    boolean isOffloadedFilteringSupported();
    boolean isOffloadedScanBatchingSupported();
    boolean isActivityAndEnergyReportingSupported();
    boolean isLe2MPhySupported();
    boolean isLeCodedPhySupported();
    boolean isLeExtendedAdvertisingSupported();
    boolean isLePeriodicAdvertisingSupported();
    int isLeAudioSupported();
    int isLeAudioBroadcastSourceSupported();
    int isLeAudioBroadcastAssistantSupported();
    int isDistanceMeasurementSupported(in AttributionSource attributionSource);
    int getLeMaximumAdvertisingDataLength();

    BluetoothActivityEnergyInfo reportActivityInfo(in AttributionSource attributionSource);

    // For Metadata
    boolean registerMetadataListener(in IBluetoothMetadataListener listener, in BluetoothDevice device, in AttributionSource attributionSource);
    boolean unregisterMetadataListener(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setMetadata(in BluetoothDevice device, in int key, in byte[] value, in AttributionSource attributionSource);
    byte[] getMetadata(in BluetoothDevice device, in int key, in AttributionSource attributionSource);

    /**
     * Requests the controller activity info asynchronously.
     * The implementor is expected to reply with the
     * {@link android.bluetooth.BluetoothActivityEnergyInfo} object placed into the Bundle with the
     * key {@link android.os.BatteryStats#RESULT_RECEIVER_CONTROLLER_KEY}.
     * The result code is ignored.
     */
    oneway void requestActivityInfo(in IBluetoothActivityEnergyInfoListener listener, in AttributionSource attributionSource);

    oneway void startBrEdr(in AttributionSource attributionSource);
    oneway void stopBle(in AttributionSource attributionSource);

    int connectAllEnabledProfiles(in BluetoothDevice device, in AttributionSource attributionSource);
    int disconnectAllEnabledProfiles(in BluetoothDevice device, in AttributionSource attributionSource);

    boolean setActiveDevice(in BluetoothDevice device, in int profiles, in AttributionSource attributionSource);
    List<BluetoothDevice> getActiveDevices(in int profile, in AttributionSource attributionSource);
    List<BluetoothDevice> getMostRecentlyConnectedDevices(in AttributionSource attributionSource);
    boolean removeActiveDevice(in int profiles, in AttributionSource attributionSource);

    oneway void registerBluetoothConnectionCallback(in IBluetoothConnectionCallback callback, in AttributionSource attributionSource);
    oneway void unregisterBluetoothConnectionCallback(in IBluetoothConnectionCallback callback, in AttributionSource attributionSource);

    boolean canBondWithoutDialog(in BluetoothDevice device, in AttributionSource attributionSource);
    String getPackageNameOfBondingApplication(in BluetoothDevice device);
    void generateLocalOobData(in int transport, IBluetoothOobDataCallback callback, in AttributionSource attributionSource);

    boolean allowLowLatencyAudio(in boolean allowed, in BluetoothDevice device);

    int isRequestAudioPolicyAsSinkSupported(in BluetoothDevice device, in AttributionSource attributionSource);
    int requestAudioPolicyAsSink(in BluetoothDevice device, in BluetoothSinkAudioPolicy policies, in AttributionSource attributionSource);
    BluetoothSinkAudioPolicy getRequestedAudioPolicyAsSink(in BluetoothDevice device, in AttributionSource attributionSource);

    int startRfcommListener(String name, in ParcelUuid uuid, in PendingIntent intent, in AttributionSource attributionSource);
    int stopRfcommListener(in ParcelUuid uuid, in AttributionSource attributionSource);
    IncomingRfcommSocketInfo retrievePendingSocketForServiceRecord(in ParcelUuid uuid, in AttributionSource attributionSource);

    oneway void setForegroundUserId(in int userId, in AttributionSource attributionSource);

    int setPreferredAudioProfiles(in BluetoothDevice device, in Bundle modeToProfileBundle, in AttributionSource source);
    Bundle getPreferredAudioProfiles(in BluetoothDevice device, in AttributionSource source);
    int registerPreferredAudioProfilesChangedCallback(in IBluetoothPreferredAudioProfilesCallback callback, in AttributionSource attributionSource);
    int unregisterPreferredAudioProfilesChangedCallback(in IBluetoothPreferredAudioProfilesCallback callback, in AttributionSource attributionSource);
    int notifyActiveDeviceChangeApplied(in BluetoothDevice device, in AttributionSource attributionSource);

    int registerBluetoothQualityReportReadyCallback(in IBluetoothQualityReportReadyCallback callback, in AttributionSource attributionSource);
    int unregisterBluetoothQualityReportReadyCallback(in IBluetoothQualityReportReadyCallback callback, in AttributionSource attributionSource);

    int getOffloadedTransportDiscoveryDataScanSupported(in AttributionSource attributionSource);

    boolean isMediaProfileConnected(in AttributionSource attributionSource);

    IBinder getBluetoothGatt();

    IBinder getBluetoothScan();

    oneway void unregAllGattClient(in AttributionSource attributionSource);

    IBinder getProfile(int profile);

    int setActiveAudioDevicePolicy(in BluetoothDevice device, int activeAudioDevicePolicy, in AttributionSource source);

    int getActiveAudioDevicePolicy(in BluetoothDevice device, in AttributionSource source);

    oneway void killBluetoothProcess();
}
