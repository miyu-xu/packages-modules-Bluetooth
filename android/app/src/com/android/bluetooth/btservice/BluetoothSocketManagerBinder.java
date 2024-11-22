/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.IBluetoothSocketManager;
import android.content.AttributionSource;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.bluetooth.flags.Flags;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class BluetoothSocketManagerBinder extends IBluetoothSocketManager.Stub {
    private static final String TAG = "BtSocketManagerBinder";

    private static final int INVALID_FD = -1;

    private static final int INVALID_CID = -1;

    static final int SOCKET_CONNECTION_STATE_LISTENING = 1;
    static final int SOCKET_CONNECTION_STATE_CONNECTING = 2;
    static final int SOCKET_CONNECTION_STATE_CONNECTED = 3;
    static final int SOCKET_CONNECTION_STATE_DISCONNECTING = 4;
    static final int SOCKET_CONNECTION_STATE_DISCONNECTED = 5;
    static final int SOCKET_ROLE_LISTEN = 1;
    static final int SOCKET_ROLE_CONNECTION = 2;
    static final int CLASSIC_POWER_MODE_ACTIVE = 0;
    static final int CLASSIC_POWER_MODE_SNIFF = 2;

    static int sClientRegistrationId = 0;
    static int sServerRegistrationId = 0;

    private AdapterService mService;
    private List<SocketProperties> mClientSocketProperties;
    private List<SocketProperties> mServerSocketProperties;

    BluetoothSocketManagerBinder(AdapterService service) {
        mService = service;
        mSocketProperties = new ArrayList<>();
    }

    void cleanUp() {
        mService = null;
        sClientRegistrationId = 0;
        sServerRegistrationId = 0;
    }

    @Override
    public ParcelFileDescriptor connectSocket(
            BluetoothDevice device, int type, ParcelUuid uuid, int port, int flag, boolean offload) {

        enforceActiveUser();

        if (!Utils.checkConnectPermissionForPreflight(mService)) {
            return null;
        }

        String brEdrAddress =
                Flags.identityAddressNullIfUnknown()
                        ? Utils.getBrEdrAddress(device)
                        : mService.getIdentityAddress(device.getAddress());
        int regId = --sClientRegistrationId;
        int appUid = Binder.getCallingUid();

        OffloadInfo offloadInfo = offload ? new OffloadInfo() : null;
        mClientSocketProperties.add(new SocketProperties(
            regId, SOCKET_ROLE_CONNECTION, SOCKET_CONNECTION_STATE_CONNECTING, offloadInfo));

        Log.i(
                TAG,
                "connectSocket: device="
                        + device
                        + ", type="
                        + type
                        + ", uuid="
                        + uuid
                        + ", port="
                        + port
                        + ", regId="
                        + regId
                        + ", offload="
                        + offload
                        + ", from "
                        + Utils.getUidPidString());

        return marshalFd(
                mService.getNative()
                        .connectSocket(
                                Utils.getBytesFromAddress(
                                        type == BluetoothSocket.TYPE_L2CAP_LE
                                                ? device.getAddress()
                                                : brEdrAddress),
                                type,
                                Utils.uuidToByteArray(uuid),
                                port,
                                flag,
                                appUid,
                                regId,
                                offload));
    }

    @Override
    public ParcelFileDescriptor createSocketChannel(
            int type, String serviceName, ParcelUuid uuid, int port, int flag, boolean offload) {

        enforceActiveUser();

        if (!Utils.checkConnectPermissionForPreflight(mService)) {
            return null;
        }
        int regId = ++sServerRegistrationId;
        int appUid = Binder.getCallingUid();
        OffloadInfo offloadInfo = offload ? new OffloadInfo() : null;
        mServerSocketProperties.add(new SocketProperties(
            regId, SOCKET_ROLE_CONNECTION, SOCKET_CONNECTION_STATE_LISTENING, offloadInfo));
        Log.i(
                TAG,
                "createSocketChannel: type="
                        + type
                        + ", serviceName="
                        + serviceName
                        + ", uuid="
                        + uuid
                        + ", port="
                        + port
                        + ", regId="
                        + regId
                        + ", offload="
                        + offload
                        + ", from "
                        + Utils.getUidPidString());

        return marshalFd(
                mService.getNative()
                        .createSocketChannel(
                                type,
                                serviceName,
                                Utils.uuidToByteArray(uuid),
                                port,
                                flag,
                                appUid,
                                regId,
                                offload));
    }

    @Override
    public void requestMaximumTxDataLength(BluetoothDevice device) {
        enforceActiveUser();

        if (!Utils.checkConnectPermissionForPreflight(mService)) {
            return;
        }

        mService.getNative()
                .requestMaximumTxDataLength(Utils.getBytesFromAddress(device.getAddress()));
    }

    @Override
    public int getL2capLocalChannelId(ParcelUuid connectionUuid, AttributionSource source) {
        AdapterService service = mService;
        if (service == null
                || !Utils.callerIsSystemOrActiveOrManagedUser(
                        service, TAG, "getL2capLocalChannelId")
                || !Utils.checkConnectPermissionForDataDelivery(
                        service, source, "BluetoothSocketManagerBinder getL2capLocalChannelId")) {
            return INVALID_CID;
        }
        Utils.enforceBluetoothPrivilegedPermission(service);
        return service.getNative().getSocketL2capLocalChannelId(connectionUuid);
    }

    @Override
    public int getL2capRemoteChannelId(ParcelUuid connectionUuid, AttributionSource source) {
        AdapterService service = mService;
        if (service == null
                || !Utils.callerIsSystemOrActiveOrManagedUser(
                        service, TAG, "getL2capRemoteChannelId")
                || !Utils.checkConnectPermissionForDataDelivery(
                        service, source, "BluetoothSocketManagerBinder getL2capRemoteChannelId")) {
            return INVALID_CID;
        }
        Utils.enforceBluetoothPrivilegedPermission(service);
        return service.getNative().getSocketL2capRemoteChannelId(connectionUuid);
    }

    void socketStateChangeCallback(int regId, UUID connUuid, int status, int role, int state,
                                    int protocol, int channel, int txMtu, int localMps, int remoteMps,
                                    int localCredit, int remoteCredit,
                                    int localCid, int remoteCid, int aclHandle, boolean offload) {
        Log.v(TAG,
                "socketStateChangeCallback: regId="
                    + regId
                    + ", connUuid="
                    + connUuid
                    + ", status="
                    + status
                    + ", role="
                    + role
                    + ", state="
                    + state
                    + ", protocol="
                    + protocol
                    + ", channel="
                    + channel
                    + ", txMtu="
                    + txMtu
                    + ", localMps="
                    + localMps
                    + ", remoteMps="
                    + remoteMps
                    + ", localCredit="
                    + localCredit
                    + ", remoteCredit="
                    + remoteCredit
                    + ", localCid="
                    + String.format("0x%04x", localCid)
                    + ", remoteCid="
                    + String.format("0x%04x", remoteCid)
                    + ", aclHandle="
                    + String.format("0x%04x", aclHandle)
                    + ", offload="
                    + offload
        );

        if (state == SOCKET_CONNECTION_STATE_CONNECTED && status != 0) {
            Log.w(TAG, "Socket connection state was not successful: status " + status);
            return;
        }
        switch (state) {
            case SOCKET_CONNECTION_STATE_CONNECTED:
                Log.d(TAG, "Callback socket connected: regId " + regId + " connUuid " + connUuid);
                break;
            case SOCKET_CONNECTION_STATE_DISCONNECTED:
                Log.d(TAG, "Callback socket disconnected: regId " + regId + " connUuid " + connUuid);
                break;
            default:
                Log.w(TAG, "Unknown socket connection state " + state);
                break;
        }
    }

    SocketProperties getSocketProperties(List<SocketProperties> socketProperties, int regId) {
        for (SocketProperties prop : socketProperties) {
            if (prop.mRegId == regId) {
                return prop;
            }
        }
        return null;
    }

    SocketProperties getSocketProperties(List<SocketProperties> socketProperties, int regId, UUID uuid) {
        for (SocketProperties prop : socketProperties) {
            if (prop.mRegId == regId && prop.mConnUuid == uuid) {
                return prop;
            }
        }
        return null;
    }

    void leDataLengthChangeCallback(int handle, int txDataLen, int rxDataLen) {
        Log.d(TAG, "leDataLengthChangeCallback: handle="
                    + String.format("0x%04x", handle)
                    + ", txDataLen="
                    + txDataLen
                    + ", rxDataLen="
                    + rxDataLen
        );
    }

    void classicPmChangeCallback(int handle, int mode, int interval) {
        Log.d(TAG, "classicPmChangeCallback: handle="
                    + String.format("0x%04x", handle)
                    + ", mode="
                    + (mode == CLASSIC_POWER_MODE_ACTIVE ? "active" : "sniff" )
                    + ", interval="
                    + interval
        );
    }
  
    static class SocketProperties {
        int mRegId;
        UUID mConnUuid;
        int mRole;
        int mState;
        int mProtocol;
        int mChannel;
        int mTxMtu;
        int mLocalMps;
        int mRemoteMps;
        int mLocalCredit;
        int mRemoteCredit;
        int mLocalCid;
        int mRemoteCid;
        int mAclHandle;
        OffloadInfo mOffloadInfo;

        SocketProperties(int regId, int role, int state, OffloadInfo offloadInfo) {
            mRegId = regId;
            mRole = role;
            mState = state;
            mOffloadInfo = offloadInfo;
        }
    }

    static class SocketAppInfo {
        int mRegId;
        int mAppUid;
        boolean mOffload;

        SocketAppInfo(int regId, int appUid, boolean offload) {
            mRegId = regId;
            mAppUid = appUid;
            mOffload = offload;
        }
    }

    static class OffloadInfo {
        int mEndPointId;
        int mHubId;
    }

    private void enforceActiveUser() {
        if (!Utils.checkCallerIsSystemOrActiveOrManagedUser(mService, TAG)) {
            throw new SecurityException("Not allowed for non-active user");
        }
    }

    private static ParcelFileDescriptor marshalFd(int fd) {
        if (fd == INVALID_FD) {
            return null;
        }
        return ParcelFileDescriptor.adoptFd(fd);
    }
}
