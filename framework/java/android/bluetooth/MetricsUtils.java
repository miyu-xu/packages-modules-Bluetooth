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

import java.util.concurrent.TimeoutException;

/**
 * Utility class with metrics methods
 */
public class MetricsUtils {
    private static final String TAG = "MetricsUtils";
    private static final int TYPE_RFCOMM = 1;
    private static final int TYPE_L2CAP_LE = 4;

    private static final int SOCKET_SUCCESS_CODE = -1;

    // Defined in BluetoothRfcommProtoEnums.RfcommConnectionResult of proto logging
    private static final int RFCOMM_CONN_RESULT_SUCCESS = 0;
    private static final int RFCOMM_CONN_RESULT_SOCKET_CONNECTION_FAILED = 1;
    private static final int RFCOMM_CONN_RESULT_SOCKET_CONNECTION_CLOSED = 2;
    private static final int RFCOMM_CONN_RESULT_UNABLE_TO_SEND_RPC = 3;
    private static final int RFCOMM_CONN_RESULT_NULL_BLUETOOTH_DEVICE = 4;
    private static final int RFCOMM_CONN_RESULT_GET_SOCKET_MANAGER_FAILED = 5;
    private static final int RFCOMM_CONN_RESULT_NULL_FILE_DESCRIPTOR = 6;
    private static final int RFCOMM_CONN_RESULT_FAILURE_UNKNOWN = 7;

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

    public static void metricsHelperL2capCoc(
            IBluetooth bluetoothProxy,
            int socketExceptionCode,
            long socketConnectionTimeMillis,
            int connType,
            BluetoothDevice device,
            int port,
            boolean auth,
            long socketCreationTimeMillis,
            long socketCreationLatencyMillis) {
        int errCode = getMetricsStatusCode(connType, socketExceptionCode);
        if (connType == TYPE_L2CAP_LE) {
            try {
                final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
                bluetoothProxy.logL2capcocClientConnection(
                        device,
                        port,
                        auth,
                        errCode,
                        socketCreationTimeMillis, // to calculate end to end latency
                        socketCreationLatencyMillis, // latency of the constructor
                        socketConnectionTimeMillis, // to calculate the latency of connect()
                        recv);
                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.w(TAG, "logL2capcocClientConnection failed due to remote exception");
            }
        }
    }

    private static int getMetricsStatusCode(int connType, int socketExceptionCode) {
        if (connType == TYPE_RFCOMM) {
            switch (socketExceptionCode) {
                case (SOCKET_SUCCESS_CODE):
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
        } else if (connType == TYPE_L2CAP_LE) {
            switch (socketExceptionCode) {
                case (SOCKET_SUCCESS_CODE):
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
        } else {
            return -1;
        }
    }
}
