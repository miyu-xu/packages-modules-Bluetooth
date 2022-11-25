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
import android.os.ServiceManager;
import android.hardware.bluetooth.V1_0.IBluetoothHciCallbacks;
import android.util.Log;

import androidx.test.runner.MonitoringInstrumentation;

public class Main extends MonitoringInstrumentation {
    private static final String TAG = "HciForwarder";

    private static final byte HCI_PACKET_TYPE_COMMAND = 1;
    private static final byte HCI_PACKET_TYPE_ACL_DATA = 2;
    private static final byte HCI_PACKET_TYPE_SCO_DATA = 3;
    private static final byte HCI_PACKET_TYPE_EVENT = 4;
    private static final byte HCI_PACKET_TYPE_ISO_DATA = 5;

    private android.hardware.bluetooth.V1_1.IBluetoothHci service_hidl = null;
    private android.hardware.bluetooth.IBluetoothHci service_aidl = null;

    class BluetoothHciCallbacksHidl extends IBluetoothHciCallbacks.Stub {
        private final DataOutputStream out;

        BluetoothHciCallbacksHidl(OutputStream out) {
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

    class BluetoothHciCallbacksAidl extends android.hardware.bluetooth.IBluetoothHciCallbacks.Stub {
        private final DataOutputStream out;

        BluetoothHciCallbacksAidl(OutputStream out) {
            this.out = new DataOutputStream(new BufferedOutputStream(out));
        }

        public void initializationComplete(int status) {

        }

        @Override
        public String getInterfaceHash() {
            return android.hardware.bluetooth.IBluetoothHciCallbacks.HASH;
        }

        @Override
        public int getInterfaceVersion() {
            return android.hardware.bluetooth.IBluetoothHciCallbacks.VERSION;
        }

        @Override
        public void hciEventReceived(byte[] data) {
            try {
                out.writeByte(HCI_PACKET_TYPE_EVENT);
                out.write(data);
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void aclDataReceived(byte[] data) {
            try {
                out.writeByte(HCI_PACKET_TYPE_ACL_DATA);
                out.write(data);
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void scoDataReceived(byte[] data) {
            try {
                out.writeByte(HCI_PACKET_TYPE_SCO_DATA);
                out.write(data);
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void isoDataReceived(byte[] data) {
            try {
                out.writeByte(HCI_PACKET_TYPE_ISO_DATA);
                out.write(data);
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
            service_hidl = android.hardware.bluetooth.V1_1.IBluetoothHci.getService(true);
            Log.d(TAG, "HIDL binder connected");
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        try {
            service_aidl = android.hardware.bluetooth.IBluetoothHci.Stub.asInterface(
                    ServiceManager.waitForDeclaredService("android.hardware.bluetooth.IBluetoothHci/default"));
            Log.d(TAG, "AIDL binder connected");
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }


        Thread serverThread = new Thread(() -> {
            server();
        });
        serverThread.start();
    }

    private void server() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(9100);
            Log.d(TAG, "Server created");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return;
        }

        Thread readThread = null;
        while (true) {
            try {
                Socket client = serverSocket.accept();
                Log.d(TAG, "New client");
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();

                if (service_hidl != null) {
                    service_hidl.close();
                    service_hidl.initialize(new BluetoothHciCallbacksHidl(out));
                }
                else {
                    service_aidl.close();
                    service_aidl.initialize(new BluetoothHciCallbacksAidl(out));
                }
                readThread = new Thread(() -> {
                    reader(in);
                });
                readThread.start();
                readThread.join();
                readThread = null;
                in.close();
                out.close();
                client.close();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            } finally {
                Log.d(TAG, "Socket closed");
            }
        }
    }

    private void reader(InputStream in) {
        DataInputStream stream = new DataInputStream(new BufferedInputStream(in));
        while (true){
            try {
                Byte type = stream.readByte();
                byte[] buf = new byte[stream.available()];
                stream.read(buf);
                if (service_hidl != null) {
                    ArrayList<Byte> packet = new ArrayList<>(buf.length);
                    for (int i = 0; i < buf.length; i++) {
                        packet.add(buf[i]);
                    }
                    switch (type) {
                        case HCI_PACKET_TYPE_COMMAND:
                            service_hidl.sendHciCommand(packet);
                            break;
                        case HCI_PACKET_TYPE_ACL_DATA:
                            service_hidl.sendAclData(packet);
                            break;
                        case HCI_PACKET_TYPE_SCO_DATA:
                            service_hidl.sendScoData(packet);
                            break;
                        default:
                            Log.e(TAG, "Unsupported packet type " + type);
                            break;
                    }
                } else {
                    switch (type) {
                        case HCI_PACKET_TYPE_COMMAND:
                            service_aidl.sendHciCommand(buf);
                            break;
                        case HCI_PACKET_TYPE_ACL_DATA:
                            service_aidl.sendAclData(buf);
                            break;
                        case HCI_PACKET_TYPE_SCO_DATA:
                            service_aidl.sendScoData(buf);
                            break;
                        default:
                            Log.e(TAG, "Unsupported packet type " + type);
                            break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                break;
            }
        }
    }

}
