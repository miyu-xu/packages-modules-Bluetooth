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

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Result of distance measurement.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementResult implements Parcelable {

    private final double mMeters;
    private final double mErrorMeters;

    private DistanceMeasurementResult(double meters, double errorMeters) {
        mMeters = meters;
        mErrorMeters = errorMeters;
    }

    /**
     * Distance measurement in meters.
     *
     * @return distance in meters.
     *
     * @hide
     */
    @SystemApi
    public double getMeters() {
        return mMeters;
    }

    /**
     * Error of distance measurement in meters.
     * <p>Must be positive.
     *
     * @return error of distance measurement in meters.
     *
     * @hide
     */
    @SystemApi
    @FloatRange(from = 0.0)
    public double getErrorMeters() {
        return mErrorMeters;
    }


    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(mMeters);
        out.writeDouble(mErrorMeters);
    }

    /** @hide **/
    @Override
    public String toString() {
        return "DistanceMeasurement["
                + "meters: " + mMeters
                + ", errorMeters: " + mErrorMeters
                + "]";
    }

    /**
     * A {@link Parcelable.Creator} to create {@link DistanceMeasurementResult} from parcel.
     *
     */
    public static final @NonNull Parcelable.Creator<DistanceMeasurementResult> CREATOR =
            new Parcelable.Creator<DistanceMeasurementResult>() {
                @Override
                public @NonNull DistanceMeasurementResult createFromParcel(@NonNull Parcel in) {
                    Builder builder = new Builder();
                    builder.setMeters(in.readDouble());
                    builder.setErrorMeters(in.readDouble());
                    return builder.build();
                }

                @Override
                public @NonNull DistanceMeasurementResult[] newArray(int size) {
                    return new DistanceMeasurementResult[size];
                }
        };

    /**
     * Builder for {@link DistanceMeasurementResult}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private double mMeters = Double.NaN;
        private double mErrorMeters = Double.NaN;

         /**
         * Set the distance measurement in meters.
         *
         * @param meters distance in meters.
         * @throws IllegalArgumentException if meters is NaN.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setMeters(double meters) {
            if (Double.isNaN(meters)) {
                throw new IllegalArgumentException("meters cannot be NaN");
            }
            mMeters = meters;
            return this;
        }

        /**
         * Set the distance error in meters.
         *
         * @param errorMeters distance error in meters.
         * @throws IllegalArgumentException if error is negative or NaN.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setErrorMeters(@FloatRange(from = 0.0) double errorMeters) {
            if (Double.isNaN(errorMeters) || errorMeters < 0.0) {
                throw new IllegalArgumentException(
                        "errorMeters must be >= 0.0 and not NaN: " + errorMeters);
            }
            mErrorMeters = errorMeters;
            return this;
        }

        /**
         * Builds the {@link DistanceMeasurement} object.
         *
         * @throws IllegalStateException if meters, error, or confidence are not set.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public DistanceMeasurementResult build() {
            if (Double.isNaN(mMeters)) {
                throw new IllegalStateException("Meters cannot be NaN");
            }

            if (Double.isNaN(mErrorMeters)) {
                throw new IllegalStateException("Error meters cannot be NaN");
            }
            return new DistanceMeasurementResult(mMeters, mErrorMeters);
        }
    }
}
