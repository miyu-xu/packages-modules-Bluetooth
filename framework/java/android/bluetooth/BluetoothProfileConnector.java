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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.CloseGuard;
import android.util.Log;

import com.android.server.bluetooth.Messages;

import java.util.List;
/**
 * Connector for Bluetooth profile proxies to bind manager service and
 * profile services
 * @param <T> The Bluetooth profile interface for this connection.
 * @hide
 */
@SuppressLint("AndroidFrameworkBluetoothPermission")
public abstract class BluetoothProfileConnector<T> {
    private final CloseGuard mCloseGuard = new CloseGuard();
    private final int mProfileId;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final BluetoothProfile mProfileProxy;
    private Context mContext;
    private final String mProfileName;
    private final String mServiceName;
    private volatile T mService;

    // -3 match with UserHandle.USER_CURRENT_OR_SELF
    private static final UserHandle USER_HANDLE_CURRENT_OR_SELF = UserHandle.of(-3);

    private static final int MESSAGE_SERVICE_CONNECTED = 100;
    private static final int MESSAGE_SERVICE_DISCONNECTED = 101;

    private ComponentName resolveSystemService(@NonNull Intent intent) {
        List<ResolveInfo> results = mContext.getPackageManager().queryIntentServices(intent, 0);
        if (results == null) {
            return null;
        }
        ComponentName comp = null;
        for (int i = 0; i < results.size(); i++) {
            ResolveInfo ri = results.get(i);
            if ((ri.serviceInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                continue;
            }
            ComponentName foundComp =
                    new ComponentName(
                            ri.serviceInfo.applicationInfo.packageName, ri.serviceInfo.name);
            if (comp != null) {
                throw new IllegalStateException(
                        "Multiple system services handle "
                                + intent
                                + ": "
                                + comp
                                + ", "
                                + foundComp);
            }
            comp = foundComp;
        }
        return comp;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            logDebug("Proxy object connected");
            mService = getServiceInterface(service);
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(mProfileId, mProfileProxy);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            logDebug("Proxy object disconnected");
            doUnbind();
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected(mProfileId);
            }
        }
    };

    BluetoothProfileConnector(BluetoothProfile profile, int profileId, String profileName,
            String serviceName) {
        mProfileId = profileId;
        mProfileProxy = profile;
        mProfileName = profileName;
        mServiceName = serviceName;
    }

    /** {@hide} */
    @Override
    public void finalize() {
        mCloseGuard.warnIfOpen();
        doUnbind();
    }

    private boolean doBind() {
        synchronized (mConnection) {
            if (mService != null) {
                // Already Binded
                return true;
            }
            logDebug("Binding service for " + mContext.getPackageName());

            Intent connectionIntent = new Intent(mServiceName);
            ComponentName comp = resolveSystemService(connectionIntent);
            if (comp == null) {
                logError("Failed to find ComponentName.");
                return false;
            }
            connectionIntent.setComponent(comp);

            if (!mContext.bindService(connectionIntent, mConnection, 0)) {
                logError("Failed to bind service.");
                mContext.unbindService(mConnection);
                return false;
            }
            mCloseGuard.open("doUnbind");
        }
        return true;
    }

    private void doUnbind() {
        synchronized (mConnection) {
            logDebug("Unbinding service for " + mContext.getPackageName());
            mCloseGuard.close();
            mContext.unbindService(mConnection);
            mService = null;
        }
    }

    // The Messenger is using the application Main thread
    Messenger mMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Messages what = Messages.values()[msg.what];
            logDebug("handleMessage from Messenger: " + what);
            switch (what) {
                case STATE_CHANGE_TO_ON:
                    doBind();
                    break;
                case STATE_CHANGE_TO_OFF:
                    doUnbind();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    });

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
                mgr.registerStateChangeMessenger(mMessenger, mServiceName);
            } catch (RemoteException re) {
                logError("Failed to register state change messenger." + re);
            }
        }
        // doBind();
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
                mgr.unregisterStateChangeMessenger(mMessenger, mServiceName);
            } catch (RemoteException re) {
                logError("Failed to unregister state change messenger" + re);
            }
        }
        doUnbind();
    }

    T getService() {
        return mService;
    }

    /**
     * This abstract function is used to implement method to get the
     * connected Bluetooth service interface.
     * @param service the connected binder service.
     * @return T the binder interface of {@code service}.
     * @hide
     */
    public abstract T getServiceInterface(IBinder service);

    private void logDebug(String log) {
        Log.d(mProfileName, log);
    }

    private void logError(String log) {
        Log.e(mProfileName, log);
    }
}
