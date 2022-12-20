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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothGatt;
import android.content.AttributionSource;
import android.os.Binder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * This class provides a way to control an active distance measurement session.
 * <p>It also defines the required {@link DistanceMeasurementSession.Callback} that must be
 * implemented in order to be notified of distance measuremen results and status events related to
 * the {@link DistanceMeasurementSession}.
 *
 * <p>To get an instance of {@link DistanceMeasurementSession}, first use
 * {@link DistanceMeasurementManager#startMeasurementSession(DistanceMeasurementParams, Executor,
 * DistanceMeasurementSession.Callback)} to request to start a session. Once the session is started,
 * a {@link DistanceMeasurementSession} object is provided through
 * {@link DistanceMeasurementSession.Callback#onStarted(DistanceMeasurementSession)}.
 * If starting a session fails, the failure is reported through
 * {@link DistanceMeasurementSession.Callback#onStartFail(int)} with the failure reason.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementSession {
    private static final String TAG = "DistanceMeasurementSession";

    private final IBluetoothGatt mGatt;
    private final ParcelUuid mUuid;
    private final DistanceMeasurementParams mDistanceMeasurementParams;
    private final Executor mExecutor;
    private final Callback mCallback;
    private final AttributionSource mAttributionSource;

    /**
     * @hide
     */
    public DistanceMeasurementSession(IBluetoothGatt gatt, ParcelUuid uuid,
            DistanceMeasurementParams params, Executor executor,
            AttributionSource attributionSource, Callback callback) {
        mGatt = gatt;
        mUuid = uuid;
        mDistanceMeasurementParams = params;
        mExecutor = executor;
        mAttributionSource = attributionSource;
        mCallback = callback;
    }

    /**
     * Stops actively ranging.
     *
     * @hide
     */
    @SystemApi
    public void stopSession() {
        try {
            final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
            mGatt.stopDistanceMeasurement(mUuid, mDistanceMeasurementParams.getDevice(),
                    mDistanceMeasurementParams.getMethod(), mAttributionSource, recv);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop session - ", e);
        }
    }

    /**
     * @hide
     */
    void onStarted() {
        executeCallback(() -> mCallback.onStarted(this));
    }

    /**
     * @hide
     */
    void onStartFail(int reason) {
        executeCallback(() -> mCallback.onStartFail(reason));
    }


    /**
     * @hide
     */
    void onStopped(int reason) {
        executeCallback(() -> mCallback.onStopped(this, reason));
    }

    /**
     * @hide
     */
    void onResult(@NonNull BluetoothDevice device,
            @NonNull DistanceMeasurementResult result) {
        executeCallback(() -> mCallback.onResult(device, result));
    }


    /**
     * @hide
     */
    private void executeCallback(@NonNull Runnable runnable) {
        final long identity = Binder.clearCallingIdentity();
        try {
            mExecutor.execute(runnable);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * Interface for receiving {@link DistanceMeasurementSession} events.
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                REASON_UNKNOWN,
                REASON_FEATURE_NOT_SUPPORTED_LOCAL,
                REASON_FEATURE_NOT_SUPPORTED_REMOTE,
                REASON_LOCAL_REQUEST,
                REASON_REMOTE_REQUEST,
                REASON_DURATION_TIMEOUT,
                REASON_NO_LE_CONNECTION,
                REASON_INVALID_PARAMETERS,
                REASON_INTERNAL_ERROR,
        })
        @interface Reason {}

        /**
         * Unknown reason.
         *
         * @hide
         */
        @SystemApi
        int REASON_UNKNOWN = 0;

        /**
         * Feature is not supported by local device.
         *
         * @hide
         */
        @SystemApi
        int REASON_FEATURE_NOT_SUPPORTED_LOCAL = 1;

        /**
         * Feature is not supported by remote device.
         *
         * @hide
         */
        @SystemApi
        int REASON_FEATURE_NOT_SUPPORTED_REMOTE = 2;

        /**
         * A local API call triggered the change, such as a call to
         * DistanceMeasurementSession.stopSession().
         *
         * @hide
         */
        @SystemApi
        int REASON_LOCAL_REQUEST = 3;

        /**
         * Remote device triggered the change.
         *
         * @hide
         */
        @SystemApi
        int REASON_REMOTE_REQUEST = 4;

        /**
         * Duration timeout.
         *
         * @hide
         */
        @SystemApi
        int REASON_DURATION_TIMEOUT = 5;


        /**
         * LE connection is required but not exist or disconnected.
         *
         * @hide
         */
        @SystemApi
        int REASON_NO_LE_CONNECTION = 6;

        /**
         * Invalid parameters.
         *
         * @hide
         */
        @SystemApi
        int REASON_INVALID_PARAMETERS = 7;

        /**
         * Internal error, such as read RSSI data fail.
         *
         * @hide
         */
        @SystemApi
        int REASON_INTERNAL_ERROR = 8;

        /**
         * Invoked when {@link DistanceMeasurementManager#startMeasurementSession(
         * DistanceMeasurementParams, Executor, DistanceMeasurementSession.Callback)} is successful.
         *
         * @param session the started {@link DistanceMeasurementSession}
         *
         * @hide
         */
        @SystemApi
        void onStarted(@NonNull DistanceMeasurementSession session);

         /**
         * Invoked if {@link DistanceMeasurementManager#startMeasurementSession(
         * DistanceMeasurementParams, Executor, DistanceMeasurementSession.Callback)} fails.
         *
         * @param reason the failure reason
         *
         * @hide
         */
        @SystemApi
        void onStartFail(@NonNull @Reason int reason);

        /**
         * Invoked when a distance measurement session stopped.
         *
         * @param reason reason for the session stop.
         *
         * @hide
         */
        @SystemApi
        void onStopped(@NonNull DistanceMeasurementSession session, @NonNull @Reason int reason);

        /**
         * Invoked when get distance measurement result.
         *
         * @param device remote device.
         * @param result {@link DistanceMeasurementResult} for this device.
         *
         * @hide
         */
        @SystemApi
        void onResult(@NonNull BluetoothDevice device,
                @NonNull DistanceMeasurementResult result);
    }
}
