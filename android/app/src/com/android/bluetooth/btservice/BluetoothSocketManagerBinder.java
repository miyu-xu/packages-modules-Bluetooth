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

import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

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

    private static int sClientRegistrationId = 0;
    private static int sServerRegistrationId = 0;

    private AdapterService mService;
    private BluetoothSocketContextMap mClientMap;
    private BluetoothSocketContextMap mServerMap;

    BluetoothSocketManagerBinder(AdapterService service) {
        mService = service;
        mClientMap = new BluetoothSocketContextMap();
        mServerMap = new BluetoothSocketContextMap();
    }

    void cleanUp() {
        mService = null;
        sClientRegistrationId = 0;
        sServerRegistrationId = 0;
        if (mClientMap != null) {
            mClientMap.clear();
        }
        if (mServerMap != null) {
            mServerMap.clear();
        }
    }

    @Override
    public ParcelFileDescriptor connectSocket(
            BluetoothDevice device, int type, ParcelUuid uuid, int port, int flag) {

        enforceActiveUser();

        if (!Utils.checkConnectPermissionForPreflight(mService)) {
            return null;
        }

        String brEdrAddress =
                Flags.identityAddressNullIfNotKnown()
                        ? Utils.getBrEdrAddress(device)
                        : mService.getIdentityAddress(device.getAddress());
        int regId = --sClientRegistrationId;
        int appUid = Binder.getCallingUid();
        mClientMap.add(regId, appUid, type, false);

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
                                regId));
    }

    @Override
    public ParcelFileDescriptor createSocketChannel(
            int type, String serviceName, ParcelUuid uuid, int port, int flag) {

        enforceActiveUser();

        if (!Utils.checkConnectPermissionForPreflight(mService)) {
            return null;
        }

        int regId = ++sServerRegistrationId;
        int appUid = Binder.getCallingUid();
        mServerMap.add(regId, appUid, type, false);

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
                                regId));
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
        service.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
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
        service.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        return service.getNative().getSocketL2capRemoteChannelId(connectionUuid);
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

    void socketStateChangeCallback(int regId, UUID connUuid, int status, int role, int state) {
        Log.i(
                TAG,
                "socketStateChangeCallback: regId="
                        + regId
                        + ", connUuid="
                        + connUuid
                        + ", status="
                        + status
                        + ", role="
                        + role
                        + ", state="
                        + state);

        if (role == SOCKET_ROLE_LISTEN) {
            handleListenSocketStateChange(regId, status, state);
        } else if (role == SOCKET_ROLE_CONNECTION) {
            handleConnectionSocketStateChange(regId, connUuid, status, state);
        }
    }

    void handleListenSocketStateChange(int regId, int status, int state) {
        switch (state) {
            case SOCKET_CONNECTION_STATE_DISCONNECTED:
                mClientMap.removeApp(regId);
                break;
        }
    }

    void handleConnectionSocketStateChange(int regId, UUID connUuid, int status, int state) {
        switch (state) {
            case SOCKET_CONNECTION_STATE_CONNECTED:
                if (status != 0) {
                    Log.w(TAG, "Socket connection state was not successful: status " + status);
                    return;
                }
                if (regId > 0) {
                    mServerMap.addConnection(regId, connUuid, false);
                } else {
                    mClientMap.addConnection(regId, connUuid, false);
                }
                break;
            case SOCKET_CONNECTION_STATE_DISCONNECTED:
                if (regId > 0) {
                    mServerMap.removeConnection(connUuid);
                } else {
                    mClientMap.removeConnection(connUuid);
                    mClientMap.removeApp(regId);
                }
                break;
            default:
                Log.w(TAG, "handleConnectionSocketStateChange: unknown state=" + state);
                break;
        }
    }
}
