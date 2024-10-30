/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

// Bluetooth Hap Client StateMachine. There is one instance per remote device.
//  - "Disconnected" and "Connected" are steady states.
//  - "Connecting" and "Disconnecting" are transient states until the
//     connection / disconnection is completed.

//                        (Disconnected)
//                           |       ^
//                   CONNECT |       | DISCONNECTED
//                           V       |
//                 (Connecting)<--->(Disconnecting)
//                           |       ^
//                 CONNECTED |       | DISCONNECT
//                           V       |
//                          (Connected)
// NOTES:
//  - If state machine is in "Connecting" state and the remote device sends
//    DISCONNECT request, the state machine transitions to "Disconnecting" state.
//  - Similarly, if the state machine is in "Disconnecting" state and the remote device
//    sends CONNECT request, the state machine transitions to "Connecting" state.

//                    DISCONNECT
//    (Connecting) ---------------> (Disconnecting)
//                 <---------------
//                      CONNECT
package com.android.bluetooth.hap;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;
import static android.bluetooth.BluetoothProfile.getConnectionStateName;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
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
import java.time.Duration;
import java.util.Scanner;

final class HapClientStateMachine extends StateMachine {
    private static final String TAG = HapClientStateMachine.class.getSimpleName();

    static final int MESSAGE_CONNECT = 1;
    static final int MESSAGE_DISCONNECT = 2;
    static final int MESSAGE_STACK_EVENT = 101;
    @VisibleForTesting static final int MESSAGE_CONNECT_TIMEOUT = 201;

    @VisibleForTesting static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final Disconnected mDisconnected = new Disconnected();
    private final Connecting mConnecting = new Connecting();
    private final Disconnecting mDisconnecting = new Disconnecting();
    private final Connected mConnected = new Connected();

    private int mConnectionState = STATE_DISCONNECTED;
    private int mLastConnectionState = -1;

    private final HapClientService mService;
    private final HapClientNativeInterface mNativeInterface;
    private final BluetoothDevice mDevice;

    HapClientStateMachine(
            HapClientService svc,
            BluetoothDevice device,
            HapClientNativeInterface gattInterface,
            Looper looper) {
        super(TAG, looper);
        mDevice = device;
        mService = svc;
        mNativeInterface = gattInterface;

        addState(mDisconnected);
        addState(mConnecting);
        addState(mDisconnecting);
        addState(mConnected);

        setInitialState(mDisconnected);
        start();
    }

    private static String messageWhatToString(int what) {
        return switch (what) {
            case MESSAGE_CONNECT -> "CONNECT";
            case MESSAGE_DISCONNECT -> "DISCONNECT";
            case MESSAGE_STACK_EVENT -> "STACK_EVENT";
            case MESSAGE_CONNECT_TIMEOUT -> "CONNECT_TIMEOUT";
            default -> Integer.toString(what);
        };
    }

    public void doQuit() {
        log("doQuit for device " + mDevice);
        quitNow();
    }

    public void cleanup() {
        log("cleanup for device " + mDevice);
    }

    int getConnectionState() {
        return mConnectionState;
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    synchronized boolean isConnected() {
        return (getConnectionState() == STATE_CONNECTED);
    }

    // This method does not check for error condition (newState == prevState)
    private void broadcastConnectionState(int newState, int prevState) {
        log(
                "Connection state "
                        + mDevice
                        + ": "
                        + getConnectionStateName(prevState)
                        + "->"
                        + getConnectionStateName(newState));

        mService.connectionStateChanged(mDevice, prevState, newState);
        Intent intent =
                new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED)
                        .putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState)
                        .putExtra(BluetoothProfile.EXTRA_STATE, newState)
                        .putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice)
                        .addFlags(
                                Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mService.sendBroadcastWithMultiplePermissions(
                intent, new String[] {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED});
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mDevice: " + mDevice);
        ProfileService.println(sb, "  StateMachine: " + this);
        // Dump the state machine logs
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        super.dump(new FileDescriptor(), printWriter, new String[] {});
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
        super.log(msg);
    }

    @VisibleForTesting
    class Disconnected extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Disconnected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mConnectionState = STATE_DISCONNECTED;

            removeDeferredMessages(MESSAGE_DISCONNECT);

            if (mLastConnectionState != -1) {
                // Don't broadcast during startup
                broadcastConnectionState(STATE_DISCONNECTED, mLastConnectionState);
            }
        }

        @Override
        public void exit() {
            log(
                    "Exit Disconnected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_DISCONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Disconnected: process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case MESSAGE_CONNECT -> {
                    log("Connecting to " + mDevice);
                    if (!mNativeInterface.connectHapClient(mDevice)) {
                        Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                        break;
                    }
                    if (mService.okToConnect(mDevice)) {
                        transitionTo(mConnecting);
                    } else {
                        // Reject the request and stay in Disconnected state
                        Log.w(
                                TAG,
                                "Outgoing HearingAccess Connecting request rejected: " + mDevice);
                    }
                }
                case MESSAGE_DISCONNECT -> {
                    Log.d(TAG, "Disconnected: DISCONNECT: call native disconnect for " + mDevice);
                    mNativeInterface.disconnectHapClient(mDevice);
                }
                case MESSAGE_STACK_EVENT -> {
                    HapClientStackEvent event = (HapClientStackEvent) message.obj;
                    Log.d(TAG, "Disconnected: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED -> {
                            processConnectionEvent(event.valueInt1);
                        }
                        default -> Log.e(TAG, "Disconnected: ignoring stack event: " + event);
                    }
                }
                default -> {
                    return NOT_HANDLED;
                }
            }
            return HANDLED;
        }

        // in Disconnected state
        private void processConnectionEvent(int state) {
            switch (state) {
                case STATE_CONNECTING -> {
                    if (mService.okToConnect(mDevice)) {
                        Log.i(
                                TAG,
                                "Incoming HearingAccess Connecting request accepted: " + mDevice);
                        transitionTo(mConnecting);
                    } else {
                        // Reject the connection and stay in Disconnected state itself
                        Log.w(
                                TAG,
                                "Incoming HearingAccess Connecting request rejected: " + mDevice);
                        mNativeInterface.disconnectHapClient(mDevice);
                    }
                }
                case STATE_CONNECTED -> {
                    Log.w(TAG, "HearingAccess Connected from Disconnected state: " + mDevice);
                    if (mService.okToConnect(mDevice)) {
                        Log.i(TAG, "Incoming HearingAccess Connected request accepted: " + mDevice);
                        transitionTo(mConnected);
                    } else {
                        // Reject the connection and stay in Disconnected state itself
                        Log.w(TAG, "Incoming HearingAccess Connected request rejected: " + mDevice);
                        mNativeInterface.disconnectHapClient(mDevice);
                    }
                }
                default -> Log.e(TAG, "Incorrect state: " + state + " device: " + mDevice);
            }
        }
    }

    @VisibleForTesting
    class Connecting extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Connecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            sendMessageDelayed(MESSAGE_CONNECT_TIMEOUT, CONNECT_TIMEOUT.toMillis());
            mConnectionState = STATE_CONNECTING;
            broadcastConnectionState(STATE_CONNECTING, mLastConnectionState);
        }

        @Override
        public void exit() {
            log(
                    "Exit Connecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_CONNECTING;
            removeMessages(MESSAGE_CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Connecting: process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case MESSAGE_CONNECT -> deferMessage(message);
                case MESSAGE_CONNECT_TIMEOUT -> {
                    Log.w(TAG, "Connecting connection timeout: " + mDevice);
                    mNativeInterface.disconnectHapClient(mDevice);
                    HapClientStackEvent disconnectEvent =
                            new HapClientStackEvent(
                                    HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
                    disconnectEvent.device = mDevice;
                    disconnectEvent.valueInt1 = STATE_DISCONNECTED;
                    sendMessage(MESSAGE_STACK_EVENT, disconnectEvent);
                }
                case MESSAGE_DISCONNECT -> {
                    log("Connecting: connection canceled to " + mDevice);
                    mNativeInterface.disconnectHapClient(mDevice);
                    transitionTo(mDisconnected);
                }
                case MESSAGE_STACK_EVENT -> {
                    HapClientStackEvent event = (HapClientStackEvent) message.obj;
                    log("Connecting: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED -> {
                            processConnectionEvent(event.valueInt1);
                        }
                        default -> Log.e(TAG, "Connecting: ignoring stack event: " + event);
                    }
                }
                default -> {
                    return NOT_HANDLED;
                }
            }
            return HANDLED;
        }

        // in Connecting state
        private void processConnectionEvent(int state) {
            switch (state) {
                case STATE_DISCONNECTED -> {
                    Log.w(TAG, "Connecting device disconnected: " + mDevice);
                    transitionTo(mDisconnected);
                }
                case STATE_CONNECTED -> transitionTo(mConnected);
                case STATE_DISCONNECTING -> {
                    Log.w(TAG, "Connecting interrupted: device is disconnecting: " + mDevice);
                    transitionTo(mDisconnecting);
                }
                default -> Log.e(TAG, "Incorrect state: " + state);
            }
        }
    }

    @VisibleForTesting
    class Disconnecting extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Disconnecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            sendMessageDelayed(MESSAGE_CONNECT_TIMEOUT, CONNECT_TIMEOUT.toMillis());
            mConnectionState = STATE_DISCONNECTING;
            broadcastConnectionState(STATE_DISCONNECTING, mLastConnectionState);
        }

        @Override
        public void exit() {
            log(
                    "Exit Disconnecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_DISCONNECTING;
            removeMessages(MESSAGE_CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Disconnecting: process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case MESSAGE_CONNECT, MESSAGE_DISCONNECT -> deferMessage(message);
                case MESSAGE_CONNECT_TIMEOUT -> {
                    Log.w(TAG, "Disconnecting connection timeout: " + mDevice);
                    mNativeInterface.disconnectHapClient(mDevice);

                    HapClientStackEvent disconnectEvent =
                            new HapClientStackEvent(
                                    HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
                    disconnectEvent.device = mDevice;
                    disconnectEvent.valueInt1 = STATE_DISCONNECTED;
                    sendMessage(MESSAGE_STACK_EVENT, disconnectEvent);
                }
                case MESSAGE_STACK_EVENT -> {
                    HapClientStackEvent event = (HapClientStackEvent) message.obj;
                    log("Disconnecting: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED -> {
                            processConnectionEvent(event.valueInt1);
                        }
                        default -> Log.e(TAG, "Disconnecting: ignoring stack event: " + event);
                    }
                }
                default -> {
                    return NOT_HANDLED;
                }
            }
            return HANDLED;
        }

        // in Disconnecting state
        private void processConnectionEvent(int state) {
            switch (state) {
                case STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected: " + mDevice);
                    transitionTo(mDisconnected);
                }
                case STATE_CONNECTED -> {
                    if (mService.okToConnect(mDevice)) {
                        Log.w(TAG, "Disconnecting interrupted: device is connected: " + mDevice);
                        transitionTo(mConnected);
                    } else {
                        // Reject the connection and stay in Disconnecting state
                        Log.w(TAG, "Incoming HearingAccess Connected request rejected: " + mDevice);
                        mNativeInterface.disconnectHapClient(mDevice);
                    }
                }
                case STATE_CONNECTING -> {
                    if (mService.okToConnect(mDevice)) {
                        Log.i(TAG, "Disconnecting interrupted: try to reconnect: " + mDevice);
                        transitionTo(mConnecting);
                    } else {
                        // Reject the connection and stay in Disconnecting state
                        Log.w(
                                TAG,
                                "Incoming HearingAccess Connecting request rejected: " + mDevice);
                        mNativeInterface.disconnectHapClient(mDevice);
                    }
                }
                default -> Log.e(TAG, "Incorrect state: " + state);
            }
        }
    }

    @VisibleForTesting
    class Connected extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Connected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mConnectionState = STATE_CONNECTED;
            removeDeferredMessages(MESSAGE_CONNECT);
            broadcastConnectionState(STATE_CONNECTED, mLastConnectionState);
        }

        @Override
        public void exit() {
            log(
                    "Exit Connected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_CONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Connected: process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case MESSAGE_DISCONNECT -> {
                    log("Disconnecting from " + mDevice);
                    if (!mNativeInterface.disconnectHapClient(mDevice)) {
                        // If error in the native stack, transition directly to Disconnected state.
                        Log.e(TAG, "Connected: error disconnecting from " + mDevice);
                        transitionTo(mDisconnected);
                        break;
                    }
                    transitionTo(mDisconnecting);
                }
                case MESSAGE_STACK_EVENT -> {
                    HapClientStackEvent event = (HapClientStackEvent) message.obj;
                    log("Connected: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED ->
                                processConnectionEvent(event.valueInt1);
                        default -> Log.e(TAG, "Connected: ignoring stack event: " + event);
                    }
                }
                default -> {
                    return NOT_HANDLED;
                }
            }
            return HANDLED;
        }

        // in Connected state
        private void processConnectionEvent(int state) {
            switch (state) {
                case STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from " + mDevice + " but still in Allowlist");
                    transitionTo(mDisconnected);
                }
                case STATE_DISCONNECTING -> {
                    Log.i(TAG, "Disconnecting from " + mDevice);
                    transitionTo(mDisconnecting);
                }
                default ->
                        Log.e(TAG, "Connection State Device: " + mDevice + " bad state: " + state);
            }
        }
    }
}
