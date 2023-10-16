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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

/**
 * Connector for Bluetooth profile proxies to bind manager service and profile services
 *
 * @param <T> The Bluetooth profile interface for this connection.
 * @hide
 */
@SuppressLint("AndroidFrameworkBluetoothPermission")
public final class BluetoothProfileConnector {
    private static final String TAG = BluetoothProfileConnector.class.getSimpleName();
    private final CloseGuard mCloseGuard = new CloseGuard();
    private final int mProfileId;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final BluetoothProfile mProfileProxy;
    private Context mContext;
    private volatile IBinder mService;

    private static final int MESSAGE_SERVICE_CONNECTED = 100;
    private static final int MESSAGE_SERVICE_DISCONNECTED = 101;

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
        public void onBluetoothStateChange(boolean up) {
            if (up) {
                doBind();
            } else {
                doUnbind();
            }
        }
    };

    private final IBluetoothProfileServiceConnection mConnection =
            new IBluetoothProfileServiceConnection.Stub() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    Log.d(
                            TAG,
                            "Proxy object connected for "
                                    + BluetoothProfile.getProfileName(mProfileId));
                    mService = service;
                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SERVICE_CONNECTED));
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    Log.d(
                            TAG,
                            "Proxy object disconnected for "
                                    + BluetoothProfile.getProfileName(mProfileId));
                    doUnbind();
                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SERVICE_DISCONNECTED));
                }
            };

    BluetoothProfileConnector(BluetoothProfile profile, int profileId) {
        mProfileId = profileId;
        mProfileProxy = profile;
    }

    /** {@hide} */
    @Override
    public void finalize() {
        mCloseGuard.warnIfOpen();
        doUnbind();
    }

    private boolean doBind() {
        synchronized (mConnection) {
            if (mService == null) {
                Log.d(
                        TAG,
                        "Binding service "
                                + BluetoothProfile.getProfileName(mProfileId)
                                + " for "
                                + mContext.getPackageName());
                mCloseGuard.open("doUnbind");
                try {
                    return BluetoothAdapter.getDefaultAdapter()
                            .getBluetoothManager()
                            .bindBluetoothProfileService(mProfileId, mConnection);
                } catch (RemoteException re) {
                    Log.e(
                            TAG,
                            "Failed to bind service. "
                                    + BluetoothProfile.getProfileName(mProfileId),
                            re);
                    return false;
                }
            }
        }
        return true;
    }

    private void doUnbind() {
        synchronized (mConnection) {
            if (mService != null) {
                Log.d(
                        TAG,
                        "Unbinding service "
                                + BluetoothProfile.getProfileName(mProfileId)
                                + " for "
                                + mContext.getPackageName());
                mCloseGuard.close();
                try {
                    BluetoothAdapter.getDefaultAdapter().getBluetoothManager()
                            .unbindBluetoothProfileService(mProfileId, mConnection);
                } catch (RemoteException re) {
                    Log.e(
                            TAG,
                            "Unable to unbind service "
                                    + BluetoothProfile.getProfileName(mProfileId),
                            re);
                } finally {
                    mService = null;
                }
            }
        }
    }

    void connect(Context context, BluetoothProfile.ServiceListener listener) {
        mContext = context;
        mServiceListener = listener;
        IBluetoothManager mgr = BluetoothAdapter.getDefaultAdapter().getBluetoothManager();

        // Preserve legacy compatibility where apps were depending on
        // registerStateChangeCallback() performing a permissions check which
        // has been relaxed in modern platform versions
        if (context.getApplicationInfo().targetSdkVersion <= Build.VERSION_CODES.R
                && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Need BLUETOOTH permission");
        }

        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                Log.e(TAG, "Failed to register state change callback.", re);
            }
        }
    }

    void disconnect() {
        if (mServiceListener != null) {
            BluetoothProfile.ServiceListener listener = mServiceListener;
            mServiceListener = null;
            listener.onServiceDisconnected(mProfileId);
        }
        IBluetoothManager mgr = BluetoothAdapter.getDefaultAdapter().getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                Log.e(TAG, "Failed to unregister state change callback", re);
            }
        }
    }

    IBinder getService() {
        return mService;
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SERVICE_CONNECTED: {
                    if (mServiceListener != null) {
                        mServiceListener.onServiceConnected(mProfileId, mProfileProxy);
                    }
                    break;
                }
                case MESSAGE_SERVICE_DISCONNECTED: {
                    if (mServiceListener != null) {
                        mServiceListener.onServiceDisconnected(mProfileId);
                    }
                    break;
                }
            }
        }
    };
}
