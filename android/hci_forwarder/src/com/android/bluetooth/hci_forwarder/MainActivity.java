/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.bluetooth.hci_forwarder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import android.app.Activity;
import android.os.Bundle;
import android.hardware.bluetooth.V1_1.IBluetoothHci;
import android.hardware.bluetooth.V1_0.IBluetoothHciCallbacks;
import android.hardware.bluetooth.V1_0.Status;
import android.util.Log;

/**
 * A minimal "Hello, World!" application.
 */
public class MainActivity extends Activity {
    private static final String TAG = "HciForwarderActivity";

    private static final Byte HCI_PACKET_TYPE_COMMAND = 1;
    private static final Byte HCI_PACKET_TYPE_ACL_DATA = 2;
    private static final Byte HCI_PACKET_TYPE_SCO_DATA = 3;
    private static final Byte HCI_PACKET_TYPE_EVENT = 4;
    private static final Byte HCI_PACKET_TYPE_ISO_DATA = 5;

    private Thread serverThread = null;
    private ServerSocket server = null;
    private final byte[] receiveBuffer = new byte[4096];

    class BluetoothHciCallbacks extends IBluetoothHciCallbacks.Stub {
        public final DataOutputStream stream;

        BluetoothHciCallbacks(OutputStream out) {
            this.stream = new DataOutputStream(out);
        }

        public void initializationComplete(int status) {

        }

        public void hciEventReceived(ArrayList<Byte> event) {
            try {
                stream.writeByte(HCI_PACKET_TYPE_EVENT);
                for (Byte b : event){
                    stream.writeByte(b);
                }
                stream.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        public void aclDataReceived(ArrayList<Byte> data) {
            try {
                stream.writeByte(HCI_PACKET_TYPE_ACL_DATA);
                for (Byte b : data){
                    stream.writeByte(b);
                }
                stream.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        public void scoDataReceived(ArrayList<Byte> data) {
            try {
                stream.writeByte(HCI_PACKET_TYPE_SCO_DATA);
                for (Byte b : data){
                    stream.writeByte(b);
                }
                stream.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            IBluetoothHci service = IBluetoothHci.getService(true);
            server = new ServerSocket(9100);
            serverThread = new Thread(() -> {
                while (true) {
                    try {
                        Socket client = server.accept();
                        InputStream in = client.getInputStream();
                        OutputStream out = client.getOutputStream();

                        service.initialize(new BluetoothHciCallbacks(out));
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
