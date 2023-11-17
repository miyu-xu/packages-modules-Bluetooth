/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

/** Utility class for socket metrics {@hide} */
public class SocketMetrics {
    private static final String TAG = "SocketMetrics";

    private static final int TYPE_RFCOMM = 1;
    private static final int TYPE_L2CAP_LE = 4;

    private static final int SOCKET_NO_ERROR = -1;

    // Defined in BluetoothProtoEnums.L2capCocConnectionResult of proto logging
    private static final int RESULT_L2CAP_CONN_UNKNOWN = 0;
    /*package*/ static final int RESULT_L2CAP_CONN_SUCCESS = 1;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_FAILED = 1000;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_CLOSED = 1001;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_UNABLE_TO_SEND_RPC = 1002;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_NULL_BLUETOOTH_DEVICE = 1003;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_GET_SOCKET_MANAGER_FAILED = 1004;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_NULL_FILE_DESCRIPTOR = 1005;
    /*package*/ static final int RESULT_L2CAP_CONN_SERVER_FAILURE = 2000;

    // Defined in BluetoothRfcommProtoEnums.RfcommConnectionResult of proto logging
    private static final int RFCOMM_CONN_RESULT_SUCCESS = 0;
    private static final int RFCOMM_CONN_RESULT_SOCKET_CONNECTION_FAILED = 1;
    private static final int RFCOMM_CONN_RESULT_SOCKET_CONNECTION_CLOSED = 2;
    private static final int RFCOMM_CONN_RESULT_UNABLE_TO_SEND_RPC = 3;
    private static final int RFCOMM_CONN_RESULT_NULL_BLUETOOTH_DEVICE = 4;
    private static final int RFCOMM_CONN_RESULT_GET_SOCKET_MANAGER_FAILED = 5;
    private static final int RFCOMM_CONN_RESULT_NULL_FILE_DESCRIPTOR = 6;
    private static final int RFCOMM_CONN_RESULT_FAILURE_UNKNOWN = 7;

    // Defined in BluetoothRfcommProtoEnums.ServiceClassUUID of proto logging
    private static final int SERVICE_CLASS_UUID_UNSPECIFIED = 0;
    private static final int SERVICE_CLASS_UUID_SERVICE_DISCOVERY_SERVER = 1;
    private static final int SERVICE_CLASS_UUID_BROWSE_GROUP_DESCRIPTOR = 2;
    private static final int SERVICE_CLASS_UUID_PUBLIC_BROWSE_GROUP = 3;
    private static final int SERVICE_CLASS_UUID_SERIAL_PORT = 4;
    private static final int SERVICE_CLASS_UUID_LAN_ACCESS_USING_PPP = 5;
    private static final int SERVICE_CLASS_UUID_DIALUP_NETWORKING = 6;
    private static final int SERVICE_CLASS_UUID_IRMC_SYNC = 7;
    private static final int SERVICE_CLASS_UUID_OBEX_OBJECT_PUSH = 8;
    private static final int SERVICE_CLASS_UUID_OBEX_FILE_TRANSFER = 9;
    private static final int SERVICE_CLASS_UUID_IRMC_SYNC_COMMAND = 10;
    private static final int SERVICE_CLASS_UUID_HEADSET = 11;
    private static final int SERVICE_CLASS_UUID_CORDLESS_TELEPHONY = 12;
    private static final int SERVICE_CLASS_UUID_AUDIO_SOURCE = 13;
    private static final int SERVICE_CLASS_UUID_AUDIO_SINK = 14;
    private static final int SERVICE_CLASS_UUID_AV_REM_CTRL_TARGET = 15;
    private static final int SERVICE_CLASS_UUID_ADV_AUDIO_DISTRIBUTION = 16;
    private static final int SERVICE_CLASS_UUID_AV_REMOTE_CONTROL = 17;
    private static final int SERVICE_CLASS_UUID_AV_REM_CTRL_CONTROL = 18;
    private static final int SERVICE_CLASS_UUID_INTERCOM = 19;
    private static final int SERVICE_CLASS_UUID_FAX = 20;
    private static final int SERVICE_CLASS_UUID_HEADSET_AUDIO_GATEWAY = 21;
    private static final int SERVICE_CLASS_UUID_WAP = 22;
    private static final int SERVICE_CLASS_UUID_WAP_CLIENT = 23;
    private static final int SERVICE_CLASS_UUID_PANU = 24;
    private static final int SERVICE_CLASS_UUID_NAP = 25;
    private static final int SERVICE_CLASS_UUID_GN = 26;
    private static final int SERVICE_CLASS_UUID_DIRECT_PRINTING = 27;
    private static final int SERVICE_CLASS_UUID_REFERENCE_PRINTING = 28;
    private static final int SERVICE_CLASS_UUID_IMAGING = 29;
    private static final int SERVICE_CLASS_UUID_IMAGING_RESPONDER = 30;
    private static final int SERVICE_CLASS_UUID_IMAGING_AUTO_ARCHIVE = 31;
    private static final int SERVICE_CLASS_UUID_IMAGING_REF_OBJECTS = 32;
    private static final int SERVICE_CLASS_UUID_HF_HANDSFREE = 33;
    private static final int SERVICE_CLASS_UUID_AG_HANDSFREE = 34;
    private static final int SERVICE_CLASS_UUID_DIR_PRT_REF_OBJ_SERVICE = 35;
    private static final int SERVICE_CLASS_UUID_REFLECTED_UI = 36;
    private static final int SERVICE_CLASS_UUID_BASIC_PRINTING = 37;
    private static final int SERVICE_CLASS_UUID_PRINTING_STATUS = 38;
    private static final int SERVICE_CLASS_UUID_HUMAN_INTERFACE = 39;
    private static final int SERVICE_CLASS_UUID_CABLE_REPLACEMENT = 40;
    private static final int SERVICE_CLASS_UUID_HCRP_PRINT = 41;
    private static final int SERVICE_CLASS_UUID_HCRP_SCAN = 42;
    private static final int SERVICE_CLASS_UUID_COMMON_ISDN_ACCESS = 43;
    private static final int SERVICE_CLASS_UUID_VIDEO_CONFERENCING_GW = 44;
    private static final int SERVICE_CLASS_UUID_UDI_MT = 45;
    private static final int SERVICE_CLASS_UUID_UDI_TA = 46;
    private static final int SERVICE_CLASS_UUID_VCP = 47;
    private static final int SERVICE_CLASS_UUID_SAP = 48;
    private static final int SERVICE_CLASS_UUID_PBAP_PCE = 49;
    private static final int SERVICE_CLASS_UUID_PBAP_PSE = 50;
    private static final int SERVICE_CLASS_UUID_PHONE_ACCESS = 51;
    private static final int SERVICE_CLASS_UUID_HEADSET_HS = 52;
    private static final int SERVICE_CLASS_UUID_MPS_PROFILE = 53;
    private static final int SERVICE_CLASS_UUID_MPS_SC = 54;
    private static final int SERVICE_CLASS_UUID_PNP_INFORMATION = 55;
    private static final int SERVICE_CLASS_UUID_GENERIC_NETWORKING = 56;
    private static final int SERVICE_CLASS_UUID_GENERIC_FILETRANSFER = 57;
    private static final int SERVICE_CLASS_UUID_GENERIC_AUDIO = 58;
    private static final int SERVICE_CLASS_UUID_GENERIC_TELEPHONY = 59;
    private static final int SERVICE_CLASS_UUID_UPNP_SERVICE = 60;
    private static final int SERVICE_CLASS_UUID_UPNP_IP_SERVICE = 61;
    private static final int SERVICE_CLASS_UUID_ESDP_UPNP_IP_PAN = 62;
    private static final int SERVICE_CLASS_UUID_ESDP_UPNP_IP_LAP = 63;
    private static final int SERVICE_CLASS_UUID_ESDP_UPNP_IP_L2CAP = 64;
    private static final int SERVICE_CLASS_UUID_VIDEO_SOURCE = 65;
    private static final int SERVICE_CLASS_UUID_VIDEO_SINK = 66;
    private static final int SERVICE_CLASS_UUID_VIDEO_DISTRIBUTION = 67;
    private static final int SERVICE_CLASS_UUID_HDP_PROFILE = 68;
    private static final int SERVICE_CLASS_UUID_HDP_SOURCE = 69;
    private static final int SERVICE_CLASS_UUID_HDP_SINK = 70;
    private static final int SERVICE_CLASS_UUID_MAP_PROFILE = 71;
    private static final int SERVICE_CLASS_UUID_MESSAGE_ACCESS = 72;
    private static final int SERVICE_CLASS_UUID_MESSAGE_NOTIFICATION = 73;
    private static final int SERVICE_CLASS_UUID_GAP_SERVER = 74;
    private static final int SERVICE_CLASS_UUID_GATT_SERVER = 75;
    private static final int SERVICE_CLASS_UUID_DEVICE_INFO = 76;
    private static final int SERVICE_CLASS_UUID_LE_HID = 77;
    private static final int SERVICE_CLASS_UUID_SCAN_PARAM = 78;
    private static final int SERVICE_CLASS_UUID_GMCS_SERVER = 79;
    private static final int SERVICE_CLASS_UUID_GTBS_SERVER = 80;
    private static final int SERVICE_CLASS_UUID_TMAS_SERVER = 81;

    protected static void logSocketConnect(
            int socketExceptionCode,
            long socketConnectionTimeNanos,
            int connType,
            BluetoothDevice device,
            int port,
            boolean auth,
            long socketCreationTimeNanos,
            long socketCreationLatencyNanos,
            UUID uuid) {
        IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService();
        int errCode = getConnectStatusCode(connType, socketExceptionCode);
        int uuidCode = getUuidCode(uuid);

        if (connType == TYPE_L2CAP_LE) {
            try {
                final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
                bluetoothProxy.logL2capcocClientConnection(
                        device,
                        port,
                        auth,
                        errCode,
                        socketCreationTimeNanos, // to calculate end to end latency
                        socketCreationLatencyNanos, // latency of the constructor
                        socketConnectionTimeNanos, // to calculate the latency of connect()
                        recv);
                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.w(TAG, "logL2capcocClientConnection failed due to remote exception");
            }
        } else if (connType == TYPE_RFCOMM) {
            try {
                final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
                bluetoothProxy.logRfcommConnectionAttempt(
                        device,
                        auth,
                        errCode,
                        socketCreationTimeNanos, // to calculate end to end latency
                        uuidCode,
                        recv);
                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.w(TAG, "logRfcommConnectionAttempt failed due to remote exception");
            }
        } else {
            Log.w(TAG, "No metrics for connection type " + connType);
        }

    }



    protected static void logSocketAccept(
            BluetoothSocket acceptedSocket,
            BluetoothSocket socket,
            int connType,
            int channel,
            int timeout,
            int result,
            long socketCreationTimeMillis,
            long socketCreationLatencyMillis,
            long socketConnectionTimeMillis) {
        if (connType != BluetoothSocket.TYPE_L2CAP_LE) {
            return;
        }
        IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService();
        if (bluetoothProxy == null) {
            Log.w(TAG, "bluetoothProxy is null while trying to log l2cap coc server connection");
            return;
        }
        try {
            final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
            bluetoothProxy.logL2capcocServerConnection(
                    acceptedSocket == null ? null : acceptedSocket.getRemoteDevice(),
                    channel,
                    socket.isAuth(),
                    result,
                    socketCreationTimeMillis, // pass creation time to calculate end to end latency
                    socketCreationLatencyMillis, // socket creation latency
                    socketConnectionTimeMillis, // send connection start time for connection latency
                    timeout,
                    recv);
            recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);

        } catch (RemoteException | TimeoutException e) {
            Log.w(TAG, "logL2capcocServerConnection failed due to remote exception");
        }
    }

    private static int getConnectStatusCode(int connType, int socketExceptionCode) {
        if (connType == TYPE_L2CAP_LE) {
            switch (socketExceptionCode) {
                case (SOCKET_NO_ERROR):
                    return RESULT_L2CAP_CONN_SUCCESS;
                case (BluetoothSocketException.NULL_DEVICE):
                    return RESULT_L2CAP_CONN_BLUETOOTH_NULL_BLUETOOTH_DEVICE;
                case (BluetoothSocketException.SOCKET_MANAGER_FAILURE):
                    return RESULT_L2CAP_CONN_BLUETOOTH_GET_SOCKET_MANAGER_FAILED;
                case (BluetoothSocketException.SOCKET_CLOSED):
                    return RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_CLOSED;
                case (BluetoothSocketException.SOCKET_CONNECTION_FAILURE):
                    return RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_FAILED;
                case (BluetoothSocketException.RPC_FAILURE):
                    return RESULT_L2CAP_CONN_BLUETOOTH_UNABLE_TO_SEND_RPC;
                default:
                    return RESULT_L2CAP_CONN_UNKNOWN;
            }
        } else if (connType == TYPE_RFCOMM) {
            switch (socketExceptionCode) {
                case (SOCKET_NO_ERROR):
                    return RFCOMM_CONN_RESULT_SUCCESS;
                case (BluetoothSocketException.NULL_DEVICE):
                    return RFCOMM_CONN_RESULT_NULL_BLUETOOTH_DEVICE;
                case (BluetoothSocketException.SOCKET_MANAGER_FAILURE):
                    return RFCOMM_CONN_RESULT_GET_SOCKET_MANAGER_FAILED;
                case (BluetoothSocketException.SOCKET_CLOSED):
                    return RFCOMM_CONN_RESULT_SOCKET_CONNECTION_CLOSED;
                case (BluetoothSocketException.SOCKET_CONNECTION_FAILURE):
                    return RFCOMM_CONN_RESULT_SOCKET_CONNECTION_FAILED;
                case (BluetoothSocketException.RPC_FAILURE):
                    return RFCOMM_CONN_RESULT_UNABLE_TO_SEND_RPC;
                default:
                    return RFCOMM_CONN_RESULT_FAILURE_UNKNOWN;
            }
        } else {
            return -1;
        }
    }

    private static int getUuidCode(UUID uuid) {
        if (uuid == UUID.fromString("00001000-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_SERVICE_DISCOVERY_SERVER;
        } else if (uuid == UUID.fromString("00001001-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_BROWSE_GROUP_DESCRIPTOR;
        } else if (uuid == UUID.fromString("00001002-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PUBLIC_BROWSE_GROUP;
        } else if (uuid == UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_SERIAL_PORT;
        } else if (uuid == UUID.fromString("00001102-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_LAN_ACCESS_USING_PPP;
        } else if (uuid == UUID.fromString("00001103-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_DIALUP_NETWORKING;
        } else if (uuid == UUID.fromString("00001104-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_IRMC_SYNC;
        } else if (uuid == UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_OBEX_OBJECT_PUSH;
        } else if (uuid == UUID.fromString("00001106-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_OBEX_FILE_TRANSFER;
        } else if (uuid == UUID.fromString("00001107-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_IRMC_SYNC_COMMAND;
        } else if (uuid == UUID.fromString("00001108-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HEADSET;
        } else if (uuid == UUID.fromString("00001109-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_CORDLESS_TELEPHONY;
        } else if (uuid == UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_AUDIO_SOURCE;
        } else if (uuid == UUID.fromString("0000110B-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_AUDIO_SINK;
        } else if (uuid == UUID.fromString("0000110C-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_AV_REM_CTRL_TARGET;
        } else if (uuid == UUID.fromString("0000110D-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_ADV_AUDIO_DISTRIBUTION;
        } else if (uuid == UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_AV_REMOTE_CONTROL;
        } else if (uuid == UUID.fromString("0000110F-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_AV_REM_CTRL_CONTROL;
        } else if (uuid == UUID.fromString("00001110-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_INTERCOM;
        } else if (uuid == UUID.fromString("00001111-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_FAX;
        } else if (uuid == UUID.fromString("00001112-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HEADSET_AUDIO_GATEWAY;
        } else if (uuid == UUID.fromString("00001113-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_WAP;
        } else if (uuid == UUID.fromString("00001114-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_WAP_CLIENT;
        } else if (uuid == UUID.fromString("00001115-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PANU;
        } else if (uuid == UUID.fromString("00001116-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_NAP;
        } else if (uuid == UUID.fromString("00001117-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GN;
        } else if (uuid == UUID.fromString("00001118-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_DIRECT_PRINTING;
        } else if (uuid == UUID.fromString("00001119-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_REFERENCE_PRINTING;
        } else if (uuid == UUID.fromString("0000111A-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_IMAGING;
        } else if (uuid == UUID.fromString("0000111B-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_IMAGING_RESPONDER;
        } else if (uuid == UUID.fromString("0000111C-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_IMAGING_AUTO_ARCHIVE;
        } else if (uuid == UUID.fromString("0000111D-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_IMAGING_REF_OBJECTS;
        } else if (uuid == UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HF_HANDSFREE;
        } else if (uuid == UUID.fromString("0000111F-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_AG_HANDSFREE;
        } else if (uuid == UUID.fromString("00001120-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_DIR_PRT_REF_OBJ_SERVICE;
        } else if (uuid == UUID.fromString("00001121-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_REFLECTED_UI;
        } else if (uuid == UUID.fromString("00001122-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_BASIC_PRINTING;
        } else if (uuid == UUID.fromString("00001123-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PRINTING_STATUS;
        } else if (uuid == UUID.fromString("00001124-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HUMAN_INTERFACE;
        } else if (uuid == UUID.fromString("00001125-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_CABLE_REPLACEMENT;
        } else if (uuid == UUID.fromString("00001126-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HCRP_PRINT;
        } else if (uuid == UUID.fromString("00001127-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HCRP_SCAN;
        } else if (uuid == UUID.fromString("00001128-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_COMMON_ISDN_ACCESS;
        } else if (uuid == UUID.fromString("00001129-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_VIDEO_CONFERENCING_GW;
        } else if (uuid == UUID.fromString("0000112A-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_UDI_MT;
        } else if (uuid == UUID.fromString("0000112B-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_UDI_TA;
        } else if (uuid == UUID.fromString("0000112C-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_VCP;
        } else if (uuid == UUID.fromString("0000112D-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_SAP;
        } else if (uuid == UUID.fromString("0000112E-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PBAP_PCE;
        } else if (uuid == UUID.fromString("0000112F-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PBAP_PSE;
        } else if (uuid == UUID.fromString("00001130-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PHONE_ACCESS;
        } else if (uuid == UUID.fromString("00001131-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HEADSET_HS;
        } else if (uuid == UUID.fromString("0000113A-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_MPS_PROFILE;
        } else if (uuid == UUID.fromString("0000113B-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_MPS_SC;
        } else if (uuid == UUID.fromString("00001200-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_PNP_INFORMATION;
        } else if (uuid == UUID.fromString("00001201-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GENERIC_NETWORKING;
        } else if (uuid == UUID.fromString("00001202-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GENERIC_FILETRANSFER;
        } else if (uuid == UUID.fromString("00001203-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GENERIC_AUDIO;
        } else if (uuid == UUID.fromString("00001204-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GENERIC_TELEPHONY;
        } else if (uuid == UUID.fromString("00001205-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_UPNP_SERVICE;
        } else if (uuid == UUID.fromString("00001206-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_UPNP_IP_SERVICE;
        } else if (uuid == UUID.fromString("00001300-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_ESDP_UPNP_IP_PAN;
        } else if (uuid == UUID.fromString("00001301-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_ESDP_UPNP_IP_LAP;
        } else if (uuid == UUID.fromString("00001302-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_ESDP_UPNP_IP_L2CAP;
        } else if (uuid == UUID.fromString("00001303-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_VIDEO_SOURCE;
        } else if (uuid == UUID.fromString("00001304-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_VIDEO_SINK;
        } else if (uuid == UUID.fromString("00001305-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_VIDEO_DISTRIBUTION;
        } else if (uuid == UUID.fromString("00001400-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HDP_PROFILE;
        } else if (uuid == UUID.fromString("00001401-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HDP_SOURCE;
        } else if (uuid == UUID.fromString("00001402-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_HDP_SINK;
        } else if (uuid == UUID.fromString("00001134-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_MAP_PROFILE;
        } else if (uuid == UUID.fromString("00001132-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_MESSAGE_ACCESS;
        } else if (uuid == UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_MESSAGE_NOTIFICATION;
        } else if (uuid == UUID.fromString("00001801-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GAP_SERVER;
        } else if (uuid == UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GATT_SERVER;
        } else if (uuid == UUID.fromString("00001812-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_LE_HID;
        } else if (uuid == UUID.fromString("00001813-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_SCAN_PARAM;
        } else if (uuid == UUID.fromString("00001849-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GMCS_SERVER;
        } else if (uuid == UUID.fromString("0000184C-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_GTBS_SERVER;
        } else if (uuid == UUID.fromString("00001855-0000-1000-8000-00805F9B34FB")) {
            return SERVICE_CLASS_UUID_TMAS_SERVER;
        } else {
            return SERVICE_CLASS_UUID_UNSPECIFIED;
        }
    }
}
