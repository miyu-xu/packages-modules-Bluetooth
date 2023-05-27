/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.BluetoothStatsLog;
import com.android.bluetooth.Utils;

import com.google.protobuf.ByteString;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Logs events related to an RFCOMM BluetoothSocket initiated by the local device. Refer to
 * go/bt_rfcomm_metric for the design.
 */
public final class RfcommSessionizer {
    private static final String TAG = "RfcommSessionizer";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    // Duration over which we track previous attempts as "retries"
    private static final long RETRY_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    // Time after connection attempt at which point we consider it to have failed
    private static final long FAILURE_TIMEOUT_MS = 30 * 1000; // 30 seconds

    // All open sockets, indexed by their attemptId
    private final HashMap<Integer, RfcommSocketMetadata> mPendingSockets = new HashMap<>();
    // All previous failed connection attempts to each peer address, sorted from most->least recent
    private final HashMap<BluetoothDevice, ArrayDeque<Long>> mFailedAttemptTimesByDevice =
            new HashMap<>();

    private final MetricsLogger mLogger;

    private final Handler mHandler;

    RfcommSessionizer(MetricsLogger logger, Looper looper) {
        mLogger = logger;
        mHandler = new Handler(looper);
    }

    private long currTimeMs() {
        // we use nanoTime() since it is guaranteed to be be monotonic
        return System.nanoTime() / 1000 / 1000;
    }

    private synchronized void onConnectionAttemptExpired(int attemptId) {
        if (mPendingSockets.containsKey(attemptId)) {
            logRfcommClientConnectionComplete(attemptId, /* success = */ false);
        }
    }

    // Drop old retries for given key, and count the number remaining
    private synchronized int getNumberOfRetriesAndDropOldOnes(BluetoothDevice device) {
        var failedAttempts = mFailedAttemptTimesByDevice.get(device);
        if (failedAttempts == null) {
            return 0;
        }
        while (failedAttempts.peekFirst() != null
                && failedAttempts.peekFirst() + RETRY_TIMEOUT_MS < currTimeMs()) {
            failedAttempts.removeFirst();
        }
        var out = failedAttempts.size();
        if (out == 0) {
            mFailedAttemptTimesByDevice.remove(device);
        }
        return out;
    }

    private synchronized BluetoothMetricsProto.BluetoothRfcommConnectionMetadata
            exportSocketMetadata(RfcommSocketMetadata socket) {
        // classOfDevice
        int classOfDevice = -1;
        var bluetoothClass = socket.mDevice.getBluetoothClass();
        if (bluetoothClass != null) {
            classOfDevice = bluetoothClass.getClassOfDevice();
        }

        // manufacturerBytes
        var addressBytes = Utils.getByteAddress(socket.mDevice);
        var manufacturerBytes = ByteString.copyFrom(Arrays.copyOfRange(addressBytes, 0, 3));

        // hashedName
        var hashedName =
                mLogger.hashMatchedString(mLogger.getMatchFromDeviceName(socket.mDevice.getName()));
        if (hashedName == null) {
            hashedName = "";
        }

        // peerDevice
        var peerDevice =
                BluetoothMetricsProto.BluetoothPeerDevice.newBuilder()
                        .setClassOfDevice(classOfDevice)
                        .setManufacturerBytes(manufacturerBytes)
                        .setHashedName(hashedName)
                        .build();

        // security
        var security =
                socket.mIsSecured
                        ? BluetoothMetricsProto.BluetoothRfcommConnectionMetadata
                                .SecurityRequirement.SECURE
                        : BluetoothMetricsProto.BluetoothRfcommConnectionMetadata
                                .SecurityRequirement.NONE;

        // metadata
        var metadataBuilder =
                BluetoothMetricsProto.BluetoothRfcommConnectionMetadata.newBuilder()
                        .setPeerDeviceInfo(peerDevice)
                        .setSecurity(security)
                        .setCallerUid(socket.mAppUid);

        if (socket.mPort > 0) {
            metadataBuilder.setPort(socket.mPort);
        } else if (socket.mUuid != null) {
            metadataBuilder.setUuid(socket.mUuid.toString());
        } else {
            Log.w(TAG, "Got invalid BluetoothSocket with null port and UUID");
        }

        return metadataBuilder.build();
    }

    /** Records when an RFCOMM connection attempt is made from an application. */
    public synchronized void logRfcommConnectionAttemptStart(
            int attemptId,
            BluetoothDevice device,
            boolean isSecured,
            ParcelUuid uuid,
            int port,
            int appUid) {
        mPendingSockets.put(
                attemptId,
                new RfcommSocketMetadata(device, isSecured, uuid, port, appUid, currTimeMs()));

        mHandler.postDelayed(() -> onConnectionAttemptExpired(attemptId), FAILURE_TIMEOUT_MS);
    }

    /** Records when an RFCOMM connection attempt completes / fails, as seen by an application. */
    public synchronized void logRfcommClientConnectionComplete(int attemptId, boolean success) {
        // pop socket from map of pending sockets
        var socket = mPendingSockets.get(attemptId);
        if (socket == null) {
            Log.e(TAG, "Got RFCOMM connection complete for unknown attemptId " + attemptId);
            return;
        }
        mPendingSockets.remove(attemptId);

        // track and update list of previous retries
        if (!success) {
            // if failed, add ourselves to list of failures
            mFailedAttemptTimesByDevice.putIfAbsent(socket.mDevice, new ArrayDeque<>());
            mFailedAttemptTimesByDevice.get(socket.mDevice).addLast(currTimeMs());
        }
        var retriesBeforeCurrent = getNumberOfRetriesAndDropOldOnes(socket.mDevice);
        if (success) {
            // if success, after counting failed retries, we should clear them
            // all out
            mFailedAttemptTimesByDevice.remove(socket.mDevice);
        }

        var status =
                success
                        ? BluetoothMetricsProto.BluetoothRfcommConnectionAttemptComplete.Status
                                .SUCCESS
                        : BluetoothMetricsProto.BluetoothRfcommConnectionAttemptComplete.Status
                                .UNKNOWN_FAILED;

        var latencyMs = currTimeMs() - socket.mStartTimeMs;

        BluetoothStatsLog.write(
                BluetoothStatsLog.BLUETOOTH_RFCOMM_CONNECTION_ATTEMPT_COMPLETE,
                status.getNumber(),
                exportSocketMetadata(socket).toByteArray(),
                (int) latencyMs,
                retriesBeforeCurrent);
    }

    /** Records when an RFCOMM connection closes, whether initiated locally or by the peer. */
    public synchronized void logRfcommClientDisconnection() {
        // TODO(aryarahul)
    }

    private static class RfcommSocketMetadata {
        final BluetoothDevice mDevice;
        final boolean mIsSecured;
        final ParcelUuid mUuid;
        final int mPort;
        final int mAppUid;
        final long mStartTimeMs;

        RfcommSocketMetadata(
                BluetoothDevice device,
                boolean isSecured,
                ParcelUuid uuid,
                int port,
                int appUid,
                long startTimeMs) {
            mDevice = device;
            mIsSecured = isSecured;
            mUuid = uuid;
            mPort = port;
            mAppUid = appUid;
            mStartTimeMs = startTimeMs;
        }
    }
}
