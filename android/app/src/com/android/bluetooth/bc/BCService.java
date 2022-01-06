/*
 * Copyright (c) 2020 The Linux Foundation. All rights reserved.
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.bluetooth.bc;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

//import android.content.AttributionSource;
import com.android.bluetooth.Utils;
import android.os.UserHandle;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothManager;
//import android.bluetooth.IBleBroadcastAudioScanAssistCallback;
import android.bluetooth.IBluetoothLeBroadcastAssistantCallback;
//import android.bluetooth.BluetoothSyncHelper;
import android.bluetooth.BluetoothLeBroadcastSourceInfo;
//import android.bluetooth.BleBroadcastSourceChannel;
//import android.bluetooth.BleBroadcastAudioScanAssistManager;
import android.bluetooth.IBluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastAssistantCallback;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.HandlerThread;
import android.util.Log;
import android.os.ParcelUuid;
import android.bluetooth.BluetoothUuid;
import java.util.ArrayList;
import android.os.ServiceManager;

import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.PeriodicAdvertisingCallback;
import android.bluetooth.le.PeriodicAdvertisingManager;
import android.bluetooth.le.PeriodicAdvertisingReport;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothStatusCodes;

import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.bluetooth.btservice.AdapterService;
/*_CSIP
//CSIP related imports
import com.android.bluetooth.groupclient.GroupService;
import android.bluetooth.BluetoothGroupCallback;
import android.bluetooth.DeviceGroup;
_CSIP*/

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.NoSuchElementException;
import android.os.SystemProperties;


import com.android.internal.util.ArrayUtils;
/** @hide */
public class BCService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = BCService.class.getSimpleName();

    private static final ParcelUuid CAS_UUID = null;
//ParcelUuid.fromString("000089FF-0000-1000-8000-00805F9B34FB");

    public static final String BC_ID = "0000184F-0000-1000-8000-00805F9B34FB";
    public static final String BS_ID = "00001852-0000-1000-8000-00805F9B34FB";
    private static BCService sBCService;
    private static final int MAX_BASS_CLIENT_STATE_MACHINES = 10;
    private static final int MAX_BASS_CLIENT_CSET_MEMBERS = 10;
    private final Map<BluetoothDevice, BassClientStateMachine> mStateMachines =
             new HashMap<>();
    private HandlerThread mStateMachinesThread;
/*
    private final Map<Integer, BassCsetManager> mSetManagers =
             new HashMap<>();
*/
    private HandlerThread mSetManagerThread;

    private AdapterService mAdapterService;

    private Map<BluetoothDevice, ArrayList<IBluetoothLeBroadcastAssistantCallback>> mAppCallbackMap =
             new HashMap<BluetoothDevice, ArrayList<IBluetoothLeBroadcastAssistantCallback>>();

    private BassUtils bassUtils = null;
    public static final int INVALID_SYNC_HANDLE = -1;
    public static final int INVALID_ADV_SID = -1;
    public static final int INVALID_ADV_ADDRESS_TYPE = -1;
    public static final int INVALID_ADV_INTERVAL = -1;
    public static final int INVALID_BROADCAST_ID = -1;
    private Map<BluetoothDevice, BluetoothDevice> mActiveSourceMap;

    /*_CSIP
    //CSET interfaces
    private GroupService mSetCoordinator = GroupService.getGroupService();
    public int mCsipAppId = -1;
    private int mQueuedOps = 0;
    _CSIP*/

    /*Caching the PAresults from Broadcast source*/
    /*This is stored at service so that each device state machine can access
    and use it as needed. Once the periodic sync in cancelled, this data will bre
    removed to ensure stable data won't used*/
    /*broadcastSrcDevice, syncHandle*/
    private Map<BluetoothDevice, Integer> mSyncHandleMap;
    /*syncHandle, parsed BaseData data*/
    private Map<Integer, BaseData> mSyncHandleVsBaseInfo;
    /*bcastSrcDevice, corresponding PAResultsMap*/
    private Map<BluetoothDevice, PAResults> mPAResultsMap;
    public class PAResults {
        public BluetoothDevice mDevice;
        public int mAddressType;
        public int mAdvSid;
        public int mSyncHandle;
        public byte metaDataLength;
        public byte[] metaData;
        public int mPAInterval;
        public int mBroadcastId;

        PAResults(BluetoothDevice device, int addressType,
                         int syncHandle, int advSid, int paInterval, int broadcastId) {
            mDevice = device;
            mAddressType = addressType;
            mAdvSid = advSid;
            mSyncHandle = syncHandle;
            mPAInterval = paInterval;
            mBroadcastId = broadcastId;
        }

        public void updateSyncHandle(int syncHandle) {
            mSyncHandle = syncHandle;
        }

        public void updateAdvSid(int advSid) {
            mAdvSid = advSid;
        }

        public void updateAddressType(int addressType) {
            mAddressType = addressType;
        }

        public void updateAdvInterval(int advInterval) {
            mPAInterval = advInterval;
        }

        public void updateBroadcastId(int broadcastId) {
            mBroadcastId = broadcastId;
        }

        public void print() {
            log("-- PAResults --");
            log("mDevice:" + mDevice);
            log("mAddressType:" + mAddressType);
            log("mAdvSid:" + mAdvSid);
            log("mSyncHandle:" + mSyncHandle);
            log("mPAInterval:" + mPAInterval);
            log("mBroadcastId:" + mBroadcastId);
            log("-- END: PAResults --");
        }
    };

    public void updatePAResultsMap(BluetoothDevice device, int addressType, int syncHandle,
                                    int advSid, int advInterval, int bId) {
          log("updatePAResultsMap: device: " + device);
          log("updatePAResultsMap: syncHandle: " + syncHandle);
          log("updatePAResultsMap: advSid: " + advSid);
          log("updatePAResultsMap: addressType: " + addressType);
          log("updatePAResultsMap: advInterval: " + advInterval);
          log("updatePAResultsMap: broadcastId: " + bId);
          log("mSyncHandleMap" + mSyncHandleMap);
          log("mPAResultsMap" + mPAResultsMap);
          //Cache the SyncHandle
          if (mSyncHandleMap != null) {
              Integer i = new Integer(syncHandle);
              mSyncHandleMap.put(device, i);
          }
          if (mPAResultsMap != null) {
              PAResults paRes = mPAResultsMap.get(device);
              if (paRes == null) {
                  log("PAResmap: add >>>");
                  paRes = new PAResults (device, addressType,
                              syncHandle, advSid, advInterval, bId);
                  if (paRes != null) {
                      paRes.print();
                      mPAResultsMap.put(device, paRes);
                  }
              } else {
                  if (advSid != INVALID_ADV_SID) {
                      paRes.updateAdvSid(advSid);
                  }
                  if (syncHandle != INVALID_SYNC_HANDLE) {
                      paRes.updateSyncHandle(syncHandle);
                  }
                  if (addressType != INVALID_ADV_ADDRESS_TYPE) {
                      paRes.updateAddressType(addressType);
                  }
                  if (advInterval != INVALID_ADV_INTERVAL) {
                      paRes.updateAdvInterval(advInterval);
                  }
                  if (bId != INVALID_BROADCAST_ID) {
                      paRes.updateBroadcastId(bId);
                  }
                  log("PAResmap: update >>>");
                  paRes.print();
                  mPAResultsMap.replace(device, paRes);
              }
          }
          log(">>mPAResultsMap" + mPAResultsMap);
      }

      public PAResults getPAResults(BluetoothDevice device) {
          PAResults res = null;
          if (mPAResultsMap != null) {
            res = mPAResultsMap.get(device);
          } else {
            Log.e(TAG, "getPAResults: mPAResultsMap is null");
        }
        return res;
      }
      public PAResults clearPAResults(BluetoothDevice device) {
          PAResults res = null;
          if (mPAResultsMap != null) {
            res = mPAResultsMap.remove(device);
          } else {
            Log.e(TAG, "getPAResults: mPAResultsMap is null");
        }
        return res;
      }

      public void updateBASE(int syncHandlemap, BaseData base) {
         if (mSyncHandleVsBaseInfo != null) {
             log("updateBASE : mSyncHandleVsBaseInfo>>");
             mSyncHandleVsBaseInfo.put(syncHandlemap, base);
         } else {
             Log.e(TAG, "updateBASE: mSyncHandleVsBaseInfo is null");
         }
      }

    public BaseData getBASE(int syncHandlemap) {
        BaseData base = null;
        if (mSyncHandleVsBaseInfo != null) {
            log("getBASE : syncHandlemap::" + syncHandlemap);
            base = mSyncHandleVsBaseInfo.get(syncHandlemap);
        } else {
            Log.e(TAG, "getBASE: mSyncHandleVsBaseInfo is null");
        }
        log("getBASE returns" + base);
        return base;
    }

    public void clearBASE(int syncHandlemap) {
        if (mSyncHandleVsBaseInfo != null) {
            log("clearBASE : mSyncHandleVsBaseInfo>>");
            mSyncHandleVsBaseInfo.remove(syncHandlemap);
        } else {
            Log.e(TAG, "updateBASE: mSyncHandleVsBaseInfo is null");
        }
    }

    public void setActiveSyncedSource(BluetoothDevice scanDelegator, BluetoothDevice sourceDevice) {
        log("setActiveSyncedSource: scanDelegator" + scanDelegator + ":: sourceDevice:" +
                sourceDevice);
        if (sourceDevice == null) {
            mActiveSourceMap.remove(scanDelegator);
        } else {
            mActiveSourceMap.put(scanDelegator, sourceDevice);
        }
    }

    public BluetoothDevice getActiveSyncedSource(BluetoothDevice scanDelegator) {
        BluetoothDevice currentSource =  mActiveSourceMap.get(scanDelegator);
        log("getActiveSyncedSource: scanDelegator" + scanDelegator + "returning " + currentSource);
        return currentSource;
    }

     @Override
     protected IProfileServiceBinder initBinder() {
         return new BluetoothLeBroadcastAssistantBinder(this);
     }

     /*_CSIP
     private BluetoothGroupCallback mBluetoothGroupCallback = new BluetoothGroupCallback() {
          public void onGroupClientAppRegistered(int status, int appId) {
              log("onCsipAppRegistered:" + status + "appId: " + appId);
              if (status == 0) {
                  mCsipAppId = appId;
              } else {
                  Log.e(TAG, "Csip registeration failed, status:" + status);
              }
          }

          public void onConnectionStateChanged (int state, BluetoothDevice device) {
              log("onConnectionStateChanged: Device: " + device + "state: " + state);
                  //notify the statemachine about CSIP connection
                  synchronized (mStateMachines) {
                      BassClientStateMachine stateMachine = getOrCreateStateMachine(device);
                      Message m = stateMachine.obtainMessage(
                                    BassClientStateMachine.CSIP_CONNECTION_STATE_CHANGED);
                      m.obj = state;
                      stateMachine.sendMessage(m);
                  }
          }

          public void onNewGroupFound (int setId,  BluetoothDevice device, UUID uuid) {     }
          public void onGroupDiscoveryStatusChanged (int setId, int status, int reason) {    }
          public void onGroupDeviceFound (int setId, BluetoothDevice device) {    }
          public void onExclusiveAccessChanged (int setId, int value, int status,
                                        List<BluetoothDevice> devices) {
              log("onLockStatusChanged: setId" + setId + devices + "status:" + status);
              BassCsetManager setMgr = null;
              setMgr = getOrCreateCSetManager(setId, null);
              if (setMgr == null) {
                      return;
              }
              log ("sending Lock status to setId:" + setId);
              Message m = setMgr.obtainMessage(BassCsetManager.LOCK_STATE_CHANGED);
              m.obj = devices;
              m.arg1 = value;
              setMgr.sendMessage(m);
          }
          public void onExclusiveAccessStatusFetched (int setId, int lockStatus) {    }
          public void onExclusiveAccessAvailable (int setId, BluetoothDevice device) {    }
     };
     _CSIP*/

     @Override
     protected boolean start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }
        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                 "AdapterService cannot be null when BCService starts");
         mStateMachines.clear();
         mStateMachinesThread = new HandlerThread("BCService.StateMachines");
         mStateMachinesThread.start();

         //mSetManagers.clear();
         mSetManagerThread = new HandlerThread("BCService.SetManagers");
         mSetManagerThread.start();

         setBCService(this);
         bassUtils = new BassUtils(this);
         //Saving PSync stuff for future addition
         mSyncHandleMap = new HashMap<BluetoothDevice, Integer>();
         mPAResultsMap = new HashMap<BluetoothDevice, PAResults>();
         mSyncHandleVsBaseInfo = new HashMap<Integer, BaseData>();
         mActiveSourceMap = new HashMap<BluetoothDevice, BluetoothDevice>();

         /*_CSIP
         //CSET initialization
         mSetCoordinator = GroupService.getGroupService();
         if (mSetCoordinator != null) {
             mSetCoordinator.registerGroupClientModule(mBluetoothGroupCallback);
         }
         _CSIP*/
         /*_PACS
         mPacsClientService = PacsClientService.getPacsClientService();
         _PACS*/

         /*_GAP
         //GAP registeration for Bass UUID notification
         if (mAdapterService != null) {
             log("register for BASS UUID notif");
            ParcelUuid bassUuid = new ParcelUuid(BassClientStateMachine.BASS_UUID);
            mAdapterService.registerUuidSrvcDisc(bassUuid);
         }
         _GAP*/
         return true;
     }

    @Override
    protected boolean stop() {
        if (DBG) {
            Log.d(TAG, "stop()");
        }

        synchronized (mStateMachines) {
             for (BassClientStateMachine sm : mStateMachines.values()) {
                 sm.doQuit();
                 sm.cleanup();
             }
             mStateMachines.clear();
        }

        if (mStateMachinesThread != null) {
             mStateMachinesThread.quitSafely();
             mStateMachinesThread = null;
        }

        if (mSetManagerThread != null) {
             mSetManagerThread.quitSafely();
             mSetManagerThread = null;
        }

        setBCService(null);

        if (mAppCallbackMap != null) {
            mAppCallbackMap.clear();
            mAppCallbackMap = null;
        }

        if (mSyncHandleMap != null) {
            mSyncHandleMap.clear();
            mSyncHandleMap = null;
        }

        if (mActiveSourceMap != null) {
            mActiveSourceMap.clear();
            mActiveSourceMap = null;
        }
        /*_CSIP
        if (mSetCoordinator != null && mCsipAppId != -1) {
           //mSetCoordinator.unregisterGroupClientModule(mCsipAppId);
        }
        _CSIP*/
        return true;
     }

     @Override
     public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Need to unregister app");
        //unregisterApp();
        return super.onUnbind(intent);
    }

    /**
     * Get the BCService instance
     * @return BCService instance
     */
    public static synchronized BCService getBCService() {
        if (sBCService == null) {
            Log.w(TAG, "getBCService(): service is NULL");
            return null;
        }

        if (!sBCService.isAvailable()) {
            Log.w(TAG, "getBCService(): service is not available");
            return null;
        }
        return sBCService;
    }

    public BassUtils getBassUtils() {
        return bassUtils;
    }

    public BluetoothDevice getDeviceForSyncHandle(int syncHandle) {
        BluetoothDevice dev = null;
        if (mSyncHandleMap != null) {
            for (Map.Entry<BluetoothDevice, Integer> entry : mSyncHandleMap.entrySet()) {
                Integer value = entry.getValue();
                if (value == syncHandle) {
                    dev = entry.getKey();
                }
            }
        }
        return dev;
    }

    private static synchronized void setBCService(BCService instance) {
        if (DBG) {
            Log.d(TAG, "setBCService(): set to: " + instance);
        }
        sBCService = instance;
    }

    /**
     * Connects the bass profile to the passed in device
     *
     * @param device is the device with which we will connect the Bass profile
     * @return true if BAss profile successfully connected, false otherwise
     */
    public boolean connect(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "connect(): " + device);
        }
        if (device == null) {
            return false;
        }

        if (getConnectionPolicy(device) == BluetoothProfile.CONNECTION_POLICY_UNKNOWN) {
            return false;
        }
        synchronized (mStateMachines) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(device);

            stateMachine.sendMessage(BassClientStateMachine.CONNECT);
        }
        return true;
    }

  /**
     * Disconnects Bassclient profile for the passed in device
     *
     * @param device is the device with which we want to disconnected the BAss client profile
     * @return true if Bass client profile successfully disconnected, false otherwise
     */
    public boolean disconnect(BluetoothDevice device) {

        if (DBG) {
            Log.d(TAG, "disconnect(): " + device);
        }
        if (device == null) {
            return false;
        }

      synchronized (mStateMachines) {
          BassClientStateMachine stateMachine = getOrCreateStateMachine(device);

          stateMachine.sendMessage(BassClientStateMachine.DISCONNECT);
        }
        return true;
    }

    List<BluetoothDevice> getConnectedDevices() {

        synchronized (mStateMachines) {
            List<BluetoothDevice> devices = new ArrayList<>();
            for (BassClientStateMachine sm : mStateMachines.values()) {
                if (sm.isConnected()) {
                    devices.add(sm.getDevice());
                }
            }
            log("getConnectedDevices: " + devices);
            return devices;
        }
    }

    /**
     * Check whether can connect to a peer device.
     * The check considers a number of factors during the evaluation.
     *
     * @param device the peer device to connect to
     * @return true if connection is allowed, otherwise false
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean okToConnect(BluetoothDevice device) {
        // Check if this is an incoming connection in Quiet mode.
        if (mAdapterService.isQuietModeEnabled()) {
            Log.e(TAG, "okToConnect: cannot connect to " + device + " : quiet mode enabled");
            return false;
        }
        // Check connection policy and accept or reject the connection.
        int connectionPolicy = getConnectionPolicy(device);
        int bondState = mAdapterService.getBondState(device);
        // Allow this connection only if the device is bonded. Any attempt to connect while
        // bonding would potentially lead to an unauthorized connection.
        if (bondState != BluetoothDevice.BOND_BONDED) {
            Log.w(TAG, "okToConnect: return false, bondState=" + bondState);
            return false;
        } else if (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_UNKNOWN
                && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            // Otherwise, reject the connection if connectionPolicy is not valid.
            Log.w(TAG, "okToConnect: return false, connectionPolicy=" + connectionPolicy);
            return false;
        }
        return true;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {

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
                final ParcelUuid[] featureUuids = device.getUuids();
                if (!ArrayUtils.contains(featureUuids,
                        new ParcelUuid(BassClientStateMachine.BASS_UUID))) {
                    continue;
                }
                int connectionState = BluetoothProfile.STATE_DISCONNECTED;
                BassClientStateMachine sm = getOrCreateStateMachine(device);
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

    public int getConnectionState(BluetoothDevice device) {
        synchronized (mStateMachines) {
            BassClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                log("getConnectionState returns STATE_DISC");
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getConnectionState();
        }
    }

    /**
     * Set the connectionPolicy of the Hearing Aid profile.
     *
     * @param device the remote device
     * @param connectionPolicy the connection policy of the profile
     * @return true on success, otherwise false
     */
    public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy) {

        if (DBG) {
            Log.d(TAG, "Saved connectionPolicy " + device + " = " + connectionPolicy);
        }
        boolean setSuccessfully;
        setSuccessfully = mAdapterService.getDatabase()
                .setProfileConnectionPolicy(device, 
                        BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT,
                        connectionPolicy);
        if (setSuccessfully && connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (setSuccessfully
                && connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return setSuccessfully;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED},
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN},
     * {@link BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    public int getConnectionPolicy(BluetoothDevice device) {

        return mAdapterService.getDatabase()
                .getProfileConnectionPolicy(device,
                    BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
    }

    public void sendBroadcastSourceSelectedCallback(BluetoothDevice device,
                        /*List<BleBroadcastSourceChannel> bChannels, */int status){
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        PAResults paRes;

        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }
        if (mPAResultsMap == null || (paRes = mPAResultsMap.get(device)) == null) {
            Log.e(TAG, "No PA result found for device " + device);
            return;
        }
        BluetoothLeBroadcastSourceInfo scanResSI = new BluetoothLeBroadcastSourceInfo(
                                    BassClientStateMachine.INVALID_SRC_ID);
        scanResSI.setSourceDevice(device);
        scanResSI.setAdvAddressType(paRes.mAddressType);
        scanResSI.setAdvertisingSid((byte)paRes.mAdvSid);
        scanResSI.setBroadcastId(paRes.mBroadcastId);

        for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
            try {
               cb.onBluetoothLeBroadcastSourceSelected(scanResSI, status/*, bChannels*/);
            } catch (RemoteException e)  {
               Log.e(TAG, "Exception while calling sendBroadcastSourceSelectedCallback");
            }
        }
    }

    public void sendAddBroadcastSourceCallback(BluetoothDevice device,
                    BluetoothLeBroadcastSourceInfo source, int status){
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }
        for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
            try {
               cb.onBluetoothLeBroadcastSourceAdded(device, source, status);
            } catch (RemoteException e)  {
               Log.e(TAG, "Exception while calling onBluetoothLeBroadcastSourceAdded");
            }
        }
    }

    public void sendUpdateBroadcastSourceCallback(BluetoothDevice device,
                    BluetoothLeBroadcastSourceInfo source,
                    int status){
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

        for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
            try {
                cb.onBluetoothLeBroadcastSourceUpdated(device, source, status);
            } catch (RemoteException e)  {
                Log.e(TAG, "Exception while calling onBluetoothLeBroadcastSourceUpdated");
            }
        }
    }
    public void sendRemoveBroadcastSourceCallback(BluetoothDevice device,
                    BluetoothLeBroadcastSourceInfo source,
                    int status){
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

        for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
            try {
                cb.onBluetoothLeBroadcastSourceRemoved(device, source, status);
            } catch (RemoteException e)  {
                Log.e(TAG, "Exception while calling onBluetoothLeBroadcastSourceRemoved");
            }
        }
    }
    public void sendSetBroadcastPINupdatedCallback(BluetoothDevice device,
                    BluetoothLeBroadcastSourceInfo source,
                    int status){
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

/*
        for (IBluetoothLeBroadcastAssistantCallback cb : cbs) {
            try {
                cb.onBleBroadcastPinUpdated(device, sourceId, status);
            } catch (RemoteException e)  {
                Log.e(TAG, "Exception while calling onBleBroadcastPinUpdated");
            }
        }
*/
    }

    public void registerCallback (BluetoothDevice device,
                    IBluetoothLeBroadcastAssistantCallback cb) {

        Log.i(TAG, "registerCallback" + device);

        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.i(TAG, "registerCallback: entry exists");
            cbs = new ArrayList<IBluetoothLeBroadcastAssistantCallback>();
        }
        cbs.add(cb);
        mAppCallbackMap.put(device, cbs);
        return;
    }

    public void unregisterCallback (BluetoothDevice device,
                IBluetoothLeBroadcastAssistantCallback cb) {

        Log.i(TAG, "unregisterCallback" + device);

        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.i(TAG, "unregisterCallback: cb list is null");
            return;
        } else {
           boolean ret = cbs.remove(cb);
           Log.i(TAG, "unregisterCallback: ret value of removal from list:" + ret);
        }
        if (cbs.size() != 0) {
            mAppCallbackMap.replace(device, cbs);
        } else {
            Log.i(TAG, "unregisterCallback: Remove the cmplete entry");
            mAppCallbackMap.remove(device);
        }
        return;
    }

    public boolean searchforBroadcastSources (BluetoothDevice device) {

        Log.i(TAG, "searchforBroadcastSources on behalf of" + device);
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return false;
        }
        boolean ret = false;
        if (bassUtils != null) {
            ret = bassUtils.searchforBroadcastSources(device, cbs);
        } else {
            Log.e(TAG, "searchforBroadcastSources :Null Bass Util Handle" + device);
            ret = false;
        }
        return ret;
    }

    public boolean stopSearchforBroadcastSources (BluetoothDevice device) {

        Log.i(TAG, "stopsearchforBroadcastSources on behalf of" + device);
        ArrayList<IBluetoothLeBroadcastAssistantCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
        }
        boolean ret = false;
        if (bassUtils != null) {
            ret = bassUtils.stopSearchforBroadcastSources(device, cbs);
        } else {
            Log.e(TAG, "stopsearchforBroadcastSources :Null Bass Util Handle" + device);
            ret = false;
        }
        return ret;
    }

    public boolean selectBroadcastSource (BluetoothDevice device, ScanResult scanRes,
                boolean isGroupOp, boolean auto) {

        Log.i(TAG, "selectBroadcastSource for " + device + "isGroupOp:" + isGroupOp);
        Log.i(TAG, "ScanResult " + scanRes);

        if (scanRes == null) {
            Log.e(TAG, "selectBroadcastSource: null Scan results");
            return false;
        }
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        listOfDevices.add(device);
        if (isRoomForBroadcastSourceAddition(listOfDevices) == false) {
            sendBroadcastSourceSelectedCallback(device, /*null,*/
                BluetoothLeBroadcastAssistantCallback.BASS_STATUS_NO_EMPTY_SLOT);
            return false;
        }
        // BleSourceInfo from scanRes
        BluetoothLeBroadcastSourceInfo scanResSI = new BluetoothLeBroadcastSourceInfo (
                BassClientStateMachine.INVALID_SRC_ID);
        scanResSI.setSourceDevice(scanRes.getDevice());
        scanResSI.setAdvAddressType(scanRes.getAddressType());
        scanResSI.setAdvertisingSid((byte)scanRes.getAdvertisingSid());

        if (isValidBroadcastSourceAddition(listOfDevices, scanResSI) == false) {
            sendBroadcastSourceSelectedCallback(device,
                /*null, */BluetoothLeBroadcastAssistantCallback.BASS_STATUS_DUPLICATE_ADDITION);
            return false;
        }
        startScanOffloadInternal(device, isGroupOp);
        synchronized (mStateMachines) {
            BassClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                return false;
            }
            Message m = sm.obtainMessage(BassClientStateMachine.SELECT_BCAST_SOURCE);
            m.obj = scanRes;
            if (auto) {
                m.arg1 = sm.AUTO;
            } else {
                m.arg1 = sm.USER;
            }
            if (isGroupOp) {
                m.arg2 = sm.GROUP_OP;
            } else {
                m.arg2 = sm.NON_GROUP_OP;
            }
            sm.sendMessage(m);
        }
        return true;
    }

/*
    public synchronized void notifyOperationCompletion(
            BluetoothDevice device, int pendingOperation) {
        log("notifyOperationCompletion: " + device + "pendingOperation: " +
            BassClientStateMachine.messageWhatToString(pendingOperation));
        //synchronized (mStateMachines) {
            switch (pendingOperation) {
                case BassClientStateMachine.START_SCAN_OFFLOAD:
                case BassClientStateMachine.STOP_SCAN_OFFLOAD:
                case BassClientStateMachine.ADD_BCAST_SOURCE:
                case BassClientStateMachine.UPDATE_BCAST_SOURCE:
                case BassClientStateMachine.REMOVE_BCAST_SOURCE:
                case BassClientStateMachine.SET_BCAST_CODE:
                    if (mQueuedOps > 0) {
                        mQueuedOps = mQueuedOps - 1;
                    } else {
                        log("not a queued op, Internal op");
                        return;
                    }
                break;
                default:
                     {
                         log("notifyOperationCompletion: unhandled case");
                         return;
                     }
              }
        //}
          if (mQueuedOps == 0) {
              log("notifyOperationCompletion: all ops are done!");
              //trigger unlock with last device
              triggerUnlockforCSet(device);
          }

    }
*/
    public synchronized boolean startScanOffload (BluetoothDevice masterDevice,
                List<BluetoothDevice> devices) {

        Log.i(TAG, "startScanOffload for " + devices);
        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                continue;
            }
            stateMachine.sendMessage(BassClientStateMachine.START_SCAN_OFFLOAD);
            //mQueuedOps = mQueuedOps + 1;
        }
        return true;
    }

    public synchronized boolean stopScanOffload (BluetoothDevice masterDevice,
            List<BluetoothDevice> devices) {

        Log.i(TAG, "stopScanOffload for " + devices);
        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                continue;
            }
            stateMachine.sendMessage(BassClientStateMachine.STOP_SCAN_OFFLOAD);
            //mQueuedOps = mQueuedOps + 1;
        }

        return true;
    }

/*
    public boolean isLocalBroadcasting() {
        return bassUtils.isLocalLEAudioBroadcasting();
    }
*/
    private boolean isValidBroadcastSourceAddition(List<BluetoothDevice> devices,
                                                BluetoothLeBroadcastSourceInfo srcInfo) {
        boolean ret = true;

        //run through all the device, if it is not valid
        //to even one device to add this source, return failure
        for (BluetoothDevice dev : devices) {
            List<BluetoothLeBroadcastSourceInfo> currentSourceInfos =
                getAllBroadcastSources(dev);
            if (currentSourceInfos == null) {
                log("currentSourceInfos is null for " + dev);
                continue;
            }
            for (int i=0; i<currentSourceInfos.size(); i++) {
                if (srcInfo.matches(currentSourceInfos.get(i))) {
                   ret = false;
                   Log.e(TAG, "isValidBroadcastSourceAddition: fails for: " +
                        dev + "&srcInfo" + srcInfo);
                   break;
                }
            }
        }

        log("isValidBroadcastSourceInfo returns: " + ret);
        return ret;
    }

    private boolean isRoomForBroadcastSourceAddition(List<BluetoothDevice> devices) {
        boolean isRoomAvail = false;

        //run through all the device, if it is not valid
        //to even one device to add this source, return failure
        for (BluetoothDevice dev : devices) {
            isRoomAvail = false;
            List<BluetoothLeBroadcastSourceInfo> currentSourceInfos =
                getAllBroadcastSources(dev);
            for (int i=0; i<currentSourceInfos.size(); i++) {
                BluetoothLeBroadcastSourceInfo srcInfo = currentSourceInfos.get(i);
                if (srcInfo.isEmpty()) {
                   isRoomAvail = true;
                   continue;
                }
            }
            if (isRoomAvail == false) {
                Log.e(TAG, "isRoomForBroadcastSourceAddition: fails for: " + dev);
                break;
            }
        }

        log("isRoomForBroadcastSourceAddition returns: " + isRoomAvail);
        return isRoomAvail;
    }

    public synchronized boolean addBroadcastSource (BluetoothDevice masterDevice,
                List<BluetoothDevice> devices, BluetoothLeBroadcastSourceInfo srcInfo) {

        Log.i(TAG, "addBroadcastSource for " + devices +
                   "SourceInfo " + srcInfo);
        if (srcInfo == null) {
            Log.e(TAG, "addBroadcastSource: null SrcInfo");
            return false;
        }
        if (isRoomForBroadcastSourceAddition(devices) == false) {
            sendAddBroadcastSourceCallback(masterDevice,
                srcInfo/*BassClientStateMachine.INVALID_SRC_ID*/,
                BluetoothLeBroadcastAssistantCallback.BASS_STATUS_NO_EMPTY_SLOT);
            //triggerUnlockforCSet(masterDevice);
            return false;
        }

        if (isValidBroadcastSourceAddition(devices, srcInfo) == false) {
            sendAddBroadcastSourceCallback(masterDevice,
                srcInfo/*BassClientStateMachine.INVALID_SRC_ID*/,
                    BluetoothLeBroadcastAssistantCallback.BASS_STATUS_DUPLICATE_ADDITION);
            //triggerUnlockforCSet(masterDevice);
            return false;
        }
        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                Log.w(TAG, "addBroadcastSource: device seem to be not available, proceed");
                continue;
            }
            Message m = stateMachine.obtainMessage(BassClientStateMachine.ADD_BCAST_SOURCE);
            m.obj = srcInfo;
            stateMachine.sendMessage(m);
            //mQueuedOps = mQueuedOps + 1;
        }
        return true;
    }

    private byte getSrcIdForCSMember(BluetoothDevice masterDevice,
                BluetoothDevice memberDevice, byte masterSrcId) {
        byte targetSrcId = -1;
        List<BluetoothLeBroadcastSourceInfo> masterSrcInfos =
                        getAllBroadcastSources(masterDevice);
        List<BluetoothLeBroadcastSourceInfo> memberSrcInfos =
                        getAllBroadcastSources(memberDevice);
        if (masterSrcInfos == null || masterSrcInfos.size() == 0 ||
            memberSrcInfos == null || memberSrcInfos.size() == 0) {
            Log.e(TAG, "master or member source Infos not available");
            return targetSrcId;
        }
        if (masterDevice.equals(memberDevice)) {
            log("master: " + masterDevice + "member:memberDevice");
            return masterSrcId;
        }
        BluetoothDevice masterSrcDevice = null;
        for (int i=0; i<masterSrcInfos.size(); i++) {
            if (masterSrcInfos.get(i).getSourceId() == masterSrcId) {
                masterSrcDevice = masterSrcInfos.get(i).getSourceDevice();
                break;
            }
        }
        if (masterSrcDevice == null) {
            Log.e(TAG, "No matching SRC Id for the operation in masterDevice");
            return targetSrcId;
        }

        //look for this srcAddress in member to retrieve the srcId
        for (int i=0; i<memberSrcInfos.size(); i++) {
            if (masterSrcDevice.equals(memberSrcInfos.get(i).getSourceDevice())) {
                targetSrcId = masterSrcInfos.get(i).getSourceId();
                break;
            }
        }
        if (targetSrcId == -1) {
            Log.e(TAG, "No matching SRC Address in the member Src Infos");
        }
        return targetSrcId;
    }

    public synchronized boolean updateBroadcastSource (BluetoothDevice masterDevice,
                                    List<BluetoothDevice> devices,
                                    BluetoothLeBroadcastSourceInfo srcInfo
                                 ) {

         int status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_SOURCE_ID;
         Log.i(TAG, "updateBroadcastSource for " + devices +
                     "masterDevice " + masterDevice +
                     "SourceInfo " + srcInfo);

         if (srcInfo == null) {
             Log.e(TAG, "updateBroadcastSource: null SrcInfo");
             return false;
         }

         for (BluetoothDevice dev : devices) {
             if (getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId()) == -1) {
                  if (devices.size() > 1) {
                      status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_GROUP_OP;
                  } else {
                      status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_SOURCE_ID;
                  }
                  sendRemoveBroadcastSourceCallback(masterDevice,
                        srcInfo/*BassClientStateMachine.INVALID_SRC_ID*/, status);
                  //triggerUnlockforCSet(masterDevice);
                  return false;
             }
         }

         for (BluetoothDevice dev : devices) {
             BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
             if (stateMachine == null) {
                 Log.w(TAG, "updateBroadcastSource: Device seem to be not available");
                 continue;
             }
             byte targetSrcId = getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId());
             srcInfo.setSourceId(targetSrcId);

             Message m = stateMachine.obtainMessage(BassClientStateMachine.UPDATE_BCAST_SOURCE);
             m.obj = srcInfo;
             m.arg1 = stateMachine.USER;
             stateMachine.sendMessage(m);
             //mQueuedOps = mQueuedOps + 1;
         }

        return true;
    }

    public synchronized boolean setBroadcastCode  (BluetoothDevice masterDevice,
                                        List<BluetoothDevice> devices,
                                        BluetoothLeBroadcastSourceInfo srcInfo) {

        int status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_SOURCE_ID;
        Log.i(TAG, "setBroadcastCode for " + devices +
                   "masterDevice" + masterDevice +
                   "Broadcast PIN" + srcInfo.getBroadcastCode());

        if (srcInfo == null) {
            Log.e(TAG, "setBroadcastCode: null SrcInfo");
            return false;
        }

        for (BluetoothDevice dev : devices) {
            if (getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId()) == -1) {
                 if (devices.size() > 1) {
                     status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_GROUP_OP;
                 } else {
                     status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_SOURCE_ID;
                 }
                 sendRemoveBroadcastSourceCallback(masterDevice,
                        srcInfo/*BassClientStateMachine.INVALID_SRC_ID*/, status);
                 //triggerUnlockforCSet(masterDevice);
                 return false;
            }
        }

        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                 Log.w(TAG, "setBroadcastCode: Device seem to be not available");
                 continue;
            }

            byte targetSrcId = getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId());
            srcInfo.setSourceId(targetSrcId);

            Message m = stateMachine.obtainMessage(BassClientStateMachine.SET_BCAST_CODE);
            m.obj = srcInfo;
            m.arg1 = stateMachine.FRESH;
            stateMachine.sendMessage(m);
            //mQueuedOps = mQueuedOps + 1;
        }

        return true;
    }

    public synchronized boolean removeBroadcastSource (BluetoothDevice masterDevice,
                            List<BluetoothDevice> devices, byte sourceId) {

        int status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_SOURCE_ID;
        Log.i(TAG,  "removeBroadcastSource for " + devices +
                   "masterDevice " + masterDevice +
                    "removeBroadcastSource: sourceId:" + sourceId);

        if (sourceId == BassClientStateMachine.INVALID_SRC_ID) {
            Log.e(TAG, "removeBroadcastSource: Invalid source Id");
            return false;
        }


        for (BluetoothDevice dev : devices) {
            if (getSrcIdForCSMember(masterDevice, dev, sourceId) == -1) {
                 if (devices.size() > 1) {
                     status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_GROUP_OP;
                 } else {
                     status = BluetoothLeBroadcastAssistantCallback.BASS_STATUS_INVALID_SOURCE_ID;
                 }
                 sendRemoveBroadcastSourceCallback(masterDevice,
                        new BluetoothLeBroadcastSourceInfo(
                            BassClientStateMachine.INVALID_SRC_ID), status);
                 //triggerUnlockforCSet(masterDevice);
                 return false;
            }
        }

        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                Log.w(TAG, "setBroadcastCode: Device seem to be not available");
                continue;
            }

            Message m = stateMachine.obtainMessage(BassClientStateMachine.REMOVE_BCAST_SOURCE);
            m.arg1 = getSrcIdForCSMember(masterDevice, dev, sourceId);
            log("removeBroadcastSource: send message to SM " + dev);
            stateMachine.sendMessage(m);
            //mQueuedOps = mQueuedOps + 1;
        }

        return true;
    }

/*
    void triggerUnlockforCSet (BluetoothDevice device) {
        //get setId
        int setId = getCsetId(device);
        BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
        if (setMgr == null) {
            Log.e(TAG, "triggerUnlockforCSet: setMgr is NULL");
            return;
        }
        //Sending UnLock to
        log ("sending Unlock to device:" + device);
        Message m = setMgr.obtainMessage(BassCsetManager.UNLOCK);
        setMgr.sendMessage(m);
    }
*/
    public List<BluetoothLeBroadcastSourceInfo> getAllBroadcastSources (BluetoothDevice device
                                    ) {
        Log.i(TAG, "getAllBroadcastSources for " + device);
        synchronized (mStateMachines) {
            BassClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                return null;
            }
            return sm.getAllBroadcastSources();
        }
    }

    private BassClientStateMachine getOrCreateStateMachine(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "getOrCreateStateMachine failed: device cannot be null");
            return null;
        }
        synchronized (mStateMachines) {
            BassClientStateMachine sm = mStateMachines.get(device);
            if (sm != null) {
                return sm;
            }
            // Limit the maximum number of state machines to avoid DoS attack
            if (mStateMachines.size() >= MAX_BASS_CLIENT_STATE_MACHINES) {
                Log.e(TAG, "Maximum number of Bassclient state machines reached: "
                        + MAX_BASS_CLIENT_STATE_MACHINES);
                return null;
            }
            if (DBG) {
                Log.d(TAG, "Creating a new state machine for " + device);
            }
            sm = BassClientStateMachine.make(device, this,
                    mStateMachinesThread.getLooper());
            mStateMachines.put(device, sm);
            return sm;
        }
    }

/*
    private BassCsetManager getOrCreateCSetManager(int setId, BluetoothDevice masterDevice) {
        if (setId == -1) {
            Log.e(TAG, "getOrCreateCSetManager failed: invalid setId");
            return null;
        }
        synchronized (mSetManagers) {
            BassCsetManager sm = mSetManagers.get(setId);
            log("getOrCreateCSetManager: hashmap Entry:" + sm);
            if (sm != null) {
                return sm;
            }
            // Limit the maximum number of set manager state machines
            if (mStateMachines.size() >= MAX_BASS_CLIENT_CSET_MEMBERS) {
                Log.e(TAG, "Maximum number of Bassclient cset members reached: "
                        + MAX_BASS_CLIENT_CSET_MEMBERS);
                return null;
            }
            if (DBG) {
                Log.d(TAG, "Creating a new set Manager for " + setId);
            }
            sm = BassCsetManager.make(setId, masterDevice, this,
                    mSetManagerThread.getLooper());
            mSetManagers.put(setId, sm);
            return sm;
        }
    }

    public boolean isLockSupportAvailable(BluetoothDevice device) {
        boolean isLockAvail = false;
        boolean forceNoCsip = SystemProperties.getBoolean(
                    "persist.vendor.service.bt.forceNoCsip", false);
        if (forceNoCsip) {
            log("forceNoCsip is set");
            return isLockAvail;
        }
        //_CSIP
        isLockAvail = mAdapterService.isGroupExclAccessSupport(device);
        //_CSIP

        log("isLockSupportAvailable for:" + device + "returns " + isLockAvail);
        return isLockAvail;
    }

    private int getCsetId(BluetoothDevice device) {
        int setId = 1;
        //_CSIP
        setId = mSetCoordinator.getRemoteDeviceGroupId(device, CAS_UUID);
        //_CSIP
        log("getCsetId return:" + setId);
        return setId;
    }
*/
    public boolean stopScanOffloadInternal (BluetoothDevice device, boolean isGroupOp) {
        boolean ret = false;
        log("stopScanOffloadInternal: device: " + device
             + "isGroupOp" + isGroupOp);
        /* Even If the request is for Grouoop, If Lock support not available
         * for that device, go ahead and treat this as single device operation
         */
/*
        if (isGroupOp && isLockSupportAvailable(device) == true) {
            int setId = getCsetId(device);
            synchronized (mSetManagers) {
                BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
                if (setMgr == null) {
                    return false;
                 }
                 Message m = setMgr.obtainMessage(BassCsetManager.BASS_GRP_STOP_SCAN_OFFLOAD);
                 setMgr.sendMessage(m);
                 //queue req and return true
                 ret = true;
            }
        } else {*/
            List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
            listOfDevices.add(device);
            ret = stopScanOffload(device, listOfDevices);
//        }
        return ret;
    }

    public boolean startScanOffloadInternal (BluetoothDevice device, boolean isGroupOp) {
        boolean ret = false;
        log("startScanOffloadInternal: device: " + device
             + "isGroupOp" + isGroupOp);
        /* Even If the request is for Grouoop, If Lock support not available
         * for that device, go ahead and treat this as single device operation
         */
/*
        if (isGroupOp&& isLockSupportAvailable(device) == true) {
            int setId = getCsetId(device);
            synchronized (mSetManagers) {
                BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
                if (setMgr == null) {
                    return false;
                 }
                 Message m = setMgr.obtainMessage(BassCsetManager.BASS_GRP_START_SCAN_OFFLOAD);
                 setMgr.sendMessage(m);
                 //queue req and return true
                 ret = true;
            }
        } else {*/
           List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
           listOfDevices.add(device);
           ret = startScanOffload(device, listOfDevices);
//        }
        return ret;
    }

    public boolean addBroadcastSourceInternal (BluetoothDevice device,
            BluetoothLeBroadcastSourceInfo srcInfo, boolean isGroupOp) {
        boolean ret = false;
        log("addBroadcastSourceInternal: device: " + device
            + "srcInfo" + srcInfo
            + "isGroupOp" + isGroupOp);
        /* Even If the request is for Group, If Lock support not available
         * for that device, go ahead and treat this as single device operation
         */
/*
        if (isGroupOp && isLockSupportAvailable(device) == true) {
            int setId = getCsetId(device);
            synchronized (mSetManagers) {
                BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
                if (setMgr == null) {
                    return false;
                 }
                 Message m = setMgr.obtainMessage(BassCsetManager.BASS_GRP_ADD_BCAST_SOURCE);
                 m.obj = srcInfo;
                 setMgr.sendMessage(m);
                 //queue req and return true
                 ret = true;
            }
        } else {*/
           List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
           listOfDevices.add(device);
           ret = addBroadcastSource(device, listOfDevices, srcInfo);
//        }
        return ret;
    }

    public boolean updateBroadcastSourceInternal (BluetoothDevice device,
                                            BluetoothLeBroadcastSourceInfo srcInfo,
                                            boolean isGroupOp) {
        boolean ret = false;
        log("updateBroadcastSourceInternal: device: " + device
            + "srcInfo" + srcInfo
            + "isGroupOp" + isGroupOp);
        /* Even If the request is for Grouoop, If Lock support not available
         * for that device, go ahead and treat this as single device operation
         */
/*
        if (isGroupOp && isLockSupportAvailable(device) == true) {
            int setId = getCsetId(device);
            synchronized (mSetManagers) {
                BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
                if (setMgr == null) {
                    return false;
                 }
                 Message m = setMgr.obtainMessage(BassCsetManager.BASS_GRP_UPDATE_BCAST_SOURCE);
                 m.obj = srcInfo;
                 setMgr.sendMessage(m);
                 //queue req and return true
                 ret = true;
            }
        } else {*/
           List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
           listOfDevices.add(device);
           ret = updateBroadcastSource(device, listOfDevices, srcInfo);
//        }
        return ret;
    }

    protected boolean setBroadcastCodeInternal (BluetoothDevice device,
                BluetoothLeBroadcastSourceInfo srcInfo, boolean isGroupOp) {
        boolean ret = false;
        log("setBroadcastCodeInternal: device: " + device
            + "srcInfo" + srcInfo
            + "isGroupOp" + isGroupOp);
        /* Even If the request is for Grouoop, If Lock support not available
         * for that device, go ahead and treat this as single device operation
         */
/*
        if (isGroupOp && isLockSupportAvailable(device) == true) {
            int setId = getCsetId(device);
            synchronized (mSetManagers) {
                BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
                if (setMgr == null) {
                    return false;
                 }
                 Message m = setMgr.obtainMessage(BassCsetManager.BASS_GRP_SET_BCAST_CODE);
                 m.obj = srcInfo;
                 setMgr.sendMessage(m);
                 //queue req and return true
                 ret = true;
            }
        } else {*/
           List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
           listOfDevices.add(device);
           ret = setBroadcastCode(device, listOfDevices, srcInfo);
//        }
        return ret;
    }

    public boolean removeBroadcastSourceInternal (BluetoothDevice device,
                        int sourceId, boolean isGroupOp) {
         boolean ret = false;

         log("removeBroadcastSourceInternal: device: " + device
             + "sourceId " + sourceId
             + "isGroupOp " + isGroupOp);
         /* Even If the request is for Grouoop, If Lock support not available
          * for that device, go ahead and treat this as single device operation
          */
/*
         if (isGroupOp && isLockSupportAvailable(device) == true) {
             int setId = getCsetId(device);
             synchronized (mSetManagers) {
                 BassCsetManager setMgr = getOrCreateCSetManager(setId, device);
                 if (setMgr == null) {
                     return false;
                  }
                  Message m = setMgr.obtainMessage(BassCsetManager.BASS_GRP_REMOVE_BCAST_SOURCE);
                  m.arg1 = sourceId;
                  setMgr.sendMessage(m);
                  //queue req and return true
                  ret = true;
             }
         } else {*/
            List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
            listOfDevices.add(device);
            ret = removeBroadcastSource(device, listOfDevices, (byte)sourceId);
//         }
         return ret;
     }

    static void log(String msg) {
        if (BassClientStateMachine.BASS_DBG) {
           Log.d(TAG, msg);
        }
    }

    /**
     * Binder object: must be a static class or memory leak may occur
     */
    @VisibleForTesting
    static class BluetoothLeBroadcastAssistantBinder extends IBluetoothLeBroadcastAssistant.Stub
            implements IProfileServiceBinder {
        private BCService mService;

        private BCService getService() {
            if (!Utils.checkCallerIsSystemOrActiveUser(TAG)) {
                return null;
            }

            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        BluetoothLeBroadcastAssistantBinder(BCService svc) {
            mService = svc;
        }

        @Override
        public void cleanup() {
            mService = null;
        }

/*
        @Override
        public boolean connect(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.connect(device);
        }

        @Override
        public boolean disconnect(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.disconnect(device);
        }
*/

        @Override
        public List<BluetoothDevice> getConnectedDevices(/*AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return new ArrayList<>();
            }
            return service.getConnectedDevices();
        }

        @Override
        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states/*,
                                AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return new ArrayList<>();
            }
            return service.getDevicesMatchingConnectionStates(states);
        }

        @Override
        public int getConnectionState(BluetoothDevice device/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return service.getConnectionState(device);
        }

        @Override
        public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy/*,
                        AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return false;
            }
           return service.setConnectionPolicy(device, connectionPolicy);
        }

/*
        @Override
        public int getConnectionPolicy(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null || !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
            }
            return service.getConnectionPolicy(device);
        }
*/
        @Override
        public int searchforBroadcastSources(BluetoothDevice device/*,
                    AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*||
                  !Utils.checkScanPermissionForDataDelivery(service, source, TAG) ||
                  !Utils.checkCallerHasCoarseOrFineLocation(
                         service, source, UserHandle.of(UserHandle.getCallingUserId()))*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_START_SEARCH_FAILED;
            }
            return service.searchforBroadcastSources(device) ?
                    BluetoothStatusCodes.SUCCESS :
                    BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_START_SEARCH_FAILED;
        }

        @Override
        public int stopSearchforBroadcastSources(BluetoothDevice device/*,
                AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*||
                  !Utils.checkScanPermissionForDataDelivery(service, source, TAG) ||
                  !Utils.checkCallerHasCoarseOrFineLocation(
                         service, source, UserHandle.of(UserHandle.getCallingUserId()))*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_STOP_SEARCH_FAILED;
            }
            return service.stopSearchforBroadcastSources(device) ?
                    BluetoothStatusCodes.SUCCESS :
                    BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_STOP_SEARCH_FAILED;
        }

        @Override
        public int selectBroadcastSource (BluetoothDevice device, ScanResult scanRes,
                    boolean isGroupOp/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_SELECT_SOURCE_FAILED;
            }
            return service.selectBroadcastSource(device, scanRes, isGroupOp, false) ?
                    BluetoothStatusCodes.SUCCESS :
                    BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_SELECT_SOURCE_FAILED;
        }

        @Override
        public int registerCallback(BluetoothDevice device,
                IBluetoothLeBroadcastAssistantCallback cb/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_REGISTER_CALLBACK_FAILED;
            }
            service.registerCallback(device, cb);
            return BluetoothStatusCodes.SUCCESS;
        }

        @Override
        public int unregisterCallback(BluetoothDevice device,
                    IBluetoothLeBroadcastAssistantCallback cb/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_UNREGISTER_CALLBACK_FAILED;
            }
            service.unregisterCallback(device, cb);
            return BluetoothStatusCodes.SUCCESS;
        }

/*
        @Override
        public boolean startScanOffload(BluetoothDevice device, boolean isGroupOp,
                        AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.startScanOffloadInternal(device, isGroupOp);
        }

        @Override
        public boolean stopScanOffload(BluetoothDevice device, boolean isGroupOp,
                        AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.stopScanOffloadInternal(device, isGroupOp);
        }
*/

        @Override
        public int addBroadcastSource(BluetoothDevice device, BluetoothLeBroadcastSourceInfo srcInfo
                                      , boolean isGroupOp/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_ADD_SOURCE_FAILED;
            }
            return service.addBroadcastSourceInternal(device, srcInfo, isGroupOp) ?
                    BluetoothStatusCodes.SUCCESS :
                    BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_ADD_SOURCE_FAILED;
        }

        @Override
        public int updateBroadcastSource (BluetoothDevice device,
                                    BluetoothLeBroadcastSourceInfo srcInfo,
                                    boolean isGroupOp/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_UPDATE_SOURCE_FAILED;
            }
            return service.updateBroadcastSourceInternal(device, srcInfo, isGroupOp) ?
                    BluetoothStatusCodes.SUCCESS :
                    BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_UPDATE_SOURCE_FAILED;
        }

/*
        @Override
        public boolean setBroadcastCode (BluetoothDevice device,
                                    BluetoothLeBroadcastSourceInfo srcInfo,
                                    boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null || !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.setBroadcastCodeInternal(device, srcInfo, isGroupOp);
        }
*/
        @Override
        public int removeBroadcastSource (BluetoothDevice device,
                                    int sourceId,
                                    boolean isGroupOp/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return BluetoothStatusCodes.SUCCESS;
            }
            return service.removeBroadcastSourceInternal(device, sourceId, isGroupOp) ?
                BluetoothStatusCodes.SUCCESS : 
                BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_REMOVE_SOURCE_FAILED;
        }
        @Override
        public List<BluetoothLeBroadcastSourceInfo> getAllBroadcastSources
                    (BluetoothDevice device/*, AttributionSource source*/) {
            BCService service = getService();
            if (service == null/*|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)*/) {
                return null;
            }
            return service.getAllBroadcastSources(device);
       }
   }
}
