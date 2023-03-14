/*
 * Copyright 2022 The Android Open Source Project
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
package com.android.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.btservice.ProfileService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.concurrent.GuardedBy;

/**
 * Test for Utils.java
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class UtilsTest {
    private static final String TAG = "UtilsTest";
    private static final int REMOVE_CHECK_INTERVAL_MILLIS = 500; // 0.5 seconds
    private static final int REMOVE_TIMEOUT_MILLIS = 60 * 1000; // 60 seconds
    private static final int SWITCH_USER_TIMEOUT_MILLIS = 40 * 1000; // 40 seconds

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Object mUserRemoveLock = new Object();
    private final Object mUserSwitchLock = new Object();
    UserManager mUserManager;
    private List<Integer> mUsersToRemove;
    private int mUserIdReceived;
    private int mForegroundUserId;

    @Before
    public void setUp() {
        mForegroundUserId = Utils.getForegroundUserId();
        int callingUid = Binder.getCallingUid();
        UserHandle callingUser = UserHandle.getUserHandleForUid(callingUid);
        Utils.setForegroundUserId(callingUser.getIdentifier());

        mUsersToRemove = new ArrayList<>();
        mUserManager = UserManager.get(mContext);
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_USER_REMOVED.equals(intent.getAction())) {
                    synchronized (mUserRemoveLock) {
                        mUserIdReceived = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                        mUserRemoveLock.notifyAll();
                    }
                } else if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
                    synchronized (mUserSwitchLock) {
                        mUserIdReceived = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                        Utils.setForegroundUserId(mForegroundUserId);
                        mUserSwitchLock.notifyAll();
                    }
                }
            }
        }, filter);
    }

    @After
    public void tearDown() {
        for (Integer userId : mUsersToRemove) {
            removeUser(userId);
        }
        Utils.setForegroundUserId(mForegroundUserId);
    }


    @Test
    public void byteArrayToShort() {
        byte[] valueBuf = new byte[] {0x01, 0x02};
        short s = Utils.byteArrayToShort(valueBuf);
        assertThat(s).isEqualTo(0x0201);
    }

    @Test
    public void byteArrayToString() {
        byte[] valueBuf = new byte[] {0x01, 0x02};
        String str = Utils.byteArrayToString(valueBuf);
        assertThat(str).isEqualTo("01 02");
    }

    @Test
    public void uuidsToByteArray() {
        ParcelUuid[] uuids = new ParcelUuid[] {
                new ParcelUuid(new UUID(10, 20)),
                new ParcelUuid(new UUID(30, 40))
        };
        ByteBuffer converter = ByteBuffer.allocate(uuids.length * 16);
        converter.order(ByteOrder.BIG_ENDIAN);
        converter.putLong(0, 10);
        converter.putLong(8, 20);
        converter.putLong(16, 30);
        converter.putLong(24, 40);
        assertThat(Utils.uuidsToByteArray(uuids)).isEqualTo(converter.array());
    }

    @Test
    public void checkServiceAvailable() {
        final String tag = "UTILS_TEST";
        assertThat(Utils.checkServiceAvailable(null, tag)).isFalse();

        ProfileService mockProfile = Mockito.mock(ProfileService.class);
        when(mockProfile.isAvailable()).thenReturn(false);
        assertThat(Utils.checkServiceAvailable(mockProfile, tag)).isFalse();

        when(mockProfile.isAvailable()).thenReturn(true);
        assertThat(Utils.checkServiceAvailable(mockProfile, tag)).isTrue();
    }

    @Test
    public void blockedByLocationOff() throws Exception {
        UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        boolean enableStatus = locationManager.isLocationEnabledForUser(userHandle);
        assertThat(Utils.blockedByLocationOff(mContext, userHandle)).isEqualTo(!enableStatus);

        locationManager.setLocationEnabledForUser(!enableStatus, userHandle);
        assertThat(Utils.blockedByLocationOff(mContext, userHandle)).isEqualTo(enableStatus);

        locationManager.setLocationEnabledForUser(enableStatus, userHandle);
    }

    @Test
    public void checkCallerHasCoarseLocation_doesNotCrash() {
        UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        boolean enabledStatus = locationManager.isLocationEnabledForUser(userHandle);

        locationManager.setLocationEnabledForUser(false, userHandle);
        assertThat(Utils.checkCallerHasCoarseLocation(mContext, null, userHandle)).isFalse();

        locationManager.setLocationEnabledForUser(true, userHandle);
        Utils.checkCallerHasCoarseLocation(mContext, null, userHandle);
        if (!enabledStatus) {
            locationManager.setLocationEnabledForUser(false, userHandle);
        }
    }

    @Test
    public void checkCallerHasCoarseOrFineLocation_doesNotCrash() {
        UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        boolean enabledStatus = locationManager.isLocationEnabledForUser(userHandle);

        locationManager.setLocationEnabledForUser(false, userHandle);
        assertThat(Utils.checkCallerHasCoarseOrFineLocation(mContext, null, userHandle)).isFalse();

        locationManager.setLocationEnabledForUser(true, userHandle);
        Utils.checkCallerHasCoarseOrFineLocation(mContext, null, userHandle);
        if (!enabledStatus) {
            locationManager.setLocationEnabledForUser(false, userHandle);
        }
    }

    @Test
    public void checkPermissionMethod_doesNotCrash() {
        try {
            Utils.checkAdvertisePermissionForDataDelivery(mContext, null, "message");
            Utils.checkAdvertisePermissionForPreflight(mContext);
            Utils.checkCallerHasWriteSmsPermission(mContext);
            Utils.checkScanPermissionForPreflight(mContext);
            Utils.checkConnectPermissionForPreflight(mContext);
        } catch (SecurityException e) {
            // SecurityException could happen.
        }
    }

    @Test
    public void enforceDumpPermission_doesNotCrash() {
        try {
            Utils.enforceDumpPermission(mContext);
        } catch (SecurityException e) {
            // SecurityException could happen.
        }
    }

    @Test
    public void getLoggableAddress() {
        assertThat(Utils.getLoggableAddress(null)).isEqualTo("00:00:00:00:00:00");

        BluetoothDevice device = TestUtils.getTestDevice(BluetoothAdapter.getDefaultAdapter(), 1);
        String loggableAddress = "xx:xx:xx:xx:" + device.getAddress().substring(12);
        assertThat(Utils.getLoggableAddress(device)).isEqualTo(loggableAddress);
    }

    @Test
    public void checkCallerIsSystemMethods_doesNotCrash() {
        Utils.checkCallerIsSystemOrActiveOrManagedUser(mContext, TAG);
        Utils.checkCallerIsSystemOrActiveOrManagedUser(null, TAG);
        Utils.checkCallerIsSystemOrActiveUser(TAG);
    }

    @Test
    public void checkCallerIsSystemMethods_afterSwitchingUser_doesNotCrash() {
        ActivityManager am = mContext.getSystemService(ActivityManager.class);
        int currentUserId = am.getCurrentUser();
        List<UserInfo> userInfos = mUserManager.getUsers();
        UserInfo userToSwitch = null;
        for (UserInfo user : userInfos) {
            if (user.id == currentUserId) {
                continue;
            } else {
                userToSwitch = user;
                break;
            }
        }
        if (userToSwitch == null) {
            userToSwitch = createUser("guest:", UserInfo.FLAG_GUEST);
        }

        synchronized (mUserSwitchLock) {
            am.switchUser(userToSwitch.id);
            Utils.checkCallerIsSystemOrActiveOrManagedUser(mContext, TAG);
            Utils.checkCallerIsSystemOrActiveOrManagedUser(null, TAG);
            Utils.checkCallerIsSystemOrActiveUser(TAG);

            try {
                while (mUserIdReceived != userToSwitch.id) {
                    mUserSwitchLock.wait(SWITCH_USER_TIMEOUT_MILLIS);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized (mUserSwitchLock) {
            am.switchUser(currentUserId);
            Utils.checkCallerIsSystemOrActiveOrManagedUser(mContext, TAG);
            Utils.checkCallerIsSystemOrActiveOrManagedUser(null, TAG);
            Utils.checkCallerIsSystemOrActiveUser(TAG);

            try {
                while (mUserIdReceived != currentUserId) {
                    mUserSwitchLock.wait(SWITCH_USER_TIMEOUT_MILLIS);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void testCopyStream() throws Exception {
        byte[] data = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bufferSize = 4;

        Utils.copyStream(in, out, bufferSize);

        assertThat(out.toByteArray()).isEqualTo(data);
    }

    @Test
    public void debugGetAdapterStateString() {
        assertThat(Utils.debugGetAdapterStateString(BluetoothAdapter.STATE_OFF))
                .isEqualTo("STATE_OFF");
        assertThat(Utils.debugGetAdapterStateString(BluetoothAdapter.STATE_ON))
                .isEqualTo("STATE_ON");
        assertThat(Utils.debugGetAdapterStateString(BluetoothAdapter.STATE_TURNING_ON))
                .isEqualTo("STATE_TURNING_ON");
        assertThat(Utils.debugGetAdapterStateString(BluetoothAdapter.STATE_TURNING_OFF))
                .isEqualTo("STATE_TURNING_OFF");
        assertThat(Utils.debugGetAdapterStateString(-124))
                .isEqualTo("UNKNOWN");
    }

    @Test
    public void ellipsize() {
        if (!Build.TYPE.equals("user")) {
            // Only ellipsize release builds
            String input = "a_long_string";
            assertThat(Utils.ellipsize(input)).isEqualTo(input);
            return;
        }

        assertThat(Utils.ellipsize("ab")).isEqualTo("ab");
        assertThat(Utils.ellipsize("abc")).isEqualTo("a⋯c");
        assertThat(Utils.ellipsize(null)).isEqualTo(null);
    }

    @Test
    public void safeCloseStream_inputStream_doesNotCrash() throws Exception {
        InputStream is = mock(InputStream.class);
        Utils.safeCloseStream(is);
        verify(is).close();

        Mockito.clearInvocations(is);
        doThrow(new IOException()).when(is).close();
        Utils.safeCloseStream(is);
    }

    @Test
    public void safeCloseStream_outputStream_doesNotCrash() throws Exception {
        OutputStream os = mock(OutputStream.class);
        Utils.safeCloseStream(os);
        verify(os).close();

        Mockito.clearInvocations(os);
        doThrow(new IOException()).when(os).close();
        Utils.safeCloseStream(os);
    }

    private UserInfo createUser(String name, int flags) {
        UserInfo user = mUserManager.createUser(name, flags);
        if (user != null) {
            mUsersToRemove.add(user.id);
        }
        return user;
    }

    private void removeUser(int userId) {
        synchronized (mUserRemoveLock) {
            mUserManager.removeUser(userId);
            waitForUserRemovalLocked(userId);
        }
    }

    @GuardedBy("mUserRemoveLock")
    private void waitForUserRemovalLocked(int userId) {
        long time = System.currentTimeMillis();
        while (mUserManager.getAliveUsers().stream().anyMatch(x -> x.id == userId)) {
            try {
                mUserRemoveLock.wait(REMOVE_CHECK_INTERVAL_MILLIS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            if (System.currentTimeMillis() - time > REMOVE_TIMEOUT_MILLIS) {
                Log.e(TAG,"Timeout waiting for removeUser. userId = " + userId);
                break;
            }
        }
    }
}
