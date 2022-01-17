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

package com.android.bluetooth.bas;

import static android.bluetooth.BluetoothDevice.PHY_LE_1M_MASK;
import static android.bluetooth.BluetoothDevice.PHY_LE_2M_MASK;
import static android.bluetooth.BluetoothDevice.TRANSPORT_AUTO;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Scanner;
import java.util.UUID;

/**
 * State machine for the battery service of a bluetooth device.
 * It maintains a {@link BluetoothGatt} object to read the battery level characteristic.
 */
public class BatteryStateMachine extends StateMachine {
    private static final boolean DBG = true;
    private static final String TAG = "BatteryStateMachine";

    static final UUID BATTERY_SERVICE_UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");

    static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    static final int CONNECT = 1;
    static final int DISCONNECT = 2;
    static final int CONNECTION_STATE_CHANGED = 3;
    private static final int CONNECT_TIMEOUT = 201;

    // NOTE: the value is not "final" - it is modified in the unit tests
    @VisibleForTesting
    static int sConnectTimeoutMs = 30000;        // 30s

    private Disconnected mDisconnected;
    private Connecting mConnecting;
    private Connected mConnected;
    private Disconnecting mDisconnecting;
    private int mLastConnectionState = -1;

    WeakReference<BatteryService> mServiceRef;

    BluetoothGatt mBluetoothGatt;
    final BluetoothDevice mDevice;

    BatteryStateMachine(BluetoothDevice device, BatteryService service, Looper looper) {
        super(TAG, looper);
        mDevice = device;
        mServiceRef = new WeakReference<>(service);

        mDisconnected = new Disconnected();
        mConnecting = new Connecting();
        mConnected = new Connected();
        mDisconnecting = new Disconnecting();

        addState(mDisconnected);
        addState(mConnecting);
        addState(mDisconnecting);
        addState(mConnected);

        setInitialState(mDisconnected);
    }

    static BatteryStateMachine make(BluetoothDevice device, BatteryService service, Looper looper) {
        Log.i(TAG, "make for device " + device);
        BatteryStateMachine sm = new BatteryStateMachine(device, service, looper);
        sm.start();
        return sm;
    }

    /**
     * Quits the state machine.
     */
    public void doQuit() {
        log("doQuit for device " + mDevice);
        quitNow();
    }

    /**
     * Cleans up the resources the state machine held.
     */
    public void cleanup() {
        log("cleanup for device " + mDevice);
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    @VisibleForTesting
    class Disconnected extends State {
        @Override
        public void enter() {
            Log.i(TAG, "Enter Disconnected(" + mDevice + "): " + messageWhatToString(
                    getCurrentMessage().what));

            removeDeferredMessages(DISCONNECT);

            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
            }
        }

        @Override
        public void exit() {
            log("Exit Disconnected(" + mDevice + "): " + messageWhatToString(
                    getCurrentMessage().what));
            mLastConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnected process message(" + mDevice + "): " + messageWhatToString(
                    message.what));

            BatteryService service = mServiceRef.get();
            switch (message.what) {
                case CONNECT:
                    log("Connecting to " + mDevice);
                    if (service != null && service.okToConnect(mDevice)) {
                        mBluetoothGatt = mDevice.connectGatt(service, /*autoConnect=*/true,
                                new GattCallback(), TRANSPORT_AUTO, /*opportunistic=*/true,
                                PHY_LE_1M_MASK | PHY_LE_2M_MASK, getHandler());
                        transitionTo(mConnecting);
                    } else {
                        // Reject the request and stay in Disconnected state
                        Log.w(TAG, "Outgoing Battery Connecting request rejected: "
                                + mDevice);
                    }
                    break;
                case DISCONNECT:
                    Log.w(TAG, "Disconnected: DISCONNECT ignored: " + mDevice);
                    break;
                case CONNECTION_STATE_CHANGED:
                    processConnectionEvent(message.arg1);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Disconnected state
        private void processConnectionEvent(int state) {
            BatteryService service = mServiceRef.get();
            switch (state) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.w(TAG, "Ignore Battery DISCONNECTED event: " + mDevice);
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    if (service != null && service.okToConnect(mDevice)) {
                        Log.i(TAG, "Incoming Battery Connected request accepted: "
                                + mDevice);
                        transitionTo(mConnected);
                    } else {
                        // Reject the connection and stay in Disconnected state itself
                        Log.w(TAG, "Incoming Battery Connected request rejected: "
                                + mDevice);
                        if (mBluetoothGatt != null) {
                            mBluetoothGatt.disconnect();
                        }
                    }
                    break;
                default:
                    Log.e(TAG, "Incorrect state: " + state + " device: " + mDevice);
                    break;
            }
        }
    }


    @VisibleForTesting
    class Connecting extends State {
        @Override
        public void enter() {
            Log.i(TAG, "Enter Connecting(" + mDevice + "): "
                    + messageWhatToString(getCurrentMessage().what));
            sendMessageDelayed(CONNECT_TIMEOUT, sConnectTimeoutMs);
        }

        @Override
        public void exit() {
            log("Exit Connecting(" + mDevice + "): "
                    + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = BluetoothProfile.STATE_CONNECTING;
            removeMessages(CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connecting process message(" + mDevice + "): "
                    + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    deferMessage(message);
                    break;
                case CONNECT_TIMEOUT:
                    Log.w(TAG, "Connecting connection timeout: " + mDevice);

                    sendMessage(DISCONNECT);
                    break;
                case DISCONNECT:
                    log("Connecting: connection canceled to " + mDevice);
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                    }
                    transitionTo(mDisconnected);
                    break;
                case CONNECTION_STATE_CHANGED:
                    processConnectionEvent(message.arg1);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Connecting state
        private void processConnectionEvent(int state) {
            switch (state) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.w(TAG, "Connecting device disconnected: " + mDevice);
                    transitionTo(mDisconnected);
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    transitionTo(mConnected);
                    break;
                default:
                    Log.e(TAG, "Incorrect state: " + state);
                    break;
            }
        }
    }

    @BluetoothProfile.BtProfileState
    int getConnectionState() {
        String currentState = getCurrentState().getName();
        switch (currentState) {
            case "Disconnected":
                return BluetoothProfile.STATE_DISCONNECTED;
            case "Connecting":
                return BluetoothProfile.STATE_CONNECTING;
            case "Connected":
                return BluetoothProfile.STATE_CONNECTED;
            case "Disconnecting":
                return BluetoothProfile.STATE_DISCONNECTING;
            default:
                Log.e(TAG, "Bad currentState: " + currentState);
                return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    @VisibleForTesting
    class Disconnecting extends State {
        @Override
        public void enter() {
            Log.i(TAG, "Enter Disconnecting(" + mDevice + "): "
                    + messageWhatToString(getCurrentMessage().what));
            sendMessageDelayed(CONNECT_TIMEOUT, sConnectTimeoutMs);
        }

        @Override
        public void exit() {
            log("Exit Disconnecting(" + mDevice + "): "
                    + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = BluetoothProfile.STATE_DISCONNECTING;
            removeMessages(CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnecting process message(" + mDevice + "): "
                    + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                case DISCONNECT:
                    deferMessage(message);
                    break;
                case CONNECT_TIMEOUT:
                    Log.w(TAG, "Disconnecting connection timeout: " + mDevice);
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                    }
                    transitionTo(mDisconnected);
                    break;
                case CONNECTION_STATE_CHANGED:
                    processConnectionEvent(message.arg1);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Disconnecting state
        private void processConnectionEvent(int state) {
            switch (state) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected: " + mDevice);
                    transitionTo(mDisconnected);
                    break;
                case BluetoothGatt.STATE_CONNECTED: {
                    BatteryService service = mServiceRef.get();
                    if (service.okToConnect(mDevice)) {
                        Log.w(TAG, "Disconnecting interrupted: device is connected: " + mDevice);
                        transitionTo(mConnected);
                    } else {
                        // Reject the connection and stay in Disconnecting state
                        Log.w(TAG, "Incoming VolumeControl Connected request rejected: "
                                + mDevice);
                        if (mBluetoothGatt != null) {
                            mBluetoothGatt.disconnect();
                        } else {
                            transitionTo(mDisconnected);
                        }
                    }
                    break;
                }
                default:
                    Log.e(TAG, "Incorrect state: " + state);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connected extends State {
        @Override
        public void enter() {
            Log.i(TAG, "Enter Connected(" + mDevice + "): "
                    + messageWhatToString(getCurrentMessage().what));
            removeDeferredMessages(CONNECT);
            if (mBluetoothGatt != null) {
                mBluetoothGatt.discoverServices();
            }
        }

        @Override
        public void exit() {
            log("Exit Connected(" + mDevice + "): "
                    + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = BluetoothProfile.STATE_CONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connected process message(" + mDevice + "): "
                    + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    Log.w(TAG, "Connected: CONNECT ignored: " + mDevice);
                    break;
                case DISCONNECT:
                    log("Disconnecting from " + mDevice);
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                    }
                    transitionTo(mDisconnecting);
                    break;
                case CONNECTION_STATE_CHANGED:
                    processConnectionEvent(message.arg1);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Connected state
        private void processConnectionEvent(int state) {
            switch (state) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from " + mDevice);
                    transitionTo(mDisconnected);
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    Log.w(TAG, "Ignore Battery CONNECTED event: " + mDevice);
                    transitionTo(mDisconnecting);
                    break;
                default:
                    Log.e(TAG, "Connection State Device: " + mDevice + " bad state: " + state);
                    break;
            }
        }
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    synchronized boolean isConnected() {
        return getCurrentState() == mConnected;
    }

    private static String messageWhatToString(int what) {
        switch (what) {
            case CONNECT:
                return "CONNECT";
            case DISCONNECT:
                return "DISCONNECT";
            case CONNECT_TIMEOUT:
                return "CONNECT_TIMEOUT";
            default:
                break;
        }
        return Integer.toString(what);
    }

    private static String profileStateToString(int state) {
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return "DISCONNECTED";
            case BluetoothProfile.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothProfile.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "DISCONNECTING";
            default:
                break;
        }
        return Integer.toString(state);
    }

    void dump(StringBuilder sb) {
        ProfileService.println(sb, "mDevice: " + mDevice);
        ProfileService.println(sb, "  StateMachine: " + this);
        // Dump the state machine logs
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        super.dump(new FileDescriptor(), printWriter, new String[]{});
        printWriter.flush();
        stringWriter.flush();
        ProfileService.println(sb, "  StateMachineLog:");
        Scanner scanner = new Scanner(stringWriter.toString());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            ProfileService.println(sb, "    " + line);
        }
        scanner.close();
    }

    @Override
    protected void log(String msg) {
        if (DBG) {
            super.log(msg);
        }
    }

    final class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            sendMessage(CONNECTION_STATE_CHANGED, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "No gatt service");
                return;
            }

            final BluetoothGattService batteryService = gatt.getService(BATTERY_SERVICE_UUID);
            if (batteryService == null) {
                Log.e(TAG, "No battery service");
                return;
            }

            final BluetoothGattCharacteristic batteryLevel =
                    batteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID);
            if (batteryLevel == null) {
                Log.e(TAG, "No battery level characteristic");
                return;
            }

            gatt.setCharacteristicNotification(batteryLevel, /*enable=*/true);
            gatt.readCharacteristic(batteryLevel);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, byte[] value) {
            if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                updateBatteryLevel(value);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, byte[] value, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Read characteristic failure on " + gatt + " " + characteristic);
                return;
            }

            if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                updateBatteryLevel(value);
            }
        }

        private void updateBatteryLevel(byte[] value) {
            int batteryLevel = value[0] & 0xFF;

            BatteryService service = mServiceRef.get();
            service.handleBatteryChanged(mDevice, batteryLevel);
        }
    }
}
