/*
 * Copyright 2023 The Android Open Source Project
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

import static com.android.server.bluetooth.BluetoothManagerService.MESSAGE_ENABLE;
import static com.android.server.bluetooth.BluetoothManagerService.MESSAGE_TIMEOUT_BIND;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@RunWith(AndroidJUnit4.class)
public class BluetoothManagerServiceTest {
    static final int TIMEOUT = 3000;
    BluetoothManagerService mManagerService;

    Context mContext = spy(
            new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()));

    @Spy
    BluetoothServerProxy mBluetoothServerProxy;
    HandlerThread mHandlerThread = new HandlerThread("BluetoothManagerServiceTest");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        BluetoothServerProxy.setInstanceForTesting(mBluetoothServerProxy);

        doReturn(mHandlerThread).when(mBluetoothServerProxy).createHandlerThread(any());
        // Mock these functions so security errors won't throw
        doReturn("name").when(mBluetoothServerProxy).settingsSecureGetString(any(),
                eq(Settings.Secure.BLUETOOTH_NAME));
        doReturn("00:11:22:33:44:55").when(mBluetoothServerProxy).settingsSecureGetString(any(),
                eq(Settings.Secure.BLUETOOTH_ADDRESS));

    }

    @After
    public void tearDown() {
        mHandlerThread.quitSafely();
    }

    @Test
    public void onUserRestrictionsChanged_disallowBluetooth_onlySendDisableMessageOnSystemUser()
            throws InterruptedException {
        doReturn(mock(Intent.class)).when(mContext).registerReceiverForAllUsers(any(), any(),
                eq(null), eq(null));

        // Spy UserManager so we can mimic the case when restriction settings changed
        UserManager userManager = mock(UserManager.class);
        doReturn(userManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(userManager).hasUserRestrictionForUser(
                eq(UserManager.DISALLOW_BLUETOOTH), any());
        doReturn(false).when(userManager).hasUserRestrictionForUser(
                eq(UserManager.DISALLOW_BLUETOOTH_SHARING), any());
        mManagerService = new BluetoothManagerService(mContext);

        // Check if disable message sent once for system user only
        // Since Message object is recycled after processed, use proxy function to get what value

        // test run on user -1, should not turning Bluetooth off
        mManagerService.onUserRestrictionsChanged(UserHandle.CURRENT);
        verify(mBluetoothServerProxy, timeout(TIMEOUT).times(0)).handlerSendWhatMessage(
                any(BluetoothManagerService.BluetoothHandler.class),
                eq(BluetoothManagerService.MESSAGE_DISABLE));

        // called from SYSTEM user, should try to toggle Bluetooth off
        mManagerService.onUserRestrictionsChanged(UserHandle.SYSTEM);
        verify(mBluetoothServerProxy, timeout(TIMEOUT)).handlerSendWhatMessage(
                any(BluetoothManagerService.BluetoothHandler.class),
                eq(BluetoothManagerService.MESSAGE_DISABLE));
    }

    @Test
    public void bindFailed() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();
        BluetoothManagerService service = new BluetoothManagerService(mContext);
        Handler handler = service.mHandler;

        doReturn(false)
                .when(mContext)
                .bindServiceAsUser(
                        any(Intent.class),
                        any(ServiceConnection.class),
                        anyInt(),
                        any(UserHandle.class));
        handler.handleMessage(handler.obtainMessage(MESSAGE_ENABLE));
        // assertThat(service.mEnable).isFalse(); // TODO(b/280518177): Cleanup managerService state
        assertThat(service.mBinding).isFalse();
        assertThat(handler.hasMessages(MESSAGE_TIMEOUT_BIND)).isFalse();
    }

    @Test
    public void bindTimeout() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();
        BluetoothManagerService service = new BluetoothManagerService(mContext);
        Handler handler = service.mHandler;

        doReturn(true)
                .when(mContext)
                .bindServiceAsUser(
                        any(Intent.class),
                        any(ServiceConnection.class),
                        anyInt(),
                        any(UserHandle.class));
        handler.handleMessage(handler.obtainMessage(MESSAGE_ENABLE));
        assertThat(service.mEnable).isTrue();
        assertThat(service.mBinding).isTrue();
        assertThat(handler.hasMessages(MESSAGE_TIMEOUT_BIND)).isTrue();
        // Force handling the message now without waiting for the timeout to fire
        handler.removeMessages(MESSAGE_TIMEOUT_BIND);
        handler.handleMessage(handler.obtainMessage(MESSAGE_TIMEOUT_BIND));

        assertThat(service.mBinding).isFalse();
        // assertThat(service.mEnable).isFalse(); // TODO(b/280518177): Cleanup managerService state
    }
}
