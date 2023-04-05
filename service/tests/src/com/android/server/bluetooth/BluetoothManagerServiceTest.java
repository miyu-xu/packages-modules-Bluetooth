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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
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
    HandlerThread mHandlerThread;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(new ContextWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext()));
        mHandlerThread = new HandlerThread("BluetoothManagerServiceTest");
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
        BluetoothServerProxy.setInstanceForTesting(mBluetoothServerProxy);
        // Mock the handler to avoid handle message & to terminate the thread after
        // test
        doReturn(mHandlerThread).when(mBluetoothServerProxy).createHandlerThread(any());
        doReturn(mHandler).when(mBluetoothServerProxy).newBluetoothHandler(any());

        // Mock these functions so security errors won't throw
        doReturn("name").when(mBluetoothServerProxy).settingsSecureGetString(any(),
                eq(Settings.Secure.BLUETOOTH_NAME));
        doReturn("00:11:22:33:44:55").when(mBluetoothServerProxy).settingsSecureGetString(any(),
                eq(Settings.Secure.BLUETOOTH_ADDRESS));

        // Spy UserManager so we can mimic the case when restriction settings changed
        UserManager userManager = spy(mContext.getSystemService(UserManager.class));
        doReturn(userManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(userManager).hasUserRestrictionForUser(
                eq(UserManager.DISALLOW_BLUETOOTH), any());
        mManagerService = new BluetoothManagerService(mContext);

        // check if disable message sent once for system user only
        // Since Message object is recycled after processed, use proxy function to get what value
        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            int arg = invocation.getArgument(1);
            if (Objects.equals(arg,
                    BluetoothManagerService.MESSAGE_DISABLE)) {
                countDownLatch.countDown();
            }
            return null;
        }).when(mBluetoothServerProxy).handlerSendWhatMessage(eq(mHandler), anyInt());

        // test run on user -1, should not turning Bluetooth off
        mManagerService.onUserRestrictionsChanged(UserHandle.CURRENT);
        assertThat(countDownLatch.await(3000, TimeUnit.MILLISECONDS)).isFalse();

        // called from SYSTEM user, should try to toggle Bluetooth off
        mManagerService.onUserRestrictionsChanged(UserHandle.SYSTEM);
        assertThat(countDownLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue();
    }
}
