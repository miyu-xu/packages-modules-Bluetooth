/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class BluetoothManagerServiceTest {
    BluetoothManagerService mManagerService;
    Context mContext;
    @Mock
    BluetoothServerProxy mBluetoothServerProxy;
    @Mock
    BluetoothManagerService.BluetoothHandler mHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(new ContextWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext()));
    }

    @After
    public void tearDown() {
        mManagerService.mBluetoothHandlerThread.quitSafely();
    }

    @Test
    public void onUserRestrictionsChanged_disallowBluetooth_onlySendDisableMessageOnSystemUser()
            throws InterruptedException {
        doReturn(mock(Intent.class)).when(mContext).registerReceiverForAllUsers(any(), any(),
                eq(null), eq(null));
        BluetoothServerProxy.setInstanceForTesting(mBluetoothServerProxy);
        doReturn(mHandler).when(mBluetoothServerProxy).newBluetoothHandler(any());
        doReturn("name").when(mBluetoothServerProxy).settingsSecureGetString(any(),
                eq(Settings.Secure.BLUETOOTH_NAME));
        doReturn("00:11:22:33:44:55").when(mBluetoothServerProxy).settingsSecureGetString(any(),
                eq(Settings.Secure.BLUETOOTH_ADDRESS));
        UserManager userManager = spy(mContext.getSystemService(UserManager.class));
        doReturn(userManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(userManager).hasUserRestrictionForUser(
                eq(UserManager.DISALLOW_BLUETOOTH), argThat(x -> {
                    return true;
                }));
        mManagerService = new BluetoothManagerService(mContext);

        // test run on user -1, should not turning Bluetooth off
        mManagerService.onUserRestrictionsChanged(UserHandle.CURRENT);
        verifyNoMoreInteractions(mHandler);

        // called from SYSTEM user, should try to toggle Bluetooth off
        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            Message arg = invocation.getArgument(0);
            if (arg != null && Objects.equals(arg.what,
                    BluetoothManagerService.MESSAGE_DISABLE)) {
                countDownLatch.countDown();
            }
            return null;
        }).when(mHandler).sendMessage(any());
        mManagerService.onUserRestrictionsChanged(UserHandle.SYSTEM);
        countDownLatch.await(3000, TimeUnit.MILLISECONDS);
    }
}
