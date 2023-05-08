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
package com.android.server.bluetooth;

// import static com.android.server.bluetooth.ServiceUtils.BluetoothServiceConnection;
import static com.android.server.bluetooth.ServiceUtils.doBind;
import static com.android.server.bluetooth.ServiceUtils.getTempAllowlistBroadcastOptions;
import static com.android.server.bluetooth.ServiceUtils.isDeviceProvisioned;
import static com.android.server.bluetooth.ServiceUtils.persistBluetoothSetting;

import static java.util.Objects.requireNonNull;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothGatt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;



/**
 * BluetoothStateMachine
 */
@VisibleForTesting
class BluetoothStateMachine extends StateMachine {
    private static final String TAG = BluetoothStateMachine.class.getSimpleName();
    private static final boolean DBG = true;//Log.isLoggable(TAG, Log.DEBUG);

    // Maximum msec to wait for service restart
    private static final int SERVICE_RESTART_TIME_MS = 400;

    // Maximum msec to wait for a bind
    private static final int TIMEOUT_BIND_MS = 3000;
    // Maximum msec to delay MESSAGE_USER_SWITCHED
    private static final int USER_SWITCHED_TIME_MS = 200;

    // Delay for retrying enable and disable in msec
    private static final int ENABLE_DISABLE_DELAY_MS = 300; // REMOVED ?

    // Messages handled by the state machine
    private static final int MESSAGE_ENABLE = 1;
    private static final int MESSAGE_DISABLE = 2;
    private static final int MESSAGE_HANDLE_ENABLE_DELAYED = 3;
    private static final int MESSAGE_HANDLE_DISABLE_DELAYED = 4;

    private static final int MESSAGE_BLUETOOTH_SERVICE_CONNECTED = 40;
    private static final int MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED = 41;
    private static final int MESSAGE_RESTART_BLUETOOTH_SERVICE = 42;

    private static final int MESSAGE_BLUETOOTH_STATE_CHANGE = 60;

    private static final int MESSAGE_TIMEOUT_BIND = 100;

    private static final int MESSAGE_GET_NAME_AND_ADDRESS = 200;

    private static final int MESSAGE_USER_SWITCHED = 300;

    private static final int MESSAGE_BIND_PROFILE_SERVICE = 401;
    // end of Messages

    private static final int BIND_FLAGS = Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT;

    private static final int SERVICE_IBLUETOOTH = 1;
    private static final int SERVICE_IBLUETOOTHGATT = 2;
    //
    // Bluetooth persisted setting is off
    private static final int BLUETOOTH_OFF = 0;
    // Bluetooth persisted setting is on
    // and Airplane mode won't affect Bluetooth state at start up
    private static final int BLUETOOTH_ON_BLUETOOTH = 1;
    // Bluetooth persisted setting is on
    // but Airplane mode will affect Bluetooth state at start up
    // and Airplane mode will have higher priority.
    @VisibleForTesting
    static final int BLUETOOTH_ON_AIRPLANE = 2;

    private final Context mContext;
    private final BluetoothManagerService mBluetoothManagerService;

    private final BluetoothUnbinded mBluetoothUnbinded = new BluetoothUnbinded();
    private final BluetoothBinding mBluetoothBinding = new BluetoothBinding();
    private final BluetoothBinded mBluetoothBinded = new BluetoothBinded();
    private final BluetoothOff mBluetoothOff = new BluetoothOff();
    private final BluetoothBleOn mBluetoothBleOn = new BluetoothBleOn();
    private final BluetoothOn mBluetoothOn = new BluetoothOn();
    //TODO addState --> parents ?
    // private final BluetoothTemporaryState mBluetoothTurningOn = new BluetoothTemporaryState(BluetoothAdapter.STATE_TURNING_ON, /* isBle */ false);
    // private final BluetoothTemporaryState mBluetoothTurningOff = new BluetoothTemporaryState(BluetoothAdapter.STATE_TURNING_OFF, /* isBle */ false);
    // private final BluetoothTemporaryState mBluetoothBleTurningOn = new BluetoothTemporaryState(BluetoothAdapter.STATE_BLE_TURNING_ON, /* isBle */ true);
    // private final BluetoothTemporaryState mBluetoothBleTurningOff = new BluetoothTemporaryState(BluetoothAdapter.STATE_TURNING_BLE_OFF, /* isBle */ true);
    private final BluetoothTurningOn mBluetoothTurningOn = new BluetoothTurningOn();
    private final BluetoothTurningOff mBluetoothTurningOff = new BluetoothTurningOff();
    private final BluetoothBleTurningOn mBluetoothBleTurningOn = new BluetoothBleTurningOn();
    private final BluetoothBleTurningOff mBluetoothBleTurningOff = new BluetoothBleTurningOff();

    private final Map<Integer, BluetoothAdapterState> AdapterStateMap = Map.of(
            BluetoothAdapter.STATE_OFF, mBluetoothOff,
            BluetoothAdapter.STATE_ON, mBluetoothOn,
            BluetoothAdapter.STATE_BLE_ON, mBluetoothBleOn,
            BluetoothAdapter.STATE_TURNING_ON, mBluetoothTurningOn,
            BluetoothAdapter.STATE_TURNING_OFF, mBluetoothTurningOff,
            BluetoothAdapter.STATE_BLE_TURNING_ON, mBluetoothBleTurningOn,
            BluetoothAdapter.STATE_BLE_TURNING_OFF, mBluetoothBleTurningOff
        );

    private final BluetoothServiceConnection mConnection = new BluetoothServiceConnection(SERVICE_IBLUETOOTH);
    private final BluetoothServiceConnection mConnectionGatt = new BluetoothServiceConnection(SERVICE_IBLUETOOTHGATT);

    private IBinder mBluetoothBinder;
    private IBluetooth mBluetooth;
    private IBluetoothGatt mBluetoothGatt;

    private boolean mShutdownInProgress;
    private boolean mGetNameAddressOnly;
    private boolean mEnable;
    private boolean mQuietEnable;
    private boolean mIsBle;

    private int mErrorRecoveryRetryCounter;
    private int mPreviousAdapterState = BluetoothAdapter.STATE_OFF;

    BluetoothStateMachine(BluetoothManagerService bms, Context ctx, Looper looper) {
        super(TAG, requireNonNull(looper));
        setDbg(DBG);
        mBluetoothManagerService = requireNonNull(bms);
        mContext = requireNonNull(ctx);

        addState(mBluetoothUnbinded);
        addState(mBluetoothBinding);
        // addState(mBluetoothOff, mBluetoothBinded);
        // addState(mBluetoothBleOn, mBluetoothBinded);
        // addState(mBluetoothOn, mBluetoothBinded);

        for(BluetoothAdapterState state: AdapterStateMap.values()) {
            addState(state, mBluetoothBinded);
        }

        setInitialState(mBluetoothUnbinded);
        start();
        TotoStateMachine toto = new TotoStateMachine();
    }

    class TotoStateMachine extends StateMachine {
        TotoState state1 = new TotoState(1);
        TotoState state2 = new TotoState(2);
        TotoState state3 = new TotoState(3);
        TotoState state4 = new TotoState(4);
        TotoStateMachine() {
            super("TOTO");
            setDbg(true);
            addState(state1);
            // addState(state2);
            addState(state3, state2);
            addState(state4, state2);
            setInitialState(state1);
            start();
            sendMessage(1, 3);
            sendMessage(1, 2);
            sendMessage(1, 4);
            sendMessage(1, 3);
            sendMessage(1, 0);
            sendMessage(1, 1);
            sendMessage(1, 0);
        }
        @VisibleForTesting
        class TotoState extends State {
            int me;
            TotoState(int tt) { me = tt; }

            @Override
            public String toString() { return "Toto(" + me +")"; }

            @Override
            public boolean equals(Object o) {
                if (o instanceof TotoState) {
                    return me == ((TotoState) o).me;
                }
                return false;
            }

            @Override
            public void enter() {
                Log.e(TAG, "WILLIAM -- Enter in " + this + " Curr=" + getCurrentState());
                if (me == 2 && Objects.equals(getCurrentState(), state2)) {
                    Log.e(TAG, "WILLIAM -- THROW! ");
                }
            }

            @Override
            public void exit() { Log.e(TAG, "WILLIAM -- exit in " + me); }

            @Override
            public boolean processMessage(Message message) {
                Log.e(TAG, "WILLIAM -- process " + message.arg1 + " in " + me);
                switch (message.arg1) {
                    case 1:
                        transitionTo(state1);
                        break;
                    case 2:
                        transitionTo(state2);
                        break;
                    case 3:
                        transitionTo(state3);
                        break;
                    case 4:
                        transitionTo(state4);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }
    }

    IBluetooth getBluetooth() {
        return mBluetooth;
    }

    void sendEnableMsg(boolean quietMode, boolean isBle) {
        sendMessage(MESSAGE_ENABLE, quietMode ? 1 : 0, isBle ? 1 : 0);
    }
    void sendDisableMsg() {
        sendMessage(MESSAGE_DISABLE);
    }
    void sendGetNameAndAddress() {
        sendMessage(MESSAGE_GET_NAME_AND_ADDRESS);
    }
    void sendUserSwitch(UserHandle userHandle) {
        sendMessage(MESSAGE_USER_SWITCHED, userHandle.getIdentifier(), 0);
    }
    void sendUserUnlocked(UserHandle userHandle) {
        // sendMessage(MESSAGE_USER_UNLOCKED, userHandle.getIdentifier(), 0);
        // This looks like a no-op with the current state machine
    }

    @VisibleForTesting
    class BluetoothUnbinded extends State {
        @Override
        public void enter() {
            mBluetooth = null;
        }

        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                // case MESSAGE_ENABLE:
                //     if (!commonEnable(message)) { break; }
                //     transitionTo(mBluetoothBinding);
                // // fall through to ENABLE
                // case MESSAGE_GET_NAME_AND_ADDRESS:
                //     mGetNameAddressOnly = message.what == MESSAGE_GET_NAME_AND_ADDRESS;
                //     log("Binding Bluetooth service");
                //     if (doBind(mContext, new Intent(IBluetooth.class.getName()), mConnection,
                //                 BIND_FLAGS, UserHandle.CURRENT)) {
                //     }
                //     break;




                case MESSAGE_GET_NAME_AND_ADDRESS:
                    mGetNameAddressOnly = true;
                    log("Binding Bluetooth service");
                    if (doBind(mContext, new Intent(IBluetooth.class.getName()), mConnection,
                                BIND_FLAGS, UserHandle.CURRENT)) {
                        transitionTo(mBluetoothBinding);
                    }
                    break;
                // fall through to ENABLE
                case MESSAGE_ENABLE:
                    if (!commonEnable(message)) { break; }
                    log("Binding Bluetooth service");
                    if (doBind(mContext, new Intent(IBluetooth.class.getName()), mConnection,
                                BIND_FLAGS, UserHandle.CURRENT)) {
                        transitionTo(mBluetoothBinding);
                    }
                    break;




                case MESSAGE_DISABLE:
                    commonDisable();
                    break;
                // case MESSAGE_USER_SWITCHED: {
                //     removeMessages(MESSAGE_USER_SWITCHED);
                //     // mBluetoothNotificationManager.createNotificationChannels();
                //     break;
                // }
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    @VisibleForTesting
    class BluetoothBinding extends State {
        @Override
        public void enter() {
            // log("Binding Bluetooth service");
            // if (doBind(mContext, new Intent(IBluetooth.class.getName()), mConnection,
            //             BIND_FLAGS, UserHandle.CURRENT)) {
            //     transitionTo(mBluetoothUnbinded);
            //     sendMessage(DISABLE)
            //     return;
            // }
            sendMessageDelayed(obtainMessage(MESSAGE_TIMEOUT_BIND), TIMEOUT_BIND_MS);
        }

        @Override
        public void exit() {
            removeMessages(MESSAGE_TIMEOUT_BIND);
        }

        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                case MESSAGE_TIMEOUT_BIND:
                    mContext.unbindService(mConnection);
                    transitionTo(mBluetoothUnbinded);
                    break;
                case MESSAGE_ENABLE:
                    if (!commonEnable(message)) { break; }
                    break;
                case MESSAGE_USER_SWITCHED:
                    removeMessages(MESSAGE_USER_SWITCHED);
                    // mBluetoothNotificationManager.createNotificationChannels();

                    message.arg2++;
                    // if user is switched when service is binding retry after a delay 
                    // TODO shouldn't we go back to unbinded ?
                    sendMessageDelayed(message, USER_SWITCHED_TIME_MS);
                    log("Retry MESSAGE_USER_SWITCHED " + message.arg2);
                    break;
                case MESSAGE_DISABLE:
                    // if (mHandler.hasMessages(MESSAGE_HANDLE_DISABLE_DELAYED) || mBinding
                    //         || mHandler.hasMessages(MESSAGE_HANDLE_ENABLE_DELAYED)) {
                    //     // We are handling enable or disable right now, wait for it.
                    //     mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_DISABLE),
                    //             ENABLE_DISABLE_DELAY_MS);
                    //     break;
                    // }

                    commonDisable();

                    // mWaitForDisableRetry = 0;
                    sendMessageDelayed(MESSAGE_HANDLE_DISABLE_DELAYED, ENABLE_DISABLE_DELAY_MS);
                    break;
                case MESSAGE_BLUETOOTH_SERVICE_CONNECTED: {
                    log("MESSAGE_BLUETOOTH_SERVICE_CONNECTED: " + message.arg1);

                    IBinder service = (IBinder) message.obj;
                    // TODO SERVICE_IBLUETOOTHGATT
                    // if (msg.arg1 == SERVICE_IBLUETOOTHGATT) {
                    //     mBluetoothGatt = IBluetoothGatt.Stub.asInterface(service);
                    //     continueFromBleOnState();
                    //     break;
                    // } // else must be SERVICE_IBLUETOOTH

                    mBluetoothBinder = service;
                    mBluetooth = IBluetooth.Stub.asInterface(service);
                    int foregroundUserId = ActivityManager.getCurrentUser();
                    try {
                        mBluetooth.setForegroundUserId(ActivityManager.getCurrentUser(),
                                mContext.getAttributionSource());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Unable to set foreground user id", e);
                    }
                    // propagateForegroundUserId(foregroundUserId);
                    transitionTo(mBluetoothOff);


                    // TODO !!!
                    //if (!isNameAndAddressSet()) {
                    //    Message getMsg = mHandler.obtainMessage(MESSAGE_GET_NAME_AND_ADDRESS);
                    //    mHandler.sendMessage(getMsg);
                    //    if (mGetNameAddressOnly) {
                    //        break;
                    //    }
                    //}

                    ////Register callback object
                    //try {
                    //    synchronousRegisterCallback(mBluetoothCallback,
                    //            mContext.getAttributionSource());
                    //} catch (RemoteException | TimeoutException e) {
                    //    Log.e(TAG, "Unable to register BluetoothCallback", e);
                    //}
                    ////Inform BluetoothAdapter instances that service is up
                    //sendBluetoothServiceUpCallback();

                    ////Do enable request
                    //try {
                    //    if (!synchronousEnable(mQuietEnable, mContext.getAttributionSource())) {
                    //        Log.e(TAG, "IBluetooth.enable() returned false");
                    //    }
                    //} catch (RemoteException | TimeoutException e) {
                    //    Log.e(TAG, "Unable to call enable()", e);
                    //}

                    //if (!mEnable) {
                    //    waitForState(Set.of(BluetoothAdapter.STATE_ON));
                    //    handleDisable();
                    //    waitForState(Set.of(BluetoothAdapter.STATE_OFF,
                    //            BluetoothAdapter.STATE_TURNING_ON,
                    //            BluetoothAdapter.STATE_TURNING_OFF,
                    //            BluetoothAdapter.STATE_BLE_TURNING_ON,
                    //            BluetoothAdapter.STATE_BLE_ON,
                    //            BluetoothAdapter.STATE_BLE_TURNING_OFF));
                    //}
                    //break;
                    break;
                }
                default:
                    return NOT_HANDLED;
                // case MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED: {
                //     break;
                // }
            }
            return HANDLED;
        }
    }
    @VisibleForTesting
    class BluetoothBinded extends State {
        @Override
        public void enter() {
            if (Objects.equals(getCurrentState(), mBluetoothBinded)) {
                throw new IllegalStateException("State not valid: " + this + " is a parent state");
            }
            requireNonNull(mBluetooth); // We can only transition to Binded if mBluetooth is valid
            mPreviousAdapterState = BluetoothAdapter.STATE_OFF;
            mBluetoothManagerService.registerCallback(mBluetooth, mBluetoothCallback);

            if (mGetNameAddressOnly || !mBluetoothManagerService.isNameAndAddressSet()) {
                sendMessage(MESSAGE_GET_NAME_AND_ADDRESS);
            }
        }

        @Override
        public void exit() {
            removeMessages(MESSAGE_BLUETOOTH_STATE_CHANGE);
            removeMessages(MESSAGE_BIND_PROFILE_SERVICE);
            mBluetoothManagerService.unregisterCallback(mBluetooth, mBluetoothCallback);
            mBluetoothBinder = null;
            mBluetooth = null;
            mContext.unbindService(mConnection);
            mBluetoothGatt = null;
            mContext.unbindService(mConnectionGatt);
        }

        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                case MESSAGE_GET_NAME_AND_ADDRESS:
                    mBluetoothManagerService.getAndUpdateNameAndAddress(mBluetooth);
                    if (mGetNameAddressOnly) {
                        mGetNameAddressOnly = false;
                        if (!hasMessages(MESSAGE_ENABLE)) {
                            // TODO make your mind
                            sendMessage(MESSAGE_DISABLE);
                            transitionTo(mBluetoothUnbinded);
                        }
                        // transitionTo(mBluetoothUnbinded);
                    }
                    break;
                case MESSAGE_BLUETOOTH_STATE_CHANGE:
                    int prevState = message.arg1;
                    int nextState = message.arg2;
                    log("MESSAGE_BLUETOOTH_STATE_CHANGE:"
                            + " From=" + BluetoothAdapter.nameForState(prevState)
                            + " To=" + BluetoothAdapter.nameForState(nextState));
                    BluetoothAdapterState prevIState = requireNonNull(AdapterStateMap.get(prevState));
                    if (!getCurrentState().equals(prevIState)) {
                        throw new IllegalStateException("Current state is: " + getCurrentState() + " vs expected " + prevIState);
                    }
                    BluetoothAdapterState nextIState = requireNonNull(AdapterStateMap.get(nextState));
                    if (getCurrentState().equals(nextIState)) {
                        break; // Nothing to do
                    }

                    mPreviousAdapterState = prevState;
                    transitionTo(nextIState);
                    break;
                case MESSAGE_ENABLE:
                    if (!commonEnable(message)) {
                        break;
                    }
                    // from case
                    //
                    // We need to wait until transitioned to STATE_OFF and
                    // the previous Bluetooth process has exited. The
                    // waiting period has three components:
                    // (a) Wait until the local state is STATE_OFF. This
                    //     is accomplished by sending delay a message
                    //     MESSAGE_HANDLE_ENABLE_DELAYED
                    // (b) Wait until the STATE_OFF state is updated to
                    //     all components.
                    // (c) Wait until the Bluetooth process exits, and
                    //     ActivityManager detects it.
                    // The waiting for (b) and (c) is accomplished by
                    // delaying the MESSAGE_RESTART_BLUETOOTH_SERVICE
                    // message. The delay time is backed off if Bluetooth
                    // continuously failed to turn on itself.
                    //
                    // mWaitForEnableRetry = 0; // TODO
                    sendMessageDelayed(MESSAGE_HANDLE_ENABLE_DELAYED, ENABLE_DISABLE_DELAY_MS);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class BluetoothAdapterState extends State {
        final int mAdapterState;
        BluetoothAdapterState(int state) {
            mAdapterState = state;
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof BluetoothAdapterState) {
                return mAdapterState == ((BluetoothAdapterState) o).mAdapterState;
            }
            return false;
        }
    }

    @VisibleForTesting
    class BluetoothOff extends BluetoothAdapterState {
        BluetoothOff() { super(BluetoothAdapter.STATE_OFF); }
        @Override
        public void enter() {
            if (mPreviousAdapterState == mAdapterState) { // Same transition == it was binding
                return;
            }
            // If Bluetooth is off, send service down event to proxy objects, and unbind
            log("Bluetooth is complete send Service Down");
            mBluetoothManagerService.sendBluetoothServiceDownCallback(); // TODO move elsewhere ?

            transitionTo(mBluetoothUnbinded);

            sendBleStateChanged(mPreviousAdapterState, mAdapterState);

            /* Currently, the OFF intent is broadcasted externally only when we transition
             * from TURNING_OFF to BLE_ON state. So if the previous state is a BLE state,
             * we are guaranteed that the OFF intent has been broadcasted earlier and we
             * can safely skip it.
             * Conversely, if the previous state is not a BLE state, it indicates that some
             * sort of crash has occurred, moving us directly to STATE_OFF without ever
             * passing through BLE_ON. We should broadcast the OFF intent in this case. */
            if (isBleState(mPreviousAdapterState)) {
                standardBroadcast(mPreviousAdapterState, mAdapterState);
            }

            // handle error state transition case from TURNING_ON to OFF
            // unbind and rebind bluetooth service and enable bluetooth
            if (mPreviousAdapterState == BluetoothAdapter.STATE_BLE_TURNING_ON && mEnable) {
                recoverBluetoothServiceFromError(false);
            }
            // If we tried to enable BT while BT was in the process of shutting down,
            // wait for the BT process to fully tear down and then force a restart
            // here.  This is a bit of a hack (b/29363429).
            if (mPreviousAdapterState == BluetoothAdapter.STATE_BLE_TURNING_OFF) {
                if (mEnable) {
                    log("Entering STATE_OFF but mEnabled is true; restarting.");
                    // waitForState(Set.of(BluetoothAdapter.STATE_OFF));
                    sendMessageDelayed(MESSAGE_RESTART_BLUETOOTH_SERVICE, SERVICE_RESTART_TIME_MS); //getServiceRestartMs());
                }
            }
        }

        @Override
        public boolean processMessage(Message message) { // BluetoothOff
            switch (message.what) {
                case MESSAGE_GET_NAME_AND_ADDRESS:
                    return NOT_HANDLED; // Handled in BluetoothBinded state
                // case MESSAGE_ENABLE: {
                //     mGetNameAddressOnly = false;
                //     // if (!synchronousEnable(mQuietEnable, mContext.getAttributionSource())) {
                //     //     transitionTo(mBluetoothOn);
                //     // } else {
                //     //     Log.e(TAG, "IBluetooth.enable() returned false");
                //     // }
                //     break;
                // }
                case MESSAGE_USER_SWITCHED:
                    removeMessages(MESSAGE_USER_SWITCHED);
                    // mBluetoothNotificationManager.createNotificationChannels();

                    message.arg2++;
                    // if user is switched when bluetooth is not enabled yet, retry after a delay
                    sendMessageDelayed(message, USER_SWITCHED_TIME_MS);
                    log("Retry MESSAGE_USER_SWITCHED " + message.arg2);
                    break;
                // case MESSAGE_ENABLE:
                //     mWaitForEnableRetry = 0;
                //     sendMessageDelayed(MESSAGE_HANDLE_ENABLE_DELAYED, ENABLE_DISABLE_DELAY_MS);
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class BluetoothBleOn extends BluetoothAdapterState {
        BluetoothBleOn() { super(BluetoothAdapter.STATE_BLE_ON); }
        @Override
        public void enter() {
            if (mPreviousAdapterState == BluetoothAdapter.STATE_TURNING_OFF) {
                log("Intermediate off, back to LE only mode");
                // For LE only mode, broadcast as is
                sendBleStateChanged(mPreviousAdapterState, mAdapterState);
                mBluetoothManagerService.sendBluetoothStateCallback(false); // BT is OFF for general users
                mBluetoothManagerService.sendBrEdrDownCallback(mBluetooth, mContext.getAttributionSource());
                standardBroadcast(mPreviousAdapterState, BluetoothAdapter.STATE_OFF); // Broadcast as STATE_OFF
            } else {
                // connect to GattService
                log("Bluetooth is in LE only mode");
                if (mBluetoothGatt != null || !mContext.getPackageManager()
                        .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    // continueFromBleOnState();
                } else {
                    log("Binding Bluetooth GATT service");
                    Intent i = new Intent(IBluetoothGatt.class.getName());
                    doBind(mContext, i, mConnectionGatt, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT,
                            UserHandle.CURRENT);
                }
                sendBleStateChanged(mPreviousAdapterState, mAdapterState);
            }




            // handle error state transition case from TURNING_ON to OFF
            // unbind and rebind bluetooth service and enable bluetooth
            if (mPreviousAdapterState == BluetoothAdapter.STATE_TURNING_ON && mEnable) {
                recoverBluetoothServiceFromError(true);
            }
            // bluetooth is working, reset the counter
            if (mErrorRecoveryRetryCounter != 0) {
                Log.w(TAG, "bluetooth is recovered from error");
                mErrorRecoveryRetryCounter = 0;
            }
        }

        @Override
        public boolean processMessage(Message message) { // BluetoothBleOn
            switch (message.what) {
                case MESSAGE_ENABLE: {
                    if (!commonEnable(message)) {
                        break;
                    }
                    if (mIsBle) {
                        Log.i(TAG, "Already at BLE_ON State");
                        break;
                    }
                    Log.w(TAG, "BT Enable in BLE_ON State, going to ON");
                    mBluetoothManagerService.onLeServiceUp(mBluetooth);
                    break;
                }
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class BluetoothTurningOn extends BluetoothAdapterState {
        BluetoothTurningOn() { super(BluetoothAdapter.STATE_TURNING_ON); }
        @Override
        public void enter() {
            sendBleStateChanged(mPreviousAdapterState, mAdapterState);
            standardBroadcast(mPreviousAdapterState, mAdapterState);
        }
        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                case MESSAGE_ENABLE: {
                    if (!commonEnable(message)) {
                        break;
                    }
                    Log.i(TAG, "MESSAGE_ENABLE: already enabled");
                    break;
                }
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }
    class BluetoothTurningOff extends BluetoothAdapterState {
        BluetoothTurningOff() { super(BluetoothAdapter.STATE_TURNING_OFF); }
        @Override
        public void enter() {
            sendBleStateChanged(mPreviousAdapterState, mAdapterState);
            standardBroadcast(mPreviousAdapterState, mAdapterState);
        }
        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                // case MESSAGE_ENABLE:
                //     mWaitForEnableRetry = 0;
                //     sendMessageDelayed(MESSAGE_HANDLE_ENABLE_DELAYED, ENABLE_DISABLE_DELAY_MS);
                default:
                    return NOT_HANDLED;
            }
            // return HANDLED;
        }
    }
    class BluetoothBleTurningOn extends BluetoothAdapterState {
        BluetoothBleTurningOn() { super(BluetoothAdapter.STATE_BLE_TURNING_ON); }
        @Override
        public void enter() {
            sendBleStateChanged(mPreviousAdapterState, mAdapterState);
        }
        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                case MESSAGE_ENABLE: {
                    if (!commonEnable(message)) {
                        break;
                    }
                    Log.i(TAG, "MESSAGE_ENABLE: already enabled");
                    break;
                }
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }
    class BluetoothBleTurningOff extends BluetoothAdapterState {
        BluetoothBleTurningOff() { super(BluetoothAdapter.STATE_BLE_TURNING_OFF); }
        @Override
        public void enter() {
            sendBleStateChanged(mPreviousAdapterState, mAdapterState);
        }
        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                // case MESSAGE_ENABLE:
                //     mWaitForEnableRetry = 0;
                //     sendMessageDelayed(MESSAGE_HANDLE_ENABLE_DELAYED, ENABLE_DISABLE_DELAY_MS);
                default:
                    return NOT_HANDLED;
            }
            // return HANDLED;
        }
    }
    class BluetoothOn extends BluetoothAdapterState {
        BluetoothOn() { super(BluetoothAdapter.STATE_ON); }
        @Override
        public void enter() {
            mBluetoothManagerService.sendBluetoothStateCallback(true);
            sendBleStateChanged(mPreviousAdapterState, mAdapterState);

            standardBroadcast(mPreviousAdapterState, mAdapterState);

            // bluetooth is working, reset the counter
            if (mErrorRecoveryRetryCounter != 0) {
                Log.w(TAG, "bluetooth is recovered from error");
                mErrorRecoveryRetryCounter = 0;
            }

        }

        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                case MESSAGE_ENABLE: {
                    if (!commonEnable(message)) {
                        break;
                    }
                    Log.i(TAG, "MESSAGE_ENABLE: already enabled");
                    break;
                }
                case MESSAGE_DISABLE: {
                    // if (!synchronousDisable(mContext.getAttributionSource())) {
                    //     transitionTo(mBluetoothOff);
                    // } else {
                    //     Log.e(TAG, "IBluetooth.disable() returned false");
                    // }
                    break;
                }
                case MESSAGE_USER_SWITCHED: {
                    removeMessages(MESSAGE_USER_SWITCHED);
                    // mBluetoothNotificationManager.createNotificationChannels();

                    /* disable and enable BT when detect a user switch */
                    // TODO RESTART USING STATE MACHINE
                    // mRestartInProgress = true;
                    // restartForReason(BluetoothProtoEnums.ENABLE_DISABLE_REASON_USER_SWITCH);
                    break;
                }
                // case MESSAGE_INIT_FLAGS_CHANGED: {
                //     mHandler.removeMessages(MESSAGE_INIT_FLAGS_CHANGED);
                //     if (mBluetoothModeChangeHelper.isMediaProfileConnected()) {
                //         Log.i(TAG, "Delaying MESSAGE_INIT_FLAGS_CHANGED by "
                //                 + DELAY_FOR_RETRY_INIT_FLAG_CHECK_MS
                //                 + " ms due to existing connections");
                //         mHandler.sendEmptyMessageDelayed(
                //                 MESSAGE_INIT_FLAGS_CHANGED,
                //                 DELAY_FOR_RETRY_INIT_FLAG_CHECK_MS);
                //         break;
                //     }
                //     if (!isDeviceProvisioned(mContext.getContentResolver())) {
                //         Log.i(TAG, "Delaying MESSAGE_INIT_FLAGS_CHANGED by "
                //                 + DELAY_FOR_RETRY_INIT_FLAG_CHECK_MS
                //                 +  "ms because device is not provisioned");
                //         mHandler.sendEmptyMessageDelayed(
                //                 MESSAGE_INIT_FLAGS_CHANGED,
                //                 DELAY_FOR_RETRY_INIT_FLAG_CHECK_MS);
                //         break;
                //     }
                //     if (mBluetooth != null && isEnabled()) {
                //         Log.i(TAG, "Restarting Bluetooth due to init flag change");
                //         restartForReason(
                //                 BluetoothProtoEnums.ENABLE_DISABLE_REASON_INIT_FLAGS_CHANGED);
                //     }
                //     break;
                // }
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        //@RequiresPermission(allOf = {
        //        android.Manifest.permission.BLUETOOTH_CONNECT,
        //        android.Manifest.permission.BLUETOOTH_PRIVILEGED
        //})
        //private void restartForReason(int reason) {
        //    mBluetoothManagerService.unregisterCallback(mBluetooth, mBluetoothCallback,
        //            mContext.getAttributionSource());

        //    if (mState == BluetoothAdapter.STATE_TURNING_OFF) {
        //        // MESSAGE_USER_SWITCHED happened right after MESSAGE_ENABLE
        //        bluetoothStateChangeHandler(mState, BluetoothAdapter.STATE_OFF);
        //        mState = BluetoothAdapter.STATE_OFF;
        //    }
        //    if (mState == BluetoothAdapter.STATE_OFF) {
        //        bluetoothStateChangeHandler(mState, BluetoothAdapter.STATE_TURNING_ON);
        //        mState = BluetoothAdapter.STATE_TURNING_ON;
        //    }

        //    waitForState(Set.of(BluetoothAdapter.STATE_ON));

        //    if (mState == BluetoothAdapter.STATE_TURNING_ON) {
        //        bluetoothStateChangeHandler(mState, BluetoothAdapter.STATE_ON);
        //    }

        //    unbindAllBluetoothProfileServices();
        //    // disable
        //    addActiveLog(reason, mContext.getPackageName(), false);
        //    handleDisable();
        //    // Pbap service need receive STATE_TURNING_OFF intent to close
        //    bluetoothStateChangeHandler(BluetoothAdapter.STATE_ON,
        //            BluetoothAdapter.STATE_TURNING_OFF);

        //    boolean didDisableTimeout =
        //            !waitForState(Set.of(BluetoothAdapter.STATE_OFF));

        //    bluetoothStateChangeHandler(BluetoothAdapter.STATE_TURNING_OFF,
        //            BluetoothAdapter.STATE_OFF);
        //    sendBluetoothServiceDownCallback();

        //    try {
        //        mBluetoothLock.writeLock().lock();
        //        if (mBluetooth != null) {
        //            mBluetooth = null;
        //            // Unbind
        //            mContext.unbindService(mConnection);
        //        }
        //        mBluetoothGatt = null;
        //    } finally {
        //        mBluetoothLock.writeLock().unlock();
        //    }

        //    //
        //    // If disabling Bluetooth times out, wait for an
        //    // additional amount of time to ensure the process is
        //    // shut down completely before attempting to restart.
        //    //
        //    if (didDisableTimeout) {
        //        SystemClock.sleep(3000);
        //    } else {
        //        SystemClock.sleep(100);
        //    }

        //    mHandler.removeMessages(MESSAGE_BLUETOOTH_STATE_CHANGE);
        //    mState = BluetoothAdapter.STATE_OFF;
        //    // enable
        //    addActiveLog(reason, mContext.getPackageName(), true);
        //    // mEnable flag could have been reset on disableBLE. Reenable it.
        //    mEnable = true;
        //    handleEnable(mQuietEnable);
        //}
    }

    private void standardBroadcast(int prevState, int newState) {
        if (prevState == BluetoothAdapter.STATE_BLE_ON) {
            // Show prevState of BLE_ON as OFF to standard users
            prevState = BluetoothAdapter.STATE_OFF;
        }
        log("Sending " + BluetoothAdapter.ACTION_STATE_CHANGED
                + ": From=" + BluetoothAdapter.nameForState(prevState)
                + " To=" + BluetoothAdapter.nameForState(newState));
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, newState);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL, null,
                getTempAllowlistBroadcastOptions());
    }

    private boolean commonEnable(Message message) {
        if (hasMessages(MESSAGE_HANDLE_DISABLE_DELAYED) || hasMessages(MESSAGE_HANDLE_ENABLE_DELAYED)) {
            // We are handling enable or disable right now, wait for it.
            // sendMessageDelayed(MESSAGE_ENABLE, quietEnable, isBle, ENABLE_DISABLE_DELAY_MS);
            return false;
        }
        removeMessages(MESSAGE_RESTART_BLUETOOTH_SERVICE);
        if (mShutdownInProgress) {
            log(getCurrentState() + ": Skip Bluetooth Enable in device shutdown process");
            return false;
        }
        mQuietEnable = (message.arg1 == 1);
        mEnable = true;
        mIsBle = (message.arg2 == 1);
        mGetNameAddressOnly = false;
        if (!mIsBle) {
            persistBluetoothSetting(mContext, BLUETOOTH_ON_BLUETOOTH);
        }
        log(getCurrentState() + ": ENABLE(" + mQuietEnable + ", " + mIsBle + ")");
        return true;
    }

    private boolean commonDisable() {
        if (hasMessages(MESSAGE_HANDLE_DISABLE_DELAYED) || hasMessages(MESSAGE_HANDLE_ENABLE_DELAYED)) {
            // We are handling enable or disable right now, wait for it.
            sendMessageDelayed(MESSAGE_DISABLE, ENABLE_DISABLE_DELAY_MS);
            return false;
        }
        removeMessages(MESSAGE_RESTART_BLUETOOTH_SERVICE);
        return true;
    }

    protected String getWhatToString(int what) {
        switch(what) {
            // TODO
            default:
                return "Unknown state: " + what;
        }
    }

    private final IBluetoothCallback mBluetoothCallback = new IBluetoothCallback.Stub() {
        @Override
        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException {
            sendMessage(MESSAGE_BLUETOOTH_STATE_CHANGE, prevState, newState);
        }
    };


    private class BluetoothServiceConnection implements ServiceConnection {
        final int mArg;
        BluetoothServiceConnection(int arg) {
            mArg = arg;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            final String name = componentName.getClassName();
            log("BluetoothServiceConnection.onServiceConnected: " + name);
            sendMessage(MESSAGE_BLUETOOTH_SERVICE_CONNECTED, mArg, 0, service);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Called if we unexpectedly disconnect.
            final String name = componentName.getClassName();
            log("BluetoothServiceConnection.onServiceDisconnected: " + name);
            sendMessage(MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED, mArg);
        }
        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "WILLIAM ------------   BINDER DIED");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.e(TAG, "WILLIAM ------------   NULL BINDING");
        }
    }

    private void sendBleStateChanged(int prevState, int newState) {
        logd("Sending BLE State Change: From=" + BluetoothAdapter.nameForState(prevState)
                + " To=" + BluetoothAdapter.nameForState(newState));
        // Send broadcast message to everyone else
        Intent intent = new Intent(BluetoothAdapter.ACTION_BLE_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, newState);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL, null, getTempAllowlistBroadcastOptions());
    }

    private void recoverBluetoothServiceFromError(boolean clearBle) {
        //Log.e(TAG, "recoverBluetoothServiceFromError");
        //try {
        //    mBluetoothLock.readLock().lock();
        //    if (mBluetooth != null) {
        //        //Unregister callback object
        //        synchronousUnregisterCallback(mBluetooth, mBluetoothCallback, mContext.getAttributionSource());
        //    }
        //} catch (RemoteException | TimeoutException e) {
        //    Log.e(TAG, "Unable to unregister", e);
        //} finally {
        //    mBluetoothLock.readLock().unlock();
        //}

        //SystemClock.sleep(500);

        //// disable
        //addActiveLog(BluetoothProtoEnums.ENABLE_DISABLE_REASON_START_ERROR,
        //        mContext.getPackageName(), false);
        //handleDisable();

        //waitForState(Set.of(BluetoothAdapter.STATE_OFF));

        //sendBluetoothServiceDownCallback();

        //try {
        //    mBluetoothLock.writeLock().lock();
        //    if (mBluetooth != null) {
        //        mBluetooth = null;
        //        // Unbind
        //        mContext.unbindService(mConnection);
        //    }
        //    mBluetoothGatt = null;
        //} finally {
        //    mBluetoothLock.writeLock().unlock();
        //}

        //mHandler.removeMessages(MESSAGE_BLUETOOTH_STATE_CHANGE);
        //mState = BluetoothAdapter.STATE_OFF;

        //if (clearBle) {
        //    clearBleApps();
        //}

        //mEnable = false;

        //// Send a Bluetooth Restart message to reenable bluetooth
        //Message restartMsg = mHandler.obtainMessage(MESSAGE_RESTART_BLUETOOTH_SERVICE);
        //mHandler.sendMessageDelayed(restartMsg, ERROR_RESTART_TIME_MS);
    }

    private boolean isBleState(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_BLE_ON:
            case BluetoothAdapter.STATE_BLE_TURNING_ON:
            case BluetoothAdapter.STATE_BLE_TURNING_OFF:
                return true;
        }
        return false;
    }

    @Override
    protected void log(String msg) {
        if (DBG) {
            //TODO check logs
            super.log(msg);
        }
    }
}
