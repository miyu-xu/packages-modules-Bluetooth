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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
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

import androidx.test.runner.MonitoringInstrumentation;

public class Main extends MonitoringInstrumentation {
    private static final String TAG = "HciForwarder";

    private static final byte HCI_PACKET_TYPE_COMMAND = 1;
    private static final byte HCI_PACKET_TYPE_ACL_DATA = 2;
    private static final byte HCI_PACKET_TYPE_SCO_DATA = 3;
    private static final byte HCI_PACKET_TYPE_EVENT = 4;
    private static final byte HCI_PACKET_TYPE_ISO_DATA = 5;

    private ServerSocket server = null;
    private IBluetoothHci service = null;

    class BluetoothHciCallbacks extends IBluetoothHciCallbacks.Stub {
        private final DataOutputStream out;

        BluetoothHciCallbacks(OutputStream out) {
            this.out = new DataOutputStream(new BufferedOutputStream(out));
        }

        public void initializationComplete(int status) {

        }

        public void hciEventReceived(ArrayList<Byte> event) {
            try {
                out.writeByte(HCI_PACKET_TYPE_EVENT);
                for (Byte b : event) {
                    out.writeByte(b);
                }
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        public void aclDataReceived(ArrayList<Byte> data) {
            try {
                out.writeByte(HCI_PACKET_TYPE_ACL_DATA);
                for (Byte b : data) {
                    out.writeByte(b);
                }
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        public void scoDataReceived(ArrayList<Byte> data) {
            try {
                out.writeByte(HCI_PACKET_TYPE_SCO_DATA);
                for (Byte b : data) {
                    out.writeByte(b);
                }
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            service = IBluetoothHci.getService("default");
            server = new ServerSocket(9100);
            while (true) {
                try {
                    Socket client = server.accept();
                    InputStream in = client.getInputStream();
                    OutputStream out = client.getOutputStream();

                    service.initialize(new BluetoothHciCallbacks(out));
                    Thread readThread = new Thread(() -> {
                        reader(in);
                    });
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void reader(InputStream in) {
        DataInputStream stream = new DataInputStream(new BufferedInputStream(in));
        byte[] buf = new byte[1024];
        try {
            Byte type = stream.readByte();
            int length = stream.read(buf);
            ArrayList<Byte> packet = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                packet.add(buf[i]);
            }
            switch (type) {
                case HCI_PACKET_TYPE_COMMAND:
                    service.sendHciCommand(packet);
                    break;
                case HCI_PACKET_TYPE_ACL_DATA:
                    service.sendAclData(packet);
                    break;
                case HCI_PACKET_TYPE_SCO_DATA:
                    service.sendScoData(packet);
                    break;
                default:
                    Log.e(TAG, "Unsupported packet type " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

}
