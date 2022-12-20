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

package android.bluetooth.le;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.content.AttributionSource;
import android.os.CancellationSignal;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.Hashtable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * This class provides methods to perform distance measurement related
 * operations. An application can start distance measurement by using
 * {@link DistanceMeasurementManagerr#startMeasurementSession}.
 * <p>
 * Use {@link BluetoothAdapter#getDistanceMeasurementManager()} to get an instance of
 * {@link DistanceMeasurementManager}.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementManager {
    private static final String TAG = "DistanceMeasurementManager";

    private final Hashtable<BluetoothDevice, DistanceMeasurementSession> mSessionTable =
            new Hashtable<>();
    private final BluetoothAdapter mBluetoothAdapter;
    private final IBluetoothManager mBluetoothManager;
    private final AttributionSource mAttributionSource;
    private final ParcelUuid mUuid;

    /**
     * Use {@link BluetoothAdapter.getDistanceMeasurementManager()} instead.
     *
     * @hide
     */
    public DistanceMeasurementManager(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = Objects.requireNonNull(bluetoothAdapter);
        mBluetoothManager = mBluetoothAdapter.getBluetoothManager();
        mAttributionSource = mBluetoothAdapter.getAttributionSource();
        mUuid = new ParcelUuid(UUID.randomUUID());
    }

    /**
     * Start distance measurement and create a {@link DistanceMeasurementSession} for this
     * operation. Once the session is started, a {@link DistanceMeasurementSession} object is
     * provided through
     * {@link DistanceMeasurementSession.Callback#onStarted(DistanceMeasurementSession)}.
     * If starting a session fails, the failure is reported through
     * {@link DistanceMeasurementSession.Callback#onStartFail(int)} with the failure reason.
     *
     * All input parameters should not be null or {@link NullPointerException} will be triggered.
     * @param params {@link DistanceMeasurementParams} of this operation.
     * @param executor {@link Executor} to run callbacks
     * @param callback {@link DistanceMeasurementSession.Callback} to associate with the
     *                 {@link DistanceMeasurementSession} that is being started.
     * @return a {@link CancellationSignal} that may be used to cancel the starting of the
     *         {@link DistanceMeasurementSession}.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public CancellationSignal startMeasurementSession(
            @NonNull DistanceMeasurementParams params,
            @NonNull Executor executor,
            @NonNull DistanceMeasurementSession.Callback callback) {
        try {
            Objects.requireNonNull(params, "params is null");
            Objects.requireNonNull(executor, "executor is null");
            Objects.requireNonNull(callback, "callback is null");

            IBluetoothGatt gatt = mBluetoothManager.getBluetoothGatt();
            DistanceMeasurementSession session = new DistanceMeasurementSession(gatt, mUuid,
                        params, executor, mAttributionSource, callback);
            CancellationSignal cancellationSignal = new CancellationSignal();
            cancellationSignal.setOnCancelListener(() -> session.stopSession());

            if (mSessionTable.containsKey(params.getDevice())) {
                Log.w(TAG, params.getDevice().getAnonymizedAddress() + " already registered");
                return cancellationSignal;
            }

            mSessionTable.put(params.getDevice(), session);
            final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
            gatt.startDistanceMeasurement(mUuid, params, mCallbackWrapper, mAttributionSource,
                    recv);
            return cancellationSignal;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get Bluetooth gatt - ", e);
            throw new IllegalStateException("Failed to get BluetoothGatt");
        }
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IDistanceMeasurementCallback mCallbackWrapper =
            new IDistanceMeasurementCallback.Stub() {
        @Override
        public void onStarted(BluetoothDevice device) {
            DistanceMeasurementSession session = mSessionTable.get(device);
            session.onStarted();
        }

        @Override
        public void onStartFail(BluetoothDevice device, int reason) {
            DistanceMeasurementSession session = mSessionTable.get(device);
            session.onStartFail(reason);
            mSessionTable.remove(device);
        }

        @Override
        public void onStopped(BluetoothDevice device, int reason) {
            DistanceMeasurementSession session = mSessionTable.get(device);
            session.onStopped(reason);
            mSessionTable.remove(device);
        }

        @Override
        public void onResult(BluetoothDevice device, DistanceMeasurementResult result) {
            DistanceMeasurementSession session = mSessionTable.get(device);
            session.onResult(device, result);
        }
    };
}
