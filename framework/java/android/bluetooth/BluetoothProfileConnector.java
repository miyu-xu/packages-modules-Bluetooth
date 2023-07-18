/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.CloseGuard;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Connector for Bluetooth profile proxies to bind manager service and profile services
 *
 * @param <T> The Bluetooth profile interface for this connection.
 * @hide
 */
@SuppressLint("AndroidFrameworkBluetoothPermission")
public final class BluetoothProfileConnector extends Handler {
    private static final String TAG = BluetoothProfileConnector.class.getSimpleName();
    private final CloseGuard mCloseGuard = new CloseGuard();
    private final int mProfileId;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final BluetoothProfile mProfileProxy;
    private String mPackageName;
    private final IBluetoothManager mBluetoothManager;
    private boolean mBound = false;

    private static final int MESSAGE_SERVICE_CONNECTED = 100;
    private static final int MESSAGE_SERVICE_DISCONNECTED = 101;

    /** @hide */
    public BluetoothProfileConnector(
            Looper looper,
            BluetoothProfile profile,
            int profileId,
            IBluetoothManager bluetoothManager) {
        super(looper);
        mProfileId = profileId;
        mProfileProxy = profile;
        mBluetoothManager = Objects.requireNonNull(bluetoothManager);
    }

    BluetoothProfileConnector(BluetoothProfile profile, int profileId) {
        this(
                Looper.getMainLooper(),
                profile,
                profileId,
                BluetoothAdapter.getDefaultAdapter().getBluetoothManager());
    }

    /** {@hide} */
    @Override
    public void finalize() {
        mCloseGuard.warnIfOpen();
        onBluetoothOff();
    }

    void onBluetoothOn(IBluetooth bluetooth, boolean maybeOff) {
        if (bluetooth == null) return;
        try {
            final SynchronousResultReceiver<IBinder> recv = SynchronousResultReceiver.get();
            bluetooth.getProfile(mProfileId, recv);
            IBinder binder = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);

            // TODO: remove
            if (!maybeOff && binder == null) {
                throw new IllegalStateException();
            }

            if (binder != null) {
                mProfileProxy.onServiceConnected(binder);
                sendEmptyMessage(MESSAGE_SERVICE_CONNECTED);
            }
            mCloseGuard.open("doUnbind");
        } catch (RemoteException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void onBluetoothOff() {
        mCloseGuard.close();
        mProfileProxy.onServiceDisconnected();
        sendEmptyMessage(MESSAGE_SERVICE_DISCONNECTED);
    }

    /** @hide */
    public void connect(String packageName, BluetoothProfile.ServiceListener listener) {
        mPackageName = packageName;
        mServiceListener = listener;
    }

    /** @hide */
    public void disconnect() {
        if (mServiceListener != null) {
            BluetoothProfile.ServiceListener listener = mServiceListener;
            mServiceListener = null;
            listener.onServiceDisconnected(mProfileId);
        }
        onBluetoothOff();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_SERVICE_CONNECTED:
                if (mServiceListener != null) {
                    mServiceListener.onServiceConnected(mProfileId, mProfileProxy);
                }
                break;
            case MESSAGE_SERVICE_DISCONNECTED:
                if (mServiceListener != null) {
                    mServiceListener.onServiceDisconnected(mProfileId);
                }
                break;
        }
    }
}
