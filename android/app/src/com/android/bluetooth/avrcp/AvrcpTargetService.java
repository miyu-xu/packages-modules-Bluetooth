/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.bluetooth.avrcp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothAvrcpTarget;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Looper;
import android.os.UserManager;
import android.sysprop.BluetoothProperties;
import android.text.TextUtils;
import android.util.Log;

import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.audio_util.BTAudioEventLogger;
import com.android.bluetooth.audio_util.MediaData;
import com.android.bluetooth.audio_util.MediaPlayerList;
import com.android.bluetooth.audio_util.MediaPlayerWrapper;
import com.android.bluetooth.audio_util.Metadata;
import com.android.bluetooth.audio_util.PlayStatus;
import com.android.bluetooth.audio_util.PlayerInfo;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.ServiceFactory;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.Objects;

/**
 * Provides Bluetooth AVRCP Target profile as a service in the Bluetooth application.
 * @hide
 */
public class AvrcpTargetService extends ProfileService {
    private static final String TAG = "AvrcpTargetService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int AVRCP_MAX_VOL = 127;
    private static final int MEDIA_KEY_EVENT_LOGGER_SIZE = 20;
    private static final String MEDIA_KEY_EVENT_LOGGER_TITLE = "Media Key Events";
    private static int sDeviceMaxVolume = 0;
    private final BTAudioEventLogger mMediaKeyEventLogger = new BTAudioEventLogger(
            MEDIA_KEY_EVENT_LOGGER_SIZE, MEDIA_KEY_EVENT_LOGGER_TITLE);

    private AvrcpVersion mAvrcpVersion;
    private MediaPlayerList mMediaPlayerList;
    private AudioManager mAudioManager;
    private AvrcpBroadcastReceiver mReceiver;
    private AvrcpNativeInterface mNativeInterface;
    private AvrcpVolumeManager mVolumeManager;
    private ServiceFactory mFactory = new ServiceFactory();

    // Only used to see if the metadata has changed from its previous value
    private MediaData mCurrentData;

    // Cover Art Service (Storage + BIP Server)
    private AvrcpCoverArtService mAvrcpCoverArtService = null;

    private static AvrcpTargetService sInstance = null;

    public static boolean isEnabled() {
        return BluetoothProperties.isProfileAvrcpTargetEnabled().orElse(false);
    }

    class ListCallback implements MediaPlayerList.MediaUpdateCallback {
        @Override
        public void run(MediaData data) {
            if (mNativeInterface == null) return;

            boolean metadata = !Objects.equals(mCurrentData.metadata, data.metadata);
            boolean state = !MediaPlayerWrapper.playstateEquals(mCurrentData.state, data.state);
            boolean queue = !Objects.equals(mCurrentData.queue, data.queue);

            if (DEBUG) {
                Log.d(TAG, "onMediaUpdated: track_changed=" + metadata
                        + " state=" + state + " queue=" + queue);
            }
            mCurrentData = data;

            mNativeInterface.sendMediaUpdate(metadata, state, queue);
        }

        @Override
        public void run(boolean availablePlayers, boolean addressedPlayers,
                boolean uids) {
            if (mNativeInterface == null) return;

            mNativeInterface.sendFolderUpdate(availablePlayers, addressedPlayers, uids);
        }
    }

    private class AvrcpBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)) {
                if (mNativeInterface == null) return;

                // Update all the playback status info for each connected device
                mNativeInterface.sendMediaUpdate(false, true, false);
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                if (mNativeInterface == null) return;

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    // If there is no connection, disconnectDevice() will do nothing
                    if (mNativeInterface.disconnectDevice(device.getAddress())) {
                        Log.d(TAG, "request to disconnect device " + device);
                    }
                }
            } else if (action.equals(AudioManager.ACTION_VOLUME_CHANGED)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType == AudioManager.STREAM_MUSIC) {
                    int volume = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                    BluetoothDevice activeDevice = getA2dpActiveDevice();
                    if (activeDevice != null
                            && !mVolumeManager.getAbsoluteVolumeSupported(activeDevice)) {
                        Log.d(TAG, "stream volume change to " + volume + " " + activeDevice);
                        mVolumeManager.storeVolumeForDevice(activeDevice, volume);
                    }
                }
            }
        }
    }

    /**
     * Set the AvrcpTargetService instance.
     */
    @VisibleForTesting
    public static void set(AvrcpTargetService instance) {
        sInstance = instance;
    }

    /**
     * Get the AvrcpTargetService instance. Returns null if the service hasn't been initialized.
     */
    public static AvrcpTargetService get() {
        return sInstance;
    }

    public AvrcpCoverArtService getCoverArtService() {
        return mAvrcpCoverArtService;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new AvrcpTargetBinder(this);
    }

    @Override
    protected void setUserUnlocked(int userId) {
        Log.i(TAG, "User unlocked, initializing the service");

        if (mMediaPlayerList != null) {
            mMediaPlayerList.init(new ListCallback());
        }
    }

    @Override
    protected boolean start() {
        if (sInstance != null) {
            Log.wtf(TAG, "The service has already been initialized");
            return false;
        }

        Log.i(TAG, "Starting the AVRCP Target Service");
        mCurrentData = new MediaData(null, null, null);

        mAudioManager = getSystemService(AudioManager.class);
        sDeviceMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mMediaPlayerList = new MediaPlayerList(Looper.myLooper(), this);

        mNativeInterface = AvrcpNativeInterface.getInterface();
        mNativeInterface.init(AvrcpTargetService.this);

        mAvrcpVersion = AvrcpVersion.getCurrentSystemPropertiesValue();

        mVolumeManager = new AvrcpVolumeManager(this, mAudioManager, mNativeInterface);

        UserManager userManager = getApplicationContext().getSystemService(UserManager.class);
        if (userManager.isUserUnlocked()) {
            mMediaPlayerList.init(new ListCallback());
        }

        if (getResources().getBoolean(R.bool.avrcp_target_enable_cover_art)) {
            if (mAvrcpVersion.isAtleastVersion(AvrcpVersion.AVRCP_VERSION_1_6)) {
                mAvrcpCoverArtService = new AvrcpCoverArtService(this);
                boolean started = mAvrcpCoverArtService.start();
                if (!started) {
                    Log.e(TAG, "Failed to start cover art service");
                    mAvrcpCoverArtService = null;
                }
            } else {
                Log.e(TAG, "Please use AVRCP version 1.6 to enable cover art");
            }
        }

        mReceiver = new AvrcpBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(AudioManager.ACTION_VOLUME_CHANGED);
        registerReceiver(mReceiver, filter);

        // Only allow the service to be used once it is initialized
        sInstance = this;
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        String deviceAddress = activeDevice != null ?
                activeDevice.getAddress() :
                AdapterService.ACTIVITY_ATTRIBUTION_NO_ACTIVE_DEVICE_ADDRESS;
        AdapterService.getAdapterService().notifyActivityAttributionInfo(
                getAttributionSource(), deviceAddress);

        return true;
    }

    @Override
    protected boolean stop() {
        Log.i(TAG, "Stopping the AVRCP Target Service");

        if (sInstance == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }

        if (mAvrcpCoverArtService != null) {
            mAvrcpCoverArtService.stop();
        }
        mAvrcpCoverArtService = null;
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        String deviceAddress = activeDevice != null ?
                activeDevice.getAddress() :
                AdapterService.ACTIVITY_ATTRIBUTION_NO_ACTIVE_DEVICE_ADDRESS;
        AdapterService.getAdapterService().notifyActivityAttributionInfo(
                getAttributionSource(), deviceAddress);

        sInstance = null;
        unregisterReceiver(mReceiver);

        // We check the interfaces first since they only get set on User Unlocked
        if (mMediaPlayerList != null) mMediaPlayerList.cleanup();
        if (mNativeInterface != null) mNativeInterface.cleanup();

        mMediaPlayerList = null;
        mNativeInterface = null;
        mAudioManager = null;
        mReceiver = null;
        return true;
    }

    private void init() {
    }

    private BluetoothDevice getA2dpActiveDevice() {
        A2dpService service = mFactory.getA2dpService();
        if (service == null) {
            return null;
        }
        return service.getActiveDevice();
    }

    private void setA2dpActiveDevice(BluetoothDevice device) {
        A2dpService service = A2dpService.getA2dpService();
        if (service == null) {
            Log.d(TAG, "setA2dpActiveDevice: A2dp service not found");
            return;
        }
        service.setActiveDevice(device);
    }

    void deviceConnected(BluetoothDevice device, boolean absoluteVolume) {
        Log.i(TAG, "deviceConnected: device=" + device + " absoluteVolume=" + absoluteVolume);
        mVolumeManager.deviceConnected(device, absoluteVolume);
        MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.AVRCP);
    }

    void deviceDisconnected(BluetoothDevice device) {
        Log.i(TAG, "deviceDisconnected: device=" + device);
        mVolumeManager.deviceDisconnected(device);
    }

    /**
     * Signal to the service that the current audio out device has changed and to inform
     * the audio service whether the new device supports absolute volume. If it does, also
     * set the absolute volume level on the remote device.
     */
    public void volumeDeviceSwitched(BluetoothDevice device) {
        if (DEBUG) {
            Log.d(TAG, "volumeDeviceSwitched: device=" + device);
        }
        mVolumeManager.volumeDeviceSwitched(device);
    }

    /**
     * Remove the stored volume for a device.
     */
    public void removeStoredVolumeForDevice(BluetoothDevice device) {
        if (device == null) return;

        mVolumeManager.removeStoredVolumeForDevice(device);
    }

    /**
     * Retrieve the remembered volume for a device. Returns -1 if there is no volume for the
     * device.
     */
    public int getRememberedVolumeForDevice(BluetoothDevice device) {
        if (device == null) return -1;

        return mVolumeManager.getVolume(device, mVolumeManager.getNewDeviceVolume());
    }

    // TODO (apanicke): Add checks to rejectlist Absolute Volume devices if they behave poorly.
    void setVolume(int avrcpVolume) {
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.d(TAG, "setVolume: no active device");
            return;
        }

        mVolumeManager.setVolume(activeDevice, avrcpVolume);
    }

    /**
     * Set the volume on the remote device. Does nothing if the device doesn't support absolute
     * volume.
     */
    public void sendVolumeChanged(int deviceVolume) {
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.d(TAG, "sendVolumeChanged: no active device");
            return;
        }

        mVolumeManager.sendVolumeChanged(activeDevice, deviceVolume);
    }

    Metadata getCurrentSongInfo() {
        Metadata metadata = mMediaPlayerList.getCurrentSongInfo();
        if (mAvrcpCoverArtService != null && metadata.image != null) {
            String imageHandle = mAvrcpCoverArtService.storeImage(metadata.image);
            if (imageHandle != null) metadata.image.setImageHandle(imageHandle);
        }
        return metadata;
    }

    PlayStatus getPlayState() {
        return PlayStatus.fromPlaybackState(mMediaPlayerList.getCurrentPlayStatus(),
                Long.parseLong(getCurrentSongInfo().duration));
    }

    String getCurrentMediaId() {
        String id = mMediaPlayerList.getCurrentMediaId();
        if (id != null && !id.isEmpty()) return id;

        Metadata song = getCurrentSongInfo();
        if (song != null && !song.mediaId.isEmpty()) return song.mediaId;

        // We always want to return something, the error string just makes debugging easier
        return "error";
    }

    List<Metadata> getNowPlayingList() {
        String currentMediaId = getCurrentMediaId();
        Metadata currentTrack = null;
        String imageHandle = null;
        List<Metadata> nowPlayingList = mMediaPlayerList.getNowPlayingList();
        if (mAvrcpCoverArtService != null) {
            for (Metadata metadata : nowPlayingList) {
                if (TextUtils.equals(metadata.mediaId, currentMediaId)) {
                    currentTrack = metadata;
                } else if (metadata.image != null) {
                    imageHandle = mAvrcpCoverArtService.storeImage(metadata.image);
                    if (imageHandle != null) {
                        metadata.image.setImageHandle(imageHandle);
                    }
                }
            }

            // Always store the current item from the queue last so we know the image is in storage
            if (currentTrack != null) {
                imageHandle = mAvrcpCoverArtService.storeImage(currentTrack.image);
                if (imageHandle != null) {
                    currentTrack.image.setImageHandle(imageHandle);
                }
            }
        }
        return nowPlayingList;
    }

    int getCurrentPlayerId() {
        return mMediaPlayerList.getCurrentPlayerId();
    }

    // TODO (apanicke): Have the Player List also contain info about the play state of each player
    List<PlayerInfo> getMediaPlayerList() {
        return mMediaPlayerList.getMediaPlayerList();
    }

    void getPlayerRoot(int playerId, MediaPlayerList.GetPlayerRootCallback cb) {
        mMediaPlayerList.getPlayerRoot(playerId, cb);
    }

    void getFolderItems(int playerId, String mediaId, MediaPlayerList.GetFolderItemsCallback cb) {
        mMediaPlayerList.getFolderItems(playerId, mediaId, cb);
    }

    void playItem(int playerId, boolean nowPlaying, String mediaId) {
        // NOTE: playerId isn't used if nowPlaying is true, since its assumed to be the current
        // active player
        mMediaPlayerList.playItem(playerId, nowPlaying, mediaId);
    }

    // TODO (apanicke): Handle key events here in the service. Currently it was more convenient to
    // handle them there but logically they make more sense handled here.
    void sendMediaKeyEvent(int event, boolean pushed) {
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        MediaPlayerWrapper player = mMediaPlayerList.getActivePlayer();
        mMediaKeyEventLogger.logd(DEBUG, TAG, "getMediaKeyEvent:" + " device=" + activeDevice
                + " event=" + event + " pushed=" + pushed
                + " to " + (player == null ? null : player.getPackageName()));
        mMediaPlayerList.sendMediaKeyEvent(event, pushed);
    }

    void setActiveDevice(BluetoothDevice device) {
        Log.i(TAG, "setActiveDevice: device=" + device);
        if (device == null) {
            Log.wtf(TAG, "setActiveDevice: could not find device " + device);
        }
        setA2dpActiveDevice(device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void getListPlayerAppAttrRsp(byte attr, byte[] attrIds, BluetoothDevice device,
                AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService getListPlayerAppAttrRsp")) {
            return;
        }
        mNativeInterface.getListPlayerAppAttrRsp(attr, attrIds, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void getPlayerAppValueRsp(byte numberAttr, byte[] values, BluetoothDevice device,
                AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService getPlayerAppValueRsp")) {
            return;
        }
        mNativeInterface.getPlayerAppValueRsp(numberAttr, values, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void sendCurrentPlayerValueRsp(byte numberAttr, byte[] attr, BluetoothDevice device,
                AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService sendCurrentPlayerValueRsp")) {
            return;
        }
        mNativeInterface.sendCurrentPlayerValueRsp(numberAttr, attr, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void sendSetPlayerAppRsp(int attrStatus, BluetoothDevice device,
                AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService sendSetPlayerAppRsp")) {
            return;
        }
        mNativeInterface.sendSetPlayerAppRsp(attrStatus, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void sendSettingsTextRsp(int numAttr, byte[] attr, int length, String[] text,
                 BluetoothDevice device, AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService sendSettingsTextRsp")) {
            return;
        }
        mNativeInterface.sendSettingsTextRsp(numAttr, attr, length, text, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void sendValueTextRsp(int numAttr, byte[] attr, int length, String[] text,
                BluetoothDevice device, AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService sendValueTextRsp")) {
            return;
        }
        mNativeInterface.sendValueTextRsp(numAttr, attr, length, text, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    void registerNotificationPlayerAppRsp(int type, byte numberAttr, byte[] attr,
                BluetoothDevice device, AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService registerNotificationPlayerAppRsp")) {
            return;
        }
        mNativeInterface.registerNotificationPlayerAppRsp(type, numberAttr, attr, device);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    BluetoothDevice getPlayerSettingCmdPendingDevice(Integer reponse,
                AttributionSource attributionSource) {
        if (!Utils.checkConnectPermissionForDataDelivery(
                this, attributionSource, "AvrcpTargetService getPlayerSettingCmdPendingDevice")) {
            return null;
        }
        return null;
    }

    //PDU ID 0x11
    private void onListPlayerAttributeRequest(BluetoothDevice device) {
        if (DEBUG)
            Log.v(TAG, "onListPlayerAttributeRequest");
        int deviceIndex =
                getIndexForDevice(mAdapter.getRemoteDevice(
                        Utils.getAddressStringFromByte(address)));
        if (deviceIndex == INVALID_DEVICE_INDEX) {
            Log.e(TAG,"invalid index for device");
            return;
        }
        CreateMusicSettingsAppCmdLookupOrUpdate(AvrcpConstants.GET_ATTRIBUTE_IDS,
                deviceIndex, true);
        mAvrcpPlayerAppSettings.onListPlayerAttributeRequest(address);
        SendPlayerSettingMsg(AvrcpConstants.GET_ATTRIBUTE_IDS, address);
    }

    //PDU ID 0x12
    private void onListPlayerAttributeValues(byte attr, BluetoothDevice device) {
        if (DEBUG)Log.v(TAG, "onListPlayerAttributeValues");
        int deviceIndex =
                getIndexForDevice(mAdapter.getRemoteDevice(
                Utils.getAddressStringFromByte(address)));
        if (deviceIndex == INVALID_DEVICE_INDEX) {
            Log.e(TAG,"invalid index for device");
            return;
        }
        CreateMusicSettingsAppCmdLookupOrUpdate(AvrcpConstants.GET_VALUE_IDS, deviceIndex, true);
        mAvrcpPlayerAppSettings.onListPlayerAttributeValues(attr, address);
        SendPlayerSettingMsg(AvrcpConstants.GET_VALUE_IDS, address);
    }

    //PDU ID 0x13
    private void onGetPlayerAttributeValues(byte attr , int[] arr,
            BluetoothDevice device) {
        if (DEBUG)
            Log.v(TAG, "onGetPlayerAttributeValues: num of attrib " + attr );
        int deviceIndex =
                getIndexForDevice(mAdapter.getRemoteDevice(
                Utils.getAddressStringFromByte(address)));
        if (deviceIndex == INVALID_DEVICE_INDEX) {
            Log.e(TAG,"invalid index for device");
            return;
        }
        CreateMusicSettingsAppCmdLookupOrUpdate(AvrcpConstants.GET_ATTRIBUTE_VALUES,
                deviceIndex, true);
        mAvrcpPlayerAppSettings.onGetPlayerAttributeValues(attr, arr, address);
        SendPlayerSettingMsg(AvrcpConstants.GET_ATTRIBUTE_VALUES, address);
    }

    //PDU 0x14
    private void setPlayerAppSetting(byte num, byte[] attr_id, byte[] attr_val,
            BluetoothDevice device) {
        if (DEBUG)
            Log.v(TAG, "setPlayerAppSetting: number of attributes" + num );
        int deviceIndex =
                getIndexForDevice(mAdapter.getRemoteDevice(
                Utils.getAddressStringFromByte(address)));
        if (deviceIndex == INVALID_DEVICE_INDEX) {
            Log.e(TAG,"invalid index for device");
            return;
        }
        CreateMusicSettingsAppCmdLookupOrUpdate(AvrcpConstants.SET_ATTRIBUTE_VALUES,
                deviceIndex, true);
        mAvrcpPlayerAppSettings.setPlayerAppSetting(num, attr_id, attr_val, address);
        SendPlayerSettingMsg(AvrcpConstants.SET_ATTRIBUTE_VALUES, address);
    }

    //PDU 0x15
    private void getPlayerAttributeText(byte num , byte[] attrIds,
            BluetoothDevice device) {
        if(DEBUG) Log.d(TAG, "getplayerattribute_text " + attr +" attrIDsNum "
                                                        + attrIds.length);
        int deviceIndex =
                getIndexForDevice(mAdapter.getRemoteDevice(
                Utils.getAddressStringFromByte(address)));
        if (deviceIndex == INVALID_DEVICE_INDEX) {
            Log.e(TAG,"invalid index for device");
            return;
        }
        CreateMusicSettingsAppCmdLookupOrUpdate(AvrcpConstants.GET_ATTRIBUTE_TEXT,
                deviceIndex, true);
        mAvrcpPlayerAppSettings.getplayerattribute_text(num, attrIds, address);
        SendPlayerSettingMsg(AvrcpConstants.GET_ATTRIBUTE_TEXT, address);
    }

    //PDU 0x16
    private void getPlayerValueText(byte attr_id , byte num_value , byte[] value,
            BluetoothDevice device) {
        int deviceIndex =
                getIndexForDevice(mAdapter.getRemoteDevice(
                Utils.getAddressStringFromByte(address)));
        if (deviceIndex == INVALID_DEVICE_INDEX) {
            Log.e(TAG,"invalid index for device");
            return;
        }
        CreateMusicSettingsAppCmdLookupOrUpdate(AvrcpConstants.GET_VALUE_TEXT, deviceIndex, true);
        mAvrcpPlayerAppSettings.getplayervalue_text(attr_id, num_value, value, address);
        SendPlayerSettingMsg(AvrcpConstants.GET_VALUE_TEXT, address);
    }

    private void SendPlayerSettingMsg(Integer cmd, byte[] address) {
        Message msg = mHandler.obtainMessage();
        msg.what = MESSAGE_PLAYERSETTINGS_TIMEOUT;
        msg.arg1 = cmd;
        msg.arg2 = 0;
        msg.obj = Utils.getAddressStringFromByte(address);
        mHandler.sendMessageDelayed(msg, 500);
    }

    private void CreateMusicSettingsAppCmdLookupOrUpdate(Integer cmd,
            int deviceIndex, boolean entry_new) {
        if (deviceIndex == INVALID_DEVICE_INDEX) {
           Log.e(TAG,"invalid index for device");
           return;
        }
        Log.v(TAG,"Cmd = " + cmd + "on index = " + deviceIndex + "new entry" + entry_new);

        if (entry_new) {
            if (deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.
                    containsKey(cmd)) {
                int cmdCount =
                        deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.get(cmd);
                Log.v(TAG,"cmdCount = " + cmdCount + "for command type = " + cmd);
                deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.put
                        (cmd, cmdCount + 1);
            } else {
                deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.put
                        (cmd, 1);
            }
        } else {
            if (deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.
                    containsKey(cmd)) {
                int PendingCmds =
                        deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.get(cmd);
                Log.v(TAG,"PendingCmds = " + PendingCmds + "for resoponse type = " + cmd);
                if (PendingCmds > 1) {
                    deviceFeatures[deviceIndex].mMusicAppCmdResponsePending
                            .put(cmd, PendingCmds - 1);
                } else if (PendingCmds == 1) {
                    deviceFeatures[deviceIndex].mMusicAppCmdResponsePending.remove(cmd);
                } else {
                    Log.e(TAG,"Invalid Player Setting Cmd count entry in lookup");
                }
            }
        }
    }

    /**
     * Dump debugging information to the string builder
     */
    public void dump(StringBuilder sb) {
        sb.append("\nProfile: AvrcpTargetService:\n");
        if (sInstance == null) {
            sb.append("AvrcpTargetService not running");
            return;
        }

        StringBuilder tempBuilder = new StringBuilder();
        tempBuilder.append("AVRCP version: " + mAvrcpVersion + "\n");

        if (mMediaPlayerList != null) {
            mMediaPlayerList.dump(tempBuilder);
        } else {
            tempBuilder.append("\nMedia Player List is empty\n");
        }

        mMediaKeyEventLogger.dump(tempBuilder);
        tempBuilder.append("\n");
        mVolumeManager.dump(tempBuilder);
        if (mAvrcpCoverArtService != null) {
            tempBuilder.append("\n");
            mAvrcpCoverArtService.dump(tempBuilder);
        }

        // Tab everything over by two spaces
        sb.append(tempBuilder.toString().replaceAll("(?m)^", "  "));
    }

    private static class AvrcpTargetBinder extends IBluetoothAvrcpTarget.Stub
            implements IProfileServiceBinder {
        private AvrcpTargetService mService;

        AvrcpTargetBinder(AvrcpTargetService service) {
            mService = service;
        }

        @Override
        public void cleanup() {
            mService = null;
        }

        @Override
        public void sendVolumeChanged(int volume) {
            if (mService == null
                    || !Utils.checkCallerIsSystemOrActiveOrManagedUser(mService, TAG)) {
                return;
            }

            mService.sendVolumeChanged(volume);
        }

        @Override
        public void getListPlayerAppAttrRsp(byte attr, byte[] attrIds, BluetoothDevice device,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                getListPlayerAppAttrRsp(attr, attrIds, device, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void getListPlayerAppAttrRsp(byte attr, byte[] attrIds, BluetoothDevice device,
                AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.getListPlayerAppAttrRsp(attr, attrIds, device, attributionSource);
        }

        @Override
        public void getPlayerAppValueRsp(byte numberAttr, byte[] values, BluetoothDevice device,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                getPlayerAppValueRsp(numberAttr, values, device, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void getPlayerAppValueRsp(byte numberAttr, byte[] values, BluetoothDevice device,
                AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.getPlayerAppValueRsp(numberAttr, values, device, attributionSource);
        }

        @Override
        public void sendCurrentPlayerValueRsp(byte numberAttr, byte[] attr, BluetoothDevice device,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                sendCurrentPlayerValueRsp(numberAttr, attr, device, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void sendCurrentPlayerValueRsp(byte numberAttr, byte[]attr, BluetoothDevice device,
                AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.sendCurrentPlayerValueRsp(numberAttr, attr, device, attributionSource);
        }

        @Override
        public void sendSetPlayerAppRsp(int attrStatus, BluetoothDevice device,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                sendSetPlayerAppRsp(attrStatus, device, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void sendSetPlayerAppRsp(int attrStatus, BluetoothDevice device,
                AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.sendSetPlayerAppRsp(attrStatus, device, attributionSource);
        }

        @Override
        public void sendSettingsTextRsp(int numAttr, byte[] attr, int length, String[] text,
                 BluetoothDevice device, AttributionSource attributionSource,
                 SynchronousResultReceiver receiver) {
            try {
                sendSettingsTextRsp(numAttr, attr, length, device, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void sendSettingsTextRsp(int numAttr, byte[] attr, int length, String[] text,
                 BluetoothDevice device, AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.sendSettingsTextRsp(numAttr, attr, length, device, attributionSource);
        }

        @Override
        public void sendValueTextRsp(int numAttr, byte[] attr, int length, String[] text,
                BluetoothDevice device, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                sendValueTextRsp(numAttr, attr, length, text, device, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void sendValueTextRsp(int numAttr, byte[] attr, int length, String[] text,
                BluetoothDevice device, AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.sendValueTextRsp(numAttr, attr, length, text, device,
                    attributionSource);
        }

        @Override
        public void registerNotificationPlayerAppRsp(int type, byte numberAttr, byte[] attr,
                BluetoothDevice device, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                registerNotificationPlayerAppRsp(type, numberAttr, attr, device,
                        attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private void registerNotificationPlayerAppRsp(int type, byte numberAttr, byte[] attr,
                BluetoothDevice device, AttributionSource attributionSource) {
            if (mService == null) {
                return;
            }
            mService.registerNotificationPlayerAppRsp(type, numberAttr, attr, device,
                    attributionSource);
        }

        @Override
        public BluetoothDevice getPlayerSettingCmdPendingDevice(Integer reponse,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                receiver.send(getPlayerSettingCmdPendingDevice(reponse, attributionSource));
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
        private BluetoothDevice getPlayerSettingCmdPendingDevice(Integer reponse,
                AttributionSource attributionSource) {
            if (mService == null) {
                return null;
            }
            return mService.getPlayerSettingCmdPendingDevice(reponse, attributionSource);
        }

    }
}
