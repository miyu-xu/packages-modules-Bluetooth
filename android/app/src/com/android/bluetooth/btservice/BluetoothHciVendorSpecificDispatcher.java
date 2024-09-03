/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.IBluetoothHciVendorSpecificCallback;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

class BluetoothHciVendorSpecificDispatcher {
    private static final String TAG = "BluetoothHciVendorSpecificDispatcher";

    private final class Registration implements IBinder.DeathRecipient {
        final IBluetoothHciVendorSpecificCallback mCallback;
        final Set<Integer> mEventCodes;
        final UUID mUuid;

        Registration(IBluetoothHciVendorSpecificCallback callback, Set<Integer> eventCodes) {
            mCallback = callback;
            mEventCodes = eventCodes;
            mUuid = UUID.randomUUID();
        }

        public void binderDied() {
            synchronized (mRegistrations) {
                mRegistrations.remove(this);
            }
        }
    }

    @GuardedBy("mRegistrations")
    private final ArrayList<Registration> mRegistrations = new ArrayList<>();

    int unregister(IBluetoothHciVendorSpecificCallback callback) {
        synchronized (mRegistrations) {
            try {
                Registration registration =
                        mRegistrations.stream()
                                .filter((r) -> r.mCallback == callback)
                                .findAny()
                                .get();

                registration.mCallback.asBinder().unlinkToDeath(registration, 0);
                mRegistrations.remove(registration);

            } catch (NoSuchElementException e) {
                Log.e(TAG, "callback was never registered");
                return BluetoothStatusCodes.ERROR_CALLBACK_NOT_REGISTERED;
            }
        }

        return BluetoothStatusCodes.SUCCESS;
    }

    int register(IBluetoothHciVendorSpecificCallback callback, Set<Integer> eventCodes) {
        synchronized (mRegistrations) {
            if (mRegistrations.stream().anyMatch((r) -> r.mCallback == callback)) {
                Log.e(TAG, "callback already registered");
                return BluetoothStatusCodes.NOT_ALLOWED;
            }

            try {
                Registration registration = new Registration(callback, eventCodes);
                unregister(callback);
                callback.asBinder().linkToDeath(registration, 0);
                mRegistrations.add(registration);
            } catch (RemoteException e) {
                return BluetoothStatusCodes.ERROR_UNKNOWN;
            }
        }

        return BluetoothStatusCodes.SUCCESS;
    }

    Optional<byte[]> getRegisteredCookie(IBluetoothHciVendorSpecificCallback callback) {
        synchronized (mRegistrations) {
            try {
                Registration registration =
                        mRegistrations.stream()
                                .filter((r) -> r.mCallback == callback)
                                .findAny()
                                .get();
                ByteBuffer cookieBb = ByteBuffer.allocate(16);
                cookieBb.putLong(registration.mUuid.getMostSignificantBits());
                cookieBb.putLong(registration.mUuid.getLeastSignificantBits());
                return Optional.of(cookieBb.array());
            } catch (NoSuchElementException e) {
                return Optional.empty();
            }
        }
    }

    void dispatchCommandReturn(
            byte[] cookie, Consumer<IBluetoothHciVendorSpecificCallback> action) {
        ByteBuffer cookieBb = ByteBuffer.wrap(cookie);
        UUID uuid = new UUID(cookieBb.getLong(), cookieBb.getLong());
        synchronized (mRegistrations) {
            try {
                Registration registration =
                        mRegistrations.stream().filter((r) -> r.mUuid == uuid).findAny().get();
                action.accept(registration.mCallback);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Command return owner not registered");
                return;
            }
        }
    }

    void broadcastEvent(int eventCode, Consumer<IBluetoothHciVendorSpecificCallback> action) {
        Stream<Registration> stream;
        synchronized (mRegistrations) {
            stream = mRegistrations.stream().filter((r) -> r.mEventCodes.contains(eventCode));
        }

        stream.forEach((r) -> action.accept(r.mCallback));
    }
}
