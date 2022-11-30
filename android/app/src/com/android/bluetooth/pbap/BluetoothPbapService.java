/*
 * Copyright (c) 2017, The Linux Foundation. All rights reserved
 * Copyright (c) 2008-2009, Motorola, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Motorola, Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.bluetooth.pbap;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.annotation.RequiresPermission;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.IBluetoothPbap;
import android.content.AttributionSource;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.sysprop.BluetoothProperties;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.bluetooth.IObexConnectionHandler;
import com.android.bluetooth.ObexServerSockets;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.sdp.SdpManager;
import com.android.bluetooth.util.DevicePolicyUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.bluetooth.btservice.AbstractionLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import java.util.Arrays;
import java.util.Collections;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BluetoothPbapService extends ProfileService implements IObexConnectionHandler {
    private static final String TAG = "BluetoothPbapService";

    /**
     * To enable PBAP DEBUG/VERBOSE logging - run below cmd in adb shell, and
     * restart com.android.bluetooth process. only enable DEBUG log:
     * "setprop log.tag.BluetoothPbapService DEBUG"; enable both VERBOSE and
     * DEBUG log: "setprop log.tag.BluetoothPbapService VERBOSE"
     */

    public static final boolean DEBUG = true;

    public static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    /**
     * The component name of the owned BluetoothPbapActivity
     */
    private static final String PBAP_ACTIVITY = BluetoothPbapActivity.class.getCanonicalName();

    /**
     * Intent indicating incoming obex authentication request which is from
     * PCE(Carkit)
     */
    static final String AUTH_CHALL_ACTION = "com.android.bluetooth.pbap.authchall";

    /**
     * Intent indicating obex session key input complete by user which is sent
     * from BluetoothPbapActivity
     */
    static final String AUTH_RESPONSE_ACTION = "com.android.bluetooth.pbap.authresponse";

    /**
     * Intent indicating user canceled obex authentication session key input
     * which is sent from BluetoothPbapActivity
     */
    static final String AUTH_CANCELLED_ACTION = "com.android.bluetooth.pbap.authcancelled";

    /**
     * Intent indicating timeout for user confirmation, which is sent to
     * BluetoothPbapActivity
     */
    static final String USER_CONFIRM_TIMEOUT_ACTION =
            "com.android.bluetooth.pbap.userconfirmtimeout";

    /**
     * Intent Extra name indicating session key which is sent from
     * BluetoothPbapActivity
     */
    static final String EXTRA_SESSION_KEY = "com.android.bluetooth.pbap.sessionkey";
    static final String EXTRA_DEVICE = "com.android.bluetooth.pbap.device";

    static final int MSG_ACQUIRE_WAKE_LOCK = 5004;
    static final int MSG_RELEASE_WAKE_LOCK = 5005;
    static final int MSG_STATE_MACHINE_DONE = 5006;

    static final int START_LISTENER = 1;
    static final int USER_TIMEOUT = 2;
    static final int SHUTDOWN = 3;
    static final int LOAD_CONTACTS = 4;
    static final int CONTACTS_LOADED = 5;
    static final int CHECK_SECONDARY_VERSION_COUNTER = 6;
    static final int ROLLOVER_COUNTERS = 7;
    static final int GET_LOCAL_TELEPHONY_DETAILS = 8;
    static final int HANDLE_VERSION_UPDATE_NOTIFICATION = 9;

    static final int USER_CONFIRM_TIMEOUT_VALUE = 30000;
    static final int RELEASE_WAKE_LOCK_DELAY = 10000;

    private PowerManager.WakeLock mWakeLock;

    private static String sLocalPhoneNum;
    private static String sLocalPhoneName;

    private ObexServerSockets mServerSockets = null;
    private DatabaseManager mDatabaseManager;

    private static final int SDP_PBAP_SERVER_VERSION = 0x0102;
    // PBAP v1.2.3, Sec. 7.1.2: local phonebook and favorites
    private static final int SDP_PBAP_SUPPORTED_REPOSITORIES = 0x0009;
    private static final int SDP_PBAP_SUPPORTED_FEATURES = 0x021F;

    /* PBAP will use Bluetooth notification ID from 1000000 (included) to 2000000 (excluded).
       The notification ID should be unique in Bluetooth package. */
    private static final int PBAP_NOTIFICATION_ID_START = 1000000;
    private static final int PBAP_NOTIFICATION_ID_END = 2000000;
    static final int VERSION_UPDATE_NOTIFICATION_DELAY = 500;

    private int mSdpHandle = -1;

    protected Context mContext;

    private PbapHandler mSessionStatusHandler;
    private HandlerThread mHandlerThread;
    private final HashMap<BluetoothDevice, PbapStateMachine> mPbapStateMachineMap = new HashMap<>();
    private volatile int mNextNotificationId = PBAP_NOTIFICATION_ID_START;

    // package and class name to which we send intent to check phone book access permission
    private static final String ACCESS_AUTHORITY_PACKAGE = "com.android.settings";
    private static final String ACCESS_AUTHORITY_CLASS =
            "com.android.settings.bluetooth.BluetoothPermissionRequest";

    private Thread mThreadLoadContacts;
    private boolean mContactsLoaded = false;

    private Thread mThreadUpdateSecVersionCounter;

    private static BluetoothPbapService sBluetoothPbapService;

    // Stores map of BD address to isRebonded for the given BT Session
    protected static HashMap<String,String> PbapSdpResponse = new HashMap <String,String>();
    // Stores map of BD address to PCE version for the given BT Session
    protected static HashMap<String,Integer> remoteVersion = new HashMap <String,Integer>();

    protected static final String PBAP_NOTIFICATION_ID = "pbap_notification";
    protected static final String PBAP_NOTIFICATION_NAME = "BT_PBAP_ADVANCE_SUPPORT";
    protected static final int PBAP_ADV_VERSION = 0x0102;
    protected static final int RECORD_LENGTH = 6;
    protected static final int VERSION_LENGTH = 2;
    protected static final int ADDRESS_LENGTH = 3;
    protected static NotificationManager mNotificationManager;

    protected static final int SDP_PBAP_LEGACY_SERVER_VERSION = 0x0101;

    protected static final int SDP_PBAP_LEGACY_SUPPORTED_REPOSITORIES = 0x0001;

    protected static final int SDP_PBAP_LEGACY_SUPPORTED_FEATURES = 0x0003;

    protected static boolean isSimSupported = true;

    protected static boolean isSupportedPbap12 = true;

    public static boolean isEnabled() {
        return BluetoothProperties.isProfilePbapServerEnabled().orElse(false);
    }

    /* To get feature support from config file */
    protected static void getFeatureSupport() {
        AdapterService adapterService = AdapterService.getAdapterService();
        if (adapterService != null) {
            isSupportedPbap12 = adapterService.getProfileFeatureInfo(AbstractionLayer.PBAP_VERSION_0102_SUPPORT);
            isSimSupported = adapterService.getProfileFeatureInfo(AbstractionLayer.PBAP_SIM_SUPPORT);
            if (DEBUG) Log.d(TAG, "isSupportedPbap12: " + isSupportedPbap12);
            if (DEBUG) Log.d(TAG, "isSimSupported: " + isSimSupported);
        }
    }

    private class BluetoothPbapContentObserver extends ContentObserver {
        BluetoothPbapContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, " onChange on contact uri ");
            sendUpdateRequest();
        }
    }

    private void sendUpdateRequest() {
        if (mContactsLoaded) {
            if (!mSessionStatusHandler.hasMessages(CHECK_SECONDARY_VERSION_COUNTER)) {
                mSessionStatusHandler.sendMessage(
                        mSessionStatusHandler.obtainMessage(CHECK_SECONDARY_VERSION_COUNTER));
            }
        }
    }

    private BluetoothPbapContentObserver mContactChangeObserver;

    private void parseIntent(final Intent intent) {
        String action = intent.getAction();
        if (DEBUG) {
            Log.d(TAG, "action: " + action);
        }
        if (BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY.equals(action)) {
            int requestType = intent.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                    BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS);
            if (requestType != BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS) {
                return;
            }

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            synchronized (mPbapStateMachineMap) {
                PbapStateMachine sm = mPbapStateMachineMap.get(device);
                if (sm == null) {
                    Log.w(TAG, "device not connected! device=" + device);
                    return;
                }
                mSessionStatusHandler.removeMessages(USER_TIMEOUT, sm);
                int access = intent.getIntExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        BluetoothDevice.CONNECTION_ACCESS_NO);
                boolean savePreference = intent.getBooleanExtra(
                        BluetoothDevice.EXTRA_ALWAYS_ALLOWED, false);

                if (access == BluetoothDevice.CONNECTION_ACCESS_YES) {
                    if (savePreference) {
                        device.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
                        if (VERBOSE) {
                            Log.v(TAG, "setPhonebookAccessPermission(ACCESS_ALLOWED)");
                        }
                    }
                    sm.sendMessage(PbapStateMachine.AUTHORIZED);
                } else {
                    if (savePreference) {
                        device.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
                        if (VERBOSE) {
                            Log.v(TAG, "setPhonebookAccessPermission(ACCESS_REJECTED)");
                        }
                    }
                    sm.sendMessage(PbapStateMachine.REJECTED);
                }
            }
        } else if (AUTH_RESPONSE_ACTION.equals(action)) {
            String sessionKey = intent.getStringExtra(EXTRA_SESSION_KEY);
            BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
            synchronized (mPbapStateMachineMap) {
                PbapStateMachine sm = mPbapStateMachineMap.get(device);
                if (sm == null) {
                    return;
                }
                Message msg = sm.obtainMessage(PbapStateMachine.AUTH_KEY_INPUT, sessionKey);
                sm.sendMessage(msg);
            }
        } else if (AUTH_CANCELLED_ACTION.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
            synchronized (mPbapStateMachineMap) {
                PbapStateMachine sm = mPbapStateMachineMap.get(device);
                if (sm == null) {
                    return;
                }
                sm.sendMessage(PbapStateMachine.AUTH_CANCELLED);
            }
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
            BluetoothDevice.ERROR);
    if (bondState == BluetoothDevice.BOND_BONDED && isSupportedPbap12) {
        BluetoothDevice remoteDevice = intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE);
        mSessionStatusHandler.sendMessageDelayed(
                    mSessionStatusHandler.obtainMessage(
                    HANDLE_VERSION_UPDATE_NOTIFICATION, remoteDevice),
                    VERSION_UPDATE_NOTIFICATION_DELAY);
    }
} else {
            Log.w(TAG, "Unhandled intent action: " + action);
        }
    }

    private BroadcastReceiver mPbapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            parseIntent(intent);
        }
    };

    private void closeService() {
        if (VERBOSE) {
            Log.v(TAG, "Pbap Service closeService");
        }

        BluetoothPbapUtils.savePbapParams(this);

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        cleanUpServerSocket();

        if (mSessionStatusHandler != null) {
            mSessionStatusHandler.removeCallbacksAndMessages(null);
        }
    }

    private void cleanUpServerSocket() {
        // Step 1, 2: clean up active server session and connection socket
        synchronized (mPbapStateMachineMap) {
            for (PbapStateMachine stateMachine : mPbapStateMachineMap.values()) {
                stateMachine.sendMessage(PbapStateMachine.DISCONNECT);
            }
        }
        // Step 3: clean up SDP record
        cleanUpSdpRecord();
        // Step 4: clean up existing server sockets
        if (mServerSockets != null) {
            mServerSockets.shutdown(false);
            mServerSockets = null;
        }
    }

    private void createSdpRecord() {
        if (mSdpHandle > -1) {
            Log.w(TAG, "createSdpRecord, SDP record already created");
            return;
        }

        getFeatureSupport();

        if (!isSimSupported) {
            Log.d(TAG ,"creating PBAP 1.1 record without sim support");
              mSdpHandle = SdpManager.getDefaultManager().createPbapPseRecord
                ("OBEX Phonebook Access Server",mServerSockets.getRfcommChannel(),
                -1, SDP_PBAP_LEGACY_SERVER_VERSION,
                SDP_PBAP_LEGACY_SUPPORTED_REPOSITORIES,
                SDP_PBAP_LEGACY_SUPPORTED_FEATURES);
        } else if (isSimSupported) {
            Log.d(TAG ,"creating PBAP 1.1 record with sim support");
            mSdpHandle = SdpManager.getDefaultManager().createPbapPseRecord
                ("OBEX Phonebook Access Server",mServerSockets.getRfcommChannel(),
                -1, SDP_PBAP_LEGACY_SERVER_VERSION,
                SDP_PBAP_SUPPORTED_REPOSITORIES,
                SDP_PBAP_LEGACY_SUPPORTED_FEATURES);
        }

        if (DEBUG) {
            Log.d(TAG, "created Sdp record, mSdpHandle=" + mSdpHandle);
        }
    }

    private void cleanUpSdpRecord() {
        if (mSdpHandle < 0) {
            Log.w(TAG, "cleanUpSdpRecord, SDP record never created");
            return;
        }
        int sdpHandle = mSdpHandle;
        mSdpHandle = -1;
        SdpManager sdpManager = SdpManager.getDefaultManager();
        if (DEBUG) {
            Log.d(TAG, "cleanUpSdpRecord, mSdpHandle=" + sdpHandle);
        }
        if (sdpManager == null) {
            Log.e(TAG, "sdpManager is null");
        } else if (!sdpManager.removeSdpRecord(sdpHandle)) {
            Log.w(TAG, "cleanUpSdpRecord, removeSdpRecord failed, sdpHandle=" + sdpHandle);
        }
    }

    /* Send SDP request to know about remote PCE Profile Support only if 1.2
     * entry for Bonded device is not found. */
    protected static boolean remoteSupportsPbap1_2(BluetoothDevice device) {
        Log.d(TAG, "checkRemoteProfileSupport");
        if (device == null) {
            Log.e(TAG, "Remote Device not fetched, Return");
            return false;
        }
        boolean isEntryFound = readRemoteProfileVersion1_2(device);
        if (isEntryFound) {
            Log.d(TAG, "Remote Supports PBAP 1.2");
            return true;
        }
        return false;
    }

    protected static boolean readRemoteProfileVersion1_2(BluetoothDevice device) {
        Log.d(TAG, "readRemoteProfileVersion ");
        try {
            final String filePath = "/data/misc/bluedroid/pce_peer_entries.conf";
            File file = new File(filePath);
            Log.d(TAG, "file length = " + (int)file.length());
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
            return readRecord(fileData, device);
        } catch (IOException io) {
            Log.e(TAG, "File Read Failed: " + io);
        }
        return false;
    }

    /* Read all records and check if entry for remote device is found
     * Returns true if 1.2 support is stored for remote else false */
    public static boolean readRecord(byte[] fileData, BluetoothDevice device) {
        for (int i = 0, j = 0 ; (j + RECORD_LENGTH) <= fileData.length; i++) {
            // Read Version from PCE Entry
            byte[] versionBytes = Arrays.copyOfRange(fileData, j, j + VERSION_LENGTH);
            int version = byteArrayToInt(versionBytes);
            j += VERSION_LENGTH;

            // Read BD ADDRESS from PCE Entry
            StringBuilder address = new StringBuilder();
            address.append(String.format("%02X", fileData[j]) + ":"
                    + String.format("%02X", fileData[j+1]) + ":"
                    + String.format("%02X", fileData[j+2]));
            j += ADDRESS_LENGTH;

            // Read rebonded from PCE Entry
            char isRebonded = (char)fileData[j++];
            Log.d(TAG, "version: " + version + ", address = " + address.toString()
                    + ", isRebonded = "+ isRebonded +" Remote Address = "
                    + device.getAddress());

            boolean isMatched = device.getAddress().toLowerCase()
                    .startsWith(address.toString().toLowerCase());
            PbapSdpResponse.put(address.toString(), Character.toString(isRebonded));
            remoteVersion.put(address.toString(), Integer.valueOf(version));
            if (isMatched) {
                return (version >= PBAP_ADV_VERSION ? true : false);
            }
        }
        return false;
    }

    /* Convert 2 bytes of data to integer */
    public static int byteArrayToInt(byte[] b)
    {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 ;
    }

    /*Creates Notification for PBAP version upgrade/downgrade */
    protected static void createNotification(BluetoothPbapService context, boolean isUpgrade) {
        if (VERBOSE) Log.v(TAG, "Create PBAP Notification for Upgrade/Downgrade");
        // create Notification channel.
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(PBAP_NOTIFICATION_ID,
                PBAP_NOTIFICATION_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        mNotificationManager.createNotificationChannel(mChannel);
        // create notification
        String title = isUpgrade ? context.getString(R.string.phonebook_advance_feature_support) :
                context.getString(R.string.remote_phonebook_feature_downgrade);
        String contentText = isUpgrade ? context.getString
                (R.string.repair_for_adv_phonebook_feature):
                context.getString(R.string.repair_for_phonebook_access_version_comp);
        int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;
        Notification notification = new Notification.Builder(context,PBAP_NOTIFICATION_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setAutoCancel(true)
            .build();

        if (mNotificationManager != null )
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        else
            Log.e(TAG,"mNotificationManager is null");
    }

    /* Checks if notification for Version Upgrade is required */
    protected static void handleNotificationTask(BluetoothPbapService service,
            BluetoothDevice remoteDevice) {
        boolean hasPbap12Support = remoteSupportsPbap1_2(remoteDevice);
        /* check if remote devices is rebonded by looking into its entry in hashmap using key
         * (0,8) i.e. first 3 bytes of bd addr in string format including colon (XX:XX:XX) */
        String isRebonded = PbapSdpResponse.get(
                remoteDevice.getAddress().substring(0,8));
        /* fetch PCE version of remote (if present) in hashmap 'remoteVersion' from key
         * (0,8) i.e. first 3 bytes of bd addr in string format including colon (XX:XX:XX) */
        Integer remotePceVersion = remoteVersion.get(
                remoteDevice.getAddress().substring(0,8));
        if (hasPbap12Support && (isRebonded.equals("N"))) {
            Log.d(TAG, "Remote Supports PBAP 1.2. Notify user");
            createNotification(service, true);
        } else if (!hasPbap12Support
                && (remotePceVersion != null &&
                    remotePceVersion.intValue() < PBAP_ADV_VERSION)
                && (isRebonded != null && isRebonded.equals("N"))) {
            Log.d(TAG, "Remote PBAP profile support downgraded");
            createNotification(service, false);
        } else {
            Log.d(TAG, "Notification Not Required.");
            if (mNotificationManager != null)
                mNotificationManager.cancelAll();
        }
    }

    static boolean isRebonded(BluetoothDevice remoteDevice) {
        String isRebonded = PbapSdpResponse.get(
                remoteDevice.getAddress().substring(0,8));
        return ((isRebonded != null) && isRebonded.equalsIgnoreCase("Y"));
    }

    private class PbapHandler extends Handler {
        private PbapHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (VERBOSE) {
                Log.v(TAG, "Handler(): got msg=" + msg.what);
            }

            switch (msg.what) {
                case START_LISTENER:
                  mServerSockets = ObexServerSockets.createWithFixedChannels
                          (sBluetoothPbapService, SdpManager.PBAP_RFCOMM_CHANNEL,
                          SdpManager.PBAP_L2CAP_PSM);
                  if (mServerSockets == null) {
                      Log.w(TAG, "ObexServerSockets.create() returned null");
                      break;
                  }
                    createSdpRecord();
                    // fetch Pbap Params to check if significant change has happened to Database
                    BluetoothPbapUtils.fetchPbapParams(mContext);
                    break;
                case USER_TIMEOUT:
                    Intent intent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL);
                    intent.setPackage(getString(R.string.pairing_ui_package));
                    PbapStateMachine stateMachine = (PbapStateMachine) msg.obj;
                    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, stateMachine.getRemoteDevice());
                    intent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                            BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS);
                    sendBroadcast(intent, BLUETOOTH_CONNECT,
                            Utils.getTempAllowlistBroadcastOptions());
                    stateMachine.sendMessage(PbapStateMachine.REJECTED);
                    break;
                case MSG_ACQUIRE_WAKE_LOCK:
                    if (mWakeLock == null) {
                        PowerManager pm = getSystemService(PowerManager.class);
                        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                "StartingObexPbapTransaction");
                        mWakeLock.setReferenceCounted(false);
                        mWakeLock.acquire();
                        Log.w(TAG, "Acquire Wake Lock");
                    }
                    mSessionStatusHandler.removeMessages(MSG_RELEASE_WAKE_LOCK);
                    mSessionStatusHandler.sendMessageDelayed(
                            mSessionStatusHandler.obtainMessage(MSG_RELEASE_WAKE_LOCK),
                            RELEASE_WAKE_LOCK_DELAY);
                    break;
                case MSG_RELEASE_WAKE_LOCK:
                    if (mWakeLock != null) {
                        mWakeLock.release();
                        mWakeLock = null;
                    }
                    break;
                case SHUTDOWN:
                    closeService();
                    break;
                case LOAD_CONTACTS:
                    loadAllContacts();
                    break;
                case CONTACTS_LOADED:
                    mContactsLoaded = true;
                    break;
                case CHECK_SECONDARY_VERSION_COUNTER:
                    updateSecondaryVersion();
                    break;
                case ROLLOVER_COUNTERS:
                    BluetoothPbapUtils.rolloverCounters();
                    break;
                case MSG_STATE_MACHINE_DONE:
                    PbapStateMachine sm = (PbapStateMachine) msg.obj;
                    BluetoothDevice remoteDevice = sm.getRemoteDevice();
                    sm.quitNow();
                    synchronized (mPbapStateMachineMap) {
                        mPbapStateMachineMap.remove(remoteDevice);
                    }
                    break;
                case GET_LOCAL_TELEPHONY_DETAILS:
                    getLocalTelephonyDetails();
                    break;
                case HANDLE_VERSION_UPDATE_NOTIFICATION:
                     BluetoothDevice remoteDev = (BluetoothDevice) msg.obj;

                     handleNotificationTask(sBluetoothPbapService, remoteDev);
                default:
                    break;
            }
        }
    }

    /**
     * Get the current connection state of PBAP with the passed in device
     *
     * @param device is the device whose connection state to PBAP we are trying to get
     * @return current connection state, one of {@link BluetoothProfile#STATE_DISCONNECTED},
     * {@link BluetoothProfile#STATE_CONNECTING}, {@link BluetoothProfile#STATE_CONNECTED}, or
     * {@link BluetoothProfile#STATE_DISCONNECTING}
     */
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public int getConnectionState(BluetoothDevice device) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        if (mPbapStateMachineMap == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }

        synchronized (mPbapStateMachineMap) {
            PbapStateMachine sm = mPbapStateMachineMap.get(device);
            if (sm == null) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getConnectionState();
        }
    }

    List<BluetoothDevice> getConnectedDevices() {
        if (mPbapStateMachineMap == null) {
            return new ArrayList<>();
        }
        synchronized (mPbapStateMachineMap) {
            return new ArrayList<>(mPbapStateMachineMap.keySet());
        }
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (mPbapStateMachineMap == null || states == null) {
            return devices;
        }
        synchronized (mPbapStateMachineMap) {
            for (int state : states) {
                for (BluetoothDevice device : mPbapStateMachineMap.keySet()) {
                    if (state == mPbapStateMachineMap.get(device).getConnectionState()) {
                        devices.add(device);
                    }
                }
            }
        }
        return devices;
    }

    /**
     * Set connection policy of the profile and tries to disconnect it if connectionPolicy is
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}
     *
     * <p> The device should already be paired.
     * Connection policy can be one of:
     * {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED},
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN},
     * {@link BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     */
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy) {
        enforceCallingOrSelfPermission(
                BLUETOOTH_PRIVILEGED, "Need BLUETOOTH_PRIVILEGED permission");
        if (DEBUG) {
            Log.d(TAG, "Saved connectionPolicy " + device + " = " + connectionPolicy);
        }

        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.PBAP,
                  connectionPolicy)) {
            return false;
        }
        if (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return true;
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
        if (device == null) {
            throw new IllegalArgumentException("Null device");
        }
        return mDatabaseManager
                .getProfileConnectionPolicy(device, BluetoothProfile.PBAP);
    }

    /**
     * Disconnects pbap server profile with device
     * @param device is the remote bluetooth device
     */
    public void disconnect(BluetoothDevice device) {
        synchronized (mPbapStateMachineMap) {
            PbapStateMachine sm = mPbapStateMachineMap.get(device);
            if (sm != null) {
                sm.sendMessage(PbapStateMachine.DISCONNECT);
            }
        }
    }

    static String getLocalPhoneNum() {
        return sLocalPhoneNum;
    }

    @VisibleForTesting
    static void setLocalPhoneName(String localPhoneName) {
        sLocalPhoneName = localPhoneName;
    }

    static String getLocalPhoneName() {
        return sLocalPhoneName;
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new PbapBinder(this);
    }

    @Override
    protected boolean start() {
        if (VERBOSE) {
            Log.v(TAG, "start()");
        }
        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),
            "DatabaseManager cannot be null when PbapService starts");

        // Enable owned Activity component
        setComponentAvailable(PBAP_ACTIVITY, true);

        mContext = this;
        mContactsLoaded = false;
        mHandlerThread = new HandlerThread("PbapHandlerThread");
        mHandlerThread.start();
        mSessionStatusHandler = new PbapHandler(mHandlerThread.getLooper());
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);
        filter.addAction(AUTH_RESPONSE_ACTION);
        filter.addAction(AUTH_CANCELLED_ACTION);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        BluetoothPbapConfig.init(this);
        registerReceiver(mPbapReceiver, filter);
        try {
            mContactChangeObserver = new BluetoothPbapContentObserver();
            getContentResolver().registerContentObserver(
                    DevicePolicyUtils.getEnterprisePhoneUri(this), false,
                    mContactChangeObserver);
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLite exception: " + e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal state exception, content observer is already registered");
        }

        setBluetoothPbapService(this);

        mSessionStatusHandler.sendMessage(
                mSessionStatusHandler.obtainMessage(GET_LOCAL_TELEPHONY_DETAILS));
        mSessionStatusHandler.sendMessage(mSessionStatusHandler.obtainMessage(LOAD_CONTACTS));
        mSessionStatusHandler.sendMessage(mSessionStatusHandler.obtainMessage(START_LISTENER));
        return true;
    }

    @Override
    protected boolean stop() {
        if (VERBOSE) {
            Log.v(TAG, "stop()");
        }
        setBluetoothPbapService(null);
        if (mSessionStatusHandler != null) {
            mSessionStatusHandler.obtainMessage(SHUTDOWN).sendToTarget();
        }
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
        mContactsLoaded = false;
        if (mContactChangeObserver == null) {
            Log.i(TAG, "Avoid unregister when receiver it is not registered");
            return true;
        }
        unregisterReceiver(mPbapReceiver);
        getContentResolver().unregisterContentObserver(mContactChangeObserver);
        mContactChangeObserver = null;
        setComponentAvailable(PBAP_ACTIVITY, false);
        return true;
    }

    /**
     * Get the current instance of {@link BluetoothPbapService}
     *
     * @return current instance of {@link BluetoothPbapService}
     */
    @VisibleForTesting
    public static synchronized BluetoothPbapService getBluetoothPbapService() {
        if (sBluetoothPbapService == null) {
            Log.w(TAG, "getBluetoothPbapService(): service is null");
            return null;
        }
        if (!sBluetoothPbapService.isAvailable()) {
            Log.w(TAG, "getBluetoothPbapService(): service is not available");
            return null;
        }
        return sBluetoothPbapService;
    }

    private static synchronized void setBluetoothPbapService(BluetoothPbapService instance) {
        if (DEBUG) {
            Log.d(TAG, "setBluetoothPbapService(): set to: " + instance);
        }
        sBluetoothPbapService = instance;
    }

    @Override
    protected void setCurrentUser(int userId) {
        Log.i(TAG, "setCurrentUser(" + userId + ")");
        UserManager userManager = getSystemService(UserManager.class);
        if (userManager.isUserUnlocked(UserHandle.of(userId))) {
            setUserUnlocked(userId);
        }
    }

    @Override
    protected void setUserUnlocked(int userId) {
        Log.i(TAG, "setUserUnlocked(" + userId + ")");
        sendUpdateRequest();
    }

    private static class PbapBinder extends IBluetoothPbap.Stub implements IProfileServiceBinder {
        private BluetoothPbapService mService;

        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        private BluetoothPbapService getService(AttributionSource source) {
            if (!Utils.checkCallerIsSystemOrActiveUser(TAG)
                    || !Utils.checkServiceAvailable(mService, TAG)
                    || !Utils.checkConnectPermissionForDataDelivery(mService, source, TAG)) {
                return null;
            }
            return mService;
        }

        PbapBinder(BluetoothPbapService service) {
            if (VERBOSE) {
                Log.v(TAG, "PbapBinder()");
            }
            mService = service;
        }

        @Override
        public void cleanup() {
            mService = null;
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices(AttributionSource source) {
            if (DEBUG) {
                Log.d(TAG, "getConnectedDevices");
            }
            BluetoothPbapService service = getService(source);
            if (service == null) {
                return new ArrayList<>(0);
            }
            return service.getConnectedDevices();
        }

        @Override
        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states,
                AttributionSource source) {
            if (DEBUG) {
                Log.d(TAG, "getDevicesMatchingConnectionStates");
            }
            BluetoothPbapService service = getService(source);
            if (service == null) {
                return new ArrayList<>(0);
            }
            return service.getDevicesMatchingConnectionStates(states);
        }

        @Override
        public int getConnectionState(BluetoothDevice device, AttributionSource source) {
            if (DEBUG) {
                Log.d(TAG, "getConnectionState: " + device);
            }
            BluetoothPbapService service = getService(source);
            if (service == null) {
                return BluetoothAdapter.STATE_DISCONNECTED;
            }
            return service.getConnectionState(device);
        }

        @Override
        public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy,
                AttributionSource source) {
            if (DEBUG) {
                Log.d(TAG, "setConnectionPolicy for device: " + device + ", policy:"
                        + connectionPolicy);
            }
            BluetoothPbapService service = getService(source);
            if (service == null) {
                return false;
            }
            return service.setConnectionPolicy(device, connectionPolicy);
        }

        @Override
        public void disconnect(BluetoothDevice device, AttributionSource source) {
            if (DEBUG) {
                Log.d(TAG, "disconnect");
            }
            BluetoothPbapService service = getService(source);
            if (service == null) {
                return;
            }
            service.disconnect(device);
        }
    }

    @Override
    public boolean onConnect(BluetoothDevice remoteDevice, BluetoothSocket socket) {
        if (remoteDevice == null || socket == null) {
            Log.e(TAG, "onConnect(): Unexpected null. remoteDevice=" + remoteDevice
                    + " socket=" + socket);
            return false;
        }

        PbapStateMachine sm = PbapStateMachine.make(this, mHandlerThread.getLooper(), remoteDevice,
                socket,  this, mSessionStatusHandler, mNextNotificationId);
        mNextNotificationId++;
        if (mNextNotificationId == PBAP_NOTIFICATION_ID_END) {
            mNextNotificationId = PBAP_NOTIFICATION_ID_START;
        }
        synchronized (mPbapStateMachineMap) {
            mPbapStateMachineMap.put(remoteDevice, sm);
        }
        sm.sendMessage(PbapStateMachine.REQUEST_PERMISSION);
        return true;
    }

    /**
     * Get the phonebook access permission for the device; if unknown, ask the user.
     * Send the result to the state machine.
     * @param stateMachine PbapStateMachine which sends the request
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public void checkOrGetPhonebookPermission(PbapStateMachine stateMachine) {
        BluetoothDevice device = stateMachine.getRemoteDevice();
        int permission = device.getPhonebookAccessPermission();
        if (DEBUG) {
            Log.d(TAG, "getPhonebookAccessPermission() = " + permission);
        }

        if (permission == BluetoothDevice.ACCESS_ALLOWED) {
            setConnectionPolicy(device, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
            stateMachine.sendMessage(PbapStateMachine.AUTHORIZED);
        } else if (permission == BluetoothDevice.ACCESS_REJECTED) {
            stateMachine.sendMessage(PbapStateMachine.REJECTED);
        } else { // permission == BluetoothDevice.ACCESS_UNKNOWN
            Intent intent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REQUEST);
            intent.setClassName(BluetoothPbapService.ACCESS_AUTHORITY_PACKAGE,
                    BluetoothPbapService.ACCESS_AUTHORITY_CLASS);
            intent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                    BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            intent.putExtra(BluetoothDevice.EXTRA_PACKAGE_NAME, this.getPackageName());
            this.sendOrderedBroadcast(intent, BLUETOOTH_CONNECT,
                    Utils.getTempAllowlistBroadcastOptions(), null/* resultReceiver */,
                    null/* scheduler */, Activity.RESULT_OK/* initialCode */, null/* initialData */,
                    null/* initialExtras */);
            if (VERBOSE) {
                Log.v(TAG, "waiting for authorization for connection from: " + device);
            }
            /* In case car kit time out and try to use HFP for phonebook
             * access, while UI still there waiting for user to confirm */
            Message msg = mSessionStatusHandler.obtainMessage(BluetoothPbapService.USER_TIMEOUT,
                    stateMachine);
            mSessionStatusHandler.sendMessageDelayed(msg, USER_CONFIRM_TIMEOUT_VALUE);
            /* We will continue the process when we receive
             * BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY from Settings app. */
        }
    }

    /**
     * Called when an unrecoverable error occurred in an accept thread.
     * Close down the server socket, and restart.
     */
    @Override
    public synchronized void onAcceptFailed() {
        Log.w(TAG, "PBAP server socket accept thread failed. Restarting the server socket");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        cleanUpServerSocket();

        if (mSessionStatusHandler != null) {
            mSessionStatusHandler.removeCallbacksAndMessages(null);
        }

        synchronized (mPbapStateMachineMap) {
            mPbapStateMachineMap.clear();
        }

        mSessionStatusHandler.sendMessage(mSessionStatusHandler.obtainMessage(START_LISTENER));
    }

    private void loadAllContacts() {
        if (mThreadLoadContacts == null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    BluetoothPbapUtils.loadAllContacts(mContext,
                            mSessionStatusHandler);
                    mThreadLoadContacts = null;
                }
            };
            mThreadLoadContacts = new Thread(r);
            mThreadLoadContacts.start();
        }
    }

    private void updateSecondaryVersion() {
        if (mThreadUpdateSecVersionCounter == null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    BluetoothPbapUtils.updateSecondaryVersionCounter(mContext,
                            mSessionStatusHandler);
                    mThreadUpdateSecVersionCounter = null;
                }
            };
            mThreadUpdateSecVersionCounter = new Thread(r);
            mThreadUpdateSecVersionCounter.start();
        }
    }

    private void getLocalTelephonyDetails() {
        TelephonyManager tm = getSystemService(TelephonyManager.class);
        if (tm != null) {
            sLocalPhoneNum = tm.getLine1Number();
            sLocalPhoneName = this.getString(R.string.localPhoneName);
        }
        if (VERBOSE)
            Log.v(TAG, "Local Phone Details- Number:" + sLocalPhoneNum
                    + ", Name:" + sLocalPhoneName);
    }
}
