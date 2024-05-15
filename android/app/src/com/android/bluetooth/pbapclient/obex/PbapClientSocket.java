/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.android.internal.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A testable object that wraps BluetoothSocket objects
 *
 * To inject input and output streams in place of an underlying L2CAP or RFCOMM connection, use the
 * the inject(InputStream input, OutputStream output) function. All sockets created after injecting
 * streams will use the injected streams instead.
 */
public class PbapClientSocket {
    private static final String TAG = PbapClientSocket.class.getSimpleName();

    // Houses the streams to be injected in place of the underlying BluetoothSocket
    private static InputStream sInjectedInput;
    private static OutputStream sInjectedOutput;

    // Houses the actual socket if used
    private final BluetoothSocket mSocket;

    // Houses injected streams, if used for this object
    private final BluetoothDevice mDevice;
    private final int mType;
    private final InputStream mInjectedInput;
    private final OutputStream mInjectedOutput;

    @VisibleForTesting
    static void inject(InputStream input, OutputStream output) {
        sInjectedInput = Objects.requireNonNull(input);
        sInjectedOutput = Objects.requireNonNull(output);
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    public static PbapClientSocket getL2capSocketForDevice(BluetoothDevice device, int psm) throws IOException {
        if (sInjectedInput != null || sInjectedOutput != null) {
            return new PbapClientSocket(device, BluetoothSocket.TYPE_L2CAP, sInjectedInput, sInjectedOutput);
        }

        BluetoothSocket socket = device.createL2capSocket(psm);
        return new PbapClientSocket(socket);
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    public static PbapClientSocket getRfcommSocketForDevice(BluetoothDevice device, int channel) throws IOException {
        if (sInjectedInput != null || sInjectedOutput != null) {
            return new PbapClientSocket(device, BluetoothSocket.TYPE_RFCOMM, sInjectedInput, sInjectedOutput);
        }
        BluetoothSocket socket = device.createRfcommSocket(channel);
        return new PbapClientSocket(socket);
    }

    private PbapClientSocket(BluetoothSocket socket) {
        mSocket = socket;

        mDevice = null;
        mType = -1;
        mInjectedInput = null;
        mInjectedOutput = null;
    }

    private PbapClientSocket(BluetoothDevice device, int type, InputStream input, OutputStream output) {
        mSocket = null;

        mDevice = device;
        mType = type;
        mInjectedInput = input;
        mInjectedOutput = output;
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    public void connect() throws IOException {
        if (mSocket != null) {
            mSocket.connect();
        }
    }

    public BluetoothDevice getRemoteDevice() {
        if (mSocket != null) {
            return mSocket.getRemoteDevice();
        }
        return mDevice;
    }

    public int getConnectionType() {
        if (mSocket != null) {
            return mSocket.getConnectionType();
        }
        return mType;
    }

    public int getMaxTransmitPacketSize() {
        if (mSocket != null) {
            return mSocket.getMaxTransmitPacketSize();
        }
        return 255; // Minimum by specification
    }

    public int getMaxReceivePacketSize() {
        if (mSocket != null) {
            return mSocket.getMaxReceivePacketSize();
        }
        return 255; // Minimum by specification
    }

    public InputStream getInputStream() throws IOException {
        if (mInjectedInput != null) {
            return mInjectedInput;
        }
        return mSocket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        if (mInjectedOutput != null) {
            return mInjectedOutput;
        }
        return mSocket.getOutputStream();
    }

    public void close() throws IOException {
        if (mSocket != null) {
            mSocket.close();
            return;
        }

        if (sInjectedInput != null) {
            sInjectedInput.close();
        }

        if (mInjectedOutput != null) {
            mInjectedOutput.close();
        }
    }

    @Override
    public String toString() {
        if (mSocket != null) {
            return mSocket.toString();
        }
        return "FakeBluetoothSocket";
    }
}
